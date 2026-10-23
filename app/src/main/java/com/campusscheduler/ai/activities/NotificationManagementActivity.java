package com.campusscheduler.ai.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.campusscheduler.ai.R;
import com.campusscheduler.ai.models.NotificationItem;
import com.campusscheduler.ai.utils.FirebaseHelper;
import com.campusscheduler.ai.utils.LocalAuthManager;

public class NotificationManagementActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private EditText etNotifTitle, etNotifMessage;
    private Button btnSendFcmBroadcast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_management);

        btnBack = findViewById(R.id.btnBack);
        etNotifTitle = findViewById(R.id.etNotifTitle);
        etNotifMessage = findViewById(R.id.etNotifMessage);
        btnSendFcmBroadcast = findViewById(R.id.btnSendFcmBroadcast);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        btnSendFcmBroadcast.setOnClickListener(v -> {
            String title = etNotifTitle.getText().toString().trim();
            String msg = etNotifMessage.getText().toString().trim();

            if (title.isEmpty() || msg.isEmpty()) {
                Toast.makeText(this, "Title and message required", Toast.LENGTH_SHORT).show();
                return;
            }

            String collegeId = LocalAuthManager.getSafeCollegeId(this, getIntent());
            String notifId = "NOTIF_" + System.currentTimeMillis();

            NotificationItem item = new NotificationItem();
            item.setId(notifId);
            item.setCollegeId(collegeId);
            item.setTitle(title);
            item.setMessage(msg);
            item.setType("ANNOUNCEMENT");
            item.setTimestamp(System.currentTimeMillis());

            FirebaseHelper.getFirestore().collection("notifications").document(notifId).set(item)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Announcement dispatched and published to college!", Toast.LENGTH_LONG).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to broadcast notification: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }
}
