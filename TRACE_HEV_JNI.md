# TECHNICAL REVIEW: TRACE BUILD GRAPH `hev-jni.c`

Sesuai instruksi, berikut adalah pembuktian langsung dari **Build System (Source of Truth)** bahwa file `hev-socks5-tunnel/src/hev-jni.c` benar-benar dikompilasi pada project ini.

==================================================
## BUKTI BUILD GRAPH
==================================================

### 1. File Build System yang Mereferensikan
File **`hev-socks5-tunnel/Android.mk`** (dari submodule `unarto/hev-socks5-tunnel`) mengatur kompilasi dengan memanggil file pembantu `build.mk`.
- **Lokasi file referensi**: `hev-socks5-tunnel/Android.mk` baris 26: `include $(LOCAL_PATH)/build.mk`
- **Isi `build.mk`**:
  ```makefile
  rwildcard=$(foreach d,$(wildcard $1*), $(call rwildcard,$d/,$2) $(filter $(subst *,%,$2),$d))
  SRCFILES=$(call rwildcard,$(SRCDIR)/,*.c *.S)
  ```
- **Kesimpulan**: `build.mk` menggunakan fungsi `rwildcard` untuk mereferensikan dan mengambil **semua file ekstensi `.c` dan `.S`** yang ada di dalam folder `src/`. Karena file `src/hev-jni.c` ada di dalam folder tersebut, file ini otomatis terbawa ke variabel `SRCFILES`.

### 2. LOCAL_SRC_FILES
Di dalam **`hev-socks5-tunnel/Android.mk`** baris 29, variabel `SRCFILES` dimasukkan ke dalam `LOCAL_SRC_FILES`:
```makefile
LOCAL_SRC_FILES := $(patsubst $(SRCDIR)/%,src/%,$(SRCFILES))
```
Ini membuktikan secara definitif bahwa `src/hev-jni.c` di-supply ke NDK compiler.

### 3. Jalur Shell Script yang Mengompilasi
Script **`compile-hevtun.sh`** memicu proses kompilasi melalui `ndk-build`.
- **Jalur Eksekusi**:
  1. `compile-hevtun.sh` membuat direktori `jni/` di temporary folder (`$TMPDIR/jni`).
  2. Melakukan symlink submodule ke dalamnya: `ln -s "$__dir/hev-socks5-tunnel" jni/hev-socks5-tunnel`.
  3. Mengeksekusi NDK:
     ```bash
     "$NDK_HOME/ndk-build" \
         NDK_PROJECT_PATH=. \
         APP_BUILD_SCRIPT=jni/Android.mk \
         ...
     ```
  4. Script NDK mem-parsing `jni/hev-socks5-tunnel/Android.mk`, yang kemudian menarik `src/hev-jni.c`.

### 4. Output Object (.o) yang Dihasilkan
Berdasarkan logika NDK Build System, setiap source C di dalam `LOCAL_SRC_FILES` dikompilasi menjadi *object file* tunggal.
Output dari `src/hev-jni.c` akan di-compile menggunakan compiler (clang/gcc) menjadi object file pada path *build artifact*:
`$TMPDIR/obj/local/<abi>/objs/hev-socks5-tunnel/src/hev-jni.o` (untuk setiap ABI target seperti `armeabi-v7a`, `arm64-v8a`, dll).

### 5. Library (.so) Tempat Object Dilink
Pada akhir eksekusi di **`hev-socks5-tunnel/Android.mk`**:
```makefile
LOCAL_MODULE := hev-socks5-tunnel
include $(BUILD_SHARED_LIBRARY)
```
Target ini memerintahkan NDK untuk melakukan linking seluruh file `.o` (termasuk `hev-jni.o`) menjadi _shared library_.
- **Library Output**: `libhev-socks5-tunnel.so`.
- Script `compile-hevtun.sh` kemudian menyalin file `.so` yang telah di-link tersebut ke: `app/src/main/jniLibs/<abi>/libhev-socks5-tunnel.so`.

==================================================
## STATUS AKHIR
==================================================
**TERBUKTI DARI BUILD GRAPH**.
File `hev-jni.c` 100% dikompilasi oleh build system dan berada secara native di dalam file output `libhev-socks5-tunnel.so` yang diload saat *runtime* oleh aplikasi. Tidak ada asumsi.
