package com.example.kasirumkm2.ui;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.example.kasirumkm2.MainActivity;
import com.example.kasirumkm2.databinding.ActivitySplashBinding;
import com.example.kasirumkm2.session.SessionManager;
import com.example.kasirumkm2.ui.OnboardingActivity;

public class SplashActivity extends AppCompatActivity {

    private ActivitySplashBinding binding;
    private SessionManager sessionManager;

    // Total splash duration in ms
    private static final int SPLASH_DURATION = 2400;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        
        binding.tvVersion.setText("v" + com.example.kasirumkm2.utils.CurrencyHelper.getAppVersion(this));

        setupInitialState();
        runAnimations();
        startDelayTimer();
    }

    /**
     * Set all animated views to their initial (invisible/offset) state
     * before animations run.
     */
    private void setupInitialState() {
        // Top pill badge
        binding.tvBadge.setAlpha(0f);
        binding.tvBadge.setTranslationY(-20f);

        // Logo outer ring
        binding.cardLogoOuter.setAlpha(0f);
        binding.cardLogoOuter.setScaleX(0.8f);
        binding.cardLogoOuter.setScaleY(0.8f);

        // App name
        binding.tvAppName.setAlpha(0f);
        binding.tvAppName.setTranslationY(30f);

        // Subtitle
        binding.tvSubtitle.setAlpha(0f);
        binding.tvSubtitle.setTranslationY(20f);

        // Divider
        binding.viewDivider.setAlpha(0f);

        // Stats
        binding.layoutStats.setAlpha(0f);
        binding.layoutStats.setTranslationY(20f);

        // Bottom elements
        binding.tvLoading.setAlpha(0f);
        binding.progressBar.setAlpha(0f);
        binding.progressBar.setProgress(0);
    }

    /**
     * Chain all entrance animations with staggered delays.
     * Total animation window: ~800ms, then progress runs until SPLASH_DURATION.
     */
    private void runAnimations() {
        DecelerateInterpolator decel = new DecelerateInterpolator(2f);
        FastOutSlowInInterpolator fastOutSlow = new FastOutSlowInInterpolator();

        // 1. Badge slides down from top — delay 80ms
        binding.tvBadge.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(600)
                .setStartDelay(80)
                .setInterpolator(decel)
                .start();

        // 2. Logo scales + fades in — delay 180ms
        binding.cardLogoOuter.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(700)
                .setStartDelay(180)
                .setInterpolator(fastOutSlow)
                .start();

        // 3. App name slides up — delay 320ms
        binding.tvAppName.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(700)
                .setStartDelay(320)
                .setInterpolator(decel)
                .start();

        // 4. Subtitle fades in — delay 420ms
        binding.tvSubtitle.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(600)
                .setStartDelay(420)
                .setInterpolator(decel)
                .start();

        // 5. Divider fades in — delay 500ms
        binding.viewDivider.animate()
                .alpha(1f)
                .setDuration(500)
                .setStartDelay(500)
                .start();

        // 6. Stats row slides up — delay 580ms
        binding.layoutStats.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(600)
                .setStartDelay(580)
                .setInterpolator(decel)
                .start();

        // 7. Bottom loading elements — delay 700ms
        binding.tvLoading.animate()
                .alpha(1f)
                .setDuration(500)
                .setStartDelay(700)
                .start();

        binding.progressBar.animate()
                .alpha(1f)
                .setDuration(400)
                .setStartDelay(750)
                .withEndAction(this::animateProgressBar)
                .start();
    }

    /**
     * Animate progress bar from 0 → 100 over ~1400ms with a natural easing curve.
     * Runs after the bar fades in.
     */
    private void animateProgressBar() {
        ObjectAnimator progressAnimator = ObjectAnimator.ofInt(binding.progressBar, "progress", 0, 100);
        progressAnimator.setDuration(1400);
        progressAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        progressAnimator.start();

        // Pulse the loading text while progress runs
        animateLoadingTextPulse();
    }

    /**
     * Fade the "Memuat aplikasi..." text in and out repeatedly
     * to give a living, breathing feel.
     */
    private void animateLoadingTextPulse() {
        ValueAnimator pulseAnimator = ValueAnimator.ofFloat(0.35f, 0.75f);
        pulseAnimator.setDuration(900);
        pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.addUpdateListener(animation ->
                binding.tvLoading.setAlpha((float) animation.getAnimatedValue())
        );
        pulseAnimator.start();
    }

    /**
     * Navigate to the next screen after SPLASH_DURATION ms.
     * Order: check onboarding first, then session status.
     */
    private void startDelayTimer() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent;

            // Check if user has seen onboarding
            SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
            boolean onboardingDone = prefs.getBoolean("onboarding_completed", false);

            if (!onboardingDone) {
                // First time install — show onboarding
                intent = new Intent(SplashActivity.this, OnboardingActivity.class);
            } else if (sessionManager.isLoggedIn()) {
                intent = new Intent(SplashActivity.this, MainActivity.class);
            } else {
                intent = new Intent(SplashActivity.this, LoginActivity.class);
            }

            startActivity(intent);
            finish();

            // Smooth crossfade transition
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }, SPLASH_DURATION);
    }
}