package com.example.rpgapp.model;

public class Badge {
    private String id;
    private String name;
    private String description;
    private String iconResource;
    private String userId;
    private String allianceBossId; // Optional field for alliance boss badges

    public Badge() {
        this.iconResource = "https://cdn-icons-png.flaticon.com/512/856/856940.png";
    }

    public Badge(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.iconResource = "https://cdn-icons-png.flaticon.com/512/856/856940.png";
    }

    public Badge(String id, String name, String description, String iconResource) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.iconResource = iconResource != null ? iconResource : "https://cdn-icons-png.flaticon.com/512/856/856940.png";
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIconResource() { return iconResource; }
    public void setIconResource(String iconResource) {
        this.iconResource = iconResource != null ? iconResource : "https://cdn-icons-png.flaticon.com/512/856/856940.png";
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getAllianceBossId() { return allianceBossId; }
    public void setAllianceBossId(String allianceBossId) { this.allianceBossId = allianceBossId; }
}
