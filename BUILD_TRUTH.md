# TECHNICAL REVIEW: BUILD SYSTEM SEBAGAI SOURCE OF TRUTH

Sesuai instruksi, laporan ini disusun **100% berdasarkan Build System dan Source Code yang benar-benar dikompilasi** pada project ini. Tidak ada asumsi dari repository upstream selain yang ditarik oleh script build.

==================================================
## TAHAP 1: TRACE BUILD SYSTEM
==================================================
**1. Trigger Build**:
- Gradle `build.gradle.kts` mendefinisikan task `compileLibssh2` dan `compileHevTunnel` yang memanggil shell script.
- Task dieksekusi saat proses `preBuild`.

**2. Shell Script (`compile-libssh2.sh`)**:
- Mengecek ketersediaan `NDK_HOME` dan `cmake`.
- Melakukan inisialisasi submodule git (jika belum ada) untuk `libssh2` dan `mbedtls`.
- **Patch Source**: Mengeksekusi script Python untuk menambal file `libssh2/src/misc.h`.
- Memanggil `cmake` menggunakan `app/src/main/cpp/CMakeLists.txt` untuk 4 ABI (armeabi-v7a, arm64-v8a, x86, x86_64).
- Output: `libssh.so` disalin ke `app/src/main/jniLibs/<abi>/`.

**3. Shell Script (`compile-hevtun.sh`)**:
- Mengecek ketersediaan `ndk-build`.
- Membuat file `Android.mk` sementara yang me-link ke folder `hev-socks5-tunnel`.
- Memanggil `ndk-build` dengan `APP_CFLAGS=-O3 -DPKGNAME=hev/socks5`.
- Output: `libhev-socks5-tunnel.so` disalin ke `app/src/main/jniLibs/<abi>/`.

==================================================
## TAHAP 2: SUBMODULE YANG BENAR
==================================================
Berdasarkan file `.gitmodules`:
1. **libssh2**: `https://github.com/libssh2/libssh2.git`
2. **mbedtls**: `https://github.com/Mbed-TLS/mbedtls.git`
3. **hev-socks5-tunnel**: `https://github.com/unarto/hev-socks5-tunnel`

**Fakta Submodule**: Project ini **tidak** menggunakan `heiher/hev-socks5-tunnel` secara langsung, melainkan _fork_ atau repo dari `unarto/hev-socks5-tunnel`.

==================================================
## TAHAP 3: TRACE BUILD GRAPH
==================================================
**Dependency Graph (libssh.so)**:
`compile-libssh2.sh` -> `CMakeLists.txt`
  -> `mbedtls` (compiled as static crypto backend)
  -> `libssh2/src/*.c` (compiled as `libssh2_static`, linked with `zlib` & `mbedcrypto`)
  -> `app/src/main/cpp/native-ssh-jni.c` (JNI Wrapper, compiled as `ssh` SHARED library)
  -> Output: `libssh.so`
  -> Runtime Kotlin: (via `System.loadLibrary("ssh")` di class yang menggunakannya)

**Dependency Graph (libhev-socks5-tunnel.so)**:
`compile-hevtun.sh`
  -> `ndk-build`
  -> `hev-socks5-tunnel/Android.mk`
  -> (Source C dari `unarto/hev-socks5-tunnel`)
  -> Output: `libhev-socks5-tunnel.so`
  -> Runtime Kotlin: `System.loadLibrary("hev-socks5-tunnel")` (di `TProxyService.kt`)

==================================================
## TAHAP 4: PATCH ANALYSIS
==================================================
**Script Pemroses**: `compile-libssh2.sh` (Python inline).
**File yang di-Patch**: `libssh2/src/misc.h`
**Perubahan**: Menambahkan makro/inline function `explicit_bzero(void *buf, size_t len)` menggunakan pointer `volatile` khusus untuk environment `__ANDROID__`.
**Alasan Patch**: NDK Android pada API level tertentu (di bawah 28) tidak menjamin ketersediaan `explicit_bzero` pada libc (`bionic`).
**Dampak Runtime**: Memory buffer (terutama yang menyimpan string JNI _password_ dan _username_) akan di-nol-kan dengan aman tanpa risiko dihapus oleh optimasi _compiler_ (karena pointer `volatile`).
**Status**: Aman & sesuai tujuan keamanan NDK. Tidak ada efek samping merusak.

==================================================
## TAHAP 5 & 6: TRACE JNI & CALL GRAPH LENGKAP
==================================================
### A. JNI `NativeSshTunnel.startSshTunnel`
- **Lokasi File**: `app/src/main/cpp/native-ssh-jni.c`
- **Call Graph**:
  `NativeSshTunnel.startSshTunnel` (Kotlin)
   -> `Java_com_sivpn_cepat_vpn_NativeSshTunnel_startSshTunnel` (JNI C)
      -> `libssh2_init(0)`
      -> `connect_tcp()` (Custom function, memanggil socket connect OS)
      -> `libssh2_session_init()`
      -> `libssh2_session_set_blocking(session, 1)` **(🚨 PERUBAHAN STATE)**
      -> `libssh2_session_handshake()`
      -> `libssh2_userauth_password()`
      -> Membuat socket lokal `server_fd` (port 1080 bind ke `INADDR_LOOPBACK`).
      -> **Blocking Loop**: `while(ssh_running) { accept(server_fd, ...); pthread_create(&tid, pipe_thread) }`

- **Ownership & Lifecycle**:
  - `global_sock` = socket eksternal SSH. (Dimiliki oleh state JNI C).
  - `global_session` = objek `LIBSSH2_SESSION`. (Dimiliki oleh state JNI C).
  - `server_fd` = socket listener SOCKS lokal. (Dimiliki oleh state JNI C).

### B. JNI `NativeSshTunnel.stopSshTunnel`
- **Lokasi File**: `app/src/main/cpp/native-ssh-jni.c`
- **Call Graph**:
  `NativeSshTunnel.stopSshTunnel` (Kotlin)
   -> `Java_com_sivpn_cepat_vpn_NativeSshTunnel_stopSshTunnel` (JNI C)
      -> `ssh_running = 0;`
      -> `shutdown(server_fd, SHUT_RDWR); close(server_fd);`
      -> `shutdown(global_sock, SHUT_RDWR); close(global_sock);`

### C. JNI Thread `pipe_thread`
- **Lokasi File**: `app/src/main/cpp/native-ssh-jni.c`
- **Call Graph**:
  `accept()` loop -> `pthread_create()` -> `pipe_thread()`
   -> `libssh2_channel_direct_tcpip_ex(session, "127.0.0.1", 1080, "127.0.0.1", 0)`
   -> **Loop `select(client_fd)`** (Hanya mendeteksi data masuk dari client SOCKS lokal)
   -> `recv(client_fd)`
   -> `libssh2_channel_write()`
   -> `libssh2_channel_read()` **(🚨 BLOCKING)**
   -> `send(client_fd)`

==================================================
## TAHAP 7: EVIDENCE ONLY (TEMUAN KRITIKAL DARI SOURCE)
==================================================

### TEMUAN 1: DEADLOCK PADA `pipe_thread` (BLOCKING I/O)
- **Bukti Source**: `app/src/main/cpp/native-ssh-jni.c`
- **Kode**:
  Baris 108: `libssh2_session_set_blocking(session, 1);`
  Baris 54: `ssize_t nread_ssh = libssh2_channel_read(channel, buffer, sizeof(buffer));`
- **Analisis**: Pembuat C code menulis komentar `// Read from channel and send to client_fd (non-blocking style check)`. Namun faktanya, `session` telah di-set menjadi **BLOCKING** di baris 108. Akibatnya, pemanggilan `libssh2_channel_read` tidak akan pernah me-return `LIBSSH2_ERROR_EAGAIN`. Thread akan **dibekukan secara absolut** oleh OS hingga ada balasan dari server SSH. Jika tidak ada balasan, thread ini tidak akan pernah kembali ke pengecekan `select(client_fd)`. Time-out `struct timeval tv = {1, 0};` pada `select` menjadi percuma karena thread nyangkut di `libssh2_channel_read`.
- **Status**: **TERBUKTI DARI SOURCE CODE**.

### TEMUAN 2: RACE CONDITION / CORRUPTION PADA LIBSSH2 (TIDAK THREAD-SAFE)
- **Bukti Source**: `app/src/main/cpp/native-ssh-jni.c`
- **Kode**:
  Baris 160: `pthread_create(&tid, NULL, pipe_thread, ta)` -> Memicu multiple thread untuk koneksi konkuren.
  Baris 50: `ssize_t nw = libssh2_channel_write(channel, ...);` (di dalam `pipe_thread`).
- **Analisis**: Setiap `pipe_thread` berbagi pointer `LIBSSH2_SESSION *session` yang sama (dikirim via argumen struct `ta`). Menurut desain arsitektur `libssh2` (yang terverifikasi di source code upstream/submodule), struktur `LIBSSH2_SESSION` **tidak memiliki internal mutex/lock**. Mengeksekusi `libssh2_channel_write` atau `read` secara bersamaan pada *session* yang sama dari thread `pthread` yang berbeda akan mengakibatkan kerusakan *state machine*, korupsi buffer, dan `SIGSEGV` (crash JNI).
- **Status**: **TERBUKTI DARI SOURCE CODE**.

### TEMUAN 3: HEV SOCKS5 TUNNEL CLEANUP
- **Bukti Source**: `hev-socks5-tunnel/src/hev-jni.c`
- **Kode**:
  Fungsi `native_stop_service` memanggil `hev_socks5_tunnel_quit()`, diikuti oleh `pthread_join(work_thread, NULL)`.
- **Analisis**: Ini adalah implementasi pembersihan yang benar dan thread-safe. `quit()` membangunkan epoll loop, dan `pthread_join` menunda return hingga *background thread* benar-benar mati. Kotlin JNI call `TProxyStopService` sangat aman dan diimplementasikan sesuai kontrak.
- **Status**: **TERBUKTI AMAN**.
