#!/bin/bash
set -o errexit
set -o pipefail
set -o nounset

__dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [[ -z "${NDK_HOME:-}" || ! -d "$NDK_HOME" ]]; then
  echo "Android NDK: NDK_HOME not found. please set env \$NDK_HOME"
  exit 1
fi

if [[ ! -f "$NDK_HOME/build/cmake/android.toolchain.cmake" ]]; then
  echo "Android NDK: android.toolchain.cmake not found under \$NDK_HOME/build/cmake"
  exit 1
fi

if ! command -v cmake >/dev/null 2>&1; then
  echo "cmake not found. Install CMake or add it to PATH."
  exit 1
fi


ensure_submodule() {
  local path="$1"
  local expected="$2"
  local description="$3"

  if [[ ! -e "$expected" ]]; then
    if [[ -f "$__dir/.gitmodules" ]] && command -v git >/dev/null 2>&1; then
      echo "$description source is missing. Initializing submodule '$path'..."
      git -C "$__dir" submodule update --init --recursive "$path"
    fi
  fi
}


ensure_submodule "libssh2" "$__dir/libssh2/include/libssh2.h" "libssh2"
ensure_submodule "mbedtls" "$__dir/mbedtls/CMakeLists.txt" "Mbed TLS"


if [[ ! -f "$__dir/libssh2/include/libssh2.h" || ! -d "$__dir/libssh2/src" ]]; then
  echo "libssh2 source is incomplete."
  exit 1
fi


if [[ ! -f "$__dir/mbedtls/CMakeLists.txt" || ! -d "$__dir/mbedtls/library" ]]; then
  echo "Mbed TLS source is incomplete."
  exit 1
fi


# ==========================================================
# PATCH LIBSSH2 FOR ANDROID NDK
# ==========================================================

patch_libssh2_android() {

  local file="$__dir/libssh2/src/misc.h"

  if [[ ! -f "$file" ]]; then
    echo "libssh2 misc.h not found, skip patch"
    return
  fi


  if grep -q "ssh2_explicit_zero.*explicit_bzero" "$file"; then

    echo "Applying libssh2 Android explicit_bzero patch..."

    python3 - "$file" <<'PY'
from pathlib import Path
import sys

path = Path(sys.argv[1])

text = path.read_text()

old = "#define ssh2_explicit_zero(buf, size) explicit_bzero(buf, size)"

new = r'''
#if defined(__ANDROID__)
static inline void explicit_bzero(void *buf, size_t len)
{
    volatile unsigned char *p = (volatile unsigned char *)buf;

    while (len--) {
        *p++ = 0;
    }
}
#endif

#define ssh2_explicit_zero(buf, size) explicit_bzero(buf, size)
'''

if old in text:
    text = text.replace(old, new)
    path.write_text(text)
    print("libssh2 patch applied successfully")
else:
    print("libssh2 already patched")
PY

  fi
}


patch_libssh2_android


abis=(armeabi-v7a arm64-v8a x86 x86_64)

build_root="${BUILD_DIR:-$__dir/build/libssh2-android}"

out_root="${OUT_DIR:-$__dir/app/src/main/jniLibs}"


clear_tmp() {

  if [[ "${KEEP_BUILD_DIR:-0}" != "1" ]]; then
    rm -rf "$build_root"
  fi

}


trap 'echo -e "Aborted, error $? in command: $BASH_COMMAND"; trap ERR; clear_tmp; exit 1' ERR INT


mkdir -p "$build_root" "$out_root"


for abi in "${abis[@]}"; do

  build_dir="$build_root/$abi"

  install_dir="$out_root/$abi"


  mkdir -p "$build_dir" "$install_dir"


  echo "========================================"
  echo "Building ABI: $abi"
  echo "========================================"


  cmake -S "$__dir/app/src/main/cpp" \
    -B "$build_dir" \
    -DCMAKE_TOOLCHAIN_FILE="$NDK_HOME/build/cmake/android.toolchain.cmake" \
    -DANDROID_ABI="$abi" \
    -DANDROID_PLATFORM=android-24 \
    -DANDROID_STL=c++_static \
    -DCMAKE_BUILD_TYPE=Release


  cmake --build "$build_dir" \
    --target ssh \
    --config Release \
    --parallel "${JOBS:-$(nproc 2>/dev/null || echo 4)}"


  cp "$build_dir/libssh.so" \
     "$install_dir/libssh.so"


  echo "Built $install_dir/libssh.so"

done


clear_tmp