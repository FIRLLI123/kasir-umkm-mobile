package com.example.kasirumkm2.ui;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.animation.OvershootInterpolator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.example.kasirumkm2.MainActivity;
import com.example.kasirumkm2.R;
import com.example.kasirumkm2.api.ApiClient;
import com.example.kasirumkm2.api.ApiService;
import com.example.kasirumkm2.data.RegisterRequest;
import com.example.kasirumkm2.databinding.ActivityRegisterBinding;
import com.example.kasirumkm2.session.SessionManager;
import com.example.kasirumkm2.utils.AirinDialog;
import com.example.kasirumkm2.utils.CurrencyHelper;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private SessionManager sessionManager;
    private ApiService apiService;
    private String currentCaptchaKey = "";

    // Current step: 1, 2, or 3
    private int currentStep = 1;
    private static final int TOTAL_STEPS = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getApiService(this);

        setupListeners();
        updateStepUI(1, false);
        loadCaptcha();
        runAirinEntrance(binding.ivAirinStep1);
    }

    // ─── Listeners ───────────────────────────────────────────────────────────────

    private void setupListeners() {
        binding.btnNext.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
            animateButtonPress(v, this::handleNextButton);
        });

        binding.btnBack.setOnClickListener(v -> onBackPressed());
        binding.tvLoginLink.setOnClickListener(v -> finish());
        binding.btnRefreshCaptcha.setOnClickListener(v -> loadCaptcha());

        // Clear errors on focus — Step 1
        binding.etCompanyName.setOnFocusChangeListener((v, f) -> { if (f) binding.tilCompanyName.setError(null); });
        binding.etName.setOnFocusChangeListener((v, f) -> { if (f) binding.tilName.setError(null); });
        binding.etAddress.setOnFocusChangeListener((v, f) -> { if (f) binding.tilAddress.setError(null); });

        // Clear errors on focus — Step 2
        binding.etPhone.setOnFocusChangeListener((v, f) -> { if (f) binding.tilPhone.setError(null); });
        binding.etEmail.setOnFocusChangeListener((v, f) -> { if (f) binding.tilEmail.setError(null); });
        binding.etPassword.setOnFocusChangeListener((v, f) -> { if (f) binding.tilPassword.setError(null); });
        binding.etPasswordConfirmation.setOnFocusChangeListener((v, f) -> { if (f) binding.tilPasswordConfirmation.setError(null); });

        // Clear errors on focus — Step 3
        binding.etCaptcha.setOnFocusChangeListener((v, f) -> { if (f) binding.tilCaptcha.setError(null); });
    }

    // ─── Step Navigation ──────────────────────────────────────────────────────────

    private void handleNextButton() {
        switch (currentStep) {
            case 1:
                if (validateStep1()) goToStep(2);
                break;
            case 2:
                if (validateStep2()) {
                    populateSummary();
                    goToStep(3);
                }
                break;
            case 3:
                attemptRegister();
                break;
        }
    }

    private void goToStep(int step) {
        int fromStep = currentStep;
        currentStep = step;
        animateStepTransition(fromStep, step);
        updateStepUI(step, true);
    }

    private void updateStepUI(int step, boolean animate) {
        // Update header text
        switch (step) {
            case 1:
                binding.tvStepTitle.setText("Data Toko");
                binding.tvStepSubtitle.setText("Langkah 1 dari 3");
                binding.tvStepBadge.setText("1/3");
                binding.btnNext.setText("Lanjut →");
                break;
            case 2:
                binding.tvStepTitle.setText("Data Akun");
                binding.tvStepSubtitle.setText("Langkah 2 dari 3");
                binding.tvStepBadge.setText("2/3");
                binding.btnNext.setText("Lanjut →");
                break;
            case 3:
                binding.tvStepTitle.setText("Konfirmasi");
                binding.tvStepSubtitle.setText("Langkah 3 dari 3 — Hampir selesai! 🎉");
                binding.tvStepBadge.setText("3/3");
                binding.btnNext.setText("Daftar Sekarang! 🚀");
                break;
        }

        // Update progress bars with animation
        updateProgressBars(step, animate);
    }

    private void updateProgressBars(int step, boolean animate) {
        int activeColor   = 0xFF0EA5A0;
        int inactiveColor = 0xFFE2E8F0;

        if (animate) {
            animateProgressBar(binding.progressStep1, step >= 1 ? activeColor : inactiveColor);
            animateProgressBar(binding.progressStep2, step >= 2 ? activeColor : inactiveColor);
            animateProgressBar(binding.progressStep3, step >= 3 ? activeColor : inactiveColor);
        } else {
            binding.progressStep1.setBackgroundColor(activeColor);
            binding.progressStep2.setBackgroundColor(inactiveColor);
            binding.progressStep3.setBackgroundColor(inactiveColor);
        }
    }

    private void animateProgressBar(View bar, int targetColor) {
        bar.setBackgroundColor(targetColor);
        bar.animate().scaleX(1.05f).setDuration(150)
                .withEndAction(() -> bar.animate().scaleX(1f).setDuration(100).start())
                .start();
    }

    // ─── Step transition animation ────────────────────────────────────────────────

    private void animateStepTransition(int from, int to) {
        View fromView = getStepLayout(from);
        View toView   = getStepLayout(to);

        if (fromView == null || toView == null) return;

        boolean forward = to > from;
        float slideOut = forward ? -80f : 80f;
        float slideIn  = forward ?  80f : -80f;

        // Slide out + fade current
        fromView.animate()
                .translationX(slideOut)
                .alpha(0f)
                .setDuration(250)
                .setInterpolator(new FastOutSlowInInterpolator())
                .withEndAction(() -> {
                    fromView.setVisibility(View.GONE);
                    fromView.setTranslationX(0f);
                    fromView.setAlpha(1f);
                })
                .start();

        // Prepare next
        toView.setTranslationX(slideIn);
        toView.setAlpha(0f);
        toView.setVisibility(View.VISIBLE);

        // Slide in + fade next
        toView.animate()
                .translationX(0f)
                .alpha(1f)
                .setDuration(300)
                .setStartDelay(100)
                .setInterpolator(new FastOutSlowInInterpolator())
                .withStartAction(() -> {
                    // Airin entrance animation
                    View airin = getAirinImage(to);
                    if (airin != null) runAirinEntrance(airin);
                })
                .start();
    }

    private View getStepLayout(int step) {
        switch (step) {
            case 1: return binding.layoutStep1;
            case 2: return binding.layoutStep2;
            case 3: return binding.layoutStep3;
            default: return null;
        }
    }

    private View getAirinImage(int step) {
        switch (step) {
            case 1: return binding.ivAirinStep1;
            case 2: return binding.ivAirinStep2;
            case 3: return binding.ivAirinStep3;
            default: return null;
        }
    }

    private void runAirinEntrance(View airin) {
        airin.setAlpha(0f);
        airin.setTranslationY(30f);
        airin.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setStartDelay(200)
                .setInterpolator(new OvershootInterpolator(1.2f))
                .start();
    }

    // ─── Validation ───────────────────────────────────────────────────────────────

    private boolean validateStep1() {
        boolean valid = true;

        String companyName = binding.etCompanyName.getText().toString().trim();
        String name        = binding.etName.getText().toString().trim();

        if (TextUtils.isEmpty(companyName)) {
            binding.tilCompanyName.setError("Nama usaha wajib diisi ya~");
            shakeView(binding.tilCompanyName);
            valid = false;
        }
        if (TextUtils.isEmpty(name)) {
            binding.tilName.setError("Nama pemilik wajib diisi ya~");
            shakeView(binding.tilName);
            valid = false;
        }

        return valid;
    }

    private boolean validateStep2() {
        boolean valid = true;

        String phone    = binding.etPhone.getText().toString().trim();
        String email    = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        String passConf = binding.etPasswordConfirmation.getText().toString().trim();

        if (TextUtils.isEmpty(phone)) {
            binding.tilPhone.setError("Nomor WA wajib diisi ya~");
            shakeView(binding.tilPhone);
            valid = false;
        }
        if (TextUtils.isEmpty(email)) {
            binding.tilEmail.setError("Email wajib diisi ya~");
            shakeView(binding.tilEmail);
            valid = false;
        }
        if (TextUtils.isEmpty(password)) {
            binding.tilPassword.setError("Password wajib diisi ya~");
            shakeView(binding.tilPassword);
            valid = false;
        } else if (password.length() < 8) {
            binding.tilPassword.setError("Password minimal 8 karakter ya~");
            shakeView(binding.tilPassword);
            valid = false;
        }
        if (TextUtils.isEmpty(passConf)) {
            binding.tilPasswordConfirmation.setError("Konfirmasi password dulu ya~");
            shakeView(binding.tilPasswordConfirmation);
            valid = false;
        }
        if (valid && !password.equals(passConf)) {
            binding.tilPasswordConfirmation.setError("Password-nya beda nih~ Coba cek lagi ya!");
            shakeView(binding.tilPasswordConfirmation);
            valid = false;
        }

        return valid;
    }

    // ─── Summary ──────────────────────────────────────────────────────────────────

    private void populateSummary() {
        binding.tvSummaryCompany.setText(binding.etCompanyName.getText().toString().trim());
        binding.tvSummaryName.setText(binding.etName.getText().toString().trim());
        binding.tvSummaryEmail.setText(binding.etEmail.getText().toString().trim());
    }

    // ─── Register API ─────────────────────────────────────────────────────────────

    private void attemptRegister() {
        String captchaValue = binding.etCaptcha.getText().toString().trim();
        if (TextUtils.isEmpty(captchaValue)) {
            binding.tilCaptcha.setError("Isi kode captcha dulu ya~");
            shakeView(binding.tilCaptcha);
            return;
        }

        setLoading(true);

        String companyName = binding.etCompanyName.getText().toString().trim();
        String name        = binding.etName.getText().toString().trim();
        String email       = binding.etEmail.getText().toString().trim();
        String phone       = binding.etPhone.getText().toString().trim();
        String address     = binding.etAddress.getText().toString().trim();
        String password    = binding.etPassword.getText().toString().trim();
        String passConf    = binding.etPasswordConfirmation.getText().toString().trim();

        RegisterRequest req = new RegisterRequest(
                companyName, name, email, password, passConf,
                phone, address,
                sessionManager.getDeviceId(),
                sessionManager.getDeviceName(),
                currentCaptchaKey, captchaValue
        );

        apiService.register(req).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonObject body = response.body();
                        JsonObject data = body.getAsJsonObject("data");
                        String token = data.get("token").getAsString();

                        JsonObject user = data.getAsJsonObject("user");
                        int userId      = user.get("id").getAsInt();
                        String userName = user.get("name").getAsString();
                        String userEmail = user.get("email").getAsString();
                        String role     = user.has("role") ? user.get("role").getAsString() : "admin";

                        sessionManager.saveSession(token, userId, userName, userEmail, role);

                        if (data.has("company") && !data.get("company").isJsonNull()) {
                            JsonObject comp = data.getAsJsonObject("company");
                            int compId      = comp.get("id").getAsInt();
                            String compName = comp.get("company_name").getAsString();
                            String compCode = comp.get("company_code").getAsString();
                            int ownerUserId = comp.has("owner_user_id") && !comp.get("owner_user_id").isJsonNull()
                                    ? comp.get("owner_user_id").getAsInt() : 0;
                            sessionManager.saveCompany(compId, compName, compCode, ownerUserId);
                        }

                        if (data.has("is_owner") && !data.get("is_owner").isJsonNull()) {
                            sessionManager.saveIsOwner(data.get("is_owner").getAsBoolean());
                        }

                        if (data.has("subscription") && !data.get("subscription").isJsonNull()) {
                            JsonObject sub  = data.getAsJsonObject("subscription");
                            String subStatus = sub.has("status") && !sub.get("status").isJsonNull() ? sub.get("status").getAsString() : "trial";
                            boolean subActive   = sub.has("is_active") && !sub.get("is_active").isJsonNull() && sub.get("is_active").getAsBoolean();
                            boolean subLifetime = sub.has("is_lifetime") && !sub.get("is_lifetime").isJsonNull() && sub.get("is_lifetime").getAsBoolean();
                            String trialEnds = sub.has("trial_ends_at") && !sub.get("trial_ends_at").isJsonNull() ? sub.get("trial_ends_at").getAsString() : "";
                            String endsAt    = sub.has("ends_at") && !sub.get("ends_at").isJsonNull() ? sub.get("ends_at").getAsString() : "";
                            sessionManager.saveSubscription(subStatus, subActive, subLifetime, trialEnds, endsAt);
                        }

                        sessionManager.updateAiChatLimit(body);
                        sessionManager.saveSavedCredentials(email, password);
                        ApiClient.resetClient();

                        // Dialog berhasil daftar!
                        AirinDialog.showSuccess(
                                RegisterActivity.this,
                                "Yeay, berhasil daftar! 🎉",
                                "Selamat datang, " + name + "!\nAirin senang banget kamu bergabung~ Yuk mulai petualangan kasirmu! 😊",
                                () -> navigateToMain()
                        );

                    } catch (Exception e) {
                        AirinDialog.showError(
                                RegisterActivity.this,
                                "Ups, ada yang aneh nih 😅",
                                "Registrasi gagal: " + e.getMessage() + "\nCoba lagi ya~",
                                null
                        );
                        loadCaptcha();
                    }
                } else {
                    String errorMsg = getString(R.string.register_failed);
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            JsonObject errorJson = new com.google.gson.JsonParser().parse(errorBody).getAsJsonObject();
                            if (errorJson.has("message")) {
                                errorMsg = errorJson.get("message").getAsString();
                            }
                        }
                    } catch (Exception ignored) { }
                    AirinDialog.showError(
                            RegisterActivity.this,
                            "Gagal Daftar 😢",
                            errorMsg + "\nTenang, coba lagi ya~ Airin tunggu! 👊",
                            null
                    );
                    loadCaptcha();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                setLoading(false);
                AirinDialog.showOffline(RegisterActivity.this, null);
            }
        });
    }

    // ─── Captcha ──────────────────────────────────────────────────────────────────

    private void loadCaptcha() {
        binding.progressBar.setVisibility(View.VISIBLE);
        apiService.getCaptcha().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                binding.progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonObject data = response.body().getAsJsonObject("data");
                        currentCaptchaKey = data.get("captcha_key").getAsString();
                        String base64 = data.get("captcha_image").getAsString();
                        if (base64.contains(",")) base64 = base64.split(",")[1];
                        byte[] decoded = android.util.Base64.decode(base64, android.util.Base64.DEFAULT);
                        android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                        binding.ivCaptcha.setImageBitmap(bmp);
                        binding.etCaptcha.setText("");
                    } catch (Exception e) {
                        android.widget.Toast.makeText(RegisterActivity.this, "Gagal memproses captcha", android.widget.Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                android.widget.Toast.makeText(RegisterActivity.this, "Gagal menghubungi server", android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ─── UI State ─────────────────────────────────────────────────────────────────

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnNext.setEnabled(!loading);
        binding.btnNext.setText(loading ? "Mendaftarkan..." : "Daftar Sekarang! 🚀");
        binding.btnNext.setAlpha(loading ? 0.7f : 1f);
    }

    // ─── Animation Helpers ────────────────────────────────────────────────────────

    private void animateButtonPress(View view, Runnable onEnd) {
        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.96f);
        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.96f);
        ObjectAnimator scaleUpX   = ObjectAnimator.ofFloat(view, "scaleX", 0.96f, 1f);
        ObjectAnimator scaleUpY   = ObjectAnimator.ofFloat(view, "scaleY", 0.96f, 1f);
        AnimatorSet down = new AnimatorSet();
        down.playTogether(scaleDownX, scaleDownY);
        down.setDuration(70);
        AnimatorSet up = new AnimatorSet();
        up.playTogether(scaleUpX, scaleUpY);
        up.setDuration(80);
        up.setInterpolator(new OvershootInterpolator(2f));
        AnimatorSet full = new AnimatorSet();
        full.playSequentially(down, up);
        full.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (onEnd != null) onEnd.run();
            }
        });
        full.start();
    }

    private void shakeView(View view) {
        view.animate()
                .translationX(10f).setDuration(70)
                .withEndAction(() -> view.animate()
                        .translationX(-10f).setDuration(70)
                        .withEndAction(() -> view.animate()
                                .translationX(5f).setDuration(50)
                                .withEndAction(() -> view.animate()
                                        .translationX(0f).setDuration(50).start())
                                .start())
                        .start())
                .start();
    }

    // ─── Back press ───────────────────────────────────────────────────────────────

    @Override
    public void onBackPressed() {
        if (currentStep > 1) {
            goToStep(currentStep - 1);
        } else {
            super.onBackPressed();
        }
    }

    // ─── Navigation ───────────────────────────────────────────────────────────────

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
