package com.example.rpgapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rpgapp.R;
import com.example.rpgapp.model.Friendship;
import com.example.rpgapp.model.User;
import com.example.rpgapp.repository.FriendshipRepository;

import java.util.ArrayList;
import java.util.List;

public class UserSearchAdapter extends RecyclerView.Adapter<UserSearchAdapter.UserViewHolder> {

    private List<User> users;
    private OnUserActionListener listener;
    private String currentUserId;

    public interface OnUserActionListener {
        void onAddFriend(User user);
        void onCancelRequest(User user);
        void onAcceptRequest(User user);
        void onRejectRequest(User user);
        void onViewProfile(User user);
    }

    public UserSearchAdapter(String currentUserId, OnUserActionListener listener) {
        this.users = new ArrayList<>();
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    public void setUsers(List<User> users) {
        this.users = users;
        notifyDataSetChanged();
    }

    public void addUsers(List<User> newUsers) {
        this.users.addAll(newUsers);
        notifyDataSetChanged();
    }

    public void clearUsers() {
        this.users.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_search, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = users.get(position);
        holder.bind(user);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    class UserViewHolder extends RecyclerView.ViewHolder {
        private ImageView avatarImageView;
        private TextView usernameTextView;
        private TextView levelTextView;
        private TextView titleTextView;
        private Button actionButton;
        private LinearLayout requestButtonsLayout;
        private Button acceptButton;
        private Button rejectButton;
        private View itemView;
        private FriendshipRepository friendshipRepository;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = itemView;
            this.friendshipRepository = new FriendshipRepository();
            avatarImageView = itemView.findViewById(R.id.avatar_image);
            usernameTextView = itemView.findViewById(R.id.username_text);
            levelTextView = itemView.findViewById(R.id.level_text);
            titleTextView = itemView.findViewById(R.id.title_text);
            actionButton = itemView.findViewById(R.id.add_friend_button);
            requestButtonsLayout = itemView.findViewById(R.id.request_buttons_layout);
            acceptButton = itemView.findViewById(R.id.accept_button);
            rejectButton = itemView.findViewById(R.id.reject_button);
        }

        public void bind(User user) {
            usernameTextView.setText(user.getUsername());
            levelTextView.setText("Level " + user.getLevel());
            titleTextView.setText(user.getTitle());

            // Postavi avatar na osnovu avatarId
            setAvatarImage(user.getAvatarId());

            // Proveri status prijateljstva i postavi odgovarajuće dugme
            friendshipRepository.getFriendshipStatus(currentUserId, user.getId(), status -> {
                updateButtonForStatus(status, user);
            });

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onViewProfile(user);
                }
            });
        }

        private void updateButtonForStatus(FriendshipRepository.FriendshipStatus status, User user) {
            switch (status) {
                case NONE:
                    // Nema prijateljstva - prikaži "Add Friend"
                    actionButton.setVisibility(View.VISIBLE);
                    requestButtonsLayout.setVisibility(View.GONE);
                    actionButton.setText("Add Friend");
                    actionButton.setEnabled(true);
                    actionButton.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onAddFriend(user);
                        }
                    });
                    break;

                case REQUEST_SENT:
                    // Poslat zahtev - prikaži "Request Sent" i omogući cancel
                    actionButton.setVisibility(View.VISIBLE);
                    requestButtonsLayout.setVisibility(View.GONE);
                    actionButton.setText("Request Sent");
                    actionButton.setEnabled(true);
                    actionButton.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onCancelRequest(user);
                        }
                    });
                    break;

                case REQUEST_RECEIVED:
                    // Primljen zahtev - prikaži "Accept" i "Reject" dugmad
                    actionButton.setVisibility(View.GONE);
                    requestButtonsLayout.setVisibility(View.VISIBLE);

                    acceptButton.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onAcceptRequest(user);
                        }
                    });

                    rejectButton.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onRejectRequest(user);
                        }
                    });
                    break;

                case FRIENDS:
                    // Već prijatelji - prikaži "Friends"
                    actionButton.setVisibility(View.VISIBLE);
                    requestButtonsLayout.setVisibility(View.GONE);
                    actionButton.setText("Friends");
                    actionButton.setEnabled(false); // Disable dugme
                    break;
            }
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
