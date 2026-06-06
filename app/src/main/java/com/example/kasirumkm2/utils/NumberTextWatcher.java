package com.example.kasirumkm2.utils;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

public class NumberTextWatcher implements TextWatcher {

    public interface OnTextChangedListener {
        void onTextChanged(String text);
    }

    private final EditText editText;
    private final OnTextChangedListener listener;
    private String current = "";

    public NumberTextWatcher(EditText editText) {
        this.editText = editText;
        this.listener = null;
    }

    public NumberTextWatcher(EditText editText, OnTextChangedListener listener) {
        this.editText = editText;
        this.listener = listener;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {}

    @Override
    public void afterTextChanged(Editable s) {
        if (!s.toString().equals(current)) {
            editText.removeTextChangedListener(this);

            // Strip out grouping separators (dots)
            String cleanString = s.toString().replaceAll("[.]", "");

            if (!cleanString.isEmpty()) {
                try {
                    double parsed = Double.parseDouble(cleanString);
                    // Format back using CurrencyHelper's dynamic formatting
                    String formatted = CurrencyHelper.formatNumber(parsed);
                    current = formatted;
                    editText.setText(formatted);
                    editText.setSelection(formatted.length());
                } catch (NumberFormatException e) {
                    // ignore
                }
            } else {
                current = "";
                editText.setText("");
            }

            editText.addTextChangedListener(this);
        }

        if (listener != null) {
            listener.onTextChanged(editText.getText().toString());
        }
    }
}
