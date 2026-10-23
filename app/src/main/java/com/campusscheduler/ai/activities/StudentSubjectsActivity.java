package com.campusscheduler.ai.activities;

import android.content.Intent;
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
import com.campusscheduler.ai.models.Subject;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.utils.FirebaseHelper;
import com.campusscheduler.ai.utils.LocalAuthManager;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class StudentSubjectsActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private ProgressBar progressBar;
    private TextView tvEmptyState;
    private RecyclerView rvSubjects;
    private GenericAdapter<Subject> adapter;
    private List<Subject> subjectList = new ArrayList<>();
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_subjects);

        currentUser = LocalAuthManager.getCurrentLocalUser(this);

        btnBack = findViewById(R.id.btnBack);
        progressBar = findViewById(R.id.progressBar);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        rvSubjects = findViewById(R.id.rvSubjects);

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        if (rvSubjects != null) {
            rvSubjects.setLayoutManager(new LinearLayoutManager(this));
            adapter = new GenericAdapter<>(subjectList, (item, holder) -> {
                holder.setText(R.id.tvTitle, item.getName() != null ? item.getName() : "Subject");
                String code = item.getCode() != null ? item.getCode() : "SUB-101";
                String type = item.getType() != null ? item.getType() : (item.isLab() ? "Lab" : "Theory");
                int credits = item.getCredits() > 0 ? item.getCredits() : 4;
                holder.setText(R.id.tvSubtitle, "Code: " + code + " | Type: " + type + " | Credits: " + credits);
            }, R.layout.item_generic);

            rvSubjects.setAdapter(adapter);
        }

        // Handle item click to launch SubjectDetailsActivity
        if (rvSubjects != null) {
            rvSubjects.addOnItemTouchListener(new RecyclerItemClickListener(this, rvSubjects, (view, position) -> {
                if (position >= 0 && position < subjectList.size()) {
                    Subject selected = subjectList.get(position);
                    Intent intent = new Intent(StudentSubjectsActivity.this, SubjectDetailsActivity.class);
                    intent.putExtra("subjectId", selected.getId());
                    intent.putExtra("subjectName", selected.getName());
                    intent.putExtra("subjectCode", selected.getCode());
                    startActivity(intent);
                }
            }));
        }

        loadStudentSubjects();
    }

    private void loadStudentSubjects() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (tvEmptyState != null) tvEmptyState.setVisibility(View.GONE);

        String collegeId = currentUser != null ? currentUser.getCollegeId() : null;

        FirebaseHelper.getSubjectsRef()
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    subjectList.clear();

                    if (querySnapshots != null && !querySnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : querySnapshots) {
                            Subject s = doc.toObject(Subject.class);
                            if (s != null) {
                                String cId = s.getCollegeId();
                                if (collegeId == null || cId == null || cId.isEmpty() || cId.equalsIgnoreCase(collegeId)) {
                                    subjectList.add(s);
                                }
                            }
                        }
                    }

                    if (subjectList.isEmpty()) {
                        if (tvEmptyState != null) {
                            tvEmptyState.setVisibility(View.VISIBLE);
                            tvEmptyState.setText("No subjects assigned for your current academic context.");
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
                        tvEmptyState.setText("No subjects assigned for your current academic context.");
                    }
                    subjectList.clear();
                    if (adapter != null) adapter.notifyDataSetChanged();
                });
    }

    // Simple Item Click Listener helper for RecyclerView
    private static class RecyclerItemClickListener implements RecyclerView.OnItemTouchListener {
        interface OnItemClickListener {
            void onItemClick(View view, int position);
        }

        private OnItemClickListener mListener;
        private android.view.GestureDetector mGestureDetector;

        public RecyclerItemClickListener(android.content.Context context, final RecyclerView recyclerView, OnItemClickListener listener) {
            mListener = listener;
            mGestureDetector = new android.view.GestureDetector(context, new android.view.GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onSingleTapUp(android.view.MotionEvent e) {
                    return true;
                }
            });
        }

        @Override
        public boolean onInterceptTouchEvent(RecyclerView view, android.view.MotionEvent e) {
            View childView = view.findChildViewUnder(e.getX(), e.getY());
            if (childView != null && mListener != null && mGestureDetector.onTouchEvent(e)) {
                mListener.onItemClick(childView, view.getChildAdapterPosition(childView));
                return true;
            }
            return false;
        }

        @Override public void onTouchEvent(RecyclerView view, android.view.MotionEvent e) {}
        @Override public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {}
    }
}
