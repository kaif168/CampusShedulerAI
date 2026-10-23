package com.campusscheduler.ai.engine;

import com.campusscheduler.ai.models.ClassSession;
import com.campusscheduler.ai.models.Faculty;
import com.campusscheduler.ai.models.LeaveRequest;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.utils.UIUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class TeacherSubstitutionEngine {

    public static class SubstituteCandidate implements Comparable<SubstituteCandidate> {
        private String facultyId;
        private String facultyName;
        private String email;
        private String designation;
        private String departmentId;
        private double score;
        private boolean isSubjectMatch;
        private boolean isAvailableInAllSlots;
        private int currentWorkload;
        private int maxWorkload;
        private String rationale;
        private String constraintsNotice;

        public SubstituteCandidate(String facultyId, String facultyName, String email, String designation,
                                   String departmentId, double score, boolean isSubjectMatch,
                                   boolean isAvailableInAllSlots, int currentWorkload, int maxWorkload,
                                   String rationale, String constraintsNotice) {
            this.facultyId = facultyId;
            this.facultyName = facultyName;
            this.email = email;
            this.designation = designation;
            this.departmentId = departmentId;
            this.score = score;
            this.isSubjectMatch = isSubjectMatch;
            this.isAvailableInAllSlots = isAvailableInAllSlots;
            this.currentWorkload = currentWorkload;
            this.maxWorkload = maxWorkload;
            this.rationale = rationale;
            this.constraintsNotice = constraintsNotice;
        }

        public String getFacultyId() { return facultyId; }
        public String getFacultyName() { return facultyName; }
        public String getEmail() { return email; }
        public String getDesignation() { return designation; }
        public String getDepartmentId() { return departmentId; }
        public double getScore() { return score; }
        public boolean isSubjectMatch() { return isSubjectMatch; }
        public boolean isAvailableInAllSlots() { return isAvailableInAllSlots; }
        public int getCurrentWorkload() { return currentWorkload; }
        public int getMaxWorkload() { return maxWorkload; }
        public String getRationale() { return rationale; }
        public String getConstraintsNotice() { return constraintsNotice; }

        @Override
        public int compareTo(SubstituteCandidate o) {
            return Double.compare(o.score, this.score); // Highest score first
        }
    }

    public static List<SubstituteCandidate> findAndRankSubstitutesForLeave(
            LeaveRequest leaveRequest,
            List<ClassSession> affectedSessions,
            List<Faculty> facultyList,
            List<ClassSession> allCollegeSessions,
            List<LeaveRequest> allApprovedLeaves
    ) {
        if (leaveRequest == null || facultyList == null || facultyList.isEmpty()) {
            return new ArrayList<>();
        }

        String requestingApplicantId = leaveRequest.getApplicantId() != null ? leaveRequest.getApplicantId() : "";
        String requestingApplicantName = leaveRequest.getApplicantName() != null ? leaveRequest.getApplicantName().trim() : "";
        String startDate = leaveRequest.getStartDate();
        String endDate = leaveRequest.getEndDate();

        // 1. Map current weekly workloads for all faculty
        Map<String, Integer> workloadMap = new HashMap<>();
        Map<String, Set<String>> occupiedSlotsByDay = new HashMap<>();

        if (allCollegeSessions != null) {
            for (ClassSession cs : allCollegeSessions) {
                String facId = cs.getFacultyId() != null ? cs.getFacultyId() : cs.getTeacherUid();
                if (facId != null && !facId.isEmpty()) {
                    workloadMap.put(facId, workloadMap.getOrDefault(facId, 0) + 1);
                    String day = cs.getDayOfWeek() != null ? cs.getDayOfWeek().toUpperCase(Locale.ROOT) : "MONDAY";
                    String slotKey = facId + "@" + day + "@" + (cs.getStartTime() != null ? cs.getStartTime() : "");
                    occupiedSlotsByDay.computeIfAbsent(facId, k -> new HashSet<>()).add(slotKey);
                }
            }
        }

        // 2. Identify faculty already on approved leave during the same dates
        Set<String> facultyOnLeave = new HashSet<>();
        if (allApprovedLeaves != null) {
            for (LeaveRequest lr : allApprovedLeaves) {
                if (lr == null || !"APPROVED".equalsIgnoreCase(lr.getStatus())) continue;
                String lrAppId = lr.getApplicantId();
                if (lrAppId != null && lr.getStartDate() != null && lr.getEndDate() != null) {
                    if (isDateRangeOverlapping(startDate, endDate, lr.getStartDate(), lr.getEndDate())) {
                        facultyOnLeave.add(lrAppId);
                        if (lr.getApplicantName() != null) {
                            facultyOnLeave.add(lr.getApplicantName().trim().toLowerCase(Locale.ROOT));
                        }
                    }
                }
            }
        }

        // 3. Extract subjects involved in affected sessions
        Set<String> affectedSubjectNames = new HashSet<>();
        Set<String> affectedSubjectCodes = new HashSet<>();
        if (affectedSessions != null) {
            for (ClassSession cs : affectedSessions) {
                if (cs.getSubjectName() != null && !cs.getSubjectName().isEmpty()) {
                    affectedSubjectNames.add(cs.getSubjectName().trim().toLowerCase(Locale.ROOT));
                }
                if (cs.getSubjectCode() != null && !cs.getSubjectCode().isEmpty()) {
                    affectedSubjectCodes.add(cs.getSubjectCode().trim().toLowerCase(Locale.ROOT));
                }
            }
        }

        List<SubstituteCandidate> candidates = new ArrayList<>();

        for (Faculty fac : facultyList) {
            if (fac == null) continue;
            String fId = fac.getId() != null ? fac.getId() : fac.getUid();
            String fName = fac.getName() != null ? fac.getName().trim() : "Faculty";

            // Exclude the teacher requesting leave
            if (fId != null && fId.equalsIgnoreCase(requestingApplicantId)) continue;
            if (fName.equalsIgnoreCase(requestingApplicantName)) continue;

            // Exclude teachers on approved leave
            if (facultyOnLeave.contains(fId) || facultyOnLeave.contains(fName.toLowerCase(Locale.ROOT))) {
                continue;
            }

            // Exclude faculty with no verified name or empty test docs
            if (fName.isEmpty() || "null".equalsIgnoreCase(fName)) continue;

            // Determine max permitted workload based on designation
            String desig = fac.getDesignation() != null ? fac.getDesignation() : "Assistant Professor";
            int maxLimit = 16;
            if (desig.toLowerCase(Locale.ROOT).contains("professor") && !desig.toLowerCase(Locale.ROOT).contains("assistant") && !desig.toLowerCase(Locale.ROOT).contains("associate")) {
                maxLimit = 10;
            } else if (desig.toLowerCase(Locale.ROOT).contains("associate")) {
                maxLimit = 14;
            }

            int currentLoad = workloadMap.getOrDefault(fId, 0);

            // 4. Check timetable availability across all affected slots
            boolean clashFound = false;
            if (affectedSessions != null) {
                for (ClassSession cs : affectedSessions) {
                    String day = cs.getDayOfWeek() != null ? cs.getDayOfWeek().toUpperCase(Locale.ROOT) : "MONDAY";
                    String slotKey = fId + "@" + day + "@" + (cs.getStartTime() != null ? cs.getStartTime() : "");
                    Set<String> facOccupied = occupiedSlotsByDay.get(fId);
                    if (facOccupied != null && facOccupied.contains(slotKey)) {
                        clashFound = true;
                        break;
                    }
                }
            }

            if (clashFound) {
                continue; // Do not assign an unavailable teacher
            }

            // 5. Score calculation
            double score = 30.0; // Base score for available non-conflicting faculty
            StringBuilder rationale = new StringBuilder("Available during all affected timetable periods.");
            StringBuilder constraints = new StringBuilder();

            // Primary Rule: Subject Qualification & Authorized Subject Mapping
            boolean subjectMatch = false;
            String spec = fac.getSpecialization() != null ? fac.getSpecialization().toLowerCase(Locale.ROOT) : "";
            List<String> qualSubs = fac.getQualifiedSubjectIds();

            for (String sub : affectedSubjectNames) {
                if (spec.contains(sub) || (qualSubs != null && qualSubs.contains(sub))) {
                    subjectMatch = true;
                    break;
                }
            }
            if (!subjectMatch) {
                for (String code : affectedSubjectCodes) {
                    if (spec.contains(code) || (qualSubs != null && qualSubs.contains(code))) {
                        subjectMatch = true;
                        break;
                    }
                }
            }

            // Also check if faculty teaches the same subject in another section
            if (!subjectMatch && allCollegeSessions != null) {
                for (ClassSession cs : allCollegeSessions) {
                    String csFac = cs.getFacultyId() != null ? cs.getFacultyId() : cs.getTeacherUid();
                    if (fId.equalsIgnoreCase(csFac)) {
                        String csSub = cs.getSubjectName() != null ? cs.getSubjectName().trim().toLowerCase(Locale.ROOT) : "";
                        if (affectedSubjectNames.contains(csSub)) {
                            subjectMatch = true;
                            rationale.append(" Already actively teaches this subject in another section.");
                            break;
                        }
                    }
                }
            }

            if (subjectMatch) {
                score += 50.0; // Substantial boost for qualified same-subject faculty
                rationale.append(" Subject-matter qualified & authorized.");
            } else {
                rationale.append(" Same department faculty.");
            }

            // Department alignment
            if (fac.getDepartmentId() != null && "DEPT_CSE".equalsIgnoreCase(fac.getDepartmentId())) {
                score += 15.0;
            }

            // Workload distribution: reward faculty with remaining capacity
            int remainingCapacity = maxLimit - currentLoad;
            if (remainingCapacity > 0) {
                score += Math.min(15.0, remainingCapacity * 2.0);
                constraints.append("Current workload: ").append(currentLoad).append("/").append(maxLimit).append(" hrs (Remaining: ").append(remainingCapacity).append(" hrs).");
            } else {
                score -= 20.0; // Penalize overloaded faculty
                constraints.append("⚠️ Approaching/exceeding weekly workload limit (").append(currentLoad).append("/").append(maxLimit).append(" hrs).");
            }

            // Experience / Seniority bonus
            try {
                if (fac.getExperience() != null) {
                    String expStr = fac.getExperience().replaceAll("[^0-9.]", "").trim();
                    if (!expStr.isEmpty()) {
                        double exp = Double.parseDouble(expStr);
                        if (exp > 0) {
                            score += Math.min(5.0, exp * 0.5);
                        }
                    }
                }
            } catch (Exception ignored) {}

            candidates.add(new SubstituteCandidate(
                    fId,
                    fName,
                    fac.getEmail(),
                    desig,
                    fac.getDepartmentId(),
                    score,
                    subjectMatch,
                    true,
                    currentLoad,
                    maxLimit,
                    rationale.toString(),
                    constraints.toString()
            ));
        }

        Collections.sort(candidates);
        return candidates;
    }

    private static boolean isDateRangeOverlapping(String start1, String end1, String start2, String end2) {
        if (start1 == null || end1 == null || start2 == null || end2 == null) return false;
        return (start1.compareTo(end2) <= 0) && (end1.compareTo(start2) >= 0);
    }
}
