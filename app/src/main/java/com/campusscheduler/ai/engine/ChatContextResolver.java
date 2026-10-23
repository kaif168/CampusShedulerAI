package com.campusscheduler.ai.engine;

import com.campusscheduler.ai.models.User;

public class ChatContextResolver {
    private final User currentUser;

    public ChatContextResolver(User user) {
        this.currentUser = user;
    }

    public String getCollegeId() {
        return currentUser != null ? currentUser.getCollegeId() : "COLLEGE_VTU_001";
    }

    public String getDepartmentId() {
        return currentUser != null ? currentUser.getDepartmentId() : "DEPT_CSE";
    }

    public String getUserName() {
        return currentUser != null ? currentUser.getName() : "Student/Faculty";
    }

    public String getUserRole() {
        return currentUser != null ? currentUser.getRole() : User.ROLE_STUDENT;
    }
}
