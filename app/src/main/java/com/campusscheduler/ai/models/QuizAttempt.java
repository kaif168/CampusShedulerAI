package com.campusscheduler.ai.models;

import java.io.Serializable;

public class QuizAttempt implements Serializable {
    private String id;
    private String quizId;
    private String studentId;
    private int score;
    private int totalQuestions;
    private long completedAt;

    public QuizAttempt() {}

    public QuizAttempt(String id, String quizId, String studentId, int score, int totalQuestions) {
        this.id = id;
        this.quizId = quizId;
        this.studentId = studentId;
        this.score = score;
        this.totalQuestions = totalQuestions;
        this.completedAt = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getQuizId() { return quizId; }
    public void setQuizId(String quizId) { this.quizId = quizId; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public int getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }

    public long getCompletedAt() { return completedAt; }
    public void setCompletedAt(long completedAt) { this.completedAt = completedAt; }
}
