package com.example.kasirumkm2.adapter;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kasirumkm2.R;
import com.example.kasirumkm2.utils.CurrencyHelper;
import com.example.kasirumkm2.utils.NumberTextWatcher;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class ProductBulkAdapter extends RecyclerView.Adapter<ProductBulkAdapter.ViewHolder> {

    public static class BulkItem {
        public String productName = "";
        public String productCode = "";
        public String unit = "";
        public String costPrice = "";
        public String priceUser = "";
        public String priceFreelancer = "";
        public String priceGrosir = "";
        public String stock = "";

        // Errors from validation
        public String nameError = null;
        public String codeError = null;
        public String unitError = null;
        public String costPriceError = null;
        public String priceUserError = null;
        public String priceFreelancerError = null;
        public String priceGrosirError = null;
        public String stockError = null;
    }

    private final List<BulkItem> items = new ArrayList<>();
    private final OnDeleteListener deleteListener;
    private final OnScanListener scanListener;

    public interface OnDeleteListener {
        void onDelete(int position);
    }

    public interface OnScanListener {
        void onScanRequested(int position);
    }

    public ProductBulkAdapter(OnDeleteListener deleteListener, OnScanListener scanListener) {
        this.deleteListener = deleteListener;
        this.scanListener = scanListener;
    }

    public void setItems(List<BulkItem> newItems) {
        this.items.clear();
        this.items.addAll(newItems);
        notifyDataSetChanged();
    }

    public List<BulkItem> getItems() {
        return items;
    }

    public void addRow() {
        items.add(new BulkItem());
        notifyItemInserted(items.size() - 1);
    }

    public void removeRow(int position) {
        if (position >= 0 && position < items.size()) {
            items.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, items.size() - position);
        }
    }

    public void updateProductCode(int position, String code) {
        if (position >= 0 && position < items.size()) {
            items.get(position).productCode = code;
            items.get(position).codeError = null;
            notifyItemChanged(position);
        }
    }

    private String formatInitialValue(String rawValue) {
        if (rawValue == null || rawValue.isEmpty()) return "";
        try {
            double parsed = CurrencyHelper.parseDouble(rawValue);
            if (parsed > 0) {
                return CurrencyHelper.formatNumber(parsed);
            }
        } catch (Exception e) {}
        return rawValue;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product_bulk_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BulkItem item = items.get(position);

        holder.tvRowNumber.setText("Produk #" + (position + 1));

        // Remove old format watchers
        if (holder.costPriceFormatWatcher != null) holder.etCostPrice.removeTextChangedListener(holder.costPriceFormatWatcher);
        if (holder.priceUserFormatWatcher != null) holder.etPriceUser.removeTextChangedListener(holder.priceUserFormatWatcher);
        if (holder.priceFreelancerFormatWatcher != null) holder.etPriceFreelancer.removeTextChangedListener(holder.priceFreelancerFormatWatcher);
        if (holder.priceGrosirFormatWatcher != null) holder.etPriceGrosir.removeTextChangedListener(holder.priceGrosirFormatWatcher);

        // Remove old model update watchers
        if (holder.nameWatcher != null) holder.etName.removeTextChangedListener(holder.nameWatcher);
        if (holder.codeWatcher != null) holder.etCode.removeTextChangedListener(holder.codeWatcher);
        if (holder.unitWatcher != null) holder.etUnit.removeTextChangedListener(holder.unitWatcher);
        if (holder.stockWatcher != null) holder.etStock.removeTextChangedListener(holder.stockWatcher);

        // Bind model text to views
        holder.etName.setText(item.productName);
        holder.etCode.setText(item.productCode);
        holder.etUnit.setText(item.unit);
        holder.etStock.setText(item.stock);
        holder.etCostPrice.setText(formatInitialValue(item.costPrice));
        holder.etPriceUser.setText(formatInitialValue(item.priceUser));
        holder.etPriceFreelancer.setText(formatInitialValue(item.priceFreelancer));
        holder.etPriceGrosir.setText(formatInitialValue(item.priceGrosir));

        // Bind errors to TextInputLayouts
        holder.tilName.setError(item.nameError);
        holder.tilCode.setError(item.codeError);
        holder.tilUnit.setError(item.unitError);
        holder.tilStock.setError(item.stockError);
        holder.tilCostPrice.setError(item.costPriceError);
        holder.tilPriceUser.setError(item.priceUserError);
        holder.tilPriceFreelancer.setError(item.priceFreelancerError);
        holder.tilPriceGrosir.setError(item.priceGrosirError);

        // Create and attach new format watchers with model update callbacks
        holder.costPriceFormatWatcher = new NumberTextWatcher(holder.etCostPrice, text -> {
            item.costPrice = text;
            item.costPriceError = null;
            holder.tilCostPrice.setError(null);
        });
        holder.priceUserFormatWatcher = new NumberTextWatcher(holder.etPriceUser, text -> {
            item.priceUser = text;
            item.priceUserError = null;
            holder.tilPriceUser.setError(null);
        });
        holder.priceFreelancerFormatWatcher = new NumberTextWatcher(holder.etPriceFreelancer, text -> {
            item.priceFreelancer = text;
            item.priceFreelancerError = null;
            holder.tilPriceFreelancer.setError(null);
        });
        holder.priceGrosirFormatWatcher = new NumberTextWatcher(holder.etPriceGrosir, text -> {
            item.priceGrosir = text;
            item.priceGrosirError = null;
            holder.tilPriceGrosir.setError(null);
        });

        holder.etCostPrice.addTextChangedListener(holder.costPriceFormatWatcher);
        holder.etPriceUser.addTextChangedListener(holder.priceUserFormatWatcher);
        holder.etPriceFreelancer.addTextChangedListener(holder.priceFreelancerFormatWatcher);
        holder.etPriceGrosir.addTextChangedListener(holder.priceGrosirFormatWatcher);

        // Define and attach new model update watchers for non-price fields
        holder.nameWatcher = new SimpleTextWatcher() {
            @Override
            public void onTextChanged(String text) {
                item.productName = text;
                item.nameError = null;
                holder.tilName.setError(null);
            }
        };
        holder.etName.addTextChangedListener(holder.nameWatcher);

        holder.codeWatcher = new SimpleTextWatcher() {
            @Override
            public void onTextChanged(String text) {
                item.productCode = text;
                item.codeError = null;
                holder.tilCode.setError(null);
            }
        };
        holder.etCode.addTextChangedListener(holder.codeWatcher);

        holder.unitWatcher = new SimpleTextWatcher() {
            @Override
            public void onTextChanged(String text) {
                item.unit = text;
                item.unitError = null;
                holder.tilUnit.setError(null);
            }
        };
        holder.etUnit.addTextChangedListener(holder.unitWatcher);

        holder.stockWatcher = new SimpleTextWatcher() {
            @Override
            public void onTextChanged(String text) {
                item.stock = text;
                item.stockError = null;
                holder.tilStock.setError(null);
            }
        };
        holder.etStock.addTextChangedListener(holder.stockWatcher);

        // Delete button listener
        holder.btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDelete(holder.getAdapterPosition());
            }
        });

        // Scan button listener
        holder.btnScan.setOnClickListener(v -> {
            if (scanListener != null) {
                scanListener.onScanRequested(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        public abstract void onTextChanged(String text);

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            onTextChanged(s.toString());
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRowNumber;
        ImageButton btnDelete;
        TextInputLayout tilName, tilCode, tilUnit, tilStock, tilCostPrice, tilPriceUser, tilPriceFreelancer, tilPriceGrosir;
        TextInputEditText etName, etCode, etUnit, etStock, etCostPrice, etPriceUser, etPriceFreelancer, etPriceGrosir;
        MaterialButton btnScan;

        TextWatcher nameWatcher, codeWatcher, unitWatcher, stockWatcher;
        TextWatcher costPriceFormatWatcher, priceUserFormatWatcher, priceFreelancerFormatWatcher, priceGrosirFormatWatcher;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRowNumber = itemView.findViewById(R.id.tvRowNumber);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            tilName = itemView.findViewById(R.id.tilName);
            tilCode = itemView.findViewById(R.id.tilCode);
            tilUnit = itemView.findViewById(R.id.tilUnit);
            tilStock = itemView.findViewById(R.id.tilStock);
            tilCostPrice = itemView.findViewById(R.id.tilCostPrice);
            tilPriceUser = itemView.findViewById(R.id.tilPriceUser);
            tilPriceFreelancer = itemView.findViewById(R.id.tilPriceFreelancer);
            tilPriceGrosir = itemView.findViewById(R.id.tilPriceGrosir);
            
            etName = itemView.findViewById(R.id.etName);
            etCode = itemView.findViewById(R.id.etCode);
            etUnit = itemView.findViewById(R.id.etUnit);
            etStock = itemView.findViewById(R.id.etStock);
            etCostPrice = itemView.findViewById(R.id.etCostPrice);
            etPriceUser = itemView.findViewById(R.id.etPriceUser);
            etPriceFreelancer = itemView.findViewById(R.id.etPriceFreelancer);
            etPriceGrosir = itemView.findViewById(R.id.etPriceGrosir);
            
            btnScan = itemView.findViewById(R.id.btnScan);
        }
    }
}
