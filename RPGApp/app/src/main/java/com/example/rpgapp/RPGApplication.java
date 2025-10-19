package com.example.rpgapp;

import android.app.Application;
import android.util.Log;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

public class RPGApplication extends Application
{
    private static final String TAG = "RPGApplication";

    @Override
    public void onCreate()
    {
        super.onCreate();
        
        Log.d(TAG, "RPGApplication onCreate() - Simple Firebase initialization");

        try
        {
            // Simple Firebase initialization using google-services.json
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this);
                Log.d(TAG, "Firebase initialized successfully");
            }

            // Configure Firebase Auth to disable verification (this helps with Google Play Services issues)
            FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
            try {
                firebaseAuth.getFirebaseAuthSettings().setAppVerificationDisabledForTesting(true);
                Log.d(TAG, "App verification disabled");
            } catch (Exception e) {
                Log.w(TAG, "Could not disable app verification: " + e.getMessage());
            }

            // Simple Firestore configuration
            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .build();
            firestore.setFirestoreSettings(settings);

            Log.d(TAG, "Firebase configured successfully");

            // 🎯 ADD YOUR STARTUP CODE HERE - RUNS ONCE WHEN APP STARTS
            executeAppStartupTasks();
        }
        catch (Exception e)
        {
            Log.e(TAG, "Error initializing Firebase: " + e.getMessage(), e);
        }
    }

    /**
     * Execute tasks that should run once when the app starts
     */
    private void executeAppStartupTasks() {
        Log.d(TAG, "Executing app startup tasks...");

        // Clean up old alliance bosses (14+ days old)
        cleanupOldAllianceBosses();

        // Check for dead bosses that might need badge awarding
        checkDeadBossesForBadgeAwarding();
    }

    /**
     * Clean up alliance bosses that are older than 14 days
     * - If HP <= 0: mark as DEAD
     * - If date expired (14+ days): mark as FAILED
     */
    private void cleanupOldAllianceBosses() {
        Log.d(TAG, "Starting alliance boss cleanup...");

        // Calculate 14 days ago
        java.util.Calendar fourteenDaysAgo = java.util.Calendar.getInstance();
        fourteenDaysAgo.add(java.util.Calendar.DAY_OF_MONTH, -14);
        java.util.Date cutoffDate = fourteenDaysAgo.getTime();

        Log.d(TAG, "Checking for alliance bosses older than: " + cutoffDate);
        Log.d(TAG, "Current date: " + new java.util.Date());

        // First, let's check all ALIVE bosses to see what we have
        FirebaseFirestore.getInstance().collection("allianceBosses")
                .whereEqualTo("status", "ALIVE")
                .get()
                .addOnSuccessListener(allBossesSnapshot -> {
                    Log.d(TAG, "Total ALIVE bosses found: " + allBossesSnapshot.size());

                    for (com.google.firebase.firestore.DocumentSnapshot doc : allBossesSnapshot.getDocuments()) {
                        java.util.Date creationDate = doc.getDate("dateOfCreation");
                        Long hpLong = (Long) doc.get("currentHp");
                        int hp = hpLong != null ? hpLong.intValue() : 0;

                        boolean isOld = creationDate != null && creationDate.before(cutoffDate);
                        Log.d(TAG, "Boss " + doc.getId() + ": HP=" + hp + ", created=" + creationDate + ", isOld=" + isOld);

                        if (isOld) {
                            processOldAllianceBoss(doc);
                        }
                    }

                    if (allBossesSnapshot.isEmpty()) {
                        Log.d(TAG, "No ALIVE alliance bosses found at all");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to query alliance bosses", e);
                });
    }

    /**
     * Process a single old alliance boss and update its status
     */
    private void processOldAllianceBoss(com.google.firebase.firestore.DocumentSnapshot bossDoc) {
        String bossId = bossDoc.getId();
        Long currentHpLong = (Long) bossDoc.get("currentHp");
        int currentHp = currentHpLong != null ? currentHpLong.intValue() : 1;
        java.util.Date creationDate = bossDoc.getDate("dateOfCreation");
        String allianceId = bossDoc.getString("allianceId");

        Log.d(TAG, "Processing old boss " + bossId + " (alliance: " + allianceId + ") with HP: " + currentHp + ", created: " + creationDate);

        // Determine new status based on HP
        String newStatus;
        if (currentHp <= 0) {
            newStatus = "DEAD";
            Log.d(TAG, "Boss " + bossId + " has HP <= 0, marking as DEAD");
        } else {
            newStatus = "FAILED";
            Log.d(TAG, "Boss " + bossId + " expired after 14 days with HP > 0, marking as FAILED");
        }

        // Update the boss status in database
        java.util.Date now = new java.util.Date();
        Log.d(TAG, "Updating boss " + bossId + " from ALIVE to " + newStatus + " at " + now);

        FirebaseFirestore.getInstance().collection("allianceBosses")
                .document(bossId)
                .update(
                    "status", newStatus,
                    "dateOfLastDyingOrFailing", now
                )
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Successfully updated boss " + bossId + " status to " + newStatus);

                    // Award badges if boss is dead
                    if ("DEAD".equals(newStatus)) {
                        awardBadgesForDeadBoss(bossId, allianceId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to update boss " + bossId + " status to " + newStatus, e);
                });
    }

    /**
     * Award special boss badges to all alliance members when a boss is defeated
     */
    private void awardBadgesForDeadBoss(String bossId, String allianceId) {
        Log.d(TAG, "Awarding badges for defeated boss " + bossId + " in alliance " + allianceId);

        if (allianceId == null) {
            Log.w(TAG, "No alliance ID found for boss " + bossId + ", cannot award badges");
            return;
        }

        // First, get all members of the alliance
        FirebaseFirestore.getInstance().collection("alliances")
                .document(allianceId)
                .get()
                .addOnSuccessListener(allianceDoc -> {
                    if (!allianceDoc.exists()) {
                        Log.w(TAG, "Alliance " + allianceId + " not found for boss " + bossId);
                        return;
                    }

                    java.util.List<String> memberIds = (java.util.List<String>) allianceDoc.get("memberIds");
                    if (memberIds == null || memberIds.isEmpty()) {
                        Log.w(TAG, "No members found in alliance " + allianceId);
                        return;
                    }

                    Log.d(TAG, "Found " + memberIds.size() + " members in alliance " + allianceId);

                    // Award badge to each member
                    for (String memberId : memberIds) {
                        awardSpecialBossBadge(memberId, bossId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get alliance members for " + allianceId, e);
                });
    }

    /**
     * Award a special boss badge to a specific user
     */
    private void awardSpecialBossBadge(String userId, String bossId) {
        Log.d(TAG, "Awarding special boss badge to user " + userId + " for boss " + bossId);

        // Check if this user already has a badge for this boss
        FirebaseFirestore.getInstance().collection("badges")
                .whereEqualTo("userId", userId)
                .whereEqualTo("allianceBossId", bossId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        Log.d(TAG, "User " + userId + " already has badge for boss " + bossId + ", skipping");
                        return;
                    }

                    // Create the badge
                    java.util.Map<String, Object> badge = new java.util.HashMap<>();
                    badge.put("id", "special_boss_" + bossId + "_" + userId);
                    badge.put("name", "Alliance Boss Slayer #" + bossId.substring(0, Math.min(6, bossId.length())));
                    badge.put("description", "Defeated a powerful alliance boss together with your alliance members!");
                    badge.put("iconResource", "https://cdn-icons-png.flaticon.com/512/856/856940.png");
                    badge.put("userId", userId);
                    badge.put("allianceBossId", bossId);
                    badge.put("dateAwarded", new java.util.Date());

                    // Save the badge to Firestore
                    FirebaseFirestore.getInstance().collection("badges")
                            .document((String) badge.get("id"))
                            .set(badge)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "✅ Successfully awarded special boss badge to user " + userId);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "❌ Failed to award badge to user " + userId + " for boss " + bossId, e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to check existing badges for user " + userId, e);
                });
    }

    /**
     * Check for dead bosses that might need badge awarding
     */
    private void checkDeadBossesForBadgeAwarding() {
        Log.d(TAG, "Checking for DEAD alliance bosses that need badge awarding...");

        FirebaseFirestore.getInstance().collection("allianceBosses")
                .whereEqualTo("status", "DEAD")
                .get()
                .addOnSuccessListener(deadBossesSnapshot -> {
                    Log.d(TAG, "Found " + deadBossesSnapshot.size() + " DEAD alliance bosses");

                    for (com.google.firebase.firestore.DocumentSnapshot doc : deadBossesSnapshot.getDocuments()) {
                        String bossId = doc.getId();
                        String allianceId = doc.getString("allianceId");

                        Log.d(TAG, "Checking DEAD boss " + bossId + " (alliance: " + allianceId + ") for badge awarding");

                        // Check if badges have already been awarded for this boss
                        checkAndAwardBadgesForDeadBoss(bossId, allianceId);
                    }

                    if (deadBossesSnapshot.isEmpty()) {
                        Log.d(TAG, "No DEAD alliance bosses found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to query DEAD alliance bosses", e);
                });
    }

    /**
     * Check if badges have been awarded for a dead boss, and award them if not
     */
    private void checkAndAwardBadgesForDeadBoss(String bossId, String allianceId) {
        if (allianceId == null) {
            Log.w(TAG, "No alliance ID found for DEAD boss " + bossId + ", cannot check badges");
            return;
        }

        // Check if any badges exist for this boss
        FirebaseFirestore.getInstance().collection("badges")
                .whereEqualTo("allianceBossId", bossId)
                .limit(1)
                .get()
                .addOnSuccessListener(badgeSnapshot -> {
                    if (badgeSnapshot.isEmpty()) {
                        Log.d(TAG, "No badges found for DEAD boss " + bossId + ", awarding badges now");
                        awardBadgesForDeadBoss(bossId, allianceId);
                    } else {
                        Log.d(TAG, "Badges already exist for DEAD boss " + bossId + ", skipping");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to check existing badges for boss " + bossId, e);
                });
    }
}
