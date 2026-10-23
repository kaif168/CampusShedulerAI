package com.campusscheduler.ai.activities;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.campusscheduler.ai.R;
import com.campusscheduler.ai.models.ClassSession;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.utils.CollegeIdentityHelper;
import com.campusscheduler.ai.utils.FirebaseHelper;
import com.campusscheduler.ai.utils.LocalAuthManager;
import com.campusscheduler.ai.utils.UIUtils;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class TeacherDashboardActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navView;
    private ImageButton btnMenu;
    private RelativeLayout btnNotification;
    private TextView tvNotificationBadge;
    private ImageView ivCollegeLogo;
    private ImageView ivHeaderAvatar;
    private ImageView ivNavTeacherAvatar;
    private User currentUser;

    private final androidx.activity.result.ActivityResultLauncher<Intent> photoPickerLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    android.net.Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        com.campusscheduler.ai.utils.ProfilePhotoHelper.uploadProfilePhoto(this, imageUri, currentUser, new com.campusscheduler.ai.utils.ProfilePhotoHelper.OnPhotoUploadedCallback() {
                            @Override
                            public void onSuccess(String photoUrl) {
                                bindUserData();
                            }

                            @Override
                            public void onFailure(Exception e) {}
                        });
                    }
                }
            }
    );

    private TextView tvTeacherTitle;
    private TextView tvTeacherSubtitle;
    private TextView tvTodayDate;
    private TextView tvNextClassSummary;
    private TextView tvNoScheduleText;

    private TextView tvSummaryTodayClassesCount;
    private TextView tvSummaryThisWeekCount;
    private TextView tvSummaryMyStudentsCount;
    private TextView tvSummaryAttendancePercent;
    private TextView tvSummaryPendingAssignmentsCount;
    private TextView tvSummaryUpcomingClassInfo;
    private TextView tvSummaryUpcomingClassTime;

    private CardView btnClassDetectionBanner;

    private CardView btnSummaryTodayClasses;
    private CardView btnSummaryThisWeek;
    private CardView btnSummaryMyStudents;
    private CardView btnSummaryAttendance;
    private CardView btnSummaryPendingAssignments;
    private CardView btnSummaryUpcomingClass;

    private Button btnViewFullTimetable;

    private LinearLayout layoutScheduleContainer;

    private CardView btnQuickSubjects;
    private CardView btnQuickStudents;
    private CardView btnQuickAttendance;
    private CardView btnQuickAssignments;
    private CardView btnQuickResources;
    private CardView btnQuickQuizzes;
    private CardView btnQuickGradebook;
    private CardView btnQuickTimetable;
    private CardView btnQuickCalendar;
    private CardView btnQuickLeave;
    private CardView btnQuickNotifications;
    private CardView btnQuickAiAssistant;

    private CardView cardUpcomingEvents;
    private CardView cardRecentActivity;
    private CardView cardPendingWork;
    private TextView tvUpcomingEventsText;
    private TextView tvRecentActivityText;
    private TextView tvPendingWorkText;

    /*
     * These are the sessions belonging to the logged-in teacher.
     */
    private final List<ClassSession> allTeacherSessions = new ArrayList<>();

    /*
     * Only today's sessions.
     */
    private final List<ClassSession> todaySessions = new ArrayList<>();

    /*
     * Firestore documents are retained separately so we can use
     * fields such as date/teacherUid even if the current ClassSession
     * model does not contain every field.
     */
    private final List<DocumentSnapshot> teacherSessionDocuments =
            new ArrayList<>();

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_teacher_dashboard);

        currentUser = LocalAuthManager.getCurrentLocalUser(this);
        String safeCollegeId = LocalAuthManager.getSafeCollegeId(this, getIntent());
        String teacherUid = getIntent().getStringExtra("teacherUid");
        String teacherName = getIntent().getStringExtra("teacherName");

        if (currentUser == null || !User.ROLE_TEACHER.equalsIgnoreCase(currentUser.getRole())) {
            currentUser = new User();
            currentUser.setRole(User.ROLE_TEACHER);
        }
        if (currentUser.getCollegeId() == null || currentUser.getCollegeId().trim().isEmpty()) {
            currentUser.setCollegeId(safeCollegeId);
        }
        if (teacherUid != null && !teacherUid.trim().isEmpty()) {
            currentUser.setUid(teacherUid);
        }
        if (teacherName != null && !teacherName.trim().isEmpty()) {
            currentUser.setName(teacherName);
        }
        LocalAuthManager.saveUserSession(this, currentUser);

        initViews();
        setupNavigationDrawer();
        bindUserData();
        setupClickListeners();

        loadTeacherProfile();
        loadDashboardData();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (currentUser == null || !User.ROLE_TEACHER.equalsIgnoreCase(currentUser.getRole()) || currentUser.getCollegeId() == null || currentUser.getCollegeId().trim().isEmpty()) {
            currentUser = LocalAuthManager.getCurrentLocalUser(this);
            String safeCollegeId = LocalAuthManager.getSafeCollegeId(this, getIntent());
            if (currentUser == null || !User.ROLE_TEACHER.equalsIgnoreCase(currentUser.getRole())) {
                currentUser = new User();
                currentUser.setRole(User.ROLE_TEACHER);
            }
            if (currentUser.getCollegeId() == null || currentUser.getCollegeId().trim().isEmpty()) {
                currentUser.setCollegeId(safeCollegeId);
            }
            LocalAuthManager.saveUserSession(this, currentUser);
        }
        bindUserData();
        loadDashboardData();
        updateNotificationBadge();
    }

    private void initViews() {

        drawerLayout =
                findViewById(R.id.drawerLayout);

        navView =
                findViewById(R.id.navView);

        btnMenu =
                findViewById(R.id.btnMenu);

        btnNotification =
                findViewById(R.id.btnNotification);

        tvNotificationBadge =
                findViewById(R.id.tvNotificationBadge);

        ivCollegeLogo =
                findViewById(R.id.ivCollegeLogo);

        ivHeaderAvatar =
                findViewById(R.id.ivHeaderAvatar);
        if (ivHeaderAvatar != null) {
            ivHeaderAvatar.setOnClickListener(v -> com.campusscheduler.ai.utils.ProfilePhotoHelper.showPhotoOptionsDialog(this, currentUser, photoPickerLauncher, () -> bindUserData()));
        }

        tvTeacherTitle =
                findViewById(R.id.tvTeacherTitle);

        tvTeacherSubtitle =
                findViewById(R.id.tvTeacherSubtitle);

        tvTodayDate =
                findViewById(R.id.tvTodayDate);

        tvNextClassSummary =
                findViewById(R.id.tvNextClassSummary);

        tvNoScheduleText =
                findViewById(R.id.tvNoScheduleText);

        tvSummaryTodayClassesCount =
                findViewById(R.id.tvSummaryTodayClassesCount);

        tvSummaryThisWeekCount =
                findViewById(R.id.tvSummaryThisWeekCount);

        tvSummaryMyStudentsCount =
                findViewById(R.id.tvSummaryMyStudentsCount);

        tvSummaryAttendancePercent =
                findViewById(R.id.tvSummaryAttendancePercent);

        tvSummaryPendingAssignmentsCount =
                findViewById(R.id.tvSummaryPendingAssignmentsCount);

        tvSummaryUpcomingClassInfo =
                findViewById(R.id.tvSummaryUpcomingClassInfo);

        tvSummaryUpcomingClassTime =
                findViewById(R.id.tvSummaryUpcomingClassTime);

        btnClassDetectionBanner =
                findViewById(R.id.btnClassDetectionBanner);

        btnSummaryTodayClasses =
                findViewById(R.id.btnSummaryTodayClasses);

        btnSummaryThisWeek =
                findViewById(R.id.btnSummaryThisWeek);

        btnSummaryMyStudents =
                findViewById(R.id.btnSummaryMyStudents);

        btnSummaryAttendance =
                findViewById(R.id.btnSummaryAttendance);

        btnSummaryPendingAssignments =
                findViewById(R.id.btnSummaryPendingAssignments);

        btnSummaryUpcomingClass =
                findViewById(R.id.btnSummaryUpcomingClass);

        btnViewFullTimetable =
                findViewById(R.id.btnViewFullTimetable);

        layoutScheduleContainer =
                findViewById(R.id.layoutScheduleContainer);

        btnQuickSubjects =
                findViewById(R.id.btnQuickSubjects);

        btnQuickStudents =
                findViewById(R.id.btnQuickStudents);

        btnQuickAttendance =
                findViewById(R.id.btnQuickAttendance);

        btnQuickAssignments =
                findViewById(R.id.btnQuickAssignments);

        btnQuickResources =
                findViewById(R.id.btnQuickResources);

        btnQuickQuizzes =
                findViewById(R.id.btnQuickQuizzes);

        btnQuickGradebook =
                findViewById(R.id.btnQuickGradebook);

        btnQuickTimetable =
                findViewById(R.id.btnQuickTimetable);

        btnQuickCalendar =
                findViewById(R.id.btnQuickCalendar);

        btnQuickLeave =
                findViewById(R.id.btnQuickLeave);

        btnQuickNotifications =
                findViewById(R.id.btnQuickNotifications);

        btnQuickAiAssistant =
                findViewById(R.id.btnQuickAiAssistant);

        cardUpcomingEvents = findViewById(R.id.cardUpcomingEvents);
        cardRecentActivity = findViewById(R.id.cardRecentActivity);
        cardPendingWork = findViewById(R.id.cardPendingWork);
        tvUpcomingEventsText = findViewById(R.id.tvUpcomingEventsText);
        tvRecentActivityText = findViewById(R.id.tvRecentActivityText);
        tvPendingWorkText = findViewById(R.id.tvPendingWorkText);
    }

    private void setupNavigationDrawer() {

        if (navView != null) {

            navView.setNavigationItemSelectedListener(this);

            setupHeaderView();
        }

        if (btnMenu != null) {

            btnMenu.setOnClickListener(v -> {

                if (drawerLayout != null) {

                    drawerLayout.openDrawer(
                            GravityCompat.START
                    );
                }
            });
        }
    }

    private void setupHeaderView() {

        if (navView == null) {
            return;
        }

        View headerView =
                navView.getHeaderView(0);

        if (headerView == null) {
            return;
        }

        TextView tvNavTeacherName =
                headerView.findViewById(
                        R.id.tvNavTeacherName
                );

        TextView tvNavTeacherDesignation =
                headerView.findViewById(
                        R.id.tvNavTeacherDesignation
                );

        TextView tvNavTeacherDepartment =
                headerView.findViewById(
                        R.id.tvNavTeacherDepartment
                );

        ImageView ivNavCollegeLogo =
                headerView.findViewById(
                        R.id.ivNavCollegeLogo
                );

        ivNavTeacherAvatar =
                headerView.findViewById(
                        R.id.ivNavTeacherAvatar
                );

        if (ivNavTeacherAvatar != null) {
            ivNavTeacherAvatar.setOnClickListener(v -> com.campusscheduler.ai.utils.ProfilePhotoHelper.showPhotoOptionsDialog(this, currentUser, photoPickerLauncher, () -> bindUserData()));
        }

        if (currentUser == null) {
            return;
        }

        if (ivNavCollegeLogo != null) {
            CollegeIdentityHelper.bindCollegeLogo(currentUser.getCollegeId(), currentUser.getCollegeName(), ivNavCollegeLogo);
        }

        if (tvNavTeacherName != null) {

            String name =
                    safe(
                            currentUser.getName(),
                            "Teacher"
                    );

            tvNavTeacherName.setText(name);
        }

        if (tvNavTeacherDesignation != null) {

            String designation =
                    safe(
                            currentUser.getDesignation(),
                            "Faculty"
                    );

            tvNavTeacherDesignation.setText(
                    designation
            );
        }

        if (tvNavTeacherDepartment != null) {

            String department =
                    safe(
                            currentUser.getDepartmentId(),
                            ""
                    );

            if (TextUtils.isEmpty(department)) {

                tvNavTeacherDepartment.setText(
                        "Department"
                );

            } else {

                tvNavTeacherDepartment.setText(
                        "Department of " + department
                );
            }
        }
    }

    private void bindUserData() {

        String todayStr =
                new SimpleDateFormat(
                        "EEE, dd MMM yyyy",
                        Locale.getDefault()
                ).format(new Date());

        if (tvTodayDate != null) {
            tvTodayDate.setText(todayStr);
        }

        if (currentUser == null) {
            return;
        }

        if (tvTeacherTitle != null) {

            tvTeacherTitle.setText(
                    safe(
                            currentUser.getName(),
                            "Teacher"
                    )
            );
        }

        if (tvTeacherSubtitle != null) {
            String designation = safe(currentUser.getDesignation(), "Faculty");
            String department = safe(currentUser.getDepartmentName() != null ? currentUser.getDepartmentName() : currentUser.getDepartmentId(), "Department");
            String collegeName = currentUser.getCollegeName() != null ?
                    CollegeIdentityHelper.formatCollegeName(currentUser.getCollegeName()) : "HKBK College of Engineering";
            tvTeacherSubtitle.setText(designation + " | " + collegeName);
        }

        setupHeaderView();

        if (ivCollegeLogo != null && currentUser != null) {
            CollegeIdentityHelper.bindCollegeLogo(currentUser.getCollegeId(), currentUser.getCollegeName(), ivCollegeLogo);
        }

        if (currentUser != null) {
            com.campusscheduler.ai.utils.ProfilePhotoHelper.bindProfileAvatar(this, currentUser.getPhotoUrl(), ivHeaderAvatar, ivNavTeacherAvatar);
        }
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
                        for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshots) {
                            com.campusscheduler.ai.models.NotificationItem item =
                                    doc.toObject(com.campusscheduler.ai.models.NotificationItem.class);
                            if (item != null) {
                                String rRole = item.getRecipientRole();
                                String cId = item.getCollegeId();
                                boolean isForTeacher = rRole == null || rRole.equalsIgnoreCase("All")
                                        || rRole.equalsIgnoreCase("Teacher") || rRole.equalsIgnoreCase("Faculty");
                                boolean matchesCollege = collegeId == null || cId == null || cId.isEmpty() || cId.equalsIgnoreCase(collegeId);

                                if (isForTeacher && matchesCollege) {
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

    private void openActivity(Class<?> target) {
        Intent intent = new Intent(this, target);
        String collegeId = (currentUser != null && currentUser.getCollegeId() != null && !currentUser.getCollegeId().trim().isEmpty())
                ? currentUser.getCollegeId() : LocalAuthManager.getSafeCollegeId(this, getIntent());
        intent.putExtra("collegeId", collegeId);
        if (currentUser != null) {
            intent.putExtra("teacherUid", currentUser.getUid());
            intent.putExtra("teacherName", currentUser.getName());
            intent.putExtra("departmentId", currentUser.getDepartmentId());
        }
        startActivity(intent);
    }

    private void setupClickListeners() {
        if (btnNotification != null) btnNotification.setOnClickListener(v -> openActivity(StudentNotificationActivity.class));
        if (btnClassDetectionBanner != null) btnClassDetectionBanner.setOnClickListener(v -> openActivity(TeacherClassDetectionActivity.class));
        if (btnSummaryTodayClasses != null) btnSummaryTodayClasses.setOnClickListener(v -> openActivity(TeacherTimetableActivity.class));
        if (btnSummaryThisWeek != null) btnSummaryThisWeek.setOnClickListener(v -> openActivity(TeacherTimetableActivity.class));
        if (btnSummaryMyStudents != null) btnSummaryMyStudents.setOnClickListener(v -> openActivity(StudentManagementActivity.class));
        if (btnSummaryAttendance != null) btnSummaryAttendance.setOnClickListener(v -> openActivity(TeacherAttendanceActivity.class));
        if (btnSummaryPendingAssignments != null) btnSummaryPendingAssignments.setOnClickListener(v -> openActivity(TeacherAssignmentActivity.class));
        if (btnSummaryUpcomingClass != null) btnSummaryUpcomingClass.setOnClickListener(v -> openActivity(TeacherTimetableActivity.class));
        if (btnViewFullTimetable != null) btnViewFullTimetable.setOnClickListener(v -> openActivity(TeacherTimetableActivity.class));
        if (btnQuickSubjects != null) btnQuickSubjects.setOnClickListener(v -> openActivity(SubjectActivity.class));
        if (btnQuickStudents != null) btnQuickStudents.setOnClickListener(v -> openActivity(StudentManagementActivity.class));
        if (btnQuickAttendance != null) btnQuickAttendance.setOnClickListener(v -> openActivity(TeacherAttendanceActivity.class));
        if (btnQuickAssignments != null) btnQuickAssignments.setOnClickListener(v -> openActivity(TeacherAssignmentActivity.class));
        if (btnQuickResources != null) btnQuickResources.setOnClickListener(v -> openActivity(TeacherResourceActivity.class));
        if (btnQuickQuizzes != null) btnQuickQuizzes.setOnClickListener(v -> openActivity(QuizActivity.class));
        if (btnQuickGradebook != null) btnQuickGradebook.setOnClickListener(v -> openActivity(TeacherGradebookActivity.class));
        if (btnQuickTimetable != null) btnQuickTimetable.setOnClickListener(v -> openActivity(TeacherTimetableActivity.class));
        if (btnQuickCalendar != null) btnQuickCalendar.setOnClickListener(v -> openActivity(TeacherCalendarActivity.class));
        if (btnQuickLeave != null) btnQuickLeave.setOnClickListener(v -> openActivity(TeacherLeaveActivity.class));
        if (btnQuickNotifications != null) btnQuickNotifications.setOnClickListener(v -> openActivity(StudentNotificationActivity.class));
        if (btnQuickAiAssistant != null) btnQuickAiAssistant.setOnClickListener(v -> openActivity(TeacherChatActivity.class));
        if (cardUpcomingEvents != null) cardUpcomingEvents.setOnClickListener(v -> openActivity(TeacherCalendarActivity.class));
        if (cardRecentActivity != null) cardRecentActivity.setOnClickListener(v -> openActivity(StudentNotificationActivity.class));
        if (cardPendingWork != null) cardPendingWork.setOnClickListener(v -> openActivity(TeacherAssignmentActivity.class));
    }

    /*
     * Loads the latest user information from Firestore.
     */
    private void loadTeacherProfile() {

        if (currentUser == null
                || TextUtils.isEmpty(currentUser.getUid())) {

            return;
        }

        FirebaseHelper.getUsersRef()
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(document -> {

                    if (document.exists()) {

                        User updatedUser =
                                document.toObject(User.class);

                        if (updatedUser != null) {

                            currentUser = updatedUser;

                            LocalAuthManager
                                    .saveUserSession(
                                            this,
                                            currentUser
                                    );

                            bindUserData();
                        }
                    }

                    loadFacultyProfile();
                })
                .addOnFailureListener(
                        e -> loadFacultyProfile()
                );
    }

    public static boolean isSameTeacher(String name1, String name2) {
        if (name1 == null || name2 == null) return false;
        String n1 = name1.trim().toLowerCase(Locale.US).replaceAll("[^a-z0-9]", "");
        String n2 = name2.trim().toLowerCase(Locale.US).replaceAll("[^a-z0-9]", "");
        if (n1.isEmpty() || n2.isEmpty()) return false;
        if (n1.equals(n2)) return true;
        String b1 = n1.replaceFirst("^(dr|prof|professor|mr|mrs|ms)", "");
        String b2 = n2.replaceFirst("^(dr|prof|professor|mr|mrs|ms)", "");
        if (!b1.isEmpty() && !b2.isEmpty()) {
            if (b1.equals(b2) || b1.contains(b2) || b2.contains(b1)) return true;
        }
        return false;
    }

    /*
     * If the faculty collection contains richer teacher data,
     * use it to update the dashboard identity.
     */
    private void loadFacultyProfile() {

        if (currentUser == null) {
            return;
        }

        String uid = currentUser.getUid();
        String safeCollegeId = (currentUser.getCollegeId() != null && !currentUser.getCollegeId().trim().isEmpty())
                ? currentUser.getCollegeId() : LocalAuthManager.getSafeCollegeId(this, getIntent());

        FirebaseHelper.getFacultyRef()
                .whereEqualTo("collegeId", safeCollegeId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot != null && !snapshot.isEmpty()) {
                        DocumentSnapshot matchedDoc = null;
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            if (!TextUtils.isEmpty(uid) && (uid.equals(doc.getId()) || uid.equals(doc.getString("uid")) || uid.equals(doc.getString("id")))) {
                                matchedDoc = doc;
                                break;
                            }
                            if (isSameTeacher(currentUser.getName(), doc.getString("name"))) {
                                matchedDoc = doc;
                                break;
                            }
                            if (currentUser.getEmail() != null && currentUser.getEmail().equalsIgnoreCase(doc.getString("email"))) {
                                matchedDoc = doc;
                                break;
                            }
                        }
                        if (matchedDoc != null) {
                            currentUser.setUid(matchedDoc.getId());
                            String name = matchedDoc.getString("name");
                            String designation = matchedDoc.getString("designation");
                            String departmentId = matchedDoc.getString("departmentId");

                            if (!TextUtils.isEmpty(name)) {
                                currentUser.setName(name);
                            }
                            if (!TextUtils.isEmpty(designation)) {
                                currentUser.setDesignation(designation);
                            }
                            if (!TextUtils.isEmpty(departmentId)) {
                                currentUser.setDepartmentId(departmentId);
                            }

                            LocalAuthManager.saveUserSession(this, currentUser);
                            bindUserData();
                            loadTeacherSessions();
                        }
                    }
                });
    }

    /*
     * Main dashboard loader.
     */
    private void loadDashboardData() {

        if (currentUser == null || TextUtils.isEmpty(currentUser.getCollegeId())) {
            showDashboardUnavailable();
            return;
        }

        if (TextUtils.isEmpty(currentUser.getUid())) {
            showLoadingState();
            FirebaseHelper.getFacultyRef().whereEqualTo("collegeId", currentUser.getCollegeId()).get()
                    .addOnSuccessListener(snap -> {
                        if (snap != null && !snap.isEmpty()) {
                            DocumentSnapshot targetDoc = snap.getDocuments().get(0);
                            for (DocumentSnapshot doc : snap.getDocuments()) {
                                String n = doc.getString("name");
                                if (n != null && currentUser.getName() != null && n.equalsIgnoreCase(currentUser.getName())) {
                                    targetDoc = doc;
                                    break;
                                }
                            }
                            currentUser.setUid(targetDoc.getId());
                            if (targetDoc.getString("name") != null) currentUser.setName(targetDoc.getString("name"));
                            if (targetDoc.getString("designation") != null) currentUser.setDesignation(targetDoc.getString("designation"));
                            if (targetDoc.getString("departmentId") != null) currentUser.setDepartmentId(targetDoc.getString("departmentId"));
                            LocalAuthManager.saveUserSession(this, currentUser);
                            bindUserData();
                        }
                        loadTeacherSessions();
                        loadAttendanceSummary();
                        loadPendingAssignments();
                        loadUpcomingEvents();
                        loadRecentActivityAndAnnouncements();
                        loadPendingAcademicWork();
                    })
                    .addOnFailureListener(e -> {
                        showDashboardUnavailable();
                    });
            return;
        }

        showLoadingState();

        loadTeacherSessions();
        loadAttendanceSummary();
        loadPendingAssignments();
        loadUpcomingEvents();
        loadRecentActivityAndAnnouncements();
        loadPendingAcademicWork();
    }

    /*
     * Load class sessions belonging to this teacher.
     */
    private void loadTeacherSessions() {

        FirebaseHelper.getClassSessionsRef()
                .get()
                .addOnSuccessListener(snapshot -> {

                    allTeacherSessions.clear();
                    todaySessions.clear();
                    teacherSessionDocuments.clear();

                    if (currentUser == null
                            || TextUtils.isEmpty(currentUser.getUid())) {

                        updateDashboardCounts();
                        renderScheduleList();

                        if (tvNextClassSummary != null) {
                            tvNextClassSummary.setText(
                                    "Teacher profile unavailable."
                            );
                        }

                        return;
                    }

                    String teacherUid = currentUser.getUid();
                    String teacherName = currentUser.getName();

                    String today = dateFormat.format(new Date());
                    String effectiveDay = UIUtils.getEffectiveAcademicDay(Calendar.getInstance());

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {

                        String storedTeacherUid =
                                firstNonEmpty(
                                        doc.getString("teacherUid"),
                                        doc.getString("facultyId"),
                                        doc.getString("teacherId"),
                                        doc.getString("facultyUid")
                                );

                        String storedTeacherName =
                                firstNonEmpty(
                                        doc.getString("teacherName"),
                                        doc.getString("facultyName")
                                );

                        ClassSession session = doc.toObject(ClassSession.class);

                        boolean match = false;
                        if (!TextUtils.isEmpty(teacherUid) && !TextUtils.isEmpty(storedTeacherUid) && (teacherUid.equals(storedTeacherUid) || (session != null && teacherUid.equals(session.getFacultyId())))) {
                            match = true;
                        }
                        if (!match && isSameTeacher(teacherName, storedTeacherName)) {
                            match = true;
                        }
                        if (!match && session != null) {
                            if (isSameTeacher(teacherName, session.getFacultyName()) || isSameTeacher(teacherName, session.getTeacherName())) {
                                match = true;
                            }
                        }
                        if (!match) {
                            continue;
                        }

                        if (session == null) {
                            continue;
                        }

                        allTeacherSessions.add(session);
                        teacherSessionDocuments.add(doc);

                        String sessionDate = firstNonEmpty(doc.getString("date"), session.getDate(), "");
                        String dayOfWeek = firstNonEmpty(doc.getString("dayOfWeek"), session.getDayOfWeek(), "");

                        if ((!TextUtils.isEmpty(sessionDate) && today.equals(sessionDate))
                                || (!TextUtils.isEmpty(dayOfWeek) && effectiveDay.equalsIgnoreCase(dayOfWeek))) {
                            todaySessions.add(session);
                        }
                    }

                    /*
                     * Sort both lists by start time.
                     */
                    sortSessions(allTeacherSessions);
                    sortSessions(todaySessions);

                    /*
                     * Update dashboard UI.
                     */
                    updateDashboardCounts();
                    renderScheduleList();
                    updateNextClassSummary();

                    /*
                     * Load actual students belonging to the
                     * teacher's teaching groups.
                     */
                    loadStudentCount();

                })
                .addOnFailureListener(e -> {

                    allTeacherSessions.clear();
                    todaySessions.clear();
                    teacherSessionDocuments.clear();

                    updateDashboardCounts();
                    renderScheduleList();

                    if (tvNextClassSummary != null) {
                        tvNextClassSummary.setText(
                                "Unable to load class sessions."
                        );
                    }

                    if (tvSummaryMyStudentsCount != null) {
                        tvSummaryMyStudentsCount.setText("—");
                    }
                });
    }

    /*
     * Calculate all dashboard session statistics.
     */
    private void updateDashboardCounts() {

        if (tvSummaryTodayClassesCount != null) {

            tvSummaryTodayClassesCount.setText(
                    String.valueOf(
                            todaySessions.size()
                    )
            );
        }

        if (tvSummaryThisWeekCount != null) {

            int weeklyCount =
                    countSessionsThisWeek(
                            teacherSessionDocuments
                    );

            tvSummaryThisWeekCount.setText(
                    String.valueOf(
                            weeklyCount
                    )
            );
        }

        updateUpcomingClassCard();
    }

    /*
     * Count real classSessions inside current week.
     */
    private int countSessionsThisWeek(
            List<DocumentSnapshot> documents) {

        Calendar calendar =
                Calendar.getInstance();

        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        Calendar weekStart = (Calendar) calendar.clone();
        weekStart.set(Calendar.DAY_OF_WEEK, weekStart.getFirstDayOfWeek());

        Calendar weekEnd = (Calendar) weekStart.clone();
        weekEnd.add(Calendar.DAY_OF_YEAR, 7);

        String startDate = dateFormat.format(weekStart.getTime());
        String endDate = dateFormat.format(weekEnd.getTime());

        int count = 0;

        for (DocumentSnapshot doc : documents) {
            String date = doc.getString("date");
            String dayOfWeek = doc.getString("dayOfWeek");

            if (!TextUtils.isEmpty(dayOfWeek)) {
                count++;
            } else if (!TextUtils.isEmpty(date)) {
                if (date.compareTo(startDate) >= 0 && date.compareTo(endDate) < 0) {
                    count++;
                }
            }
        }

        return count;
    }

    private String nextClassDayLabel = "";

    /*
     * Find the next real class from Firebase.
     */
    private DocumentSnapshot findNextUpcomingDocument() {

        Date now = new Date();
        DocumentSnapshot bestDocument = null;
        Date bestDate = null;
        String today = dateFormat.format(now);
        String effectiveDay = UIUtils.getEffectiveAcademicDay(Calendar.getInstance());
        nextClassDayLabel = "Today";

        // 1. Check remaining classes scheduled for today
        for (DocumentSnapshot doc : teacherSessionDocuments) {

            String date = doc.getString("date");
            String dayOfWeek = doc.getString("dayOfWeek");
            String startTime = doc.getString("startTime");

            if (TextUtils.isEmpty(startTime)) continue;

            if (TextUtils.isEmpty(date) && !TextUtils.isEmpty(dayOfWeek) && effectiveDay.equalsIgnoreCase(dayOfWeek)) {
                date = today;
            }

            if (TextUtils.isEmpty(date) || !today.equals(date)) continue;

            Date sessionStart = parseDateTime(date, startTime);
            if (sessionStart == null) continue;

            if (!sessionStart.after(now)) continue;

            if (bestDate == null || sessionStart.before(bestDate)) {
                bestDate = sessionStart;
                bestDocument = doc;
            }
        }

        // 2. If no remaining classes today, check tomorrow and consecutive upcoming weekdays
        if (bestDocument == null) {
            for (int dayOffset = 1; dayOffset <= 7; dayOffset++) {
                Calendar nextCal = Calendar.getInstance();
                nextCal.add(Calendar.DAY_OF_YEAR, dayOffset);
                String nextDateStr = dateFormat.format(nextCal.getTime());
                String nextDayName = new SimpleDateFormat("EEEE", Locale.US).format(nextCal.getTime());

                for (DocumentSnapshot doc : teacherSessionDocuments) {
                    String date = doc.getString("date");
                    String dayOfWeek = doc.getString("dayOfWeek");
                    String startTime = doc.getString("startTime");

                    if (TextUtils.isEmpty(startTime)) continue;

                    boolean dayMatch = (!TextUtils.isEmpty(date) && nextDateStr.equals(date))
                            || (!TextUtils.isEmpty(dayOfWeek) && nextDayName.equalsIgnoreCase(dayOfWeek));

                    if (dayMatch) {
                        Date sessionStart = parseDateTime(nextDateStr, startTime);
                        if (sessionStart != null) {
                            if (bestDate == null || sessionStart.before(bestDate)) {
                                bestDate = sessionStart;
                                bestDocument = doc;
                                nextClassDayLabel = (dayOffset == 1) ? "Tomorrow" : nextDayName;
                            }
                        }
                    }
                }

                if (bestDocument != null) {
                    break;
                }
            }
        }

        return bestDocument;
    }

    private void updateUpcomingClassCard() {

        DocumentSnapshot next =
                findNextUpcomingDocument();

        if (next == null) {

            if (tvSummaryUpcomingClassInfo != null) {

                tvSummaryUpcomingClassInfo.setText(
                        "No upcoming class"
                );
            }

            if (tvSummaryUpcomingClassTime != null) {

                tvSummaryUpcomingClassTime.setText(
                        ""
                );
            }

            return;
        }

        String subject =
                firstNonEmpty(
                        next.getString("subjectName"),
                        next.getString("subject"),
                        "Subject"
                );

        String startTime =
                next.getString("startTime");

        String endTime =
                next.getString("endTime");

        String room =
                firstNonEmpty(
                        next.getString("roomNumber"),
                        next.getString("classroom"),
                        next.getString("room"),
                        ""
                );

        String dayPrefix = TextUtils.isEmpty(nextClassDayLabel) ? "Upcoming" : nextClassDayLabel;

        if (tvSummaryUpcomingClassInfo != null) {

            tvSummaryUpcomingClassInfo.setText(
                    dayPrefix + " • " + subject
            );
        }

        if (tvSummaryUpcomingClassTime != null) {

            String time =
                    formatTime(startTime)
                            + " - "
                            + formatTime(endTime);

            if (!TextUtils.isEmpty(room)) {

                time += " • " + room;
            }

            tvSummaryUpcomingClassTime.setText(
                    time
            );
        }
    }

    /*
     * Calculate teacher's students from the teacher's actual
     * semester + section combinations.
     */
    private void loadStudentCount() {

        final Set<String> teachingGroups =
                new HashSet<>();

        for (DocumentSnapshot doc :
                teacherSessionDocuments) {

            String semester =
                    normalizeSemester(
                            doc.get("semester")
                    );

            String section =
                    firstNonEmpty(
                            doc.getString("section"),
                            doc.getString("sectionName"),
                            doc.getString("sectionId")
                    );

            if (!TextUtils.isEmpty(semester)
                    && !TextUtils.isEmpty(section)) {

                String cleanSec = section.replaceAll("(?i)^sec(tion)?_?", "").replaceAll(".*_", "").trim().toUpperCase(Locale.US);
                if (cleanSec.isEmpty()) cleanSec = "A";
                teachingGroups.add(
                        semester
                                + "|"
                                + cleanSec
                );
            }
        }

        // Query classAssignments as well to capture all assigned sections
        FirebaseHelper.getClassAssignmentsRef()
                .whereEqualTo("collegeId", currentUser.getCollegeId())
                .get()
                .addOnSuccessListener(assignSnaps -> {
                    if (assignSnaps != null) {
                        for (DocumentSnapshot aDoc : assignSnaps) {
                            String tUid = aDoc.getString("teacherUid");
                            String fId = aDoc.getString("facultyId");
                            String tName = aDoc.getString("teacherName");

                            boolean isMyAssignment = (tUid != null && tUid.equals(currentUser.getUid()))
                                    || (fId != null && fId.equals(currentUser.getUid()))
                                    || isSameTeacher(currentUser.getName(), tName)
                                    || isSameTeacher(currentUser.getName(), aDoc.getString("facultyName"));

                            if (isMyAssignment) {
                                String sem = normalizeSemester(aDoc.get("semester"));
                                String sec = firstNonEmpty(aDoc.getString("section"), aDoc.getString("sectionName"), aDoc.getString("sectionId"));
                                if (!TextUtils.isEmpty(sem) && !TextUtils.isEmpty(sec)) {
                                    String cleanSec = sec.replaceAll("(?i)^sec(tion)?_?", "").replaceAll(".*_", "").trim().toUpperCase(Locale.US);
                                    if (cleanSec.isEmpty()) cleanSec = "A";
                                    teachingGroups.add(sem + "|" + cleanSec);
                                }
                            }
                        }
                    }

                    if (teachingGroups.isEmpty()) {

                        if (tvSummaryMyStudentsCount != null) {

                            tvSummaryMyStudentsCount.setText(
                                    "0"
                            );
                        }

                        return;
                    }

                    FirebaseHelper.getStudentsRef()
                            .whereEqualTo(
                                    "collegeId",
                                    currentUser.getCollegeId()
                            )
                            .get()
                            .addOnSuccessListener(snapshot -> {

                                Set<String> uniqueStudents =
                                        new HashSet<>();

                                if (snapshot != null) {
                                    for (DocumentSnapshot doc :
                                            snapshot.getDocuments()) {

                                        String semester =
                                                normalizeSemester(
                                                        doc.get("semester")
                                                );

                                        String section =
                                                firstNonEmpty(
                                                        doc.getString(
                                                                "section"
                                                        ),
                                                        doc.getString(
                                                                "sectionName"
                                                        ),
                                                        doc.getString(
                                                                "sectionId"
                                                        )
                                                );

                                        if (TextUtils.isEmpty(
                                                semester
                                        )
                                                || TextUtils.isEmpty(
                                                section
                                        )) {

                                            continue;
                                        }

                                        String cleanSec = section.replaceAll("(?i)^sec(tion)?_?", "").replaceAll(".*_", "").trim().toUpperCase(Locale.US);
                                        if (cleanSec.isEmpty()) cleanSec = "A";

                                        String group =
                                                semester
                                                        + "|"
                                                        + cleanSec;

                                        if (teachingGroups.contains(
                                                group
                                        )) {

                                            uniqueStudents.add(
                                                    doc.getId()
                                            );
                                        }
                                    }
                                }

                                if (tvSummaryMyStudentsCount != null) {

                                    tvSummaryMyStudentsCount.setText(
                                            String.valueOf(
                                                    uniqueStudents.size()
                                            )
                                    );
                                }
                            })
                            .addOnFailureListener(e -> {

                                if (tvSummaryMyStudentsCount != null) {

                                    tvSummaryMyStudentsCount.setText(
                                            "—"
                                    );
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    if (tvSummaryMyStudentsCount != null) {
                        tvSummaryMyStudentsCount.setText("0");
                    }
                });
    }

    /*
     * Real attendance calculation.
     */
    private void loadAttendanceSummary() {

        FirebaseHelper.getAttendanceRef()
                .whereEqualTo(
                        "collegeId",
                        currentUser.getCollegeId()
                )
                .get()
                .addOnSuccessListener(snapshot -> {

                    int total =
                            0;

                    int present =
                            0;

                    String teacherUid =
                            currentUser.getUid();

                    for (DocumentSnapshot doc :
                            snapshot.getDocuments()) {

                        String recordTeacher =
                                firstNonEmpty(
                                        doc.getString(
                                                "teacherUid"
                                        ),
                                        doc.getString(
                                                "facultyId"
                                        )
                                );

                        /*
                         * Only count attendance belonging
                         * to this teacher.
                         */
                        if (!TextUtils.isEmpty(
                                recordTeacher
                        )
                                && !teacherUid.equals(
                                recordTeacher
                        )) {

                            continue;
                        }

                        total++;

                        Boolean presentValue =
                                doc.getBoolean(
                                        "present"
                                );

                        String status =
                                doc.getString(
                                        "status"
                                );

                        if (Boolean.TRUE.equals(
                                presentValue
                        )
                                || "PRESENT".equalsIgnoreCase(
                                status
                        )) {

                            present++;
                        }
                    }

                    if (tvSummaryAttendancePercent == null) {
                        return;
                    }

                    if (total == 0) {

                        tvSummaryAttendancePercent.setText(
                                "—"
                        );

                    } else {

                        double percentage =
                                (present * 100.0)
                                        / total;

                        tvSummaryAttendancePercent.setText(
                                String.format(
                                        Locale.getDefault(),
                                        "%.0f%%",
                                        percentage
                                )
                        );
                    }
                })
                .addOnFailureListener(e -> {

                    if (tvSummaryAttendancePercent != null) {

                        tvSummaryAttendancePercent.setText(
                                "—"
                        );
                    }
                });
    }

    /*
     * Real teacher assignment count.
     */
    private void loadPendingAssignments() {

        FirebaseHelper.getAssignmentsRef()
                .whereEqualTo(
                        "collegeId",
                        currentUser.getCollegeId()
                )
                .get()
                .addOnSuccessListener(snapshot -> {

                    int pending =
                            0;

                    String teacherUid =
                            currentUser.getUid();

                    for (DocumentSnapshot doc :
                            snapshot.getDocuments()) {

                        String recordTeacher =
                                firstNonEmpty(
                                        doc.getString(
                                                "teacherUid"
                                        ),
                                        doc.getString(
                                                "createdById"
                                        ),
                                        doc.getString(
                                                "createdBy"
                                        )
                                );

                        if (!teacherUid.equals(
                                recordTeacher
                        )) {

                            continue;
                        }

                        String status =
                                doc.getString(
                                        "status"
                                );

                        if ("DRAFT".equalsIgnoreCase(
                                status
                        )
                                || "CLOSED".equalsIgnoreCase(
                                status
                        )) {

                            continue;
                        }

                        pending++;
                    }

                    if (tvSummaryPendingAssignmentsCount != null) {

                        tvSummaryPendingAssignmentsCount.setText(
                                String.valueOf(
                                        pending
                                )
                        );
                    }
                })
                .addOnFailureListener(e -> {

                    if (tvSummaryPendingAssignmentsCount != null) {

                        tvSummaryPendingAssignmentsCount.setText(
                                "—"
                        );
                    }
                });
    }

    /*
     * Today's schedule.
     *
     * IMPORTANT:
     * No fake/demo classes are created.
     */
    private void renderScheduleList() {

        if (layoutScheduleContainer == null) {
            return;
        }

        layoutScheduleContainer.removeAllViews();

        if (todaySessions.isEmpty()) {

            if (tvNoScheduleText != null) {

                tvNoScheduleText.setVisibility(
                        View.VISIBLE
                );

                tvNoScheduleText.setText(
                        "No classes scheduled for today."
                );
            }

            return;
        }

        if (tvNoScheduleText != null) {

            tvNoScheduleText.setVisibility(
                    View.GONE
            );
        }

        for (ClassSession cs : todaySessions) {

            String title =
                    firstNonEmpty(
                            cs.getSubjectName(),
                            "Subject"
                    );

            String code =
                    firstNonEmpty(
                            cs.getSubjectCode(),
                            cs.getSubjectId(),
                            ""
                    );

            String start =
                    cs.getStartTime();

            String end =
                    cs.getEndTime();

            String time =
                    formatTime(start)
                            + " - "
                            + formatTime(end);

            String type =
                    firstNonEmpty(
                            cs.getClassType(),
                            cs.isLab() ? "Lab" : "Class"
                    );

            String room =
                    firstNonEmpty(
                            cs.getRoomNumber(),
                            "Location not assigned"
                    );

            String semester =
                    cs.getSemester() > 0 ? (cs.getSemester() + " Sem") : "";

            String section =
                    firstNonEmpty(
                            cs.getSectionName(),
                            cs.getSection(),
                            ""
                    );

            String location =
                    room;

            if (!TextUtils.isEmpty(
                    semester
            )) {

                location +=
                        " • " + semester;
            }

            if (!TextUtils.isEmpty(
                    section
            )) {

                location +=
                        " " + section;
            }

            String status =
                    calculateSessionStatus(
                            cs
                    );

            addScheduleCard(
                    title,
                    code,
                    time,
                    type,
                    location,
                    status
            );
        }
    }

    private String calculateSessionStatus(
            ClassSession cs) {

        String today = dateFormat.format(new Date());

        Date start =
                parseDateTime(
                        today,
                        cs.getStartTime()
                );

        Date end =
                parseDateTime(
                        today,
                        cs.getEndTime()
                );

        Date now =
                new Date();

        if (end != null
                && now.after(end)) {

            return "Completed";
        }

        if (start != null
                && end != null
                && !now.before(start)
                && now.before(end)) {

            return "Take Attendance";
        }

        if (!TextUtils.isEmpty(
                cs.getStatus()
        )
                && !"scheduled".equalsIgnoreCase(
                cs.getStatus()
        )) {

            return cs.getStatus();
        }

        return "Upcoming";
    }

    private void loadUpcomingEvents() {
        if (currentUser == null || TextUtils.isEmpty(currentUser.getCollegeId())) return;
        FirebaseHelper.getExamsRef().whereEqualTo("collegeId", currentUser.getCollegeId()).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot != null && !snapshot.isEmpty()) {
                        DocumentSnapshot doc = snapshot.getDocuments().get(0);
                        String name = firstNonEmpty(doc.getString("subjectName"), doc.getString("examName"), "Semester Exam");
                        String date = firstNonEmpty(doc.getString("examDate"), doc.getString("date"), "Upcoming");
                        if (tvUpcomingEventsText != null) {
                            tvUpcomingEventsText.setText("Exam: " + name + " (" + date + ")");
                        }
                    } else if (tvUpcomingEventsText != null) {
                        tvUpcomingEventsText.setText("No upcoming department events scheduled.");
                    }
                })
                .addOnFailureListener(e -> {
                    if (tvUpcomingEventsText != null) tvUpcomingEventsText.setText("No upcoming department events scheduled.");
                });
    }

    private void loadRecentActivityAndAnnouncements() {
        if (currentUser == null || TextUtils.isEmpty(currentUser.getCollegeId())) return;
        FirebaseHelper.getNotificationsRef().whereEqualTo("collegeId", currentUser.getCollegeId()).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot != null && !snapshot.isEmpty()) {
                        DocumentSnapshot doc = snapshot.getDocuments().get(snapshot.size() - 1);
                        String title = firstNonEmpty(doc.getString("title"), "Campus Notice");
                        String msg = firstNonEmpty(doc.getString("message"), doc.getString("body"), "");
                        if (tvRecentActivityText != null) {
                            tvRecentActivityText.setText(title + (msg.isEmpty() ? "" : ": " + msg));
                        }
                    } else if (tvRecentActivityText != null) {
                        tvRecentActivityText.setText("No recent activity or announcements.");
                    }
                })
                .addOnFailureListener(e -> {
                    if (tvRecentActivityText != null) tvRecentActivityText.setText("No recent activity or announcements.");
                });
    }

    private void loadPendingAcademicWork() {
        if (currentUser == null || TextUtils.isEmpty(currentUser.getCollegeId())) return;
        FirebaseHelper.getFirestore().collection("assignmentSubmissions")
                .whereEqualTo("collegeId", currentUser.getCollegeId())
                .whereEqualTo("status", "SUBMITTED")
                .get()
                .addOnSuccessListener(snapshot -> {
                    int unreviewed = snapshot != null ? snapshot.size() : 0;
                    if (tvPendingWorkText != null) {
                        if (unreviewed > 0) {
                            tvPendingWorkText.setText(unreviewed + " student assignment submission" + (unreviewed > 1 ? "s" : "") + " pending grading.");
                        } else {
                            tvPendingWorkText.setText("All submitted assignments have been evaluated.");
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (tvPendingWorkText != null) tvPendingWorkText.setText("No pending academic work.");
                });
    }

    private void updateNextClassSummary() {

        DocumentSnapshot next =
                findNextUpcomingDocument();

        if (tvNextClassSummary == null) {
            return;
        }

        if (next == null) {

            tvNextClassSummary.setText(
                    "No upcoming class."
            );

            return;
        }

        String subject =
                firstNonEmpty(
                        next.getString(
                                "subjectName"
                        ),
                        next.getString(
                                "subject"
                        ),
                        "Upcoming class"
                );

        String start =
                next.getString(
                        "startTime"
                );

        String end =
                next.getString(
                        "endTime"
                );

        tvNextClassSummary.setText(
                subject
                        + " • "
                        + formatTime(start)
                        + " - "
                        + formatTime(end)
        );
    }

    private void addScheduleCard(
            String title,
            String code,
            String time,
            String type,
            String location,
            String status) {

        CardView card =
                new CardView(this);

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.setMargins(
                0,
                8,
                0,
                16
        );

        card.setLayoutParams(
                cardParams
        );

        card.setRadius(12f);
        card.setCardElevation(2f);
        card.setCardBackgroundColor(
                Color.WHITE
        );

        RelativeLayout layout =
                new RelativeLayout(this);

        layout.setPadding(
                24,
                24,
                24,
                24
        );

        ImageView ivIcon =
                new ImageView(this);

        ivIcon.setId(
                View.generateViewId()
        );

        RelativeLayout.LayoutParams iconParams =
                new RelativeLayout.LayoutParams(
                        70,
                        70
                );

        iconParams.addRule(
                RelativeLayout.ALIGN_PARENT_START
        );

        iconParams.addRule(
                RelativeLayout.CENTER_VERTICAL
        );

        ivIcon.setLayoutParams(
                iconParams
        );

        ivIcon.setImageResource(
                android.R.drawable.ic_menu_today
        );

        ivIcon.setColorFilter(
                Color.parseColor("#1976D2")
        );

        layout.addView(ivIcon);

        LinearLayout infoLayout =
                new LinearLayout(this);

        infoLayout.setOrientation(
                LinearLayout.VERTICAL
        );

        RelativeLayout.LayoutParams infoParams =
                new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT
                );

        infoParams.addRule(
                RelativeLayout.END_OF,
                ivIcon.getId()
        );

        infoParams.setMarginStart(
                20
        );

        infoParams.addRule(
                RelativeLayout.START_OF,
                View.generateViewId()
        );

        infoLayout.setLayoutParams(
                infoParams
        );

        TextView tvTitle =
                new TextView(this);

        tvTitle.setText(
                TextUtils.isEmpty(code)
                        ? title
                        : title + " (" + code + ")"
        );

        tvTitle.setTextSize(14);
        tvTitle.setTypeface(
                null,
                Typeface.BOLD
        );

        tvTitle.setTextColor(
                Color.parseColor("#212121")
        );

        infoLayout.addView(
                tvTitle
        );

        TextView tvTime =
                new TextView(this);

        tvTime.setText(
                time + "  |  " + type
        );

        tvTime.setTextSize(12);

        tvTime.setTextColor(
                Color.parseColor("#757575")
        );

        infoLayout.addView(
                tvTime
        );

        TextView tvLoc =
                new TextView(this);

        tvLoc.setText(
                location
        );

        tvLoc.setTextSize(11);

        tvLoc.setTextColor(
                Color.parseColor("#9E9E9E")
        );

        infoLayout.addView(
                tvLoc
        );

        layout.addView(
                infoLayout
        );

        TextView btnStatus =
                new TextView(this);

        RelativeLayout.LayoutParams statusParams =
                new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT
                );

        statusParams.addRule(
                RelativeLayout.ALIGN_PARENT_END
        );

        statusParams.addRule(
                RelativeLayout.CENTER_VERTICAL
        );

        btnStatus.setLayoutParams(
                statusParams
        );

        btnStatus.setPadding(
                24,
                12,
                24,
                12
        );

        btnStatus.setTextSize(12);

        btnStatus.setTypeface(
                null,
                Typeface.BOLD
        );

        if ("Completed".equalsIgnoreCase(
                status
        )) {

            btnStatus.setText(
                    "✓ Completed"
            );

            btnStatus.setTextColor(
                    Color.parseColor("#2E7D32")
            );

            btnStatus.setBackgroundColor(
                    Color.parseColor("#E8F5E9")
            );

        } else if ("Take Attendance".equalsIgnoreCase(
                status
        )) {

            btnStatus.setText(
                    "Take Attendance"
            );

            btnStatus.setTextColor(
                    Color.WHITE
            );

            btnStatus.setBackgroundColor(
                    Color.parseColor("#1E88E5")
            );

            btnStatus.setOnClickListener(
                    v -> startActivity(
                            new Intent(
                                    this,
                                    TeacherAttendanceActivity.class
                            )
                    )
            );

        } else {

            btnStatus.setText(
                    "Upcoming ›"
            );

            btnStatus.setTextColor(
                    Color.parseColor("#616161")
            );

            btnStatus.setBackgroundColor(
                    Color.parseColor("#F5F5F5")
            );
        }

        layout.addView(
                btnStatus
        );

        card.addView(
                layout
        );

        layoutScheduleContainer.addView(
                card
        );
    }

    private void showLoadingState() {

        setText(
                tvSummaryTodayClassesCount,
                "..."
        );

        setText(
                tvSummaryThisWeekCount,
                "..."
        );

        setText(
                tvSummaryMyStudentsCount,
                "..."
        );

        setText(
                tvSummaryAttendancePercent,
                "..."
        );

        setText(
                tvSummaryPendingAssignmentsCount,
                "..."
        );

        setText(
                tvSummaryUpcomingClassInfo,
                "Loading..."
        );

        setText(
                tvSummaryUpcomingClassTime,
                ""
        );

        if (tvNoScheduleText != null) {

            tvNoScheduleText.setVisibility(
                    View.VISIBLE
            );

            tvNoScheduleText.setText(
                    "Loading today's schedule..."
            );
        }
    }

    private void showDashboardUnavailable() {

        setText(
                tvSummaryTodayClassesCount,
                "—"
        );

        setText(
                tvSummaryThisWeekCount,
                "—"
        );

        setText(
                tvSummaryMyStudentsCount,
                "—"
        );

        setText(
                tvSummaryAttendancePercent,
                "—"
        );

        setText(
                tvSummaryPendingAssignmentsCount,
                "—"
        );

        setText(
                tvSummaryUpcomingClassInfo,
                "Profile unavailable"
        );

        setText(
                tvSummaryUpcomingClassTime,
                ""
        );

        if (tvNoScheduleText != null) {

            tvNoScheduleText.setVisibility(
                    View.VISIBLE
            );

            tvNoScheduleText.setText(
                    "Teacher profile information is unavailable."
            );
        }
    }

    private void sortSessions(
            List<ClassSession> sessions) {

        Collections.sort(
                sessions,
                new Comparator<ClassSession>() {

                    @Override
                    public int compare(
                            ClassSession first,
                            ClassSession second) {

                        String firstTime =
                                safe(
                                        first.getStartTime(),
                                        "99:99"
                                );

                        String secondTime =
                                safe(
                                        second.getStartTime(),
                                        "99:99"
                                );

                        return firstTime.compareTo(
                                secondTime
                        );
                    }
                }
        );
    }

    private Date parseDateTime(
            String date,
            String time) {

        if (TextUtils.isEmpty(date)
                || TextUtils.isEmpty(time)) {

            return null;
        }

        String[] patterns = {
                "yyyy-MM-dd HH:mm",
                "yyyy-MM-dd H:mm",
                "yyyy-MM-dd hh:mm a",
                "yyyy-MM-dd h:mm a"
        };

        for (String pattern :
                patterns) {

            try {

                return new SimpleDateFormat(
                        pattern,
                        Locale.US
                ).parse(
                        date + " " + time
                );

            } catch (ParseException ignored) {
            }
        }

        return null;
    }

    private String formatTime(
            String time) {

        if (TextUtils.isEmpty(time)) {
            return "Time not set";
        }

        String[] patterns = {
                "HH:mm",
                "H:mm",
                "hh:mm a",
                "h:mm a"
        };

        for (String pattern :
                patterns) {

            try {

                Date parsed =
                        new SimpleDateFormat(
                                pattern,
                                Locale.US
                        ).parse(time);

                return new SimpleDateFormat(
                        "hh:mm a",
                        Locale.US
                ).format(parsed);

            } catch (ParseException ignored) {
            }
        }

        return time;
    }

    private String normalizeSemester(
            Object value) {

        if (value == null) {
            return "";
        }

        String text =
                String.valueOf(value).trim();

        if (text.isEmpty()) {
            return "";
        }

        String digits =
                text.replaceAll(
                        "[^0-9]",
                        ""
                );

        return digits.isEmpty()
                ? text
                : digits;
    }

    private String firstNonEmpty(
            String... values) {

        if (values == null) {
            return "";
        }

        for (String value :
                values) {

            if (!TextUtils.isEmpty(
                    value
            )) {

                return value;
            }
        }

        return "";
    }

    private String safe(
            String value,
            String fallback) {

        return TextUtils.isEmpty(
                value
        )
                ? fallback
                : value;
    }

    private void setText(
            TextView view,
            String value) {

        if (view != null) {
            view.setText(value);
        }
    }

    @Override
    public boolean onNavigationItemSelected(
            @NonNull MenuItem item) {

        if (drawerLayout != null) {

            drawerLayout.closeDrawer(
                    GravityCompat.START
            );
        }

        int id =
                item.getItemId();

        if (id == R.id.nav_teacher_dashboard) {

            // Already on dashboard.

        } else if (id == R.id.nav_teacher_profile) {
            openActivity(SettingsActivity.class);
        } else if (id == R.id.nav_teacher_subjects) {
            openActivity(SubjectActivity.class);
        } else if (id == R.id.nav_teacher_students) {
            openActivity(StudentManagementActivity.class);
        } else if (id == R.id.nav_teacher_timetable
                || id == R.id.nav_teacher_schedule) {
            openActivity(TeacherTimetableActivity.class);
        } else if (id == R.id.nav_teacher_availability) {
            openActivity(FacultyAvailabilityActivity.class);
        } else if (id == R.id.nav_teacher_workload) {
            openActivity(FacultyWorkloadActivity.class);
        } else if (id == R.id.nav_teacher_attendance) {
            openActivity(TeacherAttendanceActivity.class);
        } else if (id == R.id.nav_teacher_assignments) {
            openActivity(TeacherAssignmentActivity.class);
        } else if (id == R.id.nav_teacher_resources) {
            openActivity(TeacherResourceActivity.class);
        } else if (id == R.id.nav_teacher_quizzes) {
            openActivity(QuizActivity.class);
        } else if (id == R.id.nav_teacher_gradebook) {
            openActivity(TeacherGradebookActivity.class);
        } else if (id == R.id.nav_teacher_calendar
                || id == R.id.nav_teacher_events) {
            openActivity(TeacherCalendarActivity.class);
        } else if (id == R.id.nav_teacher_change_requests) {
            openActivity(ProfileChangeRequestActivity.class);
        } else if (id == R.id.nav_teacher_leave) {
            openActivity(TeacherLeaveActivity.class);
        } else if (id == R.id.nav_teacher_substitute) {
            openActivity(TeacherSubstituteActivity.class);
        } else if (id == R.id.nav_teacher_notifications
                || id == R.id.nav_teacher_messages
                || id == R.id.nav_teacher_dept_announcements
                || id == R.id.nav_teacher_college_announcements) {
            openActivity(StudentNotificationActivity.class);
        } else if (id == R.id.nav_teacher_chat) {
            openActivity(TeacherChatActivity.class);
        } else if (id == R.id.nav_teacher_department) {
            openActivity(DepartmentActivity.class);
        } else if (id == R.id.nav_teacher_college) {
            openActivity(CollegeManagementActivity.class);
        } else if (id == R.id.nav_teacher_help) {
            openActivity(HelpCenterActivity.class);
        } else if (id == R.id.nav_settings) {
            openActivity(SettingsActivity.class);

        } else if (id == R.id.nav_logout) {

            performLogout();
        }

        return true;
    }

    private void performLogout() {

        FirebaseHelper.getAuth().signOut();

        LocalAuthManager.logoutLocalUser(
                this
        );

        UIUtils.showToast(
                this,
                "Logged out successfully."
        );

        Intent intent =
                new Intent(
                        this,
                        LoginActivity.class
                );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);

        finish();
    }
}
