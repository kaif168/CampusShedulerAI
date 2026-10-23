package com.campusscheduler.ai.models;

import java.io.Serializable;

public class WorkshopItem implements Serializable {
    private String id;
    private String collegeId;
    private String title;
    private String instructor;
    private String date;
    private String venue;
    private String topic;

    public WorkshopItem() {}

    public WorkshopItem(String id, String collegeId, String title, String instructor, String date, String venue, String topic) {
        this.id = id;
        this.collegeId = collegeId;
        this.title = title;
        this.instructor = instructor;
        this.date = date;
        this.venue = venue;
        this.topic = topic;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCollegeId() { return collegeId; }
    public void setCollegeId(String collegeId) { this.collegeId = collegeId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getInstructor() { return instructor; }
    public void setInstructor(String instructor) { this.instructor = instructor; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
}
