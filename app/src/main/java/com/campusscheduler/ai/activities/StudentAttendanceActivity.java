package com.campusscheduler.ai.activities;

import android.app.Dialog;
import android.os.Bundle;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.campusscheduler.ai.R;
import com.campusscheduler.ai.adapters.StudentAttendanceAdapter;
import com.campusscheduler.ai.models.AttendanceRecord;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.utils.FirebaseHelper;
import com.campusscheduler.ai.utils.LocalAuthManager;
import com.campusscheduler.ai.utils.UIUtils;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentAttendanceActivity extends AppCompatActivity {

    private ImageButton btnAttendanceBack;
    private TextView tvOverallPercent, tvOverallSessions;
    private RecyclerView rvSubjectAttendance;
    private List<StudentAttendanceAdapter.AttendanceItem> attendanceList = new ArrayList<>();
    private StudentAttendanceAdapter adapter;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_attendance);

        currentUser = LocalAuthManager.getCurrentLocalUser(this);

        btnAttendanceBack = findViewById(R.id.btnAttendanceBack);
        tvOverallPercent = findViewById(R.id.tvOverallPercent);
        tvOverallSessions = findViewById(R.id.tvOverallSessions);
        rvSubjectAttendance = findViewById(R.id.rvSubjectAttendance);

        if (btnAttendanceBack != null) {
            btnAttendanceBack.setOnClickListener(v -> finish());
        }

        if (rvSubjectAttendance != null) {
            rvSubjectAttendance.setLayoutManager(new LinearLayoutManager(this));
            adapter = new StudentAttendanceAdapter(attendanceList, this::showBreakdownDialog);
            rvSubjectAttendance.setAdapter(adapter);
        }

        loadStudentAttendanceFromFirestore();
    }

    private void loadStudentAttendanceFromFirestore() {
        String uid = currentUser != null ? currentUser.getUid() : null;
        if (uid == null || uid.isEmpty()) {
            uid = getIntent().getStringExtra("studentUid");
        }
        if (uid == null && FirebaseHelper.getAuth().getCurrentUser() != null) {
            uid = FirebaseHelper.getAuth().getCurrentUser().getUid();
        }

        if (uid == null) {
            showEmptyAttendanceState();
            return;
        }

        final String studentUid = uid;
        FirebaseHelper.getAttendanceRecordsRef().whereEqualTo("studentId", studentUid).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    attendanceList.clear();
                    if (queryDocumentSnapshots == null || queryDocumentSnapshots.isEmpty()) {
                        FirebaseHelper.getAttendanceRecordsRef().whereEqualTo("studentUid", studentUid).get()
                                .addOnSuccessListener(secondarySnaps -> {
                                    if (secondarySnaps != null && !secondarySnaps.isEmpty()) {
                                        processAttendanceSnapshots(secondarySnaps.getDocuments());
                                    } else {
                                        showEmptyAttendanceState();
                                    }
                                });
                        return;
                    }
                    processAttendanceSnapshots(queryDocumentSnapshots.getDocuments());
                })
                .addOnFailureListener(e -> showEmptyAttendanceState());
    }

    private void processAttendanceSnapshots(List<DocumentSnapshot> docs) {
        attendanceList.clear();
        Map<String, int[]> subjectMap = new HashMap<>(); // subject -> [attended, total]

        for (DocumentSnapshot doc : docs) {
            AttendanceRecord rec = doc.toObject(AttendanceRecord.class);
            if (rec != null) {
                String sub = rec.getSubjectName() != null ? rec.getSubjectName() : "General Coursework";
                if (!subjectMap.containsKey(sub)) {
                    subjectMap.put(sub, new int[]{0, 0});
                }
                int[] stats = subjectMap.get(sub);
                stats[1]++; // total
                if (rec.isPresent()) stats[0]++; // attended
            }
        }

        int grandAttended = 0;
        int grandTotal = 0;

        for (Map.Entry<String, int[]> entry : subjectMap.entrySet()) {
            String subName = entry.getKey();
            int attended = entry.getValue()[0];
            int total = entry.getValue()[1];
            double pct = total > 0 ? (attended * 100.0) / total : 100.0;
            grandAttended += attended;
            grandTotal += total;

            attendanceList.add(new StudentAttendanceAdapter.AttendanceItem(subName, pct, attended, total));
        }

        double overallPct = grandTotal > 0 ? (grandAttended * 100.0) / grandTotal : 100.0;

        if (tvOverallPercent != null) {
            tvOverallPercent.setText(String.format("%.2f%%", overallPct));
            if (overallPct < 75.0) {
                tvOverallPercent.setTextColor(0xFFC62828); // Red alert for shortage
            } else {
                tvOverallPercent.setTextColor(0xFF2E7D32); // Green good standing
            }
        }

        if (tvOverallSessions != null) {
            String summary = grandAttended + " of " + grandTotal + " sessions attended";
            if (overallPct < 75.0) {
                int required = calculateRequiredSessionsToReach75(grandAttended, grandTotal);
                summary += " | SHORTAGE ALERT (<75%). Attend next " + required + " consecutive classes.";
            }
            tvOverallSessions.setText(summary);
        }

        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private int calculateRequiredSessionsToReach75(int attended, int total) {
        // (attended + x) / (total + x) >= 0.75
        // 4*(attended + x) >= 3*(total + x)
        // 4*attended + 4*x >= 3*total + 3*x -> x >= 3*total - 4*attended
        int req = 3 * total - 4 * attended;
        return Math.max(req, 1);
    }

    private void showEmptyAttendanceState() {
        if (tvOverallPercent != null) {
            tvOverallPercent.setText("N/A");
            tvOverallPercent.setTextColor(0xFF757575);
        }
        if (tvOverallSessions != null) {
            tvOverallSessions.setText("No attendance records available");
        }
        attendanceList.clear();
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void showBreakdownDialog(StudentAttendanceAdapter.AttendanceItem item) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_attendance_breakdown);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        TextView tvTitle = dialog.findViewById(R.id.tvBreakdownSubjectTitle);
        TextView tvPct = dialog.findViewById(R.id.tvBreakdownPercent);
        TextView tvFrac = dialog.findViewById(R.id.tvBreakdownFraction);
        Button btnClose = dialog.findViewById(R.id.btnCloseBreakdown);

        if (tvTitle != null) tvTitle.setText(item.subjectName);
        if (tvPct != null) tvPct.setText(String.format("%.1f%%", item.percentage));
        if (tvFrac != null) {
            int missed = item.total - item.attended;
            tvFrac.setText(item.attended + " of " + item.total + " attended\n" + missed + " missed");
        }

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }
}
