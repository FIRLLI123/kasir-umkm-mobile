package com.example.kasirumkm2.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kasirumkm2.R;
import com.example.kasirumkm2.utils.CurrencyHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class SubscriptionPlanAdapter extends RecyclerView.Adapter<SubscriptionPlanAdapter.PlanViewHolder> {

    public interface OnPlanClickListener {
        void onPlanClick(JsonObject plan);
    }

    private final List<JsonObject> plans = new ArrayList<>();
    private final OnPlanClickListener listener;
    private boolean isLifetimeUser = false;

    public SubscriptionPlanAdapter(OnPlanClickListener listener) {
        this.listener = listener;
    }

    public void setData(List<JsonObject> newPlans) {
        plans.clear();
        if (newPlans != null) {
            plans.addAll(newPlans);
        }
        notifyDataSetChanged();
    }

    public void setLifetimeUser(boolean isLifetimeUser) {
        this.isLifetimeUser = isLifetimeUser;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PlanViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_subscription_plan, parent, false);
        return new PlanViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlanViewHolder holder, int position) {
        holder.bind(plans.get(position), listener, isLifetimeUser);
    }

    @Override
    public int getItemCount() {
        return plans.size();
    }

    static class PlanViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardPlan;
        private final TextView tvPromoBadge;
        private final TextView tvPlanName;
        private final TextView tvPlanPrice;
        private final TextView tvPlanDuration;
        private final TextView tvPlanDescription;
        private final MaterialButton btnSelectPlan;

        public PlanViewHolder(@NonNull View itemView) {
            super(itemView);
            cardPlan = itemView.findViewById(R.id.cardPlan);
            tvPromoBadge = itemView.findViewById(R.id.tvPromoBadge);
            tvPlanName = itemView.findViewById(R.id.tvPlanName);
            tvPlanPrice = itemView.findViewById(R.id.tvPlanPrice);
            tvPlanDuration = itemView.findViewById(R.id.tvPlanDuration);
            tvPlanDescription = itemView.findViewById(R.id.tvPlanDescription);
            btnSelectPlan = itemView.findViewById(R.id.btnSelectPlan);
        }

        public void bind(JsonObject plan, OnPlanClickListener listener, boolean isLifetimeUser) {
            Context context = itemView.getContext();
            String name = plan.has("name") ? plan.get("name").getAsString() : "Paket";
            double price = plan.has("price") ? plan.get("price").getAsDouble() : 0;
            boolean isLifetime = plan.has("is_lifetime") && plan.get("is_lifetime").getAsBoolean();
            int durationDays = plan.has("duration_days") && !plan.get("duration_days").isJsonNull() ? plan.get("duration_days").getAsInt() : 0;
            String description = plan.has("description") ? plan.get("description").getAsString() : "";

            tvPlanName.setText(name);
            tvPlanPrice.setText(CurrencyHelper.formatRupiah(price));
            tvPlanDescription.setText(description);

            if (isLifetime) {
                tvPlanDuration.setText("Masa aktif: Selamanya (Lifetime)");
                
                // Visual Highlight for Lifetime (Purple Promo theme)
                tvPromoBadge.setVisibility(View.VISIBLE);
                tvPromoBadge.setText("PROMO TERBAIK");
                
                cardPlan.setStrokeColor(context.getColor(R.color.purple_primary));
                cardPlan.setStrokeWidth((int) (2.5f * context.getResources().getDisplayMetrics().density));
                cardPlan.setCardBackgroundColor(context.getColor(R.color.purple_light));
                
                // Color primary highlight button
                btnSelectPlan.setBackgroundColor(context.getColor(R.color.purple_primary));
                btnSelectPlan.setTextColor(context.getColor(R.color.white));
            } else {
                tvPlanDuration.setText("Masa aktif: " + durationDays + " Hari");
                tvPromoBadge.setVisibility(View.GONE);
                
                // Default theme
                cardPlan.setStrokeColor(context.getColor(R.color.border));
                cardPlan.setStrokeWidth((int) (1.5f * context.getResources().getDisplayMetrics().density));
                cardPlan.setCardBackgroundColor(context.getColor(R.color.white));
                
                btnSelectPlan.setBackgroundColor(context.getColor(R.color.primary));
                btnSelectPlan.setTextColor(context.getColor(R.color.white));
            }

            if (isLifetimeUser) {
                btnSelectPlan.setEnabled(false);
                btnSelectPlan.setText("AKUN AKTIF SELAMANYA");
                btnSelectPlan.setBackgroundTintList(ColorStateList.valueOf(context.getColor(R.color.text_light)));
            } else {
                btnSelectPlan.setEnabled(true);
                btnSelectPlan.setText("PILIH PAKET");
                btnSelectPlan.setBackgroundTintList(null);
            }

            btnSelectPlan.setOnClickListener(v -> listener.onPlanClick(plan));
        }
    }
}
