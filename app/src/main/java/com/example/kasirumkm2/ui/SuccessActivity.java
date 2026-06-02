package com.example.kasirumkm2.ui;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.kasirumkm2.databinding.ActivitySuccessBinding;
import com.example.kasirumkm2.utils.CurrencyHelper;

public class SuccessActivity extends AppCompatActivity {

    private ActivitySuccessBinding binding;
    private int saleId = -1;
    private String invoiceNo = "";
    private double grandTotal = 0;
    private double paidAmount = 0;
    private double changeAmount = 0;
    private String customerName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySuccessBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Get Intent extras
        saleId = getIntent().getIntExtra("sale_id", -1);
        invoiceNo = getIntent().getStringExtra("invoice_no");
        grandTotal = getIntent().getDoubleExtra("grand_total", 0);
        paidAmount = getIntent().getDoubleExtra("paid_amount", 0);
        changeAmount = getIntent().getDoubleExtra("change_amount", 0);
        customerName = getIntent().getStringExtra("customer_name");

        displayDetails();
        setupListeners();
    }

    private void displayDetails() {
        binding.tvInvoiceNo.setText(invoiceNo);
        binding.tvCustomerName.setText(customerName == null || customerName.isEmpty() ? "Pelanggan Umum (USER)" : customerName);
        binding.tvTotalBill.setText(CurrencyHelper.formatRupiah(grandTotal));
        binding.tvPaidAmount.setText(CurrencyHelper.formatRupiah(paidAmount));
        binding.tvChangeAmount.setText(CurrencyHelper.formatRupiah(changeAmount));
    }

    private void setupListeners() {
        binding.btnPrintReceipt.setOnClickListener(v -> printReceipt());
        binding.btnNewTransaction.setOnClickListener(v -> finish());
    }

    private void printReceipt() {
        com.example.kasirumkm2.session.SessionManager sessionManager = new com.example.kasirumkm2.session.SessionManager(this);
        String printerAddress = sessionManager.getPrinterAddress();
        
        if (printerAddress == null || printerAddress.isEmpty()) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Printer Belum Diatur")
                    .setMessage("Silakan atur koneksi printer Bluetooth terlebih dahulu di menu Pengaturan.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }
        
        // Custom progress loader
        android.widget.ProgressBar progressBar = new android.widget.ProgressBar(this);
        progressBar.setPadding(32, 32, 32, 32);
        androidx.appcompat.app.AlertDialog loadingDialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(progressBar)
                .setMessage("Menghubungkan ke printer & mencetak...")
                .setCancelable(false)
                .create();
        loadingDialog.show();
        
        com.example.kasirumkm2.api.ApiService apiService = com.example.kasirumkm2.api.ApiClient.getApiService(this);
        apiService.getSaleDetail(saleId).enqueue(new retrofit2.Callback<com.google.gson.JsonObject>() {
            @Override
            public void onResponse(retrofit2.Call<com.google.gson.JsonObject> call, retrofit2.Response<com.google.gson.JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    com.google.gson.JsonObject body = response.body();
                    com.google.gson.JsonObject saleObj = body.getAsJsonObject("data");
                    
                    com.example.kasirumkm2.printer.PrinterManager printerManager = new com.example.kasirumkm2.printer.PrinterManager(SuccessActivity.this);
                    printerManager.printReceiptAsync("KASIR UMKM", saleObj, printerAddress, new com.example.kasirumkm2.printer.PrinterManager.PrintCallback() {
                        @Override
                        public void onSuccess() {
                            if (!isFinishing()) {
                                loadingDialog.dismiss();
                                Toast.makeText(SuccessActivity.this, "Struk berhasil dicetak!", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            if (!isFinishing()) {
                                loadingDialog.dismiss();
                                new androidx.appcompat.app.AlertDialog.Builder(SuccessActivity.this)
                                        .setTitle("Gagal Cetak")
                                        .setMessage(errorMessage + "\n\nPastikan printer menyala, kertas terisi, dan Bluetooth HP aktif.")
                                        .setPositiveButton("OK", null)
                                        .show();
                            }
                        }
                    });
                } else {
                    loadingDialog.dismiss();
                    Toast.makeText(SuccessActivity.this, "Gagal mengambil data struk", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<com.google.gson.JsonObject> call, Throwable t) {
                loadingDialog.dismiss();
                Toast.makeText(SuccessActivity.this, "Koneksi gagal: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
