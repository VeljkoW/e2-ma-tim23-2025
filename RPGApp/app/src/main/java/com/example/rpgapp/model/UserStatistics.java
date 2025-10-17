package com.example.rpgapp.model;

import java.util.HashMap;
import java.util.Map;

public class UserStatistics {
    private String userId;
    private int activeDaysStreak;
    private int totalTasksCreated;
    private int totalTasksCompleted;
    private int totalTasksIncomplete;
    private int totalTasksCancelled;
    private int longestTaskStreak;
    private int currentTaskStreak;
    private Map<String, Integer> tasksCompletedByCategory;
    private Map<String, String> categoryNames; // categoryId -> categoryName
    private Map<String, Integer> categoryColors; // categoryId -> color
    private Map<String, Integer> xpLast7Days;
    private Map<String, Float> averageDifficultyOverTime; // datum -> prosečan XP
    private int specialMissionsStarted;
    private int specialMissionsCompleted;

    public UserStatistics()
    {
        this.tasksCompletedByCategory = new HashMap<>();
        this.categoryNames = new HashMap<>();
        this.categoryColors = new HashMap<>();
        this.xpLast7Days = new HashMap<>();
        this.averageDifficultyOverTime = new HashMap<>();
    }

    public UserStatistics(String userId)
    {
        this.userId = userId;
        this.activeDaysStreak = 0;
        this.totalTasksCreated = 0;
        this.totalTasksCompleted = 0;
        this.totalTasksIncomplete = 0;
        this.totalTasksCancelled = 0;
        this.longestTaskStreak = 0;
        this.currentTaskStreak = 0;
        this.tasksCompletedByCategory = new HashMap<>();
        this.categoryNames = new HashMap<>();
        this.categoryColors = new HashMap<>();
        this.xpLast7Days = new HashMap<>();
        this.averageDifficultyOverTime = new HashMap<>();
        this.specialMissionsStarted = 0;
        this.specialMissionsCompleted = 0;
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public int getActiveDaysStreak() { return activeDaysStreak; }
    public void setActiveDaysStreak(int activeDaysStreak) { this.activeDaysStreak = activeDaysStreak; }

    public int getTotalTasksCreated() { return totalTasksCreated; }
    public void setTotalTasksCreated(int totalTasksCreated) { this.totalTasksCreated = totalTasksCreated; }

    public int getTotalTasksCompleted() { return totalTasksCompleted; }
    public void setTotalTasksCompleted(int totalTasksCompleted) { this.totalTasksCompleted = totalTasksCompleted; }

    public int getTotalTasksIncomplete() { return totalTasksIncomplete; }
    public void setTotalTasksIncomplete(int totalTasksIncomplete) { this.totalTasksIncomplete = totalTasksIncomplete; }

    public int getTotalTasksCancelled() { return totalTasksCancelled; }
    public void setTotalTasksCancelled(int totalTasksCancelled) { this.totalTasksCancelled = totalTasksCancelled; }

    public int getLongestTaskStreak() { return longestTaskStreak; }
    public void setLongestTaskStreak(int longestTaskStreak) { this.longestTaskStreak = longestTaskStreak; }

    public int getCurrentTaskStreak() { return currentTaskStreak; }
    public void setCurrentTaskStreak(int currentTaskStreak) { this.currentTaskStreak = currentTaskStreak; }

    public Map<String, Integer> getTasksCompletedByCategory() { return tasksCompletedByCategory; }
    public void setTasksCompletedByCategory(Map<String, Integer> tasksCompletedByCategory) {
        this.tasksCompletedByCategory = tasksCompletedByCategory;
    }

    public Map<String, String> getCategoryNames() { return categoryNames; }
    public void setCategoryNames(Map<String, String> categoryNames) {
        this.categoryNames = categoryNames;
    }

    public Map<String, Integer> getCategoryColors() { return categoryColors; }
    public void setCategoryColors(Map<String, Integer> categoryColors) {
        this.categoryColors = categoryColors;
    }

    public Map<String, Integer> getXpLast7Days() { return xpLast7Days; }
    public void setXpLast7Days(Map<String, Integer> xpLast7Days) { this.xpLast7Days = xpLast7Days; }

    public Map<String, Float> getAverageDifficultyOverTime() { return averageDifficultyOverTime; }
    public void setAverageDifficultyOverTime(Map<String, Float> averageDifficultyOverTime) {
        this.averageDifficultyOverTime = averageDifficultyOverTime;
    }

    public int getSpecialMissionsStarted() { return specialMissionsStarted; }
    public void setSpecialMissionsStarted(int specialMissionsStarted) {
        this.specialMissionsStarted = specialMissionsStarted;
    }

    public int getSpecialMissionsCompleted() { return specialMissionsCompleted; }
    public void setSpecialMissionsCompleted(int specialMissionsCompleted) {
        this.specialMissionsCompleted = specialMissionsCompleted;
    }

    // Helper methods
    public void incrementTaskCompleted(String category) {
        totalTasksCompleted++;
        int count = tasksCompletedByCategory.getOrDefault(category, 0);
        tasksCompletedByCategory.put(category, count + 1);
    }

    public void addXpForDay(String date, int xp) {
        int currentXp = xpLast7Days.getOrDefault(date, 0);
        xpLast7Days.put(date, currentXp + xp);
    }

    public double getAverageTaskDifficulty()
    {
        if (totalTasksCompleted == 0) return 0.0;
        // Calculate based on XP earned per task
        int totalXp = 0;
        for (Integer xp : xpLast7Days.values()) {
            totalXp += xp;
        }
        return totalTasksCompleted > 0 ? (double) totalXp / totalTasksCompleted : 0.0;
    }
}
