package com.example.rpgapp.service;

import android.content.Context;
import android.util.Log;
import com.example.rpgapp.model.Alliance;
import com.example.rpgapp.model.AllianceBoss;
import com.example.rpgapp.repository.AllianceBossRepository;
import com.example.rpgapp.repository.AllianceRepository;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

public class AllianceBossService {
    private static final String TAG = "AllianceBossService";
    private static final int DEFAULT_ALLIANCE_BOSS_HP = 1000; // Default HP for alliance bosses

    private final AllianceBossRepository allianceBossRepository;
    private final AllianceRepository allianceRepository;

    public AllianceBossService(Context context) {
        Log.d(TAG, "AllianceBossService constructor called");

        try {
            this.allianceBossRepository = new AllianceBossRepository();
            this.allianceRepository = new AllianceRepository();

            Log.d(TAG, "AllianceBossService successfully initialized");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing AllianceBossService: " + e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Creates an alliance boss for the user's alliance
     * Validates:
     * 1. User is a member of an alliance
     * 2. User is the leader of that alliance
     * 3. No alive alliance boss already exists for that alliance
     */
    public Task<CreateAllianceBossResult> createAllianceBossForUser(String userId) {
        Log.d(TAG, "Attempting to create alliance boss for user: " + userId);

        return getUserAlliance(userId).continueWithTask(task -> {
            if (task.isSuccessful()) {
                Alliance userAlliance = task.getResult();

                if (userAlliance == null) {
                    Log.d(TAG, "User " + userId + " is not a member of any alliance");
                    return Tasks.forResult(new CreateAllianceBossResult(
                        false, null, "You must be a member of an alliance to create an alliance boss.",
                        CreateAllianceBossResult.ResultType.NOT_ALLIANCE_MEMBER));
                }

                // Check if user is the leader
                if (!userAlliance.getLeaderId().equals(userId)) {
                    Log.d(TAG, "User " + userId + " is not the leader of alliance " + userAlliance.getId());
                    return Tasks.forResult(new CreateAllianceBossResult(
                        false, null, "Only alliance leaders can create new alliance bosses.",
                        CreateAllianceBossResult.ResultType.NOT_ALLIANCE_LEADER));
                }

                // Check if there's already an alive alliance boss
                return checkForExistingAliveBoss(userAlliance.getId()).continueWithTask(checkTask -> {
                    if (checkTask.isSuccessful()) {
                        AllianceBoss existingBoss = checkTask.getResult();

                        if (existingBoss != null) {
                            Log.d(TAG, "Alliance " + userAlliance.getId() + " already has an alive boss with ID: " + existingBoss.getId());
                            return Tasks.forResult(new CreateAllianceBossResult(
                                false, existingBoss, "Your alliance already has an active boss. Defeat it before creating a new one.",
                                CreateAllianceBossResult.ResultType.ALIVE_BOSS_EXISTS));
                        }

                        // All validations passed, create the alliance boss
                        return createNewAllianceBoss(userAlliance.getId()).continueWith(createTask -> {
                            if (createTask.isSuccessful()) {
                                AllianceBoss newBoss = createTask.getResult();
                                Log.d(TAG, "Successfully created alliance boss with ID: " + newBoss.getId() + " for alliance: " + userAlliance.getId());
                                return new CreateAllianceBossResult(
                                    true, newBoss, "Alliance boss created successfully!",
                                    CreateAllianceBossResult.ResultType.SUCCESS);
                            } else {
                                Log.e(TAG, "Failed to create alliance boss", createTask.getException());
                                return new CreateAllianceBossResult(
                                    false, null, "Failed to create alliance boss. Please try again.",
                                    CreateAllianceBossResult.ResultType.CREATION_FAILED);
                            }
                        });
                    } else {
                        Log.e(TAG, "Failed to check for existing alive boss", checkTask.getException());
                        return Tasks.forResult(new CreateAllianceBossResult(
                            false, null, "Failed to check existing bosses. Please try again.",
                            CreateAllianceBossResult.ResultType.CREATION_FAILED));
                    }
                });
            } else {
                Log.e(TAG, "Failed to get user alliance", task.getException());
                return Tasks.forResult(new CreateAllianceBossResult(
                    false, null, "Failed to check alliance membership. Please try again.",
                    CreateAllianceBossResult.ResultType.CREATION_FAILED));
            }
        });
    }

    /**
     * Gets the alliance that the user is a member of
     */
    private Task<Alliance> getUserAlliance(String userId) {
        return getFirestoreAlliances().continueWith(task -> {
            if (task.isSuccessful()) {
                for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                    Alliance alliance = doc.toObject(Alliance.class);
                    if (alliance != null && alliance.getMemberIds() != null &&
                        alliance.getMemberIds().contains(userId)) {
                        alliance.setId(doc.getId()); // Set the document ID
                        Log.d(TAG, "Found user " + userId + " in alliance: " + alliance.getId() + " (" + alliance.getName() + ")");
                        return alliance;
                    }
                }
                Log.d(TAG, "User " + userId + " is not a member of any alliance");
                return null;
            } else {
                Log.e(TAG, "Failed to get alliances", task.getException());
                throw new RuntimeException("Failed to get alliances");
            }
        });
    }

    /**
     * Gets all alliances using direct Firestore call
     */
    private Task<QuerySnapshot> getFirestoreAlliances() {
        return com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("alliances").get();
    }

    /**
     * Checks if there's an existing alive alliance boss for the given alliance
     */
    private Task<AllianceBoss> checkForExistingAliveBoss(String allianceId) {
        return allianceBossRepository.getAliveAllianceBossesByAllianceId(allianceId).continueWith(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();
                if (!querySnapshot.isEmpty()) {
                    // Found an alive boss
                    DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                    AllianceBoss aliveBoss = doc.toObject(AllianceBoss.class);
                    if (aliveBoss != null) {
                        aliveBoss.setId(doc.getId()); // Set the document ID
                        Log.d(TAG, "Found existing alive alliance boss for alliance " + allianceId + ": " + aliveBoss.getId());
                        return aliveBoss;
                    }
                }
                Log.d(TAG, "No alive alliance boss found for alliance: " + allianceId);
                return null;
            } else {
                Log.e(TAG, "Failed to check for existing alive boss", task.getException());
                throw new RuntimeException("Failed to check for existing alive boss");
            }
        });
    }

    /**
     * Creates a new alliance boss for the given alliance
     */
    private Task<AllianceBoss> createNewAllianceBoss(String allianceId) {
        // First get the alliance to count its members
        return getAllianceById(allianceId).continueWithTask(task -> {
            if (task.isSuccessful()) {
                Alliance alliance = task.getResult();
                if (alliance == null) {
                    Log.e(TAG, "Alliance not found when creating boss for alliance: " + allianceId);
                    throw new RuntimeException("Alliance not found");
                }

                // Calculate HP based on member count * 100
                int memberCount = alliance.getMemberIds() != null ? alliance.getMemberIds().size() : 1;
                int calculatedHp = memberCount * 100;

                Log.d(TAG, "Creating alliance boss with HP: " + calculatedHp + " (based on " + memberCount + " members)");

                AllianceBoss newBoss = new AllianceBoss(allianceId, calculatedHp);

                return allianceBossRepository.createAllianceBoss(newBoss).continueWith(createTask -> {
                    if (createTask.isSuccessful()) {
                        String bossId = createTask.getResult().getId();
                        newBoss.setId(bossId);
                        Log.d(TAG, "Created new alliance boss with ID: " + bossId + " for alliance: " + allianceId + " with HP: " + calculatedHp);
                        return newBoss;
                    } else {
                        Log.e(TAG, "Failed to create alliance boss in database", createTask.getException());
                        throw new RuntimeException("Failed to create alliance boss in database");
                    }
                });
            } else {
                Log.e(TAG, "Failed to get alliance for HP calculation", task.getException());
                throw new RuntimeException("Failed to get alliance for HP calculation");
            }
        });
    }

    /**
     * Gets an alliance by its ID
     */
    private Task<Alliance> getAllianceById(String allianceId) {
        return com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("alliances")
                .document(allianceId)
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        Alliance alliance = task.getResult().toObject(Alliance.class);
                        if (alliance != null) {
                            alliance.setId(task.getResult().getId());
                        }
                        return alliance;
                    } else {
                        Log.e(TAG, "Failed to get alliance by ID: " + allianceId);
                        return null;
                    }
                });
    }

    /**
     * Gets an alliance boss by its ID
     */
    public Task<AllianceBoss> getAllianceBossById(String allianceBossId) {
        return allianceBossRepository.getAllianceBossById(allianceBossId).continueWith(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                AllianceBoss boss = task.getResult();
                boss.setId(allianceBossId); // Ensure the ID is set
                return boss;
            } else {
                Log.e(TAG, "Failed to get alliance boss by ID: " + allianceBossId, task.getException());
                return null;
            }
        });
    }

    /**
     * Gets all alive alliance bosses for a specific alliance
     */
    public Task<QuerySnapshot> getAliveAllianceBossesByAllianceId(String allianceId) {
        return allianceBossRepository.getAliveAllianceBossesByAllianceId(allianceId);
    }

    /**
     * Damages alliance boss by 2 HP when a user makes a shop purchase
     * Checks if user is in an alliance with an alive boss and damages it
     */
    public void damageAllianceBossFromShopPurchase(String userId) {
        Log.d(TAG, "damageAllianceBossFromShopPurchase: Checking for user " + userId);

        // Step 1: Find if user is in any alliance
        com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("alliances")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    String userAllianceId = null;

                    // Search through all alliances to find the one containing this user
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        try {
                            java.util.List<String> memberIds = (java.util.List<String>) doc.get("memberIds");
                            if (memberIds != null && memberIds.contains(userId)) {
                                userAllianceId = doc.getId();
                                Log.d(TAG, "damageAllianceBossFromShopPurchase: User found in alliance " + userAllianceId);
                                break;
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "damageAllianceBossFromShopPurchase: Error checking alliance " + doc.getId(), e);
                        }
                    }

                    if (userAllianceId != null) {
                        // Step 2: Check if this alliance has an alive boss
                        checkForAliveBossAndDamageFromShop(userAllianceId);
                    } else {
                        Log.d(TAG, "damageAllianceBossFromShopPurchase: User is not in any alliance");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "damageAllianceBossFromShopPurchase: Failed to load alliances", e);
                });
    }

    /**
     * Checks if the alliance has an alive boss and damages it by 2 HP
     */
    private void checkForAliveBossAndDamageFromShop(String allianceId) {
        Log.d(TAG, "checkForAliveBossAndDamageFromShop: Checking alliance " + allianceId);

        allianceBossRepository.getAliveAllianceBossesByAllianceId(allianceId)
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        // Found an alive boss - damage it
                        com.google.firebase.firestore.DocumentSnapshot bossDoc = querySnapshot.getDocuments().get(0);
                        AllianceBoss boss = bossDoc.toObject(AllianceBoss.class);

                        if (boss != null && boss.isAlive()) {
                            boss.setId(bossDoc.getId());
                            damageAllianceBossFromShop(boss);
                        }
                    } else {
                        Log.d(TAG, "checkForAliveBossAndDamageFromShop: No alive boss found for alliance " + allianceId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "checkForAliveBossAndDamageFromShop: Failed to check for alive boss", e);
                });
    }

    /**
     * Damages the alliance boss by 2 HP and updates it in the database
     */
    private void damageAllianceBossFromShop(AllianceBoss boss) {
        int newHp = Math.max(0, boss.getCurrentHp() - 2); // Damage by 2, minimum 0
        int originalHp = boss.getCurrentHp();

        Log.d(TAG, "damageAllianceBossFromShop: Damaging boss " + boss.getId() + " from " + originalHp + " HP to " + newHp + " HP");

        // Update the boss HP in the database
        com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("allianceBosses")
                .document(boss.getId())
                .update("currentHp", newHp)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "damageAllianceBossFromShop: Successfully damaged alliance boss! HP: " + originalHp + " -> " + newHp);

                    // Check if boss died (HP reached 0)
                    if (newHp <= 0) {
                        killAllianceBossFromShop(boss);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "damageAllianceBossFromShop: Failed to update boss HP", e);
                });
    }

    /**
     * Marks the alliance boss as dead when its HP reaches 0
     */
    private void killAllianceBossFromShop(AllianceBoss boss) {
        Log.d(TAG, "killAllianceBossFromShop: Boss " + boss.getId() + " has been defeated!");

        com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("allianceBosses")
                .document(boss.getId())
                .update(
                    "status", AllianceBoss.Status.DEAD,
                    "dateOfLastDyingOrFailing", new java.util.Date()
                )
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "killAllianceBossFromShop: Boss marked as dead in database");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "killAllianceBossFromShop: Failed to mark boss as dead", e);
                });
    }

    /**
     * Result class for alliance boss creation operations
     */
    public static class CreateAllianceBossResult {
        private final boolean success;
        private final AllianceBoss allianceBoss;
        private final String message;
        private final ResultType resultType;

        public CreateAllianceBossResult(boolean success, AllianceBoss allianceBoss, String message, ResultType resultType) {
            this.success = success;
            this.allianceBoss = allianceBoss;
            this.message = message;
            this.resultType = resultType;
        }

        public boolean isSuccess() { return success; }
        public AllianceBoss getAllianceBoss() { return allianceBoss; }
        public String getMessage() { return message; }
        public ResultType getResultType() { return resultType; }

        public enum ResultType {
            SUCCESS,
            NOT_ALLIANCE_MEMBER,
            NOT_ALLIANCE_LEADER,
            ALIVE_BOSS_EXISTS,
            CREATION_FAILED
        }
    }
}
