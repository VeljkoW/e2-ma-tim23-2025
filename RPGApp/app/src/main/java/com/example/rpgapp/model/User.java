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
    private int experiencePoints;
    private int coins;
    private boolean isEmailVerified;
    private Date registrationDate;
    private Date lastLogin;

    public User()
    {

    }

    public User(String email, String username, String avatarId) {
        this.email = email;
        this.username = username;
        this.avatarId = avatarId;
        this.level = 0;
        this.title = "Crook"; // Starting title
        this.powerPoints = 0;
        this.experiencePoints = 0;
        this.coins = 0;
        this.isEmailVerified = false;
        this.registrationDate = new Date();
    }

    // Getters
    public String getId() { return id; }
    public String getEmail() { return email; }
    public String getUsername() { return username; }
    public String getAvatarId() { return avatarId; }
    public int getLevel() { return level; }
    public String getTitle() { return title; }
    public int getPowerPoints() { return powerPoints; }
    public int getExperiencePoints() { return experiencePoints; }
    public int getCoins() { return coins; }
    public boolean isEmailVerified() { return isEmailVerified; }
    public Date getRegistrationDate() { return registrationDate; }
    public Date getLastLogin() { return lastLogin; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setEmail(String email) { this.email = email; }
    public void setUsername(String username) { this.username = username; }
    public void setAvatarId(String avatarId) { this.avatarId = avatarId; }
    public void setLevel(int level) { this.level = level; }
    public void setTitle(String title) { this.title = title; }
    public void setPowerPoints(int powerPoints) { this.powerPoints = powerPoints; }
    public void setExperiencePoints(int experiencePoints) { this.experiencePoints = experiencePoints; }
    public void setCoins(int coins) { this.coins = coins; }
    public void setEmailVerified(boolean emailVerified) { isEmailVerified = emailVerified; }
    public void setRegistrationDate(Date registrationDate) { this.registrationDate = registrationDate; }
    public void setLastLogin(Date lastLogin) { this.lastLogin = lastLogin; }

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

    public int getXpForNextLevel()
    {
        if (level == 0) return 200;

        int previousLevelXp = getXpForLevel(level);
        return (int) Math.ceil((previousLevelXp * 2 + previousLevelXp / 2.0) / 100.0) * 100;
    }

    private int getXpForLevel(int level)
    {
        if (level <= 1) return 200;

        int xp = 200;
        for (int i = 2; i <= level; i++) {
            xp = (int) Math.ceil((xp * 2 + xp / 2.0) / 100.0) * 100;
        }
        return xp;
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", email='" + email + '\'' +
                ", username='" + username + '\'' +
                ", level=" + level +
                ", title='" + title + '\'' +
                '}';
    }
}
