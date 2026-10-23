package com.campusscheduler.ai.models;

import java.io.Serializable;

public class TimeSlot implements Serializable {
    private String slotId;
    private String dayOfWeek;
    private int periodIndex;
    private String startTime;
    private String endTime;
    private boolean isBreak;

    public TimeSlot() {}

    public TimeSlot(String slotId, String dayOfWeek, int periodIndex, String startTime, String endTime) {
        this.slotId = slotId;
        this.dayOfWeek = dayOfWeek;
        this.periodIndex = periodIndex;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isBreak = false;
    }

    public TimeSlot(String slotId, String dayOfWeek, int periodIndex, String startTime, String endTime, boolean isBreak) {
        this.slotId = slotId;
        this.dayOfWeek = dayOfWeek;
        this.periodIndex = periodIndex;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isBreak = isBreak;
    }

    public String getSlotId() { return slotId; }
    public void setSlotId(String slotId) { this.slotId = slotId; }

    public String getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public int getPeriodIndex() { return periodIndex; }
    public int getPeriodNumber() { return periodIndex; }
    public void setPeriodIndex(int periodIndex) { this.periodIndex = periodIndex; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public boolean isBreak() { return isBreak; }
    public boolean isBreakSlot() { return isBreak; }
    public void setBreak(boolean breakSlot) { isBreak = breakSlot; }
}
