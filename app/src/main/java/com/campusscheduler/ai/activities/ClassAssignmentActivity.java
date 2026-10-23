package com.campusscheduler.ai.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.campusscheduler.ai.R;
import com.campusscheduler.ai.adapters.GenericAdapter;
import com.campusscheduler.ai.engine.TeacherRecommendationEngine;
import com.campusscheduler.ai.models.ClassAssignment;
import com.campusscheduler.ai.models.Classroom;
import com.campusscheduler.ai.models.Department;
import com.campusscheduler.ai.models.Laboratory;
import com.campusscheduler.ai.models.Section;
import com.campusscheduler.ai.models.Semester;
import com.campusscheduler.ai.models.Subject;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.utils.FirebaseHelper;
import com.campusscheduler.ai.utils.LocalAuthManager;
import com.campusscheduler.ai.utils.UIUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ClassAssignmentActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private FloatingActionButton fabAddAssignment;
    private Spinner spFilterDepartment, spFilterSemester;
    private TextView tvEmptyState;
    private RecyclerView rvClassAssignments;

    private GenericAdapter<ClassAssignment> adapter;
    private final List<ClassAssignment> masterAssignmentList = new ArrayList<>();
    private final List<ClassAssignment> displayedAssignmentList = new ArrayList<>();

    // Master data
    private final List<Department> departmentList = new ArrayList<>();
    private final List<Semester> semesterList = new ArrayList<>();
    private final List<Section> sectionList = new ArrayList<>();
    private final List<Subject> subjectList = new ArrayList<>();
    private final List<User> teacherList = new ArrayList<>();
    private final List<Classroom> classroomList = new ArrayList<>();
    private final List<Laboratory> laboratoryList = new ArrayList<>();

    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class_assignment);

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
        fabAddAssignment = findViewById(R.id.fabAddAssignment);
        spFilterDepartment = findViewById(R.id.spFilterDepartment);
        spFilterSemester = findViewById(R.id.spFilterSemester);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        rvClassAssignments = findViewById(R.id.rvClassAssignments);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (fabAddAssignment != null) {
            fabAddAssignment.setOnClickListener(v -> showAddEditAssignmentDialog(null));
        }

        setupRecyclerView();
        loadAllMasterData();
    }

    private void setupRecyclerView() {
        if (rvClassAssignments == null) return;

        rvClassAssignments.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GenericAdapter<>(displayedAssignmentList, (item, holder) -> {
            String subTitle = item.getSubjectName() != null ? item.getSubjectName() : "Subject";
            if (item.getSubjectCode() != null && !item.getSubjectCode().trim().isEmpty()) {
                subTitle += " (" + item.getSubjectCode() + ")";
            }
            holder.setText(R.id.tvSubjectName, subTitle);

            String badge = item.getClassType() != null ? item.getClassType() : "Theory";
            holder.setText(R.id.tvClassTypeBadge, badge);

            String teacher = item.getTeacherName() != null ? item.getTeacherName() : "Assigned Faculty";
            holder.setText(R.id.tvTeacherName, "Faculty: " + teacher);

            String contextText = "Sem: " + item.getSemester() + "th • Sec: " + (item.getSectionName() != null ? item.getSectionName() : "A")
                    + " • Room: " + (item.getRoomName() != null ? item.getRoomName() : "Unassigned")
                    + " • " + item.getWeeklyHours() + " hrs/wk";
            if (item.getAcademicYear() != null && !item.getAcademicYear().trim().isEmpty()) {
                contextText += " • AY: " + item.getAcademicYear();
            }
            holder.setText(R.id.tvContextDetails, contextText);

            View btnEdit = holder.getView(R.id.btnEditAssignment);
            if (btnEdit != null) {
                btnEdit.setOnClickListener(v -> showAddEditAssignmentDialog(item));
            }

            View btnDelete = holder.getView(R.id.btnDeleteAssignment);
            if (btnDelete != null) {
                btnDelete.setOnClickListener(v -> confirmDeleteAssignment(item));
            }
        }, R.layout.item_class_assignment);

        rvClassAssignments.setAdapter(adapter);
    }

    private void loadAllMasterData() {
        String collegeId = (currentUser != null && currentUser.getCollegeId() != null && !currentUser.getCollegeId().trim().isEmpty())
                ? currentUser.getCollegeId() : LocalAuthManager.getSafeCollegeId(this, getIntent());

        // 1. Departments
        FirebaseHelper.getDepartmentsRef().whereEqualTo("collegeId", collegeId).get()
                .addOnSuccessListener(snaps -> {
                    departmentList.clear();
                    if (snaps != null && !snaps.isEmpty()) {
                        for (DocumentSnapshot doc : snaps) {
                            Department d = doc.toObject(Department.class);
                            if (d != null) {
                                if (d.getId() == null) d.setId(doc.getId());
                                departmentList.add(d);
                            }
                        }
                    }
                    if (departmentList.isEmpty()) {
                        Department cse = new Department("DEPT_CSE", collegeId, "Computer Science & Engineering", "CSE", "HOD CSE");
                        departmentList.add(cse);
                    }
                    setupFilterSpinners();
                });

        // 2. Semesters
        FirebaseHelper.getSemestersRef().whereEqualTo("collegeId", collegeId).get()
                .addOnSuccessListener(snaps -> {
                    semesterList.clear();
                    if (snaps != null) {
                        for (DocumentSnapshot doc : snaps) {
                            Semester s = doc.toObject(Semester.class);
                            if (s != null) {
                                if (s.getId() == null) s.setId(doc.getId());
                                semesterList.add(s);
                            }
                        }
                    }
                });

        // 3. Sections
        FirebaseHelper.getSectionsRef().whereEqualTo("collegeId", collegeId).get()
                .addOnSuccessListener(snaps -> {
                    sectionList.clear();
                    if (snaps != null) {
                        for (DocumentSnapshot doc : snaps) {
                            Section s = doc.toObject(Section.class);
                            if (s != null) {
                                if (s.getId() == null) s.setId(doc.getId());
                                sectionList.add(s);
                            }
                        }
                    }
                });

        // 4. Subjects
        FirebaseHelper.getSubjectsRef().whereEqualTo("collegeId", collegeId).get()
                .addOnSuccessListener(snaps -> {
                    subjectList.clear();
                    if (snaps != null) {
                        for (DocumentSnapshot doc : snaps) {
                            Subject s = doc.toObject(Subject.class);
                            if (s != null) {
                                if (s.getId() == null) s.setId(doc.getId());
                                subjectList.add(s);
                            }
                        }
                    }
                });

        // 5. Teachers - Load from faculty collection (including 38 imported faculty) and merge users
        FirebaseHelper.getFacultyRef().whereEqualTo("collegeId", collegeId).get()
                .addOnSuccessListener(snaps -> {
                    teacherList.clear();
                    if (snaps != null) {
                        for (DocumentSnapshot doc : snaps) {
                            com.campusscheduler.ai.models.Faculty f = doc.toObject(com.campusscheduler.ai.models.Faculty.class);
                            if (f != null && f.getName() != null && !f.getName().trim().isEmpty()) {
                                User u = new User();
                                u.setUid(f.getId() != null ? f.getId() : doc.getId());
                                u.setName(f.getName());
                                u.setEmail(f.getEmail());
                                u.setRole(User.ROLE_TEACHER);
                                u.setDepartmentId(f.getDepartmentId());
                                u.setDesignation(f.getDesignation());
                                u.setSpecialization(f.getSpecialization());
                                u.setCollegeId(f.getCollegeId());
                                teacherList.add(u);
                            }
                        }
                    }

                    // Also check users collection to ensure any registered faculty accounts are included
                    FirebaseHelper.getUsersRef().whereEqualTo("collegeId", collegeId).get()
                            .addOnSuccessListener(userSnaps -> {
                                if (userSnaps != null) {
                                    for (DocumentSnapshot doc : userSnaps) {
                                        User u = doc.toObject(User.class);
                                        if (u != null) {
                                            String r = u.getRole();
                                            if (r != null && (r.equalsIgnoreCase(User.ROLE_TEACHER) || r.equalsIgnoreCase("teacher"))) {
                                                boolean exists = false;
                                                for (User t : teacherList) {
                                                    if (t.getUid() != null && t.getUid().equals(doc.getId())) {
                                                        exists = true;
                                                        break;
                                                    }
                                                }
                                                if (!exists) {
                                                    if (u.getUid() == null) u.setUid(doc.getId());
                                                    teacherList.add(u);
                                                }
                                            }
                                        }
                                    }
                                }
                            });
                });

        // 6. Classrooms & Labs
        FirebaseHelper.getClassroomsRef().whereEqualTo("collegeId", collegeId).get()
                .addOnSuccessListener(snaps -> {
                    classroomList.clear();
                    if (snaps != null) {
                        for (DocumentSnapshot doc : snaps) {
                            Classroom cr = doc.toObject(Classroom.class);
                            if (cr != null) classroomList.add(cr);
                        }
                    }
                });

        FirebaseHelper.getLaboratoriesRef().whereEqualTo("collegeId", collegeId).get()
                .addOnSuccessListener(snaps -> {
                    laboratoryList.clear();
                    if (snaps != null) {
                        for (DocumentSnapshot doc : snaps) {
                            Laboratory lab = doc.toObject(Laboratory.class);
                            if (lab != null) laboratoryList.add(lab);
                        }
                    }
                });

        // 7. Load existing Class Assignments
        loadAssignments();
    }

    private void loadAssignments() {
        String collegeId = currentUser != null && currentUser.getCollegeId() != null ?
                currentUser.getCollegeId() : LocalAuthManager.getSafeCollegeId(this, getIntent());
        if (currentUser != null && (currentUser.getCollegeId() == null || currentUser.getCollegeId().trim().isEmpty())) {
            currentUser.setCollegeId(collegeId);
        }

        FirebaseHelper.getClassAssignmentsRef().whereEqualTo("collegeId", collegeId).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    masterAssignmentList.clear();
                    if (queryDocumentSnapshots != null) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            ClassAssignment ca = doc.toObject(ClassAssignment.class);
                            if (ca != null) {
                                if (ca.getId() == null) ca.setId(doc.getId());
                                masterAssignmentList.add(ca);
                            }
                        }
                    }
                    applyFilter();
                })
                .addOnFailureListener(e -> UIUtils.showToast(this, "Failed to load assignments: " + e.getMessage()));
    }

    private void setupFilterSpinners() {
        // Department Filter
        List<String> deptNames = new ArrayList<>();
        deptNames.add("All Departments");
        for (Department d : departmentList) {
            deptNames.add(d.getName() != null ? d.getName() : (d.getCode() != null ? d.getCode() : "Dept"));
        }
        ArrayAdapter<String> deptAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, deptNames);
        deptAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spFilterDepartment.setAdapter(deptAdapter);

        // Semester Filter
        List<String> semNames = new ArrayList<>();
        semNames.add("All Semesters");
        for (int i = 1; i <= 8; i++) {
            semNames.add(i + "th Semester");
        }
        ArrayAdapter<String> semAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, semNames);
        semAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spFilterSemester.setAdapter(semAdapter);

        AdapterView.OnItemSelectedListener filterListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilter();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        spFilterDepartment.setOnItemSelectedListener(filterListener);
        spFilterSemester.setOnItemSelectedListener(filterListener);
    }

    private void applyFilter() {
        displayedAssignmentList.clear();

        int deptPos = spFilterDepartment.getSelectedItemPosition();
        int semPos = spFilterSemester.getSelectedItemPosition();

        Department filterDept = (deptPos > 0 && deptPos <= departmentList.size()) ? departmentList.get(deptPos - 1) : null;
        int filterSem = semPos > 0 ? semPos : -1;

        for (ClassAssignment ca : masterAssignmentList) {
            if (filterDept != null && filterDept.getId() != null) {
                if (ca.getDepartmentId() != null && !ca.getDepartmentId().equals(filterDept.getId())) {
                    continue;
                }
            }
            if (filterSem > 0) {
                if (ca.getSemester() != filterSem) {
                    continue;
                }
            }
            displayedAssignmentList.add(ca);
        }

        if (adapter != null) adapter.notifyDataSetChanged();

        if (displayedAssignmentList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
        }
    }

    private void showAddEditAssignmentDialog(ClassAssignment existing) {
        boolean isEdit = existing != null;

        if (subjectList.isEmpty()) {
            UIUtils.showAlert(this, "Subjects Required", "No academic subjects were found in Firestore. Please add subjects in Subject Management first before mapping classes.");
            return;
        }
        if (teacherList.isEmpty()) {
            UIUtils.showAlert(this, "Faculty Required", "No faculty members were found. Please add or import faculty first.");
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(isEdit ? "Edit Class Assignment" : "Map Class Assignment");

        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = dpToPx(16);
        layout.setPadding(pad, dpToPx(8), pad, dpToPx(8));

        // 1. Department Spinner
        layout.addView(createLabel("Department:"));
        Spinner spDept = new Spinner(this);
        List<String> dNames = new ArrayList<>();
        for (Department d : departmentList) {
            dNames.add(d.getName() != null ? d.getName() : "Department");
        }
        if (dNames.isEmpty()) dNames.add("Computer Science & Engineering");
        spDept.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, dNames));
        layout.addView(spDept);

        // 2. Academic Year
        layout.addView(createLabel("Academic Year:"));
        Spinner spYear = new Spinner(this);
        List<String> yearList = new ArrayList<>();
        Set<String> uniqueYears = new LinkedHashSet<>();
        for (Semester s : semesterList) {
            if (s.getAcademicYear() != null && !s.getAcademicYear().trim().isEmpty()) {
                uniqueYears.add(s.getAcademicYear().trim());
            }
        }
        if (uniqueYears.isEmpty()) {
            uniqueYears.add("2026-27");
            uniqueYears.add("2026-2027");
            uniqueYears.add("2025-26");
        }
        yearList.addAll(uniqueYears);
        spYear.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, yearList));
        layout.addView(spYear);

        // 3. Semester Spinner
        layout.addView(createLabel("Semester:"));
        Spinner spSem = new Spinner(this);
        List<String> sNames = new ArrayList<>();
        for (int i = 1; i <= 8; i++) sNames.add(Semester.formatSemesterOrdinal(i));
        spSem.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, sNames));
        layout.addView(spSem);

        // 4. Section Spinner
        layout.addView(createLabel("Section:"));
        Spinner spSec = new Spinner(this);
        List<String> secNames = new ArrayList<>();
        secNames.add("A");
        secNames.add("B");
        secNames.add("C");
        secNames.add("D");
        spSec.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, secNames));
        layout.addView(spSec);

        // 5. Subject Spinner
        layout.addView(createLabel("Subject:"));
        Spinner spSub = new Spinner(this);
        List<String> subNames = new ArrayList<>();
        for (Subject s : subjectList) {
            String title = s.getName() != null ? s.getName() : "Subject";
            if (s.getCode() != null) title += " (" + s.getCode() + ")";
            subNames.add(title);
        }
        spSub.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, subNames));
        layout.addView(spSub);

        // 6. Faculty Spinner
        layout.addView(createLabel("Assigned Faculty:"));
        Spinner spTeacher = new Spinner(this);
        List<String> tNames = new ArrayList<>();
        for (User u : teacherList) {
            String n = u.getName() != null ? u.getName() : u.getEmail();
            tNames.add(n != null ? n : "Faculty Member");
        }
        spTeacher.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, tNames));
        layout.addView(spTeacher);

        // 7. Weekly Teaching Hours
        layout.addView(createLabel("Weekly Hours / Credits:"));
        EditText etHours = new EditText(this);
        etHours.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etHours.setText("4");
        layout.addView(etHours);

        // 8. Class Type
        layout.addView(createLabel("Class Type:"));
        Spinner spClassType = new Spinner(this);
        String[] cTypes = {"Theory", "Lab"};
        spClassType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, cTypes));
        layout.addView(spClassType);

        // 9. Classroom / Lab Room
        layout.addView(createLabel("Assigned / Preferred Room:"));
        Spinner spRoom = new Spinner(this);
        List<String> roomNames = new ArrayList<>();
        for (Classroom cr : classroomList) {
            roomNames.add(cr.getRoomNumber() != null ? cr.getRoomNumber() : "Room");
        }
        for (Laboratory lab : laboratoryList) {
            String lName = lab.getRoomNumber() != null ? lab.getRoomNumber() : (lab.getLabName() != null ? lab.getLabName() : "Lab");
            if (!roomNames.contains(lName)) roomNames.add(lName);
        }
        if (roomNames.isEmpty()) {
            roomNames.add("101");
            roomNames.add("102");
            roomNames.add("103");
            roomNames.add("201");
            roomNames.add("202");
            roomNames.add("203");
        }
        spRoom.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, roomNames));
        layout.addView(spRoom);

        // If editing, pre-fill values
        if (isEdit) {
            // Select semester
            if (existing.getSemester() >= 1 && existing.getSemester() <= 8) {
                spSem.setSelection(existing.getSemester() - 1);
            }
            // Select section
            String curSec = ClassAssignment.normalizeSection(existing.getSectionName());
            for (int i = 0; i < secNames.size(); i++) {
                if (secNames.get(i).equalsIgnoreCase(curSec)) {
                    spSec.setSelection(i);
                    break;
                }
            }
            etHours.setText(String.valueOf(existing.getWeeklyHours() > 0 ? existing.getWeeklyHours() : 4));
            if ("Lab".equalsIgnoreCase(existing.getClassType())) {
                spClassType.setSelection(1);
            }
        }

        // Auto-update hours and class type when subject changes
        spSub.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < subjectList.size()) {
                    Subject s = subjectList.get(position);
                    int hrs = s.getWeeklyHoursRequired() > 0 ? s.getWeeklyHoursRequired() : (s.getCredits() > 0 ? s.getCredits() : 4);
                    etHours.setText(String.valueOf(hrs));
                    if (s.isLab() || "Lab".equalsIgnoreCase(s.getType())) {
                        spClassType.setSelection(1);
                    } else {
                        spClassType.setSelection(0);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        scrollView.addView(layout);
        builder.setView(scrollView);

        builder.setPositiveButton(isEdit ? "Update" : "Save", null);
        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(di -> {
            Button btnPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            btnPositive.setOnClickListener(v -> {
                if (departmentList.isEmpty()) {
                    UIUtils.showToast(this, "No department available.");
                    return;
                }
                if (teacherList.isEmpty()) {
                    UIUtils.showToast(this, "No active faculty found to assign.");
                    return;
                }

                Department dept = departmentList.get(Math.min(spDept.getSelectedItemPosition(), departmentList.size() - 1));
                String academicYear = spYear.getSelectedItem() != null ? spYear.getSelectedItem().toString() : "2026-27";
                int sem = spSem.getSelectedItemPosition() + 1;
                String secName = spSec.getSelectedItem().toString();
                Subject subject = (!subjectList.isEmpty() && spSub.getSelectedItemPosition() < subjectList.size())
                        ? subjectList.get(spSub.getSelectedItemPosition())
                        : null;
                User teacher = teacherList.get(Math.min(spTeacher.getSelectedItemPosition(), teacherList.size() - 1));

                int weeklyHrs = 4;
                try {
                    weeklyHrs = Integer.parseInt(etHours.getText().toString().trim());
                } catch (Exception ignored) {}
                if (weeklyHrs <= 0) weeklyHrs = 4;

                String classType = spClassType.getSelectedItem().toString();
                String room = spRoom.getSelectedItem() != null ? spRoom.getSelectedItem().toString() : "Room 101";

                // Lab room suitability validation
                if ("Lab".equalsIgnoreCase(classType) && ("201".equals(room) || "Room 201".equalsIgnoreCase(room))) {
                    UIUtils.showAlert(this, "Invalid Lab Room",
                            "Room 201 is designated strictly as a lecture classroom without lab equipment. Please select a designated laboratory room (e.g. 202-208, 305-308).");
                    return;
                }

                String subjectId = subject != null ? subject.getId() : "SUB_" + System.currentTimeMillis();
                String subjectName = subject != null ? subject.getName() : "Subject";
                String subjectCode = subject != null ? subject.getCode() : "";

                // Deterministic Document ID to prevent duplicates
                String cleanSub = subjectId.replaceAll("[^a-zA-Z0-9]", "");
                String assignmentDocId = "ASSIGN_" + currentUser.getCollegeId() + "_" + dept.getId() + "_" + sem + "_" + secName + "_" + cleanSub;

                // 1. Prevent duplicate subject assignment to the exact same section
                if (!isEdit) {
                    for (ClassAssignment existingCa : masterAssignmentList) {
                        if (existingCa.getSemester() == sem
                                && secName.equalsIgnoreCase(ClassAssignment.normalizeSection(existingCa.getSectionName()))
                                && subjectId.equalsIgnoreCase(existingCa.getSubjectId())) {
                            UIUtils.showAlert(this, "Duplicate Assignment", "Subject '" + subjectName + "' is already assigned to Section " + secName + " in Semester " + sem + ".");
                            return;
                        }
                    }
                }

                // 2. Check cross-section clash: Is this faculty member already teaching this subject in another section?
                ClassAssignment crossSectionAssignment = null;
                for (ClassAssignment existingCa : masterAssignmentList) {
                    if (isEdit && existing != null && existingCa.getId() != null && existingCa.getId().equals(existing.getId())) {
                        continue;
                    }
                    if (existingCa.getTeacherUid() != null && existingCa.getTeacherUid().equals(teacher.getUid())
                            && subjectId.equalsIgnoreCase(existingCa.getSubjectId())
                            && !secName.equalsIgnoreCase(ClassAssignment.normalizeSection(existingCa.getSectionName()))) {
                        crossSectionAssignment = existingCa;
                        break;
                    }
                }

                // 3. Workload calculation & designation limits
                int currentTeacherLoad = 0;
                for (ClassAssignment caItem : masterAssignmentList) {
                    if (isEdit && existing != null && caItem.getId() != null && caItem.getId().equals(existing.getId())) {
                        continue;
                    }
                    if (caItem.getTeacherUid() != null && caItem.getTeacherUid().equals(teacher.getUid())) {
                        currentTeacherLoad += caItem.getWeeklyHours();
                    }
                }
                int projectedLoad = currentTeacherLoad + weeklyHrs;
                int maxAllowedLoad = 16;
                if ("Professor".equalsIgnoreCase(teacher.getDesignation())) {
                    maxAllowedLoad = 10;
                } else if ("Associate Professor".equalsIgnoreCase(teacher.getDesignation())) {
                    maxAllowedLoad = 14;
                }

                // 4. Find alternative eligible faculty candidates
                List<TeacherRecommendationEngine.RecommendationCandidate> candidates =
                        TeacherRecommendationEngine.recommendTeachersForSubject(subject, teacherList);
                List<String> eligibleOthers = new ArrayList<>();
                for (TeacherRecommendationEngine.RecommendationCandidate cand : candidates) {
                    if (cand.getFaculty() != null && !cand.getFaculty().getUid().equals(teacher.getUid())) {
                        String desig = cand.getFaculty().getDesignation() != null ? " (" + cand.getFaculty().getDesignation() + ")" : "";
                        eligibleOthers.add("• " + cand.getFaculty().getName() + desig + " — " + cand.getRationale());
                        if (eligibleOthers.size() >= 3) break;
                    }
                }

                ClassAssignment ca = isEdit ? existing : new ClassAssignment();
                ca.setId(assignmentDocId);
                ca.setAssignmentId(assignmentDocId);
                ca.setCollegeId(currentUser.getCollegeId());
                ca.setDepartmentId(dept.getId());
                ca.setDepartmentName(dept.getName());
                ca.setAcademicYear(academicYear);
                ca.setSemester(sem);
                ca.setSection(secName);
                ca.setSectionId("SEC_" + dept.getId() + "_" + sem + "_" + secName);
                ca.setSectionName(secName);
                ca.setSubjectId(subjectId);
                ca.setSubjectName(subjectName);
                ca.setSubjectCode(subjectCode);
                ca.setFacultyId(teacher.getUid());
                ca.setTeacherUid(teacher.getUid());
                ca.setTeacherName(teacher.getName() != null ? teacher.getName() : teacher.getEmail());
                ca.setWeeklyHours(weeklyHrs);
                ca.setCredits(weeklyHrs);
                ca.setClassType(classType);
                ca.setRoomName(room);
                ca.setClassroomId("ROOM_" + room);

                // 5. If cross-section allocation or heavy workload detected, prompt with advisory and alternatives
                if (crossSectionAssignment != null || projectedLoad > maxAllowedLoad) {
                    StringBuilder warnSb = new StringBuilder();
                    if (crossSectionAssignment != null) {
                        String otherSec = crossSectionAssignment.getSectionName() != null ? crossSectionAssignment.getSectionName() : crossSectionAssignment.getSection();
                        warnSb.append("⚠️ Cross-Section Subject Allocation Notice:\n");
                        warnSb.append("Faculty '").append(teacher.getName()).append("' is ALREADY assigned to teach '")
                                .append(subjectName).append("' in Section ").append(otherSec).append(".\n\n");
                        warnSb.append("Institutional Policy:\n");
                        warnSb.append("Assigning the same faculty across multiple sections is permitted ONLY IF timetable sessions do not overlap. The Genetic Algorithm & RepairEngine will strictly enforce non-overlapping time slots.\n\n");
                    }
                    if (projectedLoad > maxAllowedLoad) {
                        warnSb.append("⚠️ Workload Advisory:\n");
                        warnSb.append("Total assigned hours for ").append(teacher.getName()).append(" will be ")
                                .append(projectedLoad).append(" hrs/wk (Designation Limit: ").append(maxAllowedLoad).append(" hrs/wk).\n\n");
                    }

                    warnSb.append("Eligible Alternative Faculty Candidates:\n");
                    if (eligibleOthers.isEmpty()) {
                        warnSb.append("• No eligible alternative faculty member found in the database for this subject.\n");
                    } else {
                        for (String candStr : eligibleOthers) {
                            warnSb.append(candStr).append("\n");
                        }
                    }
                    warnSb.append("\nWould you like to proceed with this assignment or choose an alternative faculty member?");

                    new AlertDialog.Builder(this)
                            .setTitle("Faculty Allocation Advisory")
                            .setMessage(warnSb.toString())
                            .setPositiveButton("Proceed & Save", (diag, which) -> {
                                saveAssignmentRecord(ca, assignmentDocId, dialog);
                            })
                            .setNegativeButton("Choose Alternative", null)
                            .show();
                    return;
                }

                saveAssignmentRecord(ca, assignmentDocId, dialog);
            });
        });

        dialog.show();
    }

    private void saveAssignmentRecord(ClassAssignment ca, String docId, AlertDialog parentDialog) {
        FirebaseHelper.getClassAssignmentsRef().document(docId).set(ca)
                .addOnSuccessListener(aVoid -> {
                    UIUtils.showToast(this, "Class Assignment saved successfully!");
                    if (parentDialog != null) parentDialog.dismiss();
                    loadAssignments();
                })
                .addOnFailureListener(e -> UIUtils.showAlert(this, "Save Failed", "Could not save assignment: " + e.getMessage()));
    }

    private void confirmDeleteAssignment(ClassAssignment ca) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Assignment")
                .setMessage("Are you sure you want to remove the class assignment for "
                        + ca.getSubjectName() + " (" + ca.getTeacherName() + ")?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (ca.getId() == null) return;
                    FirebaseHelper.getClassAssignmentsRef().document(ca.getId()).delete()
                            .addOnSuccessListener(aVoid -> {
                                UIUtils.showToast(this, "Assignment deleted.");
                                loadAssignments();
                            })
                            .addOnFailureListener(e -> UIUtils.showToast(this, "Delete failed: " + e.getMessage()));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private TextView createLabel(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(13f);
        tv.setTextColor(0xFF0D47A1);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setPadding(0, dpToPx(10), 0, dpToPx(2));
        return tv;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}