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
import com.example.kasirumkm2.data.LoginRequest;
import com.example.kasirumkm2.databinding.ActivitySalesHistoryBinding;
import com.example.kasirumkm2.session.SessionManager;
import com.example.kasirumkm2.utils.CurrencyHelper;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
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

    // Pagination states
    private int currentPage = 1;
    private int lastPage = 1;
    private boolean isLoading = false;
    private String searchQuery = "";
    private final android.os.Handler searchHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable searchRunnable;

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
        binding.swipeRefresh.setOnRefreshListener(() -> {
            currentPage = 1;
            loadSalesHistory(true);
        });

        binding.rvSales.addOnScrollListener(new androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(androidx.recyclerview.widget.RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    if (!isLoading && currentPage < lastPage) {
                        if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 3
                                && firstVisibleItemPosition >= 0) {
                            currentPage++;
                            loadSalesHistory(false);
                        }
                    }
                }
            }
        });
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
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }
                searchRunnable = () -> {
                    searchQuery = s.toString().trim();
                    currentPage = 1;
                    loadSalesHistory(true);
                };
                searchHandler.postDelayed(searchRunnable, 400); // 400ms debounce
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
        currentPage = 1;
        loadSalesHistory(true);
    }

    private void selectWeeklyFilter() {
        Calendar cal = Calendar.getInstance();
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        startDate = apiDateFormat.format(cal.getTime());
        
        cal.add(Calendar.DAY_OF_WEEK, 6);
        endDate = apiDateFormat.format(cal.getTime());
        
        updateDateRangeLabel();
        currentPage = 1;
        loadSalesHistory(true);
    }

    private void selectMonthlyFilter() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        startDate = apiDateFormat.format(cal.getTime());
        
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        endDate = apiDateFormat.format(cal.getTime());
        
        updateDateRangeLabel();
        currentPage = 1;
        loadSalesHistory(true);
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
                currentPage = 1;
                loadSalesHistory(true);
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

    private void loadSalesHistory(boolean isRefreshOrSearch) {
        if (isLoading) return;
        isLoading = true;

        if (isRefreshOrSearch) {
            setLoading(true);
            binding.progressBarLoadMore.setVisibility(View.GONE);
        } else {
            binding.progressBarLoadMore.setVisibility(View.VISIBLE);
        }

        Map<String, String> filters = new HashMap<>();
        filters.put("start_date", startDate);
        filters.put("end_date", endDate);
        filters.put("page", String.valueOf(currentPage));
        filters.put("per_page", "15");
        if (searchQuery != null && !searchQuery.isEmpty()) {
            filters.put("search", searchQuery);
        }

        apiService.getSalesFiltered(filters).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                isLoading = false;
                setLoading(false);
                binding.progressBarLoadMore.setVisibility(View.GONE);
                binding.swipeRefresh.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonObject body = response.body();

                        // Try to parse pagination meta info from the response root
                        if (body.has("meta") && !body.get("meta").isJsonNull()) {
                            JsonObject meta = body.getAsJsonObject("meta");
                            if (meta.has("current_page")) {
                                currentPage = meta.get("current_page").getAsInt();
                            }
                            if (meta.has("last_page")) {
                                lastPage = meta.get("last_page").getAsInt();
                            }
                        } else if (body.has("data") && body.get("data").isJsonObject()) {
                            JsonObject dataObj = body.getAsJsonObject("data");
                            if (dataObj.has("current_page")) {
                                currentPage = dataObj.get("current_page").getAsInt();
                            }
                            if (dataObj.has("last_page")) {
                                lastPage = dataObj.get("last_page").getAsInt();
                            }
                        }

                        // Extract data array
                        JsonArray dataArray = new JsonArray();
                        if (body.has("data")) {
                            if (body.get("data").isJsonArray()) {
                                dataArray = body.getAsJsonArray("data");
                            } else if (body.get("data").isJsonObject()) {
                                JsonObject paginatedData = body.getAsJsonObject("data");
                                if (paginatedData.has("data") && paginatedData.get("data").isJsonArray()) {
                                    dataArray = paginatedData.getAsJsonArray("data");
                                }
                            }
                        }

                        List<JsonObject> sales = new ArrayList<>();
                        for (int i = 0; i < dataArray.size(); i++) {
                            sales.add(dataArray.get(i).getAsJsonObject());
                        }

                        if (isRefreshOrSearch) {
                            allSales.clear();
                        }
                        allSales.addAll(sales);

                        if (isRefreshOrSearch) {
                            adapter.setData(allSales);
                        } else {
                            adapter.addData(sales);
                        }

                        showEmptyState(adapter.getItemCount() == 0);
                    } catch (Exception e) {
                        showEmptyState(adapter.getItemCount() == 0);
                    }
                } else if (response.code() == 401) {
                    handleUnauthorized();
                } else {
                    showEmptyState(adapter.getItemCount() == 0);
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                isLoading = false;
                setLoading(false);
                binding.progressBarLoadMore.setVisibility(View.GONE);
                binding.swipeRefresh.setRefreshing(false);
                Toast.makeText(SalesHistoryActivity.this, "Gagal memuat data transaksi", Toast.LENGTH_SHORT).show();
                showEmptyState(adapter.getItemCount() == 0);
            }
        });
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
        TextView tvCashier = view.findViewById(R.id.tvSheetCashier);
        LinearLayout layoutItems = view.findViewById(R.id.layoutSheetItems);
        View layoutCashDetails = view.findViewById(R.id.layoutSheetCashDetails);
        View btnReprint = view.findViewById(R.id.btnSheetReprint);
        View btnShare = view.findViewById(R.id.btnSheetShare);
        MaterialButton btnVoid = view.findViewById(R.id.btnSheetVoid);
        LinearLayout layoutVoidInfo = view.findViewById(R.id.layoutVoidInfo);
        TextView tvVoidReason = view.findViewById(R.id.tvVoidReason);

        // Set basic header details immediately from list item
        String invoiceNo = saleHeader.has("invoice_no") ? saleHeader.get("invoice_no").getAsString() : "-";
        tvInvoice.setText(invoiceNo);

        String status = saleHeader.has("status") ? saleHeader.get("status").getAsString() : "00";
        if ("98".equals(status) || "01".equals(status)) {
            tvStatus.setText("VOIDED");
            tvStatus.setBackgroundResource(R.drawable.bg_badge_danger);

            // Show void info and hide void button
            btnVoid.setVisibility(View.GONE);
            layoutVoidInfo.setVisibility(View.VISIBLE);

            // Try to get void reason from header data
            if (saleHeader.has("void_reason") && !saleHeader.get("void_reason").isJsonNull()) {
                tvVoidReason.setText("Alasan: " + saleHeader.get("void_reason").getAsString());
            }
        } else if ("00".equals(status)) {
            tvStatus.setText("SUKSES");
            tvStatus.setBackgroundResource(R.drawable.bg_badge_success);

            // Show void button only for successful transactions
            btnVoid.setVisibility(View.VISIBLE);
            layoutVoidInfo.setVisibility(View.GONE);
        } else {
            tvStatus.setText("SUKSES");
            tvStatus.setBackgroundResource(R.drawable.bg_badge_success);
            btnVoid.setVisibility(View.GONE);
            layoutVoidInfo.setVisibility(View.GONE);
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

        // Cashier/Creator info
        String cashierName = "";
        if (saleHeader.has("user") && !saleHeader.get("user").isJsonNull()) {
            JsonObject userObj = saleHeader.getAsJsonObject("user");
            if (userObj.has("name") && !userObj.get("name").isJsonNull()) {
                cashierName = userObj.get("name").getAsString();
            }
        } else if (saleHeader.has("created_by_user") && !saleHeader.get("created_by_user").isJsonNull()) {
            JsonObject userObj = saleHeader.getAsJsonObject("created_by_user");
            if (userObj.has("name") && !userObj.get("name").isJsonNull()) {
                cashierName = userObj.get("name").getAsString();
            }
        } else if (saleHeader.has("cashier") && !saleHeader.get("cashier").isJsonNull()) {
            com.google.gson.JsonElement cashierEl = saleHeader.get("cashier");
            if (cashierEl.isJsonObject()) {
                JsonObject cashierObj = cashierEl.getAsJsonObject();
                if (cashierObj.has("name") && !cashierObj.get("name").isJsonNull()) {
                    cashierName = cashierObj.get("name").getAsString();
                }
            } else {
                cashierName = cashierEl.getAsString();
            }
        } else if (saleHeader.has("created_by") && !saleHeader.get("created_by").isJsonNull()) {
            com.google.gson.JsonElement cbEl = saleHeader.get("created_by");
            if (cbEl.isJsonObject()) {
                JsonObject cbObj = cbEl.getAsJsonObject();
                if (cbObj.has("name") && !cbObj.get("name").isJsonNull()) {
                    cashierName = cbObj.get("name").getAsString();
                }
            } else {
                cashierName = cbEl.getAsString();
            }
        }
        if (!cashierName.isEmpty()) {
            tvCashier.setText("Kasir: " + cashierName);
        } else {
            tvCashier.setText("Kasir: -");
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

                        // Update void reason from detailed data if available
                        if (dataObj.has("void_reason") && !dataObj.get("void_reason").isJsonNull()) {
                            tvVoidReason.setText("Alasan: " + dataObj.get("void_reason").getAsString());
                        }

                        // Update cashier info from detailed data if available
                        String detailedCashier = "";
                        if (dataObj.has("user") && !dataObj.get("user").isJsonNull()) {
                            JsonObject userObj = dataObj.getAsJsonObject("user");
                            if (userObj.has("name") && !userObj.get("name").isJsonNull()) {
                                detailedCashier = userObj.get("name").getAsString();
                            }
                        } else if (dataObj.has("created_by_user") && !dataObj.get("created_by_user").isJsonNull()) {
                            JsonObject userObj = dataObj.getAsJsonObject("created_by_user");
                            if (userObj.has("name") && !userObj.get("name").isJsonNull()) {
                                detailedCashier = userObj.get("name").getAsString();
                            }
                        } else if (dataObj.has("cashier") && !dataObj.get("cashier").isJsonNull()) {
                            com.google.gson.JsonElement cashierEl = dataObj.get("cashier");
                            if (cashierEl.isJsonObject()) {
                                JsonObject cashierObj = cashierEl.getAsJsonObject();
                                if (cashierObj.has("name") && !cashierObj.get("name").isJsonNull()) {
                                    detailedCashier = cashierObj.get("name").getAsString();
                                }
                            } else {
                                detailedCashier = cashierEl.getAsString();
                            }
                        } else if (dataObj.has("created_by") && !dataObj.get("created_by").isJsonNull()) {
                            com.google.gson.JsonElement cbEl = dataObj.get("created_by");
                            if (cbEl.isJsonObject()) {
                                JsonObject cbObj = cbEl.getAsJsonObject();
                                if (cbObj.has("name") && !cbObj.get("name").isJsonNull()) {
                                    detailedCashier = cbObj.get("name").getAsString();
                                }
                            } else {
                                detailedCashier = cbEl.getAsString();
                            }
                        }
                        if (!detailedCashier.isEmpty()) {
                            tvCashier.setText("Kasir: " + detailedCashier);
                        }

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

        // Share Struk action listener
        btnShare.setOnClickListener(v -> {
            if (fullSaleData[0] != null) {
                shareReceiptText(fullSaleData[0]);
            } else {
                fetchAndShareReceipt(saleId);
            }
            dialog.dismiss();
        });

        // Void Transaksi action listener — Step 1: Warning Dialog
        btnVoid.setOnClickListener(v -> {
            showVoidWarningDialog(saleId, invoiceNo, grandTotal, dialog);
        });

        dialog.show();
    }

    // ======================== VOID FLOW ========================

    /**
     * Step 1: Show warning dialog — "Are you sure you want to void this transaction?"
     */
    private void showVoidWarningDialog(int saleId, String invoiceNo, double grandTotal, BottomSheetDialog parentDialog) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.void_warning_title))
                .setMessage(getString(R.string.void_warning_message))
                .setPositiveButton("Ya, Lanjutkan", (d, w) -> {
                    d.dismiss();
                    showVoidReasonDialog(saleId, invoiceNo, grandTotal, parentDialog);
                })
                .setNegativeButton("Batal", (d, w) -> d.dismiss())
                .setCancelable(true)
                .show();
    }

    /**
     * Step 2: Show void reason input dialog — user must provide reason (min 10 chars)
     */
    private void showVoidReasonDialog(int saleId, String invoiceNo, double grandTotal, BottomSheetDialog parentDialog) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_void_reason, null);
        builder.setView(dialogView);
        builder.setCancelable(false);

        AlertDialog reasonDialog = builder.create();
        if (reasonDialog.getWindow() != null) {
            reasonDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Bind views
        TextView tvVoidInvoice = dialogView.findViewById(R.id.tvVoidInvoice);
        TextInputLayout tilVoidReason = dialogView.findViewById(R.id.tilVoidReason);
        TextInputEditText etVoidReason = dialogView.findViewById(R.id.etVoidReason);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnVoidReasonCancel);
        MaterialButton btnContinue = dialogView.findViewById(R.id.btnVoidReasonContinue);

        tvVoidInvoice.setText(invoiceNo);

        // Text watcher for validation — enable button only when >= 10 chars
        etVoidReason.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int length = s.toString().trim().length();
                btnContinue.setEnabled(length >= 10);

                if (length > 0 && length < 10) {
                    tilVoidReason.setError("Minimal 10 karakter (" + length + "/10)");
                } else {
                    tilVoidReason.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnCancel.setOnClickListener(v -> reasonDialog.dismiss());

        btnContinue.setOnClickListener(v -> {
            String reason = etVoidReason.getText() != null ? etVoidReason.getText().toString().trim() : "";
            if (reason.length() < 10) {
                tilVoidReason.setError(getString(R.string.void_reason_too_short));
                return;
            }
            reasonDialog.dismiss();
            showVoidPasswordDialog(saleId, invoiceNo, grandTotal, reason, parentDialog);
        });

        reasonDialog.show();
    }

    /**
     * Step 3: Show password verification dialog — user must re-enter login password
     */
    private void showVoidPasswordDialog(int saleId, String invoiceNo, double grandTotal, String voidReason, BottomSheetDialog parentDialog) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_void_password, null);
        builder.setView(dialogView);
        builder.setCancelable(false);

        AlertDialog passwordDialog = builder.create();
        if (passwordDialog.getWindow() != null) {
            passwordDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Bind views
        TextView tvConfirmInvoice = dialogView.findViewById(R.id.tvConfirmInvoice);
        TextView tvConfirmTotal = dialogView.findViewById(R.id.tvConfirmTotal);
        TextView tvConfirmReason = dialogView.findViewById(R.id.tvConfirmReason);
        TextInputLayout tilVoidPassword = dialogView.findViewById(R.id.tilVoidPassword);
        TextInputEditText etVoidPassword = dialogView.findViewById(R.id.etVoidPassword);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnVoidPasswordCancel);
        MaterialButton btnExecute = dialogView.findViewById(R.id.btnVoidExecute);

        // Populate summary
        tvConfirmInvoice.setText(invoiceNo);
        tvConfirmTotal.setText(CurrencyHelper.formatRupiah(grandTotal));
        tvConfirmReason.setText(voidReason);

        btnCancel.setOnClickListener(v -> passwordDialog.dismiss());

        btnExecute.setOnClickListener(v -> {
            String password = etVoidPassword.getText() != null ? etVoidPassword.getText().toString().trim() : "";
            if (password.isEmpty()) {
                tilVoidPassword.setError("Password tidak boleh kosong");
                return;
            }
            tilVoidPassword.setError(null);

            // Disable button to prevent double-tap
            btnExecute.setEnabled(false);
            btnExecute.setText("Memverifikasi...");

            // Verify password by attempting login with current email
            verifyPasswordAndVoid(saleId, voidReason, password, tilVoidPassword, btnExecute, passwordDialog, parentDialog);
        });

        passwordDialog.show();
    }

    /**
     * Verify password by calling login API, then execute void if successful
     */
    private void verifyPasswordAndVoid(int saleId, String voidReason, String password,
                                        TextInputLayout tilPassword, MaterialButton btnExecute,
                                        AlertDialog passwordDialog, BottomSheetDialog parentDialog) {
        SessionManager session = new SessionManager(this);
        String email = session.getUserEmail();
        String deviceId = session.getDeviceId();
        String deviceName = session.getDeviceName();

        LoginRequest loginRequest = new LoginRequest(email, password, deviceId, deviceName);

        apiService.login(loginRequest).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject body = response.body();
                    boolean success = body.has("success") && body.get("success").getAsBoolean();

                    if (success) {
                        // Password verified! Update token if backend returned new one
                        if (body.has("data") && !body.get("data").isJsonNull()) {
                            JsonObject data = body.getAsJsonObject("data");
                            if (data.has("token") && !data.get("token").isJsonNull()) {
                                String newToken = data.get("token").getAsString();
                                session.saveSession(newToken, session.getUserId(),
                                        session.getUserName(), email, session.getUserRole());
                                // Reset API client with new token
                                ApiClient.resetClient();
                                apiService = ApiClient.getApiService(SalesHistoryActivity.this);
                            }
                        }

                        // Now execute the void
                        passwordDialog.dismiss();
                        executeVoid(saleId, voidReason, parentDialog);
                    } else {
                        // Login failed — wrong password
                        tilPassword.setError(getString(R.string.void_password_invalid));
                        btnExecute.setEnabled(true);
                        btnExecute.setText("🔒 Void Sekarang");
                    }
                } else {
                    // Server error or invalid credentials
                    tilPassword.setError(getString(R.string.void_password_invalid));
                    btnExecute.setEnabled(true);
                    btnExecute.setText("🔒 Void Sekarang");
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                tilPassword.setError("Gagal menghubungi server: " + t.getMessage());
                btnExecute.setEnabled(true);
                btnExecute.setText("🔒 Void Sekarang");
            }
        });
    }

    /**
     * Execute the actual void API call
     */
    private void executeVoid(int saleId, String voidReason, BottomSheetDialog parentDialog) {
        // Show loading
        android.widget.ProgressBar progressBar = new android.widget.ProgressBar(this);
        progressBar.setPadding(32, 32, 32, 32);
        AlertDialog loadingDialog = new AlertDialog.Builder(this)
                .setView(progressBar)
                .setMessage("Memproses void transaksi...")
                .setCancelable(false)
                .create();
        loadingDialog.show();

        JsonObject body = new JsonObject();
        body.addProperty("void_reason", voidReason);

        apiService.voidSale(saleId, body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!isFinishing()) {
                    loadingDialog.dismiss();

                    if (response.isSuccessful() && response.body() != null) {
                        // Void successful!
                        parentDialog.dismiss();
                        Toast.makeText(SalesHistoryActivity.this, getString(R.string.void_success), Toast.LENGTH_LONG).show();

                        // Refresh data
                        currentPage = 1;
                        loadSalesHistory(true);
                    } else {
                        // Try to parse error message
                        String errorMsg = getString(R.string.void_failed);
                        try {
                            if (response.errorBody() != null) {
                                String errStr = response.errorBody().string();
                                JsonObject errObj = new com.google.gson.JsonParser().parse(errStr).getAsJsonObject();
                                if (errObj.has("message")) {
                                    errorMsg = errObj.get("message").getAsString();
                                }
                            }
                        } catch (Exception ignored) {}

                        new AlertDialog.Builder(SalesHistoryActivity.this)
                                .setTitle("Void Gagal")
                                .setMessage(errorMsg)
                                .setPositiveButton("OK", null)
                                .show();
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                if (!isFinishing()) {
                    loadingDialog.dismiss();
                    new AlertDialog.Builder(SalesHistoryActivity.this)
                            .setTitle("Koneksi Gagal")
                            .setMessage("Tidak dapat menghubungi server.\n" + t.getMessage())
                            .setPositiveButton("OK", null)
                            .show();
                }
            }
        });
    }

    // ======================== END VOID FLOW ========================

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

    private void shareReceiptText(JsonObject saleObj) {
        String receiptText = com.example.kasirumkm2.printer.PrinterFormatter.buildReceiptShareText(saleObj);
        String invoiceNo = saleObj.has("invoice_no") ? saleObj.get("invoice_no").getAsString() : "";
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Struk Transaksi " + invoiceNo);
        shareIntent.putExtra(Intent.EXTRA_TEXT, receiptText);
        
        startActivity(Intent.createChooser(shareIntent, "Bagikan Struk via"));
    }

    private void fetchAndShareReceipt(int saleId) {
        android.widget.ProgressBar progressBar = new android.widget.ProgressBar(this);
        progressBar.setPadding(32, 32, 32, 32);
        AlertDialog loadingDialog = new AlertDialog.Builder(this)
                .setView(progressBar)
                .setMessage("Memuat detail transaksi...")
                .setCancelable(false)
                .create();
        loadingDialog.show();
        
        apiService.getSaleDetail(saleId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                loadingDialog.dismiss();
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject saleObj = response.body().getAsJsonObject("data");
                    shareReceiptText(saleObj);
                } else {
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
