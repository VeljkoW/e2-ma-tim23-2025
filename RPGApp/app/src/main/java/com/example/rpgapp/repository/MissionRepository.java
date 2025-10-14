package com.example.rpgapp.repository;

import com.example.rpgapp.model.Mission;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.List;

public class MissionRepository {
    private final CollectionReference missionsRef;

    public MissionRepository() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        missionsRef = db.collection("missions");
    }

    /**
     * Creates a new mission.
     */
    public Task<DocumentReference> createMission(Mission mission) {
        return missionsRef.add(mission);
    }

    /**
     * Retrieves a mission by its ID.
     */
    public Task<Mission> getMissionById(String id) {
        return missionsRef.document(id).get().continueWith(task -> {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                return task.getResult().toObject(Mission.class);
            }
            return null;
        });
    }

    /**
     * Retrieves all missions.
     */
    public Task<QuerySnapshot> getAllMissions() {
        return missionsRef.get();
    }

    /**
     * Updates a mission.
     */
    public Task<Void> updateMission(String id, Mission mission) {
        return missionsRef.document(id).set(mission);
    }

    /**
     * Deletes a mission by ID.
     */
    public Task<Void> deleteMission(String id) {
        return missionsRef.document(id).delete();
    }

    /**
     * Marks a mission as completed.
     */
    public Task<Void> completeMission(String missionId) {
        java.util.Date now = new java.util.Date();
        return missionsRef.document(missionId).update(
            "status", Mission.Status.COMPLETED,
            "finalizationDateTime", now
        );
    }

    /**
     * Marks a mission as failed.
     */
    public Task<Void> failMission(String missionId) {
        java.util.Date now = new java.util.Date();
        return missionsRef.document(missionId).update(
            "status", Mission.Status.FAILED,
            "finalizationDateTime", now
        );
    }

    /**
     * Cancels a mission.
     */
    public Task<Void> cancelMission(String missionId) {
        java.util.Date now = new java.util.Date();
        return missionsRef.document(missionId).update(
            "status", Mission.Status.CANCELLED,
            "finalizationDateTime", now
        );
    }

    /**
     * Reactivates a mission (sets status to ACTIVE, clears finalizationDateTime).
     */
    public Task<Void> activateMission(String missionId) {
        return missionsRef.document(missionId).update(
            "status", Mission.Status.ACTIVE,
            "finalizationDateTime", null
        );
    }

    /**
     * Gets missions created today for a specific user to calculate daily limits.
     */
    public Task<QuerySnapshot> getTodaysMissions(String userId) {
        // Get start and end of today
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        java.util.Date startOfDay = cal.getTime();

        cal.add(java.util.Calendar.DAY_OF_MONTH, 1);
        java.util.Date startOfNextDay = cal.getTime();

        return missionsRef
            .whereEqualTo("userId", userId)
            .whereGreaterThanOrEqualTo("createDateTime", startOfDay)
            .whereLessThan("createDateTime", startOfNextDay)
            .get();
    }

    /**
     * Creates a mission with proper XP calculation based on daily limits.
     */
    public Task<DocumentReference> createMissionWithXPCalculation(Mission mission) {
        return getTodaysMissions(mission.getUserId()).continueWithTask(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                // Count today's missions by type
                int veryEasyNormalCount = 0;
                int easyImportantCount = 0;
                int hardExtremelyImportantCount = 0;
                int specialCount = 0;

                for (com.google.firebase.firestore.DocumentSnapshot doc : task.getResult().getDocuments()) {
                    Mission existingMission = doc.toObject(Mission.class);
                    if (existingMission != null) {
                        Mission.Difficulty diff = existingMission.getDifficulty();
                        Mission.Importance imp = existingMission.getImportance();

                        if (diff != null && imp != null) {
                            // Count special importance missions (regardless of difficulty)
                            if (imp == Mission.Importance.SPECIAL) {
                                specialCount++;
                            }

                            // Count specific combinations
                            if (diff == Mission.Difficulty.VERY_EASY && imp == Mission.Importance.NORMAL) {
                                veryEasyNormalCount++;
                            }
                            if (diff == Mission.Difficulty.EASY && imp == Mission.Importance.IMPORTANT) {
                                easyImportantCount++;
                            }
                            if (diff == Mission.Difficulty.HARD && imp == Mission.Importance.EXTREMELY_IMPORTANT) {
                                hardExtremelyImportantCount++;
                            }
                        }
                    }
                }

                // Calculate XP for new mission based on counts
                int calculatedXP = mission.calculateTotalXP(veryEasyNormalCount, easyImportantCount, hardExtremelyImportantCount, specialCount);
                mission.setCalculatedTotalXP(calculatedXP);

                // Create the mission with calculated XP
                return missionsRef.add(mission);
            } else {
                // If we can't get today's missions, use default XP calculation
                return missionsRef.add(mission);
            }
        });
    }
}
