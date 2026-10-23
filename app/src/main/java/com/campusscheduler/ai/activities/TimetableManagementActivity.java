package com.campusscheduler.ai.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.campusscheduler.ai.R;
import com.campusscheduler.ai.adapters.GenericAdapter;
import com.campusscheduler.ai.models.Timetable;
import com.campusscheduler.ai.models.TimetableSlot;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.utils.CollegeIdentityHelper;
import com.campusscheduler.ai.utils.FirebaseHelper;
import com.campusscheduler.ai.utils.LocalAuthManager;
import com.campusscheduler.ai.utils.TimetablePdfExporter;
import com.campusscheduler.ai.utils.UIUtils;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TimetableManagementActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private RecyclerView rvTimetables;
    private GenericAdapter<Timetable> adapter;
    private final List<Timetable> timetableList = new ArrayList<>();
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timetable_management);

        currentUser = LocalAuthManager.getCurrentLocalUser(this);
        String safeCollegeId = LocalAuthManager.getSafeCollegeId(this, getIntent());
        if (currentUser == null) {
            currentUser = new User();
            currentUser.setCollegeId(safeCollegeId);
        } else if (currentUser.getCollegeId() == null || currentUser.getCollegeId().trim().isEmpty()) {
            currentUser.setCollegeId(safeCollegeId);
        }

        btnBack = findViewById(R.id.btnBack);
        rvTimetables = findViewById(R.id.rvTimetables);

        btnBack.setOnClickListener(v -> finish());

        rvTimetables.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GenericAdapter<>(timetableList, (item, holder) -> {
            holder.setText(R.id.tvTitle, item.getTitle() + " [" + item.getStatus() + "]");
            String semSec = (item.getSemester() > 0 ? "Sem " + item.getSemester() + " " : "")
                    + (item.getSection() != null ? "• Sec " + item.getSection() + " " : "")
                    + (item.getAcademicYear() != null ? "• " + item.getAcademicYear() : "");
            holder.setText(R.id.tvSubtitle, semSec + "\nFitness: " + String.format("%.1f", item.getFitnessScore())
                    + "% | Conflicts: " + item.getHardConflictsCount()
                    + " | Slots: " + (item.getSlots() != null ? item.getSlots().size() : 0));

            holder.itemView.setOnClickListener(v -> showTimetablePreviewDialog(item));
        }, R.layout.item_generic);
        rvTimetables.setAdapter(adapter);

        loadTimetables();
    }

    private void loadTimetables() {
        String collegeId = currentUser != null && currentUser.getCollegeId() != null ?
                currentUser.getCollegeId() : LocalAuthManager.getSafeCollegeId(this, getIntent());
        FirebaseHelper.getTimetablesRef().whereEqualTo("collegeId", collegeId).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    timetableList.clear();
                    if (queryDocumentSnapshots != null) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            Timetable t = doc.toObject(Timetable.class);
                            if (t != null) {
                                if (t.getId() == null || t.getId().isEmpty()) {
                                    t.setId(doc.getId());
                                }
                                timetableList.add(t);
                            }
                        }
                    }
                    if (timetableList.isEmpty()) {
                        UIUtils.showToast(TimetableManagementActivity.this, "No timetables generated.");
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    UIUtils.showToast(TimetableManagementActivity.this, "Failed to load timetables.");
                });
    }

    private void showTimetablePreviewDialog(Timetable timetable) {
        if (timetable == null) return;

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_timetable_details, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        ImageView dialogHkbkLogo = dialogView.findViewById(R.id.dialogHkbkLogo);
        TextView dialogHkbkTitle = dialogView.findViewById(R.id.dialogHkbkTitle);
        TextView dialogHkbkSubtitle = dialogView.findViewById(R.id.dialogHkbkSubtitle);
        TextView dialogTimetableTitle = dialogView.findViewById(R.id.dialogTimetableTitle);
        TextView dialogCohortInfo = dialogView.findViewById(R.id.dialogCohortInfo);
        TextView dialogTimetableBadge = dialogView.findViewById(R.id.dialogTimetableBadge);
        TextView dialogTvFitness = dialogView.findViewById(R.id.dialogTvFitness);
        TextView dialogTvConflicts = dialogView.findViewById(R.id.dialogTvConflicts);
        TextView dialogTvTotalSessions = dialogView.findViewById(R.id.dialogTvTotalSessions);
        LinearLayout llDialogSlotsContainer = dialogView.findViewById(R.id.llDialogSlotsContainer);
        Button btnPrintTimetable = dialogView.findViewById(R.id.btnPrintTimetable);
        Button btnDeleteTimetable = dialogView.findViewById(R.id.btnDeleteTimetable);
        Button btnCloseDialog = dialogView.findViewById(R.id.btnCloseDialog);

        if (btnPrintTimetable != null) {
            btnPrintTimetable.setOnClickListener(v -> {
                String[] printOptions = {
                        "Print Timetable (Android System Print / Save as PDF)",
                        "Share PDF Document (WhatsApp / Gmail / Drive)"
                };
                new AlertDialog.Builder(this)
                        .setTitle("Print / Export Official Timetable")
                        .setItems(printOptions, (pDialog, which) -> {
                            if (which == 0) {
                                TimetablePdfExporter.exportAndPrint(this, timetable);
                            } else {
                                TimetablePdfExporter.exportAndSharePdf(this, timetable);
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        if (dialogHkbkLogo != null) {
            CollegeIdentityHelper.bindCollegeLogo(timetable.getCollegeId(), timetable.getCollegeName(), dialogHkbkLogo);
        }
        if (dialogHkbkTitle != null) {
            String colName = timetable.getCollegeName() != null ? timetable.getCollegeName() : "HKBK College of Engineering";
            dialogHkbkTitle.setText(CollegeIdentityHelper.formatCollegeName(colName).toUpperCase());
        }
        if (dialogHkbkSubtitle != null) {
            String colAddr = timetable.getCollegeAddress() != null ? timetable.getCollegeAddress() :
                    CollegeIdentityHelper.getVerifiedCollegeAddress(timetable.getCollegeId(), timetable.getCollegeName());
            dialogHkbkSubtitle.setText("Approved by AICTE • Affiliated to VTU • " + colAddr);
        }

        if (dialogTimetableTitle != null) {
            dialogTimetableTitle.setText(timetable.getTitle() != null ? timetable.getTitle() : "Official Timetable");
        }

        if (dialogCohortInfo != null) {
            StringBuilder cohortSb = new StringBuilder();
            if (timetable.getCollegeName() != null && !timetable.getCollegeName().isEmpty()) {
                cohortSb.append(timetable.getCollegeName()).append("\n");
            }
            if (timetable.getDepartmentName() != null && !timetable.getDepartmentName().isEmpty()) {
                cohortSb.append("Dept: ").append(timetable.getDepartmentName()).append(" • ");
            } else if (timetable.getDepartmentId() != null && !timetable.getDepartmentId().isEmpty()) {
                cohortSb.append("Dept: ").append(timetable.getDepartmentId()).append(" • ");
            }
            if (timetable.getSemester() > 0) {
                cohortSb.append(timetable.getSemester()).append("th Semester ");
            }
            if (timetable.getSection() != null && !timetable.getSection().isEmpty()) {
                cohortSb.append("Section ").append(timetable.getSection()).append(" ");
            }
            if (timetable.getAcademicYear() != null && !timetable.getAcademicYear().isEmpty()) {
                cohortSb.append("• AY: ").append(timetable.getAcademicYear());
            }
            if (timetable.getClassTeacherName() != null && !timetable.getClassTeacherName().isEmpty()) {
                cohortSb.append("\nClass Teacher: ").append(timetable.getClassTeacherName());
            }
            if (timetable.getEffectiveStartDate() != null && !timetable.getEffectiveStartDate().isEmpty()) {
                cohortSb.append(" • Effective: ").append(timetable.getEffectiveStartDate())
                        .append(" to ").append(timetable.getEffectiveEndDate());
            }
            dialogCohortInfo.setText(cohortSb.toString().trim());
        }

        if (dialogTimetableBadge != null) {
            String status = timetable.getStatus() != null ? timetable.getStatus() : Timetable.STATE_PUBLISHED;
            dialogTimetableBadge.setText("OFFICIAL CLASS TIMETABLE • " + status.toUpperCase());
        }

        if (dialogTvFitness != null) {
            dialogTvFitness.setText("Fitness: " + String.format("%.1f", timetable.getFitnessScore()) + "%");
        }

        if (dialogTvConflicts != null) {
            int conflicts = timetable.getHardConflictsCount();
            dialogTvConflicts.setText("Conflicts: " + conflicts);
            dialogTvConflicts.setTextColor(conflicts == 0 ? 0xFF2E7D32 : 0xFFC62828);
        }

        List<TimetableSlot> slots = timetable.getSlots();
        if (dialogTvTotalSessions != null) {
            dialogTvTotalSessions.setText("Slots: " + (slots != null ? slots.size() : 0));
        }

        // Render Day-Wise Grouped Slots
        if (llDialogSlotsContainer != null) {
            llDialogSlotsContainer.removeAllViews();

            if (slots == null || slots.isEmpty()) {
                TextView emptyTv = new TextView(this);
                emptyTv.setText("No slot records found for this timetable.");
                emptyTv.setTextColor(0xFF78909C);
                emptyTv.setPadding(16, 24, 16, 24);
                llDialogSlotsContainer.addView(emptyTv);
            } else {
                Map<String, List<TimetableSlot>> dayMap = new LinkedHashMap<>();
                String[] standardDays = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
                for (String day : standardDays) {
                    dayMap.put(day, new ArrayList<>());
                }

                for (TimetableSlot slot : slots) {
                    if (slot == null) continue;
                    String d = slot.getDay();
                    if (d == null || d.trim().isEmpty()) {
                        d = slot.getDayOfWeek();
                    }
                    if (d == null || d.trim().isEmpty()) {
                        d = "Monday";
                    }

                    boolean matched = false;
                    for (String stdDay : standardDays) {
                        if (stdDay.equalsIgnoreCase(d.trim())) {
                            dayMap.get(stdDay).add(slot);
                            matched = true;
                            break;
                        }
                    }
                    if (!matched) {
                        if (!dayMap.containsKey(d)) {
                            dayMap.put(d, new ArrayList<>());
                        }
                        dayMap.get(d).add(slot);
                    }
                }

                LayoutInflater inflater = LayoutInflater.from(this);

                for (Map.Entry<String, List<TimetableSlot>> entry : dayMap.entrySet()) {
                    List<TimetableSlot> daySlots = entry.getValue();
                    if (daySlots.isEmpty()) continue;

                    // Sort slots chronologically (numeric minutes)
                    Collections.sort(daySlots, (a, b) -> {
                        int minA = UIUtils.parseTimeToMinutes(a.getStartTime());
                        int minB = UIUtils.parseTimeToMinutes(b.getStartTime());
                        return Integer.compare(minA, minB);
                    });

                    // Add Day Header
                    TextView dayHeader = new TextView(this);
                    dayHeader.setText("🗓️  " + entry.getKey().toUpperCase() + " (" + daySlots.size() + " SESSIONS)");
                    dayHeader.setTextSize(13f);
                    dayHeader.setTextColor(0xFF0D47A1);
                    dayHeader.setTypeface(null, android.graphics.Typeface.BOLD);
                    dayHeader.setPadding(8, 16, 8, 8);
                    llDialogSlotsContainer.addView(dayHeader);

                    boolean shortBreakShown = false;
                    boolean lunchBreakShown = false;

                    for (TimetableSlot slot : daySlots) {
                        int startMinutes = UIUtils.parseTimeToMinutes(slot.getStartTime());

                        // Short Break: 09:00 AM - 09:20 AM
                        if (!shortBreakShown && startMinutes >= 560) {
                            llDialogSlotsContainer.addView(UIUtils.createBreakCard(this, "Short Tea Break", "09:00 AM - 09:20 AM (Protected 20 mins)"));
                            shortBreakShown = true;
                        }

                        // Lunch Break: 12:50 PM - 01:40 PM
                        if (!lunchBreakShown && startMinutes >= 820) {
                            llDialogSlotsContainer.addView(UIUtils.createBreakCard(this, "College Lunch Break", "12:50 PM - 01:40 PM (Protected 50 mins)"));
                            lunchBreakShown = true;
                        }

                        View slotCard = inflater.inflate(R.layout.item_teacher_timetable_slot, llDialogSlotsContainer, false);

                        TextView tvTimeSlot = slotCard.findViewById(R.id.tvTimeSlot);
                        TextView tvDayName = slotCard.findViewById(R.id.tvDayName);
                        TextView tvSubjectTitle = slotCard.findViewById(R.id.tvSubjectTitle);
                        TextView tvSectionRoom = slotCard.findViewById(R.id.tvSectionRoom);
                        TextView tvClassTypeBadge = slotCard.findViewById(R.id.tvClassTypeBadge);

                        String timeStr = (slot.getStartTime() != null ? slot.getStartTime() : "08:20 AM")
                                + " - " + (slot.getEndTime() != null ? slot.getEndTime() : "09:00 AM");
                        if (tvTimeSlot != null) tvTimeSlot.setText(timeStr);
                        if (tvDayName != null) tvDayName.setText(slot.getDay());

                        String subCode = slot.getSubjectCode() != null && !slot.getSubjectCode().isEmpty() ? " (" + slot.getSubjectCode() + ")" : "";
                        String subTitle = (slot.getSubjectName() != null ? slot.getSubjectName() : "Class Session") + subCode;
                        if (tvSubjectTitle != null) tvSubjectTitle.setText(subTitle);

                        String teacher = slot.getTeacherName() != null && !slot.getTeacherName().isEmpty() ? slot.getTeacherName() : "Faculty";
                        String venue = slot.getRoomName() != null && !slot.getRoomName().isEmpty() ? slot.getRoomName() : "Assigned Hall";
                        if (tvSectionRoom != null) {
                            tvSectionRoom.setText("Faculty: " + teacher + " • Venue: " + venue);
                        }

                        if (tvClassTypeBadge != null) {
                            boolean isLab = slot.isLab() || "Lab".equalsIgnoreCase(slot.getClassType());
                            if (isLab) {
                                tvClassTypeBadge.setText("Laboratory • Practical");
                                tvClassTypeBadge.setBackgroundColor(0xFFE8EAF6);
                                tvClassTypeBadge.setTextColor(0xFF283593);
                            } else {
                                tvClassTypeBadge.setText("Theory • Lecture");
                                tvClassTypeBadge.setBackgroundColor(0xFFE3F2FD);
                                tvClassTypeBadge.setTextColor(0xFF1565C0);
                            }
                        }

                        llDialogSlotsContainer.addView(slotCard);
                    }

                    if ("Friday".equalsIgnoreCase(entry.getKey()) || "Saturday".equalsIgnoreCase(entry.getKey())) {
                        TextView cutoffNotice = new TextView(this);
                        if ("Friday".equalsIgnoreCase(entry.getKey())) {
                            cutoffNotice.setText("🕌 Friday Cutoff at 12:50 PM — Afternoon prayer & half-day schedule");
                        } else {
                            cutoffNotice.setText("🗓️ Saturday Schedule — 5 periods ending at 12:50 PM");
                        }
                        cutoffNotice.setTextSize(11f);
                        cutoffNotice.setTextColor(0xFF2E7D32);
                        cutoffNotice.setPadding(16, 6, 16, 16);
                        llDialogSlotsContainer.addView(cutoffNotice);
                    }
                }
            }
        }

        if (btnDeleteTimetable != null) {
            btnDeleteTimetable.setOnClickListener(v -> confirmDeleteTimetable(timetable, dialog));
        }

        if (btnCloseDialog != null) {
            btnCloseDialog.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private void confirmDeleteTimetable(Timetable timetable, AlertDialog parentDialog) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Timetable")
                .setMessage("Are you sure you want to permanently delete this timetable from the college records?")
                .setPositiveButton("Delete", (d, w) -> {
                    String targetId = timetable.getId();
                    if (targetId == null || targetId.isEmpty()) {
                        targetId = timetable.getTimetableId();
                    }
                    if (targetId != null && !targetId.isEmpty()) {
                        FirebaseHelper.getTimetablesRef().document(targetId).delete()
                                .addOnSuccessListener(aVoid -> {
                                    UIUtils.showToast(this, "Timetable deleted successfully.");
                                    if (parentDialog != null) parentDialog.dismiss();
                                    loadTimetables();
                                })
                                .addOnFailureListener(e -> {
                                    UIUtils.showToast(this, "Failed to delete timetable: " + e.getMessage());
                                });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
