package com.example.kasirumkm2.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.kasirumkm2.R;
import com.example.kasirumkm2.adapter.CustomerAdapter;
import com.example.kasirumkm2.api.ApiClient;
import com.example.kasirumkm2.api.ApiService;
import com.example.kasirumkm2.data.Customer;
import com.example.kasirumkm2.databinding.ActivityCustomerListBinding;
import com.example.kasirumkm2.utils.CurrencyHelper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CustomerListActivity extends AppCompatActivity {

    private ActivityCustomerListBinding binding;
    private ApiService apiService;
    private CustomerAdapter adapter;
    private final Gson gson = new Gson();

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
        binding = ActivityCustomerListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = ApiClient.getApiService(this);

        setupToolbar();
        setupRecyclerView();
        setupSearch();
        setupFab();
        setupSwipeRefresh();

        loadCustomers(true);
    }

    private void setupToolbar() {
        binding.btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        boolean isSelectionMode = getIntent().getBooleanExtra("is_selection_mode", false);
        adapter = new CustomerAdapter(customer -> {
            if (isSelectionMode) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("customer_id", customer.getId());
                resultIntent.putExtra("customer_name", customer.getCustomerName());
                resultIntent.putExtra("customer_group_id", customer.getCustomerGroupId());
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                Intent intent = new Intent(this, CustomerDetailActivity.class);
                intent.putExtra("customer_id", customer.getId());
                startActivity(intent);
            }
        });
        binding.rvCustomers.setLayoutManager(new LinearLayoutManager(this));
        binding.rvCustomers.setAdapter(adapter);

        binding.rvCustomers.addOnScrollListener(new androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
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
                            loadCustomers(false);
                        }
                    }
                }
            }
        });
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
                    loadCustomers(true);
                };
                searchHandler.postDelayed(searchRunnable, 400); // 400ms debounce
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupFab() {
        binding.fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, CustomerFormActivity.class);
            startActivity(intent);
        });
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeColors(getColor(R.color.primary));
        binding.swipeRefresh.setOnRefreshListener(() -> {
            currentPage = 1;
            loadCustomers(true);
        });
    }

    private void loadCustomers(boolean isRefreshOrSearch) {
        if (isLoading) return;
        isLoading = true;

        if (isRefreshOrSearch) {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.progressBarLoadMore.setVisibility(View.GONE);
        } else {
            binding.progressBarLoadMore.setVisibility(View.VISIBLE);
        }

        Map<String, String> params = new HashMap<>();
        params.put("page", String.valueOf(currentPage));
        params.put("per_page", "15");
        if (searchQuery != null && !searchQuery.isEmpty()) {
            params.put("search", searchQuery);
        }

        apiService.getCustomersFiltered(params).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                isLoading = false;
                binding.progressBar.setVisibility(View.GONE);
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

                        JsonArray dataArray = extractDataArray(body);
                        List<Customer> customers = new ArrayList<>();
                        for (int i = 0; i < dataArray.size(); i++) {
                            Customer c = gson.fromJson(dataArray.get(i), Customer.class);
                            customers.add(c);
                        }

                        if (isRefreshOrSearch) {
                            adapter.setData(customers);
                        } else {
                            adapter.addData(customers);
                        }

                        toggleEmptyState(adapter.getItemCount() == 0);
                    } catch (Exception e) {
                        CurrencyHelper.showError(binding.getRoot(), "Gagal memuat data customer");
                        toggleEmptyState(adapter.getItemCount() == 0);
                    }
                } else if (response.code() == 401) {
                    handleUnauthorized();
                } else {
                    CurrencyHelper.showError(binding.getRoot(), getString(R.string.terjadi_kesalahan));
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                isLoading = false;
                binding.progressBar.setVisibility(View.GONE);
                binding.progressBarLoadMore.setVisibility(View.GONE);
                binding.swipeRefresh.setRefreshing(false);
                CurrencyHelper.showError(binding.getRoot(), getString(R.string.tidak_ada_koneksi));
            }
        });
    }

    /**
     * Safely extract the data array from API response.
     * Handles both flat ({"data": [...]}) and paginated ({"data": {"data": [...], ...}}) structures.
     */
    private JsonArray extractDataArray(JsonObject body) {
        if (body.has("data")) {
            if (body.get("data").isJsonArray()) {
                return body.getAsJsonArray("data");
            } else if (body.get("data").isJsonObject()) {
                JsonObject paginatedData = body.getAsJsonObject("data");
                if (paginatedData.has("data") && paginatedData.get("data").isJsonArray()) {
                    return paginatedData.getAsJsonArray("data");
                }
            }
        }
        return new JsonArray();
    }

    private void toggleEmptyState(boolean empty) {
        binding.layoutEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.rvCustomers.setVisibility(empty ? View.GONE : View.VISIBLE);
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
        loadCustomers(true);
    }
}
