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
import com.campusscheduler.ai.models.Laboratory;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.repository.AcademicRepository;
import com.campusscheduler.ai.utils.LocalAuthManager;
import com.campusscheduler.ai.utils.UIUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class LaboratoryActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private FloatingActionButton fabAddLab;
    private RecyclerView rvLaboratories;
    private GenericAdapter<Laboratory> adapter;
    private List<Laboratory> labList = new ArrayList<>();
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_laboratory);

        currentUser = LocalAuthManager.getCurrentLocalUser(this);
        String safeCollegeId = LocalAuthManager.getSafeCollegeId(this, getIntent());
        if (currentUser == null) {
            currentUser = new User();
            currentUser.setCollegeId(safeCollegeId);
        } else if (currentUser.getCollegeId() == null || currentUser.getCollegeId().trim().isEmpty()) {
            currentUser.setCollegeId(safeCollegeId);
        }

        btnBack = findViewById(R.id.btnBack);
        fabAddLab = findViewById(R.id.fabAddLab);
        rvLaboratories = findViewById(R.id.rvLaboratories);

        btnBack.setOnClickListener(v -> finish());
        fabAddLab.setOnClickListener(v -> showAddLabDialog());

        rvLaboratories.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GenericAdapter<>(labList, (item, holder) -> {
            holder.setText(R.id.tvTitle, item.getLabName() + " (" + item.getLabNumber() + ")");
            holder.setText(R.id.tvSubtitle, "Capacity: " + item.getCapacity() + " | Type: " + item.getLabType());
        }, R.layout.item_generic);
        rvLaboratories.setAdapter(adapter);

        loadLabs();
    }

    private void loadLabs() {
        String collegeId = currentUser != null && currentUser.getCollegeId() != null ?
                currentUser.getCollegeId() : LocalAuthManager.getSafeCollegeId(this, getIntent());
        AcademicRepository.getInstance().getLaboratories(collegeId, task -> {
            labList.clear();
            if (task.isSuccessful() && task.getResult() != null) {
                for (var doc : task.getResult()) {
                    Laboratory l = doc.toObject(Laboratory.class);
                    if (l != null) labList.add(l);
                }
            }
            adapter.notifyDataSetChanged();
        });
    }

    private void showAddLabDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Laboratory");

        EditText etName = new EditText(this);
        etName.setHint("Lab Name (e.g. Systems Lab)");
        EditText etNum = new EditText(this);
        etNum.setHint("Lab Number (e.g. Lab 201)");
        EditText etCap = new EditText(this);
        etCap.setHint("Capacity (e.g. 40)");

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);
        layout.addView(etName);
        layout.addView(etNum);
        layout.addView(etCap);
        builder.setView(layout);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String name = etName.getText().toString().trim();
            String num = etNum.getText().toString().trim();
            String capStr = etCap.getText().toString().trim();

            if (name.isEmpty() || num.isEmpty()) {
                UIUtils.showToast(this, "Lab name and number required.");
                return;
            }

            int cap = 40;
            try { cap = Integer.parseInt(capStr); } catch (Exception e) {}

            String collegeId = currentUser != null && currentUser.getCollegeId() != null ?
                    currentUser.getCollegeId() : LocalAuthManager.getSafeCollegeId(this, getIntent());
            if (currentUser == null) {
                currentUser = new User();
                currentUser.setCollegeId(collegeId);
            } else if (currentUser.getCollegeId() == null || currentUser.getCollegeId().trim().isEmpty()) {
                currentUser.setCollegeId(collegeId);
            }

            Laboratory l = new Laboratory("LAB_" + System.currentTimeMillis(), name, cap, true);
            l.setCollegeId(collegeId);
            l.setLabNumber(num);
            AcademicRepository.getInstance().saveLaboratory(l, task -> {
                UIUtils.showToast(this, "Laboratory added!");
                loadLabs();
            });
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}
