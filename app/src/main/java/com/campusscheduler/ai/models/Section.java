package com.campusscheduler.ai.models;

import java.io.Serializable;

public class Section implements Serializable {
    private String id;
    private String collegeId;
    private String departmentId;
    private String departmentName;
    private int semester;
    private int semesterNumber;
    private String semesterId;
    private String academicYear;
    private String sectionName;
    private int studentCount;

    public Section() {}

    public Section(String id, String sectionName, int studentCount) {
        this.id = id;
        this.sectionName = sectionName;
        this.studentCount = studentCount;
    }

    public Section(String id, String collegeId, String departmentId, int semester, String sectionName, int studentCount) {
        this.id = id;
        this.collegeId = collegeId;
        this.departmentId = departmentId;
        this.semester = semester;
        this.semesterNumber = semester;
        this.sectionName = sectionName;
        this.studentCount = studentCount;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCollegeId() { return collegeId; }
    public void setCollegeId(String collegeId) { this.collegeId = collegeId; }

    public String getDepartmentId() { return departmentId; }
    public void setDepartmentId(String departmentId) { this.departmentId = departmentId; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public int getSemester() { return semester > 0 ? semester : semesterNumber; }
    public void setSemester(int semester) {
        this.semester = semester;
        this.semesterNumber = semester;
    }

    public int getSemesterNumber() { return semesterNumber > 0 ? semesterNumber : semester; }
    public void setSemesterNumber(int semesterNumber) {
        this.semesterNumber = semesterNumber;
        this.semester = semesterNumber;
    }

    public String getSemesterId() { return semesterId; }
    public void setSemesterId(String semesterId) { this.semesterId = semesterId; }

    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }

    public String getSectionName() { return sectionName; }
    public void setSectionName(String sectionName) { this.sectionName = sectionName; }

    public int getStudentCount() { return studentCount; }
    public void setStudentCount(int studentCount) { this.studentCount = studentCount; }

    public int getCapacity() { return studentCount > 0 ? studentCount : 60; }
    public void setCapacity(int capacity) { this.studentCount = capacity; }
}
