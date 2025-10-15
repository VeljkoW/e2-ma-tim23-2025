package com.example.rpgapp.repository;

import android.content.Context;
import android.util.Log;
import com.example.rpgapp.callback.AuthCallback;
import com.example.rpgapp.database.DatabaseHelper;
import com.example.rpgapp.database.UserDao;
import com.example.rpgapp.model.User;

public class UserRepository
{
    private static final String TAG = "UserRepository";

    private UserDao userDao;
    private FirebaseUserRepository firebaseRepository;

    public UserRepository(Context context)
    {
        Log.d(TAG, "UserRepo constructor called");

        try
        {
            DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);
            this.userDao = new UserDao(dbHelper);
            Log.d(TAG, "UserDao created successfully");

            this.firebaseRepository = new FirebaseUserRepository();
            Log.d(TAG, "FirebaseUserRepository created successfully");

            Log.d(TAG, "UserRepository initialization complete");
        }
        catch (Exception e)
        {
            Log.e(TAG, "Error initializating UserRepo: " + e.getMessage(), e);
            throw e;
        }
    }

    public void saveUser(User user, AuthCallback<Boolean> onComplete)
    {
        Log.d(TAG, "saveUser called for username: " + user.getUsername());
        Log.d(TAG, "Saving to Firebase first (priority)");

        // Save to Firebase first (this is the source of truth)
        firebaseRepository.saveUser(user, new AuthCallback<Boolean>() {
            @Override
            public void onResult(Boolean firebaseSuccess)
            {
                Log.d(TAG, "Firebase save result: " + firebaseSuccess);

                if (firebaseSuccess)
                {
                    // Try to save to SQLite for local caching
                    Log.d(TAG, "Attempting to insert user into SQLite database");
                    long result = userDao.insertUser(user);

                    Log.d(TAG, "SQLite insertUser result: " + result + " (>0 = success, -1 = failed)");

                    if (result == -1)
                    {
                        // If insert failed (likely duplicate email), try updating instead
                        Log.w(TAG, "SQLite insert failed, attempting update instead");
                        try {
                            userDao.updateUser(user);
                            Log.d(TAG, "SQLite update successful");
                        } catch (Exception e) {
                            Log.w(TAG, "SQLite update also failed: " + e.getMessage());
                            // Not critical - Firebase save succeeded
                        }
                    }

                    onComplete.onResult(true);
                }
                else
                {
                    Log.e(TAG, "Firebase save FAILED for user: " + user.getUsername());
                    onComplete.onResult(false);
                }
            }
        });
    }

    public void getUserById(String userId, AuthCallback<User> onComplete)
    {
        User localUser = userDao.getUserById(userId);

        if (localUser != null)
        {
            onComplete.onResult(localUser);

            // sync with firebase in the background
            firebaseRepository.getUserById(userId, new AuthCallback<User>()
            {
                @Override
                public void onResult(User firebaseUser) {
                    if (firebaseUser != null) {
                        userDao.updateUser(firebaseUser);
                    }
                }
            });
        }
        else
        {
            firebaseRepository.getUserById(userId, new AuthCallback<User>() {
                @Override
                public void onResult(User firebaseUser)
                {
                    if (firebaseUser != null)
                    {
                        // Try to insert first, if it fails due to constraint violation, update instead
                        long insertResult = userDao.insertUser(firebaseUser);
                        if (insertResult == -1) {
                            Log.d(TAG, "Insert failed (likely duplicate email), attempting update by ID first");
                            // Insert failed, likely due to duplicate email constraint
                            // Try to update the existing user by ID first
                            int updateResult = userDao.updateUser(firebaseUser);
                            Log.d(TAG, "Update by ID result: " + updateResult + " rows affected");

                            if (updateResult == 0) {
                                // ID-based update failed, try updating by email (handles ID mismatches)
                                Log.d(TAG, "ID-based update failed, trying update by email");
                                int emailUpdateResult = userDao.updateUserByEmail(firebaseUser);
                                Log.d(TAG, "Update by email result: " + emailUpdateResult + " rows affected");
                            }
                        } else {
                            Log.d(TAG, "User inserted successfully with rowId: " + insertResult);
                        }
                    }
                    onComplete.onResult(firebaseUser);
                }
            });
        }
    }

    public void getUserByEmail(String email, AuthCallback<User> onComplete)
    {
        User localUser = userDao.getUserByEmail(email);

        if (localUser != null)
        {
            onComplete.onResult(localUser);
        }
        else
        {
            firebaseRepository.getUserByEmail(email, onComplete);
        }
    }

    public void updateUser(User user, AuthCallback<Boolean> onComplete) {
        Log.d(TAG, "updateUser called for user: " + user.getUsername() + " (ID: " + user.getId() + ")");

        int result = userDao.updateUser(user);
        Log.d(TAG, "Local SQLite update result: " + result + " (>0 = success)");

        if (result > 0) {
            Log.d(TAG, "Local update successful, updating Firebase");
            firebaseRepository.updateUser(user, onComplete);
        } else {
            Log.w(TAG, "Local update failed, attempting Firebase update anyway");
            // Still try Firebase update even if local fails
            firebaseRepository.updateUser(user, new AuthCallback<Boolean>() {
                @Override
                public void onResult(Boolean success) {
                    if (success != null && success) {
                        Log.d(TAG, "Firebase update successful, re-syncing to local DB");
                        // If Firebase succeeds, try to insert/update locally again
                        long insertResult = userDao.insertUser(user);
                        if (insertResult == -1) {
                            // Insert failed, user might already exist, try update again
                            userDao.updateUser(user);
                        }
                        onComplete.onResult(true);
                    } else {
                        Log.e(TAG, "Both local and Firebase updates failed");
                        onComplete.onResult(false);
                    }
                }
            });
        }
    }

    public void updateLastLogin(String userId)
    {
        userDao.updateLastLogin(userId);

        getUserById(userId, new AuthCallback<User>()
        {
            @Override
            public void onResult(User user)
            {
                if (user != null)
                {
                    user.setLastLogin(new java.util.Date());
                    firebaseRepository.updateUser(user, new AuthCallback<Boolean>()
                    {
                        @Override
                        public void onResult(Boolean success) {
                        }
                    });
                }
            }
        });
    }

    public com.google.android.gms.tasks.Task<Void> awardXpToUser(String userId, int xpAmount) {
        Log.d(TAG, "awardXpToUser called for userId: " + userId + ", xpAmount: " + xpAmount);
        com.google.android.gms.tasks.TaskCompletionSource<Void> taskCompletionSource = new com.google.android.gms.tasks.TaskCompletionSource<>();

        // Directly use Firebase to update XP, bypassing complex local DB sync
        firebaseRepository.getUserById(userId, new AuthCallback<User>() {
            @Override
            public void onResult(User user) {
                if (user != null) {
                    Log.d(TAG, "User found via Firebase: " + user.getUsername() + ", current XP: " + user.getExperiencePoints());

                    // Add XP to user
                    user.addExperiencePoints(xpAmount);
                    Log.d(TAG, "New XP: " + user.getExperiencePoints() + " (added " + xpAmount + ")");

                    // Update directly to Firebase only
                    firebaseRepository.updateUser(user, new AuthCallback<Boolean>() {
                        @Override
                        public void onResult(Boolean success) {
                            Log.d(TAG, "Firebase update result: " + success);
                            if (success != null && success) {
                                // Try to update local DB in background, but don't fail if it doesn't work
                                try {
                                    userDao.updateUser(user);
                                    Log.d(TAG, "Local DB sync successful");
                                } catch (Exception e) {
                                    Log.w(TAG, "Local DB sync failed, but Firebase succeeded: " + e.getMessage());
                                }
                                taskCompletionSource.setResult(null);
                            } else {
                                Log.e(TAG, "Firebase update failed");
                                taskCompletionSource.setException(new Exception("Firebase update failed"));
                            }
                        }
                    });
                } else {
                    Log.e(TAG, "User not found in Firebase for ID: " + userId);
                    taskCompletionSource.setException(new Exception("User not found"));
                }
            }
        });

        return taskCompletionSource.getTask();
    }

    // Add a fallback XP awarding method that works without Firebase
    public com.google.android.gms.tasks.Task<Void> awardXpToUserFallback(String userId, int xpAmount) {
        Log.d(TAG, "awardXpToUserFallback called for userId: " + userId + ", xpAmount: " + xpAmount);
        com.google.android.gms.tasks.TaskCompletionSource<Void> taskCompletionSource = new com.google.android.gms.tasks.TaskCompletionSource<>();

        // Try local database first
        User localUser = userDao.getUserById(userId);
        if (localUser != null) {
            Log.d(TAG, "User found locally: " + localUser.getUsername() + ", current XP: " + localUser.getExperiencePoints());

            // Add XP to user
            localUser.addExperiencePoints(xpAmount);
            Log.d(TAG, "New XP: " + localUser.getExperiencePoints() + " (added " + xpAmount + ")");

            // Update local database
            int result = userDao.updateUser(localUser);
            if (result > 0) {
                Log.d(TAG, "Local XP update successful");

                // Try Firebase in background, but don't fail if it doesn't work
                firebaseRepository.updateUser(localUser, new AuthCallback<Boolean>() {
                    @Override
                    public void onResult(Boolean success) {
                        Log.d(TAG, "Background Firebase sync result: " + success);
                    }
                });

                taskCompletionSource.setResult(null);
            } else {
                Log.e(TAG, "Local XP update failed");
                taskCompletionSource.setException(new Exception("Local database update failed"));
            }
        } else {
            Log.e(TAG, "User not found locally for ID: " + userId);
            taskCompletionSource.setException(new Exception("User not found in local database"));
        }

        return taskCompletionSource.getTask();
    }

    public void updateEmailVerification(String userId, boolean isVerified, AuthCallback<Boolean> onComplete) {
        userDao.updateEmailVerification(userId, isVerified);
        firebaseRepository.updateEmailVerification(userId, isVerified, onComplete);
    }

    public boolean isUsernameExists(String username) {
        return userDao.isUsernameExists(username);
    }

    public boolean isEmailExists(String email) {
        return userDao.isEmailExists(email);
    }

    public void checkUsernameExistsAsync(String username, AuthCallback<Boolean> onComplete) {
        boolean localExists = userDao.isUsernameExists(username);

        if (localExists)
        {
            onComplete.onResult(true);
        }
        else
        {
            firebaseRepository.checkUsernameExists(username, onComplete);
        }
    }

    public void checkEmailExistsAsync(String email, AuthCallback<Boolean> onComplete) {
        boolean localExists = userDao.isEmailExists(email);

        if (localExists) {
            onComplete.onResult(true);
        } else {
            firebaseRepository.checkEmailExists(email, onComplete);
        }
    }

    public com.google.android.gms.tasks.Task<java.util.Date> getUserRegistrationDate(String userId) {
        Log.d(TAG, "getUserRegistrationDate called for userId: " + userId);

        com.google.android.gms.tasks.TaskCompletionSource<java.util.Date> taskCompletionSource =
            new com.google.android.gms.tasks.TaskCompletionSource<>();

        // First try to get from local cache
        User localUser = userDao.getUserById(userId);

        if (localUser != null && localUser.getRegistrationDate() != null) {
            Log.d(TAG, "Found registration date in local cache: " + localUser.getRegistrationDate());
            taskCompletionSource.setResult(localUser.getRegistrationDate());
            return taskCompletionSource.getTask();
        }

        // If not in local cache, get from Firebase
        Log.d(TAG, "Registration date not in local cache, fetching from Firebase");
        firebaseRepository.getUserById(userId, new AuthCallback<User>() {
            @Override
            public void onResult(User firebaseUser) {
                if (firebaseUser != null && firebaseUser.getRegistrationDate() != null) {
                    Log.d(TAG, "Found registration date in Firebase: " + firebaseUser.getRegistrationDate());

                    // Update local cache
                    userDao.updateUser(firebaseUser);

                    taskCompletionSource.setResult(firebaseUser.getRegistrationDate());
                } else {
                    Log.w(TAG, "User not found or registration date is null for userId: " + userId);
                    // Return a default date (30 days ago) if no registration date found
                    java.util.Calendar cal = java.util.Calendar.getInstance();
                    cal.add(java.util.Calendar.DAY_OF_MONTH, -30);
                    taskCompletionSource.setResult(cal.getTime());
                }
            }
        });

        return taskCompletionSource.getTask();
    }

    public com.google.android.gms.tasks.Task<Void> awardCoinsToUser(String userId, int coinsAmount) {
        Log.d(TAG, "awardCoinsToUser called for userId: " + userId + ", coinsAmount: " + coinsAmount);
        com.google.android.gms.tasks.TaskCompletionSource<Void> taskCompletionSource = new com.google.android.gms.tasks.TaskCompletionSource<>();

        // Get user and update coins
        firebaseRepository.getUserById(userId, new AuthCallback<User>() {
            @Override
            public void onResult(User user) {
                if (user != null) {
                    Log.d(TAG, "User found: " + user.getUsername() + ", current coins: " + user.getCoins());

                    // Add coins to user
                    user.addCoins(coinsAmount);
                    Log.d(TAG, "New coins total: " + user.getCoins() + " (added " + coinsAmount + ")");

                    // Update user in Firebase
                    firebaseRepository.updateUser(user, new AuthCallback<Boolean>() {
                        @Override
                        public void onResult(Boolean success) {
                            Log.d(TAG, "Firebase coins update result: " + success);
                            if (success != null && success) {
                                // Try to update local DB in background
                                try {
                                    userDao.updateUser(user);
                                    Log.d(TAG, "Local DB coins sync successful");
                                } catch (Exception e) {
                                    Log.w(TAG, "Local DB coins sync failed, but Firebase succeeded: " + e.getMessage());
                                }
                                taskCompletionSource.setResult(null);
                            } else {
                                Log.e(TAG, "Firebase coins update failed");
                                taskCompletionSource.setException(new Exception("Firebase coins update failed"));
                            }
                        }
                    });
                } else {
                    Log.e(TAG, "User not found in Firebase for ID: " + userId);
                    taskCompletionSource.setException(new Exception("User not found"));
                }
            }
        });

        return taskCompletionSource.getTask();
    }

    /**
     * Firebase-only user access method specifically for boss functionalities
     * This bypasses local database entirely to avoid sync issues
     */
    public void getUserByIdFirebaseOnly(String userId, AuthCallback<User> onComplete) {
        Log.d(TAG, "getUserByIdFirebaseOnly called for userId: " + userId);
        Log.d(TAG, "Bypassing local database, using Firebase only for boss functionality");

        firebaseRepository.getUserById(userId, new AuthCallback<User>() {
            @Override
            public void onResult(User firebaseUser) {
                Log.d(TAG, "Firebase-only user lookup result: " + (firebaseUser != null ? "SUCCESS" : "FAILED"));
                if (firebaseUser != null) {
                    Log.d(TAG, "User found: " + firebaseUser.getUsername() + " with " + firebaseUser.getPowerPoints() + " power points");
                }
                onComplete.onResult(firebaseUser);
            }
        });
    }

    /**
     * Firebase-only user update method specifically for boss functionalities
     * This bypasses local database entirely to avoid sync issues
     */
    public void updateUserFirebaseOnly(User user, AuthCallback<Boolean> onComplete) {
        Log.d(TAG, "updateUserFirebaseOnly called for user: " + user.getUsername());
        Log.d(TAG, "Bypassing local database, using Firebase only for boss functionality");

        firebaseRepository.updateUser(user, new AuthCallback<Boolean>() {
            @Override
            public void onResult(Boolean success) {
                Log.d(TAG, "Firebase-only user update result: " + success);
                onComplete.onResult(success);
            }
        });
    }
}
