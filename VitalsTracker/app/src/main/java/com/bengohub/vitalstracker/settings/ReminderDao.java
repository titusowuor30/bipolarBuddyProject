package com.bengohub.VitalsTracker.Settings;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ReminderDao {
    @Insert
    void insert(Reminder reminder);

    @Update
    void update(Reminder reminder);

    @Query("SELECT * FROM reminder WHERE id = :id")
    Reminder getReminderById(int id);

    @Query("SELECT * FROM reminder")
    List<Reminder> getAllReminders();
}
