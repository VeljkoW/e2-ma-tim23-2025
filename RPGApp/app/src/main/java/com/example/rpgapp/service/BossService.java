package com.example.rpgapp.service;

import android.content.Context;
import android.util.Log;
import com.example.rpgapp.model.Boss;
import com.example.rpgapp.model.Mission;
import com.example.rpgapp.repository.BossRepository;
import com.example.rpgapp.repository.MissionRepository;
import com.example.rpgapp.repository.UserRepository;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.Random;

public class BossService {
    private static final String TAG = "BossService";

    private final BossRepository bossRepository;
    private final MissionRepository missionRepository;
    private final UserRepository userRepository;
    private final Random random;

    public BossService(Context context) {
        Log.d(TAG, "BossService constructor called");

        try {
            this.bossRepository = new BossRepository();
            this.missionRepository = new MissionRepository(context);
            this.userRepository = new UserRepository(context);
            this.random = new Random();

            Log.d(TAG, "BossService successfully initialized");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing BossService: " + e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Creates a boss with appropriate stats based on user's boss history
     * First boss: HP=200, Level=1, Coins=200, dodge based on all missions since registration
     * Subsequent bosses: HP=(oldHP*2)+(oldHP/2), Level=oldLevel+1, Coins=oldCoins+20%, dodge based on missions since last boss
     */
    public Task<String> createBossForUser(String userId) {
        Log.d(TAG, "Creating boss for user: " + userId);

        return getNewestBossForUser(userId).continueWithTask(task -> {
            if (task.isSuccessful()) {
                Boss lastBoss = task.getResult();

                if (lastBoss == null) {
                    // First boss for this user
                    Log.d(TAG, "Creating first boss for user: " + userId);
                    return createFirstBoss(userId);
                } else {
                    // Subsequent boss
                    Log.d(TAG, "Creating subsequent boss for user: " + userId + ", previous boss level: " + lastBoss.getLevel());
                    return createSubsequentBoss(userId, lastBoss);
                }
            } else {
                Log.e(TAG, "Failed to check existing bosses", task.getException());
                throw new RuntimeException("Failed to check existing bosses");
            }
        });
    }

    /**
     * Creates the first boss for a user
     */
    private Task<String> createFirstBoss(String userId) {
        return getUserRegistrationDate(userId).continueWithTask(task -> {
            if (task.isSuccessful()) {
                java.util.Date registrationDate = task.getResult();
                java.util.Date now = new java.util.Date();

                return calculateDodgeChanceForPeriod(userId, registrationDate, now).continueWithTask(dodgeTask -> {
                    if (dodgeTask.isSuccessful()) {
                        double dodgeChance = dodgeTask.getResult();

                        Boss boss = new Boss(userId, 1, 200, 200);
                        boss.setChanceTododge(dodgeChance);
                        boss.setStartingChanceToDodge(dodgeChance); // Set the starting chance to dodge

                        return bossRepository.createBoss(boss).continueWith(createTask -> {
                            if (createTask.isSuccessful()) {
                                String bossId = createTask.getResult().getId();
                                Log.d(TAG, "First boss created successfully with ID: " + bossId + ", dodge chance: " + dodgeChance);
                                return bossId;
                            } else {
                                Log.e(TAG, "Failed to create first boss", createTask.getException());
                                throw new RuntimeException("Failed to create first boss");
                            }
                        });
                    } else {
                        throw new RuntimeException("Failed to calculate dodge chance for first boss");
                    }
                });
            } else {
                throw new RuntimeException("Failed to get user registration date");
            }
        });
    }

    /**
     * Creates a subsequent boss based on the last boss
     */
    private Task<String> createSubsequentBoss(String userId, Boss lastBoss) {
        java.util.Date now = new java.util.Date();

        return calculateDodgeChanceForPeriod(userId, lastBoss.getDateOfCreation(), now).continueWithTask(task -> {
            if (task.isSuccessful()) {
                double dodgeChance = task.getResult();

                // Calculate new stats based on last boss - use STARTING HP for correct formula
                // Formula: newHP = (oldStartingHP * 2) + (oldStartingHP / 2) = oldStartingHP * 2.5
                // This ensures: 200 -> 500 -> 1250 -> 3125 progression
                int newHp = (lastBoss.getStartingHp() * 2) + (lastBoss.getStartingHp() / 2);
                int newLevel = lastBoss.getLevel() + 1;
                int newCoins = (int) (lastBoss.getCoinReward() * 1.2); // +20%

                Boss boss = new Boss(userId, newLevel, newHp, newCoins);
                boss.setChanceTododge(dodgeChance);
                boss.setStartingChanceToDodge(dodgeChance); // Set the starting chance to dodge

                return bossRepository.createBoss(boss).continueWith(createTask -> {
                    if (createTask.isSuccessful()) {
                        String bossId = createTask.getResult().getId();
                        Log.d(TAG, "Subsequent boss created successfully with ID: " + bossId +
                                ", level: " + newLevel + ", HP: " + newHp + " (from " + lastBoss.getStartingHp() + ")" +
                                ", coins: " + newCoins + ", dodge: " + dodgeChance);
                        return bossId;
                    } else {
                        Log.e(TAG, "Failed to create subsequent boss", createTask.getException());
                        throw new RuntimeException("Failed to create subsequent boss");
                    }
                });
            } else {
                throw new RuntimeException("Failed to calculate dodge chance for subsequent boss");
            }
        });
    }

    /**
     * Gets the newest boss for a user by creation date
     */
    private Task<Boss> getNewestBossForUser(String userId) {
        return bossRepository.getBossesByUserId(userId).continueWith(task -> {
            if (task.isSuccessful()) {
                Boss newestBoss = null;
                java.util.Date latestDate = null;

                for (com.google.firebase.firestore.DocumentSnapshot doc : task.getResult().getDocuments()) {
                    Boss boss = doc.toObject(Boss.class);
                    if (boss != null && boss.getDateOfCreation() != null) {
                        // Set the document ID from the DocumentSnapshot
                        boss.setId(doc.getId());

                        if (latestDate == null || boss.getDateOfCreation().after(latestDate)) {
                            latestDate = boss.getDateOfCreation();
                            newestBoss = boss;
                        }
                    }
                }

                Log.d(TAG, "Found newest boss for user " + userId + ": " +
                      (newestBoss != null ? "Level " + newestBoss.getLevel() + " with ID " + newestBoss.getId() : "None"));
                return newestBoss;
            } else {
                Log.e(TAG, "Failed to get bosses for user", task.getException());
                throw new RuntimeException("Failed to get bosses for user");
            }
        });
    }

    /**
     * Gets the oldest alive boss for a user by creation date
     * Returns the boss that is alive (not finished) and has the earliest creation date
     */
    public Task<Boss> getOldestAliveBossForUser(String userId) {
        return bossRepository.getBossesByUserId(userId).continueWith(task -> {
            if (task.isSuccessful()) {
                Boss oldestAliveBoss = null;
                java.util.Date earliestDate = null;

                for (com.google.firebase.firestore.DocumentSnapshot doc : task.getResult().getDocuments()) {
                    Boss boss = doc.toObject(Boss.class);
                    if (boss != null && boss.getDateOfCreation() != null) {
                        // Set the document ID from the DocumentSnapshot
                        boss.setId(doc.getId());

                        // Check if boss is alive (not finished and alive status is true)
                        if (boss.isAlive() && boss.getDateOfFinishing() == null) {
                            if (earliestDate == null || boss.getDateOfCreation().before(earliestDate)) {
                                earliestDate = boss.getDateOfCreation();
                                oldestAliveBoss = boss;
                            }
                        }
                    }
                }

                Log.d(TAG, "Found oldest alive boss for user " + userId + ": " +
                      (oldestAliveBoss != null ? "Level " + oldestAliveBoss.getLevel() + " with ID " + oldestAliveBoss.getId() : "None"));
                return oldestAliveBoss;
            } else {
                Log.e(TAG, "Failed to get bosses for user", task.getException());
                throw new RuntimeException("Failed to get bosses for user");
            }
        });
    }

    /**
     * Gets user registration date using UserRepository
     */
    private Task<java.util.Date> getUserRegistrationDate(String userId) {
        return userRepository.getUserRegistrationDate(userId);
    }

    /**
     * Calculates dodge chance based on missions between two dates
     * Percentage = (ACTIVE + FAILED missions) / total missions in period
     */
    private Task<Double> calculateDodgeChanceForPeriod(String userId, java.util.Date startDate, java.util.Date endDate) {
        return missionRepository.getAllMissions().continueWith(task -> {
            if (task.isSuccessful()) {
                int totalMissions = 0;
                int activeOrFailedMissions = 0;

                for (com.google.firebase.firestore.DocumentSnapshot doc : task.getResult().getDocuments()) {
                    Mission mission = doc.toObject(Mission.class);
                    if (mission != null && mission.getUserId().equals(userId) && mission.getCreateDateTime() != null) {
                        // Check if mission was created within the specified period
                        if (!mission.getCreateDateTime().before(startDate) && !mission.getCreateDateTime().after(endDate)) {
                            totalMissions++;
                            if (mission.getStatus() == Mission.Status.ACTIVE || mission.getStatus() == Mission.Status.FAILED) {
                                activeOrFailedMissions++;
                            }
                        }
                    }
                }

                double dodgeChance = 0.0;
                if (totalMissions > 0) {
                    dodgeChance = (double) activeOrFailedMissions / totalMissions;
                    // Cap at 80% max dodge chance
                    dodgeChance = Math.min(0.8, dodgeChance);
                }

                Log.d(TAG, "Calculated dodge chance for period: " + dodgeChance +
                        " (active/failed: " + activeOrFailedMissions + ", total: " + totalMissions + ")");
                return dodgeChance;
            } else {
                Log.e(TAG, "Failed to get missions for dodge calculation", task.getException());
                return 0.0;
            }
        });
    }

    /**
     * Attempts to hit the boss using dodge chance calculation
     * @param boss The boss to attempt to hit
     * @return true if hit successful, false if boss dodged
     */
    public boolean attemptHit(Boss boss) {
        if (boss == null || !boss.isAlive()) {
            Log.w(TAG, "Cannot attempt hit on null or dead boss");
            return false;
        }

        double randomValue = random.nextDouble();
        boolean hitSuccessful = randomValue > boss.getChanceTododge();

        Log.d(TAG, "Hit attempt: random=" + randomValue + ", dodge chance=" + boss.getChanceTododge() +
              ", result=" + (hitSuccessful ? "HIT" : "DODGED"));

        return hitSuccessful;
    }

    /**
     * Attacks the boss with user's power points as damage
     * @param bossId The ID of the boss to attack
     * @param userId The ID of the attacking user
     * @param userPowerPoints The user's power points (damage amount)
     * @return Task<AttackResult> containing the result of the attack
     */
    public Task<AttackResult> attackBoss(String bossId, String userId, int userPowerPoints) {
        Log.d(TAG, "User " + userId + " attempting to attack boss " + bossId + " with " + userPowerPoints + " power points");

        return bossRepository.getBossById(bossId).continueWithTask(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                Boss boss = task.getResult();

                // Check if boss is alive
                if (!boss.isAlive()) {
                    return Tasks.forResult(new AttackResult(false, 0, "Boss is already defeated", boss, AttackResult.ResultType.BOSS_ALREADY_DEAD));
                }

                // Check if user has remaining attacks
                if (boss.getNumberOfAttacks() <= 0) {
                    return Tasks.forResult(new AttackResult(false, 0, "No attacks remaining", boss, AttackResult.ResultType.NO_ATTACKS_REMAINING));
                }

                // Attempt to hit the boss
                boolean hitSuccessful = attemptHit(boss);
                int damageDealt = 0;

                if (hitSuccessful) {
                    damageDealt = userPowerPoints;
                    int newHp = Math.max(0, boss.getHp() - damageDealt);
                    boss.setHp(newHp);
                    Log.d(TAG, "Hit successful! Dealt " + damageDealt + " damage. Boss HP: " + boss.getHp() + " -> " + newHp);
                } else {
                    Log.d(TAG, "Boss dodged the attack! No damage dealt.");
                }

                // Decrement number of attacks regardless of hit/miss
                boss.setNumberOfAttacks(boss.getNumberOfAttacks() - 1);
                boss.setDateOfLastFight(new java.util.Date());

                // Make variables final for lambda use
                final boolean finalHitSuccessful = hitSuccessful;
                final int finalDamageDealt = damageDealt;

                // Update boss in database
                return bossRepository.updateBoss(bossId, boss).continueWithTask(updateTask -> {
                    if (updateTask.isSuccessful()) {
                        // Check win/lose conditions
                        if (boss.getHp() <= 0) {
                            // Boss is defeated - player wins
                            boss.defeat();
                            return bossRepository.updateBoss(bossId, boss).continueWithTask(defeatTask -> {
                                if (defeatTask.isSuccessful()) {
                                    return bossWin(userId, boss).continueWith(winTask ->
                                        new AttackResult(finalHitSuccessful, finalDamageDealt,
                                            "Boss defeated! You won the fight!", boss, AttackResult.ResultType.BOSS_DEFEATED)
                                    );
                                } else {
                                    throw new RuntimeException("Failed to update defeated boss");
                                }
                            });
                        } else if (boss.getNumberOfAttacks() <= 0) {
                            // No more attacks left - boss wins
                            return bossLose(userId, boss).continueWith(loseTask ->
                                new AttackResult(finalHitSuccessful, finalDamageDealt,
                                    "No attacks remaining. Boss wins this fight!", boss, AttackResult.ResultType.BOSS_WINS)
                            );
                        } else {
                            // Attack continues - more attacks remaining
                            return Tasks.forResult(new AttackResult(finalHitSuccessful, finalDamageDealt,
                                finalHitSuccessful ?
                                    "Hit! Dealt " + finalDamageDealt + " damage. Boss HP: " + boss.getHp() :
                                    "Boss dodged your attack! No damage dealt.",
                                boss, AttackResult.ResultType.ATTACK_CONTINUES));
                        }
                    } else {
                        Log.e(TAG, "Failed to update boss after attack", updateTask.getException());
                        throw new RuntimeException("Failed to update boss after attack");
                    }
                });
            } else {
                Log.e(TAG, "Failed to get boss for attack", task.getException());
                throw new RuntimeException("Failed to load boss for attack");
            }
        });
    }

    /**
     * Handles boss victory (when boss wins the fight)
     * Resets attack count and updates dodge chance based on missions - does NOT increase HP
     */
    private Task<Void> bossLose(String userId, Boss boss) {
        Log.d(TAG, "Boss defeated user " + userId + ". Boss resets for next fight (no HP increase).");

        // Reset boss for next fight - do NOT increase HP, only new bosses get more HP
        boss.setHp(boss.getStartingHp()); // Reset to full health (same max HP)
        boss.setNumberOfAttacks(5); // Reset attacks for next fight
        boss.setDateOfLastFight(new java.util.Date());

        // Recalculate dodge chance based on missions since last fight
        java.util.Date lastFightDate = boss.getDateOfLastFight() != null ?
            boss.getDateOfLastFight() : boss.getDateOfCreation();
        java.util.Date now = new java.util.Date();

        return calculateDodgeChanceForPeriod(userId, lastFightDate, now).continueWithTask(dodgeTask -> {
            if (dodgeTask.isSuccessful()) {
                double newDodgeChance = dodgeTask.getResult();
                boss.setChanceTododge(newDodgeChance);
                // Note: We don't update startingChanceToDodge here because it should remain
                // the original value from when the boss was first created

                Log.d(TAG, "Boss reset for next fight: HP " + boss.getStartingHp() +
                      ", new dodge chance: " + newDodgeChance + ", attacks reset to 5");

                // Update boss in database with new stats
                return bossRepository.updateBoss(boss.getId(), boss);
            } else {
                Log.e(TAG, "Failed to recalculate dodge chance after boss win");
                // Still update boss with reset stats, just keep old dodge chance
                return bossRepository.updateBoss(boss.getId(), boss);
            }
        });
    }

    /**
     * Handles player victory (when player defeats boss)
     * Boss is marked as defeated and finished, and coins are awarded to the user
     */
    private Task<Void> bossWin(String userId, Boss boss) {
        Log.d(TAG, "User " + userId + " defeated boss level " + boss.getLevel() +
              ". Rewarding " + boss.getCoinReward() + " coins.");

        // Mark boss as defeated and set finish date
        boss.defeat(); // This sets alive=false and dateOfFinishing=now
        boss.setDateOfLastFight(new java.util.Date());

        // Update boss in database to reflect defeat
        return bossRepository.updateBoss(boss.getId(), boss).continueWithTask(updateTask -> {
            if (updateTask.isSuccessful()) {
                Log.d(TAG, "Boss level " + boss.getLevel() + " successfully defeated and updated in database");

                // Award coins to user for defeating the boss
                return userRepository.awardCoinsToUser(userId, boss.getCoinReward()).continueWith(coinTask -> {
                    if (coinTask.isSuccessful()) {
                        Log.d(TAG, "Successfully awarded " + boss.getCoinReward() + " coins to user " + userId);
                    } else {
                        Log.e(TAG, "Failed to award coins to user", coinTask.getException());
                        // Don't fail the entire boss defeat if coin award fails
                    }
                    return null; // Return Void
                });
            } else {
                Log.e(TAG, "Failed to update defeated boss in database", updateTask.getException());
                throw new RuntimeException("Failed to update defeated boss");
            }
        });
    }

    /**
     * Gets a boss by its ID
     */
    public Task<Boss> getBossById(String bossId) {
        return bossRepository.getBossById(bossId).continueWith(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                Boss boss = task.getResult();
                boss.setId(bossId); // Ensure the ID is set
                return boss;
            } else {
                Log.e(TAG, "Failed to get boss by ID: " + bossId, task.getException());
                return null;
            }
        });
    }

    /**
     * Gets all alive bosses for a specific user
     */
    public Task<QuerySnapshot> getAliveBossesByUserId(String userId) {
        return bossRepository.getAliveBossesByUserId(userId);
    }

    /**
     * Result class for attack operations
     */
    public static class AttackResult {
        private final boolean hitSuccessful;
        private final int damageDealt;
        private final String message;
        private final Boss boss;
        private final ResultType resultType;

        public AttackResult(boolean hitSuccessful, int damageDealt, String message, Boss boss, ResultType resultType) {
            this.hitSuccessful = hitSuccessful;
            this.damageDealt = damageDealt;
            this.message = message;
            this.boss = boss;
            this.resultType = resultType;
        }

        public boolean isHitSuccessful() { return hitSuccessful; }
        public int getDamageDealt() { return damageDealt; }
        public String getMessage() { return message; }
        public Boss getBoss() { return boss; }
        public ResultType getResultType() { return resultType; }

        public boolean isFightOver() {
            return resultType == ResultType.BOSS_DEFEATED ||
                   resultType == ResultType.BOSS_WINS ||
                   resultType == ResultType.BOSS_ALREADY_DEAD ||
                   resultType == ResultType.NO_ATTACKS_REMAINING;
        }

        public enum ResultType {
            ATTACK_CONTINUES,
            BOSS_DEFEATED,
            BOSS_WINS,
            BOSS_ALREADY_DEAD,
            NO_ATTACKS_REMAINING
        }
    }
}
