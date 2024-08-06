package com.bengohub.VitalsTracker.Settings;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "reminder")
public class Reminder {

    @PrimaryKey(autoGenerate = true)
    private int id;
    private String description;
    private long datetime;
    private boolean repeat;
    private String soundUri;

    // Getters and setters for all fields
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getDatetime() {
        return datetime;
    }

    public void setDatetime(long datetime) {
        this.datetime = datetime;
    }

    public boolean isRepeat() {
        return repeat;
    }

    public void setRepeat(boolean repeat) {
        this.repeat = repeat;
    }

    public String getSoundUri() {
        return soundUri;
    }

    public void setSoundUri(String soundUri) {
        this.soundUri = soundUri;
    }
}
