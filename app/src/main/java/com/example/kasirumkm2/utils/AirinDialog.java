package com.example.kasirumkm2.utils;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.kasirumkm2.R;

/**
 * AirinDialog — Custom dialog dengan karakter Airin chibi.
 *
 * Digunakan untuk memberi feedback berhasil / gagal dengan cara yang menyenangkan.
 *
 * Penggunaan:
 *   // Berhasil login
 *   AirinDialog.showSuccess(context, "Yeay, berhasil masuk!", "Halo lagi~ Airin senang kamu kembali! 😊", null);
 *
 *   // Gagal dengan callback
 *   AirinDialog.showError(context, "Ups, ada yang salah!", "Sepertinya ada masalah deh...", null);
 *
 *   // Dengan aksi setelah OK diklik
 *   AirinDialog.showSuccess(context, "Yeay!", "Berhasil daftar!", () -> navigateToMain());
 */
public class AirinDialog {

    public enum Type {
        SUCCESS,
        ERROR,
        WARNING,
        SLEEPY   // untuk kondisi offline/timeout — pakai ngantuk.png
    }

    /**
     * Tampilkan dialog sukses dengan Airin senang 🎉
     */
    public static void showSuccess(Context context, String title, String message, Runnable onOk) {
        show(context, Type.SUCCESS, title, message, "Yeay, Oke! 🎉", onOk);
    }

    /**
     * Tampilkan dialog error dengan Airin sedih 😢
     */
    public static void showError(Context context, String title, String message, Runnable onOk) {
        show(context, Type.ERROR, title, message, "Oke, coba lagi deh", onOk);
    }

    /**
     * Tampilkan dialog offline/timeout dengan Airin ngantuk 😴
     */
    public static void showOffline(Context context, Runnable onOk) {
        show(context, Type.SLEEPY,
                "Koneksinya Kemana Nih? 😴",
                "Airin ngantuk nunggu koneksi kamu~\nCek internet lalu coba lagi ya!",
                "Siap, coba lagi!",
                onOk);
    }

    /**
     * Tampilkan dialog kaget/warning ⚠️
     */
    public static void showWarning(Context context, String title, String message, Runnable onOk) {
        show(context, Type.WARNING, title, message, "Oke, mengerti!", onOk);
    }

    // ─── Core show method ────────────────────────────────────────────────────────

    public static void show(Context context, Type type, String title, String message,
                            String btnLabel, Runnable onOk) {

        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);

        View view = LayoutInflater.from(context).inflate(R.layout.dialog_airin, null);
        dialog.setContentView(view);

        // Transparent rounded window
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // ── Bind views ──
        ImageView ivAirin   = view.findViewById(R.id.ivAirinDialog);
        TextView  tvTitle   = view.findViewById(R.id.tvDialogTitle);
        TextView  tvMessage = view.findViewById(R.id.tvDialogMessage);
        Button    btnOk     = view.findViewById(R.id.btnDialogOk);

        // ── Apply type-specific content ──
        switch (type) {
            case SUCCESS:
                ivAirin.setImageResource(R.drawable.senang);
                btnOk.setBackgroundResource(R.drawable.bg_btn_dialog_success);
                break;

            case ERROR:
                ivAirin.setImageResource(R.drawable.sedih_error);
                btnOk.setBackgroundResource(R.drawable.bg_btn_dialog_error);
                break;

            case WARNING:
                ivAirin.setImageResource(R.drawable.kaget);
                btnOk.setBackgroundResource(R.drawable.bg_btn_dialog_error);
                break;

            case SLEEPY:
                ivAirin.setImageResource(R.drawable.ngantuk);
                btnOk.setBackgroundResource(R.drawable.bg_button_teal);
                break;
        }

        tvTitle.setText(title);
        tvMessage.setText(message);
        btnOk.setText(btnLabel);

        // ── OK button click ──
        btnOk.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
            animateDismiss(view, () -> {
                dialog.dismiss();
                if (onOk != null) onOk.run();
            });
        });

        dialog.show();

        // Set dialog width = 88% layar, height = wrap_content
        if (dialog.getWindow() != null) {
            android.util.DisplayMetrics dm = new android.util.DisplayMetrics();
            ((android.app.Activity) context).getWindowManager().getDefaultDisplay().getMetrics(dm);
            int width = (int) (dm.widthPixels * 0.88f);
            dialog.getWindow().setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            // Pastikan window juga tidak clip agar animasi tidak terpotong
            ((android.view.ViewGroup) dialog.getWindow().getDecorView()).setClipChildren(false);
            ((android.view.ViewGroup) dialog.getWindow().getDecorView()).setClipToPadding(false);
        }

        // ── Entrance animation ──
        animateEntrance(view, ivAirin);
    }

    // ─── Animations ──────────────────────────────────────────────────────────────

    private static void animateEntrance(View card, View airin) {
        // Card: scale + fade in
        card.setScaleX(0.7f);
        card.setScaleY(0.7f);
        card.setAlpha(0f);

        card.animate()
                .scaleX(1f).scaleY(1f).alpha(1f)
                .setDuration(350)
                .setInterpolator(new OvershootInterpolator(1.5f))
                .start();

        // Airin: bounce in separately with delay
        airin.setScaleX(0f);
        airin.setScaleY(0f);
        airin.setAlpha(0f);

        airin.animate()
                .scaleX(1f).scaleY(1f).alpha(1f)
                .setStartDelay(150)
                .setDuration(400)
                .setInterpolator(new OvershootInterpolator(2f))
                .withEndAction(() -> startIdleAnimation(airin))
                .start();
    }

    private static void startIdleAnimation(View airin) {
        // Gentle floating bob — 6dp agar tidak keluar frame
        ObjectAnimator bobUp   = ObjectAnimator.ofFloat(airin, "translationY", 0f, -6f);
        ObjectAnimator bobDown = ObjectAnimator.ofFloat(airin, "translationY", -6f, 0f);
        bobUp.setDuration(800);
        bobDown.setDuration(800);
        bobUp.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
        bobDown.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());

        AnimatorSet bobSet = new AnimatorSet();
        bobSet.playSequentially(bobUp, bobDown);
        // AnimatorSet tidak support setRepeatCount — pakai listener untuk loop
        bobSet.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (airin.isAttachedToWindow()) bobSet.start();
            }
        });
        bobSet.start();
        airin.setTag(bobSet);
    }

    private static void animateDismiss(View card, Runnable onEnd) {
        card.animate()
                .scaleX(0.8f).scaleY(0.8f).alpha(0f)
                .setDuration(200)
                .setInterpolator(new android.view.animation.AccelerateInterpolator())
                .withEndAction(onEnd)
                .start();
    }
}
