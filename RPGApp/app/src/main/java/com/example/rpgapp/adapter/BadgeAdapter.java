package com.example.rpgapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rpgapp.R;
import com.example.rpgapp.model.Badge;

import java.util.ArrayList;
import java.util.List;

public class BadgeAdapter extends RecyclerView.Adapter<BadgeAdapter.BadgeViewHolder> {

    private List<Badge> badges;
    private final OnBadgeClickListener listener;

    public interface OnBadgeClickListener {
        void onBadgeClick(Badge badge);
    }

    public BadgeAdapter(OnBadgeClickListener listener) {
        this.badges = new ArrayList<>();
        this.listener = listener;
    }

    public void setBadges(List<Badge> badges) {
        this.badges = badges != null ? badges : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BadgeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_badge, parent, false);
        return new BadgeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BadgeViewHolder holder, int position) {
        Badge badge = badges.get(position);
        holder.bind(badge);
    }

    @Override
    public int getItemCount() {
        return badges.size();
    }

    class BadgeViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivBadgeIcon;
        private final TextView tvBadgeName;

        public BadgeViewHolder(@NonNull View itemView) {
            super(itemView);
            ivBadgeIcon = itemView.findViewById(R.id.ivBadgeIcon);
            tvBadgeName = itemView.findViewById(R.id.tvBadgeName);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onBadgeClick(badges.get(position));
                }
            });
        }

        public void bind(Badge badge) {
            if (badge != null) {
                tvBadgeName.setText(badge.getName());

                // Use default badge icon - URL loading can be implemented later with Glide/Picasso
                ivBadgeIcon.setImageResource(R.drawable.ic_badge);
            }
        }
    }
}
