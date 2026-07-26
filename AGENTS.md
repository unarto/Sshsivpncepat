# AGENT RULES & SYSTEM DIRECTIVES

## 1. BUILD & JNI CONFIGURATION RESTRICTIONS
- **DILARANG SENTUH** pengaturan build dan kompilasi JNI untuk komponen berikut:
  - `hev-socks5-tunnel`
  - `libssh2`
  - `mbedtls`
  - `CMakeLists.txt` atau skrip build NDK terkait.
- Jangan menjalankan perintah kompilasi/build (`compile_applet`) di lingkungan AI Studio.

## 2. STRICT CODE INTEGRITY (NO MOCKS / PLACEHOLDERS / FAKE BINARIES)
- **DILARANG KERAS** menggunakan data hardcoded, simulasi, placeholder mock, `// TODO`, fake response, atau file biner palsu.
- Seluruh kode harus berupa logika produksi asli agar bug dan error dapat terdeteksi dengan jelas secara fail-fast.

## 3. SINGLE RESPONSIBILITY PRINCIPLE (RSP / SRP)
- Wajib menerapkan modul tanggung jawab tunggal (Single Responsibility Principle) secara ketat.
- Setiap modul/kelas harus terisolasi, khusus memegang satu fungsi/tanggung jawab tanpa memicu *directory/file bloat*.

## 4. EXECUTION CONTROL
- Selalu menunggu instruksi eksplisit dari user sebelum melakukan modifikasi kode atau langkah eksekusi berikutnya.

## 5. UI & KOTLIN FOCUS ONLY
Kamu hanya bertugas memperbaiki LOGIKA KOTLIN dan UI Android.

BATASAN MUTLAK:
- JANGAN menyentuh kode JNI.
- JANGAN mengubah file C/C++.
- JANGAN mengubah CMakeLists.txt.
- JANGAN mengubah Android NDK configuration.
- JANGAN mengubah libssh2.
- JANGAN mengubah Mbed TLS.
- JANGAN mengubah hev-tun/native library.
- JANGAN mengganti struktur package.
- JANGAN membuat ulang arsitektur aplikasi.

Area yang BOLEH diubah:
✅ Kotlin (.kt)
✅ Jetpack Compose / XML UI
✅ ViewModel
✅ State management
✅ Navigation
✅ Event handling
✅ Validasi input
✅ Logika tampilan
✅ Bug UI dan alur aplikasi

Aturan kerja:
1. Pertahankan semua API native yang sudah ada.
2. Anggap layer JNI/native sebagai komponen stabil dan tidak boleh disentuh.
3. Jika menemukan masalah yang terlihat berasal dari native, jangan memperbaiki native. Laporkan saja.
4. Jangan melakukan refactor besar.
5. Buat perubahan sekecil mungkin sesuai masalah.

Target:
Project yang sudah berhasil build harus tetap berhasil build.
Fokus hanya membuat Kotlin dan UI lebih stabil, rapi, dan bebas bug.

Ingat:
"Native layer adalah pusaka yang dikunci. Jangan dibuka tanpa izin."
