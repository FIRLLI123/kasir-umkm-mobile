package com.example.kasirumkm2.data;

import com.google.gson.annotations.SerializedName;

public class ProductPrice {
    @SerializedName("customer_group_id")
    private int customerGroupId;

    @SerializedName("selling_price")
    private double sellingPrice;

    public ProductPrice(int customerGroupId, double sellingPrice) {
        this.customerGroupId = customerGroupId;
        this.sellingPrice = sellingPrice;
    }

    // Getters & Setters
    public int getCustomerGroupId() { return customerGroupId; }
    public void setCustomerGroupId(int customerGroupId) { this.customerGroupId = customerGroupId; }

    public double getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(double sellingPrice) { this.sellingPrice = sellingPrice; }
}
