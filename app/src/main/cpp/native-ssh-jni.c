#include <jni.h>
#include <android/log.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/select.h>
#include <netinet/in.h>
#include <netinet/tcp.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <fcntl.h>
#include <errno.h>
#include <libssh2.h>

#define LOG_TAG "NativeSshTunnel"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#define MAX_CLIENTS 64
#define BUF_SIZE 16384

typedef enum {
    CLIENT_STATE_UNUSED = 0,
    CLIENT_STATE_GREETING,
    CLIENT_STATE_REQUEST,
    CLIENT_STATE_CONNECTING,
    CLIENT_STATE_ACTIVE,
    CLIENT_STATE_CLOSING
} client_state_t;

typedef struct {
    client_state_t state;
    int fd;
    LIBSSH2_CHANNEL *channel;
    
    char dst_host[256];
    int dst_port;
    
    char ssh_tx_buf[BUF_SIZE];
    size_t ssh_tx_len;
    size_t ssh_tx_offset;
    
    char local_tx_buf[BUF_SIZE];
    size_t local_tx_len;
    size_t local_tx_offset;
    
    int eof_received;
} tunnel_client_t;

static volatile int ssh_running = 0;
static int server_fd = -1;
static LIBSSH2_SESSION *global_session = NULL;
static int global_sock = -1;

static tunnel_client_t clients[MAX_CLIENTS];

static void release_start_strings(
    JNIEnv *env, jstring host, const char *c_host,
    jstring username, const char *c_user,
    jstring password, const char *c_pass
) {
    if (c_host) (*env)->ReleaseStringUTFChars(env, host, c_host);
    if (c_user) (*env)->ReleaseStringUTFChars(env, username, c_user);
    if (c_pass) (*env)->ReleaseStringUTFChars(env, password, c_pass);
}

static void set_nonblock(int fd) {
    int flags = fcntl(fd, F_GETFL, 0);
    fcntl(fd, F_SETFL, flags | O_NONBLOCK);
}

static void set_tcp_opts(int fd) {
    int opt = 1;
    setsockopt(fd, IPPROTO_TCP, TCP_NODELAY, &opt, sizeof(opt));
    setsockopt(fd, SOL_SOCKET, SO_KEEPALIVE, &opt, sizeof(opt));
}

static int connect_tcp(const char *host, int port) {
    struct addrinfo hints, *result;
    memset(&hints, 0, sizeof(hints));
    hints.ai_family = AF_UNSPEC;
    hints.ai_socktype = SOCK_STREAM;
    hints.ai_protocol = IPPROTO_TCP;

    char port_str[16];
    snprintf(port_str, sizeof(port_str), "%d", port);

    if (getaddrinfo(host, port_str, &hints, &result) != 0) {
        LOGE("Failed to resolve host %s", host);
        return -1;
    }

    int sock = -1;
    for (struct addrinfo *rp = result; rp != NULL; rp = rp->ai_next) {
        sock = socket(rp->ai_family, rp->ai_socktype, rp->ai_protocol);
        if (sock == -1) continue;

        struct timeval timeout = {15, 0};
        setsockopt(sock, SOL_SOCKET, SO_RCVTIMEO, &timeout, sizeof(timeout));
        setsockopt(sock, SOL_SOCKET, SO_SNDTIMEO, &timeout, sizeof(timeout));

        if (connect(sock, rp->ai_addr, rp->ai_addrlen) != -1) {
            set_tcp_opts(sock);
            break;
        }
        close(sock);
        sock = -1;
    }

    freeaddrinfo(result);
    return sock;
}

static LIBSSH2_SESSION* create_session(int sock) {
    LIBSSH2_SESSION *session = libssh2_session_init();
    if (!session) {
        LOGE("Failed to initialize LIBSSH2 session");
        return NULL;
    }

    libssh2_session_set_blocking(session, 1);

    int rc = libssh2_session_handshake(session, sock);
    if (rc != 0) {
        LOGE("SSH handshake failed (%d)", rc);
        libssh2_session_free(session);
        return NULL;
    }
    
    // Host Key Verification Logging
    const char *fingerprint = libssh2_hostkey_hash(session, LIBSSH2_HOSTKEY_HASH_SHA1);
    if (fingerprint) {
        char hash_str[41] = {0};
        for (int i = 0; i < 20; i++) {
            sprintf(&hash_str[i * 2], "%02X", (unsigned char)fingerprint[i]);
        }
        LOGI("Host key fingerprint (SHA1): %s", hash_str);
        // TODO: Implement known_hosts validation here in the future
    } else {
        LOGI("Could not get host key fingerprint.");
    }

    return session;
}

static int authenticate(LIBSSH2_SESSION *session, const char *username, const char *password) {
    int rc = libssh2_userauth_password(session, username, password);
    if (rc != 0) {
        LOGE("SSH authentication failed for user %s", username);
        return -1;
    }
    return 0;
}

static int create_listener(int socksPort) {
    int fd = socket(AF_INET, SOCK_STREAM, 0);
    if (fd == -1) {
        LOGE("Failed to create local server socket");
        return -1;
    }

    int opt = 1;
    setsockopt(fd, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));

    struct sockaddr_in local_sin;
    local_sin.sin_family = AF_INET;
    local_sin.sin_addr.s_addr = htonl(INADDR_LOOPBACK);
    local_sin.sin_port = htons(socksPort);

    if (bind(fd, (struct sockaddr *)&local_sin, sizeof(local_sin)) < 0) {
        LOGE("Failed to bind local SOCKS server to port %d", socksPort);
        close(fd);
        return -1;
    }

    if (listen(fd, 10) < 0) {
        LOGE("Failed to listen on local SOCKS server");
        close(fd);
        return -1;
    }

    set_nonblock(fd);
    return fd;
}

static void cleanup_client(int i) {
    if (clients[i].channel) {
        libssh2_channel_free(clients[i].channel);
        clients[i].channel = NULL;
    }
    if (clients[i].fd != -1) {
        close(clients[i].fd);
        clients[i].fd = -1;
    }
    clients[i].state = CLIENT_STATE_UNUSED;
}

static void transition_to_closing(int i) {
    tunnel_client_t *c = &clients[i];
    if (c->state != CLIENT_STATE_CLOSING) {
        c->state = CLIENT_STATE_CLOSING;
    }
}

static void handle_client(int i, fd_set *rfds, fd_set *wfds) {
    tunnel_client_t *c = &clients[i];

    if (c->state == CLIENT_STATE_GREETING) {
        if (FD_ISSET(c->fd, rfds)) {
            char buf[256];
            ssize_t n = recv(c->fd, buf, sizeof(buf), 0);
            if (n > 0) {
                if (buf[0] == 0x05) {
                    char reply[] = {0x05, 0x00};
                    send(c->fd, reply, 2, 0);
                    c->state = CLIENT_STATE_REQUEST;
                } else {
                    transition_to_closing(i);
                }
            } else if (n == 0 || (n < 0 && errno != EAGAIN && errno != EWOULDBLOCK)) {
                transition_to_closing(i);
            }
        }
        return;
    }

    if (c->state == CLIENT_STATE_REQUEST) {
        if (FD_ISSET(c->fd, rfds)) {
            char buf[512];
            ssize_t n = recv(c->fd, buf, sizeof(buf), 0);
            if (n > 4 && buf[0] == 0x05 && buf[1] == 0x01) {
                int atyp = buf[3];
                c->dst_host[0] = '\0';
                c->dst_port = 0;
                
                if (atyp == 0x01) { // IPv4
                    if (n >= 10) {
                        snprintf(c->dst_host, sizeof(c->dst_host), "%d.%d.%d.%d",
                            (unsigned char)buf[4], (unsigned char)buf[5],
                            (unsigned char)buf[6], (unsigned char)buf[7]);
                        c->dst_port = ((unsigned char)buf[8] << 8) | ((unsigned char)buf[9]);
                    }
                } else if (atyp == 0x03) { // Domain
                    int len = (unsigned char)buf[4];
                    if (n >= 5 + len + 2) {
                        memcpy(c->dst_host, buf + 5, len);
                        c->dst_host[len] = '\0';
                        c->dst_port = ((unsigned char)buf[5+len] << 8) | ((unsigned char)buf[6+len]);
                    }
                } else if (atyp == 0x04) { // IPv6
                    LOGE("SOCKS5 IPv6 not fully supported here");
                }

                if (c->dst_host[0] != '\0' && c->dst_port > 0) {
                    c->state = CLIENT_STATE_CONNECTING;
                    LOGI("SOCKS5 request to %s:%d", c->dst_host, c->dst_port);
                } else {
                    transition_to_closing(i);
                }
            } else if (n == 0 || (n < 0 && errno != EAGAIN && errno != EWOULDBLOCK)) {
                transition_to_closing(i);
            }
        }
        return;
    }

    if (c->state == CLIENT_STATE_CONNECTING) {
        c->channel = libssh2_channel_direct_tcpip_ex(global_session, c->dst_host, c->dst_port, "127.0.0.1", 0);
        if (c->channel) {
            char reply[] = {0x05, 0x00, 0x00, 0x01, 0,0,0,0, 0,0};
            send(c->fd, reply, 10, 0);
            c->state = CLIENT_STATE_ACTIVE;
            LOGI("Direct TCP/IP channel opened to %s:%d", c->dst_host, c->dst_port);
        } else {
            int err = libssh2_session_last_error(global_session, NULL, NULL, 0);
            if (err != LIBSSH2_ERROR_EAGAIN) {
                LOGE("Channel open failed to %s:%d: %d", c->dst_host, c->dst_port, err);
                char reply[] = {0x05, 0x01, 0x00, 0x01, 0,0,0,0, 0,0};
                send(c->fd, reply, 10, 0);
                transition_to_closing(i);
            }
        }
        return;
    }
    
    if (c->state == CLIENT_STATE_CLOSING) {
        if (c->channel) {
            int rc = libssh2_channel_close(c->channel);
            if (rc == LIBSSH2_ERROR_EAGAIN) return;
            
            rc = libssh2_channel_wait_closed(c->channel);
            if (rc == LIBSSH2_ERROR_EAGAIN) return;
            
            libssh2_channel_free(c->channel);
            c->channel = NULL;
        }
        if (c->fd != -1) {
            close(c->fd);
            c->fd = -1;
        }
        c->state = CLIENT_STATE_UNUSED;
        LOGI("Client slot %d fully closed", i);
        return;
    }
    
    // STATE_ACTIVE
    
    // 1. Drain local_tx_buf to local fd
    if (c->local_tx_len > 0 && FD_ISSET(c->fd, wfds)) {
        ssize_t s = send(c->fd, c->local_tx_buf + c->local_tx_offset, c->local_tx_len, 0);
        if (s > 0) {
            c->local_tx_offset += s;
            c->local_tx_len -= s;
            if (c->local_tx_len == 0) c->local_tx_offset = 0;
        } else if (s < 0 && errno != EAGAIN && errno != EWOULDBLOCK) {
            transition_to_closing(i);
            return;
        }
    }
    
    // 2. Read from SSH to local_tx_buf
    if (c->local_tx_len == 0 && !c->eof_received) {
        while (1) {
            ssize_t nread = libssh2_channel_read(c->channel, c->local_tx_buf, BUF_SIZE);
            if (nread > 0) {
                c->local_tx_len = nread;
                c->local_tx_offset = 0;
                
                // Fast path send
                ssize_t s = send(c->fd, c->local_tx_buf, c->local_tx_len, 0);
                if (s > 0) {
                    c->local_tx_offset += s;
                    c->local_tx_len -= s;
                    if (c->local_tx_len == 0) c->local_tx_offset = 0;
                } else if (s < 0 && errno != EAGAIN && errno != EWOULDBLOCK) {
                    transition_to_closing(i);
                    return;
                }
                
                if (c->local_tx_len > 0) {
                    break; // Flow control: stop reading from SSH
                }
            } else if (nread == LIBSSH2_ERROR_EAGAIN) {
                break;
            } else if (nread == LIBSSH2_ERROR_CHANNEL_CLOSED || nread == 0) {
                c->eof_received = 1;
                break;
            } else {
                LOGE("SSH read error %zd", nread);
                if (nread == LIBSSH2_ERROR_SOCKET_SEND || nread == LIBSSH2_ERROR_SOCKET_DISCONNECT || nread == LIBSSH2_ERROR_SOCKET_RECV) {
                    ssh_running = 0;
                } else {
                    transition_to_closing(i);
                }
                return;
            }
        }
    }
    
    // 3. Read from local fd to ssh_tx_buf
    if (c->ssh_tx_len == 0 && FD_ISSET(c->fd, rfds)) {
        ssize_t n = recv(c->fd, c->ssh_tx_buf, BUF_SIZE, 0);
        if (n > 0) {
            c->ssh_tx_len = n;
            c->ssh_tx_offset = 0;
        } else if (n == 0 || (n < 0 && errno != EAGAIN && errno != EWOULDBLOCK)) {
            // EOF from local
            transition_to_closing(i);
            return;
        }
    }
    
    // 4. Drain ssh_tx_buf to SSH channel
    if (c->ssh_tx_len > 0) {
        while (c->ssh_tx_len > 0) {
            ssize_t nw = libssh2_channel_write(c->channel, c->ssh_tx_buf + c->ssh_tx_offset, c->ssh_tx_len);
            if (nw > 0) {
                c->ssh_tx_offset += nw;
                c->ssh_tx_len -= nw;
                if (c->ssh_tx_len == 0) c->ssh_tx_offset = 0;
            } else if (nw == LIBSSH2_ERROR_EAGAIN) {
                break;
            } else {
                LOGE("SSH write error %zd", nw);
                if (nw == LIBSSH2_ERROR_SOCKET_SEND || nw == LIBSSH2_ERROR_SOCKET_DISCONNECT || nw == LIBSSH2_ERROR_SOCKET_RECV) {
                    ssh_running = 0;
                } else {
                    transition_to_closing(i);
                }
                return;
            }
        }
    }
    
    // 5. Check SSH EOF
    if ((c->eof_received || libssh2_channel_eof(c->channel)) && c->local_tx_len == 0) {
        transition_to_closing(i);
        return;
    }
}

static void accept_loop() {
    for (int i = 0; i < MAX_CLIENTS; i++) {
        clients[i].state = CLIENT_STATE_UNUSED;
        clients[i].fd = -1;
        clients[i].channel = NULL;
    }

    libssh2_keepalive_config(global_session, 0, 15);

    while (ssh_running) {
        fd_set rfds, wfds;
        FD_ZERO(&rfds);
        FD_ZERO(&wfds);
        
        int max_fd = -1;
        
        if (server_fd != -1) {
            FD_SET(server_fd, &rfds);
            if (server_fd > max_fd) max_fd = server_fd;
        }
        
        int want_ssh_read = 1; // Always want to read for keepalives and incoming data
        int want_ssh_write = 0;
        
        int dir = libssh2_session_block_directions(global_session);
        if (dir & LIBSSH2_SESSION_BLOCK_OUTBOUND) {
            want_ssh_write = 1;
        }

        for (int i = 0; i < MAX_CLIENTS; i++) {
            tunnel_client_t *c = &clients[i];
            
            if (c->state == CLIENT_STATE_GREETING || c->state == CLIENT_STATE_REQUEST) {
                FD_SET(c->fd, &rfds);
                if (c->fd > max_fd) max_fd = c->fd;
            } else if (c->state == CLIENT_STATE_ACTIVE && c->fd != -1) {
                if (c->ssh_tx_len == 0) { // Only read if buffer is empty
                    FD_SET(c->fd, &rfds);
                    if (c->fd > max_fd) max_fd = c->fd;
                }
                if (c->local_tx_len > 0) {
                    FD_SET(c->fd, &wfds);
                    if (c->fd > max_fd) max_fd = c->fd;
                }
            }
        }
        
        if (global_sock != -1) {
            if (want_ssh_read) FD_SET(global_sock, &rfds);
            if (want_ssh_write) FD_SET(global_sock, &wfds);
            if (global_sock > max_fd) max_fd = global_sock;
        }
        
        struct timeval tv = {0, 50000}; // 50ms timeout to prevent stalls and busy loops
        int rc = select(max_fd + 1, &rfds, &wfds, NULL, &tv);
        if (rc < 0) {
            if (errno == EINTR) continue;
            LOGE("select() error: %s", strerror(errno));
            break;
        }
        
        if (global_sock != -1) {
            int next_ka = 0;
            int ka_rc = libssh2_keepalive_send(global_session, &next_ka);
            if (ka_rc < 0 && ka_rc != LIBSSH2_ERROR_EAGAIN) {
                LOGE("Keepalive failed, session dead (%d)", ka_rc);
                break;
            }
        }

        if (server_fd != -1 && FD_ISSET(server_fd, &rfds)) {
            struct sockaddr_in client_addr;
            socklen_t client_len = sizeof(client_addr);
            int client_fd = accept(server_fd, (struct sockaddr *)&client_addr, &client_len);
            if (client_fd >= 0) {
                set_nonblock(client_fd);
                set_tcp_opts(client_fd);
                
                int slot = -1;
                for (int i = 0; i < MAX_CLIENTS; i++) {
                    if (clients[i].state == CLIENT_STATE_UNUSED) {
                        slot = i;
                        break;
                    }
                }
                if (slot != -1) {
                    clients[slot].state = CLIENT_STATE_GREETING;
                    clients[slot].fd = client_fd;
                    clients[slot].channel = NULL;
                    clients[slot].dst_host[0] = '\0';
                    clients[slot].dst_port = 0;
                    clients[slot].ssh_tx_len = 0;
                    clients[slot].ssh_tx_offset = 0;
                    clients[slot].local_tx_len = 0;
                    clients[slot].local_tx_offset = 0;
                    clients[slot].eof_received = 0;
                } else {
                    LOGE("Max clients reached, rejecting connection");
                    close(client_fd);
                }
            }
        }
        
        for (int i = 0; i < MAX_CLIENTS; i++) {
            if (clients[i].state != CLIENT_STATE_UNUSED) {
                handle_client(i, &rfds, &wfds);
            }
        }
    }
}

static void cleanup() {
    for (int i = 0; i < MAX_CLIENTS; i++) {
        if (clients[i].state != CLIENT_STATE_UNUSED) {
            cleanup_client(i);
        }
    }

    if (server_fd != -1) {
        close(server_fd);
        server_fd = -1;
    }

    if (global_session) {
        libssh2_session_set_blocking(global_session, 1);
        libssh2_session_disconnect(global_session, "SSH Tunnel closing");
        libssh2_session_free(global_session);
        global_session = NULL;
    }

    if (global_sock != -1) {
        close(global_sock);
        global_sock = -1;
    }
}

JNIEXPORT jint JNICALL 
Java_com_sivpn_cepat_vpn_NativeSshTunnel_startSshTunnel(
    JNIEnv *env, jclass clazz, jstring host, jint port, 
    jstring username, jstring password, jint socksPort
) {
    if (!host || !username || !password || port < 1 || port > 65535 || socksPort < 1 || socksPort > 65535) {
        LOGE("Invalid JNI arguments");
        return -10;
    }

    const char *c_host = (*env)->GetStringUTFChars(env, host, NULL);
    const char *c_user = (*env)->GetStringUTFChars(env, username, NULL);
    const char *c_pass = (*env)->GetStringUTFChars(env, password, NULL);

    if (!c_host || !c_user || !c_pass || c_host[0] == '\0' || c_user[0] == '\0') {
        LOGE("Failed to convert strings");
        release_start_strings(env, host, c_host, username, c_user, password, c_pass);
        return -11;
    }

    LOGI("Starting SSH Tunnel targeting %s:%d (User: %s)", c_host, port, c_user);

    if (libssh2_init(0) != 0) {
        LOGE("libssh2 initialization failed");
        release_start_strings(env, host, c_host, username, c_user, password, c_pass);
        return -1;
    }

    global_sock = connect_tcp(c_host, port);
    if (global_sock == -1) {
        libssh2_exit();
        release_start_strings(env, host, c_host, username, c_user, password, c_pass);
        return -3;
    }

    global_session = create_session(global_sock);
    if (!global_session) {
        cleanup();
        libssh2_exit();
        release_start_strings(env, host, c_host, username, c_user, password, c_pass);
        return -4;
    }

    if (authenticate(global_session, c_user, c_pass) != 0) {
        cleanup();
        libssh2_exit();
        release_start_strings(env, host, c_host, username, c_user, password, c_pass);
        return -6;
    }

    server_fd = create_listener(socksPort);
    if (server_fd == -1) {
        cleanup();
        libssh2_exit();
        release_start_strings(env, host, c_host, username, c_user, password, c_pass);
        return -7;
    }

    LOGI("SSH Tunnel connected. Local SOCKS on port %d", socksPort);

    // Switch to non-blocking mode for the multiplexing loop
    libssh2_session_set_blocking(global_session, 0);
    ssh_running = 1;

    accept_loop();

    LOGI("SSH Tunnel stopped cleanly");
    cleanup();
    libssh2_exit();
    release_start_strings(env, host, c_host, username, c_user, password, c_pass);
    return 0;
}

JNIEXPORT void JNICALL 
Java_com_sivpn_cepat_vpn_NativeSshTunnel_stopSshTunnel(JNIEnv *env, jclass clazz) {
    LOGI("Requesting components shutdown...");
    ssh_running = 0;
}
