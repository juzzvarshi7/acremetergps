package com.example.acremetergps;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polyline;
import android.util.Log;

import java.util.List;

public class MapActivity extends AppCompatActivity {
    private MapView mapView;
    private DatabaseHelper dbHelper;
    private String truckId, date;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(getApplicationContext(), getSharedPreferences("osmdroid", MODE_PRIVATE));
        setContentView(com.example.acremetergps.R.layout.activity_map);

        mapView = findViewById(com.example.acremetergps.R.id.mapView);
        mapView.setMultiTouchControls(true);
        dbHelper = new DatabaseHelper(this);

        truckId = getIntent().getStringExtra("truckId");
        date = getIntent().getStringExtra("date");

        List<GeoPoint> routePoints = dbHelper.getRoutePoints(truckId, date);
        Log.d("MapActivity", "Loaded " + routePoints.size() + " points for " + truckId + " on " + date);

        if (!routePoints.isEmpty()) {
            mapView.getController().setZoom(15.0);
            mapView.getController().setCenter(routePoints.get(0));

            Polyline routeLine = new Polyline();
            routeLine.setPoints(routePoints);
            mapView.getOverlays().add(routeLine);
        }
    }
}
