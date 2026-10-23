package com.campusscheduler.ai.engine;

import com.campusscheduler.ai.models.IATimetableItem;
import com.campusscheduler.ai.models.Subject;

import java.util.ArrayList;
import java.util.List;

public class IaSchedulingEngine {

    public static List<IATimetableItem> generateIaTimetable(
            String collegeId,
            String departmentId,
            int semester,
            String iaNumber, // IA-1, IA-2, or IA-3
            List<Subject> subjects
    ) {
        List<IATimetableItem> iaList = new ArrayList<>();
        if (subjects == null || subjects.isEmpty()) return iaList;

        int dayCount = 1;
        boolean morningSlot = true;

        for (Subject sub : subjects) {
            if (sub.getSemester() != semester) continue;
            if (sub.getCollegeId() != null && collegeId != null && !sub.getCollegeId().equalsIgnoreCase(collegeId)) {
                continue;
            }

            IATimetableItem item = new IATimetableItem();
            item.setId("IA_" + iaNumber + "_" + sub.getSubjectCode());
            item.setCollegeId(collegeId);
            item.setDepartmentId(departmentId);
            item.setSemester(semester);
            item.setIaNumber(iaNumber);
            item.setSubjectName(sub.getSubjectName());
            item.setSubjectCode(sub.getSubjectCode());
            item.setDayLabel("Test Day " + dayCount);
            item.setTimeSlotLabel(morningSlot ? "09:30 AM - 11:00 AM" : "02:00 PM - 03:30 PM");

            iaList.add(item);

            if (!morningSlot) {
                dayCount++; // Move to next day after 2 sessions per day
                morningSlot = true;
            } else {
                morningSlot = false;
            }
        }

        return iaList;
    }
}
