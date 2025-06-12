package com.example.acremetergps;

import com.example.acremetergps.BluetoothManager.TripDataCallback;
import com.example.acremetergps.R;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.os.Handler;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class TripListActivity extends AppCompatActivity {
    private String truckId;
    private DatabaseHelper dbHelper;
    private BluetoothManager bluetoothManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.example.acremetergps.R.layout.activity_trip_list);

        truckId = getIntent().getStringExtra("truckId");
        Log.d("TripListActivity", "Received truckId: " + truckId);
        dbHelper = new DatabaseHelper(this);

        bluetoothManager = BluetoothManager.getInstance(dbHelper, new Handler(), data -> {
            Log.d("TripListActivity", "BluetoothManager DataListener received: " + data);

        });

        if (bluetoothManager.isConnected()) {
            Log.d("TripListActivity", "Bluetooth connected");
            fetchTripsFromBluetooth();
            Toast.makeText(this, "Syncing trips from Bluetooth...", Toast.LENGTH_SHORT).show();
        } else {
            Log.w("TripListActivity", "Bluetooth not connected. Skipping fetchTripsFromBluetooth().");
            Toast.makeText(this, "Bluetooth not connected, showing stored trips.", Toast.LENGTH_SHORT).show();
        }

        ScrollView scrollView = findViewById(R.id.tripScroll);
        LinearLayout container = findViewById(R.id.tripContainer);

        List<TripModel> tripList = dbHelper.getTripDetails();
        Log.d("TripListActivity", "Trip list size: " + (tripList != null ? tripList.size() : "null"));
        if (tripList == null || tripList.isEmpty()) {
            Log.w(TAG, "No trips found for truckId: " + truckId);
            Toast.makeText(this, "No trips found.", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d("DB_QUERY", "Querying trips for truckId: " + truckId);
        Log.d(TAG, "Loaded " + tripList.size() + " trips for truckId: " + truckId);
        for (TripModel trip : tripList) {
            Log.d(TAG, "Adding trip view for date: " + trip.date + ", time: " + trip.time);
            View view = getLayoutInflater().inflate(R.layout.item_trip, null);
            ((TextView) view.findViewById(R.id.tvDate)).setText(trip.date);
            ((TextView) view.findViewById(R.id.tvTime)).setText(trip.time);
            ((TextView) view.findViewById(R.id.tvArea)).setText(trip.area);

            view.findViewById(R.id.btnDetails).setOnClickListener(v -> {
                Log.d(TAG, "Details clicked for trip date: " + trip.date);
                Intent intent = new Intent(this, TripDetailActivity.class);
                intent.putExtra("trip", trip);
                startActivity(intent);
            });

            view.findViewById(R.id.btnMap).setOnClickListener(v -> {
                Log.d(TAG, "Map clicked for trip date: " + trip.date);
                Intent intent = new Intent(this, MapActivity.class);
                intent.putExtra("truckId", truckId);
                intent.putExtra("date", trip.date);
                startActivity(intent);
            });

            view.findViewById(R.id.btnDownload).setOnClickListener(v -> {
                Log.d(TAG, "Download clicked for trip date: " + trip.date);
                String rawData = dbHelper.getRawData(truckId, trip.date);
                try {
                    File file = new File(getExternalFilesDir(null), "Trip_" + trip.date + ".txt");
                    FileOutputStream fos = new FileOutputStream(file);
                    fos.write(rawData.getBytes());
                    fos.close();
                    Toast.makeText(this, "Saved to " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "File saved at: " + file.getAbsolutePath());
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "Error saving file", e);
                    Toast.makeText(this, "Error saving file", Toast.LENGTH_SHORT).show();
                }
            });


            container.addView(view);
        }
    }
    private void fetchTripsFromBluetooth() {
        bluetoothManager.requestTripData(new TripDataCallback() {
            @Override
            public void onTripDataReceived(String rawData) {
                List<TripModel> trips = TripParser.parse(rawData);
                for (TripModel trip : trips) {
                    dbHelper.insertTrip(truckId, trip.date, trip.time, trip.area);
                }
                Log.d("DB_INSERT", "Inserting trip for truckId: " + truckId);
                runOnUiThread(() -> {
                    Toast.makeText(TripListActivity.this, "Synced and saved trips", Toast.LENGTH_SHORT).show();
                    recreate();
                });
            }
        });
    }

}
