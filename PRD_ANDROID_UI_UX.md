# PRD — UI/UX Design: Kasir UMKM Mobile App

> **Platform:** Android Native (Java) | **Backend:** Laravel REST API
> **Versi:** 1.0 | **Tanggal:** 31 Mei 2026 | **Status:** Draft

---

## Daftar Isi

1. [Overview](#1-overview)
2. [Design System](#2-design-system)
3. [Spesifikasi Layar](#3-spesifikasi-layar)
4. [Animasi & Transisi](#4-animasi--transisi)
5. [Alur Navigasi](#5-alur-navigasi)
6. [Panduan UX](#6-panduan-ux)
7. [Changelog](#7-changelog)

---

## 1. Overview

### Tujuan Dokumen

Dokumen ini mendefinisikan spesifikasi desain UI/UX lengkap untuk aplikasi mobile Kasir UMKM yang dikembangkan dengan Android Studio (Java). Dokumen mencakup sistem desain, palet warna, tipografi, komponen, spesifikasi per layar, serta panduan animasi dan transisi.

### 1.1 Deskripsi Produk

Kasir UMKM adalah aplikasi point-of-sale (POS) berbasis Android yang memungkinkan pemilik usaha kecil dan menengah melakukan transaksi penjualan, mengelola produk dan pelanggan, serta melihat laporan bisnis secara real-time.

### 1.2 Target Pengguna

| Role | Deskripsi | Akses |
|------|-----------|-------|
| Admin / Kasir | Pemilik usaha atau operator kasir utama | Full access |
| Staff | Karyawan kasir tambahan | Transaksi & lihat laporan |

### 1.3 Prinsip Desain

- **Clean & Fresh** — Background terang, whitespace luas, tidak crowded
- **Mobile-first** — Dioptimalkan untuk layar 5–6.7 inci
- **Speed-first** — Kasir harus bisa menyelesaikan transaksi dalam < 30 detik
- **Consistent** — Komponen dan pola interaksi seragam di seluruh aplikasi
- **Accessible** — Kontras warna minimum WCAG AA (4.5:1)

---

## 2. Design System

### 2.1 Palet Warna

Tema utama aplikasi menggunakan warna **Teal** sebagai accent primary yang memberikan kesan segar, profesional, dan terpercaya.

| Nama | HEX | Penggunaan |
|------|-----|------------|
| Primary — Teal | `#0EA5A0` | Tombol utama, navbar aktif, accent card, badge |
| Primary Light | `#E0F7F6` | Background chip customer, selected state produk |
| Primary Pale | `#F0FDFA` | Background info box, icon container |
| Text Dark | `#1E293B` | Heading, nilai nominal, teks utama |
| Text Gray | `#64748B` | Subtext, label, placeholder |
| Background Page | `#F8FAFB` | Background halaman utama |
| Background Card | `#FFFFFF` | Surface card, bottom sheet |
| Border | `#E2E8F0` | Garis pemisah, border card |
| Accent Orange | `#F97316` | Jumlah transaksi, highlight sekunder |
| Danger Red | `#EF4444` | Tombol void, error state, hapus |

### 2.2 Tipografi

Font keluarga: **Roboto** (built-in Android). Skala tipografi mengikuti Material Design 3.

| Style | Size | Weight | Penggunaan |
|-------|------|--------|------------|
| Display Large | 28sp | Bold 700 | Nilai nominal besar (total bayar) |
| Headline Medium | 20sp | SemiBold 600 | Judul halaman, nama produk besar |
| Title Medium | 16sp | SemiBold 600 | Header section, nama produk list |
| Body Large | 14sp | Regular 400 | Deskripsi, konten utama |
| Body Medium | 12sp | Regular 400 | Label, subtext, metadata |
| Label Small | 10sp | Medium 500 | Badge, chip, tag golongan |
| Caption | 11sp | Regular 400 | Timestamp, keterangan kecil |

### 2.3 Spacing & Grid

| Token | Nilai |
|-------|-------|
| Base unit | 4dp |
| Margin halaman kiri/kanan | 16dp |
| Jarak antar card | 12dp |
| Padding dalam card | 16dp |
| Corner radius card | 12dp |
| Corner radius button | 10dp |
| Corner radius chip/badge | 20dp (pill) |

### 2.4 Komponen Utama

#### 2.4.1 Bottom Navigation Bar

| Elemen | Spesifikasi |
|--------|-------------|
| Background | Putih `#FFFFFF`, border-top 1dp `#E2E8F0` |
| Tinggi | 64dp |
| Item | 4 item: Home, Kasir, Laporan, Pengaturan |
| Ikon aktif | Pill teal (36dp) dengan ikon putih |
| Ikon non-aktif | Ikon abu `#CBD5E1`, tanpa label |
| Animasi tap | Scale 0.9 → 1.0, durasi 150ms, `OvershootInterpolator` |

#### 2.4.2 Hero Metric Card

| Elemen | Spesifikasi |
|--------|-------------|
| Background | Solid Teal `#0EA5A0` |
| Border radius | 16dp |
| Padding | 16dp |
| Label | 11sp, Regular, putih 75% opacity |
| Nilai | 24sp, Bold, putih |
| Sub-label | 10sp, Regular, putih 65% opacity |
| Badge trend | Pill putih 20% opacity, ikon trend + teks 10sp |

#### 2.4.3 Mini Metric Card

| Elemen | Spesifikasi |
|--------|-------------|
| Background | Putih `#FFFFFF` |
| Border | 1dp solid `#F1F5F9` |
| Border radius | 10dp |
| Padding | 8dp 10dp |
| Label | 10sp, Regular, `#94A3B8` |
| Nilai | 13sp, SemiBold, `#1E293B` (teal untuk margin positif) |

#### 2.4.4 Product Card (POS Grid)

| Elemen | Spesifikasi |
|--------|-------------|
| Background default | Putih `#FFFFFF`, border 1dp `#F1F5F9` |
| Background selected | Mint `#F0FDFA`, border 1dp `#0EA5A0` |
| Border radius | 10dp |
| Padding | 8dp |
| Emoji produk | 20sp (sebagai visual produk) |
| Nama produk | 11sp, Medium, `#1E293B` |
| Harga jual | 12sp, Bold, `#0EA5A0` |
| Badge qty | Pill teal 16dp, angka putih 9sp, posisi top-right |

#### 2.4.5 Customer Chip

| Elemen | Spesifikasi |
|--------|-------------|
| Background | Mint `#F0FDFA` |
| Border | 1dp solid `#99F6E4` |
| Border radius | 10dp |
| Padding | 7dp 9dp |
| Avatar | 22dp circle, background teal, inisial putih 9sp bold |
| Nama | 12sp, SemiBold, `#134E4A` |
| Badge golongan | Pill `#CCFBF1`, teks `#0F766E`, 10sp Medium |

#### 2.4.6 Tombol Bayar (CTA Primary)

| Elemen | Spesifikasi |
|--------|-------------|
| Background | Solid Teal `#0EA5A0` |
| Border radius | 10dp |
| Padding | 12dp 16dp |
| Tinggi minimum | 48dp |
| Teks | 13sp, Bold, Putih, letter-spacing 0.5sp |
| State pressed | Background `#0F766E` (teal gelap), scale 0.97 |
| State disabled | Background `#E2E8F0`, teks `#94A3B8` |

---

## 3. Spesifikasi Layar

### 3.1 Layar Login

Layar pertama yang dilihat pengguna saat membuka aplikasi. Harus memberikan kesan profesional dan aman.

| Elemen | Spesifikasi |
|--------|-------------|
| Layout | Full screen, background `#F8FAFB` |
| Logo app | Center, ukuran 80dp × 80dp, border radius 20dp |
| Tagline | 16sp, Regular, `#64748B`, center |
| Input Email | Full width, height 52dp, border radius 10dp, ikon envelope kiri |
| Input Password | Full width, height 52dp, ikon eye toggle kanan |
| Tombol Login | Full width, height 52dp, CTA Primary style |
| Error state | Border input merah `#EF4444`, pesan error 12sp merah di bawah field |
| Device lock info | Teks kecil 11sp abu di bawah tombol: *"Login akan logout perangkat lain"* |

### 3.2 Layar Dashboard (Home)

Halaman utama yang ditampilkan setelah login. Memberikan ringkasan bisnis hari ini dan akses cepat ke fitur utama.

| Elemen | Spesifikasi |
|--------|-------------|
| Background | `#F8FAFB` |
| Status bar | Light mode (ikon gelap) |
| Header row | Greeting + nama pengguna (kiri), avatar inisial (kanan) |
| Hero Card | Omzet hari ini, lebar penuh, teal solid, margin kanan-kiri 16dp |
| Mini cards row | 3 kolom equal: Total Modal, Margin %, Jumlah Transaksi |
| Quick action grid | 2×2 grid: Kasir, Produk, Customer, Laporan |
| Recent transactions | List 5 item terbaru, ikon + nama + metode + jumlah hijau |
| Scroll behavior | `NestedScrollView`, hero card tidak sticky |

### 3.3 Layar Kasir / POS

Layar paling kritis — tempat proses transaksi berlangsung. Dirancang untuk kecepatan dan kenyamanan kasir.

| Elemen | Spesifikasi |
|--------|-------------|
| Layout | Full screen, background `#F8FAFB` |
| Header | Judul "Transaksi Baru" + tombol close (X merah) |
| Customer chip | Selalu tampil di atas, tap untuk ganti customer (`BottomSheetDialog`) |
| Search bar | Sticky di bawah customer chip, real-time filter produk |
| Product grid | `RecyclerView` 2 kolom, `GridLayoutManager`, snap scroll |
| Produk terpilih | Border teal + background mint + badge qty teal di pojok kanan atas |
| Bottom cart sheet | `BottomSheet` non-collapsible, tampil saat ada item, tinggi dinamis max 40% layar |
| Cart content | Daftar item + qty + subtotal tiap baris |
| Divider | Garis 1dp `#F1F5F9` |
| Total row | Label "Total" abu + nilai besar bold `#1E293B` |
| Tombol Bayar | Full width CTA Primary, di bawah total |

### 3.4 Layar Checkout / Pembayaran

Tampil setelah tap tombol Bayar. Fokus pada konfirmasi nominal dan metode pembayaran.

| Elemen | Spesifikasi |
|--------|-------------|
| Layout | `BottomSheetDialogFragment` full-height atau Activity baru |
| Total harus dibayar | Display Large 28sp, center, warna teal |
| Metode pembayaran | 3 chip horizontal: CASH, QRIS, Transfer — pilih satu, yang aktif border teal |
| Input nominal bayar | Large input field, keyboard numpad, prefix "Rp" |
| Kalkulator kembalian | Real-time: Kembalian = nominal bayar − total, warna hijau/merah |
| Tombol Proses Bayar | Full width, aktif jika nominal >= total |
| State QRIS | Tampilkan placeholder QR code area |
| State Transfer | Tampilkan info rekening tujuan dari settings |

### 3.5 Layar Sukses Transaksi & Struk

Konfirmasi visual setelah transaksi berhasil, dengan opsi cetak struk thermal.

| Elemen | Spesifikasi |
|--------|-------------|
| Animasi masuk | Checkmark circle scale-in + lottie confetti ringan |
| Teks sukses | "Transaksi Berhasil!" 20sp Bold teal, center |
| Ringkasan | Nama customer, total, metode, kembalian |
| Tombol Cetak Struk | Outline style (border teal, teks teal), icon printer kiri |
| Tombol Transaksi Baru | CTA Primary, langsung kembali ke POS dengan cart bersih |
| Preview struk | Card putih berisi format struk thermal (lihat 3.5.1) |

#### 3.5.1 Format Struk Thermal

```
================================
        [NAMA TOKO]
     [Alamat toko]
     Telp: [No. Telp]
================================
No    : INV-20260531-001
Tgl   : 31/05/2026  10:23
Kasir : Budi Santoso
Cust  : Sari Dewi (USER)
--------------------------------
Aqua 600ml
  2 x Rp 5.000        Rp 10.000
Mie Instan
  1 x Rp 3.500         Rp 3.500
--------------------------------
Subtotal             Rp 13.500
Diskon                   Rp 0
--------------------------------
TOTAL                Rp 13.500
Dibayar              Rp 20.000
Kembalian             Rp 6.500
================================
   Terima kasih atas kunjungan
        Anda! Sampai jumpa.
================================
```

| Elemen | Spesifikasi |
|--------|-------------|
| Header | Nama toko, alamat, no. telp — dari app settings |
| Divider | Garis `================================` |
| Info transaksi | No. invoice, tanggal, waktu, kasir, customer, golongan |
| Item list | Nama produk / qty × harga = subtotal (per baris) |
| Subtotal | Right-aligned |
| Diskon | Tampil jika ada |
| Grand Total | Bold, ukuran lebih besar |
| Footer | Ucapan terima kasih dari settings |

### 3.6 Layar Manajemen Produk

| Elemen | Spesifikasi |
|--------|-------------|
| List produk | `RecyclerView` 1 kolom, card putih per item |
| Item card | Emoji/icon kiri + nama + harga user (kanan bold teal) |
| Sub-info | Kode produk + unit + status (aktif/nonaktif badge) |
| FAB tambah | Teal, ikon +, posisi bottom-right 16dp dari tepi |
| Swipe action | Swipe kiri: hapus (merah) \| swipe kanan: edit (teal) |
| Form tambah/edit | `BottomSheet` atau full Activity — field: kode, nama, unit, harga modal, harga 3 golongan, status |
| Search & filter | Search bar atas + filter chip: Semua / Aktif / Nonaktif |

### 3.7 Layar Manajemen Customer

| Elemen | Spesifikasi |
|--------|-------------|
| List customer | `RecyclerView` 1 kolom, avatar inisial + nama + golongan badge |
| Badge golongan | USER: teal \| FREELANCER: orange \| GROSIR: ungu |
| Search | Search bar real-time by nama / kode / no.hp |
| Form customer | Nama, no.hp, alamat, golongan (radio button 3 pilihan) |
| Detail customer | Riwayat transaksi customer di bagian bawah form detail |

### 3.8 Layar Laporan

Halaman yang memberikan insight bisnis lengkap. Dirancang agar mudah dibaca sekilas.

| Elemen | Spesifikasi |
|--------|-------------|
| Period tab | Pill tab: Harian / Mingguan / Bulanan — sticky di atas |
| Date picker | Ikon kalender kanan atas untuk filter tanggal custom |
| Stat grid 2×2 | Total Penjualan (teal), Total Modal, Laba Bersih (orange), Margin % |
| Bar chart | 7 bar untuk weekly, 30 bar untuk monthly — bar aktif teal solid, lainnya mint |
| Payment breakdown | 3 chip horizontal: CASH / QRIS / Transfer + nominal masing-masing |
| Top produk | Ranked list 1–5, nama + progress bar teal + nominal |
| Top customer | Ranked list 1–5 berdasarkan total pembelian |
| Export | Tombol share di pojok kanan atas (share sebagai teks/PDF) |

### 3.9 Layar Pengaturan

| Elemen | Spesifikasi |
|--------|-------------|
| Profil toko | Nama toko, alamat, no. telp, ucapan struk — editable |
| Info akun | Email login, nama, device terdaftar (read only) |
| Printer thermal | Status koneksi Bluetooth, scan + pair printer |
| Logout | Tombol merah outline di bagian bawah |
| Versi app | Ditampilkan di footer settings, teks abu kecil |

---

## 4. Animasi & Transisi

### 4.1 Transisi Antar Halaman

| Aksi | Tipe Animasi | Durasi & Interpolator |
|------|--------------|-----------------------|
| Dashboard → POS | Slide up dari bawah | 300ms, `FastOutSlowInInterpolator` |
| POS → Checkout | Shared element (total card) | 350ms, `AccelerateDecelerateInterpolator` |
| Checkout → Sukses | Fade + scale-in | 400ms, `OvershootInterpolator` |
| Sukses → POS baru | Slide kanan ke kiri | 280ms, `FastOutSlowInInterpolator` |
| Bottom nav switch | Crossfade fragment | 200ms, `LinearInterpolator` |
| BottomSheet muncul | Slide up dengan spring | 350ms, `SpringForce` stiffness LOW |

### 4.2 Animasi Komponen

- **Product card tap** — Ripple effect bawaan Android + scale 1.0 → 0.94 → 1.0, 150ms
- **Badge qty muncul** — Scale 0 → 1.2 → 1.0 dengan `OvershootInterpolator`, 200ms
- **Total harga update** — `ValueAnimator` dari nilai lama ke baru, 250ms
- **Cart item masuk** — Slide-in dari kanan + fade, 180ms
- **Checkmark sukses** — `AnimatedVectorDrawable` stroke draw, 500ms
- **Bottom nav active pill** — Scale + color transition, 150ms
- **Error shake input** — `TranslateAnimation` ±8dp horizontal, 3 kali, 400ms total
- **Skeleton loading** — Shimmer animation saat data loading dari API

### 4.3 Feedback Haptic

| Aksi | Pattern |
|------|---------|
| Tap produk (tambah ke cart) | `CONFIRM` vibration pattern |
| Transaksi berhasil | `SUCCESS` vibration pattern (panjang) |
| Error / validasi gagal | `ERROR` vibration pattern |
| Swipe delete produk | `TICK` vibration ringan |

---

## 5. Alur Navigasi

### 5.1 Navigation Architecture

Aplikasi menggunakan kombinasi **Bottom Navigation + Fragment** untuk navigasi utama, dengan Activity terpisah untuk alur transaksi.

```
MainActivity (BottomNav)
├── HomeFragment          → Dashboard
├── POSActivity           → Kasir (Activity terpisah, full-screen)
│   ├── POSFragment
│   ├── CheckoutBottomSheet
│   └── SuccessFragment
├── ReportFragment        → Laporan
└── SettingsFragment      → Pengaturan
```

### 5.2 Alur Transaksi (Happy Path)

```
Buka POS
  → Tap chip customer
    → BottomSheet list customer
      → Pilih customer (golongan terbaca, harga menyesuaikan)
        → Tap produk (qty badge muncul, tap lagi = qty +1)
          → Long-press item keranjang (edit qty / hapus)
            → Tap "Bayar Sekarang"
              → Pilih metode bayar
                → Input nominal (kembalian real-time)
                  → Tap "Proses Bayar"
                    → [Loading] → Layar Sukses
                      → Tap "Cetak Struk" (Bluetooth thermal)
                        → Tap "Transaksi Baru" (cart reset)
```

### 5.3 Alur Login & Device Lock

```
Buka app
  → Cek token lokal
    ├── Valid → langsung ke Dashboard
    └── Tidak ada / expired → Layar Login
          → Login sukses → server catat device_id
            → Jika device_id berbeda:
                token lama diinvalidasi
                device lama → 401 Unauthorized
                → Paksa logout
                → Dialog: "Sesi Anda berakhir karena login di perangkat lain"
```

---

## 6. Panduan UX

### 6.1 Loading & Empty States

| State | Implementasi |
|-------|--------------|
| Loading data | Skeleton card shimmer (bukan full-screen spinner) |
| Empty list | Ilustrasi kecil + teks deskriptif + tombol aksi (contoh: *"Belum ada produk. Tambah Sekarang"*) |
| Error API | Ikon warning + pesan error + tombol "Coba Lagi" |
| Offline | Banner kuning sticky di atas: *"Tidak ada koneksi internet"* |

### 6.2 Validasi & Error Handling

- Validasi form real-time saat user keluar dari field (`onFocusChange`)
- Pesan error di bawah field yang bermasalah — warna merah `#EF4444`, 11sp
- Tombol submit tetap visible tapi **disabled** jika ada error
- Toast message untuk aksi berhasil (hijau) dan gagal (merah) — durasi `LENGTH_SHORT`
- Dialog konfirmasi untuk aksi destruktif (void transaksi, hapus produk)

### 6.3 Aksesibilitas

- Semua elemen interaktif minimum **44dp × 44dp** (touch target)
- Kontras teks: minimum **4.5:1** untuk teks normal, **3:1** untuk teks besar
- `contentDescription` pada semua ikon tanpa label teks
- Support **TalkBack** untuk screen reader
- Tidak mengandalkan warna saja sebagai indikator — selalu sertakan ikon/teks

### 6.4 Performance UX

| Target | Nilai |
|--------|-------|
| Load time pertama halaman | < 1.5 detik |
| Transaksi end-to-end (buka POS → sukses) | < 30 detik |
| Pagination produk | 20 item/page, load more saat scroll bawah |
| Image caching | Glide library |
| Offline-first | Cache lokal dengan **Room DB** untuk produk & customer |

---

## 7. Changelog

| Versi | Tanggal | Author | Perubahan |
|-------|---------|--------|-----------|
| v1.0 | 31 Mei 2026 | UI/UX Designer | Initial release — semua layar utama |

---

*Kasir UMKM — PRD UI/UX Design v1.0 — Confidential*