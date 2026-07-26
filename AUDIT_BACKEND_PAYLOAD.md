# Audit Logika Backend Custom Payload, Squid, dan WebSocket

Berdasarkan pengecekan file-file backend (HttpPayloadEngine, PayloadFormatter, PayloadInjector, SquidProxyEngine, WebSocketEngine, WebSocketFramer), berikut adalah laporan hasil auditnya:

## 1. PayloadFormatter.kt (Parser & Utils)
**Status:** Sangat Baik & Robust
*   **Logika Parsing:** Menggunakan *single-pass parser* berbasis karakter yang jauh lebih efisien dibandingkan Regular Expression, mencegah bottleneck memori/CPU saat mem-parsing payload panjang.
*   **Dukungan Tag:** Mendukung tag standar HTTP Injector seperti `[host_port]`, `[crlf]`, `[split]`, hingga tag dinamis seperti `[random=a,b]` dan `[rotate]`.
*   **Split Type:** Mendukung multiple split `[split]` (delay 200ms), `[delay_split]` (delay 1000ms), dan `[instant_split]` (0ms).

## 2. HttpPayloadEngine.kt (Payload Engine)
**Status:** Stabil
*   **Fungsi:** Menggabungkan payload string menjadi chunk untuk dikirim. Mampu membaca respon HTTP secara efisien dengan ukuran buffer maksimal 16KB untuk menghindari serangan memori *exhaustion*.
*   **Proxy Auth:** Otomatis menambahkan header `Proxy-Authorization: Basic` jika user & pass Squid proxy disediakan, baik pada payload custom maupun payload default (CONNECT).

## 3. PayloadInjector.kt (Core Proxy Server)
**Status:** Sangat Baik
*   **Arsitektur:** Menggunakan `ServerSocket` lokal yang berjalan secara *asynchronous* dengan Kotlin Coroutines. Terdapat pool job dan socket registry untuk pembersihan otomatis saat dihentikan.
*   **Deteksi Mode Cerdas:** `determineMode` mendeteksi mode tunnel (TCP, SSL, WS, WSS) secara otomatis berdasarkan isi `sni` dan `payload` (mencari header "upgrade" dan "websocket").
*   **Retry Mechanism:** Memiliki mekanisme *retry* dengan *exponential backoff* hingga 3 kali jika koneksi gagal bukan karena SSL error.
*   **TFO:** Dukungan TCP Fast Open diaktifkan jika memungkinkan.

## 4. WebSocketEngine.kt & WebSocketFramer.kt
**Status:** Sangat Sesuai Standar (RFC 6455)
*   **Handshake (Engine):** Otomatis menyuntikkan header WebSocket yang hilang (Upgrade, Connection, Sec-WebSocket-Key). Memvalidasi balasan `101 Switching Protocols` dan melakukan *hashing SHA-1* pada `Sec-WebSocket-Accept` untuk memverifikasi handshake.
*   **Framing (Framer):** Mengimplementasikan WebSocket Binary Frame (Opcode 0x2). Klien ke server selalu di-*mask* menggunakan 4-byte random key sesuai standar. Batas maksimal ukuran payload frame adalah 10MB.
*   **Keep-Alive:** Implementasi *Auto-Ping* (Opcode 0x9) setiap 30 detik untuk menjaga koneksi tetap hidup, serta merespon *Ping* dari server dengan *Pong* (Opcode 0xA).

## 5. SquidProxyEngine.kt
**Status:** Fungsional, Tapi Redundan
*   Meskipun memiliki logika koneksi HTTP CONNECT dasar, file ini terlihat redundan karena `PayloadInjector.kt` mengandalkan `HttpPayloadEngine` untuk memproses koneksi HTTP/Proxy. 

**Kesimpulan Akhir:** Logika backend untuk payload, proxy, dan websocket sudah mencapai taraf *production-ready* yang setara atau melebihi aplikasi tunneling komersial di pasaran. Penggunaan Coroutine sangat aman untuk *concurrency*, dan implementasi WS patuh terhadap standar RFC.
