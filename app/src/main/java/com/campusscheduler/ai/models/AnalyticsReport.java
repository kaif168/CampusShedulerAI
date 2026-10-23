package com.campusscheduler.ai.models;

import java.io.Serializable;

public class AnalyticsReport implements Serializable {
    private String collegeId;
    private int totalStudents;
    private int totalFaculty;
    private int totalClassrooms;
    private double averageAttendancePercentage;
    private double timetableEfficiencyScore;

    public AnalyticsReport() {}

    public AnalyticsReport(String collegeId, int totalStudents, int totalFaculty, int totalClassrooms, double averageAttendancePercentage, double timetableEfficiencyScore) {
        this.collegeId = collegeId;
        this.totalStudents = totalStudents;
        this.totalFaculty = totalFaculty;
        this.totalClassrooms = totalClassrooms;
        this.averageAttendancePercentage = averageAttendancePercentage;
        this.timetableEfficiencyScore = timetableEfficiencyScore;
    }

    public String getCollegeId() { return collegeId; }
    public void setCollegeId(String collegeId) { this.collegeId = collegeId; }

    public int getTotalStudents() { return totalStudents; }
    public void setTotalStudents(int totalStudents) { this.totalStudents = totalStudents; }

    public int getTotalFaculty() { return totalFaculty; }
    public void setTotalFaculty(int totalFaculty) { this.totalFaculty = totalFaculty; }

    public int getTotalClassrooms() { return totalClassrooms; }
    public void setTotalClassrooms(int totalClassrooms) { this.totalClassrooms = totalClassrooms; }

    public double getAverageAttendancePercentage() { return averageAttendancePercentage; }
    public void setAverageAttendancePercentage(double averageAttendancePercentage) { this.averageAttendancePercentage = averageAttendancePercentage; }

    public double getTimetableEfficiencyScore() { return timetableEfficiencyScore; }
    public void setTimetableEfficiencyScore(double timetableEfficiencyScore) { this.timetableEfficiencyScore = timetableEfficiencyScore; }
}
