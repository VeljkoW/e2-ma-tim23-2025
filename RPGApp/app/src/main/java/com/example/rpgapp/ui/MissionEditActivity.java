package com.example.rpgapp.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.rpgapp.R;
import com.example.rpgapp.model.Mission;
import com.example.rpgapp.model.Category;
import com.example.rpgapp.repository.MissionRepository;
import com.example.rpgapp.repository.CategoryRepository;
import com.example.rpgapp.service.AuthService;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseUser;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

public class MissionEditActivity extends AppCompatActivity {
    private EditText editTextName, editTextDescription, editTextRepeatInterval;
    private Spinner spinnerFrequency, spinnerRepeatUnit, spinnerCategory, spinnerDifficulty, spinnerImportance;
    private LinearLayout layoutRepeat;
    private Button buttonUpdateMission, buttonSelectDueDate;

    private MissionRepository missionRepository;
    private CategoryRepository categoryRepository;
    private AuthService authService;
    private Mission currentMission;
    private String missionId;
    private Date selectedDueDate;
    private List<Category> categories;
    private ArrayAdapter<Category> categoryAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mission_creation);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Edit Mission");
        }

        initViews();
        setupSpinners();

        missionRepository = new MissionRepository(this); // Pass context
        categoryRepository = new CategoryRepository();
        authService = new AuthService(this);

        missionId = getIntent().getStringExtra("MISSION_ID");

        if (missionId != null) {
            loadCategories();
        } else {
            finish();
        }
    }

    private void initViews() {
        editTextName = findViewById(R.id.editTextName);
        editTextDescription = findViewById(R.id.editTextDescription);
        spinnerFrequency = findViewById(R.id.spinnerFrequency);
        editTextRepeatInterval = findViewById(R.id.editTextRepeatInterval);
        spinnerRepeatUnit = findViewById(R.id.spinnerRepeatUnit);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerDifficulty = findViewById(R.id.spinnerDifficulty);
        spinnerImportance = findViewById(R.id.spinnerImportance);
        layoutRepeat = findViewById(R.id.layoutRepeat);
        buttonUpdateMission = findViewById(R.id.buttonCreateMission);
        buttonSelectDueDate = findViewById(R.id.buttonSelectDueDate);

        buttonUpdateMission.setText("Update Mission");
    }

    private void setupSpinners() {
        // Populate spinners
        ArrayAdapter<Mission.FrequencyType> frequencyAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, Mission.FrequencyType.values());
        frequencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFrequency.setAdapter(frequencyAdapter);

        ArrayAdapter<Mission.RepeatUnit> repeatUnitAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, Mission.RepeatUnit.values());
        repeatUnitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRepeatUnit.setAdapter(repeatUnitAdapter);

        // Categories will be loaded dynamically in loadCategories()

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
        buttonUpdateMission.setOnClickListener(v -> updateMission());
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        if (selectedDueDate != null) {
            calendar.setTime(selectedDueDate);
        } else {
            calendar.add(Calendar.DAY_OF_MONTH, 1); // Default to tomorrow
        }

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

    private void loadCategories() {
        FirebaseUser firebaseUser = authService.getCurrentFirebaseUser();
        if (firebaseUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        categoryRepository.getCategoriesByUserId(firebaseUser.getUid(), new CategoryRepository.CategoryCallback<List<Category>>() {
            @Override
            public void onResult(List<Category> result) {
                runOnUiThread(() -> {
                    categories = result;
                    if (categories.isEmpty()) {
                        categories = new ArrayList<>();
                        Category noCategory = new Category();
                        noCategory.setName("No categories available");
                        categories.add(noCategory);
                    }

                    categoryAdapter = new ArrayAdapter<>(MissionEditActivity.this, android.R.layout.simple_spinner_item, categories);
                    categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerCategory.setAdapter(categoryAdapter);

                    // Now load the mission data
                    loadMissionForEdit();
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(MissionEditActivity.this, "Error loading categories: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void loadMissionForEdit() {
        missionRepository.getMissionById(missionId).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                currentMission = task.getResult();
                runOnUiThread(() -> populateFields());
            } else {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Failed to load mission", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    private void populateFields() {
        if (currentMission == null) return;

        editTextName.setText(currentMission.getName());
        editTextDescription.setText(currentMission.getDescription() != null ? currentMission.getDescription() : "");

        // Set spinner selections
        if (currentMission.getFrequency() != null) {
            for (int i = 0; i < Mission.FrequencyType.values().length; i++) {
                if (Mission.FrequencyType.values()[i] == currentMission.getFrequency()) {
                    spinnerFrequency.setSelection(i);
                    break;
                }
            }
        }

        // Find and set category by categoryId
        if (currentMission.getCategoryId() != null) {
            for (int i = 0; i < categories.size(); i++) {
                if (categories.get(i).getId() != null && categories.get(i).getId().equals(currentMission.getCategoryId())) {
                    spinnerCategory.setSelection(i);
                    break;
                }
            }
        }

        if (currentMission.getDifficulty() != null) {
            for (int i = 0; i < Mission.Difficulty.values().length; i++) {
                if (Mission.Difficulty.values()[i] == currentMission.getDifficulty()) {
                    spinnerDifficulty.setSelection(i);
                    break;
                }
            }
        }

        if (currentMission.getImportance() != null) {
            for (int i = 0; i < Mission.Importance.values().length; i++) {
                if (Mission.Importance.values()[i] == currentMission.getImportance()) {
                    spinnerImportance.setSelection(i);
                    break;
                }
            }
        }

        // Handle repeat fields
        if (currentMission.getFrequency() == Mission.FrequencyType.REPEATING) {
            layoutRepeat.setVisibility(View.VISIBLE);
            if (currentMission.getRepeatInterval() != null) {
                editTextRepeatInterval.setText(String.valueOf(currentMission.getRepeatInterval()));
            }
            if (currentMission.getRepeatUnit() != null) {
                for (int i = 0; i < Mission.RepeatUnit.values().length; i++) {
                    if (Mission.RepeatUnit.values()[i] == currentMission.getRepeatUnit()) {
                        spinnerRepeatUnit.setSelection(i);
                        break;
                    }
                }
            }
        }

        // Populate due date
        selectedDueDate = currentMission.getDueDateTime();
        if (selectedDueDate != null) {
            java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault());
            buttonSelectDueDate.setText("Due: " + dateFormat.format(selectedDueDate));
        }
    }

    private void updateMission() {
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

        // Get selected category
        Category selectedCategory = (Category) spinnerCategory.getSelectedItem();
        String categoryId = null;
        if (selectedCategory != null && selectedCategory.getId() != null) {
            categoryId = selectedCategory.getId();
        }

        Mission.Difficulty difficulty = (Mission.Difficulty) spinnerDifficulty.getSelectedItem();
        Mission.Importance importance = (Mission.Importance) spinnerImportance.getSelectedItem();

        if (name.isEmpty()) {
            Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show();
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

        if (categoryId == null) {
            Toast.makeText(this, "Please select a valid category", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update current mission fields
        currentMission.setName(name);
        currentMission.setDescription(description.isEmpty() ? null : description);
        currentMission.setFrequency(frequency);
        currentMission.setRepeatInterval(repeatInterval);
        currentMission.setRepeatUnit(repeatUnit);
        currentMission.setCategoryId(categoryId);
        currentMission.setDifficulty(difficulty);
        currentMission.setImportance(importance);
        currentMission.setDueDateTime(selectedDueDate);

        // Use new method that recalculates XP based on user level
        missionRepository.updateMissionWithXPCalculation(missionId, currentMission).addOnCompleteListener(task -> {
            runOnUiThread(() -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Mission updated!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(this, "Failed to update mission", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }
}
