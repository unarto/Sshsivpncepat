# PROGRESS MAP

- [x] Tahap 1: TopBar & Hamburger Menu Icons Alignment
- [x] Tahap 2: Implementasi `SplitTunnelDialog.kt` dengan search filter
- [x] Tahap 3: Pembuatan `KillSwitchDialog.kt` & Pembaruan `TetherDialog.kt` (Hotshare & Root)
- [x] Tahap 4: Refactor `LogDialog.kt` (Device Info Header) & Pembuatan `LogCleanDialog.kt`
- [x] Tahap 5: Modal & Kartu Khusus (PingInterval, TimeLimit, HevSocksConfigCard, ForcingTls)
- [x] Tahap 6: Layouting `MainScreen.kt` (Menghapus duplicate, menyusun list flat layout)
- [x] Tahap 7: Refactor Dummy/Mock di `TetherDialog.kt` (Pembersihan mock, implementasi fail-fast exception + copy clipboard)
- [x] Tahap 8: Audit & Refactor Thread-Safety + Param Validation `TProxyService.kt`
- [x] Tahap 9: Audit menyeluruh terhadap komponen UI & desinkronisasi backend (Dokumentasi lengkap di `AUDIT.md`)

## ANTRIAN TUGAS BERTAHAP (MENUNGGU INSTRUKSI USER)
- [x] Tahap 10: Sinkronisasi penuh parameter `HevSocksConfigCard` ke `MainViewModel` & `VpnSettingsManager` (Selesai: 2026-07-24 22:45)
- [x] Tahap Refactor ViewModel: Pemisahan `MainViewModel` sesuai SRP (`MainHevSocksController`, `MainSshController`, `MainProxyController`, `MainProfileController`, `MainMonitorController`) (Selesai: 2026-07-24 23:28)
- [ ] Tahap 11: Integrasi Service Intent Hotshare Proxy (Bind `0.0.0.0` & WakeLock) di `TetherDialog`
- [ ] Tahap 12: Integrasi Service Intent Root Hotspot Routing (`RootHotspotManager`) di `TetherDialog`
- [ ] Tahap 13: Implementasi Auto-trim buffer log real-time di `LogManager.kt` (`LogCleanDialog`)
- [ ] Tahap 14: Indikator Countdown Batas Waktu di `StatusCard.kt`
- [ ] Tahap Audit & Sentralisasi Arsitektur: Sentralisasi `DefaultValues.kt`, audit MMKV single source of truth (`VpnSettingsManager` & `SettingsRepository`), pembersihan hardcoded number/string, dan pemastian AdGuard DNS default.
