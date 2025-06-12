package com.example.acremetergps;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface UserDao {
    @Insert
    void insert(User user);

    @Query("SELECT * FROM User WHERE email = :email LIMIT 1")
    User getUserByEmail(String email);

    @Query("SELECT * FROM User WHERE email = :email AND password = :password LIMIT 1")
    User login(String email, String password);
}

