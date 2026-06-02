package com.example.kasirumkm2.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;

import androidx.appcompat.app.AppCompatActivity;

import com.example.kasirumkm2.R;
import com.example.kasirumkm2.api.ApiClient;
import com.example.kasirumkm2.api.ApiService;
import com.example.kasirumkm2.data.CartItem;
import com.example.kasirumkm2.data.PaymentMethod;
import com.example.kasirumkm2.databinding.ActivityCheckoutBinding;
import com.example.kasirumkm2.databinding.ItemCheckoutSummaryBinding;
import com.example.kasirumkm2.utils.CurrencyHelper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CheckoutActivity extends AppCompatActivity {

    private ActivityCheckoutBinding binding;
    private ApiService apiService;
    private final Gson gson = new Gson();

    private List<CartItem> cartList = new ArrayList<>();
    private final List<PaymentMethod> paymentMethods = new ArrayList<>();

    private int customerId = 0;
    private String customerName = "";
    private int customerGroupId = 1;

    private double grandTotal = 0;
    private double changeAmount = 0;
    private int selectedPaymentMethodId = -1;
    private String selectedPaymentMethodCode = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCheckoutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = ApiClient.getApiService(this);

        // Get Intent extras
        customerId = getIntent().getIntExtra("customer_id", 0);
        customerName = getIntent().getStringExtra("customer_name");
        customerGroupId = getIntent().getIntExtra("customer_group_id", 1);

        String cartJson = getIntent().getStringExtra("cart_json");
        if (cartJson != null) {
            Type listType = new TypeToken<List<CartItem>>() {}.getType();
            cartList = gson.fromJson(cartJson, listType);
        }

        if (cartList.isEmpty()) {
            CurrencyHelper.showToast(this, "Keranjang belanja kosong");
            finish();
            return;
        }

        setupToolbar();
        displayCustomerInfo();
        displayOrderSummary();
        loadPaymentMethods();
        setupListeners();
    }

    private void setupToolbar() {
        binding.btnBack.setOnClickListener(v -> finish());
    }

    private void displayCustomerInfo() {
        binding.tvCustomerName.setText(customerName);
        String groupName = "USER / Reguler";
        if (customerGroupId == 2) groupName = "FREELANCER";
        else if (customerGroupId == 3) groupName = "GROSIR";
        binding.tvCustomerGroup.setText("Golongan: " + groupName);
    }

    private void displayOrderSummary() {
        binding.layoutSummaryItems.removeAllViews();
        grandTotal = 0;

        for (CartItem item : cartList) {
            ItemCheckoutSummaryBinding itemBinding = ItemCheckoutSummaryBinding.inflate(
                    LayoutInflater.from(this), binding.layoutSummaryItems, false);

            itemBinding.tvProductName.setText(item.getProduct().getProductName());
            itemBinding.tvQtyPrice.setText(item.getQty() + " x " + CurrencyHelper.formatRupiah(item.getPricePerUnit()));
            itemBinding.tvSubtotal.setText(CurrencyHelper.formatRupiah(item.getSubtotal()));

            binding.layoutSummaryItems.addView(itemBinding.getRoot());
            grandTotal += item.getSubtotal();
        }

        binding.tvTotalBill.setText(CurrencyHelper.formatRupiah(grandTotal));
    }

    private void loadPaymentMethods() {
        binding.pbPaymentMethods.setVisibility(View.VISIBLE);
        apiService.getPaymentMethods().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                binding.pbPaymentMethods.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonArray dataArray = response.body().getAsJsonArray("data");
                        paymentMethods.clear();
                        binding.rgPaymentMethods.removeAllViews();

                        for (int i = 0; i < dataArray.size(); i++) {
                            PaymentMethod pm = gson.fromJson(dataArray.get(i), PaymentMethod.class);
                            paymentMethods.add(pm);

                            RadioButton rb = new RadioButton(CheckoutActivity.this);
                            rb.setId(pm.getId());
                            rb.setText(pm.getName() + " (" + pm.getCode() + ")");
                            rb.setTag(pm.getCode());
                            rb.setTextSize(14);
                            rb.setPadding(8, 8, 8, 8);
                            rb.setButtonTintList(getColorStateList(R.color.primary));
                            
                            binding.rgPaymentMethods.addView(rb);

                            // Select first by default
                            if (i == 0) {
                                rb.setChecked(true);
                                selectPaymentMethod(pm.getId(), pm.getCode());
                            }
                        }
                    } catch (Exception e) {
                        setupStaticPaymentMethods();
                    }
                } else {
                    setupStaticPaymentMethods();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                binding.pbPaymentMethods.setVisibility(View.GONE);
                setupStaticPaymentMethods();
            }
        });
    }

    private void setupStaticPaymentMethods() {
        paymentMethods.clear();
        binding.rgPaymentMethods.removeAllViews();

        // Static fallbacks as defined in PRD
        paymentMethods.add(new PaymentMethod(1, "TUNAI (CASH)", "CASH"));
        paymentMethods.add(new PaymentMethod(2, "TRANSFER BANK", "TRANSFER"));
        paymentMethods.add(new PaymentMethod(3, "QRIS", "QRIS"));

        for (int i = 0; i < paymentMethods.size(); i++) {
            PaymentMethod pm = paymentMethods.get(i);
            RadioButton rb = new RadioButton(this);
            rb.setId(pm.getId());
            rb.setText(pm.getName());
            rb.setTag(pm.getCode());
            rb.setTextSize(14);
            rb.setPadding(8, 8, 8, 8);
            rb.setButtonTintList(getColorStateList(R.color.primary));

            binding.rgPaymentMethods.addView(rb);

            if (i == 0) {
                rb.setChecked(true);
                selectPaymentMethod(pm.getId(), pm.getCode());
            }
        }
    }

    private void selectPaymentMethod(int id, String code) {
        selectedPaymentMethodId = id;
        selectedPaymentMethodCode = code;

        // Cash Input Card visibility
        if ("CASH".equalsIgnoreCase(code)) {
            binding.cardCashInput.setVisibility(View.VISIBLE);
            binding.etPaidAmount.setText("");
            binding.tvChangeAmount.setText(CurrencyHelper.formatRupiah(0));
            changeAmount = -grandTotal; // requires input
        } else {
            binding.cardCashInput.setVisibility(View.GONE);
            changeAmount = 0; // cashless pays exact total
        }
    }

    private void setupListeners() {
        binding.rgPaymentMethods.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton rb = findViewById(checkedId);
            if (rb != null) {
                selectPaymentMethod(checkedId, (String) rb.getTag());
            }
        });

        binding.etPaidAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                calculateChange();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.btnPay.setOnClickListener(v -> processCheckout());
    }

    private void calculateChange() {
        String input = binding.etPaidAmount.getText().toString().trim();
        if (TextUtils.isEmpty(input)) {
            changeAmount = -grandTotal;
            binding.tvChangeAmount.setText(CurrencyHelper.formatRupiah(0));
            return;
        }

        try {
            double paid = Double.parseDouble(input);
            changeAmount = paid - grandTotal;
            if (changeAmount < 0) {
                binding.tvChangeAmount.setText("Kurang: " + CurrencyHelper.formatRupiah(Math.abs(changeAmount)));
                binding.tvChangeAmount.setTextColor(getColor(R.color.danger_red));
            } else {
                binding.tvChangeAmount.setText(CurrencyHelper.formatRupiah(changeAmount));
                binding.tvChangeAmount.setTextColor(getColor(R.color.primary));
            }
        } catch (NumberFormatException e) {
            changeAmount = -grandTotal;
            binding.tvChangeAmount.setText(CurrencyHelper.formatRupiah(0));
        }
    }

    private void processCheckout() {
        if (selectedPaymentMethodId == -1) {
            CurrencyHelper.showError(binding.getRoot(), "Pilih metode pembayaran terlebih dahulu");
            return;
        }

        double paidAmount = grandTotal;
        if ("CASH".equalsIgnoreCase(selectedPaymentMethodCode)) {
            String paidInput = binding.etPaidAmount.getText().toString().trim();
            if (TextUtils.isEmpty(paidInput)) {
                binding.tilPaidAmount.setError("Masukkan uang diterima");
                return;
            }
            try {
                paidAmount = Double.parseDouble(paidInput);
                if (paidAmount < grandTotal) {
                    binding.tilPaidAmount.setError("Uang diterima kurang dari total tagihan");
                    return;
                }
                binding.tilPaidAmount.setError(null);
            } catch (NumberFormatException e) {
                binding.tilPaidAmount.setError("Masukkan jumlah uang yang valid");
                return;
            }
        }

        // Build Payload
        JsonObject body = new JsonObject();
        if (customerId > 0) {
            body.addProperty("customer_id", customerId);
        } else {
            body.add("customer_id", JsonNull.INSTANCE);
        }
        body.addProperty("payment_method_id", selectedPaymentMethodId);
        body.addProperty("discount", 0);
        body.addProperty("paid_amount", paidAmount);

        JsonArray itemsArray = new JsonArray();
        for (CartItem cartItem : cartList) {
            JsonObject itemObj = new JsonObject();
            itemObj.addProperty("product_id", cartItem.getProduct().getId());
            itemObj.addProperty("qty", cartItem.getQty());
            itemsArray.add(itemObj);
        }
        body.add("items", itemsArray);

        setLoading(true);

        apiService.createSale(body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonObject resObj = response.body();
                        JsonObject saleData = resObj.getAsJsonObject("data");

                        int saleId = saleData.get("id").getAsInt();
                        String invoiceNo = saleData.get("invoice_no").getAsString();
                        double grandTotalRes = saleData.get("grand_total").getAsDouble();
                        double paidAmountRes = saleData.get("paid_amount").getAsDouble();
                        double changeAmountRes = saleData.get("change_amount").getAsDouble();

                        Intent intent = new Intent(CheckoutActivity.this, SuccessActivity.class);
                        intent.putExtra("sale_id", saleId);
                        intent.putExtra("invoice_no", invoiceNo);
                        intent.putExtra("grand_total", grandTotalRes);
                        intent.putExtra("paid_amount", paidAmountRes);
                        intent.putExtra("change_amount", changeAmountRes);
                        intent.putExtra("customer_name", customerName);
                        startActivity(intent);
                        
                        // Close POS activity flow
                        setResult(RESULT_OK);
                        finish();
                    } catch (Exception e) {
                        CurrencyHelper.showError(binding.getRoot(), "Transaksi sukses, tapi gagal membaca response");
                    }
                } else if (response.code() == 401) {
                    handleUnauthorized();
                } else {
                    String errorMsg = "Gagal memproses transaksi";
                    try {
                        if (response.errorBody() != null) {
                            String errorBodyStr = response.errorBody().string();
                            JsonObject err = new com.google.gson.JsonParser()
                                    .parse(errorBodyStr).getAsJsonObject();
                            if (err.has("message")) errorMsg = err.get("message").getAsString();

                            // Check if stock error with data
                            if (errorMsg.contains("Stok") && err.has("data") && !err.get("data").isJsonNull()) {
                                JsonObject errData = err.getAsJsonObject("data");
                                int availableStock = errData.has("available_stock") ? errData.get("available_stock").getAsInt() : 0;
                                int requestedQty = errData.has("requested_qty") ? errData.get("requested_qty").getAsInt() : 0;
                                int errProductId = errData.has("product_id") ? errData.get("product_id").getAsInt() : 0;

                                // Find product name from cart
                                String errProductName = "Produk";
                                for (CartItem ci : cartList) {
                                    if (ci.getProduct().getId() == errProductId) {
                                        errProductName = ci.getProduct().getProductName();
                                        break;
                                    }
                                }

                                new androidx.appcompat.app.AlertDialog.Builder(CheckoutActivity.this)
                                        .setTitle("⚠️ Stok Tidak Cukup")
                                        .setMessage("Stok " + errProductName + " tidak mencukupi!\n\n" +
                                                "📦 Stok tersedia: " + availableStock + "\n" +
                                                "🛒 Jumlah diminta: " + requestedQty + "\n\n" +
                                                "Silakan kurangi jumlah atau periksa stok terlebih dahulu.")
                                        .setPositiveButton("Mengerti", null)
                                        .show();
                                return;
                            }
                        }
                    } catch (Exception e) {}
                    CurrencyHelper.showError(binding.getRoot(), errorMsg);
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                setLoading(false);
                CurrencyHelper.showError(binding.getRoot(), getString(R.string.tidak_ada_koneksi));
            }
        });
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnPay.setEnabled(!loading);
    }

    private void handleUnauthorized() {
        new com.example.kasirumkm2.session.SessionManager(this).clearSession();
        ApiClient.resetClient();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
