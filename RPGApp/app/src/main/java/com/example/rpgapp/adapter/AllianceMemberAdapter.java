package com.example.rpgapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rpgapp.R;
import com.example.rpgapp.model.User;

import java.util.ArrayList;
import java.util.List;

public class AllianceMemberAdapter extends RecyclerView.Adapter<AllianceMemberAdapter.MemberViewHolder> {

    private List<User> members;
    private String leaderId;

    public AllianceMemberAdapter() {
        this.members = new ArrayList<>();
    }

    public void setMembers(List<User> members, String leaderId) {
        this.members = members;
        this.leaderId = leaderId;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alliance_member, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        User member = members.get(position);
        boolean isLeader = false;
        if (member.getId() != null && leaderId != null) {
            isLeader = member.getId().equals(leaderId);
        }
        holder.bind(member, isLeader);
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    static class MemberViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivAvatar;
        private TextView tvUsername, tvLevel, tvLeaderBadge;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvLevel = itemView.findViewById(R.id.tvLevel);
            tvLeaderBadge = itemView.findViewById(R.id.tvLeaderBadge);
        }

        public void bind(User member, boolean isLeader) {
            tvUsername.setText(member.getUsername());
            tvLevel.setText("Level " + member.getLevel());
            tvLeaderBadge.setVisibility(isLeader ? View.VISIBLE : View.GONE);

            // Set avatar
            setAvatarImage(member.getAvatarId());
        }

        private void setAvatarImage(String avatarId) {
            if (avatarId != null) {
                int resourceId = itemView.getContext().getResources()
                        .getIdentifier(avatarId, "drawable", itemView.getContext().getPackageName());
                if (resourceId != 0) {
                    ivAvatar.setImageResource(resourceId);
                }
            }
        }
    }
}
