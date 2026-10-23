package com.campusscheduler.ai.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.campusscheduler.ai.R;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.utils.FirebaseHelper;
import com.campusscheduler.ai.utils.LocalAuthManager;
import com.campusscheduler.ai.utils.UIUtils;
import com.google.android.material.navigation.NavigationView;

public class OfficialOwnerDashboardActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navView;

    private ImageButton btnMenu;
    private ImageButton btnLogout;

    private CardView btnCollegeMgmt;
    private CardView btnPlatformUpdates;
    private CardView btnOwnerAnalytics;
    private CardView btnOwnerAccess;
    private CardView btnSystemGovernance;

    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();

        window.setStatusBarColor(
                Color.rgb(13, 71, 161)
        );

        window.setNavigationBarColor(
                Color.BLACK
        );

        setContentView(
                R.layout.activity_official_owner_dashboard
        );

        currentUser =
                LocalAuthManager.getCurrentLocalUser(this);

        /*
         * ============================================================
         * OWNER SESSION PROTECTION
         * ============================================================
         */

        if (currentUser == null
                || !User.ROLE_OFFICIAL_OWNER.equals(
                currentUser.getRole())) {

            UIUtils.showToast(
                    this,
                    "Official Owner access required."
            );

            Intent intent =
                    new Intent(
                            this,
                            LoginActivity.class
                    );

            intent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK
            );

            startActivity(intent);
            finish();

            return;
        }

        /*
         * ============================================================
         * FIND VIEWS
         * ============================================================
         */

        drawerLayout =
                findViewById(R.id.drawerLayout);

        navView =
                findViewById(R.id.navView);

        btnMenu =
                findViewById(R.id.btnMenu);

        btnLogout =
                findViewById(R.id.btnLogout);

        btnCollegeMgmt =
                findViewById(R.id.btnCollegeMgmt);

        btnPlatformUpdates =
                findViewById(R.id.btnPlatformUpdates);

        btnOwnerAnalytics =
                findViewById(R.id.btnOwnerAnalytics);

        btnOwnerAccess =
                findViewById(R.id.btnOwnerAccess);

        btnSystemGovernance =
                findViewById(R.id.btnSystemGovernance);

        /*
         * ============================================================
         * DRAWER
         * ============================================================
         */

        if (navView != null) {

            navView.setNavigationItemSelectedListener(this);

            setupHeaderView();
        }

        /*
         * ============================================================
         * MENU BUTTON
         * ============================================================
         */

        if (btnMenu != null) {

            btnMenu.setOnClickListener(v -> {

                if (drawerLayout != null) {

                    drawerLayout.openDrawer(
                            GravityCompat.START
                    );
                }
            });
        }

        /*
         * ============================================================
         * LOGOUT BUTTON
         * ============================================================
         */

        if (btnLogout != null) {

            btnLogout.setOnClickListener(
                    v -> performLogout()
            );
        }

        /*
         * ============================================================
         * COLLEGE MANAGEMENT
         * ============================================================
         */

        if (btnCollegeMgmt != null) {

            btnCollegeMgmt.setOnClickListener(v -> {

                startActivity(
                        new Intent(
                                this,
                                CollegeManagementActivity.class
                        )
                );
            });
        }

        /*
         * ============================================================
         * PLATFORM UPDATES
         * ============================================================
         */

        if (btnPlatformUpdates != null) {

            btnPlatformUpdates.setOnClickListener(v -> {

                startActivity(
                        new Intent(
                                this,
                                PlatformUpdatesActivity.class
                        )
                );
            });
        }

        /*
         * ============================================================
         * PLATFORM ANALYTICS
         * ============================================================
         */

        if (btnOwnerAnalytics != null) {

            btnOwnerAnalytics.setOnClickListener(v -> {

                startActivity(
                        new Intent(
                                this,
                                OwnerAnalyticsActivity.class
                        )
                );
            });
        }

        /*
         * ============================================================
         * OFFICIAL OWNER ACCESS
         * ============================================================
         */

        if (btnOwnerAccess != null) {

            btnOwnerAccess.setOnClickListener(v -> {

                startActivity(
                        new Intent(
                                this,
                                OwnerAccessManagementActivity.class
                        )
                );
            });
        }

        /*
         * ============================================================
         * SYSTEM GOVERNANCE
         * ============================================================
         */

        if (btnSystemGovernance != null) {

            btnSystemGovernance.setOnClickListener(v -> {

                startActivity(
                        new Intent(
                                this,
                                SystemGovernanceActivity.class
                        )
                );
            });
        }
    }

    /*
     * ================================================================
     * OWNER DRAWER HEADER
     * ================================================================
     */

    private void setupHeaderView() {

        if (navView == null) {
            return;
        }

        View headerView =
                navView.getHeaderView(0);

        if (headerView == null) {
            return;
        }

        TextView tvNavOwnerName =
                headerView.findViewById(
                        R.id.tvNavOwnerName
                );

        TextView tvNavOwnerScope =
                headerView.findViewById(
                        R.id.tvNavOwnerScope
                );

        TextView tvNavOwnerSystem =
                headerView.findViewById(
                        R.id.tvNavOwnerSystem
                );

        if (tvNavOwnerName != null) {

            if (currentUser != null
                    && currentUser.getName() != null
                    && !currentUser.getName()
                    .trim()
                    .isEmpty()) {

                tvNavOwnerName.setText(
                        currentUser.getName()
                );

            } else {

                tvNavOwnerName.setText(
                        "Official Platform Owner"
                );
            }
        }

        if (tvNavOwnerScope != null) {

            tvNavOwnerScope.setText(
                    "Global Platform Governance"
            );
        }

        if (tvNavOwnerSystem != null) {

            tvNavOwnerSystem.setText(
                    "CampusSchedulerAI Master Account"
            );
        }
    }

    /*
     * ================================================================
     * NAVIGATION DRAWER
     * ================================================================
     */

    @Override
    public boolean onNavigationItemSelected(
            @NonNull MenuItem item) {

        int itemId =
                item.getItemId();

        if (drawerLayout != null) {

            drawerLayout.closeDrawer(
                    GravityCompat.START
            );
        }

        if (itemId == R.id.nav_owner_dashboard) {

            // Already on dashboard.

        } else if (itemId == R.id.nav_college_mgmt) {

            startActivity(
                    new Intent(
                            this,
                            CollegeManagementActivity.class
                    )
            );

        } else if (itemId == R.id.nav_platform_updates) {

            startActivity(
                    new Intent(
                            this,
                            PlatformUpdatesActivity.class
                    )
            );

        } else if (itemId == R.id.nav_owner_analytics) {

            startActivity(
                    new Intent(
                            this,
                            OwnerAnalyticsActivity.class
                    )
            );

        } else if (itemId == R.id.nav_support) {

            startActivity(
                    new Intent(
                            this,
                            HelpCenterActivity.class
                    )
            );

        } else if (itemId == R.id.nav_settings) {

            startActivity(
                    new Intent(
                            this,
                            SettingsActivity.class
                    )
            );

        } else if (itemId == R.id.nav_logout) {

            performLogout();
        }

        return true;
    }

    /*
     * ================================================================
     * LOGOUT
     * ================================================================
     */

    private void performLogout() {

        FirebaseHelper.getAuth().signOut();

        LocalAuthManager.logoutLocalUser(this);

        UIUtils.showToast(
                this,
                "Logged out."
        );

        Intent intent =
                new Intent(
                        this,
                        LoginActivity.class
                );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);

        finish();
    }

    /*
     * ================================================================
     * BACK BUTTON
     * ================================================================
     */

    @Override
    public void onBackPressed() {

        if (drawerLayout != null
                && drawerLayout.isDrawerOpen(
                GravityCompat.START)) {

            drawerLayout.closeDrawer(
                    GravityCompat.START
            );

            return;
        }

        super.onBackPressed();
    }
}