package com.example.kasirumkm2.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.kasirumkm2.MainActivity;
import com.example.kasirumkm2.R;
import com.example.kasirumkm2.api.ApiClient;
import com.example.kasirumkm2.api.ApiService;
import com.example.kasirumkm2.data.RegisterRequest;
import com.example.kasirumkm2.databinding.ActivityRegisterBinding;
import com.example.kasirumkm2.session.SessionManager;
import com.example.kasirumkm2.utils.CurrencyHelper;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private SessionManager sessionManager;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getApiService(this);

        setupListeners();
    }

    private void setupListeners() {
        binding.btnRegister.setOnClickListener(v -> attemptRegister());
        binding.tvLoginLink.setOnClickListener(v -> finish());

        // Clear errors on focus
        binding.etCompanyName.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) binding.tilCompanyName.setError(null);
        });
        binding.etName.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) binding.tilName.setError(null);
        });
        binding.etEmail.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) binding.tilEmail.setError(null);
        });
        binding.etPhone.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) binding.tilPhone.setError(null);
        });
        binding.etAddress.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) binding.tilAddress.setError(null);
        });
        binding.etPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) binding.tilPassword.setError(null);
        });
        binding.etPasswordConfirmation.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) binding.tilPasswordConfirmation.setError(null);
        });
    }

    private void attemptRegister() {
        String companyName = binding.etCompanyName.getText().toString().trim();
        String name = binding.etName.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String phone = binding.etPhone.getText().toString().trim();
        String address = binding.etAddress.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        String passwordConf = binding.etPasswordConfirmation.getText().toString().trim();

        // Validation
        boolean valid = true;

        if (TextUtils.isEmpty(companyName)) {
            binding.tilCompanyName.setError(getString(R.string.register_error_company_name));
            shakeView(binding.tilCompanyName);
            valid = false;
        }
        if (TextUtils.isEmpty(name)) {
            binding.tilName.setError(getString(R.string.register_error_name));
            shakeView(binding.tilName);
            valid = false;
        }
        if (TextUtils.isEmpty(email)) {
            binding.tilEmail.setError(getString(R.string.login_error_email));
            shakeView(binding.tilEmail);
            valid = false;
        }
        if (TextUtils.isEmpty(phone)) {
            binding.tilPhone.setError(getString(R.string.register_error_phone));
            shakeView(binding.tilPhone);
            valid = false;
        }
        if (TextUtils.isEmpty(address)) {
            binding.tilAddress.setError(getString(R.string.register_error_address));
            shakeView(binding.tilAddress);
            valid = false;
        }
        if (TextUtils.isEmpty(password)) {
            binding.tilPassword.setError(getString(R.string.login_error_password));
            shakeView(binding.tilPassword);
            valid = false;
        }
        if (TextUtils.isEmpty(passwordConf)) {
            binding.tilPasswordConfirmation.setError(getString(R.string.register_error_password_conf));
            shakeView(binding.tilPasswordConfirmation);
            valid = false;
        }
        if (valid && !password.equals(passwordConf)) {
            binding.tilPasswordConfirmation.setError(getString(R.string.register_error_password_match));
            shakeView(binding.tilPasswordConfirmation);
            valid = false;
        }

        if (!valid) return;

        setLoading(true);

        RegisterRequest registerRequest = new RegisterRequest(
                companyName,
                name,
                email,
                password,
                passwordConf,
                phone,
                address,
                sessionManager.getDeviceId(),
                sessionManager.getDeviceName()
        );

        apiService.register(registerRequest).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                setLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonObject body = response.body();
                        JsonObject data = body.getAsJsonObject("data");
                        String token = data.get("token").getAsString();

                        JsonObject user = data.getAsJsonObject("user");
                        int userId = user.get("id").getAsInt();
                        String name = user.get("name").getAsString();
                        String userEmail = user.get("email").getAsString();
                        String role = user.has("role") ? user.get("role").getAsString() : "admin";

                        // Save session
                        sessionManager.saveSession(token, userId, name, userEmail, role);

                        // Save company details
                        if (data.has("company") && !data.get("company").isJsonNull()) {
                            JsonObject comp = data.getAsJsonObject("company");
                            int compId = comp.get("id").getAsInt();
                            String compName = comp.get("company_name").getAsString();
                            String compCode = comp.get("company_code").getAsString();
                            sessionManager.saveCompany(compId, compName, compCode);
                        }

                        // Save subscription details
                        if (data.has("subscription") && !data.get("subscription").isJsonNull()) {
                            JsonObject sub = data.getAsJsonObject("subscription");
                            String subStatus = sub.has("status") && !sub.get("status").isJsonNull() ? sub.get("status").getAsString() : "trial";
                            boolean subActive = sub.has("is_active") && !sub.get("is_active").isJsonNull() && sub.get("is_active").getAsBoolean();
                            boolean subLifetime = sub.has("is_lifetime") && !sub.get("is_lifetime").isJsonNull() && sub.get("is_lifetime").getAsBoolean();
                            String trialEndsAt = sub.has("trial_ends_at") && !sub.get("trial_ends_at").isJsonNull() ? sub.get("trial_ends_at").getAsString() : "";
                            String endsAt = sub.has("ends_at") && !sub.get("ends_at").isJsonNull() ? sub.get("ends_at").getAsString() : "";
                            sessionManager.saveSubscription(subStatus, subActive, subLifetime, trialEndsAt, endsAt);
                        }

                        // Save AI chat limit details if available in response
                        sessionManager.updateAiChatLimit(body);

                        // Save credentials for prefilling next login
                        sessionManager.saveSavedCredentials(email, password);

                        // Reset API client to use new Bearer token
                        ApiClient.resetClient();

                        CurrencyHelper.showSuccess(binding.getRoot(), getString(R.string.register_success));

                        // Navigate to Main Activity
                        navigateToMain();
                    } catch (Exception e) {
                        CurrencyHelper.showError(binding.getRoot(), "Registrasi gagal: " + e.getMessage());
                    }
                } else {
                    String errorMsg = getString(R.string.register_failed);
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            JsonObject errorJson = new com.google.gson.JsonParser()
                                    .parse(errorBody).getAsJsonObject();
                            if (errorJson.has("message")) {
                                errorMsg = errorJson.get("message").getAsString();
                            }
                        }
                    } catch (Exception e) {
                        // ignore
                    }
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
        binding.btnRegister.setEnabled(!loading);
        binding.btnRegister.setText(loading ? "" : getString(R.string.btn_register));
        binding.etCompanyName.setEnabled(!loading);
        binding.etName.setEnabled(!loading);
        binding.etEmail.setEnabled(!loading);
        binding.etPhone.setEnabled(!loading);
        binding.etAddress.setEnabled(!loading);
        binding.etPassword.setEnabled(!loading);
        binding.etPasswordConfirmation.setEnabled(!loading);
    }

    private void shakeView(View view) {
        view.animate()
                .translationX(8f).setDuration(80)
                .withEndAction(() -> view.animate()
                        .translationX(-8f).setDuration(80)
                        .withEndAction(() -> view.animate()
                                .translationX(4f).setDuration(60)
                                .withEndAction(() -> view.animate()
                                        .translationX(0f).setDuration(60).start())
                                .start())
                        .start())
                .start();
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
