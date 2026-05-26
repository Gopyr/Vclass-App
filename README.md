# V-Class Gunadarma

Android native app untuk membantu akses V-Class Gunadarma dari HP.

## Fitur

- Login native dengan token Moodle.
- Simpan akun dan switch akun dari halaman Profil.
- Dashboard tugas, course, quiz, forum, kalender, profil, dan pengaturan.
- Quiz info, grade final, review attempt, dan status quiz tertutup/soal belum tersedia.
- Forum read-only untuk forum yang melewati batas posting.
- Settings berisi cek update dan patch info.

## Update App

Rencana update memakai GitHub Releases:

- APK disimpan sebagai release asset.
- Metadata update disimpan di `releases/update.json`.
- App membaca `update.json`, membandingkan `versionCode`, lalu menampilkan patch notes.

## Remote Config

Fix kecil untuk rule parser quiz/forum dapat dikirim lewat:

```text
releases/remote_config.json
```

Contoh yang bisa diubah tanpa upload APK:

- pola range nilai quiz
- teks status quiz tertutup
- teks status quiz tanpa soal
- teks forum read-only/cutoff
- patch notes ringan yang tampil di Settings

## Release Metadata

Contoh struktur:

```json
{
  "versionCode": 1,
  "versionName": "1.0.0",
  "apkUrl": "https://github.com/Gopyr/Vclass-App/releases/download/v1.0.0/app-debug.apk",
  "mandatory": false,
  "changelog": [
    "Login native dan simpan akun",
    "Grade quiz lebih akurat",
    "Forum read-only"
  ]
}
```

## Build

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug
```

APK debug akan ada di:

```text
app/build/outputs/apk/debug/app-debug.apk
```
