package com.example.rpgapp.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rpgapp.R;
import com.example.rpgapp.adapter.FriendRequestAdapter;
import com.example.rpgapp.adapter.FriendsAdapter;
import com.example.rpgapp.adapter.UserSearchAdapter;
import com.example.rpgapp.callback.AuthCallback;
import com.example.rpgapp.model.Friendship;
import com.example.rpgapp.model.User;
import com.example.rpgapp.repository.FriendshipRepository;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FriendsActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private RecyclerView recyclerView;
    private EditText searchEditText;
    private ProgressBar progressBar;
    private TextView emptyTextView;

    private FriendsAdapter friendsAdapter;
    private FriendRequestAdapter requestAdapter;
    private UserSearchAdapter searchAdapter;

    private FriendshipRepository friendshipRepository;
    private String currentUserId;

    private static final int TAB_FRIENDS = 0;
    private static final int TAB_REQUESTS = 1;
    private static final int TAB_SEARCH = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);

        initViews();
        initRepository();
        setupRecyclerView();
        setupTabLayout();
        setupSearch();

        loadFriends();
    }

    private void initViews() {
        tabLayout = findViewById(R.id.tab_layout);
        recyclerView = findViewById(R.id.recycler_view);
        searchEditText = findViewById(R.id.search_edit_text);
        progressBar = findViewById(R.id.progress_bar);
        emptyTextView = findViewById(R.id.empty_text_view);

        // Postavi naslov
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Friends");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void initRepository() {
        friendshipRepository = new FriendshipRepository();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Friends adapter
        friendsAdapter = new FriendsAdapter(new FriendsAdapter.OnFriendActionListener() {
            @Override
            public void onViewProfile(User friend) {
                // TODO: Otvori profil prijatelja
                Toast.makeText(FriendsActivity.this, "View profile: " + friend.getUsername(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onRemoveFriend(User friend) {
                showRemoveFriendDialog(friend);
            }

            @Override
            public void onInviteToAlliance(User friend) {
                // TODO: Pozovi prijatelja u savez
                Toast.makeText(FriendsActivity.this, "Invite to alliance: " + friend.getUsername(), Toast.LENGTH_SHORT).show();
            }
        });

        // Friend requests adapter
        requestAdapter = new FriendRequestAdapter(new FriendRequestAdapter.OnRequestActionListener() {
            @Override
            public void onAcceptRequest(Friendship friendship) {
                acceptFriendRequest(friendship);
            }

            @Override
            public void onRejectRequest(Friendship friendship) {
                rejectFriendRequest(friendship);
            }
        });

        // User search adapter
        searchAdapter = new UserSearchAdapter(currentUserId, new UserSearchAdapter.OnUserActionListener() {
            @Override
            public void onAddFriend(User user) {
                sendFriendRequest(user);
            }

            @Override
            public void onCancelRequest(User user) {
                cancelFriendRequest(user);
            }

            @Override
            public void onAcceptRequest(User user) {
                acceptFriendRequestFromSearch(user);
            }

            @Override
            public void onRejectRequest(User user) {
                rejectFriendRequestFromSearch(user);
            }

            @Override
            public void onViewProfile(User user) {
                // TODO: Otvori profil korisnika
                Toast.makeText(FriendsActivity.this, "View profile: " + user.getUsername(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupTabLayout() {
        tabLayout.addTab(tabLayout.newTab().setText("Friends"));
        tabLayout.addTab(tabLayout.newTab().setText("Requests"));
        tabLayout.addTab(tabLayout.newTab().setText("Search"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case TAB_FRIENDS:
                        showFriendsTab();
                        break;
                    case TAB_REQUESTS:
                        showRequestsTab();
                        break;
                    case TAB_SEARCH:
                        showSearchTab();
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (tabLayout.getSelectedTabPosition() == TAB_SEARCH) {
                    searchUsers(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void showFriendsTab() {
        searchEditText.setVisibility(View.GONE);
        recyclerView.setAdapter(friendsAdapter);
        loadFriends();
    }

    private void showRequestsTab() {
        searchEditText.setVisibility(View.GONE);
        recyclerView.setAdapter(requestAdapter);
        loadFriendRequests();
    }

    private void showSearchTab() {
        searchEditText.setVisibility(View.VISIBLE);
        recyclerView.setAdapter(searchAdapter);
        searchAdapter.clearUsers();
        emptyTextView.setText("Search for users by username");
        emptyTextView.setVisibility(View.VISIBLE);
    }

    private void loadFriends() {
        showLoading(true);
        friendshipRepository.getFriends(currentUserId, new AuthCallback<List<String>>() {
            @Override
            public void onResult(List<String> friendIds) {
                if (friendIds.isEmpty()) {
                    showLoading(false);
                    friendsAdapter.clearFriends();
                    emptyTextView.setText("No friends yet. Search for users to add!");
                    emptyTextView.setVisibility(View.VISIBLE);
                    return;
                }

                friendshipRepository.getUsersByIds(friendIds, new AuthCallback<List<User>>() {
                    @Override
                    public void onResult(List<User> friends) {
                        showLoading(false);
                        friendsAdapter.setFriends(friends);

                        if (friends.isEmpty()) {
                            emptyTextView.setText("No friends yet. Search for users to add!");
                            emptyTextView.setVisibility(View.VISIBLE);
                        } else {
                            emptyTextView.setVisibility(View.GONE);
                        }
                    }
                });
            }
        });
    }

    private void loadFriendRequests() {
        showLoading(true);
        friendshipRepository.getPendingFriendRequests(currentUserId, new AuthCallback<List<Friendship>>() {
            @Override
            public void onResult(List<Friendship> requests) {
                if (requests.isEmpty()) {
                    showLoading(false);
                    requestAdapter.clearRequests();
                    emptyTextView.setText("No pending friend requests");
                    emptyTextView.setVisibility(View.VISIBLE);
                    return;
                }

                // Dobavi informacije o korisnicima koji su poslali zahteve
                Map<String, User> usersMap = new HashMap<>();
                final int[] processedCount = {0};

                for (Friendship friendship : requests) {
                    String requesterId = friendship.getRequesterId();
                    friendshipRepository.getUserById(requesterId, new AuthCallback<User>() {
                        @Override
                        public void onResult(User user) {
                            if (user != null) {
                                usersMap.put(user.getId(), user);
                            }
                            processedCount[0]++;

                            if (processedCount[0] == requests.size()) {
                                showLoading(false);
                                requestAdapter.setRequests(requests, usersMap);
                                emptyTextView.setVisibility(View.GONE);
                            }
                        }
                    });
                }
            }
        });
    }

    private void searchUsers(String query) {
        if (query.trim().isEmpty()) {
            searchAdapter.clearUsers();
            emptyTextView.setText("Search for users by username");
            emptyTextView.setVisibility(View.VISIBLE);
            return;
        }

        showLoading(true);
        friendshipRepository.searchUsersByUsername(query, currentUserId, new AuthCallback<List<User>>() {
            @Override
            public void onResult(List<User> users) {
                showLoading(false);
                searchAdapter.setUsers(users);

                if (users.isEmpty()) {
                    emptyTextView.setText("No users found");
                    emptyTextView.setVisibility(View.VISIBLE);
                } else {
                    emptyTextView.setVisibility(View.GONE);
                }
            }
        });
    }

    private void sendFriendRequest(User user) {
        showLoading(true);
        friendshipRepository.sendFriendRequest(currentUserId, user.getId(), new AuthCallback<Boolean>() {
            @Override
            public void onResult(Boolean success) {
                showLoading(false);
                if (success) {
                    Toast.makeText(FriendsActivity.this, "Friend request sent to " + user.getUsername(), Toast.LENGTH_SHORT).show();
                    searchAdapter.notifyDataSetChanged(); // Refresh adapter to update button
                } else {
                    Toast.makeText(FriendsActivity.this, "Failed to send friend request.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void cancelFriendRequest(User user) {
        showLoading(true);
        friendshipRepository.cancelFriendRequest(currentUserId, user.getId(), new AuthCallback<Boolean>() {
            @Override
            public void onResult(Boolean success) {
                showLoading(false);
                if (success) {
                    Toast.makeText(FriendsActivity.this, "Friend request cancelled", Toast.LENGTH_SHORT).show();
                    searchAdapter.notifyDataSetChanged(); // Refresh adapter to update button
                } else {
                    Toast.makeText(FriendsActivity.this, "Failed to cancel request", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void acceptFriendRequestFromSearch(User user) {
        showLoading(true);
        // Prvo dobavi friendship ID
        friendshipRepository.checkExistingFriendship(currentUserId, user.getId(), new AuthCallback<Friendship>() {
            @Override
            public void onResult(Friendship friendship) {
                if (friendship != null && friendship.isPending()) {
                    friendshipRepository.acceptFriendRequest(friendship.getId(), new AuthCallback<Boolean>() {
                        @Override
                        public void onResult(Boolean success) {
                            showLoading(false);
                            if (success) {
                                Toast.makeText(FriendsActivity.this, "You are now friends with " + user.getUsername(), Toast.LENGTH_SHORT).show();
                                searchAdapter.notifyDataSetChanged(); // Refresh adapter to update button
                            } else {
                                Toast.makeText(FriendsActivity.this, "Failed to accept request", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } else {
                    showLoading(false);
                    Toast.makeText(FriendsActivity.this, "Request not found", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void rejectFriendRequestFromSearch(User user) {
        showLoading(true);
        // Prvo dobavi friendship ID
        friendshipRepository.checkExistingFriendship(currentUserId, user.getId(), new AuthCallback<Friendship>() {
            @Override
            public void onResult(Friendship friendship) {
                if (friendship != null && friendship.isPending()) {
                    friendshipRepository.rejectFriendRequest(friendship.getId(), new AuthCallback<Boolean>() {
                        @Override
                        public void onResult(Boolean success) {
                            showLoading(false);
                            if (success) {
                                Toast.makeText(FriendsActivity.this, "Friend request rejected", Toast.LENGTH_SHORT).show();
                                searchAdapter.notifyDataSetChanged(); // Refresh adapter to update button
                            } else {
                                Toast.makeText(FriendsActivity.this, "Failed to reject request", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } else {
                    showLoading(false);
                    Toast.makeText(FriendsActivity.this, "Request not found", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void acceptFriendRequest(Friendship friendship) {
        showLoading(true);
        friendshipRepository.acceptFriendRequest(friendship.getId(), new AuthCallback<Boolean>() {
            @Override
            public void onResult(Boolean success) {
                showLoading(false);
                if (success) {
                    Toast.makeText(FriendsActivity.this, "Friend request accepted", Toast.LENGTH_SHORT).show();
                    requestAdapter.removeRequest(friendship);
                    loadFriendRequests(); // Reload requests
                } else {
                    Toast.makeText(FriendsActivity.this, "Failed to accept request", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void rejectFriendRequest(Friendship friendship) {
        showLoading(true);
        friendshipRepository.rejectFriendRequest(friendship.getId(), new AuthCallback<Boolean>() {
            @Override
            public void onResult(Boolean success) {
                showLoading(false);
                if (success) {
                    Toast.makeText(FriendsActivity.this, "Friend request rejected", Toast.LENGTH_SHORT).show();
                    requestAdapter.removeRequest(friendship);
                    loadFriendRequests(); // Reload requests
                } else {
                    Toast.makeText(FriendsActivity.this, "Failed to reject request", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showRemoveFriendDialog(User friend) {
        new AlertDialog.Builder(this)
                .setTitle("Remove Friend")
                .setMessage("Are you sure you want to remove " + friend.getUsername() + " from your friends?")
                .setPositiveButton("Remove", (dialog, which) -> removeFriend(friend))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void removeFriend(User friend) {
        showLoading(true);
        friendshipRepository.removeFriend(currentUserId, friend.getId(), new AuthCallback<Boolean>() {
            @Override
            public void onResult(Boolean success) {
                showLoading(false);
                if (success) {
                    Toast.makeText(FriendsActivity.this, friend.getUsername() + " removed from friends", Toast.LENGTH_SHORT).show();
                    friendsAdapter.removeFriend(friend);
                    loadFriends(); // Reload friends
                } else {
                    Toast.makeText(FriendsActivity.this, "Failed to remove friend", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
