package com.example.rpgapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.rpgapp.R;
import com.example.rpgapp.model.Mission;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class MissionAdapter extends RecyclerView.Adapter<MissionAdapter.MissionViewHolder> {
    private List<Mission> missions;
    private OnMissionClickListener listener;

    public interface OnMissionClickListener {
        void onMissionClick(Mission mission);
    }

    public MissionAdapter(List<Mission> missions, OnMissionClickListener listener) {
        this.missions = missions;
        this.listener = listener;
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
        holder.bind(mission, listener);
    }

    @Override
    public int getItemCount() {
        return missions.size();
    }

    static class MissionViewHolder extends RecyclerView.ViewHolder {
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

        public void bind(Mission mission, OnMissionClickListener listener) {
            textViewMissionName.setText(mission.getName());
            textViewMissionCategory.setText(mission.getCategory() != null ? mission.getCategory().toString() : "");
            textViewMissionDifficulty.setText(mission.getDifficulty() != null ? mission.getDifficulty().toString() : "");
            textViewMissionStatus.setText(mission.getStatus() != null ? mission.getStatus().toString() : "");
            textViewMissionXP.setText(mission.getTotalXP() + " XP");

            // Set category color
            if (mission.getCategory() != null) {
                viewCategoryColor.setBackgroundColor(mission.getCategory().color);
            }

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
    }
}
