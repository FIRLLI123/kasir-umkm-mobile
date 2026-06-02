package com.example.kasirumkm2.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;

import com.example.kasirumkm2.R;
import com.example.kasirumkm2.api.ApiClient;
import com.example.kasirumkm2.api.ApiService;
import com.example.kasirumkm2.databinding.FragmentReportBinding;
import com.example.kasirumkm2.utils.CurrencyHelper;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReportFragment extends Fragment {

    private FragmentReportBinding binding;
    private ApiService apiService;
    private int selectedTabPosition = 0; // 0: Daily, 1: Weekly, 2: Monthly, 3: Custom

    private String startDate = "";
    private String endDate = "";
    private String customStartDate = "";
    private String customEndDate = "";

    private final SimpleDateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat displayDateFormat = new SimpleDateFormat("dd MMM yyyy", new Locale("id", "ID"));

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentReportBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        apiService = ApiClient.getApiService(requireContext());

        setupTabLayout();
        setupSwipeRefresh();
        setupDateFilterBar();

        updateDateRangeForTab();
        loadReportData();
    }

    private void setupTabLayout() {
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                selectedTabPosition = tab.getPosition();
                updateDateRangeForTab();
                
                // If custom tab and no custom dates selected, open date picker
                if (selectedTabPosition == 3 && (customStartDate.isEmpty() || customEndDate.isEmpty())) {
                    showCustomDatePicker();
                } else {
                    loadReportData();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeColors(requireContext().getColor(R.color.primary));
        binding.swipeRefresh.setOnRefreshListener(this::loadReportData);
    }

    private void setupDateFilterBar() {
        binding.cardDateFilter.setOnClickListener(v -> {
            if (selectedTabPosition == 3) {
                showCustomDatePicker();
            }
        });
    }

    private void updateDateRangeForTab() {
        Calendar cal = Calendar.getInstance();
        binding.tvChangeDateHint.setVisibility(View.GONE);

        if (selectedTabPosition == 0) {
            // Harian
            String today = CurrencyHelper.getTodayDate();
            startDate = today;
            endDate = today;
            try {
                binding.tvReportDateRange.setText("Hari Ini (" + displayDateFormat.format(new Date()) + ")");
            } catch (Exception e) {
                binding.tvReportDateRange.setText("Hari Ini (" + today + ")");
            }
        } else if (selectedTabPosition == 1) {
            // Mingguan
            cal.setFirstDayOfWeek(Calendar.MONDAY);
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            startDate = apiDateFormat.format(cal.getTime());
            String startLabel = displayDateFormat.format(cal.getTime());
            
            cal.add(Calendar.DAY_OF_WEEK, 6);
            endDate = apiDateFormat.format(cal.getTime());
            String endLabel = displayDateFormat.format(cal.getTime());
            
            binding.tvReportDateRange.setText("Minggu Ini (" + startLabel + " - " + endLabel + ")");
        } else if (selectedTabPosition == 2) {
            // Bulanan
            cal.set(Calendar.DAY_OF_MONTH, 1);
            startDate = apiDateFormat.format(cal.getTime());
            String monthLabel = new SimpleDateFormat("MMMM yyyy", new Locale("id", "ID")).format(cal.getTime());
            
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
            endDate = apiDateFormat.format(cal.getTime());
            
            binding.tvReportDateRange.setText("Bulan Ini (" + monthLabel + ")");
        } else if (selectedTabPosition == 3) {
            // Kustom
            binding.tvChangeDateHint.setVisibility(View.VISIBLE);
            if (!customStartDate.isEmpty() && !customEndDate.isEmpty()) {
                startDate = customStartDate;
                endDate = customEndDate;
                try {
                    Date start = apiDateFormat.parse(customStartDate);
                    Date end = apiDateFormat.parse(customEndDate);
                    if (start != null && end != null) {
                        binding.tvReportDateRange.setText("Kustom (" + displayDateFormat.format(start) + " - " + displayDateFormat.format(end) + ")");
                    }
                } catch (Exception e) {
                    binding.tvReportDateRange.setText("Kustom (" + customStartDate + " - " + customEndDate + ")");
                }
            } else {
                binding.tvReportDateRange.setText("Pilih Rentang Tanggal...");
            }
        }
    }

    private void showCustomDatePicker() {
        MaterialDatePicker<Pair<Long, Long>> dateRangePicker =
                MaterialDatePicker.Builder.dateRangePicker()
                        .setTitleText("Pilih Rentang Laporan")
                        .build();

        dateRangePicker.addOnPositiveButtonClickListener(selection -> {
            if (selection != null && selection.first != null && selection.second != null) {
                customStartDate = apiDateFormat.format(new Date(selection.first));
                customEndDate = apiDateFormat.format(new Date(selection.second));
                updateDateRangeForTab();
                loadReportData();
            }
        });

        dateRangePicker.addOnCancelListener(dialog -> {
            if (customStartDate.isEmpty() || customEndDate.isEmpty()) {
                // Select Today if user cancelled choosing custom dates
                binding.tabLayout.selectTab(binding.tabLayout.getTabAt(0));
            }
        });

        dateRangePicker.show(getChildFragmentManager(), "REPORT_DATE_RANGE_PICKER");
    }

    private void loadReportData() {
        if (startDate.isEmpty() || endDate.isEmpty()) return;

        setLoading(true);

        // 1. Fetch Financial Summaries
        if (selectedTabPosition == 3) {
            // Custom date ranges are loaded programmatically by fetching sales list
            loadCustomSummary();
        } else {
            loadStandardSummary();
        }

        // 2. Fetch Weekly Sales Bar Chart data
        loadWeeklyBarChart();

        // 3. Fetch Payment Methods Laporan
        loadPaymentMethodsReport();

        // 4. Fetch Top Products Laporan
        loadTopProductsReport();
    }

    private void loadStandardSummary() {
        Map<String, String> params = new HashMap<>();
        params.put("date", startDate); // for standard reports, date points to the start date

        Call<JsonObject> call;
        if (selectedTabPosition == 1) {
            call = apiService.getReportWeeklyFiltered(params);
        } else if (selectedTabPosition == 2) {
            call = apiService.getReportMonthlyFiltered(params);
        } else {
            call = apiService.getReportDailyByDate(params);
        }

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!isAdded()) return;
                binding.swipeRefresh.setRefreshing(false);
                setLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonObject body = response.body();
                        JsonObject data = body.getAsJsonObject("data");
                        updateSummaryUI(data);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (response.code() == 401) {
                    handleUnauthorized();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                if (!isAdded()) return;
                binding.swipeRefresh.setRefreshing(false);
                setLoading(false);
            }
        });
    }

    private void loadCustomSummary() {
        Map<String, String> params = new HashMap<>();
        params.put("start_date", startDate);
        params.put("end_date", endDate);

        apiService.getSalesFiltered(params).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!isAdded()) return;
                binding.swipeRefresh.setRefreshing(false);
                setLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonObject body = response.body();
                        JsonArray salesArray = body.getAsJsonArray("data");

                        double totalSales = 0;
                        double totalCost = 0;
                        double grossProfit = 0;
                        int transactionCount = 0;

                        for (int i = 0; i < salesArray.size(); i++) {
                            JsonObject sale = salesArray.get(i).getAsJsonObject();
                            String status = sale.has("status") ? sale.get("status").getAsString() : "00";
                            
                            // Only sum successful transactions
                            if ("00".equals(status)) {
                                totalSales += sale.has("grand_total") ? sale.get("grand_total").getAsDouble() : 0;
                                totalCost += sale.has("total_modal") ? sale.get("total_modal").getAsDouble() : 0;
                                grossProfit += sale.has("total_margin") ? sale.get("total_margin").getAsDouble() : 0;
                                transactionCount++;
                            }
                        }

                        JsonObject simulatedData = new JsonObject();
                        simulatedData.addProperty("net_sales", totalSales);
                        simulatedData.addProperty("modal", totalCost);
                        simulatedData.addProperty("margin", grossProfit);
                        simulatedData.addProperty("total_transactions", transactionCount);

                        updateSummaryUI(simulatedData);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (response.code() == 401) {
                    handleUnauthorized();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                if (!isAdded()) return;
                binding.swipeRefresh.setRefreshing(false);
                setLoading(false);
            }
        });
    }

    private void updateSummaryUI(JsonObject data) {
        double totalSales = data.has("net_sales") ? data.get("net_sales").getAsDouble() :
                (data.has("total_sales") ? data.get("total_sales").getAsDouble() : 0);
        double totalCost = data.has("modal") ? data.get("modal").getAsDouble() :
                (data.has("total_cost") ? data.get("total_cost").getAsDouble() : 0);
        double grossProfit = data.has("margin") ? data.get("margin").getAsDouble() :
                (data.has("gross_profit") ? data.get("gross_profit").getAsDouble() : 0);
        int transactionCount = data.has("total_transactions") ? data.get("total_transactions").getAsInt() :
                (data.has("transaction_count") ? data.get("transaction_count").getAsInt() : 0);

        // Margin % calculation
        double margin = totalSales > 0 ? (grossProfit / totalSales) * 100 : 0;

        // Display
        binding.tvOmzet.setText(CurrencyHelper.formatRupiah(totalSales));
        binding.tvModal.setText(CurrencyHelper.formatRupiah(totalCost));
        binding.tvTransaksi.setText(transactionCount + " Transaksi");
        binding.tvProfit.setText(CurrencyHelper.formatRupiah(grossProfit));
        binding.tvMarginPercent.setText(String.format(Locale.getDefault(), "(+%.1f%%)", margin));

        // Styling
        if (grossProfit >= 0) {
            binding.tvProfit.setTextColor(requireContext().getColor(R.color.success_green));
            binding.tvMarginPercent.setTextColor(requireContext().getColor(R.color.success_green));
        } else {
            binding.tvProfit.setTextColor(requireContext().getColor(R.color.danger_red));
            binding.tvMarginPercent.setTextColor(requireContext().getColor(R.color.danger_red));
        }
    }

    private void loadWeeklyBarChart() {
        // Query transactions for the current week
        Calendar cal = Calendar.getInstance();
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        String weekStart = apiDateFormat.format(cal.getTime());
        cal.add(Calendar.DAY_OF_WEEK, 6);
        String weekEnd = apiDateFormat.format(cal.getTime());

        Map<String, String> filters = new HashMap<>();
        filters.put("start_date", weekStart);
        filters.put("end_date", weekEnd);

        apiService.getSalesFiltered(filters).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonArray salesArray = response.body().getAsJsonArray("data");
                        double[] dailySales = new double[7]; // Monday to Sunday

                        for (int i = 0; i < salesArray.size(); i++) {
                            JsonObject sale = salesArray.get(i).getAsJsonObject();
                            String status = sale.has("status") ? sale.get("status").getAsString() : "00";
                            
                            if ("00".equals(status)) {
                                double grandTotal = sale.has("grand_total") ? sale.get("grand_total").getAsDouble() : 0;
                                String rawDate = sale.has("created_at") ? sale.get("created_at").getAsString() : "";
                                
                                int dayIndex = getDayOfWeekIndex(rawDate);
                                if (dayIndex >= 0 && dayIndex < 7) {
                                    dailySales[dayIndex] += grandTotal;
                                }
                            }
                        }

                        // Find maximum daily sales
                        double maxSales = 0;
                        for (double val : dailySales) {
                            if (val > maxSales) maxSales = val;
                        }

                        // Draw bars
                        setBarHeight(binding.barMon, dailySales[0], maxSales);
                        setBarHeight(binding.barTue, dailySales[1], maxSales);
                        setBarHeight(binding.barWed, dailySales[2], maxSales);
                        setBarHeight(binding.barThu, dailySales[3], maxSales);
                        setBarHeight(binding.barFri, dailySales[4], maxSales);
                        setBarHeight(binding.barSat, dailySales[5], maxSales);
                        setBarHeight(binding.barSun, dailySales[6], maxSales);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {}
        });
    }

    private int getDayOfWeekIndex(String dateStr) {
        try {
            // Expecting "yyyy-MM-dd HH:mm:ss" or ISO format
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = sdf.parse(dateStr);
            Calendar cal = Calendar.getInstance();
            if (date != null) {
                cal.setTime(date);
                // convert Calendar day (1: Sun, 2: Mon...) to index (0: Mon, 1: Tue... 6: Sun)
                int calendarDay = cal.get(Calendar.DAY_OF_WEEK);
                if (calendarDay == Calendar.SUNDAY) return 6;
                return calendarDay - 2;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    private void setBarHeight(View bar, double value, double maxValue) {
        if (maxValue == 0 || value == 0) {
            ViewGroup.LayoutParams lp = bar.getLayoutParams();
            lp.height = 0;
            bar.setLayoutParams(lp);
            return;
        }

        // Height scale max is 120dp
        float dpScale = requireContext().getResources().getDisplayMetrics().density;
        int maxHeightPx = (int) (120 * dpScale);
        int finalHeight = (int) ((value / maxValue) * maxHeightPx);

        // Bound to minimum height of 4dp for visual visibility if there is amount
        int minHeightPx = (int) (4 * dpScale);
        if (finalHeight < minHeightPx) finalHeight = minHeightPx;

        ViewGroup.LayoutParams lp = bar.getLayoutParams();
        lp.height = finalHeight;
        bar.setLayoutParams(lp);
    }

    private void loadPaymentMethodsReport() {
        Map<String, String> params = new HashMap<>();
        params.put("start_date", startDate);
        params.put("end_date", endDate);

        apiService.getReportPaymentMethods(params).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonArray dataArray = response.body().getAsJsonArray("data");
                        double cashTotal = 0;
                        double transferTotal = 0;
                        double qrisTotal = 0;

                        for (int i = 0; i < dataArray.size(); i++) {
                            JsonObject pm = dataArray.get(i).getAsJsonObject();
                            String code = pm.has("method_code") ? pm.get("method_code").getAsString() : "";
                            double amount = 0;
                            if (pm.has("total_amount")) {
                                try {
                                    amount = Double.parseDouble(pm.get("total_amount").getAsString());
                                } catch (Exception e) {
                                    try {
                                        amount = pm.get("total_amount").getAsDouble();
                                    } catch (Exception ex) {
                                        amount = 0;
                                    }
                                }
                            }

                            if ("CASH".equalsIgnoreCase(code)) {
                                cashTotal = amount;
                            } else if ("TRANSFER".equalsIgnoreCase(code)) {
                                transferTotal = amount;
                            } else if ("QRIS".equalsIgnoreCase(code)) {
                                qrisTotal = amount;
                            }
                        }

                        binding.tvReportCashAmount.setText(CurrencyHelper.formatRupiah(cashTotal));
                        binding.tvReportTransferAmount.setText(CurrencyHelper.formatRupiah(transferTotal));
                        binding.tvReportQrisAmount.setText(CurrencyHelper.formatRupiah(qrisTotal));

                        double totalPayments = cashTotal + transferTotal + qrisTotal;
                        if (totalPayments > 0) {
                            binding.progressReportCash.setProgress((int) ((cashTotal / totalPayments) * 100));
                            binding.progressReportTransfer.setProgress((int) ((transferTotal / totalPayments) * 100));
                            binding.progressReportQris.setProgress((int) ((qrisTotal / totalPayments) * 100));
                        } else {
                            binding.progressReportCash.setProgress(0);
                            binding.progressReportTransfer.setProgress(0);
                            binding.progressReportQris.setProgress(0);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {}
        });
    }

    private void loadTopProductsReport() {
        Map<String, String> params = new HashMap<>();
        params.put("start_date", startDate);
        params.put("end_date", endDate);

        apiService.getReportProducts(params).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonArray dataArray = response.body().getAsJsonArray("data");
                        binding.layoutTopProducts.removeAllViews();
                        
                        boolean isEmpty = dataArray.size() == 0;
                        binding.tvEmptyProductsReport.setVisibility(isEmpty ? View.VISIBLE : View.GONE);

                        int limit = Math.min(dataArray.size(), 5);
                        for (int i = 0; i < limit; i++) {
                            JsonObject productObj = dataArray.get(i).getAsJsonObject();

                            String name = productObj.has("product_name") ? productObj.get("product_name").getAsString() : "-";
                            int qty = 0;
                            if (productObj.has("qty_sold")) {
                                try {
                                    qty = (int) Double.parseDouble(productObj.get("qty_sold").getAsString());
                                } catch (Exception e) {
                                    try {
                                        qty = productObj.get("qty_sold").getAsInt();
                                    } catch (Exception ex) {
                                        qty = 0;
                                    }
                                }
                            }

                            double salesAmount = 0;
                            if (productObj.has("total_sales")) {
                                try {
                                    salesAmount = Double.parseDouble(productObj.get("total_sales").getAsString());
                                } catch (Exception e) {
                                    try {
                                        salesAmount = productObj.get("total_sales").getAsDouble();
                                    } catch (Exception ex) {
                                        salesAmount = 0;
                                    }
                                }
                            }

                            // Premium item view
                            View itemView = LayoutInflater.from(requireContext())
                                    .inflate(R.layout.item_checkout_summary, binding.layoutTopProducts, false);

                            TextView tvName = itemView.findViewById(R.id.tvProductName);
                            TextView tvQty = itemView.findViewById(R.id.tvQtyPrice);
                            TextView tvAmount = itemView.findViewById(R.id.tvSubtotal);

                            // Get automatic emoji category
                            String emoji = getCategoryEmoji(name);

                            tvName.setText("#" + (i + 1) + " " + emoji + " " + name);
                            tvQty.setText(qty + " unit terjual");
                            tvAmount.setText(CurrencyHelper.formatRupiah(salesAmount));
                            tvAmount.setTextColor(requireContext().getColor(R.color.primary));

                            binding.layoutTopProducts.addView(itemView);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {}
        });
    }

    private String getCategoryEmoji(String productName) {
        String name = productName.toLowerCase(Locale.getDefault());
        if (name.contains("aqua") || name.contains("mineral") || name.contains("air") || name.contains("le minerale")) return "💧";
        if (name.contains("mie") || name.contains("noodle") || name.contains("indomie")) return "🍜";
        if (name.contains("kopi") || name.contains("coffee") || name.contains("caffeno")) return "☕";
        if (name.contains("susu") || name.contains("milk")) return "🥛";
        if (name.contains("teh") || name.contains("tea")) return "🍵";
        if (name.contains("rokok") || name.contains("sampoerna") || name.contains("surya")) return "🚬";
        if (name.contains("snack") || name.contains("ciki") || name.contains("potato")) return "🍿";
        if (name.contains("sabun") || name.contains("soap") || name.contains("shampoo")) return "🧼";
        return "📦";
    }

    private void handleUnauthorized() {
        if (getContext() != null) {
            new com.example.kasirumkm2.session.SessionManager(getContext()).clearSession();
            ApiClient.resetClient();
            Intent intent = new Intent(getContext(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            if (getActivity() != null) getActivity().finish();
        }
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadReportData();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
