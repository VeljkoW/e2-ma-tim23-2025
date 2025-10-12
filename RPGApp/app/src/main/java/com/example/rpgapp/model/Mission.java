package com.example.rpgapp.model;

import androidx.annotation.ColorInt;

public class Mission {
    public enum FrequencyType {
        ONCE,
        REPEATING
    }

    public enum RepeatUnit {
        DAYS,
        WEEKS
    }

    public enum Category {
        HEALTH(0xFF4CAF50), // Green
        PROFESSION(0xFF2196F3), // Blue
        ENTERTAINMENT(0xFFFFC107), // Amber
        CHORE(0xFFF44336); // Red

        @ColorInt
        public final int color;
        Category(int color) {
            this.color = color;
        }
    }

    public enum Difficulty {
        VERY_EASY(1),
        EASY(3),
        HARD(7),
        EXTREMELY_HARD(20);

        public final int xp;
        Difficulty(int xp) {
            this.xp = xp;
        }
    }

    public enum Importance {
        NORMAL(1),
        IMPORTANT(3),
        EXTREMELY_IMPORTANT(7),
        SPECIAL(20);

        public final int xp;
        Importance(int xp) {
            this.xp = xp;
        }
    }

    private String id;
    private String name;
    private String description;
    private FrequencyType frequency;
    private Integer repeatInterval; // null if ONCE
    private RepeatUnit repeatUnit; // null if ONCE
    private Category category;
    private Difficulty difficulty;
    private Importance importance;
    private String userId;

    public Mission(String id, String name, String description, FrequencyType frequency, Integer repeatInterval, RepeatUnit repeatUnit, Category category, Difficulty difficulty, Importance importance, String userId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.frequency = frequency;
        this.repeatInterval = repeatInterval;
        this.repeatUnit = repeatUnit;
        this.category = category;
        this.difficulty = difficulty;
        this.importance = importance;
        this.userId = userId;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public FrequencyType getFrequency() { return frequency; }
    public void setFrequency(FrequencyType frequency) { this.frequency = frequency; }

    public Integer getRepeatInterval() { return repeatInterval; }
    public void setRepeatInterval(Integer repeatInterval) { this.repeatInterval = repeatInterval; }

    public RepeatUnit getRepeatUnit() { return repeatUnit; }
    public void setRepeatUnit(RepeatUnit repeatUnit) { this.repeatUnit = repeatUnit; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public Difficulty getDifficulty() { return difficulty; }
    public void setDifficulty(Difficulty difficulty) { this.difficulty = difficulty; }

    public Importance getImportance() { return importance; }
    public void setImportance(Importance importance) { this.importance = importance; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}
