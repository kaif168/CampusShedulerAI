package com.campusscheduler.ai.models;

import java.io.Serializable;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;

public class ClassAssignment implements Serializable {
    private String id;
    private String assignmentId;
    private String collegeId;
    private String departmentId;
    private String departmentName;
    private String academicYear;
    private String subjectId;
    private String subjectCode;
    private String subjectName;
    private String facultyId;
    private String teacherUid;
    private String teacherName;
    private int semester;
    private String section;
    private String sectionId;
    private String sectionName;
    private String classroomId;
    private String roomName;
    private String classType;
    private int weeklyHours;
    private int credits;

    public ClassAssignment() {}

    public ClassAssignment(String id, String collegeId, String departmentId, String subjectId, String facultyId, int semester, String section, int weeklyHours) {
        this.id = id;
        this.assignmentId = id;
        this.collegeId = collegeId;
        this.departmentId = departmentId;
        this.subjectId = subjectId;
        this.facultyId = facultyId;
        this.teacherUid = facultyId;
        this.semester = semester;
        this.section = section;
        this.sectionId = section;
        this.sectionName = section;
        this.weeklyHours = weeklyHours;
        this.credits = weeklyHours;
    }

    public String getId() { return id != null ? id : assignmentId; }
    public void setId(String id) { this.id = id; this.assignmentId = id; }

    public String getAssignmentId() { return assignmentId != null ? assignmentId : id; }
    public void setAssignmentId(String assignmentId) { this.assignmentId = assignmentId; this.id = assignmentId; }

    public String getCollegeId() { return collegeId; }
    public void setCollegeId(String collegeId) { this.collegeId = collegeId; }

    public String getDepartmentId() { return departmentId; }
    public void setDepartmentId(String departmentId) { this.departmentId = departmentId; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }

    public String getSubjectId() { return subjectId; }
    public void setSubjectId(String subjectId) { this.subjectId = subjectId; }

    public String getSubjectCode() { return subjectCode != null ? subjectCode : ""; }
    public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }

    public String getSubjectName() { return subjectName != null ? subjectName : "Subject"; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public String getFacultyId() { return facultyId != null ? facultyId : teacherUid; }
    public void setFacultyId(String facultyId) { this.facultyId = facultyId; this.teacherUid = facultyId; }

    public String getTeacherUid() { return teacherUid != null ? teacherUid : facultyId; }
    public void setTeacherUid(String teacherUid) { this.teacherUid = teacherUid; this.facultyId = teacherUid; }

    public String getTeacherName() { return teacherName != null ? teacherName : "Faculty"; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }

    @PropertyName("semester")
    public Object getSemesterRaw() { return semester; }

    @PropertyName("semester")
    public void setSemester(Object sem) {
        if (sem instanceof Number) {
            this.semester = ((Number) sem).intValue();
        } else if (sem instanceof String) {
            this.semester = parseSemesterNumber((String) sem);
        } else if (sem != null) {
            try {
                this.semester = Integer.parseInt(sem.toString().trim());
            } catch (Exception ignored) {
                this.semester = parseSemesterNumber(sem.toString());
            }
        }
    }

    @Exclude
    public int getSemester() { return semester; }

    @Exclude
    public void setSemester(int semester) {
        this.semester = semester;
    }

    public static int parseSemesterNumber(String raw) {
        if (raw == null || raw.trim().isEmpty()) return 0;
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            String digits = raw.replaceAll("[^0-9]", "");
            if (!digits.isEmpty()) {
                try {
                    return Integer.parseInt(digits);
                } catch (Exception ignored) {}
            }
        }
        return 0;
    }

    public static String normalizeSection(String sec) {
        if (sec == null) return "";
        String s = sec.trim();
        // Strip common prefixes like "Section " or "Sec "
        if (s.toLowerCase().startsWith("section")) {
            s = s.substring(7).trim();
        } else if (s.toLowerCase().startsWith("sec")) {
            s = s.substring(3).trim();
        }
        return s.toUpperCase();
    }

    public String getSection() { return section != null ? section : (sectionId != null ? sectionId : sectionName); }
    public void setSection(String section) { this.section = section; this.sectionId = section; this.sectionName = section; }

    public String getSectionId() { return sectionId != null ? sectionId : section; }
    public void setSectionId(String sectionId) { this.sectionId = sectionId; this.section = sectionId; }

    public String getSectionName() { return sectionName != null ? sectionName : section; }
    public void setSectionName(String sectionName) { this.sectionName = sectionName; this.section = sectionName; }

    public String getClassroomId() { return classroomId; }
    public void setClassroomId(String classroomId) { this.classroomId = classroomId; }

    public String getRoomName() { return roomName != null ? roomName : "Room 101"; }
    public void setRoomName(String roomName) { this.roomName = roomName; }

    public String getClassType() { return classType != null ? classType : "Theory"; }
    public void setClassType(String classType) { this.classType = classType; }

    public int getWeeklyHours() { return weeklyHours > 0 ? weeklyHours : credits; }
    public void setWeeklyHours(int weeklyHours) { this.weeklyHours = weeklyHours; if (this.credits <= 0) this.credits = weeklyHours; }

    public int getCredits() { return credits > 0 ? credits : weeklyHours; }
    public void setCredits(int credits) { this.credits = credits; if (this.weeklyHours <= 0) this.weeklyHours = credits; }

    public boolean isLab() { return "Lab".equalsIgnoreCase(classType); }
    public String getFacultyName() { return getTeacherName(); }
    public void setFacultyName(String facultyName) { setTeacherName(facultyName); }
    public String getRoomNumber() { return getRoomName(); }
    public void setRoomNumber(String roomNumber) { setRoomName(roomNumber); }
}
