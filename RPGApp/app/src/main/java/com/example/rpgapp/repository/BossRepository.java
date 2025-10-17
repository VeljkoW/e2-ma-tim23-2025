package com.example.rpgapp.repository;

import com.example.rpgapp.model.Boss;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.Date;

public class BossRepository {
    private final CollectionReference bossesRef;

    public BossRepository() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        bossesRef = db.collection("bosses");
    }

    /**
     * Creates a new boss.
     */
    public Task<DocumentReference> createBoss(Boss boss) {
        return bossesRef.add(boss);
    }

    /**
     * Retrieves a boss by its ID.
     */
    public Task<Boss> getBossById(String id) {
        return bossesRef.document(id).get().continueWith(task -> {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                Boss boss = task.getResult().toObject(Boss.class);
                if (boss != null) {
                    boss.setId(task.getResult().getId()); // Set the document ID
                }
                return boss;
            }
            return null;
        });
    }

    /**
     * Retrieves all bosses.
     */
    public Task<QuerySnapshot> getAllBosses() {
        return bossesRef.get();
    }

    /**
     * Updates a boss.
     */
    public Task<Void> updateBoss(String id, Boss boss) {
        return bossesRef.document(id).set(boss);
    }

    /**
     * Deletes a boss by ID.
     */
    public Task<Void> deleteBoss(String id) {
        return bossesRef.document(id).delete();
    }


    /**
     * Defeats a boss (sets alive to false and dateOfFinishing).
     */
    public Task<Void> defeatBoss(String bossId) {
        Date now = new Date();
        return bossesRef.document(bossId).update(
            "alive", false,
            "dateOfFinishing", now
        );
    }

    /**
     * Resets the number of attacks for a boss (for new fight session).
     */
    public Task<Void> resetBossAttacks(String bossId) {
        return bossesRef.document(bossId).update("numberOfAttacks", 0);
    }

    /**
     * Updates the boss dodge chance based on unsolved missions count.
     */
    public Task<Void> updateBossDodgeChance(String bossId, int unsolvedMissionsCount) {
        double dodgeChance = Math.min(0.8, unsolvedMissionsCount * 0.05);
        return bossesRef.document(bossId).update("chanceTododge", dodgeChance);
    }

    /**
     * Gets all bosses for a specific user ID
     */
    public Task<QuerySnapshot> getBossesByUserId(String userId) {
        return bossesRef.whereEqualTo("userId", userId).get();
    }

    /**
     * Gets all alive bosses for a specific user ID
     */
    public Task<QuerySnapshot> getAliveBossesByUserId(String userId) {
        return bossesRef.whereEqualTo("userId", userId)
                .whereEqualTo("alive", true)
                .get();
    }

    /**
     * Gets the most recent boss for a user (by creation date)
     */
    public Task<QuerySnapshot> getNewestBossForUser(String userId) {
        return bossesRef.whereEqualTo("userId", userId)
                       .orderBy("dateOfCreation", com.google.firebase.firestore.Query.Direction.DESCENDING)
                       .limit(1)
                       .get();
    }
}
