package com.campusscheduler.ai.engine;

import java.util.Locale;

public class ChatIntentClassifier {
    public enum Intent {
        NEXT_CLASS,
        FIRST_CLASS_TOMORROW,
        WHERE_NEXT_LECTURE,
        SUBJECT_IN_SECTION,
        TODAYS_TIMETABLE,
        LEAVE_STATUS,
        SUBSTITUTE_STATUS,
        TEACHING_WORKLOAD,
        MY_STUDENTS,
        ROOM_LOOKUP,
        ASSIGNMENTS,
        EXAMS,
        ATTENDANCE,
        WORKSHOP_LOOKUP,
        UNKNOWN
    }

    public static Intent classify(String query) {
        if (query == null) return Intent.UNKNOWN;
        String q = query.trim().toLowerCase(Locale.ROOT);

        // Specific multi-word queries first
        if (q.contains("tomorrow") && (q.contains("first") || q.contains("class") || q.contains("lecture") || q.contains("timetable") || q.contains("schedule"))) {
            return Intent.FIRST_CLASS_TOMORROW;
        }

        if (q.contains("where") && (q.contains("next") || q.contains("lecture") || q.contains("class"))) {
            return Intent.WHERE_NEXT_LECTURE;
        }

        if ((q.contains("which subject") || q.contains("what subject")) && (q.contains("section") || q.contains("teach"))) {
            return Intent.SUBJECT_IN_SECTION;
        }

        if (q.contains("substitut")) {
            return Intent.SUBSTITUTE_STATUS;
        }

        if (q.contains("leave")) {
            return Intent.LEAVE_STATUS;
        }

        if (q.contains("workload") || q.contains("teaching hours") || q.contains("load")) {
            return Intent.TEACHING_WORKLOAD;
        }

        if (q.contains("student") && (q.contains("section") || q.contains("my") || q.contains("belong"))) {
            return Intent.MY_STUDENTS;
        }

        if (q.contains("next class") || q.contains("next lecture") || q.contains("have next") || q.contains("upcoming class") || q.equals("next")) {
            return Intent.NEXT_CLASS;
        }

        if (q.contains("today") || (q.contains("timetable") && !q.contains("next")) || (q.contains("schedule") && !q.contains("next"))) {
            return Intent.TODAYS_TIMETABLE;
        }

        if (q.contains("room") || q.contains("where is") || q.contains("lab")) {
            return Intent.ROOM_LOOKUP;
        }

        if (q.contains("assignment") || q.contains("homework") || q.contains("due")) {
            return Intent.ASSIGNMENTS;
        }

        if (q.contains("exam") || q.contains("ia") || q.contains("test")) {
            return Intent.EXAMS;
        }

        if (q.contains("attendance") || q.contains("percentage") || q.contains("bunk")) {
            return Intent.ATTENDANCE;
        }

        if (q.contains("workshop") || q.contains("seminar") || q.contains("event")) {
            return Intent.WORKSHOP_LOOKUP;
        }

        return Intent.UNKNOWN;
    }
}
