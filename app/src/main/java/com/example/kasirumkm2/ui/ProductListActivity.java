package com.example.kasirumkm2.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.kasirumkm2.R;
import com.example.kasirumkm2.adapter.ProductAdapter;
import com.example.kasirumkm2.api.ApiClient;
import com.example.kasirumkm2.api.ApiService;
import com.example.kasirumkm2.data.Product;
import com.example.kasirumkm2.databinding.ActivityProductListBinding;
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

public class ProductListActivity extends AppCompatActivity {

    private ActivityProductListBinding binding;
    private ApiService apiService;
    private ProductAdapter adapter;
    private final Gson gson = new Gson();
    private int userGroupId = 1;

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
        binding = ActivityProductListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = ApiClient.getApiService(this);

        setupToolbar();
        setupRecyclerView();
        setupSearch();
        setupFab();
        setupSwipeRefresh();

        loadCustomerGroupsAndProducts();
    }

    private void setupToolbar() {
        binding.btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new ProductAdapter(product -> {
            Intent intent = new Intent(this, ProductDetailActivity.class);
            intent.putExtra("product_id", product.getId());
            startActivity(intent);
        });
        binding.rvProducts.setLayoutManager(new LinearLayoutManager(this));
        binding.rvProducts.setAdapter(adapter);

        binding.rvProducts.addOnScrollListener(new androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
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
                            loadProducts(false);
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
                    loadProducts(true);
                };
                searchHandler.postDelayed(searchRunnable, 400); // 400ms debounce
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupFab() {
        binding.fabAdd.setOnClickListener(v -> {
            androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(this, binding.fabAdd);
            popup.getMenu().add(android.view.Menu.NONE, 1, android.view.Menu.NONE, "Tambah Satu Produk");
            popup.getMenu().add(android.view.Menu.NONE, 2, android.view.Menu.NONE, "Tambah Banyak");
            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == 1) {
                    Intent intent = new Intent(this, ProductFormActivity.class);
                    startActivity(intent);
                    return true;
                } else if (item.getItemId() == 2) {
                    Intent intent = new Intent(this, ProductBulkActivity.class);
                    startActivity(intent);
                    return true;
                }
                return false;
            });
            popup.show();
        });
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeColors(getColor(R.color.primary));
        binding.swipeRefresh.setOnRefreshListener(() -> {
            currentPage = 1;
            loadCustomerGroupsAndProducts();
        });
    }

    private void loadCustomerGroupsAndProducts() {
        binding.progressBar.setVisibility(View.VISIBLE);
        apiService.getCustomerGroups().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonArray data = response.body().getAsJsonArray("data");
                        for (int i = 0; i < data.size(); i++) {
                            JsonObject obj = data.get(i).getAsJsonObject();
                            int id = obj.get("id").getAsInt();
                            String code = obj.get("group_code").getAsString();
                            if ("USER".equalsIgnoreCase(code)) {
                                userGroupId = id;
                                adapter.setUserGroupId(userGroupId);
                                break;
                            }
                        }
                    } catch (Exception e) {
                        // ignore
                    }
                }
                currentPage = 1;
                loadProducts(true);
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                currentPage = 1;
                loadProducts(true);
            }
        });
    }

    private void loadProducts(boolean isRefreshOrSearch) {
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

        apiService.getProductsFiltered(params).enqueue(new Callback<JsonObject>() {
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
                            // In case Laravel's standard wrapper wraps meta inside data
                            JsonObject dataObj = body.getAsJsonObject("data");
                            if (dataObj.has("current_page")) {
                                currentPage = dataObj.get("current_page").getAsInt();
                            }
                            if (dataObj.has("last_page")) {
                                lastPage = dataObj.get("last_page").getAsInt();
                            }
                        }

                        JsonArray dataArray = extractDataArray(body);
                        List<Product> products = new ArrayList<>();
                        for (int i = 0; i < dataArray.size(); i++) {
                            Product p = gson.fromJson(dataArray.get(i), Product.class);
                            products.add(p);
                        }

                        if (isRefreshOrSearch) {
                            adapter.setData(products);
                        } else {
                            adapter.addData(products);
                        }

                        toggleEmptyState(adapter.getItemCount() == 0);
                    } catch (Exception e) {
                        CurrencyHelper.showError(binding.getRoot(), "Gagal memuat data produk");
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
                // Flat: {"data": [...]}
                return body.getAsJsonArray("data");
            } else if (body.get("data").isJsonObject()) {
                // Paginated: {"data": {"data": [...], "current_page": 1, ...}}
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
        binding.rvProducts.setVisibility(empty ? View.GONE : View.VISIBLE);
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
        loadCustomerGroupsAndProducts();
    }
}
