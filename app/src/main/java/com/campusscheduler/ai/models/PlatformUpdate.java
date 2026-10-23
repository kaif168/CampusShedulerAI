package com.campusscheduler.ai.models;

import java.io.Serializable;

public class PlatformUpdate implements Serializable {
    private String id;
    private String title;
    private String version;
    private String releaseNotes;
    private long timestamp;

    public PlatformUpdate() {}

    public PlatformUpdate(String id, String title, String version, String releaseNotes) {
        this.id = id;
        this.title = title;
        this.version = version;
        this.releaseNotes = releaseNotes;
        this.timestamp = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getReleaseNotes() { return releaseNotes; }
    public void setReleaseNotes(String releaseNotes) { this.releaseNotes = releaseNotes; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
