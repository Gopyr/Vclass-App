# V-Class Gunadarma untuk Android

Vclass-App adalah aplikasi pendamping Android untuk V-Class Gunadarma. Ia membawa info mata kuliah, forum, kuis, nilai, kalender akademik, tugas, pengaturan akun, dan sebagian notifikasi ke antarmuka Jetpack Compose.

## Status saat ini

Ini proyek personal/komunitas, bukan produk resmi kampus. Aplikasinya bergantung pada ketersediaan dan perilaku layanan V-Class di hulu, jadi bisa berubah kapan saja. Akun test dan data mahasiswa asli tidak boleh masuk ke repository.

## Fitur

| Area | Keterangan |
| --- | --- |
| Dashboard | Ringkasan mata kuliah dan deadline |
| Courses | Detail mata kuliah dan navigasi tugas |
| Forums | Membaca thread dan balasan forum |
| Quizzes | Membaca info kuis lewat aturan pencocokan yang bisa dikonfigurasi |
| Grades | Melihat nilai yang tersedia |
| Calendar | Melacak event dan deadline akademik |
| Remote configuration | Aturan kuis/forum dan patch note bisa diupdate lewat JSON metadata |
| Updates | Metadata rilis publik bisa dibaca updater aplikasi |

## Kebutuhan build

| Kebutuhan | Versi |
| --- | --- |
| Android Studio | Hedgehog atau lebih baru |
| JDK | 17 |
| Android compile SDK | 36 |
| Android minimum | API 24 |

Project sudah menyertakan Gradle wrapper, jadi tidak perlu install Gradle terpisah.

## Build APK debug

```bash
./gradlew assembleDebug
```

APK-nya keluar di `app/build/outputs/apk/debug/app-debug.apk`. Install ke device atau emulator test.

## Konfigurasi dan data

Aplikasi membaca sebagian perilaku dari `releases/remote_config.json` dan metadata update dari `releases/update.json`. Kedua file itu publik dan hanya boleh berisi konfigurasi non-sensitif. Review perubahannya dengan teliti karena update remote config bisa mengubah cara konten diklasifikasi tanpa perlu APK baru.

Akses jaringan dan perilaku login ke layanan hulu adalah detail implementasi. Jangan menaruh kredensial, session cookie, atau respons API privat ke source control.

## Checklist rilis

Sebelum rilis: update `versionCode` dan `versionName`, review `CHANGELOG.md`, build dan install APK, verifikasi metadata update, dan pastikan artifact rilis tidak membawa kredensial test atau data privat. Signing rilis harus pakai private key di luar repo; signing debug hanya untuk development.

## Keterbatasan

Aplikasi ini tidak berafiliasi dan tidak didukung Universitas Gunadarma kecuali dinyatakan eksplisit oleh pemilik proyek. API hulu bisa berubah tanpa pemberitahuan, sebagian fitur butuh akun V-Class terautentikasi, dan repo ini tidak menjamin kompatibilitas dengan semua device atau deployment V-Class.

## Lisensi

[MIT License](LICENSE)
