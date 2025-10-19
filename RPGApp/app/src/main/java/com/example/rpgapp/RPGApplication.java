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
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to update boss " + bossId + " status to " + newStatus, e);
                });
    }
}
