package com.campusscheduler.ai.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.campusscheduler.ai.R;
import com.campusscheduler.ai.models.Student;

import java.util.List;
import java.util.Map;

public class TeacherAttendanceAdapter extends RecyclerView.Adapter<TeacherAttendanceAdapter.ViewHolder> {

    public interface OnAttendanceCheckListener {
        void onCheckChanged(Student student, boolean isPresent);
    }

    private final List<Student> studentList;
    private final Map<String, Boolean> attendanceMap;
    private final OnAttendanceCheckListener listener;

    public TeacherAttendanceAdapter(List<Student> studentList, Map<String, Boolean> attendanceMap, OnAttendanceCheckListener listener) {
        this.studentList = studentList;
        this.attendanceMap = attendanceMap;
        this.listener = listener;
    }

    public TeacherAttendanceAdapter(List<Student> studentList, OnAttendanceCheckListener listener) {
        this(studentList, null, listener);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_teacher_attendance, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Student student = studentList.get(position);
        holder.tvStudentName.setText(student.getName() != null ? student.getName() : "Student");

        String idStr = student.getUsn() != null && !student.getUsn().isEmpty() ? "USN: " + student.getUsn() :
                (student.getRollNumber() != null ? "Roll: " + student.getRollNumber() : "ID: " + (student.getStudentId() != null ? student.getStudentId() : ""));
        holder.tvStudentUsn.setText(idStr);

        String key = student.getStudentId() != null ? student.getStudentId() : (student.getUid() != null ? student.getUid() : student.getUsn());
        boolean isPresent = true;
        if (attendanceMap != null && key != null && attendanceMap.containsKey(key)) {
            isPresent = Boolean.TRUE.equals(attendanceMap.get(key));
        }

        holder.cbAttendanceStatus.setOnCheckedChangeListener(null);
        holder.cbAttendanceStatus.setChecked(isPresent);
        updateStatusStyle(holder.cbAttendanceStatus, isPresent);

        holder.cbAttendanceStatus.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateStatusStyle((CheckBox) buttonView, isChecked);
            if (listener != null) {
                listener.onCheckChanged(student, isChecked);
            }
        });

        holder.itemView.setOnClickListener(v -> holder.cbAttendanceStatus.toggle());
    }

    private void updateStatusStyle(CheckBox cb, boolean isChecked) {
        if (isChecked) {
            cb.setText("PRESENT");
            cb.setTextColor(Color.parseColor("#2E7D32"));
        } else {
            cb.setText("ABSENT");
            cb.setTextColor(Color.parseColor("#C62828"));
        }
    }

    @Override
    public int getItemCount() {
        return studentList != null ? studentList.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvStudentName;
        TextView tvStudentUsn;
        CheckBox cbAttendanceStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStudentName = itemView.findViewById(R.id.tvStudentName);
            tvStudentUsn = itemView.findViewById(R.id.tvStudentUsn);
            cbAttendanceStatus = itemView.findViewById(R.id.cbAttendanceStatus);
        }
    }
}
