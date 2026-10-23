package com.campusscheduler.ai.models;

import java.io.Serializable;

public class AttendanceRecord implements Serializable {
    private String id;
    private String recordId;
    private String collegeId;
    private String departmentId;
    private String subjectId;
    private String subjectName;
    private String facultyId;
    private String teacherUid;
    private String studentId;
    private String studentName;
    private String usn;
    private String date;
    private String slotId;
    private String sessionId;
    private String section;
    private int semester;
    private String teacherName;
    private String studentUsn;
    private boolean present;
    private String status;
    private long timestamp;

    public AttendanceRecord() {}

    public AttendanceRecord(String id, String collegeId, String departmentId, String subjectId, String facultyId, String studentId, String studentName, String usn, String date, String slotId, boolean present) {
        this.id = id;
        this.recordId = id;
        this.collegeId = collegeId;
        this.departmentId = departmentId;
        this.subjectId = subjectId;
        this.facultyId = facultyId;
        this.teacherUid = facultyId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.usn = usn;
        this.date = date;
        this.slotId = slotId;
        this.present = present;
        this.status = present ? "PRESENT" : "ABSENT";
        this.timestamp = System.currentTimeMillis();
    }

    public String getId() { return id != null ? id : recordId; }
    public void setId(String id) { this.id = id; this.recordId = id; }

    public String getRecordId() { return recordId != null ? recordId : id; }
    public void setRecordId(String recordId) { this.recordId = recordId; this.id = recordId; }

    public String getCollegeId() { return collegeId; }
    public void setCollegeId(String collegeId) { this.collegeId = collegeId; }

    public String getDepartmentId() { return departmentId; }
    public void setDepartmentId(String departmentId) { this.departmentId = departmentId; }

    public String getSubjectId() { return subjectId; }
    public void setSubjectId(String subjectId) { this.subjectId = subjectId; }

    public String getSubjectName() { return subjectName != null ? subjectName : "Class Lecture"; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public String getFacultyId() { return facultyId != null ? facultyId : teacherUid; }
    public void setFacultyId(String facultyId) { this.facultyId = facultyId; this.teacherUid = facultyId; }

    public String getTeacherUid() { return teacherUid != null ? teacherUid : facultyId; }
    public void setTeacherUid(String teacherUid) { this.teacherUid = teacherUid; this.facultyId = teacherUid; }

    public String getAttendanceId() { return getRecordId(); }
    public void setAttendanceId(String attendanceId) { setRecordId(attendanceId); }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getStudentUid() { return studentId; }
    public void setStudentUid(String studentUid) { this.studentId = studentUid; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getUsn() { return usn; }
    public void setUsn(String usn) { this.usn = usn; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getSlotId() { return slotId; }
    public void setSlotId(String slotId) { this.slotId = slotId; }

    public String getSessionId() { return sessionId != null ? sessionId : (slotId != null ? slotId : id); }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; if (this.slotId == null) this.slotId = sessionId; }

    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }

    public boolean isPresent() { return present; }
    public void setPresent(boolean present) { this.present = present; this.status = present ? "PRESENT" : "ABSENT"; }

    public String getSectionName() { return section; }
    public void setSectionName(String sectionName) { this.section = sectionName; }

    public String getStatus() { return status != null ? status : (present ? "PRESENT" : "ABSENT"); }
    public void setStatus(String status) { this.status = status; this.present = "PRESENT".equalsIgnoreCase(status); }

    public int getSemester() { return semester; }
    public void setSemester(int semester) { this.semester = semester; }

    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }

    public String getStudentUsn() { return studentUsn != null ? studentUsn : usn; }
    public void setStudentUsn(String studentUsn) { this.studentUsn = studentUsn; this.usn = studentUsn; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
