package com.example.kasirumkm2.ui;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.kasirumkm2.R;
import com.example.kasirumkm2.api.ApiClient;
import com.example.kasirumkm2.api.ApiService;
import com.example.kasirumkm2.data.Customer;
import com.example.kasirumkm2.databinding.ActivityCustomerDetailBinding;
import com.example.kasirumkm2.utils.CurrencyHelper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CustomerDetailActivity extends AppCompatActivity {

    private ActivityCustomerDetailBinding binding;
    private ApiService apiService;
    private final Gson gson = new Gson();
    private int customerId = -1;
    private Customer currentCustomer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCustomerDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = ApiClient.getApiService(this);
        customerId = getIntent().getIntExtra("customer_id", -1);

        if (customerId <= 0) {
            CurrencyHelper.showToast(this, "ID Customer tidak valid");
            finish();
            return;
        }

        setupToolbar();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCustomerDetails();
    }

    private void setupToolbar() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnEdit.setOnClickListener(v -> {
            if (currentCustomer != null) {
                Intent intent = new Intent(this, CustomerFormActivity.class);
                intent.putExtra("customer_id", customerId);
                startActivity(intent);
            }
        });
    }

    private void setupListeners() {
        binding.btnDelete.setOnClickListener(v -> confirmDelete());
    }

    private void loadCustomerDetails() {
        setLoading(true);
        apiService.getCustomerDetail(customerId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonObject data = response.body().getAsJsonObject("data");
                        currentCustomer = gson.fromJson(data, Customer.class);
                        displayCustomer();
                    } catch (Exception e) {
                        CurrencyHelper.showError(binding.getRoot(), "Gagal parsing data customer");
                    }
                } else if (response.code() == 401) {
                    handleUnauthorized();
                } else {
                    CurrencyHelper.showError(binding.getRoot(), "Gagal memuat detail customer");
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                setLoading(false);
                CurrencyHelper.showError(binding.getRoot(), getString(R.string.tidak_ada_koneksi));
            }
        });
    }

    private void displayCustomer() {
        if (currentCustomer == null) return;

        binding.tvName.setText(currentCustomer.getCustomerName());
        binding.tvInitials.setText(currentCustomer.getInitials());
        binding.tvCode.setText(currentCustomer.getCustomerCode());
        
        String phone = currentCustomer.getPhone();
        binding.tvPhone.setText(phone == null || phone.trim().isEmpty() ? "-" : phone);

        String address = currentCustomer.getAddress();
        binding.tvAddress.setText(address == null || address.trim().isEmpty() ? "-" : address);

        // Group Badge
        String groupName = currentCustomer.getGroupName().toUpperCase();
        binding.tvBadge.setText(groupName);

        // Group color coding
        int badgeColor;
        int badgeBg;
        if (groupName.contains("FREELANCER")) {
            badgeColor = getColor(R.color.orange_primary);
            badgeBg = getColor(R.color.orange_light);
        } else if (groupName.contains("GROSIR")) {
            badgeColor = getColor(R.color.purple_primary);
            badgeBg = getColor(R.color.purple_light);
        } else {
            badgeColor = getColor(R.color.primary);
            badgeBg = getColor(R.color.primary_light);
        }

        binding.tvBadge.setTextColor(badgeColor);
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setCornerRadius(16);
        shape.setColor(badgeBg);
        binding.tvBadge.setBackground(shape);
    }

    private void confirmDelete() {
        if (currentCustomer == null) return;

        new AlertDialog.Builder(this)
                .setTitle("Hapus Customer")
                .setMessage("Apakah Anda yakin ingin menghapus customer \"" + currentCustomer.getCustomerName() + "\"?")
                .setPositiveButton("Hapus", (dialog, which) -> deleteCustomer())
                .setNegativeButton("Batal", null)
                .show();
    }

    private void deleteCustomer() {
        setLoading(true);
        apiService.deleteCustomer(customerId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                setLoading(false);
                if (response.isSuccessful()) {
                    CurrencyHelper.showToast(CustomerDetailActivity.this, "Customer berhasil dihapus");
                    finish();
                } else if (response.code() == 401) {
                    handleUnauthorized();
                } else {
                    String errorMsg = "Gagal menghapus customer";
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
        });
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnEdit.setEnabled(!loading);
        binding.btnDelete.setEnabled(!loading);
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
