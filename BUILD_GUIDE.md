# Rekomendasi Mbed TLS untuk SiVPN Android

## Pilihan yang disarankan

Gunakan **Mbed TLS 3.6 LTS**, pin ke tag **`mbedtls-3.6.7`**.

Alasan:

- Cabang 3.6 adalah LTS dan masih menerima bug fix serta security fix sampai setidaknya Maret 2027.
- `libssh2` di repo ini hanya mensyaratkan backend mbedTLS versi **3.1.0 atau lebih baru**, jadi seri 3.6.x kompatibel dengan kebutuhan minimum `libssh2` tanpa lompat ke API mayor baru.
- Hindari Mbed TLS 4.x dulu untuk build Android aplikasi ini karena 4.x membawa perubahan API/migrasi besar. Untuk tunnel SSH JNI, stabilitas build lebih penting daripada fitur TLS 4.x terbaru.
- Ambil archive resmi `mbedtls-3.6.7.tar.bz2` atau clone tag `mbedtls-3.6.7`; jangan pakai auto-generated `source.tar.gz`/`source.zip` GitHub karena release Mbed TLS mencatat snapshot itu bisa tidak membawa dependency/generated files yang dibutuhkan untuk konfigurasi build.

## Cara pasang sebagai submodule

```bash
git submodule add https://github.com/Mbed-TLS/mbedtls.git mbedtls
git -C mbedtls checkout mbedtls-3.6.7
git add .gitmodules mbedtls
git commit -m "Add mbedtls 3.6.7 submodule"
```

## Catatan integrasi dengan libssh2

`libssh2/CMakeLists.txt` sudah punya pilihan backend crypto `mbedTLS` dan akan link ke target `libssh2::mbedcrypto` saat `find_package(MbedTLS)` berhasil. Jadi arah build Android berikutnya sebaiknya:

1. Build/install Mbed TLS 3.6.7 untuk tiap ABI Android.
2. Configure libssh2 dengan `-DCRYPTO_BACKEND=mbedTLS`.
3. Arahkan `CMAKE_PREFIX_PATH` atau `MbedTLS_DIR` ke hasil install Mbed TLS per ABI.
4. Link JNI `libssh.so` ke static `libmbedcrypto` dari Mbed TLS, bukan mengandalkan `crypto` sistem Android.

## Kesimpulan

Untuk aplikasi ini, pilihan paling aman sekarang: **Mbed TLS 3.6.7 LTS** sebagai crypto backend `libssh2`.
