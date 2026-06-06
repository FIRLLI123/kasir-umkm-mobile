package com.example.kasirumkm2.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.kasirumkm2.MainActivity;
import com.example.kasirumkm2.R;
import com.example.kasirumkm2.adapter.SalesAdapter;
import com.example.kasirumkm2.api.ApiClient;
import com.example.kasirumkm2.api.ApiService;
import com.example.kasirumkm2.databinding.FragmentHomeBinding;
import com.example.kasirumkm2.session.SessionManager;
import com.example.kasirumkm2.utils.CurrencyHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private SessionManager sessionManager;
    private ApiService apiService;
    private SalesAdapter salesAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());
        apiService = ApiClient.getApiService(requireContext());

        setupHeader();
        setupRecentSales();
        setupQuickActions();
        setupSwipeRefresh();
        updateSubscriptionCard();

        loadDashboardData();
    }

    private void setupHeader() {
        binding.tvGreeting.setText(CurrencyHelper.getGreeting(requireContext()));
        
        String compName = sessionManager.getCompanyName();
        if (compName != null && !compName.isEmpty()) {
            binding.tvUserName.setText(sessionManager.getUserName() + " · " + compName);
        } else {
            binding.tvUserName.setText(sessionManager.getUserName());
        }

        // Avatar initials
        String name = sessionManager.getUserName();
        if (name != null && !name.isEmpty()) {
            String[] parts = name.split("\\s+");
            String initials = parts.length >= 2
                    ? ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase()
                    : ("" + parts[0].charAt(0)).toUpperCase();
            binding.tvAvatar.setText(initials);
        }

        // Today date
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd MMMM yyyy", new Locale("id", "ID"));
        binding.tvTodayDate.setText(sdf.format(new Date()));
    }

    private void setupRecentSales() {
        salesAdapter = new SalesAdapter(this::showSaleDetailsBottomSheet);
        binding.rvRecentSales.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvRecentSales.setAdapter(salesAdapter);

        binding.btnViewAllSales.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), SalesHistoryActivity.class)));
    }

    private void setupQuickActions() {
        binding.cardKasir.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), POSActivity.class)));
        binding.cardProduk.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ProductListActivity.class)));
        binding.cardCustomer.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), CustomerListActivity.class)));
        binding.cardLaporan.setOnClickListener(v -> {
            if (getActivity() != null) {
                // Switch to report tab
                com.google.android.material.bottomnavigation.BottomNavigationView bottomNav =
                        getActivity().findViewById(R.id.bottomNav);
                if (bottomNav != null) bottomNav.setSelectedItemId(R.id.nav_laporan);
            }
        });
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeColors(
                requireContext().getColor(R.color.primary));
        binding.swipeRefresh.setOnRefreshListener(this::loadDashboardData);
    }

    private void loadDashboardData() {
        loadDailyReport();
        loadRecentSales();
        syncSubscriptionAndProfile();
    }

    private void syncSubscriptionAndProfile() {
        if (!isAdded()) return;

        apiService.getProfile().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonObject body = response.body();
                        sessionManager.updateAiChatLimit(body);
                        JsonObject data = body.getAsJsonObject("data");
                        
                        if (data.has("user") && !data.get("user").isJsonNull()) {
                            JsonObject userObj = data.getAsJsonObject("user");
                            String name = userObj.get("name").getAsString();
                            String email = userObj.get("email").getAsString();
                            String role = userObj.has("role") ? userObj.get("role").getAsString() : "admin";
                            sessionManager.saveSession(
                                    sessionManager.getToken(),
                                    userObj.get("id").getAsInt(),
                                    name,
                                    email,
                                    role
                            );
                        }

                        if (data.has("company") && !data.get("company").isJsonNull()) {
                            JsonObject comp = data.getAsJsonObject("company");
                            int compId = comp.get("id").getAsInt();
                            String compName = comp.get("company_name").getAsString();
                            String compCode = comp.get("company_code").getAsString();
                            sessionManager.saveCompany(compId, compName, compCode);
                        }
                        
                        if (data.has("subscription") && !data.get("subscription").isJsonNull()) {
                            JsonObject sub = data.getAsJsonObject("subscription");
                            String subStatus = sub.has("status") && !sub.get("status").isJsonNull() ? sub.get("status").getAsString() : "expired";
                            boolean subActive = sub.has("is_active") && !sub.get("is_active").isJsonNull() && sub.get("is_active").getAsBoolean();
                            boolean subLifetime = sub.has("is_lifetime") && !sub.get("is_lifetime").isJsonNull() && sub.get("is_lifetime").getAsBoolean();
                            String trialEndsAt = sub.has("trial_ends_at") && !sub.get("trial_ends_at").isJsonNull() ? sub.get("trial_ends_at").getAsString() : "";
                            String endsAt = sub.has("ends_at") && !sub.get("ends_at").isJsonNull() ? sub.get("ends_at").getAsString() : "";
                            sessionManager.saveSubscription(subStatus, subActive, subLifetime, trialEndsAt, endsAt);
                        }

                        setupHeader();
                        updateSubscriptionCard();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                // Ignore
            }
        });

        apiService.getSubscriptionActive().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonObject body = response.body();
                        sessionManager.updateAiChatLimit(body);
                        if (body.has("data") && !body.get("data").isJsonNull()) {
                            JsonObject sub = body.getAsJsonObject("data");
                            String subStatus = sub.has("status") && !sub.get("status").isJsonNull() ? sub.get("status").getAsString() : "expired";
                            boolean subActive = sub.has("is_active") && !sub.get("is_active").isJsonNull() && sub.get("is_active").getAsBoolean();
                            boolean subLifetime = sub.has("is_lifetime") && !sub.get("is_lifetime").isJsonNull() && sub.get("is_lifetime").getAsBoolean();
                            String trialEndsAt = sub.has("trial_ends_at") && !sub.get("trial_ends_at").isJsonNull() ? sub.get("trial_ends_at").getAsString() : "";
                            String endsAt = sub.has("ends_at") && !sub.get("ends_at").isJsonNull() ? sub.get("ends_at").getAsString() : "";

                            sessionManager.saveSubscription(subStatus, subActive, subLifetime, trialEndsAt, endsAt);
                            updateSubscriptionCard();
                            
                            if (getActivity() instanceof MainActivity) {
                                ((MainActivity) getActivity()).updateSubscriptionBanner();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                // Ignore
            }
        });
    }

    private void loadDailyReport() {
        Map<String, String> params = new HashMap<>();
        params.put("date", CurrencyHelper.getTodayDate());

        apiService.getReportDailyByDate(params).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!isAdded()) return;
                binding.swipeRefresh.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonObject body = response.body();
                        JsonObject data = body.getAsJsonObject("data");

                        double totalSales = data.has("net_sales") ? data.get("net_sales").getAsDouble() :
                                (data.has("total_sales") ? data.get("total_sales").getAsDouble() : 0);
                        double totalCost = data.has("modal") ? data.get("modal").getAsDouble() :
                                (data.has("total_cost") ? data.get("total_cost").getAsDouble() : 0);
                        double grossProfit = data.has("margin") ? data.get("margin").getAsDouble() :
                                (data.has("gross_profit") ? data.get("gross_profit").getAsDouble() : 0);
                        int transactionCount = data.has("total_transactions") ? data.get("total_transactions").getAsInt() :
                                (data.has("transaction_count") ? data.get("transaction_count").getAsInt() : 0);

                        // Calculate margin
                        double margin = totalSales > 0 ? (grossProfit / totalSales) * 100 : 0;

                        // Update UI
                        binding.tvOmzet.setText(CurrencyHelper.formatRupiah(totalSales));
                        binding.tvTotalModal.setText(CurrencyHelper.formatRupiah(totalCost));
                        binding.tvMargin.setText(CurrencyHelper.formatPercent(margin));
                        binding.tvTotalTransaksi.setText(String.valueOf(transactionCount));
                    } catch (Exception e) {
                        // Show defaults
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                if (!isAdded()) return;
                binding.swipeRefresh.setRefreshing(false);
            }
        });
    }

    private void loadRecentSales() {
        Map<String, String> filters = new HashMap<>();
        filters.put("start_date", CurrencyHelper.getTodayDate());
        filters.put("end_date", CurrencyHelper.getTodayDate());

        apiService.getSalesFiltered(filters).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonObject body = response.body();
                        JsonArray dataArray = body.getAsJsonArray("data");

                        List<JsonObject> salesList = new ArrayList<>();
                        int limit = Math.min(dataArray.size(), 5); // Show max 5
                        for (int i = 0; i < limit; i++) {
                            salesList.add(dataArray.get(i).getAsJsonObject());
                        }

                        salesAdapter.setData(salesList);

                        // Toggle empty state
                        binding.layoutEmptySales.setVisibility(
                                salesList.isEmpty() ? View.VISIBLE : View.GONE);
                        binding.rvRecentSales.setVisibility(
                                salesList.isEmpty() ? View.GONE : View.VISIBLE);
                    } catch (Exception e) {
                        binding.layoutEmptySales.setVisibility(View.VISIBLE);
                        binding.rvRecentSales.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                if (!isAdded()) return;
                binding.layoutEmptySales.setVisibility(View.VISIBLE);
                binding.rvRecentSales.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when coming back
        updateSubscriptionCard();
        loadDashboardData();
    }

    private void showSaleDetailsBottomSheet(JsonObject saleHeader) {
        int saleId = saleHeader.has("id") ? saleHeader.get("id").getAsInt() : -1;
        if (saleId == -1) return;

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.layout_sale_detail_sheet, null);
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
                if (!isAdded()) return;
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

                                View itemView = LayoutInflater.from(requireContext())
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

    private void printReceipt(JsonObject saleObj) {
        if (!isAdded() || getActivity() == null) return;
        SessionManager sessionManager = new SessionManager(requireContext());
        String printerAddress = sessionManager.getPrinterAddress();
        
        if (printerAddress == null || printerAddress.isEmpty()) {
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Printer Belum Diatur")
                    .setMessage("Silakan atur koneksi printer Bluetooth terlebih dahulu di menu Pengaturan.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }
        
        // Progress loader
        android.widget.ProgressBar progressBar = new android.widget.ProgressBar(requireContext());
        progressBar.setPadding(32, 32, 32, 32);
        androidx.appcompat.app.AlertDialog loadingDialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setView(progressBar)
                .setMessage("Menghubungkan ke printer & mencetak...")
                .setCancelable(false)
                .create();
        loadingDialog.show();
        
        com.example.kasirumkm2.printer.PrinterManager printerManager = new com.example.kasirumkm2.printer.PrinterManager(requireContext());
        printerManager.printReceiptAsync("KASIR UMKM", saleObj, printerAddress, new com.example.kasirumkm2.printer.PrinterManager.PrintCallback() {
            @Override
            public void onSuccess() {
                if (isAdded() && getActivity() != null) {
                    loadingDialog.dismiss();
                    Toast.makeText(requireContext(), "Struk berhasil dicetak ulang!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                if (isAdded() && getActivity() != null) {
                    loadingDialog.dismiss();
                    new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                            .setTitle("Gagal Cetak")
                            .setMessage(errorMessage + "\n\nPastikan printer menyala, kertas terisi, dan Bluetooth HP aktif.")
                            .setPositiveButton("OK", null)
                            .show();
                }
            }
        });
    }

    private void fetchAndPrintReceipt(int saleId) {
        if (!isAdded() || getActivity() == null) return;
        SessionManager sessionManager = new SessionManager(requireContext());
        String printerAddress = sessionManager.getPrinterAddress();
        
        if (printerAddress == null || printerAddress.isEmpty()) {
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Printer Belum Diatur")
                    .setMessage("Silakan atur koneksi printer Bluetooth terlebih dahulu di menu Pengaturan.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }
        
        android.widget.ProgressBar progressBar = new android.widget.ProgressBar(requireContext());
        progressBar.setPadding(32, 32, 32, 32);
        androidx.appcompat.app.AlertDialog loadingDialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setView(progressBar)
                .setMessage("Memuat detail transaksi & mencetak...")
                .setCancelable(false)
                .create();
        loadingDialog.show();
        
        apiService.getSaleDetail(saleId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!isAdded() || getActivity() == null) return;
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject saleObj = response.body().getAsJsonObject("data");
                    
                    com.example.kasirumkm2.printer.PrinterManager printerManager = new com.example.kasirumkm2.printer.PrinterManager(requireContext());
                    printerManager.printReceiptAsync("KASIR UMKM", saleObj, printerAddress, new com.example.kasirumkm2.printer.PrinterManager.PrintCallback() {
                        @Override
                        public void onSuccess() {
                            if (isAdded() && getActivity() != null) {
                                loadingDialog.dismiss();
                                Toast.makeText(requireContext(), "Struk berhasil dicetak ulang!", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            if (isAdded() && getActivity() != null) {
                                loadingDialog.dismiss();
                                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                                        .setTitle("Gagal Cetak")
                                        .setMessage(errorMessage + "\n\nPastikan printer menyala, kertas terisi, dan Bluetooth HP aktif.")
                                        .setPositiveButton("OK", null)
                                        .show();
                            }
                        }
                    });
                } else {
                    loadingDialog.dismiss();
                    Toast.makeText(requireContext(), "Gagal mengambil detail transaksi", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                if (isAdded() && getActivity() != null) {
                    loadingDialog.dismiss();
                    Toast.makeText(requireContext(), "Koneksi gagal: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateSubscriptionCard() {
        if (!isAdded() || binding == null) return;
        
        binding.cardSubscriptionStatus.setVisibility(View.VISIBLE);
        
        String status = sessionManager.getSubscriptionStatus();
        boolean isActive = sessionManager.isSubscriptionActive();
        boolean isLifetime = sessionManager.isSubscriptionLifetime();
        String endsAt = sessionManager.getSubscriptionEndsAt();
        String trialEndsAt = sessionManager.getSubscriptionTrialEndsAt();
        
        if (isLifetime) {
            binding.tvSubCardIcon.setText("👑");
            binding.tvSubCardTitle.setText(getString(R.string.subscription_status_lifetime));
            binding.tvSubCardSubtitle.setText("Terima kasih telah mendukung kami!");
            binding.btnSubCardAction.setVisibility(View.GONE);
            binding.cardSubscriptionStatus.setStrokeColor(requireContext().getColor(R.color.purple_primary));
            binding.layoutSubCardBackground.setBackgroundColor(requireContext().getColor(R.color.purple_light));
        } else if ("active".equals(status) && isActive) {
            binding.tvSubCardIcon.setText("⭐");
            binding.tvSubCardTitle.setText(getString(R.string.subscription_status_active));
            String formattedDate = formatDateString(endsAt);
            binding.tvSubCardSubtitle.setText(getString(R.string.subscription_days_remaining, formattedDate));
            binding.btnSubCardAction.setText("KELOLA");
            binding.btnSubCardAction.setVisibility(View.VISIBLE);
            binding.btnSubCardAction.setOnClickListener(v -> openSubscriptionScreen());
            binding.cardSubscriptionStatus.setStrokeColor(requireContext().getColor(R.color.border_light));
            binding.layoutSubCardBackground.setBackgroundColor(requireContext().getColor(R.color.success_green_light));
        } else if ("trial".equals(status) && isActive) {
            binding.tvSubCardIcon.setText("⏳");
            binding.tvSubCardTitle.setText(getString(R.string.subscription_status_trial));
            String formattedDate = formatDateString(trialEndsAt);
            binding.tvSubCardSubtitle.setText(getString(R.string.subscription_days_remaining, formattedDate));
            binding.btnSubCardAction.setText("UPGRADE");
            binding.btnSubCardAction.setVisibility(View.VISIBLE);
            binding.btnSubCardAction.setOnClickListener(v -> openSubscriptionScreen());
            binding.cardSubscriptionStatus.setStrokeColor(requireContext().getColor(R.color.border_light));
            binding.layoutSubCardBackground.setBackgroundColor(requireContext().getColor(R.color.blue_info_light));
        } else {
            // Expired or inactive
            binding.tvSubCardIcon.setText("⚠️");
            binding.tvSubCardTitle.setText(getString(R.string.subscription_status_expired));
            binding.tvSubCardSubtitle.setText("Akses dibatasi. Silakan lakukan aktivasi.");
            binding.btnSubCardAction.setText("UPGRADE");
            binding.btnSubCardAction.setVisibility(View.VISIBLE);
            binding.btnSubCardAction.setOnClickListener(v -> openSubscriptionScreen());
            binding.cardSubscriptionStatus.setStrokeColor(requireContext().getColor(R.color.danger_red));
            binding.layoutSubCardBackground.setBackgroundColor(requireContext().getColor(R.color.danger_red_light));
        }
    }
    
    private void openSubscriptionScreen() {
        Intent intent = new Intent(requireContext(), SubscriptionActivity.class);
        startActivity(intent);
    }
    
    private String formatDateString(String rawDate) {
        if (rawDate == null || rawDate.isEmpty()) return "-";
        try {
            String patternInput = rawDate.contains(" ") ? "yyyy-MM-dd HH:mm:ss" : "yyyy-MM-dd";
            SimpleDateFormat sdfInput = new SimpleDateFormat(patternInput, Locale.US);
            Date date = sdfInput.parse(rawDate);
            SimpleDateFormat sdfOutput = new SimpleDateFormat("dd MMM yyyy", new Locale("id", "ID"));
            return sdfOutput.format(date);
        } catch (Exception e) {
            return rawDate;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
