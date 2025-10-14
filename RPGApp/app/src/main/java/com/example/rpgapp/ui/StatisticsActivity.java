package com.example.rpgapp.ui;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rpgapp.R;
import com.example.rpgapp.model.UserStatistics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class StatisticsActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private UserStatistics statistics;

    private TextView tvActiveDays, tvCompletedCount, tvIncompleteCount, tvCancelledCount;
    private TextView tvLongestStreak, tvCurrentStreak, tvAverageDifficulty, tvAverageXP;
    private TextView tvMissionsStarted, tvMissionsCompleted;
    private RecyclerView rvCategoryStats;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        loadStatistics();
        setupClickListeners();
    }

    private void initViews()
    {
        tvActiveDays = findViewById(R.id.tvActiveDays);
        tvCompletedCount = findViewById(R.id.tvCompletedCount);
        tvIncompleteCount = findViewById(R.id.tvIncompleteCount);
        tvCancelledCount = findViewById(R.id.tvCancelledCount);
        tvLongestStreak = findViewById(R.id.tvLongestStreak);
        tvCurrentStreak = findViewById(R.id.tvCurrentStreak);
        tvAverageDifficulty = findViewById(R.id.tvAverageDifficulty);
        tvAverageXP = findViewById(R.id.tvAverageXP);
        tvMissionsStarted = findViewById(R.id.tvMissionsStarted);
        tvMissionsCompleted = findViewById(R.id.tvMissionsCompleted);
        rvCategoryStats = findViewById(R.id.rvCategoryStats);
        btnBack = findViewById(R.id.btnBack);

        rvCategoryStats.setLayoutManager(new LinearLayoutManager(this));
    }

    private void loadStatistics() {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser == null) {
            finish();
            return;
        }

        String userId = firebaseUser.getUid();
        db.collection("statistics").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        statistics = documentSnapshot.toObject(UserStatistics.class);
                        if (statistics != null) {
                            displayStatistics();
                        }
                    } else {
                        // Create initial statistics
                        statistics = new UserStatistics(userId);
                        displayStatistics();
                    }
                })
                .addOnFailureListener(e -> {
                    // Display default values
                    statistics = new UserStatistics(userId);
                    displayStatistics();
                });
    }

    private void displayStatistics() {
        // Active Days Streak
        tvActiveDays.setText(String.valueOf(statistics.getActiveDaysStreak()));

        // Tasks Overview
        tvCompletedCount.setText(String.valueOf(statistics.getTotalTasksCompleted()));
        tvIncompleteCount.setText(String.valueOf(statistics.getTotalTasksIncomplete()));
        tvCancelledCount.setText(String.valueOf(statistics.getTotalTasksCancelled()));

        // Streaks
        tvLongestStreak.setText(String.valueOf(statistics.getLongestTaskStreak()));
        tvCurrentStreak.setText(String.valueOf(statistics.getCurrentTaskStreak()));

        // Average Difficulty
        double avgDifficulty = statistics.getAverageTaskDifficulty();
        String difficultyText = getDifficultyText(avgDifficulty);
        tvAverageDifficulty.setText(difficultyText);
        tvAverageXP.setText(String.format("%.0f XP avg", avgDifficulty));

        // Special Missions
        tvMissionsStarted.setText(String.valueOf(statistics.getSpecialMissionsStarted()));
        tvMissionsCompleted.setText(String.valueOf(statistics.getSpecialMissionsCompleted()));

        // TODO: Display charts for category stats and XP last 7 days
    }

    private String getDifficultyText(double avgXP) {
        if (avgXP < 50) return "Easy";
        else if (avgXP < 100) return "Medium";
        else if (avgXP < 200) return "Hard";
        else return "Very Hard";
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
    }
}

