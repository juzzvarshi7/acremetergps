package com.example.acremetergps;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DeviceConfigurationModel {
    private String site;
    private String dateTime;
    private String mobileNumber;
    private String serverIP;
    private String pressureRange;
    private String flowRange;
    private String tamperType;
    private String SMSAltTime;

    // Constructor
    public DeviceConfigurationModel(String site, String dateTime, String mobileNumber, String serverIP,
                                    String pressureRange, String flowRange, String tamperType, String SMSAltTime) {
        this.site = site;
        this.dateTime = dateTime;
        this.mobileNumber = mobileNumber;
        this.serverIP = serverIP;
        this.pressureRange = pressureRange;
        this.flowRange = flowRange;
        this.tamperType = tamperType;
        this.SMSAltTime = SMSAltTime;
    }

    // Getters
    public String getSite() {
        return site;
    }

    public String getDateTime() {
        return dateTime;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public String getServerIP() {
        return serverIP;
    }

    public String getPressureRange() {
        return pressureRange;
    }

    public String getFlowRange() {
        return flowRange;
    }

    public String getTamperType() {
        return tamperType;
    }

    public String getSMSAltTime() {
        return SMSAltTime;
    }

}
