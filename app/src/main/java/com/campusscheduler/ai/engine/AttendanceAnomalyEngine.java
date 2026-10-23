package com.campusscheduler.ai.engine;

import com.campusscheduler.ai.models.AttendanceRecord;
import com.campusscheduler.ai.models.ClassSession;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AttendanceAnomalyEngine {

    public static class AnomalyReport {
        private String anomalyType;
        private String description;
        private String severity; // HIGH, MEDIUM, LOW
        private String sessionOrRecordId;

        public AnomalyReport(String anomalyType, String description, String severity, String sessionOrRecordId) {
            this.anomalyType = anomalyType;
            this.description = description;
            this.severity = severity;
            this.sessionOrRecordId = sessionOrRecordId;
        }

        public String getAnomalyType() { return anomalyType; }
        public String getDescription() { return description; }
        public String getSeverity() { return severity; }
        public String getSessionOrRecordId() { return sessionOrRecordId; }
    }

    public static List<AnomalyReport> detectAnomalies(List<AttendanceRecord> records, List<ClassSession> scheduledSessions) {
        List<AnomalyReport> anomalies = new ArrayList<>();
        if (records == null || records.isEmpty()) return anomalies;

        Map<String, List<AttendanceRecord>> sessionRecordMap = new HashMap<>();

        for (AttendanceRecord rec : records) {
            String key = rec.getSessionId() + "_" + rec.getDate();
            if (!sessionRecordMap.containsKey(key)) {
                sessionRecordMap.put(key, new ArrayList<>());
            }
            sessionRecordMap.get(key).add(rec);
        }

        for (Map.Entry<String, List<AttendanceRecord>> entry : sessionRecordMap.entrySet()) {
            List<AttendanceRecord> list = entry.getValue();
            
            // 1. Check for Duplicate Attendance Submissions
            if (list.size() > 1) {
                anomalies.add(new AnomalyReport(
                        "DUPLICATE_SUBMISSION",
                        "Multiple attendance submissions (" + list.size() + ") logged for session " + entry.getKey(),
                        "HIGH",
                        entry.getKey()
                ));
            }

            // Calculate attendance ratio
            int presentCount = 0;
            for (AttendanceRecord r : list) {
                if (r.isPresent()) presentCount++;
            }
            double presentPct = (presentCount * 100.0) / list.size();

            // 2. High Absence Spike Anomaly (<30% present)
            if (presentPct < 30.0 && list.size() >= 5) {
                anomalies.add(new AnomalyReport(
                        "HIGH_ABSENCE_SPIKE",
                        "Unusual absence spike detected: Only " + String.format("%.1f", presentPct) + "% present in session " + entry.getKey(),
                        "MEDIUM",
                        entry.getKey()
                ));
            }
        }

        return anomalies;
    }
}
