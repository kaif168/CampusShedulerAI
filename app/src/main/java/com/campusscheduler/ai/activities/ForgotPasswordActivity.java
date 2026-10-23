package com.campusscheduler.ai.activities;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.campusscheduler.ai.R;
import com.campusscheduler.ai.utils.FirebaseHelper;
import com.campusscheduler.ai.utils.UIUtils;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etResetEmail;
    private Button btnResetPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        etResetEmail = findViewById(R.id.etResetEmail);
        btnResetPassword = findViewById(R.id.btnResetPassword);

        btnResetPassword.setOnClickListener(v -> performPasswordReset());
    }

    private void performPasswordReset() {
        String email = etResetEmail.getText() != null ? etResetEmail.getText().toString().trim() : "";
        if (email.isEmpty()) {
            UIUtils.showToast(this, "Please enter your registered email address (personal or institutional)");
            return;
        }

        ProgressDialog pd = UIUtils.showLoadingDialog(this, "Sending Password Reset Email via Firebase Auth...");

        FirebaseHelper.getAuth().sendPasswordResetEmail(email)
                .addOnSuccessListener(aVoid -> {
                    pd.dismiss();
                    UIUtils.showAlert(this, "Password Reset Sent",
                            "A real password reset link has been sent to " + email + ". Please check your inbox and follow the link to set a new password, then log in using your new credentials.");
                })
                .addOnFailureListener(e -> {
                    pd.dismiss();
                    UIUtils.showAlert(this, "Reset Request Failed",
                            "Unable to send password reset email: " + e.getMessage());
                });
    }
}
