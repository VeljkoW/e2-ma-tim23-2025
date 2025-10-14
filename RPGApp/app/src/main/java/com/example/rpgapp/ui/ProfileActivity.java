package com.example.rpgapp.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
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
import com.example.rpgapp.model.User;
import com.example.rpgapp.service.AuthService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

public class ProfileActivity extends AppCompatActivity
{

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private AuthService authService;
    private User currentUser;

    private ImageView ivAvatar, ivQRCode;
    private TextView tvUsername, tvLevel, tvTitle, tvXPProgress, tvPowerPoints, tvCoins, tvBadgeCount;
    private ProgressBar progressXP;
    private MaterialButton btnChangePassword, btnLogout;
    private ImageButton btnBack, btnViewStatistics;
    private RecyclerView rvBadges, rvEquipment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        authService = new AuthService(this);

        initViews();
        setupRecyclerViews();
        loadUserProfile();
        setupClickListeners();
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
        progressXP = findViewById(R.id.progressXP);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnLogout = findViewById(R.id.btnLogout);
        btnBack = findViewById(R.id.btnBack);
        btnViewStatistics = findViewById(R.id.btnViewStatistics);
        rvBadges = findViewById(R.id.rvBadges);
        rvEquipment = findViewById(R.id.rvEquipment);
    }

    private void setupRecyclerViews()
    {

        rvBadges.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        rvBadges.setAdapter(new EmptyAdapter());


        rvEquipment.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        rvEquipment.setAdapter(new EmptyAdapter());
    }

    private void loadUserProfile() {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser == null) {
            finish();
            return;
        }

        String userId = firebaseUser.getUid();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentUser = documentSnapshot.toObject(User.class);
                        if (currentUser != null) {
                            displayUserProfile();
                            generateQRCode(userId);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void displayUserProfile()
    {
        tvUsername.setText(currentUser.getUsername());
        tvLevel.setText("Level " + currentUser.getLevel());
        tvTitle.setText(currentUser.getTitle());
        tvPowerPoints.setText(String.valueOf(currentUser.getPowerPoints()));
        tvCoins.setText(String.valueOf(currentUser.getCoins()));

        // XP Progress
        int currentXP = currentUser.getExperiencePoints();
        int requiredXP = currentUser.getXpForNextLevel();
        progressXP.setMax(requiredXP);
        progressXP.setProgress(currentXP);
        tvXPProgress.setText(currentXP + " / " + requiredXP + " XP");

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
