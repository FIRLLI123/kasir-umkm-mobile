package com.example.kasirumkm2.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.AnimationUtils;

import androidx.appcompat.app.AppCompatActivity;

import com.example.kasirumkm2.MainActivity;
import com.example.kasirumkm2.R;
import com.example.kasirumkm2.api.ApiClient;
import com.example.kasirumkm2.api.ApiService;
import com.example.kasirumkm2.data.LoginRequest;
import com.example.kasirumkm2.databinding.ActivityLoginBinding;
import com.example.kasirumkm2.session.SessionManager;
import com.example.kasirumkm2.utils.AirinDialog;
import com.example.kasirumkm2.utils.CurrencyHelper;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private SessionManager sessionManager;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getApiService(this);
        
        binding.tvVersion.setText("v" + com.example.kasirumkm2.utils.CurrencyHelper.getAppVersion(this));

        // Check if already logged in
        if (sessionManager.isLoggedIn()) {
            navigateToMain();
            return;
        }

        // Prefill saved credentials
        String savedEmail = sessionManager.getSavedEmail();
        String savedPassword = sessionManager.getSavedPassword();
        if (!savedEmail.isEmpty()) {
            binding.etEmail.setText(savedEmail);
        }
        if (!savedPassword.isEmpty()) {
            binding.etPassword.setText(savedPassword);
        }

        setupListeners();
    }

    private void setupListeners() {
        binding.btnLogin.setOnClickListener(v -> attemptLogin());
        binding.tvRegisterLink.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        // Clear errors on focus
        binding.etEmail.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) binding.tilEmail.setError(null);
        });
        binding.etPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) binding.tilPassword.setError(null);
        });
    }

    private void attemptLogin() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        // Validate
        boolean valid = true;
        if (TextUtils.isEmpty(email)) {
            binding.tilEmail.setError(getString(R.string.login_error_email));
            shakeView(binding.tilEmail);
            valid = false;
        }
        if (TextUtils.isEmpty(password)) {
            binding.tilPassword.setError(getString(R.string.login_error_password));
            shakeView(binding.tilPassword);
            valid = false;
        }
        if (!valid) return;

        // Show loading
        setLoading(true);

        // Build request
        LoginRequest loginRequest = new LoginRequest(
                email,
                password,
                sessionManager.getDeviceId(),
                sessionManager.getDeviceName()
        );

        // Call API
        apiService.login(loginRequest).enqueue(new Callback<JsonObject>() {
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
                        
                        // Save company details if available in response
                        if (data.has("company") && !data.get("company").isJsonNull()) {
                            JsonObject comp = data.getAsJsonObject("company");
                            int compId = comp.get("id").getAsInt();
                            String compName = comp.get("company_name").getAsString();
                            String compCode = comp.get("company_code").getAsString();
                            int ownerUserId = comp.has("owner_user_id") && !comp.get("owner_user_id").isJsonNull() ? comp.get("owner_user_id").getAsInt() : 0;
                            sessionManager.saveCompany(compId, compName, compCode, ownerUserId);
                        }

                        // Save explicit is_owner flag from backend (most reliable check)
                        if (data.has("is_owner") && !data.get("is_owner").isJsonNull()) {
                            sessionManager.saveIsOwner(data.get("is_owner").getAsBoolean());
                        }

                        // Save subscription details if available in response
                        if (data.has("subscription") && !data.get("subscription").isJsonNull()) {
                            JsonObject sub = data.getAsJsonObject("subscription");
                            String subStatus = sub.has("status") && !sub.get("status").isJsonNull() ? sub.get("status").getAsString() : "expired";
                            boolean subActive = sub.has("is_active") && !sub.get("is_active").isJsonNull() && sub.get("is_active").getAsBoolean();
                            boolean subLifetime = sub.has("is_lifetime") && !sub.get("is_lifetime").isJsonNull() && sub.get("is_lifetime").getAsBoolean();
                            String trialEndsAt = sub.has("trial_ends_at") && !sub.get("trial_ends_at").isJsonNull() ? sub.get("trial_ends_at").getAsString() : "";
                            String endsAt = sub.has("ends_at") && !sub.get("ends_at").isJsonNull() ? sub.get("ends_at").getAsString() : "";
                            sessionManager.saveSubscription(subStatus, subActive, subLifetime, trialEndsAt, endsAt);
                        }
                        
                        // Save AI chat limit details if available in response
                        sessionManager.updateAiChatLimit(body);
                        
                        // Save credentials for prefilling next time
                        sessionManager.saveSavedCredentials(email, password);

                        // Reset API client to use new token
                        ApiClient.resetClient();

                        // Dialog Yeay! sebelum navigate
                        AirinDialog.showSuccess(
                                LoginActivity.this,
                                "Yeay, berhasil masuk! 🎉",
                                "Halo lagi~ Airin senang kamu kembali! Yuk langsung mulai kasir hari ini! 😊",
                                () -> navigateToMain()
                        );

                    } catch (Exception e) {
                        AirinDialog.showError(
                                LoginActivity.this,
                                "Ups, ada yang aneh nih 😅",
                                "Login gagal: " + e.getMessage() + "\nCoba lagi ya~",
                                null
                        );
                    }
                } else {
                    // Handle error
                    String errorMsg = "Login gagal";
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
                    AirinDialog.showError(
                            LoginActivity.this,
                            "Gagal Masuk 😢",
                            errorMsg + "\nCoba cek email & password kamu ya~",
                            null
                    );
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                setLoading(false);
                AirinDialog.showOffline(LoginActivity.this, null);
            }
        });
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnLogin.setEnabled(!loading);
        binding.btnLogin.setText(loading ? "" : getString(R.string.btn_login));
        binding.etEmail.setEnabled(!loading);
        binding.etPassword.setEnabled(!loading);
    }

    private void shakeView(View view) {
        // Simple shake animation
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
