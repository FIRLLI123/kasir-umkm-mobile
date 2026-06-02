# PRD_ANDROID_STRUCTURE

## Overview

Project Android Native Java untuk aplikasi Kasir UMKM.

Dokumen ini hanya mengatur:

* struktur source code
* package
* activity
* fragment
* adapter
* session
* model
* networking

Detail UI/UX mengacu penuh ke:

```text
PRD_UI_UX_DESIGN.md
```

dan tidak boleh dibuat ulang di dokumen ini.

---

# Technology Stack

Bahasa:

```text
Java
```

Minimum SDK:

```text
26
```

Target SDK:

```text
Latest Stable
```

Library:

```text
Retrofit
Gson
RecyclerView
Material Components
Glide
Lottie
```

Belum digunakan:

```text
Room
Hilt
MVVM
Clean Architecture
```

---

# Package Structure

```text
com.kasirumkm

api
config
data
session
adapter
ui
utils
printer
```

---

# Config Package

```text
config

Config.java
```

Berisi:

```java
BASE_URL
APP_NAME
TIMEOUT
```

Base URL masih local development.

Contoh:

```java
http://10.0.2.2/kasir-umkm/public/api/
```

---

# Session Package

```text
session

SessionManager
```

Data yang disimpan:

```text
token
user_id
name
email
role
device_id
```

Storage:

```text
SharedPreferences
```

---

# API Package

```text
api

ApiClient
ApiService
ApiResponse
```

Tugas:

* retrofit
* interceptor
* logging
* token injection

---

# Data Models

## Auth

```text
User
LoginRequest
LoginResponse
```

## Customer

```text
Customer
CustomerGroup
```

## Product

```text
Product
ProductPrice
```

## Sales

```text
Sales
SalesDetail
Receipt
```

## Reports

```text
ReportDaily
ReportWeekly
ReportMonthly
ReportProduct
ReportCustomer
ReportPayment
```

---

# Adapter Package

```text
CustomerAdapter

ProductAdapter

CartAdapter

SalesAdapter
SalesDetailAdapter

ReportProductAdapter
ReportCustomerAdapter
ReportPaymentAdapter
```

---

# Activity Structure

## Auth

```text
LoginActivity
```

---

## Main

```text
MainActivity
```

Host untuk Bottom Navigation.

---

## POS

```text
POSActivity
CheckoutActivity
SuccessActivity
```

---

## Customer

```text
CustomerListActivity
CustomerFormActivity
CustomerDetailActivity
```

---

## Product

```text
ProductListActivity
ProductFormActivity
ProductDetailActivity
```

---

## Reports

```text
ReportActivity
```

Menggunakan TabLayout:

```text
Daily
Weekly
Monthly
Product
Customer
Payment
```

---

## Settings

```text
SettingsActivity
ProfileActivity
```

---

# Printer Package

Disiapkan sejak awal.

```text
printer

PrinterManager
PrinterFormatter
BluetoothPrinterHelper
```

Implementasi thermal printer dilakukan setelah transaksi selesai stabil.

---

# Navigation Flow

LoginActivity
↓

MainActivity

↓

Dashboard

↓

Customer
Product
POS
Reports
Settings

---

# Session Flow

App Open
↓

Check Token

↓

Valid
→ MainActivity

Invalid
→ LoginActivity

---

# Logout Flow

Clear Session

↓

Clear Token

↓

LoginActivity

↓

Finish All Activity
