package com.example.rpgapp.model;

public class Badge {
    private String id;
    private String name;
    private String description;
    private String iconResource;
    private boolean isUnlocked;

    public Badge() {}

    public Badge(String id, String name, String description, String iconResource)
    {
        this.id = id;
        this.name = name;
        this.description = description;
        this.iconResource = iconResource;
        this.isUnlocked = false;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIconResource() { return iconResource; }
    public void setIconResource(String iconResource) { this.iconResource = iconResource; }

    public boolean isUnlocked() { return isUnlocked; }
    public void setUnlocked(boolean unlocked) { isUnlocked = unlocked; }
}

