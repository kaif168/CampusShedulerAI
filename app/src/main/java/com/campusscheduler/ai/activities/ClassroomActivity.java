package com.campusscheduler.ai.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.campusscheduler.ai.R;
import com.campusscheduler.ai.adapters.GenericAdapter;
import com.campusscheduler.ai.models.Classroom;
import com.campusscheduler.ai.models.Laboratory;
import com.campusscheduler.ai.models.Subject;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.utils.FirebaseHelper;
import com.campusscheduler.ai.utils.LocalAuthManager;
import com.campusscheduler.ai.utils.UIUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClassroomActivity extends AppCompatActivity {

    private ImageButton btnBack, btnSyncInventory;
    private FloatingActionButton fabAddClassroom;
    private TextView tvInventoryStatus;
    private Button btnQuickSetupInventory, btnFilterAll, btnFilterClassrooms, btnFilterLabs;
    private RecyclerView rvClassrooms;

    private GenericAdapter<Classroom> adapter;
    private final List<Classroom> allFacilitiesList = new ArrayList<>();
    private final List<Classroom> displayedFacilitiesList = new ArrayList<>();

    private int activeFilter = 0; // 0 = All, 1 = Classrooms, 2 = Labs
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_classroom);

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

        btnBack = findViewById(R.id.btnBack);
        btnSyncInventory = findViewById(R.id.btnSyncInventory);
        fabAddClassroom = findViewById(R.id.fabAddClassroom);
        tvInventoryStatus = findViewById(R.id.tvInventoryStatus);
        btnQuickSetupInventory = findViewById(R.id.btnQuickSetupInventory);
        btnFilterAll = findViewById(R.id.btnFilterAll);
        btnFilterClassrooms = findViewById(R.id.btnFilterClassrooms);
        btnFilterLabs = findViewById(R.id.btnFilterLabs);
        rvClassrooms = findViewById(R.id.rvClassrooms);

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        if (btnSyncInventory != null) btnSyncInventory.setOnClickListener(v -> confirmSetupStandardInventory());
        if (btnQuickSetupInventory != null) btnQuickSetupInventory.setOnClickListener(v -> confirmSetupStandardInventory());
        if (fabAddClassroom != null) fabAddClassroom.setOnClickListener(v -> showAddFacilityDialog());

        setupFilterButtons();
        int filterExtra = getIntent() != null ? getIntent().getIntExtra("filter", 0) : 0;
        if (filterExtra != 0) {
            activeFilter = filterExtra;
        }
        setupRecyclerView();
        loadAllFacilities();
    }

    private void setupFilterButtons() {
        if (btnFilterAll != null) {
            btnFilterAll.setOnClickListener(v -> {
                activeFilter = 0;
                applyCategoryFilter();
            });
        }
        if (btnFilterClassrooms != null) {
            btnFilterClassrooms.setOnClickListener(v -> {
                activeFilter = 1;
                applyCategoryFilter();
            });
        }
        if (btnFilterLabs != null) {
            btnFilterLabs.setOnClickListener(v -> {
                activeFilter = 2;
                applyCategoryFilter();
            });
        }
    }

    private void setupRecyclerView() {
        if (rvClassrooms == null) return;

        rvClassrooms.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GenericAdapter<>(displayedFacilitiesList, (item, holder) -> {
            boolean isLab = item.isHasLabEquipment() || item.isLab();
            String title = (isLab ? "Lab " : "Room ") + item.getRoomNumber()
                    + (item.getBuilding() != null && !item.getBuilding().isEmpty() && !item.getBuilding().equalsIgnoreCase("Lab Block") && !item.getBuilding().equalsIgnoreCase("A Block") ? " (" + item.getBuilding() + ")" : "");

            TextView tvFacilityTitle = holder.itemView.findViewById(R.id.tvFacilityTitle);
            TextView tvFacilityTypeBadge = holder.itemView.findViewById(R.id.tvFacilityTypeBadge);
            TextView tvFacilityCapacity = holder.itemView.findViewById(R.id.tvFacilityCapacity);
            ImageButton btnEditFacility = holder.itemView.findViewById(R.id.btnEditFacility);
            ImageButton btnDeleteFacility = holder.itemView.findViewById(R.id.btnDeleteFacility);
            LinearLayout llLabSubjectsSection = holder.itemView.findViewById(R.id.llLabSubjectsSection);
            TextView btnManageLabSubjects = holder.itemView.findViewById(R.id.btnManageLabSubjects);
            TextView tvLabSubjectsList = holder.itemView.findViewById(R.id.tvLabSubjectsList);

            if (tvFacilityTitle != null) tvFacilityTitle.setText(title);

            if (tvFacilityTypeBadge != null) {
                if (isLab) {
                    tvFacilityTypeBadge.setText("Laboratory");
                    tvFacilityTypeBadge.setBackgroundColor(0xFFE8EAF6);
                    tvFacilityTypeBadge.setTextColor(0xFF283593);
                } else {
                    tvFacilityTypeBadge.setText("Classroom");
                    tvFacilityTypeBadge.setBackgroundColor(0xFFE3F2FD);
                    tvFacilityTypeBadge.setTextColor(0xFF1565C0);
                }
            }

            if (tvFacilityCapacity != null) {
                String bldg = item.getBuilding() != null ? item.getBuilding() : (isLab ? "Lab Block" : "A Block");
                StringBuilder equip = new StringBuilder();
                if (item.isHasProjector()) equip.append(" • Projector");
                if (item.isHasSmartDisplay()) equip.append(" • Smart Display");
                tvFacilityCapacity.setText(item.getCapacity() + " Seats • " + bldg + equip.toString());
            }

            if (btnEditFacility != null) {
                btnEditFacility.setOnClickListener(v -> showEditFacilityDialog(item));
            }

            if (btnDeleteFacility != null) {
                btnDeleteFacility.setOnClickListener(v -> confirmDeleteFacility(item));
            }

            if (llLabSubjectsSection != null) {
                if (isLab) {
                    llLabSubjectsSection.setVisibility(View.VISIBLE);
                    List<String> names = item.getSupportedSubjectNames();
                    if (names != null && !names.isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < names.size(); i++) {
                            sb.append("• ").append(names.get(i));
                            if (i < names.size() - 1) sb.append("\n");
                        }
                        if (tvLabSubjectsList != null) tvLabSubjectsList.setText(sb.toString());
                    } else {
                        if (tvLabSubjectsList != null) tvLabSubjectsList.setText("No specific lab subjects associated. Click '+ Manage Lab Subjects' to associate curriculum labs.");
                    }

                    if (btnManageLabSubjects != null) {
                        btnManageLabSubjects.setOnClickListener(v -> showManageLabSubjectsDialog(item));
                    }
                } else {
                    llLabSubjectsSection.setVisibility(View.GONE);
                }
            }
        }, R.layout.item_facility);

        rvClassrooms.setAdapter(adapter);
    }

    private void loadAllFacilities() {
        String collegeId = (currentUser != null && currentUser.getCollegeId() != null && !currentUser.getCollegeId().trim().isEmpty())
                ? currentUser.getCollegeId() : LocalAuthManager.getSafeCollegeId(this, getIntent());
        if (tvInventoryStatus != null) tvInventoryStatus.setText("Loading facilities from Firestore...");

        FirebaseHelper.getClassroomsRef().whereEqualTo("collegeId", collegeId).get()
                .addOnSuccessListener(classroomSnaps -> {
                    allFacilitiesList.clear();
                    Set<String> seenRoomNumbers = new HashSet<>();

                    if (classroomSnaps != null) {
                        for (DocumentSnapshot doc : classroomSnaps) {
                            Classroom c = doc.toObject(Classroom.class);
                            if (c != null) {
                                if (c.getId() == null) c.setId(doc.getId());
                                // Canonical check: Room 201 is strictly a classroom, not a lab
                                if ("201".equals(c.getRoomNumber()) || "Room 201".equalsIgnoreCase(c.getRoomNumber())) {
                                    c.setHasLabEquipment(false);
                                }
                                allFacilitiesList.add(c);
                                if (c.getRoomNumber() != null) seenRoomNumbers.add(c.getRoomNumber().trim().toLowerCase());
                            }
                        }
                    }

                    // Also check laboratories collection to merge any separate lab documents
                    FirebaseHelper.getLaboratoriesRef().whereEqualTo("collegeId", collegeId).get()
                            .addOnSuccessListener(labSnaps -> {
                                if (labSnaps != null) {
                                    for (DocumentSnapshot doc : labSnaps) {
                                        Laboratory lab = doc.toObject(Laboratory.class);
                                        if (lab != null) {
                                            String labNum = lab.getRoomNumber() != null ? lab.getRoomNumber() : lab.getLabNumber();
                                            if (labNum != null) {
                                                String normLab = labNum.trim().toLowerCase();
                                                if (!seenRoomNumbers.contains(normLab)) {
                                                    Classroom labRoom = new Classroom(
                                                            doc.getId(),
                                                            collegeId,
                                                            labNum,
                                                            lab.getLabName() != null ? lab.getLabName() : "Lab Block",
                                                            lab.getCapacity() > 0 ? lab.getCapacity() : 40,
                                                            true,
                                                            true
                                                    );
                                                    labRoom.setSupportedSubjectIds(lab.getSupportedSubjectIds());
                                                    labRoom.setSupportedSubjectNames(lab.getSupportedSubjectNames());
                                                    allFacilitiesList.add(labRoom);
                                                    seenRoomNumbers.add(normLab);
                                                } else {
                                                    for (Classroom existing : allFacilitiesList) {
                                                        if (existing.getRoomNumber() != null && existing.getRoomNumber().trim().equalsIgnoreCase(labNum.trim())) {
                                                            if (existing.getSupportedSubjectIds().isEmpty() && lab.getSupportedSubjectIds() != null && !lab.getSupportedSubjectIds().isEmpty()) {
                                                                existing.setSupportedSubjectIds(lab.getSupportedSubjectIds());
                                                                existing.setSupportedSubjectNames(lab.getSupportedSubjectNames());
                                                            }
                                                            break;
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                updateInventorySummary();
                                applyCategoryFilter();
                            })
                            .addOnFailureListener(e -> {
                                updateInventorySummary();
                                applyCategoryFilter();
                            });
                })
                .addOnFailureListener(e -> {
                    if (tvInventoryStatus != null) tvInventoryStatus.setText("Error loading classrooms: " + e.getMessage());
                });
    }

    private void updateInventorySummary() {
        int classrooms = 0;
        int labs = 0;
        for (Classroom c : allFacilitiesList) {
            if (c.isHasLabEquipment() || c.isLab()) {
                labs++;
            } else {
                classrooms++;
            }
        }

        if (tvInventoryStatus != null) {
            tvInventoryStatus.setText("Total: " + allFacilitiesList.size() + " Facilities (" + classrooms + " Classrooms, " + labs + " Laboratories)");
        }
    }

    private void applyCategoryFilter() {
        displayedFacilitiesList.clear();
        for (Classroom c : allFacilitiesList) {
            boolean isLab = c.isHasLabEquipment() || c.isLab();
            if (activeFilter == 1 && isLab) continue; // Only classrooms
            if (activeFilter == 2 && !isLab) continue; // Only labs
            displayedFacilitiesList.add(c);
        }

        // Sort by room number naturally
        Collections.sort(displayedFacilitiesList, (a, b) -> {
            String ra = a.getRoomNumber() != null ? a.getRoomNumber().replaceAll("[^0-9]", "") : "";
            String rb = b.getRoomNumber() != null ? b.getRoomNumber().replaceAll("[^0-9]", "") : "";
            if (!ra.isEmpty() && !rb.isEmpty()) {
                try {
                    return Integer.compare(Integer.parseInt(ra), Integer.parseInt(rb));
                } catch (Exception ignored) {}
            }
            return (a.getRoomNumber() != null ? a.getRoomNumber() : "").compareToIgnoreCase(b.getRoomNumber() != null ? b.getRoomNumber() : "");
        });

        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void confirmSetupStandardInventory() {
        new AlertDialog.Builder(this)
                .setTitle("Verify & Setup Campus Inventory")
                .setMessage("This will verify your campus inventory and create any missing standard rooms in Firestore without modifying or duplicating existing records:\n\n"
                        + "• Classrooms: 101–108, 201, 301–304 (60 seats)\n"
                        + "  (Room 201 is verified as regular classroom)\n"
                        + "• Laboratories: 202–208, 305–308 (40 seats)\n\n"
                        + "Do you want to proceed?")
                .setPositiveButton("Setup Inventory", (dialog, which) -> executeStandardInventorySetup())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void executeStandardInventorySetup() {
        String collegeId = currentUser != null && currentUser.getCollegeId() != null ?
                currentUser.getCollegeId() : LocalAuthManager.getSafeCollegeId(this, getIntent());
        if (currentUser != null && (currentUser.getCollegeId() == null || currentUser.getCollegeId().trim().isEmpty())) {
            currentUser.setCollegeId(collegeId);
        }

        // 1. Gather existing room numbers to avoid duplicates
        Set<String> existingRoomNums = new HashSet<>();
        for (Classroom c : allFacilitiesList) {
            if (c.getRoomNumber() != null) {
                existingRoomNums.add(c.getRoomNumber().trim().toLowerCase().replaceAll("[^0-9]", ""));
            }
        }

        // 2. Expected Standard Classrooms: 101-108, 201, 301-304
        String[] standardClassrooms = {"101", "102", "103", "104", "105", "106", "107", "108", "201", "301", "302", "303", "304"};

        // 3. Expected Standard Laboratories:
        Map<String, String> standardLabs = new HashMap<>();
        standardLabs.put("202", "Cloud Computing Lab");
        standardLabs.put("203", "Machine Learning Lab");
        standardLabs.put("204", "Generative AI Lab");
        standardLabs.put("205", "Additional Machine Learning Lab");
        standardLabs.put("206", "IoT Lab");
        standardLabs.put("207", "Parallel Computing Lab");
        standardLabs.put("208", "General Computer Laboratory");
        standardLabs.put("305", "Computer Laboratory 1");
        standardLabs.put("306", "Computer Laboratory 2");
        standardLabs.put("307", "Computer Laboratory 3");
        standardLabs.put("308", "Computer Laboratory 4");

        WriteBatch batch = FirebaseHelper.getFirestore().batch();
        int addedCount = 0;

        for (String crNum : standardClassrooms) {
            if (!existingRoomNums.contains(crNum)) {
                String docId = "ROOM_" + collegeId + "_" + crNum;
                Classroom c = new Classroom(docId, collegeId, crNum, "A Block", 60, true, false, false);
                // Room 201 strictly classroom
                c.setHasLabEquipment(false);
                c.setAvailable(true);
                batch.set(FirebaseHelper.getClassroomsRef().document(docId), c);
                addedCount++;
            }
        }

        for (Map.Entry<String, String> labEntry : standardLabs.entrySet()) {
            String labNum = labEntry.getKey();
            String labName = labEntry.getValue();
            if (!existingRoomNums.contains(labNum)) {
                String labDocId = "LAB_" + collegeId + "_" + labNum;
                Laboratory lab = new Laboratory(labDocId, collegeId, "DEPT_CSE", labName, labNum, 40, "Computer Lab");
                lab.setHasProjector(true);
                lab.setHasSmartDisplay(true);
                batch.set(FirebaseHelper.getLaboratoriesRef().document(labDocId), lab);

                // Also register in classrooms collection with hasLabEquipment = true
                String roomDocId = "ROOM_" + collegeId + "_" + labNum;
                Classroom crLab = new Classroom(roomDocId, collegeId, labNum, labName, 40, true, true, true);
                batch.set(FirebaseHelper.getClassroomsRef().document(roomDocId), crLab);
                addedCount++;
            }
        }

        if (addedCount == 0) {
            UIUtils.showAlert(this, "Inventory Up to Date", "All expected campus classrooms and laboratories already exist in Firestore!");
            return;
        }

        final int finalAdded = addedCount;
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    UIUtils.showAlert(this, "Inventory Configured", "Successfully provisioned " + finalAdded + " missing campus facilities in Firestore without duplicating existing rooms.");
                    loadAllFacilities();
                })
                .addOnFailureListener(e -> UIUtils.showAlert(this, "Setup Error", "Failed to setup inventory: " + e.getMessage()));
    }

    private void showAddFacilityDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Facility");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        EditText etNumber = new EditText(this);
        etNumber.setHint("Room / Lab Number (e.g. 109 or 209)");
        layout.addView(etNumber);

        EditText etCap = new EditText(this);
        etCap.setHint("Capacity (e.g. 60)");
        etCap.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(etCap);

        EditText etBldg = new EditText(this);
        etBldg.setHint("Building / Lab Name (e.g. A Block or AI Lab)");
        layout.addView(etBldg);

        Spinner spType = new Spinner(this);
        String[] types = {"Regular Classroom", "Laboratory"};
        spType.setAdapter(new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, types));
        layout.addView(spType);

        android.widget.CheckBox cbProjector = new android.widget.CheckBox(this);
        cbProjector.setText("Equipped with Projector");
        cbProjector.setChecked(true);
        layout.addView(cbProjector);

        android.widget.CheckBox cbSmartDisplay = new android.widget.CheckBox(this);
        cbSmartDisplay.setText("Equipped with Smart Display");
        cbSmartDisplay.setChecked(false);
        layout.addView(cbSmartDisplay);

        builder.setView(layout);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String num = etNumber.getText().toString().trim();
            String capStr = etCap.getText().toString().trim();
            String bldg = etBldg.getText().toString().trim();
            boolean isLab = spType.getSelectedItemPosition() == 1;

            if (num.isEmpty()) {
                UIUtils.showToast(this, "Room number is required.");
                return;
            }

            // Room 201 cannot be a lab
            final boolean facilityIsLab = !"201".equals(num) && isLab;

            int cap = facilityIsLab ? 40 : 60;
            try { cap = Integer.parseInt(capStr); } catch (Exception ignored) {}

            String collegeId = currentUser.getCollegeId();
            String docId = (facilityIsLab ? "LAB_" : "ROOM_") + collegeId + "_" + num;

            Classroom c = new Classroom(docId, collegeId, num, bldg.isEmpty() ? (facilityIsLab ? "Lab Block" : "A Block") : bldg, cap, cbProjector.isChecked(), cbSmartDisplay.isChecked(), facilityIsLab);

            WriteBatch batch = FirebaseHelper.getFirestore().batch();
            batch.set(FirebaseHelper.getClassroomsRef().document(docId), c);
            if (facilityIsLab) {
                Laboratory lab = new Laboratory(docId, collegeId, "DEPT_CSE", bldg.isEmpty() ? "Computer Lab" : bldg, num, cap, "Computer Lab");
                lab.setHasProjector(cbProjector.isChecked());
                lab.setHasSmartDisplay(cbSmartDisplay.isChecked());
                batch.set(FirebaseHelper.getLaboratoriesRef().document(docId), lab);
            }

            batch.commit()
                    .addOnSuccessListener(aVoid -> {
                        UIUtils.showToast(this, (facilityIsLab ? "Laboratory" : "Classroom") + " added successfully!");
                        loadAllFacilities();
                    })
                    .addOnFailureListener(e -> UIUtils.showToast(this, "Save error: " + e.getMessage()));
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showManageLabSubjectsDialog(Classroom facility) {
        String collegeId = currentUser != null && currentUser.getCollegeId() != null ?
                currentUser.getCollegeId() : LocalAuthManager.getSafeCollegeId(this, getIntent());
        if (currentUser != null && (currentUser.getCollegeId() == null || currentUser.getCollegeId().trim().isEmpty())) {
            currentUser.setCollegeId(collegeId);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Manage Subjects — Lab " + facility.getRoomNumber());

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(30, 20, 30, 20);

        TextView tvSubtitle = new TextView(this);
        tvSubtitle.setText("Facility: Lab " + facility.getRoomNumber() + " (" + (facility.getBuilding() != null ? facility.getBuilding() : "Lab Block") + ")");
        tvSubtitle.setTextColor(0xFF0D47A1);
        tvSubtitle.setTextSize(14f);
        tvSubtitle.setTypeface(null, android.graphics.Typeface.BOLD);
        container.addView(tvSubtitle);

        Button btnAddExisting = new Button(this);
        btnAddExisting.setText("+ Add Existing Subject");
        btnAddExisting.setBackgroundColor(0xFF1565C0);
        btnAddExisting.setTextColor(0xFFFFFFFF);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        btnParams.setMargins(0, 20, 0, 16);
        btnAddExisting.setLayoutParams(btnParams);
        container.addView(btnAddExisting);

        TextView tvHeader = new TextView(this);
        List<String> names = facility.getSupportedSubjectNames();
        List<String> ids = facility.getSupportedSubjectIds();
        tvHeader.setText("Currently Associated Subjects (" + (names != null ? names.size() : 0) + "):");
        tvHeader.setTextColor(0xFF37474F);
        tvHeader.setTextSize(13f);
        tvHeader.setTypeface(null, android.graphics.Typeface.BOLD);
        tvHeader.setPadding(0, 0, 0, 10);
        container.addView(tvHeader);

        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        LinearLayout subjectsListLayout = new LinearLayout(this);
        subjectsListLayout.setOrientation(LinearLayout.VERTICAL);

        if (names == null || names.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("No subjects currently associated with this lab.\nClick '+ Add Existing Subject' above to map curriculum subjects.");
            tvEmpty.setTextColor(0xFF78909C);
            tvEmpty.setPadding(8, 12, 8, 12);
            subjectsListLayout.addView(tvEmpty);
        } else {
            for (int i = 0; i < names.size(); i++) {
                final int index = i;
                final String sName = names.get(i);
                final String sId = (ids != null && ids.size() > i) ? ids.get(i) : null;

                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(android.view.Gravity.CENTER_VERTICAL);
                row.setPadding(8, 10, 8, 10);
                row.setBackgroundColor(i % 2 == 0 ? 0xFFF1F5F9 : 0xFFFFFFFF);

                TextView tvSubName = new TextView(this);
                tvSubName.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                tvSubName.setText("• " + sName);
                tvSubName.setTextColor(0xFF263238);
                tvSubName.setTextSize(13f);
                row.addView(tvSubName);

                Button btnRemove = new Button(this, null, android.R.attr.borderlessButtonStyle);
                btnRemove.setText("Remove");
                btnRemove.setTextColor(0xFFD32F2F);
                btnRemove.setTextSize(11f);
                btnRemove.setOnClickListener(v -> {
                    new AlertDialog.Builder(this)
                            .setTitle("Remove Subject Association?")
                            .setMessage("Remove \"" + sName + "\" from Lab " + facility.getRoomNumber() + "?\n\n(The subject itself will remain in Subject Management catalog).")
                            .setPositiveButton("Remove", (removeDialog, whichRemove) -> {
                                facility.removeSupportedSubject(sId);
                                saveLabSubjectAssociations(facility, () -> {
                                    UIUtils.showToast(this, "Removed subject association.");
                                    showManageLabSubjectsDialog(facility);
                                });
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                });
                row.addView(btnRemove);

                subjectsListLayout.addView(row);
            }
        }

        scrollView.addView(subjectsListLayout);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 450);
        scrollView.setLayoutParams(scrollParams);
        container.addView(scrollView);

        builder.setView(container);
        builder.setPositiveButton("Done", (dialog, which) -> {
            if (adapter != null) adapter.notifyDataSetChanged();
        });

        AlertDialog dialog = builder.create();

        btnAddExisting.setOnClickListener(v -> {
            dialog.dismiss();
            openAddExistingSubjectToLabDialog(facility);
        });

        dialog.show();
    }

    private void openAddExistingSubjectToLabDialog(Classroom facility) {
        String collegeId = (currentUser != null && currentUser.getCollegeId() != null && !currentUser.getCollegeId().trim().isEmpty())
                ? currentUser.getCollegeId() : LocalAuthManager.getSafeCollegeId(this, getIntent());

        ProgressDialog progress = UIUtils.showLoadingDialog(this, "Loading academic subjects...");
        FirebaseHelper.getSubjectsRef().whereEqualTo("collegeId", collegeId).get()
                .addOnSuccessListener(querySnapshot -> {
            progress.dismiss();
            List<Subject> eligibleSubjects = new ArrayList<>();
            List<String> currentIds = facility.getSupportedSubjectIds();

            if (querySnapshot != null) {
                for (DocumentSnapshot doc : querySnapshot) {
                    Subject s = doc.toObject(Subject.class);
                    if (s != null) {
                        if (s.getId() == null) s.setId(doc.getId());
                        // Exclude subjects already associated with this laboratory
                        if (currentIds == null || !currentIds.contains(s.getId())) {
                            eligibleSubjects.add(s);
                        }
                    }
                }
            }

            if (querySnapshot == null || querySnapshot.isEmpty()) {
                new AlertDialog.Builder(this)
                        .setTitle("No Subjects in Catalog")
                        .setMessage("No subjects found in the academic catalog. Please create subjects in Subject Management first.")
                        .setPositiveButton("Open Subject Management", (d, w) -> startActivity(new android.content.Intent(this, SubjectActivity.class)))
                        .setNegativeButton("Close", (d, w) -> showManageLabSubjectsDialog(facility))
                        .show();
                return;
            }

            if (eligibleSubjects.isEmpty()) {
                UIUtils.showAlert(this, "All Subjects Associated", "All available subjects in your curriculum are already associated with Lab " + facility.getRoomNumber());
                showManageLabSubjectsDialog(facility);
                return;
            }

            // Sort: Labs first, then semester and name
            Collections.sort(eligibleSubjects, (a, b) -> {
                if (a.isLab() != b.isLab()) return a.isLab() ? -1 : 1;
                if (a.getSemester() != b.getSemester()) return Integer.compare(a.getSemester(), b.getSemester());
                return (a.getSubjectName() != null ? a.getSubjectName() : "").compareToIgnoreCase(b.getSubjectName() != null ? b.getSubjectName() : "");
            });

            CharSequence[] items = new CharSequence[eligibleSubjects.size()];
            boolean[] checked = new boolean[eligibleSubjects.size()];

            for (int i = 0; i < eligibleSubjects.size(); i++) {
                Subject s = eligibleSubjects.get(i);
                String tag = s.isLab() ? "[LAB] " : "[THEORY] ";
                items[i] = tag + s.getSubjectName() + " (" + s.getSubjectCode() + ") - Sem " + s.getSemester();
            }

            new AlertDialog.Builder(this)
                    .setTitle("Add Subjects to Lab " + facility.getRoomNumber())
                    .setMultiChoiceItems(items, checked, (d, which, isChecked) -> {
                        checked[which] = isChecked;
                    })
                    .setPositiveButton("Associate Selected", (d, which) -> {
                        int addedCount = 0;
                        for (int i = 0; i < eligibleSubjects.size(); i++) {
                            if (checked[i]) {
                                Subject s = eligibleSubjects.get(i);
                                facility.addSupportedSubject(s.getId(), s.getSubjectName() + " (" + s.getSubjectCode() + ")");
                                addedCount++;
                            }
                        }

                        if (addedCount > 0) {
                            final int count = addedCount;
                            saveLabSubjectAssociations(facility, () -> {
                                UIUtils.showToast(this, "Associated " + count + " subject(s) to Lab " + facility.getRoomNumber());
                                showManageLabSubjectsDialog(facility);
                            });
                        } else {
                            showManageLabSubjectsDialog(facility);
                        }
                    })
                    .setNegativeButton("Cancel", (d, which) -> showManageLabSubjectsDialog(facility))
                    .show();
        }).addOnFailureListener(e -> {
            progress.dismiss();
            UIUtils.showAlert(this, "Error", "Failed to load subjects: " + e.getMessage());
            showManageLabSubjectsDialog(facility);
        });
    }

    private void saveLabSubjectAssociations(Classroom facility, Runnable onSuccess) {
        String collegeId = (currentUser != null && currentUser.getCollegeId() != null && !currentUser.getCollegeId().trim().isEmpty())
                ? currentUser.getCollegeId() : LocalAuthManager.getSafeCollegeId(this, getIntent());
        WriteBatch batch = FirebaseHelper.getFirestore().batch();

        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put("supportedSubjectIds", facility.getSupportedSubjectIds());
        updateMap.put("supportedSubjectNames", facility.getSupportedSubjectNames());

        if (facility.getId() != null) {
            batch.set(FirebaseHelper.getClassroomsRef().document(facility.getId()),
                    updateMap, com.google.firebase.firestore.SetOptions.merge());
        }
        String labDocId = "LAB_" + collegeId + "_" + facility.getRoomNumber();
        batch.set(FirebaseHelper.getLaboratoriesRef().document(labDocId),
                updateMap, com.google.firebase.firestore.SetOptions.merge());

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    if (adapter != null) adapter.notifyDataSetChanged();
                    if (onSuccess != null) onSuccess.run();
                })
                .addOnFailureListener(e -> UIUtils.showToast(this, "Save error: " + e.getMessage()));
    }

    private void showEditFacilityDialog(Classroom facility) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        boolean isLab = facility.isHasLabEquipment() || facility.isLab();
        builder.setTitle("Edit " + (isLab ? "Laboratory " : "Room ") + facility.getRoomNumber());

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        TextView tvRoomLabel = new TextView(this);
        tvRoomLabel.setText("Facility: " + (isLab ? "Lab " : "Room ") + facility.getRoomNumber());
        tvRoomLabel.setTextColor(0xFF0D47A1);
        tvRoomLabel.setTextSize(15f);
        tvRoomLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(tvRoomLabel);

        EditText etCap = new EditText(this);
        etCap.setHint("Capacity");
        etCap.setText(String.valueOf(facility.getCapacity()));
        etCap.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(etCap);

        EditText etBldg = new EditText(this);
        etBldg.setHint("Building / Facility Name");
        etBldg.setText(facility.getBuilding() != null ? facility.getBuilding() : "");
        layout.addView(etBldg);

        // Separate, independent Projector control
        android.widget.CheckBox cbProjector = new android.widget.CheckBox(this);
        cbProjector.setText("Equipped with Projector: " + (facility.isHasProjector() ? "Yes" : "No"));
        cbProjector.setChecked(facility.isHasProjector());
        cbProjector.setOnCheckedChangeListener((btn, isChecked) -> btn.setText("Equipped with Projector: " + (isChecked ? "Yes" : "No")));
        layout.addView(cbProjector);

        // Separate, independent Smart Display control
        android.widget.CheckBox cbSmartDisplay = new android.widget.CheckBox(this);
        cbSmartDisplay.setText("Equipped with Smart Display: " + (facility.isHasSmartDisplay() ? "Yes" : "No"));
        cbSmartDisplay.setChecked(facility.isHasSmartDisplay());
        cbSmartDisplay.setOnCheckedChangeListener((btn, isChecked) -> btn.setText("Equipped with Smart Display: " + (isChecked ? "Yes" : "No")));
        layout.addView(cbSmartDisplay);

        builder.setView(layout);
        builder.setPositiveButton("Update", (dialog, which) -> {
            String capStr = etCap.getText().toString().trim();
            String bldg = etBldg.getText().toString().trim();
            int cap = facility.getCapacity();
            try { cap = Integer.parseInt(capStr); } catch (Exception ignored) {}

            facility.setCapacity(cap);
            if (!bldg.isEmpty()) facility.setBuilding(bldg);
            facility.setHasProjector(cbProjector.isChecked());
            facility.setHasSmartDisplay(cbSmartDisplay.isChecked());

            // Canonical check: Room 201 must strictly remain classroom
            if ("201".equals(facility.getRoomNumber())) {
                facility.setHasLabEquipment(false);
            }

            WriteBatch batch = FirebaseHelper.getFirestore().batch();
            if (facility.getId() != null) {
                batch.update(FirebaseHelper.getClassroomsRef().document(facility.getId()),
                        "capacity", cap,
                        "building", facility.getBuilding(),
                        "buildingBlock", facility.getBuilding(),
                        "hasProjector", cbProjector.isChecked(),
                        "hasSmartDisplay", cbSmartDisplay.isChecked());
            }
            if (facility.isLab()) {
                String labDocId = "LAB_" + currentUser.getCollegeId() + "_" + facility.getRoomNumber();
                batch.update(FirebaseHelper.getLaboratoriesRef().document(labDocId),
                        "capacity", cap,
                        "labName", facility.getBuilding(),
                        "hasProjector", cbProjector.isChecked(),
                        "hasSmartDisplay", cbSmartDisplay.isChecked());
            }

            batch.commit()
                    .addOnSuccessListener(aVoid -> {
                        UIUtils.showToast(this, "Facility updated successfully!");
                        updateInventorySummary();
                        applyCategoryFilter();
                    })
                    .addOnFailureListener(e -> UIUtils.showToast(this, "Update failed: " + e.getMessage()));
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void confirmDeleteFacility(Classroom facility) {
        String num = facility.getRoomNumber() != null ? facility.getRoomNumber() : "";
        String collegeId = currentUser != null ? currentUser.getCollegeId() : "";

        // Check if referenced in classSessions
        ProgressDialog progress = UIUtils.showLoadingDialog(this, "Checking timetable references...");
        FirebaseHelper.getClassSessionsRef()
                .whereEqualTo("collegeId", collegeId)
                .whereEqualTo("roomNumber", num)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    progress.dismiss();
                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        UIUtils.showAlert(this, "Cannot Delete Facility",
                                "Facility " + num + " cannot be deleted because it is currently assigned to "
                                        + querySnapshot.size() + " active timetable session(s). Please reassign or publish an updated timetable before removing this facility.");
                        return;
                    }

                    new AlertDialog.Builder(this)
                            .setTitle("Delete Facility " + num)
                            .setMessage("Are you sure you want to delete " + (facility.isLab() ? "Laboratory " : "Room ") + num + "? This action cannot be undone.")
                            .setPositiveButton("Delete", (dialog, which) -> {
                                WriteBatch batch = FirebaseHelper.getFirestore().batch();
                                if (facility.getId() != null) {
                                    batch.delete(FirebaseHelper.getClassroomsRef().document(facility.getId()));
                                }
                                String labDocId = "LAB_" + collegeId + "_" + num;
                                batch.delete(FirebaseHelper.getLaboratoriesRef().document(labDocId));

                                batch.commit()
                                        .addOnSuccessListener(aVoid -> {
                                            UIUtils.showToast(this, "Facility deleted successfully.");
                                            allFacilitiesList.remove(facility);
                                            updateInventorySummary();
                                            applyCategoryFilter();
                                        })
                                        .addOnFailureListener(e -> UIUtils.showToast(this, "Delete failed: " + e.getMessage()));
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                })
                .addOnFailureListener(e -> {
                    progress.dismiss();
                    UIUtils.showAlert(this, "Error", "Failed to verify facility usage: " + e.getMessage());
                });
    }
}
