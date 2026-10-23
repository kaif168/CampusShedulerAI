package com.campusscheduler.ai.engine;

import com.campusscheduler.ai.models.ClassSession;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ClassDetectionEngine {

    public enum ClassStatus {
        NO_CLASS,
        UPCOMING,
        ONGOING,
        COMPLETED,
        CANCELLED,
        SUBSTITUTED
    }

    public static class DetectionResult {
        private ClassStatus status;
        private ClassSession session;
        private String explanation;

        public DetectionResult(ClassStatus status, ClassSession session, String explanation) {
            this.status = status;
            this.session = session;
            this.explanation = explanation;
        }

        public ClassStatus getStatus() { return status; }
        public ClassSession getSession() { return session; }
        public String getExplanation() { return explanation; }
    }

    public static DetectionResult detectCurrentClass(List<ClassSession> allSessions) {
        if (allSessions == null || allSessions.isEmpty()) {
            return new DetectionResult(ClassStatus.NO_CLASS, null, "No academic class sessions scheduled for today.");
        }

        Calendar now = Calendar.getInstance();
        String effectiveDay = com.campusscheduler.ai.utils.UIUtils.getEffectiveAcademicDay(now);

        if ("Holiday".equalsIgnoreCase(effectiveDay) || "Sunday".equalsIgnoreCase(effectiveDay)) {
            return new DetectionResult(ClassStatus.NO_CLASS, null, "No academic classes scheduled today (" + effectiveDay + ").");
        }

        // Filter sessions matching today's effective day
        List<ClassSession> todaySessions = new java.util.ArrayList<>();
        for (ClassSession cs : allSessions) {
            if (cs == null) continue;
            String day = cs.getDayOfWeek();
            if (day == null || day.trim().isEmpty() || day.equalsIgnoreCase(effectiveDay)) {
                todaySessions.add(cs);
            }
        }

        if (todaySessions.isEmpty()) {
            return new DetectionResult(ClassStatus.NO_CLASS, null, "No academic class sessions scheduled for " + effectiveDay + ".");
        }

        // Sort today's sessions chronologically
        java.util.Collections.sort(todaySessions, (a, b) -> Integer.compare(
                com.campusscheduler.ai.utils.UIUtils.parseTimeToMinutes(a.getStartTime()),
                com.campusscheduler.ai.utils.UIUtils.parseTimeToMinutes(b.getStartTime())
        ));

        int currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);

        ClassSession ongoingSession = null;
        ClassSession upcomingSession = null;
        int minUpcomingDiff = Integer.MAX_VALUE;

        for (ClassSession cs : todaySessions) {
            if ("CANCELLED".equalsIgnoreCase(cs.getStatus())) {
                return new DetectionResult(ClassStatus.CANCELLED, cs, "Class '" + cs.getSubjectName() + "' was cancelled.");
            }

            int startMin = com.campusscheduler.ai.utils.UIUtils.parseTimeToMinutes(cs.getStartTime());
            int endMin = com.campusscheduler.ai.utils.UIUtils.parseTimeToMinutes(cs.getEndTime());

            if (startMin <= 0 || endMin <= 0) {
                startMin = 8 * 60 + 20; // default 08:20 AM
                endMin = 9 * 60;        // default 09:00 AM
            }

            if (currentMinutes >= startMin && currentMinutes <= endMin) {
                if ("SUBSTITUTED".equalsIgnoreCase(cs.getStatus()) || cs.getSubstituteTeacherName() != null) {
                    return new DetectionResult(ClassStatus.SUBSTITUTED, cs, "Class '" + cs.getSubjectName() + "' is ongoing with substitute teacher " + cs.getSubstituteTeacherName());
                }
                return new DetectionResult(ClassStatus.ONGOING, cs, "Class '" + cs.getSubjectName() + "' is currently ONGOING in room " + cs.getRoomNumber());
            } else if (startMin > currentMinutes) {
                int diff = startMin - currentMinutes;
                if (diff < minUpcomingDiff) {
                    minUpcomingDiff = diff;
                    upcomingSession = cs;
                }
            }
        }

        if (upcomingSession != null) {
            return new DetectionResult(ClassStatus.UPCOMING, upcomingSession, "Next class '" + upcomingSession.getSubjectName() + "' starts in " + minUpcomingDiff + " minutes.");
        }

        // Check if all classes completed
        ClassSession lastSession = todaySessions.get(todaySessions.size() - 1);
        int lastEnd = com.campusscheduler.ai.utils.UIUtils.parseTimeToMinutes(lastSession.getEndTime());
        if (lastEnd > 0 && currentMinutes > lastEnd) {
            return new DetectionResult(ClassStatus.COMPLETED, lastSession, "All scheduled academic sessions for " + effectiveDay + " are COMPLETED.");
        }

        return new DetectionResult(ClassStatus.NO_CLASS, null, "No active class at the present moment.");
    }
}
