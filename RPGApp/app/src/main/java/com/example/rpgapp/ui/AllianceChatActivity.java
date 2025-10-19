package com.example.rpgapp.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rpgapp.R;
import com.example.rpgapp.adapter.AllianceMessageAdapter;
import com.example.rpgapp.callback.AuthCallback;
import com.example.rpgapp.model.AllianceMessage;
import com.example.rpgapp.model.User;
import com.example.rpgapp.repository.AllianceRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class AllianceChatActivity extends AppCompatActivity {

    private ImageButton btnBack, btnSend;
    private TextView tvAllianceName, tvEmptyMessages;
    private EditText etMessage;
    private RecyclerView rvMessages;
    private ProgressBar progressBar;

    private AllianceRepository allianceRepository;
    private AllianceMessageAdapter messageAdapter;
    private String allianceId;
    private String allianceName;
    private String currentUserId;
    private String currentUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alliance_chat);

        allianceRepository = new AllianceRepository();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        allianceId = getIntent().getStringExtra("ALLIANCE_ID");
        allianceName = getIntent().getStringExtra("ALLIANCE_NAME");

        initViews();
        loadCurrentUser();
        setupRecyclerView();
        loadMessages();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnSend = findViewById(R.id.btnSend);
        tvAllianceName = findViewById(R.id.tvAllianceName);
        tvEmptyMessages = findViewById(R.id.tvEmptyMessages);
        etMessage = findViewById(R.id.etMessage);
        rvMessages = findViewById(R.id.rvMessages);
        progressBar = findViewById(R.id.progressBar);

        tvAllianceName.setText(allianceName);
        btnBack.setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void loadCurrentUser() {
        FirebaseFirestore.getInstance()
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
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // Start from bottom
        rvMessages.setLayoutManager(layoutManager);
        messageAdapter = new AllianceMessageAdapter(currentUserId);
        rvMessages.setAdapter(messageAdapter);
    }

    private void loadMessages() {
        showLoading(true);
        allianceRepository.getAllianceMessages(allianceId, new AuthCallback<List<AllianceMessage>>() {
            @Override
            public void onResult(List<AllianceMessage> messages) {
                showLoading(false);
                messageAdapter.setMessages(messages);

                if (messages.isEmpty()) {
                    tvEmptyMessages.setVisibility(View.VISIBLE);
                } else {
                    tvEmptyMessages.setVisibility(View.GONE);
                    rvMessages.scrollToPosition(messages.size() - 1);
                }
            }
        });
    }

    private void sendMessage() {
        String messageText = etMessage.getText().toString().trim();

        if (messageText.isEmpty()) {
            Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUsername == null) {
            Toast.makeText(this, "Loading user data...", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSend.setEnabled(false);

        // Create message object immediately for instant display
        AllianceMessage newMessage = new AllianceMessage(allianceId, currentUserId, currentUsername, messageText);
        newMessage.setId("temp_" + System.currentTimeMillis());

        allianceRepository.sendMessage(allianceId, currentUserId, currentUsername,
                messageText, new AuthCallback<Boolean>() {
            @Override
            public void onResult(Boolean success) {
                btnSend.setEnabled(true);
                if (success) {
                    etMessage.setText("");
                    // Reload messages to get the real message with proper ID
                    loadMessages();

                    // Damage alliance boss by 4 HP when message is successfully sent
                    damageAllianceBossFromMessage();
                } else {
                    Toast.makeText(AllianceChatActivity.this,
                            "Failed to send message", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void damageAllianceBossFromMessage() {
        android.util.Log.d("AllianceChat", "Checking for alive alliance boss in alliance: " + allianceId);

        // First check if any message was already sent today in this alliance
        checkIfMessageSentToday(() -> {
            // If no message sent today, proceed with boss damage
            android.util.Log.d("AllianceChat", "No message sent today, proceeding with boss damage");

            // Check if alliance has an alive boss and damage it by 4 HP
            FirebaseFirestore.getInstance().collection("allianceBosses")
                    .whereEqualTo("allianceId", allianceId)
                    .whereEqualTo("status", "ALIVE")
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        android.util.Log.d("AllianceChat", "Query successful, found " + querySnapshot.size() + " alive bosses");

                        if (!querySnapshot.isEmpty()) {
                            // Found an alive boss - damage it by 4 HP
                            com.google.firebase.firestore.DocumentSnapshot bossDoc = querySnapshot.getDocuments().get(0);
                            String bossId = bossDoc.getId();
                            Long currentHpLong = (Long) bossDoc.get("currentHp");
                            int currentHp = currentHpLong != null ? currentHpLong.intValue() : 0;
                            int newHp = Math.max(0, currentHp - 4);

                            android.util.Log.d("AllianceChat", "Damaging boss " + bossId + " from " + currentHp + " to " + newHp + " HP");

                            // Update boss HP
                            FirebaseFirestore.getInstance().collection("allianceBosses")
                                    .document(bossId)
                                    .update("currentHp", newHp)
                                    .addOnSuccessListener(aVoid -> {
                                        android.util.Log.d("AllianceChat", "Alliance boss damaged by chat message: " + currentHp + " -> " + newHp + " (-4 HP)");
                                        // Mark as dead if HP reaches 0
                                        if (newHp <= 0) {
                                            android.util.Log.d("AllianceChat", "Boss defeated! Marking as DEAD");
                                            FirebaseFirestore.getInstance().collection("allianceBosses")
                                                    .document(bossId)
                                                    .update("status", "DEAD", "dateOfLastDyingOrFailing", new java.util.Date())
                                                    .addOnSuccessListener(aVoid2 -> {
                                                        android.util.Log.d("AllianceChat", "Boss successfully marked as DEAD");
                                                    });
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        android.util.Log.e("AllianceChat", "Failed to damage alliance boss from chat", e);
                                    });
                        } else {
                            android.util.Log.d("AllianceChat", "No alive alliance boss found for alliance " + allianceId);
                        }
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e("AllianceChat", "Failed to check for alliance boss", e);
                    });
        });
    }

    private void checkIfMessageSentToday(Runnable onNoneFoundToday) {
        // Get start and end of today
        java.util.Calendar startOfDay = java.util.Calendar.getInstance();
        startOfDay.set(java.util.Calendar.HOUR_OF_DAY, 0);
        startOfDay.set(java.util.Calendar.MINUTE, 0);
        startOfDay.set(java.util.Calendar.SECOND, 0);
        startOfDay.set(java.util.Calendar.MILLISECOND, 0);
        long startOfDayMillis = startOfDay.getTimeInMillis();

        java.util.Calendar endOfDay = java.util.Calendar.getInstance();
        endOfDay.set(java.util.Calendar.HOUR_OF_DAY, 23);
        endOfDay.set(java.util.Calendar.MINUTE, 59);
        endOfDay.set(java.util.Calendar.SECOND, 59);
        endOfDay.set(java.util.Calendar.MILLISECOND, 999);
        long endOfDayMillis = endOfDay.getTimeInMillis();

        android.util.Log.d("AllianceChat", "Checking for messages today between " + startOfDayMillis + " and " + endOfDayMillis);

        // Query for any message sent today in this alliance
        FirebaseFirestore.getInstance().collection("allianceMessages")
                .whereEqualTo("allianceId", allianceId)
                .whereGreaterThanOrEqualTo("timestamp", startOfDayMillis)
                .whereLessThanOrEqualTo("timestamp", endOfDayMillis)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int messageCount = querySnapshot.size();
                    android.util.Log.d("AllianceChat", "Found " + messageCount + " messages sent today in alliance " + allianceId);

                    if (messageCount <= 1) { // Current message is the first (or only) one today
                        onNoneFoundToday.run();
                    } else {
                        android.util.Log.d("AllianceChat", "Message already sent today - no boss damage");
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("AllianceChat", "Failed to check today's messages", e);
                    // On error, don't damage boss to be safe
                });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
