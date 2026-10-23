package com.campusscheduler.ai.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.campusscheduler.ai.R;
import com.campusscheduler.ai.models.Classroom;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.utils.ExcelImporter;
import com.campusscheduler.ai.utils.LocalAuthManager;
import com.campusscheduler.ai.utils.UIUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ClassroomImportActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private Button btnSelectRoomCsv, btnImportClassrooms;
    private TextView tvRoomImportStatus;

    private static final int REQUEST_CODE_ROOM_CSV = 1002;
    private Uri selectedRoomCsvUri = null;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_classroom_import);

        currentUser = LocalAuthManager.getCurrentLocalUser(this);

        btnBack = findViewById(R.id.btnBack);
        btnSelectRoomCsv = findViewById(R.id.btnSelectRoomCsv);
        btnImportClassrooms = findViewById(R.id.btnImportClassrooms);
        tvRoomImportStatus = findViewById(R.id.tvRoomImportStatus);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
        btnSelectRoomCsv.setOnClickListener(v -> openFilePicker());
        btnImportClassrooms.setOnClickListener(v -> performBatchImport());
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, REQUEST_CODE_ROOM_CSV);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_ROOM_CSV && resultCode == RESULT_OK && data != null) {
            selectedRoomCsvUri = data.getData();
            if (selectedRoomCsvUri != null) {
                tvRoomImportStatus.setText("File selected: " + selectedRoomCsvUri.getLastPathSegment());
            }
        }
    }

    private void performBatchImport() {
        if (selectedRoomCsvUri == null) {
            UIUtils.showToast(this, "Please browse and select a CSV file first before importing.");
            return;
        }

        String collegeId = currentUser != null && currentUser.getCollegeId() != null ?
                currentUser.getCollegeId() : LocalAuthManager.getSafeCollegeId(this, getIntent());

        List<Classroom> listToImport = new ArrayList<>();
        try {
            InputStream is = getContentResolver().openInputStream(selectedRoomCsvUri);
            listToImport = ExcelImporter.parseClassroomCsv(is, collegeId);
        } catch (Exception e) {
            UIUtils.showToast(this, "CSV parse error: " + e.getMessage());
            return;
        }

        if (listToImport.isEmpty()) {
            UIUtils.showAlert(this, "No Classroom Records Found", "The selected CSV file has no valid classroom rows. Please make sure your file is saved as CSV (*.csv) and contains classroom details.");
            return;
        }

        ProgressDialog pd = UIUtils.showLoadingDialog(this, "Batch importing " + listToImport.size() + " classrooms & labs to Firestore...");

        ExcelImporter.importClassroomsToFirestore(listToImport, new ExcelImporter.ImportCallback() {
            @Override
            public void onSuccess(int importedCount) {
                pd.dismiss();
                UIUtils.showToast(ClassroomImportActivity.this, "Successfully imported " + importedCount + " classrooms to Firestore!");
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                pd.dismiss();
                UIUtils.showToast(ClassroomImportActivity.this, "Import Error: " + errorMessage);
            }
        });
    }
}
