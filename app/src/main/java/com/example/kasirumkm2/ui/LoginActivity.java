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
                        
                        // Save credentials for prefilling next time
                        sessionManager.saveSavedCredentials(email, password);

                        // Reset API client to use new token
                        ApiClient.resetClient();

                        // Navigate
                        navigateToMain();
                    } catch (Exception e) {
                        CurrencyHelper.showError(binding.getRoot(), "Login gagal: " + e.getMessage());
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
                    CurrencyHelper.showError(binding.getRoot(), errorMsg);
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                setLoading(false);
                CurrencyHelper.showError(binding.getRoot(),
                        getString(R.string.tidak_ada_koneksi));
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
