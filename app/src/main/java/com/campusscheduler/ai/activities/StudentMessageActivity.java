package com.campusscheduler.ai.activities;

import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.campusscheduler.ai.R;
import com.campusscheduler.ai.adapters.GenericAdapter;
import com.campusscheduler.ai.models.NotificationItem;
import com.campusscheduler.ai.utils.FirebaseHelper;
import com.campusscheduler.ai.utils.UIUtils;

import java.util.ArrayList;
import java.util.List;

public class StudentMessageActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private RecyclerView rvStudentMessages;
    private GenericAdapter<NotificationItem> adapter;
    private List<NotificationItem> messageList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_message);

        btnBack = findViewById(R.id.btnBack);
        rvStudentMessages = findViewById(R.id.rvStudentMessages);

        btnBack.setOnClickListener(v -> finish());

        rvStudentMessages.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GenericAdapter<>(messageList, (item, holder) -> {
            holder.setText(R.id.tvTitle, item.getTitle());
            holder.setText(R.id.tvSubtitle, item.getMessage() + " (" + item.getTimestamp() + ")");
        }, R.layout.item_generic);
        rvStudentMessages.setAdapter(adapter);

        loadMessages();
    }

    private void loadMessages() {
        FirebaseHelper.getNotificationsRef().get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    messageList.clear();
                    for (var doc : queryDocumentSnapshots) {
                        NotificationItem n = doc.toObject(NotificationItem.class);
                        if (n != null) messageList.add(n);
                    }
                    if (messageList.isEmpty()) {
                        UIUtils.showToast(StudentMessageActivity.this, "No messages available.");
                    }
                    adapter.notifyDataSetChanged();
                });
    }
}
