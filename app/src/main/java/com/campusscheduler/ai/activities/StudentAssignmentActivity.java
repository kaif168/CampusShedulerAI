package com.campusscheduler.ai.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.campusscheduler.ai.R;
import com.campusscheduler.ai.adapters.GenericAdapter;
import com.campusscheduler.ai.models.Assignment;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.utils.FirebaseHelper;
import com.campusscheduler.ai.utils.LocalAuthManager;
import com.campusscheduler.ai.utils.UIUtils;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentAssignmentActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private RecyclerView rvStudentAssignments;
    private GenericAdapter<Assignment> adapter;
    private final List<Assignment> assignmentList = new ArrayList<>();
    private final Map<String, DocumentSnapshot> userSubmissions = new HashMap<>();
    private User currentUser;

    private Uri pendingPdfUri = null;
    private String pendingPdfName = null;
    private TextView activeDialogPdfStatusText = null;

    private final ActivityResultLauncher<String> pdfPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    pendingPdfUri = uri;
                    pendingPdfName = getFileNameFromUri(uri);
                    if (activeDialogPdfStatusText != null) {
                        activeDialogPdfStatusText.setText("Attached PDF: " + pendingPdfName);
                        activeDialogPdfStatusText.setTextColor(0xFF2E7D32);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_assignment);

        currentUser = LocalAuthManager.getCurrentLocalUser(this);

        btnBack = findViewById(R.id.btnBack);
        rvStudentAssignments = findViewById(R.id.rvStudentAssignments);

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        if (rvStudentAssignments != null) {
            rvStudentAssignments.setLayoutManager(new LinearLayoutManager(this));
            adapter = new GenericAdapter<>(assignmentList, (item, holder) -> {
                String sub = item.getSubjectName() != null ? item.getSubjectName() : "Assignment";
                holder.setText(R.id.tvTitle, item.getTitle() + " (" + sub + ")");

                if (userSubmissions.containsKey(item.getId())) {
                    DocumentSnapshot subDoc = userSubmissions.get(item.getId());
                    String status = subDoc != null ? subDoc.getString("status") : "SUBMITTED";
                    Long marks = subDoc != null ? subDoc.getLong("marks") : null;
                    if ("GRADED".equalsIgnoreCase(status) && marks != null) {
                        holder.setText(R.id.tvSubtitle, "✓ Graded • Score: " + marks + " / " + item.getMaxMarks() + " • Tap to view feedback");
                    } else {
                        holder.setText(R.id.tvSubtitle, "✓ Submitted • Pending Teacher Review • Tap to review");
                    }
                } else {
                    String due = item.getDueDateStr() != null ? item.getDueDateStr() : "TBD";
                    holder.setText(R.id.tvSubtitle, "Due: " + due + " | Max Marks: " + item.getMaxMarks() + " • Tap to submit solution");
                }

                holder.itemView.setOnClickListener(v -> showSubmitDialog(item));
            }, R.layout.item_generic);
            rvStudentAssignments.setAdapter(adapter);
        }

        loadAssignments();
    }

    private void loadAssignments() {
        String collegeId = currentUser != null ? currentUser.getCollegeId() : null;
        FirebaseHelper.getAssignmentsRef().whereEqualTo("collegeId", collegeId).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    assignmentList.clear();
                    if (queryDocumentSnapshots != null) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            Assignment a = doc.toObject(Assignment.class);
                            if (a != null) {
                                if (a.getId() == null || a.getId().isEmpty()) {
                                    a.setId(doc.getId());
                                }
                                assignmentList.add(a);
                            }
                        }
                    }
                    if (assignmentList.isEmpty()) {
                        UIUtils.showToast(StudentAssignmentActivity.this, "No assignments available for your class.");
                    }
                    loadUserSubmissions();
                })
                .addOnFailureListener(e -> {
                    UIUtils.showToast(this, "Failed to load assignments: " + e.getMessage());
                });
    }

    private void loadUserSubmissions() {
        if (currentUser == null || TextUtils.isEmpty(currentUser.getUid())) {
            if (adapter != null) adapter.notifyDataSetChanged();
            return;
        }

        FirebaseHelper.getFirestore().collection("assignmentSubmissions")
                .whereEqualTo("studentUid", currentUser.getUid())
                .get()
                .addOnSuccessListener(snapshots -> {
                    userSubmissions.clear();
                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots) {
                            String aId = doc.getString("assignmentId");
                            if (aId != null) {
                                userSubmissions.put(aId, doc);
                            }
                        }
                    }
                    if (adapter != null) adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    if (adapter != null) adapter.notifyDataSetChanged();
                });
    }

    private void showSubmitDialog(Assignment assignment) {
        if (userSubmissions.containsKey(assignment.getId())) {
            DocumentSnapshot subDoc = userSubmissions.get(assignment.getId());
            String status = subDoc != null ? subDoc.getString("status") : "SUBMITTED";
            Long marks = subDoc != null ? subDoc.getLong("marks") : null;
            String feedback = subDoc != null ? subDoc.getString("feedback") : "No comments yet";
            String pdfUrl = subDoc != null ? subDoc.getString("pdfUrl") : null;

            AlertDialog.Builder b = new AlertDialog.Builder(this);
            b.setTitle(assignment.getTitle() + " (Already Submitted)");
            String msg = "Your work for this assignment was successfully submitted.\n\nStatus: " + status;
            if (marks != null) {
                msg += "\nAwarded Marks: " + marks + " / " + assignment.getMaxMarks();
                msg += "\nFeedback: " + feedback;
            }
            b.setMessage(msg);
            if (pdfUrl != null && !pdfUrl.isEmpty()) {
                b.setNeutralButton("View Submitted PDF", (dialog, which) -> {
                    try {
                        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(pdfUrl));
                        startActivity(i);
                    } catch (Exception e) {
                        UIUtils.showToast(this, "Cannot open PDF: " + e.getMessage());
                    }
                });
            }
            b.setPositiveButton("OK", null);
            b.show();
            return;
        }

        pendingPdfUri = null;
        pendingPdfName = null;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Submit Assignment: " + assignment.getTitle());

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 24, 40, 24);

        if (assignment.getDescription() != null && !assignment.getDescription().isEmpty()) {
            TextView tvDesc = new TextView(this);
            tvDesc.setText("Instructions: " + assignment.getDescription());
            tvDesc.setTextColor(0xFF546E7A);
            tvDesc.setPadding(0, 0, 0, 16);
            layout.addView(tvDesc);
        }

        EditText etSubmission = new EditText(this);
        etSubmission.setHint("Write solution notes or summary...");
        etSubmission.setLines(3);
        layout.addView(etSubmission);

        TextView tvPdfStatus = new TextView(this);
        tvPdfStatus.setText("No PDF attached yet.");
        tvPdfStatus.setTextColor(0xFF757575);
        tvPdfStatus.setPadding(0, 16, 0, 8);
        layout.addView(tvPdfStatus);
        activeDialogPdfStatusText = tvPdfStatus;

        Button btnAttach = new Button(this);
        btnAttach.setText("📎 Attach Solution PDF Document");
        btnAttach.setBackgroundColor(0xFFE3F2FD);
        btnAttach.setTextColor(0xFF1976D2);
        btnAttach.setOnClickListener(v -> pdfPickerLauncher.launch("application/pdf"));
        layout.addView(btnAttach);

        builder.setView(layout);

        builder.setPositiveButton("Submit Assignment", (dialog, which) -> {
            String text = etSubmission.getText().toString().trim();
            if (text.isEmpty() && pendingPdfUri == null) {
                UIUtils.showToast(this, "Please provide solution text or attach a PDF document.");
                return;
            }
            if (currentUser == null || currentUser.getUid() == null) {
                UIUtils.showToast(this, "Student profile context missing.");
                return;
            }

            String subId = "SUB_" + assignment.getId() + "_" + currentUser.getUid();
            if (pendingPdfUri != null) {
                uploadPdfAndSubmit(assignment, subId, text, pendingPdfUri, pendingPdfName);
            } else {
                saveSubmissionRecord(assignment, subId, text, null, null);
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void uploadPdfAndSubmit(Assignment assignment, String subId, String text, Uri fileUri, String fileName) {
        ProgressDialog progress = UIUtils.showLoadingDialog(this, "Uploading PDF submission to Firebase Storage...");

        String storagePath = "assignmentSubmissions/" + subId + "_" + (fileName != null ? fileName : "submission.pdf");
        var storageRef = FirebaseHelper.getStorageRef().child(storagePath);

        storageRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> {
                    storageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        if (progress != null && progress.isShowing()) progress.dismiss();
                        saveSubmissionRecord(assignment, subId, text, downloadUri.toString(), fileName);
                    }).addOnFailureListener(e -> {
                        if (progress != null && progress.isShowing()) progress.dismiss();
                        UIUtils.showToast(this, "Failed to get download URL: " + e.getMessage());
                    });
                })
                .addOnFailureListener(e -> {
                    if (progress != null && progress.isShowing()) progress.dismiss();
                    UIUtils.showToast(this, "Failed to upload PDF: " + e.getMessage());
                });
    }

    private void saveSubmissionRecord(Assignment assignment, String subId, String text, String pdfUrl, String pdfFileName) {
        Map<String, Object> subData = new HashMap<>();
        subData.put("id", subId);
        subData.put("assignmentId", assignment.getId());
        subData.put("studentUid", currentUser.getUid());
        subData.put("studentName", currentUser.getName() != null ? currentUser.getName() : "Student");
        subData.put("studentUsn", currentUser.getUsn() != null ? currentUser.getUsn() : "");
        subData.put("collegeId", currentUser.getCollegeId());
        subData.put("submissionText", text);
        if (pdfUrl != null) {
            subData.put("pdfUrl", pdfUrl);
            subData.put("pdfFileName", pdfFileName);
        }
        subData.put("submittedAt", System.currentTimeMillis());
        subData.put("status", "SUBMITTED");

        FirebaseHelper.getFirestore().collection("assignmentSubmissions").document(subId).set(subData)
                .addOnSuccessListener(aVoid -> {
                    UIUtils.showAlert(this, "Submission Confirmed", "Your assignment submission has been successfully recorded in Cloud Firestore!");
                    loadUserSubmissions();
                })
                .addOnFailureListener(e -> {
                    UIUtils.showToast(this, "Failed to save submission: " + e.getMessage());
                });
    }

    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception ignored) {}
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result != null ? result : "submission.pdf";
    }
}
