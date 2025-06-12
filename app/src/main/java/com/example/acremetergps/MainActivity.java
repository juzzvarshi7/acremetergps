package com.example.acremetergps;

import static android.content.ContentValues.TAG;



import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;


import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;


public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;
    private boolean isBluetoothConnected = false;
    private String[] latestParts = null;
    private TextView connectedDeviceText;
    private LineData lineData;
    private EditText editTextDeviceId;
    private LineDataSet lineDataSet;
    private LineChart lineChart;
    private String macAddress;
    private BluetoothSocket socket;

    private BluetoothManager bluetoothManager;

    @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        RetrofitClient.init(this);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Button buttonScanBluetooth = findViewById(R.id.buttonScanBluetooth);
        Button buttonConnectDevice = findViewById(R.id.buttonConnectDevice);
        editTextDeviceId = findViewById(R.id.editTextDeviceId);

        Button buttonGetRawData = findViewById(R.id.buttonGetRawData);
        TextView textViewRawData = findViewById(R.id.textViewRawData);

        buttonGetRawData.setOnClickListener(v -> {
            if (bluetoothManager != null && bluetoothManager.isConnected()) {
                Log.d("Dashboard", "Bluetooth connected, sending RAWDATA command");
                Toast.makeText(this, "Sending RAWDATA command", Toast.LENGTH_SHORT).show();

                // Set the listener to receive data
                bluetoothManager.setListener(data -> {
                    runOnUiThread(() -> {
                        textViewRawData.setText(data);
                        Log.d("Dashboard", "Data received in rawdata: " + data);
                        Toast.makeText(this, "Data received in rawdata: " + data, Toast.LENGTH_SHORT).show();
                    });
                });

                // Send RAWDATA command through Bluetooth socket
                BluetoothSocket socket = bluetoothManager.getSocket();
                if (socket != null && socket.isConnected()) {
                    try {
                        String command = "RAWDATA";
                        socket.getOutputStream().write(command.getBytes());
                        Log.d("BluetoothSend", "Sent command: " + command);
                    } catch (IOException e) {
                        Log.e("BluetoothSend", "Send failed", e);
                    }
                } else {
                    Toast.makeText(this, "Bluetooth socket not connected", Toast.LENGTH_SHORT).show();
                }

            } else {
                Toast.makeText(this, "Bluetooth not connected", Toast.LENGTH_SHORT).show();
                Log.d("Dashboard", "Bluetooth not connected");
            }
        });

        Button btnMapView = findViewById(R.id.buttonMap); // Make sure you have this in your layout
        btnMapView.setOnClickListener(v -> {
            String truckId = latestParts[0];
            String date = latestParts[4];

            Intent intent = new Intent(MainActivity.this, MapActivity.class);
            intent.putExtra("truckId", truckId);
            intent.putExtra("date", date);
            startActivity(intent);
        });


        lineChart = findViewById(R.id.lineChart);
        TextView textViewData = findViewById(R.id.textViewData);
        Button buttonDisconnect = findViewById(R.id.buttonDisconnect);
        connectedDeviceText = findViewById(R.id.connectedDeviceText);
        Button btnTripData = findViewById(R.id.Trip_Data);
        Button btnOpenGpsTracking = findViewById(R.id.btnOpenGpsTracking);
        lineChart.setVisibility(View.GONE);

        buttonDisconnect.setOnClickListener(v -> {
            if (isBluetoothConnected && bluetoothManager != null) {
                bluetoothManager.disconnect();
                isBluetoothConnected = false;
                connectedDeviceText.setText("Disconnected");
                Toast.makeText(this, "Device disconnected", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "No device is currently connected", Toast.LENGTH_SHORT).show();
            }
        });

        bluetoothManager = BluetoothManager.getInstance(
                new DatabaseHelper(this),
                new Handler(getMainLooper()),
                data -> {
                    Log.d("BluetoothData", "Raw data: " + data);
                    if (data != null && !data.isEmpty()) {
                        String[] parts = data.trim().split("\"\"");
                        latestParts = parts;
                        if (parts.length == 11) {
                            runOnUiThread(() -> {
                                String formatted = "Device ID: " + parts[0] +
                                        "\nSL No: " + parts[1] +
                                        "\nPhone: " + parts[2] +
                                        "\nTime: " + parts[3] + ":" + parts[4] +
                                        "\nDate: " + parts[5] + "/" + parts[6] + "/" + parts[7] +
                                        "\nPressure Range: " + parts[8] +
                                        "\nFlow Range: " + parts[9] +
                                        "\nSMS Alert Time: " + parts[10];

                                textViewData.setText(formatted);

                                try {
                                    float flowValue = Float.parseFloat(parts[9]);
                                    updateChart(flowValue);
                                    lineChart.setVisibility(View.VISIBLE);
                                } catch (NumberFormatException e) {
                                    Log.e("Chart", "Invalid float: " + parts[9]);
                                    lineChart.setVisibility(View.GONE);
                                }

                            });
                        } else {
                            Log.e("BluetoothParse", "Invalid data format. Expected 11, got " + parts.length);
                            runOnUiThread(() -> lineChart.setVisibility(View.GONE));
                        }
                    } else {
                        runOnUiThread(() -> lineChart.setVisibility(View.GONE));
                    }
                }
        );

        buttonScanBluetooth.setOnClickListener(v -> {
            if (bluetoothAdapter == null) {
                Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!bluetoothAdapter.isEnabled()) {
                Toast.makeText(this, "Please enable Bluetooth first", Toast.LENGTH_SHORT).show();
                return;
            }
            bluetoothScanLauncher.launch(new Intent(this, BluetoothScanActivity.class));
        });

        buttonConnectDevice.setOnClickListener(v -> {
            String deviceId = editTextDeviceId.getText().toString();
            if (!deviceId.isEmpty()) {
                BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceId);
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Bluetooth permission denied", Toast.LENGTH_SHORT).show();
                    return;
                }
                connectToDevice(device);
            } else {
                Toast.makeText(this, "Please enter a Device ID", Toast.LENGTH_SHORT).show();
            }
        });


        findViewById(R.id.buttonOpenDeviceConfig).setOnClickListener(v -> {
            if (latestParts != null && latestParts.length == 11) {
                Intent intent = new Intent(this, DeviceConfigurationActivity.class);
                intent.putExtra("slno", latestParts[1]);
                intent.putExtra("site", latestParts[0]);
                intent.putExtra("datetime", latestParts[5] + "/" + latestParts[6] + "/" + latestParts[7] + " " + latestParts[3] + ":" + latestParts[4]);
                intent.putExtra("mobilenumber", latestParts[2]);
                intent.putExtra("pressurerange", latestParts[8]);
                intent.putExtra("flowrange", latestParts[9]);
                intent.putExtra("tampertype", "default");
                intent.putExtra("smsalttime", latestParts[10]);
                intent.putExtra("macAddress", macAddress);
                startActivity(intent);
                Toast.makeText(this, "MAC sent to config: " + macAddress, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "No data received yet", Toast.LENGTH_SHORT).show();
            }
        });

        btnOpenGpsTracking.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, GpsTrackingActivity.class);
            intent.putExtra("DEVICE_MAC", macAddress);
            startActivity(intent);
            Log.d("Dashboard", "Starting GPS Activity");
        });


        btnTripData.setOnClickListener(v -> {
            if (bluetoothManager != null && bluetoothManager.isConnected()) {
                Log.d("Dashboard", "Bluetooth connected, sending RAWDATA command");

                BluetoothSocket socket = bluetoothManager.getSocket();
                if (socket != null && socket.isConnected()) {
                    // Set the listener before sending command
                    bluetoothManager.setListener(data -> {
                        runOnUiThread(() -> {
                            Log.d(TAG, "Data received in rawdata: " + data);
                            Intent intent = new Intent(this, TruckDashboardActivity.class);
                            intent.putExtra("truck_id", getDeviceId());
                            intent.putExtra("DEVICE_MAC", macAddress);
                            intent.putExtra("latestParts", latestParts);
                            intent.putExtra("rawdata", data);
                            startActivity(intent);
                        });
                    });

                    try {
                        String command = "RAWDATA";
                        socket.getOutputStream().write(command.getBytes(StandardCharsets.UTF_8));
                        Log.d("BluetoothSend", "Sent command: " + command);
                    } catch (IOException e) {
                        Log.e("BluetoothSend", "Send failed", e);
                    }
                } else {
                    Toast.makeText(this, "Bluetooth socket not connected", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Bluetooth not connected", Toast.LENGTH_SHORT).show();
                Log.e("Dashboard", "Attempted to send RAWDATA but Bluetooth not connected.");
            }
        });
    }

    private void updateChart(float value) {
        if (lineDataSet == null) {
            lineDataSet = new LineDataSet(new ArrayList<>(), "Flow Rate");
            lineDataSet.setColor(Color.BLUE);
            lineDataSet.setValueTextColor(Color.BLACK);
            lineData = new LineData(lineDataSet);
            lineChart.setData(lineData);
        }

        int count = lineDataSet.getEntryCount();
        lineDataSet.addEntry(new Entry(count, value));
        lineData.notifyDataChanged();
        lineChart.notifyDataSetChanged();
        lineChart.invalidate();
    }
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private final ActivityResultLauncher<Intent> bluetoothScanLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    macAddress = result.getData().getStringExtra("DEVICE_MAC");
                    Toast.makeText(this, "MAC: " + macAddress, Toast.LENGTH_SHORT).show();
                    if (macAddress != null) {
                        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(macAddress);
                        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }
                        try {
                            connectToDevice(device);
                        } catch (SecurityException e) {
                            Log.e("YourTag", "Bluetooth permission denied", e);
                        }
                        saveUsedDevice(device.getAddress());
                    }
                }
            });

    @SuppressLint("SetTextI18n")
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void connectToDevice(BluetoothDevice device) {
        if (device != null) {
            connectedDeviceText.setText("Connected to: " + device.getName() + " (" + device.getAddress() + ")");
            TextView textViewStatus = findViewById(R.id.textViewStatus);
            textViewStatus.setText("Status: Connected");
            Toast.makeText(this, "Connected to " + device.getName(), Toast.LENGTH_SHORT).show();
            isBluetoothConnected = true;

            bluetoothManager.connectToDevice(device, this);
        }
    }
    private void saveUsedDevice(String macAddress) {
        SharedPreferences prefs = getSharedPreferences("connected_devices", MODE_PRIVATE);
        Set<String> connected = prefs.getStringSet("mac_list", new HashSet<>());
        Set<String> updated = new HashSet<>(connected);
        updated.add(macAddress);
        prefs.edit().putStringSet("mac_list", updated).apply();
    }
}