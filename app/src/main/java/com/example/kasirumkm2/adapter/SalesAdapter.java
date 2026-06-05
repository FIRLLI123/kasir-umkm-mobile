package com.example.kasirumkm2.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kasirumkm2.R;
import com.example.kasirumkm2.utils.CurrencyHelper;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class SalesAdapter extends RecyclerView.Adapter<SalesAdapter.ViewHolder> {

    private List<JsonObject> salesList = new ArrayList<>();
    private OnSaleClickListener listener;

    public interface OnSaleClickListener {
        void onSaleClick(JsonObject sale);
    }

    public SalesAdapter(OnSaleClickListener listener) {
        this.listener = listener;
    }

    public void setData(List<JsonObject> data) {
        this.salesList = new ArrayList<>(data);
        notifyDataSetChanged();
    }

    public void addData(List<JsonObject> data) {
        int startPosition = this.salesList.size();
        this.salesList.addAll(data);
        notifyItemRangeInserted(startPosition, data.size());
    }

    public void clearData() {
        this.salesList.clear();
        notifyDataSetChanged();
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recent_sale, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        JsonObject sale = salesList.get(position);

        String invoiceNo = sale.has("invoice_no") ? sale.get("invoice_no").getAsString() : "-";
        double grandTotal = sale.has("grand_total") ? sale.get("grand_total").getAsDouble() : 0;

        holder.tvInvoice.setText(invoiceNo);
        holder.tvAmount.setText(CurrencyHelper.formatRupiah(grandTotal));

        // Customer name
        if (sale.has("customer") && !sale.get("customer").isJsonNull()) {
            JsonObject customer = sale.getAsJsonObject("customer");
            String customerName = customer.has("customer_name") ?
                    customer.get("customer_name").getAsString() : "-";
            holder.tvCustomerName.setText(customerName);
        } else {
            holder.tvCustomerName.setText("-");
        }

        // Payment method
        if (sale.has("payment_method") && !sale.get("payment_method").isJsonNull()) {
            JsonObject pm = sale.getAsJsonObject("payment_method");
            holder.tvPaymentMethod.setText(pm.has("name") ? pm.get("name").getAsString() : "");
        } else {
            holder.tvPaymentMethod.setText("");
        }

        // Status styling
        String status = sale.has("status") ? sale.get("status").getAsString() : "00";
        if ("01".equals(status) || "98".equals(status)) {
            // Voided
            holder.tvAmount.setTextColor(holder.itemView.getContext().getColor(R.color.danger_red));
            holder.tvIcon.setText("❌");
        } else {
            holder.tvAmount.setTextColor(holder.itemView.getContext().getColor(R.color.primary));
            holder.tvIcon.setText("🧾");
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onSaleClick(sale);
        });
    }

    @Override
    public int getItemCount() {
        return salesList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvIcon, tvInvoice, tvCustomerName, tvAmount, tvPaymentMethod;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIcon = itemView.findViewById(R.id.tvIcon);
            tvInvoice = itemView.findViewById(R.id.tvInvoice);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvPaymentMethod = itemView.findViewById(R.id.tvPaymentMethod);
        }
    }
}
