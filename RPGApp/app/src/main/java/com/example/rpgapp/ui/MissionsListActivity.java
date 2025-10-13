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
import com.example.rpgapp.repository.MissionRepository;
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
    private List<Mission> missionsList;
    private Date filterDate = null;
    private String dateDisplayText = null;

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
        missionsList = new ArrayList<>();

        setupRecyclerView();
        loadUserMissions();
    }

    private void setupRecyclerView() {
        missionAdapter = new MissionAdapter(missionsList, this);
        recyclerViewMissions.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewMissions.setAdapter(missionAdapter);
    }

    private void loadUserMissions() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            missionRepository.getAllMissions().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    missionsList.clear();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : task.getResult().getDocuments()) {
                        Mission mission = doc.toObject(Mission.class);
                        if (mission != null && currentUser.getUid().equals(mission.getUserId())) {
                            mission.setId(doc.getId());

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
