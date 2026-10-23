package com.campusscheduler.ai.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.campusscheduler.ai.R;

import java.util.List;

public class StudentAttendanceAdapter extends RecyclerView.Adapter<StudentAttendanceAdapter.AttendanceViewHolder> {

    public static class AttendanceItem {
        public String subjectName;
        public double percentage;
        public int attended;
        public int total;

        public AttendanceItem(String subjectName, double percentage, int attended, int total) {
            this.subjectName = subjectName;
            this.percentage = percentage;
            this.attended = attended;
            this.total = total;
        }
    }

    private List<AttendanceItem> items;
    private OnAttendanceClickListener listener;

    public interface OnAttendanceClickListener {
        void onAttendanceClick(AttendanceItem item);
    }

    public StudentAttendanceAdapter(List<AttendanceItem> items, OnAttendanceClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AttendanceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student_attendance, parent, false);
        return new AttendanceViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull AttendanceViewHolder holder, int position) {
        AttendanceItem item = items.get(position);
        holder.tvSubject.setText(item.subjectName);

        if (item.total == 0) {
            holder.tvBadge.setText("-");
            holder.pbProgress.setProgress(0);
            holder.tvSessions.setText("No sessions yet");
        } else {
            holder.tvBadge.setText(String.format("%.2f%%", item.percentage));
            holder.pbProgress.setProgress((int) item.percentage);
            holder.tvSessions.setText(item.attended + " of " + item.total + " sessions");
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onAttendanceClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class AttendanceViewHolder extends RecyclerView.ViewHolder {
        TextView tvSubject, tvBadge, tvSessions;
        ProgressBar pbProgress;

        public AttendanceViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSubject = itemView.findViewById(R.id.tvAttendanceSubjectName);
            tvBadge = itemView.findViewById(R.id.tvAttendanceBadge);
            tvSessions = itemView.findViewById(R.id.tvAttendanceSessionsCount);
            pbProgress = itemView.findViewById(R.id.pbSubjectAttendance);
        }
    }
}
