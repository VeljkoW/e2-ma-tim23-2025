package com.example.rpgapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
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
import com.example.rpgapp.ui.ShopActivity;
import com.example.rpgapp.ui.EquipmentManagementActivity;
import com.example.rpgapp.ui.PreBossEquipmentActivity;
import com.google.android.material.appbar.MaterialToolbar;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private AuthService authService;
    private BossService bossService;
    private Button bossFightButton;
    private Button spawnBossButton;
    private Button createMissionButton;
    private Button viewMissionsButton;
    private Button createCategoryButton;
    private Button viewCategoriesButton;
    private Button calendarButton;
    private Button shopButton;
    private Button equipmentButton;
    private String currentUserId;

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
        // Get current user ID
        currentUserId = authService.getCurrentUserId();
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
        spawnBossButton = findViewById(R.id.btn_spawn_boss);
        createMissionButton = findViewById(R.id.btn_create_mission);
        viewMissionsButton = findViewById(R.id.btn_view_missions);
        createCategoryButton = findViewById(R.id.btn_create_category);
        viewCategoriesButton = findViewById(R.id.btn_view_categories);
        calendarButton = findViewById(R.id.btn_calendar);
        shopButton = findViewById(R.id.buttonShop);
        equipmentButton = findViewById(R.id.buttonEquipment);
    }

    private void setupClickListeners() {
        bossFightButton.setOnClickListener(v -> openBossFight());
        spawnBossButton.setOnClickListener(v -> spawnBoss());
        createMissionButton.setOnClickListener(v -> startActivity(new Intent(this, MissionCreationActivity.class)));
        viewMissionsButton.setOnClickListener(v -> startActivity(new Intent(this, MissionsListActivity.class)));
        createCategoryButton.setOnClickListener(v -> startActivity(new Intent(this, CategoryCreationActivity.class)));
        viewCategoriesButton.setOnClickListener(v -> startActivity(new Intent(this, CategoriesListActivity.class)));
        calendarButton.setOnClickListener(v -> startActivity(new Intent(this, CalendarActivity.class)));
        shopButton.setOnClickListener(v -> startActivity(new Intent(this, ShopActivity.class)));
        equipmentButton.setOnClickListener(v -> startActivity(new Intent(this, EquipmentManagementActivity.class)));
    }

    private void openBossFight() {
        bossService.getOldestAliveBossForUser(currentUserId)
                .addOnSuccessListener(boss -> {
                    if (boss != null) {
                        Intent intent = new Intent(MainActivity.this, PreBossEquipmentActivity.class);
                        intent.putExtra("boss_id", boss.getId());
                        startActivity(intent);
                    } else {
                        Toast.makeText(MainActivity.this, "No active boss available.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get boss", e);
                    Toast.makeText(this, "Failed to load boss: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void spawnBoss() {
        // Logic to spawn a new boss
        bossService.createBossForUser(currentUserId)
                .addOnSuccessListener(bossId -> {
                    Toast.makeText(MainActivity.this, "Boss spawned successfully!", Toast.LENGTH_SHORT).show();
                    // Refresh the boss fight button state
                    onResume();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to spawn boss", e);
                    Toast.makeText(this, "Failed to spawn boss: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check if there is an alive boss and enable/disable the button accordingly
        bossService.getOldestAliveBossForUser(currentUserId)
                .addOnSuccessListener(boss -> updateBossFightButtonState(boss != null))
                .addOnFailureListener(e -> updateBossFightButtonState(false));
    }

    private void updateBossFightButtonState(boolean hasBoss) {
        bossFightButton.setEnabled(hasBoss);

        if (hasBoss) {
            // Enable state: use original boss fight color
            bossFightButton.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.boss_fight_color));
            bossFightButton.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            bossFightButton.setAlpha(1.0f);
        } else {
            // Disabled state: use grey color and reduce opacity
            bossFightButton.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.boss_fight_disabled));
            bossFightButton.setTextColor(ContextCompat.getColor(this, R.color.text_hint));
            bossFightButton.setAlpha(0.6f);
        }
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
        } else if (id == R.id.action_profile) {
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}