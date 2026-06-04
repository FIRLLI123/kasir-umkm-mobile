package com.example.kasirumkm2.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.kasirumkm2.R;
import com.example.kasirumkm2.api.ApiClient;
import com.example.kasirumkm2.api.ApiService;
import com.example.kasirumkm2.data.Customer;
import com.example.kasirumkm2.data.CustomerGroup;
import com.example.kasirumkm2.databinding.ActivityCustomerFormBinding;
import com.example.kasirumkm2.utils.CurrencyHelper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CustomerFormActivity extends AppCompatActivity {

    private ActivityCustomerFormBinding binding;
    private ApiService apiService;
    private final Gson gson = new Gson();

    private boolean isEditMode = false;
    private int customerId = -1;
    private List<CustomerGroup> customerGroups = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCustomerFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = ApiClient.getApiService(this);

        // Check edit mode
        customerId = getIntent().getIntExtra("customer_id", -1);
        isEditMode = customerId > 0;

        setupToolbar();
        setupListeners();

        binding.btnSave.setEnabled(false); // Disable until customer groups load
        loadCustomerGroups();
    }

    private void setupToolbar() {
        binding.btnBack.setOnClickListener(v -> finish());
    }

    private void setupListeners() {
        binding.btnSave.setOnClickListener(v -> saveCustomer());
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
                        customerGroups.clear();
                        for (int i = 0; i < data.size(); i++) {
                            customerGroups.add(gson.fromJson(data.get(i), CustomerGroup.class));
                        }
                        success = true;
                    } catch (Exception e) {
                        // ignore
                    }
                }

                if (success) {
                    binding.btnSave.setEnabled(true);
                    if (isEditMode) {
                        loadCustomerData();
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

    private void loadCustomerData() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnSave.setEnabled(false);

        apiService.getCustomerDetail(customerId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                binding.progressBar.setVisibility(View.GONE);
                binding.btnSave.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonObject data = response.body().getAsJsonObject("data");
                        Customer customer = gson.fromJson(data, Customer.class);

                        binding.etCode.setText(customer.getCustomerCode());
                        binding.etName.setText(customer.getCustomerName());
                        binding.etPhone.setText(customer.getPhone());
                        binding.etAddress.setText(customer.getAddress());

                        // Set group radio dynamically
                        if (customer.getCustomerGroup() != null) {
                            String code = customer.getCustomerGroup().getCode();
                            if ("FREELANCER".equalsIgnoreCase(code)) {
                                binding.rbFreelancer.setChecked(true);
                            } else if ("GROSIR".equalsIgnoreCase(code)) {
                                binding.rbGrosir.setChecked(true);
                            } else {
                                binding.rbUser.setChecked(true);
                            }
                        } else {
                            binding.rbUser.setChecked(true);
                        }
                    } catch (Exception e) {
                        CurrencyHelper.showError(binding.getRoot(), "Gagal memuat data");
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

    private void saveCustomer() {
        String code = binding.etCode.getText().toString().trim();
        String name = binding.etName.getText().toString().trim();
        String phone = binding.etPhone.getText().toString().trim();
        String address = binding.etAddress.getText().toString().trim();

        // Validate
        boolean valid = true;
        if (TextUtils.isEmpty(code)) {
            binding.tilCode.setError("Kode customer wajib diisi");
            valid = false;
        } else {
            binding.tilCode.setError(null);
        }
        if (TextUtils.isEmpty(name)) {
            binding.tilName.setError("Nama customer wajib diisi");
            valid = false;
        } else {
            binding.tilName.setError(null);
        }
        if (!valid) return;

        // Get group ID dynamically
        String targetCode = "USER";
        if (binding.rbFreelancer.isChecked()) {
            targetCode = "FREELANCER";
        } else if (binding.rbGrosir.isChecked()) {
            targetCode = "GROSIR";
        }

        int groupId = -1;
        for (CustomerGroup cg : customerGroups) {
            if (targetCode.equalsIgnoreCase(cg.getCode())) {
                groupId = cg.getId();
                break;
            }
        }
        if (groupId == -1) {
            groupId = 1; // default fallback
        }

        // Build request body
        JsonObject body = new JsonObject();
        body.addProperty("customer_code", code);
        body.addProperty("customer_name", name);
        body.addProperty("phone", phone);
        body.addProperty("address", address);
        body.addProperty("customer_group_id", groupId);
        body.addProperty("status", "00");

        setLoading(true);

        Callback<JsonObject> callback = new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                setLoading(false);
                if (response.isSuccessful()) {
                    CurrencyHelper.showToast(CustomerFormActivity.this,
                            getString(R.string.berhasil_disimpan));
                    finish();
                } else {
                    String errorMsg = "Gagal menyimpan";
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
            apiService.updateCustomer(customerId, body).enqueue(callback);
        } else {
            apiService.createCustomer(body).enqueue(callback);
        }
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnSave.setEnabled(!loading);
    }
}
