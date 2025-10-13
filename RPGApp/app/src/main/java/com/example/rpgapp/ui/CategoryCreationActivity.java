package com.example.rpgapp.ui;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.rpgapp.R;
import com.example.rpgapp.model.Category;
import com.example.rpgapp.repository.CategoryRepository;
import com.example.rpgapp.service.AuthService;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class CategoryCreationActivity extends AppCompatActivity {
    private EditText editTextCategoryName, editTextCategoryDescription;
    private Button buttonSelectColor, buttonCreateCategory;
    private View selectedColorPreview;
    private CategoryRepository categoryRepository;
    private AuthService authService;
    private List<Integer> availableColors;
    private int selectedColor = 0xFF4CAF50; // Default green color

    // Comprehensive color palette with 60+ colors
    private static final int[] COLOR_PALETTE = {
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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_creation);

        editTextCategoryName = findViewById(R.id.editTextCategoryName);
        editTextCategoryDescription = findViewById(R.id.editTextCategoryDescription);
        buttonSelectColor = findViewById(R.id.buttonSelectColor);
        selectedColorPreview = findViewById(R.id.selectedColorPreview);
        buttonCreateCategory = findViewById(R.id.buttonCreateCategory);

        categoryRepository = new CategoryRepository();
        authService = new AuthService(this);

        setupActionBar();
        setupColorPreview();
        loadAvailableColors();

        buttonSelectColor.setOnClickListener(v -> showColorPicker());
        buttonCreateCategory.setOnClickListener(v -> createCategory());
    }

    private void setupActionBar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Create Category");
        }
    }

    private void setupColorPreview() {
        updateColorPreview();
    }

    private void updateColorPreview() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(selectedColor);
        drawable.setStroke(4, Color.GRAY);
        selectedColorPreview.setBackground(drawable);
    }

    private void loadAvailableColors() {
        FirebaseUser firebaseUser = authService.getCurrentFirebaseUser();
        if (firebaseUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        categoryRepository.getUsedColors(firebaseUser.getUid(), new CategoryRepository.CategoryCallback<List<Integer>>() {
            @Override
            public void onResult(List<Integer> usedColors) {
                runOnUiThread(() -> {
                    // We'll show all colors but disable used ones in the picker
                    // This gives users more flexibility while still preventing duplicates
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> Toast.makeText(CategoryCreationActivity.this, "Error loading used colors: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void showColorPicker() {
        FirebaseUser firebaseUser = authService.getCurrentFirebaseUser();
        if (firebaseUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get used colors first, then show picker
        categoryRepository.getUsedColors(firebaseUser.getUid(), new CategoryRepository.CategoryCallback<List<Integer>>() {
            @Override
            public void onResult(List<Integer> usedColors) {
                runOnUiThread(() -> showColorPickerDialog(usedColors));
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(CategoryCreationActivity.this, "Error loading colors: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    showColorPickerDialog(null); // Show picker anyway without used color info
                });
            }
        });
    }

    private void showColorPickerDialog(List<Integer> usedColors) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_color_picker);
        dialog.setTitle("Choose Category Color");

        GridLayout colorGrid = dialog.findViewById(R.id.colorGrid);
        Button buttonCancel = dialog.findViewById(R.id.buttonCancel);
        Button buttonConfirm = dialog.findViewById(R.id.buttonConfirm);
        View currentColorPreview = dialog.findViewById(R.id.currentColorPreview);

        // Set up grid with colors
        setupColorGrid(colorGrid, usedColors, currentColorPreview, dialog);

        // Update preview with current selected color
        updateDialogColorPreview(currentColorPreview, selectedColor);

        buttonCancel.setOnClickListener(v -> dialog.dismiss());

        buttonConfirm.setOnClickListener(v -> {
            updateColorPreview();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void setupColorGrid(GridLayout colorGrid, List<Integer> usedColors, View preview, Dialog dialog) {
        colorGrid.setColumnCount(6); // Changed from 8 to 6 columns for better fit
        colorGrid.removeAllViews();

        for (int color : COLOR_PALETTE) {
            View colorView = createColorGridItem(color, usedColors, preview);
            colorGrid.addView(colorView);
        }
    }

    private View createColorGridItem(int color, List<Integer> usedColors, View preview) {
        // Create a container layout for the color circle and cross line
        FrameLayout colorContainer = new FrameLayout(this);
        int size = (int) (40 * getResources().getDisplayMetrics().density);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = size;
        params.height = size;
        params.setMargins(4, 4, 4, 4);
        colorContainer.setLayoutParams(params);

        // Create the color circle view
        View colorView = new View(this);
        FrameLayout.LayoutParams colorParams = new FrameLayout.LayoutParams(size, size);
        colorView.setLayoutParams(colorParams);

        // Create circular background
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);

        boolean isUsed = usedColors != null && usedColors.contains(color);
        boolean isSelected = color == selectedColor;

        if (isUsed) {
            // Gray out used colors
            drawable.setStroke(3, Color.RED);
            colorView.setAlpha(0.4f);
            colorView.setEnabled(false);
            colorContainer.setEnabled(false);

            // Create diagonal cross line
            View crossLine = new View(this);
            FrameLayout.LayoutParams crossParams = new FrameLayout.LayoutParams(
                (int) (size * 1.2), // Slightly longer than diameter
                4 // 4dp thick line
            );
            crossParams.gravity = android.view.Gravity.CENTER;
            crossLine.setLayoutParams(crossParams);
            crossLine.setBackgroundColor(Color.RED);
            crossLine.setRotation(45f); // Diagonal line

            // Add both views to container
            colorContainer.addView(colorView);
            colorContainer.addView(crossLine);
        } else {
            if (isSelected) {
                // Highlight selected color
                drawable.setStroke(4, Color.BLACK);
            } else {
                // Normal color
                drawable.setStroke(2, Color.GRAY);
            }

            // Only add the color view for non-used colors
            colorContainer.addView(colorView);
        }

        colorView.setBackground(drawable);

        if (!isUsed) {
            colorContainer.setOnClickListener(v -> {
                selectedColor = color;
                updateDialogColorPreview(preview, color);

                // Update all color views in the grid to reflect new selection
                GridLayout grid = (GridLayout) colorContainer.getParent();
                for (int i = 0; i < grid.getChildCount(); i++) {
                    View child = grid.getChildAt(i);
                    updateColorViewSelection(child, i);
                }
            });
        }

        return colorContainer;
    }

    private void updateColorViewSelection(View colorView, int colorIndex) {
        if (colorIndex < COLOR_PALETTE.length) {
            int color = COLOR_PALETTE[colorIndex];
            GradientDrawable drawable = (GradientDrawable) colorView.getBackground();

            if (color == selectedColor) {
                drawable.setStroke(6, Color.BLACK); // Selected
            } else if (colorView.getAlpha() == 0.5f) {
                // Keep used color styling
            } else {
                drawable.setStroke(2, Color.GRAY); // Normal
            }
        }
    }

    private void updateDialogColorPreview(View preview, int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        drawable.setStroke(4, Color.GRAY);
        preview.setBackground(drawable);
    }

    private void createCategory() {
        String name = editTextCategoryName.getText().toString().trim();
        String description = editTextCategoryDescription.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            editTextCategoryName.setError("Category name is required");
            return;
        }

        FirebaseUser firebaseUser = authService.getCurrentFirebaseUser();
        if (firebaseUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if selected color is already used
        categoryRepository.getUsedColors(firebaseUser.getUid(), new CategoryRepository.CategoryCallback<List<Integer>>() {
            @Override
            public void onResult(List<Integer> usedColors) {
                runOnUiThread(() -> {
                    if (usedColors.contains(selectedColor)) {
                        Toast.makeText(CategoryCreationActivity.this, "This color is already used by another category. Please select a different color.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    // Create category
                    Category category = new Category(null, name, description.isEmpty() ? null : description, firebaseUser.getUid(), selectedColor);

                    buttonCreateCategory.setEnabled(false);
                    categoryRepository.createCategory(category, new CategoryRepository.CategoryCallback<String>() {
                        @Override
                        public void onResult(String categoryId) {
                            runOnUiThread(() -> {
                                Toast.makeText(CategoryCreationActivity.this, "Category created successfully", Toast.LENGTH_SHORT).show();
                                finish();
                            });
                        }

                        @Override
                        public void onError(Exception e) {
                            runOnUiThread(() -> {
                                buttonCreateCategory.setEnabled(true);
                                Toast.makeText(CategoryCreationActivity.this, "Error creating category: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> Toast.makeText(CategoryCreationActivity.this, "Error checking color availability: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }
}
