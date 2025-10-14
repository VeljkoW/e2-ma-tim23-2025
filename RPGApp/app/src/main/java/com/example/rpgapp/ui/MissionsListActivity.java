package com.example.rpgapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.rpgapp.R;
import com.example.rpgapp.adapter.MissionAdapter;
import com.example.rpgapp.model.Mission;
import com.example.rpgapp.model.Category;
import com.example.rpgapp.repository.MissionRepository;
import com.example.rpgapp.repository.CategoryRepository;
import com.example.rpgapp.service.AuthService;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MissionsListActivity extends AppCompatActivity implements MissionAdapter.OnMissionClickListener {
    private RecyclerView recyclerViewMissions;
    private TextView textViewEmptyState;
    private MissionAdapter missionAdapter;
    private MissionRepository missionRepository;
    private CategoryRepository categoryRepository;
    private AuthService authService;
    private List<Mission> missionsList;
    private List<Mission> allMissionsList; // Keep original list for filtering
    private Date filterDate = null;
    private String dateDisplayText = null;

    // Filter chips
    private TextView chipAllMissions, chipOneTimeMissions, chipRepeatingMissions;
    private FilterType currentFilter = FilterType.ALL;

    private enum FilterType {
        ALL, ONE_TIME, REPEATING
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_missions_list);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Check if we're filtering by date (coming from calendar)
        Intent intent = getIntent();
        if (intent.hasExtra("FILTER_DATE")) {
            long timestamp = intent.getLongExtra("FILTER_DATE", 0);
            filterDate = new Date(timestamp);
            dateDisplayText = intent.getStringExtra("DATE_DISPLAY");

            // Update toolbar title to show filtered date
            if (getSupportActionBar() != null && dateDisplayText != null) {
                getSupportActionBar().setTitle("Missions for " + dateDisplayText);
            }
        }

        recyclerViewMissions = findViewById(R.id.recyclerViewMissions);
        textViewEmptyState = findViewById(R.id.textViewEmptyState);

        missionRepository = new MissionRepository();
        categoryRepository = new CategoryRepository();
        authService = new AuthService(this);
        missionsList = new ArrayList<>();
        allMissionsList = new ArrayList<>(); // Initialize the original list

        setupRecyclerView();
        loadCategoriesAndMissions();

        // Initialize filter chips
        chipAllMissions = findViewById(R.id.chipAllMissions);
        chipOneTimeMissions = findViewById(R.id.chipOneTimeMissions);
        chipRepeatingMissions = findViewById(R.id.chipRepeatingMissions);

        // Set chip click listeners
        chipAllMissions.setOnClickListener(v -> setFilter(FilterType.ALL));
        chipOneTimeMissions.setOnClickListener(v -> setFilter(FilterType.ONE_TIME));
        chipRepeatingMissions.setOnClickListener(v -> setFilter(FilterType.REPEATING));

        // Initialize filter chips visual state
        updateFilterChips();
    }

    private void setupRecyclerView() {
        missionAdapter = new MissionAdapter(missionsList, this);
        recyclerViewMissions.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewMissions.setAdapter(missionAdapter);
    }

    private void loadCategoriesAndMissions() {
        FirebaseUser firebaseUser = authService.getCurrentFirebaseUser();
        if (firebaseUser == null) {
            return;
        }

        // First load categories, then missions
        categoryRepository.getCategoriesByUserId(firebaseUser.getUid(), new CategoryRepository.CategoryCallback<List<Category>>() {
            @Override
            public void onResult(List<Category> categories) {
                runOnUiThread(() -> {
                    // Set category cache in the adapter
                    missionAdapter.setCategoryCache(categories);

                    // Now load missions
                    loadUserMissions();
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    // Still try to load missions even if categories fail
                    loadUserMissions();
                });
            }
        });
    }

    private void loadUserMissions() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            missionRepository.getAllMissions().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    allMissionsList.clear(); // Clear the original list
                    missionsList.clear();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : task.getResult().getDocuments()) {
                        Mission mission = doc.toObject(Mission.class);
                        if (mission != null && currentUser.getUid().equals(mission.getUserId())) {
                            mission.setId(doc.getId());
                            allMissionsList.add(mission); // Add to the original list

                            // Apply date filter if set
                            if (filterDate == null || isMissionOnDate(mission, filterDate)) {
                                missionsList.add(mission);
                            }
                        }
                    }
                    runOnUiThread(() -> {
                        missionAdapter.notifyDataSetChanged();
                        updateEmptyState();
                    });
                }
            });
        }
    }

    private boolean isMissionOnDate(Mission mission, Date date) {
        if (mission.getDueDateTime() == null) {
            return false;
        }

        if (mission.getFrequency() == Mission.FrequencyType.ONCE) {
            // For single missions, check if due date matches
            return isSameDay(mission.getDueDateTime(), date);
        } else if (mission.getFrequency() == Mission.FrequencyType.REPEATING) {
            // For repeating missions, calculate occurrence dates
            return isRepeatingMissionOnDate(mission, date);
        }

        return false;
    }

    private boolean isRepeatingMissionOnDate(Mission mission, Date targetDate) {
        if (mission.getCreateDateTime() == null || mission.getDueDateTime() == null ||
            mission.getRepeatInterval() == null || mission.getRepeatUnit() == null) {
            return false;
        }

        Calendar startCal = Calendar.getInstance();
        startCal.setTime(mission.getCreateDateTime());

        Calendar endCal = Calendar.getInstance();
        endCal.setTime(mission.getDueDateTime());

        // Check if target date is within the mission's active period
        if (targetDate.before(mission.getCreateDateTime()) || targetDate.after(mission.getDueDateTime())) {
            return false;
        }

        // Calculate if target date falls on a repeat occurrence
        Calendar currentOccurrence = (Calendar) startCal.clone();

        while (currentOccurrence.getTime().compareTo(endCal.getTime()) <= 0) {
            if (isSameDay(currentOccurrence.getTime(), targetDate)) {
                return true;
            }

            // Add interval based on repeat unit
            switch (mission.getRepeatUnit()) {
                case DAYS:
                    currentOccurrence.add(Calendar.DAY_OF_MONTH, mission.getRepeatInterval());
                    break;
                case WEEKS:
                    currentOccurrence.add(Calendar.WEEK_OF_YEAR, mission.getRepeatInterval());
                    break;
            }
        }

        return false;
    }

    private boolean isSameDay(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(date1);
        cal2.setTime(date2);

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    private void updateEmptyState() {
        if (missionsList.isEmpty()) {
            recyclerViewMissions.setVisibility(View.GONE);
            textViewEmptyState.setVisibility(View.VISIBLE);

            // Update empty state message for filtered view
            if (filterDate != null && dateDisplayText != null) {
                textViewEmptyState.setText("No missions scheduled for " + dateDisplayText);
            }
        } else {
            recyclerViewMissions.setVisibility(View.VISIBLE);
            textViewEmptyState.setVisibility(View.GONE);
        }
    }

    private void setFilter(FilterType filterType) {
        currentFilter = filterType;
        updateFilterChips();

        missionsList.clear();

        // Get the base list to filter from
        List<Mission> baseList = filterDate != null ? getDateFilteredMissions() : allMissionsList;

        switch (filterType) {
            case ALL:
                // No frequency filter, show all missions from base list
                missionsList.addAll(baseList);
                break;
            case ONE_TIME:
                // Filter for one-time missions
                for (Mission mission : baseList) {
                    if (mission.getFrequency() == Mission.FrequencyType.ONCE) {
                        missionsList.add(mission);
                    }
                }
                break;
            case REPEATING:
                // Filter for repeating missions
                for (Mission mission : baseList) {
                    if (mission.getFrequency() == Mission.FrequencyType.REPEATING) {
                        missionsList.add(mission);
                    }
                }
                break;
        }

        missionAdapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private List<Mission> getDateFilteredMissions() {
        List<Mission> dateFiltered = new ArrayList<>();
        for (Mission mission : allMissionsList) {
            if (isMissionOnDate(mission, filterDate)) {
                dateFiltered.add(mission);
            }
        }
        return dateFiltered;
    }

    private void updateFilterChips() {
        // Reset all chips
        chipAllMissions.setSelected(false);
        chipOneTimeMissions.setSelected(false);
        chipRepeatingMissions.setSelected(false);

        // Set colors based on selection
        chipAllMissions.setBackgroundColor(currentFilter == FilterType.ALL ? 0xFF4CAF50 : 0x80000000);
        chipOneTimeMissions.setBackgroundColor(currentFilter == FilterType.ONE_TIME ? 0xFF4CAF50 : 0x80000000);
        chipRepeatingMissions.setBackgroundColor(currentFilter == FilterType.REPEATING ? 0xFF4CAF50 : 0x80000000);

        // Mark selected chip
        switch (currentFilter) {
            case ALL:
                chipAllMissions.setSelected(true);
                break;
            case ONE_TIME:
                chipOneTimeMissions.setSelected(true);
                break;
            case REPEATING:
                chipRepeatingMissions.setSelected(true);
                break;
        }
    }

    @Override
    public void onMissionClick(Mission mission) {
        Intent intent = new Intent(this, MissionDetailsActivity.class);
        intent.putExtra("MISSION_ID", mission.getId());
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserMissions(); // Refresh the list when returning from details
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
