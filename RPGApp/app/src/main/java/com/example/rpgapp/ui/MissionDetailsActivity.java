package com.example.rpgapp.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.rpgapp.R;
import com.example.rpgapp.model.Mission;
import com.example.rpgapp.model.User;
import com.example.rpgapp.model.Category;
import com.example.rpgapp.repository.MissionRepository;
import com.example.rpgapp.repository.UserRepository;
import com.example.rpgapp.repository.CategoryRepository;
import com.example.rpgapp.service.AuthService;
import com.example.rpgapp.service.BossService;
import com.example.rpgapp.service.AllianceBossService;
import com.example.rpgapp.callback.AuthCallback;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseUser;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

public class MissionDetailsActivity extends AppCompatActivity {
    private TextView textViewMissionName, textViewMissionDescription, textViewCategory, textViewDifficulty;
    private TextView textViewImportance, textViewFrequency, textViewRepeatInfo;
    private TextView textViewTotalXP, textViewCreatedDate, textViewDueDate, textViewFinalizationDate, textViewFinalizationLabel;
    private LinearLayout layoutRepeatInfo, layoutStatusSection, layoutFinalizationDate;
    private TextView chipActive, chipCompleted, chipPaused, chipCancelled;
    private Button buttonUpdateMission, buttonDeleteMission;

    private MissionRepository missionRepository;
    private Mission currentMission;
    private String missionId;
    private Mission.Status selectedStatus;

    // User repository and auth service for XP awarding
    private UserRepository userRepository;
    private AuthService authService;
    private CategoryRepository categoryRepository;
    private Map<String, Category> categoryCache;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mission_details);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        initViews();
        setupListeners();

        missionRepository = new MissionRepository(this); // Pass context
        userRepository = new UserRepository(this);
        authService = new AuthService(this);
        categoryRepository = new CategoryRepository();
        categoryCache = new HashMap<>();

        missionId = getIntent().getStringExtra("MISSION_ID");

        if (missionId != null) {
            loadCategoriesAndMissionDetails();
        } else {
            finish();
        }
    }

    private void initViews() {
        textViewMissionName = findViewById(R.id.textViewMissionName);
        textViewMissionDescription = findViewById(R.id.textViewMissionDescription);
        textViewCategory = findViewById(R.id.textViewCategory);
        textViewDifficulty = findViewById(R.id.textViewDifficulty);
        textViewImportance = findViewById(R.id.textViewImportance);
        textViewFrequency = findViewById(R.id.textViewFrequency);
        textViewRepeatInfo = findViewById(R.id.textViewRepeatInfo);
        textViewTotalXP = findViewById(R.id.textViewTotalXP);
        textViewCreatedDate = findViewById(R.id.textViewCreatedDate);
        textViewDueDate = findViewById(R.id.textViewDueDate);
        textViewFinalizationDate = findViewById(R.id.textViewFinalizationDate);
        textViewFinalizationLabel = findViewById(R.id.textViewFinalizationLabel);
        layoutRepeatInfo = findViewById(R.id.layoutRepeatInfo);
        layoutStatusSection = findViewById(R.id.layoutStatusSection);
        layoutFinalizationDate = findViewById(R.id.layoutFinalizationDate);

        // Status chips
        chipActive = findViewById(R.id.chipActive);
        chipCompleted = findViewById(R.id.chipCompleted);
        chipPaused = findViewById(R.id.chipPaused);
        chipCancelled = findViewById(R.id.chipCancelled);

        buttonUpdateMission = findViewById(R.id.buttonUpdateMission);
        buttonDeleteMission = findViewById(R.id.buttonDeleteMission);
    }

    private void setupListeners() {
        chipActive.setOnClickListener(v -> selectStatus(Mission.Status.ACTIVE));
        chipCompleted.setOnClickListener(v -> selectStatus(Mission.Status.COMPLETED));
        chipPaused.setOnClickListener(v -> selectStatus(Mission.Status.PAUSED));
        chipCancelled.setOnClickListener(v -> selectStatus(Mission.Status.CANCELLED));

        buttonUpdateMission.setOnClickListener(v -> updateMission());
        buttonDeleteMission.setOnClickListener(v -> confirmDeleteMission());
    }

    private void selectStatus(Mission.Status status) {
        // Check if current mission is in a final state
        if (isFinalState(currentMission.getStatus())) {
            Toast.makeText(this, "Cannot change status of completed or cancelled missions", Toast.LENGTH_SHORT).show();
            return;
        }

        // Handle paused state transitions
        if (currentMission.getStatus() == Mission.Status.PAUSED && status == Mission.Status.ACTIVE) {
            handleUnpauseLogic();
            return;
        }

        selectedStatus = status;
        updateStatusChips();
        changeStatusToSelected();
    }

    private boolean isFinalState(Mission.Status status) {
        return status == Mission.Status.COMPLETED || status == Mission.Status.CANCELLED;
    }

    private void handleUnpauseLogic() {
        // Check if mission is past due
        if (isPastDue()) {
            // Show dialog to choose between completed or cancelled - no option to reactivate
            showPostDueUnpauseDialog();
        } else {
            // Only allow reactivation if not past due
            selectedStatus = Mission.Status.ACTIVE;
            updateStatusChips();
            changeStatusToSelected();
        }
    }

    private boolean isPastDue() {
        if (currentMission.getDueDateTime() == null) {
            return false;
        }

        // Get start of today (00:00:00)
        Calendar startOfToday = Calendar.getInstance();
        startOfToday.set(Calendar.HOUR_OF_DAY, 0);
        startOfToday.set(Calendar.MINUTE, 0);
        startOfToday.set(Calendar.SECOND, 0);
        startOfToday.set(Calendar.MILLISECOND, 0);

        // Mission is past due if its due date is before the start of today
        return currentMission.getDueDateTime().before(startOfToday.getTime());
    }

    private void showPostDueUnpauseDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Mission Past Due")
                .setMessage("This mission is past its due date. You must either mark it as completed or cancelled - it cannot be reactivated.")
                .setPositiveButton("Mark Completed", (dialog, which) -> {
                    selectedStatus = Mission.Status.COMPLETED;
                    updateStatusChips();
                    changeStatusToSelected();
                })
                .setNegativeButton("Mark Cancelled", (dialog, which) -> {
                    selectedStatus = Mission.Status.CANCELLED;
                    updateStatusChips();
                    changeStatusToSelected();
                })
                .setCancelable(false) // Force user to make a choice
                .show();
    }

    private void updateStatusChips() {
        // Reset all chips
        chipActive.setSelected(false);
        chipCompleted.setSelected(false);
        chipPaused.setSelected(false);
        chipCancelled.setSelected(false);

        // Hide status chips and buttons for final states
        if (isFinalState(currentMission.getStatus())) {
            // Apply final state visual styling
            layoutStatusSection.setAlpha(0.6f); // Make it look disabled

            // Disable all chips
            chipActive.setEnabled(false);
            chipCompleted.setEnabled(false);
            chipPaused.setEnabled(false);
            chipCancelled.setEnabled(false);

            // Hide action buttons
            buttonUpdateMission.setVisibility(View.GONE);
            buttonDeleteMission.setVisibility(View.GONE);
        } else {
            // Enable status changes for non-final states
            layoutStatusSection.setAlpha(1.0f);

            // For paused missions, only show active option
            if (currentMission.getStatus() == Mission.Status.PAUSED) {
                chipActive.setEnabled(true);
                chipCompleted.setEnabled(false);
                chipPaused.setEnabled(false);
                chipCancelled.setEnabled(false);
            } else {
                // For active missions, show all options
                chipActive.setEnabled(true);
                chipCompleted.setEnabled(true);
                chipPaused.setEnabled(true);
                chipCancelled.setEnabled(true);
            }

            // Keep action buttons visible
            buttonUpdateMission.setVisibility(View.VISIBLE);
            buttonDeleteMission.setVisibility(View.VISIBLE);
        }

        // Set colors based on status
        chipActive.setBackgroundColor(selectedStatus == Mission.Status.ACTIVE ? 0xFF4CAF50 : 0x80000000);
        chipCompleted.setBackgroundColor(selectedStatus == Mission.Status.COMPLETED ? 0xFF2196F3 : 0x80000000);
        chipPaused.setBackgroundColor(selectedStatus == Mission.Status.PAUSED ? 0xFFFFC107 : 0x80000000);
        chipCancelled.setBackgroundColor(selectedStatus == Mission.Status.CANCELLED ? 0xFF9E9E9E : 0x80000000);

        // Mark selected chip
        switch (selectedStatus) {
            case ACTIVE:
                chipActive.setSelected(true);
                break;
            case COMPLETED:
                chipCompleted.setSelected(true);
                break;
            case PAUSED:
                chipPaused.setSelected(true);
                break;
            case CANCELLED:
                chipCancelled.setSelected(true);
                break;
        }
    }

    private void changeStatusToSelected() {
        if (selectedStatus != null && !selectedStatus.equals(currentMission.getStatus())) {
            switch (selectedStatus) {
                case COMPLETED:
                    currentMission.setFinalizationDateTime(new java.util.Date());
                    currentMission.setStatus(selectedStatus);
                    missionRepository.updateMission(missionId, currentMission);
                    awardXpForMission(); // Promenjeno - sada prosleđuje celu misiju

                    // Damage alliance boss when mission is completed
                    damageAllianceBossFromMissionCompletion();

                    setResult(RESULT_OK);
                    Toast.makeText(this, "Mission completed!", Toast.LENGTH_SHORT).show();
                    break;
                case PAUSED:
                    currentMission.setStatus(selectedStatus);
                    missionRepository.updateMission(missionId, currentMission);
                    setResult(RESULT_OK);
                    Toast.makeText(this, "Mission paused", Toast.LENGTH_SHORT).show();
                    break;
                case CANCELLED:
                    currentMission.setFinalizationDateTime(new java.util.Date());
                    currentMission.setStatus(selectedStatus);
                    missionRepository.updateMission(missionId, currentMission);
                    setResult(RESULT_OK);
                    Toast.makeText(this, "Mission cancelled", Toast.LENGTH_SHORT).show();
                    break;
                case ACTIVE:
                    currentMission.setStatus(selectedStatus);
                    missionRepository.updateMission(missionId, currentMission);
                    setResult(RESULT_OK);
                    Toast.makeText(this, "Mission reactivated", Toast.LENGTH_SHORT).show();
                    break;
            }

            // Refresh the UI to reflect the new state
            populateViews();
        }
    }

    private void loadCategoriesAndMissionDetails() {
        FirebaseUser firebaseUser = authService.getCurrentFirebaseUser();
        if (firebaseUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // First load categories, then load mission details
        categoryRepository.getCategoriesByUserId(firebaseUser.getUid(), new CategoryRepository.CategoryCallback<List<Category>>() {
            @Override
            public void onResult(List<Category> categories) {
                runOnUiThread(() -> {
                    // Populate category cache
                    categoryCache.clear();
                    for (Category category : categories) {
                        if (category.getId() != null) {
                            categoryCache.put(category.getId(), category);
                        }
                    }

                    // Now load mission details
                    loadMissionDetails();
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(MissionDetailsActivity.this, "Error loading categories: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    // Still try to load mission details even if categories fail
                    loadMissionDetails();
                });
            }
        });
    }

    private void loadMissionDetails() {
        missionRepository.getMissionById(missionId).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                currentMission = task.getResult();
                runOnUiThread(() -> populateViews());
            } else {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Failed to load mission details", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    private void populateViews() {
        if (currentMission == null) return;

        textViewMissionName.setText(currentMission.getName());
        textViewMissionDescription.setText(currentMission.getDescription() != null ?
            currentMission.getDescription() : "No description");

        // Get category name from cache using categoryId
        String categoryName = getCategoryName(currentMission.getCategoryId());
        textViewCategory.setText(categoryName);

        textViewDifficulty.setText(currentMission.getDifficulty() != null ?
            currentMission.getDifficulty().toString() : "");
        textViewImportance.setText(currentMission.getImportance() != null ?
            currentMission.getImportance().toString() : "");
        textViewFrequency.setText(currentMission.getFrequency() != null ?
            currentMission.getFrequency().toString() : "");

        // Show repeat info if mission is repeating
        if (currentMission.getFrequency() == Mission.FrequencyType.REPEATING &&
            currentMission.getRepeatInterval() != null && currentMission.getRepeatUnit() != null) {
            layoutRepeatInfo.setVisibility(View.VISIBLE);
            textViewRepeatInfo.setText(currentMission.getRepeatInterval() + " " +
                currentMission.getRepeatUnit().toString());
        } else {
            layoutRepeatInfo.setVisibility(View.GONE);
        }

        textViewTotalXP.setText(currentMission.getTotalXP() + " XP");

        // Format creation date
        if (currentMission.getCreateDateTime() != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            textViewCreatedDate.setText(dateFormat.format(currentMission.getCreateDateTime()));
        }

        // Format due date
        if (currentMission.getDueDateTime() != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            textViewDueDate.setText(dateFormat.format(currentMission.getDueDateTime()));
        }

        // Handle finalization date display - simplified approach
        if (currentMission.getFinalizationDateTime() != null && isFinalState(currentMission.getStatus())) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            textViewFinalizationDate.setText(dateFormat.format(currentMission.getFinalizationDateTime()));
            layoutFinalizationDate.setVisibility(View.VISIBLE);
        } else {
            layoutFinalizationDate.setVisibility(View.GONE);
        }

        // Set current status and update UI accordingly
        selectedStatus = currentMission.getStatus() != null ? currentMission.getStatus() : Mission.Status.ACTIVE;

        // Always show status section but handle final states appropriately
        layoutStatusSection.setVisibility(View.VISIBLE);
        updateStatusChips();

        // Set finalization label text based on mission status
        if (currentMission.getStatus() == Mission.Status.COMPLETED) {
            textViewFinalizationLabel.setText("Completed on:");
        } else if (currentMission.getStatus() == Mission.Status.CANCELLED) {
            textViewFinalizationLabel.setText("Cancelled on:");
        } else {
            textViewFinalizationLabel.setText("");
        }
    }

    private String getCategoryName(String categoryId) {
        if (categoryId != null && categoryCache.containsKey(categoryId)) {
            Category category = categoryCache.get(categoryId);
            return category.getName();
        }
        return "No Category"; // Default text if category not found
    }

    private void updateMission() {
        // Check if mission is past due and prevent updates
        if (isPastDue() && !isFinalState(currentMission.getStatus())) {
            Toast.makeText(this, "Cannot update a mission that is past its due date. Please complete or cancel it first.", Toast.LENGTH_LONG).show();
            return;
        }

        Intent intent = new Intent(this, MissionEditActivity.class);
        intent.putExtra("MISSION_ID", missionId);
        startActivity(intent);
    }

    private void confirmDeleteMission() {
        // Check if mission is past due and prevent deletion of non-final missions
        if (isPastDue() && !isFinalState(currentMission.getStatus())) {
            Toast.makeText(this, "Cannot delete a mission that is past its due date. Please complete or cancel it first.", Toast.LENGTH_LONG).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Delete Mission")
                .setMessage("Are you sure you want to delete this mission? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteMission())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteMission() {
        missionRepository.deleteMission(missionId).addOnCompleteListener(task -> {
            runOnUiThread(() -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Mission deleted", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK); // Indicate data was changed
                    finish();
                } else {
                    Toast.makeText(this, "Failed to delete mission", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void awardXpForMission() {
        android.util.Log.d("MissionDetails", "Starting XP award process for mission: " + currentMission.getName());

        // Get the current user
        authService.getCurrentUser(new AuthCallback<User>() {
            @Override
            public void onResult(User user) {
                if (user != null) {
                    android.util.Log.d("MissionDetails", "User retrieved successfully: " + user.getUsername() + " (ID: " + user.getId() + "), Level: " + user.getLevel());

                    // Calculate XP based on user level and mission difficulty/importance
                    int xpAmount = user.calculateMissionXP(currentMission.getDifficulty(), currentMission.getImportance());
                    android.util.Log.d("MissionDetails", "Calculated XP for mission based on user level " + user.getLevel() + ": " + xpAmount + " XP");

                    // Add XP to user first
                    user.addExperiencePoints(xpAmount);

                    // Check if user should level up and create boss
                    if (user.levelUpAndShouldCreateBoss()) {
                        android.util.Log.d("MissionDetails", "User leveled up! New level: " + user.getLevel());

                        // Save the updated user (with new level and XP) to database
                        userRepository.updateUser(user, new AuthCallback<Boolean>() {
                            @Override
                            public void onResult(Boolean success) {
                                if (success != null && success) {
                                    android.util.Log.d("MissionDetails", "User level updated successfully in database");

                                    // Create a new boss for the leveled up user
                                    BossService bossService = new BossService(MissionDetailsActivity.this);
                                    bossService.createBossForUser(user.getId())
                                        .addOnSuccessListener(bossId -> {
                                            android.util.Log.d("MissionDetails", "New boss created with ID: " + bossId);
                                            runOnUiThread(() -> {
                                                Toast.makeText(MissionDetailsActivity.this,
                                                    "Mission completed! Awarded " + xpAmount + " XP! Level up to " + user.getLevel() + "! New boss spawned!",
                                                    Toast.LENGTH_LONG).show();
                                            });
                                        })
                                        .addOnFailureListener(e -> {
                                            android.util.Log.e("MissionDetails", "Failed to create boss after level up", e);
                                            runOnUiThread(() -> {
                                                Toast.makeText(MissionDetailsActivity.this,
                                                    "Mission completed! Leveled up to " + user.getLevel() + " but failed to create boss: " + e.getMessage(),
                                                    Toast.LENGTH_LONG).show();
                                            });
                                        });
                                } else {
                                    android.util.Log.e("MissionDetails", "Failed to update user in database after level up");
                                    runOnUiThread(() -> {
                                        Toast.makeText(MissionDetailsActivity.this,
                                            "Mission completed! Leveled up but failed to save progress",
                                            Toast.LENGTH_SHORT).show();
                                    });
                                }
                            }
                        });
                    } else {
                        // No level up, just award XP normally
                        userRepository.awardXpToUser(user.getId(), xpAmount).addOnCompleteListener(task -> {
                            runOnUiThread(() -> {
                                if (task.isSuccessful()) {
                                    android.util.Log.d("MissionDetails", "XP awarded successfully via Firebase!");
                                    Toast.makeText(MissionDetailsActivity.this, "Mission completed! Awarded " + xpAmount + " XP!", Toast.LENGTH_LONG).show();
                                } else {
                                    // Firebase failed, try fallback method
                                    android.util.Log.w("MissionDetails", "Firebase XP award failed, trying fallback method");
                                    userRepository.awardXpToUserFallback(user.getId(), xpAmount).addOnCompleteListener(fallbackTask -> {
                                        runOnUiThread(() -> {
                                            if (fallbackTask.isSuccessful()) {
                                                android.util.Log.d("MissionDetails", "XP awarded successfully via fallback method!");
                                                Toast.makeText(MissionDetailsActivity.this, "Mission completed! Awarded " + xpAmount + " XP! (Local save)", Toast.LENGTH_LONG).show();
                                            } else {
                                                Exception exception = fallbackTask.getException();
                                                String errorMsg = exception != null ? exception.getMessage() : "Unknown error";
                                                android.util.Log.e("MissionDetails", "Both Firebase and fallback methods failed: " + errorMsg);
                                                Toast.makeText(MissionDetailsActivity.this, "Mission completed, but failed to award XP: " + errorMsg, Toast.LENGTH_LONG).show();
                                            }
                                        });
                                    });
                                }
                            });
                        });
                    }
                } else {
                    android.util.Log.e("MissionDetails", "User is null - cannot award XP");
                    runOnUiThread(() -> {
                        Toast.makeText(MissionDetailsActivity.this, "Error: Could not find user to award XP", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void damageAllianceBossFromMissionCompletion() {
        // Get the current user
        authService.getCurrentUser(new AuthCallback<User>() {
            @Override
            public void onResult(User user) {
                if (user != null) {
                    // Calculate damage based on mission difficulty and importance
                    int damageAmount = calculateAllianceBossDamageFromMission();

                    // Log the damage calculation
                    android.util.Log.d("MissionDetails", "Calculated damage for alliance boss from mission completion: " + damageAmount);

                    // Create custom damage method for exact HP amounts
                    damageAllianceBossCustomAmount(user.getId(), damageAmount);
                } else {
                    android.util.Log.e("MissionDetails", "User is null - cannot deal damage to alliance boss");
                }
            }
        });
    }

    private void damageAllianceBossCustomAmount(String userId, int damage) {
        // Find user's alliance and damage boss with exact amount
        com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("alliances")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    String userAllianceId = null;
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        try {
                            java.util.List<String> memberIds = (java.util.List<String>) doc.get("memberIds");
                            if (memberIds != null && memberIds.contains(userId)) {
                                userAllianceId = doc.getId();
                                break;
                            }
                        } catch (Exception e) {
                            android.util.Log.w("MissionDetails", "Error checking alliance", e);
                        }
                    }

                    if (userAllianceId != null) {
                        // Find alive alliance boss and damage it
                        com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("allianceBosses")
                                .whereEqualTo("allianceId", userAllianceId)
                                .whereEqualTo("status", "ALIVE")
                                .get()
                                .addOnSuccessListener(bossQuery -> {
                                    if (!bossQuery.isEmpty()) {
                                        com.google.firebase.firestore.DocumentSnapshot bossDoc = bossQuery.getDocuments().get(0);
                                        String bossId = bossDoc.getId();
                                        Long currentHpLong = (Long) bossDoc.get("currentHp");
                                        int currentHp = currentHpLong != null ? currentHpLong.intValue() : 0;
                                        int newHp = Math.max(0, currentHp - damage);

                                        // Update boss HP
                                        com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("allianceBosses")
                                                .document(bossId)
                                                .update("currentHp", newHp)
                                                .addOnSuccessListener(aVoid -> {
                                                    android.util.Log.d("MissionDetails", "Alliance boss damaged: " + currentHp + " -> " + newHp + " (damage: " + damage + ")");
                                                    // Mark as dead if HP reaches 0
                                                    if (newHp <= 0) {
                                                        com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("allianceBosses")
                                                                .document(bossId)
                                                                .update("status", "DEAD", "dateOfLastDyingOrFailing", new java.util.Date());
                                                    }
                                                });
                                    }
                                });
                    }
                });
    }

    private int calculateAllianceBossDamageFromMission() {
        // Default to 1 HP damage for easy/normal missions
        int damage = 1;

        // Check if mission qualifies for higher damage (4 HP)
        // HARD/EXTREMELY_HARD difficulty OR EXTREMELY_IMPORTANT importance = 4 damage
        if ((currentMission.getDifficulty() == Mission.Difficulty.HARD ||
             currentMission.getDifficulty() == Mission.Difficulty.EXTREMELY_HARD) ||
            (currentMission.getImportance() == Mission.Importance.EXTREMELY_IMPORTANT)) {
            damage = 4;
        }

        android.util.Log.d("MissionDetails", "Mission damage calculation: Difficulty=" + currentMission.getDifficulty() +
                          ", Importance=" + currentMission.getImportance() + ", Damage=" + damage);

        return damage;
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}

