package com.example.rpgapp.model;

import java.util.Date;
import java.util.List;

public class CalendarDay {
    private int dayNumber;
    private Date date;
    private List<Mission> missions;

    public CalendarDay(int dayNumber, Date date, List<Mission> missions) {
        this.dayNumber = dayNumber;
        this.date = date;
        this.missions = missions;
    }

    public int getDayNumber() { return dayNumber; }
    public void setDayNumber(int dayNumber) { this.dayNumber = dayNumber; }

    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }

    public List<Mission> getMissions() { return missions; }
    public void setMissions(List<Mission> missions) { this.missions = missions; }

    public boolean hasMissions() {
        return missions != null && !missions.isEmpty();
    }
}
