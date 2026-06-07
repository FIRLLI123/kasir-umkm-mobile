package com.example.kasirumkm2.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.kasirumkm2.R;
import com.example.kasirumkm2.adapter.StockBulkInAdapter;
import com.example.kasirumkm2.api.ApiClient;
import com.example.kasirumkm2.api.ApiService;
import com.example.kasirumkm2.data.Product;
import com.example.kasirumkm2.databinding.ActivityStockBulkInBinding;
import com.example.kasirumkm2.session.SessionManager;
import com.example.kasirumkm2.utils.CurrencyHelper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StockBulkInActivity extends AppCompatActivity {

    private ActivityStockBulkInBinding binding;
    private ApiService apiService;
    private StockBulkInAdapter adapter;
    private final List<Product> productList = new ArrayList<>();
    private final Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStockBulkInBinding.inflate(getLayoutInflater());
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
        setupActionButtons();

        // Load active products list first, then initialize RecyclerView
        loadProductsAndInit();
    }

    private void setupToolbar() {
        binding.btnBack.setOnClickListener(v -> finish());
    }

    private void setupActionButtons() {
        binding.btnAddRow.setOnClickListener(v -> {
            if (adapter != null) {
                adapter.addRow();
            }
        });

        binding.btnSave.setOnClickListener(v -> {
            if (adapter != null) {
                validateAndSubmit();
            }
        });
    }

    private void loadProductsAndInit() {
        setLoading(true);

        Map<String, String> params = new HashMap<>();
        params.put("per_page", "all");

        apiService.getProductsFiltered(params).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonArray dataArray = extractDataArray(response.body());
                        productList.clear();
                        for (int i = 0; i < dataArray.size(); i++) {
                            Product p = gson.fromJson(dataArray.get(i), Product.class);
                            // Only add active products
                            if (p.isActive()) {
                                productList.add(p);
                            }
                        }

                        setupRecyclerView();
                    } catch (Exception e) {
                        CurrencyHelper.showError(binding.getRoot(), "Gagal memproses daftar produk");
                    }
                } else if (response.code() == 401) {
                    handleUnauthorized();
                } else {
                    CurrencyHelper.showError(binding.getRoot(), "Gagal memuat katalog produk");
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                setLoading(false);
                CurrencyHelper.showError(binding.getRoot(), "Tidak ada koneksi internet");
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new StockBulkInAdapter(productList, position -> {
            if (adapter.getItemCount() <= 1) {
                CurrencyHelper.showError(binding.getRoot(), "Minimal harus ada 1 baris input");
            } else {
                adapter.removeRow(position);
            }
        });

        binding.rvBulkList.setLayoutManager(new LinearLayoutManager(this));
        binding.rvBulkList.setAdapter(adapter);

        // Pre-populate with 2 empty rows as per UX rules
        List<StockBulkInAdapter.BulkStockItem> initialRows = new ArrayList<>();
        initialRows.add(new StockBulkInAdapter.BulkStockItem());
        initialRows.add(new StockBulkInAdapter.BulkStockItem());
        adapter.setItems(initialRows);
    }

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

    private void validateAndSubmit() {
        List<StockBulkInAdapter.BulkStockItem> currentItems = adapter.getItems();
        boolean hasError = false;

        for (int i = 0; i < currentItems.size(); i++) {
            StockBulkInAdapter.BulkStockItem item = currentItems.get(i);

            // Product selection check
            if (item.productId == -1) {
                item.productError = "Pilih produk terlebih dahulu";
                hasError = true;
            } else {
                item.productError = null;
            }

            // Quantity validation (positive number > 0)
            if (TextUtils.isEmpty(item.qtyAdd.trim())) {
                item.qtyError = "Jumlah wajib diisi";
                hasError = true;
            } else {
                try {
                    int qty = Integer.parseInt(item.qtyAdd.trim());
                    if (qty <= 0) {
                        item.qtyError = "Jumlah harus lebih dari 0";
                        hasError = true;
                    } else {
                        item.qtyError = null;
                    }
                } catch (NumberFormatException e) {
                    item.qtyError = "Format jumlah salah";
                    hasError = true;
                }
            }
        }

        if (hasError) {
            adapter.notifyDataSetChanged();
            CurrencyHelper.showError(binding.getRoot(), "Harap lengkapi/perbaiki form sebelum menyimpan");
            return;
        }

        submitBulkData(currentItems);
    }

    private void submitBulkData(List<StockBulkInAdapter.BulkStockItem> validatedItems) {
        setLoading(true);

        JsonObject body = new JsonObject();
        
        // Format mutation date as today
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String mutationDate = sdf.format(new Date());
        body.addProperty("mutation_date", mutationDate);

        JsonArray itemsArray = new JsonArray();
        for (StockBulkInAdapter.BulkStockItem item : validatedItems) {
            JsonObject itemObj = new JsonObject();
            itemObj.addProperty("product_id", item.productId);
            itemObj.addProperty("qty", Integer.parseInt(item.qtyAdd.trim()));
            if (item.note != null && !item.note.trim().isEmpty()) {
                itemObj.addProperty("note", item.note.trim());
            } else {
                itemObj.addProperty("note", "Tambah stok massal");
            }
            itemsArray.add(itemObj);
        }
        body.add("items", itemsArray);

        apiService.bulkStockIn(body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonObject resBody = response.body();
                        JsonObject dataObj = resBody.getAsJsonObject("data");
                        JsonObject summary = dataObj.getAsJsonObject("summary");

                        int successCount = summary.get("success").getAsInt();
                        int failedCount = summary.get("failed").getAsInt();

                        JsonArray itemsRes = dataObj.getAsJsonArray("items");
                        List<StockBulkInAdapter.BulkStockItem> failedItemsList = new ArrayList<>();

                        for (int i = 0; i < validatedItems.size(); i++) {
                            StockBulkInAdapter.BulkStockItem originalItem = validatedItems.get(i);
                            JsonObject itemResObj = itemsRes.get(i).getAsJsonObject();
                            boolean itemSuccess = itemResObj.get("success").getAsBoolean();

                            if (!itemSuccess) {
                                JsonObject errorsObj = itemResObj.has("errors") && !itemResObj.get("errors").isJsonNull() 
                                        ? itemResObj.getAsJsonObject("errors") : null;
                                
                                originalItem.productError = getFirstError(errorsObj, "product_id");
                                originalItem.qtyError = getFirstError(errorsObj, "qty");
                                originalItem.noteError = getFirstError(errorsObj, "note");

                                if (originalItem.productError == null && itemResObj.has("message")) {
                                    originalItem.productError = itemResObj.get("message").getAsString();
                                }

                                failedItemsList.add(originalItem);
                            }
                        }

                        if (failedItemsList.isEmpty()) {
                            Toast.makeText(StockBulkInActivity.this, "Seluruh penambahan stok (" + successCount + " produk) berhasil disimpan!", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            adapter.setItems(failedItemsList);
                            CurrencyHelper.showError(binding.getRoot(), successCount + " item berhasil ditambahkan, " + failedCount + " gagal.");
                        }

                    } catch (Exception e) {
                        Toast.makeText(StockBulkInActivity.this, "Penambahan stok berhasil disimpan", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                } else {
                    String errorMsg = "Terjadi kesalahan saat menyimpan penambahan stok";
                    try {
                        if (response.errorBody() != null) {
                            JsonObject err = new JsonParser().parse(response.errorBody().string()).getAsJsonObject();
                            if (err.has("message")) errorMsg = err.get("message").getAsString();
                        }
                    } catch (Exception e) {}
                    CurrencyHelper.showError(binding.getRoot(), errorMsg);
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                setLoading(false);
                CurrencyHelper.showError(binding.getRoot(), "Tidak ada koneksi internet");
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

    private void handleUnauthorized() {
        new SessionManager(this).clearSession();
        ApiClient.resetClient();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
