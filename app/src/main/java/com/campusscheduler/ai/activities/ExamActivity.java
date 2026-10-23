package com.campusscheduler.ai.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.campusscheduler.ai.R;
import com.campusscheduler.ai.adapters.GenericAdapter;
import com.campusscheduler.ai.models.Exam;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.utils.FirebaseHelper;
import com.campusscheduler.ai.utils.LocalAuthManager;
import com.campusscheduler.ai.utils.UIUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class ExamActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private FloatingActionButton fabAddExam;
    private RecyclerView rvExams;
    private GenericAdapter<Exam> adapter;
    private List<Exam> examList = new ArrayList<>();
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exam);

        currentUser = LocalAuthManager.getCurrentLocalUser(this);
        String safeCollegeId = LocalAuthManager.getSafeCollegeId(this, getIntent());
        if (currentUser == null) {
            currentUser = new User();
            currentUser.setCollegeId(safeCollegeId);
        } else if (currentUser.getCollegeId() == null || currentUser.getCollegeId().trim().isEmpty()) {
            currentUser.setCollegeId(safeCollegeId);
        }

        btnBack = findViewById(R.id.btnBack);
        fabAddExam = findViewById(R.id.fabAddExam);
        rvExams = findViewById(R.id.rvExams);

        btnBack.setOnClickListener(v -> finish());
        fabAddExam.setOnClickListener(v -> showAddExamDialog());

        rvExams.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GenericAdapter<>(examList, (item, holder) -> {
            holder.setText(R.id.tvTitle, item.getSubjectName() + " (" + item.getExamType() + ")");
            holder.setText(R.id.tvSubtitle, "Date: " + item.getExamDate() + " (" + item.getStartTime() + " - " + item.getEndTime() + ") | Room: " + item.getRoomNumber());
        }, R.layout.item_generic);
        rvExams.setAdapter(adapter);

        loadExams();
    }

    private void loadExams() {
        String collegeId = currentUser != null && currentUser.getCollegeId() != null ?
                currentUser.getCollegeId() : LocalAuthManager.getSafeCollegeId(this, getIntent());
        FirebaseHelper.getExamsRef().whereEqualTo("collegeId", collegeId).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    examList.clear();
                    for (var doc : queryDocumentSnapshots) {
                        Exam e = doc.toObject(Exam.class);
                        if (e != null) examList.add(e);
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void showAddExamDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Schedule Semester Exam");

        EditText etSubject = new EditText(this);
        etSubject.setHint("Subject Name");
        EditText etDate = new EditText(this);
        etDate.setHint("Exam Date (YYYY-MM-DD)");
        EditText etRoom = new EditText(this);
        etRoom.setHint("Exam Hall / Room Number");

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);
        layout.addView(etSubject);
        layout.addView(etDate);
        layout.addView(etRoom);
        builder.setView(layout);

        builder.setPositiveButton("Schedule Exam", (dialog, which) -> {
            String sub = etSubject.getText().toString().trim();
            String dt = etDate.getText().toString().trim();
            String room = etRoom.getText().toString().trim();

            if (sub.isEmpty() || dt.isEmpty()) {
                UIUtils.showToast(this, "Subject and date required.");
                return;
            }

            String targetRoom = room.isEmpty() ? "Hall 1" : room;

            // Room conflict validation
            for (Exam existing : examList) {
                if (existing.getExamDate() != null && existing.getExamDate().equalsIgnoreCase(dt)
                        && existing.getRoomNumber() != null && existing.getRoomNumber().equalsIgnoreCase(targetRoom)) {
                    UIUtils.showAlert(this, "Exam Room Conflict Detected",
                            "Room / Hall '" + targetRoom + "' is ALREADY booked on " + dt + " for '"
                                    + existing.getSubjectName() + "'.\n\nPlease select a different exam hall.");
                    return;
                }
            }

            String collegeId = currentUser != null && currentUser.getCollegeId() != null ?
                    currentUser.getCollegeId() : LocalAuthManager.getSafeCollegeId(this, getIntent());
            if (currentUser == null) {
                currentUser = new User();
                currentUser.setCollegeId(collegeId);
            } else if (currentUser.getCollegeId() == null || currentUser.getCollegeId().trim().isEmpty()) {
                currentUser.setCollegeId(collegeId);
            }

            String id = FirebaseHelper.getExamsRef().document().getId();
            Exam e = new Exam();
            e.setId(id);
            e.setCollegeId(collegeId);
            e.setSubjectName(sub);
            e.setExamDate(dt);
            e.setStartTime("09:30 AM");
            e.setEndTime("12:30 PM");
            e.setRoomNumber(targetRoom);
            e.setExamType("Semester Main Exam");

            FirebaseHelper.getExamsRef().document(id).set(e)
                    .addOnSuccessListener(aVoid -> {
                        UIUtils.showToast(this, "Exam Scheduled!");
                        loadExams();
                    })
                    .addOnFailureListener(err -> {
                        examList.add(e);
                        adapter.notifyDataSetChanged();
                    });
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}
