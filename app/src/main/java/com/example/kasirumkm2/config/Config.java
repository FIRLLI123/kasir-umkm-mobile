package com.example.kasirumkm2.config;

public class Config {

    // Base URL - untuk emulator Android Studio
//    public static final String BASE_URL = "http://192.168.0.106:8000/api/";
    public static final String BASE_URL = "https://stagingsekolahtu.masalihsan.sch.id/api/";

    // App Info
    public static final String APP_NAME = "Kasir UMKM";

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
}
