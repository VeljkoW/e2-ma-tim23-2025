package com.example.rpgapp.model;

public class AllianceInvitation {
    private String id;
    private String allianceId;
    private String allianceName;
    private String inviterId;
    private String inviterUsername;
    private String inviteeId;
    private long timestamp;
    private InvitationStatus status;

    public enum InvitationStatus {
        PENDING,
        ACCEPTED,
        REJECTED
    }

    public AllianceInvitation() {
    }

    public AllianceInvitation(String allianceId, String allianceName, String inviterId,
                              String inviterUsername, String inviteeId) {
        this.allianceId = allianceId;
        this.allianceName = allianceName;
        this.inviterId = inviterId;
        this.inviterUsername = inviterUsername;
        this.inviteeId = inviteeId;
        this.timestamp = System.currentTimeMillis();
        this.status = InvitationStatus.PENDING;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAllianceId() {
        return allianceId;
    }

    public void setAllianceId(String allianceId) {
        this.allianceId = allianceId;
    }

    public String getAllianceName() {
        return allianceName;
    }

    public void setAllianceName(String allianceName) {
        this.allianceName = allianceName;
    }

    public String getInviterId() {
        return inviterId;
    }

    public void setInviterId(String inviterId) {
        this.inviterId = inviterId;
    }

    public String getInviterUsername() {
        return inviterUsername;
    }

    public void setInviterUsername(String inviterUsername) {
        this.inviterUsername = inviterUsername;
    }

    public String getInviteeId() {
        return inviteeId;
    }

    public void setInviteeId(String inviteeId) {
        this.inviteeId = inviteeId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public InvitationStatus getStatus() {
        return status;
    }

    public void setStatus(InvitationStatus status) {
        this.status = status;
    }

    public boolean isPending() {
        return status == InvitationStatus.PENDING;
    }
}

