package com.example.rpgapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.rpgapp.MainActivity;
import com.example.rpgapp.R;
import com.example.rpgapp.callback.AuthCallback;
import com.example.rpgapp.service.AuthService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private TextView tvRegister, tvForgotPassword;
    private ProgressBar progressBar;

    private AuthService authService;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initViews();
        initServices();
        setupClickListeners();
        prefillEmailFromIntent();
    }

    private void initViews()
    {
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);

        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        progressBar = findViewById(R.id.progressBar);
    }

    private void initServices()
    {
        authService = new AuthService(this);
    }

    private void setupClickListeners()
    {
        btnLogin.setOnClickListener(v -> attemptLogin());

        tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(this, RegisterActivity.class);
            startActivity(intent);
            finish();
        });

        tvForgotPassword.setOnClickListener(v -> {
            Toast.makeText(this, "Needs to be implemented maybe probably most likely", Toast.LENGTH_SHORT).show();
        });
    }

    private void prefillEmailFromIntent()
    {
        String email = getIntent().getStringExtra("email");
        if (email != null)
        {
            etEmail.setText(email);
        }
    }

    private void attemptLogin()
    {
        // Clear previous errors
        tilEmail.setError(null);
        tilPassword.setError(null);

        // Get input values
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

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

        if (TextUtils.isEmpty(password))
        {
            tilPassword.setError("Password is required");
            hasError = true;
        }

        if (hasError)
        {
            return;
        }

        // Show progress and disable button
        setLoading(true);

        // Attempt login
        authService.loginUser(email, password, new AuthCallback<AuthService.AuthResult>()
        {
            @Override
            public void onResult(AuthService.AuthResult result)
            {
                runOnUiThread(() -> {
                    setLoading(false);

                    if (result.isSuccess())
                    {
                        Toast.makeText(LoginActivity.this, "Welcome!", Toast.LENGTH_SHORT).show();

                        // Go to main activity
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                    else
                    {
                        Toast.makeText(LoginActivity.this, result.getMessage(), Toast.LENGTH_LONG).show();

                        // Handle specific errors
                        if (result.getMessage().contains("email"))
                        {
                            tilEmail.setError(result.getMessage());
                        }
                        else if (result.getMessage().contains("password"))
                        {
                            tilPassword.setError(result.getMessage());
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
            btnLogin.setEnabled(false);
            btnLogin.setText("");
        }
        else
        {
            progressBar.setVisibility(View.GONE);
            btnLogin.setEnabled(true);
            btnLogin.setText("Login");
        }
    }

    @Override
    protected void onStart()
    {
        super.onStart();

        // Check if user is already logged in
        if (authService.isUserLoggedIn())
        {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }
}
