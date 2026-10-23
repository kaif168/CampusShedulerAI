package com.campusscheduler.ai.utils;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.widget.Toast;

public class UIUtils {

    public static void showToast(Context context, String message) {
        if (context != null && message != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }

    public static void showAlert(Context context, String title, String message) {
        if (context == null) return;
        new AlertDialog.Builder(context)
                .setTitle(title != null ? title : "Notification")
                .setMessage(message != null ? message : "")
                .setPositiveButton("OK", null)
                .show();
    }

    public static ProgressDialog showLoadingDialog(Context context, String message) {
        if (context == null) return null;
        ProgressDialog dialog = new ProgressDialog(context);
        dialog.setMessage(message != null ? message : "Processing...");
        dialog.setCancelable(false);
        dialog.show();
        return dialog;
    }

    public static int parseTimeToMinutes(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) return 0;
        String s = timeStr.trim().toUpperCase(java.util.Locale.US);
        String[] patterns = {"hh:mm a", "h:mm a", "HH:mm", "H:mm"};
        for (String pattern : patterns) {
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(pattern, java.util.Locale.US);
                sdf.setLenient(false);
                java.util.Date date = sdf.parse(s);
                if (date != null) {
                    java.util.Calendar cal = java.util.Calendar.getInstance();
                    cal.setTime(date);
                    return cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE);
                }
            } catch (Exception ignored) {}
        }
        try {
            boolean isPm = s.contains("PM");
            boolean isAm = s.contains("AM");
            String cleaned = s.replaceAll("[^0-9:]", "").trim();
            String[] parts = cleaned.split(":");
            if (parts.length >= 2) {
                int hours = Integer.parseInt(parts[0]);
                int minutes = Integer.parseInt(parts[1]);
                if (isPm && hours < 12) hours += 12;
                if (isAm && hours == 12) hours = 0;
                return hours * 60 + minutes;
            }
        } catch (Exception ignored) {}
        return 0;
    }

    public static int getDayOrder(String day) {
        if (day == null) return 7;
        String d = day.trim().toLowerCase(java.util.Locale.US);
        if (d.startsWith("mon")) return 1;
        if (d.startsWith("tue")) return 2;
        if (d.startsWith("wed")) return 3;
        if (d.startsWith("thu")) return 4;
        if (d.startsWith("fri")) return 5;
        if (d.startsWith("sat")) return 6;
        if (d.startsWith("sun")) return 7;
        return 8;
    }

    public static String getEffectiveAcademicDay(java.util.Calendar cal) {
        if (cal == null) cal = java.util.Calendar.getInstance();
        int dow = cal.get(java.util.Calendar.DAY_OF_WEEK);
        if (dow == java.util.Calendar.MONDAY) return "Monday";
        if (dow == java.util.Calendar.TUESDAY) return "Tuesday";
        if (dow == java.util.Calendar.WEDNESDAY) return "Wednesday";
        if (dow == java.util.Calendar.THURSDAY) return "Thursday";
        if (dow == java.util.Calendar.FRIDAY) return "Friday";
        if (dow == java.util.Calendar.SATURDAY) {
            int weekNumber = cal.get(java.util.Calendar.DAY_OF_WEEK_IN_MONTH);
            if (weekNumber == 2) {
                return "Monday"; // 2nd Saturday mirrors Monday
            } else if (weekNumber == 4) {
                return "Tuesday"; // 4th Saturday mirrors Tuesday
            } else {
                return "Holiday"; // 1st & 3rd Saturday are holidays
            }
        }
        return "Sunday";
    }

    public static android.view.View createBreakCard(Context context, String title, String time) {
        androidx.cardview.widget.CardView card = new androidx.cardview.widget.CardView(context);
        android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(16, 8, 16, 8);
        card.setLayoutParams(params);
        card.setCardElevation(2f);
        card.setRadius(12f);
        card.setCardBackgroundColor(0xFFFFF8E1);

        android.widget.LinearLayout layout = new android.widget.LinearLayout(context);
        layout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        layout.setPadding(24, 16, 24, 16);
        layout.setGravity(android.view.Gravity.CENTER_VERTICAL);

        android.widget.TextView iconTv = new android.widget.TextView(context);
        iconTv.setText(title.contains("Lunch") ? "🍽️" : "☕");
        iconTv.setTextSize(18f);
        iconTv.setPadding(0, 0, 16, 0);
        layout.addView(iconTv);

        android.widget.LinearLayout textLayout = new android.widget.LinearLayout(context);
        textLayout.setOrientation(android.widget.LinearLayout.VERTICAL);

        android.widget.TextView tvTitle = new android.widget.TextView(context);
        tvTitle.setText(title);
        tvTitle.setTextColor(0xFFE65100);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setTextSize(13f);
        textLayout.addView(tvTitle);

        android.widget.TextView tvTime = new android.widget.TextView(context);
        tvTime.setText(time);
        tvTime.setTextColor(0xFF757575);
        tvTime.setTextSize(11f);
        textLayout.addView(tvTime);

        layout.addView(textLayout);
        card.addView(layout);
        return card;
    }

    public static int dpToPx(Context context, int dp) {
        if (context == null || context.getResources() == null) return dp;
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }
}
