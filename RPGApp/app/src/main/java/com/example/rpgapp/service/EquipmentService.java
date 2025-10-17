package com.example.rpgapp.service;

import android.content.Context;

import com.example.rpgapp.model.Equipment;
import com.example.rpgapp.model.User;
import com.example.rpgapp.repository.EquipmentRepository;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class EquipmentService
{

    private final EquipmentRepository equipmentRepository;
    private final FirebaseFirestore db;
    private final Context context;

    public EquipmentService(Context context)
    {
        this.context = context;
        this.equipmentRepository = new EquipmentRepository();
        this.db = FirebaseFirestore.getInstance();
    }

    public int calculateEquipmentPrice(int userLevel, int percentage)
    {
        int nextBossReward = calculateBossReward(userLevel);
        return (int) Math.ceil((nextBossReward * percentage) / 100.0);
    }

    private int calculateBossReward(int userLevel)
    {
        if (userLevel == 0 || userLevel == 1)
        {
            return 200;
        }
        
        int reward = 200;
        for (int i = 2; i <= userLevel + 1; i++)
        {
            reward = (int) Math.ceil(reward * 1.2);
        }
        return reward;
    }

    public void purchasePotion(String userId, Equipment.PotionType potionType, int userLevel, EquipmentRepository.EquipmentCallback<Equipment> callback) {
        int percentage = 0;
        switch (potionType)
        {
            case TEMP_POWER_20:
                percentage = 50;
                break;
            case TEMP_POWER_40:
                percentage = 70;
                break;
            case PERMANENT_POWER_5:
                percentage = 200;
                break;
            case PERMANENT_POWER_10:
                percentage = 1000;
                break;
        }

        int price = calculateEquipmentPrice(userLevel, percentage);

        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot ->
                {
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null && user.getCoins() >= price)
                    {
                        Equipment equipment = Equipment.createPotion(userId, potionType, price);

                        equipmentRepository.addEquipment(equipment)
                                .addOnSuccessListener(aVoid ->
                                {
                                    user.setCoins(user.getCoins() - price);
                                    db.collection("users").document(userId).set(user)
                                            .addOnSuccessListener(aVoid1 -> callback.onSuccess(equipment))
                                            .addOnFailureListener(callback::onFailure);
                                })
                                .addOnFailureListener(callback::onFailure);
                    }
                    else
                    {
                        callback.onFailure(new Exception("Not enough coins"));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void purchaseClothing(String userId, Equipment.ClothingType clothingType, int userLevel, EquipmentRepository.EquipmentCallback<Equipment> callback) {
        int percentage = 0;
        switch (clothingType) {
            case GLOVES:
            case SHIELD:
                percentage = 60;
                break;
            case BOOTS:
                percentage = 80;
                break;
        }

        int price = calculateEquipmentPrice(userLevel, percentage);

        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot ->
                {
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null && user.getCoins() >= price)
                    {
                        Equipment equipment = Equipment.createClothing(userId, clothingType, price);

                        equipmentRepository.addEquipment(equipment)
                                .addOnSuccessListener(aVoid ->
                                {
                                    user.setCoins(user.getCoins() - price);
                                    db.collection("users").document(userId).set(user)
                                            .addOnSuccessListener(aVoid1 -> callback.onSuccess(equipment))
                                            .addOnFailureListener(callback::onFailure);
                                })
                                .addOnFailureListener(callback::onFailure);
                    }
                    else
                    {
                        callback.onFailure(new Exception("Not enough coins"));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void activateEquipment(Equipment equipment, EquipmentRepository.EquipmentCallback<Void> callback)
    {
        equipment.activate();
        equipmentRepository.updateEquipment(equipment)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    public void deactivateEquipment(Equipment equipment, EquipmentRepository.EquipmentCallback<Void> callback)
    {
        equipment.deactivate();
        equipmentRepository.updateEquipment(equipment)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    public void upgradeWeapon(String userId, Equipment weapon, int userLevel, EquipmentRepository.EquipmentCallback<Equipment> callback) {
        int upgradeCost = calculateEquipmentPrice(userLevel, 60);

        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot ->
                {
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null && user.getCoins() >= upgradeCost)
                    {
                        weapon.upgradeWeapon();

                        equipmentRepository.updateEquipment(weapon)
                                .addOnSuccessListener(aVoid ->
                                {
                                    user.setCoins(user.getCoins() - upgradeCost);
                                    db.collection("users").document(userId).set(user)
                                            .addOnSuccessListener(aVoid1 -> callback.onSuccess(weapon))
                                            .addOnFailureListener(callback::onFailure);
                                })
                                .addOnFailureListener(callback::onFailure);
                    }
                    else
                    {
                        callback.onFailure(new Exception("Not enough coins"));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void getUserEquipment(String userId, EquipmentRepository.EquipmentCallback<List<Equipment>> callback)
    {
        equipmentRepository.getEquipmentList(
                equipmentRepository.getUserEquipment(userId),
                callback
        );
    }


    public void getActiveEquipment(String userId, EquipmentRepository.EquipmentCallback<List<Equipment>> callback)
    {
        equipmentRepository.getEquipmentList(
                equipmentRepository.getActiveEquipment(userId),
                callback
        );
    }

    public void getEquipmentByType(String userId, Equipment.EquipmentType type, EquipmentRepository.EquipmentCallback<List<Equipment>> callback)
    {
        equipmentRepository.getEquipmentList(
                equipmentRepository.getEquipmentByType(userId, type),
                callback
        );
    }

    public void calculateTotalPowerBonus(String userId, EquipmentRepository.EquipmentCallback<Integer> callback)
    {
        getActiveEquipment(userId, new EquipmentRepository.EquipmentCallback<List<Equipment>>()
        {
            @Override
            public void onSuccess(List<Equipment> equipmentList)
            {
                int totalBonus = 0;
                for (Equipment eq : equipmentList)
                {
                    totalBonus += eq.getPowerBonus();
                }
                callback.onSuccess(totalBonus);
            }

            @Override
            public void onFailure(Exception e)
            {
                callback.onFailure(e);
            }
        });
    }

    public void calculateTotalAttackChanceBonus(String userId, EquipmentRepository.EquipmentCallback<Integer> callback)
    {
        getActiveEquipment(userId, new EquipmentRepository.EquipmentCallback<List<Equipment>>() {
            @Override
            public void onSuccess(List<Equipment> equipmentList)
            {
                int totalBonus = 0;
                for (Equipment eq : equipmentList)
                {
                    totalBonus += eq.getAttackChanceBonus();
                }
                callback.onSuccess(totalBonus);
            }

            @Override
            public void onFailure(Exception e)
            {
                callback.onFailure(e);
            }
        });
    }

    public void calculateTotalCoinBonus(String userId, EquipmentRepository.EquipmentCallback<Integer> callback)
    {
        getActiveEquipment(userId, new EquipmentRepository.EquipmentCallback<List<Equipment>>()
        {
            @Override
            public void onSuccess(List<Equipment> equipmentList)
            {
                int totalBonus = 0;
                for (Equipment eq : equipmentList)
                {
                    totalBonus += eq.getCoinBonus();
                }
                callback.onSuccess(totalBonus);
            }

            @Override
            public void onFailure(Exception e)
            {
                callback.onFailure(e);
            }
        });
    }
}

