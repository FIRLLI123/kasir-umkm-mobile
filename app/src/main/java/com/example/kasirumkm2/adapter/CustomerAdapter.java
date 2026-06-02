package com.example.kasirumkm2.adapter;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kasirumkm2.R;
import com.example.kasirumkm2.data.Customer;

import java.util.ArrayList;
import java.util.List;

public class CustomerAdapter extends RecyclerView.Adapter<CustomerAdapter.ViewHolder> {

    private List<Customer> customerList = new ArrayList<>();
    private List<Customer> filteredList = new ArrayList<>();
    private OnCustomerClickListener listener;

    public interface OnCustomerClickListener {
        void onCustomerClick(Customer customer);
    }

    public CustomerAdapter(OnCustomerClickListener listener) {
        this.listener = listener;
    }

    public void setData(List<Customer> data) {
        this.customerList = data;
        this.filteredList = new ArrayList<>(data);
        notifyDataSetChanged();
    }

    public void filter(String query) {
        filteredList.clear();
        if (query == null || query.isEmpty()) {
            filteredList.addAll(customerList);
        } else {
            String lower = query.toLowerCase();
            for (Customer c : customerList) {
                if ((c.getCustomerName() != null && c.getCustomerName().toLowerCase().contains(lower)) ||
                    (c.getCustomerCode() != null && c.getCustomerCode().toLowerCase().contains(lower)) ||
                    (c.getPhone() != null && c.getPhone().contains(lower))) {
                    filteredList.add(c);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_customer, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Customer customer = filteredList.get(position);

        holder.tvCustomerName.setText(customer.getCustomerName());
        holder.tvInitials.setText(customer.getInitials());
        holder.tvPhone.setText(customer.getPhone() != null ? customer.getPhone() : "-");
        holder.tvCode.setText(customer.getCustomerCode() != null ? customer.getCustomerCode() : "");

        // Badge golongan
        String groupName = customer.getGroupName().toUpperCase();
        holder.tvBadge.setText(groupName);

        int bgColor, textColor;
        switch (groupName) {
            case "FREELANCER":
                bgColor = holder.itemView.getContext().getColor(R.color.badge_freelancer_bg);
                textColor = holder.itemView.getContext().getColor(R.color.badge_freelancer_text);
                break;
            case "GROSIR":
                bgColor = holder.itemView.getContext().getColor(R.color.badge_grosir_bg);
                textColor = holder.itemView.getContext().getColor(R.color.badge_grosir_text);
                break;
            default: // USER
                bgColor = holder.itemView.getContext().getColor(R.color.badge_user_bg);
                textColor = holder.itemView.getContext().getColor(R.color.badge_user_text);
                break;
        }

        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setCornerRadius(40f);
        badgeBg.setColor(bgColor);
        holder.tvBadge.setBackground(badgeBg);
        holder.tvBadge.setTextColor(textColor);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onCustomerClick(customer);
        });
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvInitials, tvCustomerName, tvPhone, tvCode, tvBadge;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInitials = itemView.findViewById(R.id.tvInitials);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
            tvPhone = itemView.findViewById(R.id.tvPhone);
            tvCode = itemView.findViewById(R.id.tvCode);
            tvBadge = itemView.findViewById(R.id.tvBadge);
        }
    }
}
