package com.campusscheduler.ai.activities;

import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.campusscheduler.ai.R;
import com.campusscheduler.ai.adapters.GenericAdapter;
import com.campusscheduler.ai.models.Assignment;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.utils.FirebaseHelper;
import com.campusscheduler.ai.utils.LocalAuthManager;

import java.util.ArrayList;
import java.util.List;

public class CourseWorkActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private RecyclerView rvCourseWork;
    private GenericAdapter<Assignment> adapter;
    private List<Assignment> courseWorkList = new ArrayList<>();
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_work);

        currentUser = LocalAuthManager.getCurrentLocalUser(this);

        btnBack = findViewById(R.id.btnBack);
        rvCourseWork = findViewById(R.id.rvCourseWork);

        btnBack.setOnClickListener(v -> finish());

        rvCourseWork.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GenericAdapter<>(courseWorkList, (item, holder) -> {
            holder.setText(R.id.tvTitle, item.getTitle() + " (" + item.getSubjectName() + ")");
            holder.setText(R.id.tvSubtitle, "Due Date: " + item.getDueDate() + " | Deadline Policy: " + (item.isAllowLateSubmission() ? "Late Allowed" : "Strict Submission Closed"));
        }, R.layout.item_generic);
        rvCourseWork.setAdapter(adapter);

        loadCourseWork();
    }

    private void loadCourseWork() {
        String collegeId = currentUser != null ? currentUser.getCollegeId() : null;
        FirebaseHelper.getAssignmentsRef().whereEqualTo("collegeId", collegeId).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    courseWorkList.clear();
                    for (var doc : queryDocumentSnapshots) {
                        Assignment a = doc.toObject(Assignment.class);
                        if (a != null) courseWorkList.add(a);
                    }
                    adapter.notifyDataSetChanged();
                });
    }
}
