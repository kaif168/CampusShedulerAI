package com.campusscheduler.ai.engine;

import com.campusscheduler.ai.models.ClassSession;
import com.campusscheduler.ai.models.Classroom;
import com.campusscheduler.ai.models.TimeSlot;

public class Gene {
    private TimeSlot timeSlot;
    private ClassSession classSession;
    private Classroom classroom;

    private boolean retained;

    public Gene(TimeSlot timeSlot, ClassSession classSession, Classroom classroom) {
        this.timeSlot = timeSlot;
        this.classSession = classSession;
        this.classroom = classroom;
        this.retained = false;
    }

    public Gene(TimeSlot timeSlot, ClassSession classSession, Classroom classroom, boolean retained) {
        this.timeSlot = timeSlot;
        this.classSession = classSession;
        this.classroom = classroom;
        this.retained = retained;
    }

    public boolean isRetained() { return retained; }
    public void setRetained(boolean retained) { this.retained = retained; }

    public TimeSlot getTimeSlot() { return timeSlot; }
    public void setTimeSlot(TimeSlot timeSlot) { this.timeSlot = timeSlot; }

    public ClassSession getClassSession() { return classSession; }
    public void setClassSession(ClassSession classSession) { this.classSession = classSession; }

    public Classroom getClassroom() { return classroom; }
    public void setClassroom(Classroom classroom) { this.classroom = classroom; }

    public String getGeneId() {
        return (classSession != null && classSession.getSessionId() != null ? classSession.getSessionId() : "SESS") +
                "_" + (timeSlot != null && timeSlot.getSlotId() != null ? timeSlot.getSlotId() : "SLOT");
    }

    public String getSubjectId() { return classSession != null ? classSession.getSubjectId() : null; }
    public String getSubjectName() { return classSession != null ? classSession.getSubjectName() : null; }
    public String getTeacherUid() { return classSession != null ? classSession.getFacultyId() : null; }
    public String getTeacherName() { return classSession != null ? classSession.getFacultyName() : null; }
    public String getSectionId() { return classSession != null ? classSession.getSection() : null; }
    public String getSectionName() { return classSession != null ? classSession.getSectionName() : null; }
    public String getRoomId() { return classroom != null ? classroom.getId() : null; }
    public String getRoomName() { return classroom != null ? classroom.getRoomNumber() : null; }
    public String getClassType() { return classSession != null ? classSession.getClassType() : "Theory"; }
    public String getDay() { return timeSlot != null ? timeSlot.getDayOfWeek() : null; }
    public String getStartTime() { return timeSlot != null ? timeSlot.getStartTime() : null; }
    public String getEndTime() { return timeSlot != null ? timeSlot.getEndTime() : null; }
}
