package com.campusscheduler.ai.activities;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.campusscheduler.ai.R;
import com.campusscheduler.ai.models.ClassSession;
import com.campusscheduler.ai.models.Faculty;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.utils.FirebaseHelper;
import com.campusscheduler.ai.utils.LocalAuthManager;
import com.campusscheduler.ai.utils.UIUtils;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FacultyWorkloadActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvFacultyIdentity, tvFacultyDesignation;
    private TextView tvWorkloadWarningBanner;
    private TextView tvMetricScheduled, tvMetricPermitted, tvMetricRemaining;
    private LinearLayout llSubjectWorkloadContainer, llTimetableSessionsList;

    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_workload);

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

        btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        tvFacultyIdentity = findViewById(R.id.tvFacultyIdentity);
        tvFacultyDesignation = findViewById(R.id.tvFacultyDesignation);
        tvWorkloadWarningBanner = findViewById(R.id.tvWorkloadWarningBanner);
        tvMetricScheduled = findViewById(R.id.tvMetricScheduled);
        tvMetricPermitted = findViewById(R.id.tvMetricPermitted);
        tvMetricRemaining = findViewById(R.id.tvMetricRemaining);
        llSubjectWorkloadContainer = findViewById(R.id.llSubjectWorkloadContainer);
        llTimetableSessionsList = findViewById(R.id.llTimetableSessionsList);

        if (tvFacultyIdentity != null && currentUser.getName() != null) {
            tvFacultyIdentity.setText(currentUser.getName());
        }

        loadFacultyProfileAndWorkload();
    }

    private void loadFacultyProfileAndWorkload() {
        String collegeId = (currentUser != null && currentUser.getCollegeId() != null && !currentUser.getCollegeId().trim().isEmpty())
                ? currentUser.getCollegeId().trim() : "COLLEGE_1789406176044";

        String myUid = currentUser != null ? currentUser.getUid() : "";
        String myName = currentUser != null && currentUser.getName() != null ? currentUser.getName().trim() : "";

        // First resolve verified designation from Firestore faculty collection
        FirebaseHelper.getFacultyRef().whereEqualTo("collegeId", collegeId).get()
                .addOnSuccessListener(facSnaps -> {
                    String verifiedDesignation = "Assistant Professor";
                    if (facSnaps != null) {
                        for (DocumentSnapshot doc : facSnaps) {
                            Faculty f = doc.toObject(Faculty.class);
                            if (f != null) {
                                boolean isMe = (myUid != null && (doc.getId().equalsIgnoreCase(myUid) || myUid.equalsIgnoreCase(f.getUid())))
                                        || (f.getName() != null && (f.getName().equalsIgnoreCase(myName) || f.getName().replace("Dr.", "").trim().equalsIgnoreCase(myName.replace("Dr.", "").trim())));
                                if (isMe && f.getDesignation() != null && !f.getDesignation().trim().isEmpty()) {
                                    verifiedDesignation = f.getDesignation().trim();
                                    break;
                                }
                            }
                        }
                    }

                    if (currentUser != null && currentUser.getDesignation() != null && !currentUser.getDesignation().trim().isEmpty()) {
                        if (!"Faculty".equalsIgnoreCase(currentUser.getDesignation()) && !"Teacher".equalsIgnoreCase(currentUser.getDesignation())) {
                            verifiedDesignation = currentUser.getDesignation().trim();
                        }
                    }

                    int permittedLimit = 16;
                    String dLower = verifiedDesignation.toLowerCase(Locale.ROOT);
                    if (dLower.contains("professor") && !dLower.contains("assistant") && !dLower.contains("associate")) {
                        permittedLimit = 10;
                    } else if (dLower.contains("associate")) {
                        permittedLimit = 14;
                    }

                    if (tvFacultyDesignation != null) {
                        tvFacultyDesignation.setText("Designation: " + verifiedDesignation + " • Dept: CSE");
                    }

                    final int finalPermittedLimit = permittedLimit;

                    // Query class sessions
                    FirebaseHelper.getClassSessionsRef().whereEqualTo("collegeId", collegeId).get()
                            .addOnSuccessListener(csSnaps -> {
                                List<ClassSession> mySessions = new ArrayList<>();
                                Map<String, Integer> subjectHours = new HashMap<>();

                                if (csSnaps != null) {
                                    for (DocumentSnapshot doc : csSnaps) {
                                        ClassSession cs = doc.toObject(ClassSession.class);
                                        if (cs != null && isUserSession(cs)) {
                                            mySessions.add(cs);
                                            String sub = cs.getSubjectName() != null ? cs.getSubjectName() : "Core Subject";
                                            subjectHours.put(sub, subjectHours.getOrDefault(sub, 0) + 1);
                                        }
                                    }
                                }

                                int scheduledCount = mySessions.size();
                                int remainingCount = Math.max(0, finalPermittedLimit - scheduledCount);

                                tvMetricPermitted.setText(finalPermittedLimit + "h");
                                tvMetricScheduled.setText(scheduledCount + "h");
                                tvMetricRemaining.setText(remainingCount + "h");

                                if (scheduledCount > finalPermittedLimit) {
                                    if (tvWorkloadWarningBanner != null) {
                                        tvWorkloadWarningBanner.setVisibility(View.VISIBLE);
                                        tvWorkloadWarningBanner.setText("⚠️ WORKLOAD ALERT: Scheduled teaching hours (" + scheduledCount + " hrs) exceed institutional limit of " + finalPermittedLimit + " hrs/week!");
                                    }
                                } else {
                                    if (tvWorkloadWarningBanner != null) {
                                        tvWorkloadWarningBanner.setVisibility(View.GONE);
                                    }
                                }

                                renderSubjectWorkload(subjectHours);
                                renderSessionsList(mySessions);
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load faculty workload: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private boolean isUserSession(ClassSession cs) {
        if (currentUser == null) return false;
        String uid = currentUser.getUid();
        String name = currentUser.getName();

        if (uid != null && (uid.equalsIgnoreCase(cs.getFacultyId()) || uid.equalsIgnoreCase(cs.getTeacherUid()))) {
            return true;
        }

        if (name != null) {
            String facName = cs.getFacultyName() != null ? cs.getFacultyName() : cs.getTeacherName();
            if (facName != null && !facName.isEmpty()) {
                String cleanUser = name.replace("Dr.", "").replace("Prof.", "").trim();
                String cleanFac = facName.replace("Dr.", "").replace("Prof.", "").trim();
                return cleanFac.equalsIgnoreCase(cleanUser) || facName.equalsIgnoreCase(name);
            }
        }
        return false;
    }

    private void renderSubjectWorkload(Map<String, Integer> subjectHours) {
        if (llSubjectWorkloadContainer == null) return;
        llSubjectWorkloadContainer.removeAllViews();

        if (subjectHours.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("No subject sessions currently scheduled.");
            tvEmpty.setTextColor(0xFF78909C);
            tvEmpty.setPadding(12, 12, 12, 12);
            llSubjectWorkloadContainer.addView(tvEmpty);
            return;
        }

        for (Map.Entry<String, Integer> entry : subjectHours.entrySet()) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(16, 12, 16, 12);
            row.setBackgroundColor(Color.WHITE);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, 6);
            row.setLayoutParams(lp);

            TextView tvSubject = new TextView(this);
            tvSubject.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            tvSubject.setText(entry.getKey());
            tvSubject.setTextColor(0xFF263238);
            tvSubject.setTextSize(13f);
            tvSubject.setTypeface(null, Typeface.BOLD);
            row.addView(tvSubject);

            TextView tvHours = new TextView(this);
            tvHours.setText(entry.getValue() + " hrs / week");
            tvHours.setTextColor(0xFF0D47A1);
            tvHours.setTextSize(13f);
            tvHours.setTypeface(null, Typeface.BOLD);
            row.addView(tvHours);

            llSubjectWorkloadContainer.addView(row);
        }
    }

    private void renderSessionsList(List<ClassSession> sessions) {
        if (llTimetableSessionsList == null) return;
        llTimetableSessionsList.removeAllViews();

        if (sessions.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("No active timetable sessions found.");
            tvEmpty.setTextColor(0xFF78909C);
            tvEmpty.setPadding(12, 12, 12, 12);
            llTimetableSessionsList.addView(tvEmpty);
            return;
        }

        Collections.sort(sessions, (a, b) -> {
            int dayComp = getDayIndex(a.getDayOfWeek()) - getDayIndex(b.getDayOfWeek());
            if (dayComp != 0) return dayComp;
            return Integer.compare(UIUtils.parseTimeToMinutes(a.getStartTime()), UIUtils.parseTimeToMinutes(b.getStartTime()));
        });

        for (ClassSession cs : sessions) {
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setPadding(16, 10, 16, 10);
            item.setBackgroundColor(Color.WHITE);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, 6);
            item.setLayoutParams(lp);

            TextView tvTitle = new TextView(this);
            String day = cs.getDayOfWeek() != null ? cs.getDayOfWeek() : "Scheduled Day";
            String time = (cs.getStartTime() != null ? cs.getStartTime() : "TBD") + " - " + (cs.getEndTime() != null ? cs.getEndTime() : "TBD");
            tvTitle.setText(day + "  •  " + time);
            tvTitle.setTextColor(0xFF0D47A1);
            tvTitle.setTextSize(12.5f);
            tvTitle.setTypeface(null, Typeface.BOLD);
            item.addView(tvTitle);

            TextView tvDetails = new TextView(this);
            tvDetails.setText("• " + cs.getSubjectName() + " (Sem " + cs.getSemester() + "-" + (cs.getSection() != null ? cs.getSection() : "A") + ")\n"
                    + "• Venue: " + (cs.getRoomNumber() != null ? cs.getRoomNumber() : "Assigned Hall")
                    + (cs.isLab() ? " [LAB]" : " [THEORY]"));
            tvDetails.setTextColor(0xFF455A64);
            tvDetails.setTextSize(12f);
            item.addView(tvDetails);

            llTimetableSessionsList.addView(item);
        }
    }

    private int getDayIndex(String day) {
        if (day == null) return 0;
        switch (day.toLowerCase(Locale.ROOT)) {
            case "monday": return 1;
            case "tuesday": return 2;
            case "wednesday": return 3;
            case "thursday": return 4;
            case "friday": return 5;
            case "saturday": return 6;
            default: return 7;
        }
    }
}
