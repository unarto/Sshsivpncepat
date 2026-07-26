# HEV SOCKS5 Tunnel + SSH Custom Payload Integration Guide

## Overview

Project ini menggunakan:

1.  hev-socks5-tunnel
2.  SSH tunnel dengan custom payload
3.  Squid proxy / SOCKS5 proxy backend

Arsitektur:

    Android VPNService
            |
            v
    TUN Interface (VPN FD)
            |
            v
    hev-socks5-tunnel
            |
            v
    Local SOCKS5 Proxy
            |
            +----------------+
            |                |
            v                v
       SSH Tunnel       Squid Proxy
     (custom payload)
            |
            v
        Internet

## Peran hev-socks5-tunnel

hev-socks5-tunnel hanya bertugas sebagai:

    TUN traffic ---> SOCKS5

hev bukan: - SSH client - Payload engine - Proxy core - VPN manager

Source submodule `hev-socks5-tunnel/` tidak boleh dirombak.

## Native Library

Output:

    libhev-socks5-tunnel.so
    libssh.so

Pemisahan:

    hev-socks5-tunnel = mengatur traffic TUN
    libssh = membuat koneksi SSH

## Backend Flow

    VPNService start
            |
            v
    Create TUN Interface
            |
            v
    Start SSH Tunnel
            |
            v
    SSH membuat SOCKS5 localhost
            |
            v
    Generate hev yaml
            |
            v
    Start hev-socks5-tunnel
            |
            v
    Traffic VPN berjalan

## SSH Custom Payload

SSH bertugas membuat koneksi menggunakan payload dan menghasilkan SOCKS5
lokal.

Contoh:

    127.0.0.1:1080

Port tersebut digunakan oleh hev.

## Squid Proxy

Squid dapat menjadi backend proxy:

    hev -> SOCKS5 -> Squid -> Internet

## Kotlin Service Responsibility

Tugas service:

1.  Load library.
2.  Membuat config yaml runtime.
3.  Menjalankan:

``` kotlin
Tunnel.main(configPath, vpnFd)
```

4.  Menghentikan:

``` kotlin
Tunnel.quit()
```

## Runtime Config

File:

    files/hev-socks5-tunnel.yaml

Contoh:

``` yaml
tunnel:
  mtu: 1500
  multi-queue: false
  ipv4: 26.26.26.2

socks5:
  address: 127.0.0.1
  port: 1080
  udp: udp

misc:
  log-level: warn
```

## Per App VPN Routing

Per-app routing dilakukan oleh Android VPNService.

hev hanya menerima trafik dari TUN.

## Build Structure

Pisahkan:

    compile-hevtun.sh
            |
            v
    libhev-socks5-tunnel.so

    compile-libssh2.sh
            |
            v
    libssh.so

## Target Architecture

    Android VPNService
            |
            v
    TUN FD
            |
            v
    hev-socks5-tunnel
            |
            v
    SOCKS5 localhost
            |
            +------------+
            |            |
            v            v
     SSH Custom     Squid Proxy
     Payload
            |
            v
     Internet

## Kesimpulan

-   hev-socks5-tunnel tetap original.
-   Tidak mengubah submodule.
-   Fokus perbaikan pada backend Kotlin.
-   SSH/custom payload dan Squid menjadi sumber SOCKS5.
-   hev hanya meneruskan trafik TUN ke SOCKS5.
