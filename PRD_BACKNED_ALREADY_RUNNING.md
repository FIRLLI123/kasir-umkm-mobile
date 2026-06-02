Urutan paling enak di Postman adalah: login, cek master bawaan, buat customer, buat product + harga per golongan, lalu buat transaksi pertama. Semua endpoint selain login pakai header:

Accept: application/json
Authorization: Bearer TOKEN_DARI_LOGIN
Content-Type: application/json
Base URL sesuaikan setup kamu. Umumnya salah satu ini:

http://localhost/kasir-umkm/public/api
http://kasir-umkm.test/api
1. Login
POST /login

{
  "email": "admin@mail.com",
  "password": "password",
  "device_id": "ANDROID123",
  "device_name": "Samsung A55"
}
Ambil data.token dari response.

2. Cek profile
GET /profile

Tanpa body.

3. Cek customer group default
GET /customer-groups

Tanpa body.
Harusnya ada USER, FREELANCER, GROSIR.

4. Cek payment method default
GET /payment-methods

Tanpa body.
Harusnya ada CASH, TRANSFER, QRIS.

5. Cek app settings default
GET /app-settings

Tanpa body.

6. Buat customer pertama
POST /customers

Contoh pakai customer_group_id: 1 dulu. Kalau hasil GET /customer-groups berbeda, sesuaikan ID-nya.

{
  "customer_code": "CUST001",
  "customer_name": "Budi Santoso",
  "phone": "081234567890",
  "address": "Jl. Melati No. 10",
  "customer_group_id": 1,
  "status": "00"
}
7. Cek detail customer
GET /customers/1

Kalau ID customer hasil create bukan 1, pakai ID sebenarnya.

8. Buat produk pertama
POST /products

{
  "product_code": "PRD001",
  "product_name": "Aqua 600ml",
  "unit": "PCS",
  "cost_price": 3000,
  "stock": 0,
  "status": "00",
  "prices": [
    {
      "customer_group_id": 1,
      "selling_price": 5000
    },
    {
      "customer_group_id": 2,
      "selling_price": 4500
    },
    {
      "customer_group_id": 3,
      "selling_price": 4000
    }
  ]
}
9. Cek list produk
GET /products

10. Buat produk kedua
POST /products

{
  "product_code": "PRD002",
  "product_name": "Mie Instan",
  "unit": "PCS",
  "cost_price": 2000,
  "stock": 0,
  "status": "00",
  "prices": [
    {
      "customer_group_id": 1,
      "selling_price": 3500
    },
    {
      "customer_group_id": 2,
      "selling_price": 3200
    },
    {
      "customer_group_id": 3,
      "selling_price": 3000
    }
  ]
}
11. Buat transaksi pertama
POST /sales

Contoh ini asumsi:

customer id = 1
payment method CASH punya id = 1
product Aqua id = 1
product Mie id = 2
{
  "customer_id": 1,
  "payment_method_id": 1,
  "discount": 0,
  "paid_amount": 20000,
  "items": [
    {
      "product_id": 1,
      "qty": 2
    },
    {
      "product_id": 2,
      "qty": 1
    }
  ]
}
Perhitungan untuk customer group USER:

Aqua 2 x 5000 = 10000
Mie 1 x 3500 = 3500
subtotal = 13500
grand_total = 13500
change_amount = 6500
12. Cek list sales
GET /sales

Bisa juga pakai filter:
GET /sales?start_date=2026-05-30&end_date=2026-05-30

13. Cek detail transaksi
GET /sales/1

14. Cek receipt
GET /sales/1/receipt

Ini data yang nanti dipakai Android untuk print thermal.

15. Cek laporan harian
GET /reports/daily

Atau pakai tanggal spesifik:
GET /reports/daily?date=2026-05-30

16. Cek laporan mingguan
GET /reports/weekly

17. Cek laporan bulanan
GET /reports/monthly

18. Cek laporan produk
GET /reports/products?start_date=2026-05-30&end_date=2026-05-30

19. Cek laporan customer
GET /reports/customers?start_date=2026-05-30&end_date=2026-05-30

20. Cek laporan payment method
GET /reports/payment-methods?start_date=2026-05-30&end_date=2026-05-30

21. Tes void transaksi
POST /sales/1/void

{
  "void_reason": "Customer batal beli"
}
Lalu cek lagi:

GET /sales/1
GET /reports/daily?date=2026-05-30
Supaya kelihatan status transaksi berubah jadi void.