package com.campusscheduler.ai.activities;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.campusscheduler.ai.R;
import com.campusscheduler.ai.models.College;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.utils.FirebaseHelper;
import com.campusscheduler.ai.utils.UIUtils;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CollegeManagementActivity extends AppCompatActivity {

    private ImageButton btnBack;

    private EditText etCollegeName;
    private EditText etCollegeCode;
    private EditText etAddress;
    private EditText etContactEmail;
    private EditText etContactPhone;

    private EditText etSearchCollege;
    private Spinner spinnerStatusFilter;

    private Button btnAddCollege;

    private TextView tvCollegeCount;
    private TextView tvEmptyState;

    private ListView listViewColleges;

    private final List<College> allCollegeList = new ArrayList<>();
    private final List<College> filteredCollegeList = new ArrayList<>();

    private ArrayAdapter<String> adapter;
    private final List<String> displayStrings = new ArrayList<>();

    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.setStatusBarColor(Color.rgb(13, 71, 161));
        window.setNavigationBarColor(Color.BLACK);

        setContentView(R.layout.activity_college_management);

        initializeViews();
        setupStatusFilter();
        setupListeners();

        loadCollegesFromFirestore();
    }

    private void initializeViews() {

        btnBack = findViewById(R.id.btnBack);

        etCollegeName = findViewById(R.id.etCollegeName);
        etCollegeCode = findViewById(R.id.etCollegeCode);
        etAddress = findViewById(R.id.etAddress);
        etContactEmail = findViewById(R.id.etContactEmail);
        etContactPhone = findViewById(R.id.etContactPhone);

        etSearchCollege = findViewById(R.id.etSearchCollege);
        spinnerStatusFilter = findViewById(R.id.spinnerStatusFilter);

        btnAddCollege = findViewById(R.id.btnAddCollege);

        tvCollegeCount = findViewById(R.id.tvCollegeCount);
        tvEmptyState = findViewById(R.id.tvEmptyState);

        listViewColleges = findViewById(R.id.listViewColleges);

        adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                displayStrings
        );

        listViewColleges.setAdapter(adapter);
    }

    private void setupStatusFilter() {

        String[] filters = {
                "All Colleges",
                "Active Colleges",
                "Inactive Colleges"
        };

        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                filters
        );

        filterAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerStatusFilter.setAdapter(filterAdapter);
    }

    private void setupListeners() {

        btnBack.setOnClickListener(v -> finish());

        btnAddCollege.setOnClickListener(v -> addCollegeToFirestore());

        spinnerStatusFilter.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            android.widget.AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {
                        applyFilters();
                    }

                    @Override
                    public void onNothingSelected(
                            android.widget.AdapterView<?> parent
                    ) {
                        applyFilters();
                    }
                }
        );

        etSearchCollege.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(
                    CharSequence s,
                    int start,
                    int count,
                    int after
            ) {
            }

            @Override
            public void onTextChanged(
                    CharSequence s,
                    int start,
                    int before,
                    int count
            ) {
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        listViewColleges.setOnItemClickListener(
                (parent, view, position, id) -> {

                    if (position >= 0 &&
                            position < filteredCollegeList.size()) {

                        College selectedCollege =
                                filteredCollegeList.get(position);

                        showCollegeActionDialog(selectedCollege);
                    }
                }
        );
    }

    // ============================================================
    // LOAD COLLEGES
    // ============================================================

    private void loadCollegesFromFirestore() {

        if (isLoading) {
            return;
        }

        isLoading = true;

        tvEmptyState.setVisibility(View.GONE);

        FirebaseHelper.getCollegesRef()
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    isLoading = false;

                    allCollegeList.clear();

                    for (DocumentSnapshot document : querySnapshot) {

                        College college =
                                document.toObject(College.class);

                        if (college == null) {
                            continue;
                        }

                        if (college.getId() == null ||
                                college.getId().trim().isEmpty()) {

                            college.setId(document.getId());
                        }

                        allCollegeList.add(college);
                    }

                    applyFilters();

                })
                .addOnFailureListener(error -> {

                    isLoading = false;

                    UIUtils.showToast(
                            this,
                            "Failed to load colleges: "
                                    + getSafeErrorMessage(error)
                    );

                    tvEmptyState.setText(
                            "Unable to load college data.\nPlease try again."
                    );

                    tvEmptyState.setVisibility(View.VISIBLE);
                });
    }

    // ============================================================
    // ADD COLLEGE
    // ============================================================

    private void addCollegeToFirestore() {

        if (isLoading) {
            return;
        }

        String name = getTrimmedText(etCollegeName);

        String code =
                getTrimmedText(etCollegeCode)
                        .toUpperCase(Locale.ROOT);

        String address = getTrimmedText(etAddress);
        String email = getTrimmedText(etContactEmail);
        String phone = getTrimmedText(etContactPhone);

        if (name.isEmpty()) {

            etCollegeName.setError(
                    "College name is required"
            );

            etCollegeName.requestFocus();
            return;
        }

        if (code.isEmpty()) {

            etCollegeCode.setError(
                    "College code is required"
            );

            etCollegeCode.requestFocus();
            return;
        }

        if (!email.isEmpty() &&
                !Patterns.EMAIL_ADDRESS
                        .matcher(email)
                        .matches()) {

            etContactEmail.setError(
                    "Enter a valid email address"
            );

            etContactEmail.requestFocus();
            return;
        }

        if (isDuplicateCollegeCode(code, null)) {

            etCollegeCode.setError(
                    "This college code is already registered"
            );

            etCollegeCode.requestFocus();

            Toast.makeText(
                    this,
                    "College code already exists.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String collegeId =
                "COLLEGE_" + System.currentTimeMillis();

        College college = new College(
                collegeId,
                name,
                code,
                address,
                email,
                phone
        );

        setLoadingState(true);

        FirebaseHelper.getCollegesRef()
                .document(collegeId)
                .set(college)
                .addOnSuccessListener(unused -> {

                    setLoadingState(false);

                    Toast.makeText(
                            this,
                            "College onboarded successfully.",
                            Toast.LENGTH_SHORT
                    ).show();

                    clearCollegeForm();

                    loadCollegesFromFirestore();
                })
                .addOnFailureListener(error -> {

                    setLoadingState(false);

                    UIUtils.showToast(
                            this,
                            "Failed to onboard college: "
                                    + getSafeErrorMessage(error)
                    );
                });
    }

    // ============================================================
    // DUPLICATE COLLEGE CODE
    // ============================================================

    private boolean isDuplicateCollegeCode(
            String code,
            String ignoreCollegeId
    ) {

        if (code == null ||
                code.trim().isEmpty()) {

            return false;
        }

        String normalizedCode =
                code.trim().toUpperCase(Locale.ROOT);

        for (College college : allCollegeList) {

            if (college == null ||
                    college.getCode() == null) {

                continue;
            }

            String existingCode =
                    college.getCode()
                            .trim()
                            .toUpperCase(Locale.ROOT);

            if (!existingCode.equals(normalizedCode)) {
                continue;
            }

            if (ignoreCollegeId != null &&
                    ignoreCollegeId.equals(college.getId())) {

                continue;
            }

            return true;
        }

        return false;
    }

    // ============================================================
    // SEARCH + FILTER
    // ============================================================

    private void applyFilters() {

        filteredCollegeList.clear();
        displayStrings.clear();

        String searchText =
                getTrimmedText(etSearchCollege)
                        .toLowerCase(Locale.ROOT);

        int selectedFilter =
                spinnerStatusFilter == null
                        ? 0
                        : spinnerStatusFilter
                        .getSelectedItemPosition();

        for (College college : allCollegeList) {

            if (college == null) {
                continue;
            }

            if (selectedFilter == 1 &&
                    !college.isActive()) {

                continue;
            }

            if (selectedFilter == 2 &&
                    college.isActive()) {

                continue;
            }

            if (!searchText.isEmpty()) {

                String name =
                        safeLower(college.getName());

                String code =
                        safeLower(college.getCode());

                String address =
                        safeLower(college.getAddress());

                String email =
                        safeLower(college.getContactEmail());

                String phone =
                        safeLower(college.getContactPhone());

                boolean matches =
                        name.contains(searchText)
                                || code.contains(searchText)
                                || address.contains(searchText)
                                || email.contains(searchText)
                                || phone.contains(searchText);

                if (!matches) {
                    continue;
                }
            }

            filteredCollegeList.add(college);

            String status =
                    college.isActive()
                            ? "ACTIVE"
                            : "INACTIVE";

            String code =
                    college.getCode() == null
                            ? "No code"
                            : college.getCode();

            String address =
                    college.getAddress() == null
                            ? ""
                            : college.getAddress();

            String display =
                    safeText(
                            college.getName(),
                            "Unnamed College"
                    )
                            + " • "
                            + code
                            + "\nStatus: "
                            + status;

            if (!address.trim().isEmpty()) {
                display += "\n" + address;
            }

            displayStrings.add(display);
        }

        adapter.notifyDataSetChanged();

        updateCollegeCount();
        updateEmptyState();
    }

    private void updateCollegeCount() {

        int visibleCount =
                filteredCollegeList.size();

        int totalCount =
                allCollegeList.size();

        if (tvCollegeCount != null) {

            tvCollegeCount.setText(
                    "Showing "
                            + visibleCount
                            + " of "
                            + totalCount
                            + " registered colleges"
            );
        }
    }

    private void updateEmptyState() {

        if (filteredCollegeList.isEmpty()) {

            if (allCollegeList.isEmpty()) {

                tvEmptyState.setText(
                        "No colleges registered yet.\n"
                                + "Use the onboarding form above to add the first college."
                );

            } else {

                tvEmptyState.setText(
                        "No colleges match the current search/filter."
                );
            }

            tvEmptyState.setVisibility(View.VISIBLE);

        } else {

            tvEmptyState.setVisibility(View.GONE);
        }
    }

    // ============================================================
    // COLLEGE ACTION DIALOG
    // ============================================================

    private void showCollegeActionDialog(College college) {

        if (college == null) {
            return;
        }

        /*
         * IMPORTANT FIX:
         *
         * Do NOT use setMessage() together with setItems()
         * here. On the previous version the message was displayed
         * but the action items were not visible correctly.
         *
         * The college information is now included in the title.
         */

        String status =
                college.isActive()
                        ? "ACTIVE"
                        : "INACTIVE";

        String title =
                safeText(
                        college.getName(),
                        "College"
                )
                        + "\n"
                        + "Code: "
                        + safeText(
                        college.getCode(),
                        "N/A"
                )
                        + " • "
                        + status;

        String[] actions = {
                "View Details",
                "Edit College",
                college.isActive()
                        ? "Deactivate College"
                        : "Activate College",
                "Assign College Admin"
        };

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(title)
                        .setItems(
                                actions,
                                (d, which) -> {

                                    switch (which) {

                                        case 0:
                                            showCollegeDetails(
                                                    college
                                            );
                                            break;

                                        case 1:
                                            showEditCollegeDialog(
                                                    college
                                            );
                                            break;

                                        case 2:
                                            toggleCollegeStatus(
                                                    college
                                            );
                                            break;

                                        case 3:
                                            showAssignCollegeAdminDialog(
                                                    college
                                            );
                                            break;
                                    }
                                }
                        )
                        .setNegativeButton(
                                "CANCEL",
                                null
                        )
                        .create();

        dialog.show();
    }

    // ============================================================
    // VIEW DETAILS
    // ============================================================

    private void showCollegeDetails(College college) {

        String phone =
                safeText(
                        college.getContactPhone(),
                        "Not provided"
                );

        String email =
                safeText(
                        college.getContactEmail(),
                        "Not provided"
                );

        String address =
                safeText(
                        college.getAddress(),
                        "Not provided"
                );

        String code =
                safeText(
                        college.getCode(),
                        "Not provided"
                );

        String id =
                safeText(
                        college.getId(),
                        "Not available"
                );

        String status =
                college.isActive()
                        ? "ACTIVE"
                        : "INACTIVE";

        String created =
                college.getCreatedAt() > 0
                        ? android.text.format.DateFormat
                        .format(
                                "dd MMM yyyy, hh:mm a",
                                college.getCreatedAt()
                        )
                        .toString()
                        : "Not available";

        String message =
                "College ID\n"
                        + id
                        + "\n\n"
                        + "College Code\n"
                        + code
                        + "\n\n"
                        + "Status\n"
                        + status
                        + "\n\n"
                        + "Address\n"
                        + address
                        + "\n\n"
                        + "Contact Email\n"
                        + email
                        + "\n\n"
                        + "Contact Phone\n"
                        + phone
                        + "\n\n"
                        + "Created\n"
                        + created;

        new AlertDialog.Builder(this)
                .setTitle(
                        safeText(
                                college.getName(),
                                "College Details"
                        )
                )
                .setMessage(message)
                .setPositiveButton(
                        "CLOSE",
                        null
                )
                .show();
    }

    // ============================================================
    // EDIT COLLEGE
    // ============================================================

    private void showEditCollegeDialog(
            College college
    ) {

        View dialogView =
                getLayoutInflater().inflate(
                        R.layout.dialog_edit_college,
                        null
                );

        EditText etName =
                dialogView.findViewById(
                        R.id.etEditCollegeName
                );

        EditText etCode =
                dialogView.findViewById(
                        R.id.etEditCollegeCode
                );

        EditText etAddressEdit =
                dialogView.findViewById(
                        R.id.etEditCollegeAddress
                );

        EditText etEmail =
                dialogView.findViewById(
                        R.id.etEditCollegeEmail
                );

        EditText etPhone =
                dialogView.findViewById(
                        R.id.etEditCollegePhone
                );

        etName.setText(
                safeText(
                        college.getName(),
                        ""
                )
        );

        etCode.setText(
                safeText(
                        college.getCode(),
                        ""
                )
        );

        etAddressEdit.setText(
                safeText(
                        college.getAddress(),
                        ""
                )
        );

        etEmail.setText(
                safeText(
                        college.getContactEmail(),
                        ""
                )
        );

        etPhone.setText(
                safeText(
                        college.getContactPhone(),
                        ""
                )
        );

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle("Edit College")
                        .setView(dialogView)
                        .setNegativeButton(
                                "CANCEL",
                                null
                        )
                        .setPositiveButton(
                                "SAVE CHANGES",
                                null
                        )
                        .create();

        dialog.setOnShowListener(d -> {

            Button positiveButton =
                    dialog.getButton(
                            AlertDialog.BUTTON_POSITIVE
                    );

            positiveButton.setOnClickListener(v -> {

                String name =
                        etName.getText()
                                .toString()
                                .trim();

                String code =
                        etCode.getText()
                                .toString()
                                .trim()
                                .toUpperCase(
                                        Locale.ROOT
                                );

                String address =
                        etAddressEdit.getText()
                                .toString()
                                .trim();

                String email =
                        etEmail.getText()
                                .toString()
                                .trim();

                String phone =
                        etPhone.getText()
                                .toString()
                                .trim();

                if (name.isEmpty()) {

                    etName.setError(
                            "College name is required"
                    );

                    return;
                }

                if (code.isEmpty()) {

                    etCode.setError(
                            "College code is required"
                    );

                    return;
                }

                if (!email.isEmpty() &&
                        !Patterns.EMAIL_ADDRESS
                                .matcher(email)
                                .matches()) {

                    etEmail.setError(
                            "Enter a valid email address"
                    );

                    return;
                }

                if (isDuplicateCollegeCode(
                        code,
                        college.getId()
                )) {

                    etCode.setError(
                            "This college code is already registered"
                    );

                    return;
                }

                college.setName(name);
                college.setCode(code);
                college.setAddress(address);
                college.setContactEmail(email);
                college.setContactPhone(phone);

                FirebaseHelper.getCollegesRef()
                        .document(college.getId())
                        .set(college)
                        .addOnSuccessListener(
                                unused -> {

                                    Toast.makeText(
                                            this,
                                            "College updated successfully.",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    dialog.dismiss();

                                    loadCollegesFromFirestore();
                                }
                        )
                        .addOnFailureListener(
                                error -> {

                                    UIUtils.showToast(
                                            this,
                                            "Update failed: "
                                                    + getSafeErrorMessage(
                                                    error
                                            )
                                    );
                                }
                        );
            });
        });

        dialog.show();
    }

    // ============================================================
    // ACTIVE / INACTIVE
    // ============================================================

    private void toggleCollegeStatus(
            College college
    ) {

        if (college == null ||
                college.getId() == null ||
                college.getId().trim().isEmpty()) {

            Toast.makeText(
                    this,
                    "College ID is missing.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        boolean newStatus =
                !college.isActive();

        String action =
                newStatus
                        ? "activate"
                        : "deactivate";

        new AlertDialog.Builder(this)
                .setTitle(
                        newStatus
                                ? "Activate College?"
                                : "Deactivate College?"
                )
                .setMessage(
                        "Are you sure you want to "
                                + action
                                + " \""
                                + safeText(
                                college.getName(),
                                "this college"
                        )
                                + "\"?"
                )
                .setNegativeButton(
                        "CANCEL",
                        null
                )
                .setPositiveButton(
                        newStatus
                                ? "ACTIVATE"
                                : "DEACTIVATE",
                        (dialog, which) -> {

                            FirebaseHelper
                                    .getCollegesRef()
                                    .document(
                                            college.getId()
                                    )
                                    .update(
                                            "active",
                                            newStatus
                                    )
                                    .addOnSuccessListener(
                                            unused -> {

                                                college.setActive(
                                                        newStatus
                                                );

                                                Toast.makeText(
                                                        this,
                                                        newStatus
                                                                ? "College activated."
                                                                : "College deactivated.",
                                                        Toast.LENGTH_SHORT
                                                ).show();

                                                applyFilters();
                                            }
                                    )
                                    .addOnFailureListener(
                                            error -> {

                                                UIUtils.showToast(
                                                        this,
                                                        "Status update failed: "
                                                                + getSafeErrorMessage(
                                                                error
                                                        )
                                                );
                                            }
                                    );
                        }
                )
                .show();
    }

    // ============================================================
    // ASSIGN COLLEGE ADMIN
    // ============================================================

    private void showAssignCollegeAdminDialog(
            College college
    ) {

        if (college == null ||
                college.getId() == null) {

            Toast.makeText(
                    this,
                    "College ID is missing.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Toast.makeText(
                this,
                "Loading available College Admins...",
                Toast.LENGTH_SHORT
        ).show();

        FirebaseHelper.getUsersRef()
                .whereEqualTo(
                        "role",
                        User.ROLE_COLLEGE_ADMIN
                )
                .get()
                .addOnSuccessListener(
                        querySnapshot -> {

                            List<User> candidates =
                                    new ArrayList<>();

                            for (DocumentSnapshot document :
                                    querySnapshot) {

                                User user =
                                        document.toObject(
                                                User.class
                                        );

                                if (user == null) {
                                    continue;
                                }

                                if (user.getUid() == null ||
                                        user.getUid()
                                                .trim()
                                                .isEmpty()) {

                                    user.setUid(
                                            document.getId()
                                    );
                                }

                                boolean noCollege =
                                        user.getCollegeId() == null
                                                || user.getCollegeId()
                                                .trim()
                                                .isEmpty();

                                boolean sameCollege =
                                        college.getId()
                                                .equals(
                                                        user.getCollegeId()
                                                );

                                if (user.isActive() &&
                                        (noCollege ||
                                                sameCollege)) {

                                    candidates.add(user);
                                }
                            }

                            if (candidates.isEmpty()) {

                                new AlertDialog.Builder(
                                        this
                                )
                                        .setTitle(
                                                "No College Admin Available"
                                        )
                                        .setMessage(
                                                "There is no active "
                                                        + "College Admin available "
                                                        + "for assignment.\n\n"
                                                        + "Create or approve a "
                                                        + "College Admin account first."
                                        )
                                        .setPositiveButton(
                                                "OK",
                                                null
                                        )
                                        .show();

                                return;
                            }

                            showAdminSelectionDialog(
                                    college,
                                    candidates
                            );
                        }
                )
                .addOnFailureListener(
                        error -> {

                            UIUtils.showToast(
                                    this,
                                    "Could not load College Admins: "
                                            + getSafeErrorMessage(
                                            error
                                    )
                            );
                        }
                );
    }

    private void showAdminSelectionDialog(
            College college,
            List<User> candidates
    ) {

        String[] names =
                new String[candidates.size()];

        for (int i = 0;
             i < candidates.size();
             i++) {

            User user =
                    candidates.get(i);

            String name =
                    safeText(
                            user.getName(),
                            "Unnamed Admin"
                    );

            String email =
                    safeText(
                            user.getEmail(),
                            "No email"
                    );

            names[i] =
                    name
                            + "\n"
                            + email;
        }

        new AlertDialog.Builder(this)
                .setTitle(
                        "Assign College Admin"
                )
                .setItems(
                        names,
                        (dialog, which) -> {

                            if (which >= 0 &&
                                    which < candidates.size()) {

                                assignAdminToCollege(
                                        college,
                                        candidates.get(which)
                                );
                            }
                        }
                )
                .setNegativeButton(
                        "CANCEL",
                        null
                )
                .show();
    }

    private void assignAdminToCollege(
            College college,
            User user
    ) {

        if (user == null ||
                user.getUid() == null ||
                user.getUid().trim().isEmpty()) {

            Toast.makeText(
                    this,
                    "Admin UID is missing.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String adminName =
                safeText(
                        user.getName(),
                        "this admin"
                );

        new AlertDialog.Builder(this)
                .setTitle(
                        "Confirm Admin Assignment"
                )
                .setMessage(
                        "Assign "
                                + adminName
                                + " as the College Admin for "
                                + safeText(
                                college.getName(),
                                "this college"
                        )
                                + "?"
                )
                .setNegativeButton(
                        "CANCEL",
                        null
                )
                .setPositiveButton(
                        "ASSIGN",
                        (dialog, which) -> {

                            FirebaseHelper.getUsersRef()
                                    .document(
                                            user.getUid()
                                    )
                                    .update(
                                            "collegeId",
                                            college.getId()
                                    )
                                    .addOnSuccessListener(
                                            unused -> {

                                                Toast.makeText(
                                                        this,
                                                        "College Admin assigned successfully.",
                                                        Toast.LENGTH_SHORT
                                                ).show();
                                            }
                                    )
                                    .addOnFailureListener(
                                            error -> {

                                                UIUtils.showToast(
                                                        this,
                                                        "Admin assignment failed: "
                                                                + getSafeErrorMessage(
                                                                error
                                                        )
                                                );
                                            }
                                    );
                        }
                )
                .show();
    }

    // ============================================================
    // FORM / UI HELPERS
    // ============================================================

    private void clearCollegeForm() {

        etCollegeName.setText("");
        etCollegeCode.setText("");
        etAddress.setText("");
        etContactEmail.setText("");
        etContactPhone.setText("");

        etCollegeName.requestFocus();
    }

    private void setLoadingState(
            boolean loading
    ) {

        isLoading = loading;

        btnAddCollege.setEnabled(
                !loading
        );

        if (loading) {

            btnAddCollege.setText(
                    "ONBOARDING..."
            );

        } else {

            btnAddCollege.setText(
                    "ONBOARD COLLEGE"
            );
        }
    }

    private String getTrimmedText(
            EditText editText
    ) {

        if (editText == null ||
                editText.getText() == null) {

            return "";
        }

        return editText.getText()
                .toString()
                .trim();
    }

    private String safeText(
            String value,
            String fallback
    ) {

        if (value == null ||
                value.trim().isEmpty()) {

            return fallback;
        }

        return value;
    }

    private String safeLower(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private String getSafeErrorMessage(
            Exception error
    ) {

        if (error == null ||
                error.getMessage() == null ||
                error.getMessage()
                        .trim()
                        .isEmpty()) {

            return "Unknown error";
        }

        return error.getMessage();
    }
}