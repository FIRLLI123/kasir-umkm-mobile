# PRD_API_INTEGRATION

## Overview

Dokumen ini mendefinisikan mapping endpoint backend Laravel ke Android.

Semua API menggunakan:

```http
Accept: application/json
Authorization: Bearer TOKEN
Content-Type: application/json
```

Kecuali login.

---

# Environment

## Emulator

```text
http://10.0.2.2/kasir-umkm/public/api/
```

## Real Device

```text
http://IP_KOMPUTER/kasir-umkm/public/api/
```

Contoh:

```text
http://192.168.1.10/kasir-umkm/public/api/
```

---

# AUTH MODULE

## Login

POST

```text
/login
```

Screen:

```text
LoginActivity
```

Action:

```text
Save Token
Save User Session
Open MainActivity
```

---

## Profile

GET

```text
/profile
```

Screen:

```text
ProfileActivity
```

---

## Logout

POST

```text
/logout
```

Screen:

```text
ProfileActivity
```

Action:

```text
Clear Session
Redirect Login
```

---

# CUSTOMER MODULE

## List

GET

```text
/customers
```

Screen:

```text
CustomerListActivity
```

---

## Detail

GET

```text
/customers/{id}
```

Screen:

```text
CustomerDetailActivity
```

---

## Create

POST

```text
/customers
```

Screen:

```text
CustomerFormActivity
```

---

## Update

PUT

```text
/customers/{id}
```

Screen:

```text
CustomerFormActivity
```

---

## Delete

DELETE

```text
/customers/{id}
```

Screen:

```text
CustomerDetailActivity
```

---

# CUSTOMER GROUP MODULE

## List

GET

```text
/customer-groups
```

Digunakan untuk:

```text
Spinner Customer Group
Customer Form
POS Customer Selection
```

---

# PRODUCT MODULE

## List

GET

```text
/products
```

Screen:

```text
ProductListActivity
POSActivity
```

---

## Detail

GET

```text
/products/{id}
```

---

## Create

POST

```text
/products
```

---

## Update

PUT

```text
/products/{id}
```

---

## Delete

DELETE

```text
/products/{id}
```

---

# PAYMENT METHOD MODULE

## List

GET

```text
/payment-methods
```

Digunakan pada:

```text
CheckoutActivity
```

---

# SALES MODULE

## Create Transaction

POST

```text
/sales
```

Screen:

```text
CheckoutActivity
```

Response:

```text
invoice_no
grand_total
paid_amount
change_amount
```

Setelah sukses:

```text
SuccessActivity
```

---

## Sales History

GET

```text
/sales
```

---

## Sales Detail

GET

```text
/sales/{id}
```

---

## Receipt

GET

```text
/sales/{id}/receipt
```

Digunakan:

```text
SuccessActivity
Printer Module
```

---

## Void

POST

```text
/sales/{id}/void
```

Digunakan:

```text
SalesDetailActivity
```

Role:

```text
Admin Only
```

---

# REPORT MODULE

## Daily

GET

```text
/reports/daily
```

---

## Weekly

GET

```text
/reports/weekly
```

---

## Monthly

GET

```text
/reports/monthly
```

---

## Product

GET

```text
/reports/products
```

---

## Customer

GET

```text
/reports/customers
```

---

## Payment Method

GET

```text
/reports/payment-methods
```

---

# HTTP RESPONSE HANDLING

## Success

```text
200
201
```

Tampilkan:

```text
Snackbar Success
```

---

## Validation Error

```text
422
```

Tampilkan:

```text
Pesan error dari backend
```

---

## Unauthorized

```text
401
```

Action:

```text
Clear Session
Redirect Login
```

Pesan:

```text
Sesi login telah berakhir
```

---

## Server Error

```text
500
```

Tampilkan:

```text
Terjadi kesalahan pada server
Silakan coba kembali
```

---

# Development Roadmap

Phase 1

```text
Login
Session
Dashboard
```

Phase 2

```text
Customer
Customer Group
```

Phase 3

```text
Product
```

Phase 4

```text
POS
Checkout
Sales
Receipt
```

Phase 5

```text
Reports
```

Phase 6

```text
Bluetooth Thermal Printer
```
