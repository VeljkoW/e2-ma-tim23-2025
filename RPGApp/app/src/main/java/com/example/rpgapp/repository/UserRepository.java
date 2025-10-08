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
        Log.d(TAG, "Attempting to insert user into SQLite database");
        
        // saves it first into the local database then if it succeeds tries to save it into firebase
        long result = userDao.insertUser(user);
        
        Log.d(TAG, "SQLite insertUser result: " + result + " (>0 = success, -1 = failed)");

        if (result != -1)
        {
            Log.d(TAG, "SQLite insert successful, now saving to Firebase");
            firebaseRepository.saveUser(user, new AuthCallback<Boolean>() {
                @Override
                public void onResult(Boolean success)
                {
                    Log.d(TAG, "Firebase save result: " + success);
                    onComplete.onResult(true);
                }
            });
        }
        else
        {
            Log.e(TAG, "SQLite insert FAILED for user: " + user.getUsername());
            onComplete.onResult(false);
        }
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
        int result = userDao.updateUser(user);

        if (result > 0)
        {
            firebaseRepository.updateUser(user, onComplete);
        }
        else
        {
            onComplete.onResult(false);
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
}
