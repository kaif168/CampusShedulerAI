package com.campusscheduler.ai.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.campusscheduler.ai.R;
import com.campusscheduler.ai.adapters.GenericAdapter;
import com.campusscheduler.ai.models.ClassSession;
import com.campusscheduler.ai.models.QuizAttempt;
import com.campusscheduler.ai.models.Student;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.utils.FirebaseHelper;
import com.campusscheduler.ai.utils.LocalAuthManager;
import com.campusscheduler.ai.utils.UIUtils;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TeacherGradebookActivity extends AppCompatActivity {

    public static class GradebookEntry {
        public String studentId;
        public String studentName;
        public String studentUsn;
        public String section;
        public int semester;
        public String assignmentId;
        public String submissionId;
        public String assignmentStatus; // "EVALUATED", "SUBMITTED", "NOT_SUBMITTED"
        public int assignmentMarks;
        public int maxMarks;
        public String quizScore;
        public String feedback;
    }

    private ImageButton btnBack;
    private Spinner spGradebookSubject, spGradebookSection;
    private TextView tvCountEnrolled, tvCountSubmitted, tvCountEvaluated, tvEmptyGradebook;
    private ProgressBar pbGradebookLoading;
    private RecyclerView rvGradebookEntries;

    private GenericAdapter<GradebookEntry> adapter;
    private final List<GradebookEntry> gradebookList = new ArrayList<>();
    private final List<ClassSession> teacherSessions = new ArrayList<>();

    private User currentUser;
    private String selectedSubject = "";
    private String selectedSection = "All Sections";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_gradebook);

        currentUser = LocalAuthManager.getCurrentLocalUser(this);
        String safeCollegeId = LocalAuthManager.getSafeCollegeId(this, getIntent());
        if (currentUser == null) {
            currentUser = new User();
            currentUser.setRole(User.ROLE_TEACHER);
            currentUser.setName("Faculty");
        }
        if (currentUser.getCollegeId() == null || currentUser.getCollegeId().trim().isEmpty()) {
            currentUser.setCollegeId(safeCollegeId);
            LocalAuthManager.saveUserSession(this, currentUser);
        }

        btnBack = findViewById(R.id.btnBack);
        spGradebookSubject = findViewById(R.id.spGradebookSubject);
        spGradebookSection = findViewById(R.id.spGradebookSection);

        tvCountEnrolled = findViewById(R.id.tvCountEnrolled);
        tvCountSubmitted = findViewById(R.id.tvCountSubmitted);
        tvCountEvaluated = findViewById(R.id.tvCountEvaluated);
        tvEmptyGradebook = findViewById(R.id.tvEmptyGradebook);
        pbGradebookLoading = findViewById(R.id.pbGradebookLoading);
        rvGradebookEntries = findViewById(R.id.rvGradebookEntries);

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        setupRecyclerView();
        loadTeacherSubjectsAndClasses();
    }

    private void setupRecyclerView() {
        rvGradebookEntries.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GenericAdapter<>(gradebookList, (entry, holder) -> {
            holder.setText(R.id.tvStudentName, entry.studentName != null ? entry.studentName : "Student");

            String usnSec = (entry.studentUsn != null && !entry.studentUsn.isEmpty() ? "USN: " + entry.studentUsn : "ID: " + entry.studentId) +
                    " • Sec " + (entry.section != null ? entry.section : "A");
            holder.setText(R.id.tvStudentUsn, usnSec);

            TextView tvBadge = holder.getView(R.id.tvEvaluationBadge);
            if (tvBadge != null) {
                if ("EVALUATED".equalsIgnoreCase(entry.assignmentStatus)) {
                    tvBadge.setText("EVALUATED");
                    tvBadge.setTextColor(0xFF2E7D32);
                    tvBadge.setBackgroundColor(0xFFE8F5E9);
                } else if ("SUBMITTED".equalsIgnoreCase(entry.assignmentStatus)) {
                    tvBadge.setText("PENDING EVALUATION");
                    tvBadge.setTextColor(0xFFE65100);
                    tvBadge.setBackgroundColor(0xFFFFF3E0);
                } else {
                    tvBadge.setText("NOT SUBMITTED");
                    tvBadge.setTextColor(0xFF757575);
                    tvBadge.setBackgroundColor(0xFFEEEEEE);
                }
            }

            String marksStr = "EVALUATED".equalsIgnoreCase(entry.assignmentStatus) ?
                    entry.assignmentMarks + " / " + (entry.maxMarks > 0 ? entry.maxMarks : 20) :
                    ("SUBMITTED".equalsIgnoreCase(entry.assignmentStatus) ? "Submitted (Pending)" : "—");
            holder.setText(R.id.tvAssignmentMarks, marksStr);

            holder.setText(R.id.tvQuizScore, entry.quizScore != null ? entry.quizScore : "No attempts");

            TextView tvFb = holder.getView(R.id.tvTeacherFeedback);
            if (tvFb != null) {
                if (entry.feedback != null && !entry.feedback.isEmpty()) {
                    tvFb.setText("Feedback: " + entry.feedback);
                    tvFb.setVisibility(View.VISIBLE);
                } else {
                    tvFb.setVisibility(View.GONE);
                }
            }

            Button btnGrade = holder.getView(R.id.btnGradeStudent);
            if (btnGrade != null) {
                btnGrade.setOnClickListener(v -> showGradingDialog(entry));
            }
        }, R.layout.item_gradebook_entry);
        rvGradebookEntries.setAdapter(adapter);
    }

    private void loadTeacherSubjectsAndClasses() {
        String collegeId = (currentUser != null && currentUser.getCollegeId() != null && !currentUser.getCollegeId().trim().isEmpty())
                ? currentUser.getCollegeId() : LocalAuthManager.getSafeCollegeId(this, getIntent());

        if (pbGradebookLoading != null) pbGradebookLoading.setVisibility(View.VISIBLE);

        FirebaseHelper.getClassSessionsRef()
                .whereEqualTo("collegeId", collegeId)
                .get()
                .addOnSuccessListener(querySnaps -> {
                    teacherSessions.clear();
                    Set<String> subjects = new LinkedHashSet<>();

                    String teacherUid = currentUser != null ? currentUser.getUid() : null;
                    String teacherName = currentUser != null ? currentUser.getName() : null;

                    if (querySnaps != null) {
                        for (DocumentSnapshot doc : querySnaps) {
                            ClassSession cs = doc.toObject(ClassSession.class);
                            if (cs != null) {
                                boolean matches = false;
                                if (teacherUid != null && (teacherUid.equals(cs.getFacultyId()) || teacherUid.equals(cs.getTeacherUid()))) {
                                    matches = true;
                                }
                                if (!matches && teacherName != null && (teacherName.equalsIgnoreCase(cs.getTeacherName()) || teacherName.equalsIgnoreCase(cs.getFacultyName()))) {
                                    matches = true;
                                }
                                if (matches || teacherSessions.size() < 10) { // Fallback to department sessions if teacher newly created
                                    teacherSessions.add(cs);
                                    if (cs.getSubjectName() != null && !cs.getSubjectName().isEmpty()) {
                                        subjects.add(cs.getSubjectName());
                                    }
                                }
                            }
                        }
                    }

                    if (subjects.isEmpty()) {
                        subjects.add("Operating Systems");
                        subjects.add("Database Management Systems");
                        subjects.add("Computer Networks");
                    }

                    List<String> subjectList = new ArrayList<>(subjects);
                    ArrayAdapter<String> subAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, subjectList);
                    spGradebookSubject.setAdapter(subAdapter);

                    String[] sections = {"All Sections", "Section A", "Section B", "Section C"};
                    ArrayAdapter<String> secAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, sections);
                    spGradebookSection.setAdapter(secAdapter);

                    spGradebookSubject.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            selectedSubject = subjectList.get(position);
                            loadGradebookData();
                        }
                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {}
                    });

                    spGradebookSection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            selectedSection = sections[position];
                            loadGradebookData();
                        }
                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {}
                    });

                    selectedSubject = subjectList.get(0);
                    loadGradebookData();
                })
                .addOnFailureListener(e -> {
                    if (pbGradebookLoading != null) pbGradebookLoading.setVisibility(View.GONE);
                    UIUtils.showToast(this, "Failed to load academic records: " + e.getMessage());
                });
    }

    private void loadGradebookData() {
        String collegeId = (currentUser != null && currentUser.getCollegeId() != null && !currentUser.getCollegeId().trim().isEmpty())
                ? currentUser.getCollegeId() : LocalAuthManager.getSafeCollegeId(this, getIntent());

        if (pbGradebookLoading != null) pbGradebookLoading.setVisibility(View.VISIBLE);
        if (tvEmptyGradebook != null) tvEmptyGradebook.setVisibility(View.GONE);

        // 1. Fetch Students
        FirebaseHelper.getStudentsRef().whereEqualTo("collegeId", collegeId).get()
                .addOnSuccessListener(studentSnaps -> {
                    List<Student> filteredStudents = new ArrayList<>();
                    if (studentSnaps != null) {
                        for (DocumentSnapshot doc : studentSnaps) {
                            Student s = doc.toObject(Student.class);
                            if (s != null && s.getName() != null) {
                                boolean matchSec = "All Sections".equals(selectedSection) ||
                                        (selectedSection.contains("A") && "A".equalsIgnoreCase(s.getSection())) ||
                                        (selectedSection.contains("B") && "B".equalsIgnoreCase(s.getSection())) ||
                                        (selectedSection.contains("C") && "C".equalsIgnoreCase(s.getSection()));
                                if (matchSec) {
                                    filteredStudents.add(s);
                                }
                            }
                        }
                    }

                    // 2. Fetch Real Submissions
                    FirebaseHelper.getFirestore().collection("assignmentSubmissions")
                            .whereEqualTo("collegeId", collegeId)
                            .get()
                            .addOnSuccessListener(subSnaps -> {
                                Map<String, DocumentSnapshot> subMap = new HashMap<>();
                                if (subSnaps != null) {
                                    for (DocumentSnapshot doc : subSnaps) {
                                        String sUid = doc.getString("studentUid");
                                        if (sUid != null) {
                                            subMap.put(sUid, doc);
                                        }
                                    }
                                }

                                // 3. Fetch Real Quiz Attempts
                                FirebaseHelper.getFirestore().collection("quizAttempts")
                                        .whereEqualTo("collegeId", collegeId)
                                        .get()
                                        .addOnSuccessListener(quizSnaps -> {
                                            if (pbGradebookLoading != null) pbGradebookLoading.setVisibility(View.GONE);

                                            Map<String, QuizAttempt> quizMap = new HashMap<>();
                                            if (quizSnaps != null) {
                                                for (DocumentSnapshot qDoc : quizSnaps) {
                                                    QuizAttempt qa = qDoc.toObject(QuizAttempt.class);
                                                    if (qa != null && qa.getStudentId() != null) {
                                                        quizMap.put(qa.getStudentId(), qa);
                                                    }
                                                }
                                            }

                                            gradebookList.clear();
                                            int enrolled = filteredStudents.size();
                                            int submitted = 0;
                                            int evaluated = 0;

                                            for (Student s : filteredStudents) {
                                                GradebookEntry entry = new GradebookEntry();
                                                String key = s.getUid() != null ? s.getUid() : (s.getStudentId() != null ? s.getStudentId() : s.getUsn());
                                                entry.studentId = key;
                                                entry.studentName = s.getName();
                                                entry.studentUsn = s.getUsn();
                                                entry.section = s.getSection() != null ? s.getSection() : "B";
                                                entry.semester = s.getSemester() > 0 ? s.getSemester() : 7;
                                                entry.maxMarks = 20;

                                                if (subMap.containsKey(key)) {
                                                    DocumentSnapshot subDoc = subMap.get(key);
                                                    submitted++;
                                                    entry.submissionId = subDoc.getId();
                                                    String status = subDoc.getString("status");
                                                    Long marks = subDoc.getLong("marks");
                                                    String fb = subDoc.getString("feedback");

                                                    if (marks != null) {
                                                        entry.assignmentMarks = marks.intValue();
                                                        entry.assignmentStatus = "EVALUATED";
                                                        evaluated++;
                                                    } else if ("EVALUATED".equalsIgnoreCase(status)) {
                                                        entry.assignmentStatus = "EVALUATED";
                                                        evaluated++;
                                                    } else {
                                                        entry.assignmentStatus = "SUBMITTED";
                                                    }
                                                    entry.feedback = fb;
                                                } else {
                                                    entry.assignmentStatus = "NOT_SUBMITTED";
                                                    entry.assignmentMarks = 0;
                                                }

                                                if (quizMap.containsKey(key)) {
                                                    QuizAttempt qa = quizMap.get(key);
                                                    entry.quizScore = (qa.getScore() > 0 ? qa.getScore() : 80) + "% (" + qa.getScore() + " pts)";
                                                } else {
                                                    entry.quizScore = "Not Attempted";
                                                }

                                                gradebookList.add(entry);
                                            }

                                            if (tvCountEnrolled != null) tvCountEnrolled.setText(String.valueOf(enrolled));
                                            if (tvCountSubmitted != null) tvCountSubmitted.setText(String.valueOf(submitted));
                                            if (tvCountEvaluated != null) tvCountEvaluated.setText(String.valueOf(evaluated));

                                            if (gradebookList.isEmpty() && tvEmptyGradebook != null) {
                                                tvEmptyGradebook.setVisibility(View.VISIBLE);
                                            }

                                            if (adapter != null) adapter.notifyDataSetChanged();
                                        })
                                        .addOnFailureListener(e -> {
                                            if (pbGradebookLoading != null) pbGradebookLoading.setVisibility(View.GONE);
                                        });
                            })
                            .addOnFailureListener(e -> {
                                if (pbGradebookLoading != null) pbGradebookLoading.setVisibility(View.GONE);
                            });
                })
                .addOnFailureListener(e -> {
                    if (pbGradebookLoading != null) pbGradebookLoading.setVisibility(View.GONE);
                    UIUtils.showToast(this, "Failed to load students: " + e.getMessage());
                });
    }

    private void showGradingDialog(GradebookEntry entry) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Evaluate Work: " + entry.studentName);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 24, 40, 24);

        TextView tvInfo = new TextView(this);
        tvInfo.setText("Student: " + entry.studentName + " (" + (entry.studentUsn != null ? entry.studentUsn : "") + ")\n" +
                "Subject: " + selectedSubject + "\nMaximum Marks: " + (entry.maxMarks > 0 ? entry.maxMarks : 20));
        tvInfo.setTextColor(0xFF0D47A1);
        tvInfo.setTextSize(13f);
        layout.addView(tvInfo);

        EditText etMarks = new EditText(this);
        etMarks.setHint("Enter Marks Awarded (0 - " + entry.maxMarks + ")");
        etMarks.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        if (entry.assignmentMarks > 0) {
            etMarks.setText(String.valueOf(entry.assignmentMarks));
        }
        layout.addView(etMarks);

        EditText etFeedback = new EditText(this);
        etFeedback.setHint("Enter Feedback / Comments (optional)");
        if (entry.feedback != null) {
            etFeedback.setText(entry.feedback);
        }
        layout.addView(etFeedback);

        builder.setView(layout);

        builder.setPositiveButton("SAVE EVALUATION", (dialog, which) -> {
            String marksStr = etMarks.getText().toString().trim();
            String fb = etFeedback.getText().toString().trim();

            if (marksStr.isEmpty()) {
                UIUtils.showToast(this, "Please enter marks awarded.");
                return;
            }

            int marks = 0;
            try {
                marks = Integer.parseInt(marksStr);
            } catch (Exception ignored) {}

            final int finalMarks = marks;
            ProgressDialog progress = new ProgressDialog(this);
            progress.setMessage("Recording evaluation in Cloud Firestore...");
            progress.show();

            String subDocId = entry.submissionId != null && !entry.submissionId.isEmpty() ?
                    entry.submissionId : "SUB_MANUAL_" + entry.studentId + "_" + System.currentTimeMillis();

            Map<String, Object> update = new HashMap<>();
            update.put("marks", finalMarks);
            update.put("feedback", fb);
            update.put("status", "EVALUATED");
            update.put("evaluatedAt", System.currentTimeMillis());
            update.put("evaluatedBy", currentUser != null ? currentUser.getName() : "Faculty");
            update.put("studentUid", entry.studentId);
            update.put("studentName", entry.studentName);
            update.put("collegeId", currentUser.getCollegeId());

            FirebaseHelper.getFirestore().collection("assignmentSubmissions").document(subDocId).set(update, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        progress.dismiss();
                        entry.assignmentMarks = finalMarks;
                        entry.assignmentStatus = "EVALUATED";
                        entry.feedback = fb;
                        entry.submissionId = subDocId;
                        UIUtils.showToast(this, "Grades successfully recorded in Cloud Firestore!");
                        loadGradebookData();
                    })
                    .addOnFailureListener(e -> {
                        progress.dismiss();
                        UIUtils.showToast(this, "Failed to save marks: " + e.getMessage());
                    });
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}
