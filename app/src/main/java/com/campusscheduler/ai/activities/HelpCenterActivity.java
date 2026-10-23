package com.campusscheduler.ai.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Window;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

import com.campusscheduler.ai.R;
import com.google.android.material.card.MaterialCardView;

public class HelpCenterActivity extends AppCompatActivity {

    private ImageButton btnBack;

    private MaterialCardView cardCollegeManagement;
    private MaterialCardView cardPlatformUpdates;
    private MaterialCardView cardPlatformAnalytics;
    private MaterialCardView cardSecurityAccount;
    private MaterialCardView cardOwnerAccess;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ============================================================
        // OWNER BLUE SYSTEM BAR
        // ============================================================

        Window window = getWindow();

        window.setStatusBarColor(
                Color.rgb(13, 71, 161)
        );

        window.setNavigationBarColor(
                Color.BLACK
        );

        // ============================================================
        // LAYOUT
        // ============================================================

        setContentView(R.layout.activity_help_center);

        // ============================================================
        // FIND VIEWS
        // ============================================================

        btnBack = findViewById(R.id.btnBack);

        cardCollegeManagement =
                findViewById(R.id.cardCollegeManagement);

        cardPlatformUpdates =
                findViewById(R.id.cardPlatformUpdates);

        cardPlatformAnalytics =
                findViewById(R.id.cardPlatformAnalytics);

        cardSecurityAccount =
                findViewById(R.id.cardSecurityAccount);

        cardOwnerAccess =
                findViewById(R.id.cardOwnerAccess);

        // ============================================================
        // BACK
        // ============================================================

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // ============================================================
        // COLLEGE MANAGEMENT
        // ============================================================

        if (cardCollegeManagement != null) {

            cardCollegeManagement.setOnClickListener(v -> {

                Intent intent = new Intent(
                        HelpCenterActivity.this,
                        CollegeManagementActivity.class
                );

                startActivity(intent);
            });
        }

        // ============================================================
        // PLATFORM UPDATES
        // ============================================================

        if (cardPlatformUpdates != null) {

            cardPlatformUpdates.setOnClickListener(v -> {

                Intent intent = new Intent(
                        HelpCenterActivity.this,
                        PlatformUpdatesActivity.class
                );

                startActivity(intent);
            });
        }

        // ============================================================
        // PLATFORM ANALYTICS
        // ============================================================

        if (cardPlatformAnalytics != null) {

            cardPlatformAnalytics.setOnClickListener(v -> {

                Intent intent = new Intent(
                        HelpCenterActivity.this,
                        OwnerAnalyticsActivity.class
                );

                startActivity(intent);
            });
        }

        // ============================================================
        // SECURITY & ACCOUNT
        // ============================================================

        if (cardSecurityAccount != null) {

            cardSecurityAccount.setOnClickListener(v -> {

                Intent intent = new Intent(
                        HelpCenterActivity.this,
                        SettingsActivity.class
                );

                startActivity(intent);
            });
        }

        // ============================================================
        // OFFICIAL OWNER ACCESS
        // ============================================================

        if (cardOwnerAccess != null) {

            cardOwnerAccess.setOnClickListener(v -> {

                Intent intent = new Intent(
                        HelpCenterActivity.this,
                        OwnerAccessManagementActivity.class
                );

                startActivity(intent);
            });
        }
    }

    // ============================================================
    // SYSTEM BACK
    // ============================================================

    @Override
    public void onBackPressed() {
        finish();
    }
}