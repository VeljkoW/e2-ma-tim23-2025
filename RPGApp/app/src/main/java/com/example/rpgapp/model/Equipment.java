package com.example.rpgapp.model;

import java.util.Date;

public class Equipment
{
    public enum EquipmentType
    {
        POTION,
        CLOTHING,
        WEAPON
    }

    // Potion subtypes
    public enum PotionType
    {
        TEMP_POWER_20,
        TEMP_POWER_40,
        PERMANENT_POWER_5,
        PERMANENT_POWER_10
    }

    public enum ClothingType
    {
        GLOVES,
        SHIELD,
        BOOTS
    }

    public enum WeaponType
    {
        SWORD,
        BOW_ARROW
    }

    private String id;
    private String userId;
    private String name;
    private EquipmentType type;
    private String subType;
    
    // Stats
    private int powerBonus;
    private int attackChanceBonus;
    private int extraAttackChance;
    private int coinBonus;
    
    // Potion specific
    private boolean isTemporary;
    private boolean isUsed;
    
    // Clothing specific
    private int remainingBattles;
    private int maxBattles;
    
    // Weapon specific
    private int upgradeLevel;
    private double currentBonus;
    
    // General
    private boolean isEquipped;
    private boolean isActive;
    private Date purchaseDate;
    private Date activationDate;
    private int purchasePrice;
    private String iconResource;

    public Equipment() {}

    // Constructor za napitke
    public static Equipment createPotion(String userId, PotionType potionType, int price)
    {
        Equipment equipment = new Equipment();
        equipment.userId = userId;
        equipment.type = EquipmentType.POTION;
        equipment.subType = potionType.name();
        equipment.purchasePrice = price;
        equipment.isEquipped = false;
        equipment.isActive = false;
        equipment.isUsed = false;
        equipment.purchaseDate = new Date();
        
        switch (potionType)
        {
            case TEMP_POWER_20:
                equipment.name = "Potion of Strength I";
                equipment.powerBonus = 20;
                equipment.isTemporary = true;
                equipment.iconResource = "ic_potion_strength_1";
                break;
            case TEMP_POWER_40:
                equipment.name = "Potion of Strength II";
                equipment.powerBonus = 40;
                equipment.isTemporary = true;
                equipment.iconResource = "ic_potion_strength_2";
                break;
            case PERMANENT_POWER_5:
                equipment.name = "Elixir of Power I";
                equipment.powerBonus = 5;
                equipment.isTemporary = false;
                equipment.iconResource = "ic_elixir_power_1";
                break;
            case PERMANENT_POWER_10:
                equipment.name = "Elixir of Power II";
                equipment.powerBonus = 10;
                equipment.isTemporary = false;
                equipment.iconResource = "ic_elixir_power_2";
                break;
        }
        return equipment;
    }

    // Constructor za odeću
    public static Equipment createClothing(String userId, ClothingType clothingType, int price)
    {
        Equipment equipment = new Equipment();
        equipment.userId = userId;
        equipment.type = EquipmentType.CLOTHING;
        equipment.subType = clothingType.name();
        equipment.purchasePrice = price;
        equipment.isEquipped = false;
        equipment.isActive = false;
        equipment.remainingBattles = 2;
        equipment.maxBattles = 2;
        equipment.purchaseDate = new Date();
        
        switch (clothingType)
        {
            case GLOVES:
                equipment.name = "Power Gloves";
                equipment.powerBonus = 10;
                equipment.iconResource = "ic_gloves";
                break;
            case SHIELD:
                equipment.name = "Guardian Shield";
                equipment.attackChanceBonus = 10;
                equipment.iconResource = "ic_shield";
                break;
            case BOOTS:
                equipment.name = "Swift Boots";
                equipment.extraAttackChance = 40;
                equipment.iconResource = "ic_boots";
                break;
        }
        return equipment;
    }

    // Constructor za oružje
    public static Equipment createWeapon(String userId, WeaponType weaponType)
    {
        Equipment equipment = new Equipment();
        equipment.userId = userId;
        equipment.type = EquipmentType.WEAPON;
        equipment.subType = weaponType.name();
        equipment.purchasePrice = 0; // Oružje se ne kupuje, dobija se od bosa
        equipment.isEquipped = false;
        equipment.isActive = false;
        equipment.upgradeLevel = 0;
        equipment.purchaseDate = new Date();
        
        switch (weaponType)
        {
            case SWORD:
                equipment.name = "Blade of Power";
                equipment.powerBonus = 5;
                equipment.currentBonus = 5.0;
                equipment.iconResource = "ic_sword";
                break;
            case BOW_ARROW:
                equipment.name = "Fortune Bow";
                equipment.coinBonus = 5;
                equipment.currentBonus = 5.0;
                equipment.iconResource = "ic_bow";
                break;
        }
        return equipment;
    }

    // Metode za upravljanje opremom
    public void activate()
    {
        this.isActive = true;
        this.activationDate = new Date();
        if (type == EquipmentType.CLOTHING)
        {
            this.isEquipped = true;
        }
    }

    public void deactivate()
    {
        this.isActive = false;
        this.isEquipped = false;
    }

    public void useInBattle()
    {
        if (type == EquipmentType.POTION && isTemporary)
        {
            this.isUsed = true;
            this.isActive = false;
        }
        else if (type == EquipmentType.CLOTHING)
        {
            this.remainingBattles--;
            if (this.remainingBattles <= 0)
            {
                this.isActive = false;
                this.isEquipped = false;
            }
        }
    }

    public void upgradeWeapon()
    {
        if (type == EquipmentType.WEAPON)
        {
            this.upgradeLevel++;
            this.currentBonus += 0.01; // +0.01% po upgrade-u
        }
    }

    public void combineWithSameType(Equipment other)
    {
        if (type == EquipmentType.CLOTHING && this.subType.equals(other.subType))
        {
            // Sabiranje bonusa za odeću
            this.powerBonus += other.powerBonus;
            this.attackChanceBonus += other.attackChanceBonus;
            this.extraAttackChance += other.extraAttackChance;
        }
        else if (type == EquipmentType.WEAPON && this.subType.equals(other.subType))
        {
            // Dodavanje 0.02% za duplikat oružja
            this.currentBonus += 0.02;
        }
    }

    public boolean canBeUsed()
    {
        if (type == EquipmentType.POTION)
        {
            return !isUsed;
        }
        else if (type == EquipmentType.CLOTHING)
        {
            return remainingBattles > 0;
        }
        return true;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public EquipmentType getType() { return type; }
    public void setType(EquipmentType type) { this.type = type; }

    public String getSubType() { return subType; }
    public void setSubType(String subType) { this.subType = subType; }

    public int getPowerBonus() { return powerBonus; }
    public void setPowerBonus(int powerBonus) { this.powerBonus = powerBonus; }

    public int getAttackChanceBonus() { return attackChanceBonus; }
    public void setAttackChanceBonus(int attackChanceBonus) { this.attackChanceBonus = attackChanceBonus; }

    public int getExtraAttackChance() { return extraAttackChance; }
    public void setExtraAttackChance(int extraAttackChance) { this.extraAttackChance = extraAttackChance; }

    public int getCoinBonus() { return coinBonus; }
    public void setCoinBonus(int coinBonus) { this.coinBonus = coinBonus; }

    public boolean isTemporary() { return isTemporary; }
    public void setTemporary(boolean temporary) { isTemporary = temporary; }

    public boolean isUsed() { return isUsed; }
    public void setUsed(boolean used) { isUsed = used; }

    public int getRemainingBattles() { return remainingBattles; }
    public void setRemainingBattles(int remainingBattles) { this.remainingBattles = remainingBattles; }

    public int getMaxBattles() { return maxBattles; }
    public void setMaxBattles(int maxBattles) { this.maxBattles = maxBattles; }

    public int getUpgradeLevel() { return upgradeLevel; }
    public void setUpgradeLevel(int upgradeLevel) { this.upgradeLevel = upgradeLevel; }

    public double getCurrentBonus() { return currentBonus; }
    public void setCurrentBonus(double currentBonus) { this.currentBonus = currentBonus; }

    public boolean isEquipped() { return isEquipped; }
    public void setEquipped(boolean equipped) { isEquipped = equipped; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public Date getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(Date purchaseDate) { this.purchaseDate = purchaseDate; }

    public Date getActivationDate() { return activationDate; }
    public void setActivationDate(Date activationDate) { this.activationDate = activationDate; }

    public int getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(int purchasePrice) { this.purchasePrice = purchasePrice; }

    public String getIconResource() { return iconResource; }
    public void setIconResource(String iconResource) { this.iconResource = iconResource; }

    @Override
    public String toString() {
        return name + " (Type: " + type + ", Active: " + isActive + ")";
    }
}
