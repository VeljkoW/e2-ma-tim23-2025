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
        }
        catch (Exception e)
        {
            Log.e(TAG, "Error initializing Firebase: " + e.getMessage(), e);
        }
    }
}
