package com.campusscheduler.ai.activities;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.campusscheduler.ai.R;
import com.campusscheduler.ai.models.ClassSession;
import com.campusscheduler.ai.models.LeaveRequest;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.utils.FirebaseHelper;
import com.campusscheduler.ai.utils.LocalAuthManager;
import com.campusscheduler.ai.utils.UIUtils;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class FacultyAvailabilityActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private Spinner spAvailabilityDay;
    private TextView tvCountFreePeriods, tvCountOccupiedPeriods, tvAvailabilitySubtitle;
    private LinearLayout llPeriodSlotsList;

    private User currentUser;
    private final String[] weekDays = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
    private final String[] periodTimes = {
            "08:20 AM - 09:10 AM",
            "09:10 AM - 10:00 AM",
            "10:20 AM - 11:10 AM",
            "11:10 AM - 12:00 PM",
            "12:00 PM - 12:50 PM",
            "01:40 PM - 02:30 PM",
            "02:30 PM - 03:20 PM",
            "03:20 PM - 04:10 PM"
    };

    private final List<ClassSession> teacherSessions = new ArrayList<>();
    private final List<LeaveRequest> approvedLeaves = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_availability);

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

        btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        spAvailabilityDay = findViewById(R.id.spAvailabilityDay);
        tvCountFreePeriods = findViewById(R.id.tvCountFreePeriods);
        tvCountOccupiedPeriods = findViewById(R.id.tvCountOccupiedPeriods);
        tvAvailabilitySubtitle = findViewById(R.id.tvAvailabilitySubtitle);
        llPeriodSlotsList = findViewById(R.id.llPeriodSlotsList);

        setupDaySpinner();
        loadTimetableAndLeaves();
    }

    private void setupDaySpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, weekDays);
        spAvailabilityDay.setAdapter(adapter);

        // Pre-select today's day
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
        String todayDay = sdf.format(new java.util.Date());
        for (int i = 0; i < weekDays.length; i++) {
            if (weekDays[i].equalsIgnoreCase(todayDay)) {
                spAvailabilityDay.setSelection(i);
                break;
            }
        }

        spAvailabilityDay.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                renderAvailabilityForDay(weekDays[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadTimetableAndLeaves() {
        String collegeId = (currentUser != null && currentUser.getCollegeId() != null && !currentUser.getCollegeId().trim().isEmpty())
                ? currentUser.getCollegeId().trim() : "COLLEGE_1789406176044";

        FirebaseHelper.getClassSessionsRef().whereEqualTo("collegeId", collegeId).get()
                .addOnSuccessListener(csSnaps -> {
                    teacherSessions.clear();
                    if (csSnaps != null) {
                        for (DocumentSnapshot doc : csSnaps) {
                            ClassSession cs = doc.toObject(ClassSession.class);
                            if (cs != null && isUserSession(cs)) {
                                teacherSessions.add(cs);
                            }
                        }
                    }

                    FirebaseHelper.getLeaveRequestsRef().whereEqualTo("collegeId", collegeId).get()
                            .addOnSuccessListener(lrSnaps -> {
                                approvedLeaves.clear();
                                if (lrSnaps != null) {
                                    for (DocumentSnapshot doc : lrSnaps) {
                                        LeaveRequest lr = doc.toObject(LeaveRequest.class);
                                        if (lr != null && "APPROVED".equalsIgnoreCase(lr.getStatus())) {
                                            if (isUserLeave(lr)) {
                                                approvedLeaves.add(lr);
                                            }
                                        }
                                    }
                                }

                                int selectedDayPos = spAvailabilityDay.getSelectedItemPosition();
                                if (selectedDayPos >= 0 && selectedDayPos < weekDays.length) {
                                    renderAvailabilityForDay(weekDays[selectedDayPos]);
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load faculty schedule: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private boolean isUserSession(ClassSession cs) {
        if (currentUser == null) return false;
        String uid = currentUser.getUid();
        String name = currentUser.getName();

        if (uid != null && (uid.equalsIgnoreCase(cs.getFacultyId()) || uid.equalsIgnoreCase(cs.getTeacherUid()))) {
            return true;
        }

        if (name != null) {
            String facName = cs.getFacultyName() != null ? cs.getFacultyName() : cs.getTeacherName();
            if (facName != null && !facName.isEmpty()) {
                String cleanUser = name.replace("Dr.", "").replace("Prof.", "").trim();
                String cleanFac = facName.replace("Dr.", "").replace("Prof.", "").trim();
                return cleanFac.equalsIgnoreCase(cleanUser) || facName.equalsIgnoreCase(name);
            }
        }
        return false;
    }

    private boolean isUserLeave(LeaveRequest lr) {
        if (currentUser == null) return false;
        String uid = currentUser.getUid();
        String name = currentUser.getName();

        if (uid != null && uid.equalsIgnoreCase(lr.getApplicantId())) return true;
        if (name != null && lr.getApplicantName() != null && name.equalsIgnoreCase(lr.getApplicantName().trim())) return true;
        return false;
    }

    private void renderAvailabilityForDay(String dayName) {
        if (llPeriodSlotsList == null) return;
        llPeriodSlotsList.removeAllViews();

        Map<Integer, ClassSession> periodMap = new HashMap<>();
        for (ClassSession cs : teacherSessions) {
            if (dayName.equalsIgnoreCase(cs.getDayOfWeek())) {
                int pNum = deducePeriod(cs.getStartTime());
                if (pNum >= 1 && pNum <= 8) {
                    periodMap.put(pNum, cs);
                }
            }
        }

        int maxPeriodsForDay = 8;
        if ("Friday".equalsIgnoreCase(dayName)) {
            maxPeriodsForDay = 5; // Cutoff at 12:50 PM
        }

        int occupiedCount = 0;
        int freeCount = 0;

        for (int p = 1; p <= maxPeriodsForDay; p++) {
            if (p == 2) {
                // Add Tea Break banner
                addBreakBanner("☕ TEA BREAK (09:00 AM - 09:20 AM • 20 MIN)", 0xFFF5F5F5, 0xFF546E7A);
            }
            if (p == 6) {
                // Add Lunch Break banner
                addBreakBanner("🍽️ LUNCH BREAK (12:50 PM - 01:40 PM • 50 MIN)", 0xFFF5F5F5, 0xFF546E7A);
            }

            ClassSession cs = periodMap.get(p);
            String timeStr = (p <= periodTimes.length) ? periodTimes[p - 1] : "Period " + p;

            LinearLayout slotCard = new LinearLayout(this);
            slotCard.setOrientation(LinearLayout.VERTICAL);
            slotCard.setPadding(16, 12, 16, 12);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, 8);
            slotCard.setLayoutParams(lp);

            TextView tvPeriodHeader = new TextView(this);
            tvPeriodHeader.setText("Period " + p + "  •  " + timeStr);
            tvPeriodHeader.setTextSize(13f);
            tvPeriodHeader.setTypeface(null, Typeface.BOLD);

            TextView tvSlotStatus = new TextView(this);
            tvSlotStatus.setTextSize(12f);

            if (cs != null) {
                occupiedCount++;
                slotCard.setBackgroundColor(0xFFEDE7F6); // Soft purple
                tvPeriodHeader.setTextColor(0xFF4A148C);

                String subNotice = cs.getSubstituteTeacherName() != null ? " [SUBSTITUTE: " + cs.getSubstituteTeacherName() + "]" : "";
                tvSlotStatus.setText("⛔ OCCUPIED: " + cs.getSubjectName() + " (Sem " + cs.getSemester() + "-" + (cs.getSection() != null ? cs.getSection() : "A") + ")\n"
                        + "Venue: " + (cs.getRoomNumber() != null ? cs.getRoomNumber() : "Assigned Hall")
                        + (cs.isLab() ? " [LAB]" : " [THEORY]") + subNotice);
                tvSlotStatus.setTextColor(0xFF311B92);
            } else {
                freeCount++;
                slotCard.setBackgroundColor(0xFFE8F5E9); // Soft green
                tvPeriodHeader.setTextColor(0xFF1B5E20);
                tvSlotStatus.setText("✅ FREE / AVAILABLE FOR CONSULTATION OR SUBSTITUTION");
                tvSlotStatus.setTextColor(0xFF2E7D32);
            }

            slotCard.addView(tvPeriodHeader);
            slotCard.addView(tvSlotStatus);
            llPeriodSlotsList.addView(slotCard);
        }

        if ("Friday".equalsIgnoreCase(dayName)) {
            addBreakBanner("🕌 FRIDAY AFTERNOON RECESS / PRAYER (Timetable concludes at 12:50 PM)", 0xFFFFFDE7, 0xFF795548);
        } else if ("Saturday".equalsIgnoreCase(dayName)) {
            addBreakBanner("📅 SATURDAY ROTATION: 2nd Sat Mon Order • 4th Sat Tue Order • 1st/3rd Holiday", 0xFFF1F8E9, 0xFF33691E);
        }

        tvCountFreePeriods.setText(String.valueOf(freeCount));
        tvCountOccupiedPeriods.setText(String.valueOf(occupiedCount));
        tvAvailabilitySubtitle.setText(dayName.toUpperCase(Locale.ROOT) + " Period Availability (" + freeCount + " Free, " + occupiedCount + " Occupied):");
    }

    private void addBreakBanner(String text, int bgColor, int textColor) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setBackgroundColor(bgColor);
        tv.setTextColor(textColor);
        tv.setTextSize(11.5f);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setPadding(16, 8, 16, 8);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 8);
        tv.setLayoutParams(lp);
        llPeriodSlotsList.addView(tv);
    }

    private int deducePeriod(String startTime) {
        if (startTime == null || startTime.trim().isEmpty()) return 1;
        int m = UIUtils.parseTimeToMinutes(startTime);
        if (m < 530) return 1;       // ~08:20 AM
        else if (m < 600) return 2;  // ~09:10 AM
        else if (m < 660) return 3;  // ~10:20 AM
        else if (m < 710) return 4;  // ~11:10 AM
        else if (m < 770) return 5;  // ~12:00 PM
        else if (m < 850) return 6;  // ~01:40 PM
        else if (m < 900) return 7;  // ~02:30 PM
        else return 8;               // ~03:20 PM
    }
}
