Berikut PRD yang bisa langsung kamu kirim ke tim mobile agar implementasi mereka seragam dengan backend multi-company yang sekarang.

PRD Mobile Sync: Multi Company Kasir UMKM

1. Overview
Aplikasi Kasir UMKM sekarang sudah berubah dari single company menjadi multi company.

Backend memakai pendekatan:

single database
shared tables
isolasi data dengan company_id
Artinya:

1 server dan 1 database bisa dipakai banyak UMKM
setiap user hanya boleh melihat data milik company-nya sendiri
frontend/mobile tidak boleh menentukan company_id
backend yang otomatis menentukan company berdasarkan user login
2. Tujuan untuk Tim Mobile
Tim mobile harus menyesuaikan flow aplikasi agar:

data yang ditampilkan selalu mengikuti company user login
response login/profile menampilkan info company
menu dan UX untuk SUPER_ADMIN dibedakan dari user biasa
flow CRUD master, transaksi, stok, dan laporan mengikuti isolasi company dari backend
3. Konsep Multi Company
3.1 Sumber Company Aktif
Company aktif diambil dari user yang sedang login.

Mobile tidak perlu mengirim:

company_id
Backend otomatis mengisi company_id dari user login.

3.2 Isolasi Data
Semua data berikut sekarang terikat ke company:

users
customer groups
customers
products
product prices
payment methods
app settings
sales
sales details
stock mutations
3.3 Dampak ke Mobile
Jika user login sebagai company A:

hanya akan melihat data company A
tidak akan bisa melihat data company B
tidak akan bisa edit/hapus data company B
4. Role User
Role yang dipakai backend
SUPER_ADMIN
ADMIN
KASIR
Dokumen awal juga menyebut OWNER, tapi saat ini backend yang aktif dan sudah disiapkan adalah:

SUPER_ADMIN
ADMIN
KASIR
Hak akses umum
SUPER_ADMIN
kelola company
lihat semua company
buat company baru
update company
nonaktifkan company
ADMIN / KASIR
hanya bekerja di company masing-masing
tidak bisa akses endpoint company management lintas tenant
5. Login Response Baru
Endpoint
POST /api/login

Request
{
  "email": "admin@mail.com",
  "password": "password",
  "device_id": "ANDROID123",
  "device_name": "Samsung A55"
}
Response
{
  "success": true,
  "message": "Login berhasil",
  "data": {
    "token": "TOKEN",
    "user": {
      "id": 1,
      "company_id": 1,
      "name": "Administrator",
      "email": "admin@mail.com",
      "role": "ADMIN",
      "status": "00"
    },
    "company": {
      "id": 1,
      "company_name": "Demo Company",
      "company_code": "DEMO",
      "status": 1
    }
  }
}
Mobile Notes
Setelah login, simpan:

token
user object
company object
Minimal company info yang perlu dipakai di mobile:

company.id
company.company_name
company.company_code
Gunakan untuk:

header/profile
identitas toko aktif
kontrol menu jika butuh
6. Profile Response Baru
Endpoint
GET /api/profile

Response
{
  "success": true,
  "message": "Profile berhasil diambil",
  "data": {
    "user": {
      "id": 1,
      "company_id": 1,
      "name": "Administrator",
      "email": "admin@mail.com",
      "role": "ADMIN"
    },
    "company": {
      "id": 1,
      "company_name": "Demo Company",
      "company_code": "DEMO",
      "status": 1
    }
  }
}
Mobile Notes
Gunakan endpoint ini saat:

app startup
refresh session
buka halaman profile
7. Aturan Penting Request Body
Jangan kirim company_id
Pada semua request create/update, mobile tidak boleh mengirim:

{
  "company_id": 1
}
Backend akan mengabaikan konsep itu dan mengambil company dari token user login.

Contoh entity yang tetap sama body-nya
customers
products
payment methods
sales
stock adjustments
app settings
Bedanya sekarang backend otomatis memasukkan company_id.

8. Company Management API
Endpoint ini hanya untuk SUPER_ADMIN.

8.1 Get Company List
GET /api/companies

Response Example
{
  "success": true,
  "message": "Daftar company berhasil diambil",
  "data": [
    {
      "id": 1,
      "company_name": "Demo Company",
      "company_code": "DEMO",
      "address": null,
      "phone": null,
      "email": null,
      "logo": null,
      "status": 1
    }
  ]
}
8.2 Create Company
POST /api/companies

Request
{
  "company_name": "Toko Maju Jaya",
  "company_code": "TMJ",
  "address": "Jl. Merdeka No. 10",
  "phone": "08123456789",
  "email": "tmj@mail.com",
  "logo": null,
  "status": 1
}
Response
{
  "success": true,
  "message": "Company berhasil dibuat",
  "data": {
    "id": 2,
    "company_name": "Toko Maju Jaya",
    "company_code": "TMJ",
    "address": "Jl. Merdeka No. 10",
    "phone": "08123456789",
    "email": "tmj@mail.com",
    "logo": null,
    "status": 1
  }
}
Catatan penting
Saat company dibuat, backend otomatis membuat data default untuk company tersebut:

customer groups default
payment methods default
app settings default
Jadi mobile tidak perlu membuat master default manual setelah create company.

8.3 Update Company
PUT /api/companies/{id}

Request
{
  "company_name": "Toko Maju Jaya Updated",
  "company_code": "TMJ",
  "address": "Jl. Merdeka No. 12",
  "phone": "08123456789",
  "email": "tmj@mail.com",
  "logo": null,
  "status": 1
}
8.4 Delete / Nonaktifkan Company
DELETE /api/companies/{id}

Behavior backend:

company tidak dihapus fisik
status diubah menjadi nonaktif
9. Perubahan Perilaku Endpoint Lama
Semua endpoint lama tetap dipakai, tetapi sekarang otomatis terisolasi per company.

Endpoint yang terdampak
GET/POST/PUT/DELETE /api/customer-groups
GET/POST/PUT/DELETE /api/customers
GET/POST/PUT/DELETE /api/products
GET/POST/PUT/DELETE /api/payment-methods
GET/PUT /api/app-settings
GET/POST /api/sales
POST /api/sales/{id}/void
GET /api/sales/{id}/receipt
GET /api/stocks
POST /api/stocks/adjustments
GET /api/stocks/{productId}/history
seluruh endpoint reports
Implikasi untuk Mobile
Mobile tetap memanggil endpoint yang sama, tetapi:

data sekarang hanya milik company user login
tidak perlu filter company manual di mobile
tidak perlu kirim company_id
10. Validasi dan Error yang Perlu Diperhatikan
Karena sekarang uniqueness banyak yang bersifat per company, maka artinya:

Boleh
company A punya product code PRD001
company B juga punya product code PRD001
Tidak boleh
dalam company yang sama, code yang sama dipakai dua kali
Contoh error
{
  "success": false,
  "message": "Kode produk sudah terdaftar, tidak boleh duplikat.",
  "data": null,
  "errors": {
    "product_code": [
      "Kode produk sudah terdaftar, tidak boleh duplikat."
    ]
  }
}
Contoh validasi relasi company
Jika user company A mengirim customer_id milik company B, backend akan menolak.

Contoh:

{
  "success": false,
  "message": "Customer tidak ditemukan atau tidak valid.",
  "data": null,
  "errors": {
    "customer_id": [
      "Customer tidak ditemukan atau tidak valid."
    ]
  }
}
Error login company
Jika user belum punya company atau company nonaktif:

{
  "success": false,
  "message": "User belum terhubung ke company manapun",
  "data": null
}
atau

{
  "success": false,
  "message": "Company user sedang tidak aktif",
  "data": null
}
11. UX Rules untuk Mobile
11.1 Setelah Login
Tampilkan identitas company aktif di:

home/dashboard
profile
drawer/sidebar jika ada
Contoh:

Demo Company
Code: DEMO
11.2 Role-based Menu
Jika role = SUPER_ADMIN, tampilkan menu:

Company List
Create Company
Edit Company
Jika role = ADMIN atau KASIR, sembunyikan menu company management.

11.3 CRUD Master
Tidak perlu ada pilihan company di form:

customer
product
payment method
customer group
app settings
Karena company sudah otomatis mengikuti akun login.

11.4 Sales
Flow sales tetap sama, hanya sekarang backend juga memvalidasi bahwa:

customer milik company yang sama
product milik company yang sama
payment method milik company yang sama
Jika ada mismatch, backend akan menolak.

11.5 Stok
Semua stok dan histori stok yang ditampilkan adalah stok milik company login saja.

11.6 Reports
Semua laporan:

daily
weekly
monthly
products
customers
payment methods
sudah otomatis hanya untuk company aktif user login.

12. Default Seed Data di Staging Saat Ini
Saat ini backend seed default:

company:

Demo Company
company_code = DEMO
user:

admin@mail.com / password
superadmin@mail.com / password
Role akun seed
admin@mail.com = ADMIN
superadmin@mail.com = SUPER_ADMIN
Mobile Recommendation
Gunakan 2 akun ini untuk test:

test flow normal tenant pakai admin@mail.com
test company management pakai superadmin@mail.com
13. Acceptance Criteria untuk Tim Mobile
Implementasi mobile dianggap sesuai jika:

Login menyimpan token, user, dan company.
Profile menampilkan info user dan company.
Mobile tidak pernah mengirim company_id ke backend.
Semua data master yang tampil mengikuti company user login.
Menu company management hanya muncul untuk SUPER_ADMIN.
SUPER_ADMIN bisa list/create/update/nonaktifkan company.
User biasa tidak bisa mengakses company management.
Semua error dari backend bisa ditampilkan ke user dengan jelas.
Flow sales, stok, dan laporan tetap berjalan tanpa perubahan body company manual.
14. Rekomendasi Urutan Implementasi Mobile
Update model login response agar support company
Update penyimpanan session/local storage:
token
user
company
Update halaman profile agar tampilkan data company
Tambahkan role check untuk SUPER_ADMIN
Tambahkan halaman company list
Tambahkan create/update/nonaktifkan company
Pastikan semua form existing tidak mengirim company_id
Tes ulang:
customer
product
sales
stocks
reports
15. Catatan Penting
Backend saat ini memakai company isolation di server side. Jadi walaupun mobile salah kirim ID entity dari company lain, backend tetap akan menolak. Ini bagus untuk security, dan mobile cukup fokus menampilkan pesan error dari backend dengan benar.

Kalau kamu mau, next saya bisa bantu ubah ini jadi:

file markdown yang lebih rapi langsung di project
versi handover per endpoint khusus tim mobile
checklist QA testing mobile untuk multi-company