package com.campusscheduler.ai.repository;

import com.campusscheduler.ai.models.*;
import com.campusscheduler.ai.utils.FirebaseHelper;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.QuerySnapshot;

public class AcademicRepository {

    private static AcademicRepository instance;

    private final android.content.Context context;

    private AcademicRepository(android.content.Context context) {
        this.context = context != null
                ? context.getApplicationContext()
                : null;
    }

    public static synchronized AcademicRepository getInstance() {

        if (instance == null) {
            instance = new AcademicRepository(null);
        }

        return instance;
    }

    public static synchronized AcademicRepository getInstance(
            android.content.Context context
    ) {

        if (instance == null) {
            instance = new AcademicRepository(context);
        }

        return instance;
    }

    // ============================================================
    // DEPARTMENTS
    // ============================================================

    public void getDepartments(
            String collegeId,
            OnCompleteListener<QuerySnapshot> listener
    ) {

        FirebaseHelper
                .getDepartmentsRef()
                .whereEqualTo("collegeId", collegeId)
                .get()
                .addOnCompleteListener(listener);
    }

    public void saveDepartment(
            Department d,
            OnCompleteListener<Void> listener
    ) {

        FirebaseHelper
                .getDepartmentsRef()
                .document(d.getId())
                .set(d)
                .addOnCompleteListener(listener);
    }

    public void deleteDepartment(
            String departmentId,
            OnCompleteListener<Void> listener
    ) {

        FirebaseHelper
                .getDepartmentsRef()
                .document(departmentId)
                .delete()
                .addOnCompleteListener(listener);
    }

    // ============================================================
    // SEMESTERS
    // ============================================================

    public void getSemesters(
            String collegeId,
            OnCompleteListener<QuerySnapshot> listener
    ) {

        FirebaseHelper
                .getSemestersRef()
                .whereEqualTo("collegeId", collegeId)
                .get()
                .addOnCompleteListener(listener);
    }

    public void saveSemester(
            Semester s,
            OnCompleteListener<Void> listener
    ) {

        FirebaseHelper
                .getSemestersRef()
                .document(s.getId())
                .set(s)
                .addOnCompleteListener(listener);
    }

    /**
     * Delete one semester from Firestore.
     */
    public void deleteSemester(
            String semesterId,
            OnCompleteListener<Void> listener
    ) {

        FirebaseHelper
                .getSemestersRef()
                .document(semesterId)
                .delete()
                .addOnCompleteListener(listener);
    }

    // ============================================================
    // FACULTY
    // ============================================================

    public void getFaculty(
            String collegeId,
            OnCompleteListener<QuerySnapshot> listener
    ) {

        FirebaseHelper
                .getFacultyRef()
                .whereEqualTo("collegeId", collegeId)
                .get()
                .addOnCompleteListener(listener);
    }

    public void saveFaculty(
            Faculty f,
            OnCompleteListener<Void> listener
    ) {

        FirebaseHelper
                .getFacultyRef()
                .document(f.getId())
                .set(f)
                .addOnCompleteListener(listener);
    }

    // ============================================================
    // LABORATORIES
    // ============================================================

    public void getLaboratories(
            String collegeId,
            OnCompleteListener<QuerySnapshot> listener
    ) {

        FirebaseHelper
                .getLaboratoriesRef()
                .whereEqualTo("collegeId", collegeId)
                .get()
                .addOnCompleteListener(listener);
    }

    public void saveLaboratory(
            Laboratory l,
            OnCompleteListener<Void> listener
    ) {

        FirebaseHelper
                .getLaboratoriesRef()
                .document(l.getId())
                .set(l)
                .addOnCompleteListener(listener);
    }

    // ============================================================
    // SECTIONS
    // ============================================================

    public void getSections(
            String collegeId,
            OnCompleteListener<QuerySnapshot> listener
    ) {

        FirebaseHelper
                .getSectionsRef()
                .whereEqualTo("collegeId", collegeId)
                .get()
                .addOnCompleteListener(listener);
    }

    public void saveSection(
            Section s,
            OnCompleteListener<Void> listener
    ) {

        FirebaseHelper
                .getSectionsRef()
                .document(s.getId())
                .set(s)
                .addOnCompleteListener(listener);
    }

    // ============================================================
    // SUBJECTS
    // ============================================================

    public void getSubjects(
            String collegeId,
            OnCompleteListener<QuerySnapshot> listener
    ) {

        FirebaseHelper
                .getSubjectsRef()
                .whereEqualTo("collegeId", collegeId)
                .get()
                .addOnCompleteListener(listener);
    }

    public void saveSubject(
            Subject s,
            OnCompleteListener<Void> listener
    ) {

        FirebaseHelper
                .getSubjectsRef()
                .document(s.getId())
                .set(s)
                .addOnCompleteListener(listener);
    }

    // ============================================================
    // STUDENTS
    // ============================================================

    public void getStudents(
            String collegeId,
            OnCompleteListener<QuerySnapshot> listener
    ) {

        FirebaseHelper
                .getStudentsRef()
                .whereEqualTo("collegeId", collegeId)
                .get()
                .addOnCompleteListener(listener);
    }

    public void saveStudent(
            Student s,
            OnCompleteListener<Void> listener
    ) {

        FirebaseHelper
                .getStudentsRef()
                .document(s.getId())
                .set(s)
                .addOnCompleteListener(listener);
    }

    // ============================================================
    // CLASSROOMS
    // ============================================================

    public void getClassrooms(
            String collegeId,
            OnCompleteListener<QuerySnapshot> listener
    ) {

        FirebaseHelper
                .getClassroomsRef()
                .whereEqualTo("collegeId", collegeId)
                .get()
                .addOnCompleteListener(listener);
    }

    public void saveClassroom(
            Classroom c,
            OnCompleteListener<Void> listener
    ) {

        FirebaseHelper
                .getClassroomsRef()
                .document(c.getId())
                .set(c)
                .addOnCompleteListener(listener);
    }
}