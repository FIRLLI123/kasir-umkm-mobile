package com.example.kasirumkm2.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.kasirumkm2.R;
import com.example.kasirumkm2.api.ApiClient;
import com.example.kasirumkm2.api.ApiService;
import com.example.kasirumkm2.databinding.ActivityProfileBinding;
import com.example.kasirumkm2.session.SessionManager;
import com.example.kasirumkm2.utils.AirinDialog;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding binding;
    private SessionManager sessionManager;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getApiService(this);

        setupToolbar();
        displayProfileDetails();
        setupChangePassword();
    }

    private void setupToolbar() {
        binding.btnBack.setOnClickListener(v -> finish());
    }

    private void displayProfileDetails() {
        String name = sessionManager.getUserName();
        String email = sessionManager.getUserEmail();
        String role = sessionManager.getUserRole();
        String deviceId = sessionManager.getDeviceId();
        String deviceName = sessionManager.getDeviceName();

        if (sessionManager.isCompanyOwner()) {
            role = "OWNER";
        }

        binding.tvName.setText(name);
        binding.tvEmail.setText(email);
        binding.tvRole.setText(role != null && !role.isEmpty() ? role.toUpperCase() : "ADMINISTRATOR");
        binding.tvDeviceId.setText(deviceId);
        binding.tvDeviceName.setText(deviceName);

        // Avatar Initials
        if (name != null && !name.isEmpty()) {
            String[] parts = name.trim().split("\\s+");
            String initials = parts.length >= 2
                    ? ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase()
                    : ("" + parts[0].charAt(0)).toUpperCase();
            binding.tvInitials.setText(initials);
        } else {
            binding.tvInitials.setText("U");
        }
    }

    private void setupChangePassword() {
        if (sessionManager.isCompanyOwner()) {
            binding.cardChangePassword.setVisibility(View.VISIBLE);
            binding.btnChangePassword.setOnClickListener(v -> executeChangePassword());
        } else {
            binding.cardChangePassword.setVisibility(View.GONE);
        }
    }

    private void executeChangePassword() {
        String oldPassword = binding.etOldPassword.getText().toString().trim();
        String newPassword = binding.etNewPassword.getText().toString().trim();
        String confirmPassword = binding.etConfirmNewPassword.getText().toString().trim();

        boolean isValid = true;

        if (TextUtils.isEmpty(oldPassword)) {
            binding.tilOldPassword.setError("Password lama wajib diisi");
            isValid = false;
        } else {
            binding.tilOldPassword.setError(null);
        }

        if (TextUtils.isEmpty(newPassword)) {
            binding.tilNewPassword.setError("Password baru wajib diisi");
            isValid = false;
        } else if (newPassword.length() < 6) {
            binding.tilNewPassword.setError("Password baru minimal 6 karakter");
            isValid = false;
        } else {
            binding.tilNewPassword.setError(null);
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            binding.tilConfirmNewPassword.setError("Konfirmasi password baru wajib diisi");
            isValid = false;
        } else if (!newPassword.equals(confirmPassword)) {
            binding.tilConfirmNewPassword.setError("Konfirmasi password baru tidak cocok");
            isValid = false;
        } else {
            binding.tilConfirmNewPassword.setError(null);
        }

        if (!isValid) return;

        JsonObject body = new JsonObject();
        body.addProperty("old_password", oldPassword);
        body.addProperty("password", newPassword);
        body.addProperty("password_confirmation", confirmPassword);

        binding.btnChangePassword.setEnabled(false);

        apiService.changePassword(body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                binding.btnChangePassword.setEnabled(true);
                if (response.isSuccessful()) {
                    AirinDialog.showSuccess(ProfileActivity.this, "Yeay! Berhasil! 🎉", "Password kamu berhasil diubah! Jangan sampai lupa ya password barunya~", () -> {
                        binding.etOldPassword.setText("");
                        binding.etNewPassword.setText("");
                        binding.etConfirmNewPassword.setText("");
                    });
                } else {
                    String errorMsg = "Gagal mengubah password";
                    try {
                        if (response.errorBody() != null) {
                            JsonObject err = new JsonParser().parse(response.errorBody().string()).getAsJsonObject();
                            if (err.has("message")) errorMsg = err.get("message").getAsString();
                        }
                    } catch (Exception e) {}
                    AirinDialog.showError(ProfileActivity.this, "Gagal Mengubah Password 😢", errorMsg, null);
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                binding.btnChangePassword.setEnabled(true);
                AirinDialog.showOffline(ProfileActivity.this, null);
            }
        });
    }
}
