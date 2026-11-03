# Tweak Launcher

Launcher Android untuk tweak performa **tanpa root**, memakai Shizuku.

## Fitur versi 1
- **Beranda**: jam, profil performa (Game / Hemat / Normal), statistik RAM, baterai, suhu, penyimpanan, aplikasi favorit.
- **Game Hub**: tombol "Boost dan Main", boost otomatis per game, profil kembali ke semula saat kamu pulang ke launcher.
- **Daftar Aplikasi**: pencarian, filter Semua / Game / Tersemat. Tahan lama sebuah aplikasi untuk menandai game, menyematkan, mengoptimalkan (compile speed), menghentikan paksa, atau membuka info.

## Yang diubah tiap profil (lewat Shizuku)
| Profil | Animasi | Refresh rate | Hemat daya |
|---|---|---|---|
| Game | 0.5x | 120 Hz | mati |
| Hemat | mati | 60 Hz | aktif |
| Normal | 1x | bawaan | mati |

## Cara build (GitHub)
1. Unggah semua isi folder ini ke repo GitHub.
2. Buka tab **Actions**, jalankan **Build APK** (atau push ke `main`).
3. Setelah selesai, unduh artifact **TweakLauncher-apk**, ekstrak, lalu pasang `app-release.apk`.

## Cara pakai
1. Pasang Shizuku (Play Store atau github.com/RikkaApps/Shizuku/releases).
2. Aktifkan Opsi pengembang, lalu Wireless debugging, lalu jalankan Shizuku.
3. Buka Tweak Launcher, tekan **Beri izin** pada kartu Shizuku.
4. Jadikan Tweak Launcher aplikasi Beranda default di Pengaturan.

## Catatan
- Setelah HP restart, Shizuku perlu dijalankan ulang.
- Paksa refresh rate belum tentu didukung semua HP. Jika gagal, aplikasi menampilkan pesan, bukan error.
- Tanpa root tidak bisa mengubah governor CPU. Aplikasi ini sengaja tidak punya "RAM booster".
