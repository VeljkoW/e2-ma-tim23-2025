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
                } else {
                    Toast.makeText(AllianceChatActivity.this,
                            "Failed to send message", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
