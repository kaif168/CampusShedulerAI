package com.campusscheduler.ai.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.campusscheduler.ai.R;
import com.campusscheduler.ai.adapters.GenericAdapter;
import com.campusscheduler.ai.models.Exam;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.utils.FirebaseHelper;
import com.campusscheduler.ai.utils.LocalAuthManager;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class StudentExamActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private ProgressBar progressBar;
    private TextView tvEmptyState;
    private RecyclerView rvStudentExams;
    private GenericAdapter<Exam> adapter;
    private List<Exam> examList = new ArrayList<>();
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_exam);

        currentUser = LocalAuthManager.getCurrentLocalUser(this);

        btnBack = findViewById(R.id.btnBack);
        progressBar = findViewById(R.id.progressBar);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        rvStudentExams = findViewById(R.id.rvStudentExams);

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        if (rvStudentExams != null) {
            rvStudentExams.setLayoutManager(new LinearLayoutManager(this));
            adapter = new GenericAdapter<>(examList, (item, holder) -> {
                String sub = item.getSubjectName() != null ? item.getSubjectName() : "Subject";
                String type = item.getExamType() != null ? item.getExamType() : "Examination / IA";
                String date = item.getExamDate() != null ? item.getExamDate() : "TBD";
                String start = item.getStartTime() != null ? item.getStartTime() : "TBD";
                String end = item.getEndTime() != null ? item.getEndTime() : "TBD";
                String room = item.getRoomNumber() != null ? item.getRoomNumber() : "TBD";

                holder.setText(R.id.tvTitle, sub + " (" + type + ")");
                holder.setText(R.id.tvSubtitle, "Date: " + date + " (" + start + " - " + end + ") | Hall/Room: " + room);
            }, R.layout.item_generic);
            rvStudentExams.setAdapter(adapter);
        }

        loadExams();
    }

    private void loadExams() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (tvEmptyState != null) tvEmptyState.setVisibility(View.GONE);

        String collegeId = currentUser != null ? currentUser.getCollegeId() : null;

        FirebaseHelper.getExamsRef().get()
                .addOnSuccessListener(querySnapshots -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    examList.clear();

                    if (querySnapshots != null && !querySnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : querySnapshots) {
                            Exam e = doc.toObject(Exam.class);
                            if (e != null) {
                                String cId = e.getCollegeId();
                                if (collegeId == null || cId == null || cId.isEmpty() || cId.equalsIgnoreCase(collegeId)) {
                                    examList.add(e);
                                }
                            }
                        }
                    }

                    if (examList.isEmpty()) {
                        if (tvEmptyState != null) {
                            tvEmptyState.setVisibility(View.VISIBLE);
                            tvEmptyState.setText("No exam or IA timetable published for your section.");
                        }
                    } else {
                        if (tvEmptyState != null) tvEmptyState.setVisibility(View.GONE);
                    }

                    if (adapter != null) adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (tvEmptyState != null) {
                        tvEmptyState.setVisibility(View.VISIBLE);
                        tvEmptyState.setText("No exam or IA timetable published for your section.");
                    }
                    examList.clear();
                    if (adapter != null) adapter.notifyDataSetChanged();
                });
    }
}
