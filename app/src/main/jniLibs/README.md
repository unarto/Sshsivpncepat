# Native Libraries Directory

Tempatkan file library native Anda (`.so`) di dalam folder sesuai dengan arsitektur CPU (ABI) di bawah ini:

- `app/src/main/jniLibs/arm64-v8a/`
- `app/src/main/jniLibs/armeabi-v7a/`
- `app/src/main/jniLibs/x86/`
- `app/src/main/jniLibs/x86_64/`

## Library yang dibutuhkan:

1. **`libhev-socks5-tunnel.so`**
   - Didapatkan dari kompilasi project atau dari rilis resmi GitHub: [heiher/hev-socks5-tunnel-android](https://github.com/heiher/hev-socks5-tunnel-android)
   - Digunakan oleh `com.sivpn.cepat.TProxyService` via JNI.

2. **`libssh.so`**
   - Hasil kompilasi Native SSH yang telah dibungkus JNI (sesuai package name `com.sivpn.cepat.vpn.NativeSshTunnel`), atau OpenSSH native yang dimodifikasi.
   - Digunakan oleh `com.sivpn.cepat.vpn/NativeSshTunnel.kt` via JNI.

**Penting:** Jika `.so` belum tersedia, aplikasi tetap dapat di-build (dikompilasi) namun saat koneksi ditekan, layanan akan gagal memuat fungsi JNI dan tidak dapat meneruskan packet (menghasilkan error `UnsatisfiedLinkError` di logcats).
