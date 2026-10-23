package com.campusscheduler.ai.activities;

import android.os.Bundle;
import android.widget.CalendarView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.campusscheduler.ai.R;
import com.campusscheduler.ai.models.ClassSession;
import com.campusscheduler.ai.models.Exam;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.utils.FirebaseHelper;
import com.campusscheduler.ai.utils.LocalAuthManager;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class TeacherCalendarActivity extends AppCompatActivity {

    private android.widget.ImageButton btnBack;
    private CalendarView calendarView;
    private TextView tvEventsForDate;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_calendar);

        btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        currentUser = LocalAuthManager.getCurrentLocalUser(this);
        String safeCollegeId = LocalAuthManager.getSafeCollegeId(this, getIntent());
        if (currentUser == null) {
            currentUser = new User();
            currentUser.setRole(User.ROLE_TEACHER);
            currentUser.setName("Faculty");
        }
        if (currentUser.getCollegeId() == null || currentUser.getCollegeId().trim().isEmpty()) {
            currentUser.setCollegeId(safeCollegeId);
            LocalAuthManager.saveUserSession(this, currentUser);
        }

        calendarView = findViewById(R.id.calendarView);
        tvEventsForDate = findViewById(R.id.tvEventsForDate);

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            loadEventsForSelectedDate(year, month + 1, dayOfMonth);
        });

        // Load for today by default
        java.util.Calendar today = java.util.Calendar.getInstance();
        loadEventsForSelectedDate(today.get(java.util.Calendar.YEAR), today.get(java.util.Calendar.MONTH) + 1, today.get(java.util.Calendar.DAY_OF_MONTH));
    }

    private void loadEventsForSelectedDate(int year, int month, int dayOfMonth) {
        String collegeId = (currentUser != null && currentUser.getCollegeId() != null && !currentUser.getCollegeId().trim().isEmpty())
                ? currentUser.getCollegeId() : LocalAuthManager.getSafeCollegeId(this, getIntent());
        String teacherUid = currentUser != null && currentUser.getUid() != null ? currentUser.getUid() : "";

        String dateStr = String.format(java.util.Locale.US, "%04d-%02d-%02d", year, month, dayOfMonth);
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(year, month - 1, dayOfMonth);
        String selectedDayOfWeek = new java.text.SimpleDateFormat("EEEE", java.util.Locale.US).format(cal.getTime());

        tvEventsForDate.setText("Loading events for " + selectedDayOfWeek + " (" + dateStr + ")...");

        FirebaseHelper.getClassSessionsRef()
                .whereEqualTo("collegeId", collegeId)
                .get()
                .addOnSuccessListener(sessionSnaps -> {
                    List<String> eventLines = new ArrayList<>();
                    java.util.Set<String> activeDays = new java.util.TreeSet<>();

                    if (sessionSnaps != null && !sessionSnaps.isEmpty()) {
                        for (DocumentSnapshot doc : sessionSnaps) {
                            ClassSession cs = doc.toObject(ClassSession.class);
                            if (cs != null) {
                                boolean isMySession = (teacherUid.equals(cs.getTeacherUid()))
                                        || (teacherUid.equals(cs.getFacultyId()))
                                        || (currentUser != null && TeacherDashboardActivity.isSameTeacher(currentUser.getName(), cs.getTeacherName()))
                                        || (currentUser != null && TeacherDashboardActivity.isSameTeacher(currentUser.getName(), cs.getFacultyName()));

                                if (!isMySession) continue;

                                if (cs.getDayOfWeek() != null && !cs.getDayOfWeek().isEmpty()) {
                                    activeDays.add(cs.getDayOfWeek());
                                }

                                boolean matchesDay = (dateStr.equalsIgnoreCase(cs.getDate()))
                                        || (cs.getDayOfWeek() != null && cs.getDayOfWeek().equalsIgnoreCase(selectedDayOfWeek));

                                if (matchesDay) {
                                    String time = cs.getStartTime() != null ? cs.getStartTime() : "TBD";
                                    if (cs.getEndTime() != null) time += " - " + cs.getEndTime();
                                    String room = cs.getRoomNumber() != null ? cs.getRoomNumber() : "Assigned Room";
                                    String sec = cs.getSection() != null ? " [Sec " + cs.getSection() + "]" : "";
                                    eventLines.add("• " + time + ": " + cs.getSubjectName() + sec + " (" + room + ")");
                                }
                            }
                        }
                    }

                    // Check exams
                    FirebaseHelper.getExamsRef()
                            .whereEqualTo("collegeId", collegeId)
                            .get()
                            .addOnSuccessListener(examSnaps -> {
                                if (examSnaps != null && !examSnaps.isEmpty()) {
                                    for (DocumentSnapshot doc : examSnaps) {
                                        Exam exam = doc.toObject(Exam.class);
                                        if (exam != null && dateStr.equalsIgnoreCase(exam.getExamDate())) {
                                            eventLines.add("• 📝 EXAM: " + exam.getSubjectName() + " (" + exam.getStartTime() + ")");
                                        }
                                    }
                                }

                                StringBuilder sb = new StringBuilder();
                                sb.append("Schedule for ").append(selectedDayOfWeek).append(" (").append(dateStr).append("):\n\n");

                                if (eventLines.isEmpty()) {
                                    sb.append("No classes or scheduled events on this date.\n");
                                } else {
                                    for (String line : eventLines) {
                                        sb.append(line).append("\n");
                                    }
                                }

                                if (!activeDays.isEmpty()) {
                                    sb.append("\n🗓️ Weekly Teaching Days: ").append(String.join(", ", activeDays));
                                }

                                tvEventsForDate.setText(sb.toString().trim());
                            })
                            .addOnFailureListener(e -> {
                                StringBuilder sb = new StringBuilder();
                                sb.append("Schedule for ").append(selectedDayOfWeek).append(" (").append(dateStr).append("):\n\n");
                                if (eventLines.isEmpty()) {
                                    sb.append("No classes scheduled on this date.");
                                } else {
                                    for (String line : eventLines) {
                                        sb.append(line).append("\n");
                                    }
                                }
                                tvEventsForDate.setText(sb.toString().trim());
                            });
                })
                .addOnFailureListener(e -> {
                    tvEventsForDate.setText("Unable to load events: " + e.getMessage());
                });
    }
}
