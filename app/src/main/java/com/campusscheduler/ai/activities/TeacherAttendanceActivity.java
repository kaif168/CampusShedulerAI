package com.campusscheduler.ai.activities;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.campusscheduler.ai.R;
import com.campusscheduler.ai.adapters.GenericAdapter;
import com.campusscheduler.ai.adapters.TeacherAttendanceAdapter;
import com.campusscheduler.ai.models.AttendanceRecord;
import com.campusscheduler.ai.models.ClassSession;
import com.campusscheduler.ai.models.Student;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.utils.FirebaseHelper;
import com.campusscheduler.ai.utils.LocalAuthManager;
import com.campusscheduler.ai.utils.UIUtils;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TeacherAttendanceActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private Button btnSaveAttendance, btnToggleAudit;
    private Button btnMarkAllPresent, btnMarkAllAbsent, btnRefreshAudit;
    private Button btnOpenDateRegister;
    private TextView tvHeaderTitle, tvAttendanceDate, tvEmptyAttendance;
    private Spinner spClassSession, spFilterSemester, spFilterSection;
    private LinearLayout llSessionConfig, llAuditFilters;
    private ProgressBar pbAttendanceLoading;
    private RecyclerView rvAttendanceStudents, rvAttendanceLogs;

    private TeacherAttendanceAdapter studentAdapter;
    private GenericAdapter<AttendanceRecord> logAdapter;

    private final List<Student> studentList = new ArrayList<>();
    private final Map<String, Boolean> attendanceMap = new HashMap<>();
    private final List<ClassSession> availableSessions = new ArrayList<>();
    private final List<AttendanceRecord> auditLogList = new ArrayList<>();

    private User currentUser;
    private String selectedDate;
    private ClassSession currentSession;
    private boolean isAuditMode = false;
    private boolean isAdmin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_attendance);

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

        isAdmin = User.ROLE_COLLEGE_ADMIN.equalsIgnoreCase(currentUser.getRole()) ||
                User.ROLE_ADMIN.equalsIgnoreCase(currentUser.getRole()) ||
                (getIntent() != null && getIntent().getBooleanExtra("isAdmin", false));

        selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        initializeViews();
        setupListeners();
        setupRecyclerViews();

        if (isAdmin) {
            // Default to Audit view for admins, with ability to toggle to Mark Attendance
            toggleAuditMode(true);
        } else {
            toggleAuditMode(false);
        }

        loadAvailableSessions();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        btnSaveAttendance = findViewById(R.id.btnSaveAttendance);
        btnToggleAudit = findViewById(R.id.btnToggleAudit);
        btnMarkAllPresent = findViewById(R.id.btnMarkAllPresent);
        btnMarkAllAbsent = findViewById(R.id.btnMarkAllAbsent);
        btnRefreshAudit = findViewById(R.id.btnRefreshAudit);
        btnOpenDateRegister = findViewById(R.id.btnOpenDateRegister);

        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        tvAttendanceDate = findViewById(R.id.tvAttendanceDate);
        tvEmptyAttendance = findViewById(R.id.tvEmptyAttendance);

        spClassSession = findViewById(R.id.spClassSession);
        spFilterSemester = findViewById(R.id.spFilterSemester);
        spFilterSection = findViewById(R.id.spFilterSection);

        llSessionConfig = findViewById(R.id.llSessionConfig);
        llAuditFilters = findViewById(R.id.llAuditFilters);
        pbAttendanceLoading = findViewById(R.id.pbAttendanceLoading);

        rvAttendanceStudents = findViewById(R.id.rvAttendanceStudents);
        rvAttendanceLogs = findViewById(R.id.rvAttendanceLogs);

        updateDateDisplay();
    }

    private void setupListeners() {
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        if (btnSaveAttendance != null) btnSaveAttendance.setOnClickListener(v -> saveAttendanceToFirestore());
        if (btnToggleAudit != null) btnToggleAudit.setOnClickListener(v -> toggleAuditMode(!isAuditMode));

        if (tvAttendanceDate != null) {
            tvAttendanceDate.setOnClickListener(v -> showDatePicker());
        }

        if (btnMarkAllPresent != null) {
            btnMarkAllPresent.setOnClickListener(v -> setAllAttendance(true));
        }

        if (btnMarkAllAbsent != null) {
            btnMarkAllAbsent.setOnClickListener(v -> setAllAttendance(false));
        }

        if (btnRefreshAudit != null) {
            btnRefreshAudit.setOnClickListener(v -> loadAuditLogs());
        }

        if (btnOpenDateRegister != null) {
            btnOpenDateRegister.setOnClickListener(v -> showDateRangeConfigDialog());
        }

        if (spClassSession != null) {
            spClassSession.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position >= 0 && position < availableSessions.size()) {
                        currentSession = availableSessions.get(position);
                        loadStudentsForCurrentSession();
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }

        setupAuditFilterSpinners();
    }

    private void setupRecyclerViews() {
        if (rvAttendanceStudents != null) {
            rvAttendanceStudents.setLayoutManager(new LinearLayoutManager(this));
            studentAdapter = new TeacherAttendanceAdapter(studentList, attendanceMap, (student, isPresent) -> {
                String key = student.getStudentId() != null ? student.getStudentId() :
                        (student.getUid() != null ? student.getUid() : student.getUsn());
                if (key != null) {
                    attendanceMap.put(key, isPresent);
                }
            });
            rvAttendanceStudents.setAdapter(studentAdapter);
        }

        if (rvAttendanceLogs != null) {
            rvAttendanceLogs.setLayoutManager(new LinearLayoutManager(this));
            logAdapter = new GenericAdapter<>(auditLogList, (item, holder) -> {
                holder.setText(R.id.tvLogStudentName, item.getStudentName() != null ? item.getStudentName() : "Student");

                String subSec = (item.getSubjectName() != null ? item.getSubjectName() : "Course") +
                        " • " + (item.getSection() != null ? "Sec " + item.getSection() : "Class Session");
                holder.setText(R.id.tvLogSubjectSection, subSec);

                holder.setText(R.id.tvLogDate, "📅 " + (item.getDate() != null ? item.getDate() : ""));

                String byTeacher = item.getTeacherName() != null && !item.getTeacherName().isEmpty() ?
                        "Marked by: " + item.getTeacherName() : "Teacher UID: " + (item.getTeacherUid() != null ? item.getTeacherUid() : "Faculty");
                holder.setText(R.id.tvLogMarkedBy, byTeacher);

                TextView tvBadge = holder.getView(R.id.tvLogStatusBadge);
                if (tvBadge != null) {
                    boolean isPres = "PRESENT".equalsIgnoreCase(item.getStatus()) || item.isPresent();
                    tvBadge.setText(isPres ? "PRESENT" : "ABSENT");
                    tvBadge.setTextColor(isPres ? 0xFF2E7D32 : 0xFFC62828);
                    tvBadge.setBackgroundColor(isPres ? 0xFFE8F5E9 : 0xFFFFEBEE);
                }
            }, R.layout.item_attendance_log);
            rvAttendanceLogs.setAdapter(logAdapter);
        }
    }

    private void toggleAuditMode(boolean audit) {
        isAuditMode = audit;
        if (isAuditMode) {
            if (tvHeaderTitle != null) tvHeaderTitle.setText("Attendance Audit Logs");
            if (btnToggleAudit != null) btnToggleAudit.setText("TAKE ATTENDANCE");
            if (btnSaveAttendance != null) btnSaveAttendance.setVisibility(View.GONE);
            if (llSessionConfig != null) llSessionConfig.setVisibility(View.GONE);
            if (llAuditFilters != null) llAuditFilters.setVisibility(View.VISIBLE);
            if (rvAttendanceStudents != null) rvAttendanceStudents.setVisibility(View.GONE);
            if (rvAttendanceLogs != null) rvAttendanceLogs.setVisibility(View.VISIBLE);
            loadAuditLogs();
        } else {
            if (tvHeaderTitle != null) tvHeaderTitle.setText(isAdmin ? "Admin • Mark Attendance" : "Faculty Attendance");
            if (btnToggleAudit != null) btnToggleAudit.setText("AUDIT LOGS");
            if (btnSaveAttendance != null) btnSaveAttendance.setVisibility(View.VISIBLE);
            if (llSessionConfig != null) llSessionConfig.setVisibility(View.VISIBLE);
            if (llAuditFilters != null) llAuditFilters.setVisibility(View.GONE);
            if (rvAttendanceStudents != null) rvAttendanceStudents.setVisibility(View.VISIBLE);
            if (rvAttendanceLogs != null) rvAttendanceLogs.setVisibility(View.GONE);
            if (currentSession != null) {
                loadStudentsForCurrentSession();
            }
        }
    }

    private void updateDateDisplay() {
        if (tvAttendanceDate != null) {
            tvAttendanceDate.setText("Date: " + selectedDate + " (Click to change)");
        }
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            selectedDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            updateDateDisplay();
            if (!isAuditMode && currentSession != null) {
                loadStudentsForCurrentSession();
            } else if (isAuditMode) {
                loadAuditLogs();
            }
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void setAllAttendance(boolean present) {
        for (Student s : studentList) {
            String key = s.getStudentId() != null ? s.getStudentId() : (s.getUid() != null ? s.getUid() : s.getUsn());
            if (key != null) {
                attendanceMap.put(key, present);
            }
        }
        if (studentAdapter != null) studentAdapter.notifyDataSetChanged();
    }

    private void setupAuditFilterSpinners() {
        String[] semesters = {"All Semesters", "1st Sem", "2nd Sem", "3rd Sem", "4th Sem", "5th Sem", "6th Sem", "7th Sem", "8th Sem"};
        spFilterSemester.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, semesters));

        String[] sections = {"All Sections", "Section A", "Section B", "Section C"};
        spFilterSection.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, sections));
    }

    private void loadAvailableSessions() {
        String collegeId = (currentUser != null && currentUser.getCollegeId() != null && !currentUser.getCollegeId().trim().isEmpty())
                ? currentUser.getCollegeId() : LocalAuthManager.getSafeCollegeId(this, getIntent());

        if (pbAttendanceLoading != null) pbAttendanceLoading.setVisibility(View.VISIBLE);

        FirebaseHelper.getClassSessionsRef()
                .whereEqualTo("collegeId", collegeId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (pbAttendanceLoading != null) pbAttendanceLoading.setVisibility(View.GONE);
                    availableSessions.clear();

                    String teacherUid = currentUser != null ? currentUser.getUid() : null;
                    String teacherName = currentUser != null ? currentUser.getName() : null;

                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            ClassSession cs = doc.toObject(ClassSession.class);
                            if (cs != null) {
                                boolean matches = isAdmin;
                                if (!matches && teacherUid != null && !teacherUid.isEmpty()) {
                                    if (teacherUid.equals(cs.getFacultyId()) || teacherUid.equals(cs.getTeacherUid())) {
                                        matches = true;
                                    }
                                }
                                if (!matches && teacherName != null && !teacherName.isEmpty()) {
                                    if (TeacherDashboardActivity.isSameTeacher(teacherName, cs.getTeacherName()) ||
                                            TeacherDashboardActivity.isSameTeacher(teacherName, cs.getFacultyName())) {
                                        matches = true;
                                    }
                                }
                                if (matches) {
                                    availableSessions.add(cs);
                                }
                            }
                        }
                    }

                    if (availableSessions.isEmpty()) {
                        // Create default active session representation from college subjects if empty
                        ClassSession defaultCs = new ClassSession();
                        defaultCs.setSessionId("SESS_DEF_" + System.currentTimeMillis());
                        defaultCs.setCollegeId(collegeId);
                        defaultCs.setSubjectName("Computer Science Core");
                        defaultCs.setSemester(7);
                        defaultCs.setSection("B");
                        defaultCs.setSectionName("B");
                        availableSessions.add(defaultCs);
                    }

                    List<String> displayList = new ArrayList<>();
                    for (ClassSession cs : availableSessions) {
                        String sName = cs.getSubjectName() != null ? cs.getSubjectName() : "Class";
                        String semSec = (cs.getSemester() > 0 ? cs.getSemester() + "th Sem • " : "") +
                                "Sec " + (cs.getSectionName() != null ? cs.getSectionName() : (cs.getSection() != null ? cs.getSection() : "A"));
                        String time = cs.getStartTime() != null ? " (" + cs.getStartTime() + ")" : "";
                        displayList.add(sName + " • " + semSec + time);
                    }

                    ArrayAdapter<String> sessionAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, displayList);
                    spClassSession.setAdapter(sessionAdapter);

                    if (!availableSessions.isEmpty()) {
                        currentSession = availableSessions.get(0);
                        if (!isAuditMode) {
                            loadStudentsForCurrentSession();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (pbAttendanceLoading != null) pbAttendanceLoading.setVisibility(View.GONE);
                    UIUtils.showToast(this, "Failed to load sessions: " + e.getMessage());
                });
    }

    private void loadStudentsForCurrentSession() {
        if (currentSession == null) return;

        String collegeId = (currentUser != null && currentUser.getCollegeId() != null && !currentUser.getCollegeId().trim().isEmpty())
                ? currentUser.getCollegeId() : LocalAuthManager.getSafeCollegeId(this, getIntent());

        if (pbAttendanceLoading != null) pbAttendanceLoading.setVisibility(View.VISIBLE);
        if (tvEmptyAttendance != null) tvEmptyAttendance.setVisibility(View.GONE);

        String targetSec = currentSession.getSectionName() != null ? currentSession.getSectionName() : currentSession.getSection();

        FirebaseHelper.getStudentsRef()
                .whereEqualTo("collegeId", collegeId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    studentList.clear();
                    attendanceMap.clear();

                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        for (DocumentSnapshot doc : querySnapshot) {
                            Student s = doc.toObject(Student.class);
                            if (s != null && s.getName() != null) {
                                boolean secMatch = targetSec == null || targetSec.isEmpty() ||
                                        targetSec.equalsIgnoreCase(s.getSection()) ||
                                        targetSec.equalsIgnoreCase(s.getSectionName());
                                boolean semMatch = currentSession.getSemester() <= 0 || s.getSemester() <= 0 ||
                                        currentSession.getSemester() == s.getSemester();

                                if (secMatch && semMatch) {
                                    studentList.add(s);
                                    String key = s.getStudentId() != null ? s.getStudentId() : (s.getUid() != null ? s.getUid() : s.getUsn());
                                    if (key != null) {
                                        attendanceMap.put(key, true); // default to Present
                                    }
                                }
                            }
                        }

                        if (studentList.isEmpty()) {
                            if (tvEmptyAttendance != null) {
                                tvEmptyAttendance.setVisibility(View.VISIBLE);
                                String sName = targetSec != null ? targetSec : "A";
                                int semNum = currentSession.getSemester();
                                tvEmptyAttendance.setText("No students enrolled in Semester " + semNum + " Section " + sName + ".");
                            }
                        }
                    }

                    // Check if attendance records already exist in Firestore for this session and date
                    loadExistingAttendanceForSession(collegeId);
                })
                .addOnFailureListener(e -> {
                    if (pbAttendanceLoading != null) pbAttendanceLoading.setVisibility(View.GONE);
                    UIUtils.showToast(this, "Failed to load students: " + e.getMessage());
                });
    }

    private void loadExistingAttendanceForSession(String collegeId) {
        String sessId = currentSession != null && currentSession.getSessionId() != null ? currentSession.getSessionId() : "DEFAULT";

        FirebaseHelper.getAttendanceRef()
                .whereEqualTo("collegeId", collegeId)
                .whereEqualTo("date", selectedDate)
                .get()
                .addOnSuccessListener(snaps -> {
                    if (pbAttendanceLoading != null) pbAttendanceLoading.setVisibility(View.GONE);
                    if (snaps != null && !snaps.isEmpty()) {
                        for (DocumentSnapshot doc : snaps) {
                            AttendanceRecord rec = doc.toObject(AttendanceRecord.class);
                            if (rec != null) {
                                String sKey = rec.getStudentId() != null ? rec.getStudentId() : rec.getStudentUid();
                                if (sKey != null) {
                                    boolean pres = "PRESENT".equalsIgnoreCase(rec.getStatus()) || rec.isPresent();
                                    attendanceMap.put(sKey, pres);
                                }
                            }
                        }
                    }

                    if (studentList.isEmpty()) {
                        if (tvEmptyAttendance != null) {
                            tvEmptyAttendance.setText("No students enrolled for Section " +
                                    (currentSession != null && currentSession.getSectionName() != null ? currentSession.getSectionName() : "") + ".");
                            tvEmptyAttendance.setVisibility(View.VISIBLE);
                        }
                    }

                    if (studentAdapter != null) studentAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    if (pbAttendanceLoading != null) pbAttendanceLoading.setVisibility(View.GONE);
                    if (studentAdapter != null) studentAdapter.notifyDataSetChanged();
                });
    }

    private void saveAttendanceToFirestore() {
        if (studentList.isEmpty()) {
            UIUtils.showToast(this, "No students to save attendance for.");
            return;
        }

        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Submitting attendance to Cloud Firestore...");
        progress.setCancelable(false);
        progress.show();

        String collegeId = (currentUser != null && currentUser.getCollegeId() != null && !currentUser.getCollegeId().trim().isEmpty())
                ? currentUser.getCollegeId() : LocalAuthManager.getSafeCollegeId(this, getIntent());
        String teacherUid = currentUser != null && currentUser.getUid() != null ? currentUser.getUid() : "FACULTY_AUTH";
        String teacherName = currentUser != null && currentUser.getName() != null ? currentUser.getName() : "Faculty";

        String sessId = currentSession != null && currentSession.getSessionId() != null ? currentSession.getSessionId() : "SESS_" + System.currentTimeMillis();
        String subName = currentSession != null && currentSession.getSubjectName() != null ? currentSession.getSubjectName() : "Core Subject";
        String secName = currentSession != null && currentSession.getSectionName() != null ? currentSession.getSectionName() : "A";
        int sem = currentSession != null ? currentSession.getSemester() : 7;

        WriteBatch batch = FirebaseHelper.getFirestore().batch();

        for (Student s : studentList) {
            String studentKey = s.getStudentId() != null ? s.getStudentId() : (s.getUid() != null ? s.getUid() : s.getUsn());
            String studentUid = s.getUid() != null ? s.getUid() : studentKey;
            boolean isPresent = attendanceMap.containsKey(studentKey) ? Boolean.TRUE.equals(attendanceMap.get(studentKey)) : true;

            // Deterministic document ID to prevent duplicate attendance records
            String recordId = "ATT_" + collegeId + "_" + sessId + "_" + studentKey + "_" + selectedDate;

            AttendanceRecord record = new AttendanceRecord();
            record.setRecordId(recordId);
            record.setAttendanceId(recordId);
            record.setCollegeId(collegeId);
            record.setTeacherUid(teacherUid);
            record.setFacultyId(teacherUid);
            record.setTeacherName(teacherName);
            record.setSessionId(sessId);
            record.setStudentId(studentUid);
            record.setStudentUid(studentUid);
            record.setStudentName(s.getName());
            record.setStudentUsn(s.getUsn());
            record.setSubjectName(subName);
            record.setSection(secName);
            record.setSemester(sem);
            record.setDate(selectedDate);
            record.setStatus(isPresent ? "PRESENT" : "ABSENT");
            record.setPresent(isPresent);
            record.setTimestamp(System.currentTimeMillis());

            batch.set(FirebaseHelper.getAttendanceRef().document(recordId), record);
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    progress.dismiss();
                    UIUtils.showAlert(this, "Attendance Confirmed",
                            "Attendance for " + studentList.size() + " students successfully persisted to Cloud Firestore for date " + selectedDate + ".");
                })
                .addOnFailureListener(e -> {
                    progress.dismiss();
                    UIUtils.showAlert(this, "Save Failed", "Cloud Firestore error: " + e.getMessage());
                });
    }

    private void loadAuditLogs() {
        String collegeId = (currentUser != null && currentUser.getCollegeId() != null && !currentUser.getCollegeId().trim().isEmpty())
                ? currentUser.getCollegeId() : LocalAuthManager.getSafeCollegeId(this, getIntent());

        if (pbAttendanceLoading != null) pbAttendanceLoading.setVisibility(View.VISIBLE);
        if (tvEmptyAttendance != null) tvEmptyAttendance.setVisibility(View.GONE);

        Query q = FirebaseHelper.getAttendanceRef().whereEqualTo("collegeId", collegeId);

        q.get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (pbAttendanceLoading != null) pbAttendanceLoading.setVisibility(View.GONE);
            auditLogList.clear();

            int semFilterPos = spFilterSemester != null ? spFilterSemester.getSelectedItemPosition() : 0;
            int secFilterPos = spFilterSection != null ? spFilterSection.getSelectedItemPosition() : 0;

            if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                for (DocumentSnapshot doc : queryDocumentSnapshots) {
                    AttendanceRecord rec = doc.toObject(AttendanceRecord.class);
                    if (rec != null) {
                        boolean pass = true;
                        if (semFilterPos > 0 && rec.getSemester() > 0 && rec.getSemester() != semFilterPos) {
                            pass = false;
                        }
                        if (secFilterPos == 1 && rec.getSection() != null && !rec.getSection().equalsIgnoreCase("A")) {
                            pass = false;
                        } else if (secFilterPos == 2 && rec.getSection() != null && !rec.getSection().equalsIgnoreCase("B")) {
                            pass = false;
                        } else if (secFilterPos == 3 && rec.getSection() != null && !rec.getSection().equalsIgnoreCase("C")) {
                            pass = false;
                        }

                        if (pass) {
                            auditLogList.add(rec);
                        }
                    }
                }
            }

            if (auditLogList.isEmpty()) {
                if (tvEmptyAttendance != null) {
                    tvEmptyAttendance.setText("No attendance audit records found in Firestore.");
                    tvEmptyAttendance.setVisibility(View.VISIBLE);
                }
            }

            if (logAdapter != null) logAdapter.notifyDataSetChanged();
        }).addOnFailureListener(e -> {
            if (pbAttendanceLoading != null) pbAttendanceLoading.setVisibility(View.GONE);
            UIUtils.showToast(this, "Failed to load audit logs: " + e.getMessage());
        });
    }

    public static class DateWiseAttendanceSession {
        public String date;
        public String dayOfWeek;
        public String section;
        public int semester;
        public String department;
        public String academicYear;
        public String subjectName;
        public String timeSlot;
        public String teacherName;
        public String status;
        public ClassSession classSession;
    }

    private void showDateRangeConfigDialog() {
        if (!isAdmin) {
            UIUtils.showAlert(this, "Admin Only", "Only authorized administrators can configure the institutional attendance working period.");
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Attendance Working-Period Configuration");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = UIUtils.dpToPx(this, 16);
        layout.setPadding(pad, pad / 2, pad, pad / 2);

        // 1. Department
        TextView tvDept = new TextView(this);
        tvDept.setText("Department:");
        tvDept.setTextColor(0xFF0D47A1);
        tvDept.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(tvDept);

        Spinner spDept = new Spinner(this);
        String[] deptOptions = {"Computer Science & Engineering", "Information Science & Engineering", "Electronics & Communication"};
        spDept.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, deptOptions));
        layout.addView(spDept);

        // 2. Academic Year
        TextView tvAy = new TextView(this);
        tvAy.setText("Academic Year:");
        tvAy.setTextColor(0xFF0D47A1);
        tvAy.setTypeface(null, android.graphics.Typeface.BOLD);
        tvAy.setPadding(0, pad / 3, 0, 0);
        layout.addView(tvAy);

        Spinner spAy = new Spinner(this);
        String[] ayOptions = {"2026-27", "2025-26", "2024-25"};
        spAy.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, ayOptions));
        layout.addView(spAy);

        // 3. Semester
        TextView tvSem = new TextView(this);
        tvSem.setText("Semester:");
        tvSem.setTextColor(0xFF0D47A1);
        tvSem.setTypeface(null, android.graphics.Typeface.BOLD);
        tvSem.setPadding(0, pad / 3, 0, 0);
        layout.addView(tvSem);

        Spinner spSem = new Spinner(this);
        String[] semOptions = {"1st Sem", "2nd Sem", "3rd Sem", "4th Sem", "5th Sem", "6th Sem", "7th Sem", "8th Sem"};
        spSem.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, semOptions));
        int defSem = (currentSession != null && currentSession.getSemester() >= 1 && currentSession.getSemester() <= 8)
                ? currentSession.getSemester() - 1 : 6;
        spSem.setSelection(defSem);
        layout.addView(spSem);

        // 4. Section
        TextView tvSec = new TextView(this);
        tvSec.setText("Section:");
        tvSec.setTextColor(0xFF0D47A1);
        tvSec.setTypeface(null, android.graphics.Typeface.BOLD);
        tvSec.setPadding(0, pad / 3, 0, 0);
        layout.addView(tvSec);

        Spinner spSec = new Spinner(this);
        String[] secOptions = {"A", "B", "C", "D"};
        spSec.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, secOptions));
        if (currentSession != null && currentSession.getSectionName() != null) {
            String curSec = currentSession.getSectionName();
            for (int i = 0; i < secOptions.length; i++) {
                if (secOptions[i].equalsIgnoreCase(curSec)) {
                    spSec.setSelection(i);
                    break;
                }
            }
        }
        layout.addView(spSec);

        // 5. Working Days
        TextView tvWd = new TextView(this);
        tvWd.setText("Working Days Configuration:");
        tvWd.setTextColor(0xFF0D47A1);
        tvWd.setTypeface(null, android.graphics.Typeface.BOLD);
        tvWd.setPadding(0, pad / 3, 0, 0);
        layout.addView(tvWd);

        Spinner spWd = new Spinner(this);
        String[] wdOptions = {
                "Monday – Friday (Regular) + 2nd/4th Saturday",
                "Monday – Friday (Full Days Only)",
                "Monday – Saturday (All Working Days)"
        };
        spWd.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, wdOptions));
        layout.addView(spWd);

        // 6. Dates
        Calendar calStart = Calendar.getInstance();
        calStart.set(Calendar.DAY_OF_MONTH, 1);
        final String[] startDateStr = {new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calStart.getTime())};

        TextView tvStartLabel = new TextView(this);
        tvStartLabel.setText("Working Period Start Date:");
        tvStartLabel.setTextColor(0xFF0D47A1);
        tvStartLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        tvStartLabel.setPadding(0, pad / 3, 0, 0);
        layout.addView(tvStartLabel);

        TextView tvStartVal = new TextView(this);
        tvStartVal.setText(startDateStr[0] + " (Click to change)");
        tvStartVal.setTextColor(0xFF1565C0);
        tvStartVal.setOnClickListener(v -> {
            new DatePickerDialog(this, (view, year, month, day) -> {
                startDateStr[0] = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day);
                tvStartVal.setText(startDateStr[0] + " (Click to change)");
            }, calStart.get(Calendar.YEAR), calStart.get(Calendar.MONTH), calStart.get(Calendar.DAY_OF_MONTH)).show();
        });
        layout.addView(tvStartVal);

        Calendar calEnd = Calendar.getInstance();
        calEnd.set(Calendar.DAY_OF_MONTH, calEnd.getActualMaximum(Calendar.DAY_OF_MONTH));
        final String[] endDateStr = {new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calEnd.getTime())};

        TextView tvEndLabel = new TextView(this);
        tvEndLabel.setText("Working Period End Date:");
        tvEndLabel.setTextColor(0xFF0D47A1);
        tvEndLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        tvEndLabel.setPadding(0, pad / 3, 0, 0);
        layout.addView(tvEndLabel);

        TextView tvEndVal = new TextView(this);
        tvEndVal.setText(endDateStr[0] + " (Click to change)");
        tvEndVal.setTextColor(0xFF1565C0);
        tvEndVal.setOnClickListener(v -> {
            new DatePickerDialog(this, (view, year, month, day) -> {
                endDateStr[0] = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day);
                tvEndVal.setText(endDateStr[0] + " (Click to change)");
            }, calEnd.get(Calendar.YEAR), calEnd.get(Calendar.MONTH), calEnd.get(Calendar.DAY_OF_MONTH)).show();
        });
        layout.addView(tvEndVal);

        builder.setView(layout);
        builder.setPositiveButton("Generate Register", (dialog, which) -> {
            if (startDateStr[0].compareTo(endDateStr[0]) > 0) {
                UIUtils.showAlert(this, "Invalid Date Range", "Working-period start date (" + startDateStr[0] + ") cannot be after end date (" + endDateStr[0] + ").");
                return;
            }
            String selectedDept = deptOptions[spDept.getSelectedItemPosition()];
            String selectedAy = ayOptions[spAy.getSelectedItemPosition()];
            int selectedSem = spSem.getSelectedItemPosition() + 1;
            String selectedSec = secOptions[spSec.getSelectedItemPosition()];
            int workingDaysMode = spWd.getSelectedItemPosition();
            generateDateWiseAttendanceRegister(selectedDept, selectedAy, selectedSem, selectedSec, startDateStr[0], endDateStr[0], workingDaysMode);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void generateDateWiseAttendanceRegister(String targetDept, String targetAy, int targetSem, String targetSec,
                                                    String startDtStr, String endDtStr, int workingDaysMode) {
        String collegeId = (currentUser != null && currentUser.getCollegeId() != null && !currentUser.getCollegeId().trim().isEmpty())
                ? currentUser.getCollegeId() : LocalAuthManager.getSafeCollegeId(this, getIntent());

        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage("Generating date-wise calendar for " + targetDept + " Sem " + targetSem + "-" + targetSec + "...");
        progress.setCancelable(false);
        progress.show();

        FirebaseHelper.getClassSessionsRef().whereEqualTo("collegeId", collegeId).get()
                .addOnSuccessListener(sessionSnaps -> {
                    List<ClassSession> cohortSessions = new ArrayList<>();
                    if (sessionSnaps != null) {
                        for (DocumentSnapshot doc : sessionSnaps) {
                            ClassSession cs = doc.toObject(ClassSession.class);
                            if (cs != null) {
                                boolean semMatch = cs.getSemester() <= 0 || cs.getSemester() == targetSem;
                                String sSec = cs.getSectionName() != null ? cs.getSectionName() : cs.getSection();
                                boolean secMatch = sSec == null || sSec.equalsIgnoreCase(targetSec);
                                if (semMatch && secMatch) {
                                    cohortSessions.add(cs);
                                }
                            }
                        }
                    }

                    FirebaseHelper.getAttendanceRef().whereEqualTo("collegeId", collegeId)
                            .get()
                            .addOnSuccessListener(attSnaps -> {
                                progress.dismiss();
                                Map<String, Boolean> existingAttendanceDates = new HashMap<>();
                                if (attSnaps != null) {
                                    for (DocumentSnapshot doc : attSnaps) {
                                        AttendanceRecord ar = doc.toObject(AttendanceRecord.class);
                                        if (ar != null && ar.getDate() != null) {
                                            if (ar.getDate().compareTo(startDtStr) >= 0 && ar.getDate().compareTo(endDtStr) <= 0) {
                                                String key = ar.getDate() + "@" + (ar.getSessionId() != null ? ar.getSessionId() : ar.getSubjectName());
                                                existingAttendanceDates.put(key, true);
                                                existingAttendanceDates.put(ar.getDate(), true);
                                            }
                                        }
                                    }
                                }

                                buildAndDisplayDateWiseList(targetDept, targetAy, targetSem, targetSec, startDtStr, endDtStr,
                                        workingDaysMode, cohortSessions, existingAttendanceDates);
                            })
                            .addOnFailureListener(e -> {
                                progress.dismiss();
                                UIUtils.showAlert(this, "Attendance Check Failed", e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    progress.dismiss();
                    UIUtils.showAlert(this, "Schedule Load Failed", e.getMessage());
                });
    }

    private void buildAndDisplayDateWiseList(String targetDept, String targetAy, int targetSem, String targetSec,
                                             String startDtStr, String endDtStr, int workingDaysMode,
                                             List<ClassSession> cohortSessions, Map<String, Boolean> existingAttendance) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        SimpleDateFormat daySdf = new SimpleDateFormat("EEEE", Locale.US);
        String todayStr = sdf.format(new Date());

        Calendar startCal = Calendar.getInstance();
        Calendar endCal = Calendar.getInstance();
        try {
            startCal.setTime(sdf.parse(startDtStr));
            endCal.setTime(sdf.parse(endDtStr));
        } catch (Exception e) {
            UIUtils.showAlert(this, "Date Error", "Invalid date range: " + e.getMessage());
            return;
        }

        List<DateWiseAttendanceSession> dateSessions = new ArrayList<>();

        while (!startCal.after(endCal)) {
            int dayOfWeekInt = startCal.get(Calendar.DAY_OF_WEEK);
            String currentDate = sdf.format(startCal.getTime());
            String dayName = daySdf.format(startCal.getTime());

            if (dayOfWeekInt == Calendar.SUNDAY) {
                startCal.add(Calendar.DAY_OF_MONTH, 1);
                continue;
            }

            String effectiveScheduleDay = dayName;
            if (dayOfWeekInt == Calendar.SATURDAY) {
                if (workingDaysMode == 1) { // Mon-Fri only
                    startCal.add(Calendar.DAY_OF_MONTH, 1);
                    continue;
                } else if (workingDaysMode == 0) { // Regular + 2nd/4th Sat
                    int dayOfMonth = startCal.get(Calendar.DAY_OF_MONTH);
                    int satNumber = (dayOfMonth - 1) / 7 + 1;

                    if (satNumber == 1 || satNumber == 3 || satNumber == 5) {
                        startCal.add(Calendar.DAY_OF_MONTH, 1);
                        continue;
                    } else if (satNumber == 2) {
                        effectiveScheduleDay = "Monday";
                    } else if (satNumber == 4) {
                        effectiveScheduleDay = "Tuesday";
                    }
                }
            }

            List<ClassSession> dayClasses = new ArrayList<>();
            for (ClassSession cs : cohortSessions) {
                String csDay = cs.getDayOfWeek() != null ? cs.getDayOfWeek() : "Monday";
                if (csDay.equalsIgnoreCase(effectiveScheduleDay)) {
                    if ("Friday".equalsIgnoreCase(dayName)) {
                        int startMin = UIUtils.parseTimeToMinutes(cs.getStartTime());
                        if (startMin >= 770) {
                            continue;
                        }
                    }
                    dayClasses.add(cs);
                }
            }

            if (dayClasses.isEmpty() && !cohortSessions.isEmpty()) {
                dayClasses.add(cohortSessions.get(0));
            }

            for (ClassSession cs : dayClasses) {
                DateWiseAttendanceSession dws = new DateWiseAttendanceSession();
                dws.date = currentDate;
                dws.dayOfWeek = dayName;
                dws.department = targetDept;
                dws.academicYear = targetAy;
                dws.semester = targetSem;
                dws.section = targetSec;
                dws.subjectName = cs.getSubjectName() != null ? cs.getSubjectName() : "Core Subject";
                dws.timeSlot = (cs.getStartTime() != null ? cs.getStartTime() : "08:20 AM") + " - " +
                        (cs.getEndTime() != null ? cs.getEndTime() : "09:10 AM");
                dws.teacherName = cs.getTeacherName() != null ? cs.getTeacherName() : (cs.getFacultyName() != null ? cs.getFacultyName() : "Assigned Faculty");
                dws.classSession = cs;

                String attKey = currentDate + "@" + (cs.getSessionId() != null ? cs.getSessionId() : cs.getSubjectName());
                boolean isCompleted = existingAttendance.containsKey(attKey) || existingAttendance.containsKey(currentDate);

                if (isCompleted) {
                    dws.status = "COMPLETED";
                } else if (currentDate.equals(todayStr)) {
                    dws.status = "TODAY - OPEN";
                } else if (currentDate.compareTo(todayStr) < 0) {
                    dws.status = "AWAITING ATTENDANCE";
                } else {
                    dws.status = "UPCOMING";
                }

                dateSessions.add(dws);
            }

            startCal.add(Calendar.DAY_OF_MONTH, 1);
        }

        if (dateSessions.isEmpty()) {
            UIUtils.showAlert(this, "No Sessions Generated",
                    "No valid academic sessions found for " + targetDept + " Semester " + targetSem + " Section " + targetSec + " in the selected date range.");
            return;
        }

        showDateWiseListDialog(targetDept, targetAy, targetSem, targetSec, startDtStr, endDtStr, dateSessions);
    }

    private void showDateWiseListDialog(String targetDept, String targetAy, int targetSem, String targetSec,
                                        String startDtStr, String endDtStr, List<DateWiseAttendanceSession> dateSessions) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Attendance Register: " + targetDept + "\n" + targetAy + " • Sem " + targetSem + "-" + targetSec +
                " (" + dateSessions.size() + " Sessions • " + startDtStr + " to " + endDtStr + ")");

        List<String> displayItems = new ArrayList<>();
        for (DateWiseAttendanceSession s : dateSessions) {
            String badge = "[" + s.status + "]";
            displayItems.add("📅 " + s.date + " (" + s.dayOfWeek + ") • Sem " + s.semester + "-" + s.section + "\n" +
                    s.subjectName + " • " + s.timeSlot + "\n" +
                    "Faculty: " + s.teacherName + "  " + badge);
        }

        builder.setItems(displayItems.toArray(new String[0]), (dialog, which) -> {
            if (which >= 0 && which < dateSessions.size()) {
                DateWiseAttendanceSession selected = dateSessions.get(which);
                selectedDate = selected.date;
                currentSession = selected.classSession;

                updateDateDisplay();
                loadStudentsForCurrentSession();

                UIUtils.showToast(this, "Opened Attendance for " + selected.date + " (Sem " + selected.semester + "-" + selected.section + ")");
            }
        });

        builder.setNegativeButton("Close", null);
        builder.show();
    }
}
