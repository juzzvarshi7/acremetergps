package com.example.acremetergps;

import com.google.gson.*;
import java.lang.reflect.Type;

public class LogModelDeserializer implements JsonDeserializer<LogModel> {

    @Override
    public LogModel deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        String rawLogData = json.getAsString(); // Get the raw log data (e.g., 9876""data""12.34""13:09:56""4""3""8.7)

        // Split by the "" separator
        String[] parts = rawLogData.split("\"\"");

        if (parts.length != 8) {
            // Handle incorrect format, or throw an exception
            throw new JsonParseException("Invalid log data format");
        }

        // Now map the parts to the LogModel attributes
        String timestamp = parts[0]; // "9876"
        String userId = parts[1]; // "data"
        String sensorData = parts[2]; // "12.34"
        String flowRate = parts[3]; // "13:09:56"
        String logTime = parts[4]; // "4"
        String channelId = parts[5]; // "3"
        String typeId = parts[6]; // "8.7"
        String analogV1 = ""; // Set empty or handle if there's more data
        String analogV2 = ""; // Set empty or handle if there's more data
        String analogV3 = ""; // Set empty or handle if there's more data
        String analogV4 = ""; // Set empty or handle if there's more data

        // Return a new LogModel object with parsed data
        return new LogModel(timestamp, userId, sensorData, flowRate, logTime, channelId, typeId, analogV1, analogV2, analogV3, analogV4);
    }
}
