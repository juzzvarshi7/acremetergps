package com.example.acremetergps;

import java.util.ArrayList;
import java.util.List;

public class TripParser {

    public static List<TripModel> parse(String rawData) {
        List<TripModel> trips = new ArrayList<>();

        if (rawData == null || rawData.trim().isEmpty()) return trips;

        String[] lines = rawData.split("\n");

        for (String line : lines) {
            String[] parts = line.trim().split(",");

            if (parts.length < 5) continue;

            String area = parts[1].trim();           // 8.82 (assuming this is area/km)
            String time = parts[3].trim();           // 14:58
            String date = parts[4].trim();           // 19-05-2025

            trips.add(new TripModel(date, time, area));
        }

        return trips;
    }

}
