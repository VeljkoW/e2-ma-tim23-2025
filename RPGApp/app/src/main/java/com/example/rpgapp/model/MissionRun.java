package com.example.rpgapp.model;

import java.util.Date;

public class MissionRun {
    public enum Status {
        PENDING,
        COMPLETED,
        FAILED
    }

    private String id;
    private String missionId;
    private String userId;
    private Date startDateTime;
    private Status status;
    private Date endDateTime; // nullable
    private int totalXp;

    public MissionRun(String id, String missionId, String userId, Date startDateTime, Status status, Date endDateTime, int totalXp) {
        this.id = id;
        this.missionId = missionId;
        this.userId = userId;
        this.startDateTime = startDateTime;
        this.status = status;
        this.endDateTime = endDateTime;
        this.totalXp = totalXp;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getMissionId() { return missionId; }
    public void setMissionId(String missionId) { this.missionId = missionId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Date getStartDateTime() { return startDateTime; }
    public void setStartDateTime(Date startDateTime) { this.startDateTime = startDateTime; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public Date getEndDateTime() { return endDateTime; }
    public void setEndDateTime(Date endDateTime) { this.endDateTime = endDateTime; }

    public int getTotalXp() { return totalXp; }
    public void setTotalXp(int totalXp) { this.totalXp = totalXp; }
}

