# PRD — Onboarding Guide Tour: Tanya Kasir

> **Fitur:** First-run Onboarding (Guide Tour)
> **Platform:** Android Native (Java)
> **Versi:** 1.0 | **Tanggal:** 9 Juni 2026 | **Status:** Draft

---

## Daftar Isi

1. [Overview](#1-overview)
2. [Karakter Airin](#2-karakter-airin)
3. [Alur & Logika Tampil](#3-alur--logika-tampil)
4. [Spesifikasi Tiap Slide](#4-spesifikasi-tiap-slide)
5. [Design System Onboarding](#5-design-system-onboarding)
6. [Animasi & Transisi](#6-animasi--transisi)
7. [Navigasi Setelah Onboarding](#7-navigasi-setelah-onboarding)
8. [Spesifikasi Teknis Android](#8-spesifikasi-teknis-android)
9. [Acceptance Criteria](#9-acceptance-criteria)
10. [Changelog](#10-changelog)

---

## 1. Overview

### Tujuan Fitur

Onboarding Guide Tour adalah layar sambutan yang ditampilkan **satu kali** saat pengguna pertama kali menginstall dan membuka aplikasi Tanya Kasir. Tujuannya:

- Memperkenalkan karakter maskot **Airin** sebagai asisten pribadi pengguna
- Menjelaskan secara singkat apa itu Tanya Kasir
- Menyampaikan fitur-fitur unggulan aplikasi
- Mengarahkan pengguna ke halaman **Login** atau **Register**

### Ruang Lingkup

| Item | Keterangan |
|------|-----------|
| Jumlah slide | 4 slide |
| Trigger tampil | Hanya saat **pertama kali install** (belum ada data onboarding di SharedPreferences) |
| Bisa dilewati | Ya — tombol "Lewati" tersedia di slide 1, 2, dan 3 |
| Setelah selesai | Tidak pernah muncul lagi (flag disimpan di SharedPreferences) |

---

## 2. Karakter Airin

### Deskripsi Karakter

Airin adalah maskot asisten AI dari aplikasi Tanya Kasir. Ia digambarkan sebagai karakter chibi perempuan dengan:

- Rambut coklat panjang diikat ke belakang
- Baju kemeja teal (sesuai warna brand `#0EA5A0`)
- Ekspresi wajah yang bervariasi sesuai konteks
- Gaya ilustrasi: **full chibi anime 2D**, bukan avatar sederhana

### Daftar Aset Gambar Airin

Semua file berada di:
```
res/drawable/
```

| Nama File | Ekspresi | Digunakan di Slide |
|-----------|----------|--------------------|
| `welcome.png` | Tangan terbuka lebar, senyum lebar, menyambut | Slide 1 |
| `menjelaskan.png` | Satu tangan menunjuk ke atas, ekspresi serius-ramah | Slide 2 |
| `goodjob.png` | Acungan jempol, ekspresi bangga | Slide 3 |
| `senang.png` | Senyum besar, ekspresi antusias | Slide 4 |

> **Catatan untuk developer:** Gambar Airin adalah aset PNG statis. Tidak ada animasi frame-by-frame pada karakter. Semua efek gerak menggunakan animasi pada `ImageView` (scale, translate, fade) — bukan animasi internal gambar.

### Ukuran & Posisi Airin

| Properti | Nilai |
|----------|-------|
| Ukuran `ImageView` | `200dp × 200dp` |
| `scaleType` | `fitCenter` |
| Posisi | Center horizontal, bagian atas konten (di atas teks) |
| Background ImageView | Transparan |

---

## 3. Alur & Logika Tampil

### Flowchart Logika

```
App dibuka pertama kali
        │
        ▼
Cek SharedPreferences
key: "onboarding_completed"
        │
   ┌────┴────┐
  false     true
   │          │
   ▼          ▼
OnboardingActivity    MainActivity / LoginActivity
(tampilkan slide)
        │
        ▼
User selesai (slide 4 tap tombol)
ATAU user tap "Lewati"
        │
        ▼
Set SharedPreferences
"onboarding_completed" = true
        │
        ▼
Ke LoginActivity atau RegisterActivity
```

### Implementasi Pengecekan

```java
// di SplashActivity.java atau MainActivity.java
SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
boolean onboardingDone = prefs.getBoolean("onboarding_completed", false);

if (!onboardingDone) {
    startActivity(new Intent(this, OnboardingActivity.class));
} else {
    startActivity(new Intent(this, LoginActivity.class));
}
finish();
```

---

## 4. Spesifikasi Tiap Slide

### Gambaran Umum Struktur Slide

Setiap slide memiliki struktur layout yang konsisten:

```
┌─────────────────────────────┐
│        [Status Bar]         │
├─────────────────────────────┤
│                             │
│      [Gambar Airin]         │  ← ImageView 200×200dp
│                             │
│      [Judul Utama]          │  ← TextView bold
│      [Subjudul/Deskripsi]   │  ← TextView muted
│                             │
│      [Konten Unik]          │  ← berbeda tiap slide
│                             │
│        ● ○ ○ ○              │  ← Dot indicator
│                             │
│    [Tombol Aksi Utama]      │  ← Button teal
│       [Lewati]              │  ← TextView link (slide 1-3)
└─────────────────────────────┘
```

---

### Slide 1 — Selamat Datang

**Tujuan:** Menyambut pengguna dan memperkenalkan nama aplikasi.

| Elemen | Spesifikasi |
|--------|-------------|
| Background | Gradient vertikal: `#E0F7F6` → `#F0FDFA` → `#FFFFFF` |
| Gambar Airin | `welcome.png` |
| Animasi Airin masuk | Slide up dari bawah 40dp + fade in, 500ms, `OvershootInterpolator` |
| Judul | **"Selamat datang di Tanya Kasir! 🎉"** |
| Ukuran judul | 22sp, Bold, `#1E293B` |
| Deskripsi | "Aplikasi kasir modern yang punya asisten AI pribadi buat bantu usaha kamu." |
| Ukuran deskripsi | 14sp, Regular, `#64748B` |
| Dot indicator | Posisi 1 aktif (teal pill), 3 sisanya abu |
| Tombol utama | **"Yuk, kenalan sama Airin →"** — full width, teal solid |
| Tombol lewati | "Lewati" — text link, `#94A3B8`, posisi bawah tombol utama |

---

### Slide 2 — Perkenalan Airin

**Tujuan:** Memperkenalkan Airin sebagai asisten AI, bukan sekadar mascot.

| Elemen | Spesifikasi |
|--------|-------------|
| Background | Gradient: `#F0FDFA` → `#FFFFFF` → `#FFF7ED` |
| Gambar Airin | `menjelaskan.png` |
| Animasi Airin | Crossfade dari slide sebelumnya + sedikit bounce scale 0.9 → 1.0 |
| Judul | **"Kenalan sama Airin dulu, yuk!"** |
| Badge AI | Pill kecil: ikon sparkles + teks "Didukung kecerdasan AI" — background `#EDE9FE`, teks `#5B21B6` |
| Info card | Card abu-putih berisi list kemampuan Airin (lihat di bawah) |
| Dot indicator | Posisi 2 aktif |
| Tombol utama | **"Lanjut →"** |
| Tombol lewati | "Lewati" |

**Isi info card kemampuan Airin:**

```
Aku bisa bantu kamu untuk:
💬  Chat & tanya jawab kapan aja
📊  Analisis laporan & omzet
💡  Kasih saran bisnis personal
🧾  Bantu input & cek transaksi
```

---

### Slide 3 — Fitur Aplikasi

**Tujuan:** Showcase fitur-fitur utama Tanya Kasir secara visual dan ringkas.

| Elemen | Spesifikasi |
|--------|-------------|
| Background | Putih `#FFFFFF` |
| Gambar Airin | `goodjob.png` |
| Animasi Airin | Crossfade + bounce |
| Judul | **"Semua yang kamu butuhkan ada di sini ✨"** |
| Ukuran judul | 20sp, Bold, `#1E293B` |
| Deskripsi | "Lengkap dengan stok, pembayaran, scan barcode, sampai dashboard omzet & margin." |
| Chip fitur | 6 chip teal (lihat tabel chip di bawah) |
| Stat row | 3 mini stat chip: Omzet, Margin, Customer |
| Dot indicator | Posisi 3 aktif |
| Tombol utama | **"Hampir selesai →"** |
| Tombol lewati | "Lewati" |

**Chip fitur (2 kolom, wrap):**

| Ikon | Label |
|------|-------|
| `ic_shopping_cart` | POS Kasir |
| `ic_scan` | Scan Barcode |
| `ic_box` | Stok Produk |
| `ic_chart_bar` | Dashboard |
| `ic_printer` | Struk Thermal |
| `ic_message_chatbot` | Chat AI |

**Style chip fitur:**
- Background: `#F0FDFA`
- Border: 1dp `#CCFBF1`
- Border radius: 8dp
- Padding: 6dp 10dp
- Teks: 11sp, Medium, `#0F766E`
- Ikon: 14dp, warna `#0EA5A0`

---

### Slide 4 — Mulai Sekarang

**Tujuan:** CTA akhir untuk mengarahkan pengguna ke Login atau Register. Tidak ada form input di sini.

| Elemen | Spesifikasi |
|--------|-------------|
| Background | Gradient: `#F0FDFA` → `#FFFFFF` |
| Gambar Airin | `senang.png` |
| Animasi Airin | Crossfade + bounce |
| Judul | **"Siap memulai? 🚀"** |
| Deskripsi | "Masuk atau daftar untuk mulai bersama Airin." |
| Dot indicator | Posisi 4 aktif |
| Tombol 1 (primer) | **"Masuk ke Tanya Kasir"** — full width, teal solid — navigasi ke `LoginActivity` |
| Tombol 2 (sekunder) | **"Daftar Akun Baru"** — full width, outline teal — navigasi ke `RegisterActivity` |
| Tombol lewati | **Tidak ada** di slide ini |

> **Catatan:** Sebelum berpindah ke `LoginActivity` atau `RegisterActivity`, set flag onboarding terlebih dahulu:
> ```java
> prefs.edit().putBoolean("onboarding_completed", true).apply();
> ```

---

## 5. Design System Onboarding

### Warna

| Token | HEX | Penggunaan |
|-------|-----|-----------|
| Primary Teal | `#0EA5A0` | Tombol utama, dot aktif, border chip |
| Primary Pale | `#F0FDFA` | Background slide, chip fitur |
| Primary Light | `#E0F7F6` | Gradient atas slide 1 |
| Text Dark | `#1E293B` | Judul utama |
| Text Gray | `#64748B` | Deskripsi, subjudul |
| Text Muted | `#94A3B8` | Tombol "Lewati", dot non-aktif |
| AI Badge BG | `#EDE9FE` | Background badge AI Airin |
| AI Badge Text | `#5B21B6` | Teks badge AI Airin |
| Card BG | `#F8FAFB` | Info card kemampuan Airin |
| Card Border | `#E2E8F0` | Border info card |

### Tipografi

| Elemen | Size | Weight | Warna |
|--------|------|--------|-------|
| Judul slide | 22sp | Bold 700 | `#1E293B` |
| Deskripsi | 14sp | Regular 400 | `#64748B` |
| Label chip fitur | 11sp | Medium 500 | `#0F766E` |
| Teks info card | 13sp | Regular 400 | `#475569` |
| Teks tombol primer | 14sp | Bold 700 | `#FFFFFF` |
| Teks tombol sekunder | 14sp | Bold 700 | `#0EA5A0` |
| Teks "Lewati" | 13sp | Regular 400 | `#94A3B8` |

### Komponen Tombol

**Tombol Primer (Masuk / Lanjut):**

| Properti | Nilai |
|----------|-------|
| Background | `#0EA5A0` |
| Border radius | 12dp |
| Padding | 14dp vertikal, 24dp horizontal |
| Height | 52dp |
| Width | `match_parent` dengan margin 24dp kanan-kiri |
| State pressed | `#0F766E`, scale 0.97 |
| State disabled | `#E2E8F0`, teks `#94A3B8` |

**Tombol Sekunder (Daftar):**

| Properti | Nilai |
|----------|-------|
| Background | Transparan |
| Border | 1.5dp solid `#0EA5A0` |
| Border radius | 12dp |
| Teks | `#0EA5A0` |
| Height | 52dp |
| Width | `match_parent` dengan margin 24dp kanan-kiri |

### Dot Indicator

| Properti | Dot Aktif | Dot Non-Aktif |
|----------|-----------|----------------|
| Shape | Pill (lebar 20dp, tinggi 8dp) | Lingkaran (diameter 8dp) |
| Warna | `#0EA5A0` | `#CBD5E1` |
| Border radius | 4dp | 50% |
| Animasi ganti | Width animate 8dp → 20dp, 200ms | Width animate 20dp → 8dp, 200ms |
| Gap antar dot | 6dp |

---

## 6. Animasi & Transisi

### Transisi Antar Slide

| Dari → Ke | Tipe | Durasi | Interpolator |
|-----------|------|--------|--------------|
| Slide 1 → 2 | Slide kiri (konten) + crossfade Airin | 350ms | `FastOutSlowInInterpolator` |
| Slide 2 → 3 | Slide kiri (konten) + crossfade Airin | 350ms | `FastOutSlowInInterpolator` |
| Slide 3 → 4 | Slide kiri (konten) + crossfade Airin | 350ms | `FastOutSlowInInterpolator` |
| Swipe kanan (back) | Slide kanan (konten) + crossfade Airin | 350ms | `FastOutSlowInInterpolator` |

> **Penting:** Gambar Airin di-*crossfade* terpisah dari konten teks. Ini menciptakan efek seolah Airin "tetap di tempat" sementara konten di belakangnya berganti. Gunakan dua `ImageView` dengan `AlphaAnimation` untuk efek ini.

### Animasi Masuk Pertama (Slide 1)

| Elemen | Animasi | Delay | Durasi |
|--------|---------|-------|--------|
| Background gradient | Fade in | 0ms | 300ms |
| Gambar Airin | Slide up 60dp + fade in | 100ms | 500ms, `OvershootInterpolator(1.1)` |
| Judul | Fade in + slide up 20dp | 350ms | 350ms |
| Deskripsi | Fade in | 500ms | 300ms |
| Tombol | Fade in + slide up 10dp | 600ms | 300ms |

### Animasi Airin Idle (Opsional, Rekomendasikan)

Setelah animasi masuk selesai, berikan animasi idle ringan pada Airin agar terasa "hidup":

```
Loop tak terbatas:
  Scale 1.0 → 1.03 → 1.0
  Durasi 1 siklus: 2000ms
  Interpolator: AccelerateDecelerateInterpolator
```

Implementasi:

```java
ObjectAnimator scaleX = ObjectAnimator.ofFloat(ivAirin, "scaleX", 1f, 1.03f, 1f);
ObjectAnimator scaleY = ObjectAnimator.ofFloat(ivAirin, "scaleY", 1f, 1.03f, 1f);
AnimatorSet idleAnim = new AnimatorSet();
idleAnim.playTogether(scaleX, scaleY);
idleAnim.setDuration(2000);
idleAnim.setRepeatCount(ValueAnimator.INFINITE); // set di masing-masing animator
idleAnim.setInterpolator(new AccelerateDecelerateInterpolator());
idleAnim.start();
```

### Animasi Tap Tombol

- Scale: `1.0 → 0.96 → 1.0`, 150ms, `OvershootInterpolator`
- Haptic: `HapticFeedbackConstants.CONFIRM`

### Navigasi Keluar (Tap Masuk / Daftar)

- Animasi: Fade out seluruh layar, 250ms
- Setelah fade out selesai → jalankan `startActivity()` + `finish()`

---

## 7. Navigasi Setelah Onboarding

### Dari Slide 4

| Aksi | Tujuan | Catatan |
|------|--------|---------|
| Tap "Masuk ke Tanya Kasir" | `LoginActivity` | Set flag onboarding = true sebelum navigate |
| Tap "Daftar Akun Baru" | `RegisterActivity` | Set flag onboarding = true sebelum navigate |

### Dari Tombol "Lewati" (Slide 1–3)

| Aksi | Tujuan |
|------|--------|
| Tap "Lewati" | Langsung loncat ke Slide 4 (bukan ke Login) |

> Tombol "Lewati" **tidak langsung** ke Login/Register — ia hanya melewati slide perkenalan dan mendarat di Slide 4 (CTA). Ini memastikan pengguna tetap melihat pilihan Masuk/Daftar.

### Intent Code Contoh

```java
// Tap "Masuk ke Tanya Kasir"
btnMasuk.setOnClickListener(v -> {
    saveOnboardingDone();
    startActivity(new Intent(OnboardingActivity.this, LoginActivity.class));
    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    finish();
});

// Tap "Daftar Akun Baru"
btnDaftar.setOnClickListener(v -> {
    saveOnboardingDone();
    startActivity(new Intent(OnboardingActivity.this, RegisterActivity.class));
    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    finish();
});

// Tap "Lewati"
btnLewati.setOnClickListener(v -> {
    viewPager.setCurrentItem(3, true); // loncat ke slide 4
});

private void saveOnboardingDone() {
    SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
    prefs.edit().putBoolean("onboarding_completed", true).apply();
}
```

---

## 8. Spesifikasi Teknis Android

### Struktur File yang Dibutuhkan

```
app/src/main/
├── java/.../
│   └── OnboardingActivity.java        ← Activity utama
│       OnboardingAdapter.java         ← ViewPager2 adapter
│       OnboardingFragment.java        ← Fragment tiap slide (atau pakai layout langsung)
│
├── res/
│   ├── layout/
│   │   ├── activity_onboarding.xml    ← Root: ViewPager2 + tombol overlay
│   │   └── item_onboarding_slide.xml  ← Layout 1 slide
│   │
│   ├── drawable/
│   │   ├── welcome.png
│   │   ├── menjelaskan.png
│   │   ├── goodjob.png
│   │   ├── senang.png
│   │   ├── bg_button_teal.xml         ← Shape drawable tombol primer
│   │   ├── bg_button_outline.xml      ← Shape drawable tombol sekunder
│   │   └── bg_dot_active.xml          ← Shape dot aktif (pill)
│   │
│   └── anim/
│       ├── fade_in.xml
│       ├── fade_out.xml
│       ├── slide_in_right.xml
│       └── slide_out_left.xml
```

### Komponen Utama

| Komponen | Rekomendasi |
|----------|-------------|
| Swipe antar slide | `ViewPager2` dengan `FragmentStateAdapter` atau `RecyclerView.Adapter` |
| Gambar Airin | `ImageView` dengan `scaleType="fitCenter"` |
| Dot indicator | Custom view atau library `me.relex:circleindicator:2.1.6` |
| Animasi | `ObjectAnimator`, `AnimatorSet`, `ViewPropertyAnimator` |
| Penyimpanan flag | `SharedPreferences` key: `onboarding_completed` |
| Gesture swipe | Bawaan `ViewPager2` — tidak perlu custom gesture |

### Layout `activity_onboarding.xml` (Konsep)

```xml
<RelativeLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <!-- ViewPager2 full screen -->
    <androidx.viewpager2.widget.ViewPager2
        android:id="@+id/viewPager"
        android:layout_width="match_parent"
        android:layout_height="match_parent"/>

    <!-- Overlay bawah: dot + tombol (di luar ViewPager agar tidak ikut slide) -->
    <LinearLayout
        android:layout_alignParentBottom="true"
        android:orientation="vertical"
        android:padding="24dp"
        android:layout_width="match_parent"
        android:layout_height="wrap_content">

        <!-- Dot indicator -->
        <me.relex.circleindicator.CircleIndicator3
            android:id="@+id/indicator"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginBottom="16dp"/>

        <!-- Tombol utama -->
        <Button android:id="@+id/btnNext" ... />

        <!-- Tombol sekunder (hanya visible di slide 4) -->
        <Button android:id="@+id/btnDaftar"
            android:visibility="gone" ... />

        <!-- Lewati -->
        <TextView android:id="@+id/tvLewati" ... />

    </LinearLayout>

</RelativeLayout>
```

### Logika Tombol Berdasarkan Posisi Slide

```java
viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
    @Override
    public void onPageSelected(int position) {
        if (position == 3) {
            // Slide terakhir
            btnNext.setText("Masuk ke Tanya Kasir");
            btnDaftar.setVisibility(View.VISIBLE);
            tvLewati.setVisibility(View.GONE);
        } else {
            btnNext.setText(position == 2 ? "Hampir selesai →" : "Lanjut →");
            if (position == 0) btnNext.setText("Yuk, kenalan sama Airin →");
            btnDaftar.setVisibility(View.GONE);
            tvLewati.setVisibility(View.VISIBLE);
        }
    }
});
```

### Data Slide (Model)

```java
public class OnboardingSlide {
    public int imageResId;      // R.drawable.welcome, dll.
    public String title;
    public String description;
    public int backgroundStartColor;
    public int backgroundEndColor;
    public int slideType;       // 0=normal, 1=dengan info card, 2=dengan chip, 3=auth CTA
}

// Data:
List<OnboardingSlide> slides = new ArrayList<>();

slides.add(new OnboardingSlide(
    R.drawable.welcome,
    "Selamat datang di Tanya Kasir! 🎉",
    "Aplikasi kasir modern yang punya asisten AI pribadi buat bantu usaha kamu.",
    Color.parseColor("#E0F7F6"),
    Color.parseColor("#FFFFFF"),
    SLIDE_TYPE_NORMAL
));

slides.add(new OnboardingSlide(
    R.drawable.menjelaskan,
    "Kenalan sama Airin dulu, yuk!",
    "",   // deskripsi di info card
    Color.parseColor("#F0FDFA"),
    Color.parseColor("#FFF7ED"),
    SLIDE_TYPE_INFO_CARD
));

slides.add(new OnboardingSlide(
    R.drawable.goodjob,
    "Semua yang kamu butuhkan ada di sini ✨",
    "Lengkap dengan stok, pembayaran, scan barcode, sampai dashboard omzet & margin.",
    Color.parseColor("#FFFFFF"),
    Color.parseColor("#FFFFFF"),
    SLIDE_TYPE_CHIPS
));

slides.add(new OnboardingSlide(
    R.drawable.senang,
    "Siap memulai? 🚀",
    "Masuk atau daftar untuk mulai bersama Airin.",
    Color.parseColor("#F0FDFA"),
    Color.parseColor("#FFFFFF"),
    SLIDE_TYPE_AUTH_CTA
));
```

---

## 9. Acceptance Criteria

### Fungsional

- [ ] Onboarding hanya muncul sekali saat pertama install
- [ ] Setelah onboarding selesai, tidak muncul lagi meski app di-restart
- [ ] Swipe kiri/kanan berfungsi untuk berpindah slide
- [ ] Tap tombol "Lanjut" pindah ke slide berikutnya
- [ ] Tap "Lewati" di slide 1–3 langsung ke slide 4
- [ ] Tap "Masuk ke Tanya Kasir" navigasi ke `LoginActivity`
- [ ] Tap "Daftar Akun Baru" navigasi ke `RegisterActivity`
- [ ] Dot indicator berubah sesuai posisi slide aktif
- [ ] Gambar Airin berganti sesuai slide dengan crossfade halus
- [ ] Tidak ada tombol "Lewati" di slide 4

### Visual

- [ ] Gambar Airin tampil dengan proporsional (tidak stretch/crop)
- [ ] Background gradient sesuai spesifikasi tiap slide
- [ ] Warna tombol sesuai design system (teal `#0EA5A0`)
- [ ] Tipografi sesuai ukuran dan weight per elemen
- [ ] Chip fitur di slide 3 tampil dengan wrap 2 kolom

### Performa

- [ ] Transisi antar slide smooth tanpa lag (target 60fps)
- [ ] Gambar Airin tidak menyebabkan memory spike (gunakan `inSampleSize` jika perlu)
- [ ] Animasi masuk pertama selesai dalam < 700ms total

### Edge Case

- [ ] Jika pengguna di slide 2/3 lalu rotate device, posisi slide tidak reset
- [ ] Back button di slide 1 keluar dari app (bukan crash)
- [ ] Back button di slide 2–4 kembali ke slide sebelumnya

---

## 10. Changelog

| Versi | Tanggal | Author | Perubahan |
|-------|---------|--------|-----------|
| v1.0 | 9 Juni 2026 | Product & UI/UX | Initial release |

---

*Tanya Kasir — PRD Onboarding Guide Tour v1.0 — Confidential*