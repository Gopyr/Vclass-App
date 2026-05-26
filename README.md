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

