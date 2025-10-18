package com.example.rpgapp.util;

import android.content.Context;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.ContextCompat;

import com.example.rpgapp.model.Equipment;
import com.example.rpgapp.repository.EquipmentRepository;

/**
 * Utility class for handling equipment rewards from boss fights
 */
public class EquipmentRewardSystem {
    private static final String TAG = "EquipmentRewardSystem";
    private static final double WEAPON_CHANCE = 0.05; // 5% chance for weapon, 95% for clothing

    private final Context context;
    private EquipmentRewardCallback callback;

    public interface EquipmentRewardCallback {
        void onRewardProcessed(Equipment equipment); // null if no equipment
    }

    public EquipmentRewardSystem(Context context) {
        this.context = context;
    }

    /**
     * Rolls for equipment reward and processes the result using boss's equipment drop chance
     */
    public void rollForEquipmentReward(String userId, double bossEquipmentDropChance, EquipmentRewardCallback callback) {
        this.callback = callback;

        // Roll for equipment drop using boss's chance
        double roll = Math.random();
        Log.d(TAG, "Equipment drop roll: " + roll + " vs boss chance: " + bossEquipmentDropChance);

        if (roll <= bossEquipmentDropChance) {
            // Player won equipment! Now determine type
            determineEquipmentType(userId);
        } else {
            Log.d(TAG, "No equipment dropped this time");
            if (callback != null) {
                callback.onRewardProcessed(null);
            }
        }
    }

    /**
     * Determines which type of equipment to award (95% clothing, 5% weapon)
     */
    private void determineEquipmentType(String userId) {
        double typeRoll = Math.random();
        Log.d(TAG, "Equipment type roll: " + typeRoll);

        Equipment.EquipmentType rewardType;
        if (typeRoll <= WEAPON_CHANCE) {
            // 5% chance for weapon
            rewardType = Equipment.EquipmentType.WEAPON;
        } else {
            // 95% chance for clothing
            rewardType = Equipment.EquipmentType.CLOTHING;
        }

        // Create and award the equipment
        awardRandomEquipment(userId, rewardType);
    }

    /**
     * Awards random equipment of the specified type to the user
     */
    private void awardRandomEquipment(String userId, Equipment.EquipmentType equipmentType) {
        Equipment rewardEquipment = null;

        if (equipmentType == Equipment.EquipmentType.CLOTHING) {
            // Randomly select clothing type
            Equipment.ClothingType[] clothingTypes = Equipment.ClothingType.values();
            Equipment.ClothingType randomClothing = clothingTypes[(int)(Math.random() * clothingTypes.length)];

            // Create clothing with price 0 since it's a reward
            rewardEquipment = Equipment.createClothing(userId, randomClothing, 0);
            Log.d(TAG, "Awarded clothing: " + rewardEquipment.getName());
        } else if (equipmentType == Equipment.EquipmentType.WEAPON) {
            // Randomly select weapon type
            Equipment.WeaponType[] weaponTypes = Equipment.WeaponType.values();
            Equipment.WeaponType randomWeapon = weaponTypes[(int)(Math.random() * weaponTypes.length)];

            rewardEquipment = Equipment.createWeapon(userId, randomWeapon);
            Log.d(TAG, "Awarded weapon: " + rewardEquipment.getName());
        }

        if (rewardEquipment != null) {
            // Save equipment to Firebase
            EquipmentRepository equipmentRepository = new EquipmentRepository();
            Equipment finalRewardEquipment = rewardEquipment;

            equipmentRepository.addEquipment(rewardEquipment)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Successfully saved equipment reward: " + finalRewardEquipment.getName());

                    // Notify callback
                    if (callback != null) {
                        callback.onRewardProcessed(finalRewardEquipment);
                    }
                })
                .addOnFailureListener(exception -> {
                    Log.e(TAG, "Failed to save equipment reward", exception);
                    Toast.makeText(context, "Equipment reward could not be saved", Toast.LENGTH_SHORT).show();

                    // Still notify callback even if save failed
                    if (callback != null) {
                        callback.onRewardProcessed(finalRewardEquipment);
                    }
                });
        }
    }

    /**
     * Utility method to add equipment reward text to a rewards container
     */
    public static void addEquipmentRewardToUI(Context context, LinearLayout rewardsContainer, Equipment equipment) {
        if (rewardsContainer == null) return;

        // Remove any existing equipment reward text
        TextView existingEquipmentText = rewardsContainer.findViewWithTag("equipment_reward");
        if (existingEquipmentText != null) {
            rewardsContainer.removeView(existingEquipmentText);
        }

        // Create new equipment reward text
        TextView tvEquipmentReward = new TextView(context);
        tvEquipmentReward.setTag("equipment_reward");
        tvEquipmentReward.setTextSize(16f);
        tvEquipmentReward.setPadding(16, 8, 16, 8);
        tvEquipmentReward.setGravity(android.view.Gravity.CENTER);

        if (equipment != null) {
            String rewardText = "🎉 Equipment Reward: " + equipment.getName() + "!";
            tvEquipmentReward.setText(rewardText);
            tvEquipmentReward.setTextColor(ContextCompat.getColor(context, android.R.color.holo_orange_light));
        } else {
            tvEquipmentReward.setText("No equipment this time");
            tvEquipmentReward.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray));
        }

        rewardsContainer.addView(tvEquipmentReward);
    }
}
