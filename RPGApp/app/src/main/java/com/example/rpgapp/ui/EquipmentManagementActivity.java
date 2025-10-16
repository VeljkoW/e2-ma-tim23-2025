package com.example.rpgapp.ui;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rpgapp.R;
import com.example.rpgapp.adapter.EquipmentAdapter;
import com.example.rpgapp.model.Equipment;
import com.example.rpgapp.model.User;
import com.example.rpgapp.repository.EquipmentRepository;
import com.example.rpgapp.service.EquipmentService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class EquipmentManagementActivity extends AppCompatActivity
{

    private TextView tvTitle;
    private RecyclerView rvPotions, rvClothing, rvWeapons;
    private ImageButton btnBack;
    
    private EquipmentService equipmentService;
    private FirebaseAuth mAuth;
    private User currentUser;
    
    private EquipmentAdapter potionsAdapter;
    private EquipmentAdapter clothingAdapter;
    private EquipmentAdapter weaponsAdapter;
    
    private List<Equipment> potionsList = new ArrayList<>();
    private List<Equipment> clothingList = new ArrayList<>();
    private List<Equipment> weaponsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_equipment_management);

        mAuth = FirebaseAuth.getInstance();
        equipmentService = new EquipmentService(this);

        initViews();
        loadUserData();
    }

    private void initViews() {
        tvTitle = findViewById(R.id.tvTitle);
        rvPotions = findViewById(R.id.rvPotions);
        rvClothing = findViewById(R.id.rvClothing);
        rvWeapons = findViewById(R.id.rvWeapons);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        // Setup RecyclerViews
        rvPotions.setLayoutManager(new LinearLayoutManager(this));
        rvClothing.setLayoutManager(new LinearLayoutManager(this));
        rvWeapons.setLayoutManager(new LinearLayoutManager(this));

        // Setup adapters with click listeners
        potionsAdapter = new EquipmentAdapter(this::onPotionClick);
        clothingAdapter = new EquipmentAdapter(this::onClothingClick);
        weaponsAdapter = new EquipmentAdapter(this::onWeaponClick);

        rvPotions.setAdapter(potionsAdapter);
        rvClothing.setAdapter(clothingAdapter);
        rvWeapons.setAdapter(weaponsAdapter);
    }

    private void loadUserData() {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        FirebaseFirestore.getInstance().collection("users")
                .document(firebaseUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    currentUser = documentSnapshot.toObject(User.class);
                    if (currentUser != null) {
                        loadEquipment();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void loadEquipment() {
        if (currentUser == null) return;

        // Load potions
        equipmentService.getEquipmentByType(currentUser.getId(), Equipment.EquipmentType.POTION,
                new EquipmentRepository.EquipmentCallback<List<Equipment>>() {
                    @Override
                    public void onSuccess(List<Equipment> result) {
                        runOnUiThread(() -> {
                            potionsList.clear();
                            // Filtriraj samo neiskorišćene napitke
                            for (Equipment eq : result) {
                                if (eq.canBeUsed()) {
                                    potionsList.add(eq);
                                }
                            }
                            potionsAdapter.setEquipmentList(potionsList);
                        });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        runOnUiThread(() -> Toast.makeText(EquipmentManagementActivity.this,
                                "Failed to load potions", Toast.LENGTH_SHORT).show());
                    }
                });

        // Load clothing
        equipmentService.getEquipmentByType(currentUser.getId(), Equipment.EquipmentType.CLOTHING,
                new EquipmentRepository.EquipmentCallback<List<Equipment>>() {
                    @Override
                    public void onSuccess(List<Equipment> result) {
                        runOnUiThread(() -> {
                            clothingList.clear();
                            // Filtriraj samo odeću koja može da se koristi
                            for (Equipment eq : result) {
                                if (eq.canBeUsed()) {
                                    clothingList.add(eq);
                                }
                            }
                            clothingAdapter.setEquipmentList(clothingList);
                        });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        runOnUiThread(() -> Toast.makeText(EquipmentManagementActivity.this,
                                "Failed to load clothing", Toast.LENGTH_SHORT).show());
                    }
                });

        // Load weapons
        equipmentService.getEquipmentByType(currentUser.getId(), Equipment.EquipmentType.WEAPON,
                new EquipmentRepository.EquipmentCallback<List<Equipment>>() {
                    @Override
                    public void onSuccess(List<Equipment> result) {
                        runOnUiThread(() -> {
                            weaponsList.clear();
                            weaponsList.addAll(result);
                            weaponsAdapter.setEquipmentList(weaponsList);
                        });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        runOnUiThread(() -> Toast.makeText(EquipmentManagementActivity.this,
                                "Failed to load weapons", Toast.LENGTH_SHORT).show());
                    }
                });
    }

    private void onPotionClick(Equipment potion) {
        String action = potion.isActive() ? "Deactivate" : "Activate";
        String message = potion.isActive()
                ? "Do you want to deactivate this potion?"
                : "Do you want to activate this potion? It will be used in your next battle.";

        new AlertDialog.Builder(this)
                .setTitle(action + " " + potion.getName())
                .setMessage(message)
                .setPositiveButton(action, (dialog, which) -> {
                    if (potion.isActive()) {
                        equipmentService.deactivateEquipment(potion, new EquipmentRepository.EquipmentCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                runOnUiThread(() -> {
                                    Toast.makeText(EquipmentManagementActivity.this, "Potion deactivated", Toast.LENGTH_SHORT).show();
                                    loadEquipment();
                                });
                            }

                            @Override
                            public void onFailure(Exception e) {
                                runOnUiThread(() -> Toast.makeText(EquipmentManagementActivity.this,
                                        "Failed to deactivate", Toast.LENGTH_SHORT).show());
                            }
                        });
                    } else {
                        equipmentService.activateEquipment(potion, new EquipmentRepository.EquipmentCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                runOnUiThread(() -> {
                                    Toast.makeText(EquipmentManagementActivity.this, "Potion activated", Toast.LENGTH_SHORT).show();
                                    loadEquipment();
                                });
                            }

                            @Override
                            public void onFailure(Exception e) {
                                runOnUiThread(() -> Toast.makeText(EquipmentManagementActivity.this,
                                        "Failed to activate", Toast.LENGTH_SHORT).show());
                            }
                        });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void onClothingClick(Equipment clothing) {
        String action = clothing.isActive() ? "Deactivate" : "Activate";
        String message = clothing.isActive()
                ? String.format("Remaining battles: %d/%d\nDo you want to deactivate?",
                    clothing.getRemainingBattles(), clothing.getMaxBattles())
                : "Do you want to activate this clothing? It will last for 2 battles.";

        new AlertDialog.Builder(this)
                .setTitle(action + " " + clothing.getName())
                .setMessage(message)
                .setPositiveButton(action, (dialog, which) -> {
                    if (clothing.isActive()) {
                        equipmentService.deactivateEquipment(clothing, new EquipmentRepository.EquipmentCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                runOnUiThread(() -> {
                                    Toast.makeText(EquipmentManagementActivity.this, "Clothing deactivated", Toast.LENGTH_SHORT).show();
                                    loadEquipment();
                                });
                            }

                            @Override
                            public void onFailure(Exception e) {
                                runOnUiThread(() -> Toast.makeText(EquipmentManagementActivity.this,
                                        "Failed to deactivate", Toast.LENGTH_SHORT).show());
                            }
                        });
                    } else {
                        equipmentService.activateEquipment(clothing, new EquipmentRepository.EquipmentCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                runOnUiThread(() -> {
                                    Toast.makeText(EquipmentManagementActivity.this, "Clothing activated", Toast.LENGTH_SHORT).show();
                                    loadEquipment();
                                });
                            }

                            @Override
                            public void onFailure(Exception e) {
                                runOnUiThread(() -> Toast.makeText(EquipmentManagementActivity.this,
                                        "Failed to activate", Toast.LENGTH_SHORT).show());
                            }
                        });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void onWeaponClick(Equipment weapon) {
        int upgradeCost = equipmentService.calculateEquipmentPrice(currentUser.getLevel(), 60);

        new AlertDialog.Builder(this)
                .setTitle(weapon.getName())
                .setMessage(String.format("Level: %d\nCurrent bonus: %.2f%%\n\nUpgrade cost: %d coins",
                        weapon.getUpgradeLevel(), weapon.getCurrentBonus(), upgradeCost))
                .setPositiveButton("Upgrade", (dialog, which) -> {
                    if (currentUser.getCoins() >= upgradeCost) {
                        equipmentService.upgradeWeapon(currentUser.getId(), weapon, currentUser.getLevel(),
                                new EquipmentRepository.EquipmentCallback<Equipment>() {
                                    @Override
                                    public void onSuccess(Equipment result) {
                                        runOnUiThread(() -> {
                                            Toast.makeText(EquipmentManagementActivity.this,
                                                    "Weapon upgraded!", Toast.LENGTH_SHORT).show();
                                            loadUserData(); // Refresh data
                                        });
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        runOnUiThread(() -> Toast.makeText(EquipmentManagementActivity.this,
                                                "Upgrade failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                                    }
                                });
                    } else {
                        Toast.makeText(EquipmentManagementActivity.this, "Not enough coins!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
