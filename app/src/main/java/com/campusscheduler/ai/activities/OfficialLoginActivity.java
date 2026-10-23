package com.campusscheduler.ai.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.campusscheduler.ai.R;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.utils.FirebaseHelper;
import com.campusscheduler.ai.utils.LocalAuthManager;
import com.campusscheduler.ai.utils.UIUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

public class OfficialLoginActivity extends AppCompatActivity {

    private EditText etEmail;
    private EditText etPassword;
    private Button btnOfficialAuth;

    private FirebaseAuth firebaseAuth;

    private static final String OFFICIAL_OWNERS_COLLECTION =
            "officialOwners";

    private static final String GLOBAL_SYSTEM_OWNER =
            "GLOBAL_SYSTEM_OWNER";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_official_login);

        firebaseAuth = FirebaseAuth.getInstance();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnOfficialAuth = findViewById(R.id.btnOfficialAuth);

        if (btnOfficialAuth != null) {
            btnOfficialAuth.setOnClickListener(
                    v -> performOfficialLogin()
            );
        }
    }

    /**
     * ============================================================
     * OFFICIAL OWNER LOGIN
     * ============================================================
     *
     * All Official Owners must authenticate through
     * Firebase Authentication.
     *
     * No password is stored or hardcoded in this application.
     */
    private void performOfficialLogin() {

        String email =
                etEmail.getText() != null
                        ? etEmail.getText().toString().trim()
                        : "";

        String password =
                etPassword.getText() != null
                        ? etPassword.getText().toString().trim()
                        : "";

        if (TextUtils.isEmpty(email)) {

            UIUtils.showToast(
                    this,
                    "Official Owner Email is required"
            );

            return;
        }

        if (TextUtils.isEmpty(password)) {

            UIUtils.showToast(
                    this,
                    "Official Owner Password is required"
            );

            return;
        }

        /*
         * Disable button while authentication is running.
         * This prevents accidental multiple login requests.
         */
        if (btnOfficialAuth != null) {
            btnOfficialAuth.setEnabled(false);
        }

        firebaseAuth
                .signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {

                    FirebaseUser firebaseUser =
                            authResult.getUser();

                    if (firebaseUser == null) {

                        restoreLoginButton();

                        UIUtils.showToast(
                                this,
                                "Authentication failed: user session missing."
                        );

                        return;
                    }

                    checkOfficialOwnerAuthorization(firebaseUser);

                })
                .addOnFailureListener(error -> {

                    restoreLoginButton();

                    UIUtils.showAlert(
                            this,
                            "Login Failed",
                            getFirebaseLoginError(error)
                    );
                });
    }

    /**
     * ============================================================
     * OFFICIAL OWNER AUTHORIZATION
     * ============================================================
     *
     * Firebase Authentication proves the identity.
     *
     * Firestore officialOwners collection proves that
     * the authenticated account is actually authorized
     * as an Official Owner.
     */
    private void checkOfficialOwnerAuthorization(
            FirebaseUser firebaseUser
    ) {

        String uid = firebaseUser.getUid();

        String email = firebaseUser.getEmail();

        if (email == null || email.trim().isEmpty()) {

            firebaseAuth.signOut();

            restoreLoginButton();

            UIUtils.showAlert(
                    this,
                    "Access Denied",
                    "Firebase account email could not be verified."
            );

            return;
        }

        /*
         * Primary lookup:
         *
         * officialOwners/{firebaseUid}
         *
         * This is the preferred and most secure structure.
         */
        FirebaseHelper.getFirestore()
                .collection(OFFICIAL_OWNERS_COLLECTION)
                .document(uid)
                .get()
                .addOnSuccessListener(ownerDocument -> {

                    if (ownerDocument.exists()) {

                        processOwnerDocument(
                                ownerDocument,
                                firebaseUser
                        );

                    } else {

                        /*
                         * Temporary compatibility lookup by email.
                         *
                         * This keeps the existing Owner Access
                         * implementation working if an owner document
                         * was created without Firebase UID as document ID.
                         */
                        checkOwnerByEmail(
                                firebaseUser,
                                email.trim().toLowerCase()
                        );
                    }

                })
                .addOnFailureListener(error -> {

                    firebaseAuth.signOut();

                    restoreLoginButton();

                    UIUtils.showAlert(
                            this,
                            "Owner Verification Failed",
                            "Could not verify Official Owner access.\n\n"
                                    + getSafeErrorMessage(error)
                    );
                });
    }

    /**
     * ============================================================
     * TEMPORARY EMAIL FALLBACK
     * ============================================================
     *
     * Used only for compatibility with the existing
     * officialOwners records.
     *
     * Later, all officialOwners documents should use
     * Firebase UID as the document ID.
     */
    private void checkOwnerByEmail(
            FirebaseUser firebaseUser,
            String normalizedEmail
    ) {

        FirebaseHelper.getFirestore()
                .collection(OFFICIAL_OWNERS_COLLECTION)
                .whereEqualTo(
                        "email",
                        normalizedEmail
                )
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    if (querySnapshot.isEmpty()) {

                        firebaseAuth.signOut();

                        restoreLoginButton();

                        UIUtils.showAlert(
                                this,
                                "Access Denied",
                                "This Firebase account has not been "
                                        + "authorized as an Official Owner."
                        );

                        return;
                    }

                    DocumentSnapshot ownerDocument =
                            querySnapshot
                                    .getDocuments()
                                    .get(0);

                    processOwnerDocument(
                            ownerDocument,
                            firebaseUser
                    );

                })
                .addOnFailureListener(error -> {

                    firebaseAuth.signOut();

                    restoreLoginButton();

                    UIUtils.showAlert(
                            this,
                            "Owner Verification Failed",
                            "Could not verify Owner Access.\n\n"
                                    + getSafeErrorMessage(error)
                    );
                });
    }

    /**
     * ============================================================
     * PROCESS AUTHORIZED OWNER
     * ============================================================
     */
    private void processOwnerDocument(
            DocumentSnapshot document,
            FirebaseUser firebaseUser
    ) {

        Boolean active =
                document.getBoolean("active");

        String role =
                document.getString("role");

        String ownerEmail =
                document.getString("email");

        String ownerName =
                document.getString("name");

        /*
         * Owner must explicitly be active.
         */
        if (!Boolean.TRUE.equals(active)) {

            firebaseAuth.signOut();

            restoreLoginButton();

            UIUtils.showAlert(
                    this,
                    "Owner Access Disabled",
                    "Your Official Owner access is currently inactive.\n\n"
                            + "Please contact the authorized platform owner."
            );

            return;
        }

        /*
         * Owner role must be exactly OFFICIAL_OWNER.
         */
        if (!User.ROLE_OFFICIAL_OWNER.equals(role)) {

            firebaseAuth.signOut();

            restoreLoginButton();

            UIUtils.showAlert(
                    this,
                    "Access Denied",
                    "This account is not assigned the Official Owner role."
            );

            return;
        }

        /*
         * Use Firebase email if Firestore owner email is missing.
         */
        if (ownerEmail == null
                || ownerEmail.trim().isEmpty()) {

            ownerEmail =
                    firebaseUser.getEmail();
        }

        /*
         * Use Firebase display name if Firestore owner name
         * is missing.
         */
        if (ownerName == null
                || ownerName.trim().isEmpty()) {

            ownerName =
                    firebaseUser.getDisplayName();

            if (ownerName == null
                    || ownerName.trim().isEmpty()) {

                ownerName = "Official Owner";
            }
        }

        /*
         * Create local application session only AFTER
         * Firebase Authentication + Owner Authorization succeeds.
         */
        User officialOwner =
                new User();

        officialOwner.setUid(
                firebaseUser.getUid()
        );

        officialOwner.setName(
                ownerName
        );

        officialOwner.setEmail(
                ownerEmail
        );

        officialOwner.setRole(
                User.ROLE_OFFICIAL_OWNER
        );

        officialOwner.setCollegeId(
                GLOBAL_SYSTEM_OWNER
        );

        LocalAuthManager.saveUserSession(
                this,
                officialOwner
        );

        UIUtils.showToast(
                this,
                "Welcome, " + ownerName
        );

        openOwnerDashboard();
    }

    /**
     * ============================================================
     * OPEN OWNER DASHBOARD
     * ============================================================
     */
    private void openOwnerDashboard() {

        Intent intent =
                new Intent(
                        this,
                        OfficialOwnerDashboardActivity.class
                );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);

        finish();
    }

    /**
     * ============================================================
     * RESTORE LOGIN BUTTON
     * ============================================================
     */
    private void restoreLoginButton() {

        if (btnOfficialAuth != null) {
            btnOfficialAuth.setEnabled(true);
        }
    }

    /**
     * ============================================================
     * FIREBASE LOGIN ERROR
     * ============================================================
     */
    private String getFirebaseLoginError(
            Exception error
    ) {

        if (error == null
                || error.getMessage() == null) {

            return "Unable to sign in. Please check your credentials.";
        }

        String message =
                error.getMessage();

        String lower =
                message.toLowerCase();

        if (lower.contains("invalid credential")
                || lower.contains("invalid login credentials")
                || lower.contains("wrong-password")
                || lower.contains("invalid-email")) {

            return "Invalid Official Owner email or password.";
        }

        if (lower.contains("user-not-found")) {

            return "No Firebase Authentication account was found for this email.";
        }

        if (lower.contains("too-many-requests")) {

            return "Too many login attempts. Please try again later.";
        }

        if (lower.contains("network")) {

            return "Network error. Please check your internet connection.";
        }

        return "Unable to sign in. Please try again.";
    }

    /**
     * ============================================================
     * SAFE FIRESTORE ERROR MESSAGE
     * ============================================================
     */
    private String getSafeErrorMessage(
            Exception error
    ) {

        if (error == null
                || error.getMessage() == null) {

            return "Unknown verification error.";
        }

        return error.getMessage();
    }

    @Override
    protected void onDestroy() {

        restoreLoginButton();

        super.onDestroy();
    }
}