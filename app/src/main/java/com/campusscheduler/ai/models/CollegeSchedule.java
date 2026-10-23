package com.campusscheduler.ai.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CollegeSchedule implements Serializable {
    private String id;
    private String collegeId;
    private String name;
    private String startTime; // e.g. "09:00 AM"
    private String endTime;   // e.g. "04:30 PM"
    private int periodDurationMinutes; // e.g. 50
    private List<TimeSlot> slots;

    public CollegeSchedule() {
        this.slots = new ArrayList<>();
    }

    public CollegeSchedule(String id, String collegeId, String name, String startTime, String endTime, int periodDurationMinutes) {
        this.id = id;
        this.collegeId = collegeId;
        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;
        this.periodDurationMinutes = periodDurationMinutes;
        this.slots = new ArrayList<>();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCollegeId() { return collegeId; }
    public void setCollegeId(String collegeId) { this.collegeId = collegeId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public int getPeriodDurationMinutes() { return periodDurationMinutes; }
    public void setPeriodDurationMinutes(int periodDurationMinutes) { this.periodDurationMinutes = periodDurationMinutes; }

    public List<TimeSlot> getSlots() { return slots; }
    public void setSlots(List<TimeSlot> slots) { this.slots = slots; }
}
