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

public class TeacherClassDetectionActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvStatusHeader, tvStatusExplanation, tvSubjectName, tvRoomNumber, tvTimeSlot, tvSectionName;
    private ProgressBar progressBar;
    private User currentUser;
    private List<ClassSession> todaySessions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_class_detection);

        currentUser = LocalAuthManager.getCurrentLocalUser(this);
        String safeCollegeId = LocalAuthManager.getSafeCollegeId(this, getIntent());
        String teacherUid = getIntent().getStringExtra("teacherUid");
        if (currentUser == null) {
            currentUser = new User();
            currentUser.setCollegeId(safeCollegeId);
            currentUser.setDepartmentId("DEPT_CSE");
            currentUser.setName("Faculty");
            if (teacherUid != null) {
                currentUser.setUid(teacherUid);
            } else if (FirebaseHelper.getAuth().getCurrentUser() != null) {
                currentUser.setUid(FirebaseHelper.getAuth().getCurrentUser().getUid());
            }
        } else {
            if (currentUser.getCollegeId() == null || currentUser.getCollegeId().trim().isEmpty()) {
                currentUser.setCollegeId(safeCollegeId);
            }
            if ((currentUser.getUid() == null || currentUser.getUid().trim().isEmpty()) && teacherUid != null) {
                currentUser.setUid(teacherUid);
            }
        }

        btnBack = findViewById(R.id.btnBack);
        tvStatusHeader = findViewById(R.id.tvStatusHeader);
        tvStatusExplanation = findViewById(R.id.tvStatusExplanation);
        tvSubjectName = findViewById(R.id.tvSubjectName);
        tvRoomNumber = findViewById(R.id.tvRoomNumber);
        tvTimeSlot = findViewById(R.id.tvTimeSlot);
        tvSectionName = findViewById(R.id.tvSectionName);
        progressBar = findViewById(R.id.progressBar);

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        loadTeacherClassDetectionData();
    }

    private void loadTeacherClassDetectionData() {
        String collegeId = currentUser != null && currentUser.getCollegeId() != null ?
                currentUser.getCollegeId() : LocalAuthManager.getSafeCollegeId(this, getIntent());
        String teacherUid = currentUser != null ? currentUser.getUid() : null;
        if (teacherUid == null || teacherUid.isEmpty()) {
            teacherUid = getIntent().getStringExtra("teacherUid");
        }
        if (teacherUid == null && FirebaseHelper.getAuth().getCurrentUser() != null) {
            teacherUid = FirebaseHelper.getAuth().getCurrentUser().getUid();
        }

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        final String finalTeacherUid = teacherUid;
        FirebaseHelper.getClassSessionsRef()
                .whereEqualTo("collegeId", collegeId)
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    todaySessions.clear();

                    if (querySnapshots != null && !querySnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : querySnapshots) {
                            ClassSession cs = doc.toObject(ClassSession.class);
                            if (cs != null) {
                                boolean matches = false;
                                if (finalTeacherUid != null && !finalTeacherUid.isEmpty()) {
                                    if (finalTeacherUid.equals(cs.getFacultyId()) || finalTeacherUid.equals(cs.getTeacherUid())) {
                                        matches = true;
                                    }
                                }
                                if (!matches && currentUser != null && currentUser.getName() != null && !currentUser.getName().isEmpty()) {
                                    if (currentUser.getName().equalsIgnoreCase(cs.getTeacherName()) ||
                                            currentUser.getName().equalsIgnoreCase(cs.getFacultyName())) {
                                        matches = true;
                                    }
                                }
                                if (matches) {
                                    todaySessions.add(cs);
                                }
                            }
                        }
                    }

                    ClassDetectionEngine.DetectionResult result = ClassDetectionEngine.detectCurrentClass(todaySessions);
                    showDetectionResult(result);
                })
                .addOnFailureListener(e -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    showDetectionResult(new ClassDetectionEngine.DetectionResult(
                            ClassDetectionEngine.ClassStatus.NO_CLASS, null, "Unable to query faculty sessions: " + e.getMessage()
                    ));
                });
    }

    private void showDetectionResult(ClassDetectionEngine.DetectionResult result) {
        if (result == null) return;

        if (tvStatusHeader != null) {
            tvStatusHeader.setText("Faculty Class Detection: " + result.getStatus().name());
        }

        if (tvStatusExplanation != null) {
            tvStatusExplanation.setText(result.getExplanation());
        }

        ClassSession session = result.getSession();
        if (session != null) {
            if (tvSubjectName != null) tvSubjectName.setText(session.getSubjectName());
            if (tvRoomNumber != null) tvRoomNumber.setText("Assigned Room: " + (session.getRoomNumber() != null ? session.getRoomNumber() : "TBD"));
            if (tvTimeSlot != null) tvTimeSlot.setText("Time Window: " + (session.getStartTime() != null ? session.getStartTime() : "TBD"));
            if (tvSectionName != null) tvSectionName.setText("Section: " + (session.getSectionName() != null ? session.getSectionName() : "Assigned Section"));
        } else {
            if (tvSubjectName != null) tvSubjectName.setText("No Active Lecture");
            if (tvRoomNumber != null) tvRoomNumber.setText("Room: --");
            if (tvTimeSlot != null) tvTimeSlot.setText("Time: --");
            if (tvSectionName != null) tvSectionName.setText("Section: --");
        }
    }
}
