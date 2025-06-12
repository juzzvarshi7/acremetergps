package com.example.acremetergps;

import static com.itextpdf.kernel.pdf.PdfName.Color;

import android.Manifest;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.UnstableApi;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import com.google.android.gms.location.Priority;
import com.google.gson.Gson;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class GpsTrackingActivity extends AppCompatActivity {
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private MapView mapView;
    private Button btnStart, btnStop, btnReplay, btnFetchTrips;
    private boolean tracking = false;
    private ArrayList<GeoPoint> tripPoints = new ArrayList<>();
    private Polyline tripLine;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private long startTime = 0;
    private double totalDistance = 0;
    private GeoPoint lastPoint = null;
    private Marker startMarker, endMarker;
    private String deviceMac;
    private BluetoothSocket bluetoothSocket;
    private BluetoothManager bluetoothManager;
    private TripMetadata meta;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.gps_tracking);

        mapView = findViewById(R.id.mapView);
        btnStart = findViewById(R.id.btnStartTrip);
        btnStop = findViewById(R.id.btnStopTrip);
        btnReplay = findViewById(R.id.btnReplayTrip);
        btnFetchTrips = findViewById(R.id.btnFetchTrips);

        deviceMac = getIntent().getStringExtra("DEVICE_MAC");
        bluetoothManager = BluetoothManager.getInstance();

        if (deviceMac == null) {
            Toast.makeText(this, "Device MAC not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (bluetoothManager.isConnected() && deviceMac.equals(bluetoothManager.getConnectedDeviceMac())) {
            Toast.makeText(this, "Reusing existing Bluetooth connection", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "No existing connection, please connect from Dashboard", Toast.LENGTH_SHORT).show();
        }

        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(18.0);
        mapView.getController().setCenter(new GeoPoint(0.0, 0.0));

        tripLine = new Polyline();
        tripLine.setWidth(0.0f);
        mapView.getOverlays().add(tripLine);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationCallback = new LocationCallback() {
            @OptIn(markerClass = UnstableApi.class)
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (!tracking) return;
                for (Location location : locationResult.getLocations()) {
                    GeoPoint point = new GeoPoint(location.getLatitude(), location.getLongitude());
                    Log.d("GPS", "New point: " + location.getLatitude() + "," + location.getLongitude());
                    tripPoints.add(point);
                    if (lastPoint != null) {
                        totalDistance += lastPoint.distanceToAsDouble(point);
                    } else {
                        addStartMarker(point);
                    }
                    lastPoint = point;
                    tripLine.setPoints(tripPoints);
                    mapView.getController().animateTo(point);
                }
                mapView.invalidate();
            }
        };

        btnStart.setOnClickListener(v -> startTrip());
        btnStop.setOnClickListener(v -> stopTrip());
        btnReplay.setOnClickListener(v -> replayTrip());
        btnFetchTrips.setOnClickListener(v -> showSavedTrips());

        requestPermissionsIfNecessary(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        });
    }

    private void startTrip() {
        btnStart.setEnabled(false);
        btnStop.setEnabled(true);
        tracking = true;
        tripPoints.clear();
        tripLine.setPoints(tripPoints);
        totalDistance = 0;
        lastPoint = null;
        startTime = SystemClock.elapsedRealtime();
        removeMarkers();
        startLocationUpdates();
        Toast.makeText(this, "Trip started", Toast.LENGTH_SHORT).show();
    }

    private void stopTrip() {
        tracking = false;
        stopLocationUpdates();
        btnStart.setEnabled(true);
        btnStop.setEnabled(false);
        long elapsedMillis = SystemClock.elapsedRealtime() - startTime;
        saveTrip(elapsedMillis);
        addEndMarker();
        Toast.makeText(this, "Trip saved\nDistance: " + String.format("%.2f", totalDistance) + " m\nDuration: " + (elapsedMillis / 1000) + " sec", Toast.LENGTH_LONG).show();
        syncLastTripToDevice();
    }

    private void saveTrip(long elapsedMillis) {
        try {
            double avgSpeed = (totalDistance / 1000.0) / (elapsedMillis / 3600000.0);
            meta = new TripMetadata(
                    "trip_" + System.currentTimeMillis(),
                    "Unknown Area",
                    new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()),
                    new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date()),
                    "",
                    String.format("%.2f", totalDistance / 1000.0),
                    String.format("%.2f", avgSpeed),
                    deviceMac
            );
            meta.trip = (int) (System.currentTimeMillis() / 1000);
            meta.distanceKm = totalDistance / 1000.0;
            meta.runtimeMs = elapsedMillis;
            meta.avgSpeedKmph = avgSpeed;
            meta.fileName = deviceMac + "_" + meta.tripNo;

            File dir = getExternalFilesDir(null);

            File gpsFile = new File(dir, meta.fileName + "_trip.txt");
            try (FileWriter fos = new FileWriter(gpsFile)) {
                for (GeoPoint p : tripPoints) {
                    fos.write(p.getLatitude() + "," + p.getLongitude() + "\n");
                }
            }

            File jsonFile = new File(dir, meta.fileName + "_trip.json");
            try (FileWriter writer = new FileWriter(jsonFile)) {
                new Gson().toJson(meta, writer);
            }

            File tripsFile = new File(dir, deviceMac + "_trips.txt");
            try (FileWriter writer = new FileWriter(tripsFile, true)) {
                writer.write(meta.fileName + "\n");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showSavedTrips() {
        try {
            File dir = getExternalFilesDir(null);
            File tripsFile = new File(dir, deviceMac + "_trips.txt");
            if (!tripsFile.exists()) {
                Toast.makeText(this, "No trips file found.", Toast.LENGTH_SHORT).show();
                return;
            }

            StringBuilder content = new StringBuilder();
            BufferedReader reader = new BufferedReader(new FileReader(tripsFile));
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            reader.close();

            new AlertDialog.Builder(this)
                    .setTitle("Saved Trip Table")
                    .setMessage(content.toString())
                    .setPositiveButton("OK", null)
                    .show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to load local trips", Toast.LENGTH_SHORT).show();
        }
    }

    private void syncLastTripToDevice() {
        bluetoothSocket = bluetoothManager.getSocket();
        if (bluetoothSocket == null) {
            Toast.makeText(this, "Bluetooth device not connected.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            OutputStream outputStream = bluetoothSocket.getOutputStream();
            File dir = getExternalFilesDir(null);
            File jsonFile = new File(dir, meta.fileName + "_trip.json");
            File gpsFile = new File(dir, meta.fileName + "_trip.txt");

            outputStream.write("<TRIP_START>\n".getBytes());
            outputStream.write("<META>\n".getBytes());
            sendFileContents(jsonFile, outputStream);
            outputStream.write("</META>\n".getBytes());
            outputStream.write("<GPS>\n".getBytes());
            sendFileContents(gpsFile, outputStream);
            outputStream.write("</GPS>\n".getBytes());
            outputStream.write("<TRIP_END>\n".getBytes());
            outputStream.flush();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to sync trip to device", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendFileContents(File file, OutputStream outputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                outputStream.write((line + "\n").getBytes());
            }
        }
    }

    private void replayTrip() {
        // Reuse loadTrip logic or combine
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, // priority
                3000 // interval in ms
        )
                .setMinUpdateIntervalMillis(2000)   // previously 'setFastestInterval'
                .build();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    private void requestPermissionsIfNecessary(String[] permissions) {
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    private void addStartMarker(GeoPoint point) {
        if (startMarker != null) mapView.getOverlays().remove(startMarker);
        startMarker = new Marker(mapView);
        startMarker.setPosition(point);
        startMarker.setTitle("Start");
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mapView.getOverlays().add(startMarker);
    }

    private void addEndMarker() {
        if (!tripPoints.isEmpty()) {
            if (endMarker != null) mapView.getOverlays().remove(endMarker);
            endMarker = new Marker(mapView);
            endMarker.setPosition(tripPoints.get(tripPoints.size() - 1));
            endMarker.setTitle("End");
            endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            mapView.getOverlays().add(endMarker);
        }
    }

    private void removeMarkers() {
        if (startMarker != null) mapView.getOverlays().remove(startMarker);
        if (endMarker != null) mapView.getOverlays().remove(endMarker);
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }
}
