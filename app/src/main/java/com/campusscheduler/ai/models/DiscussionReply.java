package com.campusscheduler.ai.models;

import java.io.Serializable;

public class DiscussionReply implements Serializable {
    private String id;
    private String authorId;
    private String authorName;
    private String authorRole;
    private String message;
    private long createdAt;

    public DiscussionReply() {}

    public DiscussionReply(String id, String authorId, String authorName, String authorRole, String message) {
        this.id = id;
        this.authorId = authorId;
        this.authorName = authorName;
        this.authorRole = authorRole;
        this.message = message;
        this.createdAt = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getAuthorRole() { return authorRole; }
    public void setAuthorRole(String authorRole) { this.authorRole = authorRole; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
