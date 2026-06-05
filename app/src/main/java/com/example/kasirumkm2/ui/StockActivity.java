package com.example.kasirumkm2.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.kasirumkm2.R;
import com.example.kasirumkm2.adapter.StockAdapter;
import com.example.kasirumkm2.api.ApiClient;
import com.example.kasirumkm2.api.ApiService;
import com.example.kasirumkm2.databinding.ActivityStockBinding;
import com.example.kasirumkm2.utils.CurrencyHelper;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StockActivity extends AppCompatActivity {

    private ActivityStockBinding binding;
    private ApiService apiService;
    private StockAdapter adapter;

    private final List<JsonObject> allStocks = new ArrayList<>();
    private boolean isDateMode = false;
    private String selectedDate = null;

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
        binding = ActivityStockBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = ApiClient.getApiService(this);

        setupRecyclerView();
        setupListeners();
        loadStocks(true);
    }

    private void setupRecyclerView() {
        adapter = new StockAdapter(this);
        binding.rvStocks.setLayoutManager(new LinearLayoutManager(this));
        binding.rvStocks.setAdapter(adapter);

        adapter.setOnItemClickListener(stockItem -> {
            Intent intent = new Intent(StockActivity.this, StockDetailActivity.class);
            intent.putExtra("product_id", stockItem.get("product_id").getAsInt());
            intent.putExtra("product_name", stockItem.has("product_name") ? stockItem.get("product_name").getAsString() : "");
            intent.putExtra("product_code", stockItem.has("product_code") ? stockItem.get("product_code").getAsString() : "");
            intent.putExtra("unit", stockItem.has("unit") ? stockItem.get("unit").getAsString() : "PCS");

            // Pass current stock
            int stock = 0;
            try {
                if (stockItem.has("current_stock")) {
                    stock = (int) Double.parseDouble(stockItem.get("current_stock").getAsString());
                } else if (stockItem.has("stock_as_of_date")) {
                    stock = (int) Double.parseDouble(stockItem.get("stock_as_of_date").getAsString());
                }
            } catch (Exception e) {
                try {
                    if (stockItem.has("current_stock")) stock = stockItem.get("current_stock").getAsInt();
                } catch (Exception ignored) {}
            }
            intent.putExtra("current_stock", stock);

            startActivity(intent);
        });
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());

        // Swipe refresh
        binding.swipeRefresh.setColorSchemeResources(R.color.primary);
        binding.swipeRefresh.setOnRefreshListener(() -> {
            currentPage = 1;
            loadStocks(true);
        });

        // Search
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
                    loadStocks(true);
                };
                searchHandler.postDelayed(searchRunnable, 400); // 400ms debounce
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Scroll listener for infinite scroll using NestedScrollView
        binding.nestedScrollView.setOnScrollChangeListener(new androidx.core.widget.NestedScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(androidx.core.widget.NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                if (scrollY == v.getChildAt(0).getMeasuredHeight() - v.getMeasuredHeight()) {
                    if (!isLoading && currentPage < lastPage) {
                        currentPage++;
                        loadStocks(false);
                    }
                }
            }
        });

        // Chip: Current Stock
        binding.chipCurrentStock.setOnClickListener(v -> {
            isDateMode = false;
            selectedDate = null;
            updateChipState();
            binding.tvDateLabel.setVisibility(View.GONE);
            currentPage = 1;
            loadStocks(true);
        });

        // Chip: Date Stock
        binding.chipDateStock.setOnClickListener(v -> showDatePicker());
    }

    private void updateChipState() {
        if (isDateMode) {
            binding.chipCurrentStock.setChipBackgroundColorResource(R.color.white);
            binding.chipCurrentStock.setTextColor(getColor(R.color.text_gray));
            binding.chipCurrentStock.setChipStrokeColorResource(R.color.border);
            binding.chipCurrentStock.setChipStrokeWidth(2f);

            binding.chipDateStock.setChipBackgroundColorResource(R.color.primary);
            binding.chipDateStock.setTextColor(getColor(R.color.white));
            binding.chipDateStock.setChipStrokeWidth(0f);
        } else {
            binding.chipCurrentStock.setChipBackgroundColorResource(R.color.primary);
            binding.chipCurrentStock.setTextColor(getColor(R.color.white));
            binding.chipCurrentStock.setChipStrokeWidth(0f);

            binding.chipDateStock.setChipBackgroundColorResource(R.color.white);
            binding.chipDateStock.setTextColor(getColor(R.color.text_gray));
            binding.chipDateStock.setChipStrokeColorResource(R.color.border);
            binding.chipDateStock.setChipStrokeWidth(2f);
        }
    }

    private void showDatePicker() {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Pilih Tanggal Stok")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            selectedDate = sdf.format(new Date(selection));

            isDateMode = true;
            updateChipState();

            // Show readable date
            SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMM yyyy", new Locale("id", "ID"));
            displayFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            binding.tvDateLabel.setText("Per " + displayFormat.format(new Date(selection)));
            binding.tvDateLabel.setVisibility(View.VISIBLE);

            currentPage = 1;
            loadStocks(true);
        });

        datePicker.show(getSupportFragmentManager(), "STOCK_DATE_PICKER");
    }

    private void loadStocks(boolean isRefreshOrSearch) {
        if (isLoading) return;
        isLoading = true;

        if (isRefreshOrSearch) {
            setLoading(true);
            binding.progressBarLoadMore.setVisibility(View.GONE);
        } else {
            binding.progressBarLoadMore.setVisibility(View.VISIBLE);
        }

        Map<String, String> params = new HashMap<>();
        if (isDateMode && selectedDate != null) {
            params.put("date", selectedDate);
        }
        params.put("page", String.valueOf(currentPage));
        params.put("per_page", "15");
        if (searchQuery != null && !searchQuery.isEmpty()) {
            params.put("search", searchQuery);
        }

        apiService.getStocks(params).enqueue(new Callback<JsonObject>() {
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

                        List<JsonObject> newStocks = new ArrayList<>();
                        for (int i = 0; i < dataArray.size(); i++) {
                            newStocks.add(dataArray.get(i).getAsJsonObject());
                        }

                        if (isRefreshOrSearch) {
                            allStocks.clear();
                        }
                        allStocks.addAll(newStocks);

                        if (isRefreshOrSearch) {
                            adapter.setData(allStocks);
                        } else {
                            adapter.addData(newStocks);
                        }

                        updateSummary();
                        showEmpty(adapter.getItemCount() == 0);
                    } catch (Exception e) {
                        showEmpty(adapter.getItemCount() == 0);
                        CurrencyHelper.showError(binding.getRoot(), "Gagal memuat data stok");
                    }
                } else if (response.code() == 401) {
                    handleUnauthorized();
                } else {
                    showEmpty(adapter.getItemCount() == 0);
                    CurrencyHelper.showError(binding.getRoot(), "Gagal memuat data stok");
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                isLoading = false;
                setLoading(false);
                binding.progressBarLoadMore.setVisibility(View.GONE);
                binding.swipeRefresh.setRefreshing(false);
                showEmpty(adapter.getItemCount() == 0);
                CurrencyHelper.showError(binding.getRoot(), getString(R.string.tidak_ada_koneksi));
            }
        });
    }

    private void updateSummary() {
        int totalProducts = allStocks.size();
        int totalStock = 0;
        int lowStock = 0;

        for (JsonObject item : allStocks) {
            int stock = 0;
            try {
                if (item.has("current_stock")) {
                    stock = (int) Double.parseDouble(item.get("current_stock").getAsString());
                } else if (item.has("stock_as_of_date")) {
                    stock = (int) Double.parseDouble(item.get("stock_as_of_date").getAsString());
                }
            } catch (Exception e) {
                try {
                    if (item.has("current_stock")) stock = item.get("current_stock").getAsInt();
                    else if (item.has("stock_as_of_date")) stock = item.get("stock_as_of_date").getAsInt();
                } catch (Exception ignored) {}
            }

            totalStock += stock;
            if (stock <= 10) lowStock++;
        }

        binding.tvTotalProducts.setText(String.valueOf(totalProducts));
        binding.tvTotalStock.setText(String.valueOf(totalStock));
        binding.tvLowStock.setText(String.valueOf(lowStock));
    }

    private void showEmpty(boolean empty) {
        binding.layoutEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.rvStocks.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (loading) {
            binding.rvStocks.setVisibility(View.GONE);
            binding.layoutEmpty.setVisibility(View.GONE);
        } else {
            binding.rvStocks.setVisibility(View.VISIBLE);
        }
    }

    private void handleUnauthorized() {
        new com.example.kasirumkm2.session.SessionManager(this).clearSession();
        ApiClient.resetClient();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        currentPage = 1;
        loadStocks(true);
    }
}
