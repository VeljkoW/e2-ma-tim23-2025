package com.example.rpgapp.model;

import androidx.annotation.ColorInt;

public class Category {
    private String id;
    private String name;
    private String description; // optional
    private String userId;
    @ColorInt
    private int color;
    private java.util.Date createDateTime;

    public Category(String id, String name, String description, String userId, int color) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.userId = userId;
        this.color = color;
        this.createDateTime = new java.util.Date();
    }

    // Required for Firestore serialization
    public Category() {}

    // Available colors for categories
    public static final int[] AVAILABLE_COLORS = {
        0xFF4CAF50, // Green
        0xFF2196F3, // Blue
        0xFFFFC107, // Amber
        0xFFF44336, // Red
        0xFF9C27B0, // Purple
        0xFFFF5722, // Deep Orange
        0xFF607D8B, // Blue Grey
        0xFF795548, // Brown
        0xFFE91E63, // Pink
        0xFF00BCD4, // Cyan
        0xFF8BC34A, // Light Green
        0xFFFF9800, // Orange
        0xFF673AB7, // Deep Purple
        0xFF3F51B5, // Indigo
        0xFF009688, // Teal
        0xFFCDDC39, // Lime
        0xFFFFEB3B, // Yellow
        0xFF9E9E9E  // Grey
    };

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public int getColor() { return color; }
    public void setColor(int color) { this.color = color; }

    public java.util.Date getCreateDateTime() { return createDateTime; }
    public void setCreateDateTime(java.util.Date createDateTime) { this.createDateTime = createDateTime; }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Category category = (Category) obj;
        return id != null ? id.equals(category.id) : category.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
