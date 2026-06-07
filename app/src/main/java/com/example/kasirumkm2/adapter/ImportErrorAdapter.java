package com.example.kasirumkm2.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kasirumkm2.R;

import java.util.ArrayList;
import java.util.List;

public class ImportErrorAdapter extends RecyclerView.Adapter<ImportErrorAdapter.ViewHolder> {

    public static class ImportErrorItem {
        public final int rowIndex;
        public final String errorMessage;

        public ImportErrorItem(int rowIndex, String errorMessage) {
            this.rowIndex = rowIndex;
            this.errorMessage = errorMessage;
        }
    }

    private final List<ImportErrorItem> items = new ArrayList<>();

    public void setItems(List<ImportErrorItem> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_import_error, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ImportErrorItem item = items.get(position);
        holder.tvRowIndex.setText("Baris " + item.rowIndex);
        holder.tvErrorMessage.setText(item.errorMessage);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRowIndex;
        TextView tvErrorMessage;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRowIndex = itemView.findViewById(R.id.tvRowIndex);
            tvErrorMessage = itemView.findViewById(R.id.tvErrorMessage);
        }
    }
}
