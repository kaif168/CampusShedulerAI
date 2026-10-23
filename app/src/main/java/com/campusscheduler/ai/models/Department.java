package com.campusscheduler.ai.models;

import java.io.Serializable;

public class Department implements Serializable {
    private String id;
    private String collegeId;
    private String name;
    private String departmentName;
    private String code;
    private String departmentCode;
    private String hodName;
    private String description;

    public Department() {}

    public Department(String id, String name, String code) {
        this.id = id;
        this.name = name;
        this.departmentName = name;
        this.code = code;
        this.departmentCode = code;
    }

    public Department(String id, String collegeId, String name, String code, String hodName) {
        this.id = id;
        this.collegeId = collegeId;
        this.name = name;
        this.departmentName = name;
        this.code = code;
        this.departmentCode = code;
        this.hodName = hodName;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCollegeId() { return collegeId; }
    public void setCollegeId(String collegeId) { this.collegeId = collegeId; }

    public String getName() { return name != null ? name : departmentName; }
    public void setName(String name) { this.name = name; this.departmentName = name; }

    public String getDepartmentName() { return departmentName != null ? departmentName : name; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; this.name = departmentName; }

    public String getCode() { return code != null ? code : departmentCode; }
    public void setCode(String code) { this.code = code; this.departmentCode = code; }

    public String getDepartmentCode() { return departmentCode != null ? departmentCode : code; }
    public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; this.code = departmentCode; }

    public String getHodName() { return hodName; }
    public void setHodName(String hodName) { this.hodName = hodName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
