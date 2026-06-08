package com.example.kasirumkm2.ui;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.kasirumkm2.R;
import com.example.kasirumkm2.api.ApiClient;
import com.example.kasirumkm2.api.ApiService;
import com.example.kasirumkm2.data.Customer;
import com.example.kasirumkm2.data.Product;
import com.example.kasirumkm2.databinding.ActivityBackupBinding;
import com.example.kasirumkm2.utils.CsvExportHelper;
import com.example.kasirumkm2.utils.CurrencyHelper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

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

public class BackupActivity extends AppCompatActivity {

    private ActivityBackupBinding binding;
    private ApiService apiService;
    private final Gson gson = new Gson();
    
    private static final int PERMISSION_REQUEST_CODE = 1002;
    private int pendingExportType = -1; // 1: Sales, 2: Products, 3: Customers, 4: SalesSummary, 5: Margin, 6: TopProducts
    private int userGroupId = 1; // Default customer group ID for user pricing

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBackupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = ApiClient.getApiService(this);

        setupToolbar();
        setupListeners();
        loadUserGroupId();
    }

    private void setupToolbar() {
        binding.btnBack.setOnClickListener(v -> finish());
    }

    private void setupListeners() {
        binding.btnExportSales.setOnClickListener(v -> handleExportClick(1));
        binding.btnExportProducts.setOnClickListener(v -> handleExportClick(2));
        binding.btnExportCustomers.setOnClickListener(v -> handleExportClick(3));
        binding.btnExportSalesSummary.setOnClickListener(v -> handleExportClick(4));
        binding.btnExportMargin.setOnClickListener(v -> handleExportClick(5));
        binding.btnExportTopProducts.setOnClickListener(v -> handleExportClick(6));
    }

    private void handleExportClick(int exportType) {
        pendingExportType = exportType;
        if (checkStoragePermission()) {
            executeExport(exportType);
        } else {
            requestStoragePermission();
        }
    }

    private boolean checkStoragePermission() {
        // Scoped storage on Q (API 29+) does not need permissions for MediaStore.Downloads
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return true;
        }
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (pendingExportType != -1) {
                    executeExport(pendingExportType);
                }
            } else {
                Toast.makeText(this, "Izin penyimpanan dibutuhkan untuk mengunduh data di Android versi ini.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void loadUserGroupId() {
        apiService.getCustomerGroups().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonArray data = response.body().getAsJsonArray("data");
                        for (int i = 0; i < data.size(); i++) {
                            JsonObject obj = data.get(i).getAsJsonObject();
                            String code = obj.get("group_code").getAsString();
                            if ("USER".equalsIgnoreCase(code)) {
                                userGroupId = obj.get("id").getAsInt();
                                break;
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {}
        });
    }

    private void executeExport(int exportType) {
        switch (exportType) {
            case 1:
                exportSales();
                break;
            case 2:
                exportProducts();
                break;
            case 3:
                exportCustomers();
                break;
            case 4:
                exportSalesSummary();
                break;
            case 5:
                exportMarginReport();
                break;
            case 6:
                exportTopProducts();
                break;
        }
    }

    private void setProgress(boolean show, String message) {
        if (show) {
            binding.tvProgressMessage.setText(message);
            binding.layoutProgressOverlay.setVisibility(View.VISIBLE);
        } else {
            binding.layoutProgressOverlay.setVisibility(View.GONE);
        }
    }

    private String generateFileName(String prefix) {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        return prefix + "_" + timestamp + ".csv";
    }

    private void exportSales() {
        setProgress(true, "Mengunduh data transaksi dari server...");

        // Request all sales for export
        Map<String, String> params = new HashMap<>();
        params.put("per_page", "all");

        apiService.getSalesFiltered(params).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                setProgress(false, "");
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonArray dataArray = extractDataArray(response.body());
                        if (dataArray == null || dataArray.size() == 0) {
                            Toast.makeText(BackupActivity.this, "Tidak ada data transaksi untuk diekspor.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String csvContent = CsvExportHelper.generateSalesCsv(dataArray);
                        String fileName = generateFileName("transaksi_backup");
                        
                        saveAndShowResult(csvContent, fileName);
                    } catch (Exception e) {
                        e.printStackTrace();
                        showErrorDialog("Format data transaksi tidak valid.");
                    }
                } else if (response.code() == 401) {
                    handleUnauthorized();
                } else {
                    showErrorDialog(getString(R.string.terjadi_kesalahan));
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                setProgress(false, "");
                showErrorDialog(getString(R.string.tidak_ada_koneksi));
            }
        });
    }

    private void exportProducts() {
        setProgress(true, "Mengunduh data katalog produk dari server...");

        // Request all products for export
        Map<String, String> params = new HashMap<>();
        params.put("per_page", "all");

        apiService.getProductsFiltered(params).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                setProgress(false, "");
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonArray dataArray = extractDataArray(response.body());
                        if (dataArray == null || dataArray.size() == 0) {
                            Toast.makeText(BackupActivity.this, "Tidak ada data produk untuk diekspor.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        List<Product> products = new ArrayList<>();
                        for (int i = 0; i < dataArray.size(); i++) {
                            products.add(gson.fromJson(dataArray.get(i), Product.class));
                        }

                        String csvContent = CsvExportHelper.generateProductsCsv(products, userGroupId);
                        String fileName = generateFileName("produk_backup");

                        saveAndShowResult(csvContent, fileName);
                    } catch (Exception e) {
                        e.printStackTrace();
                        showErrorDialog("Format data produk tidak valid.");
                    }
                } else if (response.code() == 401) {
                    handleUnauthorized();
                } else {
                    showErrorDialog(getString(R.string.terjadi_kesalahan));
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                setProgress(false, "");
                showErrorDialog(getString(R.string.tidak_ada_koneksi));
            }
        });
    }

    private void exportCustomers() {
        setProgress(true, "Mengunduh data pelanggan dari server...");

        // Request all customers for export
        Map<String, String> params = new HashMap<>();
        params.put("per_page", "all");

        apiService.getCustomersFiltered(params).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                setProgress(false, "");
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonArray dataArray = extractDataArray(response.body());
                        if (dataArray == null || dataArray.size() == 0) {
                            Toast.makeText(BackupActivity.this, "Tidak ada data customer untuk diekspor.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        List<Customer> customers = new ArrayList<>();
                        for (int i = 0; i < dataArray.size(); i++) {
                            customers.add(gson.fromJson(dataArray.get(i), Customer.class));
                        }

                        String csvContent = CsvExportHelper.generateCustomersCsv(customers);
                        String fileName = generateFileName("customer_backup");

                        saveAndShowResult(csvContent, fileName);
                    } catch (Exception e) {
                        e.printStackTrace();
                        showErrorDialog("Format data customer tidak valid.");
                    }
                } else if (response.code() == 401) {
                    handleUnauthorized();
                } else {
                    showErrorDialog(getString(R.string.terjadi_kesalahan));
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                setProgress(false, "");
                showErrorDialog(getString(R.string.tidak_ada_koneksi));
            }
        });
    }

    // ─── ANALYTIC REPORT EXPORTS ────────────────────────────────────────────────

    private void exportSalesSummary() {
        setProgress(true, getString(R.string.export_loading_summary));

        Map<String, String> params = new HashMap<>();
        params.put("per_page", "all");

        apiService.getSalesFiltered(params).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                setProgress(false, "");
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonArray dataArray = extractDataArray(response.body());
                        if (dataArray == null || dataArray.size() == 0) {
                            Toast.makeText(BackupActivity.this, "Tidak ada data transaksi untuk direkapitulasi.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String csvContent = CsvExportHelper.generateSalesSummaryCsv(dataArray);
                        String fileName = generateFileName("rekapan_penjualan");
                        saveAndShowResult(csvContent, fileName);
                    } catch (Exception e) {
                        e.printStackTrace();
                        showErrorDialog("Format data tidak valid.");
                    }
                } else if (response.code() == 401) {
                    handleUnauthorized();
                } else {
                    showErrorDialog(getString(R.string.terjadi_kesalahan));
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                setProgress(false, "");
                showErrorDialog(getString(R.string.tidak_ada_koneksi));
            }
        });
    }

    private void exportMarginReport() {
        setProgress(true, getString(R.string.export_loading_margin));

        // Need full sales WITH their detail items for margin calculation
        Map<String, String> params = new HashMap<>();
        params.put("per_page", "all");

        apiService.getSalesFiltered(params).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                setProgress(false, "");
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonArray dataArray = extractDataArray(response.body());
                        if (dataArray == null || dataArray.size() == 0) {
                            Toast.makeText(BackupActivity.this, "Tidak ada data transaksi untuk analisis margin.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String csvContent = CsvExportHelper.generateMarginReportCsv(dataArray);
                        String fileName = generateFileName("laporan_margin");
                        saveAndShowResult(csvContent, fileName);
                    } catch (Exception e) {
                        e.printStackTrace();
                        showErrorDialog("Format data tidak valid.");
                    }
                } else if (response.code() == 401) {
                    handleUnauthorized();
                } else {
                    showErrorDialog(getString(R.string.terjadi_kesalahan));
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                setProgress(false, "");
                showErrorDialog(getString(R.string.tidak_ada_koneksi));
            }
        });
    }

    private void exportTopProducts() {
        setProgress(true, getString(R.string.export_loading_products));

        // Use reports/products endpoint — it already provides qty_sold & total_sales per product
        Map<String, String> params = new HashMap<>();
        // No date filter → fetch all-time ranking
        params.put("per_page", "all");

        apiService.getReportProducts(params).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                setProgress(false, "");
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        // reports/products returns {"data": [...]}
                        JsonArray dataArray;
                        if (response.body().has("data") && response.body().get("data").isJsonArray()) {
                            dataArray = response.body().getAsJsonArray("data");
                        } else {
                            dataArray = extractDataArray(response.body());
                        }

                        if (dataArray == null || dataArray.size() == 0) {
                            Toast.makeText(BackupActivity.this, "Belum ada data produk terjual.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String csvContent = CsvExportHelper.generateProductSalesReportCsv(dataArray);
                        String fileName = generateFileName("ranking_produk");
                        saveAndShowResult(csvContent, fileName);
                    } catch (Exception e) {
                        e.printStackTrace();
                        showErrorDialog("Format data tidak valid.");
                    }
                } else if (response.code() == 401) {
                    handleUnauthorized();
                } else {
                    showErrorDialog(getString(R.string.terjadi_kesalahan));
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                setProgress(false, "");
                showErrorDialog(getString(R.string.tidak_ada_koneksi));
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

    private void saveAndShowResult(String csvContent, String fileName) {
        // Save copy to cache directory for share/open purposes
        Uri cacheUri = CsvExportHelper.saveToCache(this, csvContent, fileName);
        // Save copy to public downloads folder
        boolean savedToDownloads = CsvExportHelper.saveToDownloads(this, csvContent, fileName);

        if (savedToDownloads && cacheUri != null) {
            showSuccessDialog(fileName, cacheUri);
        } else {
            showErrorDialog("Gagal menyimpan file ke penyimpanan perangkat.");
        }
    }

    private void showSuccessDialog(String fileName, Uri cacheUri) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("🎉 Ekspor Berhasil!")
                .setMessage("Data berhasil diunduh dan disimpan di folder Downloads dengan nama:\n\n" + fileName)
                .setPositiveButton("Bagikan", (d, which) -> shareFile(cacheUri))
                .setNeutralButton("Buka File", (d, which) -> openFile(cacheUri))
                .setNegativeButton("Tutup", null)
                .create();
        
        dialog.show();
        
        // Premium button colors alignment
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColor(R.color.primary));
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(getColor(R.color.success_green));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getColor(R.color.text_gray));
    }

    private void shareFile(Uri uri) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/csv");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Bagikan file backup"));
    }

    private void openFile(Uri uri) {
        Intent openIntent = new Intent(Intent.ACTION_VIEW);
        openIntent.setDataAndType(uri, "text/csv");
        openIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(openIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Tidak ada aplikasi spreadsheet (seperti Excel/Sheets) yang ditemukan untuk membuka file CSV ini.", Toast.LENGTH_LONG).show();
        }
    }

    private void showErrorDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Gagal Ekspor")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void handleUnauthorized() {
        Toast.makeText(this, getString(R.string.sesi_berakhir), Toast.LENGTH_SHORT).show();
        new com.example.kasirumkm2.session.SessionManager(this).clearSession();
        ApiClient.resetClient();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
