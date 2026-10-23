package com.campusscheduler.ai.engine;

import java.util.ArrayList;
import java.util.List;

import com.campusscheduler.ai.models.ConstraintEvaluationReport;

public class Chromosome implements Comparable<Chromosome> {
    private List<Gene> genes;
    private double fitnessScore;
    private int hardViolations;
    private int softPenalty;
    private int retainedCount = 0;
    private int newAllocationsCount = 0;
    private int changedCount = 0;
    private List<ConstraintEvaluationReport> constraintReports = new ArrayList<>();

    public Chromosome() {
        this.genes = new ArrayList<>();
        this.fitnessScore = 0.0;
        this.hardViolations = 0;
        this.softPenalty = 0;
    }

    public Chromosome(List<Gene> genes) {
        this.genes = genes;
        this.fitnessScore = 0.0;
        this.hardViolations = 0;
        this.softPenalty = 0;
    }

    public List<Gene> getGenes() { return genes; }
    public void setGenes(List<Gene> genes) { this.genes = genes; }

    public double getFitnessScore() { return fitnessScore; }
    public void setFitnessScore(double fitnessScore) { this.fitnessScore = fitnessScore; }

    public double getFitness() { return fitnessScore; }

    public int getHardViolations() { return hardViolations; }
    public void setHardViolations(int hardViolations) { this.hardViolations = hardViolations; }

    public int getHardConflictsCount() { return hardViolations; }

    public int getSoftPenalty() { return softPenalty; }
    public void setSoftPenalty(int softPenalty) { this.softPenalty = softPenalty; }

    public int getRetainedCount() { return retainedCount; }
    public void setRetainedCount(int retainedCount) { this.retainedCount = retainedCount; }

    public int getNewAllocationsCount() { return newAllocationsCount; }
    public void setNewAllocationsCount(int newAllocationsCount) { this.newAllocationsCount = newAllocationsCount; }

    public int getChangedCount() { return changedCount; }
    public void setChangedCount(int changedCount) { this.changedCount = changedCount; }

    public List<ConstraintEvaluationReport> getConstraintReports() { return constraintReports; }
    public void setConstraintReports(List<ConstraintEvaluationReport> constraintReports) { this.constraintReports = constraintReports; }

    @Override
    public int compareTo(Chromosome o) {
        return Double.compare(o.fitnessScore, this.fitnessScore);
    }
}
