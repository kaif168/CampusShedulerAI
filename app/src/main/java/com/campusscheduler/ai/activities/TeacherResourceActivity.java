package com.campusscheduler.ai.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.campusscheduler.ai.R;
import com.campusscheduler.ai.models.ResourceItem;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.utils.FirebaseHelper;
import com.campusscheduler.ai.utils.LocalAuthManager;
import com.campusscheduler.ai.utils.UIUtils;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TeacherResourceActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private EditText etResourceTitle, etResourceUrl;
    private Button btnSelectPdf, btnUploadResource;
    private ImageButton btnClearSelectedPdf;
    private TextView tvSelectedPdfName;
    private ProgressBar pbUpload;
    private ListView listViewResources;
    private List<ResourceItem> resourceItemList;
    private List<String> resourceDisplayList;
    private ArrayAdapter<String> adapter;
    private User currentUser;

    private Uri selectedPdfUri = null;
    private String selectedPdfFileName = null;
    private long selectedPdfSizeBytes = 0;

    private final ActivityResultLauncher<Intent> pdfPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        handleSelectedPdfUri(uri);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_resource);

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

        etResourceTitle = findViewById(R.id.etResourceTitle);
        etResourceUrl = findViewById(R.id.etResourceUrl);
        btnSelectPdf = findViewById(R.id.btnSelectPdf);
        btnClearSelectedPdf = findViewById(R.id.btnClearSelectedPdf);
        tvSelectedPdfName = findViewById(R.id.tvSelectedPdfName);
        pbUpload = findViewById(R.id.pbUpload);
        btnUploadResource = findViewById(R.id.btnUploadResource);
        listViewResources = findViewById(R.id.listViewResources);

        resourceItemList = new ArrayList<>();
        resourceDisplayList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, resourceDisplayList);
        listViewResources.setAdapter(adapter);

        if (btnSelectPdf != null) {
            btnSelectPdf.setOnClickListener(v -> pickPdfDocument());
        }

        if (btnClearSelectedPdf != null) {
            btnClearSelectedPdf.setOnClickListener(v -> clearSelectedPdf());
        }

        listViewResources.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < resourceItemList.size()) {
                ResourceItem item = resourceItemList.get(position);
                openResourceItem(item);
            }
        });

        loadResourcesFromFirestore();

        btnUploadResource.setOnClickListener(v -> handleUploadAction());
    }

    private void pickPdfDocument() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        pdfPickerLauncher.launch(Intent.createChooser(intent, "Select Academic PDF Document"));
    }

    private void handleSelectedPdfUri(Uri uri) {
        selectedPdfUri = uri;
        selectedPdfFileName = "document.pdf";
        selectedPdfSizeBytes = 0;

        try {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (nameIndex >= 0) {
                    selectedPdfFileName = cursor.getString(nameIndex);
                }
                if (sizeIndex >= 0) {
                    selectedPdfSizeBytes = cursor.getLong(sizeIndex);
                }
                cursor.close();
            }
        } catch (Exception ignored) {}

        String sizeFormatted = selectedPdfSizeBytes > 0
                ? String.format(java.util.Locale.US, " (%.2f MB)", (double) selectedPdfSizeBytes / (1024 * 1024))
                : "";
        if (tvSelectedPdfName != null) {
            tvSelectedPdfName.setText(selectedPdfFileName + sizeFormatted);
        }
        if (btnClearSelectedPdf != null) {
            btnClearSelectedPdf.setVisibility(View.VISIBLE);
        }

        // Auto-fill title if empty
        if (etResourceTitle != null && etResourceTitle.getText().toString().trim().isEmpty()) {
            String titleCandidate = selectedPdfFileName.replaceAll("(?i)\\.pdf$", "").replace("_", " ");
            etResourceTitle.setText(titleCandidate);
        }
    }

    private void clearSelectedPdf() {
        selectedPdfUri = null;
        selectedPdfFileName = null;
        selectedPdfSizeBytes = 0;
        if (tvSelectedPdfName != null) {
            tvSelectedPdfName.setText("No file selected");
        }
        if (btnClearSelectedPdf != null) {
            btnClearSelectedPdf.setVisibility(View.GONE);
        }
    }

    private void openResourceItem(ResourceItem item) {
        String url = item.getFileUrl() != null && !item.getFileUrl().trim().isEmpty()
                ? item.getFileUrl()
                : item.getUrl();
        if (url == null || url.trim().isEmpty() || url.startsWith("https://campus-storage.ref/")) {
            Toast.makeText(this, "No valid link or file associated with this resource.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Intent viewIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(viewIntent);
        } catch (Exception e) {
            Toast.makeText(this, "Cannot open resource link: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void loadResourcesFromFirestore() {
        String collegeId = (currentUser != null && currentUser.getCollegeId() != null && !currentUser.getCollegeId().trim().isEmpty())
                ? currentUser.getCollegeId() : LocalAuthManager.getSafeCollegeId(this, getIntent());

        FirebaseHelper.getResourcesRef()
                .whereEqualTo("collegeId", collegeId)
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    resourceItemList.clear();
                    resourceDisplayList.clear();
                    if (querySnapshots != null && !querySnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : querySnapshots) {
                            ResourceItem item = doc.toObject(ResourceItem.class);
                            if (item != null) {
                                resourceItemList.add(item);
                                String type = item.getResourceType() != null ? item.getResourceType() : "Study Material";
                                String fn = item.getFileName() != null ? " • " + item.getFileName() : "";
                                String display = item.getTitle() + " [" + type + "]" + fn;
                                resourceDisplayList.add(display);
                            }
                        }
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading resources: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void handleUploadAction() {
        String title = etResourceTitle.getText().toString().trim();
        String url = etResourceUrl != null ? etResourceUrl.getText().toString().trim() : "";

        if (title.isEmpty()) {
            Toast.makeText(this, "Resource title required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedPdfUri != null) {
            uploadPdfFileToStorageAndSave(title, url);
        } else if (!url.isEmpty()) {
            saveResourceMetadata(title, url, null, 0, "External Link");
        } else {
            Toast.makeText(this, "Please select a PDF file or provide a web URL link.", Toast.LENGTH_LONG).show();
        }
    }

    private void uploadPdfFileToStorageAndSave(String title, String externalUrl) {
        String collegeId = (currentUser != null && currentUser.getCollegeId() != null && !currentUser.getCollegeId().trim().isEmpty())
                ? currentUser.getCollegeId() : LocalAuthManager.getSafeCollegeId(this, getIntent());

        if (pbUpload != null) pbUpload.setVisibility(View.VISIBLE);
        btnUploadResource.setEnabled(false);

        String uniqueName = System.currentTimeMillis() + "_" + (selectedPdfFileName != null ? selectedPdfFileName : "resource.pdf");
        StorageReference storageRef = FirebaseHelper.getStorageRef()
                .child("resources")
                .child(collegeId)
                .child(uniqueName);

        storageRef.putFile(selectedPdfUri)
                .addOnProgressListener(snapshot -> {
                    if (snapshot.getTotalByteCount() > 0) {
                        int progress = (int) ((100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount());
                        if (pbUpload != null) {
                            pbUpload.setIndeterminate(false);
                            pbUpload.setProgress(progress);
                        }
                    }
                })
                .addOnSuccessListener(taskSnapshot -> {
                    storageRef.getDownloadUrl()
                            .addOnSuccessListener(downloadUri -> {
                                if (pbUpload != null) pbUpload.setVisibility(View.GONE);
                                btnUploadResource.setEnabled(true);
                                String downloadUrl = downloadUri.toString();
                                saveResourceMetadata(title, downloadUrl, selectedPdfFileName, selectedPdfSizeBytes, "PDF Document");
                            })
                            .addOnFailureListener(e -> {
                                if (pbUpload != null) pbUpload.setVisibility(View.GONE);
                                btnUploadResource.setEnabled(true);
                                Toast.makeText(this, "Failed to get download URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    if (pbUpload != null) pbUpload.setVisibility(View.GONE);
                    btnUploadResource.setEnabled(true);
                    Toast.makeText(this, "Firebase Storage upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void saveResourceMetadata(String title, String fileUrl, String fileName, long fileSizeBytes, String resourceType) {
        String collegeId = (currentUser != null && currentUser.getCollegeId() != null && !currentUser.getCollegeId().trim().isEmpty())
                ? currentUser.getCollegeId() : LocalAuthManager.getSafeCollegeId(this, getIntent());
        String deptId = (currentUser != null && currentUser.getDepartmentId() != null) ? currentUser.getDepartmentId() : "DEPT_CSE";
        String teacherUid = currentUser != null ? currentUser.getUid() : "TEACHER_DEFAULT";

        String resourceId = UUID.randomUUID().toString();
        ResourceItem item = new ResourceItem();
        item.setResourceId(resourceId);
        item.setId(resourceId);
        item.setCollegeId(collegeId);
        item.setDepartmentId(deptId);
        item.setTeacherUid(teacherUid);
        item.setUploadedByFacultyId(teacherUid);
        item.setTitle(title);
        item.setUrl(fileUrl);
        item.setFileUrl(fileUrl);
        item.setFileName(fileName);
        item.setFileSizeBytes(fileSizeBytes);
        item.setResourceType(resourceType);
        item.setCreatedAt(System.currentTimeMillis());

        FirebaseHelper.getResourcesRef().document(resourceId).set(item)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Study Resource published successfully!", Toast.LENGTH_SHORT).show();
                    etResourceTitle.setText("");
                    if (etResourceUrl != null) etResourceUrl.setText("");
                    clearSelectedPdf();
                    loadResourcesFromFirestore();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to record resource in Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
