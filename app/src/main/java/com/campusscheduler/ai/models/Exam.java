package com.campusscheduler.ai.models;

import java.io.Serializable;

public class Exam implements Serializable {
    private String id;
    private String collegeId;
    private String departmentId;
    private int semester;
    private String examName;
    private String examType;
    private String subjectCode;
    private String subjectName;
    private String date;
    private String examDate;
    private String startTime;
    private String endTime;
    private String hallNumber;
    private String roomNumber;

    public Exam() {}

    public Exam(String id, String collegeId, String departmentId, int semester, String examName, String subjectCode, String subjectName, String date, String startTime, String endTime, String hallNumber) {
        this.id = id;
        this.collegeId = collegeId;
        this.departmentId = departmentId;
        this.semester = semester;
        this.examName = examName;
        this.examType = examName;
        this.subjectCode = subjectCode;
        this.subjectName = subjectName;
        this.date = date;
        this.examDate = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.hallNumber = hallNumber;
        this.roomNumber = hallNumber;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCollegeId() { return collegeId; }
    public void setCollegeId(String collegeId) { this.collegeId = collegeId; }

    public String getDepartmentId() { return departmentId; }
    public void setDepartmentId(String departmentId) { this.departmentId = departmentId; }

    public int getSemester() { return semester; }
    public void setSemester(int semester) { this.semester = semester; }

    public String getExamName() { return examName != null ? examName : examType; }
    public void setExamName(String examName) { this.examName = examName; this.examType = examName; }

    public String getExamType() { return examType != null ? examType : examName; }
    public void setExamType(String examType) { this.examType = examType; this.examName = examType; }

    public String getSubjectCode() { return subjectCode; }
    public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public String getDate() { return date != null ? date : examDate; }
    public void setDate(String date) { this.date = date; this.examDate = date; }

    public String getExamDate() { return examDate != null ? examDate : date; }
    public void setExamDate(String examDate) { this.examDate = examDate; this.date = examDate; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getHallNumber() { return hallNumber != null ? hallNumber : roomNumber; }
    public void setHallNumber(String hallNumber) { this.hallNumber = hallNumber; this.roomNumber = hallNumber; }

    public String getRoomNumber() { return roomNumber != null ? roomNumber : hallNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; this.hallNumber = roomNumber; }

    private String subjectId;
    private String invigilatorName;

    public void setExamId(String examId) { this.id = examId; }
    public String getSubjectId() { return subjectId; }
    public void setSubjectId(String subjectId) { this.subjectId = subjectId; }
    public String getInvigilatorName() { return invigilatorName; }
    public void setInvigilatorName(String invigilatorName) { this.invigilatorName = invigilatorName; }
}
