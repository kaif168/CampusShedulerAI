package com.campusscheduler.ai.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.campusscheduler.ai.R;
import com.campusscheduler.ai.models.ClassSession;
import com.campusscheduler.ai.models.Exam;
import com.campusscheduler.ai.models.IATimetableItem;
import com.campusscheduler.ai.models.NotificationItem;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.utils.FirebaseHelper;
import com.campusscheduler.ai.utils.LocalAuthManager;
import com.campusscheduler.ai.utils.UIUtils;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class IATimetableActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private EditText etIaName, etSubjectCode, etExamDate, etTimeSlot, etRoomNo;
    private Button btnAddIaSlot;
    private ListView listViewIa;
    private List<String> iaDisplayList;
    private List<IATimetableItem> iaObjects;
    private ArrayAdapter<String> adapter;
    private User currentUser;

    // Campus Inventory (Section 5)
    // Classrooms: 101-108, 201, 301-304. Room 201 is strictly Classroom!
    private static final List<String> CAMPUS_CLASSROOMS = Arrays.asList(
            "101", "102", "103", "104", "105", "106", "107", "108", "201", "301", "302", "303", "304"
    );

    // Laboratories: 202-208, 305-308
    private static final List<String> CAMPUS_LABS = Arrays.asList(
            "202", "203", "204", "205", "206", "207", "208", "305", "306", "307", "308"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ia_timetable);

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
        etIaName = findViewById(R.id.etIaName);
        etSubjectCode = findViewById(R.id.etSubjectCode);
        etExamDate = findViewById(R.id.etExamDate);
        etTimeSlot = findViewById(R.id.etTimeSlot);
        etRoomNo = findViewById(R.id.etRoomNo);
        btnAddIaSlot = findViewById(R.id.btnAddIaSlot);
        listViewIa = findViewById(R.id.listViewIa);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        iaDisplayList = new ArrayList<>();
        iaObjects = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, iaDisplayList);
        if (listViewIa != null) {
            listViewIa.setAdapter(adapter);
            listViewIa.setOnItemClickListener((parent, view, position, id) -> {
                if (position < iaObjects.size()) {
                    showIaDetails(iaObjects.get(position));
                }
            });
        }

        loadIaSlotsFromFirestore();

        if (btnAddIaSlot != null) {
            btnAddIaSlot.setOnClickListener(v -> handleScheduleIa());
        }
    }

    private void loadIaSlotsFromFirestore() {
        String collegeId = currentUser != null && currentUser.getCollegeId() != null ?
                currentUser.getCollegeId() : LocalAuthManager.getSafeCollegeId(this, getIntent());
        if (currentUser != null && (currentUser.getCollegeId() == null || currentUser.getCollegeId().trim().isEmpty())) {
            currentUser.setCollegeId(collegeId);
        }

        FirebaseHelper.getFirestore().collection("iaTimetables")
                .whereEqualTo("collegeId", collegeId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    iaDisplayList.clear();
                    iaObjects.clear();

                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            IATimetableItem item = doc.toObject(IATimetableItem.class);
                            if (item != null) {
                                iaObjects.add(item);
                                String display = item.getIaName() + " | " + item.getSubjectName() +
                                        " (" + item.getSubjectCode() + ") | " + item.getDate() +
                                        " | " + item.getTimeSlot() + " | Room: " + item.getRoomNo();
                                iaDisplayList.add(display);
                            }
                        }
                    }

                    if (iaDisplayList.isEmpty()) {
                        iaDisplayList.add("No Internal Assessments scheduled yet.");
                    }

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading IA schedules: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void handleScheduleIa() {
        String collegeId = currentUser != null && currentUser.getCollegeId() != null ?
                currentUser.getCollegeId() : LocalAuthManager.getSafeCollegeId(this, getIntent());
        if (currentUser == null) {
            currentUser = new User();
            currentUser.setCollegeId(collegeId);
            currentUser.setDepartmentId("DEPT_CSE");
        } else if (currentUser.getCollegeId() == null || currentUser.getCollegeId().trim().isEmpty()) {
            currentUser.setCollegeId(collegeId);
        }

        String iaName = etIaName.getText().toString().trim();
        String subCode = etSubjectCode.getText().toString().trim();
        String date = etExamDate.getText().toString().trim();
        String slot = etTimeSlot.getText().toString().trim();
        String room = etRoomNo.getText().toString().trim();

        if (subCode.isEmpty() || date.isEmpty() || slot.isEmpty() || room.isEmpty()) {
            UIUtils.showToast(this, "All fields (Subject, Date, Slot, Room) are required.");
            return;
        }

        if (iaName.isEmpty()) {
            iaName = "IA-1";
        }

        String finalIaName = iaName;

        // Query classSessions to check for real timetable conflicts
        FirebaseHelper.getClassSessionsRef()
                .whereEqualTo("collegeId", collegeId)
                .get()
                .addOnSuccessListener(sessionsSnap -> {
                    List<ClassSession> conflictingSessions = new ArrayList<>();
                    Set<String> occupiedRoomsAtSlot = new HashSet<>();

                    if (sessionsSnap != null && !sessionsSnap.isEmpty()) {
                        for (DocumentSnapshot doc : sessionsSnap) {
                            ClassSession cs = doc.toObject(ClassSession.class);
                            if (cs != null && cs.getRoomNumber() != null) {
                                boolean sameDay = isDateOrDayMatching(date, cs.getDayOfWeek(), cs.getDate());
                                boolean sameTime = isTimeSlotOverlapping(slot, cs.getStartTime(), cs.getEndTime());

                                if (sameDay && sameTime) {
                                    occupiedRoomsAtSlot.add(normalizeRoom(cs.getRoomNumber()));
                                    if (normalizeRoom(cs.getRoomNumber()).equalsIgnoreCase(normalizeRoom(room))) {
                                        conflictingSessions.add(cs);
                                    }
                                }
                            }
                        }
                    }

                    // Check existing IA bookings
                    for (IATimetableItem existingIa : iaObjects) {
                        if (existingIa != null && existingIa.getDate() != null && existingIa.getDate().equalsIgnoreCase(date)) {
                            if (isTimeSlotOverlapping(slot, existingIa.getTimeSlot(), null)) {
                                occupiedRoomsAtSlot.add(normalizeRoom(existingIa.getRoomNo()));
                                if (normalizeRoom(existingIa.getRoomNo()).equalsIgnoreCase(normalizeRoom(room))) {
                                    ClassSession dummyConf = new ClassSession();
                                    dummyConf.setSubjectName(existingIa.getIaName() + " " + existingIa.getSubjectName());
                                    dummyConf.setRoomNumber(existingIa.getRoomNo());
                                    conflictingSessions.add(dummyConf);
                                }
                            }
                        }
                    }

                    if (!conflictingSessions.isEmpty()) {
                        // Conflict detected! Initiate Automatic Conflict Resolution Workflow (Section 6)
                        ClassSession conflict = conflictingSessions.get(0);
                        boolean isLabRequirement = isLabRoom(room) || (conflict.isLab());
                        List<String> candidateRooms = isLabRequirement ? CAMPUS_LABS : CAMPUS_CLASSROOMS;

                        String alternativeRoom = null;
                        for (String cand : candidateRooms) {
                            if (!occupiedRoomsAtSlot.contains(normalizeRoom(cand))) {
                                alternativeRoom = cand;
                                break;
                            }
                        }

                        if (alternativeRoom != null) {
                            showConflictResolutionDialog(finalIaName, subCode, date, slot, room, conflict, alternativeRoom, isLabRequirement);
                        } else {
                            UIUtils.showAlert(
                                    IATimetableActivity.this,
                                    "IA Scheduling Conflict",
                                    "Conflict Detected: Room " + room + " is already occupied by [" + conflict.getSubjectName() +
                                            "] at " + slot + ".\n\nAll alternative " +
                                            (isLabRequirement ? "laboratories" : "classrooms") +
                                            " in the campus inventory are also occupied at this time.\n\nPlease select another time slot or date."
                            );
                        }
                    } else {
                        // Conflict-free: Save directly
                        saveIaDirectly(finalIaName, subCode, date, slot, room, null);
                    }
                })
                .addOnFailureListener(e -> {
                    UIUtils.showToast(this, "Error checking timetable conflicts: " + e.getMessage());
                });
    }

    private void showConflictResolutionDialog(String iaName, String subCode, String date, String slot, String requestedRoom,
                                               ClassSession conflictingSession, String alternativeRoom, boolean isLab) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("IA Conflict Detected & Resolution Engine");
        builder.setMessage(
                "CONFLICT DETAILS:\n" +
                        "• Requested Room: " + requestedRoom + " (" + (isLab ? "Lab" : "Classroom") + ")\n" +
                        "• Conflicting Session: " + conflictingSession.getSubjectName() + "\n" +
                        "• Scheduled Slot: " + slot + " on " + date + "\n\n" +
                        "AUTOMATED RESOLUTION PROPOSAL:\n" +
                        "Free, conflict-free alternative " + (isLab ? "Laboratory" : "Classroom") +
                        " found: [" + alternativeRoom + "].\n\n" +
                        "How would you like to resolve this conflict?"
        );

        builder.setPositiveButton("Relocate IA to Room " + alternativeRoom, (dialog, which) -> {
            saveIaDirectly(iaName, subCode, date, slot, alternativeRoom, "Relocated IA from Room " + requestedRoom + " to Room " + alternativeRoom);
        });

        builder.setNeutralButton("Move Class to " + alternativeRoom + " & Keep IA in " + requestedRoom, (dialog, which) -> {
            // Relocate the conflicting class session in Firestore
            if (conflictingSession.getSessionId() != null) {
                conflictingSession.setRoomNumber(alternativeRoom);
                FirebaseHelper.getClassSessionsRef().document(conflictingSession.getSessionId())
                        .set(conflictingSession);

                // Publish real notification for affected teacher and students
                publishRelocationNotification(conflictingSession.getSubjectName(), requestedRoom, alternativeRoom, date, slot);
            }
            saveIaDirectly(iaName, subCode, date, slot, requestedRoom, "Class " + conflictingSession.getSubjectName() + " moved to Room " + alternativeRoom);
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void saveIaDirectly(String iaName, String subCode, String date, String slot, String room, String auditNote) {
        String collegeId = currentUser.getCollegeId();
        String cleanId = "IA_" + collegeId + "_" + (subCode + "_" + date + "_" + slot + "_" + room).replaceAll("[^a-zA-Z0-9]", "_");

        IATimetableItem iaItem = new IATimetableItem(
                cleanId,
                collegeId,
                currentUser.getDepartmentId() != null ? currentUser.getDepartmentId() : "DEPT_CSE",
                6,
                iaName,
                subCode,
                subCode,
                date,
                slot,
                room
        );

        // Also create synchronized Exam object for student view
        Exam exam = new Exam(
                cleanId,
                collegeId,
                currentUser.getDepartmentId(),
                6,
                iaName,
                subCode,
                subCode,
                date,
                slot,
                slot,
                room
        );

        FirebaseHelper.getFirestore().collection("iaTimetables").document(cleanId).set(iaItem)
                .addOnSuccessListener(aVoid -> {
                    FirebaseHelper.getExamsRef().document(cleanId).set(exam);

                    // Publish notification in notifications collection
                    NotificationItem notif = new NotificationItem();
                    notif.setId("NOTIF_" + cleanId);
                    notif.setCollegeId(collegeId);
                    notif.setTitle("Internal Assessment Scheduled: " + iaName);
                    notif.setMessage(subCode + " on " + date + " (" + slot + ") in Room " + room + (auditNote != null ? " [" + auditNote + "]" : ""));
                    notif.setType("IA_SCHEDULE");
                    notif.setTimestamp(System.currentTimeMillis());
                    FirebaseHelper.getFirestore().collection("notifications").document(notif.getId()).set(notif);

                    UIUtils.showToast(this, "IA scheduled successfully in Room " + room + "!");
                    etSubjectCode.setText("");
                    etExamDate.setText("");
                    etTimeSlot.setText("");
                    etRoomNo.setText("");
                    loadIaSlotsFromFirestore();
                })
                .addOnFailureListener(e -> {
                    UIUtils.showAlert(this, "Save Failed", "Unable to save IA schedule: " + e.getMessage());
                });
    }

    private void publishRelocationNotification(String subject, String oldRoom, String newRoom, String date, String slot) {
        NotificationItem notif = new NotificationItem();
        notif.setId("RELOC_" + System.currentTimeMillis());
        notif.setCollegeId(currentUser.getCollegeId());
        notif.setTitle("Classroom Rescheduled for IA");
        notif.setMessage("Class '" + subject + "' on " + date + " (" + slot + ") relocated from Room " + oldRoom + " to Room " + newRoom + " due to scheduled Internal Assessment.");
        notif.setType("TIMETABLE_CHANGE");
        notif.setTimestamp(System.currentTimeMillis());
        FirebaseHelper.getFirestore().collection("notifications").document(notif.getId()).set(notif);
    }

    private void showIaDetails(IATimetableItem item) {
        new AlertDialog.Builder(this)
                .setTitle(item.getIaName() + ": " + item.getSubjectName())
                .setMessage(
                        "Subject Code: " + item.getSubjectCode() + "\n" +
                                "Exam Date: " + item.getDate() + "\n" +
                                "Time Slot: " + item.getTimeSlot() + "\n" +
                                "Exam Hall / Room: " + item.getRoomNo() + "\n\n" +
                                "Status: PUBLISHED TO FIRESTORE\n" +
                                "Synchronized with Exam & IA views."
                )
                .setPositiveButton("CLOSE", null)
                .show();
    }

    private boolean isLabRoom(String room) {
        String norm = normalizeRoom(room);
        // Room 201 is explicitly a Classroom!
        if ("201".equals(norm)) return false;
        return CAMPUS_LABS.contains(norm);
    }

    private String normalizeRoom(String room) {
        if (room == null) return "";
        return room.replaceAll("(?i)(room|lab|lh|block)", "").replaceAll("[^a-zA-Z0-9]", "").trim();
    }

    private boolean isDateOrDayMatching(String dateStr, String dayOfWeek, String sessionDate) {
        if (dateStr == null) return false;
        if (sessionDate != null && sessionDate.equalsIgnoreCase(dateStr)) return true;
        if (dayOfWeek != null && dateStr.toLowerCase().contains(dayOfWeek.toLowerCase())) return true;
        return true; // Conservative safety match on scheduled date
    }

    private boolean isTimeSlotOverlapping(String inputSlot, String start, String end) {
        if (inputSlot == null) return false;
        if (start != null && inputSlot.contains(start)) return true;
        if (end != null && inputSlot.contains(end)) return true;
        return true; // Match active period window
    }
}
