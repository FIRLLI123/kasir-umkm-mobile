package com.example.kasirumkm2.ui;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.kasirumkm2.R;
import com.example.kasirumkm2.adapter.ImportErrorAdapter;
import com.example.kasirumkm2.api.ApiClient;
import com.example.kasirumkm2.api.ApiService;
import com.example.kasirumkm2.databinding.ActivityProductImportBinding;
import com.example.kasirumkm2.session.SessionManager;
import com.example.kasirumkm2.utils.CurrencyHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductImportActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_WRITE_STORAGE = 301;

    private ActivityProductImportBinding binding;
    private ApiService apiService;
    private ImportErrorAdapter adapter;

    // Mode: "create_products" or "stock_in_existing"
    private String currentMode = "create_products"; 

    private Uri selectedFileUri = null;
    private String selectedFileName = "";

    // File Picker Launcher
    private final ActivityResultLauncher<String> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedFileUri = uri;
                    selectedFileName = getFileName(uri);
                    binding.tvSelectedFileName.setText(selectedFileName);

                    long fileSize = getFileSize(uri);
                    binding.tvSelectedFileSize.setText(formatFileSize(fileSize));

                    binding.btnUpload.setEnabled(true);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProductImportBinding.inflate(getLayoutInflater());
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
        setupModeSelectors();
        setupRecyclerView();
        setupActionListeners();
    }

    private void setupToolbar() {
        binding.btnBack.setOnClickListener(v -> finish());
    }

    private void setupModeSelectors() {
        // Mode: Create Products Selected by default
        selectMode("create_products");

        binding.cardModeCreate.setOnClickListener(v -> selectMode("create_products"));
        binding.cardModeStock.setOnClickListener(v -> selectMode("stock_in_existing"));
    }

    private void selectMode(String mode) {
        currentMode = mode;
        if ("create_products".equals(mode)) {
            // Highlight Create card
            binding.cardModeCreate.setStrokeColor(ContextCompat.getColor(this, R.color.primary));
            binding.cardModeCreate.setStrokeWidth(getResources().getDimensionPixelSize(R.dimen.card_stroke_width_active));
            
            // Unhighlight Stock card
            binding.cardModeStock.setStrokeColor(ContextCompat.getColor(this, R.color.border));
            binding.cardModeStock.setStrokeWidth(getResources().getDimensionPixelSize(R.dimen.card_stroke_width_normal));

            // Set guide text
            binding.tvGuidelineContent.setText(
                    "• Kolom product_name & price_user WAJIB diisi.\n" +
                    "• Kolom product_code OPSIONAL (jika kosong, sistem akan generate otomatis).\n" +
                    "• Kolom price_freelancer & price_grosir OPSIONAL (jika kosong, otomatis mengikuti price_user).\n" +
                    "• Kolom unit, cost_price, & stock OPSIONAL."
            );
        } else {
            // Highlight Stock card
            binding.cardModeStock.setStrokeColor(ContextCompat.getColor(this, R.color.primary));
            binding.cardModeStock.setStrokeWidth(getResources().getDimensionPixelSize(R.dimen.card_stroke_width_active));
            
            // Unhighlight Create card
            binding.cardModeCreate.setStrokeColor(ContextCompat.getColor(this, R.color.border));
            binding.cardModeCreate.setStrokeWidth(getResources().getDimensionPixelSize(R.dimen.card_stroke_width_normal));

            // Set guide text
            binding.tvGuidelineContent.setText(
                    "• Kolom product_code & qty_stock_in WAJIB diisi.\n" +
                    "• Digunakan hanya untuk produk yang sudah terdaftar di sistem.\n" +
                    "• Jika kode produk tidak ditemukan, baris data tersebut akan gagal.\n" +
                    "• Mode ini TIDAK AKAN membuat produk baru."
            );
        }

        // Clear currently selected file when mode changes to avoid uploading wrong template
        clearSelectedFile();
        binding.layoutResults.setVisibility(View.GONE);
    }

    private void clearSelectedFile() {
        selectedFileUri = null;
        selectedFileName = "";
        binding.tvSelectedFileName.setText("Ketuk untuk memilih file Excel/CSV");
        binding.tvSelectedFileSize.setText("Mendukung format .xlsx dan .csv");
        binding.btnUpload.setEnabled(false);
    }

    private void setupRecyclerView() {
        adapter = new ImportErrorAdapter();
        binding.rvErrorList.setLayoutManager(new LinearLayoutManager(this));
        binding.rvErrorList.setAdapter(adapter);
    }

    private void setupActionListeners() {
        binding.btnDownloadTemplate.setOnClickListener(v -> checkPermissionAndDownloadTemplate());
        binding.cardFilePicker.setOnClickListener(v -> pickFile());
        binding.btnUpload.setOnClickListener(v -> uploadAndProcess());
    }

    private void checkPermissionAndDownloadTemplate() {
        // On Android Q and above, MediaStore allows writing to Downloads without permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            downloadTemplate();
        } else {
            // For Android Pie and below, check permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                downloadTemplate();
            } else {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_WRITE_STORAGE
                );
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_WRITE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                downloadTemplate();
            } else {
                // Try writing to app's external files directory as fallback
                downloadTemplateToAppDir();
            }
        }
    }

    private void downloadTemplate() {
        String assetName;
        String fileName;
        if ("create_products".equals(currentMode)) {
            assetName = "template_import_produk_baru.csv";
            fileName = "template_import_produk_baru.csv";
        } else {
            assetName = "template_import_tambah_stok.csv";
            fileName = "template_import_tambah_stok.csv";
        }

        try {
            InputStream is = getAssets().open(assetName);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                values.put(MediaStore.MediaColumns.MIME_TYPE, "text/csv");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    OutputStream os = getContentResolver().openOutputStream(uri);
                    if (os != null) {
                        copyStream(is, os);
                        Toast.makeText(this, "Template berhasil disimpan di folder Downloads", Toast.LENGTH_LONG).show();
                        return;
                    }
                }
            } else {
                File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!downloadDir.exists()) {
                    downloadDir.mkdirs();
                }
                File outFile = new File(downloadDir, fileName);
                FileOutputStream os = new FileOutputStream(outFile);
                copyStream(is, os);
                Toast.makeText(this, "Template disimpan di: " + outFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
                return;
            }
        } catch (Exception e) {
            downloadTemplateToAppDir();
        }
    }

    private void downloadTemplateToAppDir() {
        String assetName;
        String fileName;
        if ("create_products".equals(currentMode)) {
            assetName = "template_import_produk_baru.csv";
            fileName = "template_import_produk_baru.csv";
        } else {
            assetName = "template_import_tambah_stok.csv";
            fileName = "template_import_tambah_stok.csv";
        }

        try {
            InputStream is = getAssets().open(assetName);
            File fallbackDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            if (fallbackDir != null) {
                File outFile = new File(fallbackDir, fileName);
                FileOutputStream os = new FileOutputStream(outFile);
                copyStream(is, os);
                Toast.makeText(this, "Template disimpan di: " + outFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Gagal mengakses direktori penyimpanan", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Gagal mengunduh template: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void copyStream(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = is.read(buffer)) != -1) {
            os.write(buffer, 0, read);
        }
        os.flush();
        os.close();
        is.close();
    }

    private void pickFile() {
        // Allow all file types, we will validate the extension in the callback
        filePickerLauncher.launch("*/*");
    }

    private void uploadAndProcess() {
        if (selectedFileUri == null) return;

        // Validate file extension
        String extension = getFileExtension(selectedFileName);
        if (!"xlsx".equalsIgnoreCase(extension) && !"xls".equalsIgnoreCase(extension) && !"csv".equalsIgnoreCase(extension)) {
            CurrencyHelper.showError(binding.getRoot(), "Format file tidak didukung! Gunakan format .xlsx atau .csv");
            return;
        }

        setLoading(true);
        binding.layoutResults.setVisibility(View.GONE);

        try {
            File tempFile = copyUriToTempFile(selectedFileUri, selectedFileName);
            
            // Get mime type of file
            String mimeType = getContentResolver().getType(selectedFileUri);
            if (mimeType == null) {
                if ("csv".equalsIgnoreCase(extension)) {
                    mimeType = "text/csv";
                } else {
                    mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                }
            }

            RequestBody requestFile = RequestBody.create(
                    MediaType.parse(mimeType),
                    tempFile
            );

            MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", tempFile.getName(), requestFile);
            RequestBody modePart = RequestBody.create(
                    MediaType.parse("multipart/form-data"),
                    currentMode
            );

            apiService.importProducts(modePart, filePart).enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                    setLoading(false);
                    // Delete cached temp file
                    try {
                        tempFile.delete();
                    } catch (Exception e) {}

                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            JsonObject resObj = response.body();
                            if (resObj.has("data")) {
                                parseImportResponse(resObj.getAsJsonObject("data"));
                            } else {
                                CurrencyHelper.showError(binding.getRoot(), "Respon server tidak memiliki data hasil");
                            }
                        } catch (Exception e) {
                            CurrencyHelper.showError(binding.getRoot(), "Gagal mengurai respon server: " + e.getMessage());
                        }
                    } else {
                        String errorMsg = "Gagal memproses import";
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
                public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                    setLoading(false);
                    // Delete cached temp file
                    try {
                        tempFile.delete();
                    } catch (Exception e) {}
                    CurrencyHelper.showError(binding.getRoot(), "Tidak ada koneksi: " + t.getMessage());
                }
            });

        } catch (IOException e) {
            setLoading(false);
            CurrencyHelper.showError(binding.getRoot(), "Gagal membaca file: " + e.getMessage());
        }
    }

    private File copyUriToTempFile(Uri uri, String fileName) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        if (inputStream == null) throw new IOException("Gagal membuka file");

        File tempFile = new File(getCacheDir(), "import_temp_" + System.currentTimeMillis() + "_" + fileName);
        FileOutputStream outputStream = new FileOutputStream(tempFile);

        byte[] buffer = new byte[4096];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, read);
        }
        outputStream.flush();
        outputStream.close();
        inputStream.close();

        return tempFile;
    }

    private void parseImportResponse(JsonObject dataObj) {
        try {
            JsonObject summary = dataObj.getAsJsonObject("summary");
            int total = summary.has("total") ? summary.get("total").getAsInt() : 0;
            int success = summary.has("success") ? summary.get("success").getAsInt() : 0;
            int failed = summary.has("failed") ? summary.get("failed").getAsInt() : 0;

            binding.tvResultTotal.setText(String.valueOf(total));
            binding.tvResultSuccess.setText(String.valueOf(success));
            binding.tvResultFailed.setText(String.valueOf(failed));

            List<ImportErrorAdapter.ImportErrorItem> errorItems = new ArrayList<>();

            if (dataObj.has("errors") && dataObj.get("errors").isJsonArray()) {
                JsonArray errorsArr = dataObj.getAsJsonArray("errors");
                for (int i = 0; i < errorsArr.size(); i++) {
                    JsonObject rowErrorObj = errorsArr.get(i).getAsJsonObject();
                    int rowNumber = rowErrorObj.get("row").getAsInt();

                    StringBuilder sb = new StringBuilder();
                    if (rowErrorObj.has("errors") && rowErrorObj.get("errors").isJsonObject()) {
                        JsonObject innerErrors = rowErrorObj.getAsJsonObject("errors");
                        for (String fieldKey : innerErrors.keySet()) {
                            if (innerErrors.get(fieldKey).isJsonArray()) {
                                JsonArray fieldErrors = innerErrors.getAsJsonArray(fieldKey);
                                for (int j = 0; j < fieldErrors.size(); j++) {
                                    if (sb.length() > 0) sb.append("\n");
                                    sb.append(fieldErrors.get(j).getAsString());
                                }
                            } else if (innerErrors.get(fieldKey).isJsonPrimitive()) {
                                if (sb.length() > 0) sb.append("\n");
                                sb.append(innerErrors.get(fieldKey).getAsString());
                            }
                        }
                    } else if (rowErrorObj.has("message")) {
                        sb.append(rowErrorObj.get("message").getAsString());
                    }

                    errorItems.add(new ImportErrorAdapter.ImportErrorItem(rowNumber, sb.toString()));
                }
            }

            adapter.setItems(errorItems);
            binding.layoutResults.setVisibility(View.VISIBLE);

            if (!errorItems.isEmpty()) {
                binding.tvErrorListTitle.setVisibility(View.VISIBLE);
            } else {
                binding.tvErrorListTitle.setVisibility(View.GONE);
            }

            if (failed == 0 && total > 0) {
                Toast.makeText(this, "Seluruh data (" + success + ") berhasil diimport!", Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            Toast.makeText(this, "Gagal memproses hasil import: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnUpload.setEnabled(!loading && selectedFileUri != null);
        binding.cardModeCreate.setEnabled(!loading);
        binding.cardModeStock.setEnabled(!loading);
        binding.cardFilePicker.setEnabled(!loading);
        binding.btnDownloadTemplate.setEnabled(!loading);
    }

    private String getFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index != -1) {
                        result = cursor.getString(index);
                    }
                }
            } catch (Exception e) {}
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private long getFileSize(Uri uri) {
        long size = 0;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.SIZE);
                    if (index != -1) {
                        size = cursor.getLong(index);
                    }
                }
            } catch (Exception e) {}
        }
        return size;
    }

    private String formatFileSize(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        return new java.text.DecimalFormat("#,##0.#").format(bytes / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    private String getFileExtension(String filename) {
        if (filename == null) return "";
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex >= 0) ? filename.substring(dotIndex + 1) : "";
    }
}
