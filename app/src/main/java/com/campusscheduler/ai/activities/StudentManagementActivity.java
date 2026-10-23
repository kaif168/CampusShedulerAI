package com.campusscheduler.ai.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.campusscheduler.ai.R;
import com.campusscheduler.ai.models.Student;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.utils.FirebaseHelper;
import com.campusscheduler.ai.utils.LocalAuthManager;
import com.campusscheduler.ai.utils.StudentProvisioningManager;
import com.campusscheduler.ai.utils.UIUtils;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StudentManagementActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private CardView cvRegisterStudent;
    private TextView tvStudentListTitle, tvEmptyState;
    private Spinner spSemesterFilter;
    private EditText etUsn, etStudentName, etStudentEmail, etSemester, etSection;
    private EditText etSearchStudent;
    private Button btnAddStudent, btnBulkImportStudents;
    private ListView listViewStudents;

    private List<Student> allStudentList = new ArrayList<>();
    private List<Student> filteredStudentList = new ArrayList<>();
    private List<String> displayStrings = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private User currentUser;
    private String selectedSemesterFilter = "All Semesters";
    private String searchQuery = "";

    private static final List<String> SEMESTER_OPTIONS = Arrays.asList(
            "All Semesters",
            "5th Semester",
            "6th Semester",
            "7th Semester",
            "8th Semester",
            "4th Semester",
            "3rd Semester",
            "2nd Semester",
            "1st Semester"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_management);

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
        cvRegisterStudent = findViewById(R.id.cvRegisterStudent);
        tvStudentListTitle = findViewById(R.id.tvStudentListTitle);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        spSemesterFilter = findViewById(R.id.spSemesterFilter);

        etUsn = findViewById(R.id.etUsn);
        etStudentName = findViewById(R.id.etStudentName);
        etStudentEmail = findViewById(R.id.etStudentEmail);
        etSemester = findViewById(R.id.etSemester);
        etSection = findViewById(R.id.etSection);
        btnAddStudent = findViewById(R.id.btnAddStudent);
        listViewStudents = findViewById(R.id.listViewStudents);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Configure role context: For Teachers, hide registration form and show My Students
        boolean isAdminIntent = getIntent().getBooleanExtra("isAdmin", false);
        boolean isTeacher = !isAdminIntent && (currentUser != null && User.ROLE_TEACHER.equalsIgnoreCase(currentUser.getRole()));
        if (isTeacher) {
            if (cvRegisterStudent != null) cvRegisterStudent.setVisibility(View.GONE);
            if (tvStudentListTitle != null) tvStudentListTitle.setText("My Enrolled Students");
        } else {
            if (cvRegisterStudent != null) cvRegisterStudent.setVisibility(View.VISIBLE);
            if (tvStudentListTitle != null) tvStudentListTitle.setText("Institutional Student Directory");
        }

        // Configure dynamic semester filter spinner
        if (spSemesterFilter != null) {
            ArrayAdapter<String> semAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, SEMESTER_OPTIONS);
            semAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spSemesterFilter.setAdapter(semAdapter);
            spSemesterFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    selectedSemesterFilter = SEMESTER_OPTIONS.get(position);
                    applySemesterFilter();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, displayStrings);
        if (listViewStudents != null) {
            listViewStudents.setAdapter(adapter);
            listViewStudents.setOnItemClickListener((parent, view, position, id) -> {
                if (position >= 0 && position < filteredStudentList.size()) {
                    Student selected = filteredStudentList.get(position);
                    showStudentActionDialog(selected);
                }
            });
        }

        etSearchStudent = findViewById(R.id.etSearchStudent);
        if (etSearchStudent != null) {
            etSearchStudent.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    searchQuery = s != null ? s.toString().trim().toLowerCase() : "";
                    applySemesterFilter();
                }

                @Override
                public void afterTextChanged(android.text.Editable s) {}
            });
        }

        btnBulkImportStudents = findViewById(R.id.btnBulkImportStudents);
        if (btnBulkImportStudents != null) {
            btnBulkImportStudents.setOnClickListener(v -> {
                Intent intent = new Intent(this, StudentImportActivity.class);
                intent.putExtra("collegeId", currentUser != null ? currentUser.getCollegeId() : null);
                startActivity(intent);
            });
        }

        if (btnAddStudent != null) {
            btnAddStudent.setOnClickListener(v -> addStudentToFirestore());
        }

        loadStudentsFromFirestore();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStudentsFromFirestore();
    }

    private void loadStudentsFromFirestore() {
        String collegeId = (currentUser != null && currentUser.getCollegeId() != null && !currentUser.getCollegeId().trim().isEmpty())
                ? currentUser.getCollegeId() : LocalAuthManager.getSafeCollegeId(this, getIntent());
        FirebaseHelper.getStudentsRef().whereEqualTo("collegeId", collegeId).get()
                .addOnSuccessListener(querySnapshot -> {
                    allStudentList.clear();
                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        for (DocumentSnapshot doc : querySnapshot) {
                            Student s = doc.toObject(Student.class);
                            if (s != null && s.getName() != null) {
                                if (s.getId() == null) s.setId(doc.getId());
                                if (s.getStudentId() == null) s.setStudentId(doc.getId());
                                allStudentList.add(s);
                            }
                        }
                    }
                    applySemesterFilter();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load students: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void applySemesterFilter() {
        filteredStudentList.clear();
        displayStrings.clear();

        int targetSem = -1;
        if (selectedSemesterFilter.contains("5th")) targetSem = 5;
        else if (selectedSemesterFilter.contains("6th")) targetSem = 6;
        else if (selectedSemesterFilter.contains("7th")) targetSem = 7;
        else if (selectedSemesterFilter.contains("8th")) targetSem = 8;
        else if (selectedSemesterFilter.contains("4th")) targetSem = 4;
        else if (selectedSemesterFilter.contains("3rd")) targetSem = 3;
        else if (selectedSemesterFilter.contains("2nd")) targetSem = 2;
        else if (selectedSemesterFilter.contains("1st")) targetSem = 1;

        for (Student s : allStudentList) {
            if (targetSem == -1 || s.getSemester() == targetSem) {
                if (!searchQuery.isEmpty()) {
                    String usn = s.getUsn() != null ? s.getUsn().toLowerCase() : "";
                    String name = s.getName() != null ? s.getName().toLowerCase() : "";
                    if (!usn.contains(searchQuery) && !name.contains(searchQuery)) {
                        continue;
                    }
                }
                filteredStudentList.add(s);

                boolean isProvisioned = s.getUid() != null && !s.getUid().isEmpty() && !s.getUid().startsWith("UID_STU_");
                String badge = isProvisioned ? " [AUTH ACTIVE]" : " [PENDING AUTH]";

                String display = s.getUsn() + " - " + s.getName() + " (Sem " + s.getSemester() +
                        "-" + (s.getSection() != null ? s.getSection() : "A") + ")" + badge;
                displayStrings.add(display);
            }
        }

        if (filteredStudentList.isEmpty()) {
            if (tvEmptyState != null) {
                tvEmptyState.setVisibility(View.VISIBLE);
                if (!searchQuery.isEmpty()) {
                    tvEmptyState.setText("No students match \"" + searchQuery + "\" in " + selectedSemesterFilter + ".");
                } else {
                    tvEmptyState.setText("No students found enrolled in " + selectedSemesterFilter + ".");
                }
            }
        } else {
            if (tvEmptyState != null) {
                tvEmptyState.setVisibility(View.GONE);
            }
        }

        adapter.notifyDataSetChanged();
    }

    private void addStudentToFirestore() {
        String usn = etUsn != null ? etUsn.getText().toString().trim() : "";
        String name = etStudentName != null ? etStudentName.getText().toString().trim() : "";
        String email = etStudentEmail != null ? etStudentEmail.getText().toString().trim() : "";
        String semStr = etSemester != null ? etSemester.getText().toString().trim() : "";
        String sec = etSection != null ? etSection.getText().toString().trim() : "";

        if (usn.isEmpty() || name.isEmpty() || semStr.isEmpty()) {
            Toast.makeText(this, "USN, Name, and Semester required", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Uniqueness Validation
        for (Student existing : allStudentList) {
            if (existing.getUsn() != null && existing.getUsn().trim().equalsIgnoreCase(usn)) {
                UIUtils.showAlert(this, "Duplicate USN Detected",
                        "A student with USN '" + usn + "' is already registered:\n• Name: "
                                + existing.getName() + "\n• Semester: " + existing.getSemester()
                                + " • Section: " + existing.getSection());
                return;
            }
            if (!email.isEmpty() && existing.getEmail() != null && existing.getEmail().trim().equalsIgnoreCase(email)) {
                UIUtils.showAlert(this, "Duplicate Email Detected",
                        "Email '" + email + "' is already registered to:\n• " + existing.getName()
                                + " (" + existing.getUsn() + ")");
                return;
            }
        }

        int sem = 1;
        try {
            sem = Integer.parseInt(semStr);
        } catch (Exception ignored) {}

        final String targetSec = sec.isEmpty() ? "A" : sec.toUpperCase(java.util.Locale.US);

        // 2. Section Capacity Validation
        int targetCapacity = 60; // Standard section capacity
        int currentEnrolledInTarget = 0;
        java.util.Map<String, Integer> otherSectionCounts = new java.util.HashMap<>();

        for (Student s : allStudentList) {
            if (s.getSemester() == sem) {
                String existingSec = s.getSection() != null ? s.getSection().trim().toUpperCase(java.util.Locale.US) : "A";
                if (existingSec.equals(targetSec)) {
                    currentEnrolledInTarget++;
                } else {
                    otherSectionCounts.put(existingSec, otherSectionCounts.getOrDefault(existingSec, 0) + 1);
                }
            }
        }

        // If requested section capacity exceeded (e.g. 60 vs 67)
        if (currentEnrolledInTarget >= targetCapacity) {
            StringBuilder altSb = new StringBuilder();
            altSb.append("⚠️ Section Capacity Advisory:\n");
            altSb.append("Section ").append(targetSec).append(" has reached its maximum enrollment capacity (")
                    .append(currentEnrolledInTarget).append("/").append(targetCapacity).append(" students).\n\n");

            altSb.append("Available Alternative Sections in Semester ").append(sem).append(":\n");
            String[] candidateSecs = {"A", "B", "C", "D"};
            boolean foundAlternative = false;
            for (String cSec : candidateSecs) {
                if (!cSec.equals(targetSec)) {
                    int count = otherSectionCounts.getOrDefault(cSec, 0);
                    if (count < targetCapacity) {
                        int available = targetCapacity - count;
                        altSb.append("• Section ").append(cSec).append(": ").append(count).append("/").append(targetCapacity)
                                .append(" enrolled (").append(available).append(" seats available)\n");
                        foundAlternative = true;
                    }
                }
            }
            if (!foundAlternative) {
                altSb.append("• All known sections for Semester ").append(sem).append(" are currently at or near capacity.\n");
            }
            altSb.append("\nWould you like to assign to an alternative section or proceed with an administrative capacity override?");

            final int finalSem = sem;
            new AlertDialog.Builder(this)
                    .setTitle("Section Capacity Limit Reached")
                    .setMessage(altSb.toString())
                    .setPositiveButton("Administrative Override", (diag, which) -> {
                        saveStudentDirectly(usn, name, email, finalSem, targetSec);
                    })
                    .setNegativeButton("Change Section", null)
                    .show();
            return;
        }

        saveStudentDirectly(usn, name, email, sem, targetSec);
    }

    private void saveStudentDirectly(String usn, String name, String email, int sem, String sec) {
        String collegeId = currentUser != null && currentUser.getCollegeId() != null ? currentUser.getCollegeId() : "COLLEGE_01";
        String deptId = currentUser != null && currentUser.getDepartmentId() != null ? currentUser.getDepartmentId() : "DEPT_CSE";

        // Deterministic student and user ID to prevent duplicate records
        String stuId = "STU_" + collegeId + "_" + usn.replaceAll("[^a-zA-Z0-9]", "_");
        String uid = "UID_" + stuId;

        Student s = new Student(stuId, uid, collegeId, deptId, usn, name, email, "", sem, sec);
        User u = new User(uid, name, email, User.ROLE_STUDENT, collegeId, deptId, "");
        u.setUsn(usn);
        u.setSemester(String.valueOf(sem));
        u.setSectionId(sec);

        FirebaseHelper.getStudentsRef().document(stuId).set(s).addOnSuccessListener(aVoid -> {
            FirebaseHelper.getUsersRef().document(uid).set(u);
            allStudentList.add(s);
            applySemesterFilter();

            if (etUsn != null) etUsn.setText("");
            if (etStudentName != null) etStudentName.setText("");
            if (etStudentEmail != null) etStudentEmail.setText("");
            if (etSemester != null) etSemester.setText("");
            if (etSection != null) etSection.setText("");

            Toast.makeText(this, "Student registered & saved to Cloud Firestore!", Toast.LENGTH_SHORT).show();

            // If a valid email is provided, prompt administrator to provision login credentials
            if (StudentProvisioningManager.isValidEmail(email)) {
                new AlertDialog.Builder(this)
                        .setTitle("Provision Student Login Access?")
                        .setMessage("Student profile for " + name + " (" + usn + ") is registered.\n\nWould you like to create an authentication account and send an account setup / password creation email to " + email + " now?")
                        .setPositiveButton("Provision Now", (dialog, which) -> startProvisionStudentFlow(s))
                        .setNegativeButton("Later", null)
                        .show();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Firestore error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    private void showStudentActionDialog(Student student) {
        if (student == null) return;

        boolean isProvisioned = student.getUid() != null && !student.getUid().isEmpty() && !student.getUid().startsWith("UID_STU_");
        String statusLabel = isProvisioned ? "Active / Linked" : "Not Provisioned (Firestore Only)";

        String[] options = {
                "Provision Login Access (Create Auth + Send Setup Email)",
                "Resend Password Setup / Verification Email",
                "View Student Account & Provisioning Status"
        };

        new AlertDialog.Builder(this)
                .setTitle(student.getName() + " (" + (student.getUsn() != null ? student.getUsn() : "N/A") + ")")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            startProvisionStudentFlow(student);
                            break;
                        case 1:
                            resendStudentSetupEmail(student);
                            break;
                        case 2:
                            showStudentProfileDetails(student, statusLabel);
                            break;
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void startProvisionStudentFlow(Student student) {
        if (student == null) return;

        if (student.getEmail() == null || student.getEmail().trim().isEmpty()) {
            UIUtils.showAlert(this, "Email Missing",
                    "Please edit this student's profile to include their personal or institutional email address before provisioning.");
            return;
        }

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Provisioning Student Account");
        progressDialog.setMessage("Initializing isolated credentials channel...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        StudentProvisioningManager.provisionStudent(
                this,
                currentUser,
                student,
                new StudentProvisioningManager.ProvisioningCallback() {
                    @Override
                    public void onProgress(String message) {
                        runOnUiThread(() -> progressDialog.setMessage(message));
                    }

                    @Override
                    public void onSuccess(StudentProvisioningManager.ProvisioningResult result) {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            new AlertDialog.Builder(StudentManagementActivity.this)
                                    .setTitle("Student Provisioning Complete")
                                    .setMessage("Student account successfully provisioned!\n\n" + result.detailedStatus)
                                    .setPositiveButton("Done", (d, w) -> loadStudentsFromFirestore())
                                    .show();
                        });
                    }

                    @Override
                    public void onError(String title, String errorMessage) {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            UIUtils.showAlert(StudentManagementActivity.this, title, errorMessage);
                        });
                    }
                }
        );
    }

    private void resendStudentSetupEmail(Student student) {
        if (student == null) return;

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Resending Setup Email");
        progressDialog.setMessage("Dispatching password reset link...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        StudentProvisioningManager.resendStudentSetupEmail(
                this,
                currentUser,
                student,
                new StudentProvisioningManager.ProvisioningCallback() {
                    @Override
                    public void onProgress(String message) {
                        runOnUiThread(() -> progressDialog.setMessage(message));
                    }

                    @Override
                    public void onSuccess(StudentProvisioningManager.ProvisioningResult result) {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            new AlertDialog.Builder(StudentManagementActivity.this)
                                    .setTitle("Email Dispatched")
                                    .setMessage(result.detailedStatus)
                                    .setPositiveButton("OK", null)
                                    .show();
                        });
                    }

                    @Override
                    public void onError(String title, String errorMessage) {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            UIUtils.showAlert(StudentManagementActivity.this, title, errorMessage);
                        });
                    }
                }
        );
    }

    private void showStudentProfileDetails(Student student, String statusLabel) {
        StringBuilder sb = new StringBuilder();
        sb.append("• Student Name: ").append(student.getName()).append("\n");
        sb.append("• USN / Roll No: ").append(student.getUsn() != null ? student.getUsn() : "N/A").append("\n");
        sb.append("• Email: ").append(student.getEmail() != null ? student.getEmail() : "None").append("\n");
        sb.append("• Semester: ").append(student.getSemester()).append("th Semester\n");
        sb.append("• Section: ").append(student.getSection() != null ? student.getSection() : "A").append("\n");
        sb.append("• College ID: ").append(student.getCollegeId() != null ? student.getCollegeId() : "N/A").append("\n");
        sb.append("• Auth Status: ").append(statusLabel).append("\n");
        sb.append("• Auth UID: ").append(student.getUid() != null ? student.getUid() : "Not linked");

        new AlertDialog.Builder(this)
                .setTitle("Student Account Status")
                .setMessage(sb.toString())
                .setPositiveButton("OK", null)
                .setNeutralButton("Provision Login", (d, w) -> startProvisionStudentFlow(student))
                .show();
    }
}
