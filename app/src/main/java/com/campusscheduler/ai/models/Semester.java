package com.campusscheduler.ai.models;

import java.io.Serializable;

public class Semester implements Serializable {
    private String id;
    private String collegeId;
    private String departmentId;
    private int semesterNumber;
    private String semesterName;
    private String academicYear;

    public Semester() {}

    public Semester(String id, String semesterName, int semesterNumber) {
        this.id = id;
        this.semesterName = semesterName;
        this.semesterNumber = semesterNumber;
    }

    public Semester(String id, String collegeId, String departmentId, int semesterNumber, String academicYear) {
        this.id = id;
        this.collegeId = collegeId;
        this.departmentId = departmentId;
        this.semesterNumber = semesterNumber;
        this.semesterName = "Semester " + semesterNumber;
        this.academicYear = academicYear;
    }

    public static String formatSemesterOrdinal(int semNumber) {
        switch (semNumber) {
            case 1: return "1st Semester";
            case 2: return "2nd Semester";
            case 3: return "3rd Semester";
            case 4: return "4th Semester";
            case 5: return "5th Semester";
            case 6: return "6th Semester";
            case 7: return "7th Semester";
            case 8: return "8th Semester";
            default: return semNumber + "th Semester";
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCollegeId() { return collegeId; }
    public void setCollegeId(String collegeId) { this.collegeId = collegeId; }

    public String getDepartmentId() { return departmentId; }
    public void setDepartmentId(String departmentId) { this.departmentId = departmentId; }

    public int getSemesterNumber() { return semesterNumber; }
    public void setSemesterNumber(int semesterNumber) { this.semesterNumber = semesterNumber; }

    public String getSemesterName() {
        if (semesterName != null && !semesterName.trim().isEmpty()) {
            String lower = semesterName.toLowerCase();
            if (lower.contains("1th")) return "1st Semester";
            if (lower.contains("2th")) return "2nd Semester";
            if (lower.contains("3th")) return "3rd Semester";
            return semesterName;
        }
        return formatSemesterOrdinal(semesterNumber > 0 ? semesterNumber : 1);
    }
    public void setSemesterName(String semesterName) { this.semesterName = semesterName; }

    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }
}
