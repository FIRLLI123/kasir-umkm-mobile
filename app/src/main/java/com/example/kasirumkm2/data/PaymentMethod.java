package com.example.kasirumkm2.data;

import com.google.gson.annotations.SerializedName;

public class PaymentMethod {
    @SerializedName("id")
    private int id;

    @SerializedName("method_name")
    private String name;

    @SerializedName("method_code")
    private String code;

    public PaymentMethod() {}

    public PaymentMethod(int id, String name, String code) {
        this.id = id;
        this.name = name;
        this.code = code;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    @Override
    public String toString() {
        return name;
    }
}
