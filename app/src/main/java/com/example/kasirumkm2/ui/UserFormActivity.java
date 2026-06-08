package com.example.kasirumkm2.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.kasirumkm2.R;
import com.example.kasirumkm2.api.ApiClient;
import com.example.kasirumkm2.api.ApiService;
import com.example.kasirumkm2.databinding.ActivityUserFormBinding;
import com.example.kasirumkm2.utils.CurrencyHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserFormActivity extends AppCompatActivity {

    private ActivityUserFormBinding binding;
    private ApiService apiService;
    private com.example.kasirumkm2.session.SessionManager sessionManager;

    private boolean isEditMode = false;
    private int userId = -1;
    private JsonObject userData;

    private final List<JsonObject> companyList = new ArrayList<>();
    private final List<String> companyNames = new ArrayList<>();
    private int selectedCompanyId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new com.example.kasirumkm2.session.SessionManager(this);
        apiService = ApiClient.getApiService(this);

        // Check if edit mode
        userId = getIntent().getIntExtra("user_id", -1);
        String userJson = getIntent().getStringExtra("user_json");
        isEditMode = userId > 0 && userJson != null;

        setupToolbar();
        setupListeners();

        if (sessionManager.isCompanyOwner()) {
            binding.tilCompany.setVisibility(View.GONE);
            binding.rbSuperAdmin.setVisibility(View.GONE);
            binding.tvRoleLabel.setVisibility(View.GONE);
            binding.rgRole.setVisibility(View.GONE);
            if (isEditMode) {
                loadUserData(userJson);
            }
        } else {
            loadCompaniesList(userJson);
        }

        if (isEditMode) {
            binding.tvTitle.setText("Edit Pengguna");
            if (userId == sessionManager.getUserId()) {
                binding.btnDelete.setVisibility(View.GONE);
                binding.cardStatus.setVisibility(View.GONE);
            } else {
                binding.btnDelete.setVisibility(View.VISIBLE);
                binding.cardStatus.setVisibility(View.VISIBLE);
            }
            if (userId == sessionManager.getCompanyOwnerUserId()) {
                binding.rbKasir.setEnabled(false);
                binding.rbAdmin.setEnabled(false);
                binding.rbSuperAdmin.setEnabled(false);
            }
            // tilPassword helper text for optional
            binding.tilPassword.setHelperText("Kosongkan jika tidak ingin mengubah password");
        } else {
            binding.tvTitle.setText("Tambah Pengguna");
            binding.btnDelete.setVisibility(View.GONE);
            binding.cardStatus.setVisibility(View.VISIBLE);
        }
    }

    private void setupToolbar() {
        binding.btnBack.setOnClickListener(v -> finish());
    }

    private void setupListeners() {
        binding.btnSave.setOnClickListener(v -> saveUser());
        binding.btnDelete.setOnClickListener(v -> confirmDeactivate());

        binding.actvCompany.setOnItemClickListener((parent, view, position, id) -> {
            JsonObject selectedCompany = companyList.get(position);
            selectedCompanyId = selectedCompany.get("id").getAsInt();
            binding.tilCompany.setError(null);
        });

        // Click and focus handlers to trigger exposed dropdown display
        binding.actvCompany.setOnClickListener(v -> binding.actvCompany.showDropDown());
        binding.actvCompany.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                binding.actvCompany.showDropDown();
            }
        });
    }

    private void loadCompaniesList(String userJson) {
        apiService.getCompanies().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonArray dataArray = response.body().getAsJsonArray("data");
                        companyList.clear();
                        companyNames.clear();
                        for (int i = 0; i < dataArray.size(); i++) {
                            JsonObject comp = dataArray.get(i).getAsJsonObject();
                            companyList.add(comp);
                            companyNames.add(comp.get("company_name").getAsString());
                        }

                        ArrayAdapter<String> adapter = new ArrayAdapter<>(UserFormActivity.this,
                                android.R.layout.simple_dropdown_item_1line, companyNames);
                        binding.actvCompany.setAdapter(adapter);

                        if (isEditMode) {
                            loadUserData(userJson);
                        }
                    } catch (Exception e) {
                        CurrencyHelper.showError(binding.getRoot(), "Gagal membaca daftar company");
                    }
                } else {
                    CurrencyHelper.showError(binding.getRoot(), "Gagal memuat daftar company dari server");
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                CurrencyHelper.showError(binding.getRoot(), "Tidak ada koneksi untuk memuat company");
            }
        });
    }

    private void loadUserData(String jsonStr) {
        try {
            userData = new JsonParser().parse(jsonStr).getAsJsonObject();

            String name = userData.has("name") && !userData.get("name").isJsonNull() ? userData.get("name").getAsString() : "";
            String email = userData.has("email") && !userData.get("email").isJsonNull() ? userData.get("email").getAsString() : "";
            String phone = userData.has("phone") && !userData.get("phone").isJsonNull() ? userData.get("phone").getAsString() : "";
            String role = userData.has("role") && !userData.get("role").isJsonNull() ? userData.get("role").getAsString() : "KASIR";
            String status = userData.has("status") && !userData.get("status").isJsonNull() ? userData.get("status").getAsString() : "00";

            binding.etName.setText(name);
            binding.etEmail.setText(email);
            binding.etPhone.setText(phone);
            binding.switchStatus.setChecked("00".equals(status));

            // Select role RadioButton
            if ("SUPER_ADMIN".equalsIgnoreCase(role)) {
                binding.rbSuperAdmin.setChecked(true);
            } else if ("ADMIN".equalsIgnoreCase(role)) {
                binding.rbAdmin.setChecked(true);
            } else {
                binding.rbKasir.setChecked(true);
            }

            // Select Company prefill
            if (userData.has("company_id") && !userData.get("company_id").isJsonNull()) {
                int userCompanyId = userData.get("company_id").getAsInt();
                preselectCompany(userCompanyId);
            }
        } catch (Exception e) {
            CurrencyHelper.showError(binding.getRoot(), "Gagal membaca detail data user");
        }
    }

    private void preselectCompany(int companyId) {
        for (int i = 0; i < companyList.size(); i++) {
            JsonObject comp = companyList.get(i);
            if (comp.get("id").getAsInt() == companyId) {
                binding.actvCompany.setText(comp.get("company_name").getAsString(), false);
                selectedCompanyId = companyId;
                break;
            }
        }
    }

    private void saveUser() {
        String name = binding.etName.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        String phone = binding.etPhone.getText().toString().trim();
        String status = binding.switchStatus.isChecked() ? "00" : "99";

        String role = "KASIR";
        if (!sessionManager.isCompanyOwner()) {
            if (binding.rbSuperAdmin.isChecked()) {
                role = "SUPER_ADMIN";
            } else if (binding.rbAdmin.isChecked()) {
                role = "ADMIN";
            }
        }

        // Validation
        boolean isValid = true;
        if (TextUtils.isEmpty(name)) {
            binding.tilName.setError("Nama wajib diisi");
            isValid = false;
        } else {
            binding.tilName.setError(null);
        }

        if (TextUtils.isEmpty(email)) {
            binding.tilEmail.setError("Email wajib diisi");
            isValid = false;
        } else {
            binding.tilEmail.setError(null);
        }

        if (!isEditMode && TextUtils.isEmpty(password)) {
            binding.tilPassword.setError("Password wajib diisi");
            isValid = false;
        } else if (!TextUtils.isEmpty(password) && password.length() < 6) {
            binding.tilPassword.setError("Password minimal 6 karakter");
            isValid = false;
        } else {
            binding.tilPassword.setError(null);
        }

        if (!sessionManager.isCompanyOwner()) {
            if (selectedCompanyId <= 0) {
                binding.tilCompany.setError("Silakan pilih company");
                isValid = false;
            } else {
                binding.tilCompany.setError(null);
            }
        }

        if (!isValid) return;

        JsonObject body = new JsonObject();
        body.addProperty("name", name);
        body.addProperty("email", email);
        if (!TextUtils.isEmpty(password)) {
            body.addProperty("password", password);
        }
        body.addProperty("phone", TextUtils.isEmpty(phone) ? null : phone);
        body.addProperty("role", role);
        if (!sessionManager.isCompanyOwner()) {
            body.addProperty("company_id", selectedCompanyId);
        }
        body.addProperty("status", status);

        setLoading(true);

        Callback<JsonObject> callback = new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                setLoading(false);
                if (response.isSuccessful()) {
                    CurrencyHelper.showToast(UserFormActivity.this, "User berhasil disimpan");
                    finish();
                } else {
                    String errorMsg = "Gagal menyimpan user";
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
            apiService.updateUser(userId, body).enqueue(callback);
        } else {
            apiService.createUser(body).enqueue(callback);
        }
    }

    private void confirmDeactivate() {
        new AlertDialog.Builder(this)
                .setTitle("Nonaktifkan Pengguna")
                .setMessage("Apakah Anda yakin ingin menonaktifkan pengguna ini?")
                .setPositiveButton("Nonaktifkan", (dialog, which) -> executeDeactivate())
                .setNegativeButton("Batal", null)
                .show();
    }

    private void executeDeactivate() {
        setLoading(true);

        apiService.deleteUser(userId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                setLoading(false);
                if (response.isSuccessful()) {
                    CurrencyHelper.showToast(UserFormActivity.this, "Pengguna dinonaktifkan");
                    finish();
                } else {
                    String errorMsg = "Gagal menonaktifkan pengguna";
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
