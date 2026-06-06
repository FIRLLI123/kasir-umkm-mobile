package com.example.kasirumkm2.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.kasirumkm2.R;
import com.example.kasirumkm2.api.ApiClient;
import com.example.kasirumkm2.api.ApiService;
import com.example.kasirumkm2.databinding.ActivityPaymentBinding;
import com.example.kasirumkm2.session.SessionManager;
import com.example.kasirumkm2.utils.CurrencyHelper;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PaymentActivity extends AppCompatActivity {

    private ActivityPaymentBinding binding;
    private ApiService apiService;
    private SessionManager sessionManager;

    private String transactionId;
    private double amount;
    private String qrisImage;
    private String expiredAt;
    private String planName;
    private double planPrice;

    private CountDownTimer countDownTimer;
    private final Handler pollingHandler = new Handler(Looper.getMainLooper());
    private Runnable pollingRunnable;
    private static final long POLLING_INTERVAL_MS = 10000; // 10 seconds polling

    private boolean isPaid = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPaymentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = ApiClient.getApiService(this);
        sessionManager = new SessionManager(this);

        // Retrieve data from Intent
        transactionId = getIntent().getStringExtra("transaction_id");
        amount = getIntent().getDoubleExtra("amount", 0);
        qrisImage = getIntent().getStringExtra("qris_image");
        expiredAt = getIntent().getStringExtra("expired_at");
        planName = getIntent().getStringExtra("plan_name");
        planPrice = getIntent().getDoubleExtra("plan_price", 0);

        setupUI();
        startTimer();
    }

    private void setupUI() {
        binding.btnBack.setOnClickListener(v -> handleBackPress());
        binding.tvPaymentPlanName.setText(planName);
        binding.tvPaymentInvoiceNo.setText(transactionId);
        binding.tvPaymentAmount.setText(CurrencyHelper.formatRupiah(amount));

        // Load base64 QRIS
        if (qrisImage != null && qrisImage.startsWith("data:image")) {
            try {
                String base64Data = qrisImage.substring(qrisImage.indexOf(",") + 1);
                byte[] decodedString = Base64.decode(base64Data, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                binding.ivQrisCode.setImageBitmap(decodedByte);
            } catch (Exception e) {
                binding.tvPaymentStatus.setText("Gagal merender kode QRIS.");
            }
        }

        binding.btnCheckStatus.setOnClickListener(v -> checkPaymentStatus(true));
    }

    private void startTimer() {
        long diffMs = 15 * 60 * 1000; // 15 mins default fallback
        if (expiredAt != null && !expiredAt.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                Date date = sdf.parse(expiredAt);
                if (date != null) {
                    diffMs = date.getTime() - System.currentTimeMillis();
                }
            } catch (Exception e) {
                // keep default fallback
            }
        }

        if (diffMs <= 0) {
            binding.tvPaymentCountdown.setText("Expired");
            binding.tvPaymentStatus.setText("Invoice kadaluarsa");
            binding.btnCheckStatus.setEnabled(false);
            return;
        }

        countDownTimer = new CountDownTimer(diffMs, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long minutes = (millisUntilFinished / 1000) / 60;
                long seconds = (millisUntilFinished / 1000) % 60;
                binding.tvPaymentCountdown.setText(String.format(Locale.US, "%02d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                binding.tvPaymentCountdown.setText("Expired");
                binding.tvPaymentStatus.setText("Invoice kadaluarsa");
                binding.btnCheckStatus.setEnabled(false);
                stopPolling();
                showExpiredDialog();
            }
        }.start();
    }

    private void startPolling() {
        stopPolling();
        pollingRunnable = new Runnable() {
            @Override
            public void run() {
                checkPaymentStatus(false);
                pollingHandler.postDelayed(this, POLLING_INTERVAL_MS);
            }
        };
        pollingHandler.postDelayed(pollingRunnable, POLLING_INTERVAL_MS);
    }

    private void stopPolling() {
        pollingHandler.removeCallbacksAndMessages(null);
    }

    private void checkPaymentStatus(boolean isManual) {
        if (isPaid) return;

        if (isManual) {
            binding.pbStatus.setVisibility(View.VISIBLE);
            binding.tvPaymentStatus.setText("Mengecek status...");
            binding.btnCheckStatus.setEnabled(false);
        }

        apiService.getPaymentStatus(transactionId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (isManual) {
                    binding.pbStatus.setVisibility(View.GONE);
                    binding.btnCheckStatus.setEnabled(true);
                }

                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonObject body = response.body();
                        JsonObject data = body.getAsJsonObject("data");
                        JsonObject transactionObj = data.getAsJsonObject("transaction");
                        String status = transactionObj.get("status").getAsString();

                        if ("paid".equalsIgnoreCase(status)) {
                            isPaid = true;
                            stopPolling();
                            if (countDownTimer != null) {
                                countDownTimer.cancel();
                            }
                            sessionManager.clearPendingTransaction();
                            syncActiveSubscription();
                        } else if ("expired".equalsIgnoreCase(status)) {
                            stopPolling();
                            if (countDownTimer != null) {
                                countDownTimer.cancel();
                            }
                            sessionManager.clearPendingTransaction();
                            showExpiredDialog();
                        } else {
                            // still pending
                            binding.tvPaymentStatus.setText("Menunggu pembayaran...");
                            if (isManual) {
                                Toast.makeText(PaymentActivity.this, "Pembayaran belum terdeteksi. Silakan coba sesaat lagi.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } catch (Exception e) {
                        if (isManual) {
                            CurrencyHelper.showError(binding.getRoot(), "Gagal parsing status pembayaran");
                        }
                    }
                } else {
                    if (isManual) {
                        CurrencyHelper.showError(binding.getRoot(), "Gagal mengecek status pembayaran");
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                if (isManual) {
                    binding.pbStatus.setVisibility(View.GONE);
                    binding.btnCheckStatus.setEnabled(true);
                    CurrencyHelper.showError(binding.getRoot(), getString(R.string.tidak_ada_koneksi));
                }
            }
        });
    }

    private void syncActiveSubscription() {
        apiService.getProfile().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonObject body = response.body();
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
                
                // Then fetch and sync active subscription
                apiService.getSubscriptionActive().enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            try {
                                JsonObject body = response.body();
                                if (body.has("data") && !body.get("data").isJsonNull()) {
                                    JsonObject sub = body.getAsJsonObject("data");
                                    String subStatus = sub.has("status") && !sub.get("status").isJsonNull() ? sub.get("status").getAsString() : "active";
                                    boolean subActive = sub.has("is_active") && !sub.get("is_active").isJsonNull() && sub.get("is_active").getAsBoolean();
                                    boolean subLifetime = sub.has("is_lifetime") && !sub.get("is_lifetime").isJsonNull() && sub.get("is_lifetime").getAsBoolean();
                                    String trialEndsAt = sub.has("trial_ends_at") && !sub.get("trial_ends_at").isJsonNull() ? sub.get("trial_ends_at").getAsString() : "";
                                    String endsAt = sub.has("ends_at") && !sub.get("ends_at").isJsonNull() ? sub.get("ends_at").getAsString() : "";

                                    sessionManager.saveSubscription(subStatus, subActive, subLifetime, trialEndsAt, endsAt);
                                }
                            } catch (Exception ignored) {}
                        }
                        showSuccessDialog();
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        showSuccessDialog();
                    }
                });
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                // If profile failed, still try subscription
                apiService.getSubscriptionActive().enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            try {
                                JsonObject body = response.body();
                                if (body.has("data") && !body.get("data").isJsonNull()) {
                                    JsonObject sub = body.getAsJsonObject("data");
                                    String subStatus = sub.has("status") && !sub.get("status").isJsonNull() ? sub.get("status").getAsString() : "active";
                                    boolean subActive = sub.has("is_active") && !sub.get("is_active").isJsonNull() && sub.get("is_active").getAsBoolean();
                                    boolean subLifetime = sub.has("is_lifetime") && !sub.get("is_lifetime").isJsonNull() && sub.get("is_lifetime").getAsBoolean();
                                    String trialEndsAt = sub.has("trial_ends_at") && !sub.get("trial_ends_at").isJsonNull() ? sub.get("trial_ends_at").getAsString() : "";
                                    String endsAt = sub.has("ends_at") && !sub.get("ends_at").isJsonNull() ? sub.get("ends_at").getAsString() : "";

                                    sessionManager.saveSubscription(subStatus, subActive, subLifetime, trialEndsAt, endsAt);
                                }
                            } catch (Exception ignored) {}
                        }
                        showSuccessDialog();
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        showSuccessDialog();
                    }
                });
            }
        });
    }

    private void showSuccessDialog() {
        String statusText;
        if (sessionManager.isSubscriptionLifetime()) {
            statusText = "Status Akun: Aktif Selamanya (Lifetime) 👑";
        } else {
            String endsAt = sessionManager.getSubscriptionEndsAt();
            String formattedDate = endsAt;
            if (endsAt != null && !endsAt.isEmpty()) {
                try {
                    String patternInput = endsAt.contains(" ") ? "yyyy-MM-dd HH:mm:ss" : "yyyy-MM-dd";
                    java.text.SimpleDateFormat sdfInput = new java.text.SimpleDateFormat(patternInput, java.util.Locale.US);
                    java.util.Date date = sdfInput.parse(endsAt);
                    java.text.SimpleDateFormat sdfOutput = new java.text.SimpleDateFormat("dd MMM yyyy", new java.util.Locale("id", "ID"));
                    formattedDate = sdfOutput.format(date);
                } catch (Exception ignored) {}
                statusText = "Masa Aktif Premium Hingga: " + formattedDate + " ⭐";
            } else {
                statusText = "Masa Aktif Premium Aktif ⭐";
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Pembayaran Berhasil! 🎉")
                .setMessage("Paket premium " + planName + " Anda telah aktif. Nikmati seluruh fitur premium kami sekarang!\n\n" + statusText)
                .setCancelable(false)
                .setPositiveButton("MULAI KASIR", (dialog, which) -> finish())
                .show();
    }

    private void showExpiredDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Pembayaran Expired")
                .setMessage("Batas waktu pembayaran invoice ini telah habis. Silakan pilih paket kembali.")
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> finish())
                .show();
    }

    private void handleBackPress() {
        new AlertDialog.Builder(this)
                .setTitle("Kembali?")
                .setMessage("Halaman pembayaran ini dapat ditutup. Anda dapat mengecek status langganan nanti di menu Pengaturan.")
                .setPositiveButton("YA, KEMBALI", (dialog, which) -> finish())
                .setNegativeButton("TIDAK", null)
                .show();
    }

    @Override
    public void onBackPressed() {
        handleBackPress();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopPolling();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPolling();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}
