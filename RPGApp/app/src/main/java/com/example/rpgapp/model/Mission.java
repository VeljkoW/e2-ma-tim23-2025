package com.example.rpgapp.model;

import androidx.annotation.ColorInt;

public class Mission {
    public enum FrequencyType {
        ONCE,
        REPEATING;

        @Override
        public String toString() {
            switch (this) {
                case ONCE: return "Once";
                case REPEATING: return "Repeating";
                default: return super.toString();
            }
        }
    }

    public enum RepeatUnit {
        DAYS,
        WEEKS;

        @Override
        public String toString() {
            switch (this) {
                case DAYS: return "Days";
                case WEEKS: return "Weeks";
                default: return super.toString();
            }
        }
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

        @Override
        public String toString() {
            switch (this) {
                case HEALTH: return "Health";
                case PROFESSION: return "Profession";
                case ENTERTAINMENT: return "Entertainment";
                case CHORE: return "Chore";
                default: return super.toString();
            }
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

        @Override
        public String toString() {
            switch (this) {
                case VERY_EASY: return "Very Easy (1xp)";
                case EASY: return "Easy (3xp)";
                case HARD: return "Hard (7xp)";
                case EXTREMELY_HARD: return "Extremely Hard (20xp)";
                default: return super.toString();
            }
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

        @Override
        public String toString() {
            switch (this) {
                case NORMAL: return "Normal (1xp)";
                case IMPORTANT: return "Important (3xp)";
                case EXTREMELY_IMPORTANT: return "Extremely Important (7xp)";
                case SPECIAL: return "Special (20xp)";
                default: return super.toString();
            }
        }
    }

    public enum Status {
        ACTIVE,
        PAUSED,
        CANCELLED,
        COMPLETED,
        FAILED
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
    private Status status;
    private int totalXP;
    private java.util.Date createDateTime;
    private java.util.Date finalizationDateTime;
    private java.util.Date dueDateTime; // When mission should be completed (single) or when repeating should stop

    public Mission(String id, String name, String description, FrequencyType frequency, Integer repeatInterval, RepeatUnit repeatUnit, Category category, Difficulty difficulty, Importance importance, String userId, java.util.Date dueDateTime) {
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
        this.dueDateTime = dueDateTime;
        this.status = Status.ACTIVE;
        this.createDateTime = new java.util.Date();
        this.finalizationDateTime = null;
        // XP will be calculated separately via calculateTotalXP method
        this.totalXP = (difficulty != null ? difficulty.xp : 0) + (importance != null ? importance.xp : 0);
    }

    // Required for Firestore serialization
    public Mission() {}

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

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public int getTotalXP() { return totalXP; }
    public void setTotalXP(int totalXP) { this.totalXP = totalXP; }

    public java.util.Date getCreateDateTime() { return createDateTime; }
    public void setCreateDateTime(java.util.Date createDateTime) { this.createDateTime = createDateTime; }

    public java.util.Date getFinalizationDateTime() { return finalizationDateTime; }
    public void setFinalizationDateTime(java.util.Date finalizationDateTime) { this.finalizationDateTime = finalizationDateTime; }

    public java.util.Date getDueDateTime() { return dueDateTime; }
    public void setDueDateTime(java.util.Date dueDateTime) { this.dueDateTime = dueDateTime; }

    /**
     * Calculates total XP based on difficulty, importance, and daily limits.
     * This should be called after checking existing missions for the day.
     */
    public int calculateTotalXP(int veryEasyNormalCount, int easyImportantCount, int hardExtremelyImportantCount, int specialCount) {
        if (difficulty == null || importance == null) {
            return 0;
        }

        // Check special importance limit first (only 1 per day regardless of difficulty)
        if (importance == Importance.SPECIAL && specialCount >= 1) {
            return 0;
        }

        // Check specific difficulty + importance combinations
        if (difficulty == Difficulty.VERY_EASY && importance == Importance.NORMAL && veryEasyNormalCount >= 5) {
            return 0;
        }

        if (difficulty == Difficulty.EASY && importance == Importance.IMPORTANT && easyImportantCount >= 5) {
            return 0;
        }

        if (difficulty == Difficulty.HARD && importance == Importance.EXTREMELY_IMPORTANT && hardExtremelyImportantCount >= 2) {
            return 0;
        }

        if (difficulty == Difficulty.EXTREMELY_HARD && importance == Importance.SPECIAL && hardExtremelyImportantCount >= 1) {
            return 0;
        }

        // If no limits exceeded, return normal XP
        return difficulty.xp + importance.xp;
    }

    /**
     * Sets the total XP for this mission. Should be called after calculating with daily limits.
     */
    public void setCalculatedTotalXP(int calculatedXP) {
        this.totalXP = calculatedXP;
    }
}
