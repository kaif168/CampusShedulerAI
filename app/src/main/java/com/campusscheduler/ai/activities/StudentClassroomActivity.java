package com.campusscheduler.ai.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.campusscheduler.ai.R;
import com.campusscheduler.ai.models.Classroom;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.utils.FirebaseHelper;
import com.campusscheduler.ai.utils.LocalAuthManager;
import com.google.firebase.firestore.DocumentSnapshot;

public class StudentClassroomActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvClassroomDetails;
    private ProgressBar progressBar;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_classroom);

        currentUser = LocalAuthManager.getCurrentLocalUser(this);
        String safeCollegeId = LocalAuthManager.getSafeCollegeId(this, getIntent());
        if (currentUser == null) {
            currentUser = new User();
            currentUser.setCollegeId(safeCollegeId);
            currentUser.setDepartmentName("Computer Science & Engineering");
            currentUser.setSemester("5");
        } else if (currentUser.getCollegeId() == null || currentUser.getCollegeId().trim().isEmpty()) {
            currentUser.setCollegeId(safeCollegeId);
        }

        btnBack = findViewById(R.id.btnBack);
        tvClassroomDetails = findViewById(R.id.tvClassroomDetails);
        progressBar = findViewById(R.id.progressBar);

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        loadClassroomDetails();
    }

    private void loadClassroomDetails() {
        String collegeId = currentUser != null && currentUser.getCollegeId() != null ?
                currentUser.getCollegeId() : LocalAuthManager.getSafeCollegeId(this, getIntent());

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        FirebaseHelper.getClassroomsRef()
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);

                    StringBuilder sb = new StringBuilder();
                    sb.append("YOUR CLASSROOM & LAB INFRASTRUCTURE\n");
                    sb.append("----------------------------------------\n");
                    sb.append("Department: ").append(currentUser.getDepartmentName() != null ? currentUser.getDepartmentName() : "Computer Science & Engineering").append("\n");
                    sb.append("Semester: ").append(currentUser.getSemester() != null ? currentUser.getSemester() : "5").append("\n\n");

                    boolean foundRooms = false;
                    if (querySnapshots != null && !querySnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : querySnapshots) {
                            Classroom c = doc.toObject(Classroom.class);
                            if (c != null) {
                                String cId = c.getCollegeId();
                                if (collegeId == null || cId == null || cId.isEmpty() || cId.equalsIgnoreCase(collegeId)) {
                                    foundRooms = true;
                                    sb.append("• Room ").append(c.getRoomNumber() != null ? c.getRoomNumber() : "Facility")
                                      .append(" (Building: ").append(c.getBuilding() != null ? c.getBuilding() : "Main Academic Block")
                                      .append(", Capacity: ").append(c.getCapacity())
                                      .append(c.isHasProjector() ? ", Projector Enabled" : "")
                                      .append(c.isHasLabEquipment() ? ", Lab Equipment" : "")
                                      .append(")\n");
                                }
                            }
                        }
                    }

                    if (!foundRooms) {
                        if (tvClassroomDetails != null) {
                            tvClassroomDetails.setText("No classroom information available.");
                        }
                    } else {
                        if (tvClassroomDetails != null) {
                            tvClassroomDetails.setText(sb.toString());
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (tvClassroomDetails != null) {
                        tvClassroomDetails.setText("No classroom information available.");
                    }
                });
    }
}
