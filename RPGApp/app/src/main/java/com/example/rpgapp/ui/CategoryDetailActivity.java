package com.example.rpgapp.ui;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.rpgapp.R;
import com.example.rpgapp.callback.AuthCallback;
import com.example.rpgapp.model.Category;
import com.example.rpgapp.model.User;
import com.example.rpgapp.repository.CategoryRepository;
import com.example.rpgapp.service.AuthService;
import com.google.android.material.appbar.MaterialToolbar;
import java.util.List;

public class CategoryDetailActivity extends AppCompatActivity {

    private View colorDisplay;

    private CategoryRepository categoryRepository;
    private AuthService authService;

    private String categoryId;
    private String categoryName;
    private String categoryDescription;
    private int currentColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_detail);

        // Setup toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Category Details");
        }

        // Initialize
        categoryRepository = new CategoryRepository();
        authService = new AuthService(this);

        // Get data from intent
        categoryId = getIntent().getStringExtra("CATEGORY_ID");
        categoryName = getIntent().getStringExtra("CATEGORY_NAME");
        categoryDescription = getIntent().getStringExtra("CATEGORY_DESCRIPTION");
        currentColor = getIntent().getIntExtra("CATEGORY_COLOR", Category.AVAILABLE_COLORS[0]);

        // Find views
        TextView nameText = findViewById(R.id.categoryNameDetail);
        TextView descriptionText = findViewById(R.id.categoryDescriptionDetail);
        colorDisplay = findViewById(R.id.categoryColorDisplay);
        Button changeColorButton = findViewById(R.id.changeColorButton);

        // Set data
        nameText.setText(categoryName);
        if (categoryDescription != null && !categoryDescription.isEmpty()) {
            descriptionText.setText(categoryDescription);
            descriptionText.setVisibility(View.VISIBLE);
        } else {
            descriptionText.setVisibility(View.GONE);
        }
        updateColorDisplay(currentColor);

        // Change color button
        changeColorButton.setOnClickListener(v -> showColorPicker());
    }

    private void updateColorDisplay(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        colorDisplay.setBackground(drawable);
    }

    private void showColorPicker() {
        authService.getCurrentUser(new AuthCallback<User>() {
            @Override
            public void onResult(User user) {
                if (user != null) {
                    // Get available colors (excluding already used colors by other categories)
                    categoryRepository.getUsedColors(user.getId(), new CategoryRepository.CategoryCallback<List<Integer>>() {
                        @Override
                        public void onResult(List<Integer> usedColors) {
                            runOnUiThread(() -> showColorPickerDialog(usedColors));
                        }

                        @Override
                        public void onError(Exception e) {
                            runOnUiThread(() ->
                                Toast.makeText(CategoryDetailActivity.this, "Error loading colors: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                        }
                    });
                }
            }
        });
    }

    private void showColorPickerDialog(List<Integer> usedColors) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_color_picker);
        dialog.setTitle("Choose Category Color");

        // Make the dialog wider
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                (int) (getResources().getDisplayMetrics().widthPixels * 0.9), // 90% of screen width
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        GridLayout colorGrid = dialog.findViewById(R.id.colorGrid);
        Button buttonCancel = dialog.findViewById(R.id.buttonCancel);
        Button buttonConfirm = dialog.findViewById(R.id.buttonConfirm);
        View currentColorPreview = dialog.findViewById(R.id.currentColorPreview);

        // Check if views are found
        if (colorGrid == null || buttonCancel == null || buttonConfirm == null || currentColorPreview == null) {
            Toast.makeText(this, "Error: Dialog views not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Temporary selected color
        final int[] tempSelectedColor = {currentColor};

        // Set up grid with colors
        setupColorGrid(colorGrid, usedColors, currentColorPreview, tempSelectedColor);

        // Update preview with current selected color
        updateDialogColorPreview(currentColorPreview, currentColor);

        buttonCancel.setOnClickListener(v -> {
            Toast.makeText(this, "Color change cancelled", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        buttonConfirm.setOnClickListener(v -> {
            if (tempSelectedColor[0] != currentColor) {
                Toast.makeText(this, "Updating color...", Toast.LENGTH_SHORT).show();
                updateCategoryColor(tempSelectedColor[0]);
            } else {
                Toast.makeText(this, "No color change", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    private void setupColorGrid(GridLayout colorGrid, List<Integer> usedColors, View preview, int[] tempSelectedColor) {
        colorGrid.setColumnCount(6); // Match CategoryCreationActivity
        colorGrid.removeAllViews();

        for (int color : Category.AVAILABLE_COLORS) {
            View colorView = createColorGridItem(color, usedColors, preview, tempSelectedColor, colorGrid);
            colorGrid.addView(colorView);
        }
    }

    private View createColorGridItem(int color, List<Integer> usedColors, View preview, int[] tempSelectedColor, GridLayout grid) {
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

        boolean isUsed = usedColors != null && usedColors.contains(color) && color != currentColor;
        boolean isSelected = color == tempSelectedColor[0];

        if (isUsed) {
            // Gray out used colors and add cross line
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
            colorContainer.addView(colorView);
        }

        colorView.setBackground(drawable);

        if (!isUsed) {
            colorContainer.setOnClickListener(v -> {
                tempSelectedColor[0] = color;
                updateDialogColorPreview(preview, color);

                // Update all color views in the grid to reflect new selection
                for (int i = 0; i < grid.getChildCount(); i++) {
                    View child = grid.getChildAt(i);
                    if (child instanceof FrameLayout) {
                        FrameLayout container = (FrameLayout) child;
                        View innerColorView = container.getChildAt(0);
                        if (innerColorView != null && innerColorView.getBackground() instanceof GradientDrawable) {
                            // Get the color index for this view
                            int index = i;
                            if (index < Category.AVAILABLE_COLORS.length) {
                                int viewColor = Category.AVAILABLE_COLORS[index];
                                GradientDrawable viewDrawable = (GradientDrawable) innerColorView.getBackground();
                                boolean viewIsUsed = usedColors != null && usedColors.contains(viewColor) && viewColor != currentColor;

                                if (!viewIsUsed) {
                                    if (viewColor == tempSelectedColor[0]) {
                                        viewDrawable.setStroke(4, Color.BLACK); // Selected
                                    } else {
                                        viewDrawable.setStroke(2, Color.GRAY); // Normal
                                    }
                                }
                            }
                        }
                    }
                }
            });
        }

        return colorContainer;
    }

    private void updateDialogColorPreview(View preview, int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        drawable.setStroke(4, Color.GRAY);
        preview.setBackground(drawable);
    }

    private void updateCategoryColor(int newColor) {
        authService.getCurrentUser(new AuthCallback<User>() {
            @Override
            public void onResult(User user) {
                if (user != null) {
                    Category category = new Category(categoryId, categoryName, categoryDescription, user.getId(), newColor);

                    categoryRepository.updateCategory(category, new CategoryRepository.CategoryCallback<Void>() {
                        @Override
                        public void onResult(Void result) {
                            runOnUiThread(() -> {
                                currentColor = newColor;
                                updateColorDisplay(newColor);
                                Toast.makeText(CategoryDetailActivity.this, "Color updated successfully!", Toast.LENGTH_SHORT).show();
                            });
                        }

                        @Override
                        public void onError(Exception e) {
                            runOnUiThread(() ->
                                Toast.makeText(CategoryDetailActivity.this, "Error updating color: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                        }
                    });
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
