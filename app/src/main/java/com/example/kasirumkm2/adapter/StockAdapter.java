package com.example.kasirumkm2.adapter;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kasirumkm2.R;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class StockAdapter extends RecyclerView.Adapter<StockAdapter.StockViewHolder> {

    private final List<JsonObject> stockList = new ArrayList<>();
    private final Context context;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(JsonObject stockItem);
    }

    public StockAdapter(Context context) {
        this.context = context;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setData(List<JsonObject> data) {
        stockList.clear();
        stockList.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StockViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_stock, parent, false);
        return new StockViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StockViewHolder holder, int position) {
        JsonObject item = stockList.get(position);

        String productName = item.has("product_name") ? item.get("product_name").getAsString() : "";
        String productCode = item.has("product_code") ? item.get("product_code").getAsString() : "";
        String unit = item.has("unit") ? item.get("unit").getAsString() : "PCS";

        // Parse stock — could be "current_stock" or "stock_as_of_date"
        int stock = 0;
        try {
            if (item.has("current_stock")) {
                stock = (int) Double.parseDouble(item.get("current_stock").getAsString());
            } else if (item.has("stock_as_of_date")) {
                stock = (int) Double.parseDouble(item.get("stock_as_of_date").getAsString());
            }
        } catch (Exception e) {
            try {
                if (item.has("current_stock")) {
                    stock = item.get("current_stock").getAsInt();
                } else if (item.has("stock_as_of_date")) {
                    stock = item.get("stock_as_of_date").getAsInt();
                }
            } catch (Exception ignored) {}
        }

        holder.tvProductName.setText(productName);
        holder.tvProductCode.setText(productCode);
        holder.tvUnit.setText(unit);
        holder.tvStockQty.setText(String.valueOf(stock));

        // Emoji based on product name
        holder.tvEmoji.setText(getEmoji(productName));

        // Stock level color coding
        int textColor, bgColor;
        String statusText;
        if (stock <= 0) {
            textColor = ContextCompat.getColor(context, R.color.stock_empty);
            bgColor = ContextCompat.getColor(context, R.color.stock_empty_bg);
            statusText = "Habis";
        } else if (stock <= 10) {
            textColor = ContextCompat.getColor(context, R.color.stock_low);
            bgColor = ContextCompat.getColor(context, R.color.stock_low_bg);
            statusText = "Rendah";
        } else {
            textColor = ContextCompat.getColor(context, R.color.stock_safe);
            bgColor = ContextCompat.getColor(context, R.color.stock_safe_bg);
            statusText = "Aman";
        }

        holder.tvStockQty.setTextColor(textColor);
        holder.tvStockStatus.setText(statusText);
        holder.tvStockStatus.setTextColor(textColor);

        // Badge background
        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setColor(bgColor);
        badgeBg.setCornerRadius(40f);
        holder.tvStockStatus.setBackground(badgeBg);

        // Click
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return stockList.size();
    }

    private String getEmoji(String name) {
        if (name == null) return "📦";
        String lower = name.toLowerCase();
        if (lower.contains("aqua") || lower.contains("air") || lower.contains("minum")) return "💧";
        if (lower.contains("mie") || lower.contains("indomie")) return "🍜";
        if (lower.contains("beras") || lower.contains("nasi")) return "🍚";
        if (lower.contains("gula")) return "🍬";
        if (lower.contains("kopi") || lower.contains("coffee")) return "☕";
        if (lower.contains("teh") || lower.contains("tea")) return "🍵";
        if (lower.contains("susu") || lower.contains("milk")) return "🥛";
        if (lower.contains("roti") || lower.contains("bread")) return "🍞";
        if (lower.contains("sabun") || lower.contains("soap")) return "🧼";
        if (lower.contains("rokok") || lower.contains("sigaret")) return "🚬";
        if (lower.contains("snack") || lower.contains("keripik")) return "🍿";
        if (lower.contains("minyak") || lower.contains("oil")) return "🫗";
        if (lower.contains("telur") || lower.contains("egg")) return "🥚";
        return "📦";
    }

    static class StockViewHolder extends RecyclerView.ViewHolder {
        TextView tvEmoji, tvProductName, tvProductCode, tvUnit, tvStockQty, tvStockStatus;

        StockViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEmoji = itemView.findViewById(R.id.tvEmoji);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvProductCode = itemView.findViewById(R.id.tvProductCode);
            tvUnit = itemView.findViewById(R.id.tvUnit);
            tvStockQty = itemView.findViewById(R.id.tvStockQty);
            tvStockStatus = itemView.findViewById(R.id.tvStockStatus);
        }
    }
}
