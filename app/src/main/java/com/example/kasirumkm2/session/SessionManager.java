package com.example.kasirumkm2.session;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;

import com.example.kasirumkm2.config.Config;

public class SessionManager {

    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;
    private final Context context;

    public SessionManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(Config.PREF_NAME, Context.MODE_PRIVATE);
        this.editor = prefs.edit();
    }

    /**
     * Save login session data
     */
    public void saveSession(String token, int userId, String name, String email, String role) {
        editor.putString(Config.KEY_TOKEN, token);
        editor.putInt(Config.KEY_USER_ID, userId);
        editor.putString(Config.KEY_USER_NAME, name);
        editor.putString(Config.KEY_USER_EMAIL, email);
        editor.putString(Config.KEY_USER_ROLE, role);
        editor.putString(Config.KEY_DEVICE_ID, getDeviceId());
        editor.putBoolean(Config.KEY_IS_LOGGED_IN, true);
        editor.apply();
    }

    /**
     * Check if user is logged in
     */
    public boolean isLoggedIn() {
        return prefs.getBoolean(Config.KEY_IS_LOGGED_IN, false);
    }

    /**
     * Get auth token
     */
    public String getToken() {
        return prefs.getString(Config.KEY_TOKEN, "");
    }

    /**
     * Get user ID
     */
    public int getUserId() {
        return prefs.getInt(Config.KEY_USER_ID, 0);
    }

    /**
     * Get user name
     */
    public String getUserName() {
        return prefs.getString(Config.KEY_USER_NAME, "");
    }

    /**
     * Get user email
     */
    public String getUserEmail() {
        return prefs.getString(Config.KEY_USER_EMAIL, "");
    }

    /**
     * Get user role
     */
    public String getUserRole() {
        return prefs.getString(Config.KEY_USER_ROLE, "");
    }

    /**
     * Get unique device ID using ANDROID_ID
     */
    public String getDeviceId() {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    /**
     * Get device name (manufacturer + model)
     */
    public String getDeviceName() {
        String manufacturer = android.os.Build.MANUFACTURER;
        String model = android.os.Build.MODEL;
        if (model.toLowerCase().startsWith(manufacturer.toLowerCase())) {
            return capitalize(model);
        }
        return capitalize(manufacturer) + " " + model;
    }

    /**
     * Clear active session data (logout) - preserves printer settings and saved credentials
     */
    public void clearSession() {
        editor.remove(Config.KEY_TOKEN);
        editor.remove(Config.KEY_USER_ID);
        editor.remove(Config.KEY_USER_NAME);
        editor.remove(Config.KEY_USER_EMAIL);
        editor.remove(Config.KEY_USER_ROLE);
        editor.remove(Config.KEY_COMPANY_ID);
        editor.remove(Config.KEY_COMPANY_NAME);
        editor.remove(Config.KEY_COMPANY_CODE);
        editor.putBoolean(Config.KEY_IS_LOGGED_IN, false);
        editor.apply();
    }

    /**
     * Save login credentials for quick remember
     */
    public void saveSavedCredentials(String email, String password) {
        editor.putString(Config.KEY_SAVED_EMAIL, email);
        editor.putString(Config.KEY_SAVED_PASSWORD, password);
        editor.apply();
    }

    /**
     * Get saved email
     */
    public String getSavedEmail() {
        return prefs.getString(Config.KEY_SAVED_EMAIL, "");
    }

    /**
     * Get saved password
     */
    public String getSavedPassword() {
        return prefs.getString(Config.KEY_SAVED_PASSWORD, "");
    }

    /**
     * Save active company details
     */
    public void saveCompany(int id, String name, String code) {
        editor.putInt(Config.KEY_COMPANY_ID, id);
        editor.putString(Config.KEY_COMPANY_NAME, name);
        editor.putString(Config.KEY_COMPANY_CODE, code);
        editor.apply();
    }

    /**
     * Get active company ID
     */
    public int getCompanyId() {
        return prefs.getInt(Config.KEY_COMPANY_ID, 0);
    }

    /**
     * Get active company Name
     */
    public String getCompanyName() {
        return prefs.getString(Config.KEY_COMPANY_NAME, "");
    }

    /**
     * Get active company Code
     */
    public String getCompanyCode() {
        return prefs.getString(Config.KEY_COMPANY_CODE, "");
    }

    /**
     * Save printer details
     */
    public void saveSelectedPrinter(String address, String name) {
        editor.putString(Config.KEY_PRINTER_ADDRESS, address);
        editor.putString(Config.KEY_PRINTER_NAME, name);
        editor.apply();
    }

    /**
     * Get printer MAC address
     */
    public String getPrinterAddress() {
        return prefs.getString(Config.KEY_PRINTER_ADDRESS, "");
    }

    /**
     * Get printer device name
     */
    public String getPrinterName() {
        return prefs.getString(Config.KEY_PRINTER_NAME, "");
    }

    /**
     * Remove printer config
     */
    public void clearSelectedPrinter() {
        editor.remove(Config.KEY_PRINTER_ADDRESS);
        editor.remove(Config.KEY_PRINTER_NAME);
        editor.apply();
    }

    /**
     * Capitalize first letter of string
     */
    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return "";
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        }
        return Character.toUpperCase(first) + s.substring(1);
    }
}
