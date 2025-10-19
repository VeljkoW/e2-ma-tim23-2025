package com.example.rpgapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
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
import com.example.rpgapp.model.Alliance;
import com.example.rpgapp.model.AllianceInvitation;
import com.example.rpgapp.model.Friendship;
import com.example.rpgapp.model.User;
import com.example.rpgapp.repository.AllianceRepository;
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
    private ImageButton btnBack;

    private FriendsAdapter friendsAdapter;
    private FriendRequestAdapter requestAdapter;
    private UserSearchAdapter searchAdapter;

    private FriendshipRepository friendshipRepository;
    private AllianceRepository allianceRepository;
    private String currentUserId;
    private String currentUsername;

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

        // Set initial tab and load data
        showFriendsTab();
    }

    private void initViews() {
        tabLayout = findViewById(R.id.tab_layout);
        recyclerView = findViewById(R.id.recycler_view);
        searchEditText = findViewById(R.id.search_edit_text);
        progressBar = findViewById(R.id.progress_bar);
        emptyTextView = findViewById(R.id.empty_text_view);
        btnBack = findViewById(R.id.btnBack);
        ImageButton btnScanQR = findViewById(R.id.btnScanQR);

        // Setup back button
        btnBack.setOnClickListener(v -> finish());

        // Setup QR scan button
        btnScanQR.setOnClickListener(v -> {
            Intent intent = new Intent(FriendsActivity.this, QRScannerActivity.class);
            startActivity(intent);
        });

        // Postavi naslov
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Friends");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void initRepository() {
        friendshipRepository = new FriendshipRepository();
        allianceRepository = new AllianceRepository();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Load current username
        loadCurrentUsername();
    }

    private void loadCurrentUsername() {
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null) {
                        currentUsername = user.getUsername();
                    }
                });
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Friends adapter
        friendsAdapter = new FriendsAdapter(new FriendsAdapter.OnFriendActionListener() {
            @Override
            public void onViewProfile(User friend) {
                // Navigate to profile activity
                Intent intent = new Intent(FriendsActivity.this, ProfileActivity.class);
                intent.putExtra(ProfileActivity.EXTRA_USER_ID, friend.getId());
                startActivity(intent);
            }

            @Override
            public void onRemoveFriend(User friend) {
                showRemoveFriendDialog(friend);
            }

            @Override
            public void onInviteToAlliance(User friend) {
                inviteFriendToAlliance(friend);
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

            @Override
            public void onViewProfile(User user) {
                // Navigate to profile activity
                Intent intent = new Intent(FriendsActivity.this, ProfileActivity.class);
                intent.putExtra(ProfileActivity.EXTRA_USER_ID, user.getId());
                startActivity(intent);
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
                // Navigate to profile activity
                Intent intent = new Intent(FriendsActivity.this, ProfileActivity.class);
                intent.putExtra(ProfileActivity.EXTRA_USER_ID, user.getId());
                startActivity(intent);
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
                        friendsAdapter.setFriends(friends);

                        // Load invitation status for each friend
                        loadInvitationStatuses(friends);

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

    private void loadInvitationStatuses(List<User> friends) {
        // First check if user has an alliance
        allianceRepository.getUserAlliance(currentUserId, new AuthCallback<Alliance>() {
            @Override
            public void onResult(Alliance alliance) {
                showLoading(false);
                if (alliance == null) {
                    return;
                }

                // Check invitation status and alliance membership for each friend
                Map<String, Boolean> invitedMap = new HashMap<>();
                Map<String, Boolean> inAllianceMap = new HashMap<>();
                final int[] processedCount = {0};

                for (User friend : friends) {
                    // Check if friend is already in the same alliance
                    boolean isInSameAlliance = alliance.getMemberIds().contains(friend.getId());
                    inAllianceMap.put(friend.getId(), isInSameAlliance);

                    // Check if there's a pending invitation
                    allianceRepository.checkPendingInvitation(alliance.getId(), friend.getId(),
                            new AuthCallback<AllianceInvitation>() {
                        @Override
                        public void onResult(AllianceInvitation invitation) {
                            invitedMap.put(friend.getId(), invitation != null);
                            processedCount[0]++;

                            if (processedCount[0] == friends.size()) {
                                friendsAdapter.setInvitedFriends(invitedMap);
                                friendsAdapter.setAllianceMembers(inAllianceMap);
                            }
                        }
                    });
                }
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

    private void inviteFriendToAlliance(User friend) {
        // First check if user has an alliance
        showLoading(true);
        allianceRepository.getUserAlliance(currentUserId, new AuthCallback<Alliance>() {
            @Override
            public void onResult(Alliance alliance) {
                if (alliance != null) {
                    // Check if invitation already exists
                    allianceRepository.checkPendingInvitation(alliance.getId(), friend.getId(),
                            new AuthCallback<AllianceInvitation>() {
                        @Override
                        public void onResult(AllianceInvitation existingInvitation) {
                            showLoading(false);
                            if (existingInvitation != null) {
                                // Invitation exists, ask to cancel it
                                showCancelInvitationDialog(existingInvitation, friend);
                            } else {
                                // No invitation, send new one
                                sendAllianceInvitation(alliance, friend);
                            }
                        }
                    });
                } else {
                    showLoading(false);
                    Toast.makeText(FriendsActivity.this,
                            "You must be in an alliance to invite friends", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showCancelInvitationDialog(AllianceInvitation invitation, User friend) {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Invitation")
                .setMessage("You already invited " + friend.getUsername() + " to your alliance. Do you want to cancel the invitation?")
                .setPositiveButton("Cancel Invitation", (dialog, which) -> cancelInvitation(invitation, friend))
                .setNegativeButton("Keep Invitation", null)
                .show();
    }

    private void cancelInvitation(AllianceInvitation invitation, User friend) {
        showLoading(true);
        allianceRepository.cancelInvitation(invitation.getId(), new AuthCallback<Boolean>() {
            @Override
            public void onResult(Boolean success) {
                showLoading(false);
                if (success) {
                    Toast.makeText(FriendsActivity.this,
                            "Invitation to " + friend.getUsername() + " cancelled", Toast.LENGTH_SHORT).show();
                    friendsAdapter.markAsInvited(friend.getId(), false);
                } else {
                    Toast.makeText(FriendsActivity.this,
                            "Failed to cancel invitation", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void sendAllianceInvitation(Alliance alliance, User friend) {
        if (currentUsername == null) {
            Toast.makeText(this, "Loading user data...", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);
        allianceRepository.sendInvitation(alliance.getId(), alliance.getName(),
                currentUserId, currentUsername, friend.getId(), new AuthCallback<Boolean>() {
            @Override
            public void onResult(Boolean success) {
                showLoading(false);
                if (success) {
                    Toast.makeText(FriendsActivity.this,
                            "Invitation sent to " + friend.getUsername(), Toast.LENGTH_SHORT).show();
                    friendsAdapter.markAsInvited(friend.getId(), true);
                } else {
                    Toast.makeText(FriendsActivity.this,
                            "Failed to send invitation", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
