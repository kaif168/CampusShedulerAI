package com.campusscheduler.ai.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.campusscheduler.ai.R;
import com.campusscheduler.ai.adapters.GenericAdapter;
import com.campusscheduler.ai.models.Department;
import com.campusscheduler.ai.models.Semester;
import com.campusscheduler.ai.models.Subject;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.utils.FirebaseHelper;
import com.campusscheduler.ai.utils.LocalAuthManager;
import com.campusscheduler.ai.utils.UIUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SubjectActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private Button btnAddSubjectHeader;
    private FloatingActionButton fabAddSubject;

    private EditText etSearchSubject;
    private Spinner spDepartmentFilter;
    private Spinner spSemesterFilter;

    private TextView tvSubjectCount;
    private TextView tvEmptyState;
    private RecyclerView rvSubjects;

    private GenericAdapter<Subject> adapter;

    private final List<Subject> allSubjectsList = new ArrayList<>();
    private final List<Subject> filteredSubjectsList = new ArrayList<>();
    private final List<Department> departmentList = new ArrayList<>();

    private User currentUser;
    private String selectedDeptFilter = "All Departments";
    private int selectedSemFilter = 0; // 0 = All
    private String searchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subject);

        currentUser = LocalAuthManager.getCurrentLocalUser(this);

        initializeViews();
        setupRecyclerView();
        setupListeners();

        loadDepartments();
        loadSubjects();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        btnAddSubjectHeader = findViewById(R.id.btnAddSubjectHeader);
        fabAddSubject = findViewById(R.id.fabAddSubject);

        etSearchSubject = findViewById(R.id.etSearchSubject);
        spDepartmentFilter = findViewById(R.id.spDepartmentFilter);
        spSemesterFilter = findViewById(R.id.spSemesterFilter);

        tvSubjectCount = findViewById(R.id.tvSubjectCount);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        rvSubjects = findViewById(R.id.rvSubjects);

        setupSemesterFilterSpinner();
    }

    private void setupSemesterFilterSpinner() {
        List<String> semFilterOptions = new ArrayList<>();
        semFilterOptions.add("All Semesters");
        for (int i = 1; i <= 8; i++) {
            semFilterOptions.add(Semester.formatSemesterOrdinal(i));
        }

        ArrayAdapter<String> semAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, semFilterOptions);
        spSemesterFilter.setAdapter(semAdapter);
    }

    private void setupRecyclerView() {
        rvSubjects.setLayoutManager(new LinearLayoutManager(this));

        adapter = new GenericAdapter<>(filteredSubjectsList, (item, holder) -> {
            TextView tvName = holder.getView(R.id.tvSubjectName);
            TextView tvCodeDept = holder.getView(R.id.tvSubjectCodeDept);
            TextView tvStatus = holder.getView(R.id.tvSubjectStatus);
            TextView tvSem = holder.getView(R.id.tvSubjectSem);
            TextView tvType = holder.getView(R.id.tvSubjectType);
            TextView tvCredits = holder.getView(R.id.tvSubjectCredits);
            TextView tvHours = holder.getView(R.id.tvSubjectHours);
            ImageView ivIcon = holder.getView(R.id.ivSubjectIcon);

            if (tvName != null) tvName.setText(item.getName() != null ? item.getName() : "Subject");

            String code = item.getCode() != null ? item.getCode() : "SUB";
            String dept = item.getDepartmentName() != null ? item.getDepartmentName() : "Computer Science";
            if (tvCodeDept != null) tvCodeDept.setText(code + " • " + dept);

            boolean active = item.isActive();
            if (tvStatus != null) {
                tvStatus.setText(active ? "ACTIVE" : "INACTIVE");
                tvStatus.setTextColor(active ? 0xFF2E7D32 : 0xFFC62828);
                tvStatus.setBackgroundColor(active ? 0xFFE8F5E9 : 0xFFFFEBEE);
            }

            int sem = item.getSemester();
            if (tvSem != null) tvSem.setText(sem > 0 ? (sem + "th Sem") : "Curriculum");

            boolean isLab = item.isLab() || "Lab".equalsIgnoreCase(item.getType());
            if (tvType != null) {
                tvType.setText(isLab ? "Laboratory" : (item.getType() != null ? item.getType() : "Theory"));
            }

            if (ivIcon != null) {
                ivIcon.setImageResource(isLab ? android.R.drawable.ic_menu_crop : android.R.drawable.ic_menu_agenda);
            }

            if (tvCredits != null) tvCredits.setText(item.getCredits() + " Credits");
            if (tvHours != null) tvHours.setText(item.getWeeklyHoursRequired() + " hrs/wk");

            View btnEdit = holder.getView(R.id.btnEditSubject);
            if (btnEdit != null) {
                btnEdit.setOnClickListener(v -> showAddEditSubjectDialog(item));
            }

            View btnDelete = holder.getView(R.id.btnDeleteSubject);
            if (btnDelete != null) {
                btnDelete.setOnClickListener(v -> confirmSafeDeleteSubject(item));
            }

            holder.itemView.setOnClickListener(v -> showSubjectDetailsDialog(item));
        }, R.layout.item_subject);

        rvSubjects.setAdapter(adapter);
    }

    private void setupListeners() {
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        if (btnAddSubjectHeader != null) btnAddSubjectHeader.setOnClickListener(v -> showAddEditSubjectDialog(null));
        if (fabAddSubject != null) fabAddSubject.setOnClickListener(v -> showAddEditSubjectDialog(null));

        if (etSearchSubject != null) {
            etSearchSubject.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    searchQuery = s.toString().trim().toLowerCase(Locale.US);
                    applyFilters();
                }
                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        if (spDepartmentFilter != null) {
            spDepartmentFilter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    if (position == 0) {
                        selectedDeptFilter = "All Departments";
                    } else if (position - 1 < departmentList.size()) {
                        selectedDeptFilter = departmentList.get(position - 1).getId();
                    }
                    applyFilters();
                }
                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });
        }

        if (spSemesterFilter != null) {
            spSemesterFilter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    selectedSemFilter = position; // 0 = All, 1 = 1st Sem, etc.
                    applyFilters();
                }
                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });
        }
    }

    private void loadDepartments() {
        String collegeId = currentUser != null && currentUser.getCollegeId() != null
                ? currentUser.getCollegeId() : "HKBK_COLLEGE";

        FirebaseHelper.getDepartmentsRef().whereEqualTo("collegeId", collegeId).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    departmentList.clear();
                    List<String> deptNames = new ArrayList<>();
                    deptNames.add("All Departments");

                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            Department d = doc.toObject(Department.class);
                            if (d != null) {
                                if (d.getId() == null) d.setId(doc.getId());
                                departmentList.add(d);
                                deptNames.add(d.getName() != null ? d.getName() : "Department");
                            }
                        }
                    } else {
                        // Fallback default CSE department if college not yet seeded
                        Department cse = new Department("DEPT_CSE", collegeId, "Computer Science & Engineering", "CSE", "HOD CSE");
                        departmentList.add(cse);
                        deptNames.add(cse.getName());
                    }

                    ArrayAdapter<String> deptAdapter = new ArrayAdapter<>(SubjectActivity.this,
                            android.R.layout.simple_spinner_dropdown_item, deptNames);
                    spDepartmentFilter.setAdapter(deptAdapter);
                });
    }

    private void loadSubjects() {
        String collegeId = currentUser != null && currentUser.getCollegeId() != null
                ? currentUser.getCollegeId() : "HKBK_COLLEGE";

        if (tvSubjectCount != null) {
            tvSubjectCount.setText("Loading subjects from Cloud Firestore...");
        }

        FirebaseHelper.getSubjectsRef().whereEqualTo("collegeId", collegeId).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allSubjectsList.clear();
                    if (queryDocumentSnapshots != null) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            Subject s = doc.toObject(Subject.class);
                            if (s != null) {
                                if (s.getId() == null) s.setId(doc.getId());
                                allSubjectsList.add(s);
                            }
                        }
                    }
                    applyFilters();
                })
                .addOnFailureListener(e -> {
                    // Fallback broad query if collegeId scoping was unassigned
                    FirebaseHelper.getSubjectsRef().get().addOnSuccessListener(broadSnaps -> {
                        allSubjectsList.clear();
                        if (broadSnaps != null) {
                            for (DocumentSnapshot doc : broadSnaps) {
                                Subject s = doc.toObject(Subject.class);
                                if (s != null) {
                                    if (s.getId() == null) s.setId(doc.getId());
                                    allSubjectsList.add(s);
                                }
                            }
                        }
                        applyFilters();
                    }).addOnFailureListener(err2 -> {
                        if (tvSubjectCount != null) tvSubjectCount.setText("Failed to load subjects.");
                        UIUtils.showToast(SubjectActivity.this, "Error loading subjects: " + e.getMessage());
                    });
                });
    }

    private void applyFilters() {
        filteredSubjectsList.clear();

        for (Subject s : allSubjectsList) {
            if (s == null) continue;

            // 1. Department filter
            if (!"All Departments".equalsIgnoreCase(selectedDeptFilter)) {
                if (s.getDepartmentId() != null && !s.getDepartmentId().equalsIgnoreCase(selectedDeptFilter)) {
                    continue;
                }
            }

            // 2. Semester filter
            if (selectedSemFilter > 0) {
                if (s.getSemester() != selectedSemFilter) {
                    continue;
                }
            }

            // 3. Search query
            if (!searchQuery.isEmpty()) {
                String name = s.getName() != null ? s.getName().toLowerCase(Locale.US) : "";
                String code = s.getCode() != null ? s.getCode().toLowerCase(Locale.US) : "";
                if (!name.contains(searchQuery) && !code.contains(searchQuery)) {
                    continue;
                }
            }

            filteredSubjectsList.add(s);
        }

        if (tvSubjectCount != null) {
            tvSubjectCount.setText("Showing " + filteredSubjectsList.size() + " of " + allSubjectsList.size() + " subjects");
        }

        if (tvEmptyState != null) {
            tvEmptyState.setVisibility(filteredSubjectsList.isEmpty() ? View.VISIBLE : View.GONE);
        }

        adapter.notifyDataSetChanged();
    }

    private void showAddEditSubjectDialog(Subject existing) {
        boolean isEdit = existing != null;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(isEdit ? "Edit Academic Subject" : "Add Academic Subject");

        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = dpToPx(16);
        layout.setPadding(pad, dpToPx(8), pad, dpToPx(8));

        // 1. Subject Name
        layout.addView(createLabel("Subject Name: *"));
        EditText etName = new EditText(this);
        etName.setHint("e.g. Cloud Computing");
        if (isEdit && existing.getName() != null) etName.setText(existing.getName());
        layout.addView(etName);

        // 2. Subject Code
        layout.addView(createLabel("Subject Code: *"));
        EditText etCode = new EditText(this);
        etCode.setHint("e.g. 21CS72");
        if (isEdit && existing.getCode() != null) etCode.setText(existing.getCode());
        layout.addView(etCode);

        // 3. Department Spinner
        layout.addView(createLabel("Department: *"));
        Spinner spDept = new Spinner(this);
        List<String> dNames = new ArrayList<>();
        int selectedDeptPos = 0;
        for (int i = 0; i < departmentList.size(); i++) {
            Department d = departmentList.get(i);
            dNames.add(d.getName() != null ? d.getName() : "Department");
            if (isEdit && d.getId() != null && d.getId().equalsIgnoreCase(existing.getDepartmentId())) {
                selectedDeptPos = i;
            }
        }
        if (dNames.isEmpty()) dNames.add("Computer Science & Engineering");
        spDept.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, dNames));
        spDept.setSelection(selectedDeptPos);
        layout.addView(spDept);

        // 4. Semester Spinner
        layout.addView(createLabel("Applicable Semester: *"));
        Spinner spSem = new Spinner(this);
        List<String> semNames = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            semNames.add(Semester.formatSemesterOrdinal(i));
        }
        spSem.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, semNames));
        if (isEdit && existing.getSemester() >= 1 && existing.getSemester() <= 8) {
            spSem.setSelection(existing.getSemester() - 1);
        } else {
            spSem.setSelection(6); // Default 7th Sem
        }
        layout.addView(spSem);

        // 5. Category / Type Spinner
        layout.addView(createLabel("Subject Category / Type:"));
        Spinner spType = new Spinner(this);
        String[] types = {"Theory", "Laboratory", "Integrated", "Professional Elective", "Open Elective"};
        spType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, types));
        if (isEdit) {
            boolean wasLab = existing.isLab() || "Lab".equalsIgnoreCase(existing.getType());
            spType.setSelection(wasLab ? 1 : 0);
        }
        layout.addView(spType);

        // 6. Credits
        layout.addView(createLabel("Credits (e.g. 3 or 4):"));
        EditText etCredits = new EditText(this);
        etCredits.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etCredits.setText(isEdit ? String.valueOf(existing.getCredits()) : "4");
        layout.addView(etCredits);

        // 7. Weekly Hours
        layout.addView(createLabel("Required Weekly Hours / Periods:"));
        EditText etHours = new EditText(this);
        etHours.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etHours.setText(isEdit ? String.valueOf(existing.getWeeklyHoursRequired()) : "4");
        layout.addView(etHours);

        // 8. Laboratory Checkbox
        CheckBox cbRequiresLab = new CheckBox(this);
        cbRequiresLab.setText("Requires Laboratory / Practical Equipment");
        if (isEdit) {
            cbRequiresLab.setChecked(existing.isLab() || "Lab".equalsIgnoreCase(existing.getType()));
        }
        layout.addView(cbRequiresLab);

        // 9. Status Spinner
        layout.addView(createLabel("Active Status:"));
        Spinner spStatus = new Spinner(this);
        String[] statuses = {"ACTIVE", "INACTIVE"};
        spStatus.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, statuses));
        if (isEdit && "INACTIVE".equalsIgnoreCase(existing.getStatus())) {
            spStatus.setSelection(1);
        }
        layout.addView(spStatus);

        scrollView.addView(layout);
        builder.setView(scrollView);

        builder.setPositiveButton(isEdit ? "Update Subject" : "Save Subject", null);
        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        // Custom positive button listener to prevent dismissal on validation failure
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String code = etCode.getText().toString().trim().toUpperCase(Locale.US);
            String credStr = etCredits.getText().toString().trim();
            String hourStr = etHours.getText().toString().trim();

            if (name.isEmpty()) {
                etName.setError("Subject name is required");
                return;
            }
            if (code.isEmpty()) {
                etCode.setError("Subject code is required");
                return;
            }

            int credits = 4;
            try { credits = Integer.parseInt(credStr); } catch (Exception ignored) {}

            int hours = 4;
            try { hours = Integer.parseInt(hourStr); } catch (Exception ignored) {}

            int semNum = spSem.getSelectedItemPosition() + 1;
            String category = types[spType.getSelectedItemPosition()];
            boolean isLab = cbRequiresLab.isChecked() || "Laboratory".equalsIgnoreCase(category);
            String status = statuses[spStatus.getSelectedItemPosition()];

            Department dept = !departmentList.isEmpty() && spDept.getSelectedItemPosition() < departmentList.size()
                    ? departmentList.get(spDept.getSelectedItemPosition()) : null;

            String collegeId = currentUser != null && currentUser.getCollegeId() != null
                    ? currentUser.getCollegeId() : "HKBK_COLLEGE";
            String deptId = dept != null ? dept.getId() : "DEPT_CSE";
            String deptName = dept != null ? dept.getName() : "Computer Science & Engineering";

            String cleanCode = code.replaceAll("[^a-zA-Z0-9]", "_");
            String docId = isEdit ? existing.getId() : ("SUB_" + collegeId + "_" + deptId + "_S" + semNum + "_" + cleanCode);

            Subject sub = new Subject();
            sub.setId(docId);
            sub.setCollegeId(collegeId);
            sub.setDepartmentId(deptId);
            sub.setDepartmentName(deptName);
            sub.setCode(code);
            sub.setSubjectCode(code);
            sub.setName(name);
            sub.setSubjectName(name);
            sub.setSemester(semNum);
            sub.setCredits(credits);
            sub.setWeeklyHoursRequired(hours);
            sub.setWeeklyPeriods(hours);
            sub.setLab(isLab);
            sub.setType(isLab ? "Lab" : category);
            sub.setCategory(category);
            sub.setStatus(status);

            // Persist to Cloud Firestore
            FirebaseHelper.getSubjectsRef().document(docId).set(sub, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        UIUtils.showToast(SubjectActivity.this, "Subject saved successfully: " + name);
                        dialog.dismiss();
                        loadSubjects();
                    })
                    .addOnFailureListener(e -> {
                        UIUtils.showToast(SubjectActivity.this, "Failed to save subject: " + e.getMessage());
                    });
        });
    }

    private void showSubjectDetailsDialog(Subject subject) {
        if (subject == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(subject.getName() != null ? subject.getName() : "Subject Details");

        StringBuilder sb = new StringBuilder();
        sb.append("Subject Code: ").append(subject.getCode()).append("\n");
        sb.append("Department: ").append(subject.getDepartmentName() != null ? subject.getDepartmentName() : subject.getDepartmentId()).append("\n");
        sb.append("Applicable Semester: ").append(Semester.formatSemesterOrdinal(subject.getSemester())).append("\n");
        sb.append("Classification: ").append(subject.isLab() ? "Laboratory / Practical" : subject.getType()).append("\n");
        sb.append("Credits: ").append(subject.getCredits()).append("\n");
        sb.append("Weekly Teaching Hours: ").append(subject.getWeeklyHoursRequired()).append(" hours/week\n");
        sb.append("Status: ").append(subject.getStatus()).append("\n");
        sb.append("Firestore Doc ID: ").append(subject.getId());

        builder.setMessage(sb.toString());
        builder.setPositiveButton("Edit", (dialog, which) -> showAddEditSubjectDialog(subject));
        builder.setNegativeButton("Close", null);
        builder.show();
    }

    private void confirmSafeDeleteSubject(Subject subject) {
        if (subject == null) return;

        // Check if subject is actively referenced in classAssignments
        FirebaseHelper.getClassAssignmentsRef()
                .whereEqualTo("subjectId", subject.getId())
                .get()
                .addOnSuccessListener(snaps -> {
                    int count = snaps != null ? snaps.size() : 0;
                    if (count > 0) {
                        // Protected reference: suggest deactivation
                        new AlertDialog.Builder(SubjectActivity.this)
                                .setTitle("Protected Academic Record")
                                .setMessage("Subject '" + subject.getName() + "' is currently referenced by " + count
                                        + " active Class Assignment(s) or published schedules.\n\n"
                                        + "To preserve timetable integrity, hard-deleting this subject is blocked. Would you like to DEACTIVATE it instead?")
                                .setPositiveButton("Deactivate", (d, w) -> {
                                    subject.setStatus("INACTIVE");
                                    FirebaseHelper.getSubjectsRef().document(subject.getId()).set(subject, SetOptions.merge())
                                            .addOnSuccessListener(aVoid -> {
                                                UIUtils.showToast(SubjectActivity.this, "Subject deactivated.");
                                                loadSubjects();
                                            });
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    } else {
                        // Safe to delete
                        new AlertDialog.Builder(SubjectActivity.this)
                                .setTitle("Confirm Subject Deletion")
                                .setMessage("Are you sure you want to permanently delete '" + subject.getName() + "' (" + subject.getCode() + ")?")
                                .setPositiveButton("Delete", (d, w) -> {
                                    FirebaseHelper.getSubjectsRef().document(subject.getId()).delete()
                                            .addOnSuccessListener(aVoid -> {
                                                UIUtils.showToast(SubjectActivity.this, "Subject deleted.");
                                                loadSubjects();
                                            })
                                            .addOnFailureListener(e -> {
                                                UIUtils.showToast(SubjectActivity.this, "Failed to delete: " + e.getMessage());
                                            });
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    }
                })
                .addOnFailureListener(e -> {
                    UIUtils.showToast(SubjectActivity.this, "Unable to verify references: " + e.getMessage());
                });
    }

    private TextView createLabel(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(13);
        tv.setTextColor(0xFF37474F);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setPadding(0, dpToPx(8), 0, dpToPx(4));
        return tv;
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }
}
