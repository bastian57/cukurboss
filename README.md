# CukurBoss

Aplikasi kasir (POS) barbershop **100% offline** — by DAFTECH.

Fitur: login no HP + password (+ PIN darurat), profil usaha + QR rekening,
daftar layanan/produk, kasir (tunai / transfer QR), cetak struk thermal
Bluetooth ESC/POS 58mm, data pelanggan + broadcast promo via WhatsApp,
laporan omzet harian, backup database lokal, aktivasi lisensi device-bound
(kode `CB1-...`).

## Build release

Butuh 4 GitHub Secrets (sama pola seperti AIR KITA):

- `KEYSTORE_BASE64` — isi keystore release (base64)
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

Workflow `.github/workflows/build-apk.yml` otomatis build `assembleRelease`
dan publish rolling release tag `latest` ("CukurBoss — latest").

Build lokal (debug):

```
gradle :app:assembleDebug
```

## Catatan keamanan

- Tidak ada izin INTERNET — aplikasi murni offline.
- Password & PIN disimpan sebagai hash SHA-256 + salt, bukan plaintext.
- Tidak ada secret/private key di repo. Hanya public key RSA verifikasi
  lisensi di `license/LicenseManager.kt`.
- QR transfer berisi TEKS no rekening (bukan QRIS): pelanggan salin manual
  ke m-banking, kasir konfirmasi manual. QRIS adalah upgrade path.
