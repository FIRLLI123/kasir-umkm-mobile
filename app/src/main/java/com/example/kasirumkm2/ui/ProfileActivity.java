package com.example.kasirumkm2.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.kasirumkm2.databinding.ActivityProfileBinding;
import com.example.kasirumkm2.session.SessionManager;

public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding binding;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);

        setupToolbar();
        displayProfileDetails();
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
}
