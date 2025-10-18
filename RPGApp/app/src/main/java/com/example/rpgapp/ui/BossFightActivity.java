package com.example.rpgapp.ui;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.rpgapp.R;
import com.example.rpgapp.callback.AuthCallback;
import com.example.rpgapp.model.Boss;
import com.example.rpgapp.model.Equipment;
import com.example.rpgapp.service.AuthService;
import com.example.rpgapp.service.BossService;
import com.example.rpgapp.repository.EquipmentRepository;
import com.example.rpgapp.repository.BossRepository;
import com.example.rpgapp.repository.UserRepository;
import com.example.rpgapp.util.ShakeDetector;
import com.example.rpgapp.util.EquipmentRewardSystem;

import java.util.List;

public class BossFightActivity extends AppCompatActivity {
    private static final String TAG = "BossFightActivity";

    private BossService bossService;
    private AuthService authService;
    private Boss currentBoss;
    private String bossId;

    // UI Components
    private TextView tvBossLevel;
    private TextView tvBossName;
    private TextView tvBossHp;
    private ProgressBar pbBossHp;
    private TextView tvAttacksRemaining;
    private TextView tvHitChance;
    private TextView tvDodgeChance;
    private TextView tvCoinReward;
    private TextView tvBossCreationDate;
    private Button btnAttack;
    private Button btnBack;

    // New UI Components for user stats and equipment
    private TextView tvUserCurrentPower;
    private TextView tvUserBasePower;
    private ProgressBar pbUserPower;
    private LinearLayout llEquippedItems;
    private TextView tvNoItems;

    // Animation components
    private ImageView ivBossImage;
    private TextView tvDamageIndicator;
    private TextView tvMissIndicator;
    private TextView tvVictoryMessage;
    private TextView tvDefeatMessage;
    private View vAttackFlash;

    // Chest animation components
    private RelativeLayout rlChestContainer;
    private ImageView ivChest;
    private TextView tvShakeInstruction;
    private LinearLayout llRewardsContainer;
    private TextView tvCoinRewardAmount;
    private boolean chestOpened = false;

    // Shake detection
    private SensorManager sensorManager;
    private ShakeDetector shakeDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_boss_fight);

        // Initialize services
        bossService = new BossService(this);
        authService = new AuthService(this);

        // Get boss ID from intent
        bossId = getIntent().getStringExtra("boss_id");
        if (bossId == null) {
            Toast.makeText(this, "No boss ID provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupClickListeners();
        loadBossData();

        // Initialize shake detection
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        shakeDetector = new ShakeDetector();
        shakeDetector.setOnShakeListener(this::onShakeDetected);
    }

    private void initializeViews() {
        tvBossLevel = findViewById(R.id.tv_boss_level);
        tvBossName = findViewById(R.id.tv_boss_name);
        tvBossHp = findViewById(R.id.tv_boss_hp);
        pbBossHp = findViewById(R.id.pb_boss_hp);
        tvAttacksRemaining = findViewById(R.id.tv_attacks_remaining);
        tvHitChance = findViewById(R.id.tv_hit_chance);
        tvDodgeChance = findViewById(R.id.tv_dodge_chance);
        tvCoinReward = findViewById(R.id.tv_coin_reward);
        tvBossCreationDate = findViewById(R.id.tv_boss_creation_date);
        btnAttack = findViewById(R.id.btn_attack);
        btnBack = findViewById(R.id.btn_back);

        // New UI Components initialization
        tvUserCurrentPower = findViewById(R.id.tv_user_current_power);
        tvUserBasePower = findViewById(R.id.tv_user_base_power);
        pbUserPower = findViewById(R.id.pb_user_power);
        llEquippedItems = findViewById(R.id.ll_equipped_items);
        tvNoItems = findViewById(R.id.tv_no_items);

        // Animation components
        ivBossImage = findViewById(R.id.iv_boss_image);
        tvDamageIndicator = findViewById(R.id.tv_damage_indicator);
        tvMissIndicator = findViewById(R.id.tv_miss_indicator);
        tvVictoryMessage = findViewById(R.id.tv_victory_message);
        tvDefeatMessage = findViewById(R.id.tv_defeat_message);
        vAttackFlash = findViewById(R.id.v_attack_flash);

        // Chest animation components
        rlChestContainer = findViewById(R.id.rl_chest_container);
        ivChest = findViewById(R.id.iv_chest);
        tvShakeInstruction = findViewById(R.id.tv_shake_instruction);
        llRewardsContainer = findViewById(R.id.ll_rewards_container);
        tvCoinRewardAmount = findViewById(R.id.tv_coin_reward_amount);
    }

    private void setupClickListeners() {
        btnAttack.setOnClickListener(v -> attackBoss());
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadBossData() {
        bossService.getBossById(bossId)
                .addOnSuccessListener(boss -> {
                    if (boss != null) {
                        currentBoss = boss;
                        updateUI();
                    } else {
                        Toast.makeText(this, "Boss not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load boss data", e);
                    Toast.makeText(this, "Failed to load boss: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void updateUI() {
        if (currentBoss == null) return;

        // Boss basic info
        tvBossLevel.setText("Level " + currentBoss.getLevel());
        tvBossName.setText("Boss Level " + currentBoss.getLevel());

        // HP information
        int currentHp = currentBoss.getHp();
        int maxHp = currentBoss.getStartingHp();
        tvBossHp.setText(currentHp + " / " + maxHp + " HP");

        // Update HP progress bar (red bar representing percentage)
        if (maxHp > 0) {
            int hpPercentage = (int) ((double) currentHp / maxHp * 100);
            pbBossHp.setProgress(hpPercentage);
        } else {
            pbBossHp.setProgress(0);
        }

        // Attack information
        tvAttacksRemaining.setText("Attacks Remaining: " + currentBoss.getNumberOfAttacks());

        // Hit chance (100% - dodge chance)
        double hitChance = (1.0 - currentBoss.getChanceTododge()) * 100;
        tvHitChance.setText(String.format("Hit Chance: %.1f%%", hitChance));
        tvDodgeChance.setText(String.format("Boss Dodge Chance: %.1f%%", currentBoss.getChanceTododge() * 100));

        // Rewards
        tvCoinReward.setText("Coin Reward: " + currentBoss.getCoinReward());

        // Creation date
        if (currentBoss.getDateOfCreation() != null) {
            tvBossCreationDate.setText("Created: " +
                android.text.format.DateFormat.format("MMM dd, yyyy HH:mm", currentBoss.getDateOfCreation()));
        }

        // Enable/disable attack button based on boss status and remaining attacks
        btnAttack.setEnabled(currentBoss.isAlive() && currentBoss.getNumberOfAttacks() > 0);

        if (!currentBoss.isAlive()) {
            btnAttack.setText("Boss Defeated");
        } else if (currentBoss.getNumberOfAttacks() <= 0) {
            btnAttack.setText("No Attacks Left");
        } else {
            btnAttack.setText("Attack (" + currentBoss.getNumberOfAttacks() + " left)");
        }

        // Update user power points display
        updateUserPowerUI();

        // Load and display equipped items
        loadAndDisplayEquippedItems();
    }

    private void updateUserPowerUI() {
        String userId = authService.getCurrentUserId();
        if (userId == null) return;

        authService.getCurrentUser(user -> {
            if (user != null) {
                // Update current and base power text views
                tvUserCurrentPower.setText("Current Power: " + user.getPowerPoints());
                tvUserBasePower.setText("Base Power: " + user.getStartingPowerPoints());

                // Calculate and update power progress bar (blue bar)
                int powerPercentage = (int) ((double) user.getPowerPoints() / user.getStartingPowerPoints() * 100);
                pbUserPower.setProgress(powerPercentage);
            } else {
                Log.e(TAG, "Failed to get current user for power points UI update");
            }
        });
    }

    private void loadAndDisplayEquippedItems() {
        String userId = authService.getCurrentUserId();
        if (userId == null) return;

        EquipmentRepository equipmentRepository = new EquipmentRepository();
        equipmentRepository.getEquipmentList(
            equipmentRepository.getUserEquipment(userId),
            new EquipmentRepository.EquipmentCallback<List<Equipment>>() {
                @Override
                public void onSuccess(List<Equipment> equipmentList) {
                    llEquippedItems.removeAllViews();

                    if (equipmentList == null || equipmentList.isEmpty()) {
                        tvNoItems.setVisibility(View.VISIBLE);
                    } else {
                        boolean hasActiveItems = false;

                        for (Equipment equipment : equipmentList) {
                            // Check both equipped and active (potions use isActive)
                            boolean isEquipmentActive = equipment.isEquipped() || equipment.isActive();

                            if (isEquipmentActive && equipment.canBeUsed()) {
                                hasActiveItems = true;
                                View itemView = getLayoutInflater().inflate(R.layout.item_equipped, llEquippedItems, false);
                                TextView tvItemName = itemView.findViewById(R.id.tv_item_name);
                                TextView tvItemBonus = itemView.findViewById(R.id.tv_item_bonus);

                                tvItemName.setText(equipment.getName());

                                // Show appropriate bonus info based on equipment type
                                String bonusText = "";
                                if (equipment.getPowerBonus() > 0) {
                                    bonusText += "Power: +" + equipment.getPowerBonus();
                                }
                                if (equipment.getAttackChanceBonus() > 0) {
                                    if (!bonusText.isEmpty()) bonusText += ", ";
                                    bonusText += "Hit Chance: +" + equipment.getAttackChanceBonus() + "%";
                                }
                                if (equipment.getExtraAttackChance() > 0) {
                                    if (!bonusText.isEmpty()) bonusText += ", ";
                                    bonusText += "Extra Attack: +" + equipment.getExtraAttackChance() + "%";
                                }
                                if (equipment.getCoinBonus() > 0) {
                                    if (!bonusText.isEmpty()) bonusText += ", ";
                                    bonusText += "Coins: +" + equipment.getCoinBonus() + "%";
                                }

                                tvItemBonus.setText(bonusText.isEmpty() ? "Active" : bonusText);

                                llEquippedItems.addView(itemView);
                            }
                        }

                        tvNoItems.setVisibility(hasActiveItems ? View.GONE : View.VISIBLE);
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "Failed to load equipment for display", e);
                }
            });
    }

    private void attackBoss() {
        String userId = authService.getCurrentUserId();
        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentBoss == null || !currentBoss.isAlive() || currentBoss.getNumberOfAttacks() <= 0) {
            Toast.makeText(this, "Cannot attack this boss", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable attack button during attack
        btnAttack.setEnabled(false);

        // Start attack animation sequence
        playAttackAttemptAnimation();

        // Get user's actual power points and apply equipment bonuses
        authService.getCurrentUser(user -> {
            if (user != null) {
                int basePowerPoints = user.getPowerPoints();
                Log.d(TAG, "Base user power points: " + basePowerPoints);

                // Load equipped equipment and apply bonuses
                loadEquippedEquipmentAndAttack(userId, basePowerPoints);
            } else {
                Log.e(TAG, "Failed to get current user for power points");
                Toast.makeText(this, "Failed to get user stats", Toast.LENGTH_SHORT).show();
                btnAttack.setEnabled(true); // Re-enable button on failure
            }
        });
    }

    private void loadEquippedEquipmentAndAttack(String userId, int basePowerPoints) {
        EquipmentRepository equipmentRepository = new EquipmentRepository();

        // Load all user equipment
        equipmentRepository.getEquipmentList(
            equipmentRepository.getUserEquipment(userId),
            new EquipmentRepository.EquipmentCallback<List<Equipment>>() {
                @Override
                public void onSuccess(List<Equipment> equipmentList) {
                    // Calculate equipment bonuses
                    EquipmentBonuses bonuses = calculateEquipmentBonuses(equipmentList);

                    // Apply bonuses to user stats
                    int totalPowerPoints = basePowerPoints + bonuses.powerBonus;

                    // Apply dodge reduction to boss (reduce dodge chance by extra attack chance)
                    double modifiedBossDodgeChance = Math.max(0, currentBoss.getChanceTododge() - (bonuses.extraAttackChance / 100.0));

                    // Apply coin bonus to rewards
                    int totalCoinReward = currentBoss.getCoinReward() + bonuses.coinBonus;

                    Log.d(TAG, "Equipment bonuses applied - Power: +" + bonuses.powerBonus +
                              ", Extra Attack: +" + bonuses.extraAttackChance + "%" +
                              ", Coin Bonus: +" + bonuses.coinBonus);
                    Log.d(TAG, "Total power points: " + totalPowerPoints);
                    Log.d(TAG, "Boss dodge chance reduced from " + (currentBoss.getChanceTododge() * 100) +
                              "% to " + (modifiedBossDodgeChance * 100) + "%");
                    Log.d(TAG, "Total coin reward: " + totalCoinReward);

                    // Perform the attack with enhanced stats
                    performEnhancedAttack(userId, totalPowerPoints, modifiedBossDodgeChance, totalCoinReward, bonuses);
                }

                @Override
                public void onFailure(Exception e) {
                    Log.w(TAG, "Failed to load equipment, proceeding with base stats: " + e.getMessage());
                    // Proceed with base stats if equipment loading fails
                    EquipmentBonuses emptyBonuses = new EquipmentBonuses();
                    performEnhancedAttack(userId, basePowerPoints, currentBoss.getChanceTododge(), currentBoss.getCoinReward(), emptyBonuses);
                }
            });
    }

    private EquipmentBonuses calculateEquipmentBonuses(List<Equipment> equipmentList) {
        EquipmentBonuses bonuses = new EquipmentBonuses();

        for (Equipment equipment : equipmentList) {
            // Proveravamo da li je oprema equipped ILI active (napici koriste isActive)
            boolean isEquipmentActive = equipment.isEquipped() || equipment.isActive();

            if (isEquipmentActive && equipment.canBeUsed()) {
                bonuses.powerBonus += equipment.getPowerBonus();
                bonuses.extraAttackChance += equipment.getExtraAttackChance();
                bonuses.coinBonus += equipment.getCoinBonus();

                Log.d(TAG, "Applied bonus from " + equipment.getName() +
                          " (Type: " + equipment.getType() + ")" +
                          " - Power: +" + equipment.getPowerBonus() +
                          ", Extra Attack: +" + equipment.getExtraAttackChance() + "%" +
                          ", Coin: +" + equipment.getCoinBonus());
            }
        }

        return bonuses;
    }

    private void performEnhancedAttack(String userId, int totalPowerPoints, double modifiedBossDodgeChance, int totalCoinReward, EquipmentBonuses bonuses) {
        // Here you would implement the logic to perform the attack using the enhanced stats
        // For example, you might want to call a method on bossService to handle the attack logic
        // Make sure to update the currentBoss object with the new state after the attack

        // Placeholder for attack logic - remove or replace with actual implementation
        bossService.attackBoss(bossId, userId, totalPowerPoints)
                .addOnSuccessListener(result -> {
                    // Update current boss with result
                    currentBoss = result.getBoss();

                    // Show animated result based on hit/miss
                    if (result.isHitSuccessful()) {
                        playDamageAnimation(result.getDamageDealt());
                    } else {
                        playMissAnimation();
                    }

                    // Delay UI update to let animation play
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        updateUI();

                        // Check if fight is over
                        if (result.isFightOver()) {
                            // Perform cleanup after fight ends (win or lose)
                            performPostFightCleanup(userId);

                            if (result.getResultType() == BossService.AttackResult.ResultType.BOSS_DEFEATED) {
                                playVictoryAnimation();
                            } else if (result.getResultType() == BossService.AttackResult.ResultType.BOSS_WINS) {
                                playDefeatAnimation();
                            }
                        } else {
                            // Re-enable attack button for next attack
                            btnAttack.setEnabled(true);
                        }
                    }, 1500); // Wait for damage/miss animation to complete
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to attack boss", e);
                    Toast.makeText(this, "Attack failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnAttack.setEnabled(true); // Re-enable button on failure
                });
    }

    private void playAttackAttemptAnimation() {
        // Flash effect when attempting attack
        if (vAttackFlash != null) {
            vAttackFlash.setVisibility(View.VISIBLE);
            vAttackFlash.setAlpha(0f);

            ObjectAnimator flashAnimator = ObjectAnimator.ofFloat(vAttackFlash, "alpha", 0f, 0.7f, 0f);
            flashAnimator.setDuration(500);
            flashAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            flashAnimator.start();

            // Hide flash after animation
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (vAttackFlash != null) {
                    vAttackFlash.setVisibility(View.GONE);
                }
            }, 500);
        }

        // Boss shake animation when being attacked
        if (ivBossImage != null) {
            ObjectAnimator shakeX = ObjectAnimator.ofFloat(ivBossImage, "translationX", 0, -10, 10, -5, 5, 0);
            shakeX.setDuration(400);
            shakeX.start();
        }
    }

    private void playDamageAnimation(int damage) {
        if (tvDamageIndicator != null) {
            tvDamageIndicator.setText("-" + damage);
            tvDamageIndicator.setVisibility(View.VISIBLE);
            tvDamageIndicator.setAlpha(1f);
            tvDamageIndicator.setScaleX(1f);
            tvDamageIndicator.setScaleY(1f);
            tvDamageIndicator.setTranslationY(0f);

            // Animate damage number floating up and fading
            AnimatorSet damageAnimSet = new AnimatorSet();
            ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(tvDamageIndicator, "scaleX", 1f, 1.5f, 1.2f);
            ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(tvDamageIndicator, "scaleY", 1f, 1.5f, 1.2f);
            ObjectAnimator moveUp = ObjectAnimator.ofFloat(tvDamageIndicator, "translationY", 0f, -100f);
            ObjectAnimator fadeOut = ObjectAnimator.ofFloat(tvDamageIndicator, "alpha", 1f, 0f);

            damageAnimSet.playTogether(scaleUpX, scaleUpY, moveUp, fadeOut);
            damageAnimSet.setDuration(1200);
            damageAnimSet.setInterpolator(new AccelerateDecelerateInterpolator());
            damageAnimSet.start();

            // Animate HP bar change
            animateHPBarChange();

            // Hide damage indicator after animation
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (tvDamageIndicator != null) {
                    tvDamageIndicator.setVisibility(View.GONE);
                }
            }, 1200);
        }
    }

    private void playMissAnimation() {
        if (tvMissIndicator != null) {
            tvMissIndicator.setText("MISS!");
            tvMissIndicator.setVisibility(View.VISIBLE);
            tvMissIndicator.setAlpha(1f);
            tvMissIndicator.setScaleX(1f);
            tvMissIndicator.setScaleY(1f);
            tvMissIndicator.setTranslationX(0f);

            // Animate miss text with sideways movement
            AnimatorSet missAnimSet = new AnimatorSet();
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(tvMissIndicator, "scaleX", 1f, 1.3f, 1f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(tvMissIndicator, "scaleY", 1f, 1.3f, 1f);
            ObjectAnimator moveX = ObjectAnimator.ofFloat(tvMissIndicator, "translationX", 0f, 50f, -50f, 0f);
            ObjectAnimator fadeOut = ObjectAnimator.ofFloat(tvMissIndicator, "alpha", 1f, 1f, 0f);

            missAnimSet.playTogether(scaleX, scaleY, moveX, fadeOut);
            missAnimSet.setDuration(1000);
            missAnimSet.setInterpolator(new BounceInterpolator());
            missAnimSet.start();

            // Hide miss indicator after animation
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (tvMissIndicator != null) {
                    tvMissIndicator.setVisibility(View.GONE);
                }
            }, 1000);
        }
    }

    private void animateHPBarChange() {
        if (pbBossHp != null && currentBoss != null) {
            int currentProgress = pbBossHp.getProgress();
            int maxHp = currentBoss.getStartingHp();
            int newHp = currentBoss.getHp();
            int newProgress = maxHp > 0 ? (int) ((double) newHp / maxHp * 100) : 0;

            ValueAnimator hpAnimator = ValueAnimator.ofInt(currentProgress, newProgress);
            hpAnimator.setDuration(800);
            hpAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            hpAnimator.addUpdateListener(animation -> {
                pbBossHp.setProgress((Integer) animation.getAnimatedValue());
            });
            hpAnimator.start();
        }
    }

    private void playVictoryAnimation() {
        // Show chest animation instead of simple victory message
        showVictoryChest();
    }

    private void playDefeatAnimation() {
        if (tvDefeatMessage != null) {
            tvDefeatMessage.setText("DEFEAT!");
            tvDefeatMessage.setVisibility(View.VISIBLE);
            tvDefeatMessage.setAlpha(0f);
            tvDefeatMessage.setScaleX(2f);
            tvDefeatMessage.setScaleY(2f);
            tvDefeatMessage.setRotation(0f);

            // Defeat animation - dramatic entrance with rotation
            AnimatorSet defeatAnimSet = new AnimatorSet();
            ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(tvDefeatMessage, "scaleX", 2f, 1f);
            ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(tvDefeatMessage, "scaleY", 2f, 1f);
            ObjectAnimator rotate = ObjectAnimator.ofFloat(tvDefeatMessage, "rotation", 0f, 360f);
            ObjectAnimator fadeIn = ObjectAnimator.ofFloat(tvDefeatMessage, "alpha", 0f, 1f);

            defeatAnimSet.playTogether(scaleDownX, scaleDownY, rotate, fadeIn);
            defeatAnimSet.setDuration(1500);
            defeatAnimSet.setInterpolator(new AccelerateDecelerateInterpolator());
            defeatAnimSet.start();

            // Auto-dismiss after showing defeat
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                finish(); // Return to main activity after defeat
            }, 3000);
        }
    }

    /**
     * Shows the victory chest animation with shake-to-open functionality
     */
    private void showVictoryChest() {
        if (rlChestContainer == null) return;

        // Reset chest state
        chestOpened = false;

        // Set up chest UI
        if (tvCoinRewardAmount != null) {
            tvCoinRewardAmount.setText(String.valueOf(currentBoss.getCoinReward()));
        }

        // Show chest container with entrance animation
        rlChestContainer.setVisibility(View.VISIBLE);
        rlChestContainer.setAlpha(0f);

        // Animate chest container fade in
        ObjectAnimator fadeInContainer = ObjectAnimator.ofFloat(rlChestContainer, "alpha", 0f, 1f);
        fadeInContainer.setDuration(500);
        fadeInContainer.start();

        // Animate chest entrance - scale up from small
        if (ivChest != null) {
            ivChest.setScaleX(0.3f);
            ivChest.setScaleY(0.3f);
            ivChest.setTranslationY(100f);

            AnimatorSet chestEntrance = new AnimatorSet();
            ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(ivChest, "scaleX", 0.3f, 1.1f, 1f);
            ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(ivChest, "scaleY", 0.3f, 1.1f, 1f);
            ObjectAnimator slideUp = ObjectAnimator.ofFloat(ivChest, "translationY", 100f, -20f, 0f);

            chestEntrance.playTogether(scaleUpX, scaleUpY, slideUp);
            chestEntrance.setDuration(1000);
            chestEntrance.setInterpolator(new BounceInterpolator());
            chestEntrance.start();
        }

        // Animate shake instruction with pulsing effect
        if (tvShakeInstruction != null) {
            tvShakeInstruction.setVisibility(View.VISIBLE);
            tvShakeInstruction.setAlpha(0f);
            tvShakeInstruction.setScaleX(0.8f);
            tvShakeInstruction.setScaleY(0.8f);

            // Delay showing shake instruction until chest settles
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                ObjectAnimator fadeInInstruction = ObjectAnimator.ofFloat(tvShakeInstruction, "alpha", 0f, 1f);
                fadeInInstruction.setDuration(500);
                fadeInInstruction.start();

                // Start pulsing animation for shake instruction
                startShakeInstructionPulsing();
            }, 1200);
        }

        // Set up click listener for chest container to return to menu after chest is opened
        rlChestContainer.setOnClickListener(v -> {
            if (chestOpened && llRewardsContainer != null && llRewardsContainer.getVisibility() == View.VISIBLE) {
                // User clicked after seeing rewards, return to main menu
                finish();
            }
        });
    }

    /**
     * Starts pulsing animation for the shake instruction text
     */
    private void startShakeInstructionPulsing() {
        if (tvShakeInstruction == null || chestOpened) return;

        ObjectAnimator pulseX = ObjectAnimator.ofFloat(tvShakeInstruction, "scaleX", 0.9f, 1.1f, 0.9f);
        ObjectAnimator pulseY = ObjectAnimator.ofFloat(tvShakeInstruction, "scaleY", 0.9f, 1.1f, 0.9f);
        ObjectAnimator pulse = ObjectAnimator.ofFloat(tvShakeInstruction, "alpha", 0.7f, 1f, 0.7f);

        // Set repeat properties on individual animators
        pulseX.setRepeatCount(ValueAnimator.INFINITE);
        pulseX.setRepeatMode(ValueAnimator.RESTART);
        pulseY.setRepeatCount(ValueAnimator.INFINITE);
        pulseY.setRepeatMode(ValueAnimator.RESTART);
        pulse.setRepeatCount(ValueAnimator.INFINITE);
        pulse.setRepeatMode(ValueAnimator.RESTART);

        AnimatorSet pulseSet = new AnimatorSet();
        pulseSet.playTogether(pulseX, pulseY, pulse);
        pulseSet.setDuration(1500);
        pulseSet.start();

        // Store the animator so we can stop it later
        tvShakeInstruction.setTag(pulseSet);
    }

    /**
     * Handles opening the chest when shake is detected
     */
    private void openChest() {
        if (chestOpened || rlChestContainer == null || rlChestContainer.getVisibility() != View.VISIBLE) {
            return;
        }

        Log.d(TAG, "Opening victory chest!");
        chestOpened = true;

        // Stop pulsing animation
        if (tvShakeInstruction != null && tvShakeInstruction.getTag() instanceof AnimatorSet) {
            ((AnimatorSet) tvShakeInstruction.getTag()).cancel();
        }

        // Hide shake instruction
        if (tvShakeInstruction != null) {
            ObjectAnimator fadeOutInstruction = ObjectAnimator.ofFloat(tvShakeInstruction, "alpha", 1f, 0f);
            fadeOutInstruction.setDuration(300);
            fadeOutInstruction.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    tvShakeInstruction.setVisibility(View.GONE);
                }
            });
            fadeOutInstruction.start();
        }

        // Animate chest opening
        if (ivChest != null) {
            // Chest shake animation first
            ObjectAnimator shakeX = ObjectAnimator.ofFloat(ivChest, "translationX", 0, -15, 15, -10, 10, -5, 5, 0);
            shakeX.setDuration(600);
            shakeX.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    // Change to open chest image
                    ivChest.setImageResource(R.drawable.ic_chest_open);

                    // Scale up slightly to show opening
                    ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(ivChest, "scaleX", 1f, 1.2f, 1.1f);
                    ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(ivChest, "scaleY", 1f, 1.2f, 1.1f);

                    AnimatorSet openingAnim = new AnimatorSet();
                    openingAnim.playTogether(scaleUpX, scaleUpY);
                    openingAnim.setDuration(500);
                    openingAnim.setInterpolator(new BounceInterpolator());
                    openingAnim.start();

                    // Show rewards after chest opens
                    showRewards();
                }
            });
            shakeX.start();
        } else {
            // Fallback if chest image is null
            showRewards();
        }
    }

    /**
     * Shows the reward information after chest opens
     */
    private void showRewards() {
        if (llRewardsContainer == null) return;

        // Show rewards container
        llRewardsContainer.setVisibility(View.VISIBLE);
        llRewardsContainer.setAlpha(0f);
        llRewardsContainer.setScaleX(0.5f);
        llRewardsContainer.setScaleY(0.5f);
        llRewardsContainer.setTranslationY(50f);

        // Animate rewards appearing
        AnimatorSet rewardsAnim = new AnimatorSet();
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(llRewardsContainer, "alpha", 0f, 1f);
        ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(llRewardsContainer, "scaleX", 0.5f, 1.1f, 1f);
        ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(llRewardsContainer, "scaleY", 0.5f, 1.1f, 1f);
        ObjectAnimator slideUp = ObjectAnimator.ofFloat(llRewardsContainer, "translationY", 50f, -10f, 0f);

        rewardsAnim.playTogether(fadeIn, scaleUpX, scaleUpY, slideUp);
        rewardsAnim.setDuration(800);
        rewardsAnim.setInterpolator(new BounceInterpolator());
        rewardsAnim.start();

        // Add coin reward to user's account
        addCoinRewardToUser();
    }

    /**
     * Adds the coin reward to the user's account and handles equipment drops
     */
    private void addCoinRewardToUser() {
        String userId = authService.getCurrentUserId();
        if (userId == null || currentBoss == null) return;

        int coinReward = currentBoss.getCoinReward();

        authService.getCurrentUser(user -> {
            if (user != null) {
                user.addCoins(coinReward);

                // Update user in database
                UserRepository userRepository = new UserRepository(this);
                userRepository.updateUser(user, new AuthCallback<Boolean>() {
                    @Override
                    public void onResult(Boolean success) {
                        if (success != null && success) {
                            Log.d(TAG, "Successfully added " + coinReward + " coins to user account");

                            // Roll for equipment reward after coin reward is processed
                            rollForEquipmentReward(userId);
                        } else {
                            Log.e(TAG, "Failed to update user coins in database");
                            // Still try equipment roll even if coin update fails
                            rollForEquipmentReward(userId);
                        }
                    }
                });
            }
        });
    }

    /**
     * Rolls for equipment reward using the EquipmentRewardSystem
     */
    private void rollForEquipmentReward(String userId) {
        EquipmentRewardSystem equipmentRewardSystem = new EquipmentRewardSystem(this);

        // Use the boss's specific equipment drop chance
        double bossEquipmentDropChance = currentBoss.getChanceForEquipmentReward();

        equipmentRewardSystem.rollForEquipmentReward(userId, bossEquipmentDropChance, equipment -> {
            // Update UI to show the equipment reward (or lack thereof)
            EquipmentRewardSystem.addEquipmentRewardToUI(this, llRewardsContainer, equipment);
        });
    }

    /**
     * Performs cleanup after a boss fight ends (win or lose)
     * - Unequips all user equipment and decrements remaining battles
     * - Resets boss stats to starting values
     * - Resets user power points to starting value
     */
    private void performPostFightCleanup(String userId) {
        Log.d(TAG, "Starting post-fight cleanup for user: " + userId);

        // Step 1: Reset equipment (unequip and decrement remaining battles)
        resetUserEquipment(userId, () -> {
            // Step 2: Reset boss stats to starting values
            resetBossStats(() -> {
                // Step 3: Reset user power points to starting value
                resetUserPowerPoints(userId, () -> {
                    Log.d(TAG, "Post-fight cleanup completed successfully");
                });
            });
        });
    }

    /**
     * Unequips all equipped items and decrements their remaining battles
     */
    private void resetUserEquipment(String userId, Runnable onComplete) {
        EquipmentRepository equipmentRepository = new EquipmentRepository();

        equipmentRepository.getEquipmentList(
            equipmentRepository.getUserEquipment(userId),
            new EquipmentRepository.EquipmentCallback<List<Equipment>>() {
                @Override
                public void onSuccess(List<Equipment> equipmentList) {
                    // Count equipped/active items first
                    int activeItemsCount = 0;
                    for (Equipment equipment : equipmentList) {
                        // Check both equipped and active (potions use isActive())
                        if (equipment.isEquipped() || equipment.isActive()) {
                            activeItemsCount++;
                        }
                    }

                    // If no equipment was equipped/active, proceed immediately
                    if (activeItemsCount == 0) {
                        Log.d(TAG, "No equipped/active items found, proceeding to next cleanup step");
                        onComplete.run();
                        return;
                    }

                    // Reset counters for this operation
                    equipmentUpdatesCompleted = 0;
                    totalEquipmentUpdates = activeItemsCount;

                    // Process each equipped/active item
                    for (Equipment equipment : equipmentList) {
                        // Check both equipped and active
                        if (equipment.isEquipped() || equipment.isActive()) {
                            // Use useInBattle() method which handles everything correctly
                            equipment.useInBattle();

                            Log.d(TAG, "Equipment " + equipment.getName() + " used in battle. " +
                                  "Type: " + equipment.getType() +
                                  ", isUsed: " + equipment.isUsed() +
                                  ", remainingBattles: " + equipment.getRemainingBattles());


                            // Update equipment in database
                            equipmentRepository.updateEquipment(equipment)
                                .addOnSuccessListener(result -> {
                                    // Check if all equipment has been processed
                                    checkEquipmentUpdateComplete(onComplete);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to update equipment: " + equipment.getName(), e);
                                    checkEquipmentUpdateComplete(onComplete);
                                });
                        }
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "Failed to load equipment for cleanup", e);
                    // Continue with cleanup even if equipment loading fails
                    onComplete.run();
                }
            });
    }

    // Counter to track equipment updates
    private int equipmentUpdatesCompleted = 0;
    private int totalEquipmentUpdates = 0;

    private void checkEquipmentUpdateComplete(Runnable onComplete) {
        equipmentUpdatesCompleted++;

        if (equipmentUpdatesCompleted >= totalEquipmentUpdates) {
            Log.d(TAG, "All equipment updates completed (" + equipmentUpdatesCompleted + "/" + totalEquipmentUpdates + ")");
            // Reset counters for next use
            equipmentUpdatesCompleted = 0;
            totalEquipmentUpdates = 0;
            onComplete.run();
        }
    }

    /**
     * Resets boss stats to their starting values
     */
    private void resetBossStats(Runnable onComplete) {
        if (currentBoss == null) {
            Log.w(TAG, "Cannot reset boss stats - currentBoss is null");
            onComplete.run();
            return;
        }

        Log.d(TAG, "Resetting boss stats to starting values");

        // Reset boss stats to starting values
        currentBoss.setNumberOfAttacks(currentBoss.getStartingNumberOfAttacks());
        currentBoss.setChanceTododge(currentBoss.getStartingChanceToDodge());
        currentBoss.setCoinReward(currentBoss.getStartingCoinReward());

        Log.d(TAG, "Boss stats reset - Attacks: " + currentBoss.getNumberOfAttacks() +
                  ", Dodge: " + (currentBoss.getChanceTododge() * 100) + "%" +
                  ", Coin Reward: " + currentBoss.getCoinReward());

        // Update boss in database using BossRepository directly
        BossRepository bossRepository = new BossRepository();
        bossRepository.updateBoss(currentBoss.getId(), currentBoss)
            .addOnSuccessListener(result -> {
                Log.d(TAG, "Boss stats successfully updated in database");
                onComplete.run();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to update boss stats in database", e);
                // Continue with cleanup even if boss update fails
                onComplete.run();
            });
    }

    /**
     * Resets user power points to starting value
     */
    private void resetUserPowerPoints(String userId, Runnable onComplete) {
        authService.getCurrentUser(user -> {
            if (user != null) {
                Log.d(TAG, "Resetting user power points from " + user.getPowerPoints() +
                          " to " + user.getStartingPowerPoints());

                // Reset power points to starting value
                user.setPowerPoints(user.getStartingPowerPoints());

                // Update user in database using UserRepository directly
                UserRepository userRepository = new UserRepository(this);
                userRepository.updateUser(user, new AuthCallback<Boolean>() {
                    @Override
                    public void onResult(Boolean success) {
                        if (success != null && success) {
                            Log.d(TAG, "User power points successfully reset to starting value");
                        } else {
                            Log.e(TAG, "Failed to update user power points in database");
                        }
                        onComplete.run();
                    }
                });
            } else {
                Log.e(TAG, "Cannot reset user power points - user is null");
                onComplete.run();
            }
        });
    }

    // Helper class to store equipment bonuses
    private static class EquipmentBonuses {
        int powerBonus = 0;
        int extraAttackChance = 0;
        int coinBonus = 0;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register shake detector
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            sensorManager.registerListener(shakeDetector, accelerometer, SensorManager.SENSOR_DELAY_UI);
        } else {
            Log.w(TAG, "Accelerometer not available");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister shake detector
        sensorManager.unregisterListener(shakeDetector);
    }

    /**
     * Callback method when a shake is detected
     */
    private void onShakeDetected() {
        Log.d(TAG, "Shake detected!");

        // Check if we're in victory chest mode
        if (rlChestContainer != null && rlChestContainer.getVisibility() == View.VISIBLE && !chestOpened) {
            // Open the victory chest
            openChest();
            return;
        }

        // If not in chest mode, this could be used for other shake-based abilities
        // For example, you could implement a special attack or power-up during combat
        String userId = authService.getCurrentUserId();
        if (userId != null && currentBoss != null && currentBoss.isAlive()) {
            Log.d(TAG, "Shake detected during combat - could implement special ability here");
            // You can add special combat abilities triggered by shake here if desired
        }
    }
}
