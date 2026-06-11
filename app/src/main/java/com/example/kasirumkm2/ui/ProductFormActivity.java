package com.example.kasirumkm2.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import androidx.appcompat.app.AppCompatActivity;

import com.example.kasirumkm2.R;
import com.example.kasirumkm2.session.SessionManager;
import com.example.kasirumkm2.api.ApiClient;
import com.example.kasirumkm2.api.ApiService;
import com.example.kasirumkm2.data.Product;
import com.example.kasirumkm2.data.ProductPrice;
import com.example.kasirumkm2.databinding.ActivityProductFormBinding;
import com.example.kasirumkm2.utils.AirinDialog;
import com.example.kasirumkm2.utils.CurrencyHelper;

import com.example.kasirumkm2.utils.NumberTextWatcher;
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

    private int userGroupId = 1;
    private int freelancerGroupId = 2;
    private int grosirGroupId = 3;

    private static final int PERMISSION_REQUEST_CAMERA = 101;

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(
            new ScanContract(),
            result -> {
                if (result.getContents() != null) {
                    binding.etCode.setText(result.getContents());
                    binding.tilCode.setError(null);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProductFormBinding.inflate(getLayoutInflater());
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

        // Check edit mode
        productId = getIntent().getIntExtra("product_id", -1);
        isEditMode = productId > 0;

        setupToolbar();
        setupListeners();

        binding.btnSave.setEnabled(false); // Disable until customer groups load

        if (isEditMode) {
            binding.tvTitle.setText("Edit Produk");
        }

        loadCustomerGroups();
    }

    private void loadCustomerGroups() {
        binding.progressBar.setVisibility(View.VISIBLE);
        apiService.getCustomerGroups().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                binding.progressBar.setVisibility(View.GONE);
                boolean success = false;
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
                        success = true;
                    } catch (Exception e) {
                        // ignore
                    }
                }
                
                if (success) {
                    binding.btnSave.setEnabled(true);
                    if (isEditMode) {
                        loadProductData();
                    }
                } else {
                    CurrencyHelper.showError(binding.getRoot(), "Gagal memuat customer groups. Silakan buka ulang halaman ini.");
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                CurrencyHelper.showError(binding.getRoot(), "Gagal memuat customer groups: " + t.getMessage());
            }
        });
    }

    private void setupToolbar() {
        binding.btnBack.setOnClickListener(v -> finish());
    }

    private void setupListeners() {
        binding.btnSave.setOnClickListener(v -> saveProduct());
        binding.etCostPrice.addTextChangedListener(new NumberTextWatcher(binding.etCostPrice));
        binding.etPriceUser.addTextChangedListener(new NumberTextWatcher(binding.etPriceUser));
        binding.etPriceFreelancer.addTextChangedListener(new NumberTextWatcher(binding.etPriceFreelancer));
        binding.etPriceGrosir.addTextChangedListener(new NumberTextWatcher(binding.etPriceGrosir));
        binding.tilCode.setEndIconOnClickListener(v -> checkCameraPermissionAndScan());
    }

    private void checkCameraPermissionAndScan() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startScan();
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    PERMISSION_REQUEST_CAMERA
            );
        }
    }

    private void startScan() {
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES);
        options.setPrompt("Scan Barcode");
        options.setCameraId(0);
        options.setBeepEnabled(true);
        options.setBarcodeImageEnabled(false);
        options.setCaptureActivity(PortraitCaptureActivity.class);
        options.setOrientationLocked(true);
        barcodeLauncher.launch(options);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScan();
            } else {
                android.widget.Toast.makeText(this, "Izin kamera dibutuhkan untuk scan barcode", android.widget.Toast.LENGTH_SHORT).show();
            }
        }
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
                        binding.etCostPrice.setText(CurrencyHelper.formatNumber(product.getCostPrice()));

                        // Prices
                        binding.etPriceUser.setText(CurrencyHelper.formatNumber(product.getSellingPrice(userGroupId)));
                        binding.etPriceFreelancer.setText(CurrencyHelper.formatNumber(product.getSellingPrice(freelancerGroupId)));
                        binding.etPriceGrosir.setText(CurrencyHelper.formatNumber(product.getSellingPrice(grosirGroupId)));
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
        body.addProperty("cost_price", CurrencyHelper.parseDouble(costPriceStr));
        body.addProperty("stock", Integer.parseInt(stockStr));
        body.addProperty("status", "00");

        // Prices array
        JsonArray pricesArray = new JsonArray();
        
        JsonObject priceUser = new JsonObject();
        priceUser.addProperty("customer_group_id", userGroupId);
        priceUser.addProperty("selling_price", CurrencyHelper.parseDouble(priceUserStr));
        pricesArray.add(priceUser);

        JsonObject priceFreelancer = new JsonObject();
        priceFreelancer.addProperty("customer_group_id", freelancerGroupId);
        priceFreelancer.addProperty("selling_price", CurrencyHelper.parseDouble(priceFreelancerStr));
        pricesArray.add(priceFreelancer);

        JsonObject priceGrosir = new JsonObject();
        priceGrosir.addProperty("customer_group_id", grosirGroupId);
        priceGrosir.addProperty("selling_price", CurrencyHelper.parseDouble(priceGrosirStr));
        pricesArray.add(priceGrosir);

        body.add("prices", pricesArray);

        setLoading(true);

        Callback<JsonObject> callback = new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                setLoading(false);
                if (response.isSuccessful()) {
                    String action = isEditMode ? "diperbarui" : "ditambahkan";
                    AirinDialog.showSuccess(
                            ProductFormActivity.this,
                            "Produk Berhasil " + (isEditMode ? "Diupdate" : "Ditambah") + "! 🎉",
                            "Produk sudah " + action + " ke katalog~\nAirin siap bantu jual! 🛋️",
                            () -> finish()
                    );
                } else {
                    String errorMsg = "Gagal menyimpan produk";
                    try {
                        if (response.errorBody() != null) {
                            JsonObject err = new com.google.gson.JsonParser()
                                    .parse(response.errorBody().string()).getAsJsonObject();
                            if (err.has("message")) errorMsg = err.get("message").getAsString();
                        }
                    } catch (Exception e) {}
                    AirinDialog.showError(
                            ProductFormActivity.this,
                            "Gagal Simpan Produk 😢",
                            errorMsg + "\nCoba periksa datanya ya~",
                            null
                    );
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                setLoading(false);
                AirinDialog.showOffline(ProductFormActivity.this, null);
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
