package com.example.kasirumkm2.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.kasirumkm2.R;
import com.example.kasirumkm2.api.ApiClient;
import com.example.kasirumkm2.api.ApiService;
import com.example.kasirumkm2.databinding.FragmentSettingsBinding;
import com.example.kasirumkm2.session.SessionManager;
import com.example.kasirumkm2.utils.CurrencyHelper;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private SessionManager sessionManager;
    private ApiService apiService;
    private com.example.kasirumkm2.printer.PrinterManager printerManager;
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 101;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());
        apiService = ApiClient.getApiService(requireContext());
        printerManager = new com.example.kasirumkm2.printer.PrinterManager(requireContext());

        displayProfileSummary();
        updatePrinterStatusDisplay();
        setupListeners();
    }

    private void displayProfileSummary() {
        String name = sessionManager.getUserName();
        String email = sessionManager.getUserEmail();
        String company = sessionManager.getCompanyName();

        binding.tvProfileName.setText(name);
        if (company != null && !company.isEmpty()) {
            binding.tvProfileEmail.setText(email + " · " + company);
        } else {
            binding.tvProfileEmail.setText(email);
        }

        // Avatar Initials
        if (name != null && !name.isEmpty()) {
            String[] parts = name.trim().split("\\s+");
            String initials = parts.length >= 2
                    ? ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase()
                    : ("" + parts[0].charAt(0)).toUpperCase();
            binding.tvAvatarInitials.setText(initials);
        }
    }

    private void setupListeners() {
        // Toggle Company & User Management visibility for SUPER_ADMIN
        String role = sessionManager.getUserRole();
        if ("SUPER_ADMIN".equalsIgnoreCase(role)) {
            binding.layoutOptionCompany.setVisibility(View.VISIBLE);
            binding.dividerCompany.setVisibility(View.VISIBLE);
            binding.layoutOptionUser.setVisibility(View.VISIBLE);
            binding.dividerUser.setVisibility(View.VISIBLE);
        } else {
            binding.layoutOptionCompany.setVisibility(View.GONE);
            binding.dividerCompany.setVisibility(View.GONE);
            binding.layoutOptionUser.setVisibility(View.GONE);
            binding.dividerUser.setVisibility(View.GONE);
        }

        binding.layoutOptionProfile.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), ProfileActivity.class));
        });

        binding.layoutOptionPrinter.setOnClickListener(v -> {
            if (checkBluetoothPermissions()) {
                showPrinterPickerDialog();
            } else {
                requestBluetoothPermissions();
            }
        });

        binding.layoutOptionStock.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), StockActivity.class));
        });

        binding.layoutOptionCompany.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), CompanyActivity.class));
        });

        binding.layoutOptionUser.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), UserActivity.class));
        });

        binding.layoutOptionInfo.setOnClickListener(v -> showAppInfo());

        binding.btnLogout.setOnClickListener(v -> confirmLogout());
    }

    private void showAppInfo() {
        String info = "Kasir UMKM Mobile Client\nVersi 1.0.0\n\nDidesain khusus untuk efisiensi transaksi retail & grosir UMKM.\n\nDeveloper: @Firlli.an\n";
        String link = "https://firlli.vercel.app/";
        String full = info + link;

        android.text.SpannableString spannable = new android.text.SpannableString(full);
        int start = full.indexOf(link);
        int end = start + link.length();
        spannable.setSpan(new android.text.style.ClickableSpan() {
            @Override
            public void onClick(@androidx.annotation.NonNull android.view.View widget) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(link));
                startActivity(browserIntent);
            }
        }, start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Informasi Aplikasi")
                .setMessage(spannable)
                .setPositiveButton("OK", null)
                .show();

        // Make the link clickable
        android.widget.TextView msgView = dialog.findViewById(android.R.id.message);
        if (msgView != null) {
            msgView.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
        }
    }

    private boolean checkBluetoothPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            return androidx.core.content.ContextCompat.checkSelfPermission(requireContext(),
                    android.Manifest.permission.BLUETOOTH_CONNECT) == android.content.pm.PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void requestBluetoothPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            requestPermissions(new String[]{
                    android.Manifest.permission.BLUETOOTH_CONNECT,
                    android.Manifest.permission.BLUETOOTH_SCAN
            }, REQUEST_BLUETOOTH_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                showPrinterPickerDialog();
            } else {
                CurrencyHelper.showToast(requireContext(), "Izin Bluetooth ditolak. Gagal menyambungkan ke printer.");
            }
        }
    }

    private void updatePrinterStatusDisplay() {
        String printerName = sessionManager.getPrinterName();
        String printerAddr = sessionManager.getPrinterAddress();
        if (printerName == null || printerName.isEmpty() || printerAddr == null || printerAddr.isEmpty()) {
            binding.tvSelectedPrinter.setText("Belum diatur");
        } else {
            binding.tvSelectedPrinter.setText(printerName);
        }
    }

    @android.annotation.SuppressLint("MissingPermission")
    private void showPrinterPickerDialog() {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext());
        View sheetView = getLayoutInflater().inflate(R.layout.layout_printer_picker, null);
        dialog.setContentView(sheetView);

        // Views
        android.widget.ImageButton btnClose = sheetView.findViewById(R.id.btnClose);
        android.widget.LinearLayout layoutBluetoothOff = sheetView.findViewById(R.id.layoutBluetoothOff);
        android.widget.LinearLayout layoutPermissionMissing = sheetView.findViewById(R.id.layoutPermissionMissing);
        android.widget.LinearLayout layoutDeviceContainer = sheetView.findViewById(R.id.layoutDeviceContainer);
        android.widget.ListView lvDevices = sheetView.findViewById(R.id.lvDevices);
        android.widget.TextView tvEmptyDevices = sheetView.findViewById(R.id.tvEmptyDevices);
        com.google.android.material.card.MaterialCardView cardSelectedPrinter = sheetView.findViewById(R.id.cardSelectedPrinter);
        android.widget.TextView tvActivePrinterName = sheetView.findViewById(R.id.tvActivePrinterName);
        android.widget.TextView tvActivePrinterAddress = sheetView.findViewById(R.id.tvActivePrinterAddress);
        com.google.android.material.button.MaterialButton btnDisconnect = sheetView.findViewById(R.id.btnDisconnect);
        com.google.android.material.button.MaterialButton btnTestPrint = sheetView.findViewById(R.id.btnTestPrint);
        com.google.android.material.button.MaterialButton btnEnableBluetooth = sheetView.findViewById(R.id.btnEnableBluetooth);
        com.google.android.material.button.MaterialButton btnRequestPermission = sheetView.findViewById(R.id.btnRequestPermission);
        android.widget.ProgressBar pickerProgressBar = sheetView.findViewById(R.id.pickerProgressBar);

        btnClose.setOnClickListener(v -> dialog.dismiss());

        // Refresh State Runner
        java.lang.Runnable refreshState = new java.lang.Runnable() {
            @Override
            public void run() {
                // 1. Check Permissions
                if (!checkBluetoothPermissions()) {
                    layoutPermissionMissing.setVisibility(View.VISIBLE);
                    layoutBluetoothOff.setVisibility(View.GONE);
                    layoutDeviceContainer.setVisibility(View.GONE);
                    return;
                }
                layoutPermissionMissing.setVisibility(View.GONE);

                // 2. Check Bluetooth On/Off
                android.bluetooth.BluetoothAdapter adapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter();
                if (adapter == null || !adapter.isEnabled()) {
                    layoutBluetoothOff.setVisibility(View.VISIBLE);
                    layoutDeviceContainer.setVisibility(View.GONE);
                    return;
                }
                layoutBluetoothOff.setVisibility(View.GONE);
                layoutDeviceContainer.setVisibility(View.VISIBLE);

                // 3. Load paired devices
                java.util.List<android.bluetooth.BluetoothDevice> devices = printerManager.getPrinterHelper().getPairedDevices();
                if (devices == null || devices.isEmpty()) {
                    tvEmptyDevices.setVisibility(View.VISIBLE);
                    lvDevices.setVisibility(View.GONE);
                } else {
                    tvEmptyDevices.setVisibility(View.GONE);
                    lvDevices.setVisibility(View.VISIBLE);

                    android.widget.ArrayAdapter<android.bluetooth.BluetoothDevice> arrayAdapter = new android.widget.ArrayAdapter<android.bluetooth.BluetoothDevice>(requireContext(), R.layout.item_printer_device, devices) {
                        @NonNull
                        @Override
                        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                            if (convertView == null) {
                                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_printer_device, parent, false);
                            }
                            android.bluetooth.BluetoothDevice device = getItem(position);
                            
                            android.widget.TextView tvName = convertView.findViewById(R.id.tvDeviceName);
                            android.widget.TextView tvAddress = convertView.findViewById(R.id.tvDeviceAddress);
                            android.widget.TextView tvSelect = convertView.findViewById(R.id.tvSelectIndicator);
                            
                            String name = device.getName();
                            String address = device.getAddress();
                            
                            tvName.setText(name != null ? name : "Device Tanpa Nama");
                            tvAddress.setText(address);
                            
                            String savedAddress = sessionManager.getPrinterAddress();
                            if (address.equals(savedAddress)) {
                                tvSelect.setText("Terpilih");
                                tvSelect.setTextColor(getContext().getColor(R.color.success_green));
                                tvSelect.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFF0FDF4));
                            } else {
                                tvSelect.setText("Pilih");
                                tvSelect.setTextColor(getContext().getColor(R.color.primary));
                                tvSelect.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFEFF6FF));
                            }
                            
                            return convertView;
                          }
                      };
                      lvDevices.setAdapter(arrayAdapter);
                      
                      lvDevices.setOnItemClickListener((parent, view, position, id) -> {
                          android.bluetooth.BluetoothDevice selectedDevice = devices.get(position);
                          sessionManager.saveSelectedPrinter(selectedDevice.getAddress(), selectedDevice.getName());
                          updatePrinterStatusDisplay();
                          run(); // Refresh views and state
                      });
                  }

                  // 4. Active Printer Info Card
                  String activeName = sessionManager.getPrinterName();
                  String activeAddr = sessionManager.getPrinterAddress();
                  if (activeAddr != null && !activeAddr.isEmpty()) {
                      cardSelectedPrinter.setVisibility(View.VISIBLE);
                      tvActivePrinterName.setText(activeName != null ? activeName : "Printer");
                      tvActivePrinterAddress.setText(activeAddr);
                      btnDisconnect.setVisibility(View.VISIBLE);
                      btnTestPrint.setVisibility(View.VISIBLE);
                  } else {
                      cardSelectedPrinter.setVisibility(View.GONE);
                      btnDisconnect.setVisibility(View.GONE);
                      btnTestPrint.setVisibility(View.GONE);
                  }
              }
          };

          // OnClick Listeners for Empty State Actions
          btnRequestPermission.setOnClickListener(v -> {
              requestBluetoothPermissions();
              dialog.dismiss();
          });

          btnEnableBluetooth.setOnClickListener(v -> {
              startActivity(new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS));
              dialog.dismiss();
          });

          btnDisconnect.setOnClickListener(v -> {
              sessionManager.clearSelectedPrinter();
              updatePrinterStatusDisplay();
              refreshState.run();
              CurrencyHelper.showToast(requireContext(), "Printer berhasil diputus");
          });

          btnTestPrint.setOnClickListener(v -> {
              String activeAddr = sessionManager.getPrinterAddress();
              if (activeAddr == null || activeAddr.isEmpty()) return;

              pickerProgressBar.setVisibility(View.VISIBLE);
              btnTestPrint.setEnabled(false);
              btnDisconnect.setEnabled(false);
              lvDevices.setEnabled(false);

              printerManager.printTestReceiptAsync(activeAddr, new com.example.kasirumkm2.printer.PrinterManager.PrintCallback() {
                  @Override
                  public void onSuccess() {
                      if (isAdded()) {
                          pickerProgressBar.setVisibility(View.GONE);
                          btnTestPrint.setEnabled(true);
                          btnDisconnect.setEnabled(true);
                          lvDevices.setEnabled(true);
                          CurrencyHelper.showToast(requireContext(), "Uji coba cetak berhasil!");
                      }
                  }

                  @Override
                  public void onFailure(String errorMessage) {
                      if (isAdded()) {
                          pickerProgressBar.setVisibility(View.GONE);
                          btnTestPrint.setEnabled(true);
                          btnDisconnect.setEnabled(true);
                          lvDevices.setEnabled(true);
                          new AlertDialog.Builder(requireContext())
                                  .setTitle("Gagal Cetak")
                                  .setMessage(errorMessage + "\n\nPastikan:\n1. Printer dalam keadaan menyala\n2. Bluetooth HP aktif\n3. Printer sudah terpasang (paired)")
                                  .setPositiveButton("OK", null)
                                  .show();
                      }
                  }
              });
          });

          // Initially refresh
          refreshState.run();
          dialog.show();
      }

    private void confirmLogout() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Keluar Aplikasi")
                .setMessage("Apakah Anda yakin ingin keluar dari aplikasi?")
                .setPositiveButton("Keluar", (dialog, which) -> executeLogout())
                .setNegativeButton("Batal", null)
                .show();
    }

    private void executeLogout() {
        setLoading(true);

        apiService.logout().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                setLoading(false);
                logoutLocal();
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                setLoading(false);
                // Even if network fails, force local logout to prevent lock-ins
                logoutLocal();
            }
        });
    }

    private void logoutLocal() {
        sessionManager.clearSession();
        ApiClient.resetClient();

        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnLogout.setEnabled(!loading);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
