package com.example.rpgapp.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rpgapp.R;
import com.example.rpgapp.adapter.EquipmentAdapter;
import com.example.rpgapp.model.Equipment;
import com.example.rpgapp.model.User;
import com.example.rpgapp.repository.EquipmentRepository;
import com.example.rpgapp.service.AuthService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import java.util.ArrayList;
import java.util.List;

public class ProfileActivity extends AppCompatActivity
{
    private static final String TAG = "ProfileActivity";
    public static final String EXTRA_USER_ID = "USER_ID";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private AuthService authService;
    private EquipmentRepository equipmentRepository;
    private User currentUser;
    private String viewedUserId; // ID of the user being viewed
    private boolean isOwnProfile; // Whether viewing own profile

    private ImageView ivAvatar, ivQRCode;
    private TextView tvUsername, tvLevel, tvTitle, tvXPProgress, tvPowerPoints, tvCoins, tvBadgeCount, tvQRCodeTitle;
    private ProgressBar progressXP;
    private MaterialButton btnChangePassword, btnLogout;
    private ImageButton btnBack, btnViewStatistics;
    private RecyclerView rvBadges, rvEquipment;
    private View statsGrid;

    private EquipmentAdapter equipmentAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        authService = new AuthService(this);
        equipmentRepository = new EquipmentRepository();

        // Check if viewing another user's profile
        viewedUserId = getIntent().getStringExtra(EXTRA_USER_ID);
        String currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

        if (viewedUserId == null) {
            viewedUserId = currentUserId; // View own profile by default
        }

        isOwnProfile = currentUserId != null && currentUserId.equals(viewedUserId);

        initViews();
        setupRecyclerViews();
        loadUserProfile();

        // Only setup click listeners if viewing own profile
        if (isOwnProfile) {
            setupClickListeners();
        }

        // Hide private elements if viewing another user's profile
        updateUIForProfileType();
    }

    private void initViews() {
        ivAvatar = findViewById(R.id.ivAvatar);
        ivQRCode = findViewById(R.id.ivQRCode);
        tvUsername = findViewById(R.id.tvUsername);
        tvLevel = findViewById(R.id.tvLevel);
        tvTitle = findViewById(R.id.tvTitle);
        tvXPProgress = findViewById(R.id.tvXPProgress);
        tvPowerPoints = findViewById(R.id.tvPowerPoints);
        tvCoins = findViewById(R.id.tvCoins);
        tvBadgeCount = findViewById(R.id.tvBadgeCount);
        tvQRCodeTitle = findViewById(R.id.tvQRCodeTitle);
        progressXP = findViewById(R.id.progressXP);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnLogout = findViewById(R.id.btnLogout);
        btnBack = findViewById(R.id.btnBack);
        btnViewStatistics = findViewById(R.id.btnViewStatistics);
        rvBadges = findViewById(R.id.rvBadges);
        rvEquipment = findViewById(R.id.rvEquipment);
        statsGrid = findViewById(R.id.statsGrid);
    }

    private void setupRecyclerViews()
    {
        rvBadges.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvBadges.setAdapter(new EmptyAdapter());

        // Setup equipment RecyclerView with new adapter
        rvEquipment.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        equipmentAdapter = new EquipmentAdapter(equipment -> {
            // Show equipment details when clicked
            showEquipmentDetails(equipment);
        });
        rvEquipment.setAdapter(equipmentAdapter);
    }

    private void loadUserProfile() {
        if (viewedUserId == null) {
            finish();
            return;
        }

        db.collection("users").document(viewedUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentUser = documentSnapshot.toObject(User.class);
                        if (currentUser != null) {
                            displayUserProfile();
                            generateQRCode(viewedUserId);
                            loadUserEquipment(viewedUserId);
                            // Update UI after user data is loaded so we can use username in QR title
                            updateUIForProfileType();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadUserEquipment(String userId) {
        Log.d(TAG, "Loading equipment for user: " + userId);
        equipmentRepository.getUserEquipment(userId)
                .addOnSuccessListener(querySnapshot -> {
                    List<Equipment> equipmentList = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Equipment equipment = doc.toObject(Equipment.class);
                        // Filter out used equipment - only show equipment that can still be used
                        if (equipment != null && equipment.canBeUsed()) {
                            // For other users' profiles, only show active/equipped equipment
                            if (isOwnProfile || equipment.isActive() || equipment.isEquipped()) {
                                equipmentList.add(equipment);
                            }
                        }
                    }
                    Log.d(TAG, "Equipment loaded successfully: " + equipmentList.size() + " items");
                    if (!equipmentList.isEmpty()) {
                        equipmentAdapter.setEquipmentList(equipmentList);
                    } else {
                        Log.d(TAG, "No equipment found for user");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load equipment", e);
                    Toast.makeText(ProfileActivity.this, "Failed to load equipment", Toast.LENGTH_SHORT).show();
                });
    }

    private void displayUserProfile()
    {
        tvUsername.setText(currentUser.getUsername());
        tvLevel.setText("Level " + currentUser.getLevel());
        tvTitle.setText(currentUser.getTitle());

        // XP Progress - show for all users
        int currentXP = currentUser.getExperiencePoints();
        int requiredXP = currentUser.getXpForNextLevel();
        progressXP.setMax(requiredXP);
        progressXP.setProgress(currentXP);

        if (isOwnProfile) {
            // Show all data for own profile
            tvPowerPoints.setText(String.valueOf(currentUser.getPowerPoints()));
            tvCoins.setText(String.valueOf(currentUser.getCoins()));
            tvXPProgress.setText(currentXP + " / " + requiredXP + " XP");
        } else {
            // For other users, show XP progress but hide PP and Coins values
            tvPowerPoints.setText("???");
            tvCoins.setText("???");
            tvXPProgress.setText(currentXP + " / " + requiredXP + " XP");
        }

        // Load avatar
        loadAvatar(currentUser.getAvatarId());
    }

    private void loadAvatar(String avatarId)
    {
        if (avatarId == null || avatarId.isEmpty())
        {
            avatarId = "avatar_1";
        }

        int resourceId = getResources().getIdentifier(avatarId, "drawable", getPackageName());
        if (resourceId != 0) {
            ivAvatar.setImageResource(resourceId);
        }
    }

    private void generateQRCode(String userId)
    {
        try
        {
            MultiFormatWriter writer = new MultiFormatWriter();
            BitMatrix bitMatrix = writer.encode(userId, BarcodeFormat.QR_CODE, 512, 512);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

            for (int x = 0; x < width; x++)
            {
                for (int y = 0; y < height; y++)
                {
                    bmp.setPixel(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }

            ivQRCode.setImageBitmap(bmp);
        } catch (Exception e)
        {
            e.printStackTrace();
            Toast.makeText(this, "Failed to generate QR code", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupClickListeners()
    {
        btnBack.setOnClickListener(v -> finish());

        btnViewStatistics.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, StatisticsActivity.class);
            startActivity(intent);
        });

        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        btnLogout.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        authService.logout();
                        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("No", null)
                    .show();
        });
    }

    private void showChangePasswordDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.change_password);

        final android.view.View customLayout = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        builder.setView(customLayout);

        TextInputEditText etOldPassword = customLayout.findViewById(R.id.etOldPassword);
        TextInputEditText etNewPassword = customLayout.findViewById(R.id.etNewPassword);
        TextInputEditText etConfirmPassword = customLayout.findViewById(R.id.etConfirmPassword);

        builder.setPositiveButton(R.string.save_changes, (dialog, which) -> {
            if (etOldPassword.getText() == null || etNewPassword.getText() == null || etConfirmPassword.getText() == null)
            {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            String oldPassword = etOldPassword.getText().toString().trim();
            String newPassword = etNewPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();

            if (oldPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty())
            {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPassword.equals(confirmPassword))
            {
                Toast.makeText(this, "New passwords don't match", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPassword.length() < 6)
            {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            changePassword(oldPassword, newPassword);
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void changePassword(String oldPassword, String newPassword)
    {
        authService.changePassword(oldPassword, newPassword, result -> {
            if (result.isSuccess())
            {
                Toast.makeText(this, result.getMessage(), Toast.LENGTH_SHORT).show();
            }
            else
            {
                Toast.makeText(this, result.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showEquipmentDetails(Equipment equipment) {
        StringBuilder details = new StringBuilder();
        details.append("Name: ").append(equipment.getName()).append("\n\n");
        details.append("Type: ").append(equipment.getType().name()).append("\n");

        if (equipment.getPowerBonus() > 0) {
            details.append("Power Bonus: +").append(equipment.getPowerBonus()).append("%\n");
        }
        if (equipment.getAttackChanceBonus() > 0) {
            details.append("Attack Chance: +").append(equipment.getAttackChanceBonus()).append("%\n");
        }
        if (equipment.getExtraAttackChance() > 0) {
            details.append("Extra Attack Chance: +").append(equipment.getExtraAttackChance()).append("%\n");
        }
        if (equipment.getCoinBonus() > 0) {
            details.append("Coin Bonus: +").append(equipment.getCoinBonus()).append("%\n");
        }

        if (equipment.getType() == Equipment.EquipmentType.CLOTHING) {
            details.append("\nRemaining Battles: ").append(equipment.getRemainingBattles());
        }

        if (equipment.isActive()) {
            details.append("\n\nStatus: Active");
        } else if (equipment.isEquipped()) {
            details.append("\n\nStatus: Equipped");
        }

        new AlertDialog.Builder(this)
                .setTitle("Equipment Details")
                .setMessage(details.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    private void updateUIForProfileType() {
        if (!isOwnProfile) {
            // Hide private UI elements when viewing another user's profile
            if (btnChangePassword != null) btnChangePassword.setVisibility(View.GONE);
            if (btnLogout != null) btnLogout.setVisibility(View.GONE);
            if (btnViewStatistics != null) btnViewStatistics.setVisibility(View.GONE);

            // Hide entire stats grid (PP and Coins cards)
            if (statsGrid != null) statsGrid.setVisibility(View.GONE);

            // Change QR code title to show it's the user's QR code, not yours
            if (tvQRCodeTitle != null && currentUser != null) {
                tvQRCodeTitle.setText(currentUser.getUsername() + "'s QR Code");
            }

            // Change back button behavior
            if (btnBack != null) {
                btnBack.setOnClickListener(v -> finish());
            }

            // Update title
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("User Profile");
            }
        } else {
            // Set title for own profile
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("My Profile");
            }

            // Make sure QR code title says "Your QR Code"
            if (tvQRCodeTitle != null) {
                tvQRCodeTitle.setText("Your QR Code");
            }
        }
    }

    private static class EmptyAdapter extends RecyclerView.Adapter<EmptyAdapter.EmptyViewHolder>
    {
        @Override
        public EmptyViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType)
        {
            android.view.View view = new android.view.View(parent.getContext());
            return new EmptyViewHolder(view);
        }

        @Override
        public void onBindViewHolder(EmptyViewHolder holder, int position)
        {

        }

        @Override
        public int getItemCount()
        {
            return 0;
        }

        static class EmptyViewHolder extends RecyclerView.ViewHolder
        {
            EmptyViewHolder(android.view.View itemView)
            {
                super(itemView);
            }
        }
    }
}
