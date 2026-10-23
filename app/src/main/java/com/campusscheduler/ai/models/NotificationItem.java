package com.campusscheduler.ai.models;

import java.io.Serializable;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;

public class NotificationItem implements Serializable {
    private String id;
    private String title;
    private String message;
    private String type;
    private String collegeId;
    private String departmentId;
    private String recipientRole;
    private String timestamp;
    private long createdAt;

    public NotificationItem() {}

    public NotificationItem(String id, String title, String message, String type, String collegeId, String departmentId, String recipientRole, String timestamp) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.type = type;
        this.collegeId = collegeId;
        this.departmentId = departmentId;
        this.recipientRole = recipientRole;
        this.timestamp = timestamp;
        this.createdAt = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getCollegeId() { return collegeId; }
    public void setCollegeId(String collegeId) { this.collegeId = collegeId; }

    public String getDepartmentId() { return departmentId; }
    public void setDepartmentId(String departmentId) { this.departmentId = departmentId; }

    public String getRecipientRole() { return recipientRole; }
    public void setRecipientRole(String recipientRole) { this.recipientRole = recipientRole; }

    @PropertyName("timestamp")
    public Object getTimestampRaw() { return timestamp; }

    @PropertyName("timestamp")
    public void setTimestamp(Object ts) {
        if (ts instanceof Number) {
            long ms = ((Number) ts).longValue();
            this.timestamp = String.valueOf(ms);
            this.createdAt = ms;
        } else if (ts != null) {
            this.timestamp = ts.toString();
        }
    }

    @Exclude
    public String getTimestamp() { return timestamp != null ? timestamp : "Just now"; }

    @Exclude
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    @Exclude
    public void setTimestamp(long timestampMs) { 
        this.timestamp = String.valueOf(timestampMs); 
        this.createdAt = timestampMs; 
    }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
