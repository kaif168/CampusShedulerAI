package com.campusscheduler.ai.models;

import java.io.Serializable;

public class ClassSession implements Serializable {

    private String sessionId;
    private String classAssignmentId;
    private String collegeId;
    private String departmentId;

    private String subjectId;
    private String subjectCode;
    private String subjectName;

    private String facultyId;
    private String teacherUid;
    private String facultyName;
    private String teacherName;
    private String facultyDesignation;

    private String dayOfWeek;
    private String date;
    private String startTime;
    private String endTime;

    private int semester;

    private String section;
    private String sectionId;
    private String sectionName;

    private String classroomId;
    private String roomNumber;

    private String classType;
    private boolean isLab;

    private String status;
    private String substituteTeacherName;

    public ClassSession() {
    }

    public ClassSession(
            String sessionId,
            String classAssignmentId,
            String subjectId,
            String subjectCode,
            String subjectName,
            String facultyId,
            String facultyName,
            int semester,
            String section,
            boolean isLab
    ) {
        this.sessionId = sessionId;
        this.classAssignmentId = classAssignmentId;
        this.subjectId = subjectId;
        this.subjectCode = subjectCode;
        this.subjectName = subjectName;

        this.facultyId = facultyId;
        this.teacherUid = facultyId;

        this.facultyName = facultyName;
        this.teacherName = facultyName;

        this.semester = semester;

        this.section = section;
        this.sectionName = section;

        this.isLab = isLab;
        this.classType = isLab ? "Lab" : "Theory";
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getClassAssignmentId() {
        return classAssignmentId;
    }

    public void setClassAssignmentId(String classAssignmentId) {
        this.classAssignmentId = classAssignmentId;
    }

    public String getCollegeId() {
        return collegeId;
    }

    public void setCollegeId(String collegeId) {
        this.collegeId = collegeId;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public String getFacultyId() {
        return facultyId != null ? facultyId : teacherUid;
    }

    public void setFacultyId(String facultyId) {
        this.facultyId = facultyId;
        this.teacherUid = facultyId;
    }

    public String getTeacherUid() {
        return teacherUid != null ? teacherUid : facultyId;
    }

    public void setTeacherUid(String teacherUid) {
        this.teacherUid = teacherUid;
        this.facultyId = teacherUid;
    }

    public String getFacultyName() {
        return facultyName != null ? facultyName : teacherName;
    }

    public void setFacultyName(String facultyName) {
        this.facultyName = facultyName;
        this.teacherName = facultyName;
    }

    public String getTeacherName() {
        return teacherName != null ? teacherName : facultyName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
        this.facultyName = teacherName;
    }

    public String getFacultyDesignation() {
        return facultyDesignation;
    }

    public void setFacultyDesignation(String facultyDesignation) {
        this.facultyDesignation = facultyDesignation;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public int getSemester() {
        return semester;
    }

    public void setSemester(int semester) {
        this.semester = semester;
    }

    public String getSection() {
        return section != null ? section : sectionName;
    }

    public void setSection(String section) {
        this.section = section;
        this.sectionName = section;
    }

    public String getSectionId() {
        return sectionId != null ? sectionId : section;
    }

    public void setSectionId(String sectionId) {
        this.sectionId = sectionId;
    }

    public String getSectionName() {
        return sectionName != null ? sectionName : section;
    }

    public void setSectionName(String sectionName) {
        this.sectionName = sectionName;
        this.section = sectionName;
    }

    public String getClassroomId() {
        return classroomId;
    }

    public void setClassroomId(String classroomId) {
        this.classroomId = classroomId;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getRoomId() {
        return roomNumber != null ? roomNumber : classroomId;
    }

    public void setRoomId(String roomId) {
        this.roomNumber = roomId;
        this.classroomId = roomId;
    }

    public String getClassType() {
        return classType != null
                ? classType
                : (isLab ? "Lab" : "Theory");
    }

    public void setClassType(String classType) {
        this.classType = classType;
    }

    public boolean isLab() {
        return isLab;
    }

    public void setLab(boolean lab) {
        isLab = lab;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSubstituteTeacherName() {
        return substituteTeacherName;
    }

    public void setSubstituteTeacherName(String substituteTeacherName) {
        this.substituteTeacherName = substituteTeacherName;
    }
}