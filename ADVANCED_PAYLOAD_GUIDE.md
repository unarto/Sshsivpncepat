# Advanced Payload & DPI Bypass Architecture Guide

This document outlines the advanced payload structures and DPI (Deep Packet Inspection) evasion techniques supported by the SIVPN backend injection engine.

## 1. WebSocket Direct / CDN (Cloudflare & CloudFront)
**Injection Method:** CDN Spoofing & WebSocket Upgrade
**Recommended SNI / Port:** `your-bug-host.com` / 443 (WSS) or 80 (WS)

**Payload Raw Code:**
```
GET / HTTP/1.1[crlf]Host: [host][crlf]Upgrade: websocket[crlf]Connection: Upgrade[crlf]User-Agent: [ua][crlf][crlf]
```

**Technical Explanation:** 
When the CDN (e.g., Cloudflare) receives this request, it inspects the SNI during the TLS handshake (WSS) and routes it to the frontend edge. By initiating a `GET / HTTP/1.1` request with the `Upgrade: websocket` headers, we force the CDN edge server to transition from standard HTTP to a persistent TCP-like WebSocket tunnel. The backend WebSocketEngine handles the RFC 6455 masking and framing seamlessly.

## 2. Front Inject (Enhanced Split)
**Injection Method:** Split Header Evasion (Front Inject)
**Recommended SNI / Port:** Port 80 (HTTP) or 443 (SNI mode)

**Payload Raw Code:**
```
GET http://bug-host.com/ HTTP/1.1[crlf]Host: bug-host.com[crlf][split]CONNECT [host_port] HTTP/1.1[crlf]Host: [host_port][crlf]Connection: Keep-Alive[crlf]User-Agent: [ua][crlf][crlf]
```

**Technical Explanation:**
DPI appliances often look for the `CONNECT` method as an indicator of proxy tunneling. By placing a decoy `GET` request first, followed by a `[split]` (which adds a 200ms TCP packet delay), the DPI reads the benign `GET` request, classifies the TCP stream as legitimate HTTP traffic, and stops monitoring. The subsequent `CONNECT` payload then securely negotiates with the SSH server.

## 3. Host Header Spoofing / Zero-Rating Protocol
**Injection Method:** Host Replacement & Rotation
**Recommended SNI / Port:** 80 (HTTP)

**Payload Raw Code:**
```
CONNECT [host_port] HTTP/1.1[crlf]Host: [rotate=bug1.com;bug2.com;bug3.com][crlf]X-Online-Host: [host][crlf]Connection: Keep-Alive[crlf][crlf]
```

**Technical Explanation:**
This technique targets zero-rated domains (domains free of data charges). By rotating the `Host` header across a list of allowed bug hosts, the ISP's billing system permits the traffic. The `X-Online-Host` or `X-Forwarded-For` header then forwards the real SSH endpoint resolution to the intermediary proxy.

## 4. Multi-Request Instant Split (Advanced DPI Bypass)
**Injection Method:** TCP Window Fragmentation (Instant Split)
**Recommended SNI / Port:** 80 or 8080

**Payload Raw Code:**
```
HEAD / HTTP/1.1[crlf]Host: bug-host.com[crlf][instant_split]CONNECT [host_port] HTTP/1.1[crlf]Host: [host_port][crlf][crlf]
```

**Technical Explanation:**
Unlike a time-delayed split, `[instant_split]` immediately forces the network socket to flush the buffer, splitting the TCP stream at the MTU (Maximum Transmission Unit) level without latency. The firewall receives fragmented packets (`HEAD` first, then `CONNECT`), which prevents simple string-matching DPI boxes from reassembling and identifying the tunnel signature.

---

### Audit Findings & Improvement Roadmap
During the architectural audit of the SIVPN backend, the following improvements have been identified to further optimize performance:

1. **Coroutine Thread Blocking (HttpPayloadEngine):**
   - **Issue:** The `sendPayloadChunks` function uses `Thread.sleep(1000)` and `Thread.sleep(200)` for split delays. In Kotlin Coroutines (Dispatchers.IO), this can block the thread pool, especially when multiple tunnel retries occur simultaneously.
   - **Fix:** Refactor `sendPayloadChunks` to be a `suspend` function and replace `Thread.sleep` with `kotlinx.coroutines.delay()`.

2. **WebSocket Keep-Alive Edge Cases:**
   - **Issue:** The `WebSocketEngine` triggers auto-ping strictly every 30 seconds. On unstable networks, if a ping times out, it directly breaks the loop but doesn't immediately signal the parent coroutine to aggressively tear down the connection.
   - **Fix:** Implement a proper Ping/Pong timeout tracker that forces a connection reset if a Pong is not received within 10 seconds of a Ping.
