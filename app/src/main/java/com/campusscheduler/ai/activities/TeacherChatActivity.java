package com.campusscheduler.ai.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.campusscheduler.ai.R;
import com.campusscheduler.ai.engine.AcademicChatbotEngine;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.utils.LocalAuthManager;

public class TeacherChatActivity extends AppCompatActivity {

    private android.widget.ImageButton btnBack;
    private TextView tvChatLog;
    private EditText etChatMessage;
    private Button btnSendMessage;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_chat);

        btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        currentUser = LocalAuthManager.getCurrentLocalUser(this);
        String safeCollegeId = LocalAuthManager.getSafeCollegeId(this, getIntent());
        String teacherUid = getIntent().getStringExtra("teacherUid");
        String teacherName = getIntent().getStringExtra("teacherName");

        if (currentUser == null) {
            currentUser = new User();
            currentUser.setRole(User.ROLE_TEACHER);
            currentUser.setName("Dr.Pushpa Mohan");
        }
        if (currentUser.getCollegeId() == null || currentUser.getCollegeId().trim().isEmpty()) {
            currentUser.setCollegeId(safeCollegeId);
        }
        if (teacherUid != null && !teacherUid.trim().isEmpty()) {
            currentUser.setUid(teacherUid.trim());
        }
        if (teacherName != null && !teacherName.trim().isEmpty()) {
            currentUser.setName(teacherName.trim());
        }
        LocalAuthManager.saveUserSession(this, currentUser);

        tvChatLog = findViewById(R.id.tvChatLog);
        etChatMessage = findViewById(R.id.etChatMessage);
        btnSendMessage = findViewById(R.id.btnSendMessage);

        tvChatLog.setText("AI Academic Assistant: Hello Professor! Ask me about room availability, faculty schedules, or student attendance.\n\n");

        btnSendMessage.setOnClickListener(v -> {
            String msg = etChatMessage.getText().toString().trim();
            if (!msg.isEmpty()) {
                tvChatLog.append("You: " + msg + "\n");
                etChatMessage.setText("");
                AcademicChatbotEngine.processQuery(msg, currentUser, response -> {
                    tvChatLog.append("AI Assistant: " + response + "\n\n");
                });
            }
        });
    }
}
