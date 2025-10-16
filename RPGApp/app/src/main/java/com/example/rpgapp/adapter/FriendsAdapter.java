package com.example.rpgapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rpgapp.R;
import com.example.rpgapp.model.User;

import java.util.ArrayList;
import java.util.List;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.FriendViewHolder>
{

    private List<User> friends;
    private OnFriendActionListener listener;

    public interface OnFriendActionListener {
        void onViewProfile(User friend);
        void onRemoveFriend(User friend);
        void onInviteToAlliance(User friend);
    }

    public FriendsAdapter(OnFriendActionListener listener) {
        this.friends = new ArrayList<>();
        this.listener = listener;
    }

    public void setFriends(List<User> friends) {
        this.friends = friends;
        notifyDataSetChanged();
    }

    public void addFriend(User friend) {
        this.friends.add(friend);
        notifyDataSetChanged();
    }

    public void removeFriend(User friend) {
        this.friends.remove(friend);
        notifyDataSetChanged();
    }

    public void clearFriends() {
        this.friends.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend, parent, false);
        return new FriendViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
        User friend = friends.get(position);
        holder.bind(friend);
    }

    @Override
    public int getItemCount() {
        return friends.size();
    }

    class FriendViewHolder extends RecyclerView.ViewHolder {
        private ImageView avatarImageView;
        private TextView usernameTextView;
        private TextView levelTextView;
        private TextView titleTextView;
        private Button inviteButton;
        private Button removeButton;
        private View itemView;

        public FriendViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = itemView;
            avatarImageView = itemView.findViewById(R.id.avatar_image);
            usernameTextView = itemView.findViewById(R.id.username_text);
            levelTextView = itemView.findViewById(R.id.level_text);
            titleTextView = itemView.findViewById(R.id.title_text);
            inviteButton = itemView.findViewById(R.id.invite_button);
            removeButton = itemView.findViewById(R.id.remove_button);
        }

        public void bind(User friend) {
            usernameTextView.setText(friend.getUsername());
            levelTextView.setText("Level " + friend.getLevel());
            titleTextView.setText(friend.getTitle());

            // Postavi avatar
            setAvatarImage(friend.getAvatarId());

            inviteButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onInviteToAlliance(friend);
                }
            });

            removeButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRemoveFriend(friend);
                }
            });

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onViewProfile(friend);
                }
            });
        }

        private void setAvatarImage(String avatarId) {
            if (avatarId != null) {
                switch (avatarId) {
                    case "avatar1":
                        avatarImageView.setImageResource(R.drawable.avatar_1);
                        break;
                    case "avatar2":
                        avatarImageView.setImageResource(R.drawable.avatar_2);
                        break;
                    case "avatar3":
                        avatarImageView.setImageResource(R.drawable.avatar_3);
                        break;
                    case "avatar4":
                        avatarImageView.setImageResource(R.drawable.avatar_4);
                        break;
                    case "avatar5":
                        avatarImageView.setImageResource(R.drawable.avatar_5);
                        break;
                    default:
                        avatarImageView.setImageResource(R.drawable.avatar_1);
                        break;
                }
            }
        }
    }
}
