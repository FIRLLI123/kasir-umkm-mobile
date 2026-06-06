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
        editor.remove(Config.KEY_SUB_STATUS);
        editor.remove(Config.KEY_SUB_IS_ACTIVE);
        editor.remove(Config.KEY_SUB_IS_LIFETIME);
        editor.remove(Config.KEY_SUB_TRIAL_ENDS_AT);
        editor.remove(Config.KEY_SUB_ENDS_AT);
        editor.remove("pending_transaction_id");
        editor.remove("pending_amount");
        editor.remove("pending_qris_image");
        editor.remove("pending_expired_at");
        editor.remove("pending_plan_name");
        editor.remove("pending_plan_price");
        editor.remove(Config.KEY_AI_DAILY_LIMIT);
        editor.remove(Config.KEY_AI_USED_TODAY);
        editor.remove(Config.KEY_AI_REMAINING_TODAY);
        editor.remove(Config.KEY_AI_SUB_STATUS);
        editor.remove(Config.KEY_AI_SHOW_UPGRADE);
        editor.remove(Config.KEY_AI_UPGRADE_MESSAGE);
        editor.putBoolean(Config.KEY_IS_LOGGED_IN, false);
        editor.apply();
    }

    /**
     * Save subscription data
     */
    public void saveSubscription(String status, boolean isActive, boolean isLifetime, String trialEndsAt, String endsAt) {
        editor.putString(Config.KEY_SUB_STATUS, status);
        editor.putBoolean(Config.KEY_SUB_IS_ACTIVE, isActive);
        editor.putBoolean(Config.KEY_SUB_IS_LIFETIME, isLifetime);
        editor.putString(Config.KEY_SUB_TRIAL_ENDS_AT, trialEndsAt);
        editor.putString(Config.KEY_SUB_ENDS_AT, endsAt);
        editor.apply();
    }

    /**
     * Get subscription status (e.g., 'trial', 'active', 'expired')
     */
    public String getSubscriptionStatus() {
        return prefs.getString(Config.KEY_SUB_STATUS, "expired");
    }

    /**
     * Check if subscription is active
     */
    public boolean isSubscriptionActive() {
        return prefs.getBoolean(Config.KEY_SUB_IS_ACTIVE, false);
    }

    /**
     * Check if subscription is lifetime
     */
    public boolean isSubscriptionLifetime() {
        return prefs.getBoolean(Config.KEY_SUB_IS_LIFETIME, false);
    }

    /**
     * Get subscription trial ends date
     */
    public String getSubscriptionTrialEndsAt() {
        return prefs.getString(Config.KEY_SUB_TRIAL_ENDS_AT, "");
    }

    /**
     * Get subscription ends date
     */
    public String getSubscriptionEndsAt() {
        return prefs.getString(Config.KEY_SUB_ENDS_AT, "");
    }

    /**
     * Check if subscription is expired (or inactive)
     */
    public boolean isSubscriptionExpired() {
        return !isSubscriptionActive();
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
     * Save pending transaction details
     */
    public void savePendingTransaction(String transactionId, double amount, String qrisImage, String expiredAt, String planName, double planPrice) {
        editor.putString("pending_transaction_id", transactionId);
        editor.putFloat("pending_amount", (float) amount);
        editor.putString("pending_qris_image", qrisImage);
        editor.putString("pending_expired_at", expiredAt);
        editor.putString("pending_plan_name", planName);
        editor.putFloat("pending_plan_price", (float) planPrice);
        editor.apply();
    }

    /**
     * Check if there is a pending transaction
     */
    public boolean hasPendingTransaction() {
        return !prefs.getString("pending_transaction_id", "").isEmpty();
    }

    /**
     * Get pending transaction ID
     */
    public String getPendingTransactionId() {
        return prefs.getString("pending_transaction_id", "");
    }

    /**
     * Get pending amount
     */
    public double getPendingAmount() {
        return prefs.getFloat("pending_amount", 0);
    }

    /**
     * Get pending QRIS image
     */
    public String getPendingQrisImage() {
        return prefs.getString("pending_qris_image", "");
    }

    /**
     * Get pending expiry time
     */
    public String getPendingExpiredAt() {
        return prefs.getString("pending_expired_at", "");
    }

    /**
     * Get pending plan name
     */
    public String getPendingPlanName() {
        return prefs.getString("pending_plan_name", "");
    }

    /**
     * Get pending plan price
     */
    public double getPendingPlanPrice() {
        return prefs.getFloat("pending_plan_price", 0);
    }

    /**
     * Clear pending transaction details
     */
    public void clearPendingTransaction() {
        editor.remove("pending_transaction_id");
        editor.remove("pending_amount");
        editor.remove("pending_qris_image");
        editor.remove("pending_expired_at");
        editor.remove("pending_plan_name");
        editor.remove("pending_plan_price");
        editor.apply();
    }

    /**
     * Parse and update AI chat limit from a standard JSON response
     */
    public void updateAiChatLimit(com.google.gson.JsonObject responseJson) {
        if (responseJson == null) return;
        com.google.gson.JsonObject limitObj = null;

        // 1. Check root field "ai_chat_limit"
        if (responseJson.has("ai_chat_limit") && !responseJson.get("ai_chat_limit").isJsonNull()) {
            limitObj = responseJson.getAsJsonObject("ai_chat_limit");
        }
        // 2. Check nested "data" -> "ai_chat_limit"
        else if (responseJson.has("data") && !responseJson.get("data").isJsonNull()) {
            com.google.gson.JsonObject data = responseJson.getAsJsonObject("data");
            if (data.has("ai_chat_limit") && !data.get("ai_chat_limit").isJsonNull()) {
                limitObj = data.getAsJsonObject("ai_chat_limit");
            } else if (data.has("limit_type") && "trial_daily_ai_chat".equals(data.get("limit_type").getAsString())) {
                // This is the 429 error response body structure
                limitObj = data;
            }
        }
        // 3. Fallback: if responseJson is directly the 429 response structure
        else if (responseJson.has("limit_type") && "trial_daily_ai_chat".equals(responseJson.get("limit_type").getAsString())) {
            limitObj = responseJson;
        }

        if (limitObj != null) {
            int dailyLimit = limitObj.has("daily_limit") && !limitObj.get("daily_limit").isJsonNull() ? limitObj.get("daily_limit").getAsInt() : 3;
            int usedToday = limitObj.has("used_today") && !limitObj.get("used_today").isJsonNull() ? limitObj.get("used_today").getAsInt() : 0;
            int remainingToday = limitObj.has("remaining_today") && !limitObj.get("remaining_today").isJsonNull() ? limitObj.get("remaining_today").getAsInt() : 3;
            String subStatus = limitObj.has("subscription_status") && !limitObj.get("subscription_status").isJsonNull() ? limitObj.get("subscription_status").getAsString() : "trial";
            boolean showUpgrade = limitObj.has("show_upgrade") && !limitObj.get("show_upgrade").isJsonNull() && limitObj.get("show_upgrade").getAsBoolean();
            String upgradeMsg = limitObj.has("upgrade_message") && !limitObj.get("upgrade_message").isJsonNull() ? limitObj.get("upgrade_message").getAsString() : "";
            if (upgradeMsg.isEmpty() && limitObj.has("message") && !limitObj.get("message").isJsonNull()) {
                upgradeMsg = limitObj.get("message").getAsString();
            }

            saveAiChatLimit(dailyLimit, usedToday, remainingToday, subStatus, showUpgrade, upgradeMsg);
        }
    }

    public void saveAiChatLimit(int dailyLimit, int usedToday, int remainingToday, String subStatus, boolean showUpgrade, String upgradeMsg) {
        editor.putInt(Config.KEY_AI_DAILY_LIMIT, dailyLimit);
        editor.putInt(Config.KEY_AI_USED_TODAY, usedToday);
        editor.putInt(Config.KEY_AI_REMAINING_TODAY, remainingToday);
        editor.putString(Config.KEY_AI_SUB_STATUS, subStatus);
        editor.putBoolean(Config.KEY_AI_SHOW_UPGRADE, showUpgrade);
        editor.putString(Config.KEY_AI_UPGRADE_MESSAGE, upgradeMsg);
        editor.apply();
    }

    public int getAiDailyLimit() {
        return prefs.getInt(Config.KEY_AI_DAILY_LIMIT, 3);
    }

    public int getAiUsedToday() {
        return prefs.getInt(Config.KEY_AI_USED_TODAY, 0);
    }

    public int getAiRemainingToday() {
        return prefs.getInt(Config.KEY_AI_REMAINING_TODAY, 3);
    }

    public String getAiSubStatus() {
        return prefs.getString(Config.KEY_AI_SUB_STATUS, "trial");
    }

    public boolean getAiShowUpgrade() {
        return prefs.getBoolean(Config.KEY_AI_SHOW_UPGRADE, false);
    }

    public String getAiUpgradeMessage() {
        return prefs.getString(Config.KEY_AI_UPGRADE_MESSAGE, "");
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
