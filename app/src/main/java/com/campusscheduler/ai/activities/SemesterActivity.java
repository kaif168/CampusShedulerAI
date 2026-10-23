package com.campusscheduler.ai.activities;

import android.app.AlertDialog;
import android.os.Bundle;
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
import com.campusscheduler.ai.models.Semester;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.repository.AcademicRepository;
import com.campusscheduler.ai.utils.FirebaseHelper;
import com.campusscheduler.ai.utils.LocalAuthManager;
import com.campusscheduler.ai.utils.UIUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class SemesterActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private ImageButton btnSyncSemesters;
    private FloatingActionButton fabAddSemester;
    private RecyclerView rvSemesters;

    private GenericAdapter<Semester> adapter;

    private final List<Semester> semesterList =
            new ArrayList<>();

    private final List<Department> departmentList =
            new ArrayList<>();

    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_semester);

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

        btnBack = findViewById(R.id.btnBack);
        btnSyncSemesters = findViewById(R.id.btnSyncSemesters);
        fabAddSemester = findViewById(R.id.fabAddSemester);
        rvSemesters = findViewById(R.id.rvSemesters);

        btnBack.setOnClickListener(v -> finish());
        if (btnSyncSemesters != null) {
            btnSyncSemesters.setOnClickListener(v -> confirmSetupStandardSemesters());
        }

        fabAddSemester.setOnClickListener(
                v -> showAddSemesterDialog()
        );

        rvSemesters.setLayoutManager(
                new LinearLayoutManager(this)
        );

        adapter = new GenericAdapter<>(
                semesterList,
                (item, holder) -> {

                    String semesterName =
                            item.getSemesterName();

                    if (semesterName == null ||
                            semesterName.trim().isEmpty()) {

                        semesterName =
                                Semester.formatSemesterOrdinal(item.getSemesterNumber());
                    }

                    holder.setText(
                            R.id.tvTitle,
                            semesterName
                                    + " (Number: "
                                    + item.getSemesterNumber()
                                    + ")"
                    );

                    String academicYear =
                            item.getAcademicYear();

                    if (academicYear == null ||
                            academicYear.trim().isEmpty()) {

                        academicYear =
                                "Academic year not configured";
                    }

                    holder.setText(
                            R.id.tvSubtitle,
                            "Academic Year: "
                                    + academicYear
                    );

                    /*
                     * Tap / long press on semester card
                     * opens the semester action menu.
                     */
                    holder.itemView.setOnClickListener(
                            v -> showSemesterActions(item)
                    );

                    holder.itemView.setOnLongClickListener(
                            v -> {

                                showSemesterActions(item);

                                return true;
                            }
                    );
                },
                R.layout.item_generic
        );

        rvSemesters.setAdapter(adapter);

        loadSemesters();
    }

    // ============================================================
    // LOAD SEMESTERS
    // ============================================================

    private void loadSemesters() {

        String collegeId = (currentUser != null && currentUser.getCollegeId() != null && !currentUser.getCollegeId().trim().isEmpty())
                ? currentUser.getCollegeId() : LocalAuthManager.getSafeCollegeId(this, getIntent());

        AcademicRepository
                .getInstance()
                .getSemesters(
                        collegeId,
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

                                    if (semester != null) {

                                        if (semester.getId() == null ||
                                                semester.getId()
                                                        .trim()
                                                        .isEmpty()) {

                                            semester.setId(
                                                    doc.getId()
                                            );
                                        }

                                        semesterList.add(
                                                semester
                                        );
                                    }
                                }

                                // Numerical sort (1st to 8th Semester)
                                java.util.Collections.sort(semesterList, (a, b) -> Integer.compare(a.getSemesterNumber(), b.getSemesterNumber()));

                            } else {

                                String error =
                                        task.getException() != null
                                                ? task.getException()
                                                .getMessage()
                                                : "Unable to load semesters.";

                                UIUtils.showToast(
                                        this,
                                        error
                                );
                            }

                            adapter.notifyDataSetChanged();

                            if (semesterList.isEmpty()) {

                                UIUtils.showToast(
                                        this,
                                        "No semesters found for this college."
                                );
                            }
                        }
                );
    }

    // ============================================================
    // SEMESTER ACTIONS
    // ============================================================

    private void showSemesterActions(
            Semester semester
    ) {

        String name =
                semester.getSemesterName();

        if (name == null ||
                name.trim().isEmpty()) {

            name =
                    Semester.formatSemesterOrdinal(semester.getSemesterNumber());
        }

        new AlertDialog.Builder(this)
                .setTitle(name)
                .setItems(
                        new String[]{
                                "Edit Semester",
                                "Delete Semester",
                                "Cancel"
                        },
                        (dialog, which) -> {

                            if (which == 0) {
                                showEditSemesterDialog(semester);
                            } else if (which == 1) {
                                confirmDeleteSemester(semester);
                            }
                        }
                )
                .show();
    }

    private void showEditSemesterDialog(Semester semester) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Semester");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        EditText etName = new EditText(this);
        etName.setHint("Semester Name");
        etName.setText(semester.getSemesterName() != null ? semester.getSemesterName() : Semester.formatSemesterOrdinal(semester.getSemesterNumber()));
        layout.addView(etName);

        EditText etNum = new EditText(this);
        etNum.setHint("Semester Number (1-8)");
        etNum.setText(String.valueOf(semester.getSemesterNumber()));
        etNum.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(etNum);

        EditText etYear = new EditText(this);
        etYear.setHint("Academic Year (e.g. 2026-27)");
        etYear.setText(semester.getAcademicYear() != null ? semester.getAcademicYear() : "2026-27");
        layout.addView(etYear);

        builder.setView(layout);
        builder.setPositiveButton("Save", (dialog, which) -> {
            String name = etName.getText().toString().trim();
            String numStr = etNum.getText().toString().trim();
            String year = etYear.getText().toString().trim();

            int num = semester.getSemesterNumber();
            try { num = Integer.parseInt(numStr); } catch (Exception ignored) {}

            semester.setSemesterName(name.isEmpty() ? Semester.formatSemesterOrdinal(num) : name);
            semester.setSemesterNumber(num);
            semester.setAcademicYear(year.isEmpty() ? "2026-27" : year);

            if (semester.getId() != null) {
                FirebaseHelper.getSemestersRef().document(semester.getId()).set(semester)
                        .addOnSuccessListener(aVoid -> {
                            UIUtils.showToast(this, "Semester updated successfully.");
                            loadSemesters();
                        })
                        .addOnFailureListener(e -> UIUtils.showToast(this, "Update failed: " + e.getMessage()));
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void confirmSetupStandardSemesters() {
        new AlertDialog.Builder(this)
                .setTitle("Verify & Setup Semesters (1st–8th)")
                .setMessage("This will verify semester records and create any missing canonical semesters (1st through 8th Semester) for 2026-27 without duplicating existing records.\n\nProceed?")
                .setPositiveButton("Setup Semesters", (dialog, which) -> executeStandardSemestersSetup())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void executeStandardSemestersSetup() {
        String collegeId = currentUser != null && currentUser.getCollegeId() != null ?
                currentUser.getCollegeId() : LocalAuthManager.getSafeCollegeId(this, getIntent());
        if (currentUser != null && (currentUser.getCollegeId() == null || currentUser.getCollegeId().trim().isEmpty())) {
            currentUser.setCollegeId(collegeId);
        }

        java.util.Set<Integer> existingNums = new java.util.HashSet<>();
        for (Semester s : semesterList) {
            existingNums.add(s.getSemesterNumber());
        }

        com.google.firebase.firestore.WriteBatch batch = FirebaseHelper.getFirestore().batch();
        int added = 0;

        for (int i = 1; i <= 8; i++) {
            if (!existingNums.contains(i)) {
                String docId = "SEM_" + collegeId + "_" + i;
                Semester s = new Semester(docId, collegeId, "DEPT_CSE", i, "2026-27");
                s.setSemesterName(Semester.formatSemesterOrdinal(i));
                batch.set(FirebaseHelper.getSemestersRef().document(docId), s);
                added++;
            }
        }

        if (added == 0) {
            UIUtils.showAlert(this, "Semesters Complete", "All 8 semesters (1st–8th) already exist in Firestore!");
            return;
        }

        final int finalAdded = added;
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    UIUtils.showAlert(this, "Semesters Configured", "Successfully provisioned " + finalAdded + " missing semester records in Firestore.");
                    loadSemesters();
                })
                .addOnFailureListener(e -> UIUtils.showAlert(this, "Setup Error", "Failed to setup semesters: " + e.getMessage()));
    }

    // ============================================================
    // DELETE SEMESTER
    // ============================================================

    private void confirmDeleteSemester(
            Semester semester
    ) {

        String name =
                semester.getSemesterName();

        if (name == null ||
                name.trim().isEmpty()) {

            name =
                    "Semester "
                            + semester.getSemesterNumber();
        }

        new AlertDialog.Builder(this)
                .setTitle("Delete Semester?")
                .setMessage(
                        "Are you sure you want to delete \""
                                + name
                                + "\"?\n\n"
                                + "This will remove the semester "
                                + "record from Firestore."
                )
                .setNegativeButton(
                        "Cancel",
                        null
                )
                .setPositiveButton(
                        "Delete",
                        (dialog, which) ->
                                deleteSemester(
                                        semester
                                )
                )
                .show();
    }

    private void deleteSemester(
            Semester semester
    ) {

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

        AcademicRepository
                .getInstance()
                .deleteSemester(
                        semesterId,
                        task -> {

                            if (task.isSuccessful()) {

                                UIUtils.showToast(
                                        this,
                                        "Semester deleted successfully."
                                );

                                loadSemesters();

                            } else {

                                String error =
                                        task.getException() != null
                                                ? task.getException()
                                                .getMessage()
                                                : "Unable to delete semester.";

                                UIUtils.showToast(
                                        this,
                                        "Delete failed: " + error
                                );
                            }
                        }
                );
    }

    // ============================================================
    // ADD SEMESTER
    // ============================================================

    private void showAddSemesterDialog() {
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

        builder.setTitle("Add Semester");

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

        Spinner spinnerDepartment =
                new Spinner(this);

        layout.addView(
                spinnerDepartment
        );

        EditText etName =
                new EditText(this);

        etName.setHint(
                "Semester Name (e.g. 6th Semester)"
        );

        layout.addView(etName);

        EditText etNum =
                new EditText(this);

        etNum.setHint(
                "Semester Number (e.g. 6)"
        );

        etNum.setInputType(
                android.text.InputType.TYPE_CLASS_NUMBER
        );

        layout.addView(etNum);

        EditText etAcademicYear =
                new EditText(this);

        etAcademicYear.setHint(
                "Academic Year (e.g. 2026-27)"
        );

        layout.addView(etAcademicYear);

        builder.setView(layout);

        loadDepartmentsIntoSpinner(
                spinnerDepartment
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

                                int departmentPosition =
                                        spinnerDepartment
                                                .getSelectedItemPosition();

                                if (departmentPosition < 0 ||
                                        departmentPosition >=
                                                departmentList.size()) {

                                    UIUtils.showToast(
                                            this,
                                            "Please select a department."
                                    );

                                    return;
                                }

                                Department selectedDepartment =
                                        departmentList.get(
                                                departmentPosition
                                        );

                                String name =
                                        etName.getText()
                                                .toString()
                                                .trim();

                                if (name.isEmpty()) {

                                    etName.setError(
                                            "Semester name is required"
                                    );

                                    return;
                                }

                                String numStr =
                                        etNum.getText()
                                                .toString()
                                                .trim();

                                if (numStr.isEmpty()) {

                                    etNum.setError(
                                            "Semester number is required"
                                    );

                                    return;
                                }

                                int num;

                                try {

                                    num =
                                            Integer.parseInt(
                                                    numStr
                                            );

                                } catch (Exception e) {

                                    etNum.setError(
                                            "Enter a valid number"
                                    );

                                    return;
                                }

                                if (num < 1 || num > 12) {

                                    etNum.setError(
                                            "Semester number must be between 1 and 12"
                                    );

                                    return;
                                }

                                String academicYear =
                                        etAcademicYear
                                                .getText()
                                                .toString()
                                                .trim();

                                if (academicYear.isEmpty()) {

                                    etAcademicYear.setError(
                                            "Academic year is required"
                                    );

                                    return;
                                }

                                String departmentId =
                                        selectedDepartment.getId();

                                if (departmentId == null ||
                                        departmentId.trim().isEmpty()) {

                                    UIUtils.showToast(
                                            this,
                                            "Selected department has no ID."
                                    );

                                    return;
                                }

                                String collegeId =
                                        currentUser.getCollegeId();

                                String semesterId =
                                        "SEM_"
                                                + System.currentTimeMillis();

                                Semester semester =
                                        new Semester(
                                                semesterId,
                                                name,
                                                num
                                        );

                                semester.setCollegeId(
                                        collegeId
                                );

                                semester.setDepartmentId(
                                        departmentId
                                );

                                semester.setAcademicYear(
                                        academicYear
                                );

                                AcademicRepository
                                        .getInstance()
                                        .saveSemester(
                                                semester,
                                                task -> {

                                                    if (task.isSuccessful()) {

                                                        UIUtils.showToast(
                                                                SemesterActivity.this,
                                                                "Semester added successfully."
                                                        );

                                                        dialog.dismiss();

                                                        loadSemesters();

                                                    } else {

                                                        String error =
                                                                task.getException() != null
                                                                        ? task.getException()
                                                                        .getMessage()
                                                                        : "Unable to save semester.";

                                                        UIUtils.showToast(
                                                                SemesterActivity.this,
                                                                "Save failed: " + error
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

    private void loadDepartmentsIntoSpinner(
            Spinner spinnerDepartment
    ) {

        departmentList.clear();

        String collegeId =
                currentUser.getCollegeId();

        AcademicRepository
                .getInstance()
                .getDepartments(
                        collegeId,
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

                            setDepartmentSpinner(
                                    spinnerDepartment
                            );

                            if (departmentList.isEmpty()) {

                                UIUtils.showToast(
                                        SemesterActivity.this,
                                        "No departments found. Create a department first."
                                );
                            }
                        }
                );
    }

    private void setDepartmentSpinner(
            Spinner spinnerDepartment
    ) {

        List<String> departmentNames =
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

            departmentNames.add(
                    name
            );
        }

        if (departmentNames.isEmpty()) {

            departmentNames.add(
                    "No departments available"
            );
        }

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        departmentNames
                );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerDepartment.setAdapter(
                adapter
        );

        spinnerDepartment.setEnabled(
                !departmentList.isEmpty()
        );
    }
}