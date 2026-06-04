package com.example.kasirumkm2.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
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

public class CompanyAdapter extends RecyclerView.Adapter<CompanyAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(JsonObject company);
    }

    private final Context context;
    private final List<JsonObject> companyList = new ArrayList<>();
    private final OnItemClickListener listener;

    public CompanyAdapter(Context context, OnItemClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setData(List<JsonObject> list) {
        companyList.clear();
        if (list != null) {
            companyList.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_company, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        JsonObject company = companyList.get(position);
        holder.bind(company);
    }

    @Override
    public int getItemCount() {
        return companyList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvInitial;
        private final TextView tvName;
        private final TextView tvCode;
        private final TextView tvContact;
        private final TextView tvStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInitial = itemView.findViewById(R.id.tvCompanyInitial);
            tvName = itemView.findViewById(R.id.tvCompanyName);
            tvCode = itemView.findViewById(R.id.tvCompanyCode);
            tvContact = itemView.findViewById(R.id.tvCompanyContact);
            tvStatus = itemView.findViewById(R.id.tvCompanyStatus);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onItemClick(companyList.get(pos));
                }
            });
        }

        public void bind(JsonObject comp) {
            String name = comp.has("company_name") ? comp.get("company_name").getAsString() : "-";
            String code = comp.has("company_code") ? comp.get("company_code").getAsString() : "-";
            
            tvName.setText(name);
            tvCode.setText("Kode: " + code);

            // Generate initial
            if (!name.isEmpty()) {
                tvInitial.setText(String.valueOf(name.charAt(0)).toUpperCase());
            } else {
                tvInitial.setText("C");
            }

            // Bind Address and Phone if present
            String address = comp.has("address") && !comp.get("address").isJsonNull() ? comp.get("address").getAsString() : "";
            String phone = comp.has("phone") && !comp.get("phone").isJsonNull() ? comp.get("phone").getAsString() : "";
            if (!address.isEmpty() || !phone.isEmpty()) {
                String contactStr = "";
                if (!address.isEmpty()) contactStr += address;
                if (!phone.isEmpty()) {
                    if (!contactStr.isEmpty()) contactStr += " · ";
                    contactStr += phone;
                }
                tvContact.setText(contactStr);
                tvContact.setVisibility(View.VISIBLE);
            } else {
                tvContact.setVisibility(View.GONE);
            }

            // Bind Status
            int status = comp.has("status") && !comp.get("status").isJsonNull() ? comp.get("status").getAsInt() : 1;
            if (status == 1) {
                tvStatus.setText("Aktif");
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.stock_safe));
                tvStatus.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.stock_safe_bg)));
            } else {
                tvStatus.setText("Nonaktif");
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.stock_empty));
                tvStatus.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.stock_empty_bg)));
            }
        }
    }
}
