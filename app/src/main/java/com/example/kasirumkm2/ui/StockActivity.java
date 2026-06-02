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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStockBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = ApiClient.getApiService(this);

        setupRecyclerView();
        setupListeners();
        loadStocks();
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
        binding.swipeRefresh.setOnRefreshListener(this::loadStocks);

        // Search
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterStocks(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Chip: Current Stock
        binding.chipCurrentStock.setOnClickListener(v -> {
            isDateMode = false;
            selectedDate = null;
            updateChipState();
            binding.tvDateLabel.setVisibility(View.GONE);
            loadStocks();
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

            loadStocks();
        });

        datePicker.show(getSupportFragmentManager(), "STOCK_DATE_PICKER");
    }

    private void loadStocks() {
        setLoading(true);

        Map<String, String> params = new HashMap<>();
        if (isDateMode && selectedDate != null) {
            params.put("date", selectedDate);
        }

        apiService.getStocks(params).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                setLoading(false);
                binding.swipeRefresh.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonArray dataArray = response.body().getAsJsonArray("data");
                        allStocks.clear();

                        for (int i = 0; i < dataArray.size(); i++) {
                            allStocks.add(dataArray.get(i).getAsJsonObject());
                        }

                        updateSummary();
                        filterStocks(binding.etSearch.getText().toString());
                    } catch (Exception e) {
                        showEmpty(true);
                        CurrencyHelper.showError(binding.getRoot(), "Gagal memuat data stok");
                    }
                } else if (response.code() == 401) {
                    handleUnauthorized();
                } else {
                    showEmpty(true);
                    CurrencyHelper.showError(binding.getRoot(), "Gagal memuat data stok");
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                setLoading(false);
                binding.swipeRefresh.setRefreshing(false);
                showEmpty(true);
                CurrencyHelper.showError(binding.getRoot(), getString(R.string.tidak_ada_koneksi));
            }
        });
    }

    private void filterStocks(String query) {
        if (query == null || query.trim().isEmpty()) {
            adapter.setData(allStocks);
            showEmpty(allStocks.isEmpty());
            return;
        }

        String lowerQuery = query.toLowerCase();
        List<JsonObject> filtered = new ArrayList<>();
        for (JsonObject item : allStocks) {
            String name = item.has("product_name") ? item.get("product_name").getAsString().toLowerCase() : "";
            String code = item.has("product_code") ? item.get("product_code").getAsString().toLowerCase() : "";
            if (name.contains(lowerQuery) || code.contains(lowerQuery)) {
                filtered.add(item);
            }
        }
        adapter.setData(filtered);
        showEmpty(filtered.isEmpty());
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
        loadStocks();
    }
}
