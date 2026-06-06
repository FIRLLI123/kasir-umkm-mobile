package com.example.kasirumkm2.data;

import com.google.gson.annotations.SerializedName;

public class RegisterRequest {
    @SerializedName("company_name")
    private String companyName;

    @SerializedName("name")
    private String name;

    @SerializedName("email")
    private String email;

    @SerializedName("password")
    private String password;

    @SerializedName("password_confirmation")
    private String passwordConfirmation;

    @SerializedName("phone")
    private String phone;

    @SerializedName("address")
    private String address;

    @SerializedName("device_id")
    private String deviceId;

    @SerializedName("device_name")
    private String deviceName;

    public RegisterRequest(String companyName, String name, String email, String password, String passwordConfirmation, String phone, String address, String deviceId, String deviceName) {
        this.companyName = companyName;
        this.name = name;
        this.email = email;
        this.password = password;
        this.passwordConfirmation = passwordConfirmation;
        this.phone = phone;
        this.address = address;
        this.deviceId = deviceId;
        this.deviceName = deviceName;
    }

    // Getters
    public String getCompanyName() { return companyName; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getPasswordConfirmation() { return passwordConfirmation; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public String getDeviceId() { return deviceId; }
    public String getDeviceName() { return deviceName; }
}
