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
import com.example.rpgapp.model.Friendship;
import com.example.rpgapp.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FriendRequestAdapter extends RecyclerView.Adapter<FriendRequestAdapter.RequestViewHolder> {

    private List<Friendship> friendRequests;
    private Map<String, User> usersMap; // Map za brzi pristup user objektima
    private OnRequestActionListener listener;

    public interface OnRequestActionListener {
        void onAcceptRequest(Friendship friendship);
        void onRejectRequest(Friendship friendship);
    }

    public FriendRequestAdapter(OnRequestActionListener listener) {
        this.friendRequests = new ArrayList<>();
        this.usersMap = new HashMap<>();
        this.listener = listener;
    }

    public void setRequests(List<Friendship> requests, Map<String, User> usersMap) {
        this.friendRequests = requests;
        this.usersMap = usersMap;
        notifyDataSetChanged();
    }

    public void removeRequest(Friendship friendship) {
        this.friendRequests.remove(friendship);
        notifyDataSetChanged();
    }

    public void clearRequests() {
        this.friendRequests.clear();
        this.usersMap.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend_request, parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        Friendship friendship = friendRequests.get(position);
        User requester = usersMap.get(friendship.getRequesterId());
        holder.bind(friendship, requester);
    }

    @Override
    public int getItemCount() {
        return friendRequests.size();
    }

    class RequestViewHolder extends RecyclerView.ViewHolder {
        private ImageView avatarImageView;
        private TextView usernameTextView;
        private TextView levelTextView;
        private TextView titleTextView;
        private Button acceptButton;
        private Button rejectButton;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarImageView = itemView.findViewById(R.id.avatar_image);
            usernameTextView = itemView.findViewById(R.id.username_text);
            levelTextView = itemView.findViewById(R.id.level_text);
            titleTextView = itemView.findViewById(R.id.title_text);
            acceptButton = itemView.findViewById(R.id.accept_button);
            rejectButton = itemView.findViewById(R.id.reject_button);
        }

        public void bind(Friendship friendship, User requester) {
            if (requester != null) {
                usernameTextView.setText(requester.getUsername());
                levelTextView.setText("Level " + requester.getLevel());
                titleTextView.setText(requester.getTitle());
                setAvatarImage(requester.getAvatarId());
            } else {
                usernameTextView.setText("Unknown User");
                levelTextView.setText("");
                titleTextView.setText("");
            }

            acceptButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAcceptRequest(friendship);
                }
            });

            rejectButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRejectRequest(friendship);
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
