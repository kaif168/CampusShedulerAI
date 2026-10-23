package com.campusscheduler.ai.engine;

import com.campusscheduler.ai.models.ClassAssignment;
import com.campusscheduler.ai.models.ClassSession;
import com.campusscheduler.ai.models.Classroom;
import com.campusscheduler.ai.models.Laboratory;
import com.campusscheduler.ai.models.Section;
import com.campusscheduler.ai.models.Subject;
import com.campusscheduler.ai.models.TimeSlot;
import com.campusscheduler.ai.models.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class GeneticAlgorithm {
    private final int populationSize;
    private final int maxGenerations;
    private final double mutationRate;
    private final Random random = new Random();

    public GeneticAlgorithm() {
        this(50, 100, 0.05);
    }

    public GeneticAlgorithm(int populationSize, int maxGenerations, double mutationRate) {
        this.populationSize = populationSize;
        this.maxGenerations = maxGenerations;
        this.mutationRate = mutationRate;
    }

    public Chromosome run(List<ClassAssignment> assignmentList, List<User> facultyList, List<Classroom> classroomList, List<Laboratory> labList, List<Section> sectionList, List<Subject> subjectList) {
        return run(assignmentList, facultyList, classroomList, labList, sectionList, subjectList, null);
    }

    public Chromosome run(List<ClassAssignment> assignmentList, List<User> facultyList, List<Classroom> classroomList, List<Laboratory> labList, List<Section> sectionList, List<Subject> subjectList, List<String> workingDays) {
        return run(assignmentList, facultyList, classroomList, labList, sectionList, subjectList, workingDays, null);
    }

    public Chromosome run(List<ClassAssignment> assignmentList, List<User> facultyList, List<Classroom> classroomList, List<Laboratory> labList, List<Section> sectionList, List<Subject> subjectList, List<String> workingDays, List<ClassSession> existingAllocations) {
        // Map faculty designations for workload limit checking
        Map<String, String> facultyDesignations = new HashMap<>();
        if (facultyList != null) {
            for (User u : facultyList) {
                if (u != null && u.getUid() != null) {
                    facultyDesignations.put(u.getUid(), u.getDesignation() != null ? u.getDesignation() : "Assistant Professor");
                }
            }
        }

        // Configure time slots dynamically based on selected working days
        List<TimeSlot> slots = new ArrayList<>();
        List<String> days = (workingDays != null && !workingDays.isEmpty())
                ? workingDays
                : Arrays.asList("Monday", "Tuesday", "Wednesday", "Thursday", "Friday");

        // Official 8-period regular schedule (Monday-Thursday)
        // Protected breaks: 09:00 AM - 09:20 AM (Short Break) & 12:50 PM - 01:40 PM (Lunch Break)
        String[] regularTimes = {
                "08:20 AM - 09:00 AM", // Period 1 (40 min)
                "09:20 AM - 10:10 AM", // Period 2 (50 min)
                "10:10 AM - 11:00 AM", // Period 3 (50 min)
                "11:00 AM - 11:55 AM", // Period 4 (55 min)
                "11:55 AM - 12:50 PM", // Period 5 (55 min)
                "01:40 PM - 02:30 PM", // Period 6 (50 min)
                "02:30 PM - 03:20 PM", // Period 7 (50 min)
                "03:20 PM - 04:10 PM"  // Period 8 (50 min)
        };

        // Friday and Saturday half-day schedule (Periods 1-5 strictly ending by 12:50 PM)
        String[] fridayTimes = {
                "08:20 AM - 09:00 AM", // Period 1
                "09:20 AM - 10:10 AM", // Period 2
                "10:10 AM - 11:00 AM", // Period 3
                "11:00 AM - 11:55 AM", // Period 4
                "11:55 AM - 12:50 PM"  // Period 5
        };

        for (String day : days) {
            boolean isShortDay = "Friday".equalsIgnoreCase(day) || "Saturday".equalsIgnoreCase(day);
            String[] times = isShortDay ? fridayTimes : regularTimes;
            int p = 1;
            for (String time : times) {
                String[] parts = time.split(" - ");
                slots.add(new TimeSlot("SLOT_" + day + "_" + p, day, p, parts[0], parts.length > 1 ? parts[1] : parts[0]));
                p++;
            }
        }

        // Prepare classrooms and laboratories ensuring Room 201 is Classroom and 202-208, 305-308 are Labs
        List<Classroom> rooms = new ArrayList<>();
        if (classroomList != null) {
            for (Classroom cr : classroomList) {
                if (cr != null) {
                    // Room 201 is strictly a classroom, not a laboratory
                    if ("201".equals(cr.getRoomNumber()) || "Room 201".equalsIgnoreCase(cr.getRoomNumber())) {
                        cr.setHasLabEquipment(false);
                    }
                    rooms.add(cr);
                }
            }
        }
        if (labList != null) {
            for (Laboratory lab : labList) {
                if (lab != null) {
                    Classroom labRoom = new Classroom(
                            lab.getId(),
                            lab.getCollegeId(),
                            lab.getRoomNumber() != null ? lab.getRoomNumber() : (lab.getLabName() != null ? lab.getLabName() : "Lab"),
                            "Lab Block",
                            lab.getCapacity() > 0 ? lab.getCapacity() : 30,
                            true,
                            true
                    );
                    rooms.add(labRoom);
                }
            }
        }

        // Default inventory if empty
        if (rooms.isEmpty()) {
            String[] classRoomNums = {"101", "102", "103", "104", "105", "106", "107", "108", "201", "301", "302", "303", "304"};
            for (String rNum : classRoomNums) {
                rooms.add(new Classroom("CR_" + rNum, "COLLEGE_01", rNum, "A Block", 60, true, false));
            }
            String[] labNums = {"202", "203", "204", "205", "206", "207", "208", "305", "306", "307", "308"};
            for (String lNum : labNums) {
                rooms.add(new Classroom("LAB_" + lNum, "COLLEGE_01", lNum, "Lab Block", 35, true, true));
            }
        }

        // Match existing approved allocations to preserve them
        List<Gene> lockedGenes = new ArrayList<>();
        Map<String, Integer> retainedPerSubject = new HashMap<>();
        if (existingAllocations != null && !existingAllocations.isEmpty()) {
            for (ClassSession ea : existingAllocations) {
                if (ea == null) continue;
                TimeSlot matchedSlot = null;
                for (TimeSlot ts : slots) {
                    if (ts != null && ts.getDayOfWeek() != null && ts.getDayOfWeek().equalsIgnoreCase(ea.getDayOfWeek())
                            && ts.getStartTime() != null && ts.getStartTime().equalsIgnoreCase(ea.getStartTime())) {
                        matchedSlot = ts;
                        break;
                    }
                }
                if (matchedSlot == null && !slots.isEmpty()) {
                    matchedSlot = slots.get(0);
                }
                Classroom matchedRoom = null;
                for (Classroom cr : rooms) {
                    if (cr != null && cr.getRoomNumber() != null && (cr.getRoomNumber().equalsIgnoreCase(ea.getRoomNumber()) || cr.getId().equalsIgnoreCase(ea.getClassroomId()))) {
                        matchedRoom = cr;
                        break;
                    }
                }
                if (matchedRoom == null && !rooms.isEmpty()) {
                    matchedRoom = rooms.get(0);
                }
                if (matchedSlot != null && matchedRoom != null) {
                    if (ea.getFacultyDesignation() == null && facultyDesignations.containsKey(ea.getFacultyId())) {
                        ea.setFacultyDesignation(facultyDesignations.get(ea.getFacultyId()));
                    }
                    lockedGenes.add(new Gene(matchedSlot, ea, matchedRoom, true));
                    String subKey = ea.getSubjectId() != null ? ea.getSubjectId() : ea.getSubjectName();
                    retainedPerSubject.put(subKey, retainedPerSubject.getOrDefault(subKey, 0) + 1);
                }
            }
        }

        List<ClassSession> sessions = new ArrayList<>();
        int count = 1;

        // Generate sessions for remaining hours per assignment based on weekly hours or subject credits
        for (ClassAssignment ca : assignmentList) {
            if (ca == null) continue;
            boolean isLab = "Lab".equalsIgnoreCase(ca.getClassType());
            int requiredSessions = ca.getWeeklyHours();
            if (requiredSessions <= 0 && subjectList != null) {
                for (Subject s : subjectList) {
                    if (s != null && s.getId() != null && s.getId().equals(ca.getSubjectId())) {
                        requiredSessions = s.getWeeklyHoursRequired() > 0 ? s.getWeeklyHoursRequired() : s.getCredits();
                        break;
                    }
                }
            }
            if (requiredSessions <= 0) {
                requiredSessions = isLab ? 2 : 4;
            }

            String subKey = ca.getSubjectId() != null ? ca.getSubjectId() : ca.getSubjectName();
            int alreadyRetained = retainedPerSubject.getOrDefault(subKey, 0);
            int neededSessions = Math.max(0, requiredSessions - alreadyRetained);

            for (int sessIdx = 1; sessIdx <= neededSessions; sessIdx++) {
                ClassSession cs = new ClassSession(
                        "SESS_" + count++,
                        ca.getId(),
                        ca.getSubjectId(),
                        ca.getSubjectName(),
                        ca.getSubjectName(),
                        ca.getFacultyId(),
                        ca.getTeacherName(),
                        ca.getSemester(),
                        ca.getSection(),
                        isLab
                );
                cs.setCollegeId(ca.getCollegeId());
                cs.setFacultyDesignation(facultyDesignations.getOrDefault(ca.getFacultyId(), "Assistant Professor"));
                sessions.add(cs);
            }
        }

        return generateTimetable(sessions, slots, rooms, lockedGenes);
    }

    public Chromosome generateTimetable(List<ClassSession> sessions, List<TimeSlot> slots, List<Classroom> rooms) {
        return generateTimetable(sessions, slots, rooms, null);
    }

    public Chromosome generateTimetable(List<ClassSession> sessions, List<TimeSlot> slots, List<Classroom> rooms, List<Gene> lockedGenes) {
        if ((sessions.isEmpty() && (lockedGenes == null || lockedGenes.isEmpty())) || slots.isEmpty() || rooms.isEmpty()) {
            return new Chromosome();
        }

        List<Classroom> labRooms = new ArrayList<>();
        List<Classroom> lectureRooms = new ArrayList<>();
        for (Classroom r : rooms) {
            if (r.isHasLabEquipment()) {
                labRooms.add(r);
            } else {
                lectureRooms.add(r);
            }
        }
        if (labRooms.isEmpty()) labRooms.addAll(rooms);
        if (lectureRooms.isEmpty()) lectureRooms.addAll(rooms);

        int retainedCount = lockedGenes != null ? lockedGenes.size() : 0;
        int newSessionsCount = sessions.size();

        // Organize slots by day, sorted chronologically from Period 1 (08:20 AM) onwards
        Map<String, List<TimeSlot>> dayToSlots = new java.util.LinkedHashMap<>();
        for (TimeSlot s : slots) {
            dayToSlots.computeIfAbsent(s.getDayOfWeek(), k -> new ArrayList<>()).add(s);
        }
        for (List<TimeSlot> dSlots : dayToSlots.values()) {
            Collections.sort(dSlots, (a, b) -> Integer.compare(a.getPeriodNumber(), b.getPeriodNumber()));
        }
        List<String> dayKeys = new ArrayList<>(dayToSlots.keySet());

        List<Chromosome> population = new ArrayList<>();
        for (int i = 0; i < populationSize; i++) {
            List<Gene> genes = new ArrayList<>();
            // Retain approved locked allocations
            Map<String, Integer> dayUsage = new HashMap<>();
            if (lockedGenes != null) {
                for (Gene lg : lockedGenes) {
                    genes.add(new Gene(lg.getTimeSlot(), lg.getClassSession(), lg.getClassroom(), true));
                    if (lg.getTimeSlot() != null && lg.getTimeSlot().getDayOfWeek() != null) {
                        String d = lg.getTimeSlot().getDayOfWeek();
                        int p = lg.getTimeSlot().getPeriodNumber();
                        dayUsage.put(d, Math.max(dayUsage.getOrDefault(d, 0), p));
                    }
                }
            }

            // Allocate remaining sessions starting sequentially from Period 1 (08:20 AM)
            int dayIdx = i % Math.max(1, dayKeys.size());
            for (ClassSession session : sessions) {
                String targetDay = dayKeys.get(dayIdx % dayKeys.size());
                List<TimeSlot> dSlots = dayToSlots.get(targetDay);
                int nextP = dayUsage.getOrDefault(targetDay, 0);

                TimeSlot slot;
                // 80% of chromosomes seed contiguously from 8:20 AM, 20% explore randomly for diversity
                if (dSlots != null && nextP < dSlots.size() && (i == 0 || random.nextDouble() < 0.80)) {
                    slot = dSlots.get(nextP);
                    dayUsage.put(targetDay, nextP + 1);
                } else {
                    slot = slots.get(random.nextInt(slots.size()));
                }

                List<Classroom> pool = session.isLab() ? labRooms : lectureRooms;
                Classroom room = pool.get(random.nextInt(pool.size()));
                genes.add(new Gene(slot, session, room, false));

                dayIdx++;
            }
            Chromosome chrom = new Chromosome(genes);
            chrom.setRetainedCount(retainedCount);
            chrom.setNewAllocationsCount(newSessionsCount);
            ConstraintChecker.evaluateFitness(chrom);
            population.add(chrom);
        }

        for (int gen = 0; gen < maxGenerations; gen++) {
            Collections.sort(population);

            if (population.get(0).getFitnessScore() >= 95.0 && population.get(0).getHardViolations() == 0) {
                break;
            }

            List<Chromosome> nextGen = new ArrayList<>();
            nextGen.add(population.get(0));

            while (nextGen.size() < populationSize) {
                Chromosome parent1 = tournamentSelect(population);
                Chromosome parent2 = tournamentSelect(population);
                Chromosome child = crossover(parent1, parent2);

                if (random.nextDouble() < mutationRate) {
                    mutate(child, slots, rooms);
                    RepairEngine.repair(child, slots, rooms);
                } else {
                    ConstraintChecker.evaluateFitness(child);
                }

                nextGen.add(child);
            }
            population = nextGen;
        }

        Collections.sort(population);
        return population.get(0);
    }

    private Chromosome tournamentSelect(List<Chromosome> population) {
        Chromosome best = null;
        for (int i = 0; i < 3; i++) {
            Chromosome competitor = population.get(random.nextInt(population.size()));
            if (best == null || competitor.getFitnessScore() > best.getFitnessScore()) {
                best = competitor;
            }
        }
        return best;
    }

    private Chromosome crossover(Chromosome p1, Chromosome p2) {
        List<Gene> childGenes = new ArrayList<>();
        int point = random.nextInt(p1.getGenes().size());
        for (int i = 0; i < p1.getGenes().size(); i++) {
            if (i < point) {
                childGenes.add(p1.getGenes().get(i));
            } else {
                childGenes.add(p2.getGenes().get(i));
            }
        }
        return new Chromosome(childGenes);
    }

    private void mutate(Chromosome c, List<TimeSlot> slots, List<Classroom> rooms) {
        if (c.getGenes().isEmpty()) return;
        int idx = random.nextInt(c.getGenes().size());
        Gene gene = c.getGenes().get(idx);
        if (gene.isRetained()) return;
        gene.setTimeSlot(slots.get(random.nextInt(slots.size())));

        boolean isLab = gene.getClassSession() != null && gene.getClassSession().isLab();
        List<Classroom> candidates = new ArrayList<>();
        for (Classroom r : rooms) {
            if (isLab ? r.isHasLabEquipment() : !r.isHasLabEquipment()) {
                candidates.add(r);
            }
        }
        if (candidates.isEmpty()) candidates = rooms;
        gene.setClassroom(candidates.get(random.nextInt(candidates.size())));
    }
}
