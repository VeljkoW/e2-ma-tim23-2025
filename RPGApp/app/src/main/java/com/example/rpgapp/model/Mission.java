package com.example.rpgapp.model;

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

    public enum Difficulty {
        VERY_EASY,
        EASY,
        HARD,
        EXTREMELY_HARD;

        @Override
        public String toString() {
            switch (this) {
                case VERY_EASY: return "Very Easy";
                case EASY: return "Easy";
                case HARD: return "Hard";
                case EXTREMELY_HARD: return "Extremely Hard";
                default: return super.toString();
            }
        }

        public String toStringWithXP(int userLevel) {
            int xp = getXpForLevel(userLevel);
            switch (this) {
                case VERY_EASY: return "Very Easy (" + xp + "xp)";
                case EASY: return "Easy (" + xp + "xp)";
                case HARD: return "Hard (" + xp + "xp)";
                case EXTREMELY_HARD: return "Extremely Hard (" + xp + "xp)";
                default: return super.toString();
            }
        }

        public int getXpForLevel(int userLevel) {
            int baseXp;
            switch (this) {
                case VERY_EASY: baseXp = 1; break;
                case EASY: baseXp = 3; break;
                case HARD: baseXp = 7; break;
                case EXTREMELY_HARD: baseXp = 20; break;
                default: baseXp = 1;
            }

            if (userLevel <= 1) return baseXp;

            int xp = baseXp;
            for (int i = 2; i <= userLevel; i++) {
                xp = (int) Math.round(xp + xp / 2.0);
            }
            return xp;
        }
    }

    public enum Importance {
        NORMAL,
        IMPORTANT,
        EXTREMELY_IMPORTANT,
        SPECIAL;

        @Override
        public String toString() {
            switch (this) {
                case NORMAL: return "Normal";
                case IMPORTANT: return "Important";
                case EXTREMELY_IMPORTANT: return "Extremely Important";
                case SPECIAL: return "Special";
                default: return super.toString();
            }
        }

        public String toStringWithXP(int userLevel) {
            int xp = getXpForLevel(userLevel);
            switch (this) {
                case NORMAL: return "Normal (" + xp + "xp)";
                case IMPORTANT: return "Important (" + xp + "xp)";
                case EXTREMELY_IMPORTANT: return "Extremely Important (" + xp + "xp)";
                case SPECIAL: return "Special (" + xp + "xp)";
                default: return super.toString();
            }
        }

        public int getXpForLevel(int userLevel) {
            int baseXp;
            switch (this) {
                case NORMAL: baseXp = 1; break;
                case IMPORTANT: baseXp = 3; break;
                case EXTREMELY_IMPORTANT: baseXp = 10; break;
                case SPECIAL: baseXp = 100; break;
                default: baseXp = 1;
            }

            if (userLevel <= 1) return baseXp;

            int xp = baseXp;
            for (int i = 2; i <= userLevel; i++) {
                xp = (int) Math.round(xp + xp / 2.0);
            }
            return xp;
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
    private String categoryId; // Changed from Category enum to String categoryId
    private Difficulty difficulty;
    private Importance importance;
    private String userId;
    private Status status;
    private int totalXP;
    private int userLevel; // Dodato: nivo korisnika u trenutku kreiranja misije
    private java.util.Date createDateTime;
    private java.util.Date finalizationDateTime;
    private java.util.Date dueDateTime; // When mission should be completed (single) or when repeating should stop

    public Mission(String id, String name, String description, FrequencyType frequency, Integer repeatInterval, RepeatUnit repeatUnit, String categoryId, Difficulty difficulty, Importance importance, String userId, int userLevel, java.util.Date dueDateTime) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.frequency = frequency;
        this.repeatInterval = repeatInterval;
        this.repeatUnit = repeatUnit;
        this.categoryId = categoryId;
        this.difficulty = difficulty;
        this.importance = importance;
        this.userId = userId;
        this.userLevel = userLevel;
        this.dueDateTime = dueDateTime;
        this.status = Status.ACTIVE;
        this.createDateTime = new java.util.Date();
        this.finalizationDateTime = null;
        this.totalXP = 0; // Biće izračunat preko calculateTotalXPForLevel
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

    public String getCategoryId() { return categoryId; } // Changed getter
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; } // Changed setter

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

    public int getUserLevel() { return userLevel; }
    public void setUserLevel(int userLevel) { this.userLevel = userLevel; }

    /**
     * Calculates total XP based on difficulty, importance, user level, and daily limits.
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

        // Calculate XP based on user level
        return calculateTotalXPForLevel(userLevel);
    }

    /**
     * Calculates total XP for a specific user level (difficulty + importance XP).
     */
    public int calculateTotalXPForLevel(int level) {
        if (difficulty == null || importance == null) {
            return 0;
        }

        int difficultyXp = difficulty.getXpForLevel(level);
        int importanceXp = importance.getXpForLevel(level);

        return difficultyXp + importanceXp;
    }

    /**
     * Sets the total XP for this mission. Should be called after calculating with daily limits.
     */
    public void setCalculatedTotalXP(int calculatedXP) {
        this.totalXP = calculatedXP;
    }
}
