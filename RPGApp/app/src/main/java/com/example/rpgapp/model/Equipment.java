package com.example.rpgapp.model;

public class Equipment
{
    private String id;
    private String name;
    private String type; // helmet, armor, weapon, shield
    private int powerBonus;
    private String iconResource;
    private boolean isEquipped;

    public Equipment() {}

    public Equipment(String id, String name, String type, int powerBonus, String iconResource)
    {
        this.id = id;
        this.name = name;
        this.type = type;
        this.powerBonus = powerBonus;
        this.iconResource = iconResource;
        this.isEquipped = false;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getPowerBonus() { return powerBonus; }
    public void setPowerBonus(int powerBonus) { this.powerBonus = powerBonus; }

    public String getIconResource() { return iconResource; }
    public void setIconResource(String iconResource) { this.iconResource = iconResource; }

    public boolean isEquipped() { return isEquipped; }
    public void setEquipped(boolean equipped) { isEquipped = equipped; }
}

