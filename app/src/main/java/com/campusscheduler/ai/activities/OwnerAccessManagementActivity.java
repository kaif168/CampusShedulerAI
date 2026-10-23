package com.campusscheduler.ai.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.campusscheduler.ai.R;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.utils.FirebaseHelper;
import com.campusscheduler.ai.utils.LocalAuthManager;
import com.campusscheduler.ai.utils.UIUtils;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OwnerAccessManagementActivity extends AppCompatActivity {

    private EditText etOwnerName;
    private EditText etOwnerEmail;

    private Button btnAddOwner;
    private Button btnBack;

    private TextView tvOwnerCount;
    private TextView tvEmptyOwners;

    private ListView listViewOwners;

    private final List<String> ownerItems =
            new ArrayList<>();

    private final List<DocumentSnapshot> ownerDocuments =
            new ArrayList<>();

    private ArrayAdapter<String> ownerAdapter;

    private static final String COLLECTION =
            "officialOwners";

    private User currentOwner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(
                Color.rgb(13, 71, 161)
        );

        getWindow().setNavigationBarColor(
                Color.BLACK
        );

        setContentView(
                R.layout.activity_owner_access_management
        );

        currentOwner =
                LocalAuthManager.getCurrentLocalUser(this);

        if (currentOwner == null
                || !User.ROLE_OFFICIAL_OWNER.equals(
                currentOwner.getRole())) {

            UIUtils.showToast(
                    this,
                    "Official Owner access required."
            );

            finish();
            return;
        }

        initializeViews();
        setupList();
        setupButtons();
        loadOwners();
    }

    private void initializeViews() {

        etOwnerName =
                findViewById(R.id.etOwnerName);

        etOwnerEmail =
                findViewById(R.id.etOwnerEmail);

        btnAddOwner =
                findViewById(R.id.btnAddOwner);

        btnBack =
                findViewById(R.id.btnBack);

        tvOwnerCount =
                findViewById(R.id.tvOwnerCount);

        tvEmptyOwners =
                findViewById(R.id.tvEmptyOwners);

        listViewOwners =
                findViewById(R.id.listViewOwners);
    }

    private void setupList() {

        ownerAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_list_item_1,
                        ownerItems
                );

        listViewOwners.setAdapter(
                ownerAdapter
        );

        listViewOwners.setOnItemClickListener(
                (parent, view, position, id) -> {

                    if (position < 0
                            || position >= ownerDocuments.size()) {
                        return;
                    }

                    showOwnerActions(
                            ownerDocuments.get(position)
                    );
                }
        );
    }

    private void setupButtons() {

        btnBack.setOnClickListener(
                v -> finish()
        );

        btnAddOwner.setOnClickListener(
                v -> addOwner()
        );
    }

    private void addOwner() {

        String name =
                etOwnerName.getText()
                        .toString()
                        .trim();

        String email =
                etOwnerEmail.getText()
                        .toString()
                        .trim()
                        .toLowerCase(Locale.ROOT);

        if (TextUtils.isEmpty(name)) {

            etOwnerName.setError(
                    "Enter owner name"
            );

            etOwnerName.requestFocus();

            return;
        }

        if (TextUtils.isEmpty(email)) {

            etOwnerEmail.setError(
                    "Enter owner email"
            );

            etOwnerEmail.requestFocus();

            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS
                .matcher(email)
                .matches()) {

            etOwnerEmail.setError(
                    "Enter a valid email"
            );

            etOwnerEmail.requestFocus();

            return;
        }

        /*
         * Prevent the primary owner's email from being
         * duplicated in the owner access list.
         */
        if (email.equalsIgnoreCase(
                "mohammadkaif7795@gmail.com"
        )) {

            UIUtils.showToast(
                    this,
                    "Primary Official Owner is already authorized."
            );

            return;
        }

        btnAddOwner.setEnabled(false);
        btnAddOwner.setText("ADDING...");

        FirebaseHelper.getFirestore()
                .collection(COLLECTION)
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    if (!querySnapshot.isEmpty()) {

                        btnAddOwner.setEnabled(true);
                        btnAddOwner.setText("ADD OFFICIAL OWNER");

                        UIUtils.showToast(
                                this,
                                "This email is already in the Owner Access list."
                        );

                        return;
                    }

                    String documentId =
                            "OWNER_" + System.currentTimeMillis();

                    Map<String, Object> ownerData =
                            new HashMap<>();

                    ownerData.put(
                            "name",
                            name
                    );

                    ownerData.put(
                            "email",
                            email
                    );

                    ownerData.put(
                            "role",
                            User.ROLE_OFFICIAL_OWNER
                    );

                    ownerData.put(
                            "active",
                            true
                    );

                    ownerData.put(
                            "ownerType",
                            "TEAM_OWNER"
                    );

                    ownerData.put(
                            "createdByUid",
                            currentOwner.getUid()
                    );

                    ownerData.put(
                            "createdByEmail",
                            currentOwner.getEmail()
                    );

                    ownerData.put(
                            "createdAt",
                            FieldValue.serverTimestamp()
                    );

                    FirebaseHelper.getFirestore()
                            .collection(COLLECTION)
                            .document(documentId)
                            .set(ownerData)
                            .addOnSuccessListener(unused -> {

                                etOwnerName.setText("");
                                etOwnerEmail.setText("");

                                btnAddOwner.setEnabled(true);
                                btnAddOwner.setText(
                                        "ADD OFFICIAL OWNER"
                                );

                                UIUtils.showToast(
                                        this,
                                        "Official Owner access added."
                                );

                                loadOwners();
                            })
                            .addOnFailureListener(error -> {

                                btnAddOwner.setEnabled(true);
                                btnAddOwner.setText(
                                        "ADD OFFICIAL OWNER"
                                );

                                UIUtils.showToast(
                                        this,
                                        "Failed to add owner: "
                                                + error.getMessage()
                                );
                            });
                })
                .addOnFailureListener(error -> {

                    btnAddOwner.setEnabled(true);
                    btnAddOwner.setText(
                            "ADD OFFICIAL OWNER"
                    );

                    UIUtils.showToast(
                            this,
                            "Could not check owner email: "
                                    + error.getMessage()
                    );
                });
    }

    private void loadOwners() {

        ownerItems.clear();
        ownerDocuments.clear();

        ownerAdapter.notifyDataSetChanged();

        FirebaseHelper.getFirestore()
                .collection(COLLECTION)
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    for (DocumentSnapshot document
                            : querySnapshot.getDocuments()) {

                        String name =
                                getString(
                                        document,
                                        "name",
                                        "Official Owner"
                                );

                        String email =
                                getString(
                                        document,
                                        "email",
                                        "Unknown email"
                                );

                        String role =
                                getString(
                                        document,
                                        "role",
                                        ""
                                );

                        Boolean active =
                                document.getBoolean("active");

                        String status =
                                Boolean.TRUE.equals(active)
                                        ? "ACTIVE"
                                        : "INACTIVE";

                        String type =
                                getString(
                                        document,
                                        "ownerType",
                                        "TEAM_OWNER"
                                );

                        String displayText =
                                name
                                        + "\n"
                                        + email
                                        + "\n"
                                        + "Role: "
                                        + role
                                        + "\n"
                                        + "Type: "
                                        + type
                                        + " • "
                                        + status;

                        ownerDocuments.add(document);
                        ownerItems.add(displayText);
                    }

                    ownerAdapter.notifyDataSetChanged();

                    tvOwnerCount.setText(
                            ownerDocuments.size()
                                    + " Authorized Owner(s)"
                    );

                    if (ownerDocuments.isEmpty()) {

                        tvEmptyOwners.setVisibility(
                                View.VISIBLE
                        );

                        tvEmptyOwners.setText(
                                "No additional Official Owners added yet."
                        );

                    } else {

                        tvEmptyOwners.setVisibility(
                                View.GONE
                        );
                    }
                })
                .addOnFailureListener(error -> {

                    tvOwnerCount.setText(
                            "Unable to load owners"
                    );

                    tvEmptyOwners.setVisibility(
                            View.VISIBLE
                    );

                    tvEmptyOwners.setText(
                            "Could not load Owner Access.\n"
                                    + error.getMessage()
                    );
                });
    }

    private void showOwnerActions(
            DocumentSnapshot document
    ) {

        String name =
                getString(
                        document,
                        "name",
                        "Official Owner"
                );

        String email =
                getString(
                        document,
                        "email",
                        ""
                );

        Boolean active =
                document.getBoolean("active");

        boolean isActive =
                Boolean.TRUE.equals(active);

        String action =
                isActive
                        ? "Deactivate Owner"
                        : "Activate Owner";

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(name)
                .setMessage(
                        email
                                + "\n\n"
                                + "Current status: "
                                + (isActive
                                ? "ACTIVE"
                                : "INACTIVE")
                )
                .setPositiveButton(
                        action,
                        (dialog, which) ->
                                changeOwnerStatus(
                                        document,
                                        !isActive
                                )
                )
                .setNegativeButton(
                        "Cancel",
                        null
                )
                .show();
    }

    private void changeOwnerStatus(
            DocumentSnapshot document,
            boolean newStatus
    ) {

        FirebaseHelper.getFirestore()
                .collection(COLLECTION)
                .document(document.getId())
                .update(
                        "active",
                        newStatus
                )
                .addOnSuccessListener(unused -> {

                    UIUtils.showToast(
                            this,
                            newStatus
                                    ? "Owner activated."
                                    : "Owner deactivated."
                    );

                    loadOwners();
                })
                .addOnFailureListener(error -> {

                    UIUtils.showToast(
                            this,
                            "Could not update Owner status: "
                                    + error.getMessage()
                    );
                });
    }

    private String getString(
            DocumentSnapshot document,
            String field,
            String fallback
    ) {

        String value =
                document.getString(field);

        if (value == null
                || value.trim().isEmpty()) {

            return fallback;
        }

        return value;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}

