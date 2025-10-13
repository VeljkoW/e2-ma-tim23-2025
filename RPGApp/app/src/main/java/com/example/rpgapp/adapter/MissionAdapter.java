package com.example.rpgapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.rpgapp.R;
import com.example.rpgapp.model.Mission;
import com.example.rpgapp.model.Category;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class MissionAdapter extends RecyclerView.Adapter<MissionAdapter.MissionViewHolder> {
    private List<Mission> missions;
    private OnMissionClickListener listener;
    private Map<String, Category> categoryCache;

    public interface OnMissionClickListener {
        void onMissionClick(Mission mission);
    }

    public MissionAdapter(List<Mission> missions, OnMissionClickListener listener) {
        this.missions = missions;
        this.listener = listener;
        this.categoryCache = new HashMap<>();
    }

    // Method to set categories for caching (call this from the activity/fragment)
    public void setCategoryCache(List<Category> categories) {
        categoryCache.clear();
        for (Category category : categories) {
            if (category.getId() != null) {
                categoryCache.put(category.getId(), category);
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MissionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mission, parent, false);
        return new MissionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MissionViewHolder holder, int position) {
        Mission mission = missions.get(position);
        holder.bind(mission, listener, this);
    }

    @Override
    public int getItemCount() {
        return missions.size();
    }

    class MissionViewHolder extends RecyclerView.ViewHolder {
        private TextView textViewMissionName;
        private TextView textViewMissionCategory;
        private TextView textViewMissionDifficulty;
        private TextView textViewMissionStatus;
        private TextView textViewMissionXP;
        private View viewCategoryColor;

        public MissionViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewMissionName = itemView.findViewById(R.id.textViewMissionName);
            textViewMissionCategory = itemView.findViewById(R.id.textViewMissionCategory);
            textViewMissionDifficulty = itemView.findViewById(R.id.textViewMissionDifficulty);
            textViewMissionStatus = itemView.findViewById(R.id.textViewMissionStatus);
            textViewMissionXP = itemView.findViewById(R.id.textViewMissionXP);
            viewCategoryColor = itemView.findViewById(R.id.viewCategoryColor);
        }

        public void bind(Mission mission, OnMissionClickListener listener, MissionAdapter adapter) {
            textViewMissionName.setText(mission.getName());

            // Get category name from cache using categoryId
            String categoryName = getCategoryName(mission.getCategoryId(), adapter);
            textViewMissionCategory.setText(categoryName);

            textViewMissionDifficulty.setText(mission.getDifficulty() != null ? mission.getDifficulty().toString() : "");
            textViewMissionStatus.setText(mission.getStatus() != null ? mission.getStatus().toString() : "");
            textViewMissionXP.setText(mission.getTotalXP() + " XP");

            // Set category color using categoryId
            int categoryColor = getCategoryColor(mission.getCategoryId(), adapter);
            viewCategoryColor.setBackgroundColor(categoryColor);

            // Set status background color
            if (mission.getStatus() != null) {
                int statusColor;
                switch (mission.getStatus()) {
                    case ACTIVE:
                        statusColor = 0xFF4CAF50; // Green
                        break;
                    case COMPLETED:
                        statusColor = 0xFF2196F3; // Blue
                        break;
                    case FAILED:
                        statusColor = 0xFFF44336; // Red
                        break;
                    case CANCELLED:
                        statusColor = 0xFF9E9E9E; // Gray
                        break;
                    case PAUSED:
                        statusColor = 0xFFFFC107; // Amber
                        break;
                    default:
                        statusColor = 0xFF9E9E9E; // Gray
                        break;
                }
                textViewMissionStatus.setBackgroundColor(statusColor);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMissionClick(mission);
                }
            });
        }

        private String getCategoryName(String categoryId, MissionAdapter adapter) {
            if (categoryId != null && adapter.categoryCache.containsKey(categoryId)) {
                Category category = adapter.categoryCache.get(categoryId);
                return category.getName();
            }
            return "No Category"; // Default text if category not found
        }

        private int getCategoryColor(String categoryId, MissionAdapter adapter) {
            if (categoryId != null && adapter.categoryCache.containsKey(categoryId)) {
                Category category = adapter.categoryCache.get(categoryId);
                return category.getColor();
            }
            return 0xFF9E9E9E; // Default grey color if category not found
        }
    }
}
