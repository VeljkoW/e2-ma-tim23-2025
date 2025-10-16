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
import com.example.rpgapp.model.Equipment;
import com.example.rpgapp.service.AuthService;
import com.example.rpgapp.service.EquipmentService;
import com.example.rpgapp.repository.EquipmentRepository;

import java.util.ArrayList;
import java.util.List;

public class PreBossEquipmentActivity extends AppCompatActivity {
    private static final String TAG = "PreBossEquipmentActivity";

    private RecyclerView rvClothing, rvWeapons;
    private Button btnFightBoss;
    private ImageButton btnBack;

    private EquipmentAdapter clothingAdapter;
    private EquipmentAdapter weaponAdapter;

    private AuthService authService;
    private EquipmentService equipmentService;
    private EquipmentRepository equipmentRepository;

    private String currentUserId;
    private String bossId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pre_boss_equipment);

        // Initialize services
        authService = new AuthService(this);
        equipmentService = new EquipmentService(this);
        equipmentRepository = new EquipmentRepository();

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
        loadEquipment();
    }

    private void initializeViews() {
        rvClothing = findViewById(R.id.rvClothing);
        rvWeapons = findViewById(R.id.rvWeapons);
        btnFightBoss = findViewById(R.id.btnFightBoss);
        btnBack = findViewById(R.id.btnBack);

        // Setup RecyclerViews
        rvClothing.setLayoutManager(new GridLayoutManager(this, 3));
        rvWeapons.setLayoutManager(new GridLayoutManager(this, 3));

        // Setup adapters
        clothingAdapter = new EquipmentAdapter(this::onClothingClick);
        weaponAdapter = new EquipmentAdapter(this::onWeaponClick);

        rvClothing.setAdapter(clothingAdapter);
        rvWeapons.setAdapter(weaponAdapter);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnFightBoss.setOnClickListener(v -> startBossFight());
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
    }

    private void onClothingClick(Equipment equipment) {
        toggleEquipmentSelection(equipment);
    }

    private void onWeaponClick(Equipment equipment) {
        toggleEquipmentSelection(equipment);
    }

    private void toggleEquipmentSelection(Equipment equipment) {
        boolean newEquippedState = !equipment.isEquipped();
        equipment.setEquipped(newEquippedState);

        // Update equipment in database
        equipmentRepository.updateEquipment(equipment)
            .addOnSuccessListener(result -> {
                runOnUiThread(() -> {
                    String message = equipment.getName() + " " +
                        (newEquippedState ? "equipped" : "unequipped");
                    Toast.makeText(PreBossEquipmentActivity.this, message, Toast.LENGTH_SHORT).show();

                    // Refresh the adapters to show updated state
                    if (equipment.getType() == Equipment.EquipmentType.CLOTHING) {
                        clothingAdapter.notifyDataSetChanged();
                    } else if (equipment.getType() == Equipment.EquipmentType.WEAPON) {
                        weaponAdapter.notifyDataSetChanged();
                    }
                });
            })
            .addOnFailureListener(e -> {
                runOnUiThread(() -> {
                    // Revert the local change if database update failed
                    equipment.setEquipped(!newEquippedState);
                    Toast.makeText(PreBossEquipmentActivity.this,
                        "Failed to update equipment: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
                });
            });
    }

    private void startBossFight() {
        Intent intent = new Intent(this, BossFightActivity.class);
        intent.putExtra("boss_id", bossId);
        startActivity(intent);
        finish(); // Close this activity so user can't go back to equipment selection
    }
}

