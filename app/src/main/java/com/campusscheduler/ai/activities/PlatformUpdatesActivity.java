package com.campusscheduler.ai.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.campusscheduler.ai.R;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.repository.FirebaseHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PlatformUpdatesActivity extends AppCompatActivity {

    private EditText etUpdateTitle;
    private EditText etVersion;
    private EditText etReleaseNotes;

    private Spinner spinnerUpdateType;
    private Spinner spinnerAudience;

    private Button btnPublishUpdate;

    private TextView tvHistoryCount;
    private TextView tvEmptyHistory;

    private ListView listViewUpdateHistory;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    private final List<String> historyItems = new ArrayList<>();
    private ArrayAdapter<String> historyAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_platform_updates);

        setupSystemBars();
        initializeFirebase();
        initializeViews();
        setupSpinners();
        setupHistoryList();
        setupPublishButton();

        loadUpdateHistory();
    }


    // ============================================================
    // SYSTEM BARS
    // ============================================================

    private void setupSystemBars() {

        getWindow().setStatusBarColor(
                Color.rgb(13, 71, 161)
        );

        getWindow().setNavigationBarColor(
                Color.BLACK
        );

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
    }


    // ============================================================
    // FIREBASE
    // ============================================================

    private void initializeFirebase() {

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
    }


    // ============================================================
    // VIEWS
    // ============================================================

    private void initializeViews() {

        etUpdateTitle = findViewById(
                R.id.etUpdateTitle
        );

        etVersion = findViewById(
                R.id.etVersion
        );

        etReleaseNotes = findViewById(
                R.id.etReleaseNotes
        );

        spinnerUpdateType = findViewById(
                R.id.spinnerUpdateType
        );

        spinnerAudience = findViewById(
                R.id.spinnerAudience
        );

        btnPublishUpdate = findViewById(
                R.id.btnPublishUpdate
        );

        tvHistoryCount = findViewById(
                R.id.tvHistoryCount
        );

        tvEmptyHistory = findViewById(
                R.id.tvEmptyHistory
        );

        listViewUpdateHistory = findViewById(
                R.id.listViewUpdateHistory
        );
    }


    // ============================================================
    // SPINNERS
    // ============================================================

    private void setupSpinners() {

        String[] updateTypes = {
                "New Feature",
                "Maintenance",
                "Important Update",
                "Version Announcement"
        };

        ArrayAdapter<String> typeAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        updateTypes
                );

        typeAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerUpdateType.setAdapter(
                typeAdapter
        );


        String[] audiences = {
                "All Registered Colleges",
                "Active Colleges Only"
        };

        ArrayAdapter<String> audienceAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        audiences
                );

        audienceAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerAudience.setAdapter(
                audienceAdapter
        );
    }


    // ============================================================
    // HISTORY LIST
    // ============================================================

    private void setupHistoryList() {

        historyAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_list_item_1,
                        historyItems
                );

        listViewUpdateHistory.setAdapter(
                historyAdapter
        );

        listViewUpdateHistory.setOnItemClickListener(
                (parent, view, position, id) -> {

                    Toast.makeText(
                            PlatformUpdatesActivity.this,
                            "Published update history item",
                            Toast.LENGTH_SHORT
                    ).show();
                }
        );
    }


    // ============================================================
    // PUBLISH BUTTON
    // ============================================================

    private void setupPublishButton() {

        btnPublishUpdate.setOnClickListener(
                view -> publishPlatformUpdate()
        );
    }


    // ============================================================
    // PUBLISH UPDATE
    // ============================================================

    private void publishPlatformUpdate() {

        String title =
                etUpdateTitle.getText()
                        .toString()
                        .trim();

        String version =
                etVersion.getText()
                        .toString()
                        .trim();

        String releaseNotes =
                etReleaseNotes.getText()
                        .toString()
                        .trim();


        // --------------------------------------------------------
        // VALIDATION
        // --------------------------------------------------------

        if (TextUtils.isEmpty(title)) {

            etUpdateTitle.setError(
                    "Enter update title"
            );

            etUpdateTitle.requestFocus();

            return;
        }


        if (TextUtils.isEmpty(version)) {

            etVersion.setError(
                    "Enter version"
            );

            etVersion.requestFocus();

            return;
        }


        if (TextUtils.isEmpty(releaseNotes)) {

            etReleaseNotes.setError(
                    "Enter release notes"
            );

            etReleaseNotes.requestFocus();

            return;
        }


        // --------------------------------------------------------
        // CURRENT USER
        // --------------------------------------------------------

        FirebaseUser currentUser =
                firebaseAuth.getCurrentUser();


        if (currentUser == null) {

            Toast.makeText(
                    this,
                    "Owner session not found. Please login again.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }


        // --------------------------------------------------------
        // TYPE
        // --------------------------------------------------------

        String updateType =
                spinnerUpdateType
                        .getSelectedItem()
                        .toString();

        String updateTypeCode =
                getUpdateTypeCode(
                        updateType
                );


        // --------------------------------------------------------
        // AUDIENCE
        // --------------------------------------------------------

        String audience =
                spinnerAudience
                        .getSelectedItem()
                        .toString();

        String audienceCode =
                getAudienceCode(
                        audience
                );


        // --------------------------------------------------------
        // DOCUMENT ID
        // --------------------------------------------------------

        String documentId =
                "UPDATE_"
                        + System.currentTimeMillis();


        // --------------------------------------------------------
        // DATA
        // --------------------------------------------------------

        Map<String, Object> updateData =
                new HashMap<>();


        updateData.put(
                "title",
                title
        );

        updateData.put(
                "version",
                version
        );

        updateData.put(
                "releaseNotes",
                releaseNotes
        );

        updateData.put(
                "updateType",
                updateType
        );

        updateData.put(
                "updateTypeCode",
                updateTypeCode
        );

        updateData.put(
                "audience",
                audience
        );

        updateData.put(
                "audienceCode",
                audienceCode
        );

        updateData.put(
                "publishedByUid",
                currentUser.getUid()
        );

        updateData.put(
                "publishedByEmail",
                currentUser.getEmail() == null
                        ? ""
                        : currentUser.getEmail()
        );

        updateData.put(
                "publishedByRole",
                User.ROLE_OFFICIAL_OWNER
        );

        updateData.put(
                "published",
                true
        );

        updateData.put(
                "active",
                true
        );

        updateData.put(
                "publishedAt",
                FieldValue.serverTimestamp()
        );

        updateData.put(
                "createdAt",
                FieldValue.serverTimestamp()
        );


        // --------------------------------------------------------
        // BUTTON
        // --------------------------------------------------------

        btnPublishUpdate.setEnabled(false);

        btnPublishUpdate.setText(
                "PUBLISHING..."
        );


        // --------------------------------------------------------
        // FIRESTORE
        // --------------------------------------------------------

        firestore
                .collection(
                        FirebaseHelper.COL_PLATFORM_UPDATES
                )
                .document(documentId)
                .set(updateData)
                .addOnSuccessListener(unused -> {

                    btnPublishUpdate.setEnabled(
                            true
                    );

                    btnPublishUpdate.setText(
                            "PUBLISH PLATFORM UPDATE"
                    );


                    Toast.makeText(
                            PlatformUpdatesActivity.this,
                            "Platform update published successfully!",
                            Toast.LENGTH_LONG
                    ).show();


                    clearPublishForm();

                    loadUpdateHistory();

                })
                .addOnFailureListener(error -> {

                    btnPublishUpdate.setEnabled(
                            true
                    );

                    btnPublishUpdate.setText(
                            "PUBLISH PLATFORM UPDATE"
                    );


                    Toast.makeText(
                            PlatformUpdatesActivity.this,
                            "Failed to publish update: "
                                    + error.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }


    // ============================================================
    // UPDATE TYPE CODE
    // ============================================================

    private String getUpdateTypeCode(
            String type
    ) {

        if ("New Feature".equals(type)) {

            return "NEW_FEATURE";
        }

        if ("Maintenance".equals(type)) {

            return "MAINTENANCE";
        }

        if ("Important Update".equals(type)) {

            return "IMPORTANT_UPDATE";
        }

        if ("Version Announcement".equals(type)) {

            return "VERSION_ANNOUNCEMENT";
        }

        return "GENERAL";
    }


    // ============================================================
    // AUDIENCE CODE
    // ============================================================

    private String getAudienceCode(
            String audience
    ) {

        if ("All Registered Colleges".equals(audience)) {

            return "ALL_COLLEGES";
        }

        if ("Active Colleges Only".equals(audience)) {

            return "ACTIVE_COLLEGES";
        }

        return "ALL_COLLEGES";
    }


    // ============================================================
    // CLEAR FORM
    // ============================================================

    private void clearPublishForm() {

        etUpdateTitle.setText("");

        etVersion.setText("");

        etReleaseNotes.setText("");

        spinnerUpdateType.setSelection(0);

        spinnerAudience.setSelection(0);
    }


    // ============================================================
    // LOAD HISTORY
    // ============================================================

    private void loadUpdateHistory() {

        historyItems.clear();

        historyAdapter.notifyDataSetChanged();

        tvEmptyHistory.setVisibility(
                View.GONE
        );


        firestore
                .collection(
                        FirebaseHelper.COL_PLATFORM_UPDATES
                )
                .get()
                .addOnSuccessListener(
                        querySnapshot -> {

                            List<DocumentSnapshot> documents =
                                    new ArrayList<>(
                                            querySnapshot.getDocuments()
                                    );


                            Collections.sort(
                                    documents,
                                    new Comparator<DocumentSnapshot>() {

                                        @Override
                                        public int compare(
                                                DocumentSnapshot first,
                                                DocumentSnapshot second
                                        ) {

                                            long firstTime =
                                                    getTimestampMillis(
                                                            first.get(
                                                                    "publishedAt"
                                                            )
                                                    );

                                            long secondTime =
                                                    getTimestampMillis(
                                                            second.get(
                                                                    "publishedAt"
                                                            )
                                                    );


                                            return Long.compare(
                                                    secondTime,
                                                    firstTime
                                            );
                                        }
                                    }
                            );


                            // ------------------------------------
                            // EMPTY
                            // ------------------------------------

                            if (documents.isEmpty()) {

                                tvHistoryCount.setText(
                                        "0 Published Updates"
                                );

                                tvEmptyHistory.setText(
                                        "No platform updates published yet."
                                );

                                tvEmptyHistory.setVisibility(
                                        View.VISIBLE
                                );

                                historyAdapter.notifyDataSetChanged();

                                return;
                            }


                            // ------------------------------------
                            // HISTORY
                            // ------------------------------------

                            for (DocumentSnapshot document :
                                    documents) {

                                String title =
                                        getStringValue(
                                                document,
                                                "title",
                                                "Untitled Update"
                                        );


                                String version =
                                        getStringValue(
                                                document,
                                                "version",
                                                "N/A"
                                        );


                                String updateType =
                                        getStringValue(
                                                document,
                                                "updateType",
                                                "General"
                                        );


                                String audience =
                                        getStringValue(
                                                document,
                                                "audience",
                                                "All Registered Colleges"
                                        );


                                String publisher =
                                        getStringValue(
                                                document,
                                                "publishedByEmail",
                                                "Official Owner"
                                        );


                                String dateText =
                                        formatTimestamp(
                                                document.get(
                                                        "publishedAt"
                                                )
                                        );


                                String historyText =
                                        title
                                                + "\nVersion: "
                                                + version
                                                + "\nType: "
                                                + updateType
                                                + "\nAudience: "
                                                + audience
                                                + "\nPublished by: "
                                                + publisher
                                                + "\nPublished: "
                                                + dateText;


                                historyItems.add(
                                        historyText
                                );
                            }


                            tvHistoryCount.setText(
                                    documents.size()
                                            + " Published Updates"
                            );


                            tvEmptyHistory.setVisibility(
                                    View.GONE
                            );


                            historyAdapter.notifyDataSetChanged();

                        }
                )
                .addOnFailureListener(
                        error -> {

                            tvHistoryCount.setText(
                                    "Unable to load history"
                            );

                            tvEmptyHistory.setText(
                                    "Could not load published updates.\n"
                                            + error.getMessage()
                            );

                            tvEmptyHistory.setVisibility(
                                    View.VISIBLE
                            );
                        }
                );
    }


    // ============================================================
    // TIMESTAMP HANDLING
    // ============================================================

    private long getTimestampMillis(
            Object value
    ) {

        if (value == null) {

            return 0L;
        }


        // Firestore Timestamp can be handled
        // without importing the Timestamp class.

        try {

            java.lang.reflect.Method toDateMethod =
                    value.getClass()
                            .getMethod("toDate");


            Object result =
                    toDateMethod.invoke(value);


            if (result instanceof Date) {

                return ((Date) result).getTime();
            }

        } catch (Exception ignored) {
        }


        if (value instanceof Date) {

            return ((Date) value).getTime();
        }


        if (value instanceof Long) {

            return (Long) value;
        }


        if (value instanceof Number) {

            return ((Number) value).longValue();
        }


        return 0L;
    }


    // ============================================================
    // FORMAT DATE
    // ============================================================

    private String formatTimestamp(
            Object value
    ) {

        long millis =
                getTimestampMillis(value);


        if (millis <= 0L) {

            return "Processing timestamp...";
        }


        Date date =
                new Date(millis);


        SimpleDateFormat formatter =
                new SimpleDateFormat(
                        "dd MMM yyyy, hh:mm a",
                        Locale.getDefault()
                );


        return formatter.format(date);
    }


    // ============================================================
    // SAFE STRING
    // ============================================================

    private String getStringValue(
            DocumentSnapshot document,
            String field,
            String fallback
    ) {

        String value =
                document.getString(field);


        if (value == null ||
                value.trim().isEmpty()) {

            return fallback;
        }


        return value;
    }


    // ============================================================
    // BACK
    // ============================================================

    @Override
    public void onBackPressed() {

        super.onBackPressed();
    }
}