package com.campusscheduler.ai.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.campusscheduler.ai.R;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.utils.CollegeIdentityHelper;
import com.campusscheduler.ai.utils.LocalAuthManager;

public class SettingsActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvSettingsSubtitle;
    private TextView tvSettingsEmail;
    private TextView tvSettingsPhone;
    private TextView tvSettingsRole;
    private TextView tvSettingsCollegeName;
    private TextView tvSettingsCollegeLocation;
    private TextView tvSettingsCollegeCode;
    private TextView tvSettingsCollegeTenant;
    private TextView tvSettingsCollegeDepartment;
    private TextView tvSettingsCollegeContact;
    private TextView tvSettingsPlatformAccess;
    private Button btnResetPassword;

    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ------------------------------------------------------------
        // OWNER BLUE SYSTEM BAR
        // ------------------------------------------------------------

        Window window = getWindow();

        window.setStatusBarColor(Color.rgb(13, 71, 161));
        window.setNavigationBarColor(Color.BLACK);

        // ------------------------------------------------------------
        // LAYOUT
        // ------------------------------------------------------------

        setContentView(R.layout.activity_settings);

        // ------------------------------------------------------------
        // CURRENT LOCAL USER
        // ------------------------------------------------------------

        currentUser = LocalAuthManager.getCurrentLocalUser(this);

        // ------------------------------------------------------------
        // FIND VIEWS
        // ------------------------------------------------------------

        btnBack = findViewById(R.id.btnBack);
        tvSettingsSubtitle = findViewById(R.id.tvSettingsSubtitle);
        tvSettingsEmail = findViewById(R.id.tvSettingsEmail);
        tvSettingsPhone = findViewById(R.id.tvSettingsPhone);
        tvSettingsRole = findViewById(R.id.tvSettingsRole);
        tvSettingsCollegeName = findViewById(R.id.tvSettingsCollegeName);
        tvSettingsCollegeLocation = findViewById(R.id.tvSettingsCollegeLocation);
        tvSettingsCollegeCode = findViewById(R.id.tvSettingsCollegeCode);
        tvSettingsCollegeTenant = findViewById(R.id.tvSettingsCollegeTenant);
        tvSettingsCollegeDepartment = findViewById(R.id.tvSettingsCollegeDepartment);
        tvSettingsCollegeContact = findViewById(R.id.tvSettingsCollegeContact);
        tvSettingsPlatformAccess = findViewById(R.id.tvSettingsPlatformAccess);
        btnResetPassword = findViewById(R.id.btnResetPassword);

        // ------------------------------------------------------------
        // BACK
        // ------------------------------------------------------------

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // ------------------------------------------------------------
        // ACCOUNT & INSTITUTION INFORMATION
        // ------------------------------------------------------------

        if (currentUser != null) {
            String email = currentUser.getEmail();
            String phone = currentUser.getPhone();
            String role = currentUser.getRole();

            if (tvSettingsEmail != null) {
                tvSettingsEmail.setText("Email Address: " + (email != null && !email.trim().isEmpty() ? email : "N/A"));
            }

            if (tvSettingsPhone != null) {
                tvSettingsPhone.setText("Phone Number: " + (phone != null && !phone.trim().isEmpty() ? phone : "N/A"));
            }

            if (tvSettingsRole != null) {
                tvSettingsRole.setText("Account Role: " + (role != null && !role.trim().isEmpty() ? role : "COLLEGE_ADMIN"));
            }

            if (tvSettingsSubtitle != null) {
                if (User.ROLE_COLLEGE_ADMIN.equals(role)) {
                    tvSettingsSubtitle.setText("College Administrator Account");
                } else if (User.ROLE_TEACHER.equals(role)) {
                    tvSettingsSubtitle.setText("Faculty Account");
                } else if (User.ROLE_STUDENT.equals(role)) {
                    tvSettingsSubtitle.setText("Student Account");
                } else {
                    tvSettingsSubtitle.setText("Official Owner Account");
                }
            }

            if (tvSettingsPlatformAccess != null) {
                if (User.ROLE_COLLEGE_ADMIN.equals(role)) {
                    tvSettingsPlatformAccess.setText("This account has College Administrator access for institutional management, automated timetable generation, and academic control.");
                } else if (User.ROLE_TEACHER.equals(role)) {
                    tvSettingsPlatformAccess.setText("This account has Faculty access for timetable view, attendance registration, class assignments, and grading.");
                } else if (User.ROLE_STUDENT.equals(role)) {
                    tvSettingsPlatformAccess.setText("This account has Student access for schedule viewing, attendance tracking, and academic coursework.");
                } else {
                    tvSettingsPlatformAccess.setText("This account has Official Owner access for global CampusSchedulerAI platform governance.");
                }
            }

            String collegeId = currentUser.getCollegeId() != null && !currentUser.getCollegeId().trim().isEmpty()
                    ? currentUser.getCollegeId() : "COLLEGE_1789406176044";

            // Populate preliminary local values
            String localName = currentUser.getCollegeName() != null ? CollegeIdentityHelper.formatCollegeName(currentUser.getCollegeName()) : "HKBK College of Engineering";
            String localAddr = currentUser.getCollegeAddress() != null ? currentUser.getCollegeAddress() : "Nagawara, Bengaluru, Karnataka 560045";
            String localDept = currentUser.getDepartmentName() != null ? currentUser.getDepartmentName() : "Computer Science & Engineering";

            if (tvSettingsCollegeName != null) tvSettingsCollegeName.setText("Institution: " + localName);
            if (tvSettingsCollegeLocation != null) tvSettingsCollegeLocation.setText("Campus Location: " + localAddr);
            if (tvSettingsCollegeCode != null) tvSettingsCollegeCode.setText("College Code: " + (currentUser.getCollegeCode() != null ? currentUser.getCollegeCode() : "hkbkce"));
            if (tvSettingsCollegeTenant != null) tvSettingsCollegeTenant.setText("Tenant ID: " + collegeId);
            if (tvSettingsCollegeDepartment != null) tvSettingsCollegeDepartment.setText("Department: " + localDept);

            // Asynchronously resolve authoritative college record from Firestore
            CollegeIdentityHelper.resolveCollege(this, collegeId, college -> {
                if (college == null) return;
                String cName = CollegeIdentityHelper.formatCollegeName(college.getName());
                String cAddr = CollegeIdentityHelper.formatVerifiedAddress(college.getAddress(), cName);
                String cCode = college.getCode() != null ? college.getCode().toUpperCase() : "CAMPUS";

                if (tvSettingsCollegeName != null) tvSettingsCollegeName.setText("Institution: " + cName);
                if (tvSettingsCollegeLocation != null) tvSettingsCollegeLocation.setText("Campus Location: " + cAddr);
                if (tvSettingsCollegeCode != null) tvSettingsCollegeCode.setText("College Code: " + cCode);
                if (tvSettingsCollegeTenant != null) tvSettingsCollegeTenant.setText("Tenant ID: " + college.getId());
                if (tvSettingsCollegeContact != null) {
                    String contactEmail = college.getContactEmail() != null ? college.getContactEmail() : "admin@campus.edu";
                    String contactPhone = college.getContactPhone() != null ? college.getContactPhone() : "--";
                    tvSettingsCollegeContact.setText("Official Contact: " + contactEmail + " • " + contactPhone);
                }
            });
        }

        // ------------------------------------------------------------
        // CHANGE / RESET PASSWORD
        // ------------------------------------------------------------

        if (btnResetPassword != null) {
            btnResetPassword.setOnClickListener(v -> showPasswordOptionsDialog());
        }
    }

    private void showPasswordOptionsDialog() {
        String[] options = {
                "Change Password (Enter Current Password)",
                "Send Password Reset Link to Email",
                "Open Forgot Password Screen"
        };

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Password Security")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showChangePasswordDialog();
                    } else if (which == 1) {
                        String email = currentUser != null ? currentUser.getEmail() : null;
                        if (email == null || email.trim().isEmpty()) {
                            com.campusscheduler.ai.utils.UIUtils.showAlert(this, "Email Missing",
                                    "No registered email found for this session. Please use the Forgot Password screen.");
                        } else {
                            sendPasswordResetEmail(email.trim());
                        }
                    } else if (which == 2) {
                        Intent intent = new Intent(SettingsActivity.this, ForgotPasswordActivity.class);
                        startActivity(intent);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showChangePasswordDialog() {
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        android.widget.EditText etCurrentPassword = new android.widget.EditText(this);
        etCurrentPassword.setHint("Current Password");
        etCurrentPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etCurrentPassword);

        android.widget.EditText etNewPassword = new android.widget.EditText(this);
        etNewPassword.setHint("New Password (min 6 characters)");
        etNewPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etNewPassword);

        android.widget.EditText etConfirmPassword = new android.widget.EditText(this);
        etConfirmPassword.setHint("Confirm New Password");
        etConfirmPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etConfirmPassword);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Change Password")
                .setView(layout)
                .setPositiveButton("UPDATE PASSWORD", (dialog, which) -> {
                    String current = etCurrentPassword.getText().toString().trim();
                    String newPass = etNewPassword.getText().toString().trim();
                    String confirm = etConfirmPassword.getText().toString().trim();

                    if (current.isEmpty() || newPass.isEmpty()) {
                        com.campusscheduler.ai.utils.UIUtils.showToast(this, "All password fields required");
                        return;
                    }

                    if (newPass.length() < 6) {
                        com.campusscheduler.ai.utils.UIUtils.showToast(this, "New password must be at least 6 characters");
                        return;
                    }

                    if (!newPass.equals(confirm)) {
                        com.campusscheduler.ai.utils.UIUtils.showToast(this, "New passwords do not match");
                        return;
                    }

                    reauthenticateAndChangePassword(current, newPass);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void reauthenticateAndChangePassword(String currentPassword, String newPassword) {
        com.google.firebase.auth.FirebaseUser user = com.campusscheduler.ai.utils.FirebaseHelper.getAuth().getCurrentUser();
        String email = currentUser != null && currentUser.getEmail() != null ? currentUser.getEmail() : (user != null ? user.getEmail() : null);

        if (user == null || email == null) {
            com.campusscheduler.ai.utils.UIUtils.showAlert(this, "Session Notice",
                    "Direct password update requires an active Firebase Auth login.\n\nA password reset email will be sent to your registered email instead.");
            if (email != null) {
                sendPasswordResetEmail(email);
            }
            return;
        }

        android.app.ProgressDialog pd = com.campusscheduler.ai.utils.UIUtils.showLoadingDialog(this, "Reauthenticating & updating password...");

        com.google.firebase.auth.AuthCredential credential =
                com.google.firebase.auth.EmailAuthProvider.getCredential(email, currentPassword);

        user.reauthenticate(credential)
                .addOnSuccessListener(aVoid -> {
                    user.updatePassword(newPassword)
                            .addOnSuccessListener(aVoid2 -> {
                                pd.dismiss();
                                com.campusscheduler.ai.utils.UIUtils.showAlert(this, "Password Updated",
                                        "Your account password has been successfully updated in Firebase Authentication!");
                            })
                            .addOnFailureListener(e -> {
                                pd.dismiss();
                                com.campusscheduler.ai.utils.UIUtils.showAlert(this, "Update Failed",
                                        "Could not update password: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    pd.dismiss();
                    com.campusscheduler.ai.utils.UIUtils.showAlert(this, "Reauthentication Failed",
                            "Current password was incorrect. Please check and try again, or use 'Send Password Reset Link'.");
                });
    }

    private void sendPasswordResetEmail(String email) {
        android.app.ProgressDialog pd = com.campusscheduler.ai.utils.UIUtils.showLoadingDialog(this, "Sending reset link to " + email + "...");

        com.campusscheduler.ai.utils.FirebaseHelper.getAuth().sendPasswordResetEmail(email)
                .addOnSuccessListener(aVoid -> {
                    pd.dismiss();
                    com.campusscheduler.ai.utils.UIUtils.showAlert(this, "Reset Link Sent",
                            "A real password reset link has been dispatched to " + email + ".\nPlease follow the link in your email to choose a new password.");
                })
                .addOnFailureListener(e -> {
                    pd.dismiss();
                    com.campusscheduler.ai.utils.UIUtils.showAlert(this, "Reset Failed",
                            "Unable to send password reset email: " + e.getMessage());
                });
    }

    // ------------------------------------------------------------
    // BACK BUTTON
    // ------------------------------------------------------------

    @Override
    public void onBackPressed() {
        finish();
    }
}