package com.example.kasirumkm2.data;

import com.google.gson.annotations.SerializedName;

public class LoginRequest {
    @SerializedName("email")
    private String email;

    @SerializedName("password")
    private String password;

    @SerializedName("device_id")
    private String deviceId;

    @SerializedName("device_name")
    private String deviceName;

    public LoginRequest(String email, String password, String deviceId, String deviceName) {
        this.email = email;
        this.password = password;
        this.deviceId = deviceId;
        this.deviceName = deviceName;
    }

    // Getters
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getDeviceId() { return deviceId; }
    public String getDeviceName() { return deviceName; }
}
