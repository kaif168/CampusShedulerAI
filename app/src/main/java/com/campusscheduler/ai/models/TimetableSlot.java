package com.campusscheduler.ai.models;

import java.io.Serializable;

public class TimetableSlot implements Serializable {
    private String id;
    private String slotId;
    private String timetableId;
    private String collegeId;
    private String sectionId;
    private String sectionName;
    private String dayOfWeek;
    private String day;
    private int periodIndex;
    private String startTime;
    private String endTime;
    private String subjectId;
    private String subjectCode;
    private String subjectName;
    private String facultyId;
    private String teacherUid;
    private String facultyName;
    private String teacherName;
    private String classroomId;
    private String roomId;
    private String roomNumber;
    private String roomName;
    private String classType;
    private boolean isLab;
    private boolean published;

    public TimetableSlot() {}

    public TimetableSlot(String slotId, String dayOfWeek, int periodIndex, String startTime, String endTime, String subjectId, String subjectCode, String subjectName, String facultyId, String facultyName, String classroomId, String roomNumber, boolean isLab) {
        this.slotId = slotId;
        this.id = slotId;
        this.dayOfWeek = dayOfWeek;
        this.day = dayOfWeek;
        this.periodIndex = periodIndex;
        this.startTime = startTime;
        this.endTime = endTime;
        this.subjectId = subjectId;
        this.subjectCode = subjectCode;
        this.subjectName = subjectName;
        this.facultyId = facultyId;
        this.teacherUid = facultyId;
        this.facultyName = facultyName;
        this.teacherName = facultyName;
        this.classroomId = classroomId;
        this.roomId = classroomId;
        this.roomNumber = roomNumber;
        this.roomName = roomNumber;
        this.isLab = isLab;
        this.classType = isLab ? "Lab" : "Theory";
    }

    public String getId() { return id != null ? id : slotId; }
    public void setId(String id) { this.id = id; this.slotId = id; }

    public String getSlotId() { return slotId != null ? slotId : id; }
    public void setSlotId(String slotId) { this.slotId = slotId; this.id = slotId; }

    public String getTimetableId() { return timetableId; }
    public void setTimetableId(String timetableId) { this.timetableId = timetableId; }

    public String getCollegeId() { return collegeId; }
    public void setCollegeId(String collegeId) { this.collegeId = collegeId; }

    public String getSectionId() { return sectionId; }
    public void setSectionId(String sectionId) { this.sectionId = sectionId; }

    public String getSectionName() { return sectionName; }
    public void setSectionName(String sectionName) { this.sectionName = sectionName; }

    public String getDayOfWeek() { return dayOfWeek != null ? dayOfWeek : day; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; this.day = dayOfWeek; }

    public String getDay() { return day != null ? day : dayOfWeek; }
    public void setDay(String day) { this.day = day; this.dayOfWeek = day; }

    public int getPeriodIndex() { return periodIndex; }
    public void setPeriodIndex(int periodIndex) { this.periodIndex = periodIndex; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getSubjectId() { return subjectId; }
    public void setSubjectId(String subjectId) { this.subjectId = subjectId; }

    public String getSubjectCode() { return subjectCode; }
    public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public String getFacultyId() { return facultyId != null ? facultyId : teacherUid; }
    public void setFacultyId(String facultyId) { this.facultyId = facultyId; this.teacherUid = facultyId; }

    public String getTeacherUid() { return teacherUid != null ? teacherUid : facultyId; }
    public void setTeacherUid(String teacherUid) { this.teacherUid = teacherUid; this.facultyId = teacherUid; }

    public String getFacultyName() { return facultyName != null ? facultyName : teacherName; }
    public void setFacultyName(String facultyName) { this.facultyName = facultyName; this.teacherName = facultyName; }

    public String getTeacherName() { return teacherName != null ? teacherName : facultyName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; this.facultyName = teacherName; }

    public String getClassroomId() { return classroomId != null ? classroomId : roomId; }
    public void setClassroomId(String classroomId) { this.classroomId = classroomId; this.roomId = classroomId; }

    public String getRoomId() { return roomId != null ? roomId : classroomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; this.classroomId = roomId; }

    public String getRoomNumber() { return roomNumber != null ? roomNumber : roomName; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; this.roomName = roomNumber; }

    public String getRoomName() { return roomName != null ? roomName : roomNumber; }
    public void setRoomName(String roomName) { this.roomName = roomName; this.roomNumber = roomName; }

    public String getClassType() { return classType != null ? classType : (isLab ? "Lab" : "Theory"); }
    public void setClassType(String classType) { this.classType = classType; }

    public boolean isLab() { return isLab; }
    public void setLab(boolean lab) { isLab = lab; }

    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }
}
