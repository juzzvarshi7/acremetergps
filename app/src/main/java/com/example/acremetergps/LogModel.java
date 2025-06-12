package com.example.acremetergps;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.text.ParseException;

public class LogModel {
    private String timestamp;
    private String userId;
    private String sensorData;
    private String flowRate;
    private String logTime;
    private String channelId;
    private String typeId;
    private String analogV1;
    private String analogV2;
    private String analogV3;
    private String analogV4;

    // Constructor
    public LogModel(String timestamp, String userId, String sensorData, String flowRate, String logTime,
                    String channelId, String typeId, String analogV1, String analogV2, String analogV3, String analogV4) {
        this.timestamp = timestamp;
        this.userId = userId;
        this.sensorData = sensorData;
        this.flowRate = flowRate;
        this.logTime = logTime;
        this.channelId = channelId;
        this.typeId = typeId;
        this.analogV1 = analogV1;
        this.analogV2 = analogV2 != null ? analogV2 : "0.0";;
        this.analogV3 = analogV3 != null ? analogV3 : "0.0";;
        this.analogV4 = analogV4 != null ? analogV4 : "0.0";;
    }

    // Getters
    public String getTimestamp() {
        return timestamp;
    }

    public String getUserId() {
        return userId;
    }

    public String getSensorData() {
        return sensorData;
    }

    public String getFlowRate() {
        return flowRate;
    }

    public String getLogTime() {
        return logTime;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getTypeId() {
        return typeId;
    }

    public String getAnalogV1() {
        return analogV1;
    }

    public String getAnalogV2() {
        return analogV2 != null ? analogV2 : "0.0";
    }

    public String getAnalogV3() {
        return analogV3 != null ? analogV3 : "0.0";
    }

    public String getAnalogV4() {
        return analogV4 != null ? analogV4 : "0.0";
    }

    public String getId() {
        return userId;
    }

    public Object getData() {
        return sensorData;
    }
    public boolean isInRange(String fromDate, String toDate) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        try {
            Date logDate = sdf.parse(this.timestamp); // Assuming timestamp is in the "yyyy-MM-dd" format
            Date startDate = sdf.parse(fromDate);
            Date endDate = sdf.parse(toDate);

            // Check if the log date is within the range
            return !logDate.before(startDate) && !logDate.after(endDate);
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
    }
}