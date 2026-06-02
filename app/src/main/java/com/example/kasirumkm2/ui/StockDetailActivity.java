package com.example.kasirumkm2.ui;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.kasirumkm2.R;
import com.example.kasirumkm2.adapter.StockMutationAdapter;
import com.example.kasirumkm2.api.ApiClient;
import com.example.kasirumkm2.api.ApiService;
import com.example.kasirumkm2.databinding.ActivityStockDetailBinding;
import com.example.kasirumkm2.utils.CurrencyHelper;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StockDetailActivity extends AppCompatActivity {

    private ActivityStockDetailBinding binding;
    private ApiService apiService;
    private StockMutationAdapter mutationAdapter;

    private int productId;
    private String productName = "";
    private String productCode = "";
    private String unit = "PCS";
    private int currentStock = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStockDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = ApiClient.getApiService(this);

        // Get intent data
        productId = getIntent().getIntExtra("product_id", 0);
        productName = getIntent().getStringExtra("product_name");
        productCode = getIntent().getStringExtra("product_code");
        unit = getIntent().getStringExtra("unit");
        currentStock = getIntent().getIntExtra("current_stock", 0);

        if (productName == null) productName = "";
        if (productCode == null) productCode = "";
        if (unit == null) unit = "PCS";

        setupUI();
        setupRecyclerView();
        setupListeners();
        loadMutationHistory(null, null);
    }

    private void setupUI() {
        binding.tvToolbarTitle.setText(productName);
        binding.tvProductName.setText(productName);
        binding.tvProductCode.setText(productCode + " · " + unit);
        binding.tvUnit.setText(unit);
        binding.tvEmoji.setText(getEmoji(productName));

        updateStockDisplay(currentStock);
    }

    private void updateStockDisplay(int stock) {
        currentStock = stock;
        binding.tvCurrentStock.setText(String.valueOf(stock));

        // Color coding
        int textColor, bgColor;
        String statusText;
        if (stock <= 0) {
            textColor = ContextCompat.getColor(this, R.color.stock_empty);
            bgColor = ContextCompat.getColor(this, R.color.stock_empty_bg);
            statusText = "Habis";
        } else if (stock <= 10) {
            textColor = ContextCompat.getColor(this, R.color.stock_low);
            bgColor = ContextCompat.getColor(this, R.color.stock_low_bg);
            statusText = "Rendah";
        } else {
            textColor = ContextCompat.getColor(this, R.color.stock_safe);
            bgColor = ContextCompat.getColor(this, R.color.stock_safe_bg);
            statusText = "Aman";
        }

        binding.tvCurrentStock.setTextColor(textColor);
        binding.tvStockStatus.setText(statusText);
        binding.tvStockStatus.setTextColor(textColor);

        GradientDrawable badge = new GradientDrawable();
        badge.setColor(bgColor);
        badge.setCornerRadius(40f);
        binding.tvStockStatus.setBackground(badge);
    }

    private void setupRecyclerView() {
        mutationAdapter = new StockMutationAdapter(this);
        binding.rvMutations.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMutations.setAdapter(mutationAdapter);
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());

        // FAB: Adjust stock
        binding.fabAdjust.setOnClickListener(v -> showAdjustmentSheet());

        // Date filter
        binding.tvFilterDate.setOnClickListener(v -> showDateRangePicker());
    }

    private void showDateRangePicker() {
        MaterialDatePicker<androidx.core.util.Pair<Long, Long>> dateRangePicker =
                MaterialDatePicker.Builder.dateRangePicker()
                        .setTitleText("Filter Riwayat Mutasi")
                        .build();

        dateRangePicker.addOnPositiveButtonClickListener(selection -> {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

            String startDate = sdf.format(new Date(selection.first));
            String endDate = sdf.format(new Date(selection.second));

            SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMM", new Locale("id", "ID"));
            displayFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            String rangeLabel = displayFormat.format(new Date(selection.first)) + " - " +
                    displayFormat.format(new Date(selection.second));

            binding.tvDateRange.setText("📅 " + rangeLabel);
            binding.tvDateRange.setVisibility(View.VISIBLE);

            loadMutationHistory(startDate, endDate);
        });

        dateRangePicker.show(getSupportFragmentManager(), "MUTATION_DATE_RANGE");
    }

    private void loadMutationHistory(String startDate, String endDate) {
        setLoading(true);

        Map<String, String> params = new HashMap<>();
        if (startDate != null) params.put("start_date", startDate);
        if (endDate != null) params.put("end_date", endDate);

        apiService.getStockHistory(productId, params).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonObject resBody = response.body();
                        JsonObject data = resBody.getAsJsonObject("data");

                        // Update product info from response
                        if (data.has("product")) {
                            JsonObject product = data.getAsJsonObject("product");
                            if (product.has("stock")) {
                                try {
                                    int newStock = (int) Double.parseDouble(product.get("stock").getAsString());
                                    updateStockDisplay(newStock);
                                } catch (Exception e) {
                                    try {
                                        updateStockDisplay(product.get("stock").getAsInt());
                                    } catch (Exception ignored) {}
                                }
                            }
                        }

                        // Parse mutations
                        JsonArray mutationsArray = data.getAsJsonArray("mutations");
                        List<JsonObject> mutationList = new ArrayList<>();
                        for (int i = 0; i < mutationsArray.size(); i++) {
                            mutationList.add(mutationsArray.get(i).getAsJsonObject());
                        }

                        mutationAdapter.setData(mutationList);
                        showEmptyMutations(mutationList.isEmpty());
                    } catch (Exception e) {
                        showEmptyMutations(true);
                        CurrencyHelper.showError(binding.getRoot(), "Gagal memuat riwayat mutasi");
                    }
                } else if (response.code() == 401) {
                    handleUnauthorized();
                } else {
                    showEmptyMutations(true);
                    CurrencyHelper.showError(binding.getRoot(), "Gagal memuat riwayat mutasi");
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                setLoading(false);
                showEmptyMutations(true);
                CurrencyHelper.showError(binding.getRoot(), getString(R.string.tidak_ada_koneksi));
            }
        });
    }

    private void showAdjustmentSheet() {
        BottomSheetDialog bottomSheet = new BottomSheetDialog(this);
        View sheetView = LayoutInflater.from(this).inflate(R.layout.layout_stock_adjustment_sheet, null);
        bottomSheet.setContentView(sheetView);

        // Setup sheet views
        TextView tvProductName = sheetView.findViewById(R.id.tvAdjProductName);
        TextView tvCurrentStock = sheetView.findViewById(R.id.tvAdjCurrentStock);
        TextInputLayout tilNewStock = sheetView.findViewById(R.id.tilNewStock);
        TextInputEditText etNewStock = sheetView.findViewById(R.id.etNewStock);
        TextInputEditText etNote = sheetView.findViewById(R.id.etNote);
        View layoutPreview = sheetView.findViewById(R.id.layoutPreview);
        TextView tvPreviewOld = sheetView.findViewById(R.id.tvPreviewOld);
        TextView tvPreviewNew = sheetView.findViewById(R.id.tvPreviewNew);
        TextView tvPreviewDiff = sheetView.findViewById(R.id.tvPreviewDiff);
        View btnSave = sheetView.findViewById(R.id.btnSave);
        View btnCancel = sheetView.findViewById(R.id.btnCancel);
        View progressBar = sheetView.findViewById(R.id.progressBar);

        com.google.android.material.chip.ChipGroup cgMode = sheetView.findViewById(R.id.cgMode);

        tvProductName.setText(productName);
        tvCurrentStock.setText(String.valueOf(currentStock));

        final int[] selectedMode = {R.id.chipAdd}; // Default to Tambah mode

        // Helper to update layout preview dynamically
        java.lang.Runnable updatePreview = new java.lang.Runnable() {
            @Override
            public void run() {
                String input = etNewStock.getText() != null ? etNewStock.getText().toString().trim() : "";
                if (input.isEmpty()) {
                    layoutPreview.setVisibility(View.GONE);
                    return;
                }

                try {
                    int val = Integer.parseInt(input);
                    int newStock;
                    int diff;

                    if (selectedMode[0] == R.id.chipAdd) {
                        newStock = currentStock + val;
                        diff = val;
                    } else if (selectedMode[0] == R.id.chipSubtract) {
                        newStock = currentStock - val;
                        diff = -val;
                    } else {
                        newStock = val;
                        diff = newStock - currentStock;
                    }

                    layoutPreview.setVisibility(View.VISIBLE);
                    tvPreviewOld.setText(String.valueOf(currentStock));
                    tvPreviewNew.setText(String.valueOf(newStock));

                    if (diff > 0) {
                        tvPreviewDiff.setText("(+" + diff + ")");
                        tvPreviewDiff.setTextColor(ContextCompat.getColor(StockDetailActivity.this, R.color.stock_safe));
                        tvPreviewNew.setTextColor(ContextCompat.getColor(StockDetailActivity.this, R.color.stock_safe));
                    } else if (diff < 0) {
                        tvPreviewDiff.setText("(" + diff + ")");
                        tvPreviewDiff.setTextColor(ContextCompat.getColor(StockDetailActivity.this, R.color.stock_empty));
                        tvPreviewNew.setTextColor(ContextCompat.getColor(StockDetailActivity.this, R.color.stock_empty));
                    } else {
                        tvPreviewDiff.setText("(0)");
                        tvPreviewDiff.setTextColor(ContextCompat.getColor(StockDetailActivity.this, R.color.text_light));
                        tvPreviewNew.setTextColor(ContextCompat.getColor(StockDetailActivity.this, R.color.text_dark));
                    }
                } catch (NumberFormatException e) {
                    layoutPreview.setVisibility(View.GONE);
                }
            }
        };

        // Live preview of stock change on text edit
        etNewStock.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updatePreview.run();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Mode chip selection listener
        cgMode.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == View.NO_ID) {
                cgMode.check(selectedMode[0]); // Keep current selected
                return;
            }
            selectedMode[0] = checkedId;
            tilNewStock.setError(null);

            if (checkedId == R.id.chipAdd) {
                tilNewStock.setHint("Jumlah Ditambahkan");
                tilNewStock.setHelperText("Masukkan jumlah stok yang masuk (bertambah)");
            } else if (checkedId == R.id.chipSubtract) {
                tilNewStock.setHint("Jumlah Dikurangkan");
                tilNewStock.setHelperText("Masukkan jumlah stok yang keluar (berkurang)");
            } else {
                tilNewStock.setHint("Stok Akhir");
                tilNewStock.setHelperText("Masukkan angka stok akhir yang diinginkan");
            }
            updatePreview.run();
        });

        // Initialize default choice (will trigger listener and set hint)
        cgMode.check(R.id.chipAdd);

        btnCancel.setOnClickListener(v -> bottomSheet.dismiss());

        btnSave.setOnClickListener(v -> {
            String inputStr = etNewStock.getText() != null ? etNewStock.getText().toString().trim() : "";
            String note = etNote.getText() != null ? etNote.getText().toString().trim() : "";

            if (TextUtils.isEmpty(inputStr)) {
                tilNewStock.setError("Masukkan jumlah/stok");
                return;
            }

            int val;
            try {
                val = Integer.parseInt(inputStr);
            } catch (NumberFormatException e) {
                tilNewStock.setError("Masukkan angka yang valid");
                return;
            }

            if (val < 0) {
                tilNewStock.setError("Angka tidak boleh negatif");
                return;
            }

            int newStock;
            if (selectedMode[0] == R.id.chipAdd) {
                newStock = currentStock + val;
            } else if (selectedMode[0] == R.id.chipSubtract) {
                newStock = currentStock - val;
            } else {
                newStock = val;
            }

            if (newStock < 0) {
                tilNewStock.setError("Stok akhir tidak boleh negatif (saat ini: " + currentStock + ")");
                return;
            }

            tilNewStock.setError(null);

            // Confirm Details
            int diff = newStock - currentStock;
            String actionLabel = selectedMode[0] == R.id.chipAdd ? "Penambahan" :
                    (selectedMode[0] == R.id.chipSubtract ? "Pengurangan" : "Penyesuaian");
            String diffLabel = diff > 0 ? "(+" + diff + ")" : (diff < 0 ? "(" + diff + ")" : "(0)");

            new AlertDialog.Builder(this)
                    .setTitle("Konfirmasi Aksi Stok")
                    .setMessage(actionLabel + " stok " + productName + " dari " + currentStock + " menjadi " + newStock + " " + diffLabel + "?\n\n" +
                            (note.isEmpty() ? "" : "Catatan: " + note))
                    .setPositiveButton("Simpan", (dialog, which) -> {
                        // Execute adjustment
                        progressBar.setVisibility(View.VISIBLE);
                        btnSave.setEnabled(false);

                        JsonObject body = new JsonObject();
                        body.addProperty("product_id", productId);
                        body.addProperty("new_stock", newStock);
                        if (!note.isEmpty()) {
                            body.addProperty("note", note);
                        } else {
                            String defaultNote = actionLabel + " stok: " + (diff > 0 ? "+" + diff : diff);
                            body.addProperty("note", defaultNote);
                        }

                        apiService.createStockAdjustment(body).enqueue(new Callback<JsonObject>() {
                            @Override
                            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                                progressBar.setVisibility(View.GONE);
                                btnSave.setEnabled(true);

                                if (response.isSuccessful() && response.body() != null) {
                                    bottomSheet.dismiss();
                                    CurrencyHelper.showToast(StockDetailActivity.this,
                                            "✅ Stok berhasil diperbarui: " + currentStock + " → " + newStock);

                                    // Update display & reload
                                    updateStockDisplay(newStock);
                                    loadMutationHistory(null, null);
                                } else {
                                    String errorMsg = "Gagal memproses aksi stok";
                                    try {
                                        if (response.errorBody() != null) {
                                            JsonObject err = new com.google.gson.JsonParser()
                                                    .parse(response.errorBody().string()).getAsJsonObject();
                                            if (err.has("message")) errorMsg = err.get("message").getAsString();
                                        }
                                    } catch (Exception ignored) {}
                                    CurrencyHelper.showError(binding.getRoot(), errorMsg);
                                }
                            }

                            @Override
                            public void onFailure(Call<JsonObject> call, Throwable t) {
                                progressBar.setVisibility(View.GONE);
                                btnSave.setEnabled(true);
                                CurrencyHelper.showError(binding.getRoot(), getString(R.string.tidak_ada_koneksi));
                            }
                        });
                    })
                    .setNegativeButton("Batal", null)
                    .show();
        });

        bottomSheet.show();
    }

    private void showEmptyMutations(boolean empty) {
        binding.layoutEmptyMutations.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.rvMutations.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (loading) {
            binding.rvMutations.setVisibility(View.GONE);
            binding.layoutEmptyMutations.setVisibility(View.GONE);
        }
    }

    private String getEmoji(String name) {
        if (name == null) return "📦";
        String lower = name.toLowerCase();
        if (lower.contains("aqua") || lower.contains("air") || lower.contains("minum")) return "💧";
        if (lower.contains("mie") || lower.contains("indomie")) return "🍜";
        if (lower.contains("beras") || lower.contains("nasi")) return "🍚";
        if (lower.contains("gula")) return "🍬";
        if (lower.contains("kopi") || lower.contains("coffee")) return "☕";
        if (lower.contains("teh") || lower.contains("tea")) return "🍵";
        if (lower.contains("susu") || lower.contains("milk")) return "🥛";
        if (lower.contains("roti") || lower.contains("bread")) return "🍞";
        if (lower.contains("sabun") || lower.contains("soap")) return "🧼";
        if (lower.contains("rokok") || lower.contains("sigaret")) return "🚬";
        if (lower.contains("snack") || lower.contains("keripik")) return "🍿";
        if (lower.contains("minyak") || lower.contains("oil")) return "🫗";
        if (lower.contains("telur") || lower.contains("egg")) return "🥚";
        return "📦";
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
