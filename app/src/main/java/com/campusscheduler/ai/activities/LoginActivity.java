package com.campusscheduler.ai.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.campusscheduler.ai.R;
import com.campusscheduler.ai.models.Faculty;
import com.campusscheduler.ai.models.Student;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.utils.CollegeIdentityHelper;
import com.campusscheduler.ai.utils.FirebaseHelper;
import com.campusscheduler.ai.utils.LocalAuthManager;
import com.campusscheduler.ai.utils.UIUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private View btnLogin, tvRegisterLink;
    private TextView tvForgotPassword, tvSecuredFirebase;
    private ImageButton btnTogglePassword;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegisterLink = findViewById(R.id.tvRegisterLink);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvSecuredFirebase = findViewById(R.id.tvSecuredFirebase);
        btnTogglePassword = findViewById(R.id.btnTogglePassword);

        if (btnLogin != null) {
            btnLogin.setOnClickListener(v -> performLogin());
        }

        if (btnTogglePassword != null) {
            btnTogglePassword.setOnClickListener(v -> togglePasswordVisibility());
        }

        if (tvRegisterLink != null) {
            tvRegisterLink.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
        }

        if (tvForgotPassword != null) {
            tvForgotPassword.setOnClickListener(v -> startActivity(new Intent(this, ForgotPasswordActivity.class)));
        }

        if (tvSecuredFirebase != null) {
            tvSecuredFirebase.setOnClickListener(v -> startActivity(new Intent(this, OfficialLoginActivity.class)));
        }
    }

    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            isPasswordVisible = false;
        } else {
            etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            isPasswordVisible = true;
        }
        etPassword.setSelection(etPassword.getText().length());
    }

    private void performLogin() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (email.isEmpty() || password.isEmpty()) {
            UIUtils.showToast(this, "Please enter college email and password");
            return;
        }

        if (email.equalsIgnoreCase("mohammadkaif7795@gmail.com")) {
            UIUtils.showAlert(this, "Official Owner Account", "This email belongs to the Official Owner. Please tap the secret footer area to access the Official Owner Portal.");
            return;
        }

        ProgressDialog pd = UIUtils.showLoadingDialog(this, "Authenticating with Firebase...");

        // Try Firebase Authentication first
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    String authUid = firebaseUser != null ? firebaseUser.getUid() : null;
                    if (authUid == null) {
                        pd.dismiss();
                        UIUtils.showToast(this, "Firebase Auth error: UID missing.");
                        return;
                    }

                    // Reload user state to obtain the most up-to-date email verification status
                    firebaseUser.reload().addOnCompleteListener(reloadTask -> {
                        boolean isVerified = firebaseUser.isEmailVerified();
                        if (!isVerified) {
                            pd.dismiss();
                            showEmailVerificationPrompt(firebaseUser, authUid, email);
                        } else {
                            fetchUserProfileAndNavigate(authUid, email, pd);
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    // Fallback lookup in Firestore users collection if Auth provider operation is not allowed or profile pre-registered
                    FirebaseHelper.getUsersRef().whereEqualTo("email", email).get()
                            .addOnSuccessListener(querySnapshots -> {
                                if (querySnapshots != null && !querySnapshots.isEmpty()) {
                                    DocumentSnapshot doc = querySnapshots.getDocuments().get(0);
                                    User user = doc.toObject(User.class);
                                    if (user != null) {
                                        completeLogin(user, pd);
                                        return;
                                    }
                                }

                                // Check user_invitations for initial setup
                                String key = email.toLowerCase().replaceAll("[^a-z0-9]", "_");
                                FirebaseHelper.getFirestore().collection("user_invitations").document(key).get()
                                        .addOnSuccessListener(invDoc -> {
                                            pd.dismiss();
                                            if (invDoc.exists()) {
                                                User newUser = new User();
                                                newUser.setUid(key);
                                                newUser.setEmail(email);
                                                newUser.setName(invDoc.getString("name"));
                                                newUser.setRole(invDoc.getString("role"));
                                                newUser.setCollegeId(invDoc.getString("collegeId"));
                                                newUser.setDepartmentId(invDoc.getString("departmentId"));

                                                FirebaseHelper.getUsersRef().document(key).set(newUser)
                                                        .addOnSuccessListener(aVoid -> {
                                                            completeLogin(newUser, pd);
                                                        });
                                            } else {
                                                UIUtils.showToast(LoginActivity.this, "Authentication failed: " + e.getMessage());
                                            }
                                        })
                                        .addOnFailureListener(invErr -> {
                                            pd.dismiss();
                                            UIUtils.showToast(LoginActivity.this, "Authentication failed: " + e.getMessage());
                                        });
                            })
                            .addOnFailureListener(dbErr -> {
                                pd.dismiss();
                                UIUtils.showToast(LoginActivity.this, "Authentication failed: " + e.getMessage());
                            });
                });
    }

    private void showEmailVerificationPrompt(FirebaseUser firebaseUser, String authUid, String email) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Email Verification Pending")
                .setMessage("Your email address (" + email + ") is pending email verification.\n\nPlease check your inbox (and spam folder) for the verification link sent by CampusSchedulerAI.")
                .setPositiveButton("CONTINUE TO DASHBOARD", (dialog, which) -> {
                    ProgressDialog pd = UIUtils.showLoadingDialog(this, "Loading profile...");
                    fetchUserProfileAndNavigate(authUid, email, pd);
                })
                .setNeutralButton("RESEND EMAIL", (dialog, which) -> {
                    firebaseUser.sendEmailVerification()
                            .addOnSuccessListener(v -> {
                                UIUtils.showAlert(this, "Verification Email Sent",
                                        "A fresh verification link has been sent to " + email + ".\n\nPlease open your email and tap the verification link.");
                            })
                            .addOnFailureListener(err -> {
                                UIUtils.showToast(this, "Resend failed: " + err.getMessage());
                            });
                })
                .setNegativeButton("SIGN OUT", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                })
                .show();
    }

    private void completeLogin(User user, ProgressDialog pd) {
        if (user == null) {
            if (pd != null) pd.dismiss();
            return;
        }

        String targetCollegeId = user.getCollegeId();
        if (targetCollegeId == null || targetCollegeId.trim().isEmpty()) {
            targetCollegeId = "COLLEGE_1789406176044";
            user.setCollegeId(targetCollegeId);
        }

        CollegeIdentityHelper.resolveCollege(this, targetCollegeId, college -> {
            if (college != null) {
                user.setCollegeName(CollegeIdentityHelper.formatCollegeName(college.getName()));
                user.setCollegeAddress(CollegeIdentityHelper.formatVerifiedAddress(college.getAddress(), user.getCollegeName()));
                user.setCollegeCode(college.getCode() != null ? college.getCode().toUpperCase() : "CAMPUS");
            }
            LocalAuthManager.saveUserSession(LoginActivity.this, user);
            if (pd != null) pd.dismiss();
            UIUtils.showToast(LoginActivity.this, "Sign-in successful (" + user.getRole() + ")");
            navigateBasedOnRole(user);
        });
    }

    private void fetchUserProfileAndNavigate(String authUid, String email, ProgressDialog pd) {
        FirebaseHelper.getUsersRef().document(authUid).get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        User user = doc.toObject(User.class);
                        if (user != null) {
                            completeLogin(user, pd);
                            return;
                        }
                    }

                    // Fallback 1: Query users collection by email to link UID
                    FirebaseHelper.getUsersRef().whereEqualTo("email", email).get()
                            .addOnSuccessListener(userQuery -> {
                                if (userQuery != null && !userQuery.isEmpty()) {
                                    DocumentSnapshot userDoc = userQuery.getDocuments().get(0);
                                    User user = userDoc.toObject(User.class);
                                    if (user != null) {
                                        user.setUid(authUid);
                                        FirebaseHelper.getUsersRef().document(authUid).set(user);

                                        // Update faculty table with real authUid
                                        FirebaseHelper.getFacultyRef().whereEqualTo("email", email).get()
                                                .addOnSuccessListener(facSnap -> {
                                                    if (facSnap != null && !facSnap.isEmpty()) {
                                                        facSnap.getDocuments().get(0).getReference().update("uid", authUid);
                                                    }
                                                });

                                        completeLogin(user, pd);
                                        return;
                                    }
                                }

                                // Fallback 2: Query faculty collection by email
                                FirebaseHelper.getFacultyRef().whereEqualTo("email", email).get()
                                        .addOnSuccessListener(facQuery -> {
                                            if (facQuery != null && !facQuery.isEmpty()) {
                                                DocumentSnapshot facDoc = facQuery.getDocuments().get(0);
                                                Faculty fac = facDoc.toObject(Faculty.class);
                                                if (fac != null) {
                                                    facDoc.getReference().update("uid", authUid);
                                                    User newUser = new User(authUid, fac.getName(), email, User.ROLE_TEACHER,
                                                            fac.getCollegeId(), fac.getDepartmentId(), fac.getPhone());
                                                    newUser.setDesignation(fac.getDesignation());
                                                    newUser.setSpecialization(fac.getSpecialization());
                                                    newUser.setQualification(fac.getQualification());
                                                    newUser.setExperience(fac.getExperience());
                                                    newUser.setExpertise(fac.getExpertise());
                                                    FirebaseHelper.getUsersRef().document(authUid).set(newUser);

                                                    completeLogin(newUser, pd);
                                                    return;
                                                }
                                            }

                                            // Fallback 3: Query students collection by email
                                            FirebaseHelper.getStudentsRef().whereEqualTo("email", email).get()
                                                    .addOnSuccessListener(stuQuery -> {
                                                        if (stuQuery != null && !stuQuery.isEmpty()) {
                                                            DocumentSnapshot stuDoc = stuQuery.getDocuments().get(0);
                                                            Student stu = stuDoc.toObject(Student.class);
                                                            if (stu != null) {
                                                                stuDoc.getReference().update("uid", authUid);
                                                                User newStuUser = new User(authUid, stu.getName(), email, User.ROLE_STUDENT,
                                                                        stu.getCollegeId(), stu.getDepartmentId(), stu.getPhone());
                                                                newStuUser.setUsn(stu.getUsn());
                                                                newStuUser.setSemester(stu.getSemester() > 0 ? String.valueOf(stu.getSemester()) : "1");
                                                                newStuUser.setSectionId(stu.getSection() != null ? stu.getSection() : "A");
                                                                FirebaseHelper.getUsersRef().document(authUid).set(newStuUser);

                                                                completeLogin(newStuUser, pd);
                                                                return;
                                                            }
                                                        }

                                                        pd.dismiss();
                                                        UIUtils.showAlert(this, "Profile Not Found",
                                                                "Your profile record was not found in Firestore under users/" + authUid +
                                                                        ". Please contact your College Administrator.");
                                                    })
                                                    .addOnFailureListener(err -> {
                                                        pd.dismiss();
                                                        UIUtils.showToast(this, "Failed to load student record: " + err.getMessage());
                                                    });
                                        })
                                        .addOnFailureListener(err -> {
                                            pd.dismiss();
                                            UIUtils.showToast(this, "Failed to load faculty record: " + err.getMessage());
                                        });
                            })
                            .addOnFailureListener(err -> {
                                pd.dismiss();
                                UIUtils.showToast(this, "Failed to query user profile: " + err.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    pd.dismiss();
                    UIUtils.showToast(this, "Failed to load user profile: " + e.getMessage());
                });
    }

    private void navigateBasedOnRole(User user) {
        if (isProfileIncomplete(user)) {
            Intent intent = new Intent(this, CompleteProfileActivity.class);
            intent.putExtra("user", user);
            startActivity(intent);
            finish();
            return;
        }

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

    private boolean isProfileIncomplete(User user) {
        if (user == null) return false;
        String role = user.getRole();
        if (User.ROLE_COLLEGE_ADMIN.equalsIgnoreCase(role) || User.ROLE_OFFICIAL_OWNER.equalsIgnoreCase(role)) {
            return false;
        }
        if (User.ROLE_TEACHER.equalsIgnoreCase(role)) {
            boolean missingPhone = user.getPhone() == null || user.getPhone().trim().isEmpty() || "null".equalsIgnoreCase(user.getPhone().trim());
            boolean missingQual = user.getQualification() == null || user.getQualification().trim().isEmpty() || "null".equalsIgnoreCase(user.getQualification().trim());
            boolean missingSpec = user.getSpecialization() == null || user.getSpecialization().trim().isEmpty() || "null".equalsIgnoreCase(user.getSpecialization().trim());
            return missingPhone || missingQual || missingSpec;
        }
        if (User.ROLE_STUDENT.equalsIgnoreCase(role)) {
            return user.getPhone() == null || user.getPhone().trim().isEmpty() || "null".equalsIgnoreCase(user.getPhone().trim());
        }
        return false;
    }
}
