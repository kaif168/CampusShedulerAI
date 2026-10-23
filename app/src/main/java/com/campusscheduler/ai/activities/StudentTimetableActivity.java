package com.campusscheduler.ai.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.campusscheduler.ai.R;
import com.campusscheduler.ai.adapters.GenericAdapter;
import com.campusscheduler.ai.models.ClassSession;
import com.campusscheduler.ai.models.Timetable;
import com.campusscheduler.ai.models.TimetableSlot;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.utils.FirebaseHelper;
import com.campusscheduler.ai.utils.LocalAuthManager;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class StudentTimetableActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private ProgressBar progressBar;
    private TextView tvEmptyState;
    private RecyclerView rvStudentTimetable;
    private GenericAdapter<TimetableSlot> adapter;
    private List<TimetableSlot> slotList = new ArrayList<>();
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_timetable);

        currentUser = LocalAuthManager.getCurrentLocalUser(this);

        btnBack = findViewById(R.id.btnBack);
        progressBar = findViewById(R.id.progressBar);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        rvStudentTimetable = findViewById(R.id.rvStudentTimetable);

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        if (rvStudentTimetable != null) {
            rvStudentTimetable.setLayoutManager(new LinearLayoutManager(this));
            adapter = new GenericAdapter<>(slotList, (item, holder) -> {
                String day = item.getDay() != null ? item.getDay() : "Scheduled Day";
                String sub = item.getSubjectName() != null ? item.getSubjectName() : "Subject";
                String start = item.getStartTime() != null ? item.getStartTime() : "TBD";
                String end = item.getEndTime() != null ? item.getEndTime() : "TBD";
                String teacher = item.getTeacherName() != null ? item.getTeacherName() : "Faculty Member";
                String room = item.getRoomName() != null ? item.getRoomName() : "Assigned Hall";
                String type = item.getClassType() != null ? item.getClassType() : "Core Session";

                holder.setText(R.id.tvTitle, day + ": " + sub + " (" + start + " - " + end + ")");
                holder.setText(R.id.tvSubtitle, "Faculty: " + teacher + " | Room: " + room + " [" + type + "]");
            }, R.layout.item_generic);
            rvStudentTimetable.setAdapter(adapter);
        }

        loadTimetable();
    }

    private int extractSemesterNumber(String semStr) {
        if (semStr == null) return -1;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)").matcher(semStr);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (Exception ignored) {}
        }
        return -1;
    }

    private String normalizeSection(String sec) {
        if (sec == null) return "";
        return sec.replaceAll("(?i)section", "").replaceAll("[^a-zA-Z0-9]", "").trim().toUpperCase();
    }

    private void loadTimetable() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (tvEmptyState != null) tvEmptyState.setVisibility(View.GONE);

        String collegeId = currentUser != null ? currentUser.getCollegeId() : null;
        int studentSem = extractSemesterNumber(currentUser != null ? currentUser.getSemester() : null);
        String userSecNorm = normalizeSection(currentUser != null ? currentUser.getSectionId() : null);

        FirebaseHelper.getTimetablesRef().get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    slotList.clear();

                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            Timetable tt = doc.toObject(Timetable.class);
                            if (tt != null) {
                                String cId = tt.getCollegeId();
                                if (collegeId == null || cId == null || cId.isEmpty() || cId.equalsIgnoreCase(collegeId)) {
                                    // Match semester
                                    if (studentSem > 0 && tt.getSemester() > 0 && tt.getSemester() != studentSem) {
                                        continue;
                                    }
                                    // Match section if timetable specifies section
                                    String ttSecNorm = normalizeSection(tt.getSection());
                                    if (!userSecNorm.isEmpty() && !ttSecNorm.isEmpty() && !ttSecNorm.equalsIgnoreCase(userSecNorm)) {
                                        continue;
                                    }

                                    if (tt.getSlots() != null) {
                                        for (TimetableSlot s : tt.getSlots()) {
                                            if (!userSecNorm.isEmpty()) {
                                                String slotSec = s.getSectionId() != null ? s.getSectionId() : s.getSectionName();
                                                String slotSecNorm = normalizeSection(slotSec);
                                                if (!slotSecNorm.isEmpty() && !slotSecNorm.equalsIgnoreCase(userSecNorm)) {
                                                    continue;
                                                }
                                            }
                                            slotList.add(s);
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (slotList.isEmpty()) {
                        // Try querying class_sessions if timetables slots are empty
                        FirebaseHelper.getClassSessionsRef().get()
                                .addOnSuccessListener(csSnapshots -> {
                                    if (csSnapshots != null && !csSnapshots.isEmpty()) {
                                        for (DocumentSnapshot doc : csSnapshots) {
                                            ClassSession cs = doc.toObject(ClassSession.class);
                                            if (cs != null) {
                                                String cId = cs.getCollegeId();
                                                if (collegeId == null || cId == null || cId.isEmpty() || cId.equalsIgnoreCase(collegeId)) {
                                                    if (studentSem > 0 && cs.getSemester() > 0 && cs.getSemester() != studentSem) {
                                                        continue;
                                                    }
                                                    if (!userSecNorm.isEmpty()) {
                                                        String csSec = cs.getSection() != null ? cs.getSection() : cs.getSectionId();
                                                        String csSecNorm = normalizeSection(csSec);
                                                        if (!csSecNorm.isEmpty() && !csSecNorm.equalsIgnoreCase(userSecNorm)) {
                                                            continue;
                                                        }
                                                    }
                                                    TimetableSlot ts = new TimetableSlot();
                                                    ts.setDay(cs.getDayOfWeek() != null ? cs.getDayOfWeek() : "Regular");
                                                    ts.setSubjectName(cs.getSubjectName());
                                                    ts.setStartTime(cs.getStartTime());
                                                    ts.setEndTime(cs.getEndTime());
                                                    ts.setTeacherName(cs.getTeacherName());
                                                    ts.setRoomName(cs.getRoomNumber());
                                                    ts.setClassType(cs.isLab() ? "Lab" : "Theory");
                                                    slotList.add(ts);
                                                }
                                            }
                                        }
                                    }

                                    if (slotList.isEmpty()) {
                                        if (tvEmptyState != null) {
                                            tvEmptyState.setVisibility(View.VISIBLE);
                                            tvEmptyState.setText("No published timetable available for your section.");
                                        }
                                    } else {
                                        if (tvEmptyState != null) tvEmptyState.setVisibility(View.GONE);
                                        sortSlots();
                                    }
                                    if (adapter != null) adapter.notifyDataSetChanged();
                                })
                                .addOnFailureListener(e -> {
                                    if (tvEmptyState != null) {
                                        tvEmptyState.setVisibility(View.VISIBLE);
                                        tvEmptyState.setText("No published timetable available for your section.");
                                    }
                                    if (adapter != null) adapter.notifyDataSetChanged();
                                });
                    } else {
                        if (tvEmptyState != null) tvEmptyState.setVisibility(View.GONE);
                        sortSlots();
                        if (adapter != null) adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (tvEmptyState != null) {
                        tvEmptyState.setVisibility(View.VISIBLE);
                        tvEmptyState.setText("No published timetable available for your section.");
                    }
                    if (adapter != null) adapter.notifyDataSetChanged();
                });
    }

    private void sortSlots() {
        if (slotList == null || slotList.isEmpty()) return;
        java.util.Collections.sort(slotList, (a, b) -> {
            int dayA = com.campusscheduler.ai.utils.UIUtils.getDayOrder(a.getDay());
            int dayB = com.campusscheduler.ai.utils.UIUtils.getDayOrder(b.getDay());
            if (dayA != dayB) {
                return Integer.compare(dayA, dayB);
            }
            return Integer.compare(
                    com.campusscheduler.ai.utils.UIUtils.parseTimeToMinutes(a.getStartTime()),
                    com.campusscheduler.ai.utils.UIUtils.parseTimeToMinutes(b.getStartTime())
            );
        });
    }
}
