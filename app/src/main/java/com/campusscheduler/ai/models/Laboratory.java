package com.campusscheduler.ai.models;

import java.io.Serializable;

public class Laboratory implements Serializable {
    private String id;
    private String collegeId;
    private String departmentId;
    private String labName;
    private String roomNumber;
    private String labNumber;
    private int capacity;
    private String labType;

    public Laboratory() {}

    public Laboratory(String id, String labName, int capacity, boolean available) {
        this.id = id;
        this.labName = labName;
        this.roomNumber = labName;
        this.labNumber = labName;
        this.capacity = capacity;
        this.labType = "Computer Lab";
    }

    public Laboratory(String id, String collegeId, String departmentId, String labName, String roomNumber, int capacity, String labType) {
        this.id = id;
        this.collegeId = collegeId;
        this.departmentId = departmentId;
        this.labName = labName;
        this.roomNumber = roomNumber;
        this.labNumber = roomNumber;
        this.capacity = capacity;
        this.labType = labType;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCollegeId() { return collegeId; }
    public void setCollegeId(String collegeId) { this.collegeId = collegeId; }

    public String getDepartmentId() { return departmentId; }
    public void setDepartmentId(String departmentId) { this.departmentId = departmentId; }

    public String getLabName() { return labName; }
    public void setLabName(String labName) { this.labName = labName; }

    public String getRoomNumber() { return roomNumber != null ? roomNumber : labNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; this.labNumber = roomNumber; }

    public String getLabNumber() { return labNumber != null ? labNumber : roomNumber; }
    public void setLabNumber(String labNumber) { this.labNumber = labNumber; this.roomNumber = labNumber; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public String getLabType() { return labType; }
    public void setLabType(String labType) { this.labType = labType; }

    private boolean hasProjector;
    private boolean hasSmartDisplay;

    public boolean isHasProjector() { return hasProjector; }
    public void setHasProjector(boolean hasProjector) { this.hasProjector = hasProjector; }

    public boolean isHasSmartDisplay() { return hasSmartDisplay; }
    public void setHasSmartDisplay(boolean hasSmartDisplay) { this.hasSmartDisplay = hasSmartDisplay; }

    public boolean isSmartDisplay() { return hasSmartDisplay; }
    public void setSmartDisplay(boolean smartDisplay) { this.hasSmartDisplay = smartDisplay; }

    private java.util.List<String> supportedSubjectIds = new java.util.ArrayList<>();
    private java.util.List<String> supportedSubjectNames = new java.util.ArrayList<>();

    public java.util.List<String> getSupportedSubjectIds() {
        if (supportedSubjectIds == null) supportedSubjectIds = new java.util.ArrayList<>();
        return supportedSubjectIds;
    }
    public void setSupportedSubjectIds(java.util.List<String> supportedSubjectIds) {
        this.supportedSubjectIds = supportedSubjectIds != null ? supportedSubjectIds : new java.util.ArrayList<>();
    }

    public java.util.List<String> getSupportedSubjectNames() {
        if (supportedSubjectNames == null) supportedSubjectNames = new java.util.ArrayList<>();
        return supportedSubjectNames;
    }
    public void setSupportedSubjectNames(java.util.List<String> supportedSubjectNames) {
        this.supportedSubjectNames = supportedSubjectNames != null ? supportedSubjectNames : new java.util.ArrayList<>();
    }

    public void addSupportedSubject(String subjectId, String subjectName) {
        if (subjectId == null || subjectId.trim().isEmpty()) return;
        if (supportedSubjectIds == null) supportedSubjectIds = new java.util.ArrayList<>();
        if (supportedSubjectNames == null) supportedSubjectNames = new java.util.ArrayList<>();
        if (!supportedSubjectIds.contains(subjectId)) {
            supportedSubjectIds.add(subjectId);
            supportedSubjectNames.add(subjectName != null ? subjectName : subjectId);
        }
    }

    public void removeSupportedSubject(String subjectId) {
        if (subjectId == null || supportedSubjectIds == null) return;
        int idx = supportedSubjectIds.indexOf(subjectId);
        if (idx >= 0) {
            supportedSubjectIds.remove(idx);
            if (supportedSubjectNames != null && idx < supportedSubjectNames.size()) {
                supportedSubjectNames.remove(idx);
            }
        }
    }
}
