Berikut PRD khusus update modul stok yang sudah seragam dengan backend kita saat ini. Kamu bisa langsung kirim ini ke tim mobile.

PRD Backend-Mobile Sync: Modul Stok V1

1. Overview
Modul stok sekarang sudah aktif di backend.

Sebelumnya stok hanya disiapkan sebagai field database. Sekarang stok sudah dipakai secara real untuk:

melihat stok barang saat ini
melihat stok barang per tanggal
mencatat histori mutasi stok
mengurangi stok saat transaksi penjualan
mengembalikan stok saat void transaksi
melakukan adjustment stok manual
Tujuan dokumen ini adalah agar implementasi mobile konsisten dengan behavior backend.

2. Scope
Included
Lihat stok barang saat ini
Lihat stok barang per tanggal
Lihat histori mutasi stok per produk
Adjustment stok manual
Validasi stok saat transaksi penjualan
Pengurangan stok saat sales
Pengembalian stok saat void sales
Not Included
Purchase module
Supplier module
Stock opname document
Transfer stock antar gudang
Multi warehouse
Auto reorder
3. Aturan Bisnis Stok
3.1 Stok Aktif
Field products.stock sekarang aktif dan menjadi stok berjalan saat ini.

3.2 Saat Produk Dibuat
Jika saat create product dikirim nilai stock > 0, backend akan:

set stok awal produk
membuat mutasi stok tipe INITIAL
3.3 Saat Produk Diupdate
Jika field stock dikirim pada update product, backend akan:

membandingkan stok lama dengan stok baru
membuat mutasi:
ADJUSTMENT_IN jika stok bertambah
ADJUSTMENT_OUT jika stok berkurang
3.4 Saat Sales Dibuat
Pada POST /api/sales:

backend validasi stok untuk setiap item
jika stok tidak cukup, transaksi gagal
jika stok cukup, stok dikurangi
backend membuat mutasi tipe SALE
3.5 Saat Sales Di-void
Pada POST /api/sales/{id}/void:

hanya transaksi status 00 yang bisa di-void
stok akan dikembalikan sesuai qty item
backend membuat mutasi tipe VOID
3.6 Posisi Stok Per Tanggal
Saat mobile memanggil stok per tanggal:

backend menghitung posisi stok berdasarkan histori stock_mutations
hasil merupakan stok sampai akhir hari tersebut
4. Tabel Terkait
products
Field penting:

id
product_code
product_name
unit
cost_price
stock
status
stock_mutations
Field penting:

id
product_id
mutation_date
mutation_type
reference_type
reference_id
qty_in
qty_out
stock_before
stock_after
note
created_by
status
5. Status dan Mutation Type
Product Status
00 = aktif
99 = nonaktif
Stock Mutation Type
INITIAL
ADJUSTMENT_IN
ADJUSTMENT_OUT
SALE
VOID
Reference Type
PRODUCT
SALE
SALE_VOID
6. API Endpoint Stok
Semua endpoint di bawah harus memakai:

Authorization: Bearer <token>
Accept: application/json
7. Get Current Stock
Endpoint
GET /api/stocks

Function
Menampilkan stok barang saat ini.

Query Param Optional
search
status
Response Example
{
  "success": true,
  "message": "Stok barang berhasil diambil",
  "data": [
    {
      "product_id": 1,
      "product_code": "PRD001",
      "product_name": "Aqua 600ml",
      "unit": "PCS",
      "current_stock": 20,
      "status": "00"
    }
  ]
}
Mobile Notes
Gunakan untuk halaman daftar stok
current_stock adalah stok terbaru
8. Get Stock As Of Date
Endpoint
GET /api/stocks?date=2026-06-01

Function
Menampilkan posisi stok per produk sampai tanggal tertentu.

Query Param Optional
date wajib untuk mode stok per tanggal
search
status
Response Example
{
  "success": true,
  "message": "Posisi stok per tanggal berhasil diambil",
  "data": [
    {
      "product_id": 1,
      "product_code": "PRD001",
      "product_name": "Aqua 600ml",
      "unit": "PCS",
      "status": "00",
      "stock_as_of_date": 18,
      "as_of_date": "2026-06-01"
    }
  ]
}
Mobile Notes
Gunakan untuk laporan stok berdasarkan tanggal
Nilai dihitung dari histori mutasi, bukan sekadar field products.stock
9. Get Stock Mutation History
Endpoint
GET /api/stocks/{productId}/history

Query Param Optional
start_date
end_date
Function
Menampilkan histori mutasi stok untuk 1 produk.

Response Example
{
  "success": true,
  "message": "Riwayat stok berhasil diambil",
  "data": {
    "product": {
      "id": 1,
      "product_code": "PRD001",
      "product_name": "Aqua 600ml",
      "unit": "PCS",
      "cost_price": "3000.00",
      "stock": "18.00",
      "status": "00"
    },
    "mutations": [
      {
        "id": 3,
        "product_id": 1,
        "mutation_date": "2026-06-01T10:00:00.000000Z",
        "mutation_type": "SALE",
        "reference_type": "SALE",
        "reference_id": 1,
        "qty_in": "0.00",
        "qty_out": "2.00",
        "stock_before": "20.00",
        "stock_after": "18.00",
        "note": "Pengurangan stok untuk invoice INV-20260601-0001",
        "created_by": 1,
        "status": "00"
      }
    ]
  }
}
Mobile Notes
Gunakan untuk detail histori stok produk
Tampilkan arah mutasi:
masuk jika qty_in > 0
keluar jika qty_out > 0
10. Stock Adjustment Manual
Endpoint
POST /api/stocks/adjustments

Function
Mengubah stok produk ke angka tertentu, bukan menambah qty incremental.

Request Body
{
  "product_id": 1,
  "new_stock": 25,
  "note": "Penambahan stok dari pembelian supplier"
}
Optional Field
{
  "mutation_date": "2026-06-01"
}
Response Example
{
  "success": true,
  "message": "Stok berhasil diubah",
  "data": {
    "id": 1,
    "product_code": "PRD001",
    "product_name": "Aqua 600ml",
    "unit": "PCS",
    "cost_price": "3000.00",
    "stock": "25.00",
    "status": "00"
  }
}
Important Mobile Rule
new_stock adalah stok akhir yang diinginkan, bukan qty tambahan.

Contoh:

stok sekarang 18
kirim new_stock = 25
backend anggap tambah 7
11. Perubahan Pada API Product
Create Product
POST /api/products

Sekarang field stock benar-benar dipakai.

Example
{
  "product_code": "PRD001",
  "product_name": "Aqua 600ml",
  "unit": "PCS",
  "cost_price": 3000,
  "stock": 20,
  "status": "00",
  "prices": [
    {
      "customer_group_id": 1,
      "selling_price": 5000
    }
  ]
}
Behavior
stok awal produk disimpan
mutasi INITIAL dibuat otomatis
Update Product
PUT /api/products/{id}

Jika field stock ikut dikirim, backend akan membuat mutasi adjustment otomatis.

12. Perubahan Pada API Sales
Create Sales
POST /api/sales

Sekarang sales akan mempengaruhi stok.

Behavior
validasi setiap produk harus ada
validasi stok cukup
jika stok tidak cukup, request gagal
jika sukses, stok berkurang dan mutasi SALE dibuat
Error Example
{
  "success": false,
  "message": "Stok produk tidak cukup",
  "data": {
    "product_id": 1,
    "available_stock": 3,
    "requested_qty": 5
  }
}
Mobile Notes
tampilkan pesan error langsung ke user
jika perlu, tampilkan sisa stok dari data.available_stock
13. Perubahan Pada API Void Sales
Endpoint
POST /api/sales/{id}/void

Behavior
hanya sales status 00 yang bisa di-void
stok dikembalikan
mutasi VOID dibuat otomatis
Error Example
{
  "success": false,
  "message": "Hanya transaksi sukses yang bisa di-void",
  "data": null
}
14. Format Error Response
Backend sekarang memakai format error seragam.

Format Umum
{
  "success": false,
  "message": "Pesan error",
  "data": null
}
Format Validasi
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
Contoh Error yang Harus Di-handle Mobile
Unauthorized. Silakan login terlebih dahulu.
Email atau password tidak valid
Kode produk sudah terdaftar, tidak boleh duplikat.
Customer tidak ditemukan atau tidak valid.
Produk pada item penjualan tidak ditemukan atau tidak valid.
Stok produk tidak cukup
Jumlah bayar kurang dari grand total
Produk tidak ditemukan.
Transaksi penjualan tidak ditemukan.
15. UX Rules untuk Mobile
Daftar Stok
tampilkan product_name, product_code, unit, current_stock
dukung search berdasarkan nama atau kode produk
Histori Stok
tampilkan tanggal mutasi
tampilkan tipe mutasi
tampilkan qty masuk / qty keluar
tampilkan stok sebelum dan sesudah
tampilkan catatan
Adjustment Stok
gunakan input angka stok akhir
tampilkan konfirmasi sebelum submit
tampilkan error response dari backend apa adanya jika gagal
Sales
sebelum submit, mobile tidak wajib hitung stok sendiri
backend menjadi sumber validasi utama
jika backend kirim stok tidak cukup, tampilkan pesan backend
16. Acceptance Criteria
Backend-mobile dianggap sinkron jika:

mobile bisa menampilkan stok saat ini
mobile bisa menampilkan stok per tanggal
mobile bisa menampilkan histori mutasi stok
mobile bisa melakukan adjustment stok manual
sales gagal jika stok kurang
sales sukses mengurangi stok
void sales sukses mengembalikan stok
semua error message backend bisa ditampilkan dengan jelas di mobile
17. Rekomendasi Implementasi Mobile
Urutan implementasi yang saya sarankan untuk tim mobile:

Integrasi GET /api/stocks
Integrasi GET /api/stocks/{productId}/history
Integrasi POST /api/stocks/adjustments
Update flow create product agar field stock dipakai
Update flow sales agar handle error stok
Update flow void agar refresh stok setelah sukses
Tambah halaman laporan stok per tanggal via GET /api/stocks?date=YYYY-MM-DD