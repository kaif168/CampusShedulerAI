package com.campusscheduler.ai.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.campusscheduler.ai.R;

import java.util.List;

public class StudentCourseAdapter extends RecyclerView.Adapter<StudentCourseAdapter.CourseViewHolder> {

    public static class CourseItem {
        public String title;
        public String section;
        public String badge;

        public CourseItem(String title, String section, String badge) {
            this.title = title;
            this.section = section;
            this.badge = badge;
        }
    }

    private List<CourseItem> courseList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(CourseItem item);
    }

    public StudentCourseAdapter(List<CourseItem> courseList, OnItemClickListener listener) {
        this.courseList = courseList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student_course, parent, false);
        return new CourseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CourseViewHolder holder, int position) {
        CourseItem item = courseList.get(position);
        holder.tvCourseNameCode.setText(item.title);
        holder.tvCourseSection.setText(item.section);
        holder.tvCourseBadge.setText(item.badge);

        if ("PRACTICAL".equalsIgnoreCase(item.badge)) {
            holder.tvCourseBadge.setTextColor(0xFF4CAF50); // Green for practical
        } else {
            holder.tvCourseBadge.setTextColor(0xFF5C2D91); // Purple for lecture
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return courseList != null ? courseList.size() : 0;
    }

    static class CourseViewHolder extends RecyclerView.ViewHolder {
        TextView tvCourseNameCode, tvCourseSection, tvCourseBadge;

        public CourseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCourseNameCode = itemView.findViewById(R.id.tvCourseNameCode);
            tvCourseSection = itemView.findViewById(R.id.tvCourseSection);
            tvCourseBadge = itemView.findViewById(R.id.tvCourseBadge);
        }
    }
}
