package com.campusscheduler.ai.models;

import java.io.Serializable;

public class Assignment implements Serializable {
    private String id;
    private String assignmentId;
    private String collegeId;
    private String departmentId;
    private String teacherUid;
    private String facultyId;
    private String teacherName;
    private String subjectId;
    private String subjectName;
    private String semesterId;
    private String semesterName;
    private String sectionId;
    private String sectionName;
    private String title;
    private String description;
    private String startDate;
    private long dueDate;
    private String dueDateStr;
    private int maxMarks;
    private String assignmentType;
    private String submissionType;
    private String attachmentUrl;
    private String status; // DRAFT, PUBLISHED, ONGOING, CLOSED
    private String targetAudience; // ALL, SECTION, STUDENT
    private String targetValue;
    private String submissionInstructions;
    private boolean allowLateSubmission;
    private long createdAt;

    public Assignment() {
        this.createdAt = System.currentTimeMillis();
        this.status = "PUBLISHED";
    }

    public Assignment(String id, String collegeId, String departmentId, String subjectId, String facultyId, String title, String description, long dueDate, int maxMarks) {
        this();
        this.id = id;
        this.assignmentId = id;
        this.collegeId = collegeId;
        this.departmentId = departmentId;
        this.subjectId = subjectId;
        this.facultyId = facultyId;
        this.teacherUid = facultyId;
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.maxMarks = maxMarks;
    }

    public String getId() { return id != null ? id : assignmentId; }
    public void setId(String id) { this.id = id; this.assignmentId = id; }

    public String getAssignmentId() { return assignmentId != null ? assignmentId : id; }
    public void setAssignmentId(String assignmentId) { this.assignmentId = assignmentId; this.id = assignmentId; }

    public String getCollegeId() { return collegeId; }
    public void setCollegeId(String collegeId) { this.collegeId = collegeId; }

    public String getDepartmentId() { return departmentId; }
    public void setDepartmentId(String departmentId) { this.departmentId = departmentId; }

    public String getTeacherUid() { return teacherUid != null ? teacherUid : facultyId; }
    public void setTeacherUid(String teacherUid) { this.teacherUid = teacherUid; this.facultyId = teacherUid; }

    public String getFacultyId() { return facultyId != null ? facultyId : teacherUid; }
    public void setFacultyId(String facultyId) { this.facultyId = facultyId; this.teacherUid = facultyId; }

    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }

    public String getSubjectId() { return subjectId; }
    public void setSubjectId(String subjectId) { this.subjectId = subjectId; }

    public String getSubjectName() { return subjectName != null ? subjectName : "Subject"; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public String getSemesterId() { return semesterId; }
    public void setSemesterId(String semesterId) { this.semesterId = semesterId; }

    public String getSemesterName() { return semesterName; }
    public void setSemesterName(String semesterName) { this.semesterName = semesterName; }

    public String getSectionId() { return sectionId; }
    public void setSectionId(String sectionId) { this.sectionId = sectionId; }

    public String getSectionName() { return sectionName; }
    public void setSectionName(String sectionName) { this.sectionName = sectionName; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public long getDueDate() { return dueDate; }
    public void setDueDate(long dueDate) { this.dueDate = dueDate; }

    public String getDueDateStr() { return dueDateStr != null ? dueDateStr : String.valueOf(dueDate); }
    public void setDueDateStr(String dueDateStr) { this.dueDateStr = dueDateStr; }

    public int getMaxMarks() { return maxMarks; }
    public void setMaxMarks(int maxMarks) { this.maxMarks = maxMarks; }

    public String getAssignmentType() { return assignmentType; }
    public void setAssignmentType(String assignmentType) { this.assignmentType = assignmentType; }

    public String getSubmissionType() { return submissionType; }
    public void setSubmissionType(String submissionType) { this.submissionType = submissionType; }

    public String getAttachmentUrl() { return attachmentUrl; }
    public void setAttachmentUrl(String attachmentUrl) { this.attachmentUrl = attachmentUrl; }

    public String getStatus() { return status != null ? status : "PUBLISHED"; }
    public void setStatus(String status) { this.status = status; }

    public boolean isAllowLateSubmission() { return allowLateSubmission; }
    public void setAllowLateSubmission(boolean allowLateSubmission) { this.allowLateSubmission = allowLateSubmission; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public String getTargetAudience() { return targetAudience; }
    public void setTargetAudience(String targetAudience) { this.targetAudience = targetAudience; }

    public String getTargetValue() { return targetValue; }
    public void setTargetValue(String targetValue) { this.targetValue = targetValue; }

    public String getSubmissionInstructions() { return submissionInstructions; }
    public void setSubmissionInstructions(String submissionInstructions) { this.submissionInstructions = submissionInstructions; }
}
