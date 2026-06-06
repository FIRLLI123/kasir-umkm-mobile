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
import com.example.kasirumkm2.api.ApiClient;
import com.example.kasirumkm2.api.ApiService;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private SessionManager sessionManager;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getApiService(this);

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

        // Handle double back press to exit app
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            private long backPressedTime;
            private android.widget.Toast backToast;

            @Override
            public void handleOnBackPressed() {
                if (backPressedTime + 2000 > System.currentTimeMillis()) {
                    if (backToast != null) {
                        backToast.cancel();
                    }
                    finish();
                } else {
                    backToast = android.widget.Toast.makeText(MainActivity.this, "Tekan sekali lagi untuk keluar", android.widget.Toast.LENGTH_SHORT);
                    backToast.show();
                    backPressedTime = System.currentTimeMillis();
                }
            }
        });
    }

    private void setupBottomNavigation() {
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                loadFragment(new HomeFragment());
                return true;
            } else if (id == R.id.nav_kasir) {
                if (checkExpiredAndBlock()) {
                    return false;
                }
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
        
        updateSubscriptionBanner();
        checkSubscriptionSync();
    }

    public void updateSubscriptionBanner() {
        if (sessionManager.isSubscriptionExpired()) {
            binding.layoutExpiryBanner.setVisibility(android.view.View.VISIBLE);
            binding.btnUpgradeBanner.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, com.example.kasirumkm2.ui.SubscriptionActivity.class);
                startActivity(intent);
            });
        } else {
            binding.layoutExpiryBanner.setVisibility(android.view.View.GONE);
        }
    }

    private void checkSubscriptionSync() {
        if (!sessionManager.isLoggedIn()) return;

        apiService.getProfile().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonObject body = response.body();
                        sessionManager.updateAiChatLimit(body);
                        JsonObject data = body.getAsJsonObject("data");
                        if (data.has("company") && !data.get("company").isJsonNull()) {
                            JsonObject comp = data.getAsJsonObject("company");
                            int compId = comp.get("id").getAsInt();
                            String compName = comp.get("company_name").getAsString();
                            String compCode = comp.get("company_code").getAsString();
                            sessionManager.saveCompany(compId, compName, compCode);
                        }
                    } catch (Exception ignored) {}
                }

                // Then fetch active subscription
                apiService.getSubscriptionActive().enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            try {
                                JsonObject body = response.body();
                                sessionManager.updateAiChatLimit(body);
                                if (body.has("data") && !body.get("data").isJsonNull()) {
                                    JsonObject sub = body.getAsJsonObject("data");
                                    String subStatus = sub.has("status") && !sub.get("status").isJsonNull() ? sub.get("status").getAsString() : "expired";
                                    boolean subActive = sub.has("is_active") && !sub.get("is_active").isJsonNull() && sub.get("is_active").getAsBoolean();
                                    boolean subLifetime = sub.has("is_lifetime") && !sub.get("is_lifetime").isJsonNull() && sub.get("is_lifetime").getAsBoolean();
                                    String trialEndsAt = sub.has("trial_ends_at") && !sub.get("trial_ends_at").isJsonNull() ? sub.get("trial_ends_at").getAsString() : "";
                                    String endsAt = sub.has("ends_at") && !sub.get("ends_at").isJsonNull() ? sub.get("ends_at").getAsString() : "";

                                    sessionManager.saveSubscription(subStatus, subActive, subLifetime, trialEndsAt, endsAt);
                                    updateSubscriptionBanner();
                                    
                                    // Also refresh HomeFragment if it's currently loaded to sync cards
                                    Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
                                    if (currentFragment instanceof HomeFragment) {
                                        ((HomeFragment) currentFragment).onResume();
                                    }
                                }
                            } catch (Exception e) {
                                // ignore silently
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        // ignore silently
                    }
                });
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                // If profile fails, still try to fetch active subscription
                apiService.getSubscriptionActive().enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            try {
                                JsonObject body = response.body();
                                sessionManager.updateAiChatLimit(body);
                                if (body.has("data") && !body.get("data").isJsonNull()) {
                                    JsonObject sub = body.getAsJsonObject("data");
                                    String subStatus = sub.has("status") && !sub.get("status").isJsonNull() ? sub.get("status").getAsString() : "expired";
                                    boolean subActive = sub.has("is_active") && !sub.get("is_active").isJsonNull() && sub.get("is_active").getAsBoolean();
                                    boolean subLifetime = sub.has("is_lifetime") && !sub.get("is_lifetime").isJsonNull() && sub.get("is_lifetime").getAsBoolean();
                                    String trialEndsAt = sub.has("trial_ends_at") && !sub.get("trial_ends_at").isJsonNull() ? sub.get("trial_ends_at").getAsString() : "";
                                    String endsAt = sub.has("ends_at") && !sub.get("ends_at").isJsonNull() ? sub.get("ends_at").getAsString() : "";

                                    sessionManager.saveSubscription(subStatus, subActive, subLifetime, trialEndsAt, endsAt);
                                    updateSubscriptionBanner();
                                    
                                    // Also refresh HomeFragment if it's currently loaded to sync cards
                                    Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
                                    if (currentFragment instanceof HomeFragment) {
                                        ((HomeFragment) currentFragment).onResume();
                                    }
                                }
                            } catch (Exception e) {
                                // ignore silently
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        // ignore silently
                    }
                });
            }
        });
    }

    private boolean checkExpiredAndBlock() {
        if (sessionManager.isSubscriptionExpired()) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Masa Aktif Habis")
                    .setMessage(getString(R.string.subscription_expired_desc))
                    .setCancelable(true)
                    .setPositiveButton("UPGRADE / PERPANJANG", (dialog, which) -> {
                        Intent intent = new Intent(MainActivity.this, com.example.kasirumkm2.ui.SubscriptionActivity.class);
                        startActivity(intent);
                    })
                    .setNegativeButton("BATAL", null)
                    .show();
            return true;
        }
        return false;
    }
}