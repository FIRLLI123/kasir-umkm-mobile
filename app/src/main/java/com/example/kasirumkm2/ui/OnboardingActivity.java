package com.example.kasirumkm2.ui;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.viewpager2.widget.ViewPager2;

import com.example.kasirumkm2.R;
import com.example.kasirumkm2.adapter.OnboardingAdapter;
import com.example.kasirumkm2.databinding.ActivityOnboardingBinding;

public class OnboardingActivity extends AppCompatActivity {

    private ActivityOnboardingBinding binding;
    private OnboardingAdapter adapter;
    private static final int TOTAL_SLIDES = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOnboardingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupViewPager();
        setupDots();
        setupButtons();
        runEntranceAnimation();
    }

    // ─── ViewPager2 ─────────────────────────────────────────────────────────────

    private void setupViewPager() {
        adapter = new OnboardingAdapter(this);
        binding.viewPager.setAdapter(adapter);

        // Disable over-scroll glow
        binding.viewPager.setOverScrollMode(View.OVER_SCROLL_NEVER);

        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateDots(position);
                updateButtons(position);
            }
        });
    }

    // ─── Dot Indicator ──────────────────────────────────────────────────────────

    private void setupDots() {
        binding.dot1.setSelected(true);
        binding.dot2.setSelected(false);
        binding.dot3.setSelected(false);
        binding.dot4.setSelected(false);
        binding.dot5.setSelected(false);
    }

    private void updateDots(int position) {
        binding.dot1.setSelected(position == 0);
        binding.dot2.setSelected(position == 1);
        binding.dot3.setSelected(position == 2);
        binding.dot4.setSelected(position == 3);
        binding.dot5.setSelected(position == 4);
    }

    // ─── Buttons ────────────────────────────────────────────────────────────────

    private void setupButtons() {
        binding.btnNext.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
            animateButtonPress(v, () -> {
                int current = binding.viewPager.getCurrentItem();
                if (current == TOTAL_SLIDES - 1) {
                    // Slide 4: "Masuk ke Tanya Kasir"
                    saveOnboardingDone();
                    navigateTo(LoginActivity.class);
                } else {
                    binding.viewPager.setCurrentItem(current + 1, true);
                }
            });
        });

        binding.btnDaftar.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
            animateButtonPress(v, () -> {
                saveOnboardingDone();
                navigateTo(RegisterActivity.class);
            });
        });

        binding.tvLewati.setOnClickListener(v -> {
            // Skip straight to slide 4 (not Login)
            binding.viewPager.setCurrentItem(TOTAL_SLIDES - 1, true);
        });
    }

    private void updateButtons(int position) {
        if (position == TOTAL_SLIDES - 1) {
            // Slide 5 (CTA): show both Login and Register buttons, hide "Lewati"
            binding.btnNext.setText("Masuk ke Tanya Kasir");
            binding.btnDaftar.setVisibility(View.VISIBLE);
            binding.tvLewati.setVisibility(View.GONE);
        } else {
            binding.btnDaftar.setVisibility(View.GONE);
            binding.tvLewati.setVisibility(View.VISIBLE);

            switch (position) {
                case 0:
                    binding.btnNext.setText("Yuk, kenalan sama Airin →");
                    break;
                case 1:
                    binding.btnNext.setText("Lanjut →");
                    break;
                case 2:
                    binding.btnNext.setText("Hampir selesai →");
                    break;
                case 3:
                    binding.btnNext.setText("Yuk Mulai 🤞");
                    break;
            }
        }
    }

    // ─── Animations ─────────────────────────────────────────────────────────────

    private void runEntranceAnimation() {
        // Bottom overlay fades in from below
        binding.bottomOverlay.setAlpha(0f);
        binding.bottomOverlay.setTranslationY(60f);
        binding.bottomOverlay.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay(200)
                .setInterpolator(new FastOutSlowInInterpolator())
                .start();
    }

    private void animateButtonPress(View view, Runnable onEnd) {
        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.96f);
        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.96f);
        ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(view, "scaleX", 0.96f, 1f);
        ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(view, "scaleY", 0.96f, 1f);

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

    // ─── Navigation ─────────────────────────────────────────────────────────────

    private void navigateTo(Class<?> cls) {
        // Fade out whole screen then navigate
        binding.getRoot().animate()
                .alpha(0f)
                .setDuration(250)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    startActivity(new Intent(OnboardingActivity.this, cls));
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    finish();
                })
                .start();
    }

    // ─── SharedPreferences ──────────────────────────────────────────────────────

    /**
     * Saves onboarding completion with version number.
     * Must match ONBOARDING_VERSION in SplashActivity.
     */
    private void saveOnboardingDone() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        prefs.edit()
                .putInt("onboarding_version", 1)        // must match SplashActivity.ONBOARDING_VERSION
                .putBoolean("onboarding_completed", true) // keep for backward compatibility
                .apply();
    }

    // ─── Back press handling ─────────────────────────────────────────────────────

    @Override
    public void onBackPressed() {
        int current = binding.viewPager.getCurrentItem();
        if (current > 0) {
            binding.viewPager.setCurrentItem(current - 1, true);
        } else {
            super.onBackPressed();
        }
    }
}
