package com.example.rpgapp.repository;

import android.util.Log;
import com.example.rpgapp.callback.AuthCallback;
import com.example.rpgapp.model.User;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class FirebaseUserRepository
{
    private static final String TAG = "FirebaseUserRepository";
    private static final String COLLECTION_USERS = "users";
    private FirebaseFirestore db;

    public FirebaseUserRepository()
    {
        Log.d(TAG, "FirebaseUserRepository konstruktor pozvan");

        try
        {
            db = FirebaseFirestore.getInstance();
            Log.d(TAG, "FirebaseFirestore instance uspešno kreiran");
        }
        catch (Exception e)
        {
            Log.e(TAG, "GREŠKA pri kreiranju FirebaseFirestore: " + e.getMessage(), e);
            throw e;
        }
    }

    public void saveUser(User user, AuthCallback<Boolean> onComplete)
    {
        Log.d(TAG, "saveUser pozvan za username: " + user.getUsername());
        Log.d(TAG, "Saving to Firestore collection: " + COLLECTION_USERS + ", document ID: " + user.getId());

        try
        {
            Map<String, Object> userData = new HashMap<>();

            userData.put("email", user.getEmail());
            userData.put("username", user.getUsername());
            userData.put("avatarId", user.getAvatarId());
            userData.put("level", user.getLevel());
            userData.put("title", user.getTitle());
            userData.put("powerPoints", user.getPowerPoints());
            userData.put("experiencePoints", user.getExperiencePoints());
            userData.put("coins", user.getCoins());
            userData.put("emailVerified", user.isEmailVerified());
            userData.put("active", user.isActive()); // Dodajem isActive polje
            userData.put("registrationDate", user.getRegistrationDate());
            userData.put("lastLogin", user.getLastLogin());
            userData.put("activeDaysStreak", user.getActiveDaysStreak());
            userData.put("lastActivityDayUpdate", user.getLastActivityDayUpdate());

            Log.d(TAG, "Attempting to write to Firestore...");
            db.collection(COLLECTION_USERS)
                .document(user.getId())
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✓ Firestore write SUCCESSFUL for user: " + user.getUsername());
                    onComplete.onResult(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "✗ Firestore write FAILED for user: " + user.getUsername());
                    Log.e(TAG, "Error type: " + e.getClass().getSimpleName());
                    Log.e(TAG, "Error message: " + e.getMessage());
                    e.printStackTrace();
                    onComplete.onResult(false);
                });
        }
        catch (Exception e)
        {
            Log.e(TAG, "GREŠKA u saveUser: " + e.getMessage(), e);
            onComplete.onResult(false);
        }
    }

    public void getUserById(String userId, AuthCallback<User> onComplete)
    {
        db.collection(COLLECTION_USERS)
            .document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists())
                {
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null)
                    {
                        user.setId(documentSnapshot.getId());
                    }
                    onComplete.onResult(user);
                }
                else
                {
                    onComplete.onResult(null);
                }
            })
            .addOnFailureListener(e -> {
                e.printStackTrace();
                onComplete.onResult(null);
            });
    }

    public void getUserByEmail(String email, AuthCallback<User> onComplete)
    {
        db.collection(COLLECTION_USERS)
            .whereEqualTo("email", email)
            .limit(1)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (!queryDocumentSnapshots.isEmpty())
                {
                    DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                    User user = document.toObject(User.class);
                    if (user != null)
                    {
                        user.setId(document.getId());
                    }
                    onComplete.onResult(user);
                }
                else
                {
                    onComplete.onResult(null);
                }
            })
            .addOnFailureListener(e -> {
                e.printStackTrace();
                onComplete.onResult(null);
            });
    }

    public void getUserByUsername(String username, AuthCallback<User> onComplete)
    {
        db.collection(COLLECTION_USERS)
            .whereEqualTo("username", username)
            .limit(1)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (!queryDocumentSnapshots.isEmpty())
                {
                    DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                    User user = document.toObject(User.class);
                    if (user != null)
                    {
                        user.setId(document.getId());
                    }
                    onComplete.onResult(user);
                }
                else
                {
                    onComplete.onResult(null);
                }
            })
            .addOnFailureListener(e -> {
                e.printStackTrace();
                onComplete.onResult(null);
            });
    }

    public void checkUsernameExists(String username, AuthCallback<Boolean> onComplete)
    {
        db.collection(COLLECTION_USERS)
            .whereEqualTo("username", username)
            .limit(1)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots ->
                onComplete.onResult(!queryDocumentSnapshots.isEmpty()))
            .addOnFailureListener(e -> {
                e.printStackTrace();
                onComplete.onResult(false);
            });
    }

    public void checkEmailExists(String email, AuthCallback<Boolean> onComplete)
    {
        db.collection(COLLECTION_USERS)
            .whereEqualTo("email", email)
            .limit(1)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots ->
                onComplete.onResult(!queryDocumentSnapshots.isEmpty()))
            .addOnFailureListener(e -> {
                e.printStackTrace();
                onComplete.onResult(false);
            });
    }

    public void updateUser(User user, AuthCallback<Boolean> onComplete)
    {
        Map<String, Object> updates = new HashMap<>();
        updates.put("level", user.getLevel());
        updates.put("title", user.getTitle());
        updates.put("powerPoints", user.getPowerPoints());
        updates.put("experiencePoints", user.getExperiencePoints());
        updates.put("coins", user.getCoins());
        updates.put("emailVerified", user.isEmailVerified());
        updates.put("active", user.isActive()); // Dodajem isActive
        updates.put("lastLogin", user.getLastLogin());
        updates.put("activeDaysStreak", user.getActiveDaysStreak());
        updates.put("lastActivityDayUpdate", user.getLastActivityDayUpdate());

        db.collection(COLLECTION_USERS)
            .document(user.getId())
            .update(updates)
            .addOnSuccessListener(aVoid -> onComplete.onResult(true))
            .addOnFailureListener(e -> {
                e.printStackTrace();
                onComplete.onResult(false);
            });
    }

    public void updateEmailVerification(String userId, boolean isVerified, AuthCallback<Boolean> onComplete)
    {
        db.collection(COLLECTION_USERS)
            .document(userId)
            .update("emailVerified", isVerified)
            .addOnSuccessListener(aVoid -> onComplete.onResult(true))
            .addOnFailureListener(e -> {
                e.printStackTrace();
                onComplete.onResult(false);
            });
    }

    public void activateUser(String userId, AuthCallback<Boolean> onComplete)
    {
        db.collection(COLLECTION_USERS)
            .document(userId)
            .update("active", true)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "User activated successfully: " + userId);
                onComplete.onResult(true);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to activate user: " + userId, e);
                onComplete.onResult(false);
            });
    }
}
