package com.example.kasirumkm2.data;

import com.google.gson.annotations.SerializedName;

public class Customer {
    @SerializedName("id")
    private int id;

    @SerializedName("customer_code")
    private String customerCode;

    @SerializedName("customer_name")
    private String customerName;

    @SerializedName("phone")
    private String phone;

    @SerializedName("address")
    private String address;

    @SerializedName("customer_group_id")
    private int customerGroupId;

    @SerializedName("status")
    private String status;

    @SerializedName("customer_group")
    private CustomerGroup customerGroup;

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCustomerCode() { return customerCode; }
    public void setCustomerCode(String customerCode) { this.customerCode = customerCode; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public int getCustomerGroupId() { return customerGroupId; }
    public void setCustomerGroupId(int customerGroupId) { this.customerGroupId = customerGroupId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public CustomerGroup getCustomerGroup() { return customerGroup; }
    public void setCustomerGroup(CustomerGroup customerGroup) { this.customerGroup = customerGroup; }

    /**
     * Get initials from customer name (e.g., "Budi Santoso" → "BS")
     */
    public String getInitials() {
        if (customerName == null || customerName.isEmpty()) return "?";
        String[] parts = customerName.trim().split("\\s+");
        if (parts.length >= 2) {
            return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
        }
        return ("" + parts[0].charAt(0)).toUpperCase();
    }

    /**
     * Get group name safely
     */
    public String getGroupName() {
        if (customerGroup != null && customerGroup.getName() != null) {
            return customerGroup.getName();
        }
        return "USER";
    }
}
