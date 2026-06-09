package com.example.kasirumkm2.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kasirumkm2.R;
import com.example.kasirumkm2.databinding.ItemOnboardingSlideBinding;

public class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.SlideViewHolder> {

    private final Context context;

    // Slide data
    private static final int[] AIRIN_IMAGES = {
            R.drawable.welcome,
            R.drawable.menjelaskan,
            R.drawable.goodjob,
            R.drawable.marah,
            R.drawable.senang
    };

    private static final String[] TITLES = {
            "Selamat datang di\nTanya Kasir! 🎉",
            "Kenalan sama Airin\ndulu, yuk!",
            "Semua yang kamu\nbutuhkan ada di sini ✨",
            "Eh tapi, awas ya! 😤",
            "Siap memulai? 🚀"
    };

    private static final String[] DESCRIPTIONS = {
            "Aplikasi kasir modern yang punya asisten AI pribadi buat bantu usaha kamu.",
            "",  // handled by info card
            "Lengkap dengan stok, pembayaran, scan barcode, sampai dashboard omzet & margin.",
            "",  // handled by warning card
            "Masuk atau daftar untuk mulai bersama Airin."
    };

    // Slide types
    public static final int TYPE_NORMAL   = 0;
    public static final int TYPE_INFOCARD = 1;
    public static final int TYPE_CHIPS    = 2;
    public static final int TYPE_WARNING  = 3;
    public static final int TYPE_AUTH_CTA = 4;

    private static final int[] SLIDE_TYPES = {
            TYPE_NORMAL, TYPE_INFOCARD, TYPE_CHIPS, TYPE_WARNING, TYPE_AUTH_CTA
    };

    public OnboardingAdapter(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public SlideViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemOnboardingSlideBinding b = ItemOnboardingSlideBinding.inflate(
                LayoutInflater.from(context), parent, false);
        return new SlideViewHolder(b);
    }

    @Override
    public void onBindViewHolder(@NonNull SlideViewHolder holder, int position) {
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        return AIRIN_IMAGES.length;
    }

    // ─── ViewHolder ──────────────────────────────────────────────────────────────

    class SlideViewHolder extends RecyclerView.ViewHolder {

        private final ItemOnboardingSlideBinding b;

        SlideViewHolder(ItemOnboardingSlideBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }

        void bind(int position) {
            // Airin image
            b.ivAirin.setImageResource(AIRIN_IMAGES[position]);

            // Title & Description
            b.tvTitle.setText(TITLES[position]);
            b.tvDescription.setText(DESCRIPTIONS[position]);

            int type = SLIDE_TYPES[position];

            // Background
            applyBackground(type);

            // Content sections
            b.layoutInfoCard.setVisibility(type == TYPE_INFOCARD ? View.VISIBLE : View.GONE);
            b.layoutChips.setVisibility(type == TYPE_CHIPS ? View.VISIBLE : View.GONE);
            b.layoutWarningCard.setVisibility(type == TYPE_WARNING ? View.VISIBLE : View.GONE);
            b.tvDescription.setVisibility(
                    (type == TYPE_INFOCARD || type == TYPE_WARNING) ? View.GONE : View.VISIBLE);

            // AI Badge (slide 2 only)
            b.layoutAiBadge.setVisibility(type == TYPE_INFOCARD ? View.VISIBLE : View.GONE);

            // Run entrance animations for this slide
            runSlideEntrance();
        }

        private void applyBackground(int type) {
            switch (type) {
                case TYPE_NORMAL:
                    b.slideRoot.setBackgroundResource(R.drawable.bg_onboard_slide1);
                    break;
                case TYPE_INFOCARD:
                    b.slideRoot.setBackgroundResource(R.drawable.bg_onboard_slide2);
                    break;
                case TYPE_CHIPS:
                    b.slideRoot.setBackgroundColor(0xFFFFFFFF);
                    break;
                case TYPE_WARNING:
                    b.slideRoot.setBackgroundResource(R.drawable.bg_onboard_slide_warning);
                    break;
                case TYPE_AUTH_CTA:
                    b.slideRoot.setBackgroundResource(R.drawable.bg_onboard_slide4);
                    break;
            }
        }

        private void runSlideEntrance() {
            // Airin image: slide up + fade in
            b.ivAirin.setAlpha(0f);
            b.ivAirin.setTranslationY(60f);
            b.ivAirin.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(500)
                    .setStartDelay(100)
                    .setInterpolator(new android.view.animation.OvershootInterpolator(1.1f))
                    .withEndAction(this::startIdleAnimation)
                    .start();

            // Title: fade in + slide up
            b.tvTitle.setAlpha(0f);
            b.tvTitle.setTranslationY(20f);
            b.tvTitle.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(350)
                    .setStartDelay(350)
                    .start();

            // Content area: fade in
            b.contentArea.setAlpha(0f);
            b.contentArea.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .setStartDelay(500)
                    .start();
        }

        private void startIdleAnimation() {
            ObjectAnimatorHelper.startIdleFloat(b.ivAirin);
        }
    }

    // ─── Idle animation helper ───────────────────────────────────────────────────

    public static class ObjectAnimatorHelper {
        public static void startIdleFloat(View view) {
            android.animation.ObjectAnimator scaleX =
                    android.animation.ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.03f, 1f);
            android.animation.ObjectAnimator scaleY =
                    android.animation.ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.03f, 1f);
            scaleX.setRepeatCount(android.animation.ValueAnimator.INFINITE);
            scaleY.setRepeatCount(android.animation.ValueAnimator.INFINITE);
            scaleX.setDuration(2000);
            scaleY.setDuration(2000);
            scaleX.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
            scaleY.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
            android.animation.AnimatorSet idle = new android.animation.AnimatorSet();
            idle.playTogether(scaleX, scaleY);
            idle.start();
        }
    }
}
