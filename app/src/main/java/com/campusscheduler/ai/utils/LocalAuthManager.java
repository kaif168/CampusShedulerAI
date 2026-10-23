package com.campusscheduler.ai.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.campusscheduler.ai.models.User;

public class LocalAuthManager {
    private static final String PREF_NAME = "CampusSchedulerPrefs";
    private static final String KEY_LOGGED_IN = "isLoggedIn";
    private static final String KEY_UID = "uid";
    private static final String KEY_NAME = "name";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_ROLE = "role";
    private static final String KEY_COLLEGE_ID = "collegeId";
    private static final String KEY_DEPT_ID = "departmentId";
    private static final String KEY_PHONE = "phone";
    private static final String KEY_USN = "usn";
    private static final String KEY_DEGREE = "degree";
    private static final String KEY_YEAR = "year";
    private static final String KEY_SEMESTER = "semester";
    private static final String KEY_SECTION_ID = "sectionId";
    private static final String KEY_COLLEGE_NAME = "collegeName";
    private static final String KEY_COLLEGE_ADDRESS = "collegeAddress";
    private static final String KEY_COLLEGE_CODE = "collegeCode";
    private static final String KEY_COLLEGE_PHONE = "collegePhone";
    private static final String KEY_COLLEGE_EMAIL = "collegeEmail";
    private static final String KEY_ADMISSION_TYPE = "admissionType";
    private static final String KEY_DEPT_NAME = "departmentName";
    private static final String KEY_DESIGNATION = "designation";
    private static final String KEY_SPECIALIZATION = "specialization";
    private static final String KEY_QUALIFICATION = "qualification";
    private static final String KEY_EXPERIENCE = "experience";
    private static final String KEY_EXPERTISE = "expertise";

    private final SharedPreferences prefs;

    public LocalAuthManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveSession(User user) {
        if (user == null) return;
        String collegeId = user.getCollegeId();
        if (collegeId == null || collegeId.trim().isEmpty()) {
            collegeId = "HKBK_COLLEGE";
            user.setCollegeId(collegeId);
        }
        String collegeName = user.getCollegeName();
        if (collegeName == null || collegeName.trim().isEmpty()) {
            collegeName = "HKBK College of Engineering";
            user.setCollegeName(collegeName);
        }
        String deptId = user.getDepartmentId();
        if (deptId == null || deptId.trim().isEmpty()) {
            deptId = "DEPT_CSE";
            user.setDepartmentId(deptId);
        }
        String deptName = user.getDepartmentName();
        if (deptName == null || deptName.trim().isEmpty()) {
            deptName = "Computer Science & Engineering";
            user.setDepartmentName(deptName);
        }

        prefs.edit()
                .putBoolean(KEY_LOGGED_IN, true)
                .putString(KEY_UID, user.getUid())
                .putString(KEY_NAME, user.getName())
                .putString(KEY_EMAIL, user.getEmail())
                .putString(KEY_ROLE, user.getRole())
                .putString(KEY_COLLEGE_ID, user.getCollegeId())
                .putString(KEY_DEPT_ID, user.getDepartmentId())
                .putString(KEY_PHONE, user.getPhone())
                .putString(KEY_USN, user.getUsn())
                .putString(KEY_DEGREE, user.getDegree())
                .putString(KEY_YEAR, user.getYear())
                .putString(KEY_SEMESTER, user.getSemester())
                .putString(KEY_SECTION_ID, user.getSectionId())
                .putString(KEY_COLLEGE_NAME, user.getCollegeName())
                .putString(KEY_COLLEGE_ADDRESS, user.getCollegeAddress())
                .putString(KEY_COLLEGE_CODE, user.getCollegeCode())
                .putString(KEY_ADMISSION_TYPE, user.getAdmissionType())
                .putString(KEY_DEPT_NAME, user.getDepartmentName())
                .putString(KEY_DESIGNATION, user.getDesignation())
                .putString(KEY_SPECIALIZATION, user.getSpecialization())
                .putString(KEY_QUALIFICATION, user.getQualification())
                .putString(KEY_EXPERIENCE, user.getExperience())
                .putString(KEY_EXPERTISE, user.getExpertise())
                .apply();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_LOGGED_IN, false);
    }

    public User getCurrentUser() {
        if (!isLoggedIn()) {
            try {
                com.google.firebase.auth.FirebaseUser fbUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
                if (fbUser != null) {
                    User fallback = new User();
                    fallback.setUid(fbUser.getUid());
                    fallback.setEmail(fbUser.getEmail() != null ? fbUser.getEmail() : "");
                    fallback.setName(fbUser.getDisplayName() != null && !fbUser.getDisplayName().isEmpty() ? fbUser.getDisplayName() : "Campus User");
                    fallback.setRole(User.ROLE_COLLEGE_ADMIN);
                    fallback.setCollegeId("HKBK_COLLEGE");
                    fallback.setCollegeName("HKBK College of Engineering");
                    fallback.setDepartmentId("DEPT_CSE");
                    fallback.setDepartmentName("Computer Science & Engineering");
                    saveSession(fallback);
                    return fallback;
                }
            } catch (Exception ignored) {}
            return null;
        }

        User user = new User();
        user.setUid(prefs.getString(KEY_UID, null));
        user.setName(prefs.getString(KEY_NAME, "Campus User"));
        user.setEmail(prefs.getString(KEY_EMAIL, ""));
        user.setRole(prefs.getString(KEY_ROLE, User.ROLE_COLLEGE_ADMIN));

        String collegeId = prefs.getString(KEY_COLLEGE_ID, null);
        if (collegeId == null || collegeId.trim().isEmpty()) {
            collegeId = "HKBK_COLLEGE";
        }
        user.setCollegeId(collegeId);

        String collegeName = prefs.getString(KEY_COLLEGE_NAME, null);
        if (collegeName == null || collegeName.trim().isEmpty()) {
            collegeName = "HKBK College of Engineering";
        }
        user.setCollegeName(collegeName);
        user.setCollegeAddress(prefs.getString(KEY_COLLEGE_ADDRESS, null));
        user.setCollegeCode(prefs.getString(KEY_COLLEGE_CODE, null));

        String deptId = prefs.getString(KEY_DEPT_ID, null);
        if (deptId == null || deptId.trim().isEmpty()) {
            deptId = "DEPT_CSE";
        }
        user.setDepartmentId(deptId);

        String deptName = prefs.getString(KEY_DEPT_NAME, null);
        if (deptName == null || deptName.trim().isEmpty()) {
            deptName = "Computer Science & Engineering";
        }
        user.setDepartmentName(deptName);

        user.setPhone(prefs.getString(KEY_PHONE, ""));
        user.setUsn(prefs.getString(KEY_USN, null));
        user.setDegree(prefs.getString(KEY_DEGREE, null));
        user.setYear(prefs.getString(KEY_YEAR, null));
        user.setSemester(prefs.getString(KEY_SEMESTER, null));
        user.setSectionId(prefs.getString(KEY_SECTION_ID, null));
        user.setAdmissionType(prefs.getString(KEY_ADMISSION_TYPE, null));
        user.setDesignation(prefs.getString(KEY_DESIGNATION, null));
        user.setSpecialization(prefs.getString(KEY_SPECIALIZATION, null));
        user.setQualification(prefs.getString(KEY_QUALIFICATION, null));
        user.setExperience(prefs.getString(KEY_EXPERIENCE, null));
        user.setExpertise(prefs.getString(KEY_EXPERTISE, null));
        return user;
    }

    public static User getCurrentLocalUser(Context context) {
        if (context == null) return null;
        return new LocalAuthManager(context).getCurrentUser();
    }

    public static String getSafeCollegeId(Context context, android.content.Intent intent) {
        if (intent != null && intent.hasExtra("collegeId")) {
            String id = intent.getStringExtra("collegeId");
            if (id != null && !id.trim().isEmpty()) return id.trim();
        }
        User u = getCurrentLocalUser(context);
        if (u != null && u.getCollegeId() != null && !u.getCollegeId().trim().isEmpty()) {
            return u.getCollegeId().trim();
        }
        return "HKBK_COLLEGE";
    }

    public static void saveUserSession(Context context, User user) {
        if (context != null && user != null) {
            new LocalAuthManager(context).saveSession(user);
        }
    }

    public static void logoutLocalUser(Context context) {
        if (context != null) {
            new LocalAuthManager(context).logout();
        }
    }

    public static void clearSession(Context context) {
        logoutLocalUser(context);
    }

    public void updateCollegeIdentity(String collegeId, String collegeName, String collegeAddress, String collegeCode) {
        SharedPreferences.Editor editor = prefs.edit();
        if (collegeId != null && !collegeId.trim().isEmpty()) {
            editor.putString(KEY_COLLEGE_ID, collegeId.trim());
        }
        if (collegeName != null && !collegeName.trim().isEmpty()) {
            editor.putString(KEY_COLLEGE_NAME, collegeName.trim());
        }
        if (collegeAddress != null && !collegeAddress.trim().isEmpty()) {
            editor.putString(KEY_COLLEGE_ADDRESS, collegeAddress.trim());
        }
        if (collegeCode != null && !collegeCode.trim().isEmpty()) {
            editor.putString(KEY_COLLEGE_CODE, collegeCode.trim());
        }
        editor.apply();
    }

    public String getCollegeAddress() {
        return prefs.getString(KEY_COLLEGE_ADDRESS, null);
    }

    public String getCollegeCode() {
        return prefs.getString(KEY_COLLEGE_CODE, null);
    }

    public String getCollegeName() {
        return prefs.getString(KEY_COLLEGE_NAME, "HKBK College of Engineering");
    }

    public void logout() {
        prefs.edit().clear().apply();
    }
}
