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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class StockMutationAdapter extends RecyclerView.Adapter<StockMutationAdapter.MutationViewHolder> {

    private final List<JsonObject> mutations = new ArrayList<>();
    private final Context context;

    public StockMutationAdapter(Context context) {
        this.context = context;
    }

    public void setData(List<JsonObject> data) {
        mutations.clear();
        mutations.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MutationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_stock_mutation, parent, false);
        return new MutationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MutationViewHolder holder, int position) {
        JsonObject item = mutations.get(position);

        String mutationType = item.has("mutation_type") ? item.get("mutation_type").getAsString() : "";
        String mutationDate = item.has("mutation_date") ? item.get("mutation_date").getAsString() : "";
        String note = item.has("note") && !item.get("note").isJsonNull() ? item.get("note").getAsString() : "";

        double qtyIn = parseDouble(item, "qty_in");
        double qtyOut = parseDouble(item, "qty_out");
        double stockBefore = parseDouble(item, "stock_before");
        double stockAfter = parseDouble(item, "stock_after");

        // Mutation type badge
        MutationStyle style = getMutationStyle(mutationType);
        holder.tvMutationType.setText(style.label);
        holder.tvMutationType.setTextColor(style.textColor);

        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setColor(style.bgColor);
        badgeBg.setCornerRadius(40f);
        holder.tvMutationType.setBackground(badgeBg);

        // Timeline dot color
        GradientDrawable dotBg = new GradientDrawable();
        dotBg.setColor(style.textColor);
        dotBg.setShape(GradientDrawable.OVAL);
        holder.viewDot.setBackground(dotBg);

        // Date formatting
        holder.tvMutationDate.setText(formatDate(mutationDate));

        // Qty direction
        if (qtyIn > 0) {
            holder.tvQtyDirection.setText("▲ +" + (int) qtyIn);
            holder.tvQtyDirection.setTextColor(ContextCompat.getColor(context, R.color.stock_safe));
        } else if (qtyOut > 0) {
            holder.tvQtyDirection.setText("▼ -" + (int) qtyOut);
            holder.tvQtyDirection.setTextColor(ContextCompat.getColor(context, R.color.stock_empty));
        } else {
            holder.tvQtyDirection.setText("—");
            holder.tvQtyDirection.setTextColor(ContextCompat.getColor(context, R.color.text_light));
        }

        // Stock flow
        holder.tvStockFlow.setText((int) stockBefore + " → " + (int) stockAfter);

        // Note
        if (!note.isEmpty()) {
            holder.tvNote.setVisibility(View.VISIBLE);
            holder.tvNote.setText("💬 " + note);
        } else {
            holder.tvNote.setVisibility(View.GONE);
        }

        // Timeline lines visibility
        holder.viewLineTop.setVisibility(position == 0 ? View.INVISIBLE : View.VISIBLE);
        holder.viewLineBottom.setVisibility(position == mutations.size() - 1 ? View.INVISIBLE : View.VISIBLE);
    }

    @Override
    public int getItemCount() {
        return mutations.size();
    }

    private double parseDouble(JsonObject obj, String key) {
        try {
            if (obj.has(key) && !obj.get(key).isJsonNull()) {
                return Double.parseDouble(obj.get(key).getAsString());
            }
        } catch (Exception e) {
            try {
                return obj.get(key).getAsDouble();
            } catch (Exception ignored) {}
        }
        return 0;
    }

    private String formatDate(String isoDate) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault());
            inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = inputFormat.parse(isoDate);
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm", new Locale("id", "ID"));
            return outputFormat.format(date);
        } catch (Exception e) {
            try {
                // Try simpler format
                SimpleDateFormat inputFormat2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                Date date = inputFormat2.parse(isoDate);
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm", new Locale("id", "ID"));
                return outputFormat.format(date);
            } catch (Exception e2) {
                return isoDate;
            }
        }
    }

    private MutationStyle getMutationStyle(String type) {
        switch (type) {
            case "INITIAL":
                return new MutationStyle("Stok Awal",
                        ContextCompat.getColor(context, R.color.mutation_initial),
                        ContextCompat.getColor(context, R.color.stock_safe_bg));
            case "ADJUSTMENT_IN":
                return new MutationStyle("Penyesuaian Masuk",
                        ContextCompat.getColor(context, R.color.mutation_adj_in),
                        ContextCompat.getColor(context, R.color.blue_info_light));
            case "ADJUSTMENT_OUT":
                return new MutationStyle("Penyesuaian Keluar",
                        ContextCompat.getColor(context, R.color.mutation_adj_out),
                        ContextCompat.getColor(context, R.color.yellow_warning_light));
            case "SALE":
                return new MutationStyle("Penjualan",
                        ContextCompat.getColor(context, R.color.mutation_sale),
                        ContextCompat.getColor(context, R.color.stock_empty_bg));
            case "VOID":
                return new MutationStyle("Void Transaksi",
                        ContextCompat.getColor(context, R.color.mutation_void),
                        ContextCompat.getColor(context, R.color.purple_light));
            default:
                return new MutationStyle(type,
                        ContextCompat.getColor(context, R.color.text_gray),
                        ContextCompat.getColor(context, R.color.border_light));
        }
    }

    private static class MutationStyle {
        String label;
        int textColor;
        int bgColor;

        MutationStyle(String label, int textColor, int bgColor) {
            this.label = label;
            this.textColor = textColor;
            this.bgColor = bgColor;
        }
    }

    static class MutationViewHolder extends RecyclerView.ViewHolder {
        View viewLineTop, viewDot, viewLineBottom;
        TextView tvMutationType, tvMutationDate, tvQtyDirection, tvStockFlow, tvNote;

        MutationViewHolder(@NonNull View itemView) {
            super(itemView);
            viewLineTop = itemView.findViewById(R.id.viewLineTop);
            viewDot = itemView.findViewById(R.id.viewDot);
            viewLineBottom = itemView.findViewById(R.id.viewLineBottom);
            tvMutationType = itemView.findViewById(R.id.tvMutationType);
            tvMutationDate = itemView.findViewById(R.id.tvMutationDate);
            tvQtyDirection = itemView.findViewById(R.id.tvQtyDirection);
            tvStockFlow = itemView.findViewById(R.id.tvStockFlow);
            tvNote = itemView.findViewById(R.id.tvNote);
        }
    }
}
