package com.example.kasirumkm2.data;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Product {
    @SerializedName("id")
    private int id;

    @SerializedName("product_code")
    private String productCode;

    @SerializedName("product_name")
    private String productName;

    @SerializedName("unit")
    private String unit;

    @SerializedName("cost_price")
    private double costPrice;

    @SerializedName("stock")
    private double stock;

    @SerializedName("status")
    private String status;

    @SerializedName("prices")
    private List<ProductPrice> prices;

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public double getCostPrice() { return costPrice; }
    public void setCostPrice(double costPrice) { this.costPrice = costPrice; }

    public int getStock() { return (int) stock; }
    public void setStock(int stock) { this.stock = stock; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<ProductPrice> getPrices() { return prices; }
    public void setPrices(List<ProductPrice> prices) { this.prices = prices; }

    /**
     * Check if product is active (status "00")
     */
    public boolean isActive() {
        return "00".equals(status);
    }

    /**
     * Get selling price for a specific customer group
     */
    public double getSellingPrice(int customerGroupId) {
        if (prices != null) {
            for (ProductPrice price : prices) {
                if (price.getCustomerGroupId() == customerGroupId) {
                    return price.getSellingPrice();
                }
            }
        }
        return 0;
    }

    /**
     * Get emoji icon based on product name for display
     */
    public String getEmoji() {
        String name = productName != null ? productName.toLowerCase() : "";
        if (name.contains("aqua") || name.contains("air") || name.contains("minum")) return "💧";
        if (name.contains("mie") || name.contains("indomie")) return "🍜";
        if (name.contains("beras") || name.contains("nasi")) return "🍚";
        if (name.contains("gula")) return "🍬";
        if (name.contains("kopi") || name.contains("coffee")) return "☕";
        if (name.contains("teh") || name.contains("tea")) return "🍵";
        if (name.contains("susu") || name.contains("milk")) return "🥛";
        if (name.contains("roti") || name.contains("bread")) return "🍞";
        if (name.contains("sabun") || name.contains("soap")) return "🧼";
        if (name.contains("rokok") || name.contains("sigaret")) return "🚬";
        if (name.contains("snack") || name.contains("keripik")) return "🍿";
        if (name.contains("minyak") || name.contains("oil")) return "🫗";
        if (name.contains("telur") || name.contains("egg")) return "🥚";
        return "📦"; // default
    }
}
