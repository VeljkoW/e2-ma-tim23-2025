package com.example.rpgapp.model;

import java.util.Date;

public class User {
    private String id;
    private String email;
    private String username;
    private String avatarId;
    private int level;
    private String title;
    private int powerPoints;
    private int startingPowerPoints; // Track original power points
    private int experiencePoints;
    private int coins;
    private boolean isEmailVerified;
    private Date registrationDate;
    private Date lastLogin;
    private int activeDaysStreak;
    private Date lastActivityDayUpdate; // Novi field za pracenje activity days

    public User()
    {

    }

    public User(String email, String username, String avatarId) {
        this.email = email;
        this.username = username;
        this.avatarId = avatarId;
        this.level = 1;
        this.title = "Crook"; // Starting title
        this.powerPoints = 0;
        this.startingPowerPoints = 0; // Track starting power points
        this.experiencePoints = 0;
        this.coins = 0;
        this.isEmailVerified = false;
        this.registrationDate = new Date();
        this.activeDaysStreak = 0;
        this.lastActivityDayUpdate = null;
    }

    // Getters
    public String getId() { return id; }
    public String getEmail() { return email; }
    public String getUsername() { return username; }
    public String getAvatarId() { return avatarId; }
    public int getLevel() { return level; }
    public String getTitle() { return title; }
    public int getPowerPoints() { return powerPoints; }
    public int getStartingPowerPoints() { return startingPowerPoints; }
    public int getExperiencePoints() { return experiencePoints; }
    public int getCoins() { return coins; }
    public boolean isEmailVerified() { return isEmailVerified; }
    public Date getRegistrationDate() { return registrationDate; }
    public Date getLastLogin() { return lastLogin; }
    public int getActiveDaysStreak() { return activeDaysStreak; }
    public Date getLastActivityDayUpdate() { return lastActivityDayUpdate; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setEmail(String email) { this.email = email; }
    public void setUsername(String username) { this.username = username; }
    public void setAvatarId(String avatarId) { this.avatarId = avatarId; }
    public void setLevel(int level) { this.level = level; }
    public void setTitle(String title) { this.title = title; }
    public void setPowerPoints(int powerPoints) { this.powerPoints = powerPoints; }
    public void setStartingPowerPoints(int startingPowerPoints) { this.startingPowerPoints = startingPowerPoints; }
    public void setExperiencePoints(int experiencePoints) { this.experiencePoints = experiencePoints; }
    public void setCoins(int coins) { this.coins = coins; }
    public void setEmailVerified(boolean emailVerified) { isEmailVerified = emailVerified; }
    public void setRegistrationDate(Date registrationDate) { this.registrationDate = registrationDate; }
    public void setLastLogin(Date lastLogin) { this.lastLogin = lastLogin; }
    public void setActiveDaysStreak(int activeDaysStreak) { this.activeDaysStreak = activeDaysStreak; }
    public void setLastActivityDayUpdate(Date lastActivityDayUpdate) { this.lastActivityDayUpdate = lastActivityDayUpdate; }

    public void addExperiencePoints(int xp)
    {
        this.experiencePoints += xp;
    }

    public void addCoins(int coins)
    {
        this.coins += coins;
    }

    public void addPowerPoints(int pp)
    {
        this.powerPoints += pp;
    }

    public boolean canLevelUp()
    {
        int requiredXp = getXpForNextLevel();
        return experiencePoints >= requiredXp;
    }

    public boolean levelUp()
    {
        if (!canLevelUp())
        {
            return false;
        }

        int requiredXp = getXpForNextLevel();

        this.experiencePoints -= requiredXp;

        this.level++;

        int ppToAdd = calculatePPForLevelUp(this.level);
        this.powerPoints += ppToAdd;
        this.startingPowerPoints = this.powerPoints; // Update starting power points to current level

        this.title = getTitleForLevel(this.level);

        return true;
    }

    /**
     * Levels up the user and indicates if a new boss should be created
     * @return true if user leveled up and a new boss should be spawned, false otherwise
     */
    public boolean levelUpAndShouldCreateBoss()
    {
        boolean leveledUp = levelUp();
        // Create a boss every time the user levels up
        return leveledUp;
    }

    private int calculatePPForLevelUp(int newLevel)
    {
        if (newLevel <= 1)
            return 0;
        if (newLevel == 2)
            return 40;

        int previousPP = 40;
        for (int i = 3; i <= newLevel; i++)
        {
            previousPP = (int) Math.round(previousPP + (3.0 / 4.0) * previousPP);
        }
        return previousPP;
    }

    private String getTitleForLevel(int level)
    {
        if (level >= 100)
            return "Mafia Boss";
        else if(level >= 75)
            return "Don";
        else if (level >= 50)
            return "Boss";
        else if (level >= 30)
            return "Underboss";
        else if (level >= 25)
            return "Capo";
        else if (level >= 20)
            return "Soldier";
        else if (level >= 15)
            return "Associate";
        else if (level >= 10)
            return "Hitman";
        else if (level >= 5)
            return "Thug";
        else
            return "Crook";
    }

    public int getXpForNextLevel()
    {
        // XP potreban da se pređe sa trenutnog nivoa na sledeći
        if (level == 1)
            return 200;
        int previousLevelXp = getXpRequiredForLevel(level);
        return (int) Math.ceil((previousLevelXp * 2 + previousLevelXp / 2.0) / 100.0) * 100;
    }


    private int getXpRequiredForLevel(int targetLevel)
    {
        if (targetLevel <= 1)
            return 0;

        if (targetLevel == 2)
            return 200;

        int xp = 200;
        for (int i = 3; i <= targetLevel; i++)
        {
            xp = (int) Math.ceil((xp * 2 + xp / 2.0) / 100.0) * 100;
        }
        return xp;
    }

    public int getXpForDifficulty(String difficulty)
    {
        int baseXp;
        switch (difficulty.toLowerCase()) {
            case "very_easy":
            case "veoma lak":
                baseXp = 1;
                break;
            case "easy":
            case "lak":
                baseXp = 3;
                break;
            case "hard":
            case "težak":
                baseXp = 7;
                break;
            case "extreme":
            case "extremely_hard":
            case "ekstremno težak":
                baseXp = 20;
                break;
            default:
                baseXp = 1;
        }

        if (level <= 1)
            return baseXp;

        int xp = baseXp;
        for (int i = 2; i <= level; i++)
        {
            xp = (int) Math.round(xp + xp / 2.0);
        }
        return xp;
    }
    public int getXpForImportance(String importance)
    {
        int baseXp;
        switch (importance.toLowerCase()) {
            case "normal":
            case "normalan":
                baseXp = 1;
                break;
            case "important":
            case "važan":
                baseXp = 3;
                break;
            case "very_important":
            case "ekstremno važan":
            case "extremely_important":
                baseXp = 10;
                break;
            case "special":
            case "specijalan":
                baseXp = 100;
                break;
            default:
                baseXp = 1;
        }

        if (level <= 1)
            return baseXp;

        int xp = baseXp;
        for (int i = 2; i <= level; i++)
        {
            xp = (int) Math.round(xp + xp / 2.0);
        }
        return xp;
    }

    public int calculateMissionXP(Mission.Difficulty difficulty, Mission.Importance importance)
    {
        if (difficulty == null || importance == null)
        {
            return 0;
        }

        int difficultyXp = getXpForDifficulty(difficulty.name());
        int importanceXp = getXpForImportance(importance.name());

        return difficultyXp + importanceXp;
    }

    @Override
    public String toString()
    {
        return "User{" +
                "id='" + id + '\'' +
                ", email='" + email + '\'' +
                ", username='" + username + '\'' +
                ", level=" + level +
                ", title='" + title + '\'' +
                '}';
    }
}
