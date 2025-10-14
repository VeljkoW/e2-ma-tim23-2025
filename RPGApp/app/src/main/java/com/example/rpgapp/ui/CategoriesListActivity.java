package com.example.rpgapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.rpgapp.R;
import com.example.rpgapp.adapter.CategoryAdapter;
import com.example.rpgapp.callback.AuthCallback;
import com.example.rpgapp.model.Category;
import com.example.rpgapp.model.User;
import com.example.rpgapp.repository.CategoryRepository;
import com.example.rpgapp.service.AuthService;
import com.google.android.material.appbar.MaterialToolbar;
import java.util.ArrayList;
import java.util.List;

public class CategoriesListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private CategoryAdapter adapter;
    private CategoryRepository categoryRepository;
    private AuthService authService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_categories_list);

        // Setup toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Categories");
        }

        // Initialize
        categoryRepository = new CategoryRepository();
        authService = new AuthService(this);

        // Setup RecyclerView
        recyclerView = findViewById(R.id.categoriesRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CategoryAdapter(new ArrayList<>(), this::onCategoryClick);
        recyclerView.setAdapter(adapter);

        // Load categories
        loadCategories();
    }

    private void loadCategories() {
        authService.getCurrentUser(new AuthCallback<User>() {
            @Override
            public void onResult(User user) {
                if (user != null) {
                    categoryRepository.getCategoriesByUserId(user.getId(), new CategoryRepository.CategoryCallback<List<Category>>() {
                        @Override
                        public void onResult(List<Category> categories) {
                            runOnUiThread(() -> {
                                if (categories.isEmpty()) {
                                    Toast.makeText(CategoriesListActivity.this, "No categories yet. Create one!", Toast.LENGTH_SHORT).show();
                                }
                                adapter.updateCategories(categories);
                            });
                        }

                        @Override
                        public void onError(Exception e) {
                            runOnUiThread(() ->
                                Toast.makeText(CategoriesListActivity.this, "Error loading categories: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                        }
                    });
                }
            }
        });
    }

    private void onCategoryClick(Category category) {
        Intent intent = new Intent(this, CategoryDetailActivity.class);
        intent.putExtra("CATEGORY_ID", category.getId());
        intent.putExtra("CATEGORY_NAME", category.getName());
        intent.putExtra("CATEGORY_DESCRIPTION", category.getDescription());
        intent.putExtra("CATEGORY_COLOR", category.getColor());
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCategories(); // Refresh list when returning from detail view
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

