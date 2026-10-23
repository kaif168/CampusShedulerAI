package com.campusscheduler.ai.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.campusscheduler.ai.R;
import com.campusscheduler.ai.adapters.GenericAdapter;
import com.campusscheduler.ai.models.NotificationItem;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.utils.FirebaseHelper;
import com.campusscheduler.ai.utils.LocalAuthManager;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class StudentNotificationActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private MaterialButton btnClearAll;
    private ProgressBar progressBar;
    private TextView tvEmptyState, tvNotificationTitle;
    private RecyclerView rvNotifications;
    private GenericAdapter<NotificationItem> adapter;
    private List<NotificationItem> notificationList = new ArrayList<>();
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_notification);

        currentUser = LocalAuthManager.getCurrentLocalUser(this);

        btnBack = findViewById(R.id.btnBack);
        btnClearAll = findViewById(R.id.btnClearAll);
        progressBar = findViewById(R.id.progressBar);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        tvNotificationTitle = findViewById(R.id.tvNotificationTitle);
        rvNotifications = findViewById(R.id.rvNotifications);

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        if (btnClearAll != null) {
            btnClearAll.setOnClickListener(v -> confirmClearAllNotifications());
        }

        boolean isTeacher = currentUser != null && (User.ROLE_TEACHER.equalsIgnoreCase(currentUser.getRole()) || "Faculty".equalsIgnoreCase(currentUser.getRole()));
        if (tvNotificationTitle != null) {
            tvNotificationTitle.setText(isTeacher ? "Teacher Notification Center" : "Student Notification Center");
        }

        if (rvNotifications != null) {
            rvNotifications.setLayoutManager(new LinearLayoutManager(this));
            adapter = new GenericAdapter<>(notificationList, (item, holder) -> {
                holder.setText(R.id.tvTitle, item.getTitle() != null ? item.getTitle() : "Academic Notification");
                holder.setText(R.id.tvSubtitle, (item.getMessage() != null ? item.getMessage() : "") +
                        "\n" + (item.getTimestamp() != null ? item.getTimestamp() : ""));
            }, R.layout.item_generic);
            rvNotifications.setAdapter(adapter);
        }

        loadNotifications();
    }

    private void confirmClearAllNotifications() {
        new AlertDialog.Builder(this)
                .setTitle("Clear Notifications")
                .setMessage("Are you sure you want to clear all notifications? The notification badge will be removed.")
                .setPositiveButton("Clear All", (dialog, which) -> {
                    long now = System.currentTimeMillis();
                    String uid = currentUser != null && currentUser.getUid() != null ? currentUser.getUid() : "default";
                    String email = currentUser != null && currentUser.getEmail() != null ? currentUser.getEmail().trim().toLowerCase() : "";
                    SharedPreferences sp = getSharedPreferences("campus_notifs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putLong("cleared_time_" + uid, now);
                    if (!email.isEmpty()) {
                        editor.putLong("cleared_time_" + email, now);
                    }
                    editor.apply();

                    notificationList.clear();
                    if (adapter != null) adapter.notifyDataSetChanged();
                    if (tvEmptyState != null) {
                        tvEmptyState.setVisibility(View.VISIBLE);
                        tvEmptyState.setText("All notifications have been cleared.");
                    }
                    Toast.makeText(this, "Notifications cleared.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadNotifications() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (tvEmptyState != null) tvEmptyState.setVisibility(View.GONE);

        String collegeId = currentUser != null ? currentUser.getCollegeId() : null;
        boolean isTeacher = currentUser != null && (User.ROLE_TEACHER.equalsIgnoreCase(currentUser.getRole()) || "Faculty".equalsIgnoreCase(currentUser.getRole()));

        String uid = currentUser != null && currentUser.getUid() != null ? currentUser.getUid() : "default";
        String email = currentUser != null && currentUser.getEmail() != null ? currentUser.getEmail().trim().toLowerCase() : "";
        SharedPreferences sp = getSharedPreferences("campus_notifs", MODE_PRIVATE);
        long lastCleared = Math.max(sp.getLong("cleared_time_" + uid, 0),
                !email.isEmpty() ? sp.getLong("cleared_time_" + email, 0) : 0);

        FirebaseHelper.getNotificationsRef()
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    notificationList.clear();

                    if (querySnapshots != null && !querySnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : querySnapshots) {
                            NotificationItem item = doc.toObject(NotificationItem.class);
                            if (item != null) {
                                String rRole = item.getRecipientRole();
                                String cId = item.getCollegeId();

                                boolean isForUser = rRole == null || rRole.equalsIgnoreCase("All")
                                        || (isTeacher && (rRole.equalsIgnoreCase("Teacher") || rRole.equalsIgnoreCase("Faculty")))
                                        || (!isTeacher && rRole.equalsIgnoreCase("Student"));
                                boolean matchesCollege = collegeId == null || cId == null || cId.isEmpty() || cId.equalsIgnoreCase(collegeId);

                                if (isForUser && matchesCollege) {
                                    long ts = item.getCreatedAt();
                                    if (ts <= 0 && item.getTimestamp() != null && item.getTimestamp().matches("\\d+")) {
                                        try {
                                            ts = Long.parseLong(item.getTimestamp());
                                        } catch (Exception ignored) {}
                                    }
                                    if (ts > lastCleared) {
                                        notificationList.add(item);
                                    }
                                }
                            }
                        }
                    }

                    if (notificationList.isEmpty()) {
                        if (tvEmptyState != null) {
                            tvEmptyState.setVisibility(View.VISIBLE);
                            tvEmptyState.setText("No notifications available.");
                        }
                    } else {
                        if (tvEmptyState != null) tvEmptyState.setVisibility(View.GONE);
                    }

                    if (adapter != null) adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (tvEmptyState != null) {
                        tvEmptyState.setVisibility(View.VISIBLE);
                        tvEmptyState.setText("No notifications available.");
                    }
                    notificationList.clear();
                    if (adapter != null) adapter.notifyDataSetChanged();
                });
    }
}
