package com.campusscheduler.ai.activities;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.campusscheduler.ai.R;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.utils.LocalAuthManager;

public class AdmissionTypeActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvAdmissionCategory, tvStudentUsn, tvProgramDegree, tvCollegeAffiliation, tvQuotaStatus;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admission_type);

        currentUser = LocalAuthManager.getCurrentLocalUser(this);

        btnBack = findViewById(R.id.btnBack);
        tvAdmissionCategory = findViewById(R.id.tvAdmissionCategory);
        tvStudentUsn = findViewById(R.id.tvStudentUsn);
        tvProgramDegree = findViewById(R.id.tvProgramDegree);
        tvCollegeAffiliation = findViewById(R.id.tvCollegeAffiliation);
        tvQuotaStatus = findViewById(R.id.tvQuotaStatus);

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        if (currentUser != null) {
            if (tvAdmissionCategory != null) {
                tvAdmissionCategory.setText("Admission Category: " + (currentUser.getAdmissionType() != null ? currentUser.getAdmissionType() : "Regular CET / Merit"));
            }
            if (tvStudentUsn != null) {
                tvStudentUsn.setText("Student USN / Roll No: " + (currentUser.getUsn() != null ? currentUser.getUsn() : currentUser.getUid()));
            }
            if (tvProgramDegree != null) {
                tvProgramDegree.setText("Program: " + (currentUser.getDegree() != null ? currentUser.getDegree() : "B.E. Computer Science Engineering"));
            }
            if (tvCollegeAffiliation != null) {
                tvCollegeAffiliation.setText("Institution: " + (currentUser.getCollegeName() != null ? currentUser.getCollegeName() : "CampusSchedulerAI Institution"));
            }
            if (tvQuotaStatus != null) {
                tvQuotaStatus.setText("Quota & Status: Confirmed Allotment | Active Student Record");
            }
        }
    }
}
