package com.example.kasirumkm2.printer;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

public class PrinterFormatter {

    public static final int LINE_CHARS_58MM = 32;

    // ESC/POS Commands
    public static final byte[] ESC_ALIGN_LEFT = new byte[]{0x1b, 0x61, 0x00};
    public static final byte[] ESC_ALIGN_CENTER = new byte[]{0x1b, 0x61, 0x01};
    public static final byte[] ESC_ALIGN_RIGHT = new byte[]{0x1b, 0x61, 0x02};

    public static final byte[] ESC_BOLD_ON = new byte[]{0x1b, 0x45, 0x01};
    public static final byte[] ESC_BOLD_OFF = new byte[]{0x1b, 0x45, 0x00};

    public static final byte[] ESC_DOUBLE_SIZE_ON = new byte[]{0x1d, 0x21, 0x11};
    public static final byte[] ESC_DOUBLE_SIZE_OFF = new byte[]{0x1d, 0x21, 0x00};

    public static final byte[] ESC_FEED_LINE = new byte[]{0x0a};
    public static final byte[] ESC_INIT = new byte[]{0x1b, 0x40};

    /**
     * Format a line with left text and right text aligned to margins
     */
    public static String formatRow(String left, String right) {
        return formatRow(left, right, LINE_CHARS_58MM);
    }

    public static String formatRow(String left, String right, int maxChars) {
        if (left == null) left = "";
        if (right == null) right = "";

        int totalLen = left.length() + right.length();
        if (totalLen >= maxChars) {
            // Truncate left string to fit right string
            int maxLeft = maxChars - right.length() - 3;
            if (maxLeft > 0) {
                left = left.substring(0, maxLeft) + "...";
            } else {
                left = left.substring(0, Math.max(0, maxChars - right.length()));
            }
        }

        StringBuilder sb = new StringBuilder(left);
        int spaces = maxChars - left.length() - right.length();
        for (int i = 0; i < spaces; i++) {
            sb.append(" ");
        }
        sb.append(right);
        return sb.toString();
    }

    /**
     * Get a divider dashed line (e.g., "--------------------------------")
     */
    public static String getDivider() {
        return getDivider(LINE_CHARS_58MM);
    }

    public static String getDivider(int maxChars) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < maxChars; i++) {
            sb.append("-");
        }
        return sb.toString();
    }

    /**
     * Build plain text formatted receipt for sharing.
     */
    public static String buildReceiptShareText(JsonObject sale) {
        StringBuilder sb = new StringBuilder();
        String companyName = "KASIR UMKM";
        String companyAddress = "";
        String companyPhone = "";

        if (sale.has("company") && !sale.get("company").isJsonNull()) {
            JsonObject company = sale.getAsJsonObject("company");
            if (company.has("company_name") && !company.get("company_name").isJsonNull()) {
                companyName = company.get("company_name").getAsString();
            }
            if (company.has("address") && !company.get("address").isJsonNull()) {
                companyAddress = company.get("address").getAsString();
            }
            if (company.has("phone") && !company.get("phone").isJsonNull()) {
                companyPhone = company.get("phone").getAsString();
            }
        }

        sb.append("===========================\n");
        sb.append("       ").append(companyName).append("\n");
        if (!companyAddress.isEmpty()) {
            sb.append("   ").append(companyAddress).append("\n");
        }
        if (!companyPhone.isEmpty()) {
            sb.append("   Telp: ").append(companyPhone).append("\n");
        }
        sb.append("===========================\n");

        String invoiceNo = sale.has("invoice_no") ? sale.get("invoice_no").getAsString() : "-";
        String date = sale.has("created_at") ? sale.get("created_at").getAsString() : "-";
        if (date.length() > 16) {
            date = date.substring(0, 16).replace("T", " ");
        }

        sb.append("Invoice: ").append(invoiceNo).append("\n");
        sb.append("Tanggal: ").append(date).append("\n");

        String cashierName = "";
        if (sale.has("user") && !sale.get("user").isJsonNull()) {
            JsonObject userObj = sale.getAsJsonObject("user");
            if (userObj.has("name") && !userObj.get("name").isJsonNull()) {
                cashierName = userObj.get("name").getAsString();
            }
        } else if (sale.has("created_by_user") && !sale.get("created_by_user").isJsonNull()) {
            JsonObject userObj = sale.getAsJsonObject("created_by_user");
            if (userObj.has("name") && !userObj.get("name").isJsonNull()) {
                cashierName = userObj.get("name").getAsString();
            }
        } else if (sale.has("cashier") && !sale.get("cashier").isJsonNull()) {
            com.google.gson.JsonElement cashierEl = sale.get("cashier");
            if (cashierEl.isJsonObject()) {
                JsonObject cashierObj = cashierEl.getAsJsonObject();
                if (cashierObj.has("name") && !cashierObj.get("name").isJsonNull()) {
                    cashierName = cashierObj.get("name").getAsString();
                }
            } else {
                cashierName = cashierEl.getAsString();
            }
        }
        if (!cashierName.isEmpty()) {
            sb.append("Kasir  : ").append(cashierName).append("\n");
        }

        if (sale.has("customer") && !sale.get("customer").isJsonNull()) {
            JsonObject customer = sale.getAsJsonObject("customer");
            String name = customer.get("customer_name").getAsString();
            sb.append("Pel    : ").append(name).append("\n");
        } else {
            sb.append("Pel    : Pelanggan Umum\n");
        }
        sb.append("---------------------------\n");

        if (sale.has("items")) {
            JsonArray items = sale.getAsJsonArray("items");
            for (int i = 0; i < items.size(); i++) {
                JsonObject item = items.get(i).getAsJsonObject();
                JsonObject product = item.getAsJsonObject("product");
                
                String name = product.get("product_name").getAsString();
                int qty = item.get("qty").getAsInt();
                double price = item.get("selling_price").getAsDouble();
                double subtotal = qty * price;

                sb.append(name).append("\n");
                sb.append("  ").append(qty).append(" x ").append(com.example.kasirumkm2.utils.CurrencyHelper.formatRupiah(price))
                  .append(" = ").append(com.example.kasirumkm2.utils.CurrencyHelper.formatRupiah(subtotal)).append("\n");
            }
        }
        sb.append("---------------------------\n");

        double grandTotal = sale.has("grand_total") ? sale.get("grand_total").getAsDouble() : 0;
        double paidAmount = sale.has("paid_amount") ? sale.get("paid_amount").getAsDouble() : 0;
        double changeAmount = sale.has("change_amount") ? sale.get("change_amount").getAsDouble() : 0;

        sb.append("TOTAL BILL: ").append(com.example.kasirumkm2.utils.CurrencyHelper.formatRupiah(grandTotal)).append("\n");
        sb.append("BAYAR     : ").append(com.example.kasirumkm2.utils.CurrencyHelper.formatRupiah(paidAmount)).append("\n");
        sb.append("KEMBALIAN : ").append(com.example.kasirumkm2.utils.CurrencyHelper.formatRupiah(changeAmount)).append("\n");
        sb.append("===========================\n");
        sb.append("Terima Kasih Atas Kunjungan Anda\n");
        sb.append("Aplikasi Kasir UMKM Pintar\n");

        return sb.toString();
    }
}
