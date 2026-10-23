package com.campusscheduler.ai.models;

import java.io.Serializable;

public class Student implements Serializable {
    private String id;
    private String studentId;
    private String uid;
    private String collegeId;
    private String departmentId;
    private String usn;
    private String rollNumber;
    private String name;
    private String email;
    private String phone;
    private int semester;
    private String section;

    public Student() {}

    public Student(String studentId, String collegeId, String name, String email, String rollNumber) {
        this.id = studentId;
        this.studentId = studentId;
        this.collegeId = collegeId;
        this.name = name;
        this.email = email;
        this.usn = rollNumber;
        this.rollNumber = rollNumber;
    }

    public Student(String id, String uid, String collegeId, String departmentId, String usn, String name, String email, String phone, int semester, String section) {
        this.id = id;
        this.studentId = id;
        this.uid = uid;
        this.collegeId = collegeId;
        this.departmentId = departmentId;
        this.usn = usn;
        this.rollNumber = usn;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.semester = semester;
        this.section = section;
    }

    public String getId() { return id != null ? id : studentId; }
    public void setId(String id) { this.id = id; this.studentId = id; }

    public String getStudentId() { return studentId != null ? studentId : id; }
    public void setStudentId(String studentId) { this.studentId = studentId; this.id = studentId; }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getCollegeId() { return collegeId; }
    public void setCollegeId(String collegeId) { this.collegeId = collegeId; }

    public String getDepartmentId() { return departmentId; }
    public void setDepartmentId(String departmentId) { this.departmentId = departmentId; }

    public String getUsn() { return usn != null ? usn : rollNumber; }
    public void setUsn(String usn) { this.usn = usn; this.rollNumber = usn; }

    public String getRollNumber() { return rollNumber != null ? rollNumber : usn; }
    public void setRollNumber(String rollNumber) { this.rollNumber = rollNumber; this.usn = rollNumber; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public int getSemester() { return semester; }
    public void setSemester(int semester) { this.semester = semester; }

    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }

    public String getSectionName() { return section; }
    public void setSectionName(String sectionName) { this.section = sectionName; }
}
