package com.campusscheduler.ai.activities;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.campusscheduler.ai.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class OwnerAnalyticsActivity extends AppCompatActivity {

    // ============================================================
    // VIEWS
    // ============================================================

    private ImageButton btnBack;

    private TextView tvTotalColleges;
    private TextView tvTotalUsers;
    private TextView tvSystemHealth;
    private TextView tvTimetablesGenerated;

    private CardView cardTotalColleges;
    private CardView cardTotalUsers;
    private CardView cardTimetablesGenerated;

    // ============================================================
    // FIREBASE
    // ============================================================

    private FirebaseFirestore firestore;

    // ============================================================
    // LOAD STATUS
    // ============================================================

    private boolean collegesLoaded = false;
    private boolean usersLoaded = false;
    private boolean timetablesLoaded = false;

    private boolean collegesFailed = false;
    private boolean usersFailed = false;
    private boolean timetablesFailed = false;


    // ============================================================
    // ON CREATE
    // ============================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --------------------------------------------------------
        // OWNER BLUE SYSTEM BAR
        // --------------------------------------------------------

        Window window = getWindow();

        window.setStatusBarColor(
                Color.rgb(13, 71, 161)
        );

        window.setNavigationBarColor(
                Color.BLACK
        );

        // --------------------------------------------------------
        // LAYOUT
        // --------------------------------------------------------

        setContentView(
                R.layout.activity_owner_analytics
        );

        // --------------------------------------------------------
        // FIRESTORE
        // --------------------------------------------------------

        firestore =
                FirebaseFirestore.getInstance();

        // --------------------------------------------------------
        // FIND VIEWS
        // --------------------------------------------------------

        btnBack =
                findViewById(R.id.btnBack);

        tvTotalColleges =
                findViewById(R.id.tvTotalColleges);

        tvTotalUsers =
                findViewById(R.id.tvTotalUsers);

        tvSystemHealth =
                findViewById(R.id.tvSystemHealth);

        tvTimetablesGenerated =
                findViewById(R.id.tvTimetablesGenerated);

        // Proper CardViews
        cardTotalColleges =
                findViewById(R.id.cardTotalColleges);

        cardTotalUsers =
                findViewById(R.id.cardTotalUsers);

        cardTimetablesGenerated =
                findViewById(R.id.cardTimetablesGenerated);

        // --------------------------------------------------------
        // BACK BUTTON
        // --------------------------------------------------------

        if (btnBack != null) {

            btnBack.setOnClickListener(
                    v -> finish()
            );
        }

        // --------------------------------------------------------
        // INITIAL STATE
        // --------------------------------------------------------

        if (tvTotalColleges != null) {
            tvTotalColleges.setText("—");
        }

        if (tvTotalUsers != null) {
            tvTotalUsers.setText("—");
        }

        if (tvSystemHealth != null) {
            tvSystemHealth.setText("Checking...");
        }

        if (tvTimetablesGenerated != null) {
            tvTimetablesGenerated.setText("—");
        }

        // --------------------------------------------------------
        // CARD CLICKS
        // --------------------------------------------------------

        setupAnalyticsCardClicks();

        // --------------------------------------------------------
        // LOAD REAL FIREBASE DATA
        // --------------------------------------------------------

        loadRealPlatformAnalytics();
    }


    // ============================================================
    // ANALYTICS CARD CLICKS
    // ============================================================

    private void setupAnalyticsCardClicks() {

        // --------------------------------------------------------
        // REGISTERED COLLEGES
        // --------------------------------------------------------

        if (cardTotalColleges != null) {

            cardTotalColleges.setOnClickListener(
                    v -> showCollegeList()
            );
        }


        // --------------------------------------------------------
        // PLATFORM USERS
        // --------------------------------------------------------

        if (cardTotalUsers != null) {

            cardTotalUsers.setOnClickListener(
                    v -> showUserList()
            );
        }


        // --------------------------------------------------------
        // AI TIMETABLES
        // --------------------------------------------------------

        if (cardTimetablesGenerated != null) {

            cardTimetablesGenerated.setOnClickListener(
                    v -> showTimetableList()
            );
        }

        // --------------------------------------------------------
        // SYSTEM HEALTH
        // --------------------------------------------------------
        // Informational only.
        // No fake detailed telemetry is shown.
    }


    // ============================================================
    // REAL PLATFORM ANALYTICS
    // ============================================================

    private void loadRealPlatformAnalytics() {

        // --------------------------------------------------------
        // COLLEGES
        // --------------------------------------------------------

        firestore
                .collection("colleges")
                .get()
                .addOnSuccessListener(
                        querySnapshot -> {

                            int totalColleges =
                                    querySnapshot.size();

                            tvTotalColleges.setText(
                                    String.valueOf(
                                            totalColleges
                                    )
                            );

                            collegesLoaded = true;

                            updateSystemHealth();
                        }
                )
                .addOnFailureListener(
                        error -> {

                            tvTotalColleges.setText("—");

                            collegesFailed = true;

                            updateSystemHealth();
                        }
                );


        // --------------------------------------------------------
        // USERS
        // --------------------------------------------------------

        firestore
                .collection("users")
                .get()
                .addOnSuccessListener(
                        querySnapshot -> {

                            int totalUsers =
                                    querySnapshot.size();

                            tvTotalUsers.setText(
                                    String.valueOf(
                                            totalUsers
                                    )
                            );

                            usersLoaded = true;

                            updateSystemHealth();
                        }
                )
                .addOnFailureListener(
                        error -> {

                            tvTotalUsers.setText("—");

                            usersFailed = true;

                            updateSystemHealth();
                        }
                );


        // --------------------------------------------------------
        // TIMETABLES
        // --------------------------------------------------------

        firestore
                .collection("timetables")
                .get()
                .addOnSuccessListener(
                        querySnapshot -> {

                            int totalTimetables =
                                    querySnapshot.size();

                            tvTimetablesGenerated.setText(
                                    String.valueOf(
                                            totalTimetables
                                    )
                            );

                            timetablesLoaded = true;

                            updateSystemHealth();
                        }
                )
                .addOnFailureListener(
                        error -> {

                            tvTimetablesGenerated.setText("—");

                            timetablesFailed = true;

                            updateSystemHealth();
                        }
                );
    }


    // ============================================================
    // SYSTEM HEALTH
    // ============================================================

    private void updateSystemHealth() {

        if (collegesLoaded
                && usersLoaded
                && timetablesLoaded) {

            tvSystemHealth.setText(
                    "Operational"
            );

            return;
        }


        if (collegesFailed
                || usersFailed
                || timetablesFailed) {

            tvSystemHealth.setText(
                    "Unavailable"
            );

            return;
        }


        tvSystemHealth.setText(
                "Checking..."
        );
    }


    // ============================================================
    // COLLEGE LIST
    // ============================================================

    private void showCollegeList() {

        firestore
                .collection("colleges")
                .get()
                .addOnSuccessListener(
                        this::displayCollegeList
                )
                .addOnFailureListener(
                        error ->
                                showMessage(
                                        "College Data",
                                        "Unable to load college records from Firebase."
                                )
                );
    }


    // ============================================================
    // DISPLAY COLLEGE LIST
    // ============================================================

    private void displayCollegeList(
            QuerySnapshot snapshot
    ) {

        if (snapshot.isEmpty()) {

            showMessage(
                    "Registered Colleges",
                    "No colleges are currently registered."
            );

            return;
        }


        List<DocumentSnapshot> colleges =
                new ArrayList<>(
                        snapshot.getDocuments()
                );


        Collections.sort(
                colleges,
                Comparator.comparing(
                        this::getCollegeName,
                        String.CASE_INSENSITIVE_ORDER
                )
        );


        String[] items =
                new String[colleges.size()];


        for (int i = 0;
             i < colleges.size();
             i++) {

            DocumentSnapshot doc =
                    colleges.get(i);

            String name =
                    getCollegeName(doc);

            String code =
                    getString(
                            doc,
                            "code"
                    );

            String status =
                    getCollegeStatus(doc);


            items[i] =
                    name
                            + "\n"
                            + "Code: "
                            + (
                            code.isEmpty()
                                    ? "N/A"
                                    : code
                    )
                            + " • "
                            + status;
        }


        new AlertDialog.Builder(this)
                .setTitle(
                        "Registered Colleges ("
                                + colleges.size()
                                + ")"
                )
                .setItems(
                        items,
                        (dialog, which) ->
                                showCollegeDetails(
                                        colleges.get(which)
                                )
                )
                .setNegativeButton(
                        "CLOSE",
                        null
                )
                .show();
    }


    // ============================================================
    // COLLEGE DETAILS
    // ============================================================

    private void showCollegeDetails(
            DocumentSnapshot doc
    ) {

        String name =
                getCollegeName(doc);

        String code =
                getString(
                        doc,
                        "code"
                );

        String address =
                getString(
                        doc,
                        "address"
                );

        String email =
                getString(
                        doc,
                        "contactEmail"
                );

        String phone =
                getString(
                        doc,
                        "contactPhone"
                );

        String status =
                getCollegeStatus(doc);


        StringBuilder details =
                new StringBuilder();


        details.append(
                "College Name: "
        ).append(
                name
        ).append(
                "\n\n"
        );


        details.append(
                "College Code: "
        ).append(
                code.isEmpty()
                        ? "N/A"
                        : code
        ).append(
                "\n\n"
        );


        details.append(
                "Address: "
        ).append(
                address.isEmpty()
                        ? "N/A"
                        : address
        ).append(
                "\n\n"
        );


        details.append(
                "Contact Email: "
        ).append(
                email.isEmpty()
                        ? "N/A"
                        : email
        ).append(
                "\n\n"
        );


        details.append(
                "Contact Phone: "
        ).append(
                phone.isEmpty()
                        ? "N/A"
                        : phone
        ).append(
                "\n\n"
        );


        details.append(
                "Status: "
        ).append(
                status
        );


        showMessage(
                "College Details",
                details.toString()
        );
    }


    // ============================================================
    // USER LIST
    // ============================================================

    private void showUserList() {

        firestore
                .collection("users")
                .get()
                .addOnSuccessListener(
                        this::displayUserList
                )
                .addOnFailureListener(
                        error ->
                                showMessage(
                                        "Platform Users",
                                        "Unable to load user records from Firebase."
                                )
                );
    }


    // ============================================================
    // DISPLAY USER LIST
    // ============================================================

    private void displayUserList(
            QuerySnapshot snapshot
    ) {

        if (snapshot.isEmpty()) {

            showMessage(
                    "Platform Users",
                    "No users are currently registered."
            );

            return;
        }


        List<DocumentSnapshot> users =
                new ArrayList<>(
                        snapshot.getDocuments()
                );


        Collections.sort(
                users,
                Comparator.comparing(
                        this::getUserName,
                        String.CASE_INSENSITIVE_ORDER
                )
        );


        String[] items =
                new String[users.size()];


        for (int i = 0;
             i < users.size();
             i++) {

            DocumentSnapshot doc =
                    users.get(i);

            String name =
                    getUserName(doc);

            String email =
                    getString(
                            doc,
                            "email"
                    );

            String role =
                    getString(
                            doc,
                            "role"
                    );


            items[i] =
                    name
                            + "\n"
                            + (
                            email.isEmpty()
                                    ? "Email: N/A"
                                    : email
                    )
                            + "\nRole: "
                            + (
                            role.isEmpty()
                                    ? "N/A"
                                    : role
                    );
        }


        new AlertDialog.Builder(this)
                .setTitle(
                        "Platform Users ("
                                + users.size()
                                + ")"
                )
                .setItems(
                        items,
                        (dialog, which) ->
                                showUserDetails(
                                        users.get(which)
                                )
                )
                .setNegativeButton(
                        "CLOSE",
                        null
                )
                .show();
    }


    // ============================================================
    // USER DETAILS
    // ============================================================

    private void showUserDetails(
            DocumentSnapshot doc
    ) {

        String name =
                getUserName(doc);

        String email =
                getString(
                        doc,
                        "email"
                );

        String role =
                getString(
                        doc,
                        "role"
                );

        String collegeId =
                getString(
                        doc,
                        "collegeId"
                );

        String departmentId =
                getString(
                        doc,
                        "departmentId"
                );

        String phone =
                getString(
                        doc,
                        "phone"
                );

        String active =
                getActiveStatus(doc);


        StringBuilder details =
                new StringBuilder();


        details.append(
                "Name: "
        ).append(
                name
        ).append(
                "\n\n"
        );


        details.append(
                "Email: "
        ).append(
                email.isEmpty()
                        ? "N/A"
                        : email
        ).append(
                "\n\n"
        );


        details.append(
                "Role: "
        ).append(
                role.isEmpty()
                        ? "N/A"
                        : role
        ).append(
                "\n\n"
        );


        details.append(
                "College ID: "
        ).append(
                collegeId.isEmpty()
                        ? "N/A"
                        : collegeId
        ).append(
                "\n\n"
        );


        details.append(
                "Department ID: "
        ).append(
                departmentId.isEmpty()
                        ? "N/A"
                        : departmentId
        ).append(
                "\n\n"
        );


        details.append(
                "Phone: "
        ).append(
                phone.isEmpty()
                        ? "N/A"
                        : phone
        ).append(
                "\n\n"
        );


        details.append(
                "Status: "
        ).append(
                active
        );


        showMessage(
                "User Details",
                details.toString()
        );
    }


    // ============================================================
    // TIMETABLE LIST
    // ============================================================

    private void showTimetableList() {

        firestore
                .collection("timetables")
                .get()
                .addOnSuccessListener(
                        this::displayTimetableList
                )
                .addOnFailureListener(
                        error ->
                                showMessage(
                                        "AI Timetables",
                                        "Unable to load timetable records from Firebase."
                                )
                );
    }


    // ============================================================
    // DISPLAY TIMETABLE LIST
    // ============================================================

    private void displayTimetableList(
            QuerySnapshot snapshot
    ) {

        if (snapshot.isEmpty()) {

            showMessage(
                    "AI Timetables",
                    "No timetable records are currently available."
            );

            return;
        }


        List<DocumentSnapshot> timetables =
                new ArrayList<>(
                        snapshot.getDocuments()
                );


        String[] items =
                new String[timetables.size()];


        for (int i = 0;
             i < timetables.size();
             i++) {

            DocumentSnapshot doc =
                    timetables.get(i);


            String title =
                    firstNonEmpty(
                            getString(
                                    doc,
                                    "title"
                            ),
                            getString(
                                    doc,
                                    "name"
                            ),
                            getString(
                                    doc,
                                    "timetableName"
                            ),
                            doc.getId()
                    );


            String collegeId =
                    getString(
                            doc,
                            "collegeId"
                    );


            String status =
                    firstNonEmpty(
                            getString(
                                    doc,
                                    "status"
                            ),
                            getString(
                                    doc,
                                    "state"
                            ),
                            "Available"
                    );


            items[i] =
                    title
                            + "\n"
                            + "College: "
                            + (
                            collegeId.isEmpty()
                                    ? "N/A"
                                    : collegeId
                    )
                            + "\nStatus: "
                            + status;
        }


        new AlertDialog.Builder(this)
                .setTitle(
                        "AI Timetables ("
                                + timetables.size()
                                + ")"
                )
                .setItems(
                        items,
                        (dialog, which) ->
                                showTimetableDetails(
                                        timetables.get(which)
                                )
                )
                .setNegativeButton(
                        "CLOSE",
                        null
                )
                .show();
    }


    // ============================================================
    // TIMETABLE DETAILS
    // ============================================================

    private void showTimetableDetails(
            DocumentSnapshot doc
    ) {

        StringBuilder details =
                new StringBuilder();


        Map<String, Object> data =
                doc.getData();


        if (data == null || data.isEmpty()) {

            details.append(
                    "No timetable details are available."
            );

        } else {

            details.append(
                    "Document ID: "
            ).append(
                    doc.getId()
            ).append(
                    "\n\n"
            );


            appendField(
                    details,
                    data,
                    "title"
            );


            appendField(
                    details,
                    data,
                    "name"
            );


            appendField(
                    details,
                    data,
                    "timetableName"
            );


            appendField(
                    details,
                    data,
                    "collegeId"
            );


            appendField(
                    details,
                    data,
                    "departmentId"
            );


            appendField(
                    details,
                    data,
                    "semesterId"
            );


            appendField(
                    details,
                    data,
                    "sectionId"
            );


            appendField(
                    details,
                    data,
                    "status"
            );


            appendField(
                    details,
                    data,
                    "createdAt"
            );
        }


        showMessage(
                "Timetable Details",
                details.toString()
        );
    }


    // ============================================================
    // STRING HELPER
    // ============================================================

    private String getString(
            DocumentSnapshot doc,
            String field
    ) {

        String value =
                doc.getString(field);

        return value == null
                ? ""
                : value.trim();
    }


    // ============================================================
    // COLLEGE NAME
    // ============================================================

    private String getCollegeName(
            DocumentSnapshot doc
    ) {

        return firstNonEmpty(
                getString(
                        doc,
                        "name"
                ),
                getString(
                        doc,
                        "collegeName"
                ),
                doc.getId()
        );
    }


    // ============================================================
    // COLLEGE STATUS
    // ============================================================

    private String getCollegeStatus(
            DocumentSnapshot doc
    ) {

        Boolean active =
                doc.getBoolean("active");


        if (active != null) {

            return active
                    ? "ACTIVE"
                    : "INACTIVE";
        }


        String status =
                getString(
                        doc,
                        "status"
                );


        return status.isEmpty()
                ? "UNKNOWN"
                : status;
    }


    // ============================================================
    // USER NAME
    // ============================================================

    private String getUserName(
            DocumentSnapshot doc
    ) {

        return firstNonEmpty(
                getString(
                        doc,
                        "name"
                ),
                getString(
                        doc,
                        "displayName"
                ),
                doc.getId()
        );
    }


    // ============================================================
    // USER ACTIVE STATUS
    // ============================================================

    private String getActiveStatus(
            DocumentSnapshot doc
    ) {

        Boolean active =
                doc.getBoolean("active");


        if (active != null) {

            return active
                    ? "ACTIVE"
                    : "INACTIVE";
        }


        String status =
                getString(
                        doc,
                        "status"
                );


        return status.isEmpty()
                ? "UNKNOWN"
                : status;
    }


    // ============================================================
    // FIRST NON-EMPTY VALUE
    // ============================================================

    private String firstNonEmpty(
            String... values
    ) {

        for (String value : values) {

            if (value != null
                    && !value.trim().isEmpty()) {

                return value.trim();
            }
        }


        return "N/A";
    }


    // ============================================================
    // APPEND TIMETABLE FIELD
    // ============================================================

    private void appendField(
            StringBuilder builder,
            Map<String, Object> data,
            String field
    ) {

        if (data.containsKey(field)) {

            Object value =
                    data.get(field);


            if (value != null) {

                builder.append(
                        field
                ).append(
                        ": "
                ).append(
                        String.valueOf(value)
                ).append(
                        "\n\n"
                );
            }
        }
    }


    // ============================================================
    // MESSAGE DIALOG
    // ============================================================

    private void showMessage(
            String title,
            String message
    ) {

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(
                        "OK",
                        null
                )
                .show();
    }


    // ============================================================
    // SYSTEM BACK
    // ============================================================

    @Override
    public void onBackPressed() {
        finish();
    }
}