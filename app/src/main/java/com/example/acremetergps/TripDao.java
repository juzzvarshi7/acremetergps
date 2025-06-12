package com.example.acremetergps;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;
@Dao
public interface TripDao {

    @Insert
    void insertTrip(TripMetadata trip);

    @Query("SELECT * FROM trip_table WHERE truckId = :truckId")
    List<TripMetadata> getTripsByTruckId(String truckId);

    @Query("SELECT * FROM trip_table")
    LiveData<List<TripMetadata>> getAllTrips();
}
