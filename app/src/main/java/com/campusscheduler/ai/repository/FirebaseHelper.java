package com.campusscheduler.ai.repository;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class FirebaseHelper {

    public static final String COL_USERS = "users";
    public static final String COL_COLLEGES = "colleges";
    public static final String COL_FACULTY = "faculty";
    public static final String COL_STUDENTS = "students";
    public static final String COL_DEPARTMENTS = "departments";
    public static final String COL_SUBJECTS = "subjects";
    public static final String COL_SEMESTERS = "semesters";
    public static final String COL_SECTIONS = "sections";
    public static final String COL_CLASSROOMS = "classrooms";
    public static final String COL_LABORATORIES = "laboratories";
    public static final String COL_SCHEDULE_CONFIGS = "scheduleConfigs";
    public static final String COL_CLASS_ASSIGNMENTS = "classAssignments";
    public static final String COL_CLASS_SESSIONS = "classSessions";
    public static final String COL_TIMETABLES = "timetables";
    public static final String COL_ATTENDANCE = "attendance";
    public static final String COL_ASSIGNMENTS = "assignments";
    public static final String COL_RESOURCES = "resources";
    public static final String COL_QUIZZES = "quizzes";
    public static final String COL_QUIZ_ATTEMPTS = "quizAttempts";
    public static final String COL_DISCUSSION_POSTS = "discussionPosts";
    public static final String COL_LEAVE_REQUESTS = "leaveRequests";
    public static final String COL_SUBSTITUTIONS = "substitutions";
    public static final String COL_EXAMS = "exams";
    public static final String COL_IA_TIMETABLES = "iaTimetables";
    public static final String COL_EVENTS = "events";
    public static final String COL_WORKSHOPS = "workshops";
    public static final String COL_REMINDERS = "reminders";
    public static final String COL_NOTIFICATIONS = "notifications";
    public static final String COL_PLATFORM_UPDATES = "platformUpdates";
    public static final String COL_ANALYTICS = "analyticsReports";


    // ============================================================
    // PLATFORM UPDATES
    // ============================================================

    public static CollectionReference getPlatformUpdatesRef() {
        return FirebaseFirestore
                .getInstance()
                .collection(COL_PLATFORM_UPDATES);
    }
}