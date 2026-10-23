package com.campusscheduler.ai.models;

import java.io.Serializable;
import java.util.List;

public class QuizQuestion implements Serializable {
    private String questionText;
    private List<String> options;
    private int correctOptionIndex;
    private int marks = 1;

    public QuizQuestion() {}

    public QuizQuestion(String questionText, List<String> options, int correctOptionIndex) {
        this.questionText = questionText;
        this.options = options;
        this.correctOptionIndex = correctOptionIndex;
        this.marks = 1;
    }

    public QuizQuestion(String questionText, List<String> options, int correctOptionIndex, int marks) {
        this.questionText = questionText;
        this.options = options;
        this.correctOptionIndex = correctOptionIndex;
        this.marks = marks > 0 ? marks : 1;
    }

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }

    public List<String> getOptions() { return options; }
    public void setOptions(List<String> options) { this.options = options; }

    public int getCorrectOptionIndex() { return correctOptionIndex; }
    public void setCorrectOptionIndex(int correctOptionIndex) { this.correctOptionIndex = correctOptionIndex; }

    public int getMarks() { return marks > 0 ? marks : 1; }
    public void setMarks(int marks) { this.marks = marks > 0 ? marks : 1; }
}
