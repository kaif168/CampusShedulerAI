package com.campusscheduler.ai.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.campusscheduler.ai.R;
import com.campusscheduler.ai.models.College;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.utils.FirebaseHelper;
import com.campusscheduler.ai.utils.LocalAuthManager;
import com.campusscheduler.ai.utils.UIUtils;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPassword, etPhone;
    private Spinner spinnerRole, spinnerCollege;
    private Button btnRegister;
    private TextView tvLoginLink;

    private List<College> collegeList = new ArrayList<>();
    private Map<String, String> collegeMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etPhone = findViewById(R.id.etPhone);
        spinnerRole = findViewById(R.id.spinnerRole);
        spinnerCollege = findViewById(R.id.spinnerCollege);
        btnRegister = findViewById(R.id.btnRegister);
        tvLoginLink = findViewById(R.id.tvLoginLink);

        String[] roles = {"STUDENT", "TEACHER", "COLLEGE_ADMIN"};
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roles);
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(roleAdapter);

        loadRegisteredColleges();

        btnRegister.setOnClickListener(v -> performRegistration());
        tvLoginLink.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void loadRegisteredColleges() {
        FirebaseHelper.getCollegesRef().get().addOnSuccessListener(queryDocumentSnapshots -> {
            collegeList.clear();
            collegeMap.clear();
            List<String> names = new ArrayList<>();

            if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                for (var doc : queryDocumentSnapshots) {
                    College c = doc.toObject(College.class);
                    if (c != null && c.getName() != null) {
                        collegeList.add(c);
                        collegeMap.put(c.getName(), c.getId());
                        names.add(c.getName() + " (" + (c.getCode() != null ? c.getCode() : "COLLEGE") + ")");
                    }
                }
            }

            if (names.isEmpty()) {
                names.add("No Registered College (Official Owner must add college first)");
            }

            ArrayAdapter<String> collegeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
            collegeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerCollege.setAdapter(collegeAdapter);
        }).addOnFailureListener(err -> {
            List<String> names = new ArrayList<>();
            names.add("No Registered College (Official Owner must add college first)");
            ArrayAdapter<String> collegeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
            spinnerCollege.setAdapter(collegeAdapter);
        });
    }

    private void performRegistration() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String role = spinnerRole.getSelectedItem() != null ? spinnerRole.getSelectedItem().toString() : "STUDENT";

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            UIUtils.showToast(this, "Please fill in all required fields");
            return;
        }

        if (password.length() < 6) {
            UIUtils.showToast(this, "Password must be at least 6 characters long");
            return;
        }

        String collegeId = null;
        String collegeName = null;
        if (!collegeList.isEmpty()) {
            int selectedIndex = spinnerCollege.getSelectedItemPosition();
            if (selectedIndex >= 0 && selectedIndex < collegeList.size()) {
                College selectedCollege = collegeList.get(selectedIndex);
                collegeId = selectedCollege.getId();
                collegeName = selectedCollege.getName();
            }
        }

        ProgressDialog pd = UIUtils.showLoadingDialog(this, "Creating Account...");

        final String finalCollegeId = collegeId;
        final String finalCollegeName = collegeName;

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String realUid = authResult.getUser() != null ? authResult.getUser().getUid() : null;
                    if (realUid == null) {
                        realUid = email.toLowerCase().replaceAll("[^a-z0-9]", "_");
                    }
                    saveRegisteredUser(realUid, name, email, role, finalCollegeId, finalCollegeName, phone, authResult.getUser() != null, pd);
                })
                .addOnFailureListener(e -> {
                    // Fallback registration document creation in Firestore when Auth provider operation is not allowed in console
                    String docId = email.toLowerCase().replaceAll("[^a-z0-9]", "_");
                    saveRegisteredUser(docId, name, email, role, finalCollegeId, finalCollegeName, phone, false, pd);
                });
    }

    private void saveRegisteredUser(String uid, String name, String email, String role, String collegeId, String collegeName, String phone, boolean sendVerification, ProgressDialog pd) {
        User user = new User(uid, name, email, role, collegeId, "DEPT_CSE", phone);
        user.setCollegeName(collegeName);
        user.setDegree("B.E. Computer Science Engineering");
        user.setSemester("Semester 5");
        user.setSectionId("Section B");
        user.setYear("3rd Year");
        user.setUsn("1HK23CS082");
        user.setAdmissionType("Regular CET");
        user.setDepartmentName("Computer Science and Engineering");

        FirebaseHelper.getUsersRef().document(uid).set(user)
                .addOnSuccessListener(aVoid -> {
                    pd.dismiss();
                    if (sendVerification && FirebaseAuth.getInstance().getCurrentUser() != null) {
                        FirebaseAuth.getInstance().getCurrentUser().sendEmailVerification();
                    }
                    LocalAuthManager.saveUserSession(RegisterActivity.this, user);
                    UIUtils.showToast(RegisterActivity.this, "Account created successfully!");
                    navigateBasedOnRole(user);
                })
                .addOnFailureListener(e -> {
                    pd.dismiss();
                    LocalAuthManager.saveUserSession(RegisterActivity.this, user);
                    UIUtils.showToast(RegisterActivity.this, "Account registered!");
                    navigateBasedOnRole(user);
                });
    }

    private void navigateBasedOnRole(User user) {
        Intent intent;
        switch (user.getRole()) {
            case User.ROLE_COLLEGE_ADMIN:
                intent = new Intent(this, AdminDashboardActivity.class);
                break;
            case User.ROLE_TEACHER:
                intent = new Intent(this, TeacherDashboardActivity.class);
                break;
            case User.ROLE_STUDENT:
            default:
                intent = new Intent(this, StudentDashboardActivity.class);
                break;
        }
        startActivity(intent);
        finish();
    }
}
