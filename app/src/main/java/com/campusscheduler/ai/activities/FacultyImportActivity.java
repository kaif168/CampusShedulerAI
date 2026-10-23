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
import com.campusscheduler.ai.models.Faculty;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.utils.ExcelImporter;
import com.campusscheduler.ai.utils.LocalAuthManager;
import com.campusscheduler.ai.utils.UIUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FacultyImportActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private Button btnSelectCsv;
    private Button btnImportFaculty;
    private TextView tvImportStatus;

    private Uri selectedFileUri;
    private User currentUser;

    private List<Faculty> facultyList = new ArrayList<>();

    private static final int FILE_REQUEST = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_faculty_import);

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
        btnSelectCsv = findViewById(R.id.btnSelectCsv);
        btnImportFaculty = findViewById(R.id.btnImportFaculty);
        tvImportStatus = findViewById(R.id.tvImportStatus);

        btnBack.setOnClickListener(v -> finish());

        btnSelectCsv.setOnClickListener(
                v -> openFilePicker()
        );

        btnImportFaculty.setEnabled(false);

        btnImportFaculty.setOnClickListener(
                v -> readAndPreviewFaculty()
        );
    }

    private void openFilePicker() {

        Intent intent =
                new Intent(Intent.ACTION_GET_CONTENT);

        intent.setType("*/*");

        intent.addCategory(
                Intent.CATEGORY_OPENABLE
        );

        startActivityForResult(
                Intent.createChooser(
                        intent,
                        "Select Faculty Excel / CSV File"
                ),
                FILE_REQUEST
        );
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            @Nullable Intent data
    ) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (requestCode != FILE_REQUEST) {
            return;
        }

        if (resultCode != RESULT_OK) {
            return;
        }

        if (data == null) {
            return;
        }

        if (data.getData() == null) {
            return;
        }

        selectedFileUri = data.getData();

        facultyList.clear();

        btnImportFaculty.setEnabled(true);

        String fileName =
                getFileName(selectedFileUri);

        tvImportStatus.setText(
                "File selected:\n"
                        + fileName
                        + "\n\n"
                        + "Click READ & PREVIEW FACULTY "
                        + "to validate the records."
        );
    }

    private void readAndPreviewFaculty() {

        if (selectedFileUri == null) {

            UIUtils.showToast(
                    this,
                    "Please select a file first."
            );

            return;
        }

        String collegeId = currentUser != null && currentUser.getCollegeId() != null && !currentUser.getCollegeId().trim().isEmpty() ?
                currentUser.getCollegeId() : LocalAuthManager.getSafeCollegeId(this, getIntent());
        if (currentUser == null) {
            currentUser = new User();
            currentUser.setCollegeId(collegeId);
            currentUser.setDepartmentId("DEPT_CSE");
        } else if (currentUser.getCollegeId() == null || currentUser.getCollegeId().trim().isEmpty()) {
            currentUser.setCollegeId(collegeId);
        }

        String departmentId = currentUser.getDepartmentId();
        if (departmentId == null || departmentId.trim().isEmpty()) {
            departmentId = "DEPT_CSE";
        }

        final String finalCollegeId = collegeId;
        final String finalDepartmentId = departmentId;

        final String actualFileName =
                getFileName(selectedFileUri);

        final String mimeType =
                getContentResolver().getType(
                        selectedFileUri
                );

        final boolean excelFile =
                isExcelFile(
                        actualFileName,
                        mimeType
                );

        btnImportFaculty.setEnabled(false);

        ProgressDialog progressDialog =
                ProgressDialog.show(
                        this,
                        "Please wait",
                        excelFile
                                ? "Reading Excel file..."
                                : "Reading CSV file...",
                        true,
                        false
                );

        new Thread(() -> {

            List<Faculty> result = null;

            String errorMessage = null;

            try {

                InputStream inputStream =
                        getContentResolver()
                                .openInputStream(
                                        selectedFileUri
                                );

                if (inputStream == null) {

                    throw new Exception(
                            "Unable to open the selected file."
                    );
                }

                /*
                 * IMPORTANT:
                 *
                 * We use the REAL DISPLAY_NAME from
                 * Android's file provider.
                 *
                 * We do NOT use uri.getLastPathSegment()
                 * for file type detection.
                 *
                 * Therefore an Android URI such as:
                 *
                 * document:1000000037
                 *
                 * will no longer be treated as CSV.
                 */

                if (excelFile) {

                    result =
                            ExcelImporter.parseFacultyExcel(
                                    inputStream,
                                    finalCollegeId,
                                    finalDepartmentId
                            );

                } else {

                    result =
                            ExcelImporter.parseFacultyCsv(
                                    inputStream,
                                    finalCollegeId,
                                    finalDepartmentId
                            );
                }

                inputStream.close();

            } catch (Exception e) {

                errorMessage =
                        e.getMessage();

                if (errorMessage == null
                        || errorMessage.trim().isEmpty()) {

                    errorMessage =
                            "Unable to read the selected file.";
                }
            }

            final List<Faculty> records =
                    result;

            final String finalError =
                    errorMessage;

            runOnUiThread(() -> {

                progressDialog.dismiss();

                if (finalError != null) {

                    btnImportFaculty.setEnabled(true);

                    UIUtils.showAlert(
                            FacultyImportActivity.this,
                            "File Reading Error",
                            finalError
                    );

                    return;
                }

                if (records == null
                        || records.isEmpty()) {

                    btnImportFaculty.setEnabled(true);

                    UIUtils.showAlert(
                            FacultyImportActivity.this,
                            "No Faculty Records Found",
                            "No readable faculty records were found in "
                                    + actualFileName
                    );

                    return;
                }

                facultyList =
                        new ArrayList<>(records);

                tvImportStatus.setText(
                        "File read successfully.\n\n"
                                + "File: "
                                + actualFileName
                                + "\n"
                                + "Type: "
                                + (
                                excelFile
                                        ? "Excel (.xlsx/.xls)"
                                        : "CSV"
                        )
                                + "\n"
                                + "Records found: "
                                + facultyList.size()
                                + "\n\n"
                                + "Review the preview before importing."
                );

                showPreview();

            });

        }).start();
    }

    private boolean isExcelFile(
            String fileName,
            String mimeType
    ) {

        String name =
                fileName == null
                        ? ""
                        : fileName.toLowerCase(
                        Locale.US
                );

        if (name.endsWith(".xlsx")) {
            return true;
        }

        if (name.endsWith(".xls")) {
            return true;
        }

        if (mimeType != null) {

            String mime =
                    mimeType.toLowerCase(
                            Locale.US
                    );

            if (mime.contains("spreadsheet")) {
                return true;
            }

            if (mime.contains("excel")) {
                return true;
            }

            if (mime.contains(
                    "application/vnd.openxmlformats"
            )) {
                return true;
            }

            if (mime.contains(
                    "application/vnd.ms-excel"
            )) {
                return true;
            }
        }

        return false;
    }

    private String getFileName(Uri uri) {

        if (uri == null) {
            return "Selected file";
        }

        Cursor cursor = null;

        try {

            cursor =
                    getContentResolver().query(
                            uri,
                            new String[]{
                                    OpenableColumns.DISPLAY_NAME
                            },
                            null,
                            null,
                            null
                    );

            if (cursor != null
                    && cursor.moveToFirst()) {

                int columnIndex =
                        cursor.getColumnIndex(
                                OpenableColumns.DISPLAY_NAME
                        );

                if (columnIndex >= 0) {

                    String name =
                            cursor.getString(
                                    columnIndex
                            );

                    if (name != null
                            && !name.trim().isEmpty()) {

                        return name;
                    }
                }
            }

        } catch (Exception ignored) {

        } finally {

            if (cursor != null) {
                cursor.close();
            }
        }

        String fallback =
                uri.getLastPathSegment();

        if (fallback == null
                || fallback.trim().isEmpty()) {

            return "Selected file";
        }

        return fallback;
    }

    private void showPreview() {

        int total =
                facultyList.size();

        int names = 0;
        int emails = 0;
        int phones = 0;
        int designations = 0;
        int qualifications = 0;
        int experiences = 0;
        int specializations = 0;
        int expertises = 0;
        int workloads = 0;

        StringBuilder sample =
                new StringBuilder();

        for (int i = 0;
             i < facultyList.size();
             i++) {

            Faculty faculty =
                    facultyList.get(i);

            if (hasValue(
                    faculty.getName()
            )) {
                names++;
            }

            if (hasValue(
                    faculty.getEmail()
            )) {
                emails++;
            }

            if (hasValue(
                    faculty.getPhone()
            )) {
                phones++;
            }

            if (hasValue(
                    faculty.getDesignation()
            )) {
                designations++;
            }

            if (hasValue(
                    faculty.getQualification()
            )) {
                qualifications++;
            }

            if (hasValue(
                    faculty.getExperience()
            )) {
                experiences++;
            }

            if (hasValue(
                    faculty.getSpecialization()
            )) {
                specializations++;
            }

            if (hasValue(
                    faculty.getExpertise()
            )) {
                expertises++;
            }

            if (faculty.getMaxLoadPerWeek() > 0) {
                workloads++;
            }

            if (i < 5) {

                sample.append(
                        i + 1
                );

                sample.append(". ");

                sample.append(
                        getValue(
                                faculty.getName()
                        )
                );

                sample.append(" | ");

                sample.append(
                        getValue(
                                faculty.getDesignation()
                        )
                );

                sample.append("\n");
            }
        }

        StringBuilder message =
                new StringBuilder();

        message.append(
                "Total records: "
        );

        message.append(total);

        message.append("\n\n");

        message.append(
                "Available information:\n"
        );

        message.append(
                "Name: "
        );

        message.append(names);

        message.append("/");

        message.append(total);

        message.append("\n");

        message.append(
                "Email: "
        );

        message.append(emails);

        message.append("/");

        message.append(total);

        message.append("\n");

        message.append(
                "Phone: "
        );

        message.append(phones);

        message.append("/");

        message.append(total);

        message.append("\n");

        message.append(
                "Designation: "
        );

        message.append(designations);

        message.append("/");

        message.append(total);

        message.append("\n");

        message.append(
                "Qualification: "
        );

        message.append(qualifications);

        message.append("/");

        message.append(total);

        message.append("\n");

        message.append(
                "Experience: "
        );

        message.append(experiences);

        message.append("/");

        message.append(total);

        message.append("\n");

        message.append(
                "Specialization: "
        );

        message.append(specializations);

        message.append("/");

        message.append(total);

        message.append("\n");

        message.append(
                "Expertise: "
        );

        message.append(expertises);

        message.append("/");

        message.append(total);

        message.append("\n");

        message.append(
                "Workload configured: "
        );

        message.append(workloads);

        message.append("/");

        message.append(total);

        message.append("\n\n");

        message.append(
                "First records:\n"
        );

        message.append(sample);

        message.append("\n");

        message.append(
                "Missing information remains blank."
        );

        message.append("\n");

        message.append(
                "No fake email, phone or profile information "
                        + "will be created."
        );

        message.append("\n\n");

        message.append(
                "Nothing is saved until IMPORT is pressed."
        );

        new AlertDialog.Builder(this)

                .setTitle(
                        "Faculty Import Preview"
                )

                .setMessage(
                        message.toString()
                )

                .setNegativeButton(
                        "CANCEL",
                        (dialog, which) -> {

                            btnImportFaculty
                                    .setEnabled(true);
                        }
                )

                .setPositiveButton(
                        "IMPORT",
                        (dialog, which) -> {

                            importFaculty();
                        }
                )

                .setOnCancelListener(
                        dialog -> {

                            btnImportFaculty
                                    .setEnabled(true);
                        }
                )

                .show();
    }

    private void importFaculty() {

        if (facultyList == null
                || facultyList.isEmpty()) {

            UIUtils.showToast(
                    this,
                    "No faculty records available."
            );

            return;
        }

        ProgressDialog progressDialog =
                ProgressDialog.show(
                        this,
                        "Please wait",
                        "Saving faculty records...",
                        true,
                        false
                );

        ExcelImporter.importFacultyToFirestore(
                facultyList,
                new ExcelImporter.ImportCallback() {

                    @Override
                    public void onSuccess(
                            int count
                    ) {

                        runOnUiThread(() -> {

                            progressDialog.dismiss();

                            facultyList.clear();

                            btnImportFaculty
                                    .setEnabled(false);

                            tvImportStatus.setText(
                                    "Import completed successfully.\n\n"
                                            + "Faculty records saved: "
                                            + count
                            );

                            UIUtils.showAlert(
                                    FacultyImportActivity.this,
                                    "Import Successful",
                                    count
                                            + " faculty records saved to Firestore."
                            );
                        });
                    }

                    @Override
                    public void onError(
                            String message
                    ) {

                        final String finalMessage;

                        if (message == null
                                || message.trim().isEmpty()) {

                            finalMessage =
                                    "Faculty import failed.";

                        } else {

                            finalMessage =
                                    message;
                        }

                        runOnUiThread(() -> {

                            progressDialog.dismiss();

                            btnImportFaculty
                                    .setEnabled(true);

                            UIUtils.showAlert(
                                    FacultyImportActivity.this,
                                    "Import Failed",
                                    finalMessage
                            );
                        });
                    }
                }
        );
    }

    private boolean hasValue(
            String value
    ) {

        return value != null
                && !value.trim().isEmpty();
    }

    private String getValue(
            String value
    ) {

        if (hasValue(value)) {
            return value.trim();
        }

        return "Blank";
    }
}