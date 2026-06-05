package com.example.kasirumkm2.printer;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import com.example.kasirumkm2.utils.CurrencyHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;

public class PrinterManager {

    private final BluetoothPrinterHelper printerHelper;

    public PrinterManager(Context context) {
        this.printerHelper = new BluetoothPrinterHelper(context);
    }

    public BluetoothPrinterHelper getPrinterHelper() {
        return printerHelper;
    }

    /**
     * Connect to printer device
     */
    public boolean connect(BluetoothDevice device) {
        return printerHelper.connect(device);
    }

    /**
     * Disconnect printer
     */
    public void disconnect() {
        printerHelper.disconnect();
    }

    /**
     * Print receipt for a successful transaction using JSON details from API
     */
    public boolean printReceipt(String shopName, JsonObject sale) {
        if (!printerHelper.isConnected()) return false;

        try {
            // 1. Initialize
            printerHelper.write(PrinterFormatter.ESC_INIT);

            // 2. Header (Centered, Double size bold)
            printerHelper.write(PrinterFormatter.ESC_ALIGN_CENTER);
            printerHelper.write(PrinterFormatter.ESC_DOUBLE_SIZE_ON);
            printerHelper.write(PrinterFormatter.ESC_BOLD_ON);

            String companyName = "KASIR UMKM";
            String companyAddress = "Jl. Raya UMKM Hebat No. 99";
            String companyPhone = "Telp: 0812-3456-7890";

            if (sale.has("company") && !sale.get("company").isJsonNull()) {
                JsonObject company = sale.getAsJsonObject("company");
                if (company.has("company_name") && !company.get("company_name").isJsonNull()) {
                    companyName = company.get("company_name").getAsString();
                }
                if (company.has("address") && !company.get("address").isJsonNull()) {
                    companyAddress = company.get("address").getAsString();
                }
                if (company.has("phone") && !company.get("phone").isJsonNull()) {
                    companyPhone = "Telp: " + company.get("phone").getAsString();
                } else {
                    companyPhone = "";
                }
            } else if (shopName != null && !shopName.isEmpty()) {
                companyName = shopName;
            }

            writeText(companyName + "\n");
            printerHelper.write(PrinterFormatter.ESC_DOUBLE_SIZE_OFF);
            printerHelper.write(PrinterFormatter.ESC_BOLD_OFF);

            // Sub-header (Centered)
            if (!companyAddress.isEmpty()) {
                writeText(companyAddress + "\n");
            }
            if (!companyPhone.isEmpty()) {
                writeText(companyPhone + "\n");
            }
            
            // 3. Info (Left aligned)
            printerHelper.write(PrinterFormatter.ESC_ALIGN_LEFT);
            writeText(PrinterFormatter.getDivider() + "\n");

            String invoiceNo = sale.has("invoice_no") ? sale.get("invoice_no").getAsString() : "-";
            String date = sale.has("created_at") ? sale.get("created_at").getAsString() : "-";
            
            // Format dates simply
            if (date.length() > 16) {
                date = date.substring(0, 16).replace("T", " ");
            }

            writeText("Inv  : " + invoiceNo + "\n");
            writeText("Tgl  : " + date + "\n");

            // Customer info
            if (sale.has("customer") && !sale.get("customer").isJsonNull()) {
                JsonObject customer = sale.getAsJsonObject("customer");
                String name = customer.get("customer_name").getAsString();
                writeText("Pel  : " + name + "\n");
            } else {
                writeText("Pel  : Pelanggan Umum (USER)\n");
            }

            writeText(PrinterFormatter.getDivider() + "\n");

            // 4. Items List
            if (sale.has("items")) {
                JsonArray items = sale.getAsJsonArray("items");
                for (int i = 0; i < items.size(); i++) {
                    JsonObject item = items.get(i).getAsJsonObject();
                    JsonObject product = item.getAsJsonObject("product");
                    
                    String name = product.get("product_name").getAsString();
                    int qty = item.get("qty").getAsInt();
                    double price = item.get("selling_price").getAsDouble();
                    double subtotal = qty * price;

                    // Row 1: Item Name
                    writeText(name + "\n");
                    
                    // Row 2: Qty x Price and Subtotal
                    String qtyPrice = qty + " x " + CurrencyHelper.formatRupiah(price);
                    String sub = CurrencyHelper.formatRupiah(subtotal);
                    writeText(PrinterFormatter.formatRow("  " + qtyPrice, sub) + "\n");
                }
            }

            writeText(PrinterFormatter.getDivider() + "\n");

            // 5. Totals
            double grandTotal = sale.has("grand_total") ? sale.get("grand_total").getAsDouble() : 0;
            double paidAmount = sale.has("paid_amount") ? sale.get("paid_amount").getAsDouble() : 0;
            double changeAmount = sale.has("change_amount") ? sale.get("change_amount").getAsDouble() : 0;

            writeText(PrinterFormatter.formatRow("TOTAL BILL", CurrencyHelper.formatRupiah(grandTotal)) + "\n");
            writeText(PrinterFormatter.formatRow("TUNAI/BAYAR", CurrencyHelper.formatRupiah(paidAmount)) + "\n");
            writeText(PrinterFormatter.formatRow("KEMBALIAN", CurrencyHelper.formatRupiah(changeAmount)) + "\n");

            writeText(PrinterFormatter.getDivider() + "\n");

            // 6. Footer (Centered)
            printerHelper.write(PrinterFormatter.ESC_ALIGN_CENTER);
            writeText("Terima Kasih Atas Kunjungan Anda\n");
            writeText("Barang yang sudah dibeli\n");
            writeText("tidak dapat ditukar/dikembalikan.\n\n");

            // Feed paper
            printerHelper.write(PrinterFormatter.ESC_FEED_LINE);
            printerHelper.write(PrinterFormatter.ESC_FEED_LINE);
            printerHelper.write(PrinterFormatter.ESC_FEED_LINE);

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void writeText(String text) {
        byte[] bytes = text.getBytes(StandardCharsets.US_ASCII);
        printerHelper.write(bytes);
    }

    public interface PrintCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    /**
     * Print receipt asynchronously on a background thread
     */
    public void printReceiptAsync(String shopName, JsonObject sale, String printerAddress, PrintCallback callback) {
        new Thread(() -> {
            if (printerAddress == null || printerAddress.isEmpty()) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> callback.onFailure("Printer belum diatur"));
                return;
            }
            BluetoothDevice device = printerHelper.getDeviceByAddress(printerAddress);
            if (device == null) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> callback.onFailure("Printer tidak ditemukan"));
                return;
            }
            
            boolean connected = printerHelper.connect(device);
            if (!connected) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> callback.onFailure("Gagal menghubungkan ke printer"));
                return;
            }
            
            boolean printed = printReceipt(shopName, sale);
            
            // Wait for buffer to flush before disconnect
            try { Thread.sleep(500); } catch (Exception ignored) {}
            printerHelper.disconnect();
            
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                if (printed) {
                    callback.onSuccess();
                } else {
                    callback.onFailure("Gagal mencetak struk");
                }
            });
        }).start();
    }

    /**
     * Print a test receipt asynchronously to verify connection
     */
    public void printTestReceiptAsync(String printerAddress, PrintCallback callback) {
        new Thread(() -> {
            if (printerAddress == null || printerAddress.isEmpty()) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> callback.onFailure("Printer belum diatur"));
                return;
            }
            BluetoothDevice device = printerHelper.getDeviceByAddress(printerAddress);
            if (device == null) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> callback.onFailure("Printer tidak ditemukan"));
                return;
            }
            
            boolean connected = printerHelper.connect(device);
            if (!connected) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> callback.onFailure("Gagal menghubungkan ke printer"));
                return;
            }
            
            boolean printed = false;
            try {
                printerHelper.write(PrinterFormatter.ESC_INIT);
                
                // Align Center, Double Size Bold Title
                printerHelper.write(PrinterFormatter.ESC_ALIGN_CENTER);
                printerHelper.write(PrinterFormatter.ESC_DOUBLE_SIZE_ON);
                printerHelper.write(PrinterFormatter.ESC_BOLD_ON);
                writeText("TEST PRINTER\n");
                printerHelper.write(PrinterFormatter.ESC_DOUBLE_SIZE_OFF);
                printerHelper.write(PrinterFormatter.ESC_BOLD_OFF);
                
                writeText("TANYA KASIR Mobile\n");
                printerHelper.write(PrinterFormatter.ESC_ALIGN_LEFT);
                writeText(PrinterFormatter.getDivider() + "\n");
                
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault());
                String dateStr = sdf.format(new java.util.Date());
                
                writeText("Status    : BERHASIL KONEK\n");
                writeText("Tgl/Jam   : " + dateStr + "\n");
                writeText("Fitur     : ESC/POS Thermal 58mm\n");
                writeText(PrinterFormatter.getDivider() + "\n");
                
                printerHelper.write(PrinterFormatter.ESC_ALIGN_CENTER);
                writeText("Printer thermal Anda siap!\nAplikasi ini dikembangkan\nbersama @Firlli.an\n\n");
                
                printerHelper.write(PrinterFormatter.ESC_FEED_LINE);
                printerHelper.write(PrinterFormatter.ESC_FEED_LINE);
                printerHelper.write(PrinterFormatter.ESC_FEED_LINE);
                printed = true;
            } catch (Exception e) {
                printed = false;
            }
            
            // Wait for buffer to flush
            try { Thread.sleep(500); } catch (Exception ignored) {}
            printerHelper.disconnect();
            
            boolean finalPrinted = printed;
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                if (finalPrinted) {
                    callback.onSuccess();
                } else {
                    callback.onFailure("Gagal mencetak struk uji coba");
                }
            });
        }).start();
    }
}
