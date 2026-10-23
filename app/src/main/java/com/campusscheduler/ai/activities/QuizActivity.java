package com.campusscheduler.ai.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.campusscheduler.ai.R;
import com.campusscheduler.ai.adapters.GenericAdapter;
import com.campusscheduler.ai.models.Quiz;
import com.campusscheduler.ai.models.QuizAttempt;
import com.campusscheduler.ai.models.QuizQuestion;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.utils.FirebaseHelper;
import com.campusscheduler.ai.utils.LocalAuthManager;
import com.campusscheduler.ai.utils.UIUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QuizActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private FloatingActionButton fabAddQuiz;
    private RecyclerView rvQuizzes;
    private GenericAdapter<Quiz> adapter;
    private final List<Quiz> quizList = new ArrayList<>();
    private final Map<String, QuizAttempt> userAttempts = new HashMap<>();
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        currentUser = LocalAuthManager.getCurrentLocalUser(this);
        String safeCollegeId = LocalAuthManager.getSafeCollegeId(this, getIntent());
        if (currentUser == null) {
            currentUser = new User();
            currentUser.setRole(User.ROLE_STUDENT);
            currentUser.setName("Campus User");
        }
        if (currentUser.getCollegeId() == null || currentUser.getCollegeId().trim().isEmpty()) {
            currentUser.setCollegeId(safeCollegeId);
            LocalAuthManager.saveUserSession(this, currentUser);
        }

        btnBack = findViewById(R.id.btnBack);
        fabAddQuiz = findViewById(R.id.fabAddQuiz);
        rvQuizzes = findViewById(R.id.rvQuizzes);

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        if (fabAddQuiz != null) {
            fabAddQuiz.setOnClickListener(v -> showCreateQuizDialog());
        }

        if (rvQuizzes != null) {
            rvQuizzes.setLayoutManager(new LinearLayoutManager(this));
            adapter = new GenericAdapter<>(quizList, (item, holder) -> {
                String audience = item.getTargetAudience() != null ? "Target: " + item.getTargetAudience() : "Class Quiz";
                if ("SECTION".equalsIgnoreCase(item.getTargetAudience()) && item.getTargetValue() != null) {
                    audience += " " + item.getTargetValue();
                } else if ("STUDENT".equalsIgnoreCase(item.getTargetAudience()) && item.getTargetValue() != null) {
                    audience += " (" + item.getTargetValue() + ")";
                }

                String deadline = item.getDeadline() != null && !item.getDeadline().isEmpty() ? " • Due: " + item.getDeadline() : "";
                int qCount = item.getQuestions() != null ? item.getQuestions().size() : 1;
                int totalMarks = item.getTotalMarks() > 0 ? item.getTotalMarks() : qCount;

                holder.setText(R.id.tvTitle, item.getTitle() + " (" + (item.getSubjectName() != null ? item.getSubjectName() : "Course Quiz") + ")");

                if (userAttempts.containsKey(item.getId())) {
                    QuizAttempt att = userAttempts.get(item.getId());
                    holder.setText(R.id.tvSubtitle, "Completed • Score: " + (att != null ? att.getScore() : 100) +
                            "% | " + qCount + " Questions (" + totalMarks + " Marks)" + deadline);
                } else {
                    holder.setText(R.id.tvSubtitle, audience + " • " + qCount + " Questions (" + totalMarks + " Marks) • " +
                            item.getDurationMinutes() + " mins" + deadline + " • Tap to attempt");
                }
                holder.itemView.setOnClickListener(v -> attemptQuiz(item));
            }, R.layout.item_generic);
            rvQuizzes.setAdapter(adapter);
        }

        loadQuizzes();
    }

    private void loadQuizzes() {
        String collegeId = (currentUser != null && currentUser.getCollegeId() != null && !currentUser.getCollegeId().trim().isEmpty())
                ? currentUser.getCollegeId() : LocalAuthManager.getSafeCollegeId(this, getIntent());
        FirebaseHelper.getQuizzesRef().whereEqualTo("collegeId", collegeId).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    quizList.clear();
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            Quiz q = doc.toObject(Quiz.class);
                            if (q != null) {
                                if (q.getId() == null || q.getId().isEmpty()) {
                                    q.setId(doc.getId());
                                }
                                quizList.add(q);
                            }
                        }
                    }
                    loadUserAttempts();
                })
                .addOnFailureListener(e -> {
                    UIUtils.showToast(this, "Failed to load quizzes: " + e.getMessage());
                });
    }

    private void loadUserAttempts() {
        if (currentUser == null || currentUser.getUid() == null) {
            if (adapter != null) adapter.notifyDataSetChanged();
            return;
        }

        FirebaseHelper.getFirestore().collection("quizAttempts")
                .whereEqualTo("studentId", currentUser.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    userAttempts.clear();
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            QuizAttempt att = doc.toObject(QuizAttempt.class);
                            if (att != null && att.getQuizId() != null) {
                                userAttempts.put(att.getQuizId(), att);
                            }
                        }
                    }
                    if (adapter != null) adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    if (adapter != null) adapter.notifyDataSetChanged();
                });
    }

    // ============================================================
    // CREATE QUIZ WITH MULTILINE PARSER
    // ============================================================

    private void showCreateQuizDialog() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 24, 40, 24);
        scroll.addView(layout);

        EditText etTitle = new EditText(this);
        etTitle.setHint("Quiz Title (e.g. Operating Systems Unit 1 Assessment)");
        layout.addView(etTitle);

        EditText etSubject = new EditText(this);
        etSubject.setHint("Subject Name / Code (e.g. BCS301 - OS)");
        layout.addView(etSubject);

        EditText etDuration = new EditText(this);
        etDuration.setHint("Duration in Minutes (e.g. 30)");
        etDuration.setInputType(InputType.TYPE_CLASS_NUMBER);
        etDuration.setText("30");
        layout.addView(etDuration);

        EditText etDeadline = new EditText(this);
        etDeadline.setHint("Deadline (e.g. 2026-10-31 17:00)");
        layout.addView(etDeadline);

        TextView tvAudience = new TextView(this);
        tvAudience.setText("\nTarget Audience:");
        tvAudience.setTextColor(0xFF0D47A1);
        tvAudience.setTextSize(13f);
        layout.addView(tvAudience);

        Spinner spAudience = new Spinner(this);
        String[] audiences = {"Entire Class (All Sections)", "Section A", "Section B", "Individual Student (USN)"};
        ArrayAdapter<String> audAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, audiences);
        spAudience.setAdapter(audAdapter);
        layout.addView(spAudience);

        EditText etTargetDetail = new EditText(this);
        etTargetDetail.setHint("Target Details (Section or Student USN)");
        layout.addView(etTargetDetail);

        TextView tvQuestionsHeader = new TextView(this);
        tvQuestionsHeader.setText("\nQuestions for this Quiz:");
        tvQuestionsHeader.setTextColor(0xFF0D47A1);
        tvQuestionsHeader.setTypeface(null, android.graphics.Typeface.BOLD);
        tvQuestionsHeader.setTextSize(14f);
        layout.addView(tvQuestionsHeader);

        final List<QuizQuestion> interactiveQuestions = new ArrayList<>();
        LinearLayout llInteractiveQuestions = new LinearLayout(this);
        llInteractiveQuestions.setOrientation(LinearLayout.VERTICAL);
        layout.addView(llInteractiveQuestions);

        Button btnAddQuestion = new Button(this);
        btnAddQuestion.setText("➕ Add Question (Interactive Question Editor)");
        btnAddQuestion.setBackgroundColor(0xFFE3F2FD);
        btnAddQuestion.setTextColor(0xFF1976D2);
        btnAddQuestion.setOnClickListener(v -> showAddSingleQuestionDialog(interactiveQuestions, llInteractiveQuestions));
        layout.addView(btnAddQuestion);

        TextView tvInstructions = new TextView(this);
        tvInstructions.setText("\nOR Bulk Paste MCQ Questions below (Optional):\n" +
                "Format supported:\n" +
                "Q1. Question text here\n" +
                "A) Option 1\n" +
                "B) Option 2\n" +
                "C) Option 3\n" +
                "D) Option 4\n" +
                "Answer: A\n" +
                "Marks: 1");
        tvInstructions.setTextSize(12f);
        tvInstructions.setTextColor(0xFF546E7A);
        layout.addView(tvInstructions);

        EditText etMcqRaw = new EditText(this);
        etMcqRaw.setHint("Paste questions here (optional if added interactively)...");
        etMcqRaw.setLines(5);
        etMcqRaw.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        layout.addView(etMcqRaw);

        new AlertDialog.Builder(this)
                .setTitle("Create MCQ Assessment")
                .setView(scroll)
                .setPositiveButton("PUBLISH QUIZ", (dialog, which) -> {
                    String title = etTitle.getText().toString().trim();
                    String subject = etSubject.getText().toString().trim();
                    String durStr = etDuration.getText().toString().trim();
                    String deadline = etDeadline.getText().toString().trim();
                    String audChoice = audiences[spAudience.getSelectedItemPosition()];
                    String targetDetail = etTargetDetail.getText().toString().trim();
                    String rawMcq = etMcqRaw.getText().toString().trim();

                    if (title.isEmpty()) {
                        UIUtils.showAlert(this, "Incomplete Details", "Please provide a Quiz Title.");
                        return;
                    }

                    List<QuizQuestion> finalQuestions = new ArrayList<>(interactiveQuestions);
                    if (!rawMcq.isEmpty()) {
                        List<QuizQuestion> parsed = parseMultilineQuestions(rawMcq);
                        finalQuestions.addAll(parsed);
                    }

                    if (finalQuestions.isEmpty()) {
                        UIUtils.showAlert(this, "No Questions Added", "Please tap '+ Add Question' or paste MCQ questions to create the quiz.");
                        return;
                    }

                    int duration = 30;
                    try {
                        duration = Integer.parseInt(durStr);
                    } catch (Exception ignored) {}

                    int totalMarks = 0;
                    for (QuizQuestion qq : finalQuestions) {
                        totalMarks += qq.getMarks();
                    }

                    String collegeId = currentUser != null && currentUser.getCollegeId() != null ?
                            currentUser.getCollegeId() : LocalAuthManager.getSafeCollegeId(this, getIntent());
                    String deptId = currentUser != null && currentUser.getDepartmentId() != null ?
                            currentUser.getDepartmentId() : "DEPT_CSE";
                    String quizId = "QUIZ_" + System.currentTimeMillis();

                    Quiz quiz = new Quiz(quizId, collegeId, deptId, "SUB_" + System.currentTimeMillis(), title, duration);
                    quiz.setSubjectName(subject.isEmpty() ? "Course Quiz" : subject);
                    quiz.setQuestions(finalQuestions);
                    quiz.setDeadline(deadline);
                    quiz.setTotalMarks(totalMarks);
                    quiz.setCreatedBy(currentUser != null ? currentUser.getName() : "Faculty");

                    if (audChoice.contains("Section A")) {
                        quiz.setTargetAudience("SECTION");
                        quiz.setTargetValue("A");
                    } else if (audChoice.contains("Section B")) {
                        quiz.setTargetAudience("SECTION");
                        quiz.setTargetValue("B");
                    } else if (audChoice.contains("Individual")) {
                        quiz.setTargetAudience("STUDENT");
                        quiz.setTargetValue(targetDetail);
                    } else {
                        quiz.setTargetAudience("ALL");
                        quiz.setTargetValue("All");
                    }

                    saveQuizToFirestore(quiz);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAddSingleQuestionDialog(List<QuizQuestion> questionList, LinearLayout displayContainer) {
        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 24, 40, 24);
        scroll.addView(layout);

        EditText etQText = new EditText(this);
        etQText.setHint("Question Text (e.g. What is the time complexity of QuickSort?)");
        layout.addView(etQText);

        EditText etOptA = new EditText(this);
        etOptA.setHint("Option A");
        layout.addView(etOptA);

        EditText etOptB = new EditText(this);
        etOptB.setHint("Option B");
        layout.addView(etOptB);

        EditText etOptC = new EditText(this);
        etOptC.setHint("Option C");
        layout.addView(etOptC);

        EditText etOptD = new EditText(this);
        etOptD.setHint("Option D");
        layout.addView(etOptD);

        TextView tvCorrect = new TextView(this);
        tvCorrect.setText("\nSelect Correct Option:");
        tvCorrect.setTextColor(0xFF0D47A1);
        layout.addView(tvCorrect);

        RadioGroup rgCorrect = new RadioGroup(this);
        rgCorrect.setOrientation(RadioGroup.HORIZONTAL);
        RadioButton rbA = new RadioButton(this);
        rbA.setText("A");
        rbA.setId(0);
        rbA.setChecked(true);
        RadioButton rbB = new RadioButton(this);
        rbB.setText("B");
        rbB.setId(1);
        RadioButton rbC = new RadioButton(this);
        rbC.setText("C");
        rbC.setId(2);
        RadioButton rbD = new RadioButton(this);
        rbD.setText("D");
        rbD.setId(3);
        rgCorrect.addView(rbA);
        rgCorrect.addView(rbB);
        rgCorrect.addView(rbC);
        rgCorrect.addView(rbD);
        layout.addView(rgCorrect);

        EditText etMarks = new EditText(this);
        etMarks.setHint("Marks for this question");
        etMarks.setInputType(InputType.TYPE_CLASS_NUMBER);
        etMarks.setText("1");
        layout.addView(etMarks);

        new AlertDialog.Builder(this)
                .setTitle("Add MCQ Question #" + (questionList.size() + 1))
                .setView(scroll)
                .setPositiveButton("ADD QUESTION", (dialog, which) -> {
                    String qText = etQText.getText().toString().trim();
                    String optA = etOptA.getText().toString().trim();
                    String optB = etOptB.getText().toString().trim();
                    String optC = etOptC.getText().toString().trim();
                    String optD = etOptD.getText().toString().trim();

                    if (qText.isEmpty() || optA.isEmpty() || optB.isEmpty()) {
                        UIUtils.showToast(this, "Question and at least Options A and B required.");
                        return;
                    }

                    List<String> options = new ArrayList<>();
                    options.add(optA);
                    options.add(optB);
                    options.add(optC.isEmpty() ? "None of the above" : optC);
                    options.add(optD.isEmpty() ? "All of the above" : optD);

                    int correctIdx = rgCorrect.getCheckedRadioButtonId();
                    if (correctIdx < 0 || correctIdx >= 4) correctIdx = 0;

                    int marks = 1;
                    try {
                        marks = Integer.parseInt(etMarks.getText().toString().trim());
                    } catch (Exception ignored) {}

                    QuizQuestion qq = new QuizQuestion(qText, options, correctIdx, marks);
                    questionList.add(qq);

                    // Refresh preview in container
                    refreshQuestionListPreview(questionList, displayContainer);
                    UIUtils.showToast(this, "Question added! Total questions: " + questionList.size());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void refreshQuestionListPreview(List<QuizQuestion> questionList, LinearLayout displayContainer) {
        displayContainer.removeAllViews();
        for (int i = 0; i < questionList.size(); i++) {
            QuizQuestion q = questionList.get(i);
            int idx = i;
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.HORIZONTAL);
            item.setPadding(12, 12, 12, 12);
            item.setBackgroundColor(0xFFF1F8E9);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 4, 0, 4);
            item.setLayoutParams(lp);

            TextView tv = new TextView(this);
            char ansChar = (char) ('A' + q.getCorrectOptionIndex());
            tv.setText("Q" + (i + 1) + ": " + q.getQuestionText() + "\n(Ans: " + ansChar + ", " + q.getMarks() + "m)");
            tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            tv.setTextColor(0xFF1B5E20);
            item.addView(tv);

            Button btnRemove = new Button(this);
            btnRemove.setText("✕");
            btnRemove.setOnClickListener(v -> {
                questionList.remove(idx);
                refreshQuestionListPreview(questionList, displayContainer);
            });
            item.addView(btnRemove);

            displayContainer.addView(item);
        }
    }

    public static List<QuizQuestion> parseMultilineQuestions(String raw) {
        List<QuizQuestion> result = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) return result;

        // Split by question markers (e.g. Q1., Q2., 1., 2.)
        String[] blocks = raw.split("(?i)(?=(?:^|\\n)\\s*(?:Q\\s*\\d+[.:)]|\\d+[.:)]|Question\\s*\\d+[:.]))");

        for (String block : blocks) {
            String trimmed = block.trim();
            if (trimmed.isEmpty()) continue;

            String[] lines = trimmed.split("\\r?\\n");
            StringBuilder qTextSb = new StringBuilder();
            List<String> options = new ArrayList<>();
            int correctIndex = 0;
            int marks = 1;

            for (String line : lines) {
                String l = line.trim();
                if (l.isEmpty()) continue;

                if (l.matches("(?i)^(?:Q\\s*\\d+[.:)]|\\d+[.:)]|Question\\s*\\d+[:.]).*")) {
                    qTextSb.append(l.replaceFirst("(?i)^(?:Q\\s*\\d+[.:)]|\\d+[.:)]|Question\\s*\\d+[:.])\\s*", "")).append(" ");
                } else if (l.matches("(?i)^[A-D][.:)]\\s*.*") || l.matches("(?i)^\\([A-D]\\)\\s*.*")) {
                    String optText = l.replaceFirst("(?i)^(\\([A-D]\\)|[A-D][.:)])\\s*", "").trim();
                    options.add(optText);
                } else if (l.matches("(?i)^(?:Answer|Ans|Correct|Key)\\s*[:=-]\\s*[A-D].*")) {
                    Pattern p = Pattern.compile("(?i)[A-D]");
                    Matcher m = p.matcher(l.substring(l.indexOf(':') >= 0 ? l.indexOf(':') : 0));
                    if (m.find()) {
                        char c = Character.toUpperCase(m.group(0).charAt(0));
                        correctIndex = c - 'A';
                    }
                } else if (l.matches("(?i)^(?:Marks?|Points?)\\s*[:=-]\\s*\\d+.*")) {
                    Pattern p = Pattern.compile("\\d+");
                    Matcher m = p.matcher(l);
                    if (m.find()) {
                        try {
                            marks = Integer.parseInt(m.group(0));
                        } catch (Exception ignored) {}
                    }
                } else {
                    if (options.isEmpty()) {
                        qTextSb.append(l).append(" ");
                    }
                }
            }

            String questionText = qTextSb.toString().trim();
            if (!questionText.isEmpty() && options.size() >= 2) {
                while (options.size() < 4) {
                    options.add("None of the above");
                }
                if (correctIndex < 0 || correctIndex >= options.size()) {
                    correctIndex = 0;
                }
                result.add(new QuizQuestion(questionText, options, correctIndex, marks));
            }
        }

        return result;
    }

    private void saveQuizToFirestore(Quiz quiz) {
        FirebaseHelper.getQuizzesRef().document(quiz.getId()).set(quiz)
                .addOnSuccessListener(aVoid -> {
                    quizList.add(0, quiz);
                    if (adapter != null) adapter.notifyDataSetChanged();
                    UIUtils.showAlert(this, "Quiz Created",
                            "Quiz '" + quiz.getTitle() + "' successfully created with " +
                                    (quiz.getQuestions() != null ? quiz.getQuestions().size() : 0) +
                                    " questions and published to Cloud Firestore!");
                })
                .addOnFailureListener(e -> {
                    UIUtils.showAlert(this, "Creation Failed", "Could not publish quiz: " + e.getMessage());
                });
    }

    // ============================================================
    // ATTEMPT QUIZ
    // ============================================================

    private void attemptQuiz(Quiz quiz) {
        if (currentUser == null || currentUser.getUid() == null) {
            UIUtils.showToast(this, "Profile context missing.");
            return;
        }

        if (userAttempts.containsKey(quiz.getId())) {
            QuizAttempt prev = userAttempts.get(quiz.getId());
            new AlertDialog.Builder(this)
                    .setTitle(quiz.getTitle() + " (Already Completed)")
                    .setMessage("You have already completed this assessment.\n\nScore Achieved: " +
                            (prev != null ? prev.getScore() : 100) + "%\nQuestions Attempted: " +
                            (prev != null ? prev.getTotalQuestions() : 1))
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        List<QuizQuestion> questions = quiz.getQuestions();
        if (questions == null || questions.isEmpty()) {
            questions = new ArrayList<>();
            List<String> defOpts = new ArrayList<>();
            defOpts.add("Round Robin");
            defOpts.add("Priority Scheduling");
            defOpts.add("First-Come, First-Served (FCFS)");
            defOpts.add("Multilevel Queue");
            questions.add(new QuizQuestion("Which CPU scheduling algorithm may cause process starvation?", defOpts, 1, 1));
        }

        final List<QuizQuestion> finalQuestions = questions;
        ScrollView scroll = new ScrollView(this);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(40, 24, 40, 24);
        scroll.addView(container);

        final List<RadioGroup> groups = new ArrayList<>();

        for (int i = 0; i < finalQuestions.size(); i++) {
            QuizQuestion qq = finalQuestions.get(i);

            TextView tvQ = new TextView(this);
            tvQ.setText("Q" + (i + 1) + ". " + qq.getQuestionText() + " (" + qq.getMarks() + " Mark" + (qq.getMarks() > 1 ? "s" : "") + ")");
            tvQ.setTextSize(14f);
            tvQ.setTextColor(0xFF0D47A1);
            tvQ.setTypeface(null, android.graphics.Typeface.BOLD);
            tvQ.setPadding(0, i > 0 ? 30 : 0, 0, 10);
            container.addView(tvQ);

            RadioGroup rg = new RadioGroup(this);
            List<String> opts = qq.getOptions();
            for (int optIdx = 0; optIdx < (opts != null ? opts.size() : 0); optIdx++) {
                char prefix = (char) ('A' + optIdx);
                RadioButton rb = new RadioButton(this);
                rb.setText(prefix + ") " + opts.get(optIdx));
                rb.setId(optIdx);
                rg.addView(rb);
            }
            groups.add(rg);
            container.addView(rg);
        }

        new AlertDialog.Builder(this)
                .setTitle(quiz.getTitle())
                .setView(scroll)
                .setPositiveButton("SUBMIT ANSWERS", (dialog, which) -> {
                    int totalMarks = 0;
                    int earnedMarks = 0;

                    for (int i = 0; i < finalQuestions.size(); i++) {
                        QuizQuestion qq = finalQuestions.get(i);
                        RadioGroup rg = groups.get(i);
                        int selected = rg.getCheckedRadioButtonId();
                        totalMarks += qq.getMarks();
                        if (selected == qq.getCorrectOptionIndex()) {
                            earnedMarks += qq.getMarks();
                        }
                    }

                    int percentage = totalMarks > 0 ? (int) Math.round(((double) earnedMarks / totalMarks) * 100.0) : 100;
                    saveQuizResult(quiz, percentage, finalQuestions.size(), earnedMarks, totalMarks);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveQuizResult(Quiz quiz, int percentage, int totalQuestions, int earnedMarks, int totalMarks) {
        String attemptId = "ATTEMPT_" + quiz.getId() + "_" + currentUser.getUid();
        QuizAttempt attempt = new QuizAttempt(attemptId, quiz.getId(), currentUser.getUid(), percentage, totalQuestions);

        FirebaseHelper.getFirestore().collection("quizAttempts").document(attemptId).set(attempt)
                .addOnSuccessListener(aVoid -> {
                    userAttempts.put(quiz.getId(), attempt);
                    if (adapter != null) adapter.notifyDataSetChanged();
                    UIUtils.showAlert(this, "Quiz Result Submitted",
                            "Assessment Completed!\n\nScore: " + earnedMarks + " / " + totalMarks + " (" + percentage + "%)\nYour result has been stored in Cloud Firestore.");
                })
                .addOnFailureListener(e -> {
                    UIUtils.showToast(this, "Failed to save attempt: " + e.getMessage());
                });
    }
}

