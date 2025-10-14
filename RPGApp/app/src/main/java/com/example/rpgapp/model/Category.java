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

    // Comprehensive color palette with 60+ colors
    public static final int[] AVAILABLE_COLORS = {
        // Reds
        0xFFE53E3E, 0xFFF56565, 0xFFFF6B6B, 0xFFE63946, 0xFFDC2626, 0xFFB91C1C,
        0xFF991B1B, 0xFF7F1D1D, 0xFFFF1744, 0xFFD32F2F, 0xFFC62828, 0xFFAD1457,

        // Pinks
        0xFFED64A6, 0xFFF687B3, 0xFFEC4899, 0xFFDB2777, 0xFFBE185D, 0xFF9D174D,
        0xFF831843, 0xFF701A75, 0xFFE91E63, 0xFFD81B60, 0xFFC2185B, 0xFFAD1457,

        // Purples
        0xFF9F7AEA, 0xFFB794F6, 0xFF9C88FF, 0xFF805AD5, 0xFF6B46C1, 0xFF553C9A,
        0xFF44337A, 0xFF322659, 0xFF9C27B0, 0xFF8E24AA, 0xFF7B1FA2, 0xFF6A1B9A,

        // Blues
        0xFF4299E1, 0xFF63B3ED, 0xFF7C3AED, 0xFF3182CE, 0xFF2B77CB, 0xFF2C5AA0,
        0xFF2A4A8B, 0xFF1E40AF, 0xFF2196F3, 0xFF1E88E5, 0xFF1976D2, 0xFF1565C0,

        // Cyans
        0xFF00D9FF, 0xFF0891B2, 0xFF0E7490, 0xFF155E75, 0xFF164E63, 0xFF083344,
        0xFF00BCD4, 0xFF00ACC1, 0xFF0097A7, 0xFF00838F, 0xFF006064, 0xFF004D40,

        // Teals
        0xFF38B2AC, 0xFF4FD1C7, 0xFF81E6D9, 0xFF2D3748, 0xFF285E61, 0xFF234E52,
        0xFF1D4044, 0xFF102A43, 0xFF009688, 0xFF00897B, 0xFF00796B, 0xFF00695C,

        // Greens
        0xFF48BB78, 0xFF68D391, 0xFF9AE6B4, 0xFF38A169, 0xFF2F855A, 0xFF276749,
        0xFF22543D, 0xFF1A202C, 0xFF4CAF50, 0xFF43A047, 0xFF388E3C, 0xFF2E7D32,

        // Limes
        0xFF8BC34A, 0xFF9CCC65, 0xFFAED581, 0xFF689F38, 0xFF558B2F, 0xFF33691E,
        0xFFCDDC39, 0xFFD4E157, 0xFFDCE775, 0xFF827717, 0xFF9E9D24, 0xFFA4A91A,

        // Yellows
        0xFFECC94B, 0xFFF6E05E, 0xFFFBBF24, 0xFFD69E2E, 0xFFB7791F, 0xFF975A16,
        0xFF744210, 0xFF5F370E, 0xFFFFEB3B, 0xFFFFD54F, 0xFFFFC107, 0xFFFFA000,

        // Oranges
        0xFFED8936, 0xFFFF9800, 0xFFFFA726, 0xFFFFB74D, 0xFFFFCC02, 0xFFE65100,
        0xFFEF6C00, 0xFFFF8F00, 0xFFFF6F00, 0xFFE65100, 0xFFBF360C, 0xFF9A2A00,

        // Browns
        0xFF8D6E63, 0xFFA1887F, 0xFFBCAAA4, 0xFF6D4C41, 0xFF5D4037, 0xFF4E342E,
        0xFF3E2723, 0xFF1B0000, 0xFF795548, 0xFF6D4C41, 0xFF5D4037, 0xFF4E342E,

        // Grays
        0xFF9E9E9E, 0xFFBDBDBD, 0xFFE0E0E0, 0xFF757575, 0xFF616161, 0xFF424242,
        0xFF212121, 0xFF000000, 0xFF607D8B, 0xFF546E7A, 0xFF455A64, 0xFF37474F
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
