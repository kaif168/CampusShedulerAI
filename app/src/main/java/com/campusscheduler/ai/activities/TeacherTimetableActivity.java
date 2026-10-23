package com.campusscheduler.ai.activities;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.campusscheduler.ai.R;
import com.campusscheduler.ai.adapters.GenericAdapter;
import com.campusscheduler.ai.models.ClassSession;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.utils.FirebaseHelper;
import com.campusscheduler.ai.utils.LocalAuthManager;
import com.campusscheduler.ai.utils.UIUtils;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class TeacherTimetableActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private RecyclerView rvTeacherTimetable;
    private TextView tvTeacherTimetableHeader, tvTeacherTimetableSubheader;
    private GenericAdapter<ClassSession> adapter;
    private List<ClassSession> teacherSessionList = new ArrayList<>();
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_timetable);

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
        rvTeacherTimetable = findViewById(R.id.rvTeacherTimetable);
        tvTeacherTimetableHeader = findViewById(R.id.tvTeacherTimetableHeader);
        tvTeacherTimetableSubheader = findViewById(R.id.tvTeacherTimetableSubheader);

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        if (currentUser != null && tvTeacherTimetableHeader != null) {
            tvTeacherTimetableHeader.setText("Academic Schedule for Prof. " + (currentUser.getName() != null ? currentUser.getName() : "Faculty"));
        }

        if (rvTeacherTimetable != null) {
            rvTeacherTimetable.setLayoutManager(new LinearLayoutManager(this));
            adapter = new GenericAdapter<>(teacherSessionList, (item, holder) -> {
                String timeStr = (item.getStartTime() != null ? item.getStartTime() : "08:20 AM") +
                        " - " + (item.getEndTime() != null ? item.getEndTime() : "09:10 AM");
                holder.setText(R.id.tvTimeSlot, timeStr);
                holder.setText(R.id.tvDayName, item.getDayOfWeek() != null ? item.getDayOfWeek() : "Monday");
                holder.setText(R.id.tvSubjectTitle, item.getSubjectName() != null ? item.getSubjectName() : "Class Session");
                
                String secRoom = (item.getSemester() > 0 ? item.getSemester() + "th Sem • " : "") +
                        "Section " + (item.getSectionName() != null ? item.getSectionName() : "A") +
                        " | Room: " + (item.getRoomNumber() != null ? item.getRoomNumber() : "Assigned Hall");
                holder.setText(R.id.tvSectionRoom, secRoom);

                String badge = item.getClassType() != null ? item.getClassType() : (item.isLab() ? "Lab Session" : "Theory Session");
                holder.setText(R.id.tvClassTypeBadge, badge);
            }, R.layout.item_teacher_timetable_slot);
            rvTeacherTimetable.setAdapter(adapter);
        }

        loadTeacherTimetable();
    }

    private void loadTeacherTimetable() {
        String collegeId = currentUser != null && currentUser.getCollegeId() != null ?
                currentUser.getCollegeId() : LocalAuthManager.getSafeCollegeId(this, getIntent());
        String teacherUid = currentUser != null ? currentUser.getUid() : null;
        if (teacherUid == null || teacherUid.isEmpty()) {
            teacherUid = getIntent().getStringExtra("teacherUid");
        }
        if (teacherUid == null && FirebaseHelper.getAuth().getCurrentUser() != null) {
            teacherUid = FirebaseHelper.getAuth().getCurrentUser().getUid();
        }

        final String finalTeacherUid = teacherUid;
        FirebaseHelper.getClassSessionsRef()
                .whereEqualTo("collegeId", collegeId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    teacherSessionList.clear();
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
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
                                    teacherSessionList.add(cs);
                                }
                            }
                        }
                    }

                    // Sort chronologically by day of week then start time
                    java.util.Collections.sort(teacherSessionList, (a, b) -> {
                        int dayA = UIUtils.getDayOrder(a.getDayOfWeek());
                        int dayB = UIUtils.getDayOrder(b.getDayOfWeek());
                        if (dayA != dayB) {
                            return Integer.compare(dayA, dayB);
                        }
                        return Integer.compare(UIUtils.parseTimeToMinutes(a.getStartTime()), UIUtils.parseTimeToMinutes(b.getStartTime()));
                    });

                    updateWorkloadDisplay();
                    if (teacherSessionList.isEmpty() && tvTeacherTimetableSubheader != null) {
                        tvTeacherTimetableSubheader.append("\n(No active class sessions found in Firestore for your profile)");
                    }
                    if (adapter != null) adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    UIUtils.showToast(this, "Error loading timetable: " + e.getMessage());
                });
    }

    private void updateWorkloadDisplay() {
        if (tvTeacherTimetableSubheader == null) return;

        String designation = currentUser != null ? currentUser.getDesignation() : null;
        if ((designation == null || designation.trim().isEmpty()) && !teacherSessionList.isEmpty()) {
            for (ClassSession cs : teacherSessionList) {
                if (cs.getFacultyDesignation() != null && !cs.getFacultyDesignation().trim().isEmpty()) {
                    designation = cs.getFacultyDesignation();
                    break;
                }
            }
        }
        if (designation == null || designation.trim().isEmpty()) {
            designation = "Assistant Professor";
        }

        int limit = 14;
        String dLower = designation.toLowerCase();
        if (dLower.contains("associate")) {
            limit = 16;
            designation = "Associate Professor";
        } else if (dLower.contains("assistant")) {
            limit = 14;
            designation = "Assistant Professor";
        } else if (dLower.contains("professor")) {
            limit = 10;
            designation = "Professor";
        }

        int scheduledHours = teacherSessionList.size();
        int remaining = Math.max(0, limit - scheduledHours);
        String status = (scheduledHours > limit) ? "Overload" : "Normal";

        String summary = "Designation: " + designation + "  •  Weekly Limit: " + limit + " hrs\n" +
                "Scheduled: " + scheduledHours + " hrs  •  Remaining: " + remaining + " hrs  •  Status: " + status;
        tvTeacherTimetableSubheader.setText(summary);
    }
}
