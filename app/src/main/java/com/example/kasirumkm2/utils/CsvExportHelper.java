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
