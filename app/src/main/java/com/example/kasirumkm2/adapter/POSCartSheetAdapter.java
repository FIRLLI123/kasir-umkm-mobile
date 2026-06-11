package com.example.kasirumkm2.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kasirumkm2.R;
import com.example.kasirumkm2.data.CartItem;
import com.example.kasirumkm2.utils.CurrencyHelper;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class POSCartSheetAdapter extends RecyclerView.Adapter<POSCartSheetAdapter.ViewHolder> {

    private List<CartItem> cartList = new ArrayList<>();
    private final OnCartSheetActionListener listener;

    public interface OnCartSheetActionListener {
        void onIncrementQty(CartItem item);
        void onDecrementQty(CartItem item);
    }

    public POSCartSheetAdapter(OnCartSheetActionListener listener) {
        this.listener = listener;
    }

    public void setData(List<CartItem> data) {
        this.cartList = new ArrayList<>(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pos_cart_sheet, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CartItem item = cartList.get(position);

        holder.tvProductName.setText(item.getProduct().getProductName());
        holder.tvEmoji.setText(item.getProduct().getEmoji());
        
        // Show unit price or total price for this row
        holder.tvPrice.setText(CurrencyHelper.formatRupiah(item.getPricePerUnit()));
        
        holder.tvQty.setText(String.valueOf(item.getQty()));

        // Enable/disable plus button based on stock limit
        holder.btnPlus.setEnabled(item.getQty() < item.getProduct().getStock());

        holder.btnMin.setOnClickListener(v -> {
            if (listener != null) listener.onDecrementQty(item);
        });

        holder.btnPlus.setOnClickListener(v -> {
            if (listener != null) listener.onIncrementQty(item);
        });
    }

    @Override
    public int getItemCount() {
        return cartList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEmoji, tvProductName, tvPrice, tvQty;
        MaterialButton btnMin, btnPlus;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEmoji = itemView.findViewById(R.id.tvSheetEmoji);
            tvProductName = itemView.findViewById(R.id.tvSheetProductName);
            tvPrice = itemView.findViewById(R.id.tvSheetProductPrice);
            tvQty = itemView.findViewById(R.id.tvSheetQty);
            btnMin = itemView.findViewById(R.id.btnSheetMin);
            btnPlus = itemView.findViewById(R.id.btnSheetPlus);
        }
    }
}
