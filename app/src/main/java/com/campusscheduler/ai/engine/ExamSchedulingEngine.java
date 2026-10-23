package com.campusscheduler.ai.engine;

import com.campusscheduler.ai.models.Classroom;
import com.campusscheduler.ai.models.Exam;
import com.campusscheduler.ai.models.Subject;
import com.campusscheduler.ai.models.User;

import java.util.ArrayList;
import java.util.List;

public class ExamSchedulingEngine {

    public static List<Exam> generateExamTimetable(
            String collegeId,
            List<Subject> subjects,
            List<Classroom> classrooms,
            List<User> invigilators,
            String startDateStr
    ) {
        List<Exam> generatedExams = new ArrayList<>();
        if (subjects == null || subjects.isEmpty()) return generatedExams;

        int dayOffset = 0;
        int invigilatorIdx = 0;
        int roomIdx = 0;

        for (Subject sub : subjects) {
            if (sub.getCollegeId() != null && collegeId != null && !sub.getCollegeId().equalsIgnoreCase(collegeId)) {
                continue; // Multi-college tenant isolation
            }

            Exam exam = new Exam();
            exam.setExamId("EXAM_" + sub.getSubjectCode() + "_" + (dayOffset + 1));
            exam.setCollegeId(collegeId);
            exam.setSubjectId(sub.getId());
            exam.setSubjectName(sub.getSubjectName());
            exam.setSubjectCode(sub.getSubjectCode());
            exam.setExamDate("Day " + (dayOffset + 1) + " (" + startDateStr + ")");
            exam.setStartTime("09:30 AM");
            exam.setEndTime("12:30 PM");

            // Assign room
            if (classrooms != null && !classrooms.isEmpty()) {
                Classroom room = classrooms.get(roomIdx % classrooms.size());
                exam.setRoomNumber(room.getRoomNumber());
                roomIdx++;
            } else {
                exam.setRoomNumber("Main Examination Hall");
            }

            // Assign invigilator
            if (invigilators != null && !invigilators.isEmpty()) {
                User inv = invigilators.get(invigilatorIdx % invigilators.size());
                exam.setInvigilatorName(inv.getName());
                invigilatorIdx++;
            }

            generatedExams.add(exam);
            dayOffset += 2; // 1 gap day between examinations for preparation
        }

        return generatedExams;
    }
}
