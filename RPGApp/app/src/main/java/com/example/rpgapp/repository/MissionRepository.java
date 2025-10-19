package com.example.rpgapp.repository;

import android.content.Context;
import com.example.rpgapp.model.Mission;
import com.example.rpgapp.model.User;
import com.example.rpgapp.callback.AuthCallback;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class MissionRepository {
    private final CollectionReference missionsRef;
    private final UserRepository userRepository;
    private final Context context;

    public MissionRepository(Context context) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        missionsRef = db.collection("missions");
        this.context = context;
        this.userRepository = new UserRepository(context);
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
     * Creates a mission with proper XP calculation based on user level and daily limits.
     */
    public Task<DocumentReference> createMissionWithXPCalculation(Mission mission) {
        // Create a Task that will complete when we're done
        com.google.android.gms.tasks.TaskCompletionSource<DocumentReference> taskSource =
            new com.google.android.gms.tasks.TaskCompletionSource<>();

        // First, get the user's current level
        userRepository.getUserById(mission.getUserId(), new AuthCallback<User>() {
            @Override
            public void onResult(User user) {
                int userLevel = (user != null) ? user.getLevel() : 1;

                // Set user level in mission
                mission.setUserLevel(userLevel);

                // Now get today's missions to calculate limits
                getTodaysMissions(mission.getUserId()).addOnCompleteListener(task -> {
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

                        // Calculate XP for new mission based on counts and user level
                        int calculatedXP = mission.calculateTotalXP(veryEasyNormalCount, easyImportantCount, hardExtremelyImportantCount, specialCount);
                        mission.setCalculatedTotalXP(calculatedXP);

                        // Create the mission with calculated XP
                        missionsRef.add(mission).addOnCompleteListener(createTask -> {
                            if (createTask.isSuccessful()) {
                                taskSource.setResult(createTask.getResult());
                            } else {
                                taskSource.setException(createTask.getException());
                            }
                        });
                    } else {
                        // If we can't get today's missions, use default XP calculation
                        int defaultXP = mission.calculateTotalXPForLevel(userLevel);
                        mission.setCalculatedTotalXP(defaultXP);

                        missionsRef.add(mission).addOnCompleteListener(createTask -> {
                            if (createTask.isSuccessful()) {
                                taskSource.setResult(createTask.getResult());
                            } else {
                                taskSource.setException(createTask.getException());
                            }
                        });
                    }
                });
            }
        });

        return taskSource.getTask();
    }

    /**
     * Updates a mission with proper XP recalculation based on user level.
     */
    public Task<Void> updateMissionWithXPCalculation(String missionId, Mission mission) {
        // Create a Task that will complete when we're done
        com.google.android.gms.tasks.TaskCompletionSource<Void> taskSource =
            new com.google.android.gms.tasks.TaskCompletionSource<>();

        // Get the user's current level
        userRepository.getUserById(mission.getUserId(), new AuthCallback<User>() {
            @Override
            public void onResult(User user) {
                int userLevel = (user != null) ? user.getLevel() : mission.getUserLevel(); // Keep old level if user not found

                // Update user level in mission
                mission.setUserLevel(userLevel);

                // Recalculate XP based on new difficulty/importance and current user level
                int newXP = mission.calculateTotalXPForLevel(userLevel);
                mission.setCalculatedTotalXP(newXP);

                // Update the mission
                missionsRef.document(missionId).set(mission).addOnCompleteListener(updateTask -> {
                    if (updateTask.isSuccessful()) {
                        taskSource.setResult(null);
                    } else {
                        taskSource.setException(updateTask.getException());
                    }
                });
            }
        });

        return taskSource.getTask();
    }
}
