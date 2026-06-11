package com.example.kasirumkm2.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.kasirumkm2.R;
import com.example.kasirumkm2.adapter.ProductBulkAdapter;
import com.example.kasirumkm2.api.ApiClient;
import com.example.kasirumkm2.api.ApiService;
import com.example.kasirumkm2.databinding.ActivityProductBulkBinding;
import com.example.kasirumkm2.session.SessionManager;
import com.example.kasirumkm2.utils.AirinDialog;
import com.example.kasirumkm2.utils.CurrencyHelper;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductBulkActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CAMERA = 101;

    private ActivityProductBulkBinding binding;
    private ApiService apiService;
    private ProductBulkAdapter adapter;

    private int activeScanPosition = -1;

    // ZXing Launcher
    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(
            new ScanContract(),
            result -> {
                if (result.getContents() != null && activeScanPosition >= 0) {
                    adapter.updateProductCode(activeScanPosition, result.getContents());
                }
                activeScanPosition = -1;
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProductBulkBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SessionManager sessionManager = new SessionManager(this);
        if (sessionManager.isSubscriptionExpired()) {
            Toast.makeText(this, "Masa aktif langganan Anda telah habis. Akses dibatasi.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, SubscriptionActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        if ("trial".equalsIgnoreCase(sessionManager.getSubscriptionStatus())) {
            Toast.makeText(this, "Fitur ini tidak tersedia untuk paket Trial. Silakan aktifkan langganan Premium Anda.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, SubscriptionActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        apiService = ApiClient.getApiService(this);

        setupToolbar();
        setupRecyclerView();
        setupActionButtons();

        // Load initial 3 rows
        List<ProductBulkAdapter.BulkItem> initialList = new ArrayList<>();
        initialList.add(new ProductBulkAdapter.BulkItem());
        initialList.add(new ProductBulkAdapter.BulkItem());
        initialList.add(new ProductBulkAdapter.BulkItem());
        adapter.setItems(initialList);
    }

    private void setupToolbar() {
        binding.btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new ProductBulkAdapter(
                position -> {
                    // onDelete
                    if (adapter.getItemCount() <= 1) {
                        CurrencyHelper.showError(binding.getRoot(), "Minimal harus ada 1 baris input");
                    } else {
                        adapter.removeRow(position);
                    }
                },
                position -> {
                    // onScanRequested
                    activeScanPosition = position;
                    checkCameraPermissionAndScan();
                }
        );
        binding.rvBulkList.setLayoutManager(new LinearLayoutManager(this));
        binding.rvBulkList.setAdapter(adapter);
    }

    private void setupActionButtons() {
        binding.btnAddRow.setOnClickListener(v -> adapter.addRow());
        binding.btnSave.setOnClickListener(v -> validateAndSubmit());
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
        options.setPrompt("Scan Barcode Produk");
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
                Toast.makeText(this, "Izin kamera dibutuhkan untuk scan barcode", Toast.LENGTH_SHORT).show();
                activeScanPosition = -1;
            }
        }
    }

    private void validateAndSubmit() {
        List<ProductBulkAdapter.BulkItem> currentItems = adapter.getItems();
        boolean hasError = false;

        for (int i = 0; i < currentItems.size(); i++) {
            ProductBulkAdapter.BulkItem item = currentItems.get(i);

            // Local validation
            if (TextUtils.isEmpty(item.productName.trim())) {
                item.nameError = "Nama produk wajib diisi";
                hasError = true;
            } else {
                item.nameError = null;
            }

            // Price USER (Wajib)
            if (TextUtils.isEmpty(item.priceUser.trim())) {
                item.priceUserError = "Harga USER wajib diisi";
                hasError = true;
            } else {
                try {
                    double sell = CurrencyHelper.parseDouble(item.priceUser.trim());
                    if (sell < 0) {
                        item.priceUserError = "Harga USER tidak boleh kurang dari 0";
                        hasError = true;
                    } else {
                        item.priceUserError = null;
                    }
                } catch (NumberFormatException e) {
                    item.priceUserError = "Format harga salah";
                    hasError = true;
                }
            }

            // Price FREELANCER (Opsional)
            if (!TextUtils.isEmpty(item.priceFreelancer.trim())) {
                try {
                    double freelancerPrice = CurrencyHelper.parseDouble(item.priceFreelancer.trim());
                    if (freelancerPrice < 0) {
                        item.priceFreelancerError = "Harga tidak boleh kurang dari 0";
                        hasError = true;
                    } else {
                        item.priceFreelancerError = null;
                    }
                } catch (NumberFormatException e) {
                    item.priceFreelancerError = "Format harga salah";
                    hasError = true;
                }
            } else {
                item.priceFreelancerError = null;
            }

            // Price GROSIR (Opsional)
            if (!TextUtils.isEmpty(item.priceGrosir.trim())) {
                try {
                    double grosirPrice = CurrencyHelper.parseDouble(item.priceGrosir.trim());
                    if (grosirPrice < 0) {
                        item.priceGrosirError = "Harga tidak boleh kurang dari 0";
                        hasError = true;
                    } else {
                        item.priceGrosirError = null;
                    }
                } catch (NumberFormatException e) {
                    item.priceGrosirError = "Format harga salah";
                    hasError = true;
                }
            } else {
                item.priceGrosirError = null;
            }

            if (!TextUtils.isEmpty(item.costPrice.trim())) {
                try {
                    double cost = CurrencyHelper.parseDouble(item.costPrice.trim());
                    if (cost < 0) {
                        item.costPriceError = "Harga modal tidak boleh kurang dari 0";
                        hasError = true;
                    } else {
                        item.costPriceError = null;
                    }
                } catch (NumberFormatException e) {
                    item.costPriceError = "Format harga modal salah";
                    hasError = true;
                }
            } else {
                item.costPriceError = null;
            }

            if (!TextUtils.isEmpty(item.stock.trim())) {
                try {
                    double st = Double.parseDouble(item.stock.trim());
                    if (st < 0) {
                        item.stockError = "Stok tidak boleh kurang dari 0";
                        hasError = true;
                    } else {
                        item.stockError = null;
                    }
                } catch (NumberFormatException e) {
                    item.stockError = "Format stok salah";
                    hasError = true;
                }
            } else {
                item.stockError = null;
            }
        }

        if (hasError) {
            adapter.notifyDataSetChanged();
            CurrencyHelper.showError(binding.getRoot(), "Harap perbaiki error pada beberapa baris input");
            return;
        }

        submitBulkData(currentItems);
    }

    private void submitBulkData(List<ProductBulkAdapter.BulkItem> validatedItems) {
        setLoading(true);

        JsonObject body = new JsonObject();
        JsonArray itemsArray = new JsonArray();

        for (ProductBulkAdapter.BulkItem item : validatedItems) {
            JsonObject itemObj = new JsonObject();
            itemObj.addProperty("product_name", item.productName.trim());
            
            String code = item.productCode.trim();
            itemObj.addProperty("product_code", TextUtils.isEmpty(code) ? null : code);
            
            String unit = item.unit.trim();
            itemObj.addProperty("unit", TextUtils.isEmpty(unit) ? null : unit);

            double costPrice = 0;
            if (!TextUtils.isEmpty(item.costPrice.trim())) {
                costPrice = CurrencyHelper.parseDouble(item.costPrice.trim());
            }
            itemObj.addProperty("cost_price", costPrice);

            double stock = 0;
            if (!TextUtils.isEmpty(item.stock.trim())) {
                stock = Double.parseDouble(item.stock.trim());
            }
            itemObj.addProperty("stock", stock);

            // Create prices array based on input
            JsonArray pricesArray = new JsonArray();
            
            // USER price is always added (validated as required)
            JsonObject priceUserObj = new JsonObject();
            priceUserObj.addProperty("customer_group_code", "USER");
            priceUserObj.addProperty("selling_price", CurrencyHelper.parseDouble(item.priceUser.trim()));
            pricesArray.add(priceUserObj);

            // FREELANCER price (optional)
            if (!TextUtils.isEmpty(item.priceFreelancer.trim())) {
                JsonObject priceFreelancerObj = new JsonObject();
                priceFreelancerObj.addProperty("customer_group_code", "FREELANCER");
                priceFreelancerObj.addProperty("selling_price", CurrencyHelper.parseDouble(item.priceFreelancer.trim()));
                pricesArray.add(priceFreelancerObj);
            }

            // GROSIR price (optional)
            if (!TextUtils.isEmpty(item.priceGrosir.trim())) {
                JsonObject priceGrosirObj = new JsonObject();
                priceGrosirObj.addProperty("customer_group_code", "GROSIR");
                priceGrosirObj.addProperty("selling_price", CurrencyHelper.parseDouble(item.priceGrosir.trim()));
                pricesArray.add(priceGrosirObj);
            }

            itemObj.add("prices", pricesArray);
            itemsArray.add(itemObj);
        }

        body.add("items", itemsArray);

        apiService.createProductsBulk(body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonObject resBody = response.body();
                        JsonObject dataObj = resBody.getAsJsonObject("data");
                        JsonObject summary = dataObj.getAsJsonObject("summary");

                        int total = summary.get("total").getAsInt();
                        int successCount = summary.get("success").getAsInt();
                        int failedCount = summary.get("failed").getAsInt();

                        JsonArray itemsRes = dataObj.getAsJsonArray("items");

                        List<ProductBulkAdapter.BulkItem> failedItemsList = new ArrayList<>();

                        for (int i = 0; i < validatedItems.size(); i++) {
                            ProductBulkAdapter.BulkItem originalItem = validatedItems.get(i);
                            JsonObject itemResObj = itemsRes.get(i).getAsJsonObject();
                            boolean itemSuccess = itemResObj.get("success").getAsBoolean();

                            if (!itemSuccess) {
                                // Extract validation errors from backend
                                JsonObject errorsObj = itemResObj.getAsJsonObject("errors");
                                originalItem.nameError = getFirstError(errorsObj, "product_name");
                                originalItem.codeError = getFirstError(errorsObj, "product_code");
                                originalItem.unitError = getFirstError(errorsObj, "unit");
                                originalItem.costPriceError = getFirstError(errorsObj, "cost_price");
                                originalItem.stockError = getFirstError(errorsObj, "stock");

                                // Map index-based validation errors from prices array
                                boolean hasFreelancer = !TextUtils.isEmpty(originalItem.priceFreelancer.trim());
                                boolean hasGrosir = !TextUtils.isEmpty(originalItem.priceGrosir.trim());

                                originalItem.priceUserError = getFirstError(errorsObj, "selling_price");
                                if (originalItem.priceUserError == null) {
                                    originalItem.priceUserError = getFirstError(errorsObj, "prices.0.selling_price");
                                }

                                if (hasFreelancer) {
                                    originalItem.priceFreelancerError = getFirstError(errorsObj, "prices.1.selling_price");
                                    if (hasGrosir) {
                                        originalItem.priceGrosirError = getFirstError(errorsObj, "prices.2.selling_price");
                                    }
                                } else if (hasGrosir) {
                                    originalItem.priceGrosirError = getFirstError(errorsObj, "prices.1.selling_price");
                                }

                                failedItemsList.add(originalItem);
                            }
                        }

                        if (failedItemsList.isEmpty()) {
                            // Semua berhasil — dialog Airin goodjob!
                            AirinDialog.showSuccess(
                                    ProductBulkActivity.this,
                                    "Semua Produk Tersimpan! 🎉",
                                    successCount + " produk berhasil ditambahkan ke katalog~\nAirin bangga sama kerja keras kamu! 🙌",
                                    () -> finish()
                            );
                        } else {
                            adapter.setItems(failedItemsList);
                            AirinDialog.showWarning(
                                    ProductBulkActivity.this,
                                    "Sebagian Berhasil ⚠️",
                                    successCount + " produk berhasil, " + failedCount + " gagal.\nPeriksa baris yang masih ada error ya~",
                                    null
                            );
                        }

                    } catch (Exception e) {
                        CurrencyHelper.showError(binding.getRoot(), "Gagal memproses respon server");
                    }
                } else {
                    String errorMsg = "Terjadi kesalahan saat menyimpan produk";
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
                AirinDialog.showOffline(ProductBulkActivity.this, null);
            }
        });
    }

    private String getFirstError(JsonObject errorsObj, String fieldKey) {
        if (errorsObj != null && errorsObj.has(fieldKey) && !errorsObj.get(fieldKey).isJsonNull()) {
            try {
                JsonArray arr = errorsObj.getAsJsonArray(fieldKey);
                if (arr != null && arr.size() > 0) {
                    return arr.get(0).getAsString();
                }
            } catch (Exception e) {
                // ignore
            }
        }
        return null;
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnSave.setEnabled(!loading);
        binding.btnAddRow.setEnabled(!loading);
    }
}
