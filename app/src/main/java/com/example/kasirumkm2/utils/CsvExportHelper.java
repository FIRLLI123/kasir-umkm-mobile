package com.example.kasirumkm2.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.core.content.FileProvider;

import com.example.kasirumkm2.data.Customer;
import com.example.kasirumkm2.data.Product;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class CsvExportHelper {

    /**
     * Converts sales JSON array into CSV string
     */
    public static String generateSalesCsv(JsonArray dataArray) {
        StringBuilder sb = new StringBuilder();
        // UTF-8 BOM to ensure proper character rendering in Excel
        sb.append('\ufeff');
        sb.append("ID Transaksi,No Invoice,Tanggal,Pelanggan,Subtotal,Diskon,Total Belanja,Nominal Bayar,Kembalian,Metode Pembayaran,Status\n");

        for (int i = 0; i < dataArray.size(); i++) {
            try {
                JsonObject sale = dataArray.get(i).getAsJsonObject();
                int id = sale.has("id") ? sale.get("id").getAsInt() : 0;
                String invoiceNo = sale.has("invoice_no") ? sale.get("invoice_no").getAsString() : "-";
                
                String rawDate = sale.has("created_at") ? sale.get("created_at").getAsString() : "";
                String dateStr = "";
                try {
                    dateStr = CurrencyHelper.formatDateTime(rawDate);
                } catch (Exception e) {
                    dateStr = rawDate;
                }

                String customerName = "Pelanggan Umum";
                if (sale.has("customer") && !sale.get("customer").isJsonNull()) {
                    JsonObject cust = sale.getAsJsonObject("customer");
                    customerName = cust.has("customer_name") ? cust.get("customer_name").getAsString() : "Pelanggan Umum";
                }

                double subtotal = sale.has("subtotal") ? sale.get("subtotal").getAsDouble() : 0;
                double discount = sale.has("discount") ? sale.get("discount").getAsDouble() : 0;
                double grandTotal = sale.has("grand_total") ? sale.get("grand_total").getAsDouble() : 0;
                double paidAmount = sale.has("paid_amount") ? sale.get("paid_amount").getAsDouble() : 0;
                double changeAmount = sale.has("change_amount") ? sale.get("change_amount").getAsDouble() : 0;

                String paymentMethod = "-";
                if (sale.has("payment_method") && !sale.get("payment_method").isJsonNull()) {
                    JsonObject pm = sale.getAsJsonObject("payment_method");
                    if (pm.has("method_name")) {
                        paymentMethod = pm.get("method_name").getAsString();
                    } else if (pm.has("name")) {
                        paymentMethod = pm.get("name").getAsString();
                    }
                }

                String status = "Sukses";
                String statusVal = sale.has("status") ? sale.get("status").getAsString() : "00";
                if ("01".equals(statusVal) || "98".equals(statusVal)) {
                    status = "Voided";
                }

                sb.append(escapeCsvField(String.valueOf(id))).append(",")
                        .append(escapeCsvField(invoiceNo)).append(",")
                        .append(escapeCsvField(dateStr)).append(",")
                        .append(escapeCsvField(customerName)).append(",")
                        .append(escapeCsvField(String.valueOf(subtotal))).append(",")
                        .append(escapeCsvField(String.valueOf(discount))).append(",")
                        .append(escapeCsvField(String.valueOf(grandTotal))).append(",")
                        .append(escapeCsvField(String.valueOf(paidAmount))).append(",")
                        .append(escapeCsvField(String.valueOf(changeAmount))).append(",")
                        .append(escapeCsvField(paymentMethod)).append(",")
                        .append(escapeCsvField(status)).append("\n");
            } catch (Exception e) {
                // Skip faulty rows
            }
        }
        return sb.toString();
    }

    /**
     * Generates a daily sales summary CSV (Rekapan Penjualan Harian).
     * Columns: Tanggal, Jumlah Transaksi, Omzet, Modal, Laba Kotor, Margin (%)
     */
    public static String generateSalesSummaryCsv(JsonArray dataArray) {
        StringBuilder sb = new StringBuilder();
        sb.append('\ufeff');
        sb.append("Tanggal,Jumlah Transaksi,Omzet (Rp),Modal (Rp),Laba Kotor (Rp),Margin (%)\n");

        // Group by date
        java.util.LinkedHashMap<String, double[]> byDate = new java.util.LinkedHashMap<>();
        // Each value: [count, omzet, modal, margin]

        for (int i = 0; i < dataArray.size(); i++) {
            try {
                JsonObject sale = dataArray.get(i).getAsJsonObject();
                String status = sale.has("status") ? sale.get("status").getAsString() : "00";
                if (!"00".equals(status)) continue; // skip voided

                String rawDate = sale.has("created_at") ? sale.get("created_at").getAsString() : "";
                String dateKey = rawDate.length() >= 10 ? rawDate.substring(0, 10) : rawDate;

                double omzet = sale.has("grand_total") ? sale.get("grand_total").getAsDouble() : 0;
                double modal = sale.has("total_modal") ? sale.get("total_modal").getAsDouble() : 0;
                double laba = sale.has("total_margin") ? sale.get("total_margin").getAsDouble() :
                              (omzet - modal);

                double[] entry = byDate.getOrDefault(dateKey, new double[]{0, 0, 0, 0});
                entry[0]++;
                entry[1] += omzet;
                entry[2] += modal;
                entry[3] += laba;
                byDate.put(dateKey, entry);
            } catch (Exception ignored) {}
        }

        for (java.util.Map.Entry<String, double[]> entry : byDate.entrySet()) {
            double[] v = entry.getValue();
            double marginPct = v[1] > 0 ? (v[3] / v[1]) * 100 : 0;
            sb.append(escapeCsvField(entry.getKey())).append(",")
              .append(escapeCsvField(String.valueOf((int) v[0]))).append(",")
              .append(escapeCsvField(String.format(java.util.Locale.getDefault(), "%.0f", v[1]))).append(",")
              .append(escapeCsvField(String.format(java.util.Locale.getDefault(), "%.0f", v[2]))).append(",")
              .append(escapeCsvField(String.format(java.util.Locale.getDefault(), "%.0f", v[3]))).append(",")
              .append(escapeCsvField(String.format(java.util.Locale.getDefault(), "%.1f", marginPct))).append("\n");
        }
        return sb.toString();
    }

    /**
     * Generates a per-product margin report CSV.
     * Iterates through all sales detail items.
     * Columns: Nama Produk, Total Qty Terjual, Total Omzet, Total Modal, Laba Kotor, Margin (%)
     */
    public static String generateMarginReportCsv(JsonArray salesArray) {
        StringBuilder sb = new StringBuilder();
        sb.append('\ufeff');
        sb.append("Nama Produk,Kode Produk,Total Qty Terjual,Harga Modal (Rp),Harga Jual (Rp),Total Omzet (Rp),Total Modal (Rp),Laba Kotor (Rp),Margin (%)\n");

        // Map: productName -> [qty, hargaModal, hargaJual, omzet, modal, laba]
        java.util.LinkedHashMap<String, double[]> byProduct = new java.util.LinkedHashMap<>();
        java.util.LinkedHashMap<String, String> productCodes = new java.util.LinkedHashMap<>();

        for (int i = 0; i < salesArray.size(); i++) {
            try {
                JsonObject sale = salesArray.get(i).getAsJsonObject();
                String status = sale.has("status") ? sale.get("status").getAsString() : "00";
                if (!"00".equals(status)) continue;

                // Try to get items from details / items key
                JsonArray items = null;
                if (sale.has("details") && sale.get("details").isJsonArray()) {
                    items = sale.getAsJsonArray("details");
                } else if (sale.has("items") && sale.get("items").isJsonArray()) {
                    items = sale.getAsJsonArray("items");
                }
                if (items == null) continue;

                for (int j = 0; j < items.size(); j++) {
                    JsonObject item = items.get(j).getAsJsonObject();
                    String productName = "-";
                    String productCode = "-";

                    if (item.has("product") && !item.get("product").isJsonNull()) {
                        JsonObject prod = item.getAsJsonObject("product");
                        productName = prod.has("product_name") ? prod.get("product_name").getAsString() : "-";
                        productCode = prod.has("product_code") ? prod.get("product_code").getAsString() : "-";
                    } else {
                        productName = item.has("product_name") ? item.get("product_name").getAsString() : "-";
                    }

                    double qty = item.has("qty") ? item.get("qty").getAsDouble() : 0;
                    double hargaModal = item.has("cost_price") ? item.get("cost_price").getAsDouble() :
                                       (item.has("modal_price") ? item.get("modal_price").getAsDouble() : 0);
                    double hargaJual = item.has("unit_price") ? item.get("unit_price").getAsDouble() :
                                      (item.has("selling_price") ? item.get("selling_price").getAsDouble() : 0);
                    double totalOmzet = item.has("subtotal") ? item.get("subtotal").getAsDouble() : (hargaJual * qty);
                    double totalModal = hargaModal * qty;
                    double laba = totalOmzet - totalModal;

                    double[] entry = byProduct.getOrDefault(productName, new double[]{0, 0, 0, 0, 0, 0});
                    entry[0] += qty;        // total qty
                    entry[1] = hargaModal;  // unit modal (take latest)
                    entry[2] = hargaJual;   // unit jual (take latest)
                    entry[3] += totalOmzet; // cumulative omzet
                    entry[4] += totalModal; // cumulative modal
                    entry[5] += laba;       // cumulative laba
                    byProduct.put(productName, entry);
                    productCodes.put(productName, productCode);
                }
            } catch (Exception ignored) {}
        }

        for (java.util.Map.Entry<String, double[]> entry : byProduct.entrySet()) {
            double[] v = entry.getValue();
            double marginPct = v[3] > 0 ? (v[5] / v[3]) * 100 : 0;
            String code = productCodes.getOrDefault(entry.getKey(), "-");
            sb.append(escapeCsvField(entry.getKey())).append(",")
              .append(escapeCsvField(code)).append(",")
              .append(escapeCsvField(String.format(java.util.Locale.getDefault(), "%.0f", v[0]))).append(",")
              .append(escapeCsvField(String.format(java.util.Locale.getDefault(), "%.0f", v[1]))).append(",")
              .append(escapeCsvField(String.format(java.util.Locale.getDefault(), "%.0f", v[2]))).append(",")
              .append(escapeCsvField(String.format(java.util.Locale.getDefault(), "%.0f", v[3]))).append(",")
              .append(escapeCsvField(String.format(java.util.Locale.getDefault(), "%.0f", v[4]))).append(",")
              .append(escapeCsvField(String.format(java.util.Locale.getDefault(), "%.0f", v[5]))).append(",")
              .append(escapeCsvField(String.format(java.util.Locale.getDefault(), "%.1f", marginPct))).append("\n");
        }
        return sb.toString();
    }

    /**
     * Generates a product sales ranking report CSV.
     * Sorted by total omzet descending.
     * Columns: Ranking, Nama Produk, Total Qty Terjual, Total Omzet (Rp)
     */
    public static String generateProductSalesReportCsv(JsonArray dataArray) {
        StringBuilder sb = new StringBuilder();
        sb.append('\ufeff');
        sb.append("Ranking,Nama Produk,Total Qty Terjual,Total Omzet (Rp)\n");

        java.util.LinkedHashMap<String, double[]> byProduct = new java.util.LinkedHashMap<>();

        for (int i = 0; i < dataArray.size(); i++) {
            try {
                JsonObject item = dataArray.get(i).getAsJsonObject();
                String name = item.has("product_name") ? item.get("product_name").getAsString() : "-";
                double qty = 0, omzet = 0;

                if (item.has("qty_sold")) {
                    try { qty = item.get("qty_sold").getAsDouble(); } catch (Exception e) { qty = 0; }
                }
                if (item.has("total_sales")) {
                    try { omzet = item.get("total_sales").getAsDouble(); } catch (Exception e) { omzet = 0; }
                }

                double[] entry = byProduct.getOrDefault(name, new double[]{0, 0});
                entry[0] += qty;
                entry[1] += omzet;
                byProduct.put(name, entry);
            } catch (Exception ignored) {}
        }

        // Sort by omzet descending
        java.util.List<java.util.Map.Entry<String, double[]>> sorted =
            new java.util.ArrayList<>(byProduct.entrySet());
        sorted.sort((a, b) -> Double.compare(b.getValue()[1], a.getValue()[1]));

        int rank = 1;
        for (java.util.Map.Entry<String, double[]> entry : sorted) {
            double[] v = entry.getValue();
            sb.append(escapeCsvField(String.valueOf(rank++))).append(",")
              .append(escapeCsvField(entry.getKey())).append(",")
              .append(escapeCsvField(String.format(java.util.Locale.getDefault(), "%.0f", v[0]))).append(",")
              .append(escapeCsvField(String.format(java.util.Locale.getDefault(), "%.0f", v[1]))).append("\n");
        }
        return sb.toString();
    }

    /**
     * Converts products list into CSV string
     */
    public static String generateProductsCsv(List<Product> products, int userGroupId) {
        StringBuilder sb = new StringBuilder();
        sb.append('\ufeff');
        sb.append("ID Produk,Kode Produk,Nama Produk,Satuan,Harga Modal,Harga Jual,Stok,Status\n");

        for (Product p : products) {
            try {
                String statusStr = p.isActive() ? "Aktif" : "Nonaktif";
                double sellingPrice = p.getSellingPrice(userGroupId);
                if (sellingPrice == 0 && p.getPrices() != null && !p.getPrices().isEmpty()) {
                    sellingPrice = p.getPrices().get(0).getSellingPrice();
                }

                sb.append(escapeCsvField(String.valueOf(p.getId()))).append(",")
                        .append(escapeCsvField(p.getProductCode())).append(",")
                        .append(escapeCsvField(p.getProductName())).append(",")
                        .append(escapeCsvField(p.getUnit())).append(",")
                        .append(escapeCsvField(String.valueOf(p.getCostPrice()))).append(",")
                        .append(escapeCsvField(String.valueOf(sellingPrice))).append(",")
                        .append(escapeCsvField(String.valueOf(p.getStock()))).append(",")
                        .append(escapeCsvField(statusStr)).append("\n");
            } catch (Exception e) {
                // Skip faulty rows
            }
        }
        return sb.toString();
    }

    /**
     * Converts customer list into CSV string
     */
    public static String generateCustomersCsv(List<Customer> customers) {
        StringBuilder sb = new StringBuilder();
        sb.append('\ufeff');
        sb.append("ID Customer,Kode Customer,Nama Customer,Telepon,Alamat,Grup Customer,Status\n");

        for (Customer c : customers) {
            try {
                String statusStr = "00".equals(c.getStatus()) ? "Aktif" : "Nonaktif";
                sb.append(escapeCsvField(String.valueOf(c.getId()))).append(",")
                        .append(escapeCsvField(c.getCustomerCode())).append(",")
                        .append(escapeCsvField(c.getCustomerName())).append(",")
                        .append(escapeCsvField(c.getPhone())).append(",")
                        .append(escapeCsvField(c.getAddress())).append(",")
                        .append(escapeCsvField(c.getGroupName())).append(",")
                        .append(escapeCsvField(statusStr)).append("\n");
            } catch (Exception e) {
                // Skip faulty rows
            }
        }
        return sb.toString();
    }

    /**
     * Escapes CSV fields to prevent issues with commas, quotes, and newlines
     */
    private static String escapeCsvField(String field) {
        if (field == null) {
            return "";
        }
        String stringValue = field.trim();
        if (stringValue.contains(",") || stringValue.contains("\"") || stringValue.contains("\n") || stringValue.contains("\r")) {
            return "\"" + stringValue.replace("\"", "\"\"") + "\"";
        }
        return stringValue;
    }

    /**
     * Saves CSV content to cache for sharing/opening and returns its FileProvider Uri
     */
    public static Uri saveToCache(Context context, String csvContent, String fileName) {
        try {
            File exportDir = new File(context.getCacheDir(), "exports");
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }
            File file = new File(exportDir, fileName);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(csvContent.getBytes(StandardCharsets.UTF_8));
            }
            return FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Saves CSV content directly to the public Downloads folder
     * @return boolean indicating success
     */
    public static boolean saveToDownloads(Context context, String csvContent, String fileName) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentResolver resolver = context.getContentResolver();
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "text/csv");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
                if (uri != null) {
                    try (OutputStream os = resolver.openOutputStream(uri)) {
                        if (os != null) {
                            os.write(csvContent.getBytes(StandardCharsets.UTF_8));
                            return true;
                        }
                    }
                }
            } else {
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs();
                }
                File file = new File(downloadsDir, fileName);
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(csvContent.getBytes(StandardCharsets.UTF_8));
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
