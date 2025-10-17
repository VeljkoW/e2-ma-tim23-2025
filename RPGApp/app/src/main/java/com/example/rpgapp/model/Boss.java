package com.example.rpgapp.model;

import java.util.Date;

public class Boss {
    private String id;
    private String userId; // User who is fighting this boss
    private int level;
    private int hp;
    private int startingHp; // Track original HP for percentage calculations
    private int numberOfAttacks; // Number of attacks by user at boss (max 5 at one fight)
    private int startingNumberOfAttacks; // Track original number of attacks
    private double chanceTododge; // Calculated from unsolved missions
    private double startingChanceToDodge; // Track original chance to dodge
    private int coinReward;
    private int startingCoinReward; // Track original coin reward
    private double chanceForEquipmentReward; // Default 20%
    private boolean alive;
    private Date dateOfLastFight;
    private Date dateOfCreation;
    private Date dateOfFinishing;

    // Required for Firestore serialization
    public Boss() {
        this.chanceForEquipmentReward = 0.2; // Default 20%
        this.alive = true;
        this.numberOfAttacks = 5; // Start with 5 attacks available
        this.startingNumberOfAttacks = 5; // Track starting attacks
        this.dateOfCreation = new Date();
    }

    public Boss(String userId, int level, int hp, int coinReward) {
        this();
        this.userId = userId;
        this.level = level;
        this.hp = hp;
        this.startingHp = hp; // Set starting HP when boss is created
        this.coinReward = coinReward;
        this.startingCoinReward = coinReward; // Set starting coin reward
        this.chanceTododge = 0.0; // Will be calculated based on unsolved missions
        this.startingChanceToDodge = 0.0; // Set starting chance to dodge
    }

    // Getters
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public int getLevel() { return level; }
    public int getHp() { return hp; }
    public int getStartingHp() { return startingHp; }
    public int getNumberOfAttacks() { return numberOfAttacks; }
    public int getStartingNumberOfAttacks() { return startingNumberOfAttacks; }
    public double getChanceTododge() { return chanceTododge; }
    public double getStartingChanceToDodge() { return startingChanceToDodge; }
    public int getCoinReward() { return coinReward; }
    public int getStartingCoinReward() { return startingCoinReward; }
    public double getChanceForEquipmentReward() { return chanceForEquipmentReward; }
    public boolean isAlive() { return alive; }
    public Date getDateOfLastFight() { return dateOfLastFight; }
    public Date getDateOfCreation() { return dateOfCreation; }
    public Date getDateOfFinishing() { return dateOfFinishing; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setLevel(int level) { this.level = level; }
    public void setHp(int hp) { this.hp = hp; }
    public void setStartingHp(int startingHp) { this.startingHp = startingHp; }
    public void setNumberOfAttacks(int numberOfAttacks) { this.numberOfAttacks = numberOfAttacks; }
    public void setStartingNumberOfAttacks(int startingNumberOfAttacks) { this.startingNumberOfAttacks = startingNumberOfAttacks; }
    public void setChanceTododge(double chanceTododge) { this.chanceTododge = chanceTododge; }
    public void setStartingChanceToDodge(double startingChanceToDodge) { this.startingChanceToDodge = startingChanceToDodge; }
    public void setCoinReward(int coinReward) { this.coinReward = coinReward; }
    public void setStartingCoinReward(int startingCoinReward) { this.startingCoinReward = startingCoinReward; }
    public void setChanceForEquipmentReward(double chanceForEquipmentReward) { this.chanceForEquipmentReward = chanceForEquipmentReward; }
    public void setAlive(boolean alive) { this.alive = alive; }
    public void setDateOfLastFight(Date dateOfLastFight) { this.dateOfLastFight = dateOfLastFight; }
    public void setDateOfCreation(Date dateOfCreation) { this.dateOfCreation = dateOfCreation; }
    public void setDateOfFinishing(Date dateOfFinishing) { this.dateOfFinishing = dateOfFinishing; }

    // Utility methods
    public void defeat() {
        this.alive = false;
        this.dateOfFinishing = new Date();
    }

    public double getHpPercentage() {
        if (startingHp <= 0) return 0.0;
        return (double) hp / startingHp * 100.0;
    }

    public boolean canBeAttacked() {
        return alive && numberOfAttacks > 0 && hp > 0;
    }

    @Override
    public String toString() {
        return "Boss{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", level=" + level +
                ", hp=" + hp +
                ", startingHp=" + startingHp +
                ", numberOfAttacks=" + numberOfAttacks +
                ", chanceTododge=" + chanceTododge +
                ", coinReward=" + coinReward +
                ", alive=" + alive +
                ", dateOfCreation=" + dateOfCreation +
                '}';
    }
}
