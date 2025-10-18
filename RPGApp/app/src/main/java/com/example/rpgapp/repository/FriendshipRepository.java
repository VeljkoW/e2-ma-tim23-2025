package com.example.rpgapp.repository;

import android.util.Log;
import com.example.rpgapp.callback.AuthCallback;
import com.example.rpgapp.model.Friendship;
import com.example.rpgapp.model.User;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FriendshipRepository {
    private static final String TAG = "FriendshipRepository";
    private static final String COLLECTION_FRIENDSHIPS = "friendships";
    private static final String COLLECTION_USERS = "users";
    private FirebaseFirestore db;

    public FriendshipRepository() {
        db = FirebaseFirestore.getInstance();
    }

    /**
     * Kreira normalizovan friendship ID od dva user ID-a
     * Uvek stavlja manji ID prvi da bi se izbeglo dupliciranje
     */
    private String createFriendshipId(String userId1, String userId2) {
        if (userId1.compareTo(userId2) < 0) {
            return userId1 + "_" + userId2;
        } else {
            return userId2 + "_" + userId1;
        }
    }

    /**
     * Šalje zahtev za prijateljstvo
     */
    public void sendFriendRequest(String fromUserId, String toUserId, AuthCallback<Boolean> callback) {
        Log.d(TAG, "Sending friend request from " + fromUserId + " to " + toUserId);

        // Proverava da li već postoji prijateljstvo
        checkExistingFriendship(fromUserId, toUserId, new AuthCallback<Friendship>() {
            @Override
            public void onResult(Friendship existingFriendship) {
                if (existingFriendship != null) {
                    Log.d(TAG, "Friendship already exists with status: " + existingFriendship.getStatus());
                    callback.onResult(false);
                    return;
                }

                // Kreira novi friendship
                String friendshipId = createFriendshipId(fromUserId, toUserId);

                Map<String, Object> friendshipData = new HashMap<>();
                friendshipData.put("userId1", fromUserId.compareTo(toUserId) < 0 ? fromUserId : toUserId);
                friendshipData.put("userId2", fromUserId.compareTo(toUserId) < 0 ? toUserId : fromUserId);
                friendshipData.put("requesterId", fromUserId);
                friendshipData.put("status", Friendship.FriendshipStatus.PENDING.name());
                friendshipData.put("requestDate", new Date());

                db.collection(COLLECTION_FRIENDSHIPS)
                    .document(friendshipId)
                    .set(friendshipData)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Friend request sent successfully");
                        callback.onResult(true);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to send friend request", e);
                        callback.onResult(false);
                    });
            }
        });
    }

    /**
     * Proverava da li postoji prijateljstvo između dva korisnika
     */
    public void checkExistingFriendship(String userId1, String userId2, AuthCallback<Friendship> callback) {
        String friendshipId = createFriendshipId(userId1, userId2);

        db.collection(COLLECTION_FRIENDSHIPS)
            .document(friendshipId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Friendship friendship = documentSnapshot.toObject(Friendship.class);
                    if (friendship != null) {
                        friendship.setId(documentSnapshot.getId());
                    }
                    callback.onResult(friendship);
                } else {
                    callback.onResult(null);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error checking existing friendship", e);
                callback.onResult(null);
            });
    }

    /**
     * Prihvata zahtev za prijateljstvo
     */
    public void acceptFriendRequest(String friendshipId, AuthCallback<Boolean> callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", Friendship.FriendshipStatus.ACCEPTED.name());
        updates.put("acceptedDate", new Date());

        db.collection(COLLECTION_FRIENDSHIPS)
            .document(friendshipId)
            .update(updates)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Friend request accepted");
                callback.onResult(true);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to accept friend request", e);
                callback.onResult(false);
            });
    }

    /**
     * Odbija zahtev za prijateljstvo
     */
    public void rejectFriendRequest(String friendshipId, AuthCallback<Boolean> callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", Friendship.FriendshipStatus.REJECTED.name());

        db.collection(COLLECTION_FRIENDSHIPS)
            .document(friendshipId)
            .update(updates)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Friend request rejected");
                callback.onResult(true);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to reject friend request", e);
                callback.onResult(false);
            });
    }

    /**
     * Dobavlja sve prihvaćene prijatelje za korisnika
     */
    public void getFriends(String userId, AuthCallback<List<String>> callback) {
        List<String> friendIds = new ArrayList<>();

        // Query gde je korisnik userId1
        db.collection(COLLECTION_FRIENDSHIPS)
            .whereEqualTo("userId1", userId)
            .whereEqualTo("status", Friendship.FriendshipStatus.ACCEPTED.name())
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    Friendship friendship = doc.toObject(Friendship.class);
                    friendIds.add(friendship.getUserId2());
                }

                // Query gde je korisnik userId2
                db.collection(COLLECTION_FRIENDSHIPS)
                    .whereEqualTo("userId2", userId)
                    .whereEqualTo("status", Friendship.FriendshipStatus.ACCEPTED.name())
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots2 -> {
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots2) {
                            Friendship friendship = doc.toObject(Friendship.class);
                            friendIds.add(friendship.getUserId1());
                        }
                        callback.onResult(friendIds);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error getting friends (userId2)", e);
                        callback.onResult(friendIds);
                    });
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error getting friends (userId1)", e);
                callback.onResult(new ArrayList<>());
            });
    }

    /**
     * Dobavlja pending zahteve za prijateljstvo koje je korisnik primio
     */
    public void getPendingFriendRequests(String userId, AuthCallback<List<Friendship>> callback) {
        List<Friendship> pendingRequests = new ArrayList<>();

        // Query gde je korisnik userId1 i nije requester
        db.collection(COLLECTION_FRIENDSHIPS)
            .whereEqualTo("userId1", userId)
            .whereEqualTo("status", Friendship.FriendshipStatus.PENDING.name())
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    Friendship friendship = doc.toObject(Friendship.class);
                    friendship.setId(doc.getId());
                    // Dodaj samo ako trenutni korisnik nije poslao zahtev
                    if (!friendship.getRequesterId().equals(userId)) {
                        pendingRequests.add(friendship);
                    }
                }

                // Query gde je korisnik userId2 i nije requester
                db.collection(COLLECTION_FRIENDSHIPS)
                    .whereEqualTo("userId2", userId)
                    .whereEqualTo("status", Friendship.FriendshipStatus.PENDING.name())
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots2 -> {
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots2) {
                            Friendship friendship = doc.toObject(Friendship.class);
                            friendship.setId(doc.getId());
                            // Dodaj samo ako trenutni korisnik nije poslao zahtev
                            if (!friendship.getRequesterId().equals(userId)) {
                                pendingRequests.add(friendship);
                            }
                        }
                        callback.onResult(pendingRequests);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error getting pending requests (userId2)", e);
                        callback.onResult(pendingRequests);
                    });
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error getting pending requests (userId1)", e);
                callback.onResult(new ArrayList<>());
            });
    }

    /**
     * Dobavlja sve pending zahteve koje je korisnik poslao
     */
    public void getSentFriendRequests(String userId, AuthCallback<List<Friendship>> callback) {
        db.collection(COLLECTION_FRIENDSHIPS)
            .whereEqualTo("requesterId", userId)
            .whereEqualTo("status", Friendship.FriendshipStatus.PENDING.name())
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<Friendship> sentRequests = new ArrayList<>();
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    Friendship friendship = doc.toObject(Friendship.class);
                    friendship.setId(doc.getId());
                    sentRequests.add(friendship);
                }
                callback.onResult(sentRequests);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error getting sent requests", e);
                callback.onResult(new ArrayList<>());
            });
    }

    /**
     * Uklanja prijateljstvo (unfriend)
     */
    public void removeFriend(String userId1, String userId2, AuthCallback<Boolean> callback) {
        String friendshipId = createFriendshipId(userId1, userId2);

        db.collection(COLLECTION_FRIENDSHIPS)
            .document(friendshipId)
            .delete()
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Friendship removed successfully");
                callback.onResult(true);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to remove friendship", e);
                callback.onResult(false);
            });
    }

    /**
     * Pretražuje korisnike po username-u - prikazuje samo aktivne korisnike
     */
    public void searchUsersByUsername(String searchQuery, String currentUserId, AuthCallback<List<User>> callback) {
        if (searchQuery == null || searchQuery.trim().isEmpty()) {
            callback.onResult(new ArrayList<>());
            return;
        }

        String searchLower = searchQuery.toLowerCase();
        String searchUpper = searchQuery.toUpperCase();

        db.collection(COLLECTION_USERS)
            .orderBy("username")
            .startAt(searchQuery)
            .endAt(searchQuery + "\uf8ff")
            .limit(20)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<User> users = new ArrayList<>();
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    User user = doc.toObject(User.class);
                    user.setId(doc.getId());
                    // Prikazuj samo aktivne korisnike i ne prikazuj trenutnog korisnika
                    if (!user.getId().equals(currentUserId) && user.isActive()) {
                        users.add(user);
                    }
                }
                callback.onResult(users);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error searching users", e);
                callback.onResult(new ArrayList<>());
            });
    }

    /**
     * Dobavlja korisnika po ID-u
     */
    public void getUserById(String userId, AuthCallback<User> callback) {
        db.collection(COLLECTION_USERS)
            .document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null) {
                        user.setId(documentSnapshot.getId());
                    }
                    callback.onResult(user);
                } else {
                    callback.onResult(null);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error getting user by ID", e);
                callback.onResult(null);
            });
    }

    /**
     * Dobavlja status prijateljstva između dva korisnika
     */
    public void getFriendshipStatus(String userId1, String userId2, AuthCallback<FriendshipStatus> callback) {
        String friendshipId = createFriendshipId(userId1, userId2);

        db.collection(COLLECTION_FRIENDSHIPS)
            .document(friendshipId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Friendship friendship = documentSnapshot.toObject(Friendship.class);
                    if (friendship != null) {
                        friendship.setId(documentSnapshot.getId());

                        // Odredi status na osnovu prijateljstva
                        if (friendship.getStatus() == Friendship.FriendshipStatus.ACCEPTED) {
                            callback.onResult(FriendshipStatus.FRIENDS);
                        } else if (friendship.getStatus() == Friendship.FriendshipStatus.PENDING) {
                            // Proveri ko je poslao zahtev
                            if (friendship.getRequesterId().equals(userId1)) {
                                callback.onResult(FriendshipStatus.REQUEST_SENT);
                            } else {
                                callback.onResult(FriendshipStatus.REQUEST_RECEIVED);
                            }
                        } else {
                            callback.onResult(FriendshipStatus.NONE);
                        }
                    } else {
                        callback.onResult(FriendshipStatus.NONE);
                    }
                } else {
                    callback.onResult(FriendshipStatus.NONE);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error getting friendship status", e);
                callback.onResult(FriendshipStatus.NONE);
            });
    }

    /**
     * Enum za status prijateljstva
     */
    public enum FriendshipStatus {
        NONE,               // Nema prijateljstva
        REQUEST_SENT,       // Trenutni korisnik je poslao zahtev
        REQUEST_RECEIVED,   // Trenutni korisnik je primio zahtev
        FRIENDS             // Već prijatelji
    }

    /**
     * Uklanja zahtev za prijateljstvo (cancel sent request)
     */
    public void cancelFriendRequest(String fromUserId, String toUserId, AuthCallback<Boolean> callback) {
        String friendshipId = createFriendshipId(fromUserId, toUserId);

        db.collection(COLLECTION_FRIENDSHIPS)
            .document(friendshipId)
            .delete()
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Friend request cancelled successfully");
                callback.onResult(true);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to cancel friend request", e);
                callback.onResult(false);
            });
    }

    /**
     * Dobavlja više korisnika po njihovim ID-ima
     */
    public void getUsersByIds(List<String> userIds, AuthCallback<List<User>> callback) {
        if (userIds == null || userIds.isEmpty()) {
            callback.onResult(new ArrayList<>());
            return;
        }

        List<User> users = new ArrayList<>();
        final int[] processedCount = {0};

        for (String userId : userIds) {
            getUserById(userId, new AuthCallback<User>() {
                @Override
                public void onResult(User user) {
                    if (user != null) {
                        users.add(user);
                    }
                    processedCount[0]++;

                    if (processedCount[0] == userIds.size()) {
                        callback.onResult(users);
                    }
                }
            });
        }
    }
}
