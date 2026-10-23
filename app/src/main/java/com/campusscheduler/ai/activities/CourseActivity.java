package com.campusscheduler.ai.activities;

import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.campusscheduler.ai.R;
import com.campusscheduler.ai.adapters.GenericAdapter;
import com.campusscheduler.ai.models.Subject;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.repository.AcademicRepository;
import com.campusscheduler.ai.utils.LocalAuthManager;

import java.util.ArrayList;
import java.util.List;

public class CourseActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private RecyclerView rvCourses;
    private GenericAdapter<Subject> adapter;
    private List<Subject> courseList = new ArrayList<>();
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course);

        currentUser = LocalAuthManager.getCurrentLocalUser(this);

        btnBack = findViewById(R.id.btnBack);
        rvCourses = findViewById(R.id.rvCourses);

        btnBack.setOnClickListener(v -> finish());

        rvCourses.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GenericAdapter<>(courseList, (item, holder) -> {
            holder.setText(R.id.tvTitle, item.getSubjectName() + " (" + (item.getSubjectCode() != null ? item.getSubjectCode() : "SUB") + ")");
            holder.setText(R.id.tvSubtitle, "Type: " + (item.getType() != null ? item.getType() : "Theory") + " | Credits: " + item.getCredits() + " | Weekly Periods: " + item.getWeeklyPeriods());
        }, R.layout.item_generic);
        rvCourses.setAdapter(adapter);

        loadCourses();
    }

    private void loadCourses() {
        String collegeId = currentUser != null ? currentUser.getCollegeId() : null;
        AcademicRepository.getInstance().getSubjects(collegeId, task -> {
            courseList.clear();
            if (task.isSuccessful() && task.getResult() != null) {
                for (var doc : task.getResult()) {
                    Subject s = doc.toObject(Subject.class);
                    if (s != null) courseList.add(s);
                }
            }
            if (courseList.isEmpty()) {
                Subject s1 = new Subject("SUB_1", "Operating Systems", "18CS61", 4, false);
                Subject s2 = new Subject("SUB_2", "System Software Lab", "18CSL67", 2, true);
                courseList.add(s1);
                courseList.add(s2);
            }
            adapter.notifyDataSetChanged();
        });
    }
}
