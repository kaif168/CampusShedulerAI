package com.campusscheduler.ai.engine;

import com.campusscheduler.ai.models.Assignment;
import com.campusscheduler.ai.models.ClassSession;
import com.campusscheduler.ai.models.Exam;
import com.campusscheduler.ai.models.LeaveRequest;
import com.campusscheduler.ai.models.Student;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.utils.FirebaseHelper;
import com.campusscheduler.ai.utils.UIUtils;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class AcademicChatbotEngine {

    public interface ChatCallback {
        void onResponse(String answerText);
    }

    public static void processQuery(String query, User user, ChatCallback callback) {
        if (callback == null) return;
        if (query == null || query.trim().isEmpty()) {
            callback.onResponse("Please enter a question about your timetable, classes, workload, or leaves.");
            return;
        }

        String collegeId = (user != null && user.getCollegeId() != null && !user.getCollegeId().trim().isEmpty())
                ? user.getCollegeId().trim() : "COLLEGE_1789406176044";

        ChatIntentClassifier.Intent intent = ChatIntentClassifier.classify(query);

        switch (intent) {
            case NEXT_CLASS:
            case WHERE_NEXT_LECTURE:
                handleNextClassQuery(user, collegeId, callback, false);
                break;

            case FIRST_CLASS_TOMORROW:
                handleFirstClassTomorrowQuery(user, collegeId, callback);
                break;

            case SUBJECT_IN_SECTION:
                handleSubjectInSectionQuery(query, user, collegeId, callback);
                break;

            case TODAYS_TIMETABLE:
                handleTodaysTimetableQuery(user, collegeId, callback);
                break;

            case LEAVE_STATUS:
                handleLeaveStatusQuery(user, collegeId, callback);
                break;

            case SUBSTITUTE_STATUS:
                handleSubstituteStatusQuery(user, collegeId, callback);
                break;

            case TEACHING_WORKLOAD:
                handleWorkloadQuery(user, collegeId, callback);
                break;

            case MY_STUDENTS:
                handleMyStudentsQuery(user, collegeId, callback);
                break;

            case ASSIGNMENTS:
                handleAssignmentsQuery(collegeId, callback);
                break;

            case EXAMS:
                handleExamsQuery(collegeId, callback);
                break;

            case ATTENDANCE:
                handleAttendanceQuery(user, callback);
                break;

            case ROOM_LOOKUP:
            case WORKSHOP_LOOKUP:
            case UNKNOWN:
            default:
                String name = user != null && user.getName() != null ? user.getName() : "Faculty";
                callback.onResponse("Hello " + name + "! I am your CampusScheduler AI Academic Assistant.\n\n"
                        + "You can ask me questions such as:\n"
                        + "• \"What is my next class?\"\n"
                        + "• \"What is my first class tomorrow?\"\n"
                        + "• \"Where is my next lecture?\"\n"
                        + "• \"What is my timetable today?\"\n"
                        + "• \"Am I on approved leave?\"\n"
                        + "• \"Who is substituting for me?\"\n"
                        + "• \"What is my teaching workload?\"\n"
                        + "• \"Which subject do I teach in Section A?\"\n"
                        + "• \"Which students belong to my section?\"");
                break;
        }
    }

    private static boolean isUserTeacherSession(ClassSession cs, User user) {
        if (cs == null || user == null) return false;
        String uid = user.getUid();
        String name = user.getName();

        if (uid != null && !uid.trim().isEmpty()) {
            if (uid.equalsIgnoreCase(cs.getFacultyId()) || uid.equalsIgnoreCase(cs.getTeacherUid())) {
                return true;
            }
        }

        if (name != null && !name.trim().isEmpty()) {
            String facName = cs.getFacultyName() != null ? cs.getFacultyName().trim() : (cs.getTeacherName() != null ? cs.getTeacherName().trim() : "");
            if (!facName.isEmpty()) {
                String cleanUser = name.replace("Dr.", "").replace("Prof.", "").trim();
                String cleanFac = facName.replace("Dr.", "").replace("Prof.", "").trim();
                if (facName.equalsIgnoreCase(name.trim()) || cleanFac.equalsIgnoreCase(cleanUser)
                        || facName.toLowerCase(Locale.ROOT).contains(cleanUser.toLowerCase(Locale.ROOT))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void handleNextClassQuery(User user, String collegeId, ChatCallback callback, boolean whereOnly) {
        TimeZone istTz = TimeZone.getTimeZone("Asia/Kolkata");
        Calendar nowCal = Calendar.getInstance(istTz, Locale.US);
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        sdfDate.setTimeZone(istTz);
        String todayDateStr = sdfDate.format(nowCal.getTime());

        SimpleDateFormat sdfDay = new SimpleDateFormat("EEEE", Locale.US);
        sdfDay.setTimeZone(istTz);
        String todayDayOfWeek = sdfDay.format(nowCal.getTime());

        int currentMinutesOfDay = nowCal.get(Calendar.HOUR_OF_DAY) * 60 + nowCal.get(Calendar.MINUTE);

        // Check approved leaves first
        FirebaseHelper.getLeaveRequestsRef().whereEqualTo("collegeId", collegeId).get()
                .addOnSuccessListener(leaveSnaps -> {
                    if (leaveSnaps != null && !leaveSnaps.isEmpty()) {
                        for (DocumentSnapshot doc : leaveSnaps) {
                            LeaveRequest lr = doc.toObject(LeaveRequest.class);
                            if (lr != null && "APPROVED".equalsIgnoreCase(lr.getStatus())) {
                                boolean isUser = (lr.getApplicantId() != null && user != null && lr.getApplicantId().equalsIgnoreCase(user.getUid()))
                                        || (lr.getApplicantName() != null && user != null && user.getName() != null && lr.getApplicantName().equalsIgnoreCase(user.getName()));
                                if (isUser && lr.getStartDate() != null && lr.getEndDate() != null) {
                                    if (todayDateStr.compareTo(lr.getStartDate()) >= 0 && todayDateStr.compareTo(lr.getEndDate()) <= 0) {
                                        String subName = lr.getSubstitutedFacultyName() != null ? lr.getSubstitutedFacultyName() : "Department Substitute";
                                        callback.onResponse("📋 Approved Leave Notice:\n"
                                                + "You are currently on approved leave for today (" + todayDateStr + ").\n"
                                                + "• Reason: " + lr.getReason() + "\n"
                                                + "• Assigned Substitute: " + subName + "\n\n"
                                                + "You have no active teaching sessions assigned to you today.");
                                        return;
                                    }
                                }
                            }
                        }
                    }

                    // Query class sessions
                    FirebaseHelper.getClassSessionsRef().whereEqualTo("collegeId", collegeId).get()
                            .addOnSuccessListener(csSnaps -> {
                                if (csSnaps == null || csSnaps.isEmpty()) {
                                    callback.onResponse("No published class sessions found for your institution in Firestore.");
                                    return;
                                }

                                List<ClassSession> teacherSessions = new ArrayList<>();
                                for (DocumentSnapshot doc : csSnaps) {
                                    ClassSession cs = doc.toObject(ClassSession.class);
                                    if (cs != null && isUserTeacherSession(cs, user)) {
                                        teacherSessions.add(cs);
                                    }
                                }

                                if (teacherSessions.isEmpty()) {
                                    callback.onResponse("No timetable sessions currently assigned to " + (user != null && user.getName() != null ? user.getName() : "your faculty profile") + ".");
                                    return;
                                }

                                // 1. Check today's sessions
                                List<ClassSession> todayList = new ArrayList<>();
                                for (ClassSession cs : teacherSessions) {
                                    String day = cs.getDayOfWeek() != null ? cs.getDayOfWeek() : "Monday";
                                    if (day.equalsIgnoreCase(todayDayOfWeek)) {
                                        // Skip sessions that have been replaced/substituted away
                                        if ("SUBSTITUTED".equalsIgnoreCase(cs.getStatus()) || cs.getSubstituteTeacherName() != null) {
                                            continue;
                                        }
                                        todayList.add(cs);
                                    }
                                }

                                // Sort today's sessions by start time
                                Collections.sort(todayList, (a, b) -> Integer.compare(
                                        UIUtils.parseTimeToMinutes(a.getStartTime()),
                                        UIUtils.parseTimeToMinutes(b.getStartTime())
                                ));

                                ClassSession ongoingSession = null;
                                ClassSession nextSession = null;

                                for (ClassSession cs : todayList) {
                                    int startMin = UIUtils.parseTimeToMinutes(cs.getStartTime());
                                    int endMin = UIUtils.parseTimeToMinutes(cs.getEndTime());
                                    if (endMin <= startMin) endMin = startMin + 50;

                                    if (currentMinutesOfDay >= startMin && currentMinutesOfDay < endMin) {
                                        ongoingSession = cs;
                                    } else if (startMin > currentMinutesOfDay) {
                                        if (nextSession == null) {
                                            nextSession = cs;
                                        }
                                    }
                                }

                                if (ongoingSession != null) {
                                    int endMin = UIUtils.parseTimeToMinutes(ongoingSession.getEndTime());
                                    if (endMin <= 0) endMin = UIUtils.parseTimeToMinutes(ongoingSession.getStartTime()) + 50;
                                    int remaining = Math.max(1, endMin - currentMinutesOfDay);

                                    StringBuilder sb = new StringBuilder();
                                    sb.append("⏳ Ongoing Class Right Now:\n")
                                      .append("• Subject: ").append(ongoingSession.getSubjectName())
                                      .append(ongoingSession.getSubjectCode() != null ? " (" + ongoingSession.getSubjectCode() + ")" : "")
                                      .append("\n• Cohort: Semester ").append(ongoingSession.getSemester()).append(" - Section ").append(ongoingSession.getSection() != null ? ongoingSession.getSection() : "A")
                                      .append("\n• Venue: ").append(ongoingSession.getRoomNumber() != null ? ongoingSession.getRoomNumber() : "Assigned Classroom")
                                      .append(ongoingSession.isLab() ? " [LABORATORY]" : " [THEORY]")
                                      .append("\n• Time: ").append(ongoingSession.getStartTime()).append(" - ").append(ongoingSession.getEndTime())
                                      .append(" (Ends in ").append(remaining).append(" mins)\n");

                                    if (nextSession != null) {
                                        sb.append("\n➡️ Next Scheduled Class After This:\n")
                                          .append("• Subject: ").append(nextSession.getSubjectName())
                                          .append("\n• Cohort: Semester ").append(nextSession.getSemester()).append(" - Section ").append(nextSession.getSection() != null ? nextSession.getSection() : "A")
                                          .append("\n• Time: ").append(nextSession.getStartTime()).append(" - ").append(nextSession.getEndTime())
                                          .append("\n• Venue: ").append(nextSession.getRoomNumber() != null ? nextSession.getRoomNumber() : "Assigned Room");
                                    } else {
                                        sb.append("\nThis is your last scheduled lecture for today.");
                                    }
                                    callback.onResponse(sb.toString());
                                    return;
                                }

                                if (nextSession != null) {
                                    StringBuilder sb = new StringBuilder();
                                    sb.append("🔔 Next Scheduled Class:\n")
                                      .append("• Subject: ").append(nextSession.getSubjectName())
                                      .append(nextSession.getSubjectCode() != null ? " (" + nextSession.getSubjectCode() + ")" : "")
                                      .append("\n• Cohort: Semester ").append(nextSession.getSemester()).append(" - Section ").append(nextSession.getSection() != null ? nextSession.getSection() : "A")
                                      .append("\n• Date: ").append(todayDateStr).append(" (Today, ").append(todayDayOfWeek).append(")")
                                      .append("\n• Time: ").append(nextSession.getStartTime()).append(" - ").append(nextSession.getEndTime())
                                      .append("\n• Venue: ").append(nextSession.getRoomNumber() != null ? nextSession.getRoomNumber() : "Assigned Hall")
                                      .append(nextSession.isLab() ? " [LAB]" : " [THEORY]")
                                      .append("\n• Department: ").append(nextSession.getDepartmentId() != null ? nextSession.getDepartmentId() : "CSE")
                                      .append("\n• Academic Year: 2026-2027");
                                    callback.onResponse(sb.toString());
                                    return;
                                }

                                // If no more classes today, find next teaching day
                                String[] weekDays = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
                                int todayIdx = -1;
                                for (int i = 0; i < weekDays.length; i++) {
                                    if (weekDays[i].equalsIgnoreCase(todayDayOfWeek)) {
                                        todayIdx = i;
                                        break;
                                    }
                                }

                                ClassSession futureSession = null;
                                String futureDay = null;
                                int daysAhead = 1;

                                for (int step = 1; step <= 7; step++) {
                                    int nextDayIdx = (todayIdx + step) % weekDays.length;
                                    String candidateDay = weekDays[nextDayIdx];
                                    List<ClassSession> candidateList = new ArrayList<>();
                                    for (ClassSession cs : teacherSessions) {
                                        if (candidateDay.equalsIgnoreCase(cs.getDayOfWeek())) {
                                            candidateList.add(cs);
                                        }
                                    }
                                    if (!candidateList.isEmpty()) {
                                        Collections.sort(candidateList, (a, b) -> Integer.compare(
                                                UIUtils.parseTimeToMinutes(a.getStartTime()),
                                                UIUtils.parseTimeToMinutes(b.getStartTime())
                                        ));
                                        futureSession = candidateList.get(0);
                                        futureDay = candidateDay;
                                        daysAhead = step;
                                        break;
                                    }
                                }

                                if (futureSession != null) {
                                    Calendar futureCal = (Calendar) nowCal.clone();
                                    futureCal.add(Calendar.DAY_OF_MONTH, daysAhead);
                                    String futureDateStr = sdfDate.format(futureCal.getTime());

                                    StringBuilder sb = new StringBuilder();
                                    sb.append("You have no further classes scheduled for today.\n\n")
                                      .append("📅 Next Scheduled Class:\n")
                                      .append("• Date: ").append(futureDateStr).append(" (").append(futureDay).append(")\n")
                                      .append("• Subject: ").append(futureSession.getSubjectName())
                                      .append(futureSession.getSubjectCode() != null ? " (" + futureSession.getSubjectCode() + ")" : "")
                                      .append("\n• Cohort: Semester ").append(futureSession.getSemester()).append(" - Section ").append(futureSession.getSection() != null ? futureSession.getSection() : "A")
                                      .append("\n• Time: ").append(futureSession.getStartTime()).append(" - ").append(futureSession.getEndTime())
                                      .append("\n• Venue: ").append(futureSession.getRoomNumber() != null ? futureSession.getRoomNumber() : "Assigned Hall")
                                      .append("\n• Department: ").append(futureSession.getDepartmentId() != null ? futureSession.getDepartmentId() : "CSE");
                                    callback.onResponse(sb.toString());
                                } else {
                                    callback.onResponse("No future classes found in the published academic timetable for your profile.");
                                }
                            })
                            .addOnFailureListener(e -> callback.onResponse("Failed to query timetable sessions: " + e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onResponse("Failed to check leave status: " + e.getMessage()));
    }

    private static void handleFirstClassTomorrowQuery(User user, String collegeId, ChatCallback callback) {
        TimeZone istTz = TimeZone.getTimeZone("Asia/Kolkata");
        Calendar cal = Calendar.getInstance(istTz, Locale.US);
        cal.add(Calendar.DAY_OF_MONTH, 1);

        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        sdfDate.setTimeZone(istTz);
        String tomorrowDateStr = sdfDate.format(cal.getTime());

        SimpleDateFormat sdfDay = new SimpleDateFormat("EEEE", Locale.US);
        sdfDay.setTimeZone(istTz);
        String tomorrowDay = sdfDay.format(cal.getTime());

        if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            callback.onResponse("Tomorrow is Sunday (Institutional Holiday). No academic sessions are scheduled.");
            return;
        }

        FirebaseHelper.getClassSessionsRef().whereEqualTo("collegeId", collegeId).get()
                .addOnSuccessListener(csSnaps -> {
                    if (csSnaps == null || csSnaps.isEmpty()) {
                        callback.onResponse("No published timetable found.");
                        return;
                    }

                    List<ClassSession> tomorrowSessions = new ArrayList<>();
                    for (DocumentSnapshot doc : csSnaps) {
                        ClassSession cs = doc.toObject(ClassSession.class);
                        if (cs != null && isUserTeacherSession(cs, user)) {
                            if (tomorrowDay.equalsIgnoreCase(cs.getDayOfWeek())) {
                                tomorrowSessions.add(cs);
                            }
                        }
                    }

                    if (tomorrowSessions.isEmpty()) {
                        callback.onResponse("You have no classes scheduled for tomorrow (" + tomorrowDay + ", " + tomorrowDateStr + ").");
                        return;
                    }

                    Collections.sort(tomorrowSessions, (a, b) -> Integer.compare(
                            UIUtils.parseTimeToMinutes(a.getStartTime()),
                            UIUtils.parseTimeToMinutes(b.getStartTime())
                    ));

                    ClassSession first = tomorrowSessions.get(0);
                    StringBuilder sb = new StringBuilder();
                    sb.append("🌅 First Class Tomorrow (").append(tomorrowDay).append(", ").append(tomorrowDateStr).append("):\n")
                      .append("• Subject: ").append(first.getSubjectName())
                      .append(first.getSubjectCode() != null ? " (" + first.getSubjectCode() + ")" : "")
                      .append("\n• Cohort: Semester ").append(first.getSemester()).append(" - Section ").append(first.getSection() != null ? first.getSection() : "A")
                      .append("\n• Time: ").append(first.getStartTime()).append(" - ").append(first.getEndTime())
                      .append("\n• Venue: ").append(first.getRoomNumber() != null ? first.getRoomNumber() : "Assigned Hall")
                      .append(first.isLab() ? " [LAB]" : " [THEORY]")
                      .append("\n• Total classes tomorrow: ").append(tomorrowSessions.size());
                    callback.onResponse(sb.toString());
                })
                .addOnFailureListener(e -> callback.onResponse("Error retrieving tomorrow's schedule: " + e.getMessage()));
    }

    private static void handleSubjectInSectionQuery(String query, User user, String collegeId, ChatCallback callback) {
        String secTarget = "A";
        String q = query.toUpperCase(Locale.ROOT);
        if (q.contains("SECTION B") || q.contains("SEC B")) secTarget = "B";
        else if (q.contains("SECTION C") || q.contains("SEC C")) secTarget = "C";
        else if (q.contains("SECTION D") || q.contains("SEC D")) secTarget = "D";
        final String finalSec = secTarget;

        FirebaseHelper.getClassSessionsRef().whereEqualTo("collegeId", collegeId).get()
                .addOnSuccessListener(csSnaps -> {
                    if (csSnaps == null || csSnaps.isEmpty()) {
                        callback.onResponse("No class sessions found.");
                        return;
                    }

                    Map<String, String> subjects = new HashMap<>();
                    int sem = 0;
                    for (DocumentSnapshot doc : csSnaps) {
                        ClassSession cs = doc.toObject(ClassSession.class);
                        if (cs != null && isUserTeacherSession(cs, user)) {
                            String sec = cs.getSection() != null ? cs.getSection() : cs.getSectionId();
                            if (sec != null && sec.equalsIgnoreCase(finalSec)) {
                                subjects.put(cs.getSubjectName(), cs.getSubjectCode() != null ? cs.getSubjectCode() : "Core");
                                sem = cs.getSemester();
                            }
                        }
                    }

                    if (subjects.isEmpty()) {
                        callback.onResponse("You are not currently assigned to teach any subjects in Section " + finalSec + ".");
                        return;
                    }

                    StringBuilder sb = new StringBuilder("📚 Teaching Assignments for Section " + finalSec + " (Semester " + sem + "):\n");
                    int i = 1;
                    for (Map.Entry<String, String> entry : subjects.entrySet()) {
                        sb.append(i++).append(". ").append(entry.getKey()).append(" [").append(entry.getValue()).append("]\n");
                    }
                    callback.onResponse(sb.toString());
                })
                .addOnFailureListener(e -> callback.onResponse("Error retrieving subject assignments."));
    }

    private static void handleTodaysTimetableQuery(User user, String collegeId, ChatCallback callback) {
        TimeZone istTz = TimeZone.getTimeZone("Asia/Kolkata");
        Calendar nowCal = Calendar.getInstance(istTz, Locale.US);
        SimpleDateFormat sdfDay = new SimpleDateFormat("EEEE", Locale.US);
        sdfDay.setTimeZone(istTz);
        String todayDay = sdfDay.format(nowCal.getTime());

        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        sdfDate.setTimeZone(istTz);
        String todayDate = sdfDate.format(nowCal.getTime());

        FirebaseHelper.getClassSessionsRef().whereEqualTo("collegeId", collegeId).get()
                .addOnSuccessListener(csSnaps -> {
                    if (csSnaps == null || csSnaps.isEmpty()) {
                        callback.onResponse("No timetable sessions published in Firestore.");
                        return;
                    }

                    List<ClassSession> todayList = new ArrayList<>();
                    for (DocumentSnapshot doc : csSnaps) {
                        ClassSession cs = doc.toObject(ClassSession.class);
                        if (cs != null && isUserTeacherSession(cs, user)) {
                            if (todayDay.equalsIgnoreCase(cs.getDayOfWeek())) {
                                todayList.add(cs);
                            }
                        }
                    }

                    if (todayList.isEmpty()) {
                        callback.onResponse("You have no teaching sessions scheduled today (" + todayDay + ", " + todayDate + ").");
                        return;
                    }

                    Collections.sort(todayList, (a, b) -> Integer.compare(
                            UIUtils.parseTimeToMinutes(a.getStartTime()),
                            UIUtils.parseTimeToMinutes(b.getStartTime())
                    ));

                    StringBuilder sb = new StringBuilder("📅 Your Timetable Today (" + todayDay + ", " + todayDate + "):\n\n");
                    int i = 1;
                    for (ClassSession cs : todayList) {
                        sb.append(i++).append(". ").append(cs.getStartTime()).append(" - ").append(cs.getEndTime()).append("\n")
                          .append("   • ").append(cs.getSubjectName())
                          .append(" | Sem ").append(cs.getSemester()).append("-").append(cs.getSection() != null ? cs.getSection() : "A")
                          .append("\n   • Venue: ").append(cs.getRoomNumber() != null ? cs.getRoomNumber() : "Assigned Hall")
                          .append(cs.isLab() ? " [LAB]" : " [THEORY]")
                          .append("\n\n");
                    }
                    callback.onResponse(sb.toString().trim());
                })
                .addOnFailureListener(e -> callback.onResponse("Error retrieving today's timetable."));
    }

    private static void handleLeaveStatusQuery(User user, String collegeId, ChatCallback callback) {
        if (user == null) {
            callback.onResponse("User profile is missing.");
            return;
        }

        FirebaseHelper.getLeaveRequestsRef().whereEqualTo("collegeId", collegeId).get()
                .addOnSuccessListener(querySnaps -> {
                    if (querySnaps == null || querySnaps.isEmpty()) {
                        callback.onResponse("You have no leave requests on record. Your academic standing is active.");
                        return;
                    }

                    List<LeaveRequest> userLeaves = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnaps) {
                        LeaveRequest lr = doc.toObject(LeaveRequest.class);
                        if (lr != null) {
                            if ((lr.getApplicantId() != null && lr.getApplicantId().equalsIgnoreCase(user.getUid()))
                                    || (lr.getApplicantName() != null && user.getName() != null && lr.getApplicantName().equalsIgnoreCase(user.getName()))) {
                                userLeaves.add(lr);
                            }
                        }
                    }

                    if (userLeaves.isEmpty()) {
                        callback.onResponse("You have no pending or approved leave requests. You are in active teaching duty.");
                        return;
                    }

                    StringBuilder sb = new StringBuilder("📋 Your Leave Requests & Status:\n\n");
                    int i = 1;
                    for (LeaveRequest lr : userLeaves) {
                        String status = lr.getStatus() != null ? lr.getStatus() : "PENDING";
                        sb.append(i++).append(". ").append(lr.getStartDate())
                          .append(lr.getEndDate() != null && !lr.getEndDate().equals(lr.getStartDate()) ? " to " + lr.getEndDate() : "")
                          .append("\n   • Status: [").append(status).append("]")
                          .append("\n   • Reason: ").append(lr.getReason() != null ? lr.getReason() : "Personal/Official")
                          .append("\n   • Substitute: ").append(lr.getSubstitutedFacultyName() != null ? lr.getSubstitutedFacultyName() : "None Assigned")
                          .append("\n\n");
                    }
                    callback.onResponse(sb.toString().trim());
                })
                .addOnFailureListener(e -> callback.onResponse("Failed to query leave records."));
    }

    private static void handleSubstituteStatusQuery(User user, String collegeId, ChatCallback callback) {
        if (user == null) {
            callback.onResponse("User profile is missing.");
            return;
        }

        FirebaseHelper.getLeaveRequestsRef().whereEqualTo("collegeId", collegeId).get()
                .addOnSuccessListener(querySnaps -> {
                    List<String> mySubstitutes = new ArrayList<>();
                    List<String> substitutingForOthers = new ArrayList<>();

                    if (querySnaps != null && !querySnaps.isEmpty()) {
                        for (DocumentSnapshot doc : querySnaps) {
                            LeaveRequest lr = doc.toObject(LeaveRequest.class);
                            if (lr != null && "APPROVED".equalsIgnoreCase(lr.getStatus())) {
                                boolean isMine = (lr.getApplicantId() != null && lr.getApplicantId().equalsIgnoreCase(user.getUid()))
                                        || (lr.getApplicantName() != null && user.getName() != null && lr.getApplicantName().equalsIgnoreCase(user.getName()));
                                if (isMine && lr.getSubstitutedFacultyName() != null) {
                                    mySubstitutes.add("• For dates " + lr.getStartDate() + " to " + lr.getEndDate() + ": " + lr.getSubstitutedFacultyName() + " is assigned as your substitute.");
                                }

                                boolean isSubstituting = (lr.getSubstitutedFacultyId() != null && lr.getSubstitutedFacultyId().equalsIgnoreCase(user.getUid()))
                                        || (lr.getSubstitutedFacultyName() != null && user.getName() != null && lr.getSubstitutedFacultyName().equalsIgnoreCase(user.getName()));
                                if (isSubstituting) {
                                    substitutingForOthers.add("• You are assigned to substitute for " + lr.getApplicantName() + " (" + lr.getStartDate() + " to " + lr.getEndDate() + ").");
                                }
                            }
                        }
                    }

                    StringBuilder sb = new StringBuilder();
                    if (!mySubstitutes.isEmpty()) {
                        sb.append("🔄 Substitutes Assigned For You:\n");
                        for (String s : mySubstitutes) sb.append(s).append("\n");
                    }

                    if (!substitutingForOthers.isEmpty()) {
                        if (sb.length() > 0) sb.append("\n");
                        sb.append("🤝 You Are Substituting For:\n");
                        for (String s : substitutingForOthers) sb.append(s).append("\n");
                    }

                    if (sb.length() == 0) {
                        callback.onResponse("There are currently no active substitution assignments involving your profile.");
                    } else {
                        callback.onResponse(sb.toString().trim());
                    }
                })
                .addOnFailureListener(e -> callback.onResponse("Failed to check substitution records."));
    }

    private static void handleWorkloadQuery(User user, String collegeId, ChatCallback callback) {
        String designation = user != null && user.getDesignation() != null ? user.getDesignation() : "Assistant Professor";
        int permittedHours = 16;
        if ("Professor".equalsIgnoreCase(designation)) {
            permittedHours = 10;
        } else if ("Associate Professor".equalsIgnoreCase(designation)) {
            permittedHours = 14;
        }

        final int maxHours = permittedHours;

        FirebaseHelper.getClassSessionsRef().whereEqualTo("collegeId", collegeId).get()
                .addOnSuccessListener(csSnaps -> {
                    int scheduledHours = 0;
                    Map<String, Integer> subjectHours = new HashMap<>();

                    if (csSnaps != null && !csSnaps.isEmpty()) {
                        for (DocumentSnapshot doc : csSnaps) {
                            ClassSession cs = doc.toObject(ClassSession.class);
                            if (cs != null && isUserTeacherSession(cs, user)) {
                                scheduledHours++;
                                String sub = cs.getSubjectName() != null ? cs.getSubjectName() : "Core Subject";
                                subjectHours.put(sub, subjectHours.getOrDefault(sub, 0) + 1);
                            }
                        }
                    }

                    int remaining = Math.max(0, maxHours - scheduledHours);
                    boolean isOverloaded = scheduledHours > maxHours;

                    StringBuilder sb = new StringBuilder();
                    sb.append("📊 Teaching Workload Summary:\n")
                      .append("• Faculty: ").append(user != null && user.getName() != null ? user.getName() : "Faculty")
                      .append(" (").append(designation).append(")\n")
                      .append("• Permitted Weekly Limit: ").append(maxHours).append(" hrs\n")
                      .append("• Scheduled Teaching Hours: ").append(scheduledHours).append(" hrs\n")
                      .append("• Remaining Workload Capacity: ").append(remaining).append(" hrs\n")
                      .append("• Compliance Status: ").append(isOverloaded ? "⚠️ WORKLOAD EXCEEDED" : "✅ Within Norms").append("\n\n")
                      .append("Subject-wise Hours:\n");

                    if (subjectHours.isEmpty()) {
                        sb.append("  (No subjects scheduled yet)\n");
                    } else {
                        for (Map.Entry<String, Integer> entry : subjectHours.entrySet()) {
                            sb.append("  • ").append(entry.getKey()).append(": ").append(entry.getValue()).append(" hrs/week\n");
                        }
                    }

                    callback.onResponse(sb.toString().trim());
                })
                .addOnFailureListener(e -> callback.onResponse("Unable to calculate teaching workload: " + e.getMessage()));
    }

    private static void handleMyStudentsQuery(User user, String collegeId, ChatCallback callback) {
        FirebaseHelper.getClassSessionsRef().whereEqualTo("collegeId", collegeId).get()
                .addOnSuccessListener(csSnaps -> {
                    String mySec = "A";
                    int mySem = 7;
                    if (csSnaps != null && !csSnaps.isEmpty()) {
                        for (DocumentSnapshot doc : csSnaps) {
                            ClassSession cs = doc.toObject(ClassSession.class);
                            if (cs != null && isUserTeacherSession(cs, user)) {
                                if (cs.getSection() != null) mySec = cs.getSection();
                                if (cs.getSemester() > 0) mySem = cs.getSemester();
                                break;
                            }
                        }
                    }

                    final String targetSec = mySec;
                    final int targetSem = mySem;

                    FirebaseHelper.getStudentsRef().whereEqualTo("collegeId", collegeId).get()
                            .addOnSuccessListener(stuSnaps -> {
                                List<String> studentNames = new ArrayList<>();
                                if (stuSnaps != null && !stuSnaps.isEmpty()) {
                                    for (DocumentSnapshot doc : stuSnaps) {
                                        Student s = doc.toObject(Student.class);
                                        if (s != null) {
                                            if (s.getSection() != null && s.getSection().equalsIgnoreCase(targetSec)) {
                                                studentNames.add(s.getName() + " (" + (s.getUsn() != null ? s.getUsn() : "N/A") + ")");
                                            }
                                        }
                                    }
                                }

                                StringBuilder sb = new StringBuilder();
                                sb.append("👥 Students in your assigned cohort (Semester ").append(targetSem).append(" - Section ").append(targetSec).append("):\n")
                                  .append("Total Registered: ").append(studentNames.size()).append("\n\n");

                                int count = Math.min(10, studentNames.size());
                                for (int i = 0; i < count; i++) {
                                    sb.append(i + 1).append(". ").append(studentNames.get(i)).append("\n");
                                }
                                if (studentNames.size() > 10) {
                                    sb.append("... and ").append(studentNames.size() - 10).append(" more students.");
                                } else if (studentNames.isEmpty()) {
                                    sb.append("(No students enrolled in Section ").append(targetSec).append(" yet.)");
                                }
                                callback.onResponse(sb.toString().trim());
                            })
                            .addOnFailureListener(e -> callback.onResponse("Failed to query student roster."));
                })
                .addOnFailureListener(e -> callback.onResponse("Failed to identify assigned section."));
    }

    private static void handleAssignmentsQuery(String collegeId, ChatCallback callback) {
        FirebaseHelper.getAssignmentsRef().whereEqualTo("collegeId", collegeId).get()
                .addOnSuccessListener(querySnapshots -> {
                    if (querySnapshots == null || querySnapshots.isEmpty()) {
                        callback.onResponse("No pending assignments found in Firestore.");
                        return;
                    }
                    StringBuilder sb = new StringBuilder("📝 Current Coursework Assignments:\n");
                    int idx = 1;
                    for (DocumentSnapshot doc : querySnapshots) {
                        Assignment a = doc.toObject(Assignment.class);
                        if (a != null) {
                            sb.append(idx++).append(". ").append(a.getTitle())
                              .append(" (Due: ").append(a.getDueDateStr() != null ? a.getDueDateStr() : "TBD").append(")\n");
                        }
                    }
                    callback.onResponse(sb.toString().trim());
                })
                .addOnFailureListener(e -> callback.onResponse("Unable to fetch assignments."));
    }

    private static void handleExamsQuery(String collegeId, ChatCallback callback) {
        FirebaseHelper.getExamsRef().whereEqualTo("collegeId", collegeId).get()
                .addOnSuccessListener(querySnapshots -> {
                    if (querySnapshots == null || querySnapshots.isEmpty()) {
                        callback.onResponse("No examination schedules published in Firestore for your department.");
                        return;
                    }
                    StringBuilder sb = new StringBuilder("Upcoming Examinations:\n");
                    int idx = 1;
                    for (DocumentSnapshot doc : querySnapshots) {
                        Exam ex = doc.toObject(Exam.class);
                        if (ex != null) {
                            sb.append(idx++).append(". ").append(ex.getSubjectName())
                              .append(" - Date: ").append(ex.getExamDate() != null ? ex.getExamDate() : "Scheduled")
                              .append(" in ").append(ex.getRoomNumber() != null ? ex.getRoomNumber() : "Exam Hall")
                              .append("\n");
                        }
                    }
                    callback.onResponse(sb.toString().trim());
                })
                .addOnFailureListener(e -> callback.onResponse("Unable to fetch exam schedules."));
    }

    private static void handleAttendanceQuery(User user, ChatCallback callback) {
        if (user == null) {
            callback.onResponse("Profile not available.");
            return;
        }

        FirebaseHelper.getAttendanceRecordsRef().whereEqualTo("collegeId", user.getCollegeId()).get()
                .addOnSuccessListener(querySnapshots -> {
                    if (querySnapshots == null || querySnapshots.isEmpty()) {
                        callback.onResponse("No attendance records logged in the system yet.");
                        return;
                    }
                    int total = querySnapshots.size();
                    callback.onResponse("📋 Attendance System Standing:\n• Total Institutional Attendance Records: " + total + "\n• Department Attendance Register: Active and up to date.");
                })
                .addOnFailureListener(e -> callback.onResponse("Unable to calculate attendance overview."));
    }
}
