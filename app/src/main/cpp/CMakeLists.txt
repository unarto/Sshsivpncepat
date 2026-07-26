cmake_minimum_required(VERSION 3.22.1)

project(SiVpnNativeLibs LANGUAGES C CXX)

# ============================================================================
# Android NDK Configuration
# ============================================================================

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -O3 -Wall -Wextra")
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,--build-id=none -Wl,--hash-style=gnu")


# ============================================================================
# Android Libraries
# ============================================================================

find_library(log-lib log)

if(NOT log-lib)
    message(FATAL_ERROR "Android log library not found!")
endif()


# ============================================================================
# Project Root Detection
# ============================================================================

set(PROJECT_ROOT "${CMAKE_CURRENT_SOURCE_DIR}/../../../..")

message(STATUS "Project Root: ${PROJECT_ROOT}")


# ============================================================================
# Mbed TLS
# ============================================================================

set(MBEDTLS_DIR "${PROJECT_ROOT}/mbedtls")

set(MBEDTLS_INCLUDE_DIR "${MBEDTLS_DIR}/include")


if(
    NOT EXISTS "${MBEDTLS_DIR}/library"
    OR
    (
        NOT EXISTS "${MBEDTLS_INCLUDE_DIR}/mbedtls/config.h"
        AND
        NOT EXISTS "${MBEDTLS_INCLUDE_DIR}/mbedtls/mbedtls_config.h"
        AND
        NOT EXISTS "${MBEDTLS_INCLUDE_DIR}/mbedtls/build_info.h"
    )
)

message(FATAL_ERROR
"Mbed TLS source is incomplete at ${MBEDTLS_DIR}"
)

endif()


set(ENABLE_PROGRAMS OFF CACHE BOOL "Disable Mbed TLS programs" FORCE)

set(ENABLE_TESTING OFF CACHE BOOL "Disable Mbed TLS tests" FORCE)


add_subdirectory(
    "${MBEDTLS_DIR}"
    "${CMAKE_BINARY_DIR}/mbedtls"
    EXCLUDE_FROM_ALL
)


# ============================================================================
# LibSSH2
# ============================================================================

set(LIBSSH2_DIR "${PROJECT_ROOT}/libssh2")

set(LIBSSH2_INCLUDE_DIR "${LIBSSH2_DIR}/include")

set(LIBSSH2_SRC_DIR "${LIBSSH2_DIR}/src")


file(GLOB LIBSSH2_SOURCES
    "${LIBSSH2_SRC_DIR}/*.c"
)


list(FILTER LIBSSH2_SOURCES
    EXCLUDE REGEX
    ".*/(openssl|libgcrypt|wolfssl|wincng|os400qc3|blowfish)\\.c$"
)


add_library(
    libssh2_static
    STATIC
    ${LIBSSH2_SOURCES}
)


target_include_directories(
    libssh2_static

    PUBLIC
        "${LIBSSH2_INCLUDE_DIR}"

    PRIVATE
        "${LIBSSH2_SRC_DIR}"
        "${MBEDTLS_INCLUDE_DIR}"
        "${CMAKE_CURRENT_SOURCE_DIR}"
)


# Configure LibSSH2

target_compile_definitions(
    libssh2_static

    PRIVATE
        HAVE_CONFIG_H
)


target_link_libraries(
    libssh2_static

    PUBLIC
        mbedtls
        mbedx509
        mbedcrypto
)


message(STATUS "LibSSH2 static library configured with MbedTLS")


# ============================================================================
# Native SSH JNI
# ============================================================================

add_library(
    ssh
    SHARED
    native-ssh-jni.c
)


target_include_directories(
    ssh

    PRIVATE
        "${LIBSSH2_INCLUDE_DIR}"
        "${CMAKE_CURRENT_SOURCE_DIR}"
)


target_link_libraries(
    ssh

    PRIVATE
        libssh2_static
        mbedtls
        mbedx509
        mbedcrypto
        ${log-lib}
)


target_compile_definitions(
    ssh

    PRIVATE
        JNILIB=1
        HAVE_CONFIG_H
)


message(STATUS "Native SSH JNI library configured")


# ============================================================================
# Summary
# ============================================================================

message(STATUS "==============================================")

message(STATUS "Android NDK Build Configuration Summary")

message(STATUS "==============================================")

message(STATUS "Android ABI: ${ANDROID_ABI}")

message(STATUS "Project Root: ${PROJECT_ROOT}")

message(STATUS "LibSSH2 Path: ${LIBSSH2_DIR}")

message(STATUS "Mbed TLS Path: ${MBEDTLS_DIR}")

message(STATUS "SSH Compression: DISABLED (zlib not used)")

message(STATUS "Output Targets: ssh")

message(STATUS "==============================================")