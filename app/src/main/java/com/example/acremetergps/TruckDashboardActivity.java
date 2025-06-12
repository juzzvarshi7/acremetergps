package com.example.acremetergps;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import androidx.appcompat.app.AppCompatActivity;

import com.example.acremetergps.R;

public class TruckDashboardActivity extends AppCompatActivity {
    private String[] latestParts;
    private TextView textViewData;
    private String deviceId;
    private BluetoothManager bluetoothManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_truck_main);

        textViewData = findViewById(R.id.textViewData);
        Button btnTripDetails = findViewById(R.id.btnTripDetails);
        Button btnSyncTrips = findViewById(R.id.btnSyncTrips);

        BluetoothManager bluetoothManager = BluetoothManager.getInstance();
        if (bluetoothManager != null) {
            bluetoothManager.setTripDataListener(data -> {
                Log.d("TruckDashboardActivity", "Trip data received: " + data);
            });
            bluetoothManager.requestTripData(data -> {
                Log.d("TruckDashboardActivity", "Requested trip data: " + data);
            });
        } else {
            Log.e("TruckDashboardActivity", "BluetoothManager instance is null! Did you initialize it in DashboardActivity?");
            Toast.makeText(this, "BluetoothManager not initialized!", Toast.LENGTH_LONG).show();
            // Handle error or maybe finish the activity gracefully
            finish();
        }
        // Receive latestParts from Intent extras
        latestParts = getIntent().getStringArrayExtra("latestParts");

        if (latestParts != null && latestParts.length == 11) {
            deviceId = latestParts[0];
            String formatted = "Device ID: " + latestParts[0] +
                    "\nSL No: " + latestParts[1] +
                    "\nPhone: " + latestParts[2] +
                    "\nTime: " + latestParts[3] + ":" + latestParts[4] +
                    "\nDate: " + latestParts[5] + "/" + latestParts[6] + "/" + latestParts[7] +
                    "\nPressure Range: " + latestParts[8] +
                    "\nFlow Range: " + latestParts[9] +
                    "\nSMS Alert Time: " + latestParts[10];

            textViewData.setText(formatted);
        } else {
            textViewData.setText("No Bluetooth data received.");
        }

        btnTripDetails.setOnClickListener(v -> {
            if (deviceId != null) {
                String[] rawdata = getIntent().getStringArrayExtra("rawdata");
                Intent intent = new Intent(TruckDashboardActivity.this, TripListActivity.class);
                intent.putExtra("truckId", latestParts[0]);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Device ID not available yet.", Toast.LENGTH_SHORT).show();
            }
        });

        btnSyncTrips.setOnClickListener(v -> {
            // Setup trip data listener
            bluetoothManager.setTripDataListener(rawData -> {
                runOnUiThread(() -> {
                    if (rawData != null && !rawData.isEmpty()) {
                        Log.d("TripData", "Received: " + rawData);
                        parseAndStoreTripData(rawData);
                    } else {
                        Toast.makeText(this, "No trip data received.", Toast.LENGTH_SHORT).show();
                    }
                });
            });

            // Send command to fetch all trip data
            bluetoothManager.sendCommand("0x55AA");
            bluetoothManager.sendCommand("RAWDATA");
            Toast.makeText(this, "Requesting raw data...", Toast.LENGTH_SHORT).show();
            Toast.makeText(this, "Sending command", Toast.LENGTH_SHORT).show();
        });

    }

    private void parseAndStoreTripData(String rawData) {
        DatabaseHelper db = new DatabaseHelper(this);

        String[] entries = rawData.split(";;"); // assuming each trip ends with ;;
        int count = 0;

        for (String entry : entries) {
            if (entry.trim().isEmpty()) continue;

            String[] parts = entry.split(",");

            if (parts.length >= 7) {
                String tno = parts[0].trim();
                String area = parts[1].trim();
                String km = parts[2].trim();
                String time = parts[3].trim();
                String date = parts[4].trim();
                String rhrs = parts[5].trim();
                String speed = parts[6].trim();

                db.insertTrip(tno, area, km, time, date, rhrs, speed);
                count++;
            }
        }

        Toast.makeText(this, count + " trips synced!", Toast.LENGTH_LONG).show();
    }
}
