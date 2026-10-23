package com.campusscheduler.ai.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.campusscheduler.ai.R;
import com.campusscheduler.ai.models.User;

public class StudentProfileDialog extends Dialog {

    private User user;

    public StudentProfileDialog(@NonNull Context context, User user) {
        super(context);
        this.user = user;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_student_profile);

        if (getWindow() != null) {
            getWindow().setLayout(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        TextView tvName = findViewById(R.id.dialogStudentName);
        TextView tvUsn = findViewById(R.id.dialogStudentUsn);
        TextView tvClass = findViewById(R.id.dialogStudentClass);
        TextView tvBranch = findViewById(R.id.dialogStudentBranch);
        Button btnClose = findViewById(R.id.btnCloseProfile);

        if (user != null) {
            if (tvName != null && user.getName() != null) tvName.setText(user.getName().toUpperCase());
            if (tvUsn != null && user.getUid() != null) tvUsn.setText(user.getUid().toUpperCase());
            if (tvClass != null) tvClass.setText("4th year | Section G");
            if (tvBranch != null) tvBranch.setText("Computer Science and Engineering");
        }

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dismiss());
        }
    }
}
