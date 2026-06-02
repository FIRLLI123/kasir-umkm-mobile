package com.example.kasirumkm2.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.kasirumkm2.R;
import com.example.kasirumkm2.api.ApiClient;
import com.example.kasirumkm2.api.ApiService;
import com.example.kasirumkm2.data.Product;
import com.example.kasirumkm2.data.ProductPrice;
import com.example.kasirumkm2.databinding.ActivityProductFormBinding;
import com.example.kasirumkm2.utils.CurrencyHelper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductFormActivity extends AppCompatActivity {

    private ActivityProductFormBinding binding;
    private ApiService apiService;
    private final Gson gson = new Gson();

    private boolean isEditMode = false;
    private int productId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProductFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = ApiClient.getApiService(this);

        // Check edit mode
        productId = getIntent().getIntExtra("product_id", -1);
        isEditMode = productId > 0;

        setupToolbar();
        setupListeners();

        if (isEditMode) {
            binding.tvTitle.setText("Edit Produk");
            loadProductData();
        }
    }

    private void setupToolbar() {
        binding.btnBack.setOnClickListener(v -> finish());
    }

    private void setupListeners() {
        binding.btnSave.setOnClickListener(v -> saveProduct());
    }

    private void loadProductData() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnSave.setEnabled(false);

        apiService.getProductDetail(productId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                binding.progressBar.setVisibility(View.GONE);
                binding.btnSave.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonObject data = response.body().getAsJsonObject("data");
                        Product product = gson.fromJson(data, Product.class);

                        binding.etCode.setText(product.getProductCode());
                        binding.etName.setText(product.getProductName());
                        binding.etUnit.setText(product.getUnit());
                        binding.etStock.setText(String.valueOf(product.getStock()));
                        binding.etCostPrice.setText(String.valueOf((int) product.getCostPrice()));

                        // Prices
                        binding.etPriceUser.setText(String.valueOf((int) product.getSellingPrice(1)));
                        binding.etPriceFreelancer.setText(String.valueOf((int) product.getSellingPrice(2)));
                        binding.etPriceGrosir.setText(String.valueOf((int) product.getSellingPrice(3)));
                    } catch (Exception e) {
                        CurrencyHelper.showError(binding.getRoot(), "Gagal memuat detail produk");
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                binding.btnSave.setEnabled(true);
                CurrencyHelper.showError(binding.getRoot(), getString(R.string.tidak_ada_koneksi));
            }
        });
    }

    private void saveProduct() {
        String code = binding.etCode.getText().toString().trim();
        String name = binding.etName.getText().toString().trim();
        String unit = binding.etUnit.getText().toString().trim();
        String stockStr = binding.etStock.getText().toString().trim();
        String costPriceStr = binding.etCostPrice.getText().toString().trim();
        String priceUserStr = binding.etPriceUser.getText().toString().trim();
        String priceFreelancerStr = binding.etPriceFreelancer.getText().toString().trim();
        String priceGrosirStr = binding.etPriceGrosir.getText().toString().trim();

        // Validate
        boolean valid = true;
        if (TextUtils.isEmpty(code)) {
            binding.tilCode.setError("Kode produk wajib diisi");
            valid = false;
        } else {
            binding.tilCode.setError(null);
        }

        if (TextUtils.isEmpty(name)) {
            binding.tilName.setError("Nama produk wajib diisi");
            valid = false;
        } else {
            binding.tilName.setError(null);
        }

        if (TextUtils.isEmpty(unit)) {
            binding.tilUnit.setError("Satuan wajib diisi");
            valid = false;
        } else {
            binding.tilUnit.setError(null);
        }

        if (TextUtils.isEmpty(stockStr)) {
            binding.tilStock.setError("Stok wajib diisi");
            valid = false;
        } else {
            binding.tilStock.setError(null);
        }

        if (TextUtils.isEmpty(costPriceStr)) {
            binding.tilCostPrice.setError("Harga modal wajib diisi");
            valid = false;
        } else {
            binding.tilCostPrice.setError(null);
        }

        if (TextUtils.isEmpty(priceUserStr)) {
            binding.tilPriceUser.setError("Harga USER wajib diisi");
            valid = false;
        } else {
            binding.tilPriceUser.setError(null);
        }

        if (TextUtils.isEmpty(priceFreelancerStr)) {
            binding.tilPriceFreelancer.setError("Harga Freelancer wajib diisi");
            valid = false;
        } else {
            binding.tilPriceFreelancer.setError(null);
        }

        if (TextUtils.isEmpty(priceGrosirStr)) {
            binding.tilPriceGrosir.setError("Harga Grosir wajib diisi");
            valid = false;
        } else {
            binding.tilPriceGrosir.setError(null);
        }

        if (!valid) return;

        // Build Request Body
        JsonObject body = new JsonObject();
        body.addProperty("product_code", code);
        body.addProperty("product_name", name);
        body.addProperty("unit", unit);
        body.addProperty("cost_price", Double.parseDouble(costPriceStr));
        body.addProperty("stock", Integer.parseInt(stockStr));
        body.addProperty("status", "00");

        // Prices array
        JsonArray pricesArray = new JsonArray();
        
        JsonObject priceUser = new JsonObject();
        priceUser.addProperty("customer_group_id", 1);
        priceUser.addProperty("selling_price", Double.parseDouble(priceUserStr));
        pricesArray.add(priceUser);

        JsonObject priceFreelancer = new JsonObject();
        priceFreelancer.addProperty("customer_group_id", 2);
        priceFreelancer.addProperty("selling_price", Double.parseDouble(priceFreelancerStr));
        pricesArray.add(priceFreelancer);

        JsonObject priceGrosir = new JsonObject();
        priceGrosir.addProperty("customer_group_id", 3);
        priceGrosir.addProperty("selling_price", Double.parseDouble(priceGrosirStr));
        pricesArray.add(priceGrosir);

        body.add("prices", pricesArray);

        setLoading(true);

        Callback<JsonObject> callback = new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                setLoading(false);
                if (response.isSuccessful()) {
                    CurrencyHelper.showToast(ProductFormActivity.this, getString(R.string.berhasil_disimpan));
                    finish();
                } else {
                    String errorMsg = "Gagal menyimpan produk";
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
        };

        if (isEditMode) {
            apiService.updateProduct(productId, body).enqueue(callback);
        } else {
            apiService.createProduct(body).enqueue(callback);
        }
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnSave.setEnabled(!loading);
    }
}
