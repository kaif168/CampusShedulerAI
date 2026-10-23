package com.campusscheduler.ai.activities;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.campusscheduler.ai.R;
import com.campusscheduler.ai.models.ClassSession;
import com.campusscheduler.ai.models.LeaveRequest;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.utils.FirebaseHelper;
import com.campusscheduler.ai.utils.LocalAuthManager;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class TeacherSubstituteActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvEmptyDelegatedToMe, tvEmptyMyCovered;
    private LinearLayout llDelegatedToMeList, llMyCoveredList;

    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_substitute);

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

        tvEmptyDelegatedToMe = findViewById(R.id.tvEmptyDelegatedToMe);
        tvEmptyMyCovered = findViewById(R.id.tvEmptyMyCovered);
        llDelegatedToMeList = findViewById(R.id.llDelegatedToMeList);
        llMyCoveredList = findViewById(R.id.llMyCoveredList);

        loadSubstitutionData();
    }

    private void loadSubstitutionData() {
        String collegeId = (currentUser != null && currentUser.getCollegeId() != null && !currentUser.getCollegeId().trim().isEmpty())
                ? currentUser.getCollegeId().trim() : "COLLEGE_1789406176044";

        FirebaseHelper.getClassSessionsRef().whereEqualTo("collegeId", collegeId).get()
                .addOnSuccessListener(querySnaps -> {
                    if (llDelegatedToMeList != null) llDelegatedToMeList.removeAllViews();
                    if (llMyCoveredList != null) llMyCoveredList.removeAllViews();

                    List<ClassSession> delegatedToMe = new ArrayList<>();
                    List<ClassSession> myCoveredByOthers = new ArrayList<>();

                    String myUid = currentUser != null ? currentUser.getUid() : "";
                    String myName = currentUser != null && currentUser.getName() != null ? currentUser.getName().trim() : "";

                    if (querySnaps != null) {
                        for (DocumentSnapshot doc : querySnaps) {
                            ClassSession cs = doc.toObject(ClassSession.class);
                            if (cs != null) {
                                String subName = cs.getSubstituteTeacherName();
                                String subId = cs.getFacultyId();
                                String origName = cs.getFacultyName() != null ? cs.getFacultyName() : cs.getTeacherName();
                                String origUid = cs.getFacultyId() != null ? cs.getFacultyId() : cs.getTeacherUid();

                                // 1. Check if assigned as substitute for someone else
                                if (subName != null && !subName.isEmpty()) {
                                    boolean iAmSub = subName.equalsIgnoreCase(myName) || (myUid != null && myUid.equalsIgnoreCase(subId));
                                    if (iAmSub) {
                                        delegatedToMe.add(cs);
                                    }
                                }

                                // 2. Check if my class is covered by someone else
                                boolean isMyOriginalClass = (origUid != null && origUid.equalsIgnoreCase(myUid))
                                        || (origName != null && origName.equalsIgnoreCase(myName));
                                if (isMyOriginalClass && subName != null && !subName.isEmpty() && !subName.equalsIgnoreCase(myName)) {
                                    myCoveredByOthers.add(cs);
                                }
                            }
                        }
                    }

                    // Render Section 1: Delegated to Me
                    if (delegatedToMe.isEmpty()) {
                        if (tvEmptyDelegatedToMe != null) tvEmptyDelegatedToMe.setVisibility(View.VISIBLE);
                    } else {
                        if (tvEmptyDelegatedToMe != null) tvEmptyDelegatedToMe.setVisibility(View.GONE);
                        for (ClassSession cs : delegatedToMe) {
                            View card = createSessionCard(cs, true);
                            if (llDelegatedToMeList != null) llDelegatedToMeList.addView(card);
                        }
                    }

                    // Render Section 2: Covered by Others
                    if (myCoveredByOthers.isEmpty()) {
                        if (tvEmptyMyCovered != null) tvEmptyMyCovered.setVisibility(View.VISIBLE);
                    } else {
                        if (tvEmptyMyCovered != null) tvEmptyMyCovered.setVisibility(View.GONE);
                        for (ClassSession cs : myCoveredByOthers) {
                            View card = createSessionCard(cs, false);
                            if (llMyCoveredList != null) llMyCoveredList.addView(card);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load substitution data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private View createSessionCard(ClassSession cs, boolean iAmSubstitute) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(16, 14, 16, 14);
        card.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 10);
        card.setLayoutParams(lp);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(cs.getSubjectName() != null ? cs.getSubjectName() : "Core Subject");
        tvTitle.setTextSize(14f);
        tvTitle.setTextColor(0xFF0D47A1);
        tvTitle.setTypeface(null, Typeface.BOLD);
        card.addView(tvTitle);

        TextView tvTimeVenue = new TextView(this);
        String day = cs.getDayOfWeek() != null ? cs.getDayOfWeek() : "Scheduled Day";
        String time = (cs.getStartTime() != null ? cs.getStartTime() : "TBD") + " - " + (cs.getEndTime() != null ? cs.getEndTime() : "TBD");
        String venue = cs.getRoomNumber() != null ? cs.getRoomNumber() : "Assigned Hall";
        String cohort = "Sem " + cs.getSemester() + " - Sec " + (cs.getSection() != null ? cs.getSection() : "A");
        tvTimeVenue.setText("📅 " + day + " (" + time + ")  •  " + cohort + "\n📍 Venue: " + venue + (cs.isLab() ? " [LAB]" : " [THEORY]"));
        tvTimeVenue.setTextColor(0xFF37474F);
        tvTimeVenue.setTextSize(12.5f);
        card.addView(tvTimeVenue);

        TextView tvDelegation = new TextView(this);
        if (iAmSubstitute) {
            String orig = cs.getFacultyName() != null ? cs.getFacultyName() : (cs.getTeacherName() != null ? cs.getTeacherName() : "Original Faculty");
            tvDelegation.setText("• Original Teacher: " + orig + "\n• Substitute Assigned: You (Confirmed by Admin)");
            tvDelegation.setTextColor(0xFF1B5E20);
        } else {
            String sub = cs.getSubstituteTeacherName() != null ? cs.getSubstituteTeacherName() : "Substitute Assigned";
            tvDelegation.setText("• Original Teacher: You (On Leave)\n• Substitute Faculty: " + sub + " (Active)");
            tvDelegation.setTextColor(0xFFE65100);
        }
        tvDelegation.setTextSize(12f);
        tvDelegation.setTypeface(null, Typeface.BOLD);
        card.addView(tvDelegation);

        return card;
    }
}
