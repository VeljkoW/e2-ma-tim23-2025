package com.example.rpgapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rpgapp.R;
import com.example.rpgapp.model.Equipment;

import java.util.ArrayList;
import java.util.List;

public class EquipmentAdapter extends RecyclerView.Adapter<EquipmentAdapter.EquipmentViewHolder> {

    private List<Equipment> equipmentList;
    private final OnEquipmentClickListener listener;

    public interface OnEquipmentClickListener {
        void onEquipmentClick(Equipment equipment);
    }

    public EquipmentAdapter(OnEquipmentClickListener listener) {
        this.equipmentList = new ArrayList<>();
        this.listener = listener;
    }

    public void setEquipmentList(List<Equipment> equipmentList) {
        this.equipmentList = equipmentList != null ? equipmentList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EquipmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_equipment, parent, false);
        return new EquipmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EquipmentViewHolder holder, int position) {
        Equipment equipment = equipmentList.get(position);
        holder.bind(equipment, listener);
    }

    @Override
    public int getItemCount() {
        return equipmentList.size();
    }

    public static class EquipmentViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivEquipmentIcon;
        private final TextView tvEquipmentName;
        private final TextView tvEquipmentBonus;
        private final TextView tvEquipmentStatus;

        public EquipmentViewHolder(@NonNull View itemView) {
            super(itemView);
            ivEquipmentIcon = itemView.findViewById(R.id.ivEquipmentIcon);
            tvEquipmentName = itemView.findViewById(R.id.tvEquipmentName);
            tvEquipmentBonus = itemView.findViewById(R.id.tvEquipmentBonus);
            tvEquipmentStatus = itemView.findViewById(R.id.tvEquipmentStatus);
        }

        public void bind(Equipment equipment, OnEquipmentClickListener listener) {
            tvEquipmentName.setText(equipment.getName());

            // Display bonus information
            String bonusText = getBonusText(equipment);
            tvEquipmentBonus.setText(bonusText);

            // Display status
            String statusText = getStatusText(equipment);
            tvEquipmentStatus.setText(statusText);
            tvEquipmentStatus.setVisibility(statusText.isEmpty() ? View.GONE : View.VISIBLE);

            // Set icon based on equipment type
            setEquipmentIcon(equipment);

            // Highlight if equipped/active
            if (equipment.isActive() || equipment.isEquipped()) {
                itemView.setAlpha(1.0f);
                itemView.setBackgroundResource(R.drawable.equipment_active_background);
            } else {
                itemView.setAlpha(0.7f);
                itemView.setBackgroundResource(R.drawable.equipment_inactive_background);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEquipmentClick(equipment);
                }
            });
        }

        private String getBonusText(Equipment equipment) {
            StringBuilder bonus = new StringBuilder();

            if (equipment.getPowerBonus() > 0) {
                bonus.append("+").append(equipment.getPowerBonus()).append("% PP");
            }

            if (equipment.getAttackChanceBonus() > 0) {
                if (bonus.length() > 0) bonus.append(" | ");
                bonus.append("+").append(equipment.getAttackChanceBonus()).append("% Attack");
            }

            if (equipment.getExtraAttackChance() > 0) {
                if (bonus.length() > 0) bonus.append(" | ");
                bonus.append("+").append(equipment.getExtraAttackChance()).append("% Extra Attack");
            }

            if (equipment.getCoinBonus() > 0) {
                if (bonus.length() > 0) bonus.append(" | ");
                bonus.append("+").append(equipment.getCoinBonus()).append("% Coins");
            }

            return bonus.toString();
        }

        private String getStatusText(Equipment equipment) {
            if (equipment.isActive()) {
                if (equipment.getType() == Equipment.EquipmentType.CLOTHING && equipment.getRemainingBattles() > 0) {
                    return "Active (" + equipment.getRemainingBattles() + " battles left)";
                } else if (equipment.getType() == Equipment.EquipmentType.POTION && equipment.isTemporary()) {
                    return equipment.isUsed() ? "Used" : "Ready";
                }
                return "Active";
            } else if (equipment.isEquipped()) {
                return "Equipped";
            }
            return "";
        }

        private void setEquipmentIcon(Equipment equipment) {
            int iconResId = R.drawable.ic_equipment_default;

            // Try to get custom icon
            String iconResource = equipment.getIconResource();
            if (iconResource != null && !iconResource.isEmpty()) {
                int customIconResId = itemView.getContext().getResources()
                        .getIdentifier(iconResource, "drawable", itemView.getContext().getPackageName());
                if (customIconResId != 0) {
                    iconResId = customIconResId;
                }
            } else {
                // Set default icon based on type
                switch (equipment.getType()) {
                    case POTION:
                        iconResId = R.drawable.ic_potion;
                        break;
                    case CLOTHING:
                        if (equipment.getSubType() != null) {
                            switch (equipment.getSubType()) {
                                case "GLOVES":
                                    iconResId = R.drawable.ic_gloves;
                                    break;
                                case "SHIELD":
                                    iconResId = R.drawable.ic_shield;
                                    break;
                                case "BOOTS":
                                    iconResId = R.drawable.ic_boots;
                                    break;
                            }
                        }
                        break;
                    case WEAPON:
                        if (equipment.getSubType() != null) {
                            switch (equipment.getSubType()) {
                                case "SWORD":
                                    iconResId = R.drawable.ic_sword;
                                    break;
                                case "BOW_ARROW":
                                    iconResId = R.drawable.ic_bow;
                                    break;
                            }
                        }
                        break;
                }
            }

            ivEquipmentIcon.setImageResource(iconResId);
        }
    }
}
