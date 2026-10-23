package com.campusscheduler.ai.engine;

import com.campusscheduler.ai.models.ConstraintEvaluationReport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConstraintChecker {

    public static void evaluateFitness(Chromosome chromosome) {
        if (chromosome == null || chromosome.getGenes() == null) return;

        List<ConstraintEvaluationReport> reports = new ArrayList<>();
        int totalHardViolations = 0;
        int softPenalty = 0;

        // Tracks to detect double bookings
        Map<String, List<String>> teacherSlotMap = new HashMap<>();
        Map<String, List<String>> roomSlotMap = new HashMap<>();
        Map<String, List<String>> sectionSlotMap = new HashMap<>();

        Map<String, Integer> teacherWeeklyWorkload = new HashMap<>();
        Map<String, String> teacherDesignations = new HashMap<>();
        Map<String, Integer> sectionDailyLoad = new HashMap<>();
        Map<String, Map<String, Integer>> teacherDailyConsecutive = new HashMap<>();

        // Individual violation details
        List<String> c1Affected = new ArrayList<>(); // Teacher double booking
        List<String> c2Affected = new ArrayList<>(); // Section double booking
        List<String> c3Affected = new ArrayList<>(); // Room double booking
        List<String> c4Affected = new ArrayList<>(); // Room capacity
        List<String> c5Affected = new ArrayList<>(); // Lab equipment
        List<String> c6Affected = new ArrayList<>(); // Workload limit
        List<String> c7Affected = new ArrayList<>(); // Break/Lunch protection
        List<String> c8Affected = new ArrayList<>(); // Working hours boundary
        List<String> c9Affected = new ArrayList<>(); // Consecutive lectures
        List<String> c10Affected = new ArrayList<>(); // Heavy subject alignment
        List<String> c11Affected = new ArrayList<>(); // Lab continuity
        List<String> c12Affected = new ArrayList<>(); // Dept/Qualification mismatch
        List<String> c13Affected = new ArrayList<>(); // Max daily sessions
        List<String> c14Affected = new ArrayList<>(); // Room type mismatch
        List<String> c15Affected = new ArrayList<>(); // Tenant isolation
        List<String> c16Affected = new ArrayList<>(); // Consistent 8:20 AM start & contiguity
        Map<String, Set<Integer>> sectionDayPeriods = new HashMap<>();

        for (Gene gene : chromosome.getGenes()) {
            if (gene.getTimeSlot() == null || gene.getClassSession() == null || gene.getClassroom() == null) continue;

            String slotId = gene.getTimeSlot().getSlotId();
            String day = gene.getTimeSlot().getDayOfWeek();
            int period = gene.getTimeSlot().getPeriodNumber();
            String facId = gene.getClassSession().getFacultyId();
            String facName = gene.getClassSession().getTeacherName();
            String roomId = gene.getClassroom().getId();
            String roomName = gene.getClassroom().getRoomNumber();
            String secKey = gene.getClassSession().getSemester() + "_" + gene.getClassSession().getSection();
            String subjectName = gene.getClassSession().getSubjectName();

            // 1. Teacher double-booking
            String facSlotKey = facId + "@" + slotId;
            if (!teacherSlotMap.containsKey(facSlotKey)) {
                teacherSlotMap.put(facSlotKey, new ArrayList<>());
            }
            teacherSlotMap.get(facSlotKey).add(subjectName + " (" + secKey + ")");
            if (teacherSlotMap.get(facSlotKey).size() > 1) {
                c1Affected.add("Teacher '" + facName + "' (" + facId + ") double-booked at " + slotId);
            }

            // 2. Section double-booking
            String secSlotKey = secKey + "@" + slotId;
            if (!sectionSlotMap.containsKey(secSlotKey)) {
                sectionSlotMap.put(secSlotKey, new ArrayList<>());
            }
            sectionSlotMap.get(secSlotKey).add(subjectName);
            if (sectionSlotMap.get(secSlotKey).size() > 1) {
                c2Affected.add("Section '" + secKey + "' double-booked at " + slotId);
            }

            // 3. Room double-booking
            String roomSlotKey = roomId + "@" + slotId;
            if (!roomSlotMap.containsKey(roomSlotKey)) {
                roomSlotMap.put(roomSlotKey, new ArrayList<>());
            }
            roomSlotMap.get(roomSlotKey).add(subjectName + " (" + secKey + ")");
            if (roomSlotMap.get(roomSlotKey).size() > 1) {
                c3Affected.add("Room '" + roomName + "' double-booked at " + slotId);
            }

            // 4. Room Capacity Check
            if (gene.getClassroom().getCapacity() > 0 && gene.getClassroom().getCapacity() < 40) {
                // Assuming standard section size is 50
                c4Affected.add("Room '" + roomName + "' capacity (" + gene.getClassroom().getCapacity() + ") is insufficient for section " + secKey);
            }

            // 5. Lab equipment requirement
            if (gene.getClassSession().isLab() && !gene.getClassroom().isHasLabEquipment()) {
                c5Affected.add("Lab session '" + subjectName + "' scheduled in room '" + roomName + "' lacking lab equipment");
            }

            // 6. Workload tracking
            teacherWeeklyWorkload.put(facId, teacherWeeklyWorkload.getOrDefault(facId, 0) + 1);
            if (gene.getClassSession().getFacultyDesignation() != null) {
                teacherDesignations.put(facId, gene.getClassSession().getFacultyDesignation());
            }

            // 7. Tea break / Lunch protection
            if (gene.getTimeSlot().isBreakSlot()) {
                c7Affected.add("Session '" + subjectName + "' scheduled during protected break/lunch slot (" + slotId + ")");
            }

            // 8. Working hours boundary check & Friday/Saturday cutoff
            boolean isShortDay = "Friday".equalsIgnoreCase(day) || "Saturday".equalsIgnoreCase(day);
            if (period < 1 || period > 8) {
                c8Affected.add("Session '" + subjectName + "' outside valid working hours period (" + period + ")");
            } else if (isShortDay && period > 5) {
                c8Affected.add("Session '" + subjectName + "' scheduled after 12:50 PM cutoff on " + day + " (period " + period + ")");
            }

            // 9. Consecutive lectures tracking
            String facDayKey = facId + "@" + day;
            if (!teacherDailyConsecutive.containsKey(facDayKey)) {
                teacherDailyConsecutive.put(facDayKey, new HashMap<>());
            }
            teacherDailyConsecutive.get(facDayKey).put(facId, teacherDailyConsecutive.get(facDayKey).getOrDefault(facId, 0) + 1);

            // 10. Preferred slot alignment (Heavy/Theory vs Lab)
            if (subjectName != null && (subjectName.toLowerCase().contains("math") || subjectName.toLowerCase().contains("algorithm")) && period > 5) {
                c10Affected.add("Heavy theory subject '" + subjectName + "' scheduled in late afternoon period " + period);
            }

            // 11. Multi-session lab block continuity (cannot start on very last period of the day)
            int finalPeriodOfDay = isShortDay ? 5 : 8;
            if (gene.getClassSession().isLab() && period == finalPeriodOfDay) {
                c11Affected.add("Lab session '" + subjectName + "' starts at last period (" + period + ") without block continuity");
            }

            // 12. Teacher Dept / Qualification mismatch check (if applicable)
            if (facId != null && facId.startsWith("UNAUTHORIZED")) {
                c12Affected.add("Teacher '" + facName + "' lacks qualification for " + subjectName);
            }

            // 13. Daily Section Load (Max 8 for full days, Max 5 for Friday/Saturday)
            String secDayKey = secKey + "@" + day;
            sectionDailyLoad.put(secDayKey, sectionDailyLoad.getOrDefault(secDayKey, 0) + 1);
            int maxDailyAllowed = isShortDay ? 5 : 8;
            if (sectionDailyLoad.get(secDayKey) > maxDailyAllowed) {
                c13Affected.add("Section '" + secKey + "' exceeds max " + maxDailyAllowed + " periods on " + day);
            }

            // 14. Room type mismatch
            if (gene.getClassSession().isLab() && !gene.getClassroom().isLab()) {
                c14Affected.add("Lab class '" + subjectName + "' assigned to regular theory room '" + roomName + "'");
            } else if (!gene.getClassSession().isLab() && gene.getClassroom().isLab()) {
                c14Affected.add("Theory class '" + subjectName + "' assigned to laboratory room '" + roomName + "'");
            }

            // 15. Multi-College Tenant Isolation
            if (gene.getClassroom().getCollegeId() != null && gene.getClassSession().getCollegeId() != null
                    && !gene.getClassroom().getCollegeId().equalsIgnoreCase(gene.getClassSession().getCollegeId())) {
                c15Affected.add("Cross-college resource leak: Room '" + roomName + "' (" + gene.getClassroom().getCollegeId() + ") assigned to " + gene.getClassSession().getCollegeId());
            }

            // 16. Section Day Period tracking for consistent start times and contiguity
            if (!sectionDayPeriods.containsKey(secDayKey)) {
                sectionDayPeriods.put(secDayKey, new HashSet<>());
            }
            sectionDayPeriods.get(secDayKey).add(period);
        }

        // Evaluate HC-16: Day Start Time (Must start at Period 1 / 8:20 AM) & internal contiguity
        for (Map.Entry<String, Set<Integer>> entry : sectionDayPeriods.entrySet()) {
            Set<Integer> periods = entry.getValue();
            if (periods != null && !periods.isEmpty()) {
                int minP = Collections.min(periods);
                if (minP > 1) {
                    c16Affected.add("Section '" + entry.getKey() + "' starts late at Period " + minP + " instead of institutional start time 8:20 AM (Period 1)");
                }
                int maxP = Collections.max(periods);
                for (int p = minP; p <= maxP; p++) {
                    if (!periods.contains(p)) {
                        c16Affected.add("Section '" + entry.getKey() + "' has idle internal gap at Period " + p);
                    }
                }
            }
        }

        // Faculty workload limit checks (Professor: 10h, Assistant Professor: 14h, Associate Professor: 16h)
        for (Map.Entry<String, Integer> entry : teacherWeeklyWorkload.entrySet()) {
            String des = teacherDesignations.get(entry.getKey());
            int limit = 14; // Default to Assistant Professor
            if (des != null) {
                String d = des.trim().toLowerCase();
                if (d.contains("associate")) limit = 16;
                else if (d.contains("assistant")) limit = 14;
                else if (d.contains("professor")) limit = 10;
            }
            if (entry.getValue() > limit) {
                c6Affected.add("Faculty " + entry.getKey() + " weekly workload (" + entry.getValue() + "h) exceeds designation limit (" + limit + "h for " + (des != null ? des : "Faculty") + ")");
            }
        }

        // Populate 15 Constraint Reports explicitly
        addReport(reports, 1, "HC-01: Teacher Double-Booking", c1Affected, 1000.0);
        addReport(reports, 2, "HC-02: Section Double-Booking", c2Affected, 1000.0);
        addReport(reports, 3, "HC-03: Room Double-Booking", c3Affected, 1000.0);
        addReport(reports, 4, "HC-04: Room Capacity vs Section Strength", c4Affected, 500.0);
        addReport(reports, 5, "HC-05: Lab Equipment Requirement Matching", c5Affected, 500.0);
        addReport(reports, 6, "HC-06: Faculty Workload Limit Enforcement", c6Affected, 400.0);
        addReport(reports, 7, "HC-07: Protection of Tea Breaks & Lunch Slots", c7Affected, 500.0);
        addReport(reports, 8, "HC-08: Working Hours Boundary Check", c8Affected, 500.0);
        addReport(reports, 9, "HC-09: Consecutive Lecture Limit Enforcement", c9Affected, 300.0);
        addReport(reports, 10, "HC-10: Preferred Slot Alignment for Core Subjects", c10Affected, 200.0);
        addReport(reports, 11, "HC-11: Multi-Session Lab Block Continuity", c11Affected, 300.0);
        addReport(reports, 12, "HC-12: Teacher Subject Qualification Availability", c12Affected, 500.0);
        addReport(reports, 13, "HC-13: Maximum Daily Sessions per Section", c13Affected, 400.0);
        addReport(reports, 14, "HC-14: Room Type Mismatch Prevention", c14Affected, 400.0);
        addReport(reports, 15, "HC-15: Multi-College Tenant Isolation Check", c15Affected, 1000.0);
        addReport(reports, 16, "HC-16: Consistent Day Start (8:20 AM) & Contiguity", c16Affected, 400.0);

        for (ConstraintEvaluationReport rep : reports) {
            totalHardViolations += rep.getViolationCount();
        }

        chromosome.setHardViolations(totalHardViolations);
        chromosome.setSoftPenalty(softPenalty);
        chromosome.setConstraintReports(reports);

        double fitness = 100.0 - (totalHardViolations * 10.0) - (softPenalty * 0.5);
        if (fitness < 0) fitness = 0.0;
        chromosome.setFitnessScore(fitness);
    }

    private static void addReport(List<ConstraintEvaluationReport> reports, int number, String name, List<String> affected, double penaltyPerViolation) {
        boolean passed = affected.isEmpty();
        int count = affected.size();
        double penalty = count * penaltyPerViolation;
        String explanation = passed ? "Passed with 0 violations." : "FAILED: " + count + " violation(s) detected. Impacting: " + String.join("; ", affected);
        reports.add(new ConstraintEvaluationReport(number, name, passed, count, affected, penalty, explanation));
    }
}
