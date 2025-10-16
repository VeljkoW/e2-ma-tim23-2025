package com.example.rpgapp.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rpgapp.R;
import com.example.rpgapp.adapter.ShopItemAdapter;
import com.example.rpgapp.model.Equipment;
import com.example.rpgapp.model.User;
import com.example.rpgapp.repository.EquipmentRepository;
import com.example.rpgapp.service.EquipmentService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ShopActivity extends AppCompatActivity {

    private static final String TAG = "ShopActivity";

    private TextView tvCoins, tvShopTitle;
    private RecyclerView rvPotions, rvClothing;
    private ImageButton btnBack;

    private EquipmentService equipmentService;
    private FirebaseAuth mAuth;
    private User currentUser;

    private ShopItemAdapter potionsAdapter;
    private ShopItemAdapter clothingAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop);

        mAuth = FirebaseAuth.getInstance();
        equipmentService = new EquipmentService(this);

        initViews();
        loadUserData();
    }

    private void initViews() {
        tvCoins = findViewById(R.id.tvCoins);
        tvShopTitle = findViewById(R.id.tvShopTitle);
        rvPotions = findViewById(R.id.rvPotions);
        rvClothing = findViewById(R.id.rvClothing);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        // Setup RecyclerViews
        rvPotions.setLayoutManager(new LinearLayoutManager(this));
        rvClothing.setLayoutManager(new LinearLayoutManager(this));
    }

    private void loadUserData() {
        Log.d(TAG, "loadUserData: Starting to load user data");
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser == null) {
            Log.e(TAG, "loadUserData: No authenticated user");
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d(TAG, "loadUserData: Loading user from Firestore: " + firebaseUser.getUid());
        FirebaseFirestore.getInstance().collection("users")
                .document(firebaseUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Log.d(TAG, "loadUserData: Successfully loaded document");
                    currentUser = documentSnapshot.toObject(User.class);
                    if (currentUser != null) {
                        Log.d(TAG, "loadUserData: User loaded - Level: " + currentUser.getLevel() + ", Coins: " + currentUser.getCoins());
                        updateCoinsDisplay();
                        setupShopItems();
                    } else {
                        Log.e(TAG, "loadUserData: currentUser is null after conversion");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "loadUserData: Failed to load user data", e);
                    Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void updateCoinsDisplay() {
        if (currentUser != null) {
            tvCoins.setText(String.format("💰 %d coins", currentUser.getCoins()));
        }
    }

    private void setupShopItems()
    {
        Log.d(TAG, "setupShopItems: Starting setup");
        if (currentUser == null)
        {
            Log.e(TAG, "setupShopItems: currentUser is null, aborting");
            return;
        }

        // Setup potions
        List<ShopItem> potionItems = new ArrayList<>();
        for (Equipment.PotionType type : Equipment.PotionType.values()) {
            int percentage = getPotionPercentage(type);
            int price = equipmentService.calculateEquipmentPrice(currentUser.getLevel(), percentage);
            potionItems.add(new ShopItem(type.name(), getPotionName(type), getPotionDescription(type), price, ShopItem.ItemType.POTION));
            Log.d(TAG, "setupShopItems: Added potion - " + type.name() + " at " + price + " coins");
        }

        potionsAdapter = new ShopItemAdapter(potionItems, this::onPotionPurchase);
        rvPotions.setAdapter(potionsAdapter);
        Log.d(TAG, "setupShopItems: Set potions adapter with " + potionItems.size() + " items");

        // Setup clothing
        List<ShopItem> clothingItems = new ArrayList<>();
        Log.d(TAG, "setupShopItems: ClothingType values count: " + Equipment.ClothingType.values().length);
        for (Equipment.ClothingType type : Equipment.ClothingType.values()) {
            int percentage = getClothingPercentage(type);
            int price = equipmentService.calculateEquipmentPrice(currentUser.getLevel(), percentage);
            clothingItems.add(new ShopItem(type.name(), getClothingName(type), getClothingDescription(type), price, ShopItem.ItemType.CLOTHING));
            Log.d(TAG, "setupShopItems: Added clothing - " + type.name() + " (" + getClothingName(type) + ") at " + price + " coins");
        }

        clothingAdapter = new ShopItemAdapter(clothingItems, this::onClothingPurchase);
        rvClothing.setAdapter(clothingAdapter);
        Log.d(TAG, "setupShopItems: Set clothing adapter with " + clothingItems.size() + " items");
    }

    private void onPotionPurchase(ShopItem item) {
        if (currentUser == null) return;

        Equipment.PotionType potionType = Equipment.PotionType.valueOf(item.getId());

        new AlertDialog.Builder(this)
                .setTitle("Purchase Potion")
                .setMessage(String.format("Do you want to buy %s for %d coins?", item.getName(), item.getPrice()))
                .setPositiveButton("Buy", (dialog, which) -> {
                    if (currentUser.getCoins() >= item.getPrice()) {
                        equipmentService.purchasePotion(currentUser.getId(), potionType, currentUser.getLevel(),
                                new EquipmentRepository.EquipmentCallback<Equipment>() {
                                    @Override
                                    public void onSuccess(Equipment result) {
                                        runOnUiThread(() -> {
                                            Toast.makeText(ShopActivity.this, "Potion purchased!", Toast.LENGTH_SHORT).show();
                                            loadUserData(); // Refresh coins
                                        });
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        runOnUiThread(() -> {
                                            Toast.makeText(ShopActivity.this, "Purchase failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                                    }
                                });
                    } else {
                        Toast.makeText(this, "Not enough coins!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void onClothingPurchase(ShopItem item) {
        if (currentUser == null) return;

        Equipment.ClothingType clothingType = Equipment.ClothingType.valueOf(item.getId());

        new AlertDialog.Builder(this)
                .setTitle("Purchase Clothing")
                .setMessage(String.format("Do you want to buy %s for %d coins?", item.getName(), item.getPrice()))
                .setPositiveButton("Buy", (dialog, which) -> {
                    if (currentUser.getCoins() >= item.getPrice()) {
                        equipmentService.purchaseClothing(currentUser.getId(), clothingType, currentUser.getLevel(),
                                new EquipmentRepository.EquipmentCallback<Equipment>() {
                                    @Override
                                    public void onSuccess(Equipment result) {
                                        runOnUiThread(() -> {
                                            Toast.makeText(ShopActivity.this, "Clothing purchased!", Toast.LENGTH_SHORT).show();
                                            loadUserData(); // Refresh coins
                                        });
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        runOnUiThread(() -> {
                                            Toast.makeText(ShopActivity.this, "Purchase failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                                    }
                                });
                    } else {
                        Toast.makeText(this, "Not enough coins!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Helper methods
    private int getPotionPercentage(Equipment.PotionType type) {
        switch (type) {
            case TEMP_POWER_20: return 50;
            case TEMP_POWER_40: return 70;
            case PERMANENT_POWER_5: return 200;
            case PERMANENT_POWER_10: return 1000;
            default: return 0;
        }
    }

    private String getPotionName(Equipment.PotionType type) {
        switch (type) {
            case TEMP_POWER_20: return "Potion of Strength I";
            case TEMP_POWER_40: return "Potion of Strength II";
            case PERMANENT_POWER_5: return "Elixir of Power I";
            case PERMANENT_POWER_10: return "Elixir of Power II";
            default: return "Unknown";
        }
    }

    private String getPotionDescription(Equipment.PotionType type) {
        switch (type) {
            case TEMP_POWER_20: return "+20% PP for one battle";
            case TEMP_POWER_40: return "+40% PP for one battle";
            case PERMANENT_POWER_5: return "+5% PP permanently";
            case PERMANENT_POWER_10: return "+10% PP permanently";
            default: return "";
        }
    }

    private int getClothingPercentage(Equipment.ClothingType type) {
        switch (type) {
            case GLOVES:
            case SHIELD: return 60;
            case BOOTS: return 80;
            default: return 0;
        }
    }

    private String getClothingName(Equipment.ClothingType type) {
        switch (type) {
            case GLOVES: return "Power Gloves";
            case SHIELD: return "Guardian Shield";
            case BOOTS: return "Swift Boots";
            default: return "Unknown";
        }
    }

    private String getClothingDescription(Equipment.ClothingType type) {
        switch (type) {
            case GLOVES: return "+10% PP for 2 battles";
            case SHIELD: return "+10% attack chance for 2 battles";
            case BOOTS: return "+40% chance for extra attack (2 battles)";
            default: return "";
        }
    }

    // ShopItem helper class
    public static class ShopItem {
        public enum ItemType { POTION, CLOTHING }

        private String id;
        private String name;
        private String description;
        private int price;
        private ItemType type;

        public ShopItem(String id, String name, String description, int price, ItemType type) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.price = price;
            this.type = type;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public int getPrice() { return price; }
        public ItemType getType() { return type; }
    }
}
