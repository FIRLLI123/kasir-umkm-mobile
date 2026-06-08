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

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(JsonObject user);
    }

    private final Context context;
    private final List<JsonObject> userList = new ArrayList<>();
    private final OnItemClickListener listener;

    public UserAdapter(Context context, OnItemClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setData(List<JsonObject> list) {
        userList.clear();
        if (list != null) {
            userList.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        JsonObject user = userList.get(position);
        holder.bind(user);
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvInitial;
        private final TextView tvName;
        private final TextView tvRole;
        private final TextView tvEmail;
        private final TextView tvCompany;
        private final TextView tvStatus;
        private final TextView tvPhone;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInitial = itemView.findViewById(R.id.tvUserInitial);
            tvName = itemView.findViewById(R.id.tvUserName);
            tvRole = itemView.findViewById(R.id.tvUserRole);
            tvEmail = itemView.findViewById(R.id.tvUserEmail);
            tvCompany = itemView.findViewById(R.id.tvUserCompany);
            tvStatus = itemView.findViewById(R.id.tvUserStatus);
            tvPhone = itemView.findViewById(R.id.tvUserPhone);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onItemClick(userList.get(pos));
                }
            });
        }

        public void bind(JsonObject user) {
            String name = user.has("name") && !user.get("name").isJsonNull() ? user.get("name").getAsString() : "-";
            String email = user.has("email") && !user.get("email").isJsonNull() ? user.get("email").getAsString() : "-";
            String role = user.has("role") && !user.get("role").isJsonNull() ? user.get("role").getAsString() : "KASIR";
            
            tvName.setText(name);
            tvEmail.setText(email);
            tvRole.setText(role.toUpperCase());

            // Generate initial
            if (!name.isEmpty()) {
                tvInitial.setText(String.valueOf(name.charAt(0)).toUpperCase());
            } else {
                tvInitial.setText("U");
            }

            // Role badge colors
            if ("SUPER_ADMIN".equalsIgnoreCase(role)) {
                tvRole.setTextColor(ContextCompat.getColor(context, R.color.badge_grosir_text));
                tvRole.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.badge_grosir_bg)));
            } else if ("ADMIN".equalsIgnoreCase(role)) {
                tvRole.setTextColor(ContextCompat.getColor(context, R.color.badge_freelancer_text));
                tvRole.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.badge_freelancer_bg)));
            } else {
                tvRole.setTextColor(ContextCompat.getColor(context, R.color.badge_user_text));
                tvRole.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.badge_user_bg)));
            }

            // Bind Company Name
            com.example.kasirumkm2.session.SessionManager session = new com.example.kasirumkm2.session.SessionManager(context);
            if (session.isCompanyOwner()) {
                tvCompany.setVisibility(View.GONE);
            } else {
                tvCompany.setVisibility(View.VISIBLE);
                String companyName = "-";
                if (user.has("company") && !user.get("company").isJsonNull()) {
                    JsonObject comp = user.getAsJsonObject("company");
                    if (comp.has("company_name") && !comp.get("company_name").isJsonNull()) {
                        companyName = comp.get("company_name").getAsString();
                    }
                }
                tvCompany.setText("🏢 Toko: " + companyName);
            }

            // Bind Phone
            String phone = user.has("phone") && !user.get("phone").isJsonNull() ? user.get("phone").getAsString() : "";
            if (phone.isEmpty()) {
                tvPhone.setText("📞 No HP: -");
            } else {
                tvPhone.setText("📞 No HP: " + phone);
            }

            // Bind Status
            String status = user.has("status") && !user.get("status").isJsonNull() ? user.get("status").getAsString() : "00";
            if ("00".equals(status)) {
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
