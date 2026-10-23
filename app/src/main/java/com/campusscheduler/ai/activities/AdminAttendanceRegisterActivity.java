package com.campusscheduler.ai.activities;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.campusscheduler.ai.R;
import com.campusscheduler.ai.models.AttendanceRecord;
import com.campusscheduler.ai.models.ClassSession;
import com.campusscheduler.ai.models.Student;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.utils.FirebaseHelper;
import com.campusscheduler.ai.utils.LocalAuthManager;
import com.campusscheduler.ai.utils.UIUtils;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminAttendanceRegisterActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private Spinner spDepartment, spAcademicYear, spSemester, spSection, spWorkingDaysMode;
    private EditText etStartDate, etEndDate;
    private Button btnGenerateRegister;
    private LinearLayout llRegisterStats, llRegisterEntriesContainer;
    private TextView tvTotalSessions, tvCompletedSessions, tvAwaitingSessions, tvRegisterTitle;

    private User currentUser;
    private final List<DateWiseAttendanceSession> generatedSessions = new ArrayList<>();
    private final Map<String, List<AttendanceRecord>> cachedAttendanceMap = new HashMap<>();

    public static class DateWiseAttendanceSession {
        public String date;
        public String dayOfWeek;
        public String department;
        public String academicYear;
        public int semester;
        public String section;
        public String subjectName;
        public String subjectCode;
        public String timeSlot;
        public String teacherName;
        public String roomNumber;
        public String status;
        public ClassSession classSession;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_attendance_register);

        currentUser = LocalAuthManager.getCurrentLocalUser(this);

        btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        spDepartment = findViewById(R.id.spDepartment);
        spAcademicYear = findViewById(R.id.spAcademicYear);
        spSemester = findViewById(R.id.spSemester);
        spSection = findViewById(R.id.spSection);
        spWorkingDaysMode = findViewById(R.id.spWorkingDaysMode);

        etStartDate = findViewById(R.id.etStartDate);
        etEndDate = findViewById(R.id.etEndDate);
        btnGenerateRegister = findViewById(R.id.btnGenerateRegister);

        llRegisterStats = findViewById(R.id.llRegisterStats);
        llRegisterEntriesContainer = findViewById(R.id.llRegisterEntriesContainer);
        tvTotalSessions = findViewById(R.id.tvTotalSessions);
        tvCompletedSessions = findViewById(R.id.tvCompletedSessions);
        tvAwaitingSessions = findViewById(R.id.tvAwaitingSessions);
        tvRegisterTitle = findViewById(R.id.tvRegisterTitle);

        setupSpinners();
        setupDatePickers();

        if (btnGenerateRegister != null) {
            btnGenerateRegister.setOnClickListener(v -> generateWorkingPeriodRegister());
        }

        // Auto-generate on open
        generateWorkingPeriodRegister();
    }

    private void setupSpinners() {
        String[] depts = {"Computer Science & Engineering (DEPT_CSE)", "Information Science (DEPT_ISE)", "Electronics (DEPT_ECE)"};
        spDepartment.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, depts));

        String[] ayList = {"2026-2027", "2025-2026"};
        spAcademicYear.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, ayList));

        String[] sems = {"Semester 7", "Semester 5", "Semester 3", "Semester 1", "Semester 8", "Semester 6", "Semester 4", "Semester 2"};
        spSemester.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, sems));

        String[] secs = {"Section A", "Section B", "Section C", "Section D"};
        spSection.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, secs));

        String[] modes = {
                "Standard (Mon-Fri + 2nd & 4th Sat)",
                "Mon-Fri Only (5-day week)",
                "All Mon-Sat (6-day week)"
        };
        spWorkingDaysMode.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, modes));
    }

    private void setupDatePickers() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Calendar now = Calendar.getInstance();

        // Default start date = 1st of current month
        Calendar start = Calendar.getInstance();
        start.set(Calendar.DAY_OF_MONTH, 1);
        etStartDate.setText(sdf.format(start.getTime()));

        // Default end date = today
        etEndDate.setText(sdf.format(now.getTime()));

        etStartDate.setOnClickListener(v -> {
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                Calendar c = Calendar.getInstance();
                c.set(year, month, dayOfMonth);
                etStartDate.setText(sdf.format(c.getTime()));
            }, start.get(Calendar.YEAR), start.get(Calendar.MONTH), start.get(Calendar.DAY_OF_MONTH)).show();
        });

        etEndDate.setOnClickListener(v -> {
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                Calendar c = Calendar.getInstance();
                c.set(year, month, dayOfMonth);
                etEndDate.setText(sdf.format(c.getTime()));
            }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show();
        });
    }

    private void generateWorkingPeriodRegister() {
        String collegeId = (currentUser != null && currentUser.getCollegeId() != null && !currentUser.getCollegeId().trim().isEmpty())
                ? currentUser.getCollegeId().trim() : "COLLEGE_1789406176044";

        String semStr = spSemester.getSelectedItem() != null ? spSemester.getSelectedItem().toString() : "Semester 7";
        int targetSem = Integer.parseInt(semStr.replaceAll("[^0-9]", ""));
        String secStr = spSection.getSelectedItem() != null ? spSection.getSelectedItem().toString() : "Section A";
        String targetSec = secStr.replace("Section", "").trim();

        String startDtStr = etStartDate.getText().toString().trim();
        String endDtStr = etEndDate.getText().toString().trim();
        int workingDaysMode = spWorkingDaysMode.getSelectedItemPosition();

        if (startDtStr.isEmpty() || endDtStr.isEmpty() || startDtStr.compareTo(endDtStr) > 0) {
            Toast.makeText(this, "Please select a valid date range.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Fetch published class sessions for this cohort
        FirebaseHelper.getClassSessionsRef().whereEqualTo("collegeId", collegeId).get()
                .addOnSuccessListener(csSnaps -> {
                    List<ClassSession> cohortSessions = new ArrayList<>();
                    if (csSnaps != null) {
                        for (DocumentSnapshot doc : csSnaps) {
                            ClassSession cs = doc.toObject(ClassSession.class);
                            if (cs != null && cs.getSemester() == targetSem) {
                                String csSec = cs.getSection() != null ? cs.getSection() : cs.getSectionId();
                                if (csSec != null && csSec.equalsIgnoreCase(targetSec)) {
                                    cohortSessions.add(cs);
                                }
                            }
                        }
                    }

                    // Query attendance logs to check completed dates
                    FirebaseHelper.getAttendanceRecordsRef().whereEqualTo("collegeId", collegeId).get()
                            .addOnSuccessListener(attSnaps -> {
                                cachedAttendanceMap.clear();
                                if (attSnaps != null) {
                                    for (DocumentSnapshot doc : attSnaps) {
                                        AttendanceRecord ar = doc.toObject(AttendanceRecord.class);
                                        if (ar != null && ar.getDate() != null) {
                                            String d = ar.getDate();
                                            cachedAttendanceMap.computeIfAbsent(d, k -> new ArrayList<>()).add(ar);
                                        }
                                    }
                                }

                                buildDateWiseSessions(startDtStr, endDtStr, workingDaysMode, targetSem, targetSec, cohortSessions);
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to query class sessions: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void buildDateWiseSessions(String startDtStr, String endDtStr, int workingDaysMode,
                                       int targetSem, String targetSec, List<ClassSession> cohortSessions) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        SimpleDateFormat daySdf = new SimpleDateFormat("EEEE", Locale.US);
        String todayStr = sdf.format(new Date());

        Calendar startCal = Calendar.getInstance();
        Calendar endCal = Calendar.getInstance();
        try {
            startCal.setTime(sdf.parse(startDtStr));
            endCal.setTime(sdf.parse(endDtStr));
        } catch (Exception e) {
            Toast.makeText(this, "Invalid date format", Toast.LENGTH_SHORT).show();
            return;
        }

        generatedSessions.clear();
        int completedCount = 0;
        int awaitingCount = 0;

        while (!startCal.after(endCal)) {
            int dayOfWeekInt = startCal.get(Calendar.DAY_OF_WEEK);
            String currentDate = sdf.format(startCal.getTime());
            String dayName = daySdf.format(startCal.getTime());

            if (dayOfWeekInt == Calendar.SUNDAY) {
                startCal.add(Calendar.DAY_OF_MONTH, 1);
                continue;
            }

            String effectiveDay = dayName;
            if (dayOfWeekInt == Calendar.SATURDAY) {
                if (workingDaysMode == 1) { // 5-day week
                    startCal.add(Calendar.DAY_OF_MONTH, 1);
                    continue;
                } else if (workingDaysMode == 0) { // 2nd/4th Sat
                    int dayOfMonth = startCal.get(Calendar.DAY_OF_MONTH);
                    int satNum = (dayOfMonth - 1) / 7 + 1;
                    if (satNum == 1 || satNum == 3 || satNum == 5) {
                        startCal.add(Calendar.DAY_OF_MONTH, 1);
                        continue;
                    } else if (satNum == 2) {
                        effectiveDay = "Monday";
                    } else if (satNum == 4) {
                        effectiveDay = "Tuesday";
                    }
                }
            }

            List<ClassSession> dayClasses = new ArrayList<>();
            for (ClassSession cs : cohortSessions) {
                String csDay = cs.getDayOfWeek() != null ? cs.getDayOfWeek() : "Monday";
                if (csDay.equalsIgnoreCase(effectiveDay)) {
                    if ("Friday".equalsIgnoreCase(dayName)) {
                        int startMin = UIUtils.parseTimeToMinutes(cs.getStartTime());
                        if (startMin >= 770) continue; // Recess after 12:50 PM
                    }
                    dayClasses.add(cs);
                }
            }

            // If no sessions for day in timetable, synthesize standard period
            if (dayClasses.isEmpty() && !cohortSessions.isEmpty()) {
                dayClasses.add(cohortSessions.get(0));
            }

            for (ClassSession cs : dayClasses) {
                DateWiseAttendanceSession s = new DateWiseAttendanceSession();
                s.date = currentDate;
                s.dayOfWeek = dayName;
                s.semester = targetSem;
                s.section = targetSec;
                s.subjectName = cs.getSubjectName() != null ? cs.getSubjectName() : "Core Subject";
                s.subjectCode = cs.getSubjectCode() != null ? cs.getSubjectCode() : "CS701";
                s.timeSlot = (cs.getStartTime() != null ? cs.getStartTime() : "08:20 AM") + " - " + (cs.getEndTime() != null ? cs.getEndTime() : "09:10 AM");
                s.teacherName = cs.getFacultyName() != null ? cs.getFacultyName() : (cs.getTeacherName() != null ? cs.getTeacherName() : "Assigned Faculty");
                s.roomNumber = cs.getRoomNumber() != null ? cs.getRoomNumber() : "Hall 301";
                s.classSession = cs;

                boolean hasLogs = cachedAttendanceMap.containsKey(currentDate);
                if (hasLogs) {
                    s.status = "COMPLETED";
                    completedCount++;
                } else if (currentDate.compareTo(todayStr) <= 0) {
                    s.status = "AWAITING ATTENDANCE";
                    awaitingCount++;
                } else {
                    s.status = "UPCOMING";
                }

                generatedSessions.add(s);
            }

            startCal.add(Calendar.DAY_OF_MONTH, 1);
        }

        renderGeneratedSessions();

        if (llRegisterStats != null) {
            llRegisterStats.setVisibility(View.VISIBLE);
            tvTotalSessions.setText(String.valueOf(generatedSessions.size()));
            tvCompletedSessions.setText(String.valueOf(completedCount));
            tvAwaitingSessions.setText(String.valueOf(awaitingCount));
        }
    }

    private void renderGeneratedSessions() {
        if (llRegisterEntriesContainer == null) return;
        llRegisterEntriesContainer.removeAllViews();

        if (generatedSessions.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("No academic sessions found for the specified working period and cohort.");
            tvEmpty.setTextColor(0xFF78909C);
            tvEmpty.setPadding(20, 20, 20, 20);
            tvEmpty.setBackgroundColor(Color.WHITE);
            tvEmpty.setGravity(android.view.Gravity.CENTER);
            llRegisterEntriesContainer.addView(tvEmpty);
            return;
        }

        for (int i = 0; i < generatedSessions.size(); i++) {
            DateWiseAttendanceSession s = generatedSessions.get(i);

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(16, 12, 16, 12);
            card.setBackgroundColor(Color.WHITE);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, 8);
            card.setLayoutParams(lp);
            card.setClickable(true);
            card.setFocusable(true);

            // Row 1: Date & Day + Status Badge
            LinearLayout topRow = new LinearLayout(this);
            topRow.setOrientation(LinearLayout.HORIZONTAL);
            topRow.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

            TextView tvDate = new TextView(this);
            tvDate.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            tvDate.setText("📅 " + s.date + " (" + s.dayOfWeek + ") • Sem " + s.semester + "-" + s.section);
            tvDate.setTextColor(0xFF0D47A1);
            tvDate.setTextSize(13f);
            tvDate.setTypeface(null, Typeface.BOLD);
            topRow.addView(tvDate);

            TextView tvBadge = new TextView(this);
            tvBadge.setTextSize(10.5f);
            tvBadge.setTypeface(null, Typeface.BOLD);
            tvBadge.setPadding(10, 4, 10, 4);

            if ("COMPLETED".equalsIgnoreCase(s.status)) {
                tvBadge.setText("COMPLETED");
                tvBadge.setTextColor(0xFF1B5E20);
                tvBadge.setBackgroundColor(0xFFE8F5E9);
            } else if ("AWAITING ATTENDANCE".equalsIgnoreCase(s.status)) {
                tvBadge.setText("AWAITING LOG");
                tvBadge.setTextColor(0xFFE65100);
                tvBadge.setBackgroundColor(0xFFFFF3E0);
            } else {
                tvBadge.setText("UPCOMING");
                tvBadge.setTextColor(0xFF546E7A);
                tvBadge.setBackgroundColor(0xFFECEFF1);
            }
            topRow.addView(tvBadge);
            card.addView(topRow);

            // Row 2: Subject & Time
            TextView tvSub = new TextView(this);
            tvSub.setText(s.subjectName + " (" + s.subjectCode + ")  •  " + s.timeSlot);
            tvSub.setTextColor(0xFF263238);
            tvSub.setTextSize(12.5f);
            tvSub.setPadding(0, 4, 0, 0);
            card.addView(tvSub);

            // Row 3: Faculty & Room
            TextView tvFac = new TextView(this);
            tvFac.setText("Faculty: " + s.teacherName + "  •  Venue: " + s.roomNumber);
            tvFac.setTextColor(0xFF546E7A);
            tvFac.setTextSize(11.5f);
            card.addView(tvFac);

            // Tapping card opens the read-only breakdown dialog
            card.setOnClickListener(v -> showSessionBreakdownDialog(s));

            llRegisterEntriesContainer.addView(card);
        }
    }

    private void showSessionBreakdownDialog(DateWiseAttendanceSession session) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Attendance Record: " + session.date);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(36, 16, 36, 16);

        TextView tvHeader = new TextView(this);
        tvHeader.setText("• Course: " + session.subjectName + "\n"
                + "• Cohort: Semester " + session.semester + " - Section " + session.section + "\n"
                + "• Time: " + session.timeSlot + "\n"
                + "• Faculty: " + session.teacherName + "\n"
                + "• Venue: " + session.roomNumber);
        tvHeader.setTextSize(13f);
        tvHeader.setTextColor(0xFF263238);
        layout.addView(tvHeader);

        List<AttendanceRecord> records = cachedAttendanceMap.get(session.date);

        if (records == null || records.isEmpty()) {
            TextView tvNotice = new TextView(this);
            tvNotice.setText("\n⚠️ Attendance has not yet been recorded by the assigned faculty for this date and session.\n(Read-only institutional register view.)");
            tvNotice.setTextColor(0xFFE65100);
            tvNotice.setTextSize(13f);
            tvNotice.setTypeface(null, Typeface.BOLD);
            layout.addView(tvNotice);
        } else {
            int presentCount = 0;
            int total = records.size();
            for (AttendanceRecord ar : records) {
                if (ar.isPresent() || "PRESENT".equalsIgnoreCase(ar.getStatus())) {
                    presentCount++;
                }
            }
            int absentCount = total - presentCount;
            double pct = total > 0 ? (presentCount * 100.0) / total : 0.0;

            TextView tvStats = new TextView(this);
            tvStats.setText("\n📊 Recorded Summary:\n"
                    + "• Total Logged: " + total + " students\n"
                    + "• Present: " + presentCount + " | Absent: " + absentCount + " (" + String.format(Locale.US, "%.1f", pct) + "%)\n");
            tvStats.setTextColor(0xFF1B5E20);
            tvStats.setTextSize(13f);
            tvStats.setTypeface(null, Typeface.BOLD);
            layout.addView(tvStats);

            TextView tvRosterTitle = new TextView(this);
            tvRosterTitle.setText("Student Standing List (Read-Only):");
            tvRosterTitle.setTextSize(12.5f);
            tvRosterTitle.setTextColor(0xFF37474F);
            tvRosterTitle.setTypeface(null, Typeface.BOLD);
            layout.addView(tvRosterTitle);

            ScrollView svRoster = new ScrollView(this);
            svRoster.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 350));
            LinearLayout rosterList = new LinearLayout(this);
            rosterList.setOrientation(LinearLayout.VERTICAL);

            for (AttendanceRecord ar : records) {
                TextView tvStu = new TextView(this);
                boolean isP = ar.isPresent() || "PRESENT".equalsIgnoreCase(ar.getStatus());
                String badge = isP ? "[PRESENT ✅]" : "[ABSENT ❌]";
                String sName = ar.getStudentName() != null ? ar.getStudentName() : (ar.getStudentId() != null ? ar.getStudentId() : "Student");
                tvStu.setText(sName + " — " + badge);
                tvStu.setTextColor(isP ? 0xFF2E7D32 : 0xFFC62828);
                tvStu.setTextSize(12f);
                tvStu.setPadding(0, 4, 0, 4);
                rosterList.addView(tvStu);
            }
            svRoster.addView(rosterList);
            layout.addView(svRoster);
        }

        builder.setView(layout);
        builder.setPositiveButton("CLOSE", null);
        builder.show();
    }
}
