package com.campusscheduler.ai.models;

import java.io.Serializable;

public class LeaveRequest implements Serializable {
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";

    private String id;
    private String leaveId;
    private String requestId;
    private String collegeId;
    private String applicantId;
    private String applicantUid;
    private String applicantName;
    private String applicantRole;
    private String startDate;
    private String endDate;
    private String reason;
    private String status;
    private String substitutedFacultyId;
    private String substitutedFacultyName;
    private long createdAt;

    public LeaveRequest() {}

    public LeaveRequest(String id, String collegeId, String applicantId, String applicantName, String applicantRole, String startDate, String endDate, String reason) {
        this.id = id;
        this.leaveId = id;
        this.requestId = id;
        this.collegeId = collegeId;
        this.applicantId = applicantId;
        this.applicantUid = applicantId;
        this.applicantName = applicantName;
        this.applicantRole = applicantRole;
        this.startDate = startDate;
        this.endDate = endDate;
        this.reason = reason;
        this.status = STATUS_PENDING;
        this.createdAt = System.currentTimeMillis();
    }

    public String getId() { return id != null ? id : (leaveId != null ? leaveId : requestId); }
    public void setId(String id) { this.id = id; this.leaveId = id; this.requestId = id; }

    public String getLeaveId() { return leaveId != null ? leaveId : id; }
    public void setLeaveId(String leaveId) { this.leaveId = leaveId; this.id = leaveId; this.requestId = leaveId; }

    public String getRequestId() { return requestId != null ? requestId : id; }
    public void setRequestId(String requestId) { this.requestId = requestId; this.id = requestId; this.leaveId = requestId; }

    public String getCollegeId() { return collegeId; }
    public void setCollegeId(String collegeId) { this.collegeId = collegeId; }

    public String getApplicantId() { return applicantId != null ? applicantId : applicantUid; }
    public void setApplicantId(String applicantId) { this.applicantId = applicantId; this.applicantUid = applicantId; }

    public String getApplicantUid() { return applicantUid != null ? applicantUid : applicantId; }
    public void setApplicantUid(String applicantUid) { this.applicantUid = applicantUid; this.applicantId = applicantUid; }

    public String getApplicantName() { return applicantName; }
    public void setApplicantName(String applicantName) { this.applicantName = applicantName; }

    public String getApplicantRole() { return applicantRole; }
    public void setApplicantRole(String applicantRole) { this.applicantRole = applicantRole; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSubstitutedFacultyId() { return substitutedFacultyId; }
    public void setSubstitutedFacultyId(String substitutedFacultyId) { this.substitutedFacultyId = substitutedFacultyId; }

    public String getSubstitutedFacultyName() { return substitutedFacultyName; }
    public void setSubstitutedFacultyName(String substitutedFacultyName) { this.substitutedFacultyName = substitutedFacultyName; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
