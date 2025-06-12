package com.example.acremetergps;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.util.Log;

import android.Manifest;
import android.app.Activity;
import android.widget.Toast;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class BluetoothManager {

    private static final String TAG = "BluetoothManager";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private static BluetoothManager instance;
    private TripDataCallback tripDataCallback;

    private BluetoothSocket socket;
    private InputStream inputStream;
    private final DatabaseHelper dbHelper;
    private final Handler handler;
    private DataListener listener;
    private BluetoothDevice connectedDevice;
    public interface DataListener {
        void onDataReceived(String data);
    }

    private BluetoothManager(DatabaseHelper dbHelper, Handler handler, DataListener listener) {
        this.dbHelper = dbHelper;
        this.handler = handler;
        this.listener = listener;
    }

    // Singleton access
    public static BluetoothManager getInstance(DatabaseHelper dbHelper, Handler handler, DataListener listener) {
        if (instance == null) {
            instance = new BluetoothManager(dbHelper, handler, listener);
        }
        return instance;
    }

    public void setSocket(BluetoothSocket socket) {
        this.socket = socket;
    }

    public void sendCommand(String command) {
        if (socket != null && socket.isConnected()) {
            try {
                socket.getOutputStream().write(command.getBytes());
            } catch (IOException e) {
                Log.e("BluetoothSend", "Send failed", e);
            }
        }
    }


    // Optional: if already initialized and you only want to access the socket later
    public static BluetoothManager getInstance() {
        return instance;
    }

    public void connectToDevice(BluetoothDevice device, Context context) {
        new Thread(() -> {
            try {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "Bluetooth permission not granted");
                    return;
                }

                socket = device.createRfcommSocketToServiceRecord(MY_UUID);
                socket.connect();
                connectedDevice = device;
                inputStream = socket.getInputStream();
                Log.d(TAG, "Connected to device: " + device.getName());
                listenForData();
            } catch (IOException e) {
                Log.e(TAG, "Connection failed", e);
            }
        }).start();
    }

    //
    public void connectingToDevice(BluetoothDevice device, Context context) {
        new Thread(() -> {
            try {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "Bluetooth permission not granted");
                    return;
                }

                socket = device.createRfcommSocketToServiceRecord(MY_UUID);
                socket.connect();

                inputStream = socket.getInputStream();
                Log.d(TAG, "Connected to device: " + device.getName());

                if (context instanceof Activity) {
                    ((Activity) context).runOnUiThread(() ->
                            Toast.makeText(context, "Connected to " + device.getName(), Toast.LENGTH_SHORT).show()
                    );
                }

                listenForData();
            } catch (IOException e) {
                Log.e(TAG, "Connection failed", e);
                if (context instanceof Activity) {
                    ((Activity) context).runOnUiThread(() ->
                            Toast.makeText(context, "Connection failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
                }
            }
        }).start();
    }

    public BluetoothSocket getSocket() {
        return socket;
    }


    private void listenForData() {
        new Thread(() -> {
            byte[] buffer = new byte[1024];
            int bytes;
            while (!Thread.currentThread().isInterrupted() && socket != null && socket.isConnected()) {
                try {
                    if (inputStream.available() > 0) {
                        bytes = inputStream.read(buffer);
                        String receivedData = new String(buffer, 0, bytes);
                        Log.d(TAG, "Data received in deviceconfig: " + receivedData);

                        handler.post(() -> {
                            if (listener != null) {
                                Log.d(TAG, "Calling listener.onDataReceived()");
                                listener.onDataReceived(receivedData);
                                Log.d(TAG, "Calling listener.onDataReceived(): " + receivedData);
                                Log.d(TAG, "Listener is: " + (listener == null ? "NULL" : "NON-NULL"));
                                Log.d("BluetoothManager", "Data received: " + receivedData);
                            } else {
                                Log.w(TAG, "No listener set to receive data");
                            }
                            if (tripDataCallback != null) {
                                Log.d("TripDataDebug", "Raw data received: " + receivedData);
                                tripDataCallback.onTripDataReceived(receivedData);
                            }
                        });
                        dbHelper.parseAndInsertLog("user123", receivedData);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error reading from input stream device config"+ e.getMessage(), e);
                    break;
                }
            }
        }).start();
    }

    public interface TripDataCallback {
        void onTripDataReceived(String data);
    }

    public void setTripDataListener(TripDataCallback callback) {
        this.tripDataCallback = callback;
    }

    public void requestTripData(TripDataCallback callback) {
        this.tripDataCallback = callback;
        sendCommand("0x55aa");
        startListeningForResponse();
    }


    private void startListeningForResponse() {
        new Thread(() -> {
            byte[] buffer = new byte[1024];
            int bytes;

            try {
                while ((bytes = inputStream.read(buffer)) != -1) {
                    String response = new String(buffer, 0, bytes, StandardCharsets.UTF_8);

                    if (tripDataCallback != null) {
                        tripDataCallback.onTripDataReceived(response);
                    }
                    // Optionally break after full data received depending on your protocol
                }
            } catch (IOException e) {
                if (tripDataCallback != null) {
                    e.getMessage();
                }
            }
        }).start();
    }

    public String getConnectedDeviceMac() {
        if (connectedDevice != null) {
            return connectedDevice.getAddress();
        }
        return null;
    }

    public void setListener(DataListener listener) {
        this.listener = listener;
        Log.d(TAG, "Listener set to: " + listener);
        Log.d(TAG, "BluetoothManager setListener() called");
    }

    public void disconnect() {
        if (socket != null) {
            try {
                if (socket.isConnected()) {
                    socket.close();
                    Log.d(TAG, "Bluetooth socket closed successfully.");
                }
            } catch (IOException e) {
                Log.e(TAG, "Error closing socket", e);
            } finally {
                socket = null;
                inputStream = null;
            }
        }
    }

    public void sendData(String data) {
        if (socket != null && socket.isConnected()) {
            new Thread(() -> {
                try {
                    socket.getOutputStream().write(data.getBytes());
                    Log.d(TAG, "Sent data: " + data);
                } catch (IOException e) {
                    Log.e(TAG, "Error sending data", e);
                }
            }).start();
        } else {
            Log.e(TAG, "Socket is null or not connected");
        }
    }


    public boolean isConnected() {
        return socket != null && socket.isConnected();
    }

}