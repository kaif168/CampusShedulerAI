package com.campusscheduler.ai.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.campusscheduler.ai.R;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.utils.CollegeIdentityHelper;
import com.campusscheduler.ai.utils.FirebaseHelper;
import com.campusscheduler.ai.utils.LocalAuthManager;
import com.campusscheduler.ai.utils.UIUtils;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;

public class AdminDashboardActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navView;

    private ImageButton btnMenu;
    private ImageButton btnNotifications;
    private ImageButton btnSettings;
    private ImageButton btnLogout;
    private ImageView ivCollegeLogo;

    private TextView tvAdminTitle;
    private TextView tvAdminSubtitle;
    private TextView tvInstitutionName;
    private TextView tvInstitutionLocation;
    private TextView tvAdminIdentity;
    private TextView tvAdminStatusSummary;

    // 8 Live metric counter views
    private TextView tvCountDepartments;
    private TextView tvCountFaculty;
    private TextView tvCountStudents;
    private TextView tvCountSubjects;
    private TextView tvCountClassrooms;
    private TextView tvCountLaboratories;
    private TextView tvCountAssignments;
    private TextView tvCountTimetables;

    // Primary action
    private CardView btnGenerateTimetable;

    // Academic management tiles
    private CardView btnDepartments;
    private CardView btnFaculty;
    private CardView btnStudents;
    private CardView btnSubjects;
    private CardView btnSemesters;
    private CardView btnClassAssignments;
    private CardView btnClassrooms;
    private CardView btnExcelImport;

    // Academic operations tiles
    private CardView btnWorkload;
    private CardView btnAttendance;
    private CardView btnSubstitution;
    private CardView btnExamScheduling;
    private CardView btnReports;
    private CardView btnBroadcasts;

    private User currentUser;
    private String resolvedCollegeId = null;

    // Live counts
    private int departmentCount = 0;
    private int facultyCount = 0;
    private int studentCount = 0;
    private int subjectCount = 0;
    private int classroomCount = 0;
    private int laboratoryCount = 0;
    private int assignmentCount = 0;
    private int timetableCount = 0;

    private int completedQueries = 0;
    private boolean queryFailed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        currentUser = LocalAuthManager.getCurrentLocalUser(this);

        initializeViews();
        setupNavigationDrawer();
        setupAdminInformation();
        setupDashboardButtons();
        setupHeaderActionButtons();

        loadLiveOverview();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentUser == null) {
            currentUser = LocalAuthManager.getCurrentLocalUser(this);
        }
        setupAdminInformation();
        loadLiveOverview();
    }

    private void initializeViews() {
        drawerLayout = findViewById(R.id.drawerLayout);
        navView = findViewById(R.id.navView);

        btnMenu = findViewById(R.id.btnMenu);
        btnNotifications = findViewById(R.id.btnNotifications);
        btnSettings = findViewById(R.id.btnSettings);
        btnLogout = findViewById(R.id.btnLogout);
        ivCollegeLogo = findViewById(R.id.ivCollegeLogo);

        tvAdminTitle = findViewById(R.id.tvAdminTitle);
        tvAdminSubtitle = findViewById(R.id.tvAdminSubtitle);
        tvInstitutionName = findViewById(R.id.tvInstitutionName);
        tvInstitutionLocation = findViewById(R.id.tvInstitutionLocation);
        tvAdminIdentity = findViewById(R.id.tvAdminIdentity);
        tvAdminStatusSummary = findViewById(R.id.tvAdminStatusSummary);

        // Metric counter views
        tvCountDepartments = findViewById(R.id.tvCountDepartments);
        tvCountFaculty = findViewById(R.id.tvCountFaculty);
        tvCountStudents = findViewById(R.id.tvCountStudents);
        tvCountSubjects = findViewById(R.id.tvCountSubjects);
        tvCountClassrooms = findViewById(R.id.tvCountClassrooms);
        tvCountLaboratories = findViewById(R.id.tvCountLaboratories);
        tvCountAssignments = findViewById(R.id.tvCountAssignments);
        tvCountTimetables = findViewById(R.id.tvCountTimetables);

        // Hero card
        btnGenerateTimetable = findViewById(R.id.btnGenerateTimetable);

        // Management tiles
        btnDepartments = findViewById(R.id.btnDepartments);
        btnFaculty = findViewById(R.id.btnFaculty);
        btnStudents = findViewById(R.id.btnStudents);
        btnSubjects = findViewById(R.id.btnSubjects);
        btnSemesters = findViewById(R.id.btnSemesters);
        btnClassAssignments = findViewById(R.id.btnClassAssignments);
        btnClassrooms = findViewById(R.id.btnClassrooms);
        btnExcelImport = findViewById(R.id.btnExcelImport);

        // Operation tiles
        btnWorkload = findViewById(R.id.btnWorkload);
        btnAttendance = findViewById(R.id.btnAttendance);
        btnSubstitution = findViewById(R.id.btnSubstitution);
        btnExamScheduling = findViewById(R.id.btnExamScheduling);
        btnReports = findViewById(R.id.btnReports);
        btnBroadcasts = findViewById(R.id.btnBroadcasts);
    }

    private void setupNavigationDrawer() {
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
    }

    private void setupAdminInformation() {
        if (currentUser == null) {
            currentUser = LocalAuthManager.getCurrentLocalUser(this);
        }

        String targetCollegeId = "COLLEGE_1789406176044";
        if (currentUser != null && currentUser.getCollegeId() != null && !currentUser.getCollegeId().trim().isEmpty()) {
            targetCollegeId = currentUser.getCollegeId().trim();
        }
        resolvedCollegeId = targetCollegeId;
        final String finalTargetCollegeId = targetCollegeId;

        String adminName = (currentUser != null && currentUser.getName() != null && !currentUser.getName().trim().isEmpty())
                ? currentUser.getName().trim() : "Administrator";

        if (tvAdminIdentity != null) {
            tvAdminIdentity.setText(adminName + " • College Controller");
        }

        // Apply preliminary values from local session if available
        if (currentUser != null && currentUser.getCollegeName() != null && !currentUser.getCollegeName().trim().isEmpty()) {
            String initialName = CollegeIdentityHelper.formatCollegeName(currentUser.getCollegeName());
            if (tvAdminTitle != null) tvAdminTitle.setText(initialName);
            if (tvInstitutionName != null) tvInstitutionName.setText(initialName);
            if (tvInstitutionLocation != null && currentUser.getCollegeAddress() != null) {
                tvInstitutionLocation.setText(currentUser.getCollegeAddress());
            }
            if (ivCollegeLogo != null) {
                CollegeIdentityHelper.bindCollegeLogo(finalTargetCollegeId, initialName, ivCollegeLogo);
            }
        }

        // Asynchronously resolve authoritative college record from Firestore
        CollegeIdentityHelper.resolveCollege(this, finalTargetCollegeId, college -> {
            if (college == null) return;
            String name = CollegeIdentityHelper.formatCollegeName(college.getName());
            String address = CollegeIdentityHelper.formatVerifiedAddress(college.getAddress(), name);
            String code = college.getCode() != null ? college.getCode().toUpperCase() : finalTargetCollegeId;

            if (tvAdminTitle != null) {
                tvAdminTitle.setText(name);
            }
            if (tvAdminSubtitle != null) {
                tvAdminSubtitle.setText("Admin Console • " + code);
            }
            if (ivCollegeLogo != null) {
                CollegeIdentityHelper.bindCollegeLogo(college.getId(), name, ivCollegeLogo);
            }
            if (tvInstitutionName != null) {
                tvInstitutionName.setText(name);
            }
            if (tvInstitutionLocation != null) {
                tvInstitutionLocation.setText(address);
            }

            if (currentUser != null) {
                currentUser.setCollegeId(college.getId());
                currentUser.setCollegeName(name);
                currentUser.setCollegeAddress(address);
                currentUser.setCollegeCode(code);
                LocalAuthManager.saveUserSession(this, currentUser);
            }

            setupHeaderView();
        });
    }

    private void setupHeaderView() {
        if (navView == null) return;
        View headerView = navView.getHeaderView(0);
        if (headerView == null || currentUser == null) return;

        TextView tvNavAdminName = headerView.findViewById(R.id.tvNavAdminName);
        TextView tvNavAdminCollege = headerView.findViewById(R.id.tvNavAdminCollege);
        TextView tvNavAdminRole = headerView.findViewById(R.id.tvNavAdminRole);

        if (tvNavAdminName != null) {
            String name = currentUser.getName();
            tvNavAdminName.setText(name != null && !name.trim().isEmpty() ? name : "College Administrator");
        }
        if (tvNavAdminCollege != null) {
            String collegeName = currentUser.getCollegeName() != null ? currentUser.getCollegeName() : "HKBK College of Engineering";
            String code = currentUser.getCollegeCode() != null ? " (" + currentUser.getCollegeCode() + ")" : "";
            tvNavAdminCollege.setText(CollegeIdentityHelper.formatCollegeName(collegeName) + code);
        }
        if (tvNavAdminRole != null) {
            tvNavAdminRole.setText("Academic Controller");
        }
    }

    private void setupHeaderActionButtons() {
        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v -> startActivity(new Intent(this, NotificationManagementActivity.class)));
        }
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        }
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> performLogout());
        }
    }

    private void setupDashboardButtons() {
        // Hero Card
        if (btnGenerateTimetable != null) {
            btnGenerateTimetable.setOnClickListener(v -> openActivity(TimetableGeneratorActivity.class));
        }

        // Overview Metric Cards
        View cardMetricDepts = findViewById(R.id.cardMetricDepartments);
        View cardMetricFaculty = findViewById(R.id.cardMetricFaculty);
        View cardMetricStudents = findViewById(R.id.cardMetricStudents);
        View cardMetricSubjects = findViewById(R.id.cardMetricSubjects);
        View cardMetricClassrooms = findViewById(R.id.cardMetricClassrooms);
        View cardMetricLabs = findViewById(R.id.cardMetricLaboratories);
        View cardMetricAssignments = findViewById(R.id.cardMetricAssignments);
        View cardMetricTimetables = findViewById(R.id.cardMetricTimetables);

        if (cardMetricDepts != null) cardMetricDepts.setOnClickListener(v -> openActivity(DepartmentActivity.class));
        if (cardMetricFaculty != null) cardMetricFaculty.setOnClickListener(v -> openActivity(FacultyActivity.class));
        if (cardMetricStudents != null) cardMetricStudents.setOnClickListener(v -> openActivity(StudentManagementActivity.class));
        if (cardMetricSubjects != null) cardMetricSubjects.setOnClickListener(v -> openActivity(SubjectActivity.class));
        if (cardMetricClassrooms != null) cardMetricClassrooms.setOnClickListener(v -> {
            Intent intent = new Intent(this, ClassroomActivity.class);
            String cid = resolvedCollegeId != null ? resolvedCollegeId : (currentUser != null && currentUser.getCollegeId() != null ? currentUser.getCollegeId() : "COLLEGE_1789406176044");
            intent.putExtra("collegeId", cid);
            intent.putExtra("filter", 1);
            startActivity(intent);
        });
        if (cardMetricLabs != null) cardMetricLabs.setOnClickListener(v -> {
            Intent intent = new Intent(this, ClassroomActivity.class);
            String cid = resolvedCollegeId != null ? resolvedCollegeId : (currentUser != null && currentUser.getCollegeId() != null ? currentUser.getCollegeId() : "COLLEGE_1789406176044");
            intent.putExtra("collegeId", cid);
            intent.putExtra("filter", 2);
            startActivity(intent);
        });
        if (cardMetricAssignments != null) cardMetricAssignments.setOnClickListener(v -> openActivity(ClassAssignmentActivity.class));
        if (cardMetricTimetables != null) cardMetricTimetables.setOnClickListener(v -> openActivity(TimetableManagementActivity.class));

        // Academic Management Cards
        if (btnDepartments != null) {
            btnDepartments.setOnClickListener(v -> openActivity(DepartmentActivity.class));
        }
        if (btnFaculty != null) {
            btnFaculty.setOnClickListener(v -> openActivity(FacultyActivity.class));
        }
        if (btnStudents != null) {
            btnStudents.setOnClickListener(v -> openActivity(StudentManagementActivity.class));
        }
        if (btnSubjects != null) {
            btnSubjects.setOnClickListener(v -> openActivity(SubjectActivity.class));
        }
        if (btnSemesters != null) {
            btnSemesters.setOnClickListener(v -> openActivity(SectionActivity.class));
        }
        if (btnClassAssignments != null) {
            btnClassAssignments.setOnClickListener(v -> openActivity(ClassAssignmentActivity.class));
        }
        if (btnClassrooms != null) {
            btnClassrooms.setOnClickListener(v -> openActivity(ClassroomActivity.class));
        }
        if (btnExcelImport != null) {
            btnExcelImport.setOnClickListener(v -> showBulkImportChooserDialog());
        }

        // Academic Operations Cards
        if (btnWorkload != null) {
            btnWorkload.setOnClickListener(v -> openActivity(FacultyActivity.class));
        }
        if (btnAttendance != null) {
            btnAttendance.setOnClickListener(v -> openActivity(AdminAttendanceRegisterActivity.class));
        }
        if (btnSubstitution != null) {
            btnSubstitution.setOnClickListener(v -> openActivity(TeacherLeaveActivity.class));
        }
        if (btnExamScheduling != null) {
            btnExamScheduling.setOnClickListener(v -> openActivity(IATimetableActivity.class));
        }
        if (btnReports != null) {
            btnReports.setOnClickListener(v -> openActivity(ReportsActivity.class));
        }
        if (btnBroadcasts != null) {
            btnBroadcasts.setOnClickListener(v -> openActivity(NotificationManagementActivity.class));
        }
    }

    /**
     * REAL FIRESTORE LIVE OVERVIEW:
     * Queries 8 real collections for this college:
     * 1. departments
     * 2. faculty
     * 3. students
     * 4. subjects
     * 5. classrooms
     * 6. laboratories
     * 7. classAssignments
     * 8. timetables
     */
    private void loadLiveOverview() {
        if (tvAdminStatusSummary != null) {
            tvAdminStatusSummary.setText("Querying live institutional metrics from Cloud Firestore...");
        }

        resetOverviewState();

        String collegeId = resolvedCollegeId;
        if (collegeId == null && currentUser != null) {
            collegeId = currentUser.getCollegeId();
        }

        if (collegeId == null || collegeId.trim().isEmpty()) {
            // Attempt to resolve collegeId from Firestore colleges
            FirebaseHelper.getCollegesRef().limit(1).get().addOnSuccessListener(snap -> {
                if (snap != null && !snap.isEmpty()) {
                    DocumentSnapshot firstCol = snap.getDocuments().get(0);
                    resolvedCollegeId = firstCol.getId();
                    fetchMetricsForCollege(resolvedCollegeId);
                } else {
                    resolvedCollegeId = "COLLEGE_1789406176044";
                    fetchMetricsForCollege(resolvedCollegeId);
                }
            }).addOnFailureListener(e -> {
                resolvedCollegeId = "COLLEGE_1789406176044";
                fetchMetricsForCollege(resolvedCollegeId);
            });
            return;
        }

        fetchMetricsForCollege(collegeId);
    }

    private void fetchMetricsForCollege(String collegeId) {
        // 1. Departments
        loadCount(FirebaseHelper.getDepartmentsRef(), collegeId, count -> {
            departmentCount = count;
            if (tvCountDepartments != null) tvCountDepartments.setText(String.valueOf(count));
            onQueryFinished();
        });

        // 2. Faculty
        loadCount(FirebaseHelper.getFacultyRef(), collegeId, count -> {
            facultyCount = count;
            if (tvCountFaculty != null) tvCountFaculty.setText(String.valueOf(count));
            onQueryFinished();
        });

        // 3. Students
        loadCount(FirebaseHelper.getStudentsRef(), collegeId, count -> {
            studentCount = count;
            if (tvCountStudents != null) tvCountStudents.setText(String.valueOf(count));
            onQueryFinished();
        });

        // 4. Subjects
        loadCount(FirebaseHelper.getSubjectsRef(), collegeId, count -> {
            subjectCount = count;
            if (tvCountSubjects != null) tvCountSubjects.setText(String.valueOf(count));
            onQueryFinished();
        });

        // 5. Classrooms
        loadCount(FirebaseHelper.getClassroomsRef(), collegeId, count -> {
            classroomCount = count;
            if (tvCountClassrooms != null) tvCountClassrooms.setText(String.valueOf(count));
            onQueryFinished();
        });

        // 6. Laboratories
        loadCount(FirebaseHelper.getLaboratoriesRef(), collegeId, count -> {
            laboratoryCount = count;
            if (tvCountLaboratories != null) tvCountLaboratories.setText(String.valueOf(count));
            onQueryFinished();
        });

        // 7. Class Assignments
        loadCount(FirebaseHelper.getClassAssignmentsRef(), collegeId, count -> {
            assignmentCount = count;
            if (tvCountAssignments != null) tvCountAssignments.setText(String.valueOf(count));
            onQueryFinished();
        });

        // 8. Timetables
        loadCount(FirebaseHelper.getTimetablesRef(), collegeId, count -> {
            timetableCount = count;
            if (tvCountTimetables != null) tvCountTimetables.setText(String.valueOf(count));
            onQueryFinished();
        });
    }

    private void loadCount(Query query, String collegeId, CountCallback callback) {
        query.whereEqualTo("collegeId", collegeId).get()
                .addOnSuccessListener(snap -> {
                    int c = snap != null ? snap.size() : 0;
                    callback.onResult(c);
                })
                .addOnFailureListener(error -> {
                    // Try fallback without collegeId filter if tenant scoping is broad
                    query.get().addOnSuccessListener(broadSnap -> {
                        int c = broadSnap != null ? broadSnap.size() : 0;
                        callback.onResult(c);
                    }).addOnFailureListener(err2 -> {
                        queryFailed = true;
                        callback.onResult(0);
                    });
                });
    }

    private void onQueryFinished() {
        completedQueries++;
        if (completedQueries < 8) return;

        if (tvAdminStatusSummary != null) {
            if (queryFailed && (departmentCount + facultyCount + studentCount + classroomCount) == 0) {
                tvAdminStatusSummary.setText("Live overview could not be loaded. Please check Firestore connection.");
            } else {
                tvAdminStatusSummary.setText("Live System Status: Active • Total Venues: " + (classroomCount + laboratoryCount)
                        + " • Faculty: " + facultyCount + " • Mappings: " + assignmentCount);
            }
        }
    }

    private void resetOverviewState() {
        departmentCount = 0;
        facultyCount = 0;
        studentCount = 0;
        subjectCount = 0;
        classroomCount = 0;
        laboratoryCount = 0;
        assignmentCount = 0;
        timetableCount = 0;

        completedQueries = 0;
        queryFailed = false;

        if (tvCountDepartments != null) tvCountDepartments.setText("--");
        if (tvCountFaculty != null) tvCountFaculty.setText("--");
        if (tvCountStudents != null) tvCountStudents.setText("--");
        if (tvCountSubjects != null) tvCountSubjects.setText("--");
        if (tvCountClassrooms != null) tvCountClassrooms.setText("--");
        if (tvCountLaboratories != null) tvCountLaboratories.setText("--");
        if (tvCountAssignments != null) tvCountAssignments.setText("--");
        if (tvCountTimetables != null) tvCountTimetables.setText("--");
    }

    private interface CountCallback {
        void onResult(int count);
    }

    private void performLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Logout")
                .setMessage("Are you sure you want to securely log out of the College Admin Console?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    LocalAuthManager.clearSession(AdminDashboardActivity.this);
                    Intent intent = new Intent(AdminDashboardActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (drawerLayout != null) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }

        if (itemId == R.id.nav_admin_dashboard) {
            // Already here
        } else if (itemId == R.id.nav_ai_timetable_gen) {
            openActivity(TimetableGeneratorActivity.class);
        } else if (itemId == R.id.nav_faculty_mgmt) {
            openActivity(FacultyActivity.class);
        } else if (itemId == R.id.nav_student_mgmt) {
            openActivity(StudentManagementActivity.class);
        } else if (itemId == R.id.nav_subject_mgmt) {
            openActivity(SubjectActivity.class);
        } else if (itemId == R.id.nav_class_assignments) {
            openActivity(ClassAssignmentActivity.class);
        } else if (itemId == R.id.nav_room_lab_mgmt) {
            openActivity(ClassroomActivity.class);
        } else if (itemId == R.id.nav_department_mgmt) {
            openActivity(DepartmentActivity.class);
        } else if (itemId == R.id.nav_semester_mgmt) {
            openActivity(SemesterActivity.class);
        } else if (itemId == R.id.nav_section_mgmt) {
            openActivity(SectionActivity.class);
        } else if (itemId == R.id.nav_excel_import) {
            showBulkImportChooserDialog();
        } else if (itemId == R.id.nav_attendance_audit) {
            openActivity(AdminAttendanceRegisterActivity.class);
        } else if (itemId == R.id.nav_teacher_substitution) {
            openActivity(TeacherLeaveActivity.class);
        } else if (itemId == R.id.nav_profile_change_requests) {
            openActivity(ProfileChangeRequestActivity.class);
        } else if (itemId == R.id.nav_reports_analytics) {
            openActivity(ReportsActivity.class);
        } else if (itemId == R.id.nav_exam_ia_mgmt) {
            openActivity(IATimetableActivity.class);
        } else if (itemId == R.id.nav_announcements) {
            openActivity(NotificationManagementActivity.class);
        } else if (itemId == R.id.nav_support) {
            openActivity(HelpCenterActivity.class);
        } else if (itemId == R.id.nav_settings) {
            openActivity(SettingsActivity.class);
        } else if (itemId == R.id.nav_logout) {
            performLogout();
        }

        return true;
    }

    private void showBulkImportChooserDialog() {
        String[] options = {
                "Faculty Master Import (Excel / CSV)",
                "Student Bulk Import (Excel / CSV)",
                "Classrooms & Laboratories Import (CSV)"
        };
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Academic Data Bulk Import")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        openActivity(FacultyImportActivity.class);
                    } else if (which == 1) {
                        openActivity(StudentImportActivity.class);
                    } else if (which == 2) {
                        openActivity(ClassroomImportActivity.class);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openActivity(Class<?> target) {
        Intent intent = new Intent(this, target);
        String collegeId = resolvedCollegeId != null ? resolvedCollegeId :
                (currentUser != null && currentUser.getCollegeId() != null ? currentUser.getCollegeId() : "COLLEGE_1789406176044");
        String collegeName = currentUser != null && currentUser.getCollegeName() != null ?
                currentUser.getCollegeName() : "HKBK College of Engineering";
        String collegeAddress = currentUser != null && currentUser.getCollegeAddress() != null ?
                currentUser.getCollegeAddress() : "Nagawara, Bengaluru, Karnataka 560045";
        intent.putExtra("collegeId", collegeId);
        intent.putExtra("collegeName", collegeName);
        intent.putExtra("collegeAddress", collegeAddress);
        intent.putExtra("isAdmin", true);
        intent.putExtra("departmentId", currentUser != null && currentUser.getDepartmentId() != null ? currentUser.getDepartmentId() : "DEPT_CSE");
        startActivity(intent);
    }
}