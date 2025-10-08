package com.example.rpgapp;

import android.app.Application;
import android.util.Log;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class RPGApplication extends Application
{
    private static final String TAG = "RPGApplication";

    @Override
    public void onCreate()
    {
        super.onCreate();
        
        Log.d(TAG, "RPGApplication onCreate() pozvano!");

        try
        {
            // Initialize Firebase with default configuration
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this);
                Log.d(TAG, "Firebase uspešno inicijalizovan!");
            } else {
                Log.d(TAG, "Firebase već inicijalizovan!");
            }

            // Initialize Firebase Auth
            FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

            // Disable app verification for development - multiple approaches
            try {
                firebaseAuth.getFirebaseAuthSettings().setAppVerificationDisabledForTesting(true);
                Log.d(TAG, "App verification disabled successfully");
            } catch (Exception e) {
                Log.w(TAG, "Could not disable app verification: " + e.getMessage());
            }

            // Additional approach: Set auth emulator for development (optional)
            try {
                // Only for local development - comment out for production
                // firebaseAuth.useEmulator("10.0.2.2", 9099);
                Log.d(TAG, "Firebase Auth configured for development");
            } catch (Exception e) {
                Log.w(TAG, "Auth emulator setup failed: " + e.getMessage());
            }

            // Initialize Firestore
            FirebaseFirestore firestore = FirebaseFirestore.getInstance();

            Log.d(TAG, "Firebase komponente uspešno inicijalizovane sa disabled app verification");
        }
        catch (Exception e)
        {
            Log.e(TAG, "Greška pri inicijalizaciji Firebase: " + e.getMessage(), e);
        }
    }
}
