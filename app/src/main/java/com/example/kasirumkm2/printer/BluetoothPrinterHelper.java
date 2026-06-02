package com.example.kasirumkm2.printer;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BluetoothPrinterHelper {

    // Standard SPP UUID for Bluetooth Serial board (thermal printers)
    private static final UUID PRINTER_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket socket;
    private OutputStream outputStream;

    public BluetoothPrinterHelper(Context context) {
        this.context = context;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    /**
     * Check if bluetooth is supported on this device
     */
    public boolean isBluetoothSupported() {
        return bluetoothAdapter != null;
    }

    /**
     * Check if bluetooth is enabled
     */
    public boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    /**
     * Scan and get all paired bluetooth devices
     */
    @SuppressLint("MissingPermission")
    public List<BluetoothDevice> getPairedDevices() {
        List<BluetoothDevice> devices = new ArrayList<>();
        if (bluetoothAdapter == null) return devices;

        // Check permission if android 12+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return devices;
            }
        }

        Set<BluetoothDevice> paired = bluetoothAdapter.getBondedDevices();
        if (paired != null) {
            devices.addAll(paired);
        }
        return devices;
    }

    /**
     * Connect to a specific bluetooth device
     */
    @SuppressLint("MissingPermission")
    public boolean connect(BluetoothDevice device) {
        if (device == null) return false;

        // Check permission if android 12+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }

        try {
            disconnect(); // Reset previous socket if any
            socket = device.createRfcommSocketToServiceRecord(PRINTER_UUID);
            socket.connect();
            outputStream = socket.getOutputStream();
            return true;
        } catch (IOException e) {
            disconnect();
            return false;
        }
    }

    /**
     * Check if currently connected
     */
    public boolean isConnected() {
        return socket != null && socket.isConnected() && outputStream != null;
    }

    /**
     * Write raw bytes/commands to printer
     */
    public boolean write(byte[] bytes) {
        if (!isConnected()) return false;
        try {
            outputStream.write(bytes);
            outputStream.flush();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Get a BluetoothDevice by its MAC address
     */
    public BluetoothDevice getDeviceByAddress(String address) {
        if (bluetoothAdapter == null || address == null || address.isEmpty()) return null;
        try {
            return bluetoothAdapter.getRemoteDevice(address);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Disconnect socket safely
     */
    public void disconnect() {
        try {
            if (outputStream != null) {
                outputStream.close();
                outputStream = null;
            }
            if (socket != null) {
                socket.close();
                socket = null;
            }
        } catch (IOException e) {
            // ignore
        }
    }
}
