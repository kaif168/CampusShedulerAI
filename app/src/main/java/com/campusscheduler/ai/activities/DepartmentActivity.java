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
import com.campusscheduler.ai.models.Department;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.repository.AcademicRepository;
import com.campusscheduler.ai.utils.LocalAuthManager;
import com.campusscheduler.ai.utils.UIUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class DepartmentActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private FloatingActionButton fabAddDepartment;
    private RecyclerView rvDepartments;
    private GenericAdapter<Department> adapter;
    private List<Department> departmentList = new ArrayList<>();
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_department);

        currentUser = LocalAuthManager.getCurrentLocalUser(this);
        String safeCollegeId = LocalAuthManager.getSafeCollegeId(this, getIntent());
        if (currentUser == null) {
            currentUser = new User();
            currentUser.setRole(User.ROLE_COLLEGE_ADMIN);
            currentUser.setName("Campus Administrator");
        }
        if (currentUser.getCollegeId() == null || currentUser.getCollegeId().trim().isEmpty()) {
            currentUser.setCollegeId(safeCollegeId);
            LocalAuthManager.saveUserSession(this, currentUser);
        }

        btnBack = findViewById(R.id.btnBack);
        fabAddDepartment = findViewById(R.id.fabAddDepartment);
        rvDepartments = findViewById(R.id.rvDepartments);

        btnBack.setOnClickListener(v -> finish());
        fabAddDepartment.setOnClickListener(v -> showAddDepartmentDialog());

        rvDepartments.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GenericAdapter<>(departmentList, (item, holder) -> {
            holder.setText(R.id.tvTitle, item.getDepartmentName() + " (" + item.getDepartmentCode() + ")");
            holder.setText(R.id.tvSubtitle, item.getDescription() != null ? item.getDescription() : "Active Department");
        }, R.layout.item_generic);
        rvDepartments.setAdapter(adapter);

        loadDepartments();
    }

    private void loadDepartments() {
        String collegeId = (currentUser != null && currentUser.getCollegeId() != null && !currentUser.getCollegeId().trim().isEmpty())
                ? currentUser.getCollegeId() : LocalAuthManager.getSafeCollegeId(this, getIntent());
        AcademicRepository.getInstance().getDepartments(collegeId, task -> {
            departmentList.clear();
            if (task.isSuccessful() && task.getResult() != null) {
                for (var doc : task.getResult()) {
                    Department d = doc.toObject(Department.class);
                    if (d != null) departmentList.add(d);
                }
            }
            adapter.notifyDataSetChanged();
        });
    }

    private void showAddDepartmentDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Department");

        EditText etName = new EditText(this);
        etName.setHint("Department Name");
        EditText etCode = new EditText(this);
        etCode.setHint("Department Code");

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);
        layout.addView(etName);
        layout.addView(etCode);
        builder.setView(layout);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String name = etName.getText().toString().trim();
            String code = etCode.getText().toString().trim();

            if (name.isEmpty() || code.isEmpty()) {
                UIUtils.showToast(this, "Name and code required.");
                return;
            }

            String collegeId = (currentUser != null && currentUser.getCollegeId() != null && !currentUser.getCollegeId().trim().isEmpty())
                    ? currentUser.getCollegeId() : LocalAuthManager.getSafeCollegeId(this, getIntent());
            Department d = new Department("DEP_" + System.currentTimeMillis(), name, code);
            d.setCollegeId(collegeId);
            AcademicRepository.getInstance().saveDepartment(d, task -> {
                UIUtils.showToast(this, "Department added!");
                loadDepartments();
            });
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}
