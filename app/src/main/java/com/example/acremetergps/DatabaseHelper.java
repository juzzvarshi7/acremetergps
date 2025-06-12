package com.example.acremetergps;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.util.Log;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.osmdroid.util.GeoPoint;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.*;
import java.util.List;
import java.util.Locale;


public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "sensor_logs.db";
    private static final int DB_VERSION = 6;

    public static final String TABLE_LOGS = "logs";
    public static final String COL_ID = "id";
    public static final String COL_TIMESTAMP = "timestamp";
    public static final String COL_USER_ID = "user_id";
    public static final String COL_DATA = "sensor_data";
    public static final String COL_ED = "ed";

    private final Context context;

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create logs table
        String queryLogs = "CREATE TABLE IF NOT EXISTS " + TABLE_LOGS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_TIMESTAMP + " TEXT, " +
                COL_USER_ID + " TEXT, " +
                COL_DATA + " TEXT, " +
                "flow_rate REAL, " +
                "log_time TEXT, " +
                "channel_id TEXT, " +
                "type_id TEXT, " +
                "analog_v1 REAL, " +
                "analog_v2 REAL, " +
                "analog_v3 REAL, " +
                "analog_v4 REAL, " +
                COL_ED + " INTEGER DEFAULT 0)";
        db.execSQL(queryLogs);

        String queryDeviceConfigDetailed = "CREATE TABLE IF NOT EXISTS device_config_detailed (" +
                "slno INTEGER PRIMARY KEY AUTOINCREMENT," +
                "site TEXT," +
                "time TEXT," +
                "mobilenumber TEXT," +
                "pressurerange TEXT," +
                "flowrange TEXT," +
                "tampertype TEXT," +
                "smsalttime TEXT" +
                ");";
        db.execSQL(queryDeviceConfigDetailed);


        String createConfigTable = "CREATE TABLE IF NOT EXISTS device_config_table (" +
                "mac_address TEXT PRIMARY KEY," +
                "config_data TEXT" +
                ");";
        db.execSQL(createConfigTable);

        // Create device_configurations table
        String queryDeviceConfig = "CREATE TABLE IF NOT EXISTS device_configurations (" +
                "mac_address TEXT PRIMARY KEY, " +
                "date_time TEXT, " +
                "server_ip TEXT, " +
                "site TEXT, " +
                "mobile_number TEXT, " +
                "sms_alt_time TEXT, " +
                "flow_range TEXT, " +
                "pressure_range TEXT, " +
                "tamper_type TEXT)";
        db.execSQL(queryDeviceConfig);

        String trip_data = "CREATE TABLE IF NOT EXISTS trip_data (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "tno TEXT," +
                "area TEXT,"+
                "km TEXT,"+
                "time TEXT,"+
                "date TEXT," +
                "rhrs TEXT," +
                "speed TEXT)";
        db.execSQL(trip_data);

        String raw_logs = "CREATE TABLE IF NOT EXISTS raw_logs (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT,"+
                "user_id TEXT,"+
                "date TEXT,"+
                "time TEXT,"+
                "latitude REAL,"+
                "longitude REAL)";
        db.execSQL(raw_logs);
    }
    public List<TripModel> getTripDetails() {
        List<TripModel> trips = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM trip_data ORDER BY date DESC, time DESC", null);
        if (cursor.moveToFirst()) {
            do {
                String tno = cursor.getString(cursor.getColumnIndexOrThrow("tno"));
                String area = cursor.getString(cursor.getColumnIndexOrThrow("area"));
                String km = cursor.getString(cursor.getColumnIndexOrThrow("km"));
                String time = cursor.getString(cursor.getColumnIndexOrThrow("time"));
                String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));
                String rhrs = cursor.getString(cursor.getColumnIndexOrThrow("rhrs"));
                String speed = cursor.getString(cursor.getColumnIndexOrThrow("speed"));

                trips.add(new TripModel(date, time, area));
            } while (cursor.moveToNext());
        }

        cursor.close();
        return trips;
    }

    public List<GeoPoint> getRoutePoints(String userId, String date) {
        List<GeoPoint> points = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT latitude, longitude FROM raw_logs WHERE user_id = ? AND date = ? ORDER BY time ASC", new String[]{userId, date});

        if (cursor.moveToFirst()) {
            do {
                double lat = cursor.getDouble(0);
                double lon = cursor.getDouble(1);
                points.add(new GeoPoint(lat, lon));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return points;
    }

    public String getRawData(String userId, String date) {
        StringBuilder sb = new StringBuilder();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT time, latitude, longitude FROM raw_logs WHERE user_id = ? AND date = ? ORDER BY time ASC", new String[]{userId, date});

        if (cursor.moveToFirst()) {
            do {
                sb.append(cursor.getString(0)).append(", ")
                        .append(cursor.getDouble(1)).append(", ")
                        .append(cursor.getDouble(2)).append("\n");
            } while (cursor.moveToNext());
        }
        cursor.close();
        return sb.toString();
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 4) {  // bump DB_VERSION to 4
            db.execSQL("DROP TABLE IF EXISTS device_config_detailed");
        }
        onCreate(db);
    }


    public boolean insertOrUpdateDeviceConfig(String mac, String site, String dateTime,
                                              String mobileNumber, String serverIp, String pressureRange,
                                              String flowRange, String tamperType, String smsAltTime) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("mac_address", mac);
        values.put("site", site);
        values.put("date_time", dateTime);
        values.put("mobile_number", mobileNumber);
        values.put("server_ip", serverIp);
        values.put("pressure_range", pressureRange);
        values.put("flow_range", flowRange);
        values.put("tamper_type", tamperType);
        values.put("sms_alt_time", smsAltTime);
        long result = db.replace("device_configurations", null, values); // replaces if exists
        return result != -1;
    }

    public Cursor getDeviceConfig(String mac) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM device_configurations WHERE mac_address = ?", new String[]{mac});
    }

    public Cursor getConfigurationHistory(String macAddress) {
        SQLiteDatabase db = this.getReadableDatabase();

        // Assuming you have a table named `device_configurations` with a `mac_address` column
        String query = "SELECT * FROM device_configurations WHERE mac_address = ?";
        return db.rawQuery(query, new String[]{macAddress});
    }


    public void insertLog(long timestamp, String userId, String data) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_TIMESTAMP, timestamp);
        values.put(COL_USER_ID, userId);
        values.put(COL_DATA, data);
        values.put(COL_ED, 0);
        db.insert(TABLE_LOGS, null, values);
        Log.d("DBHelper", "Inserted log for user:" + userId + ", data: " + data);

        Cursor cursor = getAllLogs();
        Log.d("DBHelper", "Fetched logs count: " + cursor.getCount());
        if (cursor != null && cursor.moveToFirst()) {
            int count = 0;
            do {
                int dataIndex = cursor.getColumnIndex(COL_DATA);
                for (int i = 0; i<5; i++) {
                    if (dataIndex != -1) {
                        Log.d("DBHelper", "Row " + count + ": " + cursor.getString(dataIndex));
                    } else {
                        Log.w("DBHelper", "COL_DATA not found in cursor.");
                    }
                    int index = cursor.getColumnIndex(COL_DATA);
                    if (index==-1) {
                        Log.d("DBHelper", "Missing expected column:" + COL_DATA);
                    }
                }
                count++;
            } while (cursor.moveToNext() && count < 5);
        }
        cursor.close();
    }

    public void insertTrip(String tno, String area, String km, String time, String date, String rhrs, String speed) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("tno", tno);
        values.put("area", area);
        values.put("km", km);
        values.put("time", time);
        values.put("date", date);
        values.put("rhrs", rhrs);
        values.put("speed", speed);
        long result = db.insert("trip_data", null, values);
        if (result == -1) {
            Log.e("DB_INSERT", "FAILED to insert trip: " + tno + ", " + date);
        } else {
            Log.d("DB_INSERT", "Trip inserted: " + tno + ", " + date);
        }
        db.insert("trip_data", null, values);
    }



    /* public void insertLog(String timestamp, String userId, String sensorData,
                          double flowRate, String logTime, String channelId, String typeId,
                          double analogV1) {

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_TIMESTAMP, timestamp);
        values.put(COL_USER_ID, userId);
        values.put(COL_DATA, sensorData);
        values.put("flow_rate", flowRate);
        values.put("log_time", logTime);
        values.put("channel_id", channelId);
        values.put("type_id", typeId);
        values.put("analog_v1", analogV1);
        values.put(COL_ED, 0);

        db.insert(TABLE_LOGS, null, values);
    }
    */
    public void insertTrip(String userId, String date, String time, String area) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("user_id", userId);
        values.put("date", date);
        values.put("time", time);
        values.put("area", area);

        db.insert("trip_data", null, values);
    }


    // Returns a Cursor for all logs from the database
    public Cursor getAllLogs() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_LOGS, null, null, null, null, null, COL_TIMESTAMP + " DESC");
    }


    public List<LogModel> getLogsBetweenDates(String startDate, String endDate) {
        List<LogModel> logs = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT * FROM " + TABLE_LOGS +
                " WHERE datetime(timestamp) BETWEEN datetime(?) AND datetime(?) ORDER BY datetime(timestamp) DESC";

        Cursor cursor = db.rawQuery(query, new String[]{startDate, endDate});
        Log.d("DBHelper", "getLogsBetweenDates: row count = " + cursor.getCount());

        if (cursor.moveToFirst()) {
            do {
                LogModel log = new LogModel(
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_TIMESTAMP)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_DATA)),
                        cursor.getString(cursor.getColumnIndexOrThrow("flow_rate")),
                        cursor.getString(cursor.getColumnIndexOrThrow("log_time")),
                        cursor.getString(cursor.getColumnIndexOrThrow("channel_id")),
                        cursor.getString(cursor.getColumnIndexOrThrow("type_id")),
                        cursor.getString(cursor.getColumnIndexOrThrow("analog_v1")),
                        cursor.getString(cursor.getColumnIndexOrThrow("analog_v2")),
                        cursor.getString(cursor.getColumnIndexOrThrow("analog_v3")),
                        cursor.getString(cursor.getColumnIndexOrThrow("analog_v4"))
                );
                logs.add(log);
            } while (cursor.moveToNext());
        } else {
            Log.w("DBHelper", "No logs found between " + startDate + " and " + endDate);
        }

        cursor.close();

        for (int i = 0; i < Math.min(logs.size(), 3); i++) {
            Log.d("LocalDB", logs.get(i).toString());
        }

        return logs;
    }

    public void saveDeviceConfig(String macAddress, String config) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("mac_address", macAddress);
        cv.put("config_data", config);

        // Try update first
        int rows = db.update("device_config_table", cv, "mac_address=?", new String[]{macAddress});
        if (rows == 0) {
            db.insert("device_config_table", null, cv);
        }
    }

    public String getDeviceConfigString(String mac) {
        Cursor cursor = getDeviceConfig(mac);
        if (cursor != null && cursor.moveToFirst()) {
            String config = cursor.getString(cursor.getColumnIndexOrThrow("config_data"));
            cursor.close();
            return config;
        }
        return null;  // no config found
    }


    public void parseAndInsertLog(String userId, String rawData) {
        String[] lines = rawData.trim().split("\\r?\\n");

        if (lines.length < 7) {
            Log.e("DBHelper", "Invalid data format received. Expected 7+ lines, got " + lines.length);
            return; // skip insertion
        }

        try {
            String sensorData = lines[0].trim();
            String data2 = lines[1].trim();      // not saved separately now
            double flowRate = Double.parseDouble(lines[2].trim()); // e.g., 78.45
            String logTime = lines[3].trim();    // e.g., 11:10:34
            String channelId = lines[4].trim();  // e.g., 4
            String typeId = lines[5].trim();     // e.g., 5
            double analogV1 = Double.parseDouble(lines[6].trim()); // e.g., 1.2

            String currentTimestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COL_TIMESTAMP, currentTimestamp);
            values.put(COL_USER_ID, userId);
            values.put(COL_DATA, sensorData + " | " + data2); // combine for now
            values.put("flow_rate", flowRate);
            values.put("log_time", logTime);
            values.put("channel_id", channelId);
            values.put("type_id", typeId);
            values.put("analog_v1", analogV1);
            values.put("analog_v2", 0.0); // default
            values.put("analog_v3", 0.0);
            values.put("analog_v4", 0.0);
            values.put(COL_ED, 0);

            db.insert(TABLE_LOGS, null, values);
            Log.d("DBHelper", "Structured log inserted for user: " + userId);

        } catch (Exception e) {
            Log.e("DBHelper", "Error parsing or inserting log: " + e.getMessage());
        }
    }


    public void insertLog(LogModel log) {
        insertLog(Long.parseLong(log.getTimestamp()), log.getUserId(), log.getSensorData());
    }

    public void insertTrip(Trip1 trip) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("tripId", trip.tripId);
        values.put("startTime", trip.startTime);
        values.put("endTime", trip.endTime);
        values.put("distance", trip.distance);
        db.insert("trips_table", null, values);
    }

    public void insertTrips(String truckId, List<TripModel> trips) {
        SQLiteDatabase db = this.getWritableDatabase();

        for (TripModel trip : trips) {
            ContentValues values = new ContentValues();
            values.put("truck_id", truckId);
            values.put("date", trip.date);
            values.put("time", trip.time);
            values.put("area", trip.area);

            db.insertWithOnConflict("logs", null, values, SQLiteDatabase.CONFLICT_IGNORE);
        }
    }



}
