package com.campusscheduler.ai.models;

import java.io.Serializable;

public class Classroom implements Serializable {
    private String id;
    private String collegeId;
    private String roomNumber;
    private String buildingBlock;
    private String building;
    private int capacity;
    private boolean available = true;
    private boolean hasProjector;
    private boolean hasSmartDisplay;
    private boolean hasLabEquipment;

    public Classroom() {}

    public Classroom(String id, String roomNumber, int capacity, boolean available) {
        this.id = id;
        this.roomNumber = roomNumber;
        this.capacity = capacity;
        this.available = available;
        this.buildingBlock = "Main Block";
        this.building = "Main Block";
    }

    public Classroom(String id, String collegeId, String roomNumber, String buildingBlock, int capacity, boolean hasProjector, boolean hasLabEquipment) {
        this.id = id;
        this.collegeId = collegeId;
        this.roomNumber = roomNumber;
        this.buildingBlock = buildingBlock;
        this.building = buildingBlock;
        this.capacity = capacity;
        this.hasProjector = hasProjector;
        this.hasSmartDisplay = false;
        this.hasLabEquipment = hasLabEquipment;
        this.available = true;
    }

    public Classroom(String id, String collegeId, String roomNumber, String buildingBlock, int capacity, boolean hasProjector, boolean hasSmartDisplay, boolean hasLabEquipment) {
        this.id = id;
        this.collegeId = collegeId;
        this.roomNumber = roomNumber;
        this.buildingBlock = buildingBlock;
        this.building = buildingBlock;
        this.capacity = capacity;
        this.hasProjector = hasProjector;
        this.hasSmartDisplay = hasSmartDisplay;
        this.hasLabEquipment = hasLabEquipment;
        this.available = true;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCollegeId() { return collegeId; }
    public void setCollegeId(String collegeId) { this.collegeId = collegeId; }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    public String getBuildingBlock() { return buildingBlock != null ? buildingBlock : building; }
    public void setBuildingBlock(String buildingBlock) { this.buildingBlock = buildingBlock; this.building = buildingBlock; }

    public String getBuilding() { return building != null ? building : buildingBlock; }
    public void setBuilding(String building) { this.building = building; this.buildingBlock = building; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    public boolean isHasProjector() { return hasProjector; }
    public void setHasProjector(boolean hasProjector) { this.hasProjector = hasProjector; }

    public boolean isHasSmartDisplay() { return hasSmartDisplay; }
    public void setHasSmartDisplay(boolean hasSmartDisplay) { this.hasSmartDisplay = hasSmartDisplay; }

    public boolean isSmartDisplay() { return hasSmartDisplay; }
    public void setSmartDisplay(boolean smartDisplay) { this.hasSmartDisplay = smartDisplay; }

    public boolean isHasLabEquipment() { return hasLabEquipment; }
    public void setHasLabEquipment(boolean hasLabEquipment) { this.hasLabEquipment = hasLabEquipment; }

    public boolean isLab() { return hasLabEquipment; }
    public void setLab(boolean isLab) { this.hasLabEquipment = isLab; }

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
