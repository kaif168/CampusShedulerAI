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
import com.campusscheduler.ai.models.DiscussionPost;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.utils.FirebaseHelper;
import com.campusscheduler.ai.utils.LocalAuthManager;
import com.campusscheduler.ai.utils.UIUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class DiscussionActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private FloatingActionButton fabNewPost;
    private RecyclerView rvDiscussion;
    private GenericAdapter<DiscussionPost> adapter;
    private List<DiscussionPost> postList = new ArrayList<>();
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discussion);

        currentUser = LocalAuthManager.getCurrentLocalUser(this);
        String safeCollegeId = LocalAuthManager.getSafeCollegeId(this, getIntent());
        if (currentUser == null) {
            currentUser = new User();
            currentUser.setCollegeId(safeCollegeId);
        } else if (currentUser.getCollegeId() == null || currentUser.getCollegeId().trim().isEmpty()) {
            currentUser.setCollegeId(safeCollegeId);
        }

        btnBack = findViewById(R.id.btnBack);
        fabNewPost = findViewById(R.id.fabNewPost);
        rvDiscussion = findViewById(R.id.rvDiscussion);

        btnBack.setOnClickListener(v -> finish());
        fabNewPost.setOnClickListener(v -> showCreatePostDialog());

        rvDiscussion.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GenericAdapter<>(postList, (item, holder) -> {
            holder.setText(R.id.tvTitle, item.getTitle() + " (by " + item.getAuthorName() + ")");
            holder.setText(R.id.tvSubtitle, item.getContent() + " | Subject: " + item.getSubjectName());
        }, R.layout.item_generic);
        rvDiscussion.setAdapter(adapter);

        loadPosts();
    }

    private void loadPosts() {
        String collegeId = currentUser != null && currentUser.getCollegeId() != null ?
                currentUser.getCollegeId() : LocalAuthManager.getSafeCollegeId(this, getIntent());
        FirebaseHelper.getDiscussionPostsRef().whereEqualTo("collegeId", collegeId).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    postList.clear();
                    for (var doc : queryDocumentSnapshots) {
                        DiscussionPost p = doc.toObject(DiscussionPost.class);
                        if (p != null) postList.add(p);
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void showCreatePostDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create Discussion Topic");

        EditText etTitle = new EditText(this);
        etTitle.setHint("Topic Title");
        EditText etContent = new EditText(this);
        etContent.setHint("Question / Discussion text");

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);
        layout.addView(etTitle);
        layout.addView(etContent);
        builder.setView(layout);

        builder.setPositiveButton("Post Topic", (dialog, which) -> {
            String title = etTitle.getText().toString().trim();
            String content = etContent.getText().toString().trim();

            if (title.isEmpty() || content.isEmpty()) {
                UIUtils.showToast(this, "Title and content required.");
                return;
            }

            String collegeId = currentUser != null && currentUser.getCollegeId() != null ?
                    currentUser.getCollegeId() : LocalAuthManager.getSafeCollegeId(this, getIntent());
            if (currentUser == null) {
                currentUser = new User();
                currentUser.setCollegeId(collegeId);
                currentUser.setName("Academic User");
                currentUser.setRole("STUDENT");
            } else if (currentUser.getCollegeId() == null || currentUser.getCollegeId().trim().isEmpty()) {
                currentUser.setCollegeId(collegeId);
            }

            String id = FirebaseHelper.getDiscussionPostsRef().document().getId();
            DiscussionPost p = new DiscussionPost();
            p.setPostId(id);
            p.setCollegeId(collegeId);
            p.setAuthorUid(currentUser.getUid() != null ? currentUser.getUid() : "USER_ID");
            p.setAuthorName(currentUser.getName() != null ? currentUser.getName() : "Academic User");
            p.setAuthorRole(currentUser.getRole() != null ? currentUser.getRole() : "STUDENT");
            p.setTitle(title);
            p.setContent(content);
            p.setSubjectName("General Academic");

            FirebaseHelper.getDiscussionPostsRef().document(id).set(p)
                    .addOnSuccessListener(aVoid -> {
                        UIUtils.showToast(this, "Topic posted!");
                        loadPosts();
                    })
                    .addOnFailureListener(e -> {
                        postList.add(p);
                        adapter.notifyDataSetChanged();
                    });
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}
