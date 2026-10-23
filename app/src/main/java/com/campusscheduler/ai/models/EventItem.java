package com.campusscheduler.ai.models;

import java.io.Serializable;

public class EventItem implements Serializable {
    private String id;
    private String collegeId;
    private String title;
    private String description;
    private String date;
    private String location;
    private String organizer;

    public EventItem() {}

    public EventItem(String id, String collegeId, String title, String description, String date, String location, String organizer) {
        this.id = id;
        this.collegeId = collegeId;
        this.title = title;
        this.description = description;
        this.date = date;
        this.location = location;
        this.organizer = organizer;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCollegeId() { return collegeId; }
    public void setCollegeId(String collegeId) { this.collegeId = collegeId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getOrganizer() { return organizer; }
    public void setOrganizer(String organizer) { this.organizer = organizer; }
}
