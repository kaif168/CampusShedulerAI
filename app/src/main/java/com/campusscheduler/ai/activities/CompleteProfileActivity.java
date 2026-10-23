package com.campusscheduler.ai.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.campusscheduler.ai.R;
import com.campusscheduler.ai.models.Faculty;
import com.campusscheduler.ai.models.Student;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.utils.FirebaseHelper;
import com.campusscheduler.ai.utils.LocalAuthManager;
import com.campusscheduler.ai.utils.UIUtils;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class CompleteProfileActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextView tvWelcomeTitle, tvProfileName, tvProfileId, tvProfileDept, tvProfileCohort;
    private TextInputEditText etPhone, etEmail;
    private LinearLayout layoutFacultyFields;
    private Spinner spinnerDesignation;
    private TextInputEditText etQualification, etExperience, etSpecialization, etExpertise;
    private Button btnSaveProfile;

    private User currentUser;
    private String userRole;
    private String facultyDocId = null;
    private String studentDocId = null;

    private final String[] designations = {
            "Assistant Professor",
            "Associate Professor",
            "Professor",
            "Professor & HOD",
            "Adjunct Faculty",
            "Visiting Faculty",
            "Lecturer"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complete_profile);

        initViews();
        loadUserContext();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setTitle("Complete Your Profile");
            setSupportActionBar(toolbar);
        }

        tvWelcomeTitle = findViewById(R.id.tvWelcomeTitle);
        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileId = findViewById(R.id.tvProfileId);
        tvProfileDept = findViewById(R.id.tvProfileDept);
        tvProfileCohort = findViewById(R.id.tvProfileCohort);

        etPhone = findViewById(R.id.etPhone);
        etEmail = findViewById(R.id.etEmail);

        layoutFacultyFields = findViewById(R.id.layoutFacultyFields);
        spinnerDesignation = findViewById(R.id.spinnerDesignation);
        etQualification = findViewById(R.id.etQualification);
        etExperience = findViewById(R.id.etExperience);
        etSpecialization = findViewById(R.id.etSpecialization);
        etExpertise = findViewById(R.id.etExpertise);

        btnSaveProfile = findViewById(R.id.btnSaveProfile);

        if (spinnerDesignation != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, designations);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerDesignation.setAdapter(adapter);
        }

        if (btnSaveProfile != null) {
            btnSaveProfile.setOnClickListener(v -> saveProfile());
        }
    }

    private void loadUserContext() {
        if (getIntent().hasExtra("user")) {
            currentUser = (User) getIntent().getSerializableExtra("user");
        }

        if (currentUser == null && getIntent().hasExtra("role")) {
            currentUser = new User();
            currentUser.setRole(getIntent().getStringExtra("role"));
            currentUser.setName(getIntent().getStringExtra("name"));
            currentUser.setEmail(getIntent().getStringExtra("email"));
            currentUser.setUid(getIntent().getStringExtra("uid"));
            currentUser.setDepartmentName(getIntent().getStringExtra("departmentName"));
            currentUser.setUsn(getIntent().getStringExtra("usn"));
            currentUser.setSemester(getIntent().getStringExtra("semester"));
            currentUser.setSectionId(getIntent().getStringExtra("sectionId"));
        }

        if (currentUser == null) {
            currentUser = LocalAuthManager.getCurrentLocalUser(this);
        }

        if (currentUser == null) {
            UIUtils.showToast(this, "Session expired. Please sign in again.");
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        userRole = currentUser.getRole() != null ? currentUser.getRole() : User.ROLE_STUDENT;

        // Populate read-only institutional info
        tvProfileName.setText("Name: " + (currentUser.getName() != null ? currentUser.getName() : "Faculty / Student"));
        String deptName = currentUser.getDepartmentName() != null ? currentUser.getDepartmentName() : "Computer Science and Engineering";
        tvProfileDept.setText("🔒 Department: " + deptName);

        // Prepopulate email and phone if present
        if (currentUser.getEmail() != null && !currentUser.getEmail().isEmpty()) {
            etEmail.setText(currentUser.getEmail());
        }
        if (currentUser.getPhone() != null && !currentUser.getPhone().isEmpty()) {
            etPhone.setText(currentUser.getPhone());
        }

        if (User.ROLE_TEACHER.equalsIgnoreCase(userRole)) {
            setupTeacherUi();
        } else {
            setupStudentUi();
        }
    }

    private void setupTeacherUi() {
        tvWelcomeTitle.setText("Faculty Profile Completion");
        layoutFacultyFields.setVisibility(View.VISIBLE);

        String idText = currentUser.getUid() != null ? currentUser.getUid() : "--";
        tvProfileId.setText("🔒 Faculty ID / UID: " + idText);
        tvProfileCohort.setText("🔒 Academic Status: Faculty Member (CSE)");

        if (currentUser.getQualification() != null) etQualification.setText(currentUser.getQualification());
        if (currentUser.getSpecialization() != null) etSpecialization.setText(currentUser.getSpecialization());
        if (currentUser.getExperience() != null) etExperience.setText(currentUser.getExperience());
        if (currentUser.getExpertise() != null) etExpertise.setText(currentUser.getExpertise());

        if (currentUser.getDesignation() != null) {
            for (int i = 0; i < designations.length; i++) {
                if (designations[i].equalsIgnoreCase(currentUser.getDesignation().trim())) {
                    spinnerDesignation.setSelection(i);
                    break;
                }
            }
        }

        // Fetch existing faculty record to fill details if any
        String email = currentUser.getEmail();
        String uid = currentUser.getUid();
        if (email != null && !email.isEmpty()) {
            FirebaseHelper.getFacultyRef().whereEqualTo("email", email).get()
                    .addOnSuccessListener(query -> {
                        if (query != null && !query.isEmpty()) {
                            DocumentSnapshot doc = query.getDocuments().get(0);
                            facultyDocId = doc.getId();
                            tvProfileId.setText("🔒 Faculty ID: " + facultyDocId);
                            Faculty f = doc.toObject(Faculty.class);
                            if (f != null) {
                                populateFacultyFields(f);
                            }
                        } else if (uid != null) {
                            FirebaseHelper.getFacultyRef().document(uid).get()
                                    .addOnSuccessListener(doc -> {
                                        if (doc != null && doc.exists()) {
                                            facultyDocId = doc.getId();
                                            tvProfileId.setText("🔒 Faculty ID: " + facultyDocId);
                                            Faculty f = doc.toObject(Faculty.class);
                                            if (f != null) populateFacultyFields(f);
                                        }
                                    });
                        }
                    });
        }
    }

    private void populateFacultyFields(Faculty f) {
        if (f.getPhone() != null && !f.getPhone().isEmpty() && (etPhone.getText() == null || etPhone.getText().toString().isEmpty())) {
            etPhone.setText(f.getPhone());
        }
        if (f.getQualification() != null && !f.getQualification().isEmpty() && (etQualification.getText() == null || etQualification.getText().toString().isEmpty())) {
            etQualification.setText(f.getQualification());
        }
        if (f.getSpecialization() != null && !f.getSpecialization().isEmpty() && (etSpecialization.getText() == null || etSpecialization.getText().toString().isEmpty())) {
            etSpecialization.setText(f.getSpecialization());
        }
        if (f.getExperience() != null && !f.getExperience().isEmpty() && (etExperience.getText() == null || etExperience.getText().toString().isEmpty())) {
            etExperience.setText(f.getExperience());
        }
        if (f.getExpertise() != null && !f.getExpertise().isEmpty() && (etExpertise.getText() == null || etExpertise.getText().toString().isEmpty())) {
            etExpertise.setText(f.getExpertise());
        }
        if (f.getDesignation() != null) {
            for (int i = 0; i < designations.length; i++) {
                if (designations[i].equalsIgnoreCase(f.getDesignation().trim())) {
                    spinnerDesignation.setSelection(i);
                    break;
                }
            }
        }
    }

    private void setupStudentUi() {
        tvWelcomeTitle.setText("Student Profile Contact Setup");
        layoutFacultyFields.setVisibility(View.GONE);

        String usn = currentUser.getUsn() != null ? currentUser.getUsn() : currentUser.getUid();
        tvProfileId.setText("🔒 USN: " + (usn != null ? usn : "--"));

        String sem = currentUser.getSemester() != null ? currentUser.getSemester() : "7";
        String sec = currentUser.getSectionId() != null ? currentUser.getSectionId() : "A";
        tvProfileCohort.setText("🔒 Semester: " + sem + " | Section: " + sec);

        String email = currentUser.getEmail();
        if (email != null && !email.isEmpty()) {
            FirebaseHelper.getStudentsRef().whereEqualTo("email", email).get()
                    .addOnSuccessListener(query -> {
                        if (query != null && !query.isEmpty()) {
                            DocumentSnapshot doc = query.getDocuments().get(0);
                            studentDocId = doc.getId();
                            Student s = doc.toObject(Student.class);
                            if (s != null) {
                                if (s.getPhone() != null && !s.getPhone().isEmpty() && (etPhone.getText() == null || etPhone.getText().toString().isEmpty())) {
                                    etPhone.setText(s.getPhone());
                                }
                                tvProfileCohort.setText("🔒 Semester: " + s.getSemester() + " | Section: " + s.getSection());
                            }
                        }
                    });
        }
    }

    private void saveProfile() {
        String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";

        if (phone.isEmpty()) {
            etPhone.setError("Contact phone number is required");
            etPhone.requestFocus();
            return;
        }

        if (phone.length() < 10) {
            etPhone.setError("Please enter a valid 10-digit mobile number");
            etPhone.requestFocus();
            return;
        }

        if (email.isEmpty() || !email.contains("@")) {
            etEmail.setError("Valid email address is required");
            etEmail.requestFocus();
            return;
        }

        ProgressDialog pd = UIUtils.showLoadingDialog(this, "Saving profile information...");

        currentUser.setPhone(phone);
        currentUser.setEmail(email);

        Map<String, Object> userUpdates = new HashMap<>();
        userUpdates.put("phone", phone);
        userUpdates.put("email", email);

        if (User.ROLE_TEACHER.equalsIgnoreCase(userRole)) {
            String designation = spinnerDesignation.getSelectedItem() != null ? spinnerDesignation.getSelectedItem().toString() : "Assistant Professor";
            String qualification = etQualification.getText() != null ? etQualification.getText().toString().trim() : "";
            String specialization = etSpecialization.getText() != null ? etSpecialization.getText().toString().trim() : "";
            String experience = etExperience.getText() != null ? etExperience.getText().toString().trim() : "";
            String expertise = etExpertise.getText() != null ? etExpertise.getText().toString().trim() : "";

            if (qualification.isEmpty()) {
                pd.dismiss();
                etQualification.setError("Highest qualification is required");
                etQualification.requestFocus();
                return;
            }

            if (specialization.isEmpty()) {
                pd.dismiss();
                etSpecialization.setError("Specialization is required");
                etSpecialization.requestFocus();
                return;
            }

            currentUser.setDesignation(designation);
            currentUser.setQualification(qualification);
            currentUser.setSpecialization(specialization);
            currentUser.setExperience(experience);
            currentUser.setExpertise(expertise);

            userUpdates.put("designation", designation);
            userUpdates.put("qualification", qualification);
            userUpdates.put("specialization", specialization);
            userUpdates.put("experience", experience);
            userUpdates.put("expertise", expertise);

            // Update faculty document
            Map<String, Object> facUpdates = new HashMap<>();
            facUpdates.put("phone", phone);
            facUpdates.put("email", email);
            facUpdates.put("designation", designation);
            facUpdates.put("qualification", qualification);
            facUpdates.put("specialization", specialization);
            facUpdates.put("experience", experience);
            facUpdates.put("expertise", expertise);
            facUpdates.put("status", "ACTIVE");
            if (currentUser.getUid() != null) {
                facUpdates.put("uid", currentUser.getUid());
            }

            if (facultyDocId != null) {
                FirebaseHelper.getFacultyRef().document(facultyDocId).update(facUpdates);
            } else if (currentUser.getUid() != null) {
                FirebaseHelper.getFacultyRef().document(currentUser.getUid()).update(facUpdates);
            }
        } else {
            // Student update
            Map<String, Object> stuUpdates = new HashMap<>();
            stuUpdates.put("phone", phone);
            stuUpdates.put("email", email);
            if (currentUser.getUid() != null) {
                stuUpdates.put("uid", currentUser.getUid());
            }

            if (studentDocId != null) {
                FirebaseHelper.getStudentsRef().document(studentDocId).update(stuUpdates);
            } else if (currentUser.getUsn() != null) {
                FirebaseHelper.getStudentsRef().document(currentUser.getUsn()).update(stuUpdates);
            }
        }

        // Update users collection
        if (currentUser.getUid() != null) {
            FirebaseHelper.getUsersRef().document(currentUser.getUid()).update(userUpdates)
                    .addOnCompleteListener(task -> finalizeProfileSave(pd));
        } else {
            finalizeProfileSave(pd);
        }
    }

    private void finalizeProfileSave(ProgressDialog pd) {
        LocalAuthManager.saveUserSession(CompleteProfileActivity.this, currentUser);
        if (pd != null) pd.dismiss();

        UIUtils.showToast(CompleteProfileActivity.this, "Profile updated successfully!");

        Intent nextIntent;
        if (User.ROLE_TEACHER.equalsIgnoreCase(userRole)) {
            nextIntent = new Intent(CompleteProfileActivity.this, TeacherDashboardActivity.class);
        } else {
            nextIntent = new Intent(CompleteProfileActivity.this, StudentDashboardActivity.class);
        }
        startActivity(nextIntent);
        finish();
    }
}
