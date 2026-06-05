package com.example.kasirumkm2.utils;

import android.content.Context;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.example.kasirumkm2.R;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CurrencyHelper {

    private static final DecimalFormat formatter;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("id", "ID"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        formatter = new DecimalFormat("#,###", symbols);
    }

    /**
     * Format angka ke format Rupiah: Rp 13.500
     */
    public static String formatRupiah(double amount) {
        return "Rp " + formatter.format(amount);
    }

    /**
     * Format angka ke format Rupiah tanpa prefix: 13.500
     */
    public static String formatNumber(double amount) {
        return formatter.format(amount);
    }

    /**
     * Parse double value from formatted number or rupiah string
     */
    public static double parseDouble(String valueStr) {
        if (valueStr == null || valueStr.isEmpty()) return 0;
        try {
            // Remove Rp prefix, dot grouping separators, and replace comma decimals with dots
            String clean = valueStr.replace("Rp", "")
                    .replace(".", "")
                    .replace(",", ".")
                    .replaceAll("\\s+", "");
            return Double.parseDouble(clean);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Format tanggal dari API (yyyy-MM-dd HH:mm:ss) ke display (dd/MM/yyyy HH:mm)
     */
    public static String formatDateTime(String dateTimeStr) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy  HH:mm", Locale.getDefault());
            Date date = inputFormat.parse(dateTimeStr);
            return outputFormat.format(date);
        } catch (Exception e) {
            return dateTimeStr;
        }
    }

    /**
     * Format tanggal dari API (yyyy-MM-dd) ke display (dd/MM/yyyy)
     */
    public static String formatDate(String dateStr) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date date = inputFormat.parse(dateStr);
            return outputFormat.format(date);
        } catch (Exception e) {
            return dateStr;
        }
    }

    /**
     * Get today's date in yyyy-MM-dd format
     */
    public static String getTodayDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }

    /**
     * Get greeting based on current time
     */
    public static String getGreeting(Context context) {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour >= 5 && hour < 11) return context.getString(R.string.greeting_morning);
        if (hour >= 11 && hour < 15) return context.getString(R.string.greeting_afternoon);
        if (hour >= 15 && hour < 18) return context.getString(R.string.greeting_evening);
        return context.getString(R.string.greeting_night);
    }

    /**
     * Format margin percentage
     */
    public static String formatPercent(double value) {
        return String.format(Locale.getDefault(), "%.1f%%", value);
    }

    /**
     * Show success snackbar
     */
    public static void showSuccess(View view, String message) {
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT)
                .setBackgroundTint(view.getContext().getColor(R.color.success_green))
                .setTextColor(view.getContext().getColor(R.color.white))
                .show();
    }

    /**
     * Show error snackbar
     */
    public static void showError(View view, String message) {
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT)
                .setBackgroundTint(view.getContext().getColor(R.color.danger_red))
                .setTextColor(view.getContext().getColor(R.color.white))
                .show();
    }

    /**
     * Show toast (short)
     */
    public static void showToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Get dynamic app version name from PackageInfo
     */
    public static String getAppVersion(Context context) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                return context.getPackageManager().getPackageInfo(
                        context.getPackageName(),
                        android.content.pm.PackageManager.PackageInfoFlags.of(0)
                ).versionName;
            } else {
                return context.getPackageManager().getPackageInfo(
                        context.getPackageName(),
                        0
                ).versionName;
            }
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            return "1.0.0";
        }
    }
}
