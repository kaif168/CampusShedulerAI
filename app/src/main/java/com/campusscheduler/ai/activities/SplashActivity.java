package com.campusscheduler.ai.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import com.campusscheduler.ai.R;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.utils.LocalAuthManager;

public class SplashActivity extends AppCompatActivity {
    private static final String TAG = "SplashActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                LocalAuthManager auth = new LocalAuthManager(this);
                if (auth.isLoggedIn()) {
                    User user = auth.getCurrentUser();
                    navigateBasedOnRole(user);
                } else {
                    navigateToLogin();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking session in SplashActivity: " + e.getMessage(), e);
                navigateToLogin();
            }
        }, 2000);
    }

    private void navigateToLogin() {
        try {
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
        } catch (Exception e) {
            Log.e(TAG, "Failed launching LoginActivity: " + e.getMessage(), e);
        }
        finish();
    }

    private void navigateBasedOnRole(User user) {
        if (user == null) {
            navigateToLogin();
            return;
        }

        String role = user.getRole() != null ? user.getRole().toUpperCase() : "";
        try {
            switch (role) {
                case User.ROLE_OFFICIAL_OWNER:
                    startActivity(new Intent(this, OfficialOwnerDashboardActivity.class));
                    break;
                case User.ROLE_COLLEGE_ADMIN:
                case "ADMIN":
                case "HOD":
                    startActivity(new Intent(this, AdminDashboardActivity.class));
                    break;
                case User.ROLE_TEACHER:
                case "FACULTY":
                    startActivity(new Intent(this, TeacherDashboardActivity.class));
                    break;
                case User.ROLE_STUDENT:
                default:
                    startActivity(new Intent(this, StudentDashboardActivity.class));
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed navigation based on role: " + e.getMessage(), e);
            navigateToLogin();
        }
        finish();
    }
}
