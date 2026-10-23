package com.campusscheduler.ai.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.campusscheduler.ai.models.Faculty;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.utils.ExcelImporter;
import com.campusscheduler.ai.utils.FirebaseHelper;
import com.campusscheduler.ai.utils.LocalAuthManager;
import com.campusscheduler.ai.utils.TeacherProvisioningManager;
import com.campusscheduler.ai.utils.UIUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FacultyActivity extends AppCompatActivity {

    // ============================================================
    // VIEWS
    // ============================================================

    private ImageButton btnBack;
    private ImageButton btnSyncFaculty;
    private FloatingActionButton fabAddFaculty;
    private RecyclerView rvFaculty;

    private final ActivityResultLauncher<String[]> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    processFacultyImportUri(uri);
                }
            });

    // ============================================================
    // DATA
    // ============================================================

    private final List<Faculty> facultyList = new ArrayList<>();

    private FacultyAdapter adapter;

    private User currentUser;

    // ============================================================
    // LIFECYCLE
    // ============================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_faculty);

        currentUser = LocalAuthManager.getCurrentLocalUser(this);
        String safeCollegeId = LocalAuthManager.getSafeCollegeId(this, getIntent());
        if (currentUser == null) {
            currentUser = new User();
            currentUser.setRole(User.ROLE_COLLEGE_ADMIN);
            currentUser.setName("Campus Administrator");
        }
        if (currentUser.getCollegeId() == null || currentUser.getCollegeId().trim().isEmpty()) {
            currentUser.setCollegeId(safeCollegeId);
            LocalAuthManager.saveUserSession(this, currentUser);
        }

        initializeViews();
        setupRecyclerView();
        setupButtons();

        loadFaculty();
    }

    // ============================================================
    // INITIALIZE VIEWS
    // ============================================================

    private void initializeViews() {

        btnBack = findViewById(R.id.btnBack);

        btnSyncFaculty = findViewById(R.id.btnSyncFaculty);

        fabAddFaculty = findViewById(R.id.fabAddFaculty);

        rvFaculty = findViewById(R.id.rvFaculty);
    }

    // ============================================================
    // RECYCLER VIEW
    // ============================================================

    private void setupRecyclerView() {

        if (rvFaculty == null) {
            return;
        }

        rvFaculty.setLayoutManager(
                new LinearLayoutManager(this)
        );

        adapter = new FacultyAdapter(
                facultyList,
                faculty -> showFacultyActionDialog(faculty)
        );

        rvFaculty.setAdapter(adapter);
    }

    // ============================================================
    // BUTTONS
    // ============================================================

    private void setupButtons() {

        if (btnBack != null) {

            btnBack.setOnClickListener(
                    v -> finish()
            );
        }

        if (btnSyncFaculty != null) {

            btnSyncFaculty.setOnClickListener(
                    v -> confirmReconcileFacultyMaster()
            );
        }

        if (fabAddFaculty != null) {

            fabAddFaculty.setOnClickListener(
                    v -> showAddFacultyDialog()
            );
        }
    }

    private void confirmReconcileFacultyMaster() {
        new AlertDialog.Builder(this)
                .setTitle("Reconcile Faculty Master")
                .setMessage("Import and reconcile faculty records from the official HKBK CSE Faculty Master Excel sheet (HKBK_CSE_Faculty_Master_Import.xlsx).\n\n"
                        + "This verifies the 38 faculty entries and assigns workload rules:\n"
                        + "• Professor: 10 hrs/week\n"
                        + "• Associate Professor: 16 hrs/week\n"
                        + "• Assistant Professor: 14 hrs/week\n\n"
                        + "Select the Faculty Master file to proceed:")
                .setPositiveButton("Choose Excel / CSV File", (dialog, which) -> {
                    filePickerLauncher.launch(new String[]{
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            "application/vnd.ms-excel",
                            "text/comma-separated-values",
                            "text/csv",
                            "*/*"
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void processFacultyImportUri(Uri uri) {
        String collegeId = currentUser != null && currentUser.getCollegeId() != null ?
                currentUser.getCollegeId() : LocalAuthManager.getSafeCollegeId(this, getIntent());
        if (currentUser != null && (currentUser.getCollegeId() == null || currentUser.getCollegeId().trim().isEmpty())) {
            currentUser.setCollegeId(collegeId);
        }
        String deptId = currentUser != null && currentUser.getDepartmentId() != null ?
                currentUser.getDepartmentId() : "DEPT_CSE";

        ProgressDialog progress = UIUtils.showLoadingDialog(this, "Importing & reconciling Faculty Master from Excel...");

        new Thread(() -> {
            try (InputStream is = getContentResolver().openInputStream(uri)) {
                List<Faculty> parsedList = ExcelImporter.parseFacultyExcel(is, collegeId, deptId);
                if (parsedList == null || parsedList.isEmpty()) {
                    try (InputStream isCsv = getContentResolver().openInputStream(uri)) {
                        parsedList = ExcelImporter.parseFacultyCsv(isCsv, collegeId, deptId);
                    }
                }

                if (parsedList == null || parsedList.isEmpty()) {
                    runOnUiThread(() -> {
                        progress.dismiss();
                        UIUtils.showAlert(this, "Import Empty", "No valid faculty records found in the selected file.");
                    });
                    return;
                }

                final List<Faculty> finalParsed = parsedList;
                runOnUiThread(() -> {
                    ExcelImporter.importFacultyToFirestore(finalParsed, new ExcelImporter.ImportCallback() {
                        @Override
                        public void onSuccess(int importedCount) {
                            progress.dismiss();
                            UIUtils.showAlert(FacultyActivity.this, "Reconciliation Complete",
                                    "Successfully reconciled " + importedCount + " faculty members with standard workload limits in Cloud Firestore!");
                            loadFaculty();
                        }

                        @Override
                        public void onError(String errorMessage) {
                            progress.dismiss();
                            UIUtils.showAlert(FacultyActivity.this, "Reconciliation Error", errorMessage);
                        }
                    });
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    progress.dismiss();
                    UIUtils.showAlert(FacultyActivity.this, "Import Error", e.getMessage() != null ? e.getMessage() : "Failed to read file.");
                });
            }
        }).start();
    }

    // ============================================================
    // LOAD FACULTY
    // ============================================================

    private void loadFaculty() {

        facultyList.clear();

        notifyAdapter();

        String collegeId = (currentUser != null && currentUser.getCollegeId() != null && !currentUser.getCollegeId().trim().isEmpty())
                ? currentUser.getCollegeId() : LocalAuthManager.getSafeCollegeId(this, getIntent());

        FirebaseHelper
                .getFacultyRef()
                .whereEqualTo("collegeId", collegeId)
                .get()
                .addOnSuccessListener(
                        querySnapshot -> {

                            facultyList.clear();

                            if (querySnapshot != null) {

                                for (DocumentSnapshot doc :
                                        querySnapshot.getDocuments()) {

                                    Faculty faculty =
                                            doc.toObject(Faculty.class);

                                    if (faculty == null) {
                                        continue;
                                    }

                                    if (safe(faculty.getId()).isEmpty()) {

                                        faculty.setId(doc.getId());
                                    }

                                    facultyList.add(faculty);
                                }
                            }

                            // Query real assigned class workloads to ensure accurate workload hours
                            FirebaseHelper.getClassAssignmentsRef()
                                    .whereEqualTo("collegeId", collegeId)
                                    .get()
                                    .addOnSuccessListener(assignSnaps -> {
                                        java.util.Map<String, Integer> workloadMap = new java.util.HashMap<>();
                                        if (assignSnaps != null) {
                                            for (DocumentSnapshot aDoc : assignSnaps) {
                                                com.campusscheduler.ai.models.ClassAssignment ca =
                                                        aDoc.toObject(com.campusscheduler.ai.models.ClassAssignment.class);
                                                if (ca != null && ca.getWeeklyHours() > 0) {
                                                    int hrs = ca.getWeeklyHours();
                                                    if (ca.getTeacherUid() != null) {
                                                        workloadMap.put(ca.getTeacherUid(), workloadMap.getOrDefault(ca.getTeacherUid(), 0) + hrs);
                                                    }
                                                    if (ca.getFacultyId() != null) {
                                                        workloadMap.put(ca.getFacultyId(), workloadMap.getOrDefault(ca.getFacultyId(), 0) + hrs);
                                                    }
                                                    if (ca.getTeacherName() != null) {
                                                        workloadMap.put(ca.getTeacherName().toLowerCase(java.util.Locale.US),
                                                                workloadMap.getOrDefault(ca.getTeacherName().toLowerCase(java.util.Locale.US), 0) + hrs);
                                                    }
                                                }
                                            }
                                        }

                                        for (Faculty f : facultyList) {
                                            int load = 0;
                                            if (f.getId() != null && workloadMap.containsKey(f.getId())) {
                                                load = workloadMap.get(f.getId());
                                            } else if (f.getUid() != null && workloadMap.containsKey(f.getUid())) {
                                                load = workloadMap.get(f.getUid());
                                            } else if (f.getName() != null && workloadMap.containsKey(f.getName().toLowerCase(java.util.Locale.US))) {
                                                load = workloadMap.get(f.getName().toLowerCase(java.util.Locale.US));
                                            }
                                            f.setWorkloadHours(load);

                                            int standardMax = Faculty.getWorkloadForDesignation(f.getDesignation());
                                            if (standardMax > 0 && f.getMaxWorkloadHours() <= 0) {
                                                f.setMaxWorkloadHours(standardMax);
                                                f.setMaxLoadPerWeek(standardMax);
                                            }
                                        }
                                        notifyAdapter();
                                    })
                                    .addOnFailureListener(e -> notifyAdapter());
                        }
                )
                .addOnFailureListener(
                        error -> {

                            notifyAdapter();

                            String message =
                                    "Unable to load faculty: "
                                            + safeError(error);

                            UIUtils.showToast(
                                    this,
                                    message
                            );
                        }
                );
    }

    // ============================================================
    // FACULTY ACTION MENU
    // ============================================================

    private void showFacultyActionDialog(Faculty faculty) {
        if (faculty == null) return;

        boolean active = isFacultyActive(faculty);
        String status = active ? "ACTIVE" : "INACTIVE";
        String displayName = safe(faculty.getName(), "Faculty Member");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(displayName);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        StringBuilder details = new StringBuilder();
        details.append("Faculty ID: ").append(safe(faculty.getId(), "Not available")).append("\n");
        details.append("Status: ").append(status).append("\n");
        details.append("Designation: ").append(safe(faculty.getDesignation(), "Not provided")).append("\n");
        details.append("Department: ").append(safe(faculty.getDepartmentId(), "CSE")).append("\n");
        details.append("Email: ").append(safe(faculty.getEmail(), "Not provided")).append("\n");
        details.append("Phone: ").append(safe(faculty.getPhone(), "Not provided")).append("\n");
        details.append("Qualification: ").append(safe(faculty.getQualification(), "Not provided")).append("\n");
        details.append("Experience: ").append(safe(faculty.getExperience(), "Not provided")).append("\n");
        details.append("Specialization: ").append(safe(faculty.getSpecialization(), "Not provided")).append("\n");
        int assigned = faculty.getWorkloadHours();
        int maxLoad = faculty.getMaxWorkloadHours() > 0 ? faculty.getMaxWorkloadHours() : Faculty.getWorkloadForDesignation(faculty.getDesignation());
        int remaining = Math.max(0, maxLoad - assigned);
        details.append("Weekly Workload Limit: ").append(maxLoad).append(" hrs/week\n");
        details.append("Current Assigned Workload: ").append(assigned).append(" hrs/week\n");
        details.append("Remaining Capacity: ").append(remaining).append(" hrs/week");

        TextView tvDetails = new TextView(this);
        tvDetails.setText(details.toString());
        tvDetails.setTextSize(14f);
        tvDetails.setTextColor(0xFF263238);
        tvDetails.setLineSpacing(4f, 1.1f);
        layout.addView(tvDetails);

        builder.setView(layout);

        builder.setPositiveButton("EDIT", (dialog, which) -> {
            showEditFacultyDialog(faculty);
        });

        builder.setNeutralButton("PROVISION LOGIN", (dialog, which) -> {
            provisionFacultyLogin(faculty);
        });

        builder.setNegativeButton("ACTIONS", (dialog, which) -> {
            showFacultyMoreActions(faculty);
        });

        builder.show();
    }

    private void showFacultyMoreActions(Faculty faculty) {
        if (faculty == null) return;
        boolean active = isFacultyActive(faculty);
        String[] options = {
                active ? "Deactivate Faculty Member" : "Activate Faculty Member",
                "View Full Profile Details",
                "Delete Faculty Member"
        };

        new AlertDialog.Builder(this)
                .setTitle(safe(faculty.getName(), "Faculty Actions"))
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        toggleFacultyStatus(faculty);
                    } else if (which == 1) {
                        showFacultyDetails(faculty);
                    } else if (which == 2) {
                        confirmDeleteFaculty(faculty);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void provisionFacultyLogin(Faculty faculty) {
        if (faculty == null) return;
        String email = safe(faculty.getEmail());
        if (email.isEmpty() || !isValidEmail(email)) {
            new AlertDialog.Builder(this)
                    .setTitle("Email Address Required")
                    .setMessage("This faculty member does not have a valid personal or institutional email address configured yet.\n\nPlease edit the faculty profile and provide their email address first (e.g. teacher@gmail.com, prof@college.edu.in).")
                    .setPositiveButton("EDIT PROFILE", (d, w) -> showEditFacultyDialog(faculty))
                    .setNegativeButton("CANCEL", null)
                    .show();
            return;
        }

        String displayName = safe(faculty.getName(), "Faculty Member");
        String[] options = {
                "Provision Login Access (Create Auth + Send Setup Email)",
                "Resend Password Setup / Verification Email",
                "View Login Account Status"
        };

        new AlertDialog.Builder(this)
                .setTitle("Teacher Provisioning • " + displayName)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        startProvisionTeacherFlow(faculty);
                    } else if (which == 1) {
                        resendFacultySetupEmail(faculty);
                    } else if (which == 2) {
                        showFacultyAccountInfo(faculty);
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void startProvisionTeacherFlow(Faculty faculty) {
        String email = safe(faculty.getEmail());
        if (email.isEmpty() || !isValidEmail(email)) {
            UIUtils.showAlert(this, "Email Required",
                    "Please provide a valid personal or institutional email address (e.g. teacher@gmail.com, prof@college.edu.in).");
            return;
        }

        ProgressDialog pd = UIUtils.showLoadingDialog(this, "Provisioning teacher in Firebase Auth & Cloud Firestore...");
        TeacherProvisioningManager.provisionTeacher(this, currentUser, faculty, new TeacherProvisioningManager.ProvisioningCallback() {
            @Override
            public void onProgress(String message) {
                runOnUiThread(() -> {
                    if (pd != null && pd.isShowing()) {
                        pd.setMessage(message);
                    }
                });
            }

            @Override
            public void onSuccess(TeacherProvisioningManager.ProvisioningResult result) {
                runOnUiThread(() -> {
                    if (pd != null && pd.isShowing()) pd.dismiss();
                    new AlertDialog.Builder(FacultyActivity.this)
                            .setTitle("Teacher Provisioning Successful")
                            .setMessage(result.detailedStatus)
                            .setPositiveButton("OK", null)
                            .show();
                    loadFaculty();
                });
            }

            @Override
            public void onError(String title, String errorMessage) {
                runOnUiThread(() -> {
                    if (pd != null && pd.isShowing()) pd.dismiss();
                    new AlertDialog.Builder(FacultyActivity.this)
                            .setTitle(title)
                            .setMessage(errorMessage)
                            .setPositiveButton("OK", null)
                            .show();
                });
            }
        });
    }

    private void resendFacultySetupEmail(Faculty faculty) {
        String email = safe(faculty.getEmail());
        if (email.isEmpty() || !isValidEmail(email)) {
            UIUtils.showAlert(this, "Email Required", "Please provide a valid email address first.");
            return;
        }

        ProgressDialog pd = UIUtils.showLoadingDialog(this, "Dispatching setup email via Firebase Authentication...");
        TeacherProvisioningManager.resendSetupEmail(email, new TeacherProvisioningManager.ProvisioningCallback() {
            @Override
            public void onProgress(String message) {
                runOnUiThread(() -> {
                    if (pd != null && pd.isShowing()) pd.setMessage(message);
                });
            }

            @Override
            public void onSuccess(TeacherProvisioningManager.ProvisioningResult result) {
                runOnUiThread(() -> {
                    if (pd != null && pd.isShowing()) pd.dismiss();
                    UIUtils.showAlert(FacultyActivity.this, "Email Dispatched", result.detailedStatus);
                });
            }

            @Override
            public void onError(String title, String errorMessage) {
                runOnUiThread(() -> {
                    if (pd != null && pd.isShowing()) pd.dismiss();
                    UIUtils.showAlert(FacultyActivity.this, title, errorMessage);
                });
            }
        });
    }

    private void showFacultyAccountInfo(Faculty faculty) {
        StringBuilder sb = new StringBuilder();
        sb.append("Teacher: ").append(faculty.getName()).append("\n");
        sb.append("Email: ").append(safe(faculty.getEmail(), "Not set")).append("\n");
        sb.append("Designation: ").append(safe(faculty.getDesignation(), "Faculty")).append("\n");
        sb.append("Department: ").append(safe(faculty.getDepartmentId(), "DEPT_CSE")).append("\n");
        sb.append("College ID: ").append(safe(faculty.getCollegeId(), "Not set")).append("\n");
        sb.append("Linked UID: ").append(faculty.getUid() != null ? faculty.getUid() : "Pending Provisioning").append("\n\n");
        sb.append("Authentication Status: ");
        if (faculty.getUid() != null && !faculty.getUid().trim().isEmpty()) {
            sb.append("Provisioned & Synchronized in Firestore\n");
            sb.append("The teacher can sign in using their email and password (or set/reset via the setup link).");
        } else {
            sb.append("Not provisioned yet.\nTap 'PROVISION NOW' to create authentication access and dispatch the setup email.");
        }

        new AlertDialog.Builder(this)
                .setTitle("Teacher Login Account Status")
                .setMessage(sb.toString())
                .setPositiveButton("CLOSE", null)
                .setNeutralButton("PROVISION NOW", (d, w) -> startProvisionTeacherFlow(faculty))
                .show();
    }

    // ============================================================
    // VIEW DETAILS
    // ============================================================

    private void showFacultyDetails(Faculty faculty) {

        StringBuilder details =
                new StringBuilder();

        details.append("Faculty ID: ")
                .append(
                        safe(
                                faculty.getId(),
                                "Not available"
                        )
                );

        details.append("\n\nName: ")
                .append(
                        safe(
                                faculty.getName(),
                                "Not provided"
                        )
                );

        details.append("\nDesignation: ")
                .append(
                        safe(
                                faculty.getDesignation(),
                                "Not provided"
                        )
                );

        details.append("\nDepartment ID: ")
                .append(
                        safe(
                                faculty.getDepartmentId(),
                                "Not provided"
                        )
                );

        details.append("\nEmail: ")
                .append(
                        safe(
                                faculty.getEmail(),
                                "Not provided"
                        )
                );

        details.append("\nPhone: ")
                .append(
                        safe(
                                faculty.getPhone(),
                                "Not provided"
                        )
                );

        details.append("\nQualification: ")
                .append(
                        safe(
                                faculty.getQualification(),
                                "Not provided"
                        )
                );

        details.append("\nExperience: ")
                .append(
                        safe(
                                faculty.getExperience(),
                                "Not provided"
                        )
                );

        details.append("\nSpecialization: ")
                .append(
                        safe(
                                faculty.getSpecialization(),
                                "Not provided"
                        )
                );

        details.append("\nExpertise: ")
                .append(
                        safe(
                                faculty.getExpertise(),
                                "Not provided"
                        )
                );

        details.append("\nMaximum Workload: ")
                .append(
                        faculty.getMaxWorkloadHours()
                )
                .append(" hrs/week");

        details.append("\nCurrent Workload: ")
                .append(
                        faculty.getWorkloadHours()
                )
                .append(" hrs/week");

        details.append("\nStatus: ")
                .append(
                        isFacultyActive(faculty)
                                ? "ACTIVE"
                                : "INACTIVE"
                );

        new AlertDialog.Builder(this)
                .setTitle("Faculty Details")
                .setMessage(details.toString())
                .setPositiveButton(
                        "CLOSE",
                        null
                )
                .show();
    }

    // ============================================================
    // ADD FACULTY
    // ============================================================

    private void showAddFacultyDialog() {

        if (currentUser == null) {

            UIUtils.showToast(
                    this,
                    "Admin profile not available."
            );

            return;
        }

        String collegeId =
                safe(currentUser.getCollegeId());

        String departmentId =
                safe(currentUser.getDepartmentId());

        if (collegeId.isEmpty()) {

            UIUtils.showToast(
                    this,
                    "College ID is missing."
            );

            return;
        }

        LinearLayout layout =
                createDialogLayout();

        EditText etName =
                createEditText("Faculty Name *");

        EditText etEmail =
                createEditText("Email (personal or institutional)");
        etEmail.setHint("e.g. teacher@gmail.com, prof@college.edu.in");

        etEmail.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        );

        EditText etPhone =
                createEditText("Phone (optional)");

        etPhone.setInputType(
                InputType.TYPE_CLASS_PHONE
        );

        EditText etDesignation =
                createEditText("Designation");

        EditText etQualification =
                createEditText("Qualification");

        EditText etExperience =
                createEditText("Experience");

        EditText etSpecialization =
                createEditText("Specialization");

        EditText etExpertise =
                createEditText("Expertise");

        layout.addView(etName);
        layout.addView(etEmail);
        layout.addView(etPhone);
        layout.addView(etDesignation);
        layout.addView(etQualification);
        layout.addView(etExperience);
        layout.addView(etSpecialization);
        layout.addView(etExpertise);

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle("Add Faculty Member")
                        .setView(layout)
                        .setNegativeButton(
                                "CANCEL",
                                null
                        )
                        .setPositiveButton(
                                "SAVE",
                                null
                        )
                        .create();

        dialog.setOnShowListener(
                ignored -> {

                    dialog.getButton(
                            AlertDialog.BUTTON_POSITIVE
                    ).setOnClickListener(
                            v -> {

                                String name =
                                        getText(etName);

                                if (name.isEmpty()) {

                                    etName.setError(
                                            "Faculty name is required"
                                    );

                                    etName.requestFocus();

                                    return;
                                }

                                String email =
                                        getText(etEmail);

                                if (!email.isEmpty()
                                        && !isValidEmail(email)) {

                                    etEmail.setError(
                                            "Enter a valid email"
                                    );

                                    etEmail.requestFocus();

                                    return;
                                }

                                String phone =
                                        getText(etPhone);

                                String designation =
                                        getText(etDesignation);

                                int maxLoad =
                                        Faculty.getWorkloadForDesignation(
                                                designation
                                        );

                                String facultyId =
                                        "FAC_"
                                                + System.currentTimeMillis();

                                Faculty faculty =
                                        new Faculty(
                                                facultyId,
                                                "",
                                                collegeId,
                                                departmentId,
                                                name,
                                                email,
                                                phone,
                                                designation,
                                                getText(
                                                        etSpecialization
                                                ),
                                                maxLoad
                                        );

                                faculty.setQualification(
                                        getText(etQualification)
                                );

                                faculty.setExperience(
                                        getText(etExperience)
                                );

                                faculty.setExpertise(
                                        getText(etExpertise)
                                );

                                faculty.setWorkloadHours(0);

                                faculty.setMaxWorkloadHours(
                                        maxLoad
                                );

                                faculty.setMaxLoadPerWeek(
                                        maxLoad
                                );

                                faculty.setStatus("ACTIVE");

                                FirebaseHelper
                                        .getFacultyRef()
                                        .document(facultyId)
                                        .set(faculty)
                                        .addOnSuccessListener(
                                                unused -> {

                                                    dialog.dismiss();

                                                    UIUtils.showToast(
                                                            this,
                                                            "Faculty saved."
                                                    );

                                                    loadFaculty();

                                                    if (!email.isEmpty() && isValidEmail(email)) {
                                                        new AlertDialog.Builder(this)
                                                                .setTitle("Provision Login Access?")
                                                                .setMessage("Faculty profile for " + name + " (" + email + ") saved.\n\nWould you like to provision login access and send an account setup email now?")
                                                                .setPositiveButton("PROVISION NOW", (d, w) -> startProvisionTeacherFlow(faculty))
                                                                .setNegativeButton("LATER", null)
                                                                .show();
                                                    }
                                                }
                                        )
                                        .addOnFailureListener(
                                                error -> {

                                                    String message =
                                                            "Save failed: "
                                                                    + safeError(error);

                                                    UIUtils.showToast(
                                                            this,
                                                            message
                                                    );
                                                }
                                        );
                            }
                    );
                }
        );

        dialog.show();
    }

    // ============================================================
    // EDIT FACULTY
    // ============================================================

    private void showEditFacultyDialog(Faculty faculty) {

        if (faculty == null) {
            return;
        }

        LinearLayout layout =
                createDialogLayout();

        EditText etName =
                createEditText("Faculty Name");

        EditText etEmail =
                createEditText("Email (personal or institutional)");
        etEmail.setHint("e.g. teacher@gmail.com, prof@college.edu.in");

        etEmail.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        );

        EditText etPhone =
                createEditText("Phone");

        EditText etDesignation =
                createEditText("Designation");

        EditText etQualification =
                createEditText("Qualification");

        EditText etExperience =
                createEditText("Experience");

        EditText etSpecialization =
                createEditText("Specialization");

        EditText etShortName =
                createEditText("Short Name (e.g. MK)");

        EditText etDepartmentId =
                createEditText("Department ID (e.g. DEPT_CSE)");

        EditText etExpertise =
                createEditText("Expertise");

        etName.setText(
                safe(faculty.getName())
        );

        etShortName.setText(
                safe(faculty.getShortName())
        );

        etDepartmentId.setText(
                safe(faculty.getDepartmentId(), "DEPT_CSE")
        );

        etEmail.setText(
                safe(faculty.getEmail())
        );

        etPhone.setText(
                safe(faculty.getPhone())
        );

        etDesignation.setText(
                safe(faculty.getDesignation())
        );

        etQualification.setText(
                safe(faculty.getQualification())
        );

        etExperience.setText(
                safe(faculty.getExperience())
        );

        etSpecialization.setText(
                safe(faculty.getSpecialization())
        );

        etExpertise.setText(
                safe(faculty.getExpertise())
        );

        final int assignedHours = faculty.getWorkloadHours();
        final TextView tvWorkloadStatus = new TextView(this);
        tvWorkloadStatus.setTextSize(13f);
        tvWorkloadStatus.setTextColor(0xFF0D47A1);
        tvWorkloadStatus.setPadding(0, 10, 0, 10);
        int initialMax = Faculty.getWorkloadForDesignation(faculty.getDesignation());
        tvWorkloadStatus.setText("Workload: Assigned " + assignedHours + "h | Max " + initialMax + "h | Remaining " + Math.max(0, initialMax - assignedHours) + "h");

        etDesignation.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int dynMax = Faculty.getWorkloadForDesignation(s != null ? s.toString() : "");
                int dynRem = Math.max(0, dynMax - assignedHours);
                tvWorkloadStatus.setText("Workload: Assigned " + assignedHours + "h | Max " + dynMax + "h | Remaining " + dynRem + "h");
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        layout.addView(etName);
        layout.addView(etShortName);
        layout.addView(etDepartmentId);
        layout.addView(etEmail);
        layout.addView(etPhone);
        layout.addView(etDesignation);
        layout.addView(tvWorkloadStatus);
        layout.addView(etQualification);
        layout.addView(etExperience);
        layout.addView(etSpecialization);
        layout.addView(etExpertise);

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle("Edit Faculty")
                        .setView(layout)
                        .setNegativeButton(
                                "CANCEL",
                                null
                        )
                        .setPositiveButton(
                                "SAVE",
                                null
                        )
                        .create();

        dialog.setOnShowListener(
                ignored -> {

                    dialog.getButton(
                            AlertDialog.BUTTON_POSITIVE
                    ).setOnClickListener(
                            v -> {

                                String name =
                                        getText(etName);

                                if (name.isEmpty()) {

                                    etName.setError(
                                            "Name is required"
                                    );

                                    etName.requestFocus();

                                    return;
                                }

                                String email =
                                        getText(etEmail);

                                if (!email.isEmpty()
                                        && !isValidEmail(email)) {

                                    etEmail.setError(
                                            "Invalid email"
                                    );

                                    etEmail.requestFocus();

                                    return;
                                }

                                String designation =
                                        getText(etDesignation);

                                int workload =
                                        Faculty.getWorkloadForDesignation(
                                                designation
                                        );

                                String finalId =
                                        safe(faculty.getId());

                                if (finalId.isEmpty()) {

                                    UIUtils.showToast(
                                            this,
                                            "Faculty ID is missing."
                                    );

                                    return;
                                }

                                FirebaseHelper
                                        .getFacultyRef()
                                        .document(finalId)
                                        .update(

                                                "name",
                                                name,

                                                "shortName",
                                                getText(etShortName),

                                                "departmentId",
                                                getText(etDepartmentId),

                                                "email",
                                                email,

                                                "phone",
                                                getText(etPhone),

                                                "designation",
                                                designation,

                                                "qualification",
                                                getText(
                                                        etQualification
                                                ),

                                                "experience",
                                                getText(
                                                        etExperience
                                                ),

                                                "specialization",
                                                getText(
                                                        etSpecialization
                                                ),

                                                "expertise",
                                                getText(
                                                        etExpertise
                                                ),

                                                "maxWorkloadHours",
                                                workload,

                                                "maxLoadPerWeek",
                                                workload
                                        )
                                        .addOnSuccessListener(
                                                unused -> {

                                                    dialog.dismiss();

                                                    UIUtils.showToast(
                                                            this,
                                                            "Faculty updated."
                                                    );

                                                    loadFaculty();

                                                    if (!email.isEmpty() && isValidEmail(email) && !email.equalsIgnoreCase(safe(faculty.getEmail()))) {
                                                        faculty.setEmail(email);
                                                        new AlertDialog.Builder(this)
                                                                .setTitle("Email Updated • Provision Login?")
                                                                .setMessage("Faculty email address was updated to " + email + ".\n\nWould you like to provision/update login access and dispatch an account setup email now?")
                                                                .setPositiveButton("PROVISION NOW", (d, w) -> startProvisionTeacherFlow(faculty))
                                                                .setNegativeButton("LATER", null)
                                                                .show();
                                                    }
                                                }
                                        )
                                        .addOnFailureListener(
                                                error -> {

                                                    String message =
                                                            "Update failed: "
                                                                    + safeError(error);

                                                    UIUtils.showToast(
                                                            this,
                                                            message
                                                    );
                                                }
                                        );
                            }
                    );
                }
        );

        dialog.show();
    }

    // ============================================================
    // ACTIVATE / DEACTIVATE
    // ============================================================

    private void toggleFacultyStatus(Faculty faculty) {

        if (faculty == null) {
            return;
        }

        String id =
                safe(faculty.getId());

        if (id.isEmpty()) {

            UIUtils.showToast(
                    this,
                    "Faculty ID is missing."
            );

            return;
        }

        boolean currentlyActive =
                isFacultyActive(faculty);

        boolean newActive =
                !currentlyActive;

        String action =
                newActive
                        ? "activate"
                        : "deactivate";

        new AlertDialog.Builder(this)
                .setTitle(
                        newActive
                                ? "Activate Faculty?"
                                : "Deactivate Faculty?"
                )
                .setMessage(
                        "Are you sure you want to "
                                + action
                                + " "
                                + safe(
                                faculty.getName(),
                                "this faculty member"
                        )
                                + "?"
                )
                .setNegativeButton(
                        "CANCEL",
                        null
                )
                .setPositiveButton(
                        newActive
                                ? "ACTIVATE"
                                : "DEACTIVATE",
                        (dialog, which) ->
                                updateFacultyStatus(
                                        faculty,
                                        newActive
                                )
                )
                .show();
    }

    private void updateFacultyStatus(
            Faculty faculty,
            boolean active
    ) {

        String id =
                safe(faculty.getId());

        if (id.isEmpty()) {
            return;
        }

        String newStatus =
                active
                        ? "ACTIVE"
                        : "INACTIVE";

        FirebaseHelper
                .getFacultyRef()
                .document(id)
                .update(
                        "status",
                        newStatus
                )
                .addOnSuccessListener(
                        unused -> {

                            String uid =
                                    safe(faculty.getUid());

                            if (!uid.isEmpty()) {

                                FirebaseHelper
                                        .getUsersRef()
                                        .document(uid)
                                        .update(
                                                "active",
                                                active,
                                                "status",
                                                newStatus
                                        );
                            }

                            UIUtils.showToast(
                                    this,
                                    active
                                            ? "Faculty activated."
                                            : "Faculty deactivated."
                            );

                            loadFaculty();
                        }
                )
                .addOnFailureListener(
                        error -> {

                            String message =
                                    "Status update failed: "
                                            + safeError(error);

                            UIUtils.showToast(
                                    this,
                                    message
                            );
                        }
                );
    }

    // ============================================================
    // DELETE FACULTY
    // ============================================================

    private void confirmDeleteFaculty(Faculty faculty) {

        if (faculty == null) {
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Delete Faculty?")
                .setMessage(
                        "This will permanently remove "
                                + safe(
                                faculty.getName(),
                                "this faculty member"
                        )
                                + " from the Faculty master data.\n\n"
                                + "This action cannot be undone."
                )
                .setNegativeButton(
                        "CANCEL",
                        null
                )
                .setPositiveButton(
                        "DELETE",
                        (dialog, which) ->
                                deleteFaculty(faculty)
                )
                .show();
    }

    private void deleteFaculty(Faculty faculty) {

        String id =
                safe(faculty.getId());

        if (id.isEmpty()) {

            UIUtils.showToast(
                    this,
                    "Faculty ID is missing."
            );

            return;
        }

        FirebaseHelper
                .getFacultyRef()
                .document(id)
                .delete()
                .addOnSuccessListener(
                        unused -> {

                            String uid =
                                    safe(faculty.getUid());

                            if (!uid.isEmpty()) {

                                FirebaseHelper
                                        .getUsersRef()
                                        .document(uid)
                                        .delete();
                            }

                            deleteInvitationIfPossible(
                                    faculty
                            );

                            UIUtils.showToast(
                                    this,
                                    "Faculty deleted."
                            );

                            loadFaculty();
                        }
                )
                .addOnFailureListener(
                        error -> {

                            String message =
                                    "Delete failed: "
                                            + safeError(error);

                            UIUtils.showToast(
                                    this,
                                    message
                            );
                        }
                );
    }

    // ============================================================
    // DELETE INVITATION
    // ============================================================

    private void deleteInvitationIfPossible(
            Faculty faculty
    ) {

        String email =
                safe(faculty.getEmail())
                        .toLowerCase(Locale.ROOT);

        if (!isValidEmail(email)) {
            return;
        }

        String key =
                email.replaceAll(
                        "[^a-z0-9]",
                        "_"
                );

        FirebaseHelper
                .getFirestore()
                .collection("user_invitations")
                .document(key)
                .delete();
    }

    // ============================================================
    // STATUS HELPER
    // ============================================================

    private boolean isFacultyActive(
            Faculty faculty
    ) {

        if (faculty == null) {
            return false;
        }

        String status =
                safe(faculty.getStatus());

        if (status.isEmpty()) {
            return true;
        }

        return status.equalsIgnoreCase("ACTIVE");
    }

    // ============================================================
    // UI HELPERS
    // ============================================================

    private LinearLayout createDialogLayout() {

        LinearLayout layout =
                new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        int padding =
                dpToPx(20);

        layout.setPadding(
                padding,
                dpToPx(8),
                padding,
                dpToPx(8)
        );

        return layout;
    }

    private EditText createEditText(
            String hint
    ) {

        EditText editText =
                new EditText(this);

        editText.setHint(hint);

        editText.setPadding(
                0,
                dpToPx(8),
                0,
                dpToPx(8)
        );

        return editText;
    }

    private int dpToPx(int dp) {

        float density =
                getResources()
                        .getDisplayMetrics()
                        .density;

        return Math.round(
                dp * density
        );
    }

    // ============================================================
    // ADAPTER REFRESH
    // ============================================================

    private void notifyAdapter() {

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    // ============================================================
    // TEXT HELPERS
    // ============================================================

    private String getText(
            EditText editText
    ) {

        if (editText == null) {
            return "";
        }

        return editText
                .getText()
                .toString()
                .trim();
    }

    private String safe(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value.trim();
    }

    private String safe(
            String value,
            String fallback
    ) {

        String clean =
                safe(value);

        if (clean.isEmpty()) {
            return fallback;
        }

        return clean;
    }

    private String safeError(
            Exception error
    ) {

        if (error == null) {
            return "Unknown error";
        }

        String message =
                error.getMessage();

        if (message == null
                || message.trim().isEmpty()) {

            return "Unknown error";
        }

        return message;
    }

    private boolean isValidEmail(
            String email
    ) {
        return TeacherProvisioningManager.isValidEmail(email);
    }

    // ============================================================
    // FACULTY ADAPTER
    // ============================================================

    private static class FacultyAdapter
            extends RecyclerView.Adapter<
            FacultyAdapter.FacultyViewHolder> {

        interface OnFacultyClick {
            void onClick(Faculty faculty);
        }

        private final List<Faculty> list;

        private final OnFacultyClick listener;

        FacultyAdapter(
                List<Faculty> list,
                OnFacultyClick listener
        ) {

            this.list = list;

            this.listener = listener;
        }

        @Override
        public FacultyViewHolder onCreateViewHolder(
                ViewGroup parent,
                int viewType
        ) {

            View view =
                    LayoutInflater
                            .from(parent.getContext())
                            .inflate(
                                    R.layout.item_generic,
                                    parent,
                                    false
                            );

            return new FacultyViewHolder(view);
        }

        @Override
        public void onBindViewHolder(
                FacultyViewHolder holder,
                int position
        ) {

            Faculty faculty =
                    list.get(position);

            String name =
                    safeStatic(
                            faculty.getName()
                    );

            if (name.isEmpty()) {
                name = "Unnamed Faculty";
            }

            String designation =
                    safeStatic(
                            faculty.getDesignation()
                    );

            if (designation.isEmpty()) {
                designation = "Designation pending";
            }

            String status =
                    safeStatic(
                            faculty.getStatus()
                    );

            if (status.isEmpty()) {
                status = "ACTIVE";
            }

            String email =
                    safeStatic(
                            faculty.getEmail()
                    );

            if (email.isEmpty()) {
                email = "Email not provided";
            }

            String title =
                    name
                            + " ("
                            + designation
                            + ")";

            int assigned = faculty.getWorkloadHours();
            int maxLoad = faculty.getMaxWorkloadHours() > 0 ? faculty.getMaxWorkloadHours() : Faculty.getWorkloadForDesignation(faculty.getDesignation());
            int remaining = Math.max(0, maxLoad - assigned);

            String subtitle =
                    "Status: "
                            + status
                            + " | "
                            + email
                            + "\nWorkload: "
                            + assigned
                            + " / "
                            + maxLoad
                            + " hrs/wk ("
                            + remaining
                            + " hrs capacity left)";

            holder.tvTitle.setText(title);

            holder.tvSubtitle.setText(subtitle);

            holder.itemView.setOnClickListener(
                    v -> {

                        if (listener != null) {
                            listener.onClick(faculty);
                        }
                    }
            );
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class FacultyViewHolder
                extends RecyclerView.ViewHolder {

            TextView tvTitle;

            TextView tvSubtitle;

            FacultyViewHolder(View itemView) {

                super(itemView);

                tvTitle =
                        itemView.findViewById(
                                R.id.tvTitle
                        );

                tvSubtitle =
                        itemView.findViewById(
                                R.id.tvSubtitle
                        );
            }
        }

        private static String safeStatic(
                String value
        ) {

            if (value == null) {
                return "";
            }

            return value.trim();
        }
    }
}