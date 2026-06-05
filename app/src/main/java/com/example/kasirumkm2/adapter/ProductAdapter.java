package com.example.kasirumkm2.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kasirumkm2.R;
import com.example.kasirumkm2.data.Product;
import com.example.kasirumkm2.utils.CurrencyHelper;

import java.util.ArrayList;
import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {

    private List<Product> productList = new ArrayList<>();
    private List<Product> filteredList = new ArrayList<>();
    private OnProductClickListener listener;
    private int userGroupId = 1; // Default fallback

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    public ProductAdapter(OnProductClickListener listener) {
        this.listener = listener;
    }

    public void setData(List<Product> data) {
        this.productList = new ArrayList<>(data);
        this.filteredList = new ArrayList<>(data);
        notifyDataSetChanged();
    }

    public void addData(List<Product> data) {
        int startPosition = this.filteredList.size();
        this.productList.addAll(data);
        this.filteredList.addAll(data);
        notifyItemRangeInserted(startPosition, data.size());
    }

    public void clearData() {
        this.productList.clear();
        this.filteredList.clear();
        notifyDataSetChanged();
    }


    public void setUserGroupId(int userGroupId) {
        this.userGroupId = userGroupId;
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
                .inflate(R.layout.item_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = filteredList.get(position);

        holder.tvProductName.setText(product.getProductName());
        holder.tvEmoji.setText(product.getEmoji());
        holder.tvCode.setText(product.getProductCode() != null ? product.getProductCode() : "");
        holder.tvStock.setText("Stok: " + product.getStock() + " " + (product.getUnit() != null ? product.getUnit() : "pcs"));

        // Use dynamic selling price based on dynamic userGroupId
        double price = product.getSellingPrice(userGroupId);
        holder.tvPrice.setText(CurrencyHelper.formatRupiah(price));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onProductClick(product);
        });
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEmoji, tvProductName, tvCode, tvStock, tvPrice;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEmoji = itemView.findViewById(R.id.tvEmoji);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvCode = itemView.findViewById(R.id.tvCode);
            tvStock = itemView.findViewById(R.id.tvStock);
            tvPrice = itemView.findViewById(R.id.tvPrice);
        }
    }
}
