package com.campusscheduler.ai.models;

import java.io.Serializable;

public class IATimetableItem implements Serializable {
    private String id;
    private String collegeId;
    private String departmentId;
    private int semester;
    private String iaName; // "IA-1", "IA-2", "IA-3"
    private String subjectCode;
    private String subjectName;
    private String date;
    private String timeSlot;
    private String roomNo;

    public IATimetableItem() {}

    public IATimetableItem(String id, String collegeId, String departmentId, int semester, String iaName, String subjectCode, String subjectName, String date, String timeSlot, String roomNo) {
        this.id = id;
        this.collegeId = collegeId;
        this.departmentId = departmentId;
        this.semester = semester;
        this.iaName = iaName;
        this.subjectCode = subjectCode;
        this.subjectName = subjectName;
        this.date = date;
        this.timeSlot = timeSlot;
        this.roomNo = roomNo;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCollegeId() { return collegeId; }
    public void setCollegeId(String collegeId) { this.collegeId = collegeId; }

    public String getDepartmentId() { return departmentId; }
    public void setDepartmentId(String departmentId) { this.departmentId = departmentId; }

    public int getSemester() { return semester; }
    public void setSemester(int semester) { this.semester = semester; }

    public String getIaName() { return iaName; }
    public void setIaName(String iaName) { this.iaName = iaName; }

    public String getSubjectCode() { return subjectCode; }
    public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTimeSlot() { return timeSlot; }
    public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }

    public String getRoomNo() { return roomNo; }
    public void setRoomNo(String roomNo) { this.roomNo = roomNo; }

    public void setIaNumber(String iaNumber) { this.iaName = iaNumber; }
    public void setDayLabel(String dayLabel) { this.date = dayLabel; }
    public void setTimeSlotLabel(String timeSlotLabel) { this.timeSlot = timeSlotLabel; }
}
