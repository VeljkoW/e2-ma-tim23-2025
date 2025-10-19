package com.example.rpgapp.repository;

import com.example.rpgapp.callback.AuthCallback;
import com.example.rpgapp.model.Alliance;
import com.example.rpgapp.model.AllianceInvitation;
import com.example.rpgapp.model.AllianceMessage;
import com.example.rpgapp.model.User;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AllianceRepository {
    private static final String ALLIANCES_COLLECTION = "alliances";
    private static final String INVITATIONS_COLLECTION = "allianceInvitations";
    private static final String MESSAGES_COLLECTION = "allianceMessages";
    private static final String USERS_COLLECTION = "users";

    private final FirebaseFirestore db;

    public AllianceRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    // Create new alliance
    public void createAlliance(String name, String leaderId, AuthCallback<Alliance> callback) {
        String allianceId = db.collection(ALLIANCES_COLLECTION).document().getId();
        Alliance alliance = new Alliance(allianceId, name, leaderId);

        db.collection(ALLIANCES_COLLECTION)
                .document(allianceId)
                .set(alliance)
                .addOnSuccessListener(aVoid -> callback.onResult(alliance))
                .addOnFailureListener(e -> callback.onResult(null));
    }

    // Get user's current alliance
    public void getUserAlliance(String userId, AuthCallback<Alliance> callback) {
        db.collection(ALLIANCES_COLLECTION)
                .whereArrayContains("memberIds", userId)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        Alliance alliance = querySnapshot.getDocuments().get(0).toObject(Alliance.class);
                        callback.onResult(alliance);
                    } else {
                        callback.onResult(null);
                    }
                })
                .addOnFailureListener(e -> callback.onResult(null));
    }

    // Send invitation to friend
    public void sendInvitation(String allianceId, String allianceName, String inviterId,
                               String inviterUsername, String inviteeId, AuthCallback<Boolean> callback) {
        AllianceInvitation invitation = new AllianceInvitation(allianceId, allianceName,
                inviterId, inviterUsername, inviteeId);

        String invitationId = db.collection(INVITATIONS_COLLECTION).document().getId();
        invitation.setId(invitationId);

        db.collection(INVITATIONS_COLLECTION)
                .document(invitationId)
                .set(invitation)
                .addOnSuccessListener(aVoid -> callback.onResult(true))
                .addOnFailureListener(e -> callback.onResult(false));
    }

    // Get pending invitations for user
    public void getPendingInvitations(String userId, AuthCallback<List<AllianceInvitation>> callback) {
        db.collection(INVITATIONS_COLLECTION)
                .whereEqualTo("inviteeId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<AllianceInvitation> invitations = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        AllianceInvitation invitation = doc.toObject(AllianceInvitation.class);
                        if (invitation != null && invitation.getStatus() == AllianceInvitation.InvitationStatus.PENDING) {
                            invitations.add(invitation);
                        }
                    }
                    // Sort by timestamp manually
                    invitations.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                    callback.onResult(invitations);
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    callback.onResult(new ArrayList<>());
                });
    }

    // Accept invitation
    public void acceptInvitation(AllianceInvitation invitation, String currentAllianceId,
                                 AuthCallback<Boolean> callback) {
        // First leave current alliance if exists
        if (currentAllianceId != null) {
            leaveAlliance(currentAllianceId, invitation.getInviteeId(), success -> {
                if (success) {
                    joinAlliance(invitation, callback);
                } else {
                    callback.onResult(false);
                }
            });
        } else {
            joinAlliance(invitation, callback);
        }
    }

    private void joinAlliance(AllianceInvitation invitation, AuthCallback<Boolean> callback) {
        // Update alliance members
        db.collection(ALLIANCES_COLLECTION)
                .document(invitation.getAllianceId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Alliance alliance = documentSnapshot.toObject(Alliance.class);
                    if (alliance != null) {
                        List<String> members = alliance.getMemberIds();
                        if (!members.contains(invitation.getInviteeId())) {
                            members.add(invitation.getInviteeId());

                            db.collection(ALLIANCES_COLLECTION)
                                    .document(invitation.getAllianceId())
                                    .update("memberIds", members)
                                    .addOnSuccessListener(aVoid -> {
                                        // Update invitation status
                                        updateInvitationStatus(invitation.getId(),
                                                AllianceInvitation.InvitationStatus.ACCEPTED, callback);
                                    })
                                    .addOnFailureListener(e -> callback.onResult(false));
                        } else {
                            callback.onResult(true);
                        }
                    } else {
                        callback.onResult(false);
                    }
                })
                .addOnFailureListener(e -> callback.onResult(false));
    }

    // Reject invitation
    public void rejectInvitation(String invitationId, AuthCallback<Boolean> callback) {
        updateInvitationStatus(invitationId, AllianceInvitation.InvitationStatus.REJECTED, callback);
    }

    private void updateInvitationStatus(String invitationId,
                                        AllianceInvitation.InvitationStatus status,
                                        AuthCallback<Boolean> callback) {
        db.collection(INVITATIONS_COLLECTION)
                .document(invitationId)
                .update("status", status)
                .addOnSuccessListener(aVoid -> callback.onResult(true))
                .addOnFailureListener(e -> callback.onResult(false));
    }

    // Leave alliance
    public void leaveAlliance(String allianceId, String userId, AuthCallback<Boolean> callback) {
        db.collection(ALLIANCES_COLLECTION)
                .document(allianceId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Alliance alliance = documentSnapshot.toObject(Alliance.class);
                    if (alliance != null) {
                        List<String> members = alliance.getMemberIds();
                        members.remove(userId);

                        db.collection(ALLIANCES_COLLECTION)
                                .document(allianceId)
                                .update("memberIds", members)
                                .addOnSuccessListener(aVoid -> callback.onResult(true))
                                .addOnFailureListener(e -> callback.onResult(false));
                    } else {
                        callback.onResult(false);
                    }
                })
                .addOnFailureListener(e -> callback.onResult(false));
    }

    // Delete alliance (leader only, mission not active)
    public void deleteAlliance(String allianceId, AuthCallback<Boolean> callback) {
        db.collection(ALLIANCES_COLLECTION)
                .document(allianceId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onResult(true))
                .addOnFailureListener(e -> callback.onResult(false));
    }

    // Send message
    public void sendMessage(String allianceId, String senderId, String senderUsername,
                           String messageText, AuthCallback<Boolean> callback) {
        AllianceMessage message = new AllianceMessage(allianceId, senderId, senderUsername, messageText);
        String messageId = db.collection(MESSAGES_COLLECTION).document().getId();
        message.setId(messageId);

        db.collection(MESSAGES_COLLECTION)
                .document(messageId)
                .set(message)
                .addOnSuccessListener(aVoid -> callback.onResult(true))
                .addOnFailureListener(e -> callback.onResult(false));
    }

    // Get messages for alliance
    public void getAllianceMessages(String allianceId, AuthCallback<List<AllianceMessage>> callback) {
        db.collection(MESSAGES_COLLECTION)
                .whereEqualTo("allianceId", allianceId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<AllianceMessage> messages = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        AllianceMessage message = doc.toObject(AllianceMessage.class);
                        if (message != null) {
                            messages.add(message);
                        }
                    }
                    // Sort by timestamp manually
                    messages.sort((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));
                    callback.onResult(messages);
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    callback.onResult(new ArrayList<>());
                });
    }

    // Get alliance members as User objects
    public void getAllianceMembers(List<String> memberIds, AuthCallback<List<User>> callback) {
        if (memberIds == null || memberIds.isEmpty()) {
            callback.onResult(new ArrayList<>());
            return;
        }

        // Firebase whereIn has a limit of 10 items, so we need to handle this differently
        List<User> users = new ArrayList<>();
        final int[] processedCount = {0};

        for (String userId : memberIds) {
            db.collection(USERS_COLLECTION)
                    .document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            users.add(user);
                        }
                        processedCount[0]++;

                        if (processedCount[0] == memberIds.size()) {
                            callback.onResult(users);
                        }
                    })
                    .addOnFailureListener(e -> {
                        processedCount[0]++;
                        if (processedCount[0] == memberIds.size()) {
                            callback.onResult(users);
                        }
                    });
        }
    }

    // Get alliance by ID
    public void getAllianceById(String allianceId, AuthCallback<Alliance> callback) {
        db.collection(ALLIANCES_COLLECTION)
                .document(allianceId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Alliance alliance = documentSnapshot.toObject(Alliance.class);
                    callback.onResult(alliance);
                })
                .addOnFailureListener(e -> callback.onResult(null));
    }

    // Check if invitation already exists
    public void checkPendingInvitation(String allianceId, String inviteeId, AuthCallback<AllianceInvitation> callback) {
        db.collection(INVITATIONS_COLLECTION)
                .whereEqualTo("allianceId", allianceId)
                .whereEqualTo("inviteeId", inviteeId)
                .whereEqualTo("status", AllianceInvitation.InvitationStatus.PENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        AllianceInvitation invitation = querySnapshot.getDocuments().get(0).toObject(AllianceInvitation.class);
                        callback.onResult(invitation);
                    } else {
                        callback.onResult(null);
                    }
                })
                .addOnFailureListener(e -> callback.onResult(null));
    }

    // Cancel invitation
    public void cancelInvitation(String invitationId, AuthCallback<Boolean> callback) {
        db.collection(INVITATIONS_COLLECTION)
                .document(invitationId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onResult(true))
                .addOnFailureListener(e -> callback.onResult(false));
    }

    // Get all alliances
    public Task<QuerySnapshot> getAllAlliances() {
        return db.collection(ALLIANCES_COLLECTION).get();
    }
}
