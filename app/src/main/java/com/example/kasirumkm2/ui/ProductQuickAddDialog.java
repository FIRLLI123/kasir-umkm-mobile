package com.example.kasirumkm2.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.kasirumkm2.api.ApiClient;
import com.example.kasirumkm2.api.ApiService;
import com.example.kasirumkm2.data.Product;
import com.example.kasirumkm2.databinding.DialogProductQuickAddBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductQuickAddDialog extends BottomSheetDialogFragment {

    private static final int PERMISSION_REQUEST_CAMERA = 201;

    public interface OnProductAddedListener {
        void onProductAdded(Product product);
    }

    private DialogProductQuickAddBinding binding;
    private ApiService apiService;
    private OnProductAddedListener listener;
    private String prefilledName = "";

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(
            new ScanContract(),
            result -> {
                if (result.getContents() != null) {
                    binding.etCode.setText(result.getContents());
                    binding.tilCode.setError(null);
                }
            }
    );

    public static ProductQuickAddDialog newInstance(String prefilledName, OnProductAddedListener listener) {
        ProductQuickAddDialog fragment = new ProductQuickAddDialog();
        fragment.listener = listener;
        Bundle args = new Bundle();
        args.putString("prefilled_name", prefilledName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            prefilledName = getArguments().getString("prefilled_name", "");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogProductQuickAddBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        apiService = ApiClient.getApiService(requireContext());

        if (!TextUtils.isEmpty(prefilledName)) {
            binding.etName.setText(prefilledName);
        }

        binding.etCostPrice.addTextChangedListener(new com.example.kasirumkm2.utils.NumberTextWatcher(binding.etCostPrice));
        binding.etPriceUser.addTextChangedListener(new com.example.kasirumkm2.utils.NumberTextWatcher(binding.etPriceUser));
        binding.etPriceFreelancer.addTextChangedListener(new com.example.kasirumkm2.utils.NumberTextWatcher(binding.etPriceFreelancer));
        binding.etPriceGrosir.addTextChangedListener(new com.example.kasirumkm2.utils.NumberTextWatcher(binding.etPriceGrosir));

        binding.btnCancel.setOnClickListener(v -> dismiss());
        binding.btnScan.setOnClickListener(v -> checkCameraPermissionAndScan());
        binding.btnSave.setOnClickListener(v -> validateAndSubmit());
    }

    private void checkCameraPermissionAndScan() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startScan();
        } else {
            requestPermissions(
                    new String[]{Manifest.permission.CAMERA},
                    PERMISSION_REQUEST_CAMERA
            );
        }
    }

    private void startScan() {
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES);
        options.setPrompt("Scan Barcode");
        options.setCameraId(0);
        options.setBeepEnabled(true);
        options.setBarcodeImageEnabled(false);
        barcodeLauncher.launch(options);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScan();
            } else {
                Toast.makeText(requireContext(), "Izin kamera dibutuhkan untuk scan barcode", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void validateAndSubmit() {
        String name = binding.etName.getText().toString().trim();
        String code = binding.etCode.getText().toString().trim();
        String unit = binding.etUnit.getText().toString().trim();
        String stockStr = binding.etStock.getText().toString().trim();
        String costPriceStr = binding.etCostPrice.getText().toString().trim();
        
        String priceUserStr = binding.etPriceUser.getText().toString().trim();
        String priceFreelancerStr = binding.etPriceFreelancer.getText().toString().trim();
        String priceGrosirStr = binding.etPriceGrosir.getText().toString().trim();

        boolean hasError = false;

        if (TextUtils.isEmpty(name)) {
            binding.tilName.setError("Nama produk wajib diisi");
            hasError = true;
        } else {
            binding.tilName.setError(null);
        }

        // Validate USER price (Wajib)
        if (TextUtils.isEmpty(priceUserStr)) {
            binding.tilPriceUser.setError("Harga USER wajib diisi");
            hasError = true;
        } else {
            try {
                double sell = com.example.kasirumkm2.utils.CurrencyHelper.parseDouble(priceUserStr);
                if (sell < 0) {
                    binding.tilPriceUser.setError("Harga tidak boleh kurang dari 0");
                    hasError = true;
                } else {
                    binding.tilPriceUser.setError(null);
                }
            } catch (NumberFormatException e) {
                binding.tilPriceUser.setError("Format harga salah");
                hasError = true;
            }
        }

        // Validate Freelancer price (Opsional)
        if (!TextUtils.isEmpty(priceFreelancerStr)) {
            try {
                double sell = com.example.kasirumkm2.utils.CurrencyHelper.parseDouble(priceFreelancerStr);
                if (sell < 0) {
                    binding.tilPriceFreelancer.setError("Harga tidak boleh kurang dari 0");
                    hasError = true;
                } else {
                    binding.tilPriceFreelancer.setError(null);
                }
            } catch (NumberFormatException e) {
                binding.tilPriceFreelancer.setError("Format harga salah");
                hasError = true;
            }
        } else {
            binding.tilPriceFreelancer.setError(null);
        }

        // Validate Grosir price (Opsional)
        if (!TextUtils.isEmpty(priceGrosirStr)) {
            try {
                double sell = com.example.kasirumkm2.utils.CurrencyHelper.parseDouble(priceGrosirStr);
                if (sell < 0) {
                    binding.tilPriceGrosir.setError("Harga tidak boleh kurang dari 0");
                    hasError = true;
                } else {
                    binding.tilPriceGrosir.setError(null);
                }
            } catch (NumberFormatException e) {
                binding.tilPriceGrosir.setError("Format harga salah");
                hasError = true;
            }
        } else {
            binding.tilPriceGrosir.setError(null);
        }

        if (!TextUtils.isEmpty(costPriceStr)) {
            try {
                double cost = com.example.kasirumkm2.utils.CurrencyHelper.parseDouble(costPriceStr);
                if (cost < 0) {
                    binding.tilCostPrice.setError("Harga modal tidak boleh kurang dari 0");
                    hasError = true;
                } else {
                    binding.tilCostPrice.setError(null);
                }
            } catch (NumberFormatException e) {
                binding.tilCostPrice.setError("Format harga modal salah");
                hasError = true;
            }
        } else {
            binding.tilCostPrice.setError(null);
        }

        if (!TextUtils.isEmpty(stockStr)) {
            try {
                double st = Double.parseDouble(stockStr);
                if (st < 0) {
                    binding.tilStock.setError("Stok tidak boleh kurang dari 0");
                    hasError = true;
                } else {
                    binding.tilStock.setError(null);
                }
            } catch (NumberFormatException e) {
                binding.tilStock.setError("Format stok salah");
                hasError = true;
            }
        } else {
            binding.tilStock.setError(null);
        }

        if (hasError) return;

        submitData(name, code, unit, costPriceStr, stockStr, priceUserStr, priceFreelancerStr, priceGrosirStr);
    }

    private void submitData(String name, String code, String unit, String costPriceStr, String stockStr,
                            String priceUserStr, String priceFreelancerStr, String priceGrosirStr) {
        setLoading(true);

        JsonObject body = new JsonObject();
        body.addProperty("product_name", name);
        body.addProperty("product_code", TextUtils.isEmpty(code) ? null : code);
        body.addProperty("unit", TextUtils.isEmpty(unit) ? null : unit);

        double costPrice = 0;
        if (!TextUtils.isEmpty(costPriceStr)) {
            costPrice = com.example.kasirumkm2.utils.CurrencyHelper.parseDouble(costPriceStr);
        }
        body.addProperty("cost_price", costPrice);

        double stock = 0;
        if (!TextUtils.isEmpty(stockStr)) {
            stock = Double.parseDouble(stockStr);
        }
        body.addProperty("stock", stock);

        // Prices array
        JsonArray pricesArray = new JsonArray();

        // USER (Wajib)
        JsonObject priceUserObj = new JsonObject();
        priceUserObj.addProperty("customer_group_code", "USER");
        priceUserObj.addProperty("selling_price", com.example.kasirumkm2.utils.CurrencyHelper.parseDouble(priceUserStr));
        pricesArray.add(priceUserObj);

        // FREELANCER (Opsional)
        if (!TextUtils.isEmpty(priceFreelancerStr)) {
            JsonObject priceFreelancerObj = new JsonObject();
            priceFreelancerObj.addProperty("customer_group_code", "FREELANCER");
            priceFreelancerObj.addProperty("selling_price", com.example.kasirumkm2.utils.CurrencyHelper.parseDouble(priceFreelancerStr));
            pricesArray.add(priceFreelancerObj);
        }

        // GROSIR (Opsional)
        if (!TextUtils.isEmpty(priceGrosirStr)) {
            JsonObject priceGrosirObj = new JsonObject();
            priceGrosirObj.addProperty("customer_group_code", "GROSIR");
            priceGrosirObj.addProperty("selling_price", com.example.kasirumkm2.utils.CurrencyHelper.parseDouble(priceGrosirStr));
            pricesArray.add(priceGrosirObj);
        }

        body.add("prices", pricesArray);

        apiService.createProductQuick(body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonObject dataObj = response.body().getAsJsonObject("data");
                        Product product = new Gson().fromJson(dataObj, Product.class);

                        Toast.makeText(requireContext(), "Produk berhasil ditambahkan", Toast.LENGTH_SHORT).show();
                        if (listener != null) {
                            listener.onProductAdded(product);
                        }
                        dismiss();
                    } catch (Exception e) {
                        Toast.makeText(requireContext(), "Gagal memproses data produk", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String errorMsg = "Gagal menyimpan produk";
                    try {
                        if (response.errorBody() != null) {
                            JsonObject err = new com.google.gson.JsonParser()
                                    .parse(response.errorBody().string()).getAsJsonObject();
                            if (err.has("message")) {
                                errorMsg = err.get("message").getAsString();
                            }
                            if (err.has("errors")) {
                                JsonObject errors = err.getAsJsonObject("errors");
                                if (errors.has("product_code")) {
                                    binding.tilCode.setError(errors.getAsJsonArray("product_code").get(0).getAsString());
                                }
                                if (errors.has("product_name")) {
                                    binding.tilName.setError(errors.getAsJsonArray("product_name").get(0).getAsString());
                                }
                                if (errors.has("selling_price")) {
                                    binding.tilPriceUser.setError(errors.getAsJsonArray("selling_price").get(0).getAsString());
                                }
                                
                                // Map index-based validation errors from prices array
                                boolean hasFreelancer = !TextUtils.isEmpty(priceFreelancerStr);
                                boolean hasGrosir = !TextUtils.isEmpty(priceGrosirStr);

                                if (errors.has("prices.0.selling_price")) {
                                    binding.tilPriceUser.setError(errors.getAsJsonArray("prices.0.selling_price").get(0).getAsString());
                                }
                                
                                if (hasFreelancer && errors.has("prices.1.selling_price")) {
                                    binding.tilPriceFreelancer.setError(errors.getAsJsonArray("prices.1.selling_price").get(0).getAsString());
                                    if (hasGrosir && errors.has("prices.2.selling_price")) {
                                        binding.tilPriceGrosir.setError(errors.getAsJsonArray("prices.2.selling_price").get(0).getAsString());
                                    }
                                } else if (hasGrosir && errors.has("prices.1.selling_price")) {
                                    binding.tilPriceGrosir.setError(errors.getAsJsonArray("prices.1.selling_price").get(0).getAsString());
                                }

                                errorMsg = "Harap periksa kembali input Anda";
                            }
                        }
                    } catch (Exception e) {}
                    Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                setLoading(false);
                Toast.makeText(requireContext(), "Tidak ada koneksi internet", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnSave.setEnabled(!loading);
        binding.btnCancel.setEnabled(!loading);
        binding.btnScan.setEnabled(!loading);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
