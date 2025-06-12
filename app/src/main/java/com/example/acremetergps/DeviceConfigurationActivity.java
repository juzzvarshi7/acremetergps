package com.example.acremetergps;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.RequiresPermission;

import com.example.acremetergps.R;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

public class DeviceConfigurationActivity extends Activity {

    private EditText etSlNo, etSite, etDateTime, etMobileNumber,
            etPressureRange, etFlowRange, etTamperType, etSmsAltTime;

    private Button btnEdit, btnSave;

    private LogModel currentLog;
    private DatabaseHelper dbHelper;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private BluetoothDevice bluetoothDevice;
    private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // Standard SPP UUID


    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_configuration);

        etSlNo = findViewById(R.id.etSlNo);
        etSite = findViewById(R.id.etSite);
        etDateTime = findViewById(R.id.etDateTime);
        etMobileNumber = findViewById(R.id.etMobileNumber);
        etPressureRange = findViewById(R.id.etPressureRange);
        etFlowRange = findViewById(R.id.etFlowRange);
        etTamperType = findViewById(R.id.etTamperType);
        etSmsAltTime = findViewById(R.id.etSmsAltTime);

        Intent intent = getIntent();
        Log.d("DEBUG", "EditText found: " + findViewById(R.id.etSlNo));
        etSlNo.setText(intent.getStringExtra("slno"));
        etSite.setText(intent.getStringExtra("site"));
        etDateTime.setText(intent.getStringExtra("datetime"));
        etMobileNumber.setText(intent.getStringExtra("mobilenumber"));
        etPressureRange.setText(intent.getStringExtra("pressurerange"));
        etFlowRange.setText(intent.getStringExtra("flowrange"));
        etTamperType.setText(intent.getStringExtra("tampertype"));
        etSmsAltTime.setText(intent.getStringExtra("smsalttime"));
        String macAddress = intent.getStringExtra("macAddress");

        Log.d("DeviceConfig", "Received MAC Address: " + macAddress);

        if (macAddress == null || macAddress.isEmpty()) {
            Toast.makeText(this, "Invalid MAC address, cannot connect.", Toast.LENGTH_LONG).show();
            finish(); // Close the activity gracefully instead of crashing
            return;
        }

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothDevice = bluetoothAdapter.getRemoteDevice(macAddress);

        new Thread(() -> {
            try {
                bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(MY_UUID);
                bluetoothSocket.connect();
                runOnUiThread(() -> Toast.makeText(this, "Connected to device", Toast.LENGTH_SHORT).show());
                BluetoothManager.getInstance().setSocket(bluetoothSocket);
            } catch (IOException e) {
                Log.e("BluetoothConnect", "Failed to connect", e);
            }
        }).start();


        dbHelper = new DatabaseHelper(this);

        btnEdit = findViewById(R.id.btnEditConfig);
        btnSave = findViewById(R.id.btnSaveConfig);

        disableAllFields();

        btnEdit.setOnClickListener(v -> enableAllFields());

        btnSave.setOnClickListener(v -> {
            try {
                saveDataToLocalDB();
                sendDataToDevice();
                disableAllFields();
                Toast.makeText(this, "Configuration saved successfully", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                String errorMsg = "Error saving config: " + e.getMessage();
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                Log.e("ConfigSave", errorMsg, e);  // Logs message and stacktrace
            }
        });

        getDeviceData();
    }

    private void getDeviceData() {
        String rawData = getIntent().getStringExtra("raw_data_from_device");
        if (rawData == null || rawData.isEmpty()) {
            currentLog = loadLastSavedLog();
        } else {
            currentLog = parseDeviceData(rawData);
        }
        if (currentLog != null) {
            etSlNo.setText(currentLog.getUserId());
            etSite.setText(currentLog.getChannelId());
            etDateTime.setText(currentLog.getTimestamp());
            etMobileNumber.setText(currentLog.getTypeId());
            etPressureRange.setText(currentLog.getAnalogV1());
            etFlowRange.setText(currentLog.getAnalogV2());
            etTamperType.setText(currentLog.getAnalogV3());
            etSmsAltTime.setText(currentLog.getAnalogV4());
        } else {
            Toast.makeText(this, "Failed to load device data", Toast.LENGTH_LONG).show();
        }
    }
    private LogModel loadLastSavedLog() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        LogModel log = null;

        String query = "SELECT * FROM device_config_detailed ORDER BY rowid DESC LIMIT 1";
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            log = new LogModel(
                    "", // timestamp — not in this table, leave blank or set manually
                    cursor.getString(cursor.getColumnIndexOrThrow("slno")),
                    "", // sensorData — not used here
                    "", // flowRate — not used
                    "", // logTime — not used
                    cursor.getString(cursor.getColumnIndexOrThrow("site")),
                    cursor.getString(cursor.getColumnIndexOrThrow("mobilenumber")),
                    cursor.getString(cursor.getColumnIndexOrThrow("pressurerange")),
                    cursor.getString(cursor.getColumnIndexOrThrow("flowrange")),
                    cursor.getString(cursor.getColumnIndexOrThrow("tampertype")),
                    cursor.getString(cursor.getColumnIndexOrThrow("smsalttime"))
            );
        }

        cursor.close();
        db.close();
        return log;
    }


    private LogModel parseDeviceData(String rawData) {
        if (rawData == null || rawData.isEmpty()) return null;

        String[] parts = rawData.split(","); // Adjust delimiter as needed
        if (parts.length < 11) return null;

        return new LogModel(
                parts[0],  // timestamp
                parts[1],  // userId
                parts[2],  // sensorData
                parts[3],  // flowRate
                parts[4],  // logTime
                parts[5],  // channelId
                parts[6],  // typeId
                parts[7],  // analogV1
                parts[8],  // analogV2
                parts[9],  // analogV3
                parts[10]  // analogV4
        );
    }

    private void enableAllFields() {

        // Enable editable fields and make focusable for input
        EditText[] editableFields = {
                etSlNo, etDateTime, etSite, etMobileNumber, etPressureRange,
                etFlowRange, etTamperType, etSmsAltTime
        };

        for (EditText et : editableFields) {
            et.setEnabled(true);
            et.setFocusable(true);
            et.setFocusableInTouchMode(true);
        }

        // Optionally request focus on first editable field
        etSite.requestFocus();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
            }
        } catch (IOException e) {
            Log.e("BluetoothClose", "Error closing socket", e);
        }
    }


    private void disableAllFields() {
        etSlNo.setEnabled(false);
        etSite.setEnabled(false);
        etDateTime.setEnabled(false);
        etMobileNumber.setEnabled(false);
        etPressureRange.setEnabled(false);
        etFlowRange.setEnabled(false);
        etTamperType.setEnabled(false);
        etSmsAltTime.setEnabled(false);
    }

    private void saveDataToLocalDB() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        String sql = "INSERT OR REPLACE INTO device_config_detailed " +
                "(slno, site, datetime, mobilenumber, pressurerange, flowrange, tampertype, smsalttime) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        Object[] args = new Object[]{
                etSlNo.getText().toString(),
                etSite.getText().toString(),
                etDateTime.getText().toString(),
                etMobileNumber.getText().toString(),
                etPressureRange.getText().toString(),
                etFlowRange.getText().toString(),
                etTamperType.getText().toString(),
                etSmsAltTime.getText().toString()
        };

        db.execSQL(sql, args);
        db.close();
    }

    // Utility method to convert bytes to hex string for logging
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    private void sendDataToDevice() {
        String command = "0xAA55\"" +
                etSlNo.getText().toString() + "\"" +
                etSite.getText().toString() + "\"" +
                etDateTime.getText().toString() + "\"" +
                etMobileNumber.getText().toString() + "\"" +
                etPressureRange.getText().toString() + "\"" +
                etFlowRange.getText().toString() + "\"" +
                etTamperType.getText().toString() + "\"" +
                etSmsAltTime.getText().toString() + "\"";

        BluetoothManager bluetoothManager = BluetoothManager.getInstance();
        if (bluetoothManager.isConnected()) {
            bluetoothManager.sendCommand(command);
        } else {
            Toast.makeText(this, "Bluetooth device not connected", Toast.LENGTH_SHORT).show();
        }

        Log.d("BluetoothSend", "Sending command: " + command);
        Toast.makeText(this, "Seding Command" + command, Toast.LENGTH_SHORT).show();
    }

    private String buildCommandFromInputs() {
        return String.format("CMD,%s,%s,%s,%s,%s,%s",
                etSite.getText().toString(),
                etMobileNumber.getText().toString(),
                etPressureRange.getText().toString(),
                etFlowRange.getText().toString(),
                etTamperType.getText().toString(),
                etSmsAltTime.getText().toString());
    }

}
