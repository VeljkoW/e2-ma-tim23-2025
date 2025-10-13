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
import com.example.rpgapp.repository.MissionRepository;
import com.google.android.material.appbar.MaterialToolbar;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MissionDetailsActivity extends AppCompatActivity {
    private TextView textViewMissionName, textViewMissionDescription, textViewCategory, textViewDifficulty;
    private TextView textViewImportance, textViewFrequency, textViewRepeatInfo;
    private TextView textViewTotalXP, textViewCreatedDate, textViewDueDate;
    private LinearLayout layoutRepeatInfo, layoutStatusSection;
    private TextView chipActive, chipCompleted, chipPaused, chipCancelled;
    private Button buttonUpdateMission, buttonDeleteMission;

    private MissionRepository missionRepository;
    private Mission currentMission;
    private String missionId;
    private Mission.Status selectedStatus;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mission_details);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        initViews();
        setupListeners();

        missionRepository = new MissionRepository();
        missionId = getIntent().getStringExtra("MISSION_ID");

        if (missionId != null) {
            loadMissionDetails();
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
        layoutRepeatInfo = findViewById(R.id.layoutRepeatInfo);
        layoutStatusSection = findViewById(R.id.layoutStatusSection);

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
        selectedStatus = status;
        updateStatusChips();
        changeStatusToSelected();
    }

    private void updateStatusChips() {
        // Reset all chips
        chipActive.setSelected(false);
        chipCompleted.setSelected(false);
        chipPaused.setSelected(false);
        chipCancelled.setSelected(false);

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
                    missionRepository.completeMission(missionId);
                    break;
                case PAUSED:
                    // For PAUSED, update directly
                    currentMission.setStatus(selectedStatus);
                    missionRepository.updateMission(missionId, currentMission);
                    break;
                case CANCELLED:
                    missionRepository.cancelMission(missionId);
                    break;
                case ACTIVE:
                    missionRepository.activateMission(missionId);
                    break;
            }

            currentMission.setStatus(selectedStatus);
            Toast.makeText(this, "Status updated to " + selectedStatus.toString(), Toast.LENGTH_SHORT).show();
        }
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

        textViewCategory.setText(currentMission.getCategory() != null ?
            currentMission.getCategory().toString() : "");
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

        // Check if mission is past due
        boolean isPastDue = false;
        if (currentMission.getDueDateTime() != null) {
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 23);
            today.set(Calendar.MINUTE, 59);
            today.set(Calendar.SECOND, 59);
            isPastDue = currentMission.getDueDateTime().before(today.getTime());
        }

        // Hide status section for past-due missions
        if (isPastDue) {
            layoutStatusSection.setVisibility(View.GONE);
        } else {
            layoutStatusSection.setVisibility(View.VISIBLE);
            // Set current status
            selectedStatus = currentMission.getStatus() != null ? currentMission.getStatus() : Mission.Status.ACTIVE;
            updateStatusChips();
        }
    }

    private void updateMission() {
        Intent intent = new Intent(this, MissionEditActivity.class);
        intent.putExtra("MISSION_ID", missionId);
        startActivity(intent);
    }

    private void confirmDeleteMission() {
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
                    finish();
                } else {
                    Toast.makeText(this, "Failed to delete mission", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
