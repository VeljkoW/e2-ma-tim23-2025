package com.example.rpgapp.repository;

import com.example.rpgapp.model.Equipment;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class EquipmentRepository
{

    private final FirebaseFirestore db;
    private static final String COLLECTION_NAME = "equipment";

    public EquipmentRepository()
    {
        this.db = FirebaseFirestore.getInstance();
    }

    public Task<Void> addEquipment(Equipment equipment)
    {
        if (equipment.getId() == null)
        {
            String id = db.collection(COLLECTION_NAME).document().getId();
            equipment.setId(id);
        }
        return db.collection(COLLECTION_NAME)
                .document(equipment.getId())
                .set(equipment);
    }

    public Task<Void> updateEquipment(Equipment equipment)
    {
        return db.collection(COLLECTION_NAME)
                .document(equipment.getId())
                .set(equipment);
    }

    public Task<Void> deleteEquipment(String equipmentId)
    {
        return db.collection(COLLECTION_NAME)
                .document(equipmentId)
                .delete();
    }

    public Task<DocumentSnapshot> getEquipmentById(String equipmentId)
    {
        return db.collection(COLLECTION_NAME)
                .document(equipmentId)
                .get();
    }

    // Preuzimanje sve opreme korisnika
    public Task<QuerySnapshot> getUserEquipment(String userId) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .get();
    }

    // Preuzimanje aktivne opreme korisnika
    public Task<QuerySnapshot> getActiveEquipment(String userId) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .whereEqualTo("active", true)
                .get();
    }

    public Task<QuerySnapshot> getEquipmentByType(String userId, Equipment.EquipmentType type)
    {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .whereEqualTo("type", type.name())
                .get();
    }

    public Task<QuerySnapshot> getUnusedPotions(String userId)
    {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .whereEqualTo("type", Equipment.EquipmentType.POTION.name())
                .whereEqualTo("used", false)
                .get();
    }

    public Task<QuerySnapshot> getActiveClothing(String userId)
    {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .whereEqualTo("type", Equipment.EquipmentType.CLOTHING.name())
                .whereEqualTo("active", true)
                .whereGreaterThan("remainingBattles", 0)
                .get();
    }

    public Task<QuerySnapshot> getUserWeapons(String userId)
    {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .whereEqualTo("type", Equipment.EquipmentType.WEAPON.name())
                .get();
    }

    public interface EquipmentCallback<T>
    {
        void onSuccess(T result);
        void onFailure(Exception e);
    }

    public void getEquipmentList(Task<QuerySnapshot> task, EquipmentCallback<List<Equipment>> callback)
    {
        task.addOnSuccessListener(queryDocumentSnapshots ->
        {
            List<Equipment> equipmentList = new ArrayList<>();
            for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments())
            {
                Equipment equipment = doc.toObject(Equipment.class);
                if (equipment != null)
                {
                    equipmentList.add(equipment);
                }
            }
            callback.onSuccess(equipmentList);
        }).addOnFailureListener(callback::onFailure);
    }
}

