package com.example.rpgapp.ui;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.rpgapp.R;
import com.example.rpgapp.model.Boss;
import com.example.rpgapp.service.AuthService;
import com.example.rpgapp.service.BossService;

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

    // Animation components
    private ImageView ivBossImage;
    private TextView tvDamageIndicator;
    private TextView tvMissIndicator;
    private TextView tvVictoryMessage;
    private TextView tvDefeatMessage;
    private View vAttackFlash;

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

        // Animation components
        ivBossImage = findViewById(R.id.iv_boss_image);
        tvDamageIndicator = findViewById(R.id.tv_damage_indicator);
        tvMissIndicator = findViewById(R.id.tv_miss_indicator);
        tvVictoryMessage = findViewById(R.id.tv_victory_message);
        tvDefeatMessage = findViewById(R.id.tv_defeat_message);
        vAttackFlash = findViewById(R.id.v_attack_flash);
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

        // Get user's actual power points for damage calculation
        authService.getCurrentUser(user -> {
            if (user != null) {
                int userPowerPoints = user.getPowerPoints();
                Log.d(TAG, "Using user's power points for damage: " + userPowerPoints);

                // Perform the attack with user's actual power points
                performAttack(userId, userPowerPoints);
            } else {
                Log.e(TAG, "Failed to get current user for power points");
                Toast.makeText(this, "Failed to get user stats", Toast.LENGTH_SHORT).show();
                btnAttack.setEnabled(true); // Re-enable button on failure
            }
        });
    }

    private void performAttack(String userId, int userPowerPoints) {
        bossService.attackBoss(bossId, userId, userPowerPoints)
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
        if (tvVictoryMessage != null) {
            tvVictoryMessage.setText("VICTORY!");
            tvVictoryMessage.setVisibility(View.VISIBLE);
            tvVictoryMessage.setAlpha(0f);
            tvVictoryMessage.setScaleX(0.5f);
            tvVictoryMessage.setScaleY(0.5f);
            tvVictoryMessage.setTranslationY(200f);

            // Victory animation - scale up and slide in from bottom
            AnimatorSet victoryAnimSet = new AnimatorSet();
            ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(tvVictoryMessage, "scaleX", 0.5f, 1.2f, 1f);
            ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(tvVictoryMessage, "scaleY", 0.5f, 1.2f, 1f);
            ObjectAnimator slideUp = ObjectAnimator.ofFloat(tvVictoryMessage, "translationY", 200f, -50f, 0f);
            ObjectAnimator fadeIn = ObjectAnimator.ofFloat(tvVictoryMessage, "alpha", 0f, 1f);

            victoryAnimSet.playTogether(scaleUpX, scaleUpY, slideUp, fadeIn);
            victoryAnimSet.setDuration(1500);
            victoryAnimSet.setInterpolator(new BounceInterpolator());
            victoryAnimSet.start();

            // Auto-dismiss after showing victory
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                finish(); // Return to main activity after victory
            }, 3000);
        }
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
}
