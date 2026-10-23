package com.campusscheduler.ai.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.campusscheduler.ai.R;
import com.campusscheduler.ai.adapters.GenericAdapter;
import com.campusscheduler.ai.engine.ClassDetectionEngine;
import com.campusscheduler.ai.models.ClassSession;
import com.campusscheduler.ai.models.Student;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.utils.CollegeIdentityHelper;
import com.campusscheduler.ai.utils.FirebaseHelper;
import com.campusscheduler.ai.utils.LocalAuthManager;
import com.campusscheduler.ai.utils.ProfilePhotoHelper;
import com.campusscheduler.ai.utils.UIUtils;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StudentDashboardActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navView;
    private ImageButton btnMenu, btnNotification;
    private TextView tvNotificationBadge;
    private ImageView ivProfileAvatar, ivCollegeLogo;
    private TextView tvStudentName, tvStudentDegree, tvStudentSemSec, tvStudentCollege, tvDateCard;
    private TextView tvCurrentClassSubject, tvCurrentClassInfo;
    private CardView btnAttendance, btnAssignments, btnExaminations, btnIA, btnAskAI, cvCurrentClass;
    private RecyclerView rvTodaysClasses;
    private GenericAdapter<ClassSession> todaysClassesAdapter;
    private List<ClassSession> todaysClassesList = new ArrayList<>();
    private User currentUser;

    private final ActivityResultLauncher<Intent> photoPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        ProfilePhotoHelper.uploadProfilePhoto(this, imageUri, currentUser, new ProfilePhotoHelper.OnPhotoUploadedCallback() {
                            @Override
                            public void onSuccess(String photoUrl) {
                                bindStudentProfileData();
                                setupHeaderView();
                            }

                            @Override
                            public void onFailure(Exception e) {}
                        });
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        currentUser = LocalAuthManager.getCurrentLocalUser(this);
        String safeCollegeId = LocalAuthManager.getSafeCollegeId(this, getIntent());
        if (currentUser == null) {
            currentUser = new User();
            currentUser.setCollegeId(safeCollegeId);
            currentUser.setDepartmentId("DEPT_CSE");
            currentUser.setRole("student");
            currentUser.setName("Engineering Student");
        } else if (currentUser.getCollegeId() == null || currentUser.getCollegeId().trim().isEmpty()) {
            currentUser.setCollegeId(safeCollegeId);
        }

        drawerLayout = findViewById(R.id.drawerLayout);
        navView = findViewById(R.id.navView);
        btnMenu = findViewById(R.id.btnMenu);
        btnNotification = findViewById(R.id.btnNotification);
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge);
        ivCollegeLogo = findViewById(R.id.ivCollegeLogo);
        ivProfileAvatar = findViewById(R.id.ivProfileAvatar);

        tvStudentName = findViewById(R.id.tvStudentName);
        tvStudentDegree = findViewById(R.id.tvStudentDegree);
        tvStudentSemSec = findViewById(R.id.tvStudentSemSec);
        tvStudentCollege = findViewById(R.id.tvStudentCollege);
        tvDateCard = findViewById(R.id.tvDateCard);

        tvCurrentClassSubject = findViewById(R.id.tvCurrentClassSubject);
        tvCurrentClassInfo = findViewById(R.id.tvCurrentClassInfo);
        cvCurrentClass = findViewById(R.id.cvCurrentClass);

        rvTodaysClasses = findViewById(R.id.rvTodaysClasses);

        btnAttendance = findViewById(R.id.btnAttendance);
        btnAssignments = findViewById(R.id.btnAssignments);
        btnExaminations = findViewById(R.id.btnExaminations);
        btnIA = findViewById(R.id.btnIA);
        btnAskAI = findViewById(R.id.btnAskAI);

        if (navView != null) {
            navView.setNavigationItemSelectedListener(this);
            setupHeaderView();
        }

        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> {
                if (drawerLayout != null) {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            });
        }

        if (btnNotification != null) {
            btnNotification.setOnClickListener(v -> openActivity(StudentNotificationActivity.class));
        }

        if (ivProfileAvatar != null) {
            ivProfileAvatar.setOnClickListener(v -> ProfilePhotoHelper.showPhotoOptionsDialog(this, currentUser, photoPickerLauncher, () -> {
                bindStudentProfileData();
                setupHeaderView();
            }));
        }

        if (cvCurrentClass != null) {
            cvCurrentClass.setOnClickListener(v -> openActivity(StudentClassDetectionActivity.class));
        }

        if (btnAttendance != null) btnAttendance.setOnClickListener(v -> openActivity(StudentAcademicsActivity.class));
        if (btnAssignments != null) btnAssignments.setOnClickListener(v -> openActivity(StudentAssignmentActivity.class));
        if (btnExaminations != null) btnExaminations.setOnClickListener(v -> openActivity(StudentExamActivity.class));
        if (btnIA != null) btnIA.setOnClickListener(v -> openActivity(StudentExamActivity.class));
        if (btnAskAI != null) btnAskAI.setOnClickListener(v -> openActivity(StudentChatActivity.class));

        // Setup Todays Classes RecyclerView
        if (rvTodaysClasses != null) {
            rvTodaysClasses.setLayoutManager(new LinearLayoutManager(this));
            todaysClassesAdapter = new GenericAdapter<>(todaysClassesList, (item, holder) -> {
                holder.setText(R.id.tvTitle, item.getSubjectName() + " (" + (item.getStartTime() != null ? item.getStartTime() : "TBD") + ")");
                holder.setText(R.id.tvSubtitle, "Room: " + (item.getRoomNumber() != null ? item.getRoomNumber() : "TBD") +
                        " | Faculty: " + (item.getTeacherName() != null ? item.getTeacherName() : "Faculty") +
                        " | Status: " + (item.getStatus() != null ? item.getStatus() : "SCHEDULED"));
            }, R.layout.item_generic);
            rvTodaysClasses.setAdapter(todaysClassesAdapter);
        }

        bindStudentProfileData();
        loadRealClassData();
        refreshStudentProfileFromFirestore();
    }

    private void refreshStudentProfileFromFirestore() {
        if (currentUser == null) return;
        String uid = currentUser.getUid();
        String email = currentUser.getEmail();
        if (uid == null && FirebaseHelper.getAuth().getCurrentUser() != null) {
            uid = FirebaseHelper.getAuth().getCurrentUser().getUid();
        }

        if (uid != null) {
            FirebaseHelper.getUsersRef().document(uid).get().addOnSuccessListener(doc -> {
                if (doc != null && doc.exists()) {
                    User u = doc.toObject(User.class);
                    if (u != null) {
                        applyUpdatedUser(u);
                    }
                }
            });
        }

        if (email != null && !email.trim().isEmpty()) {
            FirebaseHelper.getStudentsRef().whereEqualTo("email", email.trim()).get().addOnSuccessListener(snap -> {
                if (snap != null && !snap.isEmpty()) {
                    Student s = snap.getDocuments().get(0).toObject(Student.class);
                    if (s != null) {
                        boolean changed = false;
                        if (s.getSemester() > 0) {
                            String semStr = "Semester " + s.getSemester();
                            if (!semStr.equalsIgnoreCase(currentUser.getSemester())) {
                                currentUser.setSemester(semStr);
                                changed = true;
                            }
                        }
                        if (s.getSection() != null && !s.getSection().trim().isEmpty()) {
                            if (!s.getSection().equalsIgnoreCase(currentUser.getSectionId())) {
                                currentUser.setSectionId(s.getSection());
                                changed = true;
                            }
                        }
                        if (s.getUsn() != null && !s.getUsn().trim().isEmpty()) {
                            if (!s.getUsn().equalsIgnoreCase(currentUser.getUsn())) {
                                currentUser.setUsn(s.getUsn());
                                changed = true;
                            }
                        }
                        if (s.getName() != null && !s.getName().trim().isEmpty() && !s.getName().equalsIgnoreCase(currentUser.getName())) {
                            currentUser.setName(s.getName());
                            changed = true;
                        }
                        if (changed) {
                            LocalAuthManager.saveUserSession(StudentDashboardActivity.this, currentUser);
                            bindStudentProfileData();
                            setupHeaderView();
                            loadRealClassData();
                        }
                    }
                }
            });
        }
    }

    private void applyUpdatedUser(User u) {
        boolean changed = false;
        if (u.getName() != null && !u.getName().equalsIgnoreCase(currentUser.getName())) {
            currentUser.setName(u.getName());
            changed = true;
        }
        if (u.getSemester() != null && !u.getSemester().equalsIgnoreCase(currentUser.getSemester())) {
            currentUser.setSemester(u.getSemester());
            changed = true;
        }
        if (u.getSectionId() != null && !u.getSectionId().equalsIgnoreCase(currentUser.getSectionId())) {
            currentUser.setSectionId(u.getSectionId());
            changed = true;
        }
        if (u.getDepartmentName() != null && !u.getDepartmentName().equalsIgnoreCase(currentUser.getDepartmentName())) {
            currentUser.setDepartmentName(u.getDepartmentName());
            changed = true;
        }
        if (u.getDepartmentId() != null && !u.getDepartmentId().equalsIgnoreCase(currentUser.getDepartmentId())) {
            currentUser.setDepartmentId(u.getDepartmentId());
            changed = true;
        }
        if (u.getDegree() != null && !u.getDegree().equalsIgnoreCase(currentUser.getDegree())) {
            currentUser.setDegree(u.getDegree());
            changed = true;
        }
        if (u.getUsn() != null && !u.getUsn().equalsIgnoreCase(currentUser.getUsn())) {
            currentUser.setUsn(u.getUsn());
            changed = true;
        }
        if (u.getPhotoUrl() != null && !u.getPhotoUrl().equalsIgnoreCase(currentUser.getPhotoUrl())) {
            currentUser.setPhotoUrl(u.getPhotoUrl());
            changed = true;
        }
        if (changed) {
            LocalAuthManager.saveUserSession(this, currentUser);
            bindStudentProfileData();
            setupHeaderView();
            loadRealClassData();
        }
    }

    private void bindStudentProfileData() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM", Locale.getDefault());
        if (tvDateCard != null) tvDateCard.setText(sdf.format(new Date()));

        if (currentUser == null) return;

        if (tvStudentName != null && currentUser.getName() != null) {
            tvStudentName.setText(currentUser.getName());
        }

        if (tvStudentDegree != null) {
            tvStudentDegree.setText(currentUser.getDegree() != null ? currentUser.getDegree() :
                    (currentUser.getDepartmentName() != null ? currentUser.getDepartmentName() : "Engineering Program"));
        }

        if (tvStudentSemSec != null) {
            String sem = currentUser.getSemester();
            String sec = currentUser.getSectionId();
            if (sem == null && sec == null) {
                tvStudentSemSec.setText("Semester • Section Pending");
            } else if (sem == null) {
                tvStudentSemSec.setText(sec.startsWith("Section") ? sec : ("Section " + sec));
            } else if (sec == null) {
                tvStudentSemSec.setText(sem.startsWith("Semester") ? sem : ("Semester " + sem));
            } else {
                String semDisplay = sem.startsWith("Semester") ? sem : ("Semester " + sem);
                String secDisplay = sec.startsWith("Section") ? sec : ("Section " + sec);
                tvStudentSemSec.setText(semDisplay + " • " + secDisplay);
            }
        }

        if (tvStudentCollege != null) {
            String colName = currentUser.getCollegeName() != null ?
                    CollegeIdentityHelper.formatCollegeName(currentUser.getCollegeName()) : "HKBK College of Engineering";
            tvStudentCollege.setText(colName);
        }

        if (ivCollegeLogo != null) {
            CollegeIdentityHelper.bindCollegeLogo(currentUser.getCollegeId(), currentUser.getCollegeName(), ivCollegeLogo);
        }

        if (ivProfileAvatar != null) {
            ProfilePhotoHelper.bindProfileAvatar(this, currentUser.getPhotoUrl(), ivProfileAvatar);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateNotificationBadge();
        refreshStudentProfileFromFirestore();
    }

    private void updateNotificationBadge() {
        if (tvNotificationBadge == null) return;
        if (currentUser == null) {
            tvNotificationBadge.setVisibility(View.GONE);
            return;
        }

        String uid = currentUser.getUid() != null ? currentUser.getUid() : "default";
        String email = currentUser.getEmail() != null ? currentUser.getEmail().trim().toLowerCase() : "";
        android.content.SharedPreferences sp = getSharedPreferences("campus_notifs", MODE_PRIVATE);
        long lastCleared = Math.max(sp.getLong("cleared_time_" + uid, 0),
                !email.isEmpty() ? sp.getLong("cleared_time_" + email, 0) : 0);

        String collegeId = currentUser.getCollegeId();

        FirebaseHelper.getNotificationsRef()
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    int unreadCount = 0;
                    if (querySnapshots != null && !querySnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : querySnapshots) {
                            com.campusscheduler.ai.models.NotificationItem item =
                                     doc.toObject(com.campusscheduler.ai.models.NotificationItem.class);
                            if (item != null) {
                                String rRole = item.getRecipientRole();
                                String cId = item.getCollegeId();
                                boolean isForStudent = rRole == null || rRole.equalsIgnoreCase("All")
                                        || rRole.equalsIgnoreCase("Student");
                                boolean matchesCollege = collegeId == null || cId == null || cId.isEmpty() || cId.equalsIgnoreCase(collegeId);

                                if (isForStudent && matchesCollege) {
                                    long ts = item.getCreatedAt();
                                    if (ts <= 0 && item.getTimestamp() != null && item.getTimestamp().matches("\\d+")) {
                                        try {
                                            ts = Long.parseLong(item.getTimestamp());
                                        } catch (Exception ignored) {}
                                    }
                                    if (ts > lastCleared) {
                                        unreadCount++;
                                    }
                                }
                            }
                        }
                    }

                    if (unreadCount > 0) {
                        tvNotificationBadge.setText(unreadCount > 9 ? "9+" : String.valueOf(unreadCount));
                        tvNotificationBadge.setVisibility(View.VISIBLE);
                    } else {
                        tvNotificationBadge.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    if (tvNotificationBadge != null) tvNotificationBadge.setVisibility(View.GONE);
                });
    }

    private void setupHeaderView() {
        View headerView = navView.getHeaderView(0);
        if (headerView == null || currentUser == null) return;

        TextView tvNavStudentName = headerView.findViewById(R.id.tvNavStudentName);
        TextView tvNavStudentUsn = headerView.findViewById(R.id.tvNavStudentUsn);
        TextView tvNavStudentDegree = headerView.findViewById(R.id.tvNavStudentDegree);
        TextView tvNavStudentYearSem = headerView.findViewById(R.id.tvNavStudentYearSem);
        TextView tvNavStudentDept = headerView.findViewById(R.id.tvNavStudentDept);
        ImageView ivNavStudentAvatar = headerView.findViewById(R.id.ivNavStudentAvatar);

        if (tvNavStudentName != null && currentUser.getName() != null) {
            tvNavStudentName.setText(currentUser.getName());
        }

        if (tvNavStudentUsn != null) {
            tvNavStudentUsn.setText(currentUser.getUsn() != null ? "ID: " + currentUser.getUsn() :
                    (currentUser.getEmail() != null ? currentUser.getEmail() : "ID: Registered"));
        }

        if (tvNavStudentDegree != null) {
            tvNavStudentDegree.setText(currentUser.getDegree() != null ? currentUser.getDegree() : "B.E. Computer Science Engineering");
        }

        if (tvNavStudentYearSem != null) {
            String yr = currentUser.getYear() != null ? currentUser.getYear() : "Engineering";
            String sem = currentUser.getSemester() != null ? currentUser.getSemester() : "Pending";
            tvNavStudentYearSem.setText("Year: " + yr + " | " + sem);
        }

        if (tvNavStudentDept != null) {
            tvNavStudentDept.setText(currentUser.getDepartmentName() != null ? "Dept: " + currentUser.getDepartmentName() :
                    (currentUser.getDepartmentId() != null ? "Dept: " + currentUser.getDepartmentId() : "Dept: Engineering"));
        }

        if (ivNavStudentAvatar != null) {
            ivNavStudentAvatar.setOnClickListener(v -> ProfilePhotoHelper.showPhotoOptionsDialog(this, currentUser, photoPickerLauncher, () -> {
                bindStudentProfileData();
                setupHeaderView();
            }));
            ProfilePhotoHelper.bindProfileAvatar(this, currentUser.getPhotoUrl(), ivNavStudentAvatar);
        }
    }

    private void openActivity(Class<?> target) {
        Intent intent = new Intent(this, target);
        String collegeId = currentUser != null && currentUser.getCollegeId() != null ?
                currentUser.getCollegeId() : LocalAuthManager.getSafeCollegeId(this, getIntent());
        String studentUid = currentUser != null ? currentUser.getUid() : null;
        if (studentUid == null && FirebaseHelper.getAuth().getCurrentUser() != null) {
            studentUid = FirebaseHelper.getAuth().getCurrentUser().getUid();
        }
        intent.putExtra("collegeId", collegeId);
        intent.putExtra("studentUid", studentUid);
        if (currentUser != null) {
            intent.putExtra("collegeName", currentUser.getCollegeName());
            intent.putExtra("departmentId", currentUser.getDepartmentId());
            intent.putExtra("departmentName", currentUser.getDepartmentName());
            intent.putExtra("semester", currentUser.getSemester());
            intent.putExtra("sectionId", currentUser.getSectionId());
        }
        startActivity(intent);
    }

    private int extractSemesterNumber(String semStr) {
        if (semStr == null) return -1;
        Matcher m = Pattern.compile("(\\d+)").matcher(semStr);
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

    private void loadRealClassData() {
        String collegeId = currentUser != null && currentUser.getCollegeId() != null ?
                currentUser.getCollegeId() : LocalAuthManager.getSafeCollegeId(this, getIntent());

        int studentSem = extractSemesterNumber(currentUser != null ? currentUser.getSemester() : null);
        String userSecNorm = normalizeSection(currentUser != null ? currentUser.getSectionId() : null);

        FirebaseHelper.getClassSessionsRef()
                .whereEqualTo("collegeId", collegeId)
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    todaysClassesList.clear();

                    if (querySnapshots != null && !querySnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : querySnapshots) {
                            ClassSession cs = doc.toObject(ClassSession.class);
                            if (cs != null) {
                                // Match semester if student has verified semester
                                if (studentSem > 0 && cs.getSemester() > 0 && cs.getSemester() != studentSem) {
                                    continue;
                                }

                                // Match section if student has verified section
                                if (!userSecNorm.isEmpty()) {
                                    String csSec = cs.getSection() != null ? cs.getSection() : cs.getSectionId();
                                    String csSecNorm = normalizeSection(csSec);
                                    if (!csSecNorm.isEmpty() && !csSecNorm.equalsIgnoreCase(userSecNorm)) {
                                        continue;
                                    }
                                }
                                todaysClassesList.add(cs);
                            }
                        }
                    }

                    if (todaysClassesAdapter != null) {
                        todaysClassesAdapter.notifyDataSetChanged();
                    }

                    ClassDetectionEngine.DetectionResult detection = ClassDetectionEngine.detectCurrentClass(todaysClassesList);
                    if (detection != null && detection.getSession() != null) {
                        ClassSession active = detection.getSession();
                        if (tvCurrentClassSubject != null) tvCurrentClassSubject.setText(active.getSubjectName());
                        if (tvCurrentClassInfo != null) {
                            tvCurrentClassInfo.setText("Time: " + (active.getStartTime() != null ? active.getStartTime() : "TBD") +
                                    " | Room: " + (active.getRoomNumber() != null ? active.getRoomNumber() : "TBD") +
                                    "\nFaculty: " + (active.getTeacherName() != null ? active.getTeacherName() : "Faculty Member") +
                                    " | Status: " + detection.getStatus().name());
                        }
                    } else {
                        if (tvCurrentClassSubject != null) tvCurrentClassSubject.setText("No Active Class");
                        if (tvCurrentClassInfo != null) {
                            tvCurrentClassInfo.setText(detection != null ? detection.getExplanation() : "No class session currently active.");
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (tvCurrentClassSubject != null) tvCurrentClassSubject.setText("No Class Record Found");
                    if (tvCurrentClassInfo != null) tvCurrentClassInfo.setText("No active timetable sessions published in Firestore yet.");
                });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        if (drawerLayout != null) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }

        int itemId = item.getItemId();
        if (itemId == R.id.nav_academics) {
            openActivity(StudentAcademicsActivity.class);
        } else if (itemId == R.id.nav_admission_type) {
            openActivity(AdmissionTypeActivity.class);
        } else if (itemId == R.id.nav_timetable) {
            openActivity(StudentTimetableActivity.class);
        } else if (itemId == R.id.nav_subjects) {
            openActivity(StudentSubjectsActivity.class);
        } else if (itemId == R.id.nav_calendar) {
            openActivity(TeacherCalendarActivity.class);
        } else if (itemId == R.id.nav_settings) {
            openActivity(SettingsActivity.class);
        } else if (itemId == R.id.nav_profile_change_requests) {
            openActivity(ProfileChangeRequestActivity.class);
        } else if (itemId == R.id.nav_exam_ia) {
            openActivity(StudentExamActivity.class);
        } else if (itemId == R.id.nav_notifications) {
            openActivity(StudentNotificationActivity.class);
        } else if (itemId == R.id.nav_about) {
            openActivity(AboutActivity.class);
        } else if (itemId == R.id.nav_logout) {
            performLogout();
        }
        return true;
    }

    private void performLogout() {
        FirebaseHelper.getAuth().signOut();
        LocalAuthManager.logoutLocalUser(this);
        UIUtils.showToast(this, "Logged out.");
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
