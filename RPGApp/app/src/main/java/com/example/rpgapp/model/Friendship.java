package com.example.rpgapp.model;

import java.util.Date;

public class Friendship {
    private String id;
    private String userId1;
    private String userId2;
    private FriendshipStatus status;
    private String requesterId; // ID korisnika koji je poslao zahtev
    private Date requestDate;
    private Date acceptedDate;

    public enum FriendshipStatus {
        PENDING,    // Čeka se odobrenje
        ACCEPTED,   // Prijateljstvo prihvaćeno
        REJECTED,   // Prijateljstvo odbijeno
        BLOCKED     // Blokiran korisnik
    }

    public Friendship() {
        // Potreban prazan konstruktor za Firebase
    }

    public Friendship(String userId1, String userId2, String requesterId) {
        this.userId1 = userId1;
        this.userId2 = userId2;
        this.requesterId = requesterId;
        this.status = FriendshipStatus.PENDING;
        this.requestDate = new Date();
    }

    // Getters
    public String getId() { return id; }
    public String getUserId1() { return userId1; }
    public String getUserId2() { return userId2; }
    public FriendshipStatus getStatus() { return status; }
    public String getRequesterId() { return requesterId; }
    public Date getRequestDate() { return requestDate; }
    public Date getAcceptedDate() { return acceptedDate; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setUserId1(String userId1) { this.userId1 = userId1; }
    public void setUserId2(String userId2) { this.userId2 = userId2; }
    public void setStatus(FriendshipStatus status) { this.status = status; }
    public void setRequesterId(String requesterId) { this.requesterId = requesterId; }
    public void setRequestDate(Date requestDate) { this.requestDate = requestDate; }
    public void setAcceptedDate(Date acceptedDate) { this.acceptedDate = acceptedDate; }

    // Helper metode
    public String getOtherUserId(String currentUserId) {
        if (userId1.equals(currentUserId)) {
            return userId2;
        } else if (userId2.equals(currentUserId)) {
            return userId1;
        }
        return null;
    }

    public boolean isPending() {
        return status == FriendshipStatus.PENDING;
    }

    public boolean isAccepted() {
        return status == FriendshipStatus.ACCEPTED;
    }

    public boolean isRequestedBy(String userId) {
        return requesterId.equals(userId);
    }
}

