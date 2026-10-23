package com.campusscheduler.ai.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Timetable implements Serializable {
    public static final String STATE_PUBLISHED = "PUBLISHED";
    public static final String STATE_DRAFT = "DRAFT";

    private String id;
    private String timetableId;
    private String collegeId;
    private String departmentId;
    private int semester;
    private String section;
    private String academicYear;
    private String collegeName;
    private String collegeAddress;
    private String departmentName;
    private String classTeacherId;
    private String classTeacherName;
    private String effectiveStartDate;
    private String effectiveEndDate;
    private String publishedBy;
    private String title;
    private String status;
    private double fitnessScore;
    private int hardConflictsCount;
    private String coordinatorName;
    private String hodName;
    private String principalName;
    private String rationale;
    private long createdAt;
    private long publishedAt;
    private List<TimetableSlot> slots;

    public Timetable() {
        this.slots = new ArrayList<>();
    }

    public Timetable(String id, String collegeId, String departmentId, int semester, String section, String academicYear, double fitnessScore, String rationale) {
        this.id = id;
        this.timetableId = id;
        this.collegeId = collegeId;
        this.departmentId = departmentId;
        this.semester = semester;
        this.section = section;
        this.academicYear = academicYear;
        this.fitnessScore = fitnessScore;
        this.rationale = rationale;
        this.status = STATE_DRAFT;
        this.title = "Semester " + semester + " (" + section + ") Timetable";
        this.createdAt = System.currentTimeMillis();
        this.slots = new ArrayList<>();
    }

    public String getId() { return id != null ? id : timetableId; }
    public void setId(String id) { this.id = id; this.timetableId = id; }

    public String getTimetableId() { return timetableId != null ? timetableId : id; }
    public void setTimetableId(String timetableId) { this.timetableId = timetableId; this.id = timetableId; }

    public String getCollegeId() { return collegeId; }
    public void setCollegeId(String collegeId) { this.collegeId = collegeId; }

    public String getDepartmentId() { return departmentId; }
    public void setDepartmentId(String departmentId) { this.departmentId = departmentId; }

    public int getSemester() { return semester; }
    public void setSemester(int semester) { this.semester = semester; }

    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }

    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }

    public String getCollegeName() { return collegeName != null ? collegeName : "HKBK College of Engineering"; }
    public void setCollegeName(String collegeName) { this.collegeName = collegeName; }

    public String getCollegeAddress() { return collegeAddress; }
    public void setCollegeAddress(String collegeAddress) { this.collegeAddress = collegeAddress; }

    public String getDepartmentName() { return departmentName != null ? departmentName : "Computer Science & Engineering"; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public String getClassTeacherId() { return classTeacherId; }
    public void setClassTeacherId(String classTeacherId) { this.classTeacherId = classTeacherId; }

    public String getClassTeacherName() { return classTeacherName != null ? classTeacherName : "Assigned Class Teacher"; }
    public void setClassTeacherName(String classTeacherName) { this.classTeacherName = classTeacherName; }

    public String getEffectiveStartDate() { return effectiveStartDate != null ? effectiveStartDate : "2026-08-01"; }
    public void setEffectiveStartDate(String effectiveStartDate) { this.effectiveStartDate = effectiveStartDate; }

    public String getEffectiveEndDate() { return effectiveEndDate != null ? effectiveEndDate : "2026-12-31"; }
    public void setEffectiveEndDate(String effectiveEndDate) { this.effectiveEndDate = effectiveEndDate; }

    public String getPublishedBy() { return publishedBy; }
    public void setPublishedBy(String publishedBy) { this.publishedBy = publishedBy; }

    public String getTitle() { return title != null ? title : ("Semester " + semester + " (" + section + ") Timetable"); }
    public void setTitle(String title) { this.title = title; }

    public String getStatus() { return status != null ? status : STATE_PUBLISHED; }
    public void setStatus(String status) { this.status = status; }

    public double getFitnessScore() { return fitnessScore; }
    public void setFitnessScore(double fitnessScore) { this.fitnessScore = fitnessScore; }

    public int getHardConflictsCount() { return hardConflictsCount; }
    public void setHardConflictsCount(int hardConflictsCount) { this.hardConflictsCount = hardConflictsCount; }

    public String getCoordinatorName() { return coordinatorName != null ? coordinatorName : "Dr. Sivarajan"; }
    public void setCoordinatorName(String coordinatorName) { this.coordinatorName = coordinatorName; }

    public String getHodName() { return hodName != null ? hodName : "Dr. Vilasini M"; }
    public void setHodName(String hodName) { this.hodName = hodName; }

    public String getPrincipalName() { return principalName != null ? principalName : "Dr. Pushpa Mohan"; }
    public void setPrincipalName(String principalName) { this.principalName = principalName; }

    public String getRationale() { return rationale; }
    public void setRationale(String rationale) { this.rationale = rationale; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getPublishedAt() { return publishedAt; }
    public void setPublishedAt(long publishedAt) { this.publishedAt = publishedAt; }

    public List<TimetableSlot> getSlots() { return slots; }
    public void setSlots(List<TimetableSlot> slots) { this.slots = slots; }
}
