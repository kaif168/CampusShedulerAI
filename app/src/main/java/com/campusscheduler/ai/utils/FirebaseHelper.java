package com.campusscheduler.ai.utils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class FirebaseHelper {

    public static FirebaseAuth getAuth() {
        return FirebaseAuth.getInstance();
    }

    public static FirebaseFirestore getFirestore() {
        return FirebaseFirestore.getInstance();
    }

    public static FirebaseStorage getStorage() {
        return FirebaseStorage.getInstance();
    }

    public static StorageReference getStorageRef() {
        return getStorage().getReference();
    }

    public static CollectionReference getUsersRef() {
        return getFirestore().collection("users");
    }

    public static CollectionReference getCollegesRef() {
        return getFirestore().collection("colleges");
    }

    public static CollectionReference getDepartmentsRef() {
        return getFirestore().collection("departments");
    }

    public static CollectionReference getSubjectsRef() {
        return getFirestore().collection("subjects");
    }

    public static CollectionReference getSemestersRef() {
        return getFirestore().collection("semesters");
    }

    public static CollectionReference getSectionsRef() {
        return getFirestore().collection("sections");
    }

    public static CollectionReference getClassroomsRef() {
        return getFirestore().collection("classrooms");
    }

    public static CollectionReference getLaboratoriesRef() {
        return getFirestore().collection("laboratories");
    }

    public static CollectionReference getFacultyRef() {
        return getFirestore().collection("faculty");
    }

    public static CollectionReference getStudentsRef() {
        return getFirestore().collection("students");
    }

    public static CollectionReference getClassAssignmentsRef() {
        return getFirestore().collection("classAssignments");
    }

    public static CollectionReference getClassSessionsRef() {
        return getFirestore().collection("classSessions");
    }

    public static CollectionReference getTimetablesRef() {
        return getFirestore().collection("timetables");
    }

    public static CollectionReference getAttendanceRef() {
        return getFirestore().collection("attendance");
    }

    public static CollectionReference getAttendanceRecordsRef() {
        return getAttendanceRef();
    }

    public static CollectionReference getAssignmentsRef() {
        return getFirestore().collection("assignments");
    }

    public static CollectionReference getResourcesRef() {
        return getFirestore().collection("resources");
    }

    public static CollectionReference getQuizzesRef() {
        return getFirestore().collection("quizzes");
    }

    public static CollectionReference getDiscussionPostsRef() {
        return getFirestore().collection("discussionPosts");
    }

    public static CollectionReference getLeaveRequestsRef() {
        return getFirestore().collection("leaveRequests");
    }

    public static CollectionReference getLeavesRef() {
        return getLeaveRequestsRef();
    }

    public static CollectionReference getExamsRef() {
        return getFirestore().collection("exams");
    }

    public static CollectionReference getSubstitutionsRef() {
        return getFirestore().collection("substitutions");
    }

    public static CollectionReference getIaTimetablesRef() {
        return getFirestore().collection("iaTimetables");
    }

    public static CollectionReference getRemindersRef() {
        return getFirestore().collection("reminders");
    }

    public static CollectionReference getNotificationsRef() {
        return getFirestore().collection("notifications");
    }

    public static CollectionReference getUpdatesRef() {
        return getFirestore().collection("platformUpdates");
    }

    public static CollectionReference getReportsRef() {
        return getFirestore().collection("analyticsReports");
    }

    public static CollectionReference getProfileChangeRequestsRef() {
        return getFirestore().collection("profile_change_requests");
    }
}
