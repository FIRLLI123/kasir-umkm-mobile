package com.example.kasirumkm2.data;

/**
 * Cart item for POS - temporary in-memory item representing a product added to cart
 */
public class CartItem {
    private Product product;
    private int qty;
    private double pricePerUnit; // price based on customer group
    private double subtotal;

    public CartItem(Product product, int qty, double pricePerUnit) {
        this.product = product;
        this.qty = qty;
        this.pricePerUnit = pricePerUnit;
        this.subtotal = qty * pricePerUnit;
    }

    // Getters & Setters
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public int getQty() { return qty; }
    public void setQty(int qty) {
        this.qty = qty;
        this.subtotal = qty * pricePerUnit;
    }

    public double getPricePerUnit() { return pricePerUnit; }
    public void setPricePerUnit(double pricePerUnit) {
        this.pricePerUnit = pricePerUnit;
        this.subtotal = qty * pricePerUnit;
    }

    public double getSubtotal() { return subtotal; }

    /**
     * Increase qty by 1 and recalculate subtotal
     */
    public void incrementQty() {
        this.qty++;
        this.subtotal = qty * pricePerUnit;
    }

    /**
     * Decrease qty by 1 (min 1) and recalculate subtotal
     */
    public void decrementQty() {
        if (this.qty > 1) {
            this.qty--;
            this.subtotal = qty * pricePerUnit;
        }
    }
}
