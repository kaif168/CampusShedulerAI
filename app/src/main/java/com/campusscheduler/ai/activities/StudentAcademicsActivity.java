package com.campusscheduler.ai.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.campusscheduler.ai.R;
import com.campusscheduler.ai.adapters.StudentAttendanceAdapter;
import com.campusscheduler.ai.models.AttendanceRecord;
import com.campusscheduler.ai.models.Subject;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.utils.FirebaseHelper;
import com.campusscheduler.ai.utils.LocalAuthManager;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentAcademicsActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvCurrentTerm, tvExamResultsSummary, tvOverallPercent, tvAttendanceSummary, tvNoDataState;
    private Spinner spinnerSemester;
    private ProgressBar progressBar;
    private RecyclerView rvSubjectAttendance;
    private StudentAttendanceAdapter adapter;
    private List<StudentAttendanceAdapter.AttendanceItem> attendanceList = new ArrayList<>();
    private User currentUser;
    private int selectedSemester = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_academics);

        currentUser = LocalAuthManager.getCurrentLocalUser(this);
        String safeCollegeId = LocalAuthManager.getSafeCollegeId(this, getIntent());
        String studentUid = getIntent().getStringExtra("studentUid");
        if (currentUser == null) {
            currentUser = new User();
            currentUser.setCollegeId(safeCollegeId);
            currentUser.setDepartmentId("DEPT_CSE");
            currentUser.setRole("student");
            currentUser.setName("Student");
            if (studentUid != null) {
                currentUser.setUid(studentUid);
            } else if (FirebaseHelper.getAuth().getCurrentUser() != null) {
                currentUser.setUid(FirebaseHelper.getAuth().getCurrentUser().getUid());
            }
        } else {
            if (currentUser.getCollegeId() == null || currentUser.getCollegeId().trim().isEmpty()) {
                currentUser.setCollegeId(safeCollegeId);
            }
            if ((currentUser.getUid() == null || currentUser.getUid().trim().isEmpty()) && studentUid != null) {
                currentUser.setUid(studentUid);
            }
        }

        btnBack = findViewById(R.id.btnBack);
        tvCurrentTerm = findViewById(R.id.tvCurrentTerm);
        tvExamResultsSummary = findViewById(R.id.tvExamResultsSummary);
        tvOverallPercent = findViewById(R.id.tvOverallPercent);
        tvAttendanceSummary = findViewById(R.id.tvAttendanceSummary);
        tvNoDataState = findViewById(R.id.tvNoDataState);
        spinnerSemester = findViewById(R.id.spinnerSemester);
        progressBar = findViewById(R.id.progressBar);
        rvSubjectAttendance = findViewById(R.id.rvSubjectAttendance);

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        if (spinnerSemester != null) {
            String[] sems = {"Semester 1", "Semester 2", "Semester 3", "Semester 4", "Semester 5", "Semester 6", "Semester 7", "Semester 8"};
            ArrayAdapter<String> spinAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, sems);
            spinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerSemester.setAdapter(spinAdapter);

            int studentSemNum = 5;
            if (currentUser != null && currentUser.getSemester() != null) {
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)").matcher(currentUser.getSemester());
                if (m.find()) {
                    try {
                        studentSemNum = Integer.parseInt(m.group(1));
                    } catch (Exception ignored) {}
                }
            }
            int defaultIndex = (studentSemNum >= 1 && studentSemNum <= 8) ? (studentSemNum - 1) : 4;
            selectedSemester = defaultIndex + 1;
            spinnerSemester.setSelection(defaultIndex);
            spinnerSemester.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    selectedSemester = position + 1;
                    loadAcademicsData();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }

        if (rvSubjectAttendance != null) {
            rvSubjectAttendance.setLayoutManager(new LinearLayoutManager(this));
            adapter = new StudentAttendanceAdapter(attendanceList, item -> {
                if (item != null && item.subjectName != null) {
                    Intent intent = new Intent(StudentAcademicsActivity.this, SubjectDetailsActivity.class);
                    intent.putExtra("subjectName", item.subjectName);
                    startActivity(intent);
                }
            });
            rvSubjectAttendance.setAdapter(adapter);
        }

        loadAcademicsData();
    }

    private void loadAcademicsData() {
        if (currentUser == null) return;

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (tvNoDataState != null) tvNoDataState.setVisibility(View.GONE);

        if (tvCurrentTerm != null) {
            tvCurrentTerm.setText("Academic Term: 2026-2027 (Semester " + selectedSemester + ")");
        }

        // Fetch Exam Summary
        FirebaseHelper.getExamsRef().get()
                .addOnSuccessListener(querySnapshots -> {
                    if (querySnapshots != null && !querySnapshots.isEmpty()) {
                        int count = querySnapshots.size();
                        if (tvExamResultsSummary != null) {
                            tvExamResultsSummary.setText("Scheduled Examinations: " + count + " papers published in Firestore.");
                        }
                    } else {
                        if (tvExamResultsSummary != null) {
                            tvExamResultsSummary.setText("No exam results published for this term yet.");
                        }
                    }
                });

        // Fetch Real Attendance Records
        FirebaseHelper.getAttendanceRecordsRef().whereEqualTo("studentId", currentUser.getUid()).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    attendanceList.clear();

                    if (queryDocumentSnapshots == null || queryDocumentSnapshots.isEmpty()) {
                        showEmptyStateAndLoadSubjects();
                        return;
                    }

                    Map<String, int[]> subjectMap = new HashMap<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        AttendanceRecord rec = doc.toObject(AttendanceRecord.class);
                        if (rec != null) {
                            String sub = rec.getSubjectName() != null ? rec.getSubjectName() : "General Subject";
                            if (!subjectMap.containsKey(sub)) {
                                subjectMap.put(sub, new int[]{0, 0});
                            }
                            int[] stats = subjectMap.get(sub);
                            stats[1]++; // Total
                            if (rec.isPresent()) stats[0]++; // Attended
                        }
                    }

                    if (subjectMap.isEmpty()) {
                        showEmptyStateAndLoadSubjects();
                        return;
                    }

                    if (progressBar != null) progressBar.setVisibility(View.GONE);

                    int grandAttended = 0;
                    int grandTotal = 0;

                    for (Map.Entry<String, int[]> entry : subjectMap.entrySet()) {
                        String subName = entry.getKey();
                        int attended = entry.getValue()[0];
                        int total = entry.getValue()[1];
                        double pct = total > 0 ? (attended * 100.0) / total : 0.0;
                        grandAttended += attended;
                        grandTotal += total;

                        attendanceList.add(new StudentAttendanceAdapter.AttendanceItem(subName, pct, attended, total));
                    }

                    double overallPct = grandTotal > 0 ? (grandAttended * 100.0) / grandTotal : 0.0;

                    if (tvOverallPercent != null) {
                        tvOverallPercent.setText(String.format("%.1f%%", overallPct));
                        if (overallPct < 75.0) {
                            tvOverallPercent.setTextColor(0xFFC62828); // Red shortage
                        } else {
                            tvOverallPercent.setTextColor(0xFF2E7D32); // Green good standing
                        }
                    }

                    if (tvAttendanceSummary != null) {
                        String summary = grandAttended + " of " + grandTotal + " sessions attended";
                        if (overallPct < 75.0) {
                            int req = Math.max(3 * grandTotal - 4 * grandAttended, 1);
                            summary += " | SHORTAGE WARNING (<75%). Attend next " + req + " consecutive sessions.";
                        }
                        tvAttendanceSummary.setText(summary);
                    }

                    if (adapter != null) adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    showEmptyStateAndLoadSubjects();
                });
    }

    private void showEmptyStateAndLoadSubjects() {
        if (tvOverallPercent != null) {
            tvOverallPercent.setText("N/A");
            tvOverallPercent.setTextColor(0xFF757575);
        }
        if (tvAttendanceSummary != null) {
            tvAttendanceSummary.setText("No attendance records available");
        }

        FirebaseHelper.getSubjectsRef().get()
                .addOnSuccessListener(querySnapshots -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    attendanceList.clear();
                    if (querySnapshots != null && !querySnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : querySnapshots) {
                            Subject sub = doc.toObject(Subject.class);
                            if (sub != null && sub.getName() != null) {
                                attendanceList.add(new StudentAttendanceAdapter.AttendanceItem(sub.getName(), 0.0, 0, 0));
                            }
                        }
                    }

                    if (attendanceList.isEmpty()) {
                        if (tvNoDataState != null) {
                            tvNoDataState.setVisibility(View.VISIBLE);
                            tvNoDataState.setText("No subjects assigned for this academic context.");
                        }
                    } else {
                        if (tvNoDataState != null) tvNoDataState.setVisibility(View.GONE);
                    }
                    if (adapter != null) adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (tvNoDataState != null) {
                        tvNoDataState.setVisibility(View.VISIBLE);
                        tvNoDataState.setText("No subjects assigned for this academic context.");
                    }
                    attendanceList.clear();
                    if (adapter != null) adapter.notifyDataSetChanged();
                });
    }
}
