package com.campusscheduler.ai.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.campusscheduler.ai.R;
import com.campusscheduler.ai.engine.ClassDetectionEngine;
import com.campusscheduler.ai.models.ClassSession;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.utils.FirebaseHelper;
import com.campusscheduler.ai.utils.LocalAuthManager;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class StudentClassDetectionActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvStatusHeader, tvStatusExplanation, tvSubjectName, tvRoomNumber, tvTimeSlot, tvTeacherName;
    private ProgressBar progressBar;
    private User currentUser;
    private List<ClassSession> todaySessions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_class_detection);

        currentUser = LocalAuthManager.getCurrentLocalUser(this);
        String safeCollegeId = LocalAuthManager.getSafeCollegeId(this, getIntent());
        if (currentUser == null) {
            currentUser = new User();
            currentUser.setCollegeId(safeCollegeId);
            currentUser.setDepartmentId("DEPT_CSE");
            currentUser.setRole("student");
        } else if (currentUser.getCollegeId() == null || currentUser.getCollegeId().trim().isEmpty()) {
            currentUser.setCollegeId(safeCollegeId);
        }

        btnBack = findViewById(R.id.btnBack);
        tvStatusHeader = findViewById(R.id.tvStatusHeader);
        tvStatusExplanation = findViewById(R.id.tvStatusExplanation);
        tvSubjectName = findViewById(R.id.tvSubjectName);
        tvRoomNumber = findViewById(R.id.tvRoomNumber);
        tvTimeSlot = findViewById(R.id.tvTimeSlot);
        tvTeacherName = findViewById(R.id.tvTeacherName);
        progressBar = findViewById(R.id.progressBar);

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        loadClassDetectionData();
    }

    private void loadClassDetectionData() {
        String collegeId = currentUser != null && currentUser.getCollegeId() != null ?
                currentUser.getCollegeId() : LocalAuthManager.getSafeCollegeId(this, getIntent());

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        FirebaseHelper.getClassSessionsRef()
                .whereEqualTo("collegeId", collegeId)
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    todaySessions.clear();

                    if (querySnapshots != null && !querySnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : querySnapshots) {
                            ClassSession cs = doc.toObject(ClassSession.class);
                            if (cs != null) todaySessions.add(cs);
                        }
                    }

                    ClassDetectionEngine.DetectionResult result = ClassDetectionEngine.detectCurrentClass(todaySessions);
                    showDetectionResult(result);
                })
                .addOnFailureListener(e -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    showDetectionResult(new ClassDetectionEngine.DetectionResult(
                            ClassDetectionEngine.ClassStatus.NO_CLASS, null, "Unable to query class sessions: " + e.getMessage()
                    ));
                });
    }

    private void showDetectionResult(ClassDetectionEngine.DetectionResult result) {
        if (result == null) return;

        if (tvStatusHeader != null) {
            tvStatusHeader.setText("Class Detection: " + result.getStatus().name());
        }

        if (tvStatusExplanation != null) {
            tvStatusExplanation.setText(result.getExplanation());
        }

        ClassSession session = result.getSession();
        if (session != null) {
            if (tvSubjectName != null) tvSubjectName.setText(session.getSubjectName());
            if (tvRoomNumber != null) tvRoomNumber.setText("Room: " + (session.getRoomNumber() != null ? session.getRoomNumber() : "Assigned Hall"));
            if (tvTimeSlot != null) tvTimeSlot.setText("Time: " + (session.getStartTime() != null ? session.getStartTime() : "TBD"));
            if (tvTeacherName != null) tvTeacherName.setText("Faculty: " + (session.getTeacherName() != null ? session.getTeacherName() : "Faculty Member"));
        } else {
            if (tvSubjectName != null) tvSubjectName.setText("No Session Active");
            if (tvRoomNumber != null) tvRoomNumber.setText("Room: --");
            if (tvTimeSlot != null) tvTimeSlot.setText("Time: --");
            if (tvTeacherName != null) tvTeacherName.setText("Faculty: --");
        }
    }
}
