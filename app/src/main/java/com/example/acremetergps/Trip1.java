package com.example.acremetergps;

import java.util.ArrayList;
import java.util.List;

public class Trip1 {
    public int tripId;
    public String startTime;
    public String endTime;
    public double distance;

    public Trip1(int tripId, String startTime, String endTime, double distance) {
        this.tripId = tripId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.distance = distance;
    }

    public List<Trip1> parseTrips(String rawData) {
        List<Trip1> trips = new ArrayList<>();
        String[] lines = rawData.split("\n");

        for (String line : lines) {
            if (line.trim().isEmpty()) continue;

            String[] parts = line.split(";");
            int tripId = 0;
            String start = "", end = "";
            double distance = 0;

            for (String part : parts) {
                String[] keyValue = part.split("=");
                if (keyValue.length != 2) continue;

                switch (keyValue[0].trim()) {
                    case "tripId":
                        tripId = Integer.parseInt(keyValue[1].trim());
                        break;
                    case "start":
                        start = keyValue[1].trim();
                        break;
                    case "end":
                        end = keyValue[1].trim();
                        break;
                    case "distance":
                        distance = Double.parseDouble(keyValue[1].trim());
                        break;
                }
            }

            trips.add(new Trip1(tripId, start, end, distance));
        }
        return trips;
    }
}
