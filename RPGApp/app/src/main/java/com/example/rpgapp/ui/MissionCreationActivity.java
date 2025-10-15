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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

public class MissionCreationActivity extends AppCompatActivity {
    private EditText editTextName, editTextDescription, editTextRepeatInterval;
    private Spinner spinnerFrequency, spinnerRepeatUnit, spinnerCategory, spinnerDifficulty, spinnerImportance;
    private LinearLayout layoutRepeat;
    private Button buttonCreateMission, buttonSelectDueDate;
    private MissionRepository missionRepository;
    private CategoryRepository categoryRepository;
    private AuthService authService;
    private Date selectedDueDate;
    private List<Category> categories;
    private ArrayAdapter<Category> categoryAdapter;

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

        missionRepository = new MissionRepository(this); // Pass context
        categoryRepository = new CategoryRepository();
        authService = new AuthService(this);

        // Setup action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Create Mission");
        }

        // Populate spinners
        ArrayAdapter<Mission.FrequencyType> frequencyAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, Mission.FrequencyType.values());
        frequencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFrequency.setAdapter(frequencyAdapter);

        ArrayAdapter<Mission.RepeatUnit> repeatUnitAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, Mission.RepeatUnit.values());
        repeatUnitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRepeatUnit.setAdapter(repeatUnitAdapter);

        // Load categories dynamically from Firebase
        loadCategories();

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
                        // Add a placeholder item if no categories exist
                        categories = new ArrayList<>();
                        Category noCategory = new Category();
                        noCategory.setName("No categories available - Create one first");
                        categories.add(noCategory);
                        buttonCreateMission.setEnabled(false);
                        Toast.makeText(MissionCreationActivity.this, "Please create a category first", Toast.LENGTH_LONG).show();
                    } else {
                        buttonCreateMission.setEnabled(true);
                    }

                    categoryAdapter = new ArrayAdapter<>(MissionCreationActivity.this, android.R.layout.simple_spinner_item, categories);
                    categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerCategory.setAdapter(categoryAdapter);
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(MissionCreationActivity.this, "Error loading categories: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
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

        // Get selected category
        Category selectedCategory = (Category) spinnerCategory.getSelectedItem();
        String categoryId = null;
        if (selectedCategory != null && selectedCategory.getId() != null) {
            categoryId = selectedCategory.getId();
        }

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
        if (categoryId == null) {
            Toast.makeText(this, "Please select a valid category", Toast.LENGTH_SHORT).show();
            return;
        }

        // Note: userLevel will be set by repository after fetching user data
        Mission mission = new Mission(
                null, // id will be set by repo/db
                name,
                description.isEmpty() ? null : description,
                frequency,
                repeatInterval,
                repeatUnit,
                categoryId,
                difficulty,
                importance,
                userId,
                1, // Temporary userLevel, will be updated by repository
                selectedDueDate
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

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
