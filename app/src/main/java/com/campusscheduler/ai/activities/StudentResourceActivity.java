package com.campusscheduler.ai.activities;

import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.campusscheduler.ai.R;
import com.campusscheduler.ai.adapters.GenericAdapter;
import com.campusscheduler.ai.models.ResourceItem;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.utils.FirebaseHelper;
import com.campusscheduler.ai.utils.LocalAuthManager;
import com.campusscheduler.ai.utils.UIUtils;

import java.util.ArrayList;
import java.util.List;

public class StudentResourceActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private RecyclerView rvStudentResources;
    private GenericAdapter<ResourceItem> adapter;
    private List<ResourceItem> resourceList = new ArrayList<>();
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_resource);

        currentUser = LocalAuthManager.getCurrentLocalUser(this);

        btnBack = findViewById(R.id.btnBack);
        rvStudentResources = findViewById(R.id.rvStudentResources);

        btnBack.setOnClickListener(v -> finish());

        rvStudentResources.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GenericAdapter<>(resourceList, (item, holder) -> {
            String type = item.getResourceType() != null ? item.getResourceType() : "Document";
            holder.setText(R.id.tvTitle, "[" + type + "] " + item.getTitle());
            String sub = item.getFileName() != null ? "File: " + item.getFileName() : (item.getDescription() != null ? item.getDescription() : "Study Material");
            holder.setText(R.id.tvSubtitle, sub);
            holder.itemView.setOnClickListener(v -> {
                String targetUrl = item.getFileUrl() != null && !item.getFileUrl().trim().isEmpty() ? item.getFileUrl() : item.getUrl();
                if (targetUrl != null && !targetUrl.trim().isEmpty() && !targetUrl.startsWith("https://campus-storage.ref/")) {
                    try {
                        android.content.Intent viewIntent = new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(targetUrl));
                        startActivity(viewIntent);
                    } catch (Exception e) {
                        UIUtils.showToast(StudentResourceActivity.this, "Cannot open resource: " + e.getMessage());
                    }
                } else {
                    UIUtils.showToast(StudentResourceActivity.this, "No valid link attached to this resource.");
                }
            });
        }, R.layout.item_generic);
        rvStudentResources.setAdapter(adapter);

        loadResources();
    }

    private void loadResources() {
        String collegeId = currentUser != null ? currentUser.getCollegeId() : null;
        FirebaseHelper.getResourcesRef().whereEqualTo("collegeId", collegeId).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    resourceList.clear();
                    for (var doc : queryDocumentSnapshots) {
                        ResourceItem r = doc.toObject(ResourceItem.class);
                        if (r != null) resourceList.add(r);
                    }
                    if (resourceList.isEmpty()) {
                        UIUtils.showToast(StudentResourceActivity.this, "No resources available.");
                    }
                    adapter.notifyDataSetChanged();
                });
    }
}
