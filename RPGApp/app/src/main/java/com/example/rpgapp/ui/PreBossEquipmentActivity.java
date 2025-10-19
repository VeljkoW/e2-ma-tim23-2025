package com.example.rpgapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rpgapp.R;
import com.example.rpgapp.adapter.EquipmentAdapter;
import com.example.rpgapp.callback.AuthCallback;
import com.example.rpgapp.model.Equipment;
import com.example.rpgapp.model.User;
import com.example.rpgapp.model.Boss;
import com.example.rpgapp.service.AuthService;
import com.example.rpgapp.repository.EquipmentRepository;
import com.example.rpgapp.repository.UserRepository;
import com.example.rpgapp.repository.BossRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PreBossEquipmentActivity extends AppCompatActivity {

    private RecyclerView rvClothing, rvWeapons, rvPotions;
    private Button btnFightBoss;
    private ImageButton btnBack;

    private EquipmentAdapter clothingAdapter;
    private EquipmentAdapter weaponAdapter;
    private EquipmentAdapter potionAdapter;

    private AuthService authService;
    private EquipmentRepository equipmentRepository;
    private UserRepository userRepository;
    private BossRepository bossRepository;

    private String currentUserId;
    private String bossId;
    private User currentUser;
    private Boss currentBoss;
    private Random random;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pre_boss_equipment);

        // Initialize services and repositories
        authService = new AuthService(this);
        equipmentRepository = new EquipmentRepository();
        userRepository = new UserRepository(this);
        bossRepository = new BossRepository();
        random = new Random();

        // Get current user ID and boss ID
        currentUserId = authService.getCurrentUserId();
        bossId = getIntent().getStringExtra("boss_id");

        if (bossId == null) {
            Toast.makeText(this, "Error: No boss selected", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupClickListeners();
        loadUserAndBoss();
    }

    private void initializeViews() {
        rvClothing = findViewById(R.id.rvClothing);
        rvWeapons = findViewById(R.id.rvWeapons);
        rvPotions = findViewById(R.id.rvPotions);
        btnFightBoss = findViewById(R.id.btnFightBoss);
        btnBack = findViewById(R.id.btnBack);

        // Setup RecyclerViews
        rvClothing.setLayoutManager(new GridLayoutManager(this, 3));
        rvWeapons.setLayoutManager(new GridLayoutManager(this, 3));
        rvPotions.setLayoutManager(new GridLayoutManager(this, 3));

        // Setup adapters
        clothingAdapter = new EquipmentAdapter(this::onClothingClick);
        weaponAdapter = new EquipmentAdapter(this::onWeaponClick);
        potionAdapter = new EquipmentAdapter(this::onPotionClick);

        rvClothing.setAdapter(clothingAdapter);
        rvWeapons.setAdapter(weaponAdapter);
        rvPotions.setAdapter(potionAdapter);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnFightBoss.setOnClickListener(v -> startBossFight());
    }

    private void loadUserAndBoss() {
        // Load current user
        userRepository.getUserById(currentUserId, new AuthCallback<User>() {
            @Override
            public void onResult(User user) {
                if (user != null) {
                    currentUser = user;
                    loadBoss();
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(PreBossEquipmentActivity.this,
                            "Failed to load user data", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
            }
        });
    }

    private void loadBoss() {
        bossRepository.getBossById(bossId)
            .addOnSuccessListener(boss -> {
                if (boss != null) {
                    currentBoss = boss;
                    loadEquipment();
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(PreBossEquipmentActivity.this,
                            "Failed to load boss data", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
            })
            .addOnFailureListener(e -> {
                runOnUiThread(() -> {
                    Toast.makeText(PreBossEquipmentActivity.this,
                        "Failed to load boss: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
            });
    }

    private void loadEquipment() {
        // Load clothing equipment
        equipmentRepository.getEquipmentList(
            equipmentRepository.getEquipmentByType(currentUserId, Equipment.EquipmentType.CLOTHING),
            new EquipmentRepository.EquipmentCallback<List<Equipment>>() {
                @Override
                public void onSuccess(List<Equipment> equipmentList) {
                    runOnUiThread(() -> {
                        List<Equipment> availableClothing = new ArrayList<>();
                        for (Equipment equipment : equipmentList) {
                            if (equipment.canBeUsed()) {
                                availableClothing.add(equipment);
                            }
                        }
                        clothingAdapter.setEquipmentList(availableClothing);
                    });
                }

                @Override
                public void onFailure(Exception e) {
                    runOnUiThread(() ->
                        Toast.makeText(PreBossEquipmentActivity.this,
                            "Failed to load clothing: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show()
                    );
                }
            });

        // Load weapon equipment
        equipmentRepository.getEquipmentList(
            equipmentRepository.getEquipmentByType(currentUserId, Equipment.EquipmentType.WEAPON),
            new EquipmentRepository.EquipmentCallback<List<Equipment>>() {
                @Override
                public void onSuccess(List<Equipment> equipmentList) {
                    runOnUiThread(() -> {
                        List<Equipment> availableWeapons = new ArrayList<>();
                        for (Equipment equipment : equipmentList) {
                            if (equipment.canBeUsed()) {
                                availableWeapons.add(equipment);
                            }
                        }
                        weaponAdapter.setEquipmentList(availableWeapons);
                    });
                }

                @Override
                public void onFailure(Exception e) {
                    runOnUiThread(() ->
                        Toast.makeText(PreBossEquipmentActivity.this,
                            "Failed to load weapons: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show()
                    );
                }
            });

        // Load potion equipment
        equipmentRepository.getEquipmentList(
            equipmentRepository.getEquipmentByType(currentUserId, Equipment.EquipmentType.POTION),
            new EquipmentRepository.EquipmentCallback<List<Equipment>>() {
                @Override
                public void onSuccess(List<Equipment> equipmentList) {
                    runOnUiThread(() -> {
                        List<Equipment> availablePotions = new ArrayList<>();
                        for (Equipment equipment : equipmentList) {
                            if (equipment.canBeUsed()) {
                                availablePotions.add(equipment);
                            }
                        }
                        potionAdapter.setEquipmentList(availablePotions);
                    });
                }

                @Override
                public void onFailure(Exception e) {
                    runOnUiThread(() ->
                        Toast.makeText(PreBossEquipmentActivity.this,
                            "Failed to load potions: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show()
                    );
                }
            });
    }

    private void onClothingClick(Equipment equipment) {
        toggleEquipmentSelection(equipment);
    }

    private void onWeaponClick(Equipment equipment) {
        toggleEquipmentSelection(equipment);
    }

    private void onPotionClick(Equipment equipment) {
        toggleEquipmentSelection(equipment);
    }

    private void toggleEquipmentSelection(Equipment equipment) {
        // Validation checks before allowing equipment selection
        if (!canEquipItem(equipment)) {
            return; // Exit early if equipment cannot be equipped
        }

        boolean newActiveState = !isEquipmentActive(equipment);

        // Calculate equipment effects before updating
        EquipmentEffects effects = calculateEquipmentEffects(equipment);

        // Apply or remove effects based on new state
        if (newActiveState) {
            applyEquipmentEffects(effects);
            // Set both isActive and isEquipped for all types
            equipment.setActive(true);
            equipment.setEquipped(true);
        } else {
            removeEquipmentEffects(effects);
            // Set both isActive and isEquipped to false for all types
            equipment.setActive(false);
            equipment.setEquipped(false);
        }

        // Update equipment in database
        equipmentRepository.updateEquipment(equipment)
            .addOnSuccessListener(result -> updateUserAndBoss(newActiveState, effects))
            .addOnFailureListener(e -> runOnUiThread(() -> {
                // Revert all changes if database update failed
                if (newActiveState) {
                    equipment.setActive(false);
                    equipment.setEquipped(false);
                    removeEquipmentEffects(effects);
                } else {
                    equipment.setActive(true);
                    equipment.setEquipped(true);
                    applyEquipmentEffects(effects);
                }
                Toast.makeText(PreBossEquipmentActivity.this,
                    "Failed to update equipment: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            }));
    }

    /**
     * Check if equipment is active (either equipped or activated)
     */
    private boolean isEquipmentActive(Equipment equipment) {
        // Check both isActive and isEquipped to handle all cases
        return equipment.isActive() || equipment.isEquipped();
    }

    /**
     * Validates if equipment can be equipped based on current state and remaining battles
     */
    private boolean canEquipItem(Equipment equipment) {
        // Check if trying to unequip an already equipped/active item
        if (isEquipmentActive(equipment)) {
            // Allow unequipping - this is valid
            return true;
        }

        // For clothing items, check remaining battles
        if (equipment.getType() == Equipment.EquipmentType.CLOTHING) {
            if (equipment.getRemainingBattles() <= 0) {
                runOnUiThread(() ->
                    Toast.makeText(PreBossEquipmentActivity.this,
                        equipment.getName() + " has no remaining battles!",
                        Toast.LENGTH_SHORT).show()
                );
                return false;
            }
        }

        // For potions, check if already used
        if (equipment.getType() == Equipment.EquipmentType.POTION) {
            if (equipment.isUsed()) {
                runOnUiThread(() ->
                    Toast.makeText(PreBossEquipmentActivity.this,
                        equipment.getName() + " has already been used!",
                        Toast.LENGTH_SHORT).show()
                );
                return false;
            }
        }

        // Additional check: ensure equipment can be used (uses existing method from Equipment class)
        if (!equipment.canBeUsed()) {
            runOnUiThread(() ->
                Toast.makeText(PreBossEquipmentActivity.this,
                    equipment.getName() + " cannot be used at this time!",
                    Toast.LENGTH_SHORT).show()
            );
            return false;
        }

        return true;
    }

    private EquipmentEffects calculateEquipmentEffects(Equipment equipment) {
        EquipmentEffects effects = new EquipmentEffects();

        // Power points bonus (added to user)
        effects.powerPointsBonus = equipment.getPowerBonus();

        // Coin bonus (added to boss coin reward)
        effects.coinBonus = equipment.getCoinBonus();

        // Attack chance bonus (reduces boss chance to dodge)
        effects.attackChanceBonus = equipment.getAttackChanceBonus();

        // Extra attack chance (chance to add +1 attack to boss numberOfAttacks)
        effects.extraAttackChance = equipment.getExtraAttackChance();
        effects.extraAttackGranted = (effects.extraAttackChance > 0) &&
            (random.nextInt(100) < effects.extraAttackChance);

        return effects;
    }

    private void applyEquipmentEffects(EquipmentEffects effects) {
        // Apply to user
        if (effects.powerPointsBonus > 0) {
            currentUser.setPowerPoints(currentUser.getPowerPoints() + effects.powerPointsBonus);
        }

        // Apply to boss
        if (effects.coinBonus > 0) {
            currentBoss.setCoinReward(currentBoss.getCoinReward() + effects.coinBonus);
        }

        if (effects.attackChanceBonus > 0) {
            double newChanceToDodge = Math.max(0,
                currentBoss.getChanceTododge() - (effects.attackChanceBonus / 100.0));
            currentBoss.setChanceTododge(newChanceToDodge);
        }

        if (effects.extraAttackGranted) {
            currentBoss.setNumberOfAttacks(currentBoss.getNumberOfAttacks() + 1);
        }
    }

    private void removeEquipmentEffects(EquipmentEffects effects) {
        // Remove from user
        if (effects.powerPointsBonus > 0) {
            currentUser.setPowerPoints(Math.max(0,
                currentUser.getPowerPoints() - effects.powerPointsBonus));
        }

        // Remove from boss
        if (effects.coinBonus > 0) {
            currentBoss.setCoinReward(Math.max(0,
                currentBoss.getCoinReward() - effects.coinBonus));
        }

        if (effects.attackChanceBonus > 0) {
            double newChanceToDodge = Math.min(1.0,
                currentBoss.getChanceTododge() + (effects.attackChanceBonus / 100.0));
            currentBoss.setChanceTododge(newChanceToDodge);
        }

        if (effects.extraAttackGranted) {
            currentBoss.setNumberOfAttacks(Math.max(0,
                currentBoss.getNumberOfAttacks() - 1));
        }
    }

    private void updateUserAndBoss(boolean equipped, EquipmentEffects effects) {
        // Update user in database
        userRepository.updateUser(currentUser, new AuthCallback<Boolean>() {
            @Override
            public void onResult(Boolean success) {
                if (success != null && success) {
                    // Update boss in database using the boss's ID
                    bossRepository.updateBoss(currentBoss.getId(), currentBoss)
                        .addOnSuccessListener(aVoid -> runOnUiThread(() -> {
                            String message = equipped ? "Equipment equipped" : "Equipment unequipped";
                            if (effects.extraAttackGranted) {
                                message += " (Extra attack granted!)";
                            }
                            Toast.makeText(PreBossEquipmentActivity.this, message, Toast.LENGTH_SHORT).show();

                            // Refresh the adapters
                            clothingAdapter.notifyDataSetChanged();
                            weaponAdapter.notifyDataSetChanged();
                            potionAdapter.notifyDataSetChanged();
                        }))
                        .addOnFailureListener(e -> runOnUiThread(() ->
                            Toast.makeText(PreBossEquipmentActivity.this,
                                "Failed to update boss: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                        ));
                } else {
                    runOnUiThread(() ->
                        Toast.makeText(PreBossEquipmentActivity.this,
                            "Failed to update user data", Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

    private void startBossFight() {
        Intent intent = new Intent(this, BossFightActivity.class);
        intent.putExtra("boss_id", bossId);
        startActivity(intent);
        finish(); // Close this activity so user can't go back to equipment selection
    }

    // Helper class to track equipment effects
    private static class EquipmentEffects {
        int powerPointsBonus = 0;
        int coinBonus = 0;
        int attackChanceBonus = 0;
        int extraAttackChance = 0;
        boolean extraAttackGranted = false;
    }
}
