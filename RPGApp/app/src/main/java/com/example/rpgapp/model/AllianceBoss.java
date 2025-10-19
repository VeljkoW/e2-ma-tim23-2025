package com.example.rpgapp.model;

import java.util.Date;

public class AllianceBoss {
    private String id;
    private String allianceId; // ID of the alliance this boss belongs to
    private Date dateOfCreation;
    private Status status; // ALIVE, DEAD, FAILED
    private int startingHp;
    private int currentHp;
    private Date dateOfLastDyingOrFailing; // Date when boss died or failed

    // Status enum
    public enum Status {
        ALIVE,
        DEAD,
        FAILED
    }

    // Required for Firestore serialization
    public AllianceBoss() {
        this.dateOfCreation = new Date();
        this.status = Status.ALIVE;
    }

    public AllianceBoss(String allianceId, int startingHp) {
        this();
        this.allianceId = allianceId;
        this.startingHp = startingHp;
        this.currentHp = startingHp;
    }

    // Getters
    public String getId() { return id; }
    public String getAllianceId() { return allianceId; }
    public Date getDateOfCreation() { return dateOfCreation; }
    public Status getStatus() { return status; }
    public int getStartingHp() { return startingHp; }
    public int getCurrentHp() { return currentHp; }
    public Date getDateOfLastDyingOrFailing() { return dateOfLastDyingOrFailing; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setAllianceId(String allianceId) { this.allianceId = allianceId; }
    public void setDateOfCreation(Date dateOfCreation) { this.dateOfCreation = dateOfCreation; }
    public void setStatus(Status status) { this.status = status; }
    public void setStartingHp(int startingHp) { this.startingHp = startingHp; }
    public void setCurrentHp(int currentHp) { this.currentHp = currentHp; }
    public void setDateOfLastDyingOrFailing(Date dateOfLastDyingOrFailing) { this.dateOfLastDyingOrFailing = dateOfLastDyingOrFailing; }

    // Utility methods
    public boolean isAlive() {
        return status == Status.ALIVE;
    }

    public void die() {
        this.status = Status.DEAD;
        this.dateOfLastDyingOrFailing = new Date();
    }

    public void fail() {
        this.status = Status.FAILED;
        this.dateOfLastDyingOrFailing = new Date();
    }

    public double getHpPercentage() {
        if (startingHp <= 0) return 0.0;
        return (double) currentHp / startingHp * 100.0;
    }

    @androidx.annotation.NonNull
    @Override
    public String toString() {
        return "AllianceBoss{" +
                "id='" + id + '\'' +
                ", allianceId='" + allianceId + '\'' +
                ", dateOfCreation=" + dateOfCreation +
                ", status=" + status +
                ", startingHp=" + startingHp +
                ", currentHp=" + currentHp +
                ", dateOfLastDyingOrFailing=" + dateOfLastDyingOrFailing +
                '}';
    }
}
