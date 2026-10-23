package com.campusscheduler.ai.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.campusscheduler.ai.R;
import com.campusscheduler.ai.models.Faculty;
import com.campusscheduler.ai.models.Student;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.utils.FirebaseHelper;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

public class MasterDataReconciliationActivity extends AppCompatActivity {

    private static final String TAG = "MasterReconciliation";

    private TextView tvStatus, tvLogs;
    private ProgressBar progressBar;
    private Button btnStartReconciliation, btnVerifyOnly;
    private ScrollView scrollView;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final StringBuilder logBuffer = new StringBuilder();

    // Institutional Canonical Constants
    private static final String CANONICAL_COLLEGE_ID = "COLLEGE_1789406176044";
    private static final String CANONICAL_COLLEGE_NAME = "HKBK College of Engineering";
    private static final String CANONICAL_DEPT_ID = "DEP_1791378689578";
    private static final String CANONICAL_DEPT_NAME = "Computer Science and Engineering";

    // Preserved Admin Accounts
    private static final Set<String> PRESERVED_EMAILS = new HashSet<>(Arrays.asList(
            "admin@gmail.com",
            "adminhkbk@gmail.com",
            "mohammadkaif0702@gmail.com",
            "ruheenatabasum873@gmail.com"
    ));

    private static final Set<String> PRESERVED_UIDS = new HashSet<>(Arrays.asList(
            "uid_1789469665854",
            "uid_1789468438991",
            "zjF54G8zY5TdLWCes6RLlhupeKG3",
            "UZXopK8QXuVtdxcR4WlpYPERBKy1"
    ));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_master_data_reconciliation);

        tvStatus = findViewById(R.id.tvStatus);
        tvLogs = findViewById(R.id.tvLogs);
        progressBar = findViewById(R.id.progressBar);
        btnStartReconciliation = findViewById(R.id.btnStartReconciliation);
        btnVerifyOnly = findViewById(R.id.btnVerifyOnly);
        scrollView = findViewById(R.id.scrollView);

        btnStartReconciliation.setOnClickListener(v -> executeReconciliation());
        btnVerifyOnly.setOnClickListener(v -> executeVerificationOnly());

        appendLog("Master Data Reconciliation Activity Loaded.");
        appendLog("Target Institutional College: " + CANONICAL_COLLEGE_NAME + " (" + CANONICAL_COLLEGE_ID + ")");
        appendLog("Target Department: " + CANONICAL_DEPT_NAME + " (" + CANONICAL_DEPT_ID + ")");

        if (getIntent().getBooleanExtra("autoStart", false)) {
            appendLog("Auto-start trigger received. Starting reconciliation immediately...");
            executeReconciliation();
        }
    }

    private void appendLog(String message) {
        Log.i(TAG, message);
        mainHandler.post(() -> {
            logBuffer.append(message).append("\n");
            if (tvLogs != null) {
                tvLogs.setText(logBuffer.toString());
            }
            if (scrollView != null) {
                scrollView.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    private void setProgress(boolean inProgress, String status) {
        mainHandler.post(() -> {
            if (progressBar != null) progressBar.setVisibility(inProgress ? View.VISIBLE : View.GONE);
            if (btnStartReconciliation != null) btnStartReconciliation.setEnabled(!inProgress);
            if (btnVerifyOnly != null) btnVerifyOnly.setEnabled(!inProgress);
            if (tvStatus != null) tvStatus.setText(status);
        });
    }

    private void executeReconciliation() {
        setProgress(true, "Executing reconciliation...");
        appendLog("\n=======================================================");
        appendLog("STARTING MASTER RECONCILIATION TASK");
        appendLog("=======================================================");

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                FirebaseFirestore db = FirebaseHelper.getFirestore();

                // ---------------------------------------------------------
                // PHASE 1: PURGE OBSOLETE DATA FROM 8 TARGET COLLECTIONS
                // ---------------------------------------------------------
                appendLog("\n--- PHASE 1: CLEANING OBSOLETE ACADEMIC COLLECTIONS ---");
                String[] targetCollections = {
                        "faculty",
                        "students",
                        "subjects",
                        "timetables",
                        "iaTimetables",
                        "classAssignments",
                        "classSessions",
                        "attendance",
                        "leaveRequests"
                };

                for (String colName : targetCollections) {
                    appendLog("Purging collection: " + colName + "...");
                    QuerySnapshot snap = Tasks.await(db.collection(colName).get());
                    int count = snap.size();
                    appendLog("Found " + count + " documents in " + colName);

                    if (count > 0) {
                        deleteInBatches(db, snap.getDocuments());
                        appendLog("Successfully purged " + count + " documents from " + colName);
                    }
                }

                // Clean obsolete test users, strictly preserving Admin/Owner accounts
                appendLog("\nCleaning obsolete non-admin user documents in 'users'...");
                QuerySnapshot usersSnap = Tasks.await(db.collection("users").get());
                List<DocumentSnapshot> obsoleteUsers = new ArrayList<>();
                int preservedCount = 0;

                for (DocumentSnapshot doc : usersSnap.getDocuments()) {
                    String uid = doc.getId();
                    String email = doc.getString("email");
                    String role = doc.getString("role");

                    boolean isPreserved = (email != null && PRESERVED_EMAILS.contains(email.toLowerCase().trim()))
                            || PRESERVED_UIDS.contains(uid)
                            || User.ROLE_COLLEGE_ADMIN.equalsIgnoreCase(role)
                            || User.ROLE_OFFICIAL_OWNER.equalsIgnoreCase(role);

                    if (isPreserved) {
                        preservedCount++;
                        appendLog("Preserved account: " + uid + " (" + email + ", role: " + role + ")");
                    } else {
                        obsoleteUsers.add(doc);
                    }
                }

                if (!obsoleteUsers.isEmpty()) {
                    appendLog("Deleting " + obsoleteUsers.size() + " stale user accounts...");
                    deleteInBatches(db, obsoleteUsers);
                    appendLog("Stale users deleted.");
                }
                appendLog("Institutional admins preserved: " + preservedCount);

                // ---------------------------------------------------------
                // PHASE 2: IMPORT CANONICAL MASTER DATASETS
                // ---------------------------------------------------------
                appendLog("\n--- PHASE 2: IMPORTING MASTER CANONICAL DATA ---");

                // 1. Faculty Master (38 records)
                appendLog("Loading assets/data/new_faculty_master.json...");
                String facultyJson = loadAsset("data/new_faculty_master.json");
                JSONArray facArray = new JSONArray(facultyJson);
                appendLog("Parsed " + facArray.length() + " faculty records from master JSON.");

                WriteBatch facBatch = db.batch();
                int facOps = 0;

                for (int i = 0; i < facArray.length(); i++) {
                    JSONObject obj = facArray.getJSONObject(i);
                    String facId = obj.optString("Faculty ID", "FAC_" + (i + 1)).trim();
                    String name = obj.optString("Faculty Name", "").trim();
                    String shortName = obj.optString("Faculty Short Name", "").trim();
                    String designation = obj.optString("Designation (as provided)", "").trim();
                    String email = obj.optString("Email", "").trim();
                    String phone = obj.optString("Phone", "").trim();
                    String qual = obj.optString("Qualification", "").trim();
                    String exp = obj.optString("Experience", "").trim();
                    String spec = obj.optString("Specialization", "").trim();
                    String expertise = obj.optString("Expertise", "").trim();
                    String status = obj.optString("Profile Status", "PENDING").trim();

                    Faculty f = new Faculty();
                    f.setId(facId);
                    f.setName(name);
                    f.setShortName(shortName);
                    f.setDesignation(designation);
                    f.setDepartmentId(CANONICAL_DEPT_ID);
                    f.setCollegeId(CANONICAL_COLLEGE_ID);
                    f.setStatus(status.isEmpty() ? "PENDING" : status);

                    // Strictly preserve blanks without fake placeholders
                    f.setEmail(email.isEmpty() ? null : email);
                    f.setPhone(phone.isEmpty() ? null : phone);
                    f.setQualification(qual.isEmpty() ? null : qual);
                    f.setExperience(exp.isEmpty() ? null : exp);
                    f.setSpecialization(spec.isEmpty() ? null : spec);
                    f.setExpertise(expertise.isEmpty() ? null : expertise);

                    DocumentReference ref = db.collection("faculty").document(facId);
                    facBatch.set(ref, f);
                    facOps++;

                    if (facOps >= 200) {
                        Tasks.await(facBatch.commit());
                        facBatch = db.batch();
                        facOps = 0;
                    }
                }
                if (facOps > 0) {
                    Tasks.await(facBatch.commit());
                }
                appendLog("Successfully imported " + facArray.length() + " Faculty Master records (status: PENDING, blank contact fields preserved).");

                // 2. Student Master - Section A (65 records)
                appendLog("\nLoading assets/data/new_students_a.json...");
                String stuAJson = loadAsset("data/new_students_a.json");
                JSONArray stuAArray = new JSONArray(stuAJson);
                appendLog("Parsed " + stuAArray.length() + " Section A student records.");

                importStudentBatch(db, stuAArray, "A", 7);
                appendLog("Successfully imported Section A students (" + stuAArray.length() + " records).");

                // 3. Student Master - Section B (62 records)
                appendLog("\nLoading assets/data/new_students_b.json...");
                String stuBJson = loadAsset("data/new_students_b.json");
                JSONArray stuBArray = new JSONArray(stuBJson);
                appendLog("Parsed " + stuBArray.length() + " Section B student records.");

                importStudentBatch(db, stuBArray, "B", 7);
                appendLog("Successfully imported Section B students (" + stuBArray.length() + " records).");

                // ---------------------------------------------------------
                // PHASE 3: POST-IMPORT VALIDATION & VERIFICATION
                // ---------------------------------------------------------
                appendLog("\n--- PHASE 3: VERIFYING POST-RECONCILIATION FIRESTORE COUNTS ---");
                performCountsCheck(db);

                appendLog("\n=======================================================");
                appendLog("MASTER RECONCILIATION FINISHED SUCCESSFULLY!");
                appendLog("=======================================================");
                setProgress(false, "Reconciliation Completed Successfully!");

            } catch (Exception e) {
                appendLog("ERROR DURING RECONCILIATION: " + e.getMessage());
                Log.e(TAG, "Reconciliation failure", e);
                setProgress(false, "Reconciliation Failed: " + e.getMessage());
            }
        });
    }

    private void importStudentBatch(FirebaseFirestore db, JSONArray arr, String section, int semester) throws Exception {
        WriteBatch batch = db.batch();
        int ops = 0;

        for (int i = 0; i < arr.length(); i++) {
            JSONObject obj = arr.getJSONObject(i);
            String usn = obj.optString("USN", "").trim();
            String name = obj.optString("Name", "").trim();
            String phone = obj.optString("Phone Number", "").trim();
            String email = obj.optString("Email ID", "").trim();
            String year = obj.optString("Year", "3rd Year").trim();

            if (usn.isEmpty()) continue;

            Student s = new Student();
            s.setId(usn);
            s.setStudentId(usn);
            s.setUsn(usn);
            s.setRollNumber(usn);
            s.setName(name);
            s.setEmail(email.isEmpty() ? null : email);
            s.setPhone(phone.isEmpty() ? null : phone);
            s.setDepartmentId(CANONICAL_DEPT_ID);
            s.setCollegeId(CANONICAL_COLLEGE_ID);
            s.setSemester(semester);
            s.setSection(section);

            // Write student document
            batch.set(db.collection("students").document(usn), s);
            ops++;

            // Create invited user doc for instant login / lookup
            if (!email.isEmpty()) {
                String key = email.toLowerCase().replaceAll("[^a-z0-9]", "_");
                Map<String, Object> invitation = new HashMap<>();
                invitation.put("email", email);
                invitation.put("name", name);
                invitation.put("usn", usn);
                invitation.put("role", User.ROLE_STUDENT);
                invitation.put("collegeId", CANONICAL_COLLEGE_ID);
                invitation.put("departmentId", CANONICAL_DEPT_ID);
                invitation.put("studentDocId", usn);
                invitation.put("semester", String.valueOf(semester));
                invitation.put("sectionId", section);
                invitation.put("status", "INVITED");

                batch.set(db.collection("user_invitations").document(key), invitation);
                ops++;
            }

            if (ops >= 200) {
                Tasks.await(batch.commit());
                batch = db.batch();
                ops = 0;
            }
        }

        if (ops > 0) {
            Tasks.await(batch.commit());
        }
    }

    private void deleteInBatches(FirebaseFirestore db, List<DocumentSnapshot> docs) throws Exception {
        WriteBatch batch = db.batch();
        int ops = 0;
        for (DocumentSnapshot doc : docs) {
            batch.delete(doc.getReference());
            ops++;
            if (ops >= 200) {
                Tasks.await(batch.commit());
                batch = db.batch();
                ops = 0;
            }
        }
        if (ops > 0) {
            Tasks.await(batch.commit());
        }
    }

    private void executeVerificationOnly() {
        setProgress(true, "Querying Firestore counts...");
        appendLog("\n--- RUNNING COUNT VERIFICATION ---");
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                FirebaseFirestore db = FirebaseHelper.getFirestore();
                performCountsCheck(db);
                setProgress(false, "Verification Complete.");
            } catch (Exception e) {
                appendLog("Verification error: " + e.getMessage());
                setProgress(false, "Verification error.");
            }
        });
    }

    private void performCountsCheck(FirebaseFirestore db) throws Exception {
        int facCount = Tasks.await(db.collection("faculty").get()).size();
        int stuCount = Tasks.await(db.collection("students").get()).size();
        int subCount = Tasks.await(db.collection("subjects").get()).size();
        int ttCount = Tasks.await(db.collection("timetables").get()).size();
        int iaCount = Tasks.await(db.collection("iaTimetables").get()).size();
        int assignCount = Tasks.await(db.collection("classAssignments").get()).size();
        int sessCount = Tasks.await(db.collection("classSessions").get()).size();
        int attCount = Tasks.await(db.collection("attendance").get()).size();
        int leaveCount = Tasks.await(db.collection("leaveRequests").get()).size();
        int userCount = Tasks.await(db.collection("users").get()).size();
        int collegeCount = Tasks.await(db.collection("colleges").get()).size();
        int deptCount = Tasks.await(db.collection("departments").get()).size();

        appendLog("--------------------------------------------------");
        appendLog("FIRESTORE VERIFICATION REPORT:");
        appendLog("Faculty Count       : " + facCount + " (Expected: 38)");
        appendLog("Student Count       : " + stuCount + " (Expected: 127 = 65 Sec A + 62 Sec B)");
        appendLog("Subjects Count      : " + subCount + " (Expected: 0 purged)");
        appendLog("Timetables Count    : " + ttCount + " (Expected: 0 purged)");
        appendLog("IA Timetables Count : " + iaCount + " (Expected: 0 purged)");
        appendLog("Class Assignments   : " + assignCount + " (Expected: 0 purged)");
        appendLog("Class Sessions      : " + sessCount + " (Expected: 0 purged)");
        appendLog("Attendance Records  : " + attCount + " (Expected: 0 purged)");
        appendLog("Leave Requests      : " + leaveCount + " (Expected: 0 purged)");
        appendLog("Admin / User Docs   : " + userCount + " (Admins preserved)");
        appendLog("Colleges (Protected): " + collegeCount + " (Untouched)");
        appendLog("Departments (Prot.) : " + deptCount + " (Untouched)");
        appendLog("--------------------------------------------------");
    }

    private String loadAsset(String path) throws Exception {
        InputStream is = getAssets().open(path);
        byte[] buffer = new byte[is.available()];
        is.read(buffer);
        is.close();
        return new String(buffer, StandardCharsets.UTF_8);
    }
}
