# Tweak Launcher

Launcher Android untuk tweak performa **tanpa root**, memakai Shizuku. Tampilannya bisa dikustom penuh.

## Fitur
**Performa**
- Profil Game / Hemat / Normal (animasi, refresh rate, mode hemat daya), bisa diubah, ditambah, dan dihapus.
- Profil khusus per aplikasi dan jadwal profil per jam, berjalan lewat layanan latar.
- Halaman Perawatan: skor kesehatan HP, alat cepat (cache, optimasi, tidurkan aplikasi), aplikasi terakhir dan paling sering dipakai, serta Boost per aplikasi.
- Overlay monitor (Hz, RAM, suhu, baterai) di atas aplikasi lain.
- Optimalkan (compile speed), hentikan paksa, tidurkan aplikasi (dan cari yang jarang dipakai), bersihkan cache, dexopt.
- Atur DPI dan resolusi dengan konfirmasi 15 detik dan kembali otomatis.

**Tampilan**
- Tema siap pakai, mode AMOLED / Gelap / Terang, warna aksen bebas (hex), font, sudut kartu, kepekatan kartu.
- Latar warna polos atau wallpaper sistem dengan redaman.
- Bentuk ikon (squircle, bulat, kotak membulat, kotak, tetesan), ukuran ikon, label, kolom.
- Susunan Beranda: tampil/sembunyi dan urutan (jam, profil, statistik, aplikasi, widget).
- Badge notifikasi di ikon (angka atau titik, warna dan pengecualian per aplikasi), butuh akses notifikasi.
- Mode tampilan PDA (gabungan Palm dan Pocket PC): layar LCD, menu Start, tombol keras, ikon monokrom. Dipilih di Setelan > Tampilan.
- Mode tampilan Windows 95: desktop teal, taskbar, menu Start, jendela abu-abu timbul.
- Mode tampilan Windows XP: bukit dan langit, taskbar biru, tombol start hijau, menu Start dua kolom.
- Mode tampilan DOS: terminal sungguhan (DIR, jalankan program dengan nama atau nomor, MEM, MODE, SH lewat Shizuku), warna hijau, abu-abu, atau amber.
- Widget Android asli (jam, cuaca, dll), folder, dock, sembunyikan aplikasi.
- Daftar aplikasi gaya daftar (dengan indeks huruf) atau kotak, filter, pencarian.
- Gestur: geser atas/bawah, ketuk dua kali, tahan lama; aksinya bisa dipilih.

**Lainnya**
- Ekspor dan impor semua pengaturan (JSON).
- Pembaruan langsung dari aplikasi (GitHub Releases).

## Cara build dan rilis
1. Repo harus **publik** supaya fitur pembaruan di aplikasi bisa membaca rilis.
2. Setiap push ke `main`, GitHub Actions membuat APK dan **GitHub Release** (`v1.0.<nomor run>`).
3. Pasang APK dari tab Releases untuk pertama kali. Setelah itu, pembaruan cukup lewat **Setelan > Pembaruan**.

> APK memakai keystore tetap `app/release.keystore` supaya versi baru bisa menimpa versi lama. Keystore ini ada di repo, jadi hanya cocok untuk pemakaian pribadi. Jika repo dipakai luas, pindahkan ke GitHub Secrets.

## Cara pakai
1. Pasang Shizuku (Play Store atau github.com/RikkaApps/Shizuku/releases), aktifkan Wireless debugging, jalankan Shizuku.
2. Buka Tweak Launcher, tekan **Beri izin** pada kartu Shizuku.
3. Jadikan Tweak Launcher aplikasi Beranda default.
4. Fitur otomasi butuh izin tambahan yang diminta saat diaktifkan: Akses penggunaan (profil per aplikasi) dan Tampil di atas aplikasi lain (overlay).

## Catatan
- Setelah HP restart, Shizuku perlu dijalankan ulang.
- Paksa refresh rate belum tentu didukung semua HP; jika gagal, aplikasi menampilkan pesan.
- Tanpa root tidak bisa mengubah governor CPU. Tidak ada "RAM booster" karena justru memperlambat.
