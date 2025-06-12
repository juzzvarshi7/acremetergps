package com.example.acremetergps;

import java.io.Serializable;

public class TripModel implements java.io.Serializable {
    public String date;
    public String time;
    public String area;

    public TripModel(String date, String time, String area) {
        this.date = date;
        this.time = time;
        this.area = area;
    }
}
