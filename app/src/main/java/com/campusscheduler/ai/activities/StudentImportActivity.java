package com.campusscheduler.ai.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.campusscheduler.ai.R;
import com.campusscheduler.ai.models.Student;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.utils.ExcelImporter;
import com.campusscheduler.ai.utils.LocalAuthManager;
import com.campusscheduler.ai.utils.UIUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StudentImportActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private Button btnSelectFile;
    private Button btnImportStudents;
    private TextView tvImportStatus;

    private Uri selectedFileUri;
    private User currentUser;
    private final List<Student> studentList = new ArrayList<>();

    private static final int FILE_REQUEST = 2002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_import);

        currentUser = LocalAuthManager.getCurrentLocalUser(this);
        String safeCollegeId = LocalAuthManager.getSafeCollegeId(this, getIntent());
        if (currentUser == null) {
            currentUser = new User();
            currentUser.setCollegeId(safeCollegeId);
            currentUser.setDepartmentId("DEPT_CSE");
        } else if (currentUser.getCollegeId() == null || currentUser.getCollegeId().trim().isEmpty()) {
            currentUser.setCollegeId(safeCollegeId);
        }

        btnBack = findViewById(R.id.btnBack);
        btnSelectFile = findViewById(R.id.btnSelectFile);
        btnImportStudents = findViewById(R.id.btnImportStudents);
        tvImportStatus = findViewById(R.id.tvImportStatus);

        btnBack.setOnClickListener(v -> finish());
        btnSelectFile.setOnClickListener(v -> openFilePicker());

        btnImportStudents.setEnabled(false);
        btnImportStudents.setOnClickListener(v -> readAndPreviewStudents());
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(
                Intent.createChooser(intent, "Select Student Excel / CSV File"),
                FILE_REQUEST
        );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != FILE_REQUEST || resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }

        selectedFileUri = data.getData();
        studentList.clear();
        btnImportStudents.setEnabled(true);

        String fileName = getFileName(selectedFileUri);
        tvImportStatus.setText("File selected:\n" + fileName + "\n\nClick READ & PREVIEW STUDENTS to validate the records.");
    }

    private void readAndPreviewStudents() {
        if (selectedFileUri == null) {
            UIUtils.showToast(this, "Please select a file first.");
            return;
        }

        String collegeId = currentUser != null && currentUser.getCollegeId() != null && !currentUser.getCollegeId().trim().isEmpty() ?
                currentUser.getCollegeId() : LocalAuthManager.getSafeCollegeId(this, getIntent());
        String departmentId = currentUser != null && currentUser.getDepartmentId() != null && !currentUser.getDepartmentId().trim().isEmpty() ?
                currentUser.getDepartmentId() : "DEPT_CSE";

        final String actualFileName = getFileName(selectedFileUri);
        final String mimeType = getContentResolver().getType(selectedFileUri);
        final boolean excelFile = isExcelFile(actualFileName, mimeType);

        btnImportStudents.setEnabled(false);

        ProgressDialog progressDialog = ProgressDialog.show(
                this,
                "Please wait",
                excelFile ? "Reading student Excel file..." : "Reading student CSV file...",
                true,
                false
        );

        new Thread(() -> {
            List<Student> result = null;
            String errorMessage = null;

            try {
                InputStream inputStream = getContentResolver().openInputStream(selectedFileUri);
                if (inputStream == null) {
                    throw new Exception("Unable to open the selected file.");
                }

                if (excelFile) {
                    result = ExcelImporter.parseStudentExcel(inputStream, collegeId, departmentId);
                } else {
                    result = ExcelImporter.parseStudentCsv(inputStream, collegeId, departmentId);
                }
                inputStream.close();

            } catch (Exception e) {
                errorMessage = e.getMessage() != null ? e.getMessage() : "Unable to read the selected file.";
            }

            final List<Student> records = result;
            final String finalError = errorMessage;

            runOnUiThread(() -> {
                progressDialog.dismiss();
                btnImportStudents.setEnabled(true);

                if (finalError != null) {
                    UIUtils.showAlert(StudentImportActivity.this, "File Reading Error", finalError);
                    return;
                }

                if (records == null || records.isEmpty()) {
                    UIUtils.showAlert(StudentImportActivity.this, "Import Empty", "No valid student records found in the selected file.");
                    return;
                }

                studentList.clear();
                studentList.addAll(records);

                showPreviewDialog();
            });
        }).start();
    }

    private void showPreviewDialog() {
        StringBuilder sb = new StringBuilder();
        sb.append("Total student records found: ").append(studentList.size()).append("\n\n");
        sb.append("First records preview:\n");

        int previewLimit = Math.min(5, studentList.size());
        for (int i = 0; i < previewLimit; i++) {
            Student s = studentList.get(i);
            sb.append("• ").append(s.getUsn()).append(" - ").append(s.getName())
                    .append(" (Sem ").append(s.getSemester()).append("-").append(s.getSection()).append(")\n");
        }

        if (studentList.size() > previewLimit) {
            sb.append("... and ").append(studentList.size() - previewLimit).append(" more students.");
        }

        sb.append("\n\nDo you want to import and save these students to Cloud Firestore?");

        new AlertDialog.Builder(this)
                .setTitle("Student Import Preview")
                .setMessage(sb.toString())
                .setPositiveButton("IMPORT TO FIRESTORE", (dialog, which) -> saveStudentsToFirestore())
                .setNegativeButton("CANCEL", null)
                .show();
    }

    private void saveStudentsToFirestore() {
        ProgressDialog progress = UIUtils.showLoadingDialog(this, "Saving students to Cloud Firestore...");

        ExcelImporter.importStudentsToFirestore(studentList, new ExcelImporter.ImportCallback() {
            @Override
            public void onSuccess(int importedCount) {
                progress.dismiss();
                UIUtils.showAlert(StudentImportActivity.this, "Import Successful",
                        "Successfully imported " + importedCount + " students to Cloud Firestore 'students' and 'users' collections!");
                tvImportStatus.setText("Successfully imported " + importedCount + " student records.");
            }

            @Override
            public void onError(String errorMessage) {
                progress.dismiss();
                UIUtils.showAlert(StudentImportActivity.this, "Import Failed", errorMessage);
            }
        });
    }

    private String getFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) {
                        result = cursor.getString(index);
                    }
                }
            } catch (Exception ignored) {}
        }
        if (result == null) {
            result = uri.getPath();
            if (result != null) {
                int cut = result.lastIndexOf('/');
                if (cut != -1) {
                    result = result.substring(cut + 1);
                }
            }
        }
        return result != null ? result : "unknown_file";
    }

    private boolean isExcelFile(String fileName, String mimeType) {
        if (fileName != null) {
            String lower = fileName.toLowerCase(Locale.US);
            if (lower.endsWith(".xlsx") || lower.endsWith(".xls")) {
                return true;
            }
        }
        if (mimeType != null) {
            String lower = mimeType.toLowerCase(Locale.US);
            if (lower.contains("spreadsheet") || lower.contains("excel")) {
                return true;
            }
        }
        return false;
    }
}
