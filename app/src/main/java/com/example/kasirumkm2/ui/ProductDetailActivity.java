package com.example.kasirumkm2.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.kasirumkm2.R;
import com.example.kasirumkm2.api.ApiClient;
import com.example.kasirumkm2.api.ApiService;
import com.example.kasirumkm2.data.Product;
import com.example.kasirumkm2.databinding.ActivityProductDetailBinding;
import com.example.kasirumkm2.utils.CurrencyHelper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductDetailActivity extends AppCompatActivity {

    private ActivityProductDetailBinding binding;
    private ApiService apiService;
    private final Gson gson = new Gson();
    private int productId = -1;
    private Product currentProduct;

    private int userGroupId = 1;
    private int freelancerGroupId = 2;
    private int grosirGroupId = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProductDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = ApiClient.getApiService(this);
        productId = getIntent().getIntExtra("product_id", -1);

        if (productId <= 0) {
            CurrencyHelper.showToast(this, "ID Produk tidak valid");
            finish();
            return;
        }

        setupToolbar();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCustomerGroupsAndDetails();
    }

    private void loadCustomerGroupsAndDetails() {
        setLoading(true);
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
                loadProductDetails();
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                loadProductDetails();
            }
        });
    }

    private void setupToolbar() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnEdit.setOnClickListener(v -> {
            if (currentProduct != null) {
                Intent intent = new Intent(this, ProductFormActivity.class);
                intent.putExtra("product_id", productId);
                startActivity(intent);
            }
        });
    }

    private void setupListeners() {
        binding.btnDelete.setOnClickListener(v -> confirmDelete());
    }

    private void loadProductDetails() {
        setLoading(true);
        apiService.getProductDetail(productId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonObject data = response.body().getAsJsonObject("data");
                        currentProduct = gson.fromJson(data, Product.class);
                        displayProduct();
                    } catch (Exception e) {
                        CurrencyHelper.showError(binding.getRoot(), "Gagal parsing data produk");
                    }
                } else if (response.code() == 401) {
                    handleUnauthorized();
                } else {
                    CurrencyHelper.showError(binding.getRoot(), "Gagal memuat detail produk");
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                setLoading(false);
                CurrencyHelper.showError(binding.getRoot(), getString(R.string.tidak_ada_koneksi));
            }
        });
    }

    private void displayProduct() {
        if (currentProduct == null) return;

        binding.tvProductName.setText(currentProduct.getProductName());
        binding.tvEmoji.setText(currentProduct.getEmoji());
        binding.tvCode.setText(currentProduct.getProductCode());
        binding.tvStock.setText(currentProduct.getStock() + " " + (currentProduct.getUnit() != null ? currentProduct.getUnit() : "pcs"));
        binding.tvCostPrice.setText(CurrencyHelper.formatRupiah(currentProduct.getCostPrice()));

        // Display Active/Inactive Badge
        if (currentProduct.isActive()) {
            binding.tvBadgeStatus.setText("AKTIF");
            binding.tvBadgeStatus.setTextColor(getColor(R.color.primary));
            binding.tvBadgeStatus.setBackgroundResource(R.drawable.badge_primary_bg);
        } else {
            binding.tvBadgeStatus.setText("NON-AKTIF");
            binding.tvBadgeStatus.setTextColor(getColor(R.color.text_light));
            binding.tvBadgeStatus.setBackgroundResource(R.drawable.badge_inactive_bg);
        }

        // Prices & Profit Margin Calculation
        double cost = currentProduct.getCostPrice();

        // USER Group
        double priceUser = currentProduct.getSellingPrice(userGroupId);
        binding.tvPriceUser.setText(CurrencyHelper.formatRupiah(priceUser));
        binding.tvMarginUser.setText(calculateMarginText(cost, priceUser));

        // FREELANCER Group
        double priceFreelancer = currentProduct.getSellingPrice(freelancerGroupId);
        binding.tvPriceFreelancer.setText(CurrencyHelper.formatRupiah(priceFreelancer));
        binding.tvMarginFreelancer.setText(calculateMarginText(cost, priceFreelancer));

        // GROSIR Group
        double priceGrosir = currentProduct.getSellingPrice(grosirGroupId);
        binding.tvPriceGrosir.setText(CurrencyHelper.formatRupiah(priceGrosir));
        binding.tvMarginGrosir.setText(calculateMarginText(cost, priceGrosir));
    }

    private String calculateMarginText(double cost, double selling) {
        double profit = selling - cost;
        double markupPercent = cost > 0 ? (profit / cost) * 100 : 0;
        return String.format("Margin Keuntungan: %s (+%.1f%%)", CurrencyHelper.formatRupiah(profit), markupPercent);
    }

    private void confirmDelete() {
        if (currentProduct == null) return;

        new AlertDialog.Builder(this)
                .setTitle("Hapus Produk")
                .setMessage("Apakah Anda yakin ingin menghapus produk \"" + currentProduct.getProductName() + "\"?")
                .setPositiveButton("Hapus", (dialog, which) -> deleteProduct())
                .setNegativeButton("Batal", null)
                .show();
    }

    private void deleteProduct() {
        setLoading(true);
        apiService.deleteProduct(productId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                setLoading(false);
                if (response.isSuccessful()) {
                    CurrencyHelper.showToast(ProductDetailActivity.this, "Produk berhasil dihapus");
                    finish();
                } else if (response.code() == 401) {
                    handleUnauthorized();
                } else {
                    String errorMsg = "Gagal menghapus produk";
                    try {
                        if (response.errorBody() != null) {
                            JsonObject err = new com.google.gson.JsonParser()
                                    .parse(response.errorBody().string()).getAsJsonObject();
                            if (err.has("message")) errorMsg = err.get("message").getAsString();
                        }
                    } catch (Exception e) {}
                    CurrencyHelper.showError(binding.getRoot(), errorMsg);
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                setLoading(false);
                CurrencyHelper.showError(binding.getRoot(), getString(R.string.tidak_ada_koneksi));
            }
        });
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnEdit.setEnabled(!loading);
        binding.btnDelete.setEnabled(!loading);
    }

    private void handleUnauthorized() {
        new com.example.kasirumkm2.session.SessionManager(this).clearSession();
        ApiClient.resetClient();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
