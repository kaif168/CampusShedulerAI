package com.campusscheduler.ai.engine;

import com.campusscheduler.ai.models.Subject;
import com.campusscheduler.ai.models.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TeacherRecommendationEngine {

    public static class RecommendationCandidate implements Comparable<RecommendationCandidate> {
        private User faculty;
        private double suitabilityScore;
        private String rationale;

        public RecommendationCandidate(User faculty, double suitabilityScore, String rationale) {
            this.faculty = faculty;
            this.suitabilityScore = suitabilityScore;
            this.rationale = rationale;
        }

        public User getFaculty() { return faculty; }
        public double getSuitabilityScore() { return suitabilityScore; }
        public String getRationale() { return rationale; }

        @Override
        public int compareTo(RecommendationCandidate o) {
            return Double.compare(o.suitabilityScore, this.suitabilityScore);
        }
    }

    public static List<RecommendationCandidate> recommendTeachersForSubject(Subject subject, List<User> facultyList) {
        if (subject == null || facultyList == null) return new ArrayList<>();

        List<RecommendationCandidate> candidates = new ArrayList<>();
        String subName = subject.getSubjectName() != null ? subject.getSubjectName().toLowerCase() : "";
        String deptId = subject.getDepartmentId();

        for (User fac : facultyList) {
            // Multi-college tenant check
            if (subject.getCollegeId() != null && fac.getCollegeId() != null
                    && !subject.getCollegeId().equalsIgnoreCase(fac.getCollegeId())) {
                continue;
            }

            double score = 40.0;
            StringBuilder rationale = new StringBuilder("Eligible faculty member.");

            // Department match
            if (fac.getDepartmentId() != null && deptId != null && fac.getDepartmentId().equalsIgnoreCase(deptId)) {
                score += 30.0;
                rationale.append(" Primary department match.");
            }

            // Specialization match
            if (fac.getSpecialization() != null && !subName.isEmpty()
                    && fac.getSpecialization().toLowerCase().contains(subName)) {
                score += 30.0;
                rationale.append(" Subject domain expertise match.");
            }

            // Designation workload limit check
            int maxLimit = 16;
            if ("Professor".equalsIgnoreCase(fac.getDesignation())) maxLimit = 10;
            else if ("Associate Professor".equalsIgnoreCase(fac.getDesignation())) maxLimit = 14;

            score += (maxLimit / 2.0); // Reward higher qualified designation bandwidth

            candidates.add(new RecommendationCandidate(fac, score, rationale.toString()));
        }

        Collections.sort(candidates);
        return candidates;
    }
}
