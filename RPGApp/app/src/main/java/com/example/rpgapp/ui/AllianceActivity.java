package com.example.rpgapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rpgapp.R;
import com.example.rpgapp.adapter.AllianceMemberAdapter;
import com.example.rpgapp.callback.AuthCallback;
import com.example.rpgapp.model.Alliance;
import com.example.rpgapp.model.AllianceBoss;
import com.example.rpgapp.model.User;
import com.example.rpgapp.repository.AllianceBossRepository;
import com.example.rpgapp.repository.AllianceRepository;
import com.example.rpgapp.service.AllianceBossService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class AllianceActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvAllianceName, tvLeaderName, tvMemberCount, tvEmptyState;
    private RecyclerView rvMembers;
    private Button btnChat, btnDeleteAlliance, btnLeaveAlliance;
    private ProgressBar progressBar;
    private ScrollView allianceContainer;
    private LinearLayout emptyContainer;

    // Alliance Boss UI components
    private CardView cvBossStatus;
    private MaterialButton btnCreateBoss;
    private ImageView ivBossLogo;
    private TextView tvBossName, tvDaysRemaining, tvHpText;
    private View viewHpBar;

    private AllianceRepository allianceRepository;
    private AllianceBossRepository allianceBossRepository;
    private AllianceBossService bossService;
    private AllianceMemberAdapter memberAdapter;
    private String currentUserId;
    private Alliance currentAlliance;
    private AllianceBoss currentAllianceBoss;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alliance);

        allianceRepository = new AllianceRepository();
        allianceBossRepository = new AllianceBossRepository();
        bossService = new AllianceBossService(this);
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        initViews();
        setupRecyclerView();
        loadAlliance();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvAllianceName = findViewById(R.id.tvAllianceName);
        tvLeaderName = findViewById(R.id.tvLeaderName);
        tvMemberCount = findViewById(R.id.tvMemberCount);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        rvMembers = findViewById(R.id.rvMembers);
        btnChat = findViewById(R.id.btnChat);
        btnDeleteAlliance = findViewById(R.id.btnDeleteAlliance);
        btnLeaveAlliance = findViewById(R.id.btnLeaveAlliance);
        progressBar = findViewById(R.id.progressBar);
        allianceContainer = findViewById(R.id.allianceContainer);
        emptyContainer = findViewById(R.id.emptyContainer);

        // Alliance Boss UI components
        cvBossStatus = findViewById(R.id.cvBossStatus);
        btnCreateBoss = findViewById(R.id.btnCreateBoss);
        ivBossLogo = findViewById(R.id.ivBossLogo);
        tvBossName = findViewById(R.id.tvBossName);
        tvDaysRemaining = findViewById(R.id.tvDaysRemaining);
        tvHpText = findViewById(R.id.tvHpText);
        viewHpBar = findViewById(R.id.viewHpBar);

        btnBack.setOnClickListener(v -> finish());

        Button btnCreateAlliance = findViewById(R.id.btnCreateAlliance);
        btnCreateAlliance.setOnClickListener(v -> showCreateAllianceDialog());

        btnChat.setOnClickListener(v -> openChat());
        btnDeleteAlliance.setOnClickListener(v -> confirmDeleteAlliance());
        btnLeaveAlliance.setOnClickListener(v -> confirmLeaveAlliance());
        btnCreateBoss.setOnClickListener(v -> showCreateBossDialog());
    }

    private void setupRecyclerView() {
        rvMembers.setLayoutManager(new LinearLayoutManager(this));
        memberAdapter = new AllianceMemberAdapter();
        rvMembers.setAdapter(memberAdapter);
    }

    private void loadAlliance() {
        showLoading(true);
        allianceRepository.getUserAlliance(currentUserId, new AuthCallback<Alliance>() {
            @Override
            public void onResult(Alliance alliance) {
                showLoading(false);
                if (alliance != null) {
                    currentAlliance = alliance;
                    displayAlliance(alliance);
                } else {
                    showEmptyState();
                }
            }
        });
    }

    private void displayAlliance(Alliance alliance) {
        allianceContainer.setVisibility(View.VISIBLE);
        emptyContainer.setVisibility(View.GONE);

        tvAllianceName.setText(alliance.getName());
        tvMemberCount.setText(alliance.getMemberIds().size() + " members");

        // Load leader name
        allianceRepository.getAllianceMembers(List.of(alliance.getLeaderId()), new AuthCallback<List<User>>() {
            @Override
            public void onResult(List<User> users) {
                if (!users.isEmpty()) {
                    tvLeaderName.setText("Leader: " + users.get(0).getUsername());
                }
            }
        });

        // Load members
        allianceRepository.getAllianceMembers(alliance.getMemberIds(), new AuthCallback<List<User>>() {
            @Override
            public void onResult(List<User> members) {
                memberAdapter.setMembers(members, alliance.getLeaderId());
            }
        });

        // Show/hide buttons based on role
        boolean isLeader = alliance.getLeaderId().equals(currentUserId);
        btnDeleteAlliance.setVisibility(isLeader && !alliance.isMissionActive() ? View.VISIBLE : View.GONE);
        btnLeaveAlliance.setVisibility(!isLeader && !alliance.isMissionActive() ? View.VISIBLE : View.GONE);

        // Load boss data
        loadBossData(alliance.getId());
    }

    private void loadBossData(String allianceId) {
        // Load alive alliance bosses for this alliance
        allianceBossRepository.getAliveAllianceBossesByAllianceId(allianceId).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (!task.getResult().isEmpty()) {
                    // Found alive boss(es), get the first one
                    AllianceBoss boss = task.getResult().getDocuments().get(0).toObject(AllianceBoss.class);
                    if (boss != null) {
                        boss.setId(task.getResult().getDocuments().get(0).getId());
                        currentAllianceBoss = boss;
                    }
                } else {
                    // No alive boss found
                    currentAllianceBoss = null;
                }
                updateBossDisplay();
            } else {
                // Error loading boss data, assume no boss exists
                currentAllianceBoss = null;
                updateBossDisplay();
            }
        });
    }

    private void showEmptyState() {
        allianceContainer.setVisibility(View.GONE);
        emptyContainer.setVisibility(View.VISIBLE);
    }

    private void showCreateAllianceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create Alliance");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_alliance, null);
        builder.setView(dialogView);

        TextInputEditText etAllianceName = dialogView.findViewById(R.id.etAllianceName);

        builder.setPositiveButton("Create", (dialog, which) -> {
            String name = etAllianceName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter alliance name", Toast.LENGTH_SHORT).show();
                return;
            }
            createAlliance(name);
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void createAlliance(String name) {
        showLoading(true);
        allianceRepository.createAlliance(name, currentUserId, new AuthCallback<Alliance>() {
            @Override
            public void onResult(Alliance alliance) {
                showLoading(false);
                if (alliance != null) {
                    Toast.makeText(AllianceActivity.this, "Alliance created!", Toast.LENGTH_SHORT).show();
                    loadAlliance();
                } else {
                    Toast.makeText(AllianceActivity.this, "Failed to create alliance", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void openChat() {
        if (currentAlliance != null) {
            Intent intent = new Intent(this, AllianceChatActivity.class);
            intent.putExtra("ALLIANCE_ID", currentAlliance.getId());
            intent.putExtra("ALLIANCE_NAME", currentAlliance.getName());
            startActivity(intent);
        }
    }

    private void confirmDeleteAlliance() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Alliance")
                .setMessage("Are you sure you want to delete this alliance? All members will be removed.")
                .setPositiveButton("Delete", (dialog, which) -> deleteAlliance())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAlliance() {
        if (currentAlliance != null) {
            showLoading(true);
            allianceRepository.deleteAlliance(currentAlliance.getId(), new AuthCallback<Boolean>() {
                @Override
                public void onResult(Boolean success) {
                    showLoading(false);
                    if (success) {
                        Toast.makeText(AllianceActivity.this, "Alliance deleted", Toast.LENGTH_SHORT).show();
                        loadAlliance();
                    } else {
                        Toast.makeText(AllianceActivity.this, "Failed to delete alliance", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void confirmLeaveAlliance() {
        new AlertDialog.Builder(this)
                .setTitle("Leave Alliance")
                .setMessage("Are you sure you want to leave this alliance?")
                .setPositiveButton("Leave", (dialog, which) -> leaveAlliance())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void leaveAlliance() {
        if (currentAlliance != null) {
            showLoading(true);
            allianceRepository.leaveAlliance(currentAlliance.getId(), currentUserId, new AuthCallback<Boolean>() {
                @Override
                public void onResult(Boolean success) {
                    showLoading(false);
                    if (success) {
                        Toast.makeText(AllianceActivity.this, "Left alliance", Toast.LENGTH_SHORT).show();
                        loadAlliance();
                    } else {
                        Toast.makeText(AllianceActivity.this, "Failed to leave alliance", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void showCreateBossDialog() {
        if (currentAlliance == null) return;

        // Check if user is alliance leader
        boolean isLeader = currentAlliance.getLeaderId().equals(currentUserId);
        if (!isLeader) {
            Toast.makeText(this, "Only alliance leaders can create bosses", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if an alive alliance boss already exists
        if (currentAllianceBoss != null && currentAllianceBoss.isAlive()) {
            Toast.makeText(this, "Your alliance already has an active boss. Defeat it before creating a new one.", Toast.LENGTH_LONG).show();
            return;
        }

        // Show confirmation dialog without "reset" language
        new AlertDialog.Builder(this)
                .setTitle("Create Alliance Boss")
                .setMessage("This will create a new alliance boss for your alliance. The boss will be active for 14 days. Are you sure?")
                .setPositiveButton("Create", (dialog, which) -> createAllianceBoss())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void createAllianceBoss() {
        if (currentAlliance == null) return;

        showLoading(true);
        bossService.createAllianceBossForUser(currentUserId).addOnCompleteListener(task -> {
            showLoading(false);
            if (task.isSuccessful()) {
                AllianceBossService.CreateAllianceBossResult result = task.getResult();
                if (result.isSuccess()) {
                    Toast.makeText(AllianceActivity.this, result.getMessage(), Toast.LENGTH_SHORT).show();
                    currentAllianceBoss = result.getAllianceBoss();
                    updateBossDisplay();
                } else {
                    Toast.makeText(AllianceActivity.this, result.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(AllianceActivity.this, "Failed to create alliance boss", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateBossDisplay() {
        if (currentAlliance == null) return;

        // Check if user is alliance leader
        boolean isLeader = currentAlliance.getLeaderId().equals(currentUserId);

        if (currentAllianceBoss != null && currentAllianceBoss.isAlive()) {
            // Show boss status
            cvBossStatus.setVisibility(View.VISIBLE);
            btnCreateBoss.setVisibility(View.GONE);

            displayBossStatus(currentAllianceBoss);
        } else {
            // Show create boss button
            cvBossStatus.setVisibility(View.GONE);
            btnCreateBoss.setVisibility(View.VISIBLE);
            btnCreateBoss.setEnabled(isLeader);

            if (!isLeader) {
                btnCreateBoss.setText("Create Alliance Boss (Leader Only)");
                btnCreateBoss.setAlpha(0.5f);
            } else {
                btnCreateBoss.setText("Create Alliance Boss");
                btnCreateBoss.setAlpha(1.0f);
            }
        }
    }

    private void displayBossStatus(AllianceBoss boss) {
        // Use a generic name since AllianceBoss doesn't have getName()
        tvBossName.setText("Alliance Boss");

        // Calculate days remaining manually (14 days from creation)
        long creationTime = boss.getDateOfCreation().getTime();
        long currentTime = System.currentTimeMillis();
        long daysPassed = (currentTime - creationTime) / (1000 * 60 * 60 * 24);
        long daysRemaining = 14 - daysPassed;

        if (daysRemaining > 0) {
            tvDaysRemaining.setText(daysRemaining + " days remaining");
        } else {
            tvDaysRemaining.setText("Expired");
        }

        // Update HP display using correct method names
        tvHpText.setText(boss.getCurrentHp() + " / " + boss.getStartingHp());

        // Update HP bar width using correct HP calculation
        float hpPercentage = (float) boss.getCurrentHp() / boss.getStartingHp();
        int maxWidth = getResources().getDisplayMetrics().widthPixels - 64; // Account for margins
        int hpBarWidth = (int) (maxWidth * hpPercentage);

        viewHpBar.getLayoutParams().width = hpBarWidth;
        viewHpBar.requestLayout();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
