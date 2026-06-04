package com.example.kasirumkm2;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.kasirumkm2.databinding.ActivityMainBinding;
import com.example.kasirumkm2.session.SessionManager;
import com.example.kasirumkm2.ui.HomeFragment;
import com.example.kasirumkm2.ui.LoginActivity;
import com.example.kasirumkm2.ui.ReportFragment;
import com.example.kasirumkm2.ui.SettingsFragment;
import com.example.kasirumkm2.ui.POSActivity;
import com.example.kasirumkm2.ui.ChatActivity;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);

        // Check login
        if (!sessionManager.isLoggedIn()) {
            navigateToLogin();
            return;
        }

        setupBottomNavigation();

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }
    }

    private void setupBottomNavigation() {
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                loadFragment(new HomeFragment());
                return true;
            } else if (id == R.id.nav_kasir) {
                // Launch POS as separate activity
                startActivity(new Intent(this, POSActivity.class));
                return false; // Don't switch tab, stay on current
            } else if (id == R.id.nav_chat) {
                // Launch AI Chat as standalone activity
                startActivity(new Intent(this, ChatActivity.class));
                return false; // Don't switch tab
            } else if (id == R.id.nav_laporan) {
                loadFragment(new ReportFragment());
                return true;
            } else if (id == R.id.nav_pengaturan) {
                loadFragment(new SettingsFragment());
                return true;
            }
            return false;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        android.R.anim.fade_in,
                        android.R.anim.fade_out
                )
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ensure the home tab is re-selected when returning from POS
        if (binding != null && binding.bottomNav.getSelectedItemId() == R.id.nav_kasir) {
            binding.bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }
}