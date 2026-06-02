package com.example.kasirumkm2.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.kasirumkm2.R;
import com.example.kasirumkm2.adapter.SalesAdapter;
import com.example.kasirumkm2.api.ApiClient;
import com.example.kasirumkm2.api.ApiService;
import com.example.kasirumkm2.databinding.ActivitySalesHistoryBinding;
import com.example.kasirumkm2.session.SessionManager;
import com.example.kasirumkm2.utils.CurrencyHelper;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SalesHistoryActivity extends AppCompatActivity {

    private ActivitySalesHistoryBinding binding;
    private ApiService apiService;
    private SalesAdapter adapter;
    private List<JsonObject> allSales = new ArrayList<>();
    private String startDate = "";
    private String endDate = "";
    private final SimpleDateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat displayDateFormat = new SimpleDateFormat("dd MMM yyyy", new Locale("id", "ID"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySalesHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = ApiClient.getApiService(this);

        setupToolbar();
        setupRecyclerView();
        setupFilters();
        setupSearch();

        // Default to Today
        selectTodayFilter();
    }

    private void setupToolbar() {
        binding.btnBack.setOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        adapter = new SalesAdapter(this::showSaleDetailsBottomSheet);
        binding.rvSales.setLayoutManager(new LinearLayoutManager(this));
        binding.rvSales.setAdapter(adapter);

        binding.swipeRefresh.setColorSchemeColors(getColor(R.color.primary));
        binding.swipeRefresh.setOnRefreshListener(this::loadSalesHistory);
    }

    private void setupFilters() {
        binding.chipToday.setOnClickListener(v -> selectTodayFilter());
        binding.chipWeekly.setOnClickListener(v -> selectWeeklyFilter());
        binding.chipMonthly.setOnClickListener(v -> selectMonthlyFilter());
        binding.chipCustom.setOnClickListener(v -> showCustomDatePicker());
    }

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterSalesLocal(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void selectTodayFilter() {
        String today = CurrencyHelper.getTodayDate();
        startDate = today;
        endDate = today;
        updateDateRangeLabel();
        loadSalesHistory();
    }

    private void selectWeeklyFilter() {
        Calendar cal = Calendar.getInstance();
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        startDate = apiDateFormat.format(cal.getTime());
        
        cal.add(Calendar.DAY_OF_WEEK, 6);
        endDate = apiDateFormat.format(cal.getTime());
        
        updateDateRangeLabel();
        loadSalesHistory();
    }

    private void selectMonthlyFilter() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        startDate = apiDateFormat.format(cal.getTime());
        
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        endDate = apiDateFormat.format(cal.getTime());
        
        updateDateRangeLabel();
        loadSalesHistory();
    }

    private void showCustomDatePicker() {
        MaterialDatePicker<Pair<Long, Long>> dateRangePicker =
                MaterialDatePicker.Builder.dateRangePicker()
                        .setTitleText("Pilih Rentang Tanggal")
                        .build();

        dateRangePicker.addOnPositiveButtonClickListener(selection -> {
            if (selection != null && selection.first != null && selection.second != null) {
                startDate = apiDateFormat.format(new Date(selection.first));
                endDate = apiDateFormat.format(new Date(selection.second));
                updateDateRangeLabel();
                loadSalesHistory();
            }
        });

        dateRangePicker.addOnCancelListener(dialog -> {
            // Rollback to today chip selection if user cancelled custom picker
            binding.chipToday.setChecked(true);
            selectTodayFilter();
        });

        dateRangePicker.show(getSupportFragmentManager(), "CUSTOM_DATE_PICKER");
    }

    private void updateDateRangeLabel() {
        try {
            Date start = apiDateFormat.parse(startDate);
            Date end = apiDateFormat.parse(endDate);
            if (start != null && end != null) {
                binding.tvDateRange.setText("Rentang: " + displayDateFormat.format(start) + " - " + displayDateFormat.format(end));
            }
        } catch (Exception e) {
            binding.tvDateRange.setText("Rentang: " + startDate + " - " + endDate);
        }
    }

    private void loadSalesHistory() {
        setLoading(true);
        Map<String, String> filters = new HashMap<>();
        filters.put("start_date", startDate);
        filters.put("end_date", endDate);

        apiService.getSalesFiltered(filters).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                setLoading(false);
                binding.swipeRefresh.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonObject body = response.body();
                        JsonArray dataArray = body.getAsJsonArray("data");
                        allSales.clear();

                        for (int i = 0; i < dataArray.size(); i++) {
                            allSales.add(dataArray.get(i).getAsJsonObject());
                        }

                        // Apply search filter if there's text
                        filterSalesLocal(binding.etSearch.getText().toString());
                    } catch (Exception e) {
                        showEmptyState(true);
                    }
                } else if (response.code() == 401) {
                    handleUnauthorized();
                } else {
                    showEmptyState(true);
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                setLoading(false);
                binding.swipeRefresh.setRefreshing(false);
                Toast.makeText(SalesHistoryActivity.this, "Gagal memuat data transaksi", Toast.LENGTH_SHORT).show();
                showEmptyState(true);
            }
        });
    }

    private void filterSalesLocal(String query) {
        if (query.trim().isEmpty()) {
            adapter.setData(allSales);
            showEmptyState(allSales.isEmpty());
            return;
        }

        List<JsonObject> filtered = new ArrayList<>();
        String lowerQuery = query.toLowerCase(Locale.getDefault());

        for (JsonObject sale : allSales) {
            String invoice = sale.has("invoice_no") ? sale.get("invoice_no").getAsString().toLowerCase(Locale.getDefault()) : "";
            
            String customerName = "";
            if (sale.has("customer") && !sale.get("customer").isJsonNull()) {
                customerName = sale.getAsJsonObject("customer").get("customer_name").getAsString().toLowerCase(Locale.getDefault());
            }

            if (invoice.contains(lowerQuery) || customerName.contains(lowerQuery)) {
                filtered.add(sale);
            }
        }

        adapter.setData(filtered);
        showEmptyState(filtered.isEmpty());
    }

    private void showSaleDetailsBottomSheet(JsonObject saleHeader) {
        int saleId = saleHeader.has("id") ? saleHeader.get("id").getAsInt() : -1;
        if (saleId == -1) return;

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.layout_sale_detail_sheet, null);
        dialog.setContentView(view);

        // Bind basic layout views
        TextView tvInvoice = view.findViewById(R.id.tvSheetInvoice);
        TextView tvStatus = view.findViewById(R.id.tvSheetStatus);
        TextView tvDate = view.findViewById(R.id.tvSheetDate);
        TextView tvCustomer = view.findViewById(R.id.tvSheetCustomer);
        TextView tvPayment = view.findViewById(R.id.tvSheetPayment);
        TextView tvSubtotal = view.findViewById(R.id.tvSheetSubtotal);
        TextView tvDiscount = view.findViewById(R.id.tvSheetDiscount);
        TextView tvGrandTotal = view.findViewById(R.id.tvSheetGrandTotal);
        TextView tvPaid = view.findViewById(R.id.tvSheetPaid);
        TextView tvChange = view.findViewById(R.id.tvSheetChange);
        LinearLayout layoutItems = view.findViewById(R.id.layoutSheetItems);
        View layoutCashDetails = view.findViewById(R.id.layoutSheetCashDetails);
        View btnReprint = view.findViewById(R.id.btnSheetReprint);

        // Set basic header details immediately from list item
        String invoiceNo = saleHeader.has("invoice_no") ? saleHeader.get("invoice_no").getAsString() : "-";
        tvInvoice.setText(invoiceNo);

        String status = saleHeader.has("status") ? saleHeader.get("status").getAsString() : "00";
        if ("98".equals(status) || "01".equals(status)) {
            tvStatus.setText("VOIDED");
            tvStatus.setBackgroundResource(R.drawable.bg_badge_danger);
        } else {
            tvStatus.setText("SUKSES");
            tvStatus.setBackgroundResource(R.drawable.bg_badge_success);
        }

        String rawDate = saleHeader.has("created_at") ? saleHeader.get("created_at").getAsString() : "-";
        tvDate.setText("Tanggal: " + CurrencyHelper.formatDateTime(rawDate));

        // Customer details
        if (saleHeader.has("customer") && !saleHeader.get("customer").isJsonNull()) {
            JsonObject cust = saleHeader.getAsJsonObject("customer");
            tvCustomer.setText("Pelanggan: " + cust.get("customer_name").getAsString());
        } else {
            tvCustomer.setText("Pelanggan: Pelanggan Umum (USER)");
        }

        // Payment method
        if (saleHeader.has("payment_method") && !saleHeader.get("payment_method").isJsonNull()) {
            JsonObject pm = saleHeader.getAsJsonObject("payment_method");
            tvPayment.setText("Pembayaran: " + pm.get("method_name").getAsString());
        } else {
            tvPayment.setText("Pembayaran: -");
        }

        // Totals
        double grandTotal = saleHeader.has("grand_total") ? saleHeader.get("grand_total").getAsDouble() : 0;
        double discount = saleHeader.has("discount") ? saleHeader.get("discount").getAsDouble() : 0;
        double subtotal = saleHeader.has("subtotal") ? saleHeader.get("subtotal").getAsDouble() : (grandTotal + discount);
        double paidAmount = saleHeader.has("paid_amount") ? saleHeader.get("paid_amount").getAsDouble() : 0;
        double changeAmount = saleHeader.has("change_amount") ? saleHeader.get("change_amount").getAsDouble() : 0;

        tvSubtotal.setText(CurrencyHelper.formatRupiah(subtotal));
        tvDiscount.setText(CurrencyHelper.formatRupiah(discount));
        tvGrandTotal.setText(CurrencyHelper.formatRupiah(grandTotal));

        // Check if payment method is cash to toggle details
        boolean isCash = false;
        if (saleHeader.has("payment_method") && !saleHeader.get("payment_method").isJsonNull()) {
            String code = saleHeader.getAsJsonObject("payment_method").get("method_code").getAsString();
            isCash = "CASH".equalsIgnoreCase(code);
        }
        layoutCashDetails.setVisibility(isCash ? View.VISIBLE : View.GONE);
        tvPaid.setText(CurrencyHelper.formatRupiah(paidAmount));
        tvChange.setText(CurrencyHelper.formatRupiah(changeAmount));

        // Preloaded sale detail container for printing
        final JsonObject[] fullSaleData = new JsonObject[1];

        // Fetch detailed items from API
        apiService.getSaleDetail(saleId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonObject dataObj = response.body().getAsJsonObject("data");
                        fullSaleData[0] = dataObj;
                        if (dataObj.has("items")) {
                            JsonArray itemsArray = dataObj.getAsJsonArray("items");
                            layoutItems.removeAllViews();

                            for (int i = 0; i < itemsArray.size(); i++) {
                                JsonObject itemObj = itemsArray.get(i).getAsJsonObject();
                                JsonObject prodObj = itemObj.getAsJsonObject("product");

                                View itemView = LayoutInflater.from(SalesHistoryActivity.this)
                                        .inflate(R.layout.item_checkout_summary, layoutItems, false);
                                TextView tvProdName = itemView.findViewById(R.id.tvProductName);
                                TextView tvQtyPrice = itemView.findViewById(R.id.tvQtyPrice);
                                TextView tvItemSub = itemView.findViewById(R.id.tvSubtotal);

                                String name = prodObj.has("product_name") ? prodObj.get("product_name").getAsString() : "-";
                                int qty = itemObj.has("qty") ? itemObj.get("qty").getAsInt() : 0;
                                double price = itemObj.has("selling_price") ? itemObj.get("selling_price").getAsDouble() : 0;
                                double itemSub = itemObj.has("subtotal") ? itemObj.get("subtotal").getAsDouble() : (qty * price);

                                tvProdName.setText(name);
                                tvQtyPrice.setText(qty + " x " + CurrencyHelper.formatRupiah(price));
                                tvItemSub.setText(CurrencyHelper.formatRupiah(itemSub));

                                layoutItems.addView(itemView);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                // Keep showing empty items or header only
            }
        });

        // Reprint Struk action listener
        btnReprint.setOnClickListener(v -> {
            if (fullSaleData[0] != null) {
                printReceipt(fullSaleData[0]);
            } else {
                fetchAndPrintReceipt(saleId);
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void showEmptyState(boolean show) {
        binding.layoutEmptySales.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.rvSales.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void printReceipt(JsonObject saleObj) {
        SessionManager sessionManager = new SessionManager(this);
        String printerAddress = sessionManager.getPrinterAddress();
        
        if (printerAddress == null || printerAddress.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("Printer Belum Diatur")
                    .setMessage("Silakan atur koneksi printer Bluetooth terlebih dahulu di menu Pengaturan.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }
        
        // Progress loader
        android.widget.ProgressBar progressBar = new android.widget.ProgressBar(this);
        progressBar.setPadding(32, 32, 32, 32);
        AlertDialog loadingDialog = new AlertDialog.Builder(this)
                .setView(progressBar)
                .setMessage("Menghubungkan ke printer & mencetak...")
                .setCancelable(false)
                .create();
        loadingDialog.show();
        
        com.example.kasirumkm2.printer.PrinterManager printerManager = new com.example.kasirumkm2.printer.PrinterManager(this);
        printerManager.printReceiptAsync("KASIR UMKM", saleObj, printerAddress, new com.example.kasirumkm2.printer.PrinterManager.PrintCallback() {
            @Override
            public void onSuccess() {
                if (!isFinishing()) {
                    loadingDialog.dismiss();
                    Toast.makeText(SalesHistoryActivity.this, "Struk berhasil dicetak ulang!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                if (!isFinishing()) {
                    loadingDialog.dismiss();
                    new AlertDialog.Builder(SalesHistoryActivity.this)
                            .setTitle("Gagal Cetak")
                            .setMessage(errorMessage + "\n\nPastikan printer menyala, kertas terisi, dan Bluetooth HP aktif.")
                            .setPositiveButton("OK", null)
                            .show();
                }
            }
        });
    }

    private void fetchAndPrintReceipt(int saleId) {
        SessionManager sessionManager = new SessionManager(this);
        String printerAddress = sessionManager.getPrinterAddress();
        
        if (printerAddress == null || printerAddress.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("Printer Belum Diatur")
                    .setMessage("Silakan atur koneksi printer Bluetooth terlebih dahulu di menu Pengaturan.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }
        
        android.widget.ProgressBar progressBar = new android.widget.ProgressBar(this);
        progressBar.setPadding(32, 32, 32, 32);
        AlertDialog loadingDialog = new AlertDialog.Builder(this)
                .setView(progressBar)
                .setMessage("Memuat detail transaksi & mencetak...")
                .setCancelable(false)
                .create();
        loadingDialog.show();
        
        apiService.getSaleDetail(saleId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject saleObj = response.body().getAsJsonObject("data");
                    
                    com.example.kasirumkm2.printer.PrinterManager printerManager = new com.example.kasirumkm2.printer.PrinterManager(SalesHistoryActivity.this);
                    printerManager.printReceiptAsync("KASIR UMKM", saleObj, printerAddress, new com.example.kasirumkm2.printer.PrinterManager.PrintCallback() {
                        @Override
                        public void onSuccess() {
                            if (!isFinishing()) {
                                loadingDialog.dismiss();
                                Toast.makeText(SalesHistoryActivity.this, "Struk berhasil dicetak ulang!", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            if (!isFinishing()) {
                                loadingDialog.dismiss();
                                new AlertDialog.Builder(SalesHistoryActivity.this)
                                        .setTitle("Gagal Cetak")
                                        .setMessage(errorMessage + "\n\nPastikan printer menyala, kertas terisi, dan Bluetooth HP aktif.")
                                        .setPositiveButton("OK", null)
                                        .show();
                            }
                        }
                    });
                } else {
                    loadingDialog.dismiss();
                    Toast.makeText(SalesHistoryActivity.this, "Gagal mengambil detail transaksi", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                loadingDialog.dismiss();
                Toast.makeText(SalesHistoryActivity.this, "Koneksi gagal: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleUnauthorized() {
        new SessionManager(this).clearSession();
        ApiClient.resetClient();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
