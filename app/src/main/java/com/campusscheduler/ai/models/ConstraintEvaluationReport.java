package com.campusscheduler.ai.models;

import java.util.ArrayList;
import java.util.List;

public class ConstraintEvaluationReport {
    private int constraintNumber;
    private String constraintName;
    private boolean isPassed;
    private int violationCount;
    private List<String> affectedEntities;
    private double calculatedPenalty;
    private String explanation;

    public ConstraintEvaluationReport() {
        this.affectedEntities = new ArrayList<>();
    }

    public ConstraintEvaluationReport(int constraintNumber, String constraintName, boolean isPassed, int violationCount, List<String> affectedEntities, double calculatedPenalty, String explanation) {
        this.constraintNumber = constraintNumber;
        this.constraintName = constraintName;
        this.isPassed = isPassed;
        this.violationCount = violationCount;
        this.affectedEntities = affectedEntities != null ? affectedEntities : new ArrayList<>();
        this.calculatedPenalty = calculatedPenalty;
        this.explanation = explanation;
    }

    public int getConstraintNumber() {
        return constraintNumber;
    }

    public void setConstraintNumber(int constraintNumber) {
        this.constraintNumber = constraintNumber;
    }

    public String getConstraintName() {
        return constraintName;
    }

    public void setConstraintName(String constraintName) {
        this.constraintName = constraintName;
    }

    public boolean isPassed() {
        return isPassed;
    }

    public void setPassed(boolean passed) {
        isPassed = passed;
    }

    public int getViolationCount() {
        return violationCount;
    }

    public void setViolationCount(int violationCount) {
        this.violationCount = violationCount;
    }

    public List<String> getAffectedEntities() {
        return affectedEntities;
    }

    public void setAffectedEntities(List<String> affectedEntities) {
        this.affectedEntities = affectedEntities;
    }

    public double getCalculatedPenalty() {
        return calculatedPenalty;
    }

    public void setCalculatedPenalty(double calculatedPenalty) {
        this.calculatedPenalty = calculatedPenalty;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }
}
