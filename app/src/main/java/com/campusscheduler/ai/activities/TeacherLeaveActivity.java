package com.campusscheduler.ai.activities;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
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
import com.campusscheduler.ai.engine.TeacherSubstitutionEngine;
import com.campusscheduler.ai.models.ClassSession;
import com.campusscheduler.ai.models.Faculty;
import com.campusscheduler.ai.models.LeaveRequest;
import com.campusscheduler.ai.models.NotificationItem;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.utils.FirebaseHelper;
import com.campusscheduler.ai.utils.LocalAuthManager;
import com.campusscheduler.ai.utils.UIUtils;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class TeacherLeaveActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvLeaveHeaderTitle;

    // Teacher View
    private ScrollView svTeacherContainer;
    private EditText etStartDate, etEndDate, etReason;
    private Button btnApplyLeave;
    private TextView tvTeacherEmptyLeave;
    private LinearLayout llTeacherLeaveHistoryList;

    // Admin View
    private LinearLayout llAdminApprovalContainer;
    private TextView tvAdminEmptyLeave;
    private ListView lvPendingLeaveRequests;
    private final List<LeaveRequest> pendingRequests = new ArrayList<>();
    private final List<String> requestDisplayList = new ArrayList<>();
    private ArrayAdapter<String> leaveAdapter;

    private User currentUser;
    private boolean isAdmin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_leave);

        currentUser = LocalAuthManager.getCurrentLocalUser(this);
        String safeCollegeId = LocalAuthManager.getSafeCollegeId(this, getIntent());
        String teacherUid = getIntent().getStringExtra("teacherUid");
        String teacherName = getIntent().getStringExtra("teacherName");

        if (currentUser == null) {
            currentUser = new User();
            currentUser.setRole(User.ROLE_TEACHER);
            currentUser.setName("Dr.Pushpa Mohan");
        }
        if (currentUser.getCollegeId() == null || currentUser.getCollegeId().trim().isEmpty()) {
            currentUser.setCollegeId(safeCollegeId);
        }
        if (teacherUid != null && !teacherUid.trim().isEmpty()) {
            currentUser.setUid(teacherUid.trim());
        }
        if (teacherName != null && !teacherName.trim().isEmpty()) {
            currentUser.setName(teacherName.trim());
        }
        LocalAuthManager.saveUserSession(this, currentUser);

        isAdmin = User.ROLE_COLLEGE_ADMIN.equalsIgnoreCase(currentUser.getRole())
                || User.ROLE_ADMIN.equalsIgnoreCase(currentUser.getRole())
                || User.ROLE_OFFICIAL_OWNER.equalsIgnoreCase(currentUser.getRole())
                || (getIntent() != null && getIntent().getBooleanExtra("isAdmin", false));

        btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        tvLeaveHeaderTitle = findViewById(R.id.tvLeaveHeaderTitle);
        svTeacherContainer = findViewById(R.id.svTeacherContainer);
        llAdminApprovalContainer = findViewById(R.id.llAdminApprovalContainer);

        etStartDate = findViewById(R.id.etStartDate);
        etEndDate = findViewById(R.id.etEndDate);
        etReason = findViewById(R.id.etReason);
        btnApplyLeave = findViewById(R.id.btnApplyLeave);
        tvTeacherEmptyLeave = findViewById(R.id.tvTeacherEmptyLeave);
        llTeacherLeaveHistoryList = findViewById(R.id.llTeacherLeaveHistoryList);

        tvAdminEmptyLeave = findViewById(R.id.tvAdminEmptyLeave);
        lvPendingLeaveRequests = findViewById(R.id.lvPendingLeaveRequests);

        if (isAdmin) {
            if (tvLeaveHeaderTitle != null) tvLeaveHeaderTitle.setText("Leave Approvals & AI Substitution");
            if (svTeacherContainer != null) svTeacherContainer.setVisibility(View.GONE);
            if (llAdminApprovalContainer != null) llAdminApprovalContainer.setVisibility(View.VISIBLE);

            leaveAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, requestDisplayList);
            if (lvPendingLeaveRequests != null) {
                lvPendingLeaveRequests.setAdapter(leaveAdapter);
                lvPendingLeaveRequests.setOnItemClickListener((parent, view, position, id) -> {
                    if (position >= 0 && position < pendingRequests.size()) {
                        showAdminRequestActionDialog(pendingRequests.get(position));
                    }
                });
            }
            loadAdminPendingRequests();
        } else {
            if (tvLeaveHeaderTitle != null) tvLeaveHeaderTitle.setText("Apply Leave & Track Substitution");
            if (svTeacherContainer != null) svTeacherContainer.setVisibility(View.VISIBLE);
            if (llAdminApprovalContainer != null) llAdminApprovalContainer.setVisibility(View.GONE);

            setupDatePickers();
            if (btnApplyLeave != null) {
                btnApplyLeave.setOnClickListener(v -> submitTeacherLeaveRequest());
            }
            loadTeacherLeaveHistory();
        }
    }

    private void setupDatePickers() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        if (etStartDate != null) {
            etStartDate.setOnClickListener(v -> {
                new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                    cal.set(year, month, dayOfMonth);
                    etStartDate.setText(sdf.format(cal.getTime()));
                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
            });
        }

        if (etEndDate != null) {
            etEndDate.setOnClickListener(v -> {
                new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                    cal.set(year, month, dayOfMonth);
                    etEndDate.setText(sdf.format(cal.getTime()));
                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
            });
        }
    }

    private void submitTeacherLeaveRequest() {
        String startDate = etStartDate.getText().toString().trim();
        String endDate = etEndDate.getText().toString().trim();
        String reason = etReason.getText().toString().trim();

        if (startDate.isEmpty() || endDate.isEmpty() || reason.isEmpty()) {
            Toast.makeText(this, "Please fill in start date, end date, and reason.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (startDate.compareTo(endDate) > 0) {
            Toast.makeText(this, "Start date cannot be after end date.", Toast.LENGTH_SHORT).show();
            return;
        }

        String collegeId = (currentUser != null && currentUser.getCollegeId() != null && !currentUser.getCollegeId().trim().isEmpty())
                ? currentUser.getCollegeId().trim() : "COLLEGE_1789406176044";
        String applicantId = currentUser != null && currentUser.getUid() != null ? currentUser.getUid() : "TEACHER_DEFAULT";
        String applicantName = currentUser != null && currentUser.getName() != null ? currentUser.getName() : "Faculty Member";

        String reqId = "LEAVE_" + System.currentTimeMillis();
        LeaveRequest req = new LeaveRequest(reqId, collegeId, applicantId, applicantName, "TEACHER", startDate, endDate, reason);

        FirebaseHelper.getLeaveRequestsRef().document(reqId).set(req)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Leave request submitted successfully for administrative review.", Toast.LENGTH_LONG).show();
                    etStartDate.setText("");
                    etEndDate.setText("");
                    etReason.setText("");
                    loadTeacherLeaveHistory();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to submit leave request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadTeacherLeaveHistory() {
        String collegeId = (currentUser != null && currentUser.getCollegeId() != null && !currentUser.getCollegeId().trim().isEmpty())
                ? currentUser.getCollegeId().trim() : "COLLEGE_1789406176044";

        FirebaseHelper.getLeaveRequestsRef().whereEqualTo("collegeId", collegeId).get()
                .addOnSuccessListener(querySnaps -> {
                    if (llTeacherLeaveHistoryList != null) {
                        llTeacherLeaveHistoryList.removeAllViews();
                    }

                    List<LeaveRequest> myLeaves = new ArrayList<>();
                    if (querySnaps != null && !querySnaps.isEmpty()) {
                        for (DocumentSnapshot doc : querySnaps) {
                            LeaveRequest lr = doc.toObject(LeaveRequest.class);
                            if (lr != null) {
                                boolean isMine = (lr.getApplicantId() != null && currentUser != null && lr.getApplicantId().equalsIgnoreCase(currentUser.getUid()))
                                        || (lr.getApplicantName() != null && currentUser != null && currentUser.getName() != null && lr.getApplicantName().equalsIgnoreCase(currentUser.getName()));
                                if (isMine) {
                                    myLeaves.add(lr);
                                }
                            }
                        }
                    }

                    if (myLeaves.isEmpty()) {
                        if (tvTeacherEmptyLeave != null) tvTeacherEmptyLeave.setVisibility(View.VISIBLE);
                        return;
                    }

                    if (tvTeacherEmptyLeave != null) tvTeacherEmptyLeave.setVisibility(View.GONE);

                    // Sort newest first
                    Collections.sort(myLeaves, (a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));

                    for (LeaveRequest lr : myLeaves) {
                        LinearLayout card = new LinearLayout(this);
                        card.setOrientation(LinearLayout.VERTICAL);
                        card.setPadding(16, 12, 16, 12);
                        card.setBackgroundColor(Color.WHITE);
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        lp.setMargins(0, 0, 0, 10);
                        card.setLayoutParams(lp);

                        TextView tvDates = new TextView(this);
                        String dateStr = lr.getStartDate() + (lr.getEndDate() != null && !lr.getEndDate().equals(lr.getStartDate()) ? " to " + lr.getEndDate() : "");
                        tvDates.setText("📅 Leave Period: " + dateStr);
                        tvDates.setTextColor(0xFF0D47A1);
                        tvDates.setTypeface(null, Typeface.BOLD);
                        tvDates.setTextSize(13.5f);
                        card.addView(tvDates);

                        TextView tvReason = new TextView(this);
                        tvReason.setText("Reason: " + (lr.getReason() != null ? lr.getReason() : "Personal/Official"));
                        tvReason.setTextColor(0xFF455A64);
                        tvReason.setTextSize(12f);
                        card.addView(tvReason);

                        TextView tvStatus = new TextView(this);
                        String st = lr.getStatus() != null ? lr.getStatus() : "PENDING";
                        if ("APPROVED".equalsIgnoreCase(st)) {
                            String sub = lr.getSubstitutedFacultyName() != null ? lr.getSubstitutedFacultyName() : "Substitute Assigned";
                            tvStatus.setText("Status: APPROVED ✅ (Substitute: " + sub + ")");
                            tvStatus.setTextColor(0xFF2E7D32);
                        } else if ("REJECTED".equalsIgnoreCase(st)) {
                            tvStatus.setText("Status: REJECTED ❌");
                            tvStatus.setTextColor(0xFFC62828);
                        } else {
                            tvStatus.setText("Status: PENDING ADMIN REVIEW ⏳");
                            tvStatus.setTextColor(0xFFE65100);
                        }
                        tvStatus.setTypeface(null, Typeface.BOLD);
                        tvStatus.setTextSize(12f);
                        card.addView(tvStatus);

                        if (llTeacherLeaveHistoryList != null) {
                            llTeacherLeaveHistoryList.addView(card);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (tvTeacherEmptyLeave != null) {
                        tvTeacherEmptyLeave.setVisibility(View.VISIBLE);
                        tvTeacherEmptyLeave.setText("Unable to load leave history: " + e.getMessage());
                    }
                });
    }

    private void loadAdminPendingRequests() {
        String collegeId = (currentUser != null && currentUser.getCollegeId() != null && !currentUser.getCollegeId().trim().isEmpty())
                ? currentUser.getCollegeId().trim() : "COLLEGE_1789406176044";

        FirebaseHelper.getLeaveRequestsRef().whereEqualTo("collegeId", collegeId).get()
                .addOnSuccessListener(querySnapshot -> {
                    pendingRequests.clear();
                    requestDisplayList.clear();

                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            LeaveRequest req = doc.toObject(LeaveRequest.class);
                            if (req != null) {
                                if (req.getLeaveId() == null) req.setLeaveId(doc.getId());
                                if (req.getId() == null) req.setId(doc.getId());

                                String status = req.getStatus() != null ? req.getStatus() : "PENDING";
                                if ("PENDING".equalsIgnoreCase(status)) {
                                    pendingRequests.add(req);
                                    String dates = req.getStartDate() + (req.getEndDate() != null && !req.getEndDate().equals(req.getStartDate()) ? " to " + req.getEndDate() : "");
                                    String item = "👤 " + (req.getApplicantName() != null ? req.getApplicantName() : "Faculty")
                                            + " (" + dates + ")\n• Reason: " + req.getReason()
                                            + "\n• Action Required: Review & Assign AI Substitute";
                                    requestDisplayList.add(item);
                                }
                            }
                        }
                    }

                    if (tvAdminEmptyLeave != null) {
                        tvAdminEmptyLeave.setVisibility(pendingRequests.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                    if (leaveAdapter != null) leaveAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load leave requests: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showAdminRequestActionDialog(LeaveRequest request) {
        if (request == null) return;

        String collegeId = (currentUser != null && currentUser.getCollegeId() != null && !currentUser.getCollegeId().trim().isEmpty())
                ? currentUser.getCollegeId().trim() : "COLLEGE_1789406176044";

        // Query affected class sessions for the requesting teacher
        FirebaseHelper.getClassSessionsRef().whereEqualTo("collegeId", collegeId).get()
                .addOnSuccessListener(csSnaps -> {
                    List<ClassSession> allCollegeSessions = new ArrayList<>();
                    List<ClassSession> affectedSessions = new ArrayList<>();

                    if (csSnaps != null) {
                        for (DocumentSnapshot doc : csSnaps) {
                            ClassSession cs = doc.toObject(ClassSession.class);
                            if (cs != null) {
                                allCollegeSessions.add(cs);
                                boolean isApplicant = (cs.getFacultyId() != null && cs.getFacultyId().equalsIgnoreCase(request.getApplicantId()))
                                        || (cs.getTeacherUid() != null && cs.getTeacherUid().equalsIgnoreCase(request.getApplicantId()))
                                        || (cs.getFacultyName() != null && request.getApplicantName() != null && cs.getFacultyName().equalsIgnoreCase(request.getApplicantName().trim()));
                                if (isApplicant) {
                                    affectedSessions.add(cs);
                                }
                            }
                        }
                    }

                    // Query all faculty to run substitution engine
                    FirebaseHelper.getFacultyRef().whereEqualTo("collegeId", collegeId).get()
                            .addOnSuccessListener(facSnaps -> {
                                List<Faculty> facultyList = new ArrayList<>();
                                if (facSnaps != null) {
                                    for (DocumentSnapshot doc : facSnaps) {
                                        Faculty f = doc.toObject(Faculty.class);
                                        if (f != null) {
                                            if (f.getId() == null) f.setId(doc.getId());
                                            facultyList.add(f);
                                        }
                                    }
                                }

                                // Run AI substitution engine
                                List<TeacherSubstitutionEngine.SubstituteCandidate> rankedCandidates =
                                        TeacherSubstitutionEngine.findAndRankSubstitutesForLeave(
                                                request, affectedSessions, facultyList, allCollegeSessions, null
                                        );

                                renderAdminSubstitutionDialog(request, affectedSessions, rankedCandidates);
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to analyze timetable: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void renderAdminSubstitutionDialog(
            LeaveRequest request,
            List<ClassSession> affectedSessions,
            List<TeacherSubstitutionEngine.SubstituteCandidate> candidates
    ) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Leave Approval & Substitute Allocation");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(36, 16, 36, 16);

        // Header Summary
        TextView tvSummary = new TextView(this);
        String dates = request.getStartDate() + (request.getEndDate() != null && !request.getEndDate().equals(request.getStartDate()) ? " to " + request.getEndDate() : "");
        StringBuilder sbSummary = new StringBuilder();
        sbSummary.append("• Requesting Faculty: ").append(request.getApplicantName()).append("\n")
                 .append("• Leave Dates: ").append(dates).append("\n")
                 .append("• Reason: ").append(request.getReason()).append("\n")
                 .append("• Affected Teaching Sessions: ").append(affectedSessions.size()).append(" periods\n");

        if (!affectedSessions.isEmpty()) {
            Set<String> subNames = new HashSet<>();
            for (ClassSession cs : affectedSessions) {
                subNames.add(cs.getSubjectName() != null ? cs.getSubjectName() : "Core Subject");
            }
            sbSummary.append("• Course(s) Involved: ").append(subNames.toString()).append("\n");
        }
        tvSummary.setText(sbSummary.toString());
        tvSummary.setTextColor(0xFF263238);
        tvSummary.setTextSize(13f);
        layout.addView(tvSummary);

        // Top AI Recommendation Highlight
        TextView tvRecTitle = new TextView(this);
        tvRecTitle.setText("\n🤖 AI-Recommended Substitute:");
        tvRecTitle.setTypeface(null, Typeface.BOLD);
        tvRecTitle.setTextColor(0xFF0D47A1);
        tvRecTitle.setTextSize(14f);
        layout.addView(tvRecTitle);

        TextView tvRecDetails = new TextView(this);
        if (!candidates.isEmpty()) {
            TeacherSubstitutionEngine.SubstituteCandidate top = candidates.get(0);
            String matchNotice = top.isSubjectMatch() ? "✅ Subject-Matter Qualified & Authorized" : "ℹ️ Available Faculty (Same Department)";
            tvRecDetails.setText("• Candidate: " + top.getFacultyName() + " (" + top.getDesignation() + ")\n"
                    + "• Match: " + matchNotice + "\n"
                    + "• Timetable Slots: Free in all affected periods\n"
                    + "• Workload Bandwidth: " + top.getCurrentWorkload() + "/" + top.getMaxWorkload() + " hrs\n"
                    + "• AI Rationale: " + top.getRationale());
            tvRecDetails.setTextColor(0xFF1B5E20);
        } else {
            tvRecDetails.setText("⚠️ No fully clash-free faculty candidate discovered automatically. Administrative override required.");
            tvRecDetails.setTextColor(0xFFC62828);
        }
        tvRecDetails.setTextSize(12.5f);
        layout.addView(tvRecDetails);

        // Dropdown to allow selecting / overriding candidate
        TextView tvChooserLabel = new TextView(this);
        tvChooserLabel.setText("\nSelect Approved Substitute Faculty:");
        tvChooserLabel.setTextSize(12.5f);
        tvChooserLabel.setTextColor(0xFF37474F);
        layout.addView(tvChooserLabel);

        Spinner spCandidates = new Spinner(this);
        List<String> candidateDisplay = new ArrayList<>();
        for (TeacherSubstitutionEngine.SubstituteCandidate c : candidates) {
            String badge = c.isSubjectMatch() ? "[SUBJECT MATCH]" : "[AVAILABLE]";
            candidateDisplay.add(c.getFacultyName() + " (" + c.getDesignation() + ") " + badge);
        }
        if (candidateDisplay.isEmpty()) {
            candidateDisplay.add("Department Emergency Pool");
        }

        ArrayAdapter<String> spinAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, candidateDisplay);
        spCandidates.setAdapter(spinAdapter);
        layout.addView(spCandidates);

        builder.setView(layout);

        builder.setPositiveButton("APPROVE & ASSIGN", (dialog, which) -> {
            int selectedIdx = spCandidates.getSelectedItemPosition();
            TeacherSubstitutionEngine.SubstituteCandidate chosenCandidate =
                    (selectedIdx >= 0 && selectedIdx < candidates.size()) ? candidates.get(selectedIdx) : null;

            String chosenSubId = chosenCandidate != null ? chosenCandidate.getFacultyId() : "SUB_DEPT_POOL";
            String chosenSubName = chosenCandidate != null ? chosenCandidate.getFacultyName() : "Department Substitute";

            executeLeaveApproval(request, affectedSessions, chosenSubId, chosenSubName);
        });

        builder.setNegativeButton("REJECT LEAVE", (dialog, which) -> {
            executeLeaveRejection(request);
        });

        builder.setNeutralButton("CANCEL", null);
        builder.show();
    }

    private void executeLeaveApproval(
            LeaveRequest request,
            List<ClassSession> affectedSessions,
            String substituteId,
            String substituteName
    ) {
        String reqDocId = request.getLeaveId() != null ? request.getLeaveId() : request.getId();
        String dates = request.getStartDate() + (request.getEndDate() != null && !request.getEndDate().equals(request.getStartDate()) ? " to " + request.getEndDate() : "");

        // 1. Update leave request document
        FirebaseHelper.getLeaveRequestsRef().document(reqDocId)
                .update(
                        "status", "APPROVED",
                        "substitutedFacultyId", substituteId,
                        "substitutedFacultyName", substituteName,
                        "approvedAt", System.currentTimeMillis()
                )
                .addOnSuccessListener(aVoid -> {
                    // 2. Update affected class sessions
                    for (ClassSession cs : affectedSessions) {
                        String csDocId = cs.getSessionId() != null ? cs.getSessionId() : cs.getClassAssignmentId();
                        if (csDocId != null && !csDocId.isEmpty()) {
                            FirebaseHelper.getClassSessionsRef().document(csDocId)
                                    .update(
                                            "status", "SUBSTITUTED",
                                            "substituteTeacherName", substituteName,
                                            "substituteFacultyId", substituteId
                                    );
                        }
                    }

                    // 3. Dispatch Notification to Requesting Teacher
                    String notifId1 = "NOTIF_REQ_" + System.currentTimeMillis();
                    NotificationItem notif1 = new NotificationItem(
                            notifId1,
                            "Leave Request Approved",
                            "Your leave for " + dates + " has been approved. Substitute Faculty: " + substituteName + " has been assigned to your classes.",
                            "LEAVE_APPROVAL",
                            request.getCollegeId(),
                            "DEPT_CSE",
                            "Teacher",
                            String.valueOf(System.currentTimeMillis())
                    );
                    FirebaseHelper.getNotificationsRef().document(notifId1).set(notif1);

                    // 4. Dispatch Notification to Assigned Substitute Faculty
                    String notifId2 = "NOTIF_SUB_" + System.currentTimeMillis();
                    NotificationItem notif2 = new NotificationItem(
                            notifId2,
                            "Substitute Teaching Assignment",
                            "You have been assigned as substitute faculty for " + request.getApplicantName() + " (" + dates + "). Please review affected timetable sessions.",
                            "SUBSTITUTE_ASSIGNMENT",
                            request.getCollegeId(),
                            "DEPT_CSE",
                            "Teacher",
                            String.valueOf(System.currentTimeMillis())
                    );
                    FirebaseHelper.getNotificationsRef().document(notifId2).set(notif2);

                    Toast.makeText(this, "Leave approved and substitute " + substituteName + " successfully assigned!", Toast.LENGTH_LONG).show();
                    loadAdminPendingRequests();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Approval failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void executeLeaveRejection(LeaveRequest request) {
        String reqDocId = request.getLeaveId() != null ? request.getLeaveId() : request.getId();
        String dates = request.getStartDate() + (request.getEndDate() != null && !request.getEndDate().equals(request.getStartDate()) ? " to " + request.getEndDate() : "");

        FirebaseHelper.getLeaveRequestsRef().document(reqDocId)
                .update("status", "REJECTED", "rejectedAt", System.currentTimeMillis())
                .addOnSuccessListener(aVoid -> {
                    String notifId = "NOTIF_REJ_" + System.currentTimeMillis();
                    NotificationItem notif = new NotificationItem(
                            notifId,
                            "Leave Request Decision: Rejected",
                            "Your leave request for " + dates + " could not be approved due to administrative constraints.",
                            "LEAVE_REJECTED",
                            request.getCollegeId(),
                            "DEPT_CSE",
                            "Teacher",
                            String.valueOf(System.currentTimeMillis())
                    );
                    FirebaseHelper.getNotificationsRef().document(notifId).set(notif);

                    Toast.makeText(this, "Leave request marked as REJECTED.", Toast.LENGTH_SHORT).show();
                    loadAdminPendingRequests();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Rejection failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
