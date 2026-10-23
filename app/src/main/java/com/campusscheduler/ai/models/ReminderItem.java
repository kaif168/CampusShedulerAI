package com.campusscheduler.ai.models;

import java.io.Serializable;

public class ReminderItem implements Serializable {
    private String id;
    private String reminderId;
    private String userId;
    private String userUid;
    private String title;
    private String description;
    private String date;
    private String time;
    private String dateTime;
    private boolean completed;

    public ReminderItem() {}

    public ReminderItem(String id, String userId, String title, String dateTime) {
        this.id = id;
        this.reminderId = id;
        this.userId = userId;
        this.userUid = userId;
        this.title = title;
        this.dateTime = dateTime;
        this.completed = false;
    }

    public String getId() { return id != null ? id : reminderId; }
    public void setId(String id) { this.id = id; this.reminderId = id; }

    public String getReminderId() { return reminderId != null ? reminderId : id; }
    public void setReminderId(String reminderId) { this.reminderId = reminderId; this.id = reminderId; }

    public String getUserId() { return userId != null ? userId : userUid; }
    public void setUserId(String userId) { this.userId = userId; this.userUid = userId; }

    public String getUserUid() { return userUid != null ? userUid : userId; }
    public void setUserUid(String userUid) { this.userUid = userUid; this.userId = userUid; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description != null ? description : title; }
    public void setDescription(String description) { this.description = description; }

    public String getDate() { return date != null ? date : (dateTime != null ? dateTime : "Today"); }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time != null ? time : "09:00 AM"; }
    public void setTime(String time) { this.time = time; }

    public String getDateTime() { return dateTime; }
    public void setDateTime(String dateTime) { this.dateTime = dateTime; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
}
