package com.example.acremetergps;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.acremetergps.R;

public class TripDetailActivity extends AppCompatActivity {

    TextView truckId, area, time, km, runHrs, speed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.example.acremetergps.R.layout.activity_trip_detail);

        truckId = findViewById(R.id.tvTruckId);
        area = findViewById(R.id.tvArea);
        time = findViewById(R.id.tvTime);
        km = findViewById(R.id.tvKm);
        runHrs = findViewById(R.id.tvRunHrs);
        speed = findViewById(R.id.tvSpeed);

        TripModel trip = (TripModel) getIntent().getSerializableExtra("trip");
        if (trip != null) {
            area.setText(trip.area);
            time.setText(trip.time);
        }
    }
}
