package com.campusscheduler.ai.models;

import java.io.Serializable;

public class ProfileChangeRequest implements Serializable {
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";

    private String id;
    private String collegeId;
    private String userUid;
    private String userName;
    private String userRole; // TEACHER or STUDENT
    private String fieldName; // Display Name, Phone Number, Semester, Section, Profile Photo
    private String previousValue;
    private String requestedValue;
    private String reason;
    private String status; // PENDING, APPROVED, REJECTED
    private String adminDecisionNotes;
    private long createdAt;
    private long reviewedAt;

    public ProfileChangeRequest() {
        this.createdAt = System.currentTimeMillis();
        this.status = STATUS_PENDING;
    }

    public ProfileChangeRequest(String id, String collegeId, String userUid, String userName, String userRole,
                                String fieldName, String previousValue, String requestedValue, String reason) {
        this();
        this.id = id;
        this.collegeId = collegeId;
        this.userUid = userUid;
        this.userName = userName;
        this.userRole = userRole;
        this.fieldName = fieldName;
        this.previousValue = previousValue;
        this.requestedValue = requestedValue;
        this.reason = reason;
        this.status = STATUS_PENDING;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCollegeId() { return collegeId; }
    public void setCollegeId(String collegeId) { this.collegeId = collegeId; }

    public String getUserUid() { return userUid; }
    public void setUserUid(String userUid) { this.userUid = userUid; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }

    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }

    public String getPreviousValue() { return previousValue; }
    public void setPreviousValue(String previousValue) { this.previousValue = previousValue; }

    public String getRequestedValue() { return requestedValue; }
    public void setRequestedValue(String requestedValue) { this.requestedValue = requestedValue; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAdminDecisionNotes() { return adminDecisionNotes; }
    public void setAdminDecisionNotes(String adminDecisionNotes) { this.adminDecisionNotes = adminDecisionNotes; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(long reviewedAt) { this.reviewedAt = reviewedAt; }
}
