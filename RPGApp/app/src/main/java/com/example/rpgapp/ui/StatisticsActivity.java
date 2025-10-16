package com.example.rpgapp.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.rpgapp.R;
import com.example.rpgapp.model.UserStatistics;
import com.example.rpgapp.service.StatisticsService;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatisticsActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private StatisticsService statisticsService;
    private UserStatistics statistics;

    private TextView tvActiveDays, tvCompletedCount, tvIncompleteCount, tvCancelledCount, tvCreatedCount;
    private TextView tvLongestStreak, tvCurrentStreak, tvAverageDifficulty, tvAverageXP;
    private TextView tvMissionsStarted, tvMissionsCompleted;
    private PieChart pieChartTasks;
    private BarChart barChartCategories;
    private LineChart lineChartXP;
    private LineChart lineChartDifficulty;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        mAuth = FirebaseAuth.getInstance();
        statisticsService = new StatisticsService(this);

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
        tvCreatedCount = findViewById(R.id.tvCreatedCount);
        tvLongestStreak = findViewById(R.id.tvLongestStreak);
        tvCurrentStreak = findViewById(R.id.tvCurrentStreak);
        tvAverageDifficulty = findViewById(R.id.tvAverageDifficulty);
        tvAverageXP = findViewById(R.id.tvAverageXP);
        tvMissionsStarted = findViewById(R.id.tvMissionsStarted);
        tvMissionsCompleted = findViewById(R.id.tvMissionsCompleted);
        pieChartTasks = findViewById(R.id.pieChartTasks);
        barChartCategories = findViewById(R.id.barChartCategories);
        lineChartXP = findViewById(R.id.lineChartXP);
        lineChartDifficulty = findViewById(R.id.lineChartDifficulty);
        btnBack = findViewById(R.id.btnBack);
    }

    private void loadStatistics() {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser == null) {
            finish();
            return;
        }

        String userId = firebaseUser.getUid();

        statisticsService.loadStatistics(userId, result -> {
            if (result != null)
            {
                statistics = result;
                runOnUiThread(this::displayStatistics);
            }
            else
            {
                Toast.makeText(this, "Failed to load statistics", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayStatistics() {
        if (statistics == null) return;

        // Active Days Streak
        tvActiveDays.setText(String.valueOf(statistics.getActiveDaysStreak()));

        // Tasks Overview
        tvCreatedCount.setText(String.valueOf(statistics.getTotalTasksCreated()));
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

        // Setup charts
        setupPieChart();
        setupBarChart();
        setupXPLineChart();
        setupDifficultyLineChart();
    }

    private void setupPieChart() {
        List<PieEntry> entries = new ArrayList<>();

        int completed = statistics.getTotalTasksCompleted();
        int incomplete = statistics.getTotalTasksIncomplete();
        int cancelled = statistics.getTotalTasksCancelled();

        if (completed > 0) entries.add(new PieEntry(completed, "Completed"));
        if (incomplete > 0) entries.add(new PieEntry(incomplete, "Incomplete"));
        if (cancelled > 0) entries.add(new PieEntry(cancelled, "Cancelled"));

        if (entries.isEmpty()) {
            pieChartTasks.setNoDataText("No tasks data available");
            pieChartTasks.invalidate();
            return;
        }

        PieDataSet dataSet = new PieDataSet(entries, "");

        // Boje za grafikon
        List<Integer> colors = new ArrayList<>();
        if (completed > 0) colors.add(ContextCompat.getColor(this, R.color.success_color));
        if (incomplete > 0) colors.add(ContextCompat.getColor(this, R.color.warning_color));
        if (cancelled > 0) colors.add(ContextCompat.getColor(this, R.color.error_color));

        dataSet.setColors(colors);
        dataSet.setValueTextSize(14f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setSliceSpace(2f);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        pieChartTasks.setData(data);
        pieChartTasks.setDrawHoleEnabled(true);
        pieChartTasks.setHoleRadius(50f);
        pieChartTasks.setTransparentCircleRadius(55f);
        pieChartTasks.setHoleColor(Color.TRANSPARENT);
        pieChartTasks.getDescription().setEnabled(false);
        pieChartTasks.setDrawEntryLabels(false);
        pieChartTasks.getLegend().setEnabled(false);
        pieChartTasks.animateY(1000);
        pieChartTasks.invalidate();
    }

    private void setupBarChart() {
        Map<String, Integer> categoryMap = statistics.getTasksCompletedByCategory();
        Map<String, String> categoryNames = statistics.getCategoryNames();
        Map<String, Integer> categoryColors = statistics.getCategoryColors();

        if (categoryMap.isEmpty()) {
            barChartCategories.setNoDataText("No category data available");
            barChartCategories.invalidate();
            return;
        }

        // Sortiraj kategorije po broju zadataka
        List<Map.Entry<String, Integer>> sortedCategories = new ArrayList<>(categoryMap.entrySet());
        Collections.sort(sortedCategories, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> e1, Map.Entry<String, Integer> e2) {
                return e2.getValue().compareTo(e1.getValue());
            }
        });

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        int index = 0;
        for (Map.Entry<String, Integer> entry : sortedCategories) {
            String categoryId = entry.getKey();
            entries.add(new BarEntry(index, entry.getValue()));

            String categoryName = categoryNames.getOrDefault(categoryId, "Unknown");
            labels.add(categoryName);

            Integer color = categoryColors.get(categoryId);
            colors.add(color != null ? color : ContextCompat.getColor(this, R.color.primary_color));

            index++;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Tasks by Category");
        dataSet.setColors(colors);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(ContextCompat.getColor(this, R.color.text_primary));

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.8f);
        data.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        barChartCategories.setData(data);
        barChartCategories.getDescription().setEnabled(false);
        barChartCategories.setFitBars(true);
        barChartCategories.animateY(1000);

        // X Axis
        XAxis xAxis = barChartCategories.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        xAxis.setDrawGridLines(false);

        // Y Axis
        YAxis leftAxis = barChartCategories.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setGranularity(1f);
        leftAxis.setTextColor(ContextCompat.getColor(this, R.color.text_primary));

        barChartCategories.getAxisRight().setEnabled(false);
        barChartCategories.getLegend().setEnabled(false);
        barChartCategories.invalidate();
    }

    private void setupXPLineChart() {
        Map<String, Integer> xpMap = statistics.getXpLast7Days();

        if (xpMap.isEmpty()) {
            lineChartXP.setNoDataText("No XP data for last 7 days");
            lineChartXP.invalidate();
            return;
        }

        // Kreiraj listu poslednjih 7 dana
        List<String> last7Days = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar cal = Calendar.getInstance();

        for (int i = 6; i >= 0; i--) {
            Calendar tempCal = (Calendar) cal.clone();
            tempCal.add(Calendar.DAY_OF_YEAR, -i);
            last7Days.add(sdf.format(tempCal.getTime()));
        }

        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        SimpleDateFormat labelFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());

        for (int i = 0; i < last7Days.size(); i++) {
            String dateKey = last7Days.get(i);
            int xp = xpMap.getOrDefault(dateKey, 0);
            entries.add(new Entry(i, xp));

            try {
                Calendar tempCal = Calendar.getInstance();
                tempCal.setTime(sdf.parse(dateKey));
                labels.add(labelFormat.format(tempCal.getTime()));
            } catch (Exception e) {
                labels.add(dateKey.substring(5));
            }
        }

        LineDataSet dataSet = new LineDataSet(entries, "XP per Day");
        dataSet.setColor(ContextCompat.getColor(this, R.color.accent_color));
        dataSet.setCircleColor(ContextCompat.getColor(this, R.color.accent_color));
        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(5f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(ContextCompat.getColor(this, R.color.text_primary));
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(ContextCompat.getColor(this, R.color.accent_color));
        dataSet.setFillAlpha(50);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        LineData data = new LineData(dataSet);
        data.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        lineChartXP.setData(data);
        lineChartXP.getDescription().setEnabled(false);
        lineChartXP.animateX(1000);

        // X Axis
        XAxis xAxis = lineChartXP.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        xAxis.setDrawGridLines(false);

        // Y Axis
        YAxis leftAxis = lineChartXP.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(ContextCompat.getColor(this, R.color.text_secondary));

        lineChartXP.getAxisRight().setEnabled(false);

        Legend legend = lineChartXP.getLegend();
        legend.setEnabled(true);
        legend.setTextColor(ContextCompat.getColor(this, R.color.text_primary));

        lineChartXP.invalidate();
    }

    private void setupDifficultyLineChart() {
        Map<String, Float> difficultyMap = statistics.getAverageDifficultyOverTime();

        if (difficultyMap.isEmpty()) {
            lineChartDifficulty.setNoDataText("No difficulty data available");
            lineChartDifficulty.invalidate();
            return;
        }

        // Sortiraj po datumu
        List<Map.Entry<String, Float>> sortedEntries = new ArrayList<>(difficultyMap.entrySet());
        Collections.sort(sortedEntries, new Comparator<Map.Entry<String, Float>>() {
            @Override
            public int compare(Map.Entry<String, Float> e1, Map.Entry<String, Float> e2) {
                return e1.getKey().compareTo(e2.getKey());
            }
        });

        // Uzmi poslednjih 10 dana
        int maxEntries = Math.min(10, sortedEntries.size());
        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        SimpleDateFormat labelFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        int startIndex = Math.max(0, sortedEntries.size() - maxEntries);

        for (int i = startIndex; i < sortedEntries.size(); i++) {
            Map.Entry<String, Float> entry = sortedEntries.get(i);
            entries.add(new Entry(i - startIndex, entry.getValue()));

            try {
                Calendar tempCal = Calendar.getInstance();
                tempCal.setTime(dateFormat.parse(entry.getKey()));
                labels.add(labelFormat.format(tempCal.getTime()));
            } catch (Exception e) {
                labels.add(entry.getKey().substring(5));
            }
        }

        LineDataSet dataSet = new LineDataSet(entries, "Average XP per Task");
        dataSet.setColor(ContextCompat.getColor(this, R.color.primary_color));
        dataSet.setCircleColor(ContextCompat.getColor(this, R.color.primary_color));
        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(5f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(ContextCompat.getColor(this, R.color.text_primary));
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(ContextCompat.getColor(this, R.color.primary_color));
        dataSet.setFillAlpha(50);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        LineData data = new LineData(dataSet);
        data.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format("%.1f", value);
            }
        });

        lineChartDifficulty.setData(data);
        lineChartDifficulty.getDescription().setEnabled(false);
        lineChartDifficulty.animateX(1000);

        // X Axis
        XAxis xAxis = lineChartDifficulty.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        xAxis.setDrawGridLines(false);

        // Y Axis
        YAxis leftAxis = lineChartDifficulty.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(ContextCompat.getColor(this, R.color.text_secondary));

        lineChartDifficulty.getAxisRight().setEnabled(false);

        Legend legend = lineChartDifficulty.getLegend();
        legend.setEnabled(true);
        legend.setTextColor(ContextCompat.getColor(this, R.color.text_primary));

        lineChartDifficulty.invalidate();
    }

    private String getDifficultyText(double avgXP) {
        if (avgXP == 0) return "No tasks completed";
        else if (avgXP < 5) return "Very Easy";
        else if (avgXP < 10) return "Easy";
        else if (avgXP < 20) return "Hard";
        else return "Extremely Hard";
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
    }
}
