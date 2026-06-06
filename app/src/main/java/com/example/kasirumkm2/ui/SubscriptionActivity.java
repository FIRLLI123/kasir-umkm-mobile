package com.example.kasirumkm2.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.kasirumkm2.R;
import com.example.kasirumkm2.adapter.SubscriptionPlanAdapter;
import com.example.kasirumkm2.api.ApiClient;
import com.example.kasirumkm2.api.ApiService;
import com.example.kasirumkm2.databinding.ActivitySubscriptionBinding;
import com.example.kasirumkm2.utils.CurrencyHelper;
import com.example.kasirumkm2.session.SessionManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SubscriptionActivity extends AppCompatActivity {

    private ActivitySubscriptionBinding binding;
    private ApiService apiService;
    private SubscriptionPlanAdapter adapter;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySubscriptionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = ApiClient.getApiService(this);
        sessionManager = new SessionManager(this);

        // Check for pending transaction
        if (sessionManager.hasPendingTransaction()) {
            String expiredAt = sessionManager.getPendingExpiredAt();
            boolean isExpired = false;
            if (expiredAt != null && !expiredAt.isEmpty()) {
                try {
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US);
                    java.util.Date date = sdf.parse(expiredAt);
                    if (date != null && date.getTime() <= System.currentTimeMillis()) {
                        isExpired = true;
                    }
                } catch (Exception e) {
                    // keep default false
                }
            }

            if (!isExpired) {
                Intent intent = new Intent(this, PaymentActivity.class);
                intent.putExtra("transaction_id", sessionManager.getPendingTransactionId());
                intent.putExtra("amount", sessionManager.getPendingAmount());
                intent.putExtra("qris_image", sessionManager.getPendingQrisImage());
                intent.putExtra("expired_at", expiredAt);
                intent.putExtra("plan_name", sessionManager.getPendingPlanName());
                intent.putExtra("plan_price", sessionManager.getPendingPlanPrice());
                startActivity(intent);
                finish();
                return;
            } else {
                sessionManager.clearPendingTransaction();
            }
        }

        setupToolbar();
        setupRecyclerView();
        setupSwipeRefresh();

        updateSubtitleText();
        syncSubscriptionStatus();
        loadPlans();
    }

    private void updateSubtitleText() {
        String status = sessionManager.getSubscriptionStatus();
        boolean isActive = sessionManager.isSubscriptionActive();
        boolean isLifetime = sessionManager.isSubscriptionLifetime();
        String endsAt = sessionManager.getSubscriptionEndsAt();
        String trialEndsAt = sessionManager.getSubscriptionTrialEndsAt();

        if (isLifetime) {
            binding.tvSubSubtitle.setText("Status: Aktif Selamanya (Lifetime) 👑\nTerima kasih atas dukungan penuh Anda!");
            binding.tvSubSubtitle.setTextColor(getColor(R.color.purple_primary));
        } else if ("active".equals(status) && isActive) {
            String formattedDate = formatDateString(endsAt);
            binding.tvSubSubtitle.setText("Status: Premium Aktif ⭐\nBerlaku hingga: " + formattedDate);
            binding.tvSubSubtitle.setTextColor(getColor(R.color.success_green));
        } else if ("trial".equals(status) && isActive) {
            String formattedDate = formatDateString(trialEndsAt);
            binding.tvSubSubtitle.setText("Status: Masa Trial ⏳\nBerlaku hingga: " + formattedDate);
            binding.tvSubSubtitle.setTextColor(getColor(R.color.blue_info));
        } else {
            binding.tvSubSubtitle.setText("Status: Masa Aktif Habis ⚠️\nSilakan pilih paket di bawah untuk melanjutkan.");
            binding.tvSubSubtitle.setTextColor(getColor(R.color.danger_red));
        }
    }

    private void syncSubscriptionStatus() {
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
                            updateSubtitleText();
                            if (adapter != null) {
                                adapter.setLifetimeUser(sessionManager.isSubscriptionLifetime());
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                // ignore
            }
        });
    }

    private String formatDateString(String rawDate) {
        if (rawDate == null || rawDate.isEmpty()) return "-";
        try {
            String patternInput = rawDate.contains(" ") ? "yyyy-MM-dd HH:mm:ss" : "yyyy-MM-dd";
            java.text.SimpleDateFormat sdfInput = new java.text.SimpleDateFormat(patternInput, java.util.Locale.US);
            java.util.Date date = sdfInput.parse(rawDate);
            java.text.SimpleDateFormat sdfOutput = new java.text.SimpleDateFormat("dd MMM yyyy", new java.util.Locale("id", "ID"));
            return sdfOutput.format(date);
        } catch (Exception e) {
            return rawDate;
        }
    }

    private void setupToolbar() {
        binding.btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new SubscriptionPlanAdapter(this::checkoutPlan);
        binding.rvPlans.setLayoutManager(new LinearLayoutManager(this));
        binding.rvPlans.setAdapter(adapter);
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(R.color.primary);
        binding.swipeRefresh.setOnRefreshListener(() -> {
            syncSubscriptionStatus();
            loadPlans();
        });
    }

    private void loadPlans() {
        setLoading(true);
        binding.tvError.setVisibility(View.GONE);

        apiService.getSubscriptionPlans().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                setLoading(false);
                binding.swipeRefresh.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonObject body = response.body();
                        JsonObject dataObj = body.getAsJsonObject("data");
                        JsonArray plansArray = dataObj.getAsJsonArray("plans");

                        List<JsonObject> plansList = new ArrayList<>();
                        for (int i = 0; i < plansArray.size(); i++) {
                            plansList.add(plansArray.get(i).getAsJsonObject());
                        }

                        adapter.setData(plansList);
                        adapter.setLifetimeUser(sessionManager.isSubscriptionLifetime());
                    } catch (Exception e) {
                        binding.tvError.setVisibility(View.VISIBLE);
                        binding.tvError.setText("Gagal memproses data paket.");
                    }
                } else {
                    binding.tvError.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                setLoading(false);
                binding.swipeRefresh.setRefreshing(false);
                binding.tvError.setVisibility(View.VISIBLE);
                binding.tvError.setText(getString(R.string.tidak_ada_koneksi));
            }
        });
    }

    private void checkoutPlan(JsonObject plan) {
        String planCode = plan.get("code").getAsString();
        String planName = plan.get("name").getAsString();
        double planPrice = plan.get("price").getAsDouble();

        // Prevent click if user is already lifetime
        if (sessionManager.isSubscriptionLifetime()) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Akun Aktif Selamanya")
                    .setMessage("Akun Anda saat ini memiliki status Aktif Selamanya (Lifetime). Anda tidak perlu membeli paket lagi.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        // Confirmation message builder based on current subscription state
        String currentStatus = sessionManager.getSubscriptionStatus();
        String confirmationMessage;

        if ("trial".equals(currentStatus)) {
            confirmationMessage = "Anda saat ini berada dalam masa Trial.\n\n" +
                    "Dengan membeli paket " + planName + " (" + CurrencyHelper.formatRupiah(planPrice) + ")," +
                    " status akun Anda akan berubah menjadi Premium Aktif.\n\nApakah Anda ingin melanjutkan?";
        } else if ("active".equals(currentStatus) && sessionManager.isSubscriptionActive()) {
            String currentEndsAt = sessionManager.getSubscriptionEndsAt();
            String formattedDate = formatDateString(currentEndsAt);
            confirmationMessage = "Akun Premium Anda saat ini aktif hingga " + formattedDate + ".\n\n" +
                    "Dengan membeli paket " + planName + " (" + CurrencyHelper.formatRupiah(planPrice) + ")," +
                    " masa aktif premium Anda akan diperpanjang secara otomatis.\n\nApakah Anda ingin melanjutkan?";
        } else { // Expired or inactive
            confirmationMessage = "Akun Anda saat ini sudah berakhir (Expired).\n\n" +
                    "Dengan membeli paket " + planName + " (" + CurrencyHelper.formatRupiah(planPrice) + ")," +
                    " status akun Anda akan langsung aktif kembali.\n\nApakah Anda ingin melanjutkan?";
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Konfirmasi Pembelian")
                .setMessage(confirmationMessage)
                .setPositiveButton("YA, BAYAR", (dialog, which) -> executeCheckout(planCode, planName, planPrice))
                .setNegativeButton("BATAL", null)
                .show();
    }

    private void executeCheckout(String planCode, String planName, double planPrice) {
        setLoading(true);

        JsonObject body = new JsonObject();
        body.addProperty("plan_code", planCode);
        body.addProperty("keterangan", "Upgrade ke paket " + planName);

        apiService.subscriptionCheckout(body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                setLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonObject resBody = response.body();
                        sessionManager.updateAiChatLimit(resBody);
                        JsonObject data = resBody.getAsJsonObject("data");

                        // Extract payment details from nested structure
                        JsonObject transactionObj = data.getAsJsonObject("transaction");
                        String transactionId = transactionObj.get("invoice_no").getAsString();

                        JsonObject gatewayObj = data.getAsJsonObject("gateway");
                        double amount = gatewayObj.get("total_amount").getAsDouble();
                        String qrisImage = gatewayObj.has("qris_image") && !gatewayObj.get("qris_image").isJsonNull() ? gatewayObj.get("qris_image").getAsString() : "";
                        String expiredAt = gatewayObj.has("expired_at") && !gatewayObj.get("expired_at").isJsonNull() ? gatewayObj.get("expired_at").getAsString() : "";

                        // Save pending transaction details to SessionManager
                        sessionManager.savePendingTransaction(transactionId, amount, qrisImage, expiredAt, planName, planPrice);

                        // Start PaymentActivity
                        Intent intent = new Intent(SubscriptionActivity.this, PaymentActivity.class);
                        intent.putExtra("transaction_id", transactionId);
                        intent.putExtra("amount", amount);
                        intent.putExtra("qris_image", qrisImage);
                        intent.putExtra("expired_at", expiredAt);
                        intent.putExtra("plan_name", planName);
                        intent.putExtra("plan_price", planPrice);
                        startActivity(intent);
                        
                        finish(); // Finish subscription selection screen
                    } catch (Exception e) {
                        CurrencyHelper.showError(binding.getRoot(), "Gagal membuat invoice: " + e.getMessage());
                    }
                } else {
                    String errorMsg = "Checkout gagal";
                    try {
                        if (response.errorBody() != null) {
                            JsonObject err = new com.google.gson.JsonParser()
                                    .parse(response.errorBody().string()).getAsJsonObject();
                            if (err.has("message")) errorMsg = err.get("message").getAsString();
                        }
                    } catch (Exception ignored) {}
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
        binding.rvPlans.setVisibility(loading ? View.GONE : View.VISIBLE);
    }
}
