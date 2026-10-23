package com.campusscheduler.ai.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.campusscheduler.ai.R;
import com.campusscheduler.ai.models.Assignment;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.utils.FirebaseHelper;
import com.campusscheduler.ai.utils.LocalAuthManager;
import com.campusscheduler.ai.utils.UIUtils;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class TeacherAssignmentActivity extends AppCompatActivity {

    private EditText etAssignmentTitle, etAssignmentSubject, etTargetValue, etAssignmentDesc, etDueDate, etMaxMarks;
    private Spinner spTargetAudience;
    private ImageButton btnBack;
    private Button btnCreateAssignment;
    private ListView listViewAssignments;
    private List<String> assignmentsList;
    private List<Assignment> assignmentObjects;
    private ArrayAdapter<String> adapter;
    private User currentUser;

    private static final String[] AUDIENCE_OPTIONS = {
            "Whole Section (e.g. 4 A)",
            "Individual Student (USN)",
            "All Enrolled Students"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_assignment);

        btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

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

        etAssignmentTitle = findViewById(R.id.etAssignmentTitle);
        etAssignmentSubject = findViewById(R.id.etAssignmentSubject);
        spTargetAudience = findViewById(R.id.spTargetAudience);
        etTargetValue = findViewById(R.id.etTargetValue);
        etAssignmentDesc = findViewById(R.id.etAssignmentDesc);
        etDueDate = findViewById(R.id.etDueDate);
        etMaxMarks = findViewById(R.id.etMaxMarks);
        btnCreateAssignment = findViewById(R.id.btnCreateAssignment);
        listViewAssignments = findViewById(R.id.listViewAssignments);

        if (spTargetAudience != null) {
            ArrayAdapter<String> audAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_dropdown_item, AUDIENCE_OPTIONS);
            spTargetAudience.setAdapter(audAdapter);
        }

        assignmentsList = new ArrayList<>();
        assignmentObjects = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, assignmentsList);
        listViewAssignments.setAdapter(adapter);

        listViewAssignments.setOnItemClickListener((parent, view, position, id) -> {
            if (position < assignmentObjects.size()) {
                showAssignmentSubmissions(assignmentObjects.get(position));
            }
        });

        loadAssignmentsFromFirestore();

        btnCreateAssignment.setOnClickListener(v -> createAndSaveAssignment());
    }

    private void loadAssignmentsFromFirestore() {
        String collegeId = (currentUser != null && currentUser.getCollegeId() != null && !currentUser.getCollegeId().trim().isEmpty())
                ? currentUser.getCollegeId() : LocalAuthManager.getSafeCollegeId(this, getIntent());

        FirebaseHelper.getAssignmentsRef()
                .whereEqualTo("collegeId", collegeId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    assignmentsList.clear();
                    assignmentObjects.clear();
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            Assignment assignment = doc.toObject(Assignment.class);
                            if (assignment != null) {
                                if (assignment.getId() == null || assignment.getId().isEmpty()) {
                                    assignment.setId(doc.getId());
                                }
                                assignmentObjects.add(assignment);
                                String targetInfo = "";
                                if (assignment.getTargetAudience() != null) {
                                    targetInfo = " | Target: " + assignment.getTargetAudience() +
                                            (assignment.getTargetValue() != null ? " (" + assignment.getTargetValue() + ")" : "");
                                }
                                String display = assignment.getTitle() +
                                        (assignment.getSubjectName() != null ? " (" + assignment.getSubjectName() + ")" : "") +
                                        "\nDue: " + (assignment.getDueDateStr() != null ? assignment.getDueDateStr() : "TBD") +
                                        " | Marks: " + assignment.getMaxMarks() + targetInfo +
                                        "\n[Tap to View Student Submissions & Grade]";
                                assignmentsList.add(display);
                            }
                        }
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading assignments: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void createAndSaveAssignment() {
        String collegeId = (currentUser != null && currentUser.getCollegeId() != null && !currentUser.getCollegeId().trim().isEmpty())
                ? currentUser.getCollegeId() : LocalAuthManager.getSafeCollegeId(this, getIntent());
        String deptId = (currentUser != null && currentUser.getDepartmentId() != null) ? currentUser.getDepartmentId() : "DEPT_CSE";
        String teacherUid = currentUser != null ? currentUser.getUid() : "TEACHER_DEFAULT";
        String teacherName = currentUser != null ? currentUser.getName() : "Faculty";

        String title = etAssignmentTitle.getText().toString().trim();
        String subject = etAssignmentSubject != null ? etAssignmentSubject.getText().toString().trim() : "";
        String desc = etAssignmentDesc.getText().toString().trim();
        String due = etDueDate.getText().toString().trim();
        String marksStr = etMaxMarks.getText().toString().trim();
        String targetVal = etTargetValue != null ? etTargetValue.getText().toString().trim() : "";

        if (title.isEmpty()) {
            Toast.makeText(this, "Assignment title required", Toast.LENGTH_SHORT).show();
            return;
        }

        int maxMarks = 20;
        if (!marksStr.isEmpty()) {
            try {
                maxMarks = Integer.parseInt(marksStr);
            } catch (NumberFormatException ignored) {}
        }

        int selectedAudienceIdx = spTargetAudience != null ? spTargetAudience.getSelectedItemPosition() : 0;
        String targetAudience = "ALL";
        if (selectedAudienceIdx == 0) {
            targetAudience = "SECTION";
            if (targetVal.isEmpty()) targetVal = "4 A";
        } else if (selectedAudienceIdx == 1) {
            targetAudience = "STUDENT";
        }

        String assignmentId = "ASSIGN_" + System.currentTimeMillis();
        Assignment assignment = new Assignment();
        assignment.setAssignmentId(assignmentId);
        assignment.setId(assignmentId);
        assignment.setCollegeId(collegeId);
        assignment.setDepartmentId(deptId);
        assignment.setTeacherUid(teacherUid);
        assignment.setTeacherName(teacherName);
        assignment.setTitle(title);
        assignment.setSubjectName(subject.isEmpty() ? "General" : subject);
        assignment.setDescription(desc);
        assignment.setSubmissionInstructions(desc);
        assignment.setDueDateStr(due.isEmpty() ? "Next Week" : due);
        assignment.setMaxMarks(maxMarks);
        assignment.setTargetAudience(targetAudience);
        assignment.setTargetValue(targetVal);
        assignment.setStatus("PUBLISHED");
        assignment.setCreatedAt(System.currentTimeMillis());

        FirebaseHelper.getAssignmentsRef().document(assignmentId).set(assignment)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Assignment published to Cloud Firestore!", Toast.LENGTH_SHORT).show();
                    etAssignmentTitle.setText("");
                    if (etAssignmentSubject != null) etAssignmentSubject.setText("");
                    if (etTargetValue != null) etTargetValue.setText("");
                    etAssignmentDesc.setText("");
                    etDueDate.setText("");
                    etMaxMarks.setText("");
                    loadAssignmentsFromFirestore();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to publish assignment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showAssignmentSubmissions(Assignment assignment) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(assignment.getTitle() + " - Submissions");

        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 24, 32, 24);
        scroll.addView(layout);

        TextView tvLoading = new TextView(this);
        tvLoading.setText("Loading submissions from Cloud Firestore...");
        tvLoading.setPadding(0, 16, 0, 16);
        layout.addView(tvLoading);

        builder.setView(scroll);
        builder.setPositiveButton("Close", null);
        AlertDialog dialog = builder.create();
        dialog.show();

        FirebaseHelper.getFirestore().collection("assignmentSubmissions")
                .whereEqualTo("assignmentId", assignment.getId())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    layout.removeAllViews();
                    if (queryDocumentSnapshots == null || queryDocumentSnapshots.isEmpty()) {
                        TextView tvEmpty = new TextView(this);
                        tvEmpty.setText("No students have submitted this assignment yet.");
                        tvEmpty.setPadding(0, 20, 0, 20);
                        layout.addView(tvEmpty);
                        return;
                    }

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        LinearLayout subCard = new LinearLayout(this);
                        subCard.setOrientation(LinearLayout.VERTICAL);
                        subCard.setPadding(24, 20, 24, 20);
                        subCard.setBackgroundColor(0xFFE8EAF6);
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        lp.setMargins(0, 0, 0, 20);
                        subCard.setLayoutParams(lp);

                        String studentName = doc.getString("studentName") != null ? doc.getString("studentName") : "Student";
                        String studentUsn = doc.getString("studentUsn") != null ? doc.getString("studentUsn") : "";
                        String subText = doc.getString("submissionText") != null ? doc.getString("submissionText") : "";
                        String pdfUrl = doc.getString("pdfUrl");
                        String pdfName = doc.getString("pdfFileName") != null ? doc.getString("pdfFileName") : "Attached Document";
                        String status = doc.getString("status") != null ? doc.getString("status") : "SUBMITTED";
                        Long marks = doc.getLong("marks");

                        TextView tvInfo = new TextView(this);
                        tvInfo.setText(studentName + (!studentUsn.isEmpty() ? " (" + studentUsn + ")" : ""));
                        tvInfo.setTextSize(15f);
                        tvInfo.setTypeface(null, android.graphics.Typeface.BOLD);
                        tvInfo.setTextColor(0xFF0D47A1);
                        subCard.addView(tvInfo);

                        TextView tvStatus = new TextView(this);
                        tvStatus.setText("Status: " + status + (marks != null ? " • Awarded Marks: " + marks + " / " + assignment.getMaxMarks() : ""));
                        tvStatus.setTextSize(12f);
                        tvStatus.setTextColor(0xFF37474F);
                        subCard.addView(tvStatus);

                        if (!subText.isEmpty()) {
                            TextView tvText = new TextView(this);
                            tvText.setText("Notes: " + subText);
                            tvText.setTextSize(13f);
                            tvText.setPadding(0, 8, 0, 8);
                            subCard.addView(tvText);
                        }

                        if (pdfUrl != null && !pdfUrl.isEmpty()) {
                            Button btnViewPdf = new Button(this);
                            btnViewPdf.setText("📄 View / Download PDF (" + pdfName + ")");
                            btnViewPdf.setBackgroundColor(0xFF1976D2);
                            btnViewPdf.setTextColor(0xFFFFFFFF);
                            btnViewPdf.setOnClickListener(v -> {
                                try {
                                    Intent viewIntent = new Intent(Intent.ACTION_VIEW);
                                    viewIntent.setData(Uri.parse(pdfUrl));
                                    viewIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(viewIntent);
                                } catch (Exception e) {
                                    Toast.makeText(this, "Could not open PDF viewer: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                            subCard.addView(btnViewPdf);
                        }

                        Button btnGrade = new Button(this);
                        btnGrade.setText("⭐ Grade Submission");
                        btnGrade.setOnClickListener(v -> showGradeSubmissionDialog(doc, assignment, dialog));
                        subCard.addView(btnGrade);

                        layout.addView(subCard);
                    }
                })
                .addOnFailureListener(e -> {
                    tvLoading.setText("Failed to load submissions: " + e.getMessage());
                });
    }

    private void showGradeSubmissionDialog(DocumentSnapshot subDoc, Assignment assignment, AlertDialog parentDialog) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Grade: " + (subDoc.getString("studentName") != null ? subDoc.getString("studentName") : "Student"));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 24, 40, 24);

        EditText etMarks = new EditText(this);
        etMarks.setHint("Marks (out of " + assignment.getMaxMarks() + ")");
        etMarks.setInputType(InputType.TYPE_CLASS_NUMBER);
        layout.addView(etMarks);

        EditText etFeedback = new EditText(this);
        etFeedback.setHint("Teacher Feedback / Comments");
        layout.addView(etFeedback);

        builder.setView(layout);
        builder.setPositiveButton("Save Grade", (d, which) -> {
            String marksStr = etMarks.getText().toString().trim();
            if (marksStr.isEmpty()) {
                Toast.makeText(this, "Marks cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            int marksVal = Integer.parseInt(marksStr);
            String feedback = etFeedback.getText().toString().trim();

            subDoc.getReference().update(
                    "marks", marksVal,
                    "feedback", feedback,
                    "status", "GRADED",
                    "gradedAt", System.currentTimeMillis(),
                    "gradedBy", currentUser != null ? currentUser.getName() : "Faculty"
            ).addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Grade recorded in Firestore!", Toast.LENGTH_SHORT).show();
                parentDialog.dismiss();
            }).addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to save grade: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}
