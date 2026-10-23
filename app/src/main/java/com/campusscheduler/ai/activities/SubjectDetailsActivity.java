package com.campusscheduler.ai.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.campusscheduler.ai.R;
import com.campusscheduler.ai.adapters.GenericAdapter;
import com.campusscheduler.ai.models.Assignment;
import com.campusscheduler.ai.models.Quiz;
import com.campusscheduler.ai.models.ResourceItem;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.utils.FirebaseHelper;
import com.campusscheduler.ai.utils.LocalAuthManager;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class SubjectDetailsActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvSubjectHeaderTitle, tvSubjectName, tvSubjectCode, tvTeacher, tvDepartment, tvSemester, tvCredits, tvClassType;
    private TextView tvSectionTitle, tvEmptyState;
    private Button tabOverview, tabSyllabus, tabCourseWork, tabResources, tabAssignments, tabQuizzes;
    private LinearLayout layoutOverviewSection;
    private ProgressBar progressBar;
    private RecyclerView rvSectionItems;

    private User currentUser;
    private String subjectId = "";
    private String subjectName = "Subject Details";
    private String subjectCode = "SUB-101";
    private String teacherName = "Faculty Member";

    private List<String> displayList = new ArrayList<>();
    private GenericAdapter<String> adapter;

    private int activeTab = 1; // 1: Overview, 2: Syllabus, 3: Course Work, 4: Resources, 5: Assignments, 6: Quizzes

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subject_details);

        currentUser = LocalAuthManager.getCurrentLocalUser(this);

        if (getIntent() != null) {
            if (getIntent().hasExtra("subjectId")) subjectId = getIntent().getStringExtra("subjectId");
            if (getIntent().hasExtra("subjectName")) subjectName = getIntent().getStringExtra("subjectName");
            if (getIntent().hasExtra("subjectCode")) subjectCode = getIntent().getStringExtra("subjectCode");
            if (getIntent().hasExtra("teacherName")) teacherName = getIntent().getStringExtra("teacherName");
        }

        btnBack = findViewById(R.id.btnBack);
        tvSubjectHeaderTitle = findViewById(R.id.tvSubjectHeaderTitle);
        tvSubjectName = findViewById(R.id.tvSubjectName);
        tvSubjectCode = findViewById(R.id.tvSubjectCode);
        tvTeacher = findViewById(R.id.tvTeacher);
        tvDepartment = findViewById(R.id.tvDepartment);
        tvSemester = findViewById(R.id.tvSemester);
        tvCredits = findViewById(R.id.tvCredits);
        tvClassType = findViewById(R.id.tvClassType);

        tvSectionTitle = findViewById(R.id.tvSectionTitle);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        layoutOverviewSection = findViewById(R.id.layoutOverviewSection);
        progressBar = findViewById(R.id.progressBar);

        tabOverview = findViewById(R.id.tabOverview);
        tabSyllabus = findViewById(R.id.tabSyllabus);
        tabCourseWork = findViewById(R.id.tabCourseWork);
        tabResources = findViewById(R.id.tabResources);
        tabAssignments = findViewById(R.id.tabAssignments);
        tabQuizzes = findViewById(R.id.tabQuizzes);

        rvSectionItems = findViewById(R.id.rvSectionItems);

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        if (tvSubjectHeaderTitle != null) tvSubjectHeaderTitle.setText(subjectName);
        if (tvSubjectName != null) tvSubjectName.setText(subjectName);
        if (tvSubjectCode != null) tvSubjectCode.setText("Subject Code: " + (subjectCode != null ? subjectCode : "SUB-101"));
        if (tvTeacher != null) tvTeacher.setText("Teacher: " + (teacherName != null ? teacherName : "Faculty Member"));
        if (tvDepartment != null) tvDepartment.setText("Department: " + (currentUser != null && currentUser.getDepartmentName() != null ? currentUser.getDepartmentName() : "Computer Science and Engineering"));
        if (tvSemester != null) tvSemester.setText("Semester: " + (currentUser != null && currentUser.getSemester() != null ? currentUser.getSemester() : "Semester 5"));
        if (tvCredits != null) tvCredits.setText("Credits: 4.0");
        if (tvClassType != null) tvClassType.setText("Class Type: Theory / Core Academic");

        if (rvSectionItems != null) {
            rvSectionItems.setLayoutManager(new LinearLayoutManager(this));
            adapter = new GenericAdapter<>(displayList, (item, holder) -> {
                holder.setText(R.id.tvTitle, item);
                holder.setText(R.id.tvSubtitle, "Subject Record Item");
            }, R.layout.item_generic);
            rvSectionItems.setAdapter(adapter);
        }

        setupTabClickListeners();
        selectTab(1); // Default Overview
    }

    private void setupTabClickListeners() {
        if (tabOverview != null) tabOverview.setOnClickListener(v -> selectTab(1));
        if (tabSyllabus != null) tabSyllabus.setOnClickListener(v -> selectTab(2));
        if (tabCourseWork != null) tabCourseWork.setOnClickListener(v -> selectTab(3));
        if (tabResources != null) tabResources.setOnClickListener(v -> selectTab(4));
        if (tabAssignments != null) tabAssignments.setOnClickListener(v -> selectTab(5));
        if (tabQuizzes != null) tabQuizzes.setOnClickListener(v -> selectTab(6));
    }

    private void selectTab(int tabIndex) {
        activeTab = tabIndex;
        resetTabColors();

        switch (tabIndex) {
            case 1:
                if (tabOverview != null) tabOverview.setTextColor(Color.WHITE);
                if (layoutOverviewSection != null) layoutOverviewSection.setVisibility(View.VISIBLE);
                if (tvSectionTitle != null) tvSectionTitle.setText("Subject Overview");
                loadOverviewData();
                break;
            case 2:
                if (tabSyllabus != null) tabSyllabus.setTextColor(Color.WHITE);
                if (layoutOverviewSection != null) layoutOverviewSection.setVisibility(View.GONE);
                if (tvSectionTitle != null) tvSectionTitle.setText("Syllabus");
                loadSyllabusData();
                break;
            case 3:
                if (tabCourseWork != null) tabCourseWork.setTextColor(Color.WHITE);
                if (layoutOverviewSection != null) layoutOverviewSection.setVisibility(View.GONE);
                if (tvSectionTitle != null) tvSectionTitle.setText("Course Work");
                loadCourseWorkData();
                break;
            case 4:
                if (tabResources != null) tabResources.setTextColor(Color.WHITE);
                if (layoutOverviewSection != null) layoutOverviewSection.setVisibility(View.GONE);
                if (tvSectionTitle != null) tvSectionTitle.setText("Resources / Study Material");
                loadResourcesData();
                break;
            case 5:
                if (tabAssignments != null) tabAssignments.setTextColor(Color.WHITE);
                if (layoutOverviewSection != null) layoutOverviewSection.setVisibility(View.GONE);
                if (tvSectionTitle != null) tvSectionTitle.setText("Assignments");
                loadAssignmentsData();
                break;
            case 6:
                if (tabQuizzes != null) tabQuizzes.setTextColor(Color.WHITE);
                if (layoutOverviewSection != null) layoutOverviewSection.setVisibility(View.GONE);
                if (tvSectionTitle != null) tvSectionTitle.setText("Quizzes & Assessments");
                loadQuizzesData();
                break;
        }
    }

    private void resetTabColors() {
        int defaultColor = Color.parseColor("#E0D0FF");
        if (tabOverview != null) tabOverview.setTextColor(defaultColor);
        if (tabSyllabus != null) tabSyllabus.setTextColor(defaultColor);
        if (tabCourseWork != null) tabCourseWork.setTextColor(defaultColor);
        if (tabResources != null) tabResources.setTextColor(defaultColor);
        if (tabAssignments != null) tabAssignments.setTextColor(defaultColor);
        if (tabQuizzes != null) tabQuizzes.setTextColor(defaultColor);
    }

    private void loadOverviewData() {
        displayList.clear();
        displayList.add("Module 1: Fundamental Concepts & Architecture");
        displayList.add("Module 2: Process Management & Scheduling Algorithms");
        displayList.add("Module 3: Memory Management & Virtual Memory");
        displayList.add("Module 4: Storage & File Systems");
        displayList.add("Module 5: Security & Protection");
        if (adapter != null) adapter.notifyDataSetChanged();
        if (tvEmptyState != null) tvEmptyState.setVisibility(View.GONE);
    }

    private void loadSyllabusData() {
        displayList.clear();
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        FirebaseHelper.getFirestore().collection("syllabi")
                .whereEqualTo("subjectName", subjectName)
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (querySnapshots != null && !querySnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : querySnapshots) {
                            String title = doc.getString("title");
                            if (title != null) displayList.add(title);
                        }
                    }

                    if (displayList.isEmpty()) {
                        showEmptyState("No syllabus available for this subject.");
                    } else {
                        if (tvEmptyState != null) tvEmptyState.setVisibility(View.GONE);
                        if (adapter != null) adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    showEmptyState("No syllabus available for this subject.");
                });
    }

    private void loadCourseWorkData() {
        displayList.clear();
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        FirebaseHelper.getAssignmentsRef()
                .whereEqualTo("subjectName", subjectName)
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (querySnapshots != null && !querySnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : querySnapshots) {
                            Assignment a = doc.toObject(Assignment.class);
                            if (a != null && a.getTitle() != null) {
                                displayList.add(a.getTitle() + " (Due: " + a.getDueDate() + ")");
                            }
                        }
                    }

                    if (displayList.isEmpty()) {
                        showEmptyState("No coursework published for this subject.");
                    } else {
                        if (tvEmptyState != null) tvEmptyState.setVisibility(View.GONE);
                        if (adapter != null) adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    showEmptyState("No coursework published for this subject.");
                });
    }

    private void loadResourcesData() {
        displayList.clear();
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        FirebaseHelper.getResourcesRef()
                .whereEqualTo("subjectName", subjectName)
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (querySnapshots != null && !querySnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : querySnapshots) {
                            ResourceItem r = doc.toObject(ResourceItem.class);
                            if (r != null && r.getTitle() != null) {
                                displayList.add(r.getTitle() + " [" + (r.getResourceType() != null ? r.getResourceType() : "Resource") + "]");
                            }
                        }
                    }

                    if (displayList.isEmpty()) {
                        showEmptyState("No study resources uploaded for this subject.");
                    } else {
                        if (tvEmptyState != null) tvEmptyState.setVisibility(View.GONE);
                        if (adapter != null) adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    showEmptyState("No study resources uploaded for this subject.");
                });
    }

    private void loadAssignmentsData() {
        displayList.clear();
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        FirebaseHelper.getAssignmentsRef()
                .whereEqualTo("subjectName", subjectName)
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (querySnapshots != null && !querySnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : querySnapshots) {
                            Assignment a = doc.toObject(Assignment.class);
                            if (a != null && a.getTitle() != null) {
                                displayList.add("Assignment: " + a.getTitle() + " | Deadline: " + a.getDueDate());
                            }
                        }
                    }

                    if (displayList.isEmpty()) {
                        showEmptyState("No assignments assigned for this subject.");
                    } else {
                        if (tvEmptyState != null) tvEmptyState.setVisibility(View.GONE);
                        if (adapter != null) adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    showEmptyState("No assignments assigned for this subject.");
                });
    }

    private void loadQuizzesData() {
        displayList.clear();
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        FirebaseHelper.getQuizzesRef()
                .whereEqualTo("subjectName", subjectName)
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (querySnapshots != null && !querySnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : querySnapshots) {
                            Quiz q = doc.toObject(Quiz.class);
                            if (q != null && q.getTitle() != null) {
                                displayList.add("Quiz: " + q.getTitle() + " (" + q.getDurationMinutes() + " mins)");
                            }
                        }
                    }

                    if (displayList.isEmpty()) {
                        showEmptyState("No quizzes published for this subject.");
                    } else {
                        if (tvEmptyState != null) tvEmptyState.setVisibility(View.GONE);
                        if (adapter != null) adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    showEmptyState("No quizzes published for this subject.");
                });
    }

    private void showEmptyState(String message) {
        displayList.clear();
        if (adapter != null) adapter.notifyDataSetChanged();
        if (tvEmptyState != null) {
            tvEmptyState.setVisibility(View.VISIBLE);
            tvEmptyState.setText(message);
        }
    }
}
