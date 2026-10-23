package com.campusscheduler.ai.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.utils.LocalAuthManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        LocalAuthManager auth = new LocalAuthManager(this);
        if (auth.isLoggedIn()) {
            User user = auth.getCurrentUser();
            switch (user.getRole()) {
                case User.ROLE_OFFICIAL_OWNER:
                    startActivity(new Intent(this, OfficialOwnerDashboardActivity.class));
                    break;
                case User.ROLE_COLLEGE_ADMIN:
                    startActivity(new Intent(this, AdminDashboardActivity.class));
                    break;
                case User.ROLE_TEACHER:
                    startActivity(new Intent(this, TeacherDashboardActivity.class));
                    break;
                case User.ROLE_STUDENT:
                default:
                    startActivity(new Intent(this, StudentDashboardActivity.class));
                    break;
            }
        } else {
            startActivity(new Intent(this, SplashActivity.class));
        }
        finish();
    }
}
