package com.example.kasirumkm2.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.kasirumkm2.R;
import com.example.kasirumkm2.api.ApiClient;
import com.example.kasirumkm2.api.ApiService;
import com.example.kasirumkm2.databinding.ActivityCompanyFormBinding;
import com.example.kasirumkm2.session.SessionManager;
import com.example.kasirumkm2.utils.CurrencyHelper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CompanyFormActivity extends AppCompatActivity {

    private ActivityCompanyFormBinding binding;
    private ApiService apiService;

    private boolean isEditMode = false;
    private int companyId = -1;
    private JsonObject companyData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCompanyFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = ApiClient.getApiService(this);

        // Check if edit mode
        companyId = getIntent().getIntExtra("company_id", -1);
        String companyJson = getIntent().getStringExtra("company_json");
        isEditMode = companyId > 0 && companyJson != null;

        setupToolbar();
        setupListeners();

        if (isEditMode) {
            binding.tvTitle.setText("Edit Company");
            binding.btnDelete.setVisibility(View.VISIBLE);
            loadCompanyData(companyJson);
        } else {
            binding.tvTitle.setText("Tambah Company");
            binding.btnDelete.setVisibility(View.GONE);
        }
    }

    private void setupToolbar() {
        binding.btnBack.setOnClickListener(v -> finish());
    }

    private void setupListeners() {
        binding.btnSave.setOnClickListener(v -> saveCompany());
        binding.btnDelete.setOnClickListener(v -> confirmDeactivate());
    }

    private void loadCompanyData(String jsonStr) {
        try {
            companyData = new JsonParser().parse(jsonStr).getAsJsonObject();
            
            String name = companyData.has("company_name") && !companyData.get("company_name").isJsonNull() 
                    ? companyData.get("company_name").getAsString() : "";
            String code = companyData.has("company_code") && !companyData.get("company_code").isJsonNull() 
                    ? companyData.get("company_code").getAsString() : "";
            String email = companyData.has("email") && !companyData.get("email").isJsonNull() 
                    ? companyData.get("email").getAsString() : "";
            String phone = companyData.has("phone") && !companyData.get("phone").isJsonNull() 
                    ? companyData.get("phone").getAsString() : "";
            String address = companyData.has("address") && !companyData.get("address").isJsonNull() 
                    ? companyData.get("address").getAsString() : "";
            int status = companyData.has("status") && !companyData.get("status").isJsonNull() 
                    ? companyData.get("status").getAsInt() : 1;

            binding.etName.setText(name);
            binding.etCode.setText(code);
            binding.etEmail.setText(email);
            binding.etPhone.setText(phone);
            binding.etAddress.setText(address);
            binding.switchStatus.setChecked(status == 1);
            
            // Code is usually unique and shouldn't be edited once created
            binding.etCode.setEnabled(false);
            binding.tilCode.setEnabled(false);
        } catch (Exception e) {
            CurrencyHelper.showError(binding.getRoot(), "Gagal membaca data company");
        }
    }

    private void saveCompany() {
        String name = binding.etName.getText().toString().trim();
        String code = binding.etCode.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String phone = binding.etPhone.getText().toString().trim();
        String address = binding.etAddress.getText().toString().trim();
        int status = binding.switchStatus.isChecked() ? 1 : 0;

        // Validation
        boolean isValid = true;
        if (TextUtils.isEmpty(code)) {
            binding.tilCode.setError("Kode company wajib diisi");
            isValid = false;
        } else {
            binding.tilCode.setError(null);
        }

        if (TextUtils.isEmpty(name)) {
            binding.tilName.setError("Nama company wajib diisi");
            isValid = false;
        } else {
            binding.tilName.setError(null);
        }

        if (!isValid) return;

        JsonObject body = new JsonObject();
        body.addProperty("company_name", name);
        body.addProperty("company_code", code);
        body.addProperty("email", TextUtils.isEmpty(email) ? null : email);
        body.addProperty("phone", TextUtils.isEmpty(phone) ? null : phone);
        body.addProperty("address", TextUtils.isEmpty(address) ? null : address);
        body.addProperty("logo", (String) null);
        body.addProperty("status", status);

        setLoading(true);

        Callback<JsonObject> callback = new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                setLoading(false);
                if (response.isSuccessful()) {
                    CurrencyHelper.showToast(CompanyFormActivity.this, "Company berhasil disimpan");
                    finish();
                } else {
                    String errorMsg = "Gagal menyimpan";
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
                CurrencyHelper.showError(binding.getRoot(), getString(R.string.tidak_ada_koneksi));
            }
        };

        if (isEditMode) {
            apiService.updateCompany(companyId, body).enqueue(callback);
        } else {
            apiService.createCompany(body).enqueue(callback);
        }
    }

    private void confirmDeactivate() {
        new AlertDialog.Builder(this)
                .setTitle("Nonaktifkan Company")
                .setMessage("Apakah Anda yakin ingin menonaktifkan company ini? User dari company ini tidak akan bisa login.")
                .setPositiveButton("Nonaktifkan", (dialog, which) -> executeDeactivate())
                .setNegativeButton("Batal", null)
                .show();
    }

    private void executeDeactivate() {
        setLoading(true);

        apiService.deleteCompany(companyId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                setLoading(false);
                if (response.isSuccessful()) {
                    CurrencyHelper.showToast(CompanyFormActivity.this, "Company dinonaktifkan");
                    finish();
                } else {
                    String errorMsg = "Gagal menonaktifkan company";
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
                CurrencyHelper.showError(binding.getRoot(), getString(R.string.tidak_ada_koneksi));
            }
        });
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnSave.setEnabled(!loading);
        binding.btnDelete.setEnabled(!loading);
    }
}
