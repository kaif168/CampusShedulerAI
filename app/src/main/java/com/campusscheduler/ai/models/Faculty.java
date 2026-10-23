package com.campusscheduler.ai.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Faculty implements Serializable {

    private String id;
    private String uid;

    private String collegeId;
    private String departmentId;

    private String name;
    private String email;
    private String phone;

    private String designation;
    private String shortName;

    // Faculty profile information
    private String qualification;
    private String experience;
    private String specialization;
    private String expertise;

    // Workload
    private int workloadHours;
    private int maxWorkloadHours;
    private int maxLoadPerWeek;

    // Subjects the faculty is qualified/allowed to teach
    private List<String> qualifiedSubjectIds;

    // Account/profile status
    private String status;


    // ---------------------------------------------------------
    // Empty constructor - REQUIRED for Firestore
    // ---------------------------------------------------------
    public Faculty() {
        this.qualifiedSubjectIds = new ArrayList<>();
        this.status = "ACTIVE";
    }


    // ---------------------------------------------------------
    // Basic constructor
    // ---------------------------------------------------------
    public Faculty(
            String id,
            String collegeId,
            String name,
            String email,
            String designation
    ) {
        this.id = id;
        this.uid = null;
        this.collegeId = collegeId;
        this.name = name;
        this.email = email;
        this.designation = designation;

        this.qualification = "";
        this.experience = "";
        this.specialization = "";
        this.expertise = "";

        int workload = getWorkloadForDesignation(designation);

        this.workloadHours = 0;
        this.maxLoadPerWeek = workload;
        this.maxWorkloadHours = workload;

        this.qualifiedSubjectIds = new ArrayList<>();
        this.status = "ACTIVE";
    }


    // ---------------------------------------------------------
    // Main constructor used by importer / existing code
    // ---------------------------------------------------------
    public Faculty(
            String id,
            String uid,
            String collegeId,
            String departmentId,
            String name,
            String email,
            String phone,
            String designation,
            String specialization,
            int maxLoadPerWeek
    ) {
        this.id = id;
        this.uid = uid;

        this.collegeId = collegeId;
        this.departmentId = departmentId;

        this.name = name;
        this.email = email;
        this.phone = phone;

        this.designation = designation;

        this.qualification = "";
        this.experience = "";
        this.specialization = specialization;
        this.expertise = "";

        this.workloadHours = 0;
        this.maxLoadPerWeek = maxLoadPerWeek;
        this.maxWorkloadHours = maxLoadPerWeek;

        this.qualifiedSubjectIds = new ArrayList<>();
        this.status = "ACTIVE";
    }


    // =========================================================
    // GETTERS
    // =========================================================

    public String getId() {
        return id;
    }

    public String getUid() {
        return uid;
    }

    public String getCollegeId() {
        return collegeId;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getDesignation() {
        return designation;
    }

    public String getShortName() {
        return shortName;
    }

    public String getQualification() {
        return qualification;
    }

    public String getExperience() {
        return experience;
    }

    public String getSpecialization() {
        return specialization;
    }

    public String getExpertise() {
        return expertise;
    }

    public int getWorkloadHours() {
        return workloadHours;
    }

    public int getMaxWorkloadHours() {
        return maxWorkloadHours;
    }

    public int getMaxLoadPerWeek() {
        return maxLoadPerWeek;
    }

    public List<String> getQualifiedSubjectIds() {
        if (qualifiedSubjectIds == null) {
            qualifiedSubjectIds = new ArrayList<>();
        }
        return qualifiedSubjectIds;
    }

    public String getStatus() {
        return status;
    }


    // =========================================================
    // SETTERS
    // =========================================================

    public void setId(String id) {
        this.id = id;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public void setCollegeId(String collegeId) {
        this.collegeId = collegeId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public void setQualification(String qualification) {
        this.qualification = qualification;
    }

    public void setExperience(String experience) {
        this.experience = experience;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public void setExpertise(String expertise) {
        this.expertise = expertise;
    }

    public void setWorkloadHours(int workloadHours) {
        this.workloadHours = workloadHours;
    }

    public void setMaxWorkloadHours(int maxWorkloadHours) {
        this.maxWorkloadHours = maxWorkloadHours;
    }

    public void setMaxLoadPerWeek(int maxLoadPerWeek) {
        this.maxLoadPerWeek = maxLoadPerWeek;
    }

    public void setQualifiedSubjectIds(List<String> qualifiedSubjectIds) {
        this.qualifiedSubjectIds =
                qualifiedSubjectIds != null
                        ? qualifiedSubjectIds
                        : new ArrayList<>();
    }

    public void setStatus(String status) {
        this.status = status;
    }


    // =========================================================
    // WORKLOAD RULE
    //
    // Professor           = 10 hours
    // Assistant Professor = 14 hours
    // Associate Professor = 16 hours
    // =========================================================

    public static int getWorkloadForDesignation(String designation) {

        if (designation == null || designation.trim().isEmpty()) {
            return 14;
        }

        String d = designation
                .trim()
                .toLowerCase()
                .replace(".", "");

        // Associate Professor must be checked BEFORE Professor
        if (d.contains("associate professor")
                || d.equals("associate")) {
            return 16;
        }

        if (d.contains("assistant professor")
                || d.contains("assistant")) {
            return 14;
        }

        if (d.equals("professor")
                || d.contains("professor")
                || d.contains("hod")
                || d.contains("principal")) {
            return 10;
        }

        // Default standard faculty workload limit (Assistant Professor)
        return 14;
    }
}