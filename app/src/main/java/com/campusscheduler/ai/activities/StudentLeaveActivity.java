package com.campusscheduler.ai.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.campusscheduler.ai.R;
import com.campusscheduler.ai.adapters.GenericAdapter;
import com.campusscheduler.ai.models.LeaveRequest;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.utils.FirebaseHelper;
import com.campusscheduler.ai.utils.LocalAuthManager;
import com.campusscheduler.ai.utils.UIUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class StudentLeaveActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private FloatingActionButton fabApplyLeave;
    private RecyclerView rvStudentLeave;
    private GenericAdapter<LeaveRequest> adapter;
    private List<LeaveRequest> leaveList = new ArrayList<>();
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_leave);

        currentUser = LocalAuthManager.getCurrentLocalUser(this);
        String safeCollegeId = LocalAuthManager.getSafeCollegeId(this, getIntent());
        String studentUid = getIntent().getStringExtra("studentUid");
        if (currentUser == null) {
            currentUser = new User();
            currentUser.setCollegeId(safeCollegeId);
            currentUser.setDepartmentId("DEPT_CSE");
            currentUser.setRole("student");
            currentUser.setName("Student");
            if (studentUid != null) {
                currentUser.setUid(studentUid);
            } else if (FirebaseHelper.getAuth().getCurrentUser() != null) {
                currentUser.setUid(FirebaseHelper.getAuth().getCurrentUser().getUid());
            }
        } else {
            if (currentUser.getCollegeId() == null || currentUser.getCollegeId().trim().isEmpty()) {
                currentUser.setCollegeId(safeCollegeId);
            }
            if ((currentUser.getUid() == null || currentUser.getUid().trim().isEmpty()) && studentUid != null) {
                currentUser.setUid(studentUid);
            }
        }

        btnBack = findViewById(R.id.btnBack);
        fabApplyLeave = findViewById(R.id.fabApplyLeave);
        rvStudentLeave = findViewById(R.id.rvStudentLeave);

        btnBack.setOnClickListener(v -> finish());
        fabApplyLeave.setOnClickListener(v -> showApplyDialog());

        rvStudentLeave.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GenericAdapter<>(leaveList, (item, holder) -> {
            holder.setText(R.id.tvTitle, "Leave: " + item.getStartDate() + " to " + item.getEndDate() + " [" + item.getStatus() + "]");
            holder.setText(R.id.tvSubtitle, "Reason: " + item.getReason());
        }, R.layout.item_generic);
        rvStudentLeave.setAdapter(adapter);

        loadLeaves();
    }

    private void loadLeaves() {
        String uid = currentUser != null ? currentUser.getUid() : null;
        if (uid == null || uid.isEmpty()) {
            uid = getIntent().getStringExtra("studentUid");
        }
        if (uid == null && FirebaseHelper.getAuth().getCurrentUser() != null) {
            uid = FirebaseHelper.getAuth().getCurrentUser().getUid();
        }
        FirebaseHelper.getLeavesRef().whereEqualTo("applicantUid", uid).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    leaveList.clear();
                    for (var doc : queryDocumentSnapshots) {
                        LeaveRequest lr = doc.toObject(LeaveRequest.class);
                        if (lr != null) leaveList.add(lr);
                    }
                    if (leaveList.isEmpty()) {
                        UIUtils.showToast(StudentLeaveActivity.this, "No leave records available.");
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void showApplyDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Apply for Student Leave");

        EditText etStart = new EditText(this);
        etStart.setHint("Start Date (YYYY-MM-DD)");
        EditText etEnd = new EditText(this);
        etEnd.setHint("End Date (YYYY-MM-DD)");
        EditText etReason = new EditText(this);
        etReason.setHint("Reason for Leave");

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);
        layout.addView(etStart);
        layout.addView(etEnd);
        layout.addView(etReason);
        builder.setView(layout);

        builder.setPositiveButton("Submit Application", (dialog, which) -> {
            String start = etStart.getText().toString().trim();
            String end = etEnd.getText().toString().trim();
            String reason = etReason.getText().toString().trim();

            if (start.isEmpty() || end.isEmpty() || reason.isEmpty()) {
                UIUtils.showToast(this, "All fields required.");
                return;
            }

            String collegeId = currentUser != null && currentUser.getCollegeId() != null ?
                    currentUser.getCollegeId() : LocalAuthManager.getSafeCollegeId(this, getIntent());
            String applicantUid = currentUser != null ? currentUser.getUid() : null;
            if (applicantUid == null && FirebaseHelper.getAuth().getCurrentUser() != null) {
                applicantUid = FirebaseHelper.getAuth().getCurrentUser().getUid();
            }

            String id = FirebaseHelper.getLeavesRef().document().getId();
            LeaveRequest lr = new LeaveRequest();
            lr.setRequestId(id);
            lr.setLeaveId(id);
            lr.setCollegeId(collegeId);
            lr.setApplicantUid(applicantUid != null ? applicantUid : "STUDENT_UID");
            lr.setApplicantId(applicantUid != null ? applicantUid : "STUDENT_UID");
            lr.setApplicantName(currentUser.getName() != null ? currentUser.getName() : "Student");
            lr.setApplicantRole(User.ROLE_STUDENT);
            lr.setStartDate(start);
            lr.setEndDate(end);
            lr.setReason(reason);
            lr.setStatus(LeaveRequest.STATUS_PENDING);

            FirebaseHelper.getLeavesRef().document(id).set(lr)
                    .addOnSuccessListener(aVoid -> {
                        UIUtils.showToast(this, "Leave application submitted!");
                        loadLeaves();
                    })
                    .addOnFailureListener(e -> {
                        leaveList.add(lr);
                        adapter.notifyDataSetChanged();
                    });
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}
