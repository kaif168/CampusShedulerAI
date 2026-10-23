package com.campusscheduler.ai.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DiscussionPost implements Serializable {
    private String id;
    private String postId;
    private String collegeId;
    private String departmentId;
    private String subjectName;
    private String authorId;
    private String authorUid;
    private String authorName;
    private String authorRole;
    private String title;
    private String content;
    private long createdAt;
    private List<DiscussionReply> replies;

    public DiscussionPost() {
        this.replies = new ArrayList<>();
    }

    public DiscussionPost(String id, String collegeId, String departmentId, String authorId, String authorName, String authorRole, String title, String content) {
        this.id = id;
        this.postId = id;
        this.collegeId = collegeId;
        this.departmentId = departmentId;
        this.authorId = authorId;
        this.authorUid = authorId;
        this.authorName = authorName;
        this.authorRole = authorRole;
        this.title = title;
        this.content = content;
        this.createdAt = System.currentTimeMillis();
        this.replies = new ArrayList<>();
    }

    public String getId() { return id != null ? id : postId; }
    public void setId(String id) { this.id = id; this.postId = id; }

    public String getPostId() { return postId != null ? postId : id; }
    public void setPostId(String postId) { this.postId = postId; this.id = postId; }

    public String getCollegeId() { return collegeId; }
    public void setCollegeId(String collegeId) { this.collegeId = collegeId; }

    public String getDepartmentId() { return departmentId; }
    public void setDepartmentId(String departmentId) { this.departmentId = departmentId; }

    public String getSubjectName() { return subjectName != null ? subjectName : "General Academic Discussion"; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public String getAuthorId() { return authorId != null ? authorId : authorUid; }
    public void setAuthorId(String authorId) { this.authorId = authorId; this.authorUid = authorId; }

    public String getAuthorUid() { return authorUid != null ? authorUid : authorId; }
    public void setAuthorUid(String authorUid) { this.authorUid = authorUid; this.authorId = authorUid; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getAuthorRole() { return authorRole; }
    public void setAuthorRole(String authorRole) { this.authorRole = authorRole; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public List<DiscussionReply> getReplies() { return replies; }
    public void setReplies(List<DiscussionReply> replies) { this.replies = replies; }
}
