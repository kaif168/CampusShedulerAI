package com.campusscheduler.ai.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.campusscheduler.ai.R;
import com.campusscheduler.ai.adapters.GenericAdapter;
import com.campusscheduler.ai.models.ReminderItem;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.utils.FirebaseHelper;
import com.campusscheduler.ai.utils.LocalAuthManager;
import com.campusscheduler.ai.utils.UIUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class ReminderActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private FloatingActionButton fabAddReminder;
    private RecyclerView rvReminders;
    private GenericAdapter<ReminderItem> adapter;
    private List<ReminderItem> reminderList = new ArrayList<>();
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder);

        currentUser = LocalAuthManager.getCurrentLocalUser(this);

        btnBack = findViewById(R.id.btnBack);
        fabAddReminder = findViewById(R.id.fabAddReminder);
        rvReminders = findViewById(R.id.rvReminders);

        btnBack.setOnClickListener(v -> finish());
        fabAddReminder.setOnClickListener(v -> showAddReminderDialog());

        rvReminders.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GenericAdapter<>(reminderList, (item, holder) -> {
            holder.setText(R.id.tvTitle, item.getTitle() + " (" + item.getDate() + " at " + item.getTime() + ")");
            holder.setText(R.id.tvSubtitle, item.getDescription());
        }, R.layout.item_generic);
        rvReminders.setAdapter(adapter);

        loadReminders();
    }

    private void loadReminders() {
        String uid = currentUser != null ? currentUser.getUid() : null;
        FirebaseHelper.getRemindersRef().whereEqualTo("userUid", uid).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    reminderList.clear();
                    for (var doc : queryDocumentSnapshots) {
                        ReminderItem r = doc.toObject(ReminderItem.class);
                        if (r != null) reminderList.add(r);
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void showAddReminderDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Set Class / Academic Reminder");

        EditText etTitle = new EditText(this);
        etTitle.setHint("Reminder Title");
        EditText etDate = new EditText(this);
        etDate.setHint("Date (YYYY-MM-DD)");
        EditText etTime = new EditText(this);
        etTime.setHint("Time (HH:mm)");

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);
        layout.addView(etTitle);
        layout.addView(etDate);
        layout.addView(etTime);
        builder.setView(layout);

        builder.setPositiveButton("Set Reminder", (dialog, which) -> {
            String title = etTitle.getText().toString().trim();
            String date = etDate.getText().toString().trim();
            String time = etTime.getText().toString().trim();

            if (title.isEmpty()) {
                UIUtils.showToast(this, "Title required.");
                return;
            }

            String id = FirebaseHelper.getRemindersRef().document().getId();
            ReminderItem r = new ReminderItem();
            r.setReminderId(id);
            r.setUserUid(currentUser != null ? currentUser.getUid() : "STUDENT_1");
            r.setTitle(title);
            r.setDate(date.isEmpty() ? "Today" : date);
            r.setTime(time.isEmpty() ? "08:20 AM" : time);
            r.setDescription("Scheduled Academic Alert");

            FirebaseHelper.getRemindersRef().document(id).set(r)
                    .addOnSuccessListener(aVoid -> {
                        UIUtils.showToast(this, "Reminder created!");
                        loadReminders();
                    })
                    .addOnFailureListener(e -> {
                        reminderList.add(r);
                        adapter.notifyDataSetChanged();
                    });
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}
