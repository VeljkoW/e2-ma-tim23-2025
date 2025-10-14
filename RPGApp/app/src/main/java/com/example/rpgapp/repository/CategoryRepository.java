package com.example.rpgapp.repository;

import com.example.rpgapp.model.Category;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CategoryRepository {
    private static final String COLLECTION_NAME = "categories";
    private final FirebaseFirestore db;

    public CategoryRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public interface CategoryCallback<T> {
        void onResult(T result);
        void onError(Exception e);
    }

    public void createCategory(Category category, CategoryCallback<String> callback) {
        String categoryId = db.collection(COLLECTION_NAME).document().getId();
        category.setId(categoryId);

        db.collection(COLLECTION_NAME)
                .document(categoryId)
                .set(category)
                .addOnSuccessListener(aVoid -> callback.onResult(categoryId))
                .addOnFailureListener(callback::onError);
    }

    public void getCategoriesByUserId(String userId, CategoryCallback<List<Category>> callback) {
        db.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Category> categories = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Category category = document.toObject(Category.class);
                        categories.add(category);
                    }
                    // Sort by creation time in code instead of database query
                    categories.sort((c1, c2) -> {
                        if (c1.getCreateDateTime() == null && c2.getCreateDateTime() == null) return 0;
                        if (c1.getCreateDateTime() == null) return 1;
                        if (c2.getCreateDateTime() == null) return -1;
                        return c1.getCreateDateTime().compareTo(c2.getCreateDateTime());
                    });
                    callback.onResult(categories);
                })
                .addOnFailureListener(callback::onError);
    }

    public void getUsedColors(String userId, CategoryCallback<List<Integer>> callback) {
        getCategoriesByUserId(userId, new CategoryCallback<List<Category>>() {
            @Override
            public void onResult(List<Category> categories) {
                List<Integer> usedColors = new ArrayList<>();
                for (Category category : categories) {
                    usedColors.add(category.getColor());
                }
                callback.onResult(usedColors);
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        });
    }

    public void getAvailableColors(String userId, CategoryCallback<List<Integer>> callback) {
        getUsedColors(userId, new CategoryCallback<List<Integer>>() {
            @Override
            public void onResult(List<Integer> usedColors) {
                List<Integer> availableColors = new ArrayList<>();
                for (int color : Category.AVAILABLE_COLORS) {
                    if (!usedColors.contains(color)) {
                        availableColors.add(color);
                    }
                }
                callback.onResult(availableColors);
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        });
    }

    public void updateCategory(Category category, CategoryCallback<Void> callback) {
        db.collection(COLLECTION_NAME)
                .document(category.getId())
                .set(category)
                .addOnSuccessListener(aVoid -> callback.onResult(null))
                .addOnFailureListener(callback::onError);
    }

    public void deleteCategory(String categoryId, CategoryCallback<Void> callback) {
        db.collection(COLLECTION_NAME)
                .document(categoryId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onResult(null))
                .addOnFailureListener(callback::onError);
    }
}
