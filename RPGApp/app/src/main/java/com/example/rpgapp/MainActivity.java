package com.example.rpgapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.rpgapp.callback.AuthCallback;
import com.example.rpgapp.model.User;
import com.example.rpgapp.service.AuthService;
import com.example.rpgapp.service.BossService;
import com.example.rpgapp.ui.BossFightActivity;
import com.example.rpgapp.ui.CalendarActivity;
import com.example.rpgapp.ui.CategoriesListActivity;
import com.example.rpgapp.ui.CategoryCreationActivity;
import com.example.rpgapp.ui.LoginActivity;
import com.example.rpgapp.ui.ProfileActivity;
import com.example.rpgapp.ui.MissionCreationActivity;
import com.example.rpgapp.ui.MissionsListActivity;
import com.google.android.material.appbar.MaterialToolbar;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private AuthService authService;
    private BossService bossService;
    private Button bossFightButton;
    private Button createMissionButton;
    private Button viewMissionsButton;
    private Button createCategoryButton;
    private Button viewCategoriesButton;
    private Button calendarButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize services
        authService = new AuthService(this);
        bossService = new BossService(this);

        // Check if user is logged in
        if (!authService.isUserLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
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
        authService.getCurrentUser(new AuthCallback<>() {
            @Override
            public void onResult(User user) {
                if (user != null) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Welcome, " + user.getUsername() + "!", Toast.LENGTH_SHORT).show());
                }
            }
        });

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        bossFightButton = findViewById(R.id.btn_boss_fight);
        createMissionButton = findViewById(R.id.btn_create_mission);
        viewMissionsButton = findViewById(R.id.btn_view_missions);
        createCategoryButton = findViewById(R.id.btn_create_category);
        viewCategoriesButton = findViewById(R.id.btn_view_categories);
        calendarButton = findViewById(R.id.btn_calendar);
    }

    private void setupClickListeners() {
        bossFightButton.setOnClickListener(v -> openBossFight());
        createMissionButton.setOnClickListener(v -> startActivity(new Intent(this, MissionCreationActivity.class)));
        viewMissionsButton.setOnClickListener(v -> startActivity(new Intent(this, MissionsListActivity.class)));
        createCategoryButton.setOnClickListener(v -> startActivity(new Intent(this, CategoryCreationActivity.class)));
        viewCategoriesButton.setOnClickListener(v -> startActivity(new Intent(this, CategoriesListActivity.class)));
        calendarButton.setOnClickListener(v -> startActivity(new Intent(this, CalendarActivity.class)));
    }

    private void openBossFight() {
        String currentUserId = authService.getCurrentUserId();
        if (currentUserId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get the oldest existing alive boss for the user
        bossService.getOldestAliveBossForUser(currentUserId)
                .addOnSuccessListener(boss -> {
                    if (boss != null) {
                        Intent intent = new Intent(MainActivity.this, BossFightActivity.class);
                        intent.putExtra("boss_id", boss.getId());
                        startActivity(intent);
                    } else {
                        Toast.makeText(MainActivity.this, "No active boss available. Creating new boss...", Toast.LENGTH_SHORT).show();
                        // Create a new boss if none exists
                        bossService.createBossForUser(currentUserId)
                                .addOnSuccessListener(bossId -> {
                                    Intent intent = new Intent(MainActivity.this, BossFightActivity.class);
                                    intent.putExtra("boss_id", bossId);
                                    startActivity(intent);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to create boss", e);
                                    Toast.makeText(MainActivity.this, "Failed to create boss: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get boss", e);
                    Toast.makeText(this, "Failed to load boss: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
        // Add listener for Create Category button
        findViewById(R.id.btn_create_category).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, com.example.rpgapp.ui.CategoryCreationActivity.class);
            startActivity(intent);
        });

        // Add listener for View Categories button
        findViewById(R.id.btn_view_categories).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, com.example.rpgapp.ui.CategoriesListActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.buttonShop).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, com.example.rpgapp.ui.ShopActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.buttonEquipment).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, com.example.rpgapp.ui.EquipmentManagementActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_logout) {
            authService.signOut()
                    .addOnCompleteListener(task -> {
                        startActivity(new Intent(MainActivity.this, LoginActivity.class));
                        finish();
                    });
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
}