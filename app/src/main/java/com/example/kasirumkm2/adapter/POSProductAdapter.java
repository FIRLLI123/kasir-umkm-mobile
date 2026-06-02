package com.example.kasirumkm2.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kasirumkm2.R;
import com.example.kasirumkm2.data.CartItem;
import com.example.kasirumkm2.data.Product;
import com.example.kasirumkm2.utils.CurrencyHelper;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class POSProductAdapter extends RecyclerView.Adapter<POSProductAdapter.ViewHolder> {

    private List<Product> productList = new ArrayList<>();
    private List<Product> filteredList = new ArrayList<>();
    private Map<Integer, CartItem> cartMap = new HashMap<>(); // Key: Product ID
    private int selectedCustomerGroupId = 1; // Default: USER
    private OnCartActionListener listener;

    public interface OnCartActionListener {
        void onAddToCart(Product product);
        void onIncrementQty(Product product);
        void onDecrementQty(Product product);
    }

    public POSProductAdapter(OnCartActionListener listener) {
        this.listener = listener;
    }

    public void setData(List<Product> data) {
        this.productList = data;
        this.filteredList = new ArrayList<>(data);
        notifyDataSetChanged();
    }

    public void setCartState(List<CartItem> cartList, int customerGroupId) {
        this.cartMap.clear();
        for (CartItem item : cartList) {
            this.cartMap.put(item.getProduct().getId(), item);
        }
        this.selectedCustomerGroupId = customerGroupId;
        notifyDataSetChanged();
    }

    public void filter(String query) {
        filteredList.clear();
        if (query == null || query.isEmpty()) {
            filteredList.addAll(productList);
        } else {
            String lower = query.toLowerCase();
            for (Product p : productList) {
                if ((p.getProductName() != null && p.getProductName().toLowerCase().contains(lower)) ||
                    (p.getProductCode() != null && p.getProductCode().toLowerCase().contains(lower))) {
                    filteredList.add(p);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pos_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = filteredList.get(position);

        holder.tvProductName.setText(product.getProductName());
        holder.tvEmoji.setText(product.getEmoji());
        holder.tvStock.setText("Stok: " + product.getStock() + " " + (product.getUnit() != null ? product.getUnit() : "pcs"));

        // Get dynamic selling price based on customer group!
        double price = product.getSellingPrice(selectedCustomerGroupId);
        holder.tvPrice.setText(CurrencyHelper.formatRupiah(price));

        // Check stock
        boolean outOfStock = product.getStock() <= 0;

        // Check if item is in cart
        CartItem cartItem = cartMap.get(product.getId());
        if (cartItem != null) {
            holder.btnTambah.setVisibility(View.GONE);
            holder.layoutQty.setVisibility(View.VISIBLE);
            holder.tvQty.setText(String.valueOf(cartItem.getQty()));

            // Disable plus button if qty equals stock
            holder.btnPlus.setEnabled(cartItem.getQty() < product.getStock());
        } else {
            holder.btnTambah.setVisibility(View.VISIBLE);
            holder.layoutQty.setVisibility(View.GONE);

            // Disable add button if out of stock
            holder.btnTambah.setEnabled(!outOfStock);
            if (outOfStock) {
                holder.btnTambah.setText("Habis");
            } else {
                holder.btnTambah.setText("Tambah");
            }
        }

        // Click listeners
        holder.btnTambah.setOnClickListener(v -> {
            if (listener != null) listener.onAddToCart(product);
        });

        holder.btnPlus.setOnClickListener(v -> {
            if (listener != null) listener.onIncrementQty(product);
        });

        holder.btnMin.setOnClickListener(v -> {
            if (listener != null) listener.onDecrementQty(product);
        });
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEmoji, tvProductName, tvStock, tvPrice, tvQty;
        MaterialButton btnTambah, btnMin, btnPlus;
        View layoutQty;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEmoji = itemView.findViewById(R.id.tvEmoji);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvStock = itemView.findViewById(R.id.tvStock);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvQty = itemView.findViewById(R.id.tvQty);
            btnTambah = itemView.findViewById(R.id.btnTambah);
            btnMin = itemView.findViewById(R.id.btnMin);
            btnPlus = itemView.findViewById(R.id.btnPlus);
            layoutQty = itemView.findViewById(R.id.layoutQty);
        }
    }
}
