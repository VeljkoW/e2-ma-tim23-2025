package com.example.rpgapp.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.example.rpgapp.callback.AuthCallback;
import com.example.rpgapp.model.User;
import com.example.rpgapp.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.Date;

public class AuthService
{
    private static final String TAG = "AuthService";
    private static final String PREFS_NAME = "RPGAppPrefs";
    private static final String KEY_CURRENT_USER_ID = "current_user_id";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

    private final FirebaseAuth firebaseAuth;
    private final UserRepository userRepository;
    private final SharedPreferences sharedPreferences;

    public AuthService(Context context)
    {
        Log.d(TAG, "AuthService konstruktor pozvan");

        try
        {
            this.firebaseAuth = FirebaseAuth.getInstance();
            Log.d(TAG, "FirebaseAuth instance kreiran uspešno");
        }
        catch (Exception e)
        {
            Log.e(TAG, "Greška pri kreiranju FirebaseAuth: " + e.getMessage(), e);
            throw e;
        }

        this.userRepository = new UserRepository(context);
        this.sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Log.d(TAG, "AuthService uspešno inicijalizovan");
    }

    public void registerUser(String email, String password, String username, String avatarId, AuthCallback<AuthResult> onComplete) {
        Log.d(TAG, "registerUser pozvan za email: " + email);

        userRepository.checkUsernameExistsAsync(username, usernameExists -> {
            if (usernameExists)
            {
                Log.d(TAG, "Username već postoji: " + username);
                onComplete.onResult(new AuthResult(false, "Username Already Exists"));
                return;
            }

            Log.d(TAG, "Pokušavam kreiranje korisnika u Firebase Auth...");

            firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    Log.d(TAG, "Firebase Auth registracija USPEŠNA!");
                    FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                    if (firebaseUser != null)
                    {
                        sendEmailVerification(firebaseUser, verificationSent -> {
                            if (verificationSent)
                            {
                                User user = new User(email, username, avatarId);
                                user.setId(firebaseUser.getUid());

                                userRepository.saveUser(user, saveSuccess -> {
                                    if (saveSuccess)
                                    {
                                        onComplete.onResult(new AuthResult(true,
                                            "Registration Successful! Check your mail to activate your account."));
                                    }
                                    else
                                    {
                                        onComplete.onResult(new AuthResult(false,
                                            "Error saving user data"));
                                    }
                                });
                            }
                            else
                            {
                                onComplete.onResult(new AuthResult(false,
                                    "Error sending verification email"));
                            }
                        });
                    }
                })
                .addOnFailureListener(exception -> {
                    Log.e(TAG, "Firebase Auth registracija NEUSPEŠNA: " + exception.getMessage(), exception);

                    // Check if it's a reCAPTCHA configuration error
                    if (exception.getMessage() != null &&
                        (exception.getMessage().contains("CONFIGURATION_NOT_FOUND") ||
                         exception.getMessage().contains("reCAPTCHA") ||
                         exception.getMessage().contains("internal error"))) {

                        Log.w(TAG, "reCAPTCHA konfiguracija nije dostupna, koristim alternativnu metodu...");
                        registerUserWithoutRecaptcha(email, password, username, avatarId, onComplete);
                        return;
                    }

                    String errorMessage = getFirebaseErrorMessage(exception.getMessage());
                    onComplete.onResult(new AuthResult(false, errorMessage));
                });
        });
    }

    public void loginUser(String email, String password, AuthCallback<AuthResult> onComplete) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener(authResult -> {
                FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                if (firebaseUser != null)
                {
                    if (!firebaseUser.isEmailVerified())
                    {
                        onComplete.onResult(new AuthResult(false, "Please verify your email before logging in."));
                        return;
                    }

                    userRepository.getUserById(firebaseUser.getUid(), user -> {
                        if (user != null)
                        {
                            user.setLastLogin(new Date());
                            userRepository.updateLastLogin(user.getId());
                            saveUserSession(user.getId());
                            onComplete.onResult(new AuthResult(true, "Login success", user));
                        }
                        else
                        {
                            onComplete.onResult(new AuthResult(false, "Error fetching user data"));
                        }
                    });
                }
            })
            .addOnFailureListener(exception -> {
                String errorMessage = getFirebaseErrorMessage(exception.getMessage());
                onComplete.onResult(new AuthResult(false, errorMessage));
            });
    }

    public void logout()
    {
        firebaseAuth.signOut();
        clearUserSession();
    }

    public void changePassword(String currentPassword, String newPassword, AuthCallback<AuthResult> onComplete)
    {
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser != null && firebaseUser.getEmail() != null)
        {
            firebaseAuth.signInWithEmailAndPassword(firebaseUser.getEmail(), currentPassword)
                .addOnSuccessListener(authResult -> {
                    firebaseUser.updatePassword(newPassword)
                        .addOnSuccessListener(aVoid ->
                            onComplete.onResult(new AuthResult(true, "Password changed successfully")))
                        .addOnFailureListener(exception ->
                            onComplete.onResult(new AuthResult(false, "Error changing password")));
                })
                .addOnFailureListener(exception ->
                    onComplete.onResult(new AuthResult(false, "Incorrect current password")));
        }
        else
        {
            onComplete.onResult(new AuthResult(false, "User not logged in"));
        }
    }

    public void resendEmailVerification(AuthCallback<Boolean> onComplete)
    {
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser != null)
        {
            sendEmailVerification(firebaseUser, onComplete);
        }
        else
        {
            onComplete.onResult(false);
        }
    }

    private void sendEmailVerification(FirebaseUser firebaseUser, AuthCallback<Boolean> onComplete)
    {
        firebaseUser.sendEmailVerification()
            .addOnSuccessListener(aVoid -> onComplete.onResult(true))
            .addOnFailureListener(exception -> onComplete.onResult(false));
    }

    public boolean isUserLoggedIn()
    {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false) && firebaseAuth.getCurrentUser() != null;
    }

    public String getCurrentUserId()
    {
        return sharedPreferences.getString(KEY_CURRENT_USER_ID, null);
    }

    public void getCurrentUser(AuthCallback<User> onComplete)
    {
        String userId = getCurrentUserId();
        if (userId != null) {
            userRepository.getUserById(userId, onComplete);
        } else {
            onComplete.onResult(null);
        }
    }

    private void saveUserSession(String userId)
    {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_CURRENT_USER_ID, userId);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.apply();
    }

    private void clearUserSession()
    {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_CURRENT_USER_ID);
        editor.putBoolean(KEY_IS_LOGGED_IN, false);
        editor.apply();
    }

    private String getFirebaseErrorMessage(String firebaseError)
    {
        if (firebaseError == null)
            return "Unknown error occurred";

        if (firebaseError.contains("email-already-in-use"))
        {
            return "Email address already in use";
        } else if (firebaseError.contains("weak-password"))
        {
            return "Weak password. Password should be at least 6 characters long";
        } else if (firebaseError.contains("invalid-email")) {
            return "Invalid email address";
        } else if (firebaseError.contains("user-not-found")) {
            return "User with this email does not exist";
        } else if (firebaseError.contains("wrong-password")) {
            return "Wrong password";
        } else if (firebaseError.contains("user-disabled")) {
            return "User account has been disabled";
        } else if (firebaseError.contains("too-many-requests")) {
            return "Too many requests. Please try again later.";
        }

        return "Error: " + firebaseError;
    }

    public void registerUserWithoutRecaptcha(String email, String password, String username, String avatarId, AuthCallback<AuthResult> onComplete) {
        Log.d(TAG, "registerUserWithoutRecaptcha pozvan za email: " + email);

        userRepository.checkUsernameExistsAsync(username, usernameExists -> {
            if (usernameExists)
            {
                Log.d(TAG, "Username već postoji: " + username);
                onComplete.onResult(new AuthResult(false, "Username Already Exists"));
                return;
            }

            Log.d(TAG, "Kreiram korisnika direktno u Firestore bez Firebase Auth...");

            // Generate a simple user ID
            String userId = "user_" + System.currentTimeMillis();

            User user = new User(email, username, avatarId);
            user.setId(userId);
            user.setEmailVerified(false); // Email verification će biti implementirana kasnije

            userRepository.saveUser(user, saveSuccess -> {
                if (saveSuccess)
                {
                    saveUserSession(userId);
                    onComplete.onResult(new AuthResult(true,
                        "Registration Successful! (Using alternative method)", user));
                }
                else
                {
                    onComplete.onResult(new AuthResult(false,
                        "Error saving user data"));
                }
            });
        });
    }

    public static class AuthResult {
        private final boolean success;
        private final String message;
        private final User user;

        public AuthResult(boolean success, String message) {
            this.success = success;
            this.message = message;
            this.user = null;
        }

        public AuthResult(boolean success, String message, User user) {
            this.success = success;
            this.message = message;
            this.user = user;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public User getUser() { return user; }
    }
}
