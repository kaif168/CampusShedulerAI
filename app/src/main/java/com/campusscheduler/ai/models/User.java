package com.campusscheduler.ai.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class User implements Serializable {
    public static final String ROLE_OFFICIAL_OWNER = "OFFICIAL_OWNER";
    public static final String ROLE_COLLEGE_ADMIN = "COLLEGE_ADMIN";
    public static final String ROLE_ADMIN = "COLLEGE_ADMIN";
    public static final String ROLE_TEACHER = "TEACHER";
    public static final String ROLE_STUDENT = "STUDENT";

    private String uid;
    private String name;
    private String email;
    private String role;
    private String collegeId;
    private String departmentId;
    private String phone;
    private boolean active;
    private long createdAt;
    private List<String> subjects;

    public User() {
        this.subjects = new ArrayList<>();
    }

    public User(String uid, String name, String email, String role, String collegeId) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.role = role;
        this.collegeId = collegeId;
        this.active = true;
        this.createdAt = System.currentTimeMillis();
        this.subjects = new ArrayList<>();
    }

    public User(String uid, String name, String email, String role, String collegeId, String departmentId, String phone) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.role = role;
        this.collegeId = collegeId;
        this.departmentId = departmentId;
        this.phone = phone;
        this.active = true;
        this.createdAt = System.currentTimeMillis();
        this.subjects = new ArrayList<>();
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getCollegeId() { return collegeId; }
    public void setCollegeId(String collegeId) { this.collegeId = collegeId; }

    public String getDepartmentId() { return departmentId; }
    public void setDepartmentId(String departmentId) { this.departmentId = departmentId; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public List<String> getSubjects() { return subjects; }
    public void setSubjects(List<String> subjects) { this.subjects = subjects; }

    private String specialization;
    private String designation;

    private String usn;
    private String degree;
    private String year;
    private String semester;
    private String sectionId;
    private String collegeName;
    private String admissionType;
    private String departmentName;

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    private String qualification;
    private String experience;
    private String expertise;

    public String getQualification() { return qualification; }
    public void setQualification(String qualification) { this.qualification = qualification; }

    public String getExperience() { return experience; }
    public void setExperience(String experience) { this.experience = experience; }

    public String getExpertise() { return expertise; }
    public void setExpertise(String expertise) { this.expertise = expertise; }

    public String getUsn() { return usn; }
    public void setUsn(String usn) { this.usn = usn; }

    public String getDegree() { return degree; }
    public void setDegree(String degree) { this.degree = degree; }

    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }

    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }

    public String getSectionId() { return sectionId; }
    public void setSectionId(String sectionId) { this.sectionId = sectionId; }

    public String getCollegeName() { return collegeName; }
    public void setCollegeName(String collegeName) { this.collegeName = collegeName; }

    public String getAdmissionType() { return admissionType; }
    public void setAdmissionType(String admissionType) { this.admissionType = admissionType; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    private String collegeAddress;
    private String collegeCode;
    private String photoUrl;

    public String getCollegeAddress() { return collegeAddress; }
    public void setCollegeAddress(String collegeAddress) { this.collegeAddress = collegeAddress; }

    public String getCollegeCode() { return collegeCode; }
    public void setCollegeCode(String collegeCode) { this.collegeCode = collegeCode; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
}
