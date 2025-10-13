package com.example.rpgapp.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.rpgapp.R;
import com.example.rpgapp.model.Mission;
import com.example.rpgapp.repository.MissionRepository;
import com.example.rpgapp.service.AuthService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Calendar;
import java.util.Date;

public class MissionCreationActivity extends AppCompatActivity {
    private EditText editTextName, editTextDescription, editTextRepeatInterval;
    private Spinner spinnerFrequency, spinnerRepeatUnit, spinnerCategory, spinnerDifficulty, spinnerImportance;
    private LinearLayout layoutRepeat;
    private Button buttonCreateMission, buttonSelectDueDate;
    private MissionRepository missionRepository;
    private AuthService authService;
    private Date selectedDueDate;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mission_creation);

        editTextName = findViewById(R.id.editTextName);
        editTextDescription = findViewById(R.id.editTextDescription);
        spinnerFrequency = findViewById(R.id.spinnerFrequency);
        editTextRepeatInterval = findViewById(R.id.editTextRepeatInterval);
        spinnerRepeatUnit = findViewById(R.id.spinnerRepeatUnit);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerDifficulty = findViewById(R.id.spinnerDifficulty);
        spinnerImportance = findViewById(R.id.spinnerImportance);
        layoutRepeat = findViewById(R.id.layoutRepeat);
        buttonCreateMission = findViewById(R.id.buttonCreateMission);
        buttonSelectDueDate = findViewById(R.id.buttonSelectDueDate);

        missionRepository = new MissionRepository(); // Adjust if using DI or singleton
        authService = new AuthService(this);

        // Populate spinners
        ArrayAdapter<Mission.FrequencyType> frequencyAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, Mission.FrequencyType.values());
        frequencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFrequency.setAdapter(frequencyAdapter);

        ArrayAdapter<Mission.RepeatUnit> repeatUnitAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, Mission.RepeatUnit.values());
        repeatUnitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRepeatUnit.setAdapter(repeatUnitAdapter);

        ArrayAdapter<Mission.Category> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, Mission.Category.values());
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        ArrayAdapter<Mission.Difficulty> difficultyAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, Mission.Difficulty.values());
        difficultyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDifficulty.setAdapter(difficultyAdapter);

        ArrayAdapter<Mission.Importance> importanceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, Mission.Importance.values());
        importanceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerImportance.setAdapter(importanceAdapter);

        spinnerFrequency.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Mission.FrequencyType freq = (Mission.FrequencyType) spinnerFrequency.getSelectedItem();
                layoutRepeat.setVisibility(freq == Mission.FrequencyType.REPEATING ? View.VISIBLE : View.GONE);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        buttonSelectDueDate.setOnClickListener(v -> showDatePicker());
        buttonCreateMission.setOnClickListener(v -> createMission());
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, 1); // Default to tomorrow

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(year, month, dayOfMonth, 23, 59, 59); // End of day
                    selectedDueDate = selectedCalendar.getTime();

                    // Update button text to show selected date
                    java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault());
                    buttonSelectDueDate.setText("Due: " + dateFormat.format(selectedDueDate));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        // Set minimum date to today
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void createMission() {
        String name = editTextName.getText().toString().trim();
        String description = editTextDescription.getText().toString().trim();
        Mission.FrequencyType frequency = (Mission.FrequencyType) spinnerFrequency.getSelectedItem();
        Integer repeatInterval = null;
        Mission.RepeatUnit repeatUnit = null;
        if (frequency == Mission.FrequencyType.REPEATING) {
            String intervalStr = editTextRepeatInterval.getText().toString().trim();
            if (!intervalStr.isEmpty()) {
                repeatInterval = Integer.parseInt(intervalStr);
            }
            repeatUnit = (Mission.RepeatUnit) spinnerRepeatUnit.getSelectedItem();
        }
        Mission.Category category = (Mission.Category) spinnerCategory.getSelectedItem();
        Mission.Difficulty difficulty = (Mission.Difficulty) spinnerDifficulty.getSelectedItem();
        Mission.Importance importance = (Mission.Importance) spinnerImportance.getSelectedItem();
        String userId = getCurrentUserId();

        if (name.isEmpty()) {
            Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (userId == null) {
            Toast.makeText(this, "You must be logged in to create missions", Toast.LENGTH_SHORT).show();
            return;
        }
        if (frequency == Mission.FrequencyType.REPEATING && (repeatInterval == null || repeatInterval <= 0)) {
            Toast.makeText(this, "Repeat interval must be positive", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedDueDate == null) {
            Toast.makeText(this, "Please select a due date", Toast.LENGTH_SHORT).show();
            return;
        }

        Mission mission = new Mission(
                null, // id will be set by repo/db
                name,
                description.isEmpty() ? null : description,
                frequency,
                repeatInterval,
                repeatUnit,
                category,
                difficulty,
                importance,
                userId,
                selectedDueDate // Add the due date
        );
        missionRepository.createMissionWithXPCalculation(mission).addOnCompleteListener(task -> {
            runOnUiThread(() -> {
                if (task.isSuccessful()) {
                    Toast.makeText(MissionCreationActivity.this, "Mission created!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Exception e = task.getException();
                    String errorMsg = (e != null) ? e.getMessage() : "Unknown error";
                    Toast.makeText(MissionCreationActivity.this, "Failed to create mission: " + errorMsg, Toast.LENGTH_LONG).show();
                    if (e != null) {
                        e.printStackTrace();
                    }
                }
            });
        });
    }

    private String getCurrentUserId() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            return currentUser.getUid();
        }
        return null; // Return null if no user is authenticated
    }
}
