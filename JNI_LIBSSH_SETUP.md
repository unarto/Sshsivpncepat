# App Readiness Audit

Status: **belum siap untuk rilis/production APK**.

## Blocking issues

1. **Native libraries belum tersedia di APK.** `app/src/main/jniLibs` hanya berisi README, sementara service menolak start kalau `libssh.so` atau `libhev-socks5-tunnel.so` tidak berhasil dimuat.
2. **`libssh2` source di working tree belum lengkap.** Folder `libssh2` saat ini tidak berisi `include/libssh2.h` dan `src/`, sehingga `compile-libssh2.sh` akan berhenti sebelum build.
3. **Mbed TLS belum dipasang.** Build libssh2 Android sekarang sengaja memerlukan folder `mbedtls/` agar tidak link ke `libcrypto` sistem Android yang tidak aman/portable untuk APK.
4. **Gradle wrapper bisa jalan, tapi environment ini tidak bisa download distribusi Gradle karena proxy 403.** Validasi build APK tetap perlu dijalankan di mesin/CI yang bisa akses `services.gradle.org`, Google Maven, Maven Central, dan Gradle Plugin Portal.
5. **Release signing belum siap tanpa secret.** Build release membutuhkan `KEYSTORE_PATH`, `STORE_PASSWORD`, dan `KEY_PASSWORD` atau file `my-upload-key.jks` lokal.

## Fixed during this audit

1. Native SSH shutdown dibuat lebih aman: `stopSshTunnel()` tidak lagi menutup `global_sock` langsung untuk menghindari double-close FD; fungsi itu hanya melakukan `shutdown()` agar thread utama yang melakukan cleanup final.
2. Cleanup session menunggu client handler selesai sebentar sebelum `libssh2_session_free()`, mengurangi risiko use-after-free saat tunnel dimatikan.
3. Pengiriman data dari channel SSH ke socket lokal sekarang memakai loop `send_full()` supaya data tidak hilang ketika `send()` menulis sebagian.
4. Alokasi argumen thread client sekarang dicek agar tidak crash ketika `malloc()` gagal.
5. `compile-libssh2.sh` sekarang memvalidasi keberadaan source Mbed TLS sebelum build.

## Recommended next steps

1. Pasang submodule `libssh2` dan `mbedtls` secara lengkap, lalu commit `.gitmodules`.
2. Update CMake/build native agar memakai crypto backend Mbed TLS dan menghasilkan `libssh.so` untuk semua ABI.
3. Build `libhev-socks5-tunnel.so` dan `libssh.so`, lalu pastikan keduanya masuk ke `app/src/main/jniLibs/<abi>/` atau diproduksi sebagai artifact CI yang dipakai sebelum build APK.
4. Jalankan `./gradlew test` dan `./gradlew assembleDebug` di environment dengan akses repository Gradle/Maven.
5. Uji manual flow VPN di Android 14+ karena foreground service type `specialUse` dan permission notification/runtime bisa berbeda per device.
