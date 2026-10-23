package com.campusscheduler.ai.models;

import java.io.Serializable;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;

public class Subject implements Serializable {
    private String id;
    private String collegeId;
    private String departmentId;
    private String code;
    private String subjectCode;
    private String name;
    private String subjectName;
    private String type;
    private int semester;
    private int credits;
    private int weeklyHoursRequired;
    private int weeklyPeriods;
    private boolean isLab;

    public Subject() {}

    public Subject(String id, String name, String code, int credits, boolean isLab) {
        this.id = id;
        this.name = name;
        this.subjectName = name;
        this.code = code;
        this.subjectCode = code;
        this.credits = credits;
        this.isLab = isLab;
        this.type = isLab ? "Lab" : "Theory";
        this.weeklyHoursRequired = credits;
        this.weeklyPeriods = credits;
    }

    public Subject(String id, String collegeId, String departmentId, String code, String name, int semester, int credits, int weeklyHoursRequired, boolean isLab) {
        this.id = id;
        this.collegeId = collegeId;
        this.departmentId = departmentId;
        this.code = code;
        this.subjectCode = code;
        this.name = name;
        this.subjectName = name;
        this.semester = semester;
        this.credits = credits;
        this.weeklyHoursRequired = weeklyHoursRequired;
        this.weeklyPeriods = weeklyHoursRequired;
        this.isLab = isLab;
        this.type = isLab ? "Lab" : "Theory";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCollegeId() { return collegeId; }
    public void setCollegeId(String collegeId) { this.collegeId = collegeId; }

    public String getDepartmentId() { return departmentId; }
    public void setDepartmentId(String departmentId) { this.departmentId = departmentId; }

    public String getCode() { return code != null ? code : subjectCode; }
    public void setCode(String code) { this.code = code; this.subjectCode = code; }

    public String getSubjectCode() { return subjectCode != null ? subjectCode : code; }
    public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; this.code = subjectCode; }

    public String getName() { return name != null ? name : subjectName; }
    public void setName(String name) { this.name = name; this.subjectName = name; }

    public String getSubjectName() { return subjectName != null ? subjectName : name; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; this.name = subjectName; }

    public String getType() { return type != null ? type : (isLab ? "Lab" : "Theory"); }
    public void setType(String type) { this.type = type; }

    private String departmentName;
    private String category;
    private String status = "ACTIVE";

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public String getCategory() { return category != null ? category : getType(); }
    public void setCategory(String category) { this.category = category; }

    public String getStatus() { return status != null ? status : "ACTIVE"; }
    public void setStatus(String status) { this.status = status; }

    public boolean isActive() { return !"INACTIVE".equalsIgnoreCase(status); }

    @PropertyName("semester")
    public Object getSemesterRaw() { return semester; }

    @PropertyName("semester")
    public void setSemester(Object val) {
        if (val instanceof Number) {
            this.semester = ((Number) val).intValue();
        } else if (val != null) {
            try {
                String clean = val.toString().replaceAll("[^0-9]", "");
                this.semester = clean.isEmpty() ? 0 : Integer.parseInt(clean);
            } catch (Exception ignored) {
                this.semester = 0;
            }
        }
    }

    @Exclude
    public int getSemester() { return semester; }

    @Exclude
    public void setSemester(int semester) { this.semester = semester; }

    public int getCredits() { return credits; }
    public void setCredits(int credits) { this.credits = credits; }

    public int getWeeklyHoursRequired() { return weeklyHoursRequired > 0 ? weeklyHoursRequired : weeklyPeriods; }
    public void setWeeklyHoursRequired(int weeklyHoursRequired) { this.weeklyHoursRequired = weeklyHoursRequired; this.weeklyPeriods = weeklyHoursRequired; }

    public int getWeeklyPeriods() { return weeklyPeriods > 0 ? weeklyPeriods : weeklyHoursRequired; }
    public void setWeeklyPeriods(int weeklyPeriods) { this.weeklyPeriods = weeklyPeriods; this.weeklyHoursRequired = weeklyPeriods; }

    public boolean isLab() { return isLab; }
    public void setLab(boolean lab) { isLab = lab; }
}
