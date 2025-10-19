package com.example.rpgapp.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.rpgapp.R;
import com.example.rpgapp.callback.AuthCallback;
import com.example.rpgapp.repository.FriendshipRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import java.util.List;

public class QRScannerActivity extends AppCompatActivity {
    private static final String TAG = "QRScannerActivity";
    private static final int CAMERA_PERMISSION_REQUEST = 100;

    private DecoratedBarcodeView barcodeView;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FriendshipRepository friendshipRepository;
    private String currentUserId;
    private boolean isScanning = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scanner);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        friendshipRepository = new FriendshipRepository();

        if (mAuth.getCurrentUser() == null) {
            finish();
            return;
        }

        currentUserId = mAuth.getCurrentUser().getUid();
        barcodeView = findViewById(R.id.barcode_scanner);

        // Check camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST);
        } else {
            startScanning();
        }
    }

    private void startScanning() {
        barcodeView.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (result != null && isScanning) {
                    isScanning = false;
                    handleScannedCode(result.getText());
                }
            }

            @Override
            public void possibleResultPoints(List<ResultPoint> resultPoints) {
                // Optional: handle possible result points
            }
        });
    }

    private void handleScannedCode(String scannedUserId) {
        Log.d(TAG, "Scanned user ID: " + scannedUserId);

        // Check if scanning own QR code
        if (scannedUserId.equals(currentUserId)) {
            Toast.makeText(this, "You cannot add yourself as a friend!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Check if user exists and send friend request
        db.collection("users").document(scannedUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String username = documentSnapshot.getString("username");
                        sendFriendRequest(scannedUserId, username);
                    } else {
                        Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching user", e);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void sendFriendRequest(String targetUserId, String username) {
        friendshipRepository.sendFriendRequest(currentUserId, targetUserId, new AuthCallback<Boolean>() {
            @Override
            public void onResult(Boolean success) {
                if (success) {
                    Toast.makeText(QRScannerActivity.this,
                            "Friend request sent to " + username,
                            Toast.LENGTH_LONG).show();

                    // Open the user's profile
                    Intent intent = new Intent(QRScannerActivity.this, ProfileActivity.class);
                    intent.putExtra(ProfileActivity.EXTRA_USER_ID, targetUserId);
                    startActivity(intent);
                } else {
                    Toast.makeText(QRScannerActivity.this,
                            "Failed to send friend request. You may already be friends or have a pending request.",
                            Toast.LENGTH_LONG).show();
                }
                finish();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanning();
            } else {
                Toast.makeText(this, "Camera permission is required to scan QR codes",
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (barcodeView != null) {
            barcodeView.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (barcodeView != null) {
            barcodeView.pause();
        }
    }
}

