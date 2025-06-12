package com.example.acremetergps;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "trip_table")
public class TripMetadata {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String tripId;
    public String truckId;
    public String area;
    public String date;
    public String time;
    public String runHours;
    public String km;
    public String speed;
    public String rawData;

    public int trip;
    public double distanceKm;
    public long runtimeMs;
    public double avgSpeedKmph;
    public String fileName;
    public int tripNo;

    public TripMetadata(String tripId, String area, String date, String time, String runHours, String km, String speed, String truckId) {
        this.tripId = tripId;
        this.area = area;
        this.date = date;
        this.time = time;
        this.runHours = runHours;
        this.km = km;
        this.speed = speed;
        this.truckId = truckId;
    }

    // Add Getters
    public String getTripId() { return tripId; }
    public String getArea() { return area; }
    public String getDate() { return date; }
    public String getTime() { return time; }
    public String getRunHours() { return runHours; }
    public String getKm() { return km; }
    public String getSpeed() { return speed; }
    public String getTruckId() { return truckId; }
}
