package com.example.kasirumkm2.config;

public class Config {

    // Base URL - untuk emulator Android Studio
//    public static final String BASE_URL = "http://192.168.7.100:8000/api/";
    public static final String BASE_URL = "https://stagingsekolahtu.masalihsan.sch.id/api/";

    // App Info
    public static final String APP_NAME = "TANYA KASIR";

    // Network
    public static final int TIMEOUT_CONNECT = 30; // seconds
    public static final int TIMEOUT_READ = 30;    // seconds
    public static final int TIMEOUT_WRITE = 30;   // seconds

    // Pagination
    public static final int PAGE_SIZE = 20;

    // Session Keys
    public static final String PREF_NAME = "kasir_umkm_session";
    public static final String KEY_TOKEN = "token";
    public static final String KEY_USER_ID = "user_id";
    public static final String KEY_USER_NAME = "name";
    public static final String KEY_USER_EMAIL = "email";
    public static final String KEY_USER_ROLE = "role";
    public static final String KEY_DEVICE_ID = "device_id";
    public static final String KEY_IS_LOGGED_IN = "is_logged_in";
    
    // Printer Settings
    public static final String KEY_PRINTER_ADDRESS = "printer_address";
    public static final String KEY_PRINTER_NAME = "printer_name";

    // Remembered Credentials
    public static final String KEY_SAVED_EMAIL = "saved_email";
    public static final String KEY_SAVED_PASSWORD = "saved_password";

    // Company Settings
    public static final String KEY_COMPANY_ID = "company_id";
    public static final String KEY_COMPANY_NAME = "company_name";
    public static final String KEY_COMPANY_CODE = "company_code";

    // Subscription Settings
    public static final String KEY_SUB_STATUS = "subscription_status";
    public static final String KEY_SUB_IS_ACTIVE = "subscription_is_active";
    public static final String KEY_SUB_IS_LIFETIME = "subscription_is_lifetime";
    public static final String KEY_SUB_TRIAL_ENDS_AT = "subscription_trial_ends_at";
    public static final String KEY_SUB_ENDS_AT = "subscription_ends_at";

    // AI Chat Limit Settings
    public static final String KEY_AI_DAILY_LIMIT = "ai_daily_limit";
    public static final String KEY_AI_USED_TODAY = "ai_used_today";
    public static final String KEY_AI_REMAINING_TODAY = "ai_remaining_today";
    public static final String KEY_AI_SUB_STATUS = "ai_subscription_status";
    public static final String KEY_AI_SHOW_UPGRADE = "ai_show_upgrade";
    public static final String KEY_AI_UPGRADE_MESSAGE = "ai_upgrade_message";
}

