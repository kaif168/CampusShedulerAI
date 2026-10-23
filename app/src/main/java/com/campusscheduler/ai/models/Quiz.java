package com.campusscheduler.ai.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Quiz implements Serializable {
    private String id;
    private String quizId;
    private String collegeId;
    private String departmentId;
    private String subjectId;
    private String subjectName;
    private String title;
    private int durationMinutes;
    private String deadline;
    private String targetAudience; // "ALL", "SECTION", "STUDENT"
    private String targetValue;    // e.g. section "A" or student USN
    private int totalMarks;
    private String createdBy;
    private List<QuizQuestion> questions;

    public Quiz() {
        this.questions = new ArrayList<>();
    }

    public Quiz(String id, String collegeId, String departmentId, String subjectId, String title, int durationMinutes) {
        this.id = id;
        this.quizId = id;
        this.collegeId = collegeId;
        this.departmentId = departmentId;
        this.subjectId = subjectId;
        this.title = title;
        this.durationMinutes = durationMinutes;
        this.questions = new ArrayList<>();
    }

    public String getId() { return id != null ? id : quizId; }
    public void setId(String id) { this.id = id; this.quizId = id; }

    public String getQuizId() { return quizId != null ? quizId : id; }
    public void setQuizId(String quizId) { this.quizId = quizId; this.id = quizId; }

    public String getCollegeId() { return collegeId; }
    public void setCollegeId(String collegeId) { this.collegeId = collegeId; }

    public String getDepartmentId() { return departmentId; }
    public void setDepartmentId(String departmentId) { this.departmentId = departmentId; }

    public String getSubjectId() { return subjectId; }
    public void setSubjectId(String subjectId) { this.subjectId = subjectId; }

    public String getSubjectName() { return subjectName != null ? subjectName : "Subject Quiz"; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    public String getDeadline() { return deadline != null ? deadline : ""; }
    public void setDeadline(String deadline) { this.deadline = deadline; }

    public String getTargetAudience() { return targetAudience != null ? targetAudience : "ALL"; }
    public void setTargetAudience(String targetAudience) { this.targetAudience = targetAudience; }

    public String getTargetValue() { return targetValue != null ? targetValue : ""; }
    public void setTargetValue(String targetValue) { this.targetValue = targetValue; }

    public int getTotalMarks() { return totalMarks; }
    public void setTotalMarks(int totalMarks) { this.totalMarks = totalMarks; }

    public String getCreatedBy() { return createdBy != null ? createdBy : ""; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public List<QuizQuestion> getQuestions() { return questions; }
    public void setQuestions(List<QuizQuestion> questions) { this.questions = questions; }
}
