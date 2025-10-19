package com.example.rpgapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.rpgapp.R;
import com.example.rpgapp.callback.AuthCallback;
import com.example.rpgapp.service.AuthService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout tilEmail, tilUsername, tilPassword, tilConfirmPassword;
    private TextInputEditText etEmail, etUsername, etPassword, etConfirmPassword;
    private ImageView ivAvatar1, ivAvatar2, ivAvatar3, ivAvatar4, ivAvatar5;
    private MaterialButton btnRegister;
    private TextView tvLogin;
    private ProgressBar progressBar;

    private AuthService authService;
    private String selectedAvatarId = "avatar_1"; // Default selection
    private ImageView selectedAvatarView;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initViews();
        initServices();
        setupAvatarSelection();
        setupClickListeners();
    }

    private void initViews()
    {
        tilEmail = findViewById(R.id.tilEmail);
        tilUsername = findViewById(R.id.tilUsername);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);

        etEmail = findViewById(R.id.etEmail);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        ivAvatar1 = findViewById(R.id.ivAvatar1);
        ivAvatar2 = findViewById(R.id.ivAvatar2);
        ivAvatar3 = findViewById(R.id.ivAvatar3);
        ivAvatar4 = findViewById(R.id.ivAvatar4);
        ivAvatar5 = findViewById(R.id.ivAvatar5);

        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);
        progressBar = findViewById(R.id.progressBar);

        // Set default selected avatar
        selectedAvatarView = ivAvatar1;
        ivAvatar1.setSelected(true);

        // Position avatar scroll view to center to show that there are 5 avatars
        findViewById(R.id.avatarScrollView).post(() -> {
            android.widget.HorizontalScrollView scrollView = findViewById(R.id.avatarScrollView);
            int scrollWidth = scrollView.getChildAt(0).getWidth();
            int viewWidth = scrollView.getWidth();
            int scrollTo = (scrollWidth - viewWidth) / 2;
            scrollView.scrollTo(scrollTo, 0);
        });
    }

    private void initServices()
    {
        authService = new AuthService(this);
    }

    private void setupAvatarSelection()
    {
        View.OnClickListener avatarClickListener = v -> {
            // Deselect previous avatar
            if (selectedAvatarView != null)
            {
                selectedAvatarView.setSelected(false);
            }

            // Select new avatar
            v.setSelected(true);
            selectedAvatarView = (ImageView) v;
            selectedAvatarId = (String) v.getTag();
        };

        ivAvatar1.setOnClickListener(avatarClickListener);
        ivAvatar2.setOnClickListener(avatarClickListener);
        ivAvatar3.setOnClickListener(avatarClickListener);
        ivAvatar4.setOnClickListener(avatarClickListener);
        ivAvatar5.setOnClickListener(avatarClickListener);
    }

    private void setupClickListeners()
    {
        btnRegister.setOnClickListener(v -> attemptRegistration());

        tvLogin.setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void attemptRegistration()
    {
        // Clear previous errors
        tilEmail.setError(null);
        tilUsername.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);

        // Get input values
        String email = etEmail.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();

        // Validate inputs
        boolean hasError = false;

        if (TextUtils.isEmpty(email))
        {
            tilEmail.setError("Email is required");
            hasError = true;
        }
        else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches())
        {
            tilEmail.setError("Input a valid email address");
            hasError = true;
        }

        if (TextUtils.isEmpty(username))
        {
            tilUsername.setError("Username is required");
            hasError = true;
        }
        else if (username.length() < 3)
        {
            tilUsername.setError("Username must be at least 3 characters");
            hasError = true;
        }
        else if (username.length() > 20)
        {
            tilUsername.setError("Username must be at most 20 characters");
            hasError = true;
        }
        else if (!username.matches("^[a-zA-Z0-9_]+$"))
        {
            tilUsername.setError("Username can only contain letters, numbers, and underscores");
            hasError = true;
        }

        if (TextUtils.isEmpty(password))
        {
            tilPassword.setError("Password is required");
            hasError = true;
        }
        else if (password.length() < 6)
        {
            tilPassword.setError("Password must be at least 6 characters");
            hasError = true;
        }

        if (TextUtils.isEmpty(confirmPassword))
        {
            tilConfirmPassword.setError("Confirm your password");
            hasError = true;
        }
        else if (!password.equals(confirmPassword))
        {
            tilConfirmPassword.setError("Passwords do not match");
            hasError = true;
        }

        if (hasError)
        {
            return;
        }

        // Show progress and disable button
        setLoading(true);

        // Attempt registration
        authService.registerUser(email, password, username, selectedAvatarId, new AuthCallback<AuthService.AuthResult>()
        {
            @Override
            public void onResult(AuthService.AuthResult result)
            {
                runOnUiThread(() -> {
                    setLoading(false);

                    if (result.isSuccess())
                    {
                        Toast.makeText(RegisterActivity.this, result.getMessage(), Toast.LENGTH_LONG).show();

                        // Go to login screen
                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                        intent.putExtra("email", email);
                        startActivity(intent);
                        finish();
                    }
                    else
                    {
                        Toast.makeText(RegisterActivity.this, result.getMessage(), Toast.LENGTH_LONG).show();

                        // Handle specific errors
                        if (result.getMessage().contains("username"))
                        {
                            tilUsername.setError(result.getMessage());
                        }
                        else if (result.getMessage().contains("email"))
                        {
                            tilEmail.setError(result.getMessage());
                        }
                    }
                });
            }
        });
    }

    private void setLoading(boolean loading)
    {
        if (loading)
        {
            progressBar.setVisibility(View.VISIBLE);
            btnRegister.setEnabled(false);
            btnRegister.setText("");
        }
        else
        {
            progressBar.setVisibility(View.GONE);
            btnRegister.setEnabled(true);
            btnRegister.setText("Register");
        }
    }
}
