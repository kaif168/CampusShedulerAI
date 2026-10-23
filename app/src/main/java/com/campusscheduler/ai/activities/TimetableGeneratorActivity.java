package com.campusscheduler.ai.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.campusscheduler.ai.R;
import com.campusscheduler.ai.engine.Chromosome;
import com.campusscheduler.ai.engine.ExplainableAI;
import com.campusscheduler.ai.engine.Gene;
import com.campusscheduler.ai.engine.GeneticAlgorithm;
import com.campusscheduler.ai.models.ClassAssignment;
import com.campusscheduler.ai.models.ClassSession;
import com.campusscheduler.ai.models.Classroom;
import com.campusscheduler.ai.models.Department;
import com.campusscheduler.ai.models.Laboratory;
import com.campusscheduler.ai.models.NotificationItem;
import com.campusscheduler.ai.models.Section;
import com.campusscheduler.ai.models.Semester;
import com.campusscheduler.ai.models.Subject;
import com.campusscheduler.ai.models.Timetable;
import com.campusscheduler.ai.models.TimetableSlot;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.repository.AcademicRepository;
import com.campusscheduler.ai.utils.CollegeIdentityHelper;
import com.campusscheduler.ai.utils.FirebaseHelper;
import com.campusscheduler.ai.utils.LocalAuthManager;
import com.campusscheduler.ai.utils.UIUtils;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TimetableGeneratorActivity extends AppCompatActivity {

    // ---------------------------------------------------------
    // EXISTING UI
    // ---------------------------------------------------------

    private ImageButton btnBack;
    private Button btnRunGAEngine;
    private Button btnPublishTimetable;

    private TextView tvFitnessScore;
    private TextView tvConflicts;
    private TextView tvXAIExplanation;

    // ---------------------------------------------------------
    // NEW CONFIGURATION UI
    // ---------------------------------------------------------

    private Spinner spinnerDepartment;
    private Spinner spinnerSemester;
    private Spinner spinnerSection;
    private Spinner spinnerAcademicYear;

    private CheckBox checkMonday;
    private CheckBox checkTuesday;
    private CheckBox checkWednesday;
    private CheckBox checkThursday;
    private CheckBox checkFriday;
    private CheckBox checkSaturday;

    // ---------------------------------------------------------
    // SUBJECT SELECTION & PREVIEW UI
    // ---------------------------------------------------------

    private TextView tvSubjectSelectionSubtitle;
    private TextView btnToggleSelectAll;
    private TextView tvSubjectSelectionStatus;
    private LinearLayout llSubjectListContainer;
    private Button btnOpenClassMapping;
    private LinearLayout llTimetableSlotsPreview;
    private TextView tvPreviewDepartment;
    private TextView tvPreviewAcademicYear;
    private TextView tvPreviewSemSec;
    private TextView tvPreviewTimetableBadge;

    private Spinner spinnerClassTeacher;
    private TextView tvWefDate;
    private android.widget.EditText etCoordinatorName;
    private android.widget.EditText etHodName;
    private android.widget.EditText etPrincipalName;
    private Button btnExportPdf;
    private String selectedWefDate = "2026-08-01";

    // Independent subject selection per cohort: key = deptId + "_S" + semNum + "_" + secName
    private final Map<String, Set<String>> scopedSelectedSubjectIds = new HashMap<>();

    // ---------------------------------------------------------
    // USER
    // ---------------------------------------------------------

    private User currentUser;

    // ---------------------------------------------------------
    // GENERATED RESULT
    // ---------------------------------------------------------

    private Chromosome lastGeneratedResult;

    // ---------------------------------------------------------
    // MASTER DATA
    // ---------------------------------------------------------

    private final List<User> facultyList = new ArrayList<>();
    private final List<Classroom> classroomList = new ArrayList<>();
    private final List<Laboratory> labList = new ArrayList<>();
    private final List<Subject> subjectList = new ArrayList<>();

    // All Firebase assignments
    private final List<ClassAssignment> allAssignmentList = new ArrayList<>();

    // Currently selected/filterable assignments
    private final List<ClassAssignment> assignmentList = new ArrayList<>();

    // Department data
    private final List<Department> departmentList = new ArrayList<>();

    // Semester data for selected department
    private final List<Semester> allSemesterList = new ArrayList<>();
    private final List<Semester> filteredSemesterList = new ArrayList<>();

    // Section data for selected department + semester
    private final List<Section> sectionList = new ArrayList<>();

    // Academic years extracted from semesters
    private final List<String> academicYearList = new ArrayList<>();

    // ---------------------------------------------------------
    // SELECTED CONFIGURATION
    // ---------------------------------------------------------

    private Department selectedDepartment;
    private Semester selectedSemester;
    private Section selectedSection;
    private String selectedAcademicYear;

    // ---------------------------------------------------------
    // LIFECYCLE
    // ---------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_timetable_generator);

        initializeViews();
        initializeDefaultUI();

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        if (btnRunGAEngine != null) btnRunGAEngine.setOnClickListener(v -> runGeneticAlgorithm());
        if (btnPublishTimetable != null) btnPublishTimetable.setOnClickListener(v -> publishTimetable());

        setupDepartmentSpinnerListener();
        setupAcademicYearSpinnerListener();
        setupSemesterSpinnerListener();
        setupSectionSpinnerListener();

        currentUser = LocalAuthManager.getCurrentLocalUser(this);
        String safeCollegeId = LocalAuthManager.getSafeCollegeId(this, getIntent());
        if (currentUser == null) {
            currentUser = new User();
            currentUser.setRole(User.ROLE_COLLEGE_ADMIN);
            currentUser.setName("Campus Administrator");
        }
        if (currentUser.getCollegeId() == null || currentUser.getCollegeId().trim().isEmpty()) {
            currentUser.setCollegeId(safeCollegeId);
            LocalAuthManager.saveUserSession(this, currentUser);
        }

        loadMasterDataFromFirestore();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentUser == null || currentUser.getCollegeId() == null || currentUser.getCollegeId().trim().isEmpty()) {
            currentUser = LocalAuthManager.getCurrentLocalUser(this);
            String safeCollegeId = LocalAuthManager.getSafeCollegeId(this, getIntent());
            if (currentUser == null) {
                currentUser = new User();
                currentUser.setRole(User.ROLE_COLLEGE_ADMIN);
                currentUser.setName("Campus Administrator");
            }
            if (currentUser.getCollegeId() == null || currentUser.getCollegeId().trim().isEmpty()) {
                currentUser.setCollegeId(safeCollegeId);
                LocalAuthManager.saveUserSession(this, currentUser);
            }
        }
        loadMasterDataFromFirestore();
    }

    // ---------------------------------------------------------
    // INITIALIZE VIEWS
    // ---------------------------------------------------------

    private void initializeViews() {

        // Existing
        btnBack = findViewById(R.id.btnBack);
        btnRunGAEngine = findViewById(R.id.btnRunGAEngine);
        btnPublishTimetable = findViewById(R.id.btnPublishTimetable);

        tvFitnessScore = findViewById(R.id.tvFitnessScore);
        tvConflicts = findViewById(R.id.tvConflicts);
        tvXAIExplanation = findViewById(R.id.tvXAIExplanation);

        // New configuration
        spinnerDepartment = findViewById(R.id.spinnerDepartment);
        spinnerSemester = findViewById(R.id.spinnerSemester);
        spinnerSection = findViewById(R.id.spinnerSection);
        spinnerAcademicYear = findViewById(R.id.spinnerAcademicYear);

        checkMonday = findViewById(R.id.checkMonday);
        checkTuesday = findViewById(R.id.checkTuesday);
        checkWednesday = findViewById(R.id.checkWednesday);
        checkThursday = findViewById(R.id.checkThursday);
        checkFriday = findViewById(R.id.checkFriday);
        checkSaturday = findViewById(R.id.checkSaturday);

        // Subject selection and preview UI
        tvSubjectSelectionSubtitle = findViewById(R.id.tvSubjectSelectionSubtitle);
        btnToggleSelectAll = findViewById(R.id.btnToggleSelectAll);
        tvSubjectSelectionStatus = findViewById(R.id.tvSubjectSelectionStatus);
        llSubjectListContainer = findViewById(R.id.llSubjectListContainer);
        btnOpenClassMapping = findViewById(R.id.btnOpenClassMapping);
        llTimetableSlotsPreview = findViewById(R.id.llTimetableSlotsPreview);
        tvPreviewDepartment = findViewById(R.id.tvPreviewDepartment);
        tvPreviewAcademicYear = findViewById(R.id.tvPreviewAcademicYear);
        tvPreviewSemSec = findViewById(R.id.tvPreviewSemSec);
        tvPreviewTimetableBadge = findViewById(R.id.tvPreviewTimetableBadge);

        spinnerClassTeacher = findViewById(R.id.spinnerClassTeacher);
        tvWefDate = findViewById(R.id.tvWefDate);
        etCoordinatorName = findViewById(R.id.etCoordinatorName);
        etHodName = findViewById(R.id.etHodName);
        etPrincipalName = findViewById(R.id.etPrincipalName);
        btnExportPdf = findViewById(R.id.btnExportPdf);

        if (tvWefDate != null) {
            tvWefDate.setOnClickListener(v -> {
                java.util.Calendar c = java.util.Calendar.getInstance();
                new android.app.DatePickerDialog(this, (view, year, month, day) -> {
                    selectedWefDate = String.format(java.util.Locale.US, "%04d-%02d-%02d", year, month + 1, day);
                    tvWefDate.setText("W.E.F.: " + selectedWefDate + " (Click to change)");
                }, 2026, 7, 1).show();
            });
        }

        if (btnExportPdf != null) {
            btnExportPdf.setOnClickListener(v -> {
                if (lastGeneratedResult == null) {
                    UIUtils.showToast(this, "Please generate timetable first.");
                    return;
                }
                Timetable tt = buildTimetableFromCurrentState();
                com.campusscheduler.ai.utils.TimetablePdfExporter.exportAndSharePdf(this, tt);
            });
        }

        if (btnToggleSelectAll != null) {
            btnToggleSelectAll.setOnClickListener(v -> toggleSelectAll());
        }

        if (btnOpenClassMapping != null) {
            btnOpenClassMapping.setOnClickListener(v -> {
                Intent intent = new Intent(this, ClassAssignmentActivity.class);
                startActivity(intent);
            });
        }
    }

    // ---------------------------------------------------------
    // DEFAULT UI
    // ---------------------------------------------------------

    private void initializeDefaultUI() {

        setSpinnerItems(
                spinnerDepartment,
                createSingleItemList("Loading departments...")
        );

        setSpinnerItems(
                spinnerAcademicYear,
                createSingleItemList("Select academic year")
        );

        setSpinnerItems(
                spinnerSemester,
                createSingleItemList("Select semester")
        );

        setSpinnerItems(
                spinnerSection,
                createSingleItemList("Select section")
        );

        spinnerSemester.setEnabled(false);
        spinnerSection.setEnabled(false);
        spinnerAcademicYear.setEnabled(false);

        btnRunGAEngine.setEnabled(false);

        tvFitnessScore.setText("Fitness Score: --");
        tvConflicts.setText("Hard Conflicts: --");

        tvXAIExplanation.setText(
                "Select Department, Academic Year, Semester and Section, " +
                        "then choose working days and click Generate AI Timetable."
        );
    }

    // ---------------------------------------------------------
    // FIREBASE MASTER DATA
    // ---------------------------------------------------------

    private void loadMasterDataFromFirestore() {

        String collegeId = currentUser.getCollegeId();

        // -----------------------------------------------------
        // FACULTY
        // -----------------------------------------------------

        AcademicRepository.getInstance().getFaculty(collegeId, task -> {

            facultyList.clear();

            if (task.isSuccessful() && task.getResult() != null) {

                for (DocumentSnapshot doc : task.getResult()) {

                    User user = doc.toObject(User.class);

                    if (user != null) {
                        facultyList.add(user);
                    }
                }
            }
            if (facultyList.isEmpty()) {
                FirebaseHelper.getFacultyRef().whereEqualTo("collegeId", collegeId).get().addOnSuccessListener(snap -> {
                    if (snap != null) {
                        for (DocumentSnapshot d : snap) {
                            User u = new User();
                            u.setUid(d.getId());
                            u.setName(d.getString("name"));
                            u.setEmail(d.getString("email"));
                            u.setDepartmentId(d.getString("departmentId"));
                            facultyList.add(u);
                        }
                    }
                    updateClassTeacherSpinner();
                });
            } else {
                updateClassTeacherSpinner();
            }
        });

        // -----------------------------------------------------
        // CLASSROOMS
        // -----------------------------------------------------

        AcademicRepository.getInstance().getClassrooms(collegeId, task -> {

            classroomList.clear();

            if (task.isSuccessful() && task.getResult() != null) {

                for (DocumentSnapshot doc : task.getResult()) {

                    Classroom classroom =
                            doc.toObject(Classroom.class);

                    if (classroom != null) {
                        classroomList.add(classroom);
                    }
                }
            }
        });

        // -----------------------------------------------------
        // LABORATORIES
        // -----------------------------------------------------

        FirebaseHelper.getLaboratoriesRef()
                .whereEqualTo("collegeId", collegeId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    labList.clear();
                    if (queryDocumentSnapshots != null) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            Laboratory lab = doc.toObject(Laboratory.class);
                            if (lab != null) {
                                labList.add(lab);
                            }
                        }
                    }
                });

        // -----------------------------------------------------
        // SUBJECTS
        // -----------------------------------------------------

        AcademicRepository.getInstance().getSubjects(collegeId, task -> {

            subjectList.clear();

            if (task.isSuccessful() && task.getResult() != null) {

                for (DocumentSnapshot doc : task.getResult()) {

                    Subject subject =
                            doc.toObject(Subject.class);

                    if (subject != null) {
                        subjectList.add(subject);
                    }
                }
            }
        });

        // -----------------------------------------------------
        // CLASS ASSIGNMENTS
        // -----------------------------------------------------

        FirebaseHelper.getClassAssignmentsRef()
                .whereEqualTo("collegeId", collegeId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    allAssignmentList.clear();

                    if (queryDocumentSnapshots != null) {

                        for (DocumentSnapshot doc :
                                queryDocumentSnapshots) {

                            ClassAssignment assignment =
                                    doc.toObject(ClassAssignment.class);

                            if (assignment != null) {
                                allAssignmentList.add(assignment);
                            }
                        }
                    }
                    if (selectedSection != null) {
                        populateEligibleSubjectsUI();
                    }
                })
                .addOnFailureListener(e -> {

                    UIUtils.showToast(
                            TimetableGeneratorActivity.this,
                            "Failed to load class assignments."
                    );
                });

        // -----------------------------------------------------
        // DEPARTMENTS
        // -----------------------------------------------------

        FirebaseHelper.getDepartmentsRef()
                .whereEqualTo("collegeId", collegeId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    departmentList.clear();

                    if (queryDocumentSnapshots != null) {

                        for (DocumentSnapshot doc :
                                queryDocumentSnapshots) {

                            Department department =
                                    doc.toObject(Department.class);

                            if (department != null) {

                                if (department.getId() == null ||
                                        department.getId().trim().isEmpty()) {

                                    department.setId(doc.getId());
                                }

                                departmentList.add(department);
                            }
                        }
                    }

                    populateDepartmentSpinner();

                })
                .addOnFailureListener(e -> {

                    UIUtils.showAlert(
                            TimetableGeneratorActivity.this,
                            "Department Loading Failed",
                            "Unable to load departments from Firestore.\n\n"
                                    + e.getMessage()
                    );
                });
    }

    // ---------------------------------------------------------
    // DEPARTMENT SPINNER
    // ---------------------------------------------------------

    private void populateDepartmentSpinner() {

        List<String> names = new ArrayList<>();

        for (Department department : departmentList) {

            String name = department.getName();

            if (name == null || name.trim().isEmpty()) {
                name = department.getCode();
            }

            if (name == null || name.trim().isEmpty()) {
                name = "Department";
            }

            names.add(name);
        }

        if (names.isEmpty()) {

            names.add("No departments available");

            setSpinnerItems(spinnerDepartment, names);

            spinnerDepartment.setEnabled(false);
            spinnerSemester.setEnabled(false);
            spinnerSection.setEnabled(false);
            spinnerAcademicYear.setEnabled(false);

            btnRunGAEngine.setEnabled(false);

            tvXAIExplanation.setText(
                    "No departments were found for this college. " +
                            "Please create a department first."
            );

            return;
        }

        setSpinnerItems(spinnerDepartment, names);

        spinnerDepartment.setEnabled(true);

        // Automatically select first department
        if (!departmentList.isEmpty()) {

            selectedDepartment = departmentList.get(0);

            loadSemestersForDepartment(selectedDepartment);
        }
    }

    private void setupDepartmentSpinnerListener() {

        spinnerDepartment.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            android.widget.AdapterView<?> parent,
                            android.view.View view,
                            int position,
                            long id) {

                        if (departmentList.isEmpty()) {
                            return;
                        }

                        if (position < 0 ||
                                position >= departmentList.size()) {
                            return;
                        }

                        selectedDepartment =
                                departmentList.get(position);

                        loadSemestersForDepartment(
                                selectedDepartment
                        );
                    }

                    @Override
                    public void onNothingSelected(
                            android.widget.AdapterView<?> parent) {
                    }
                }
        );
    }

    // ---------------------------------------------------------
    // LOAD SEMESTERS
    // ---------------------------------------------------------

    private void loadSemestersForDepartment(
            Department department) {

        if (department == null ||
                department.getId() == null) {
            return;
        }

        spinnerSemester.setEnabled(false);
        spinnerSection.setEnabled(false);
        spinnerAcademicYear.setEnabled(false);

        btnRunGAEngine.setEnabled(false);

        setSpinnerItems(
                spinnerSemester,
                createSingleItemList("Loading semesters...")
        );

        setSpinnerItems(
                spinnerSection,
                createSingleItemList("Select section")
        );

        String collegeId = currentUser.getCollegeId();

        FirebaseHelper.getSemestersRef()
                .whereEqualTo("collegeId", collegeId)
                .whereEqualTo("departmentId", department.getId())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    allSemesterList.clear();

                    if (queryDocumentSnapshots != null) {

                        for (DocumentSnapshot doc :
                                queryDocumentSnapshots) {

                            Semester semester =
                                    doc.toObject(Semester.class);

                            if (semester != null) {

                                if (semester.getId() == null ||
                                        semester.getId().trim().isEmpty()) {

                                    semester.setId(doc.getId());
                                }

                                allSemesterList.add(semester);
                            }
                        }
                    }

                    populateAcademicYearSpinner();

                })
                .addOnFailureListener(e -> {

                    UIUtils.showAlert(
                            TimetableGeneratorActivity.this,
                            "Semester Loading Failed",
                            "Unable to load semesters.\n\n"
                                    + e.getMessage()
                    );
                });
    }

    // ---------------------------------------------------------
    // ACADEMIC YEAR
    // ---------------------------------------------------------

    private void populateAcademicYearSpinner() {

        academicYearList.clear();

        Set<String> uniqueYears =
                new LinkedHashSet<>();

        for (Semester semester : allSemesterList) {

            String year = semester.getAcademicYear();

            if (year != null &&
                    !year.trim().isEmpty()) {

                uniqueYears.add(year.trim());
            }
        }

        academicYearList.addAll(uniqueYears);

        if (academicYearList.isEmpty()) {

            setSpinnerItems(
                    spinnerAcademicYear,
                    createSingleItemList(
                            "No academic year available"
                    )
            );

            spinnerAcademicYear.setEnabled(false);

            filteredSemesterList.clear();

            setSpinnerItems(
                    spinnerSemester,
                    createSingleItemList(
                            "No semester available"
                    )
            );

            spinnerSemester.setEnabled(false);

            spinnerSection.setEnabled(false);

            btnRunGAEngine.setEnabled(false);

            tvXAIExplanation.setText(
                    "Academic year information is missing " +
                            "for the selected department."
            );

            return;
        }

        setSpinnerItems(
                spinnerAcademicYear,
                academicYearList
        );

        spinnerAcademicYear.setEnabled(true);

        selectedAcademicYear =
                academicYearList.get(0);

        filterSemestersByAcademicYear(
                selectedAcademicYear
        );
    }

    private void setupAcademicYearSpinnerListener() {

        spinnerAcademicYear.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            android.widget.AdapterView<?> parent,
                            android.view.View view,
                            int position,
                            long id) {

                        if (academicYearList.isEmpty()) {
                            return;
                        }

                        if (position < 0 ||
                                position >= academicYearList.size()) {
                            return;
                        }

                        selectedAcademicYear =
                                academicYearList.get(position);

                        filterSemestersByAcademicYear(
                                selectedAcademicYear
                        );
                    }

                    @Override
                    public void onNothingSelected(
                            android.widget.AdapterView<?> parent) {
                    }
                }
        );
    }

    // ---------------------------------------------------------
    // FILTER SEMESTERS BY ACADEMIC YEAR
    // ---------------------------------------------------------

    private void filterSemestersByAcademicYear(
            String academicYear) {

        filteredSemesterList.clear();

        for (Semester semester : allSemesterList) {

            if (academicYear != null &&
                    academicYear.equals(
                            semester.getAcademicYear())) {

                filteredSemesterList.add(semester);
            }
        }

        List<String> semesterNames =
                new ArrayList<>();

        for (Semester semester :
                filteredSemesterList) {

            String name = semester.getSemesterName();

            if (name == null ||
                    name.trim().isEmpty()) {

                name = "Semester "
                        + semester.getSemesterNumber();
            }

            semesterNames.add(name);
        }

        if (semesterNames.isEmpty()) {

            semesterNames.add(
                    "No semester available"
            );

            setSpinnerItems(
                    spinnerSemester,
                    semesterNames
            );

            spinnerSemester.setEnabled(false);
            spinnerSection.setEnabled(false);
            btnRunGAEngine.setEnabled(false);

            return;
        }

        setSpinnerItems(
                spinnerSemester,
                semesterNames
        );

        spinnerSemester.setEnabled(true);

        selectedSemester =
                filteredSemesterList.get(0);

        loadSectionsForSemester(
                selectedSemester
        );
    }

    // ---------------------------------------------------------
    // SEMESTER SPINNER
    // ---------------------------------------------------------

    private void setupSemesterSpinnerListener() {

        spinnerSemester.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            android.widget.AdapterView<?> parent,
                            android.view.View view,
                            int position,
                            long id) {

                        if (filteredSemesterList.isEmpty()) {
                            return;
                        }

                        if (position < 0 ||
                                position >=
                                        filteredSemesterList.size()) {
                            return;
                        }

                        selectedSemester =
                                filteredSemesterList.get(position);

                        loadSectionsForSemester(
                                selectedSemester
                        );
                    }

                    @Override
                    public void onNothingSelected(
                            android.widget.AdapterView<?> parent) {
                    }
                }
        );
    }

    // ---------------------------------------------------------
    // LOAD SECTIONS
    // ---------------------------------------------------------

    private void loadSectionsForSemester(
            Semester semester) {

        if (selectedDepartment == null ||
                selectedDepartment.getId() == null ||
                semester == null) {

            return;
        }

        spinnerSection.setEnabled(false);
        btnRunGAEngine.setEnabled(false);

        setSpinnerItems(
                spinnerSection,
                createSingleItemList(
                        "Loading sections..."
                )
        );

        String collegeId =
                currentUser.getCollegeId();

        int semesterNumber =
                semester.getSemesterNumber();

        FirebaseHelper.getSectionsRef()
                .whereEqualTo("collegeId", collegeId)
                .whereEqualTo(
                        "departmentId",
                        selectedDepartment.getId()
                )
                .whereEqualTo(
                        "semester",
                        semesterNumber
                )
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    sectionList.clear();

                    if (queryDocumentSnapshots != null) {

                        for (DocumentSnapshot doc :
                                queryDocumentSnapshots) {

                            Section section =
                                    doc.toObject(Section.class);

                            if (section != null) {

                                if (section.getId() == null ||
                                        section.getId().trim().isEmpty()) {

                                    section.setId(doc.getId());
                                }

                                sectionList.add(section);
                            }
                        }
                    }

                    populateSectionSpinner();

                })
                .addOnFailureListener(e -> {

                    UIUtils.showAlert(
                            TimetableGeneratorActivity.this,
                            "Section Loading Failed",
                            "Unable to load sections.\n\n"
                                    + e.getMessage()
                    );
                });
    }

    // ---------------------------------------------------------
    // SECTION SPINNER
    // ---------------------------------------------------------

    private void populateSectionSpinner() {

        List<String> sectionNames =
                new ArrayList<>();

        for (Section section : sectionList) {

            String name =
                    section.getSectionName();

            if (name == null ||
                    name.trim().isEmpty()) {

                name = section.getId();
            }

            sectionNames.add(name);
        }

        if (sectionNames.isEmpty()) {

            sectionNames.add(
                    "No sections available"
            );

            setSpinnerItems(
                    spinnerSection,
                    sectionNames
            );

            spinnerSection.setEnabled(false);
            btnRunGAEngine.setEnabled(false);
            selectedSection = null;
            populateEligibleSubjectsUI();

            tvXAIExplanation.setText(
                    "No sections found for the selected " +
                            "department and semester."
            );

            return;
        }

        setSpinnerItems(
                spinnerSection,
                sectionNames
        );

        spinnerSection.setEnabled(true);

        selectedSection =
                sectionList.get(0);

        updateGenerateButtonState();
        populateEligibleSubjectsUI();
    }

    // ---------------------------------------------------------
    // SECTION LISTENER
    // ---------------------------------------------------------

    private void setupSectionSpinnerListener() {

        spinnerSection.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            android.widget.AdapterView<?> parent,
                            android.view.View view,
                            int position,
                            long id) {

                        if (sectionList.isEmpty()) {
                            return;
                        }

                        if (position < 0 ||
                                position >= sectionList.size()) {
                            return;
                        }

                        selectedSection =
                                sectionList.get(position);

                        updateGenerateButtonState();
                        populateEligibleSubjectsUI();
                    }

                    @Override
                    public void onNothingSelected(
                            android.widget.AdapterView<?> parent) {
                    }
                }
        );
    }

    // ---------------------------------------------------------
    // COHORT IDENTIFIER & SCOPED SUBJECT SELECTION
    // ---------------------------------------------------------

    private String getCohortKey() {
        if (selectedDepartment == null || selectedSemester == null || selectedSection == null) {
            return "";
        }
        String deptId = selectedDepartment.getId() != null ? selectedDepartment.getId() : "DEPT";
        int sem = selectedSemester.getSemesterNumber();
        String sec = selectedSection.getSectionName() != null ? selectedSection.getSectionName().trim() : "SEC";
        return deptId + "_S" + sem + "_" + sec;
    }

    private List<ClassAssignment> getCohortAssignments() {
        List<ClassAssignment> cohortList = new ArrayList<>();
        if (selectedDepartment == null || selectedSemester == null || selectedSection == null) {
            return cohortList;
        }

        String collegeId = currentUser != null ? currentUser.getCollegeId() : "";
        int targetSemNum = selectedSemester.getSemesterNumber();
        String targetSecNorm = ClassAssignment.normalizeSection(selectedSection.getSectionName());

        for (ClassAssignment assignment : allAssignmentList) {
            if (assignment == null) continue;

            if (assignment.getCollegeId() != null && !assignment.getCollegeId().equalsIgnoreCase(collegeId)) {
                continue;
            }

            boolean deptMatches = false;
            if (selectedDepartment.getId() != null && selectedDepartment.getId().equalsIgnoreCase(assignment.getDepartmentId())) {
                deptMatches = true;
            } else if (selectedDepartment.getName() != null && selectedDepartment.getName().equalsIgnoreCase(assignment.getDepartmentName())) {
                deptMatches = true;
            } else if (assignment.getDepartmentId() == null || assignment.getDepartmentId().trim().isEmpty()) {
                if (assignment.getSubjectId() != null) {
                    for (Subject s : subjectList) {
                        if (s != null && s.getId() != null && s.getId().equalsIgnoreCase(assignment.getSubjectId())) {
                            if (selectedDepartment.getId() != null && selectedDepartment.getId().equalsIgnoreCase(s.getDepartmentId())) {
                                deptMatches = true;
                            }
                            break;
                        }
                    }
                }
            }

            if (!deptMatches) continue;

            int assignSem = assignment.getSemester();
            if (assignSem <= 0 && assignment.getSubjectId() != null) {
                for (Subject s : subjectList) {
                    if (s != null && s.getId() != null && s.getId().equalsIgnoreCase(assignment.getSubjectId())) {
                        assignSem = s.getSemester();
                        break;
                    }
                }
            }

            if (assignSem != targetSemNum) continue;

            String assignSecNorm = ClassAssignment.normalizeSection(assignment.getSectionName());
            boolean sectionMatches = false;
            if (selectedSection.getId() != null && selectedSection.getId().equalsIgnoreCase(assignment.getSectionId())) {
                sectionMatches = true;
            } else if (!targetSecNorm.isEmpty() && targetSecNorm.equalsIgnoreCase(assignSecNorm)) {
                sectionMatches = true;
            } else if (selectedSection.getSectionName() != null &&
                    selectedSection.getSectionName().equalsIgnoreCase(assignment.getSectionName())) {
                sectionMatches = true;
            }

            if (!sectionMatches) continue;

            if (assignment.getAcademicYear() != null && !assignment.getAcademicYear().trim().isEmpty()
                    && selectedAcademicYear != null && !selectedAcademicYear.trim().isEmpty()) {
                String aNorm = assignment.getAcademicYear().replaceAll("[^0-9]", "");
                String sNorm = selectedAcademicYear.replaceAll("[^0-9]", "");
                if (!aNorm.isEmpty() && !sNorm.isEmpty() && !aNorm.equals(sNorm)) {
                    continue;
                }
            }

            cohortList.add(assignment);
        }
        return cohortList;
    }

    private void populateEligibleSubjectsUI() {
        if (llSubjectListContainer == null) return;
        llSubjectListContainer.removeAllViews();

        String cohortKey = getCohortKey();
        if (cohortKey.isEmpty()) {
            if (tvSubjectSelectionStatus != null) {
                tvSubjectSelectionStatus.setVisibility(View.VISIBLE);
                tvSubjectSelectionStatus.setText("Select an academic group above to load eligible subjects.");
            }
            if (btnToggleSelectAll != null) btnToggleSelectAll.setVisibility(View.GONE);
            updateGenerateButtonState();
            return;
        }

        List<ClassAssignment> cohortAssignments = getCohortAssignments();

        if (cohortAssignments.isEmpty()) {
            if (tvSubjectSelectionStatus != null) {
                tvSubjectSelectionStatus.setVisibility(View.VISIBLE);
                tvSubjectSelectionStatus.setText("No mapped class assignments found for "
                        + (selectedSemester != null ? selectedSemester.getSemesterName() : "Semester")
                        + " Section " + (selectedSection != null ? selectedSection.getSectionName() : "A")
                        + ".\nClick the button below to map subjects & faculty.");
            }
            if (tvSubjectSelectionSubtitle != null) {
                tvSubjectSelectionSubtitle.setText("0 eligible subjects mapped for this cohort");
            }
            if (btnToggleSelectAll != null) btnToggleSelectAll.setVisibility(View.GONE);
            updateGenerateButtonState();
            return;
        }

        if (tvSubjectSelectionStatus != null) {
            tvSubjectSelectionStatus.setVisibility(View.GONE);
        }
        if (btnToggleSelectAll != null) {
            btnToggleSelectAll.setVisibility(View.VISIBLE);
        }

        // Initialize scoped selection if not already present for this cohort
        Set<String> selectedSet = scopedSelectedSubjectIds.get(cohortKey);
        if (selectedSet == null) {
            selectedSet = new HashSet<>();
            for (ClassAssignment ca : cohortAssignments) {
                if (ca.getSubjectId() != null && !ca.getSubjectId().trim().isEmpty()) {
                    selectedSet.add(ca.getSubjectId().trim());
                }
            }
            scopedSelectedSubjectIds.put(cohortKey, selectedSet);
        }
        final Set<String> activeSelectedSet = selectedSet;

        if (tvSubjectSelectionSubtitle != null) {
            tvSubjectSelectionSubtitle.setText("Choose subjects for "
                    + selectedSemester.getSemesterName() + " Section "
                    + selectedSection.getSectionName() + " (" + cohortAssignments.size() + " mapped):");
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (ClassAssignment assignment : cohortAssignments) {
            View itemView = inflater.inflate(R.layout.item_generator_subject, llSubjectListContainer, false);

            CheckBox cb = itemView.findViewById(R.id.cbSubjectSelected);
            TextView tvName = itemView.findViewById(R.id.tvSubjectNameCode);
            TextView tvBadge = itemView.findViewById(R.id.tvSubjectTypeBadge);
            TextView tvHours = itemView.findViewById(R.id.tvSubjectHoursCredits);
            TextView tvFaculty = itemView.findViewById(R.id.tvFacultyAndVenue);

            final String subId = assignment.getSubjectId() != null ? assignment.getSubjectId().trim() : "";
            String subName = assignment.getSubjectName() != null ? assignment.getSubjectName() : "Subject";
            String subCode = assignment.getSubjectCode() != null ? assignment.getSubjectCode() : "";

            if (!subCode.isEmpty()) {
                tvName.setText(subName + " (" + subCode + ")");
            } else {
                tvName.setText(subName);
            }

            boolean isLab = "Lab".equalsIgnoreCase(assignment.getClassType()) || assignment.isLab();
            if (isLab) {
                tvBadge.setText("Laboratory");
                tvBadge.setBackgroundColor(0xFFE8EAF6);
                tvBadge.setTextColor(0xFF283593);
            } else {
                tvBadge.setText("Theory");
                tvBadge.setBackgroundColor(0xFFE3F2FD);
                tvBadge.setTextColor(0xFF1565C0);
            }

            tvHours.setText(assignment.getWeeklyHours() + " hrs/wk • " + assignment.getCredits() + " Credits");

            String fName = assignment.getFacultyName() != null && !assignment.getFacultyName().isEmpty() ? assignment.getFacultyName() : "Faculty assigned";
            String rName = assignment.getRoomNumber() != null && !assignment.getRoomNumber().isEmpty() ? assignment.getRoomNumber() : (isLab ? "Lab Block" : "Room 101");
            tvFaculty.setText("Faculty: " + fName + " • Venue: " + rName);

            cb.setChecked(activeSelectedSet.contains(subId));
            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    activeSelectedSet.add(subId);
                } else {
                    activeSelectedSet.remove(subId);
                }
                updateSelectAllButtonText(activeSelectedSet, cohortAssignments);
                updateGenerateButtonState();
            });

            itemView.setOnClickListener(v -> cb.toggle());

            llSubjectListContainer.addView(itemView);
        }

        updateSelectAllButtonText(activeSelectedSet, cohortAssignments);
        updateGenerateButtonState();
    }

    private void updateSelectAllButtonText(Set<String> selectedSet, List<ClassAssignment> cohortAssignments) {
        if (btnToggleSelectAll == null) return;
        boolean allSelected = selectedSet.size() >= cohortAssignments.size();
        btnToggleSelectAll.setText(allSelected ? "Deselect All" : "Select All");
    }

    private void toggleSelectAll() {
        String cohortKey = getCohortKey();
        if (cohortKey.isEmpty()) return;
        List<ClassAssignment> cohortAssignments = getCohortAssignments();
        if (cohortAssignments.isEmpty()) return;

        Set<String> selectedSet = scopedSelectedSubjectIds.get(cohortKey);
        if (selectedSet == null) {
            selectedSet = new HashSet<>();
            scopedSelectedSubjectIds.put(cohortKey, selectedSet);
        }

        if (selectedSet.size() >= cohortAssignments.size()) {
            selectedSet.clear();
        } else {
            for (ClassAssignment ca : cohortAssignments) {
                if (ca.getSubjectId() != null) selectedSet.add(ca.getSubjectId().trim());
            }
        }
        populateEligibleSubjectsUI();
    }

    private void renderTimetablePreview(Chromosome chromosome) {
        if (llTimetableSlotsPreview == null || chromosome == null) return;
        llTimetableSlotsPreview.removeAllViews();

        // 1. Update Preview Header Card
        TextView tvHkbkCollegeTitle = findViewById(R.id.tvHkbkCollegeTitle);
        TextView tvHkbkCollegeSubtitle = findViewById(R.id.tvHkbkCollegeSubtitle);
        ImageView ivHkbkTimetableLogo = findViewById(R.id.ivHkbkTimetableLogo);
        String collegeName = currentUser != null && currentUser.getCollegeName() != null ?
                CollegeIdentityHelper.formatCollegeName(currentUser.getCollegeName()) : "HKBK College of Engineering";
        String collegeAddr = currentUser != null && currentUser.getCollegeAddress() != null ?
                currentUser.getCollegeAddress() : CollegeIdentityHelper.getVerifiedCollegeAddress(currentUser != null ? currentUser.getCollegeId() : null, collegeName);

        if (tvHkbkCollegeTitle != null) {
            tvHkbkCollegeTitle.setText(collegeName.toUpperCase());
        }
        if (tvHkbkCollegeSubtitle != null) {
            tvHkbkCollegeSubtitle.setText("Approved by AICTE, New Delhi • Affiliated to VTU, Belagavi • " + collegeAddr);
        }
        if (ivHkbkTimetableLogo != null && currentUser != null) {
            CollegeIdentityHelper.bindCollegeLogo(currentUser.getCollegeId(), collegeName, ivHkbkTimetableLogo);
        }

        if (tvPreviewDepartment != null && selectedDepartment != null) {
            tvPreviewDepartment.setText("Department: " + selectedDepartment.getName());
        }
        if (tvPreviewAcademicYear != null && selectedAcademicYear != null) {
            tvPreviewAcademicYear.setText("Academic Year: " + selectedAcademicYear);
        }
        if (tvPreviewSemSec != null && selectedSemester != null && selectedSection != null) {
            tvPreviewSemSec.setText("Semester: " + selectedSemester.getSemesterName() + " | Section: " + selectedSection.getSectionName());
        }
        if (tvPreviewTimetableBadge != null) {
            tvPreviewTimetableBadge.setText("OFFICIAL CLASS TIMETABLE • AI GENERATED");
        }

        List<Gene> genes = chromosome.getGenes();
        if (genes == null || genes.isEmpty()) {
            TextView emptyTv = new TextView(this);
            emptyTv.setText("No slots generated.");
            emptyTv.setTextColor(0xFF78909C);
            emptyTv.setPadding(16, 16, 16, 16);
            llTimetableSlotsPreview.addView(emptyTv);
            return;
        }

        // 2. Group slots by day
        Map<String, List<Gene>> dayMap = new LinkedHashMap<>();
        String[] standardDays = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        for (String day : standardDays) {
            dayMap.put(day, new ArrayList<>());
        }

        for (Gene g : genes) {
            if (g == null) continue;
            String d = g.getDay() != null ? g.getDay().trim() : "Monday";
            boolean matched = false;
            for (String stdDay : standardDays) {
                if (stdDay.equalsIgnoreCase(d)) {
                    dayMap.get(stdDay).add(g);
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                if (!dayMap.containsKey(d)) {
                    dayMap.put(d, new ArrayList<>());
                }
                dayMap.get(d).add(g);
            }
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (Map.Entry<String, List<Gene>> entry : dayMap.entrySet()) {
            List<Gene> dayGenes = entry.getValue();
            if (dayGenes.isEmpty()) continue;

            // Sort chronologically by start time (numeric minutes)
            Collections.sort(dayGenes, (a, b) -> {
                int minA = UIUtils.parseTimeToMinutes(a.getStartTime());
                int minB = UIUtils.parseTimeToMinutes(b.getStartTime());
                return Integer.compare(minA, minB);
            });

            // Prominent Day Header
            TextView dayHeader = new TextView(this);
            dayHeader.setText("🗓️  " + entry.getKey().toUpperCase() + " (" + dayGenes.size() + " SESSIONS)");
            dayHeader.setTextSize(13f);
            dayHeader.setTextColor(0xFF0D47A1);
            dayHeader.setTypeface(null, android.graphics.Typeface.BOLD);
            dayHeader.setPadding(8, 16, 8, 8);
            llTimetableSlotsPreview.addView(dayHeader);

            boolean shortBreakShown = false;
            boolean lunchBreakShown = false;

            for (Gene g : dayGenes) {
                int startMinutes = UIUtils.parseTimeToMinutes(g.getStartTime());

                // Short Break: 09:00 AM - 09:20 AM (Period 2 starts at 09:20 AM = 560 min)
                if (!shortBreakShown && startMinutes >= 560) {
                    llTimetableSlotsPreview.addView(UIUtils.createBreakCard(this, "Short Tea Break", "09:00 AM - 09:20 AM (Protected 20 mins)"));
                    shortBreakShown = true;
                }

                // Lunch Break: 12:50 PM - 01:40 PM (Period 6 starts at 01:40 PM = 820 min)
                if (!lunchBreakShown && startMinutes >= 820) {
                    llTimetableSlotsPreview.addView(UIUtils.createBreakCard(this, "College Lunch Break", "12:50 PM - 01:40 PM (Protected 50 mins)"));
                    lunchBreakShown = true;
                }

                View slotCard = inflater.inflate(R.layout.item_teacher_timetable_slot, llTimetableSlotsPreview, false);

                TextView tvTimeSlot = slotCard.findViewById(R.id.tvTimeSlot);
                TextView tvDayName = slotCard.findViewById(R.id.tvDayName);
                TextView tvSubjectTitle = slotCard.findViewById(R.id.tvSubjectTitle);
                TextView tvSectionRoom = slotCard.findViewById(R.id.tvSectionRoom);
                TextView tvClassTypeBadge = slotCard.findViewById(R.id.tvClassTypeBadge);

                String timeStr = (g.getStartTime() != null ? g.getStartTime() : "08:20 AM")
                        + " - " + (g.getEndTime() != null ? g.getEndTime() : "09:00 AM");
                if (tvTimeSlot != null) tvTimeSlot.setText(timeStr);
                if (tvDayName != null) tvDayName.setText(g.getDay());

                String subTitle = g.getSubjectName() != null ? g.getSubjectName() : "Class Session";
                if (tvSubjectTitle != null) tvSubjectTitle.setText(subTitle);

                String teacher = g.getTeacherName() != null && !g.getTeacherName().isEmpty() ? g.getTeacherName() : "Faculty";
                String venue = g.getRoomName() != null && !g.getRoomName().isEmpty() ? g.getRoomName() : "Assigned Room";
                if (tvSectionRoom != null) {
                    tvSectionRoom.setText("Faculty: " + teacher + " • Venue: " + venue);
                }

                if (tvClassTypeBadge != null) {
                    boolean isLab = "Lab".equalsIgnoreCase(g.getClassType());
                    if (isLab) {
                        tvClassTypeBadge.setText("Laboratory • Practical");
                        tvClassTypeBadge.setBackgroundColor(0xFFE8EAF6);
                        tvClassTypeBadge.setTextColor(0xFF283593);
                    } else {
                        tvClassTypeBadge.setText("Theory • Lecture");
                        tvClassTypeBadge.setBackgroundColor(0xFFE3F2FD);
                        tvClassTypeBadge.setTextColor(0xFF1565C0);
                    }
                }

                llTimetableSlotsPreview.addView(slotCard);
            }

            if ("Friday".equalsIgnoreCase(entry.getKey()) || "Saturday".equalsIgnoreCase(entry.getKey())) {
                TextView cutoffNotice = new TextView(this);
                if ("Friday".equalsIgnoreCase(entry.getKey())) {
                    cutoffNotice.setText("🕌 Friday Cutoff at 12:50 PM — Afternoon prayer & half-day schedule");
                } else {
                    cutoffNotice.setText("🗓️ Saturday Schedule — 5 periods ending at 12:50 PM");
                }
                cutoffNotice.setTextSize(11f);
                cutoffNotice.setTextColor(0xFF2E7D32);
                cutoffNotice.setPadding(16, 6, 16, 16);
                llTimetableSlotsPreview.addView(cutoffNotice);
            }
        }
    }

    // ---------------------------------------------------------
    // GENERATE BUTTON STATE
    // ---------------------------------------------------------

    private void updateGenerateButtonState() {

        boolean ready =
                selectedDepartment != null &&
                        selectedSemester != null &&
                        selectedSection != null &&
                        selectedAcademicYear != null &&
                        !selectedAcademicYear.trim().isEmpty();

        String cohortKey = getCohortKey();
        Set<String> selectedSet = scopedSelectedSubjectIds.get(cohortKey);
        boolean hasSelectedSubjects = selectedSet != null && !selectedSet.isEmpty();

        btnRunGAEngine.setEnabled(ready && hasSelectedSubjects);
    }

    // ---------------------------------------------------------
    // WORKING DAYS
    // ---------------------------------------------------------

    private List<String> getSelectedWorkingDays() {

        List<String> days = new ArrayList<>();

        if (checkMonday.isChecked()) {
            days.add("Monday");
        }

        if (checkTuesday.isChecked()) {
            days.add("Tuesday");
        }

        if (checkWednesday.isChecked()) {
            days.add("Wednesday");
        }

        if (checkThursday.isChecked()) {
            days.add("Thursday");
        }

        if (checkFriday.isChecked()) {
            days.add("Friday");
        }

        if (checkSaturday.isChecked()) {
            days.add("Saturday");
        }

        return days;
    }

    // ---------------------------------------------------------
    // MAIN GENERATION
    // ---------------------------------------------------------

    private void runGeneticAlgorithm() {

        if (selectedDepartment == null ||
                selectedSemester == null ||
                selectedSection == null ||
                selectedAcademicYear == null) {

            UIUtils.showToast(
                    this,
                    "Please select Department, Academic Year, Semester and Section."
            );

            return;
        }

        List<String> selectedWorkingDays =
                getSelectedWorkingDays();

        if (selectedWorkingDays.isEmpty()) {

            UIUtils.showToast(
                    this,
                    "Please select at least one working day."
            );

            return;
        }

        String collegeId =
                currentUser.getCollegeId();

        if (collegeId == null ||
                collegeId.trim().isEmpty()) {

            UIUtils.showToast(
                    this,
                    "College ID is missing."
            );

            return;
        }

        // -----------------------------------------------------
        // FILTER ACTUAL ASSIGNMENTS
        // -----------------------------------------------------

        assignmentList.clear();

        int targetSemNum = selectedSemester.getSemesterNumber();
        String targetSecNorm = ClassAssignment.normalizeSection(selectedSection.getSectionName());

        int countMatchingDept = 0;
        int countMatchingSem = 0;
        int countMatchingSec = 0;

        for (ClassAssignment assignment : allAssignmentList) {
            if (assignment == null) continue;

            // 1. College Scope
            if (assignment.getCollegeId() != null && !assignment.getCollegeId().equalsIgnoreCase(collegeId)) {
                continue;
            }

            // 2. Department Resolution
            boolean deptMatches = false;
            if (selectedDepartment.getId() != null && selectedDepartment.getId().equalsIgnoreCase(assignment.getDepartmentId())) {
                deptMatches = true;
            } else if (selectedDepartment.getName() != null && selectedDepartment.getName().equalsIgnoreCase(assignment.getDepartmentName())) {
                deptMatches = true;
            } else if (assignment.getDepartmentId() == null || assignment.getDepartmentId().trim().isEmpty()) {
                // Backward-compatibility: resolve department from related subject
                if (assignment.getSubjectId() != null) {
                    for (Subject s : subjectList) {
                        if (s != null && s.getId() != null && s.getId().equalsIgnoreCase(assignment.getSubjectId())) {
                            if (selectedDepartment.getId() != null && selectedDepartment.getId().equalsIgnoreCase(s.getDepartmentId())) {
                                deptMatches = true;
                            }
                            break;
                        }
                    }
                }
            }

            if (!deptMatches) continue;
            countMatchingDept++;

            // 3. Semester Resolution
            int assignSem = assignment.getSemester();
            if (assignSem <= 0 && assignment.getSubjectId() != null) {
                // Backward-compatibility: resolve semester from related subject
                for (Subject s : subjectList) {
                    if (s != null && s.getId() != null && s.getId().equalsIgnoreCase(assignment.getSubjectId())) {
                        assignSem = s.getSemester();
                        break;
                    }
                }
            }

            if (assignSem != targetSemNum) continue;
            countMatchingSem++;

            // 4. Section Resolution
            String assignSecNorm = ClassAssignment.normalizeSection(assignment.getSectionName());
            boolean sectionMatches = false;

            if (selectedSection.getId() != null && selectedSection.getId().equalsIgnoreCase(assignment.getSectionId())) {
                sectionMatches = true;
            } else if (!targetSecNorm.isEmpty() && targetSecNorm.equalsIgnoreCase(assignSecNorm)) {
                sectionMatches = true;
            } else if (selectedSection.getSectionName() != null &&
                    selectedSection.getSectionName().equalsIgnoreCase(assignment.getSectionName())) {
                sectionMatches = true;
            }

            if (!sectionMatches) continue;
            countMatchingSec++;

            // 5. Academic Year (if present on assignment)
            if (assignment.getAcademicYear() != null && !assignment.getAcademicYear().trim().isEmpty()
                    && selectedAcademicYear != null && !selectedAcademicYear.trim().isEmpty()) {
                String aNorm = assignment.getAcademicYear().replaceAll("[^0-9]", "");
                String sNorm = selectedAcademicYear.replaceAll("[^0-9]", "");
                if (!aNorm.isEmpty() && !sNorm.isEmpty() && !aNorm.equals(sNorm)) {
                    continue;
                }
            }

            assignmentList.add(assignment);
        }

        // -----------------------------------------------------
        // DIAGNOSTIC CHECK IF EMPTY
        // -----------------------------------------------------

        if (assignmentList.isEmpty()) {
            StringBuilder diagMsg = new StringBuilder();
            diagMsg.append("No class assignments found for:\n");
            diagMsg.append("• Department: ").append(selectedDepartment.getName()).append("\n");
            diagMsg.append("• Academic Year: ").append(selectedAcademicYear).append("\n");
            diagMsg.append("• Semester: ").append(selectedSemester.getSemesterName()).append("\n");
            diagMsg.append("• Section: ").append(selectedSection.getSectionName()).append("\n\n");

            diagMsg.append("Diagnostic Analysis:\n");
            if (allAssignmentList.isEmpty()) {
                diagMsg.append("• No class assignments exist in Firestore for this college (0 total records).\n");
            } else if (countMatchingDept == 0) {
                diagMsg.append("• Found ").append(allAssignmentList.size()).append(" assignments in Firestore, but none belong to department '")
                        .append(selectedDepartment.getName()).append("'.\n");
            } else if (countMatchingSem == 0) {
                diagMsg.append("• Found ").append(countMatchingDept).append(" assignments for department '")
                        .append(selectedDepartment.getName()).append("', but none for Semester ").append(targetSemNum).append(".\n");
            } else if (countMatchingSec == 0) {
                diagMsg.append("• Found ").append(countMatchingSem).append(" assignments for Semester ").append(targetSemNum)
                        .append(", but none mapped to Section '").append(selectedSection.getSectionName()).append("'.\n");
            } else {
                diagMsg.append("• Assignments exist for this section but have mismatched academic year or unassigned hours.\n");
            }

            diagMsg.append("\nPlease use 'Map Class Assignments' to map the subjects and faculty for this semester and section.");

            new AlertDialog.Builder(this)
                    .setTitle("Class Assignments Required")
                    .setMessage(diagMsg.toString())
                    .setPositiveButton("Map Class Assignments", (dialog, which) -> {
                        startActivity(new Intent(this, ClassAssignmentActivity.class));
                    })
                    .setNegativeButton("Close", null)
                    .show();

            return;
        }

        // Filter cohort assignments to include only selected subjects
        List<ClassAssignment> cohortAssignments = new ArrayList<>(assignmentList);
        String cohortKey = getCohortKey();
        Set<String> selectedSet = scopedSelectedSubjectIds.get(cohortKey);

        assignmentList.clear();
        for (ClassAssignment ca : cohortAssignments) {
            if (ca.getSubjectId() != null && selectedSet != null && selectedSet.contains(ca.getSubjectId().trim())) {
                assignmentList.add(ca);
            }
        }

        if (assignmentList.isEmpty()) {
            UIUtils.showAlert(this, "No Subjects Selected", "Please select at least one subject to schedule from the 'Select Subjects to Schedule' list above.");
            return;
        }

        // -----------------------------------------------------
        // DISPLAY GENERATION INFORMATION
        // -----------------------------------------------------

        String dayText =
                joinDays(selectedWorkingDays);

        ProgressDialog progressDialog =
                UIUtils.showLoadingDialog(
                        this,
                        "Preparing AI timetable...\n"
                                + "Department: "
                                + selectedDepartment.getName()
                                + "\n"
                                + "Semester: "
                                + selectedSemester.getSemesterName()
                                + "\n"
                                + "Section: "
                                + selectedSection.getSectionName()
                                + "\n"
                                + "Academic Year: "
                                + selectedAcademicYear
                                + "\n"
                                + "Working Days: "
                                + dayText
                                + "\n\n"
                                + "Creating GA population...\n"
                                + "Evaluating constraints..."
                );

        // -----------------------------------------------------
        // RUN EXISTING GA
        // -----------------------------------------------------

        FirebaseHelper.getClassSessionsRef()
                .whereEqualTo("collegeId", currentUser.getCollegeId())
                .whereEqualTo("sectionName", selectedSection.getSectionName())
                .get()
                .addOnCompleteListener(existingSessionsTask -> {
                    List<ClassSession> existingSessions = new ArrayList<>();
                    if (existingSessionsTask.isSuccessful() && existingSessionsTask.getResult() != null) {
                        for (DocumentSnapshot doc : existingSessionsTask.getResult()) {
                            ClassSession cs = doc.toObject(ClassSession.class);
                            if (cs != null) existingSessions.add(cs);
                        }
                    }

                    try {
                        GeneticAlgorithm ga =
                                new GeneticAlgorithm(
                                        50,
                                        100,
                                        0.05
                                );

                        lastGeneratedResult =
                                ga.run(
                                        assignmentList,
                                        facultyList,
                                        classroomList,
                                        labList,
                                        sectionList,
                                        subjectList,
                                        selectedWorkingDays,
                                        existingSessions
                                );

                        progressDialog.dismiss();

                        if (lastGeneratedResult == null) {

                            UIUtils.showAlert(
                                    this,
                                    "Generation Failed",
                                    "The Genetic Algorithm did not return a timetable."
                            );

                            return;
                        }

                        // -------------------------------------------------
                        // FITNESS
                        // -------------------------------------------------

                        tvFitnessScore.setText(
                                "Fitness Score: "
                                        + String.format(
                                        "%.1f",
                                        lastGeneratedResult.getFitness()
                                )
                                        + "%"
                        );

                        // -------------------------------------------------
                        // CONFLICTS
                        // -------------------------------------------------

                        if (lastGeneratedResult
                                .getHardConflictsCount() == 0) {

                            tvConflicts.setText(
                                    "Hard Conflicts: 0 (Feasible Timetable)"
                            );

                            tvConflicts.setTextColor(
                                    0xFF2E7D32
                            );

                        } else {

                            tvConflicts.setText(
                                    "Hard Conflicts: "
                                            + lastGeneratedResult
                                            .getHardConflictsCount()
                            );

                            tvConflicts.setTextColor(
                                    0xFFC62828
                            );
                        }

                        // -------------------------------------------------
                        // XAI
                        // -------------------------------------------------

                        String explanation =
                                ExplainableAI.generateRationale(
                                        lastGeneratedResult
                                );

                        tvXAIExplanation.setText(
                                "Configuration\n"
                                        + "Department: "
                                        + selectedDepartment.getName()
                                        + "\n"
                                        + "Academic Year: "
                                        + selectedAcademicYear
                                        + "\n"
                                        + "Semester: "
                                        + selectedSemester.getSemesterName()
                                        + "\n"
                                        + "Section: "
                                        + selectedSection.getSectionName()
                                        + "\n"
                                        + "Working Days: "
                                        + dayText
                                        + "\n\n"
                                        + explanation
                        );

                        renderTimetablePreview(lastGeneratedResult);

                        UIUtils.showToast(
                                TimetableGeneratorActivity.this,
                                "AI Timetable Generated Successfully!"
                        );

                    } catch (Exception e) {

                        progressDialog.dismiss();

                        UIUtils.showAlert(
                                TimetableGeneratorActivity.this,
                                "Generation Error",
                                e.getMessage() != null
                                        ? e.getMessage()
                                        : "Unknown error occurred."
                        );
                    }

                });
    }

    // ---------------------------------------------------------
    // PUBLISH TIMETABLE
    // ---------------------------------------------------------

    private void publishTimetable() {

        if (lastGeneratedResult == null ||
                lastGeneratedResult.getGenes() == null ||
                lastGeneratedResult.getGenes().isEmpty()) {

            UIUtils.showToast(
                    this,
                    "Please generate the timetable first."
            );

            return;
        }

        String collegeId =
                currentUser.getCollegeId();

        if (collegeId == null ||
                collegeId.trim().isEmpty()) {

            UIUtils.showToast(
                    this,
                    "College ID is missing."
            );

            return;
        }

        showPublishConfirmationDialog();
    }

    private void updateClassTeacherSpinner() {
        if (spinnerClassTeacher == null) return;
        List<String> teacherNames = new ArrayList<>();
        int selectedIndex = 0;
        for (int i = 0; i < facultyList.size(); i++) {
            User u = facultyList.get(i);
            String name = u.getName() != null ? u.getName() : "Faculty";
            teacherNames.add(name);
            if (name.toLowerCase(java.util.Locale.US).contains("pushpa")) {
                selectedIndex = i;
            }
        }
        if (teacherNames.isEmpty()) {
            teacherNames.add("Dr. Pushpa Mohan (Class Teacher)");
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, teacherNames);
        spinnerClassTeacher.setAdapter(adapter);
        spinnerClassTeacher.setSelection(selectedIndex);
    }

    private Timetable buildTimetableFromCurrentState() {
        String collegeId = currentUser.getCollegeId();
        Timetable timetable = new Timetable();
        timetable.setTimetableId("TT_PREVIEW_" + System.currentTimeMillis());
        timetable.setCollegeId(collegeId);
        timetable.setDepartmentId(selectedDepartment != null ? selectedDepartment.getId() : "");
        timetable.setDepartmentName(selectedDepartment != null ? selectedDepartment.getName() : "Computer Science & Engineering");

        String cName = currentUser != null && currentUser.getCollegeName() != null ?
                CollegeIdentityHelper.formatCollegeName(currentUser.getCollegeName()) : "HKBK College of Engineering";
        String cAddr = currentUser != null && currentUser.getCollegeAddress() != null ?
                currentUser.getCollegeAddress() : CollegeIdentityHelper.getVerifiedCollegeAddress(collegeId, cName);

        timetable.setCollegeName(cName);
        timetable.setCollegeAddress(cAddr);

        String classTeacher = (spinnerClassTeacher != null && spinnerClassTeacher.getSelectedItem() != null)
                ? spinnerClassTeacher.getSelectedItem().toString() : "Dr. Pushpa Mohan";
        timetable.setClassTeacherName(classTeacher);
        timetable.setEffectiveStartDate(selectedWefDate != null ? selectedWefDate : "2026-08-01");
        timetable.setEffectiveEndDate("2026-12-31");
        timetable.setCoordinatorName(etCoordinatorName != null && !etCoordinatorName.getText().toString().trim().isEmpty()
                ? etCoordinatorName.getText().toString().trim() : "Dr. Sivarajan");
        timetable.setHodName(etHodName != null && !etHodName.getText().toString().trim().isEmpty()
                ? etHodName.getText().toString().trim() : "Dr. Vilasini M");
        timetable.setPrincipalName(etPrincipalName != null && !etPrincipalName.getText().toString().trim().isEmpty()
                ? etPrincipalName.getText().toString().trim() : "Dr. Pushpa Mohan");

        timetable.setSemester(selectedSemester != null ? selectedSemester.getSemesterNumber() : 7);
        timetable.setSection(selectedSection != null ? selectedSection.getSectionName() : "A");
        timetable.setAcademicYear(selectedAcademicYear != null ? selectedAcademicYear : "2026-27");

        List<TimetableSlot> slots = new ArrayList<>();
        if (lastGeneratedResult != null && lastGeneratedResult.getGenes() != null) {
            for (Gene g : lastGeneratedResult.getGenes()) {
                if (g == null || g.getClassSession() == null || g.getTimeSlot() == null) continue;
                TimetableSlot slot = new TimetableSlot();
                slot.setSlotId(g.getTimeSlot().getSlotId());
                slot.setDay(g.getTimeSlot().getDayOfWeek());
                slot.setDayOfWeek(g.getTimeSlot().getDayOfWeek());
                slot.setPeriodIndex(g.getTimeSlot().getPeriodNumber());
                slot.setStartTime(g.getTimeSlot().getStartTime());
                slot.setEndTime(g.getTimeSlot().getEndTime());
                slot.setSubjectName(g.getClassSession().getSubjectName());
                slot.setSubjectCode(g.getClassSession().getSubjectCode());
                slot.setTeacherName(g.getClassSession().getTeacherName());
                slot.setRoomName(g.getClassroom() != null ? g.getClassroom().getRoomNumber() : "CR 101");
                slot.setLab(g.getClassSession().isLab());
                slots.add(slot);
            }
        }
        timetable.setSlots(slots);
        return timetable;
    }

    private void showPublishConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Publish Timetable & Assign Class Teacher");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = UIUtils.dpToPx(this, 16);
        layout.setPadding(pad, pad / 2, pad, pad / 2);

        TextView tvDetails = new TextView(this);
        String semSec = (selectedSemester != null ? selectedSemester.getSemesterName() : "Sem") + " • Section " +
                (selectedSection != null ? selectedSection.getSectionName() : "A");
        tvDetails.setText("• Department: " + (selectedDepartment != null ? selectedDepartment.getName() : "N/A") +
                "\n• Cohort: " + semSec +
                "\n• Academic Year: " + selectedAcademicYear +
                "\n• Fitness Score: " + String.format(java.util.Locale.US, "%.1f%%", lastGeneratedResult.getFitness()));
        tvDetails.setTextColor(0xFF263238);
        tvDetails.setPadding(0, 0, 0, pad / 2);
        layout.addView(tvDetails);

        TextView tvTeacherLabel = new TextView(this);
        tvTeacherLabel.setText("Class Teacher for this Section:");
        tvTeacherLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTeacherLabel.setTextColor(0xFF0D47A1);
        layout.addView(tvTeacherLabel);

        Spinner spClassTeacher = new Spinner(this);
        List<String> teacherDisplayNames = new ArrayList<>();
        List<User> eligibleFaculty = new ArrayList<>();
        for (User u : facultyList) {
            if (u != null && (selectedDepartment == null || u.getDepartmentId() == null ||
                    u.getDepartmentId().isEmpty() || u.getDepartmentId().equalsIgnoreCase(selectedDepartment.getId()))) {
                eligibleFaculty.add(u);
                teacherDisplayNames.add(u.getName() != null ? u.getName() : (u.getEmail() != null ? u.getEmail() : "Faculty"));
            }
        }
        if (teacherDisplayNames.isEmpty()) {
            for (User u : facultyList) {
                eligibleFaculty.add(u);
                teacherDisplayNames.add(u.getName() != null ? u.getName() : "Faculty");
            }
        }
        if (teacherDisplayNames.isEmpty()) {
            teacherDisplayNames.add("Pushpa Mohan (Default Class Teacher)");
        }
        spClassTeacher.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, teacherDisplayNames));
        layout.addView(spClassTeacher);
        if (spinnerClassTeacher != null && spinnerClassTeacher.getSelectedItemPosition() >= 0 && spinnerClassTeacher.getSelectedItemPosition() < eligibleFaculty.size()) {
            spClassTeacher.setSelection(spinnerClassTeacher.getSelectedItemPosition());
        }

        TextView tvStartDateLabel = new TextView(this);
        tvStartDateLabel.setText("Effective Start Date:");
        tvStartDateLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        tvStartDateLabel.setTextColor(0xFF0D47A1);
        tvStartDateLabel.setPadding(0, pad / 2, 0, 0);
        layout.addView(tvStartDateLabel);

        final String[] effectiveStartDate = {selectedWefDate != null && !selectedWefDate.isEmpty() ? selectedWefDate : "2026-08-01"};
        TextView tvStartDate = new TextView(this);
        tvStartDate.setText(effectiveStartDate[0] + " (Click to change)");
        tvStartDate.setTextColor(0xFF1565C0);
        tvStartDate.setPadding(0, 4, 0, pad / 2);
        tvStartDate.setOnClickListener(v -> {
            java.util.Calendar c = java.util.Calendar.getInstance();
            new android.app.DatePickerDialog(this, (view, year, month, day) -> {
                effectiveStartDate[0] = String.format(java.util.Locale.US, "%04d-%02d-%02d", year, month + 1, day);
                tvStartDate.setText(effectiveStartDate[0] + " (Click to change)");
            }, c.get(java.util.Calendar.YEAR), 7, 1).show();
        });
        layout.addView(tvStartDate);

        TextView tvEndDateLabel = new TextView(this);
        tvEndDateLabel.setText("Effective End Date:");
        tvEndDateLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        tvEndDateLabel.setTextColor(0xFF0D47A1);
        layout.addView(tvEndDateLabel);

        final String[] effectiveEndDate = {"2026-12-31"};
        TextView tvEndDate = new TextView(this);
        tvEndDate.setText(effectiveEndDate[0] + " (Click to change)");
        tvEndDate.setTextColor(0xFF1565C0);
        tvEndDate.setPadding(0, 4, 0, pad / 2);
        tvEndDate.setOnClickListener(v -> {
            java.util.Calendar c = java.util.Calendar.getInstance();
            new android.app.DatePickerDialog(this, (view, year, month, day) -> {
                effectiveEndDate[0] = String.format(java.util.Locale.US, "%04d-%02d-%02d", year, month + 1, day);
                tvEndDate.setText(effectiveEndDate[0] + " (Click to change)");
            }, c.get(java.util.Calendar.YEAR), 11, 31).show();
        });
        layout.addView(tvEndDate);

        builder.setView(layout);
        builder.setPositiveButton("Publish Timetable", (dialog, which) -> {
            String selectedTeacherId = "";
            String selectedTeacherName = "";
            int pos = spClassTeacher.getSelectedItemPosition();
            if (pos >= 0 && pos < eligibleFaculty.size()) {
                User u = eligibleFaculty.get(pos);
                selectedTeacherId = u.getUid() != null ? u.getUid() : "";
                selectedTeacherName = u.getName() != null ? u.getName() : "";
            } else if (spClassTeacher.getSelectedItem() != null) {
                selectedTeacherName = spClassTeacher.getSelectedItem().toString();
            }

            executePublishTimetable(selectedTeacherId, selectedTeacherName, effectiveStartDate[0], effectiveEndDate[0]);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void executePublishTimetable(String classTeacherId, String classTeacherName, String effectiveStartDate, String effectiveEndDate) {
        String collegeId = currentUser.getCollegeId();
        ProgressDialog dialog =
                UIUtils.showLoadingDialog(
                        this,
                        "Publishing timetable & creating class sessions..."
                );

        String timetableId =
                FirebaseHelper
                        .getTimetablesRef()
                        .document()
                        .getId();

        Timetable timetable =
                new Timetable();

        timetable.setTimetableId(
                timetableId
        );

        timetable.setCollegeId(
                collegeId
        );

        timetable.setDepartmentId(
                selectedDepartment != null ? selectedDepartment.getId() : ""
        );

        timetable.setDepartmentName(
                selectedDepartment != null ? selectedDepartment.getName() : "Computer Science & Engineering"
        );

        String cName = currentUser != null && currentUser.getCollegeName() != null ?
                CollegeIdentityHelper.formatCollegeName(currentUser.getCollegeName()) : "HKBK College of Engineering";
        String cAddr = currentUser != null && currentUser.getCollegeAddress() != null ?
                currentUser.getCollegeAddress() : CollegeIdentityHelper.getVerifiedCollegeAddress(collegeId, cName);

        timetable.setCollegeName(cName);
        timetable.setCollegeAddress(cAddr);

        timetable.setClassTeacherId(classTeacherId);
        timetable.setClassTeacherName(classTeacherName);
        timetable.setEffectiveStartDate(effectiveStartDate);
        timetable.setEffectiveEndDate(effectiveEndDate);
        timetable.setCoordinatorName(etCoordinatorName != null && !etCoordinatorName.getText().toString().trim().isEmpty()
                ? etCoordinatorName.getText().toString().trim() : "Dr. Sivarajan");
        timetable.setHodName(etHodName != null && !etHodName.getText().toString().trim().isEmpty()
                ? etHodName.getText().toString().trim() : "Dr. Vilasini M");
        timetable.setPrincipalName(etPrincipalName != null && !etPrincipalName.getText().toString().trim().isEmpty()
                ? etPrincipalName.getText().toString().trim() : "Dr. Pushpa Mohan");
        timetable.setPublishedBy(currentUser.getName() != null ? currentUser.getName() : currentUser.getEmail());

        timetable.setSemester(
                selectedSemester != null ? selectedSemester.getSemesterNumber() : 1
        );

        timetable.setSection(
                selectedSection != null ? selectedSection.getSectionName() : "A"
        );

        timetable.setAcademicYear(
                selectedAcademicYear != null ? selectedAcademicYear : ""
        );

        timetable.setTitle(
                "AI Timetable - "
                        + (selectedDepartment != null ? selectedDepartment.getName() : "Dept")
                        + " - "
                        + (selectedSemester != null ? selectedSemester.getSemesterName() : "Sem")
                        + " - "
                        + (selectedSection != null ? selectedSection.getSectionName() : "Sec")
        );

        timetable.setStatus(
                Timetable.STATE_PUBLISHED
        );

        timetable.setFitnessScore(
                lastGeneratedResult.getFitness()
        );

        timetable.setHardConflictsCount(
                lastGeneratedResult
                        .getHardConflictsCount()
        );

        timetable.setPublishedAt(
                System.currentTimeMillis()
        );

        List<TimetableSlot> slots =
                new ArrayList<>();

        for (Gene gene :
                lastGeneratedResult.getGenes()) {

            // -------------------------------------------------
            // TIMETABLE SLOT
            // -------------------------------------------------

            TimetableSlot slot =
                    new TimetableSlot();

            slot.setId(
                    gene.getGeneId()
            );

            slot.setTimetableId(
                    timetableId
            );

            slot.setCollegeId(
                    collegeId
            );

            slot.setDay(
                    gene.getDay()
            );

            slot.setPeriodIndex(
                    gene.getTimeSlot() != null ? gene.getTimeSlot().getPeriodNumber() : 0
            );

            slot.setStartTime(
                    gene.getStartTime()
            );

            slot.setEndTime(
                    gene.getEndTime()
            );

            slot.setSubjectId(
                    gene.getSubjectId()
            );

            slot.setSubjectName(
                    gene.getSubjectName()
            );

            slot.setTeacherUid(
                    gene.getTeacherUid()
            );

            slot.setTeacherName(
                    gene.getTeacherName()
            );

            slot.setSectionId(
                    gene.getSectionId()
            );

            slot.setSectionName(
                    gene.getSectionName()
            );

            slot.setRoomId(
                    gene.getRoomId()
            );

            slot.setRoomName(
                    gene.getRoomName()
            );

            slot.setClassType(
                    gene.getClassType()
            );

            slot.setPublished(true);

            slots.add(slot);

            // -------------------------------------------------
            // CLASS SESSION
            // -------------------------------------------------

            ClassSession session =
                    new ClassSession();

            session.setSessionId(
                    "SESS_" + gene.getGeneId()
            );

            session.setCollegeId(
                    collegeId
            );

            session.setSubjectId(
                    gene.getSubjectId()
            );

            session.setSubjectName(
                    gene.getSubjectName()
            );

            session.setTeacherUid(
                    gene.getTeacherUid()
            );

            session.setFacultyId(
                    gene.getTeacherUid()
            );

            session.setTeacherName(
                    gene.getTeacherName()
            );

            session.setFacultyName(
                    gene.getTeacherName()
            );

            session.setSectionId(
                    gene.getSectionId()
            );

            session.setSectionName(
                    gene.getSectionName()
            );

            session.setSection(
                    gene.getSectionName() != null ? gene.getSectionName() : gene.getSectionId()
            );

            if (selectedSemester != null) {
                session.setSemester(selectedSemester.getSemesterNumber());
            }

            if (selectedDepartment != null) {
                session.setDepartmentId(selectedDepartment.getId());
            }

            session.setClassType(gene.getClassType());
            session.setLab("Lab".equalsIgnoreCase(gene.getClassType()));

            session.setClassroomId(
                    gene.getRoomId()
            );

            session.setRoomNumber(
                    gene.getRoomName()
            );

            session.setDayOfWeek(
                    gene.getDay()
            );

            session.setStartTime(
                    gene.getStartTime()
            );

            session.setEndTime(
                    gene.getEndTime()
            );

            FirebaseHelper
                    .getClassSessionsRef()
                    .document(
                            session.getSessionId()
                    )
                    .set(session);
        }

        timetable.setSlots(slots);

        // -----------------------------------------------------
        // SAVE TIMETABLE
        // -----------------------------------------------------

        FirebaseHelper
                .getTimetablesRef()
                .document(timetableId)
                .set(timetable)
                .addOnSuccessListener(aVoid -> {

                    // Dispatch notification to college notification feed
                    String notifId = FirebaseHelper.getNotificationsRef().document().getId();
                    String deptName = selectedDepartment != null ? selectedDepartment.getName() : "Department";
                    String semName = selectedSemester != null ? selectedSemester.getSemesterName() : "Semester";
                    String secName = selectedSection != null ? selectedSection.getSectionName() : "A";
                    String timestampStr = new java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault()).format(new java.util.Date());

                    NotificationItem notif = new NotificationItem(
                            notifId,
                            "Official Timetable Published",
                            "Official Timetable for " + deptName + " • " + semName + " Section " + secName + " (" + selectedAcademicYear + ") is now active.",
                            "TIMETABLE",
                            collegeId,
                            selectedDepartment != null ? selectedDepartment.getId() : "",
                            "ALL",
                            timestampStr
                    );
                    FirebaseHelper.getNotificationsRef().document(notifId).set(notif);

                    dialog.dismiss();

                    UIUtils.showToast(
                            TimetableGeneratorActivity.this,
                            "Timetable Published to Firestore & Notifications Sent!"
                    );

                    finish();

                })
                .addOnFailureListener(e -> {

                    dialog.dismiss();

                    UIUtils.showAlert(
                            TimetableGeneratorActivity.this,
                            "Publish Failed",
                            e.getMessage() != null
                                    ? e.getMessage()
                                    : "Unable to publish timetable."
                    );
                });
    }

    // ---------------------------------------------------------
    // SPINNER HELPER
    // ---------------------------------------------------------

    private void setSpinnerItems(
            Spinner spinner,
            List<String> items) {

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        items
                );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinner.setAdapter(adapter);
    }

    private List<String> createSingleItemList(
            String text) {

        List<String> list =
                new ArrayList<>();

        list.add(text);

        return list;
    }

    // ---------------------------------------------------------
    // DAY TEXT HELPER
    // ---------------------------------------------------------

    private String joinDays(
            List<String> days) {

        if (days == null ||
                days.isEmpty()) {

            return "None";
        }

        StringBuilder builder =
                new StringBuilder();

        for (int i = 0;
             i < days.size();
             i++) {

            builder.append(days.get(i));

            if (i < days.size() - 1) {
                builder.append(", ");
            }
        }

        return builder.toString();
    }
}