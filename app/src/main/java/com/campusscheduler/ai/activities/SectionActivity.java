package com.campusscheduler.ai.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.campusscheduler.ai.R;
import com.campusscheduler.ai.adapters.GenericAdapter;
import com.campusscheduler.ai.models.Department;
import com.campusscheduler.ai.models.Section;
import com.campusscheduler.ai.models.Semester;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.repository.AcademicRepository;
import com.campusscheduler.ai.utils.LocalAuthManager;
import com.campusscheduler.ai.utils.UIUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class SectionActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private FloatingActionButton fabAddSection;
    private RecyclerView rvSections;

    private GenericAdapter<Section> adapter;

    private final List<Section> sectionList =
            new ArrayList<>();

    private final List<Department> departmentList =
            new ArrayList<>();

    private final List<Semester> semesterList =
            new ArrayList<>();

    private User currentUser;

    private Spinner spinnerDepartment;
    private Spinner spinnerSemester;

    private int selectedDepartmentPosition = -1;
    private int selectedSemesterPosition = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_section);

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

        initializeViews();
        setupRecyclerView();

        btnBack.setOnClickListener(v -> finish());

        fabAddSection.setOnClickListener(
                v -> showAddSectionDialog()
        );

        loadSections();
    }

    // ============================================================
    // INITIALIZE
    // ============================================================

    private void initializeViews() {

        btnBack = findViewById(R.id.btnBack);
        fabAddSection = findViewById(R.id.fabAddSection);
        rvSections = findViewById(R.id.rvSections);
    }

    // ============================================================
    // RECYCLER VIEW
    // ============================================================

    private void setupRecyclerView() {

        rvSections.setLayoutManager(
                new LinearLayoutManager(this)
        );

        adapter = new GenericAdapter<>(
                sectionList,
                (item, holder) -> {

                    String sectionName =
                            item.getSectionName();

                    if (sectionName == null ||
                            sectionName.trim().isEmpty()) {

                        sectionName = "Unnamed Section";
                    }

                    String departmentName =
                            item.getDepartmentName();

                    if (departmentName == null ||
                            departmentName.trim().isEmpty()) {

                        departmentName = "Department not configured";
                    }

                    String displaySection = sectionName.toUpperCase(java.util.Locale.US).startsWith("SECTION")
                            ? sectionName : "Section " + sectionName;
                    String title = displaySection + " • " + departmentName;

                    holder.setText(
                            R.id.tvTitle,
                            title
                    );

                    String yearInfo = item.getAcademicYear() != null && !item.getAcademicYear().trim().isEmpty()
                            ? " (" + item.getAcademicYear() + ")" : "";
                    holder.setText(
                            R.id.tvSubtitle,
                            "Semester " + item.getSemester() + yearInfo + " • Capacity: " + item.getStudentCount() + " Students"
                    );

                    /*
                     * Tap section card for actions.
                     */
                    holder.itemView.setOnClickListener(
                            v -> showSectionActions(item)
                    );

                    /*
                     * Long press also opens actions.
                     */
                    holder.itemView.setOnLongClickListener(
                            v -> {

                                showSectionActions(item);

                                return true;
                            }
                    );
                },
                R.layout.item_generic
        );

        rvSections.setAdapter(adapter);
    }

    // ============================================================
    // LOAD SECTIONS
    // ============================================================

    private void loadSections() {

        String collegeId = (currentUser != null && currentUser.getCollegeId() != null && !currentUser.getCollegeId().trim().isEmpty())
                ? currentUser.getCollegeId() : LocalAuthManager.getSafeCollegeId(this, getIntent());

        AcademicRepository
                .getInstance()
                .getSections(
                        collegeId,
                        task -> {

                            sectionList.clear();

                            if (task.isSuccessful() &&
                                    task.getResult() != null) {

                                for (var doc :
                                        task.getResult()) {

                                    Section section =
                                            doc.toObject(
                                                    Section.class
                                            );

                                    if (section != null) {

                                        /*
                                         * Firestore document ID
                                         * becomes model ID if
                                         * model field is empty.
                                         */
                                        if (section.getId() == null ||
                                                section.getId()
                                                        .trim()
                                                        .isEmpty()) {

                                            section.setId(
                                                    doc.getId()
                                            );
                                        }

                                        sectionList.add(
                                                section
                                        );
                                    }
                                }

                            } else {

                                String error =
                                        task.getException() != null
                                                ? task.getException()
                                                .getMessage()
                                                : "Unable to load sections.";

                                UIUtils.showToast(
                                        this,
                                        error
                                );
                            }

                            adapter.notifyDataSetChanged();

                            if (sectionList.isEmpty()) {

                                UIUtils.showToast(
                                        this,
                                        "No sections found."
                                );
                            }
                        }
                );
    }

    // ============================================================
    // SECTION ACTIONS
    // ============================================================

    private void showSectionActions(
            Section section
    ) {

        String name =
                section.getSectionName();

        if (name == null ||
                name.trim().isEmpty()) {

            name = "Section";
        }

        new AlertDialog.Builder(this)
                .setTitle(name)
                .setItems(
                        new String[]{
                                "Delete Section",
                                "Cancel"
                        },
                        (dialog, which) -> {

                            if (which == 0) {

                                confirmDeleteSection(
                                        section
                                );
                            }
                        }
                )
                .show();
    }

    // ============================================================
    // DELETE SECTION
    // ============================================================

    private void confirmDeleteSection(
            Section section
    ) {

        String name =
                section.getSectionName();

        if (name == null ||
                name.trim().isEmpty()) {

            name = "this section";
        }

        new AlertDialog.Builder(this)
                .setTitle("Delete Section?")
                .setMessage(
                        "Are you sure you want to delete \""
                                + name
                                + "\"?\n\n"
                                + "The section record will be "
                                + "removed from Firestore."
                )
                .setNegativeButton(
                        "Cancel",
                        null
                )
                .setPositiveButton(
                        "Delete",
                        (dialog, which) ->
                                deleteSection(
                                        section
                                )
                )
                .show();
    }

    private void deleteSection(
            Section section
    ) {

        String sectionId =
                section.getId();

        if (sectionId == null ||
                sectionId.trim().isEmpty()) {

            UIUtils.showToast(
                    this,
                    "Section ID is missing."
            );

            return;
        }

        /*
         * Delete directly through the existing
         * centralized FirebaseHelper.
         *
         * No new collection is created.
         */
        com.campusscheduler.ai.utils.FirebaseHelper
                .getSectionsRef()
                .document(sectionId)
                .delete()
                .addOnSuccessListener(unused -> {

                    UIUtils.showToast(
                            this,
                            "Section deleted successfully."
                    );

                    loadSections();
                })
                .addOnFailureListener(e -> {

                    UIUtils.showToast(
                            this,
                            "Delete failed: "
                                    + e.getMessage()
                    );
                });
    }

    // ============================================================
    // ADD SECTION
    // ============================================================

    private void showAddSectionDialog() {
        String safeCollegeId = currentUser != null && currentUser.getCollegeId() != null && !currentUser.getCollegeId().trim().isEmpty() ?
                currentUser.getCollegeId() : LocalAuthManager.getSafeCollegeId(this, getIntent());
        if (currentUser == null) {
            currentUser = new User();
            currentUser.setCollegeId(safeCollegeId);
            currentUser.setDepartmentId("DEPT_CSE");
        } else if (currentUser.getCollegeId() == null || currentUser.getCollegeId().trim().isEmpty()) {
            currentUser.setCollegeId(safeCollegeId);
        }

        AlertDialog.Builder builder =
                new AlertDialog.Builder(this);

        builder.setTitle("Add Section");

        LinearLayout layout =
                new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        layout.setPadding(
                40,
                20,
                40,
                20
        );

        // ========================================================
        // DEPARTMENT
        // ========================================================

        Spinner departmentSpinner =
                new Spinner(this);

        spinnerDepartment =
                departmentSpinner;

        layout.addView(
                departmentSpinner
        );

        // ========================================================
        // SEMESTER
        // ========================================================

        Spinner semesterSpinner =
                new Spinner(this);

        spinnerSemester =
                semesterSpinner;

        layout.addView(
                semesterSpinner
        );

        // ========================================================
        // SECTION NAME
        // ========================================================

        EditText etName =
                new EditText(this);

        etName.setHint(
                "Section Name (e.g. A)"
        );

        layout.addView(
                etName
        );

        // ========================================================
        // STUDENT COUNT
        // ========================================================

        EditText etCount =
                new EditText(this);

        etCount.setHint(
                "Student Count (e.g. 60)"
        );

        etCount.setInputType(
                InputType.TYPE_CLASS_NUMBER
        );

        layout.addView(
                etCount
        );

        builder.setView(layout);

        /*
         * Load real departments.
         */
        loadDepartmentsForDialog(
                departmentSpinner
        );

        /*
         * Semester is initially disabled.
         * It will be populated after department selection.
         */
        semesterSpinner.setEnabled(false);

        departmentSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {

                        selectedDepartmentPosition =
                                position;

                        loadSemestersForDialog(
                                semesterSpinner,
                                position
                        );
                    }

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent
                    ) {
                    }
                }
        );

        builder.setPositiveButton(
                "Add",
                null
        );

        builder.setNegativeButton(
                "Cancel",
                null
        );

        AlertDialog dialog =
                builder.create();

        dialog.setOnShowListener(
                dialogInterface -> {

                    dialog.getButton(
                            AlertDialog.BUTTON_POSITIVE
                    ).setOnClickListener(
                            v -> {

                                // -----------------------------
                                // DEPARTMENT & SEMESTER RESOLUTION
                                // -----------------------------
                                if (spinnerDepartment != null && spinnerDepartment.getSelectedItemPosition() >= 0
                                        && spinnerDepartment.getSelectedItemPosition() < departmentList.size()) {
                                    selectedDepartmentPosition = spinnerDepartment.getSelectedItemPosition();
                                }
                                if (spinnerSemester != null && spinnerSemester.getSelectedItemPosition() >= 0
                                        && spinnerSemester.getSelectedItemPosition() < semesterList.size()) {
                                    selectedSemesterPosition = spinnerSemester.getSelectedItemPosition();
                                }

                                if (selectedDepartmentPosition < 0 ||
                                        selectedDepartmentPosition >=
                                                departmentList.size()) {

                                    UIUtils.showToast(
                                            this,
                                            "Please select a department."
                                    );

                                    return;
                                }

                                if (selectedSemesterPosition < 0 ||
                                        selectedSemesterPosition >=
                                                semesterList.size()) {

                                    UIUtils.showToast(
                                            this,
                                            "Please select a semester."
                                    );

                                    return;
                                }

                                // -----------------------------
                                // SECTION NAME
                                // -----------------------------

                                String name =
                                        etName.getText()
                                                .toString()
                                                .trim();

                                if (name.isEmpty()) {

                                    etName.setError(
                                            "Section name is required"
                                    );

                                    return;
                                }

                                // -----------------------------
                                // STUDENT COUNT & CAPACITY VALIDATION
                                // -----------------------------

                                String countString =
                                        etCount.getText()
                                                .toString()
                                                .trim();

                                if (countString.isEmpty()) {

                                    etCount.setError(
                                            "Student count is required"
                                    );

                                    return;
                                }

                                int studentCount;

                                try {

                                    studentCount =
                                            Integer.parseInt(
                                                    countString
                                            );

                                } catch (Exception e) {

                                    etCount.setError(
                                            "Enter a valid number"
                                    );

                                    return;
                                }

                                if (studentCount < 0) {

                                    etCount.setError(
                                            "Student count cannot be negative"
                                    );

                                    return;
                                }

                                Department department =
                                        departmentList.get(
                                                selectedDepartmentPosition
                                        );

                                Semester semester =
                                        semesterList.get(
                                                selectedSemesterPosition
                                        );

                                String departmentId =
                                        department.getId();

                                if (departmentId == null ||
                                        departmentId.trim().isEmpty()) {

                                    UIUtils.showToast(
                                            this,
                                            "Department ID is missing."
                                    );

                                    return;
                                }

                                String semesterId =
                                        semester.getId();

                                if (semesterId == null ||
                                        semesterId.trim().isEmpty()) {

                                    UIUtils.showToast(
                                            this,
                                            "Semester ID is missing."
                                    );

                                    return;
                                }

                                String departmentName =
                                        department.getDepartmentName();

                                if (departmentName == null ||
                                        departmentName.trim().isEmpty()) {

                                    departmentName =
                                            department.getName();
                                }

                                // Enforce 60-seat ceiling rule with available capacity recommendations
                                if (studentCount > 60) {
                                    int excess = studentCount - 60;
                                    StringBuilder altBuilder = new StringBuilder();
                                    for (Section s : sectionList) {
                                        if (s.getDepartmentId() != null && s.getDepartmentId().equalsIgnoreCase(departmentId)
                                                && s.getSemester() == semester.getSemesterNumber()) {
                                            int remaining = 60 - s.getStudentCount();
                                            if (remaining > 0) {
                                                altBuilder.append("\n• Section ").append(s.getSectionName())
                                                        .append(" has ").append(remaining).append(" seats available.");
                                            }
                                        }
                                    }
                                    String advice = altBuilder.length() > 0 ?
                                            "\n\nSuggested alternative section capacity in Semester " + semester.getSemesterNumber() + ":" + altBuilder.toString() :
                                            "\n\nPlease create an additional section or reduce student count to 60.";
                                    UIUtils.showAlert(SectionActivity.this, "Section Capacity Exceeded",
                                            "Standard section capacity is limited to 60 students. You entered " + studentCount + " students (" + excess + " excess)." + advice);
                                    return;
                                }

                                // -----------------------------
                                // DUPLICATE SECTION CHECK (Scoped to Department & Semester)
                                // -----------------------------
                                for (Section existingSec : sectionList) {
                                    if (existingSec.getDepartmentId() != null && existingSec.getDepartmentId().equalsIgnoreCase(departmentId)) {
                                        boolean sameSem = (existingSec.getSemester() == semester.getSemesterNumber())
                                                || (existingSec.getSemesterId() != null && existingSec.getSemesterId().equalsIgnoreCase(semesterId));
                                        if (sameSem && existingSec.getSectionName() != null
                                                && existingSec.getSectionName().trim().equalsIgnoreCase(name)) {
                                            UIUtils.showAlert(SectionActivity.this, "Duplicate Section",
                                                    "Section '" + name + "' already exists for Semester " + semester.getSemesterNumber() + " in " + departmentName + ".");
                                            return;
                                        }
                                    }
                                }

                                // -----------------------------
                                // CREATE SECTION
                                // -----------------------------

                                String sectionId =
                                        "SEC_"
                                                + System.currentTimeMillis();

                                Section section =
                                        new Section(
                                                sectionId,
                                                name,
                                                studentCount
                                        );

                                /*
                                 * Correct college relationship.
                                 */
                                section.setCollegeId(
                                        currentUser.getCollegeId()
                                 );

                                /*
                                 * Correct department relationship.
                                 */
                                section.setDepartmentId(
                                        departmentId
                                );

                                section.setDepartmentName(
                                        departmentName
                                );

                                /*
                                 * IMPORTANT:
                                 *
                                 * Generator searches:
                                 *
                                 * departmentId
                                 * semester
                                 *
                                 * Therefore save the SAME
                                 * semester number as SemesterActivity.
                                 */
                                section.setSemester(
                                        semester.getSemesterNumber()
                                );
                                section.setSemesterNumber(
                                        semester.getSemesterNumber()
                                );
                                section.setSemesterId(
                                        semesterId
                                );
                                if (semester.getAcademicYear() != null && !semester.getAcademicYear().trim().isEmpty()) {
                                    section.setAcademicYear(
                                            semester.getAcademicYear().trim()
                                    );
                                }

                                // -----------------------------
                                // SAVE
                                // -----------------------------

                                AcademicRepository
                                        .getInstance()
                                        .saveSection(
                                                section,
                                                task -> {

                                                    if (task.isSuccessful()) {

                                                        UIUtils.showToast(
                                                                SectionActivity.this,
                                                                "Section added successfully."
                                                        );

                                                        dialog.dismiss();

                                                        loadSections();

                                                    } else {

                                                        String error =
                                                                task.getException() != null
                                                                        ? task.getException()
                                                                        .getMessage()
                                                                        : "Unable to save section.";

                                                        UIUtils.showToast(
                                                                SectionActivity.this,
                                                                "Save failed: "
                                                                        + error
                                                        );
                                                    }
                                                }
                                        );
                            }
                    );
                }
        );

        dialog.show();
    }

    // ============================================================
    // LOAD DEPARTMENTS
    // ============================================================

    private void loadDepartmentsForDialog(
            Spinner spinner
    ) {

        departmentList.clear();

        AcademicRepository
                .getInstance()
                .getDepartments(
                        currentUser.getCollegeId(),
                        task -> {

                            departmentList.clear();

                            if (task.isSuccessful() &&
                                    task.getResult() != null) {

                                for (var doc :
                                        task.getResult()) {

                                    Department department =
                                            doc.toObject(
                                                    Department.class
                                            );

                                    if (department != null) {

                                        if (department.getId() == null ||
                                                department.getId()
                                                        .trim()
                                                        .isEmpty()) {

                                            department.setId(
                                                    doc.getId()
                                            );
                                        }

                                        departmentList.add(
                                                department
                                        );
                                    }
                                }
                            }

                            List<String> names =
                                    new ArrayList<>();

                            for (Department department :
                                    departmentList) {

                                String name =
                                        department.getDepartmentName();

                                if (name == null ||
                                        name.trim().isEmpty()) {

                                    name =
                                            department.getName();
                                }

                                if (name == null ||
                                        name.trim().isEmpty()) {

                                    name =
                                            department.getId();
                                }

                                names.add(name);
                            }

                            if (names.isEmpty()) {

                                names.add(
                                        "No departments available"
                                );
                            }

                            ArrayAdapter<String> adapter =
                                    new ArrayAdapter<>(
                                            SectionActivity.this,
                                            android.R.layout.simple_spinner_item,
                                            names
                                    );

                            adapter.setDropDownViewResource(
                                    android.R.layout.simple_spinner_dropdown_item
                            );

                            spinner.setAdapter(adapter);

                            spinner.setEnabled(
                                    !departmentList.isEmpty()
                            );

                            if (!departmentList.isEmpty()) {

                                selectedDepartmentPosition = 0;

                                loadSemestersForDialog(
                                        spinnerSemester,
                                        0
                                );
                            }
                        }
                );
    }

    // ============================================================
    // LOAD SEMESTERS FOR SELECTED DEPARTMENT
    // ============================================================

    private void loadSemestersForDialog(
            Spinner spinner,
            int departmentPosition
    ) {

        semesterList.clear();

        selectedSemesterPosition = -1;

        if (departmentPosition < 0 ||
                departmentPosition >=
                        departmentList.size()) {

            spinner.setEnabled(false);

            return;
        }

        Department department =
                departmentList.get(
                        departmentPosition
                );

        String departmentId =
                department.getId();

        if (departmentId == null ||
                departmentId.trim().isEmpty()) {

            spinner.setEnabled(false);

            return;
        }

        AcademicRepository
                .getInstance()
                .getSemesters(
                        currentUser.getCollegeId(),
                        task -> {

                            semesterList.clear();

                            if (task.isSuccessful() &&
                                    task.getResult() != null) {

                                for (var doc :
                                        task.getResult()) {

                                    Semester semester =
                                            doc.toObject(
                                                    Semester.class
                                            );

                                    if (semester == null) {
                                        continue;
                                    }

                                    if (semester.getId() == null ||
                                            semester.getId()
                                                    .trim()
                                                    .isEmpty()) {

                                        semester.setId(
                                                doc.getId()
                                        );
                                    }

                                    /*
                                     * Only show semesters
                                     * belonging to selected
                                     * department.
                                     */
                                    if (departmentId.equals(
                                            semester.getDepartmentId()
                                    )) {

                                        semesterList.add(
                                                semester
                                        );
                                    }
                                }
                            }

                            List<String> names =
                                    new ArrayList<>();

                            for (Semester semester :
                                    semesterList) {

                                String name =
                                        semester.getSemesterName();

                                if (name == null ||
                                        name.trim().isEmpty()) {

                                    name =
                                            "Semester "
                                                    + semester.getSemesterNumber();
                                }

                                String year =
                                        semester.getAcademicYear();

                                if (year != null &&
                                        !year.trim().isEmpty()) {

                                    name +=
                                            " • "
                                                    + year;
                                }

                                names.add(name);
                            }

                            if (names.isEmpty()) {

                                names.add(
                                        "No semesters for this department"
                                );
                            }

                            ArrayAdapter<String> adapter =
                                    new ArrayAdapter<>(
                                            SectionActivity.this,
                                            android.R.layout.simple_spinner_item,
                                            names
                                    );

                            adapter.setDropDownViewResource(
                                    android.R.layout.simple_spinner_dropdown_item
                            );

                            spinner.setAdapter(adapter);

                            spinner.setEnabled(
                                    !semesterList.isEmpty()
                            );

                            if (!semesterList.isEmpty()) {
                                selectedSemesterPosition = 0;
                            }

                            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                @Override
                                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                    selectedSemesterPosition = position;
                                }

                                @Override
                                public void onNothingSelected(AdapterView<?> parent) {
                                }
                            });
                        }
                );
    }
}