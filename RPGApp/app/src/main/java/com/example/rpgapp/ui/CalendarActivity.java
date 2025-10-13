package com.example.rpgapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.rpgapp.R;
import com.example.rpgapp.adapter.CalendarAdapter;
import com.example.rpgapp.model.CalendarDay;
import com.example.rpgapp.model.Mission;
import com.example.rpgapp.repository.MissionRepository;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.*;

public class CalendarActivity extends AppCompatActivity implements CalendarAdapter.OnDayClickListener {
    private TextView textViewMonthYear;
    private Button buttonPrevMonth, buttonNextMonth;
    private RecyclerView recyclerViewCalendar;
    private CalendarAdapter calendarAdapter;
    private MissionRepository missionRepository;

    private Calendar currentCalendar;
    private List<Mission> allMissions;
    private List<CalendarDay> calendarDays;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        initViews();
        setupCalendar();
        loadMissions();
    }

    private void initViews() {
        textViewMonthYear = findViewById(R.id.textViewMonthYear);
        buttonPrevMonth = findViewById(R.id.buttonPrevMonth);
        buttonNextMonth = findViewById(R.id.buttonNextMonth);
        recyclerViewCalendar = findViewById(R.id.recyclerViewCalendar);

        buttonPrevMonth.setOnClickListener(v -> navigateMonth(-1));
        buttonNextMonth.setOnClickListener(v -> navigateMonth(1));
    }

    private void setupCalendar() {
        currentCalendar = Calendar.getInstance();
        calendarDays = new ArrayList<>();

        calendarAdapter = new CalendarAdapter(calendarDays, this);
        recyclerViewCalendar.setLayoutManager(new GridLayoutManager(this, 7)); // 7 days per week
        recyclerViewCalendar.setAdapter(calendarAdapter);

        missionRepository = new MissionRepository();
        allMissions = new ArrayList<>();
    }

    private void loadMissions() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            missionRepository.getAllMissions().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    allMissions.clear();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : task.getResult().getDocuments()) {
                        Mission mission = doc.toObject(Mission.class);
                        if (mission != null && currentUser.getUid().equals(mission.getUserId())) {
                            mission.setId(doc.getId());
                            allMissions.add(mission);
                        }
                    }
                    runOnUiThread(() -> updateCalendar());
                }
            });
        }
    }

    private void navigateMonth(int direction) {
        currentCalendar.add(Calendar.MONTH, direction);
        updateCalendar();
    }

    private void updateCalendar() {
        updateMonthYearDisplay();
        generateCalendarDays();
        calendarAdapter.notifyDataSetChanged();
    }

    private void updateMonthYearDisplay() {
        SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        textViewMonthYear.setText(monthYearFormat.format(currentCalendar.getTime()));
    }

    private void generateCalendarDays() {
        calendarDays.clear();

        Calendar calendar = (Calendar) currentCalendar.clone();
        calendar.set(Calendar.DAY_OF_MONTH, 1);

        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        for (int day = 1; day <= daysInMonth; day++) {
            calendar.set(Calendar.DAY_OF_MONTH, day);
            Date dayDate = calendar.getTime();

            List<Mission> dayMissions = getMissionsForDay(dayDate);
            CalendarDay calendarDay = new CalendarDay(day, dayDate, dayMissions);
            calendarDays.add(calendarDay);
        }
    }

    private List<Mission> getMissionsForDay(Date date) {
        List<Mission> dayMissions = new ArrayList<>();
        Calendar dayCalendar = Calendar.getInstance();
        dayCalendar.setTime(date);

        for (Mission mission : allMissions) {
            if (mission.getDueDateTime() != null) {
                if (mission.getFrequency() == Mission.FrequencyType.ONCE) {
                    // For single missions, check if due date matches
                    if (isSameDay(mission.getDueDateTime(), date)) {
                        dayMissions.add(mission);
                    }
                } else if (mission.getFrequency() == Mission.FrequencyType.REPEATING) {
                    // For repeating missions, calculate occurrence dates
                    if (isRepeatingMissionOnDate(mission, date)) {
                        dayMissions.add(mission);
                    }
                }
            }
        }

        // Sort by creation date
        dayMissions.sort((m1, m2) -> m1.getCreateDateTime().compareTo(m2.getCreateDateTime()));
        return dayMissions;
    }

    @Override
    public void onDayClick(CalendarDay day) {
        // Handle day click - show all missions for the selected date
        if (!day.getMissions().isEmpty()) {
            // Navigate to missions list filtered by the selected date
            Intent intent = new Intent(this, MissionsListActivity.class);
            intent.putExtra("FILTER_DATE", day.getDate().getTime()); // Pass date as timestamp
            intent.putExtra("DATE_DISPLAY", android.text.format.DateFormat.format("MMMM dd, yyyy", day.getDate()).toString());
            startActivity(intent);
        }
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

        Calendar targetCal = Calendar.getInstance();
        targetCal.setTime(targetDate);

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

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh mission data when returning from other activities
        loadMissions();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
