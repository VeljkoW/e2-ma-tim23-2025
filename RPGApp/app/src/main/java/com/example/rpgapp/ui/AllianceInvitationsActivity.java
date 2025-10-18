package com.example.rpgapp.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rpgapp.R;
import com.example.rpgapp.adapter.AllianceInvitationAdapter;
import com.example.rpgapp.callback.AuthCallback;
import com.example.rpgapp.model.Alliance;
import com.example.rpgapp.model.AllianceInvitation;
import com.example.rpgapp.repository.AllianceRepository;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class AllianceInvitationsActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private RecyclerView rvInvitations;
    private ProgressBar progressBar;
    private TextView tvEmptyState;

    private AllianceRepository allianceRepository;
    private AllianceInvitationAdapter invitationAdapter;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alliance_invitations);

        allianceRepository = new AllianceRepository();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        initViews();
        setupRecyclerView();
        loadInvitations();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        rvInvitations = findViewById(R.id.rvInvitations);
        progressBar = findViewById(R.id.progressBar);
        tvEmptyState = findViewById(R.id.tvEmptyState);

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        rvInvitations.setLayoutManager(new LinearLayoutManager(this));
        invitationAdapter = new AllianceInvitationAdapter(new AllianceInvitationAdapter.OnInvitationActionListener() {
            @Override
            public void onAccept(AllianceInvitation invitation) {
                handleAcceptInvitation(invitation);
            }

            @Override
            public void onReject(AllianceInvitation invitation) {
                handleRejectInvitation(invitation);
            }
        });
        rvInvitations.setAdapter(invitationAdapter);
    }

    private void loadInvitations() {
        showLoading(true);
        allianceRepository.getPendingInvitations(currentUserId, new AuthCallback<List<AllianceInvitation>>() {
            @Override
            public void onResult(List<AllianceInvitation> invitations) {
                showLoading(false);
                invitationAdapter.setInvitations(invitations);

                if (invitations.isEmpty()) {
                    tvEmptyState.setVisibility(View.VISIBLE);
                } else {
                    tvEmptyState.setVisibility(View.GONE);
                }
            }
        });
    }

    private void handleAcceptInvitation(AllianceInvitation invitation) {
        // Check if user is already in an alliance
        showLoading(true);
        allianceRepository.getUserAlliance(currentUserId, new AuthCallback<Alliance>() {
            @Override
            public void onResult(Alliance currentAlliance) {
                if (currentAlliance != null) {
                    // User is already in an alliance, ask if they want to leave
                    showLeaveAllianceDialog(invitation, currentAlliance);
                } else {
                    // User is not in an alliance, accept directly
                    acceptInvitation(invitation, null);
                }
            }
        });
    }

    private void showLeaveAllianceDialog(AllianceInvitation invitation, Alliance currentAlliance) {
        showLoading(false);

        new AlertDialog.Builder(this)
                .setTitle("Leave Current Alliance?")
                .setMessage("You are already in \"" + currentAlliance.getName() +
                        "\". Do you want to leave it and join \"" + invitation.getAllianceName() + "\"?")
                .setPositiveButton("Yes, Join New", (dialog, which) -> {
                    if (currentAlliance.isMissionActive()) {
                        Toast.makeText(this, "Cannot leave alliance during active mission",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        acceptInvitation(invitation, currentAlliance.getId());
                    }
                })
                .setNegativeButton("No, Stay", null)
                .show();
    }

    private void acceptInvitation(AllianceInvitation invitation, String currentAllianceId) {
        showLoading(true);
        allianceRepository.acceptInvitation(invitation, currentAllianceId, new AuthCallback<Boolean>() {
            @Override
            public void onResult(Boolean success) {
                showLoading(false);
                if (success) {
                    Toast.makeText(AllianceInvitationsActivity.this,
                            "Joined alliance: " + invitation.getAllianceName(), Toast.LENGTH_SHORT).show();
                    loadInvitations();
                } else {
                    Toast.makeText(AllianceInvitationsActivity.this,
                            "Failed to join alliance", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void handleRejectInvitation(AllianceInvitation invitation) {
        showLoading(true);
        allianceRepository.rejectInvitation(invitation.getId(), new AuthCallback<Boolean>() {
            @Override
            public void onResult(Boolean success) {
                showLoading(false);
                if (success) {
                    Toast.makeText(AllianceInvitationsActivity.this,
                            "Invitation rejected", Toast.LENGTH_SHORT).show();
                    loadInvitations();
                } else {
                    Toast.makeText(AllianceInvitationsActivity.this,
                            "Failed to reject invitation", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}

