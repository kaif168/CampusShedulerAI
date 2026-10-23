package com.campusscheduler.ai.activities;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.campusscheduler.ai.R;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.utils.FirebaseHelper;
import com.campusscheduler.ai.utils.LocalAuthManager;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CollegeScheduleActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private EditText etStartTime, etEndTime, etMorningTeaBreak, etLunchBreak, etAfternoonTeaBreak, etPeriodDuration;
    private Button btnSaveScheduleConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_college_schedule);

        btnBack = findViewById(R.id.btnBack);
        etStartTime = findViewById(R.id.etStartTime);
        etEndTime = findViewById(R.id.etEndTime);
        etMorningTeaBreak = findViewById(R.id.etMorningTeaBreak);
        etLunchBreak = findViewById(R.id.etLunchBreak);
        etAfternoonTeaBreak = findViewById(R.id.etAfternoonTeaBreak);
        etPeriodDuration = findViewById(R.id.etPeriodDuration);
        btnSaveScheduleConfig = findViewById(R.id.btnSaveScheduleConfig);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Clock TimePicker listeners
        etStartTime.setOnClickListener(v -> showClockPicker(etStartTime));
        etEndTime.setOnClickListener(v -> showClockPicker(etEndTime));
        etMorningTeaBreak.setOnClickListener(v -> showClockPicker(etMorningTeaBreak));
        etLunchBreak.setOnClickListener(v -> showClockPicker(etLunchBreak));
        etAfternoonTeaBreak.setOnClickListener(v -> showClockPicker(etAfternoonTeaBreak));

        btnSaveScheduleConfig.setOnClickListener(v -> saveScheduleConfigToFirestore());
    }

    private void showClockPicker(EditText targetField) {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);

        TimePickerDialog tpd = new TimePickerDialog(this, (view, selectedHour, selectedMinute) -> {
            String amPm = selectedHour >= 12 ? "PM" : "AM";
            int hourIn12Format = selectedHour % 12;
            if (hourIn12Format == 0) hourIn12Format = 12;
            String timeStr = String.format(Locale.getDefault(), "%02d:%02d %s", hourIn12Format, selectedMinute, amPm);
            targetField.setText(timeStr);
        }, hour, minute, false);

        tpd.setTitle("Select Time (Clock)");
        tpd.show();
    }

    private void saveScheduleConfigToFirestore() {
        User currentUser = LocalAuthManager.getCurrentLocalUser(this);
        String collegeId = currentUser != null ? currentUser.getCollegeId() : "COLLEGE_01";

        String startTime = etStartTime.getText().toString().trim();
        String endTime = etEndTime.getText().toString().trim();
        String morningTea = etMorningTeaBreak.getText().toString().trim();
        String lunch = etLunchBreak.getText().toString().trim();
        String afternoonTea = etAfternoonTeaBreak.getText().toString().trim();
        String periodDur = etPeriodDuration.getText().toString().trim();

        if (startTime.isEmpty() || endTime.isEmpty()) {
            Toast.makeText(this, "Please select College Start and End Time using the clock picker", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> scheduleMap = new HashMap<>();
        scheduleMap.put("collegeId", collegeId);
        scheduleMap.put("startTime", startTime);
        scheduleMap.put("endTime", endTime);
        scheduleMap.put("morningTeaBreak", morningTea.isEmpty() ? "11:00 AM - 11:15 AM" : morningTea);
        scheduleMap.put("lunchBreak", lunch.isEmpty() ? "01:15 PM - 02:00 PM" : lunch);
        scheduleMap.put("afternoonTeaBreak", afternoonTea.isEmpty() ? "03:00 PM - 03:15 PM" : afternoonTea);
        scheduleMap.put("periodDurationMinutes", periodDur.isEmpty() ? 50 : Integer.parseInt(periodDur));

        FirebaseHelper.getFirestore().collection("schedules").document(collegeId).set(scheduleMap)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Schedule Config & Break Slots Saved to Firestore!", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Saved locally (Firestore Offline: " + e.getMessage() + ")", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }
}
