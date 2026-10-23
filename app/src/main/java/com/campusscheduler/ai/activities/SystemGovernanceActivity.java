package com.campusscheduler.ai.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.campusscheduler.ai.R;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.utils.LocalAuthManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class SystemGovernanceActivity extends AppCompatActivity {

    private ImageButton btnBack;

    private TextView tvOwnerAccessStatus;

    private TextView tvFirebaseAuthStatus;
    private TextView tvFirestoreStatus;

    private TextView tvCollegesStatus;
    private TextView tvUsersStatus;
    private TextView tvPlatformUpdatesStatus;
    private TextView tvTimetablesStatus;

    private TextView tvSecurityRulesStatus;
    private TextView tvOverallStatus;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    private boolean authCheckComplete = false;
    private boolean firestoreCheckComplete = false;

    private boolean authAvailable = false;
    private boolean firestoreAvailable = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // =========================================================
        // STATUS / NAVIGATION BAR
        // =========================================================

        Window window = getWindow();
        window.setStatusBarColor(Color.rgb(13, 71, 161));
        window.setNavigationBarColor(Color.BLACK);

        setContentView(R.layout.activity_system_governance);

        // =========================================================
        // INITIALIZE VIEWS
        // =========================================================

        btnBack = findViewById(R.id.btnBack);

        tvOwnerAccessStatus = findViewById(R.id.tvOwnerAccessStatus);

        tvFirebaseAuthStatus = findViewById(R.id.tvFirebaseAuthStatus);
        tvFirestoreStatus = findViewById(R.id.tvFirestoreStatus);

        tvCollegesStatus = findViewById(R.id.tvCollegesStatus);
        tvUsersStatus = findViewById(R.id.tvUsersStatus);
        tvPlatformUpdatesStatus = findViewById(R.id.tvPlatformUpdatesStatus);
        tvTimetablesStatus = findViewById(R.id.tvTimetablesStatus);

        tvSecurityRulesStatus = findViewById(R.id.tvSecurityRulesStatus);

        tvOverallStatus = findViewById(R.id.tvOverallStatus);

        // =========================================================
        // BACK BUTTON
        // =========================================================

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // =========================================================
        // FIREBASE
        // =========================================================

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // =========================================================
        // OWNER ACCESS CHECK
        // =========================================================

        checkOwnerAccess();

        // =========================================================
        // FIREBASE AUTH CHECK
        // =========================================================

        checkFirebaseAuthentication();

        // =========================================================
        // FIRESTORE CHECK
        // =========================================================

        checkFirestoreConnection();

        // =========================================================
        // DATA COLLECTION CHECKS
        // =========================================================

        checkCollection(
                "colleges",
                tvCollegesStatus
        );

        checkCollection(
                "users",
                tvUsersStatus
        );

        checkCollection(
                "platformUpdates",
                tvPlatformUpdatesStatus
        );

        checkCollection(
                "timetables",
                tvTimetablesStatus
        );

        // =========================================================
        // SECURITY RULE STATUS
        // =========================================================

        if (tvSecurityRulesStatus != null) {
            tvSecurityRulesStatus.setText("Verification Required");
            tvSecurityRulesStatus.setTextColor(Color.rgb(239, 108, 0));
        }
    }

    // =============================================================
    // OWNER ACCESS
    // =============================================================

    private void checkOwnerAccess() {

        User currentUser =
                LocalAuthManager.getCurrentLocalUser(this);

        if (currentUser == null) {

            if (tvOwnerAccessStatus != null) {
                tvOwnerAccessStatus.setText(
                        "No local owner session"
                );
                tvOwnerAccessStatus.setTextColor(
                        Color.rgb(198, 40, 40)
                );
            }

            return;
        }

        String role = currentUser.getRole();

        if (User.ROLE_OFFICIAL_OWNER.equals(role)) {

            if (tvOwnerAccessStatus != null) {
                tvOwnerAccessStatus.setText(
                        "Authorized Official Owner"
                );
                tvOwnerAccessStatus.setTextColor(
                        Color.rgb(46, 125, 50)
                );
            }

        } else {

            if (tvOwnerAccessStatus != null) {
                tvOwnerAccessStatus.setText(
                        "Owner role verification required"
                );
                tvOwnerAccessStatus.setTextColor(
                        Color.rgb(239, 108, 0)
                );
            }
        }
    }

    // =============================================================
    // FIREBASE AUTHENTICATION CHECK
    // =============================================================

    private void checkFirebaseAuthentication() {

        if (firebaseAuth == null) {

            authCheckComplete = true;
            authAvailable = false;

            updateAuthStatus(
                    "Unavailable",
                    Color.rgb(198, 40, 40)
            );

            updateOverallStatus();

            return;
        }

        if (firebaseAuth.getCurrentUser() != null) {

            authCheckComplete = true;
            authAvailable = true;

            updateAuthStatus(
                    "Connected",
                    Color.rgb(46, 125, 50)
            );

        } else {

            authCheckComplete = true;
            authAvailable = false;

            updateAuthStatus(
                    "No Firebase User Session",
                    Color.rgb(239, 108, 0)
            );
        }

        updateOverallStatus();
    }

    private void updateAuthStatus(
            String message,
            int color
    ) {

        if (tvFirebaseAuthStatus != null) {
            tvFirebaseAuthStatus.setText(message);
            tvFirebaseAuthStatus.setTextColor(color);
        }
    }

    // =============================================================
    // FIRESTORE CONNECTION CHECK
    // =============================================================

    private void checkFirestoreConnection() {

        if (firestore == null) {

            firestoreCheckComplete = true;
            firestoreAvailable = false;

            updateFirestoreStatus(
                    "Unavailable",
                    Color.rgb(198, 40, 40)
            );

            updateOverallStatus();

            return;
        }

        firestore.collection("users")
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {

                    firestoreCheckComplete = true;
                    firestoreAvailable = true;

                    updateFirestoreStatus(
                            "Connected",
                            Color.rgb(46, 125, 50)
                    );

                    updateOverallStatus();
                })
                .addOnFailureListener(error -> {

                    firestoreCheckComplete = true;
                    firestoreAvailable = false;

                    updateFirestoreStatus(
                            "Connection / Rules Error",
                            Color.rgb(198, 40, 40)
                    );

                    updateOverallStatus();
                });
    }

    private void updateFirestoreStatus(
            String message,
            int color
    ) {

        if (tvFirestoreStatus != null) {
            tvFirestoreStatus.setText(message);
            tvFirestoreStatus.setTextColor(color);
        }
    }

    // =============================================================
    // COLLECTION CHECK
    // =============================================================

    private void checkCollection(
            String collectionName,
            TextView statusView
    ) {

        if (firestore == null || statusView == null) {
            return;
        }

        firestore.collection(collectionName)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (snapshot != null && !snapshot.isEmpty()) {

                        statusView.setText("Available");
                        statusView.setTextColor(
                                Color.rgb(46, 125, 50)
                        );

                    } else {

                        statusView.setText("No Records");
                        statusView.setTextColor(
                                Color.rgb(239, 108, 0)
                        );
                    }
                })
                .addOnFailureListener(error -> {

                    statusView.setText("Read Failed");
                    statusView.setTextColor(
                            Color.rgb(198, 40, 40)
                    );
                });
    }

    // =============================================================
    // OVERALL STATUS
    // =============================================================

    private void updateOverallStatus() {

        if (tvOverallStatus == null) {
            return;
        }

        if (!authCheckComplete || !firestoreCheckComplete) {

            tvOverallStatus.setText(
                    "Checking platform services..."
            );

            tvOverallStatus.setTextColor(
                    Color.rgb(21, 101, 192)
            );

            return;
        }

        if (authAvailable && firestoreAvailable) {

            tvOverallStatus.setText(
                    "Platform services reachable"
            );

            tvOverallStatus.setTextColor(
                    Color.rgb(46, 125, 50)
            );

        } else {

            tvOverallStatus.setText(
                    "Platform verification incomplete"
            );

            tvOverallStatus.setTextColor(
                    Color.rgb(239, 108, 0)
            );
        }
    }

    // =============================================================
    // BACK
    // =============================================================

    @Override
    public void onBackPressed() {
        finish();
    }
}