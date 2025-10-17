package com.example.rpgapp.model;

import java.util.ArrayList;
import java.util.List;

public class Alliance {
    private String id;
    private String name;
    private String leaderId;
    private List<String> memberIds;
    private long createdAt;
    private boolean missionActive;
    private String missionId;

    public Alliance() {
        this.memberIds = new ArrayList<>();
        this.missionActive = false;
    }

    public Alliance(String id, String name, String leaderId) {
        this.id = id;
        this.name = name;
        this.leaderId = leaderId;
        this.memberIds = new ArrayList<>();
        this.memberIds.add(leaderId);
        this.createdAt = System.currentTimeMillis();
        this.missionActive = false;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLeaderId() {
        return leaderId;
    }

    public void setLeaderId(String leaderId) {
        this.leaderId = leaderId;
    }

    public List<String> getMemberIds() {
        return memberIds;
    }

    public void setMemberIds(List<String> memberIds) {
        this.memberIds = memberIds;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isMissionActive() {
        return missionActive;
    }

    public void setMissionActive(boolean missionActive) {
        this.missionActive = missionActive;
    }

    public String getMissionId() {
        return missionId;
    }

    public void setMissionId(String missionId) {
        this.missionId = missionId;
    }
}

