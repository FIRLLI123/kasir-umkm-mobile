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
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.kasirumkm2.R;

/**
 * AirinDialog — Custom dialog dengan karakter Airin chibi.
 *
 * Penggunaan:
 *   AirinDialog.showSuccess(ctx, "Yeay!", "Berhasil!", () -> doSomething());
 *   AirinDialog.showError(ctx, "Ups!", "Ada yang salah~", null);
 *   AirinDialog.showOffline(ctx, null);
 *   AirinDialog.showWarning(ctx, "Hati-hati!", "Pesan warning", null);
 *   AirinDialog.showConfirm(ctx, "Judul", "Pesan", "Ya, keluar", "Batal, lanjut", onYes, onNo);
 */
public class AirinDialog {

    public enum Type {
        SUCCESS,   // senang.png — teal button
        ERROR,     // sedih_error.png — red button
        WARNING,   // kaget.png — red button
        SLEEPY,    // ngantuk.png — teal button (offline)
        BINGUNG,   // bingung.png — teal button (confirm/warning)
        INFO,      // menjelaskan.png — teal button (info/app info)
        ANGRY      // marah.png — red button
    }

    // ─── Convenience shorthands ────────────────────────────────────────────────

    public static void showSuccess(Context context, String title, CharSequence message, Runnable onOk) {
        show(context, Type.SUCCESS, title, message, "Yeay, Oke! 🎉", null, onOk, null);
    }

    public static void showError(Context context, String title, CharSequence message, Runnable onOk) {
        show(context, Type.ERROR, title, message, "Oke, coba lagi deh", null, onOk, null);
    }

    public static void showOffline(Context context, Runnable onOk) {
        show(context, Type.SLEEPY,
                "Koneksinya Kemana Nih? 😴",
                "Airin ngantuk nunggu koneksi kamu~\nCek internet lalu coba lagi ya!",
                "Siap, coba lagi!", null, onOk, null);
    }

    public static void showWarning(Context context, String title, CharSequence message, Runnable onOk) {
        show(context, Type.WARNING, title, message, "Oke, mengerti!", null, onOk, null);
    }

    public static void showAngry(Context context, String title, CharSequence message, Runnable onOk) {
        show(context, Type.ANGRY, title, message, "Siap bos! 💢", null, onOk, null);
    }

    public static void showInfo(Context context, String title, CharSequence message, Runnable onOk) {
        show(context, Type.INFO, title, message, "Oke, Paham!", null, onOk, null);
    }

    /**
     * Dialog konfirmasi 2 tombol — pakai Airin bingung.
     *
     * @param positiveLabel label tombol utama (ya / lanjut)
     * @param negativeLabel label tombol sekunder (tidak / batal)
     * @param onPositive    callback jika positif diklik
     * @param onNegative    callback jika negatif diklik (boleh null = dismiss saja)
     */
    public static void showConfirm(Context context, String title, CharSequence message,
                                   String positiveLabel, String negativeLabel,
                                   Runnable onPositive, Runnable onNegative) {
        show(context, Type.BINGUNG, title, message, positiveLabel, negativeLabel, onPositive, onNegative);
    }

    // ─── Core ─────────────────────────────────────────────────────────────────

    public static void show(Context context, Type type,
                            String title, CharSequence message,
                            String posLabel, String negLabel,
                            Runnable onPositive, Runnable onNegative) {

        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);

        View view = LayoutInflater.from(context).inflate(R.layout.dialog_airin, null);
        dialog.setContentView(view);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // ── Bind views ──
        ImageView ivAirin   = view.findViewById(R.id.ivAirinDialog);
        TextView  tvTitle   = view.findViewById(R.id.tvDialogTitle);
        TextView  tvMessage = view.findViewById(R.id.tvDialogMessage);
        Button    btnOk     = view.findViewById(R.id.btnDialogOk);
        Button    btnNeg    = view.findViewById(R.id.btnDialogNegative);

        // ── Type-specific styling ──
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
                btnOk.setBackgroundResource(R.drawable.bg_btn_dialog_success);
                break;
            case BINGUNG:
                ivAirin.setImageResource(R.drawable.bingung);
                btnOk.setBackgroundResource(R.drawable.bg_btn_dialog_error);
                break;
            case INFO:
                ivAirin.setImageResource(R.drawable.menjelaskan);
                btnOk.setBackgroundResource(R.drawable.bg_btn_dialog_success);
                break;
            case ANGRY:
                ivAirin.setImageResource(R.drawable.marah);
                btnOk.setBackgroundResource(R.drawable.bg_btn_dialog_error);
                break;
        }

        tvTitle.setText(title);
        tvMessage.setText(message);
        tvMessage.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
        btnOk.setText(posLabel);

        // ── Negative button (only for confirm mode) ──
        if (negLabel != null) {
            btnNeg.setVisibility(View.VISIBLE);
            btnNeg.setText(negLabel);
            btnNeg.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                animateDismiss(view, () -> {
                    dialog.dismiss();
                    if (onNegative != null) onNegative.run();
                });
            });
        }

        // ── Positive button ──
        btnOk.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
            animateDismiss(view, () -> {
                dialog.dismiss();
                if (onPositive != null) onPositive.run();
            });
        });

        dialog.show();

        // ── Set width = 88% layar ──
        if (dialog.getWindow() != null) {
            android.util.DisplayMetrics dm = new android.util.DisplayMetrics();
            ((android.app.Activity) context).getWindowManager().getDefaultDisplay().getMetrics(dm);
            int width = (int) (dm.widthPixels * 0.88f);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
            ((ViewGroup) dialog.getWindow().getDecorView()).setClipChildren(false);
            ((ViewGroup) dialog.getWindow().getDecorView()).setClipToPadding(false);
        }

        // ── Entrance animation ──
        animateEntrance(view, ivAirin);
    }

    // ─── Animations ────────────────────────────────────────────────────────────

    private static void animateEntrance(View card, View airin) {
        card.setScaleX(0.7f);
        card.setScaleY(0.7f);
        card.setAlpha(0f);
        card.animate()
                .scaleX(1f).scaleY(1f).alpha(1f)
                .setDuration(350)
                .setInterpolator(new OvershootInterpolator(1.5f))
                .start();

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
