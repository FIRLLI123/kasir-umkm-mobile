package com.example.kasirumkm2.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.kasirumkm2.R;
import com.example.kasirumkm2.adapter.POSProductAdapter;
import com.example.kasirumkm2.api.ApiClient;
import com.example.kasirumkm2.api.ApiService;
import com.example.kasirumkm2.data.CartItem;
import com.example.kasirumkm2.data.Product;
import com.example.kasirumkm2.databinding.ActivityPosBinding;
import com.example.kasirumkm2.session.SessionManager;
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

public class POSActivity extends AppCompatActivity {

    private static final int REQUEST_CHOOSE_CUSTOMER = 100;
    private static final int REQUEST_CHECKOUT = 200;

    private ActivityPosBinding binding;
    private ApiService apiService;
    private POSProductAdapter adapter;
    private final Gson gson = new Gson();

    private final List<Product> productList = new ArrayList<>();
    private final List<CartItem> cartList = new ArrayList<>();

    // Initial Customer is not chosen
    private int selectedCustomerId = -1;
    private String selectedCustomerName = "Pilih Customer...";
    private int selectedCustomerGroupId = -1;

    private int userGroupId = 1;
    private int freelancerGroupId = 2;
    private int grosirGroupId = 3;

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
        binding = ActivityPosBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SessionManager sessionManager = new SessionManager(this);
        if (sessionManager.isSubscriptionExpired()) {
            android.widget.Toast.makeText(this, "Masa aktif langganan Anda telah habis. Akses dibatasi.", android.widget.Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, SubscriptionActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        apiService = ApiClient.getApiService(this);

        setupToolbar();
        setupCustomerSelector();
        setupRecyclerView();
        setupSearch();
        setupSwipeRefresh();
        setupCheckoutPanel();

        loadCustomerGroups();
    }

    private void loadCustomerGroups() {
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
                                if (selectedCustomerId <= 0) {
                                    selectedCustomerGroupId = id;
                                }
                            } else if ("FREELANCER".equalsIgnoreCase(code)) {
                                freelancerGroupId = id;
                            } else if ("GROSIR".equalsIgnoreCase(code)) {
                                grosirGroupId = id;
                            }
                        }
                    } catch (Exception e) {
                        // ignore
                    }
                }
                updateCustomerDisplay();
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

    private void setupToolbar() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnClearCart.setOnClickListener(v -> clearCart());
    }

    private void setupCustomerSelector() {
        binding.btnChooseCustomer.setOnClickListener(v -> {
            Intent intent = new Intent(this, CustomerListActivity.class);
            intent.putExtra("is_selection_mode", true);
            startActivityForResult(intent, REQUEST_CHOOSE_CUSTOMER);
        });
        updateCustomerDisplay();
    }

    private void setupRecyclerView() {
        adapter = new POSProductAdapter(new POSProductAdapter.OnCartActionListener() {
            @Override
            public void onAddToCart(Product product) {
                addToCart(product);
            }

            @Override
            public void onIncrementQty(Product product) {
                incrementCartItem(product);
            }

            @Override
            public void onDecrementQty(Product product) {
                decrementCartItem(product);
            }
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

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeColors(getColor(R.color.primary));
        binding.swipeRefresh.setOnRefreshListener(() -> {
            currentPage = 1;
            loadProducts(true);
        });
    }

    private void setupCheckoutPanel() {
        binding.btnCheckout.setOnClickListener(v -> {
            if (cartList.isEmpty()) return;

            Intent intent = new Intent(this, CheckoutActivity.class);
            intent.putExtra("cart_json", gson.toJson(cartList));
            intent.putExtra("customer_id", selectedCustomerId);
            intent.putExtra("customer_name", selectedCustomerName);
            intent.putExtra("customer_group_id", selectedCustomerGroupId);
            startActivityForResult(intent, REQUEST_CHECKOUT);
        });
        updateCheckoutPanel();
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
                            JsonObject dataObj = body.getAsJsonObject("data");
                            if (dataObj.has("current_page")) {
                                currentPage = dataObj.get("current_page").getAsInt();
                            }
                            if (dataObj.has("last_page")) {
                                lastPage = dataObj.get("last_page").getAsInt();
                            }
                        }

                        JsonArray dataArray = extractDataArray(body);
                        List<Product> newProducts = new ArrayList<>();
                        for (int i = 0; i < dataArray.size(); i++) {
                            Product p = gson.fromJson(dataArray.get(i), Product.class);
                            newProducts.add(p);
                        }

                        if (isRefreshOrSearch) {
                            productList.clear();
                        }
                        productList.addAll(newProducts);

                        if (isRefreshOrSearch) {
                            adapter.setData(productList);
                        } else {
                            adapter.addData(newProducts);
                        }
                        adapter.setCartState(cartList, selectedCustomerGroupId);
                        toggleEmptyState(productList.isEmpty());
                    } catch (Exception e) {
                        CurrencyHelper.showError(binding.getRoot(), "Gagal memuat produk");
                        toggleEmptyState(productList.isEmpty());
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

    private void addToCart(Product product) {
        // Double check stock
        if (product.getStock() <= 0) {
            CurrencyHelper.showError(binding.getRoot(), "Stok produk habis");
            return;
        }

        double price = product.getSellingPrice(selectedCustomerGroupId);
        CartItem item = new CartItem(product, 1, price);
        cartList.add(item);

        adapter.setCartState(cartList, selectedCustomerGroupId);
        updateCheckoutPanel();
    }

    private void incrementCartItem(Product product) {
        for (CartItem item : cartList) {
            if (item.getProduct().getId() == product.getId()) {
                if (item.getQty() < product.getStock()) {
                    item.incrementQty();
                } else {
                    CurrencyHelper.showError(binding.getRoot(), "Jumlah pesanan melebihi stok");
                }
                break;
            }
        }
        adapter.setCartState(cartList, selectedCustomerGroupId);
        updateCheckoutPanel();
    }

    private void decrementCartItem(Product product) {
        CartItem toRemove = null;
        for (CartItem item : cartList) {
            if (item.getProduct().getId() == product.getId()) {
                if (item.getQty() > 1) {
                    item.decrementQty();
                } else {
                    toRemove = item;
                }
                break;
            }
        }
        if (toRemove != null) {
            cartList.remove(toRemove);
        }
        adapter.setCartState(cartList, selectedCustomerGroupId);
        updateCheckoutPanel();
    }

    private void clearCart() {
        if (cartList.isEmpty()) return;
        cartList.clear();
        adapter.setCartState(cartList, selectedCustomerGroupId);
        updateCheckoutPanel();
        CurrencyHelper.showToast(this, "Keranjang dibersihkan");
    }

    private void updateCheckoutPanel() {
        int totalItems = 0;
        double totalPrice = 0;

        for (CartItem item : cartList) {
            totalItems += item.getQty();
            totalPrice += item.getSubtotal();
        }

        binding.tvTotalItems.setText(totalItems + " Item");
        binding.tvTotalPrice.setText(CurrencyHelper.formatRupiah(totalPrice));
        
        boolean customerSelected = selectedCustomerId > 0;
        binding.btnCheckout.setEnabled(totalItems > 0 && customerSelected);
        if (totalItems > 0 && !customerSelected) {
            binding.btnCheckout.setText("Pilih Customer Dulu");
        } else {
            binding.btnCheckout.setText(totalItems > 0 ? "Bayar (" + totalItems + ")" : "Bayar");
        }
    }

    private void updateCustomerDisplay() {
        binding.tvCustomerName.setText(selectedCustomerName);
        String groupName = "Belum Terpilih";
        if (selectedCustomerId > 0) {
            if (selectedCustomerGroupId == freelancerGroupId) groupName = "FREELANCER";
            else if (selectedCustomerGroupId == grosirGroupId) groupName = "GROSIR";
            else groupName = "USER / Reguler";
        }

        binding.tvCustomerGroup.setText("Golongan: " + groupName);
        binding.btnChooseCustomer.setText(selectedCustomerId > 0 ? "Ganti" : "Pilih");
    }

    private void recalculateCartPrices() {
        for (CartItem item : cartList) {
            double newPrice = item.getProduct().getSellingPrice(selectedCustomerGroupId);
            item.setPricePerUnit(newPrice);
            item.setQty(item.getQty()); // force subtotal refresh
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CHOOSE_CUSTOMER && resultCode == RESULT_OK && data != null) {
            selectedCustomerId = data.getIntExtra("customer_id", 0);
            selectedCustomerName = data.getStringExtra("customer_name");
            selectedCustomerGroupId = data.getIntExtra("customer_group_id", 1);

            updateCustomerDisplay();
            recalculateCartPrices();
            adapter.setCartState(cartList, selectedCustomerGroupId);
            updateCheckoutPanel();

            CurrencyHelper.showToast(this, "Customer diubah ke: " + selectedCustomerName);
        } else if (requestCode == REQUEST_CHECKOUT && resultCode == RESULT_OK) {
            // Reset cart list
            cartList.clear();
            
            // Reset customer selection to default (unset)
            selectedCustomerId = -1;
            selectedCustomerName = "Pilih Customer...";
            selectedCustomerGroupId = userGroupId;

            updateCustomerDisplay();
            updateCheckoutPanel();
            adapter.setCartState(cartList, selectedCustomerGroupId);
        }
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
        // Load customer groups first to ensure dynamic pricing matches correctly
        loadCustomerGroups();
    }
}
