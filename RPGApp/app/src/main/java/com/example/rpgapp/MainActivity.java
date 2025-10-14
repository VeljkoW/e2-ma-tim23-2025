package com.example.rpgapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.rpgapp.callback.AuthCallback;
import com.example.rpgapp.model.User;
import com.example.rpgapp.service.AuthService;
import com.example.rpgapp.ui.LoginActivity;
import com.example.rpgapp.ui.ProfileActivity;
import com.example.rpgapp.database.DatabaseHelper;
import com.google.android.material.appbar.MaterialToolbar;

public class MainActivity extends AppCompatActivity {

    private AuthService authService;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize auth service
        authService = new AuthService(this);

        // Check if user is logged in
        if (!authService.isUserLoggedIn())
        {
            redirectToLogin();
            return;
        }

        setContentView(R.layout.activity_main);

        // Setup toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        // Welcome user
        authService.getCurrentUser(new AuthCallback<User>()
        {
            @Override
            public void onResult(User user) {
                if (user != null)
                {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "Welcome, " + user.getUsername() + "!", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if (id == R.id.action_logout)
        {
            logout();
            return true;
        }
        else if (id == R.id.action_profile)
        {
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void logout()
    {
        authService.logout();
        redirectToLogin();
    }

    private void redirectToLogin()
    {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}