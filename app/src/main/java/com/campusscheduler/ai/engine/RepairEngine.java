package com.campusscheduler.ai.engine;

import com.campusscheduler.ai.models.Classroom;
import com.campusscheduler.ai.models.TimeSlot;

import java.util.List;

public class RepairEngine {

    private static final int MAX_REPAIR_ROUNDS = 3;

    public static void repair(
            Chromosome chromosome,
            List<TimeSlot> availableSlots,
            List<Classroom> availableRooms) {

        if (chromosome == null || chromosome.getGenes() == null) {
            return;
        }

        if ((availableSlots == null || availableSlots.isEmpty())
                && (availableRooms == null || availableRooms.isEmpty())) {
            return;
        }

        ConstraintChecker.evaluateFitness(chromosome);

        if (chromosome.getHardViolations() == 0) {
            return;
        }

        for (int round = 0; round < MAX_REPAIR_ROUNDS; round++) {

            boolean improved = false;

            for (Gene gene : chromosome.getGenes()) {

                if (gene == null || gene.isRetained()) {
                    continue;
                }

                int beforeHard = chromosome.getHardViolations();
                double beforeFitness = chromosome.getFitness();

                // -----------------------------
                // Try TimeSlot repairs
                // -----------------------------
                if (availableSlots != null) {

                    for (TimeSlot candidate : availableSlots) {

                        if (candidate == null || sameSlot(candidate, gene.getTimeSlot())) {
                            continue;
                        }

                        TimeSlot original = gene.getTimeSlot();

                        gene.setTimeSlot(candidate);
                        ConstraintChecker.evaluateFitness(chromosome);

                        if (isBetter(
                                chromosome.getHardViolations(),
                                chromosome.getFitness(),
                                beforeHard,
                                beforeFitness)) {

                            beforeHard = chromosome.getHardViolations();
                            beforeFitness = chromosome.getFitness();
                            improved = true;

                        } else {
                            gene.setTimeSlot(original);
                            ConstraintChecker.evaluateFitness(chromosome);
                        }

                        if (chromosome.getHardViolations() == 0) {
                            return;
                        }
                    }
                }

                // -----------------------------
                // Try Classroom repairs
                // -----------------------------
                if (availableRooms != null) {

                    for (Classroom candidate : availableRooms) {

                        if (candidate == null || !candidate.isAvailable()) {
                            continue;
                        }

                        if (sameRoom(candidate, gene.getClassroom())) {
                            continue;
                        }

                        Classroom original = gene.getClassroom();

                        gene.setClassroom(candidate);
                        ConstraintChecker.evaluateFitness(chromosome);

                        if (isBetter(
                                chromosome.getHardViolations(),
                                chromosome.getFitness(),
                                beforeHard,
                                beforeFitness)) {

                            beforeHard = chromosome.getHardViolations();
                            beforeFitness = chromosome.getFitness();
                            improved = true;

                        } else {
                            gene.setClassroom(original);
                            ConstraintChecker.evaluateFitness(chromosome);
                        }

                        if (chromosome.getHardViolations() == 0) {
                            return;
                        }
                    }
                }
            }

            if (!improved) {
                break;
            }
        }

        ConstraintChecker.evaluateFitness(chromosome);
    }

    private static boolean isBetter(
            int candidateHard,
            double candidateFitness,
            int currentHard,
            double currentFitness) {

        if (candidateHard < currentHard) {
            return true;
        }

        return candidateHard == currentHard
                && candidateFitness > currentFitness;
    }

    private static boolean sameSlot(TimeSlot a, TimeSlot b) {

        if (a == null || b == null) {
            return a == b;
        }

        if (a.getSlotId() != null && b.getSlotId() != null) {
            return a.getSlotId().equals(b.getSlotId());
        }

        return a == b;
    }

    private static boolean sameRoom(Classroom a, Classroom b) {

        if (a == null || b == null) {
            return a == b;
        }

        if (a.getId() != null && b.getId() != null) {
            return a.getId().equals(b.getId());
        }

        return a == b;
    }
}