package com.example.rpgapp.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.rpgapp.R;
import com.example.rpgapp.model.CalendarDay;
import com.example.rpgapp.model.Mission;

import java.util.List;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder> {
    private List<CalendarDay> calendarDays;
    private OnDayClickListener listener;

    public interface OnDayClickListener {
        void onDayClick(CalendarDay day);
    }

    public CalendarAdapter(List<CalendarDay> calendarDays, OnDayClickListener listener) {
        this.calendarDays = calendarDays;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CalendarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_day, parent, false);
        return new CalendarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CalendarViewHolder holder, int position) {
        CalendarDay day = calendarDays.get(position);
        holder.bind(day, listener);
    }

    @Override
    public int getItemCount() {
        return calendarDays.size();
    }

    static class CalendarViewHolder extends RecyclerView.ViewHolder {
        private TextView textViewDayNumber;
        private LinearLayout layoutMissions;
        private View viewEmptyDay;

        public CalendarViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewDayNumber = itemView.findViewById(R.id.textViewDayNumber);
            layoutMissions = itemView.findViewById(R.id.layoutMissions);
            viewEmptyDay = itemView.findViewById(R.id.viewEmptyDay);
        }

        public void bind(CalendarDay day, OnDayClickListener listener) {
            textViewDayNumber.setText(String.valueOf(day.getDayNumber()));
            layoutMissions.removeAllViews();

            if (day.hasMissions()) {
                viewEmptyDay.setVisibility(View.GONE);

                // Add mission indicators for each mission
                for (Mission mission : day.getMissions()) {
                    TextView missionIndicator = createMissionIndicator(itemView.getContext(), mission);
                    layoutMissions.addView(missionIndicator);
                }
            } else {
                viewEmptyDay.setVisibility(View.VISIBLE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDayClick(day);
                }
            });
        }

        private TextView createMissionIndicator(Context context, Mission mission) {
            TextView indicator = new TextView(context);

            // Set mission name (truncated if too long)
            String displayName = mission.getName();
            if (displayName.length() > 15) {
                displayName = displayName.substring(0, 12) + "...";
            }
            indicator.setText(displayName);

            // Set category color as background
            int backgroundColor = mission.getCategory() != null ?
                mission.getCategory().color : 0xFF9E9E9E;
            indicator.setBackgroundColor(backgroundColor);

            // Set text color and styling
            indicator.setTextColor(0xFFFFFFFF); // White text
            indicator.setTextSize(10f);
            indicator.setPadding(8, 4, 8, 4);

            // Set layout params
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 2, 0, 2);
            indicator.setLayoutParams(params);

            return indicator;
        }
    }
}
