package com.campusscheduler.ai.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.campusscheduler.ai.R;
import com.campusscheduler.ai.models.NotificationItem;
import com.campusscheduler.ai.models.ProfileChangeRequest;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.utils.FirebaseHelper;
import com.campusscheduler.ai.utils.LocalAuthManager;
import com.campusscheduler.ai.utils.UIUtils;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProfileChangeRequestActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvHeaderTitle, tvListHeader, tvEmptyRequests;
    private Button btnNewChangeRequest;
    private ListView lvChangeRequests;

    private User currentUser;
    private boolean isAdmin = false;

    private final List<ProfileChangeRequest> requestList = new ArrayList<>();
    private final List<String> requestDisplayList = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_change_request);

        currentUser = LocalAuthManager.getCurrentLocalUser(this);
        String safeCollegeId = LocalAuthManager.getSafeCollegeId(this, getIntent());
        if (currentUser == null) {
            currentUser = new User();
            currentUser.setRole(User.ROLE_TEACHER);
            currentUser.setName("Faculty");
        }
        if (currentUser.getCollegeId() == null || currentUser.getCollegeId().trim().isEmpty()) {
            currentUser.setCollegeId(safeCollegeId);
        }

        isAdmin = User.ROLE_COLLEGE_ADMIN.equalsIgnoreCase(currentUser.getRole())
                || User.ROLE_ADMIN.equalsIgnoreCase(currentUser.getRole())
                || User.ROLE_OFFICIAL_OWNER.equalsIgnoreCase(currentUser.getRole())
                || (getIntent() != null && getIntent().getBooleanExtra("isAdmin", false));

        btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        tvListHeader = findViewById(R.id.tvListHeader);
        tvEmptyRequests = findViewById(R.id.tvEmptyRequests);
        btnNewChangeRequest = findViewById(R.id.btnNewChangeRequest);
        lvChangeRequests = findViewById(R.id.lvChangeRequests);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, requestDisplayList);
        lvChangeRequests.setAdapter(adapter);

        if (isAdmin) {
            if (tvHeaderTitle != null) tvHeaderTitle.setText("Admin • Profile Change Approvals");
            if (btnNewChangeRequest != null) btnNewChangeRequest.setVisibility(View.GONE);
            if (tvListHeader != null) tvListHeader.setText("Pending Faculty & Student Profile Modification Requests:");

            lvChangeRequests.setOnItemClickListener((parent, view, position, id) -> {
                if (position >= 0 && position < requestList.size()) {
                    showAdminReviewDialog(requestList.get(position));
                }
            });
        } else {
            if (tvHeaderTitle != null) tvHeaderTitle.setText("Profile Change Requests");
            if (btnNewChangeRequest != null) {
                btnNewChangeRequest.setVisibility(View.VISIBLE);
                btnNewChangeRequest.setOnClickListener(v -> showSubmitRequestDialog());
            }
            if (tvListHeader != null) tvListHeader.setText("Your Submitted Modification Requests:");
        }

        loadChangeRequests();
    }

    private void loadChangeRequests() {
        String collegeId = (currentUser != null && currentUser.getCollegeId() != null && !currentUser.getCollegeId().trim().isEmpty())
                ? currentUser.getCollegeId().trim() : "COLLEGE_1789406176044";

        FirebaseHelper.getProfileChangeRequestsRef().whereEqualTo("collegeId", collegeId).get()
                .addOnSuccessListener(querySnaps -> {
                    requestList.clear();
                    requestDisplayList.clear();

                    if (querySnaps != null && !querySnaps.isEmpty()) {
                        for (DocumentSnapshot doc : querySnaps) {
                            ProfileChangeRequest req = doc.toObject(ProfileChangeRequest.class);
                            if (req != null) {
                                if (req.getId() == null) req.setId(doc.getId());

                                if (isAdmin) {
                                    // Admins see all pending requests
                                    if (ProfileChangeRequest.STATUS_PENDING.equalsIgnoreCase(req.getStatus())) {
                                        requestList.add(req);
                                    }
                                } else {
                                    // Regular user only sees their own requests
                                    String myUid = currentUser != null ? currentUser.getUid() : "";
                                    if (myUid != null && myUid.equalsIgnoreCase(req.getUserUid())) {
                                        requestList.add(req);
                                    }
                                }
                            }
                        }
                    }

                    // Sort newest first
                    Collections.sort(requestList, (a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));

                    for (ProfileChangeRequest r : requestList) {
                        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US);
                        String dateStr = sdf.format(new Date(r.getCreatedAt()));
                        String st = r.getStatus() != null ? r.getStatus() : "PENDING";

                        if (isAdmin) {
                            String item = "👤 " + r.getUserName() + " (" + (r.getUserRole() != null ? r.getUserRole() : "User") + ")\n"
                                    + "• Field: " + r.getFieldName() + "\n"
                                    + "• From: \"" + (r.getPreviousValue() != null ? r.getPreviousValue() : "N/A") + "\" ➡️ \"" + r.getRequestedValue() + "\"\n"
                                    + "• Reason: " + r.getReason() + "\n"
                                    + "• Status: [" + st + "] • " + dateStr;
                            requestDisplayList.add(item);
                        } else {
                            String badge = "APPROVED".equalsIgnoreCase(st) ? "✅ APPROVED" : ("REJECTED".equalsIgnoreCase(st) ? "❌ REJECTED" : "⏳ PENDING REVIEW");
                            String item = "• Field: " + r.getFieldName() + " ➡️ \"" + r.getRequestedValue() + "\"\n"
                                    + "• Reason: " + r.getReason() + "\n"
                                    + "• Status: " + badge + " (" + dateStr + ")";
                            requestDisplayList.add(item);
                        }
                    }

                    if (tvEmptyRequests != null) {
                        tvEmptyRequests.setVisibility(requestList.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                    if (adapter != null) adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load change requests: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showSubmitRequestDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Request Profile or Academic Update");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 16, 40, 16);

        TextView tvInstructions = new TextView(this);
        tvInstructions.setText("Changes to protected profile or academic records require administrative verification.");
        tvInstructions.setTextSize(12.5f);
        tvInstructions.setTextColor(0xFF546E7A);
        layout.addView(tvInstructions);

        TextView tvFieldLabel = new TextView(this);
        tvFieldLabel.setText("\nSelect Field to Modify:");
        tvFieldLabel.setTextSize(13f);
        tvFieldLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(tvFieldLabel);

        Spinner spField = new Spinner(this);
        boolean isStudent = currentUser != null && User.ROLE_STUDENT.equalsIgnoreCase(currentUser.getRole());
        List<String> fieldOptions = new ArrayList<>();
        fieldOptions.add("Display Name");
        fieldOptions.add("Contact Phone Number");
        if (isStudent) {
            fieldOptions.add("Academic Progression (Semester & Section)");
        }
        fieldOptions.add("Profile Photo Link");
        ArrayAdapter<String> fieldAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, fieldOptions);
        spField.setAdapter(fieldAdapter);
        layout.addView(spField);

        TextView tvNewValLabel = new TextView(this);
        tvNewValLabel.setText("\nEnter New Requested Value:");
        tvNewValLabel.setTextSize(13f);
        tvNewValLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(tvNewValLabel);

        EditText etNewValue = new EditText(this);
        etNewValue.setHint("e.g. Semester 6 - Section A, or new phone number");
        layout.addView(etNewValue);

        TextView tvReasonLabel = new TextView(this);
        tvReasonLabel.setText("\nReason for Change:");
        tvReasonLabel.setTextSize(13f);
        tvReasonLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(tvReasonLabel);

        EditText etReason = new EditText(this);
        etReason.setHint("Provide reason for institutional record");
        layout.addView(etReason);

        builder.setView(layout);

        builder.setPositiveButton("SUBMIT REQUEST", (dialog, which) -> {
            String selectedField = spField.getSelectedItem() != null ? spField.getSelectedItem().toString() : "Display Name";
            String newVal = etNewValue.getText().toString().trim();
            String reason = etReason.getText().toString().trim();

            if (newVal.isEmpty() || reason.isEmpty()) {
                Toast.makeText(this, "Please enter both the requested value and reason.", Toast.LENGTH_SHORT).show();
                return;
            }

            String currentVal = "N/A";
            if ("Display Name".equalsIgnoreCase(selectedField)) {
                currentVal = currentUser.getName();
            } else if ("Contact Phone Number".equalsIgnoreCase(selectedField)) {
                currentVal = currentUser.getPhone() != null ? currentUser.getPhone() : "N/A";
            } else if (selectedField.contains("Semester")) {
                currentVal = "Sem " + (currentUser.getSemester() != null ? currentUser.getSemester() : "1") + " - Sec " + (currentUser.getSectionId() != null ? currentUser.getSectionId() : "A");
            }

            String collegeId = (currentUser != null && currentUser.getCollegeId() != null && !currentUser.getCollegeId().trim().isEmpty())
                    ? currentUser.getCollegeId().trim() : "COLLEGE_1789406176044";
            String uid = currentUser != null ? currentUser.getUid() : "USER_DEFAULT";
            String userName = currentUser != null ? currentUser.getName() : "Requester";
            String role = currentUser != null ? currentUser.getRole() : "STUDENT";

            String reqId = "PCR_" + System.currentTimeMillis();
            ProfileChangeRequest pcr = new ProfileChangeRequest(reqId, collegeId, uid, userName, role, selectedField, currentVal, newVal, reason);

            FirebaseHelper.getProfileChangeRequestsRef().document(reqId).set(pcr)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Change request submitted successfully for admin approval.", Toast.LENGTH_LONG).show();
                        loadChangeRequests();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to submit request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        builder.setNegativeButton("CANCEL", null);
        builder.show();
    }

    private void showAdminReviewDialog(ProfileChangeRequest request) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Review Profile Modification");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        TextView tvDetails = new TextView(this);
        tvDetails.setText("• Requester: " + request.getUserName() + " (" + request.getUserRole() + ")\n"
                + "• UID: " + request.getUserUid() + "\n"
                + "• Target Field: " + request.getFieldName() + "\n"
                + "• Previous Value: " + request.getPreviousValue() + "\n"
                + "• Requested Value: " + request.getRequestedValue() + "\n"
                + "• Reason: " + request.getReason());
        tvDetails.setTextSize(13.5f);
        tvDetails.setTextColor(0xFF263238);
        layout.addView(tvDetails);

        builder.setView(layout);

        builder.setPositiveButton("APPROVE & APPLY", (dialog, which) -> {
            applyApprovedChange(request);
        });

        builder.setNegativeButton("REJECT", (dialog, which) -> {
            rejectChangeRequest(request);
        });

        builder.setNeutralButton("CLOSE", null);
        builder.show();
    }

    private void applyApprovedChange(ProfileChangeRequest request) {
        String reqId = request.getId();
        String uid = request.getUserUid();
        String field = request.getFieldName();
        String newVal = request.getRequestedValue();

        Map<String, Object> userUpdates = new HashMap<>();
        Map<String, Object> entityUpdates = new HashMap<>();

        if ("Display Name".equalsIgnoreCase(field)) {
            userUpdates.put("name", newVal);
            entityUpdates.put("name", newVal);
        } else if ("Contact Phone Number".equalsIgnoreCase(field)) {
            userUpdates.put("phone", newVal);
            entityUpdates.put("phone", newVal);
        } else if (field.contains("Semester")) {
            // E.g. "Semester 6 - Section A" or "6"
            String cleanSem = newVal.replaceAll("[^0-9]", "");
            int semInt = cleanSem.isEmpty() ? 1 : Integer.parseInt(cleanSem);
            String sec = "A";
            if (newVal.toUpperCase(Locale.ROOT).contains("SECTION B") || newVal.endsWith("B")) sec = "B";
            else if (newVal.toUpperCase(Locale.ROOT).contains("SECTION C") || newVal.endsWith("C")) sec = "C";

            userUpdates.put("semester", String.valueOf(semInt));
            userUpdates.put("sectionId", sec);
            entityUpdates.put("semester", semInt);
            entityUpdates.put("section", sec);
        } else if ("Profile Photo Link".equalsIgnoreCase(field)) {
            userUpdates.put("photoUrl", newVal);
            entityUpdates.put("photoUrl", newVal);
        }

        // 1. Update users/{uid}
        FirebaseHelper.getUsersRef().document(uid).update(userUpdates)
                .addOnSuccessListener(aVoid -> {
                    // 2. If student, update students collection as well
                    if ("STUDENT".equalsIgnoreCase(request.getUserRole())) {
                        FirebaseHelper.getStudentsRef().whereEqualTo("uid", uid).get()
                                .addOnSuccessListener(sSnaps -> {
                                    if (sSnaps != null && !sSnaps.isEmpty()) {
                                        sSnaps.getDocuments().get(0).getReference().update(entityUpdates);
                                    }
                                });
                    }

                    // 3. Update request status
                    FirebaseHelper.getProfileChangeRequestsRef().document(reqId)
                            .update("status", "APPROVED", "reviewedAt", System.currentTimeMillis())
                            .addOnSuccessListener(v -> {
                                // 4. Dispatch notification to user
                                String notifId = "NOTIF_PCR_" + System.currentTimeMillis();
                                NotificationItem notif = new NotificationItem(
                                        notifId,
                                        "Profile Update Approved",
                                        "Your requested update for " + field + " has been approved and applied to your official record.",
                                        "PROFILE_UPDATE_APPROVED",
                                        request.getCollegeId(),
                                        "DEPT_CSE",
                                        request.getUserRole(),
                                        String.valueOf(System.currentTimeMillis())
                                );
                                FirebaseHelper.getNotificationsRef().document(notifId).set(notif);

                                Toast.makeText(this, "Profile modification approved and applied to canonical record!", Toast.LENGTH_LONG).show();
                                loadChangeRequests();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to apply profile update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void rejectChangeRequest(ProfileChangeRequest request) {
        String reqId = request.getId();
        FirebaseHelper.getProfileChangeRequestsRef().document(reqId)
                .update("status", "REJECTED", "reviewedAt", System.currentTimeMillis())
                .addOnSuccessListener(aVoid -> {
                    String notifId = "NOTIF_PCR_REJ_" + System.currentTimeMillis();
                    NotificationItem notif = new NotificationItem(
                            notifId,
                            "Profile Update Rejected",
                            "Your requested update for " + request.getFieldName() + " was reviewed and rejected by the administration.",
                            "PROFILE_UPDATE_REJECTED",
                            request.getCollegeId(),
                            "DEPT_CSE",
                            request.getUserRole(),
                            String.valueOf(System.currentTimeMillis())
                    );
                    FirebaseHelper.getNotificationsRef().document(notifId).set(notif);

                    Toast.makeText(this, "Request marked as REJECTED.", Toast.LENGTH_SHORT).show();
                    loadChangeRequests();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
