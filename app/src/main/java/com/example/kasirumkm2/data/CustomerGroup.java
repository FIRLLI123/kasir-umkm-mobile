package com.example.kasirumkm2.data;

import com.google.gson.annotations.SerializedName;

public class CustomerGroup {
    @SerializedName("id")
    private int id;

    @SerializedName("group_name")
    private String name;

    @SerializedName("group_code")
    private String code;

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    @Override
    public String toString() {
        return name; // for Spinner display
    }
}
