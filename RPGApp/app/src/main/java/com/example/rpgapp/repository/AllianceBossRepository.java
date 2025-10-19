package com.example.rpgapp.repository;

import com.example.rpgapp.model.AllianceBoss;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class AllianceBossRepository {
    private final CollectionReference allianceBossesRef;

    public AllianceBossRepository() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        allianceBossesRef = db.collection("allianceBosses");
    }

    /**
     * Creates a new alliance boss.
     */
    public Task<DocumentReference> createAllianceBoss(AllianceBoss allianceBoss) {
        return allianceBossesRef.add(allianceBoss);
    }

    /**
     * Retrieves an alliance boss by its ID.
     */
    public Task<AllianceBoss> getAllianceBossById(String id) {
        return allianceBossesRef.document(id).get().continueWith(task -> {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                AllianceBoss allianceBoss = task.getResult().toObject(AllianceBoss.class);
                if (allianceBoss != null) {
                    allianceBoss.setId(task.getResult().getId()); // Set the document ID
                }
                return allianceBoss;
            }
            return null;
        });
    }

    /**
     * Retrieves all alliance bosses.
     */
    public Task<QuerySnapshot> getAllAllianceBosses() {
        return allianceBossesRef.get();
    }

    /**
     * Updates an alliance boss.
     */
    public Task<Void> updateAllianceBoss(String id, AllianceBoss allianceBoss) {
        return allianceBossesRef.document(id).set(allianceBoss);
    }

    /**
     * Deletes an alliance boss by ID.
     */
    public Task<Void> deleteAllianceBoss(String id) {
        return allianceBossesRef.document(id).delete();
    }

    /**
     * Gets all alliance bosses for a specific alliance ID
     */
    public Task<QuerySnapshot> getAllianceBossesByAllianceId(String allianceId) {
        return allianceBossesRef.whereEqualTo("allianceId", allianceId).get();
    }

    /**
     * Gets all alive alliance bosses for a specific alliance ID
     */
    public Task<QuerySnapshot> getAliveAllianceBossesByAllianceId(String allianceId) {
        return allianceBossesRef.whereEqualTo("allianceId", allianceId)
                .whereEqualTo("status", AllianceBoss.Status.ALIVE)
                .get();
    }

    /**
     * Gets the most recent alliance boss for an alliance (by creation date)
     */
    public Task<QuerySnapshot> getNewestAllianceBossForAlliance(String allianceId) {
        return allianceBossesRef.whereEqualTo("allianceId", allianceId)
                .orderBy("dateOfCreation", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get();
    }

    /**
     * Gets all alive alliance bosses across all alliances
     */
    public Task<QuerySnapshot> getAllAliveAllianceBosses() {
        return allianceBossesRef.whereEqualTo("status", AllianceBoss.Status.ALIVE).get();
    }

    /**
     * Sets alliance boss status to DEAD
     */
    public Task<Void> killAllianceBoss(String allianceBossId) {
        return allianceBossesRef.document(allianceBossId).update(
            "status", AllianceBoss.Status.DEAD,
            "dateOfLastDyingOrFailing", new java.util.Date()
        );
    }

    /**
     * Sets alliance boss status to FAILED
     */
    public Task<Void> failAllianceBoss(String allianceBossId) {
        return allianceBossesRef.document(allianceBossId).update(
            "status", AllianceBoss.Status.FAILED,
            "dateOfLastDyingOrFailing", new java.util.Date()
        );
    }
}
