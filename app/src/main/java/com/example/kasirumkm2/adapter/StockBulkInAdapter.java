package com.example.kasirumkm2.adapter;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kasirumkm2.R;
import com.example.kasirumkm2.data.Product;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

public class StockBulkInAdapter extends RecyclerView.Adapter<StockBulkInAdapter.ViewHolder> {

    public static class BulkStockItem {
        public int productId = -1;
        public String productName = "";
        public String productCode = "";
        public int currentStock = 0;
        public String qtyAdd = "";
        public String note = "";

        // UI Error messages
        public String productError = null;
        public String qtyError = null;
        public String noteError = null;
    }

    private final List<BulkStockItem> items = new ArrayList<>();
    private final List<Product> allProducts;
    private final List<String> productDisplayList = new ArrayList<>();
    private final OnDeleteListener deleteListener;
    private final OnScanRequestedListener scanListener;

    public interface OnDeleteListener {
        void onDelete(int position);
    }

    public interface OnScanRequestedListener {
        void onScanRequested(int position);
    }

    public StockBulkInAdapter(List<Product> allProducts, OnDeleteListener deleteListener, OnScanRequestedListener scanListener) {
        this.allProducts = allProducts;
        this.deleteListener = deleteListener;
        this.scanListener = scanListener;

        for (Product p : allProducts) {
            String code = p.getProductCode() != null && !p.getProductCode().isEmpty() ? p.getProductCode() : "-";
            productDisplayList.add(p.getProductName() + " (" + code + ") - Stok: " + p.getStock());
        }
    }

    public void setItems(List<BulkStockItem> newItems) {
        this.items.clear();
        this.items.addAll(newItems);
        notifyDataSetChanged();
    }

    public List<BulkStockItem> getItems() {
        return items;
    }

    public void addRow() {
        items.add(new BulkStockItem());
        notifyItemInserted(items.size() - 1);
    }

    public void removeRow(int position) {
        if (position >= 0 && position < items.size()) {
            items.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, items.size() - position);
        }
    }

    private Product findProductByDisplayString(String display) {
        for (Product p : allProducts) {
            String code = p.getProductCode() != null && !p.getProductCode().isEmpty() ? p.getProductCode() : "-";
            String s = p.getProductName() + " (" + code + ") - Stok: " + p.getStock();
            if (s.equals(display)) {
                return p;
            }
        }
        return null;
    }

    private boolean isProductSelectedElsewhere(int productId, int currentPosition) {
        for (int i = 0; i < items.size(); i++) {
            if (i != currentPosition && items.get(i).productId == productId) {
                return true;
            }
        }
        return false;
    }

    private void updateFinalStock(ViewHolder holder, BulkStockItem item) {
        if (item.productId == -1) {
            holder.etFinalStock.setText("");
            return;
        }
        String qtyStr = item.qtyAdd.trim();
        if (qtyStr.isEmpty()) {
            holder.etFinalStock.setText("");
            return;
        }
        try {
            int qty = Integer.parseInt(qtyStr);
            int finalStock = item.currentStock + qty;
            holder.etFinalStock.setText(String.valueOf(finalStock));
        } catch (NumberFormatException e) {
            holder.etFinalStock.setText("");
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_stock_bulk_in_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BulkStockItem item = items.get(position);

        holder.tvRowNumber.setText("Produk #" + (position + 1));

        // Remove old text watch listeners to prevent recycled view updates
        if (holder.qtyWatcher != null) holder.etQtyAdd.removeTextChangedListener(holder.qtyWatcher);
        if (holder.noteWatcher != null) holder.etNote.removeTextChangedListener(holder.noteWatcher);
        if (holder.productWatcher != null) holder.actvProduct.removeTextChangedListener(holder.productWatcher);

        // Set up adapter for AutoCompleteTextView
        ArrayAdapter<String> productAdapter = new ArrayAdapter<>(holder.itemView.getContext(),
                android.R.layout.simple_dropdown_item_1line, productDisplayList);
        holder.actvProduct.setAdapter(productAdapter);

        // Bind model text/values to views
        String currentText = "";
        if (item.productId != -1) {
            String code = item.productCode != null && !item.productCode.isEmpty() ? item.productCode : "-";
            currentText = item.productName + " (" + code + ") - Stok: " + item.currentStock;
        }
        holder.actvProduct.setText(currentText, false);

        holder.etQtyAdd.setText(item.qtyAdd);
        holder.etNote.setText(item.note);
        holder.etCurrentStock.setText(item.productId != -1 ? String.valueOf(item.currentStock) : "");

        if (item.productId != -1 && !item.qtyAdd.isEmpty()) {
            try {
                int qty = Integer.parseInt(item.qtyAdd);
                holder.etFinalStock.setText(String.valueOf(item.currentStock + qty));
            } catch (Exception e) {
                holder.etFinalStock.setText("");
            }
        } else {
            holder.etFinalStock.setText("");
        }

        // Bind error states
        holder.tilProduct.setError(item.productError);
        holder.tilQtyAdd.setError(item.qtyError);
        holder.tilNote.setError(item.noteError);

        // Dropdown Click & Focus listeners
        holder.actvProduct.setOnClickListener(v -> holder.actvProduct.showDropDown());
        holder.actvProduct.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                holder.actvProduct.showDropDown();
            } else {
                String entered = holder.actvProduct.getText().toString();
                Product matched = findProductByDisplayString(entered);
                if (matched == null) {
                    if (item.productId != -1) {
                        String code = item.productCode != null && !item.productCode.isEmpty() ? item.productCode : "-";
                        String restoreText = item.productName + " (" + code + ") - Stok: " + item.currentStock;
                        holder.actvProduct.setText(restoreText, false);
                    } else {
                        holder.actvProduct.setText("", false);
                    }
                }
            }
        });

        // Dropdown Item Selected Listener
        holder.actvProduct.setOnItemClickListener((parent, view, selectionPosition, id) -> {
            String selected = (String) parent.getItemAtPosition(selectionPosition);
            Product p = findProductByDisplayString(selected);
            int currentPos = holder.getAdapterPosition();
            if (p != null && currentPos != RecyclerView.NO_POSITION) {
                if (isProductSelectedElsewhere(p.getId(), currentPos)) {
                    item.productId = -1;
                    item.productName = "";
                    item.productCode = "";
                    item.currentStock = 0;
                    item.qtyAdd = "";
                    item.productError = "Produk ini sudah dipilih di baris lain";
                    holder.tilProduct.setError(item.productError);
                    holder.actvProduct.setText("", false);
                    holder.etCurrentStock.setText("");
                    holder.etQtyAdd.setText("");
                    holder.etFinalStock.setText("");
                } else {
                    item.productId = p.getId();
                    item.productName = p.getProductName();
                    item.productCode = p.getProductCode();
                    item.currentStock = p.getStock();
                    item.productError = null;
                    holder.tilProduct.setError(null);
                    holder.etCurrentStock.setText(String.valueOf(p.getStock()));
                    updateFinalStock(holder, item);
                }
            }
        });

        // Define and attach new model update watchers for text fields
        holder.qtyWatcher = new SimpleTextWatcher() {
            @Override
            public void onTextChanged(String text) {
                item.qtyAdd = text;
                item.qtyError = null;
                holder.tilQtyAdd.setError(null);
                updateFinalStock(holder, item);
            }
        };
        holder.etQtyAdd.addTextChangedListener(holder.qtyWatcher);

        holder.noteWatcher = new SimpleTextWatcher() {
            @Override
            public void onTextChanged(String text) {
                item.note = text;
                item.noteError = null;
                holder.tilNote.setError(null);
            }
        };
        holder.etNote.addTextChangedListener(holder.noteWatcher);

        holder.productWatcher = new SimpleTextWatcher() {
            @Override
            public void onTextChanged(String text) {
                if (text.isEmpty() && item.productId != -1) {
                    item.productId = -1;
                    item.productName = "";
                    item.productCode = "";
                    item.currentStock = 0;
                    item.qtyAdd = "";
                    item.productError = null;
                    holder.tilProduct.setError(null);
                    holder.etCurrentStock.setText("");
                    holder.etQtyAdd.setText("");
                    holder.etFinalStock.setText("");
                }
            }
        };
        holder.actvProduct.addTextChangedListener(holder.productWatcher);

        // Delete Row Action
        holder.btnDelete.setOnClickListener(v -> {
            int currentPos = holder.getAdapterPosition();
            if (deleteListener != null && currentPos != RecyclerView.NO_POSITION) {
                deleteListener.onDelete(currentPos);
            }
        });

        // Scan Barcode Action
        holder.btnScan.setOnClickListener(v -> {
            int currentPos = holder.getAdapterPosition();
            if (scanListener != null && currentPos != RecyclerView.NO_POSITION) {
                scanListener.onScanRequested(currentPos);
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
        com.google.android.material.button.MaterialButton btnScan;
        TextInputLayout tilProduct, tilCurrentStock, tilQtyAdd, tilFinalStock, tilNote;
        AutoCompleteTextView actvProduct;
        TextInputEditText etCurrentStock, etQtyAdd, etFinalStock, etNote;

        TextWatcher qtyWatcher, noteWatcher, productWatcher;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRowNumber = itemView.findViewById(R.id.tvRowNumber);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnScan = itemView.findViewById(R.id.btnScan);
            tilProduct = itemView.findViewById(R.id.tilProduct);
            tilCurrentStock = itemView.findViewById(R.id.tilCurrentStock);
            tilQtyAdd = itemView.findViewById(R.id.tilQtyAdd);
            tilFinalStock = itemView.findViewById(R.id.tilFinalStock);
            tilNote = itemView.findViewById(R.id.tilNote);

            actvProduct = itemView.findViewById(R.id.actvProduct);
            etCurrentStock = itemView.findViewById(R.id.etCurrentStock);
            etQtyAdd = itemView.findViewById(R.id.etQtyAdd);
            etFinalStock = itemView.findViewById(R.id.etFinalStock);
            etNote = itemView.findViewById(R.id.etNote);
        }
    }
}
