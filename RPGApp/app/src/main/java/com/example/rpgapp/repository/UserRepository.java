package com.example.rpgapp.repository;

import android.content.Context;
import android.util.Log;
import com.example.rpgapp.callback.AuthCallback;
import com.example.rpgapp.database.DatabaseHelper;
import com.example.rpgapp.database.UserDao;
import com.example.rpgapp.model.User;

import java.util.Calendar;
import java.util.Date;

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
                        userDao.insertUser(firebaseUser);
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

                    int levelsBefore = user.getLevel();
                    while (user.canLevelUp())
                    {
                        user.levelUp();
                        Log.d(TAG, "LEVEL UP! User leveled up to level: " + user.getLevel() + ", Title: " + user.getTitle() + ", PP: " + user.getPowerPoints());
                    }

                    if (user.getLevel() > levelsBefore)
                    {
                        Log.d(TAG, "User advanced from level " + levelsBefore + " to level " + user.getLevel());
                    }

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

            int levelsBefore = localUser.getLevel();
            while (localUser.canLevelUp())
            {
                localUser.levelUp();
                Log.d(TAG, "LEVEL UP! User leveled up to level: " + localUser.getLevel() + ", Title: " + localUser.getTitle() + ", PP: " + localUser.getPowerPoints());
            }

            if (localUser.getLevel() > levelsBefore)
            {
                Log.d(TAG, "User advanced from level " + levelsBefore + " to level " + localUser.getLevel());
            }

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
    public void updateActiveDaysOnLogin(String userId)
    {
        // Delegiraj poziv ka FirebaseUserRepository
        firebaseRepository.getUserById(userId, new AuthCallback<User>() {
            @Override
            public void onResult(User user) {
                if (user != null)
                {
                    Date lastActivityDay = user.getLastActivityDayUpdate(); // Koristimo lastActivityDayUpdate umesto lastLogin
                    Date now = new Date();

                    Calendar lastActivityCal = Calendar.getInstance();
                    Calendar nowCal = Calendar.getInstance();
                    nowCal.setTime(now);

                    if (lastActivityDay != null)
                    {
                        lastActivityCal.setTime(lastActivityDay);

                        // Postavi na početak dana za poređenje
                        lastActivityCal.set(Calendar.HOUR_OF_DAY, 0);
                        lastActivityCal.set(Calendar.MINUTE, 0);
                        lastActivityCal.set(Calendar.SECOND, 0);
                        lastActivityCal.set(Calendar.MILLISECOND, 0);

                        nowCal.set(Calendar.HOUR_OF_DAY, 0);
                        nowCal.set(Calendar.MINUTE, 0);
                        nowCal.set(Calendar.SECOND, 0);
                        nowCal.set(Calendar.MILLISECOND, 0);

                        long diffInMillis = nowCal.getTimeInMillis() - lastActivityCal.getTimeInMillis();
                        long daysDiff = diffInMillis / (1000 * 60 * 60 * 24);

                        if (daysDiff == 0)
                        {
                            // Isti dan - ne ažuriramo lastActivityDayUpdate, samo lastLogin
                            user.setLastLogin(now);
                        }
                        else if (daysDiff == 1)
                        {
                            // Uzastopni dan - povećavamo streak i ažuriramo oba datuma
                            user.setActiveDaysStreak(user.getActiveDaysStreak() + 1);
                            user.setLastActivityDayUpdate(now);
                            user.setLastLogin(now);
                        }
                        else
                        {
                            // Prekinut streak - resetujemo na 1 i ažuriramo oba datuma
                            user.setActiveDaysStreak(1);
                            user.setLastActivityDayUpdate(now);
                            user.setLastLogin(now);
                        }

                        if(user.getActiveDaysStreak() == 0)
                        {
                            user.setActiveDaysStreak(1);
                            user.setLastActivityDayUpdate(now);
                            user.setLastLogin(now);
                        }
                    }
                    else
                    {
                        // Prvi put - postavljamo oba datuma
                        user.setActiveDaysStreak(1);
                        user.setLastActivityDayUpdate(now);
                        user.setLastLogin(now);
                    }

                    firebaseRepository.updateUser(user, new AuthCallback<Boolean>()
                    {
                        @Override
                        public void onResult(Boolean success) {
                            if (success != null && success)
                            {
                                userDao.updateUser(user);
                            }
                        }
                    });
                }
            }
        });
    }

    public void activateUser(String userId, AuthCallback<Boolean> onComplete) {
        // Delegiraj poziv ka FirebaseUserRepository
        firebaseRepository.activateUser(userId, onComplete);
    }
}
