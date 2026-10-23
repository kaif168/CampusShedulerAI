package com.campusscheduler.ai.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.campusscheduler.ai.models.Timetable;
import com.campusscheduler.ai.models.TimetableSlot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * TimetablePdfExporter
 *
 * Generates official landscape A4 printable timetables adhering to institutional standards:
 * 1. Strict Landscape page orientation (ISO A4 Landscape: 842 x 595 pt).
 * 2. Official institutional header with college logo, full verified college name, address,
 *    department ribbon, effective start date (W.E.F.: DD-MM-YYYY), academic year, and class teacher.
 * 3. Complete weekly timetable grid (Monday - Saturday) with Period 1 starting at 08:20 AM.
 * 4. Protected Short Tea Break (09:00 - 09:20 AM) and College Lunch Break (12:50 - 01:40 PM).
 * 5. Friday 12:50 PM hard cutoff recess and Saturday 2nd/4th working schedule policies.
 * 6. Crisp, stroke-only table borders (no grey overlay / blocking artifacts).
 * 7. Three official signature areas: Timetable Coordinator, HOD, and Principal.
 * 8. Integrated with Android Print Framework (PrintManager) and direct PDF export/share.
 */
public class TimetablePdfExporter {

    public static final int PAGE_WIDTH_PT = 842;   // A4 Landscape width in points (72 dpi)
    public static final int PAGE_HEIGHT_PT = 595;  // A4 Landscape height in points (72 dpi)

    /**
     * Opens the Android system print dialog in landscape A4 orientation.
     */
    public static void exportAndPrint(Activity activity, Timetable timetable) {
        if (activity == null || timetable == null) return;

        PrintManager printManager = (PrintManager) activity.getSystemService(Context.PRINT_SERVICE);
        if (printManager == null) {
            Toast.makeText(activity, "Print service is not available on this device.", Toast.LENGTH_SHORT).show();
            return;
        }

        String jobName = "CampusSchedulerAI_Timetable_" +
                (timetable.getSection() != null ? timetable.getSection() : "Official");

        PrintAttributes attributes = new PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4.asLandscape())
                .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build();

        printManager.print(jobName, new TimetablePrintAdapter(activity, timetable), attributes);
    }

    /**
     * Saves the timetable as an official PDF in app cache and opens share/view chooser.
     */
    public static void exportAndSharePdf(Activity activity, Timetable timetable) {
        if (activity == null || timetable == null) return;

        try {
            File pdfDir = new File(activity.getCacheDir(), "timetables");
            if (!pdfDir.exists()) pdfDir.mkdirs();

            String sem = timetable.getSemester() > 0 ? "Sem" + timetable.getSemester() : "Sem";
            String sec = timetable.getSection() != null ? "Sec" + timetable.getSection() : "A";
            File pdfFile = new File(pdfDir, "Official_Timetable_" + sem + "_" + sec + ".pdf");

            PdfDocument document = new PdfDocument();
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH_PT, PAGE_HEIGHT_PT, 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);

            drawTimetableCanvas(activity, page.getCanvas(), timetable, PAGE_WIDTH_PT, PAGE_HEIGHT_PT);

            document.finishPage(page);

            FileOutputStream fos = new FileOutputStream(pdfFile);
            document.writeTo(fos);
            document.close();
            fos.close();

            Uri fileUri = FileProvider.getUriForFile(
                    activity,
                    "com.campusscheduler.ai.fileprovider",
                    pdfFile
            );

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Official Class Timetable - " + sem + " " + sec);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            activity.startActivity(Intent.createChooser(shareIntent, "Share or Save Official Timetable PDF"));

        } catch (Exception e) {
            Toast.makeText(activity, "Failed to generate PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * PrintDocumentAdapter implementation for Android Print Framework.
     */
    public static class TimetablePrintAdapter extends PrintDocumentAdapter {
        private final Context context;
        private final Timetable timetable;

        public TimetablePrintAdapter(Context context, Timetable timetable) {
            this.context = context;
            this.timetable = timetable;
        }

        @Override
        public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes,
                             CancellationSignal cancellationSignal, LayoutResultCallback callback, Bundle extras) {
            if (cancellationSignal.isCanceled()) {
                callback.onLayoutCancelled();
                return;
            }

            PrintDocumentInfo info = new PrintDocumentInfo.Builder("Official_Timetable.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build();

            callback.onLayoutFinished(info, true);
        }

        @Override
        public void onWrite(PageRange[] pages, ParcelFileDescriptor destination,
                            CancellationSignal cancellationSignal, WriteResultCallback callback) {
            PdfDocument document = new PdfDocument();
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH_PT, PAGE_HEIGHT_PT, 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);

            if (cancellationSignal.isCanceled()) {
                callback.onWriteCancelled();
                document.close();
                return;
            }

            drawTimetableCanvas(context, page.getCanvas(), timetable, PAGE_WIDTH_PT, PAGE_HEIGHT_PT);

            document.finishPage(page);

            try {
                document.writeTo(new FileOutputStream(destination.getFileDescriptor()));
                callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});
            } catch (IOException e) {
                callback.onWriteFailed(e.toString());
            } finally {
                document.close();
            }
        }
    }

    /**
     * Backward-compatible overload.
     */
    public static void drawTimetableCanvas(Canvas canvas, Timetable timetable, int width, int height) {
        drawTimetableCanvas(null, canvas, timetable, width, height);
    }

    /**
     * Precise canvas rendering engine for official Landscape A4 timetable.
     * Guaranteed no grey overlay covering the table cells.
     */
    public static void drawTimetableCanvas(Context context, Canvas canvas, Timetable timetable, int width, int height) {
        if (canvas == null) return;
        if (timetable == null) timetable = new Timetable();

        // Base Paints
        Paint paintText = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint paintBox = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintBox.setStyle(Paint.Style.FILL);

        // Strict Stroke Paint for all borders
        Paint paintBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintBorder.setStyle(Paint.Style.STROKE);
        paintBorder.setColor(0xFFB0BEC5);
        paintBorder.setStrokeWidth(0.85f);

        // 1. Clean White Page Canvas Background
        paintBox.setColor(Color.WHITE);
        canvas.drawRect(0, 0, width, height, paintBox);

        float leftMargin = 24f;
        float rightMargin = width - 24f;

        // -------------------------------------------------------------------------
        // HEADER SECTION
        // -------------------------------------------------------------------------

        // Top Navy Blue Banner
        float bannerTop = 14f;
        float bannerBottom = 52f;
        paintBox.setColor(0xFF0D47A1); // Deep Institutional Navy Blue
        canvas.drawRect(leftMargin, bannerTop, rightMargin, bannerBottom, paintBox);

        // Draw College Logo / Crest on left
        Bitmap logoBitmap = null;
        if (context != null) {
            try {
                int logoResId = CollegeIdentityHelper.getCollegeLogoResId(
                        timetable.getCollegeId(),
                        timetable.getCollegeName()
                );
                logoBitmap = BitmapFactory.decodeResource(context.getResources(), logoResId);
            } catch (Exception ignored) {}
        }

        float logoCenterX = leftMargin + 24f;
        float logoCenterY = (bannerTop + bannerBottom) / 2f;
        float logoRadius = 15f;

        // White circular emblem backing
        paintBox.setColor(Color.WHITE);
        canvas.drawCircle(logoCenterX, logoCenterY, logoRadius, paintBox);

        if (logoBitmap != null) {
            Rect srcRect = new Rect(0, 0, logoBitmap.getWidth(), logoBitmap.getHeight());
            Rect destRect = new Rect(
                    (int) (logoCenterX - 11f),
                    (int) (logoCenterY - 11f),
                    (int) (logoCenterX + 11f),
                    (int) (logoCenterY + 11f)
            );
            canvas.drawBitmap(logoBitmap, srcRect, destRect, null);
        } else {
            // High-resolution vector style initial emblem
            paintText.setColor(0xFF0D47A1);
            paintText.setTextSize(8f);
            paintText.setFakeBoldText(true);
            paintText.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("HKBK", logoCenterX, logoCenterY + 3f, paintText);
        }

        // Prominent Centered College Name
        paintText.setColor(Color.WHITE);
        paintText.setTextSize(13.5f);
        paintText.setFakeBoldText(true);
        paintText.setTextAlign(Paint.Align.CENTER);
        String collegeName = timetable.getCollegeName() != null ?
                CollegeIdentityHelper.formatCollegeName(timetable.getCollegeName()).toUpperCase(Locale.ROOT) : "HKBK COLLEGE OF ENGINEERING";
        canvas.drawText(collegeName, width / 2f, 32f, paintText);

        // Verified Address & Affiliation Subtitle
        paintText.setColor(0xFFDCEEFF);
        paintText.setTextSize(7.5f);
        paintText.setFakeBoldText(false);
        String collegeAddress = timetable.getCollegeAddress();
        if (collegeAddress == null || collegeAddress.trim().isEmpty()) {
            collegeAddress = CollegeIdentityHelper.getVerifiedCollegeAddress(timetable.getCollegeId(), timetable.getCollegeName());
        }
        canvas.drawText(collegeAddress + "  •  Affiliated to VTU  •  Approved by AICTE", width / 2f, 45f, paintText);

        // Department Ribbon Banner
        float ribbonTop = 52f;
        float ribbonBottom = 72f;
        paintBox.setColor(0xFFE3F2FD); // Soft Ice Blue Ribbon
        canvas.drawRect(leftMargin, ribbonTop, rightMargin, ribbonBottom, paintBox);

        paintText.setColor(0xFF0D47A1);
        paintText.setTextSize(9.5f);
        paintText.setFakeBoldText(true);
        paintText.setTextAlign(Paint.Align.CENTER);
        String deptName = timetable.getDepartmentName() != null ?
                timetable.getDepartmentName().toUpperCase(Locale.ROOT) : "COMPUTER SCIENCE & ENGINEERING";
        canvas.drawText("DEPARTMENT OF " + deptName + "  •  OFFICIAL CLASS TIMETABLE", width / 2f, 66f, paintText);

        // Ribbon divider line
        paintBorder.setColor(0xFFBBDEFB);
        canvas.drawLine(leftMargin, ribbonBottom, rightMargin, ribbonBottom, paintBorder);

        // -------------------------------------------------------------------------
        // METADATA SUBHEADER
        // -------------------------------------------------------------------------
        String semStr = timetable.getSemester() > 0 ? timetable.getSemester() + "th Semester" : "Semester";
        String secStr = "Section " + (timetable.getSection() != null ? timetable.getSection() : "A");
        String ayStr = timetable.getAcademicYear() != null ? timetable.getAcademicYear() : "2026-27";
        String classTeacher = timetable.getClassTeacherName() != null ? timetable.getClassTeacherName() : "Dr. Pushpa Mohan";
        String wefStr = "W.E.F.: " + formatWefDate(timetable.getEffectiveStartDate());

        // Left: W.E.F. and Academic Year
        paintText.setColor(0xFF263238);
        paintText.setTextSize(7.5f);
        paintText.setFakeBoldText(false);
        paintText.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(wefStr + "   |   Academic Year: " + ayStr, leftMargin + 2f, 87f, paintText);

        // Center: TIME TABLE — SEMESTER (SECTION)
        paintText.setColor(0xFF0D47A1);
        paintText.setTextSize(9.5f);
        paintText.setFakeBoldText(true);
        paintText.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("TIME TABLE — " + semStr.toUpperCase(Locale.ROOT) + " (" + secStr.toUpperCase(Locale.ROOT) + ")", width / 2f, 87f, paintText);

        // Right: Class Teacher Name
        paintText.setColor(0xFF263238);
        paintText.setTextSize(7.5f);
        paintText.setFakeBoldText(false);
        paintText.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("Class Teacher: " + classTeacher, rightMargin - 2f, 87f, paintText);

        // -------------------------------------------------------------------------
        // TIMETABLE GRID DIMENSIONS & COLUMNS
        // -------------------------------------------------------------------------
        float gridLeft = leftMargin; // 24f
        float gridTop = 96f;
        float headerRowHeight = 26f;
        float dayRowHeight = 56f;

        // Perfectly balanced column widths spanning exactly 794 pt (from 24 to 818)
        float[] colX = new float[12];
        colX[0] = gridLeft;           // 24
        colX[1] = colX[0] + 76f;      // 100
        colX[2] = colX[1] + 80f;      // 180
        colX[3] = colX[2] + 32f;      // 212
        colX[4] = colX[3] + 80f;      // 292
        colX[5] = colX[4] + 80f;      // 372
        colX[6] = colX[5] + 80f;      // 452
        colX[7] = colX[6] + 80f;      // 532
        colX[8] = colX[7] + 46f;      // 578
        colX[9] = colX[8] + 80f;      // 658
        colX[10] = colX[9] + 80f;     // 738
        colX[11] = colX[10] + 80f;    // 818

        float gridRight = colX[11];

        // -------------------------------------------------------------------------
        // DRAW TABLE HEADER ROW
        // -------------------------------------------------------------------------
        paintBox.setColor(0xFF1565C0); // Royal Navy Header
        canvas.drawRect(gridLeft, gridTop, gridRight, gridTop + headerRowHeight, paintBox);

        paintText.setColor(Color.WHITE);
        paintText.setTextAlign(Paint.Align.CENTER);

        // Header Labels
        drawHeaderCell(canvas, colX[0], colX[1], gridTop, headerRowHeight, "DAY", "");
        drawHeaderCell(canvas, colX[1], colX[2], gridTop, headerRowHeight, "PERIOD 1", "08:20-09:00");
        drawHeaderCell(canvas, colX[2], colX[3], gridTop, headerRowHeight, "TEA", "BREAK");
        drawHeaderCell(canvas, colX[3], colX[4], gridTop, headerRowHeight, "PERIOD 2", "09:20-10:10");
        drawHeaderCell(canvas, colX[4], colX[5], gridTop, headerRowHeight, "PERIOD 3", "10:10-11:00");
        drawHeaderCell(canvas, colX[5], colX[6], gridTop, headerRowHeight, "PERIOD 4", "11:00-11:55");
        drawHeaderCell(canvas, colX[6], colX[7], gridTop, headerRowHeight, "PERIOD 5", "11:55-12:50");
        drawHeaderCell(canvas, colX[7], colX[8], gridTop, headerRowHeight, "LUNCH", "BREAK");
        drawHeaderCell(canvas, colX[8], colX[9], gridTop, headerRowHeight, "PERIOD 6", "01:40-02:30");
        drawHeaderCell(canvas, colX[9], colX[10], gridTop, headerRowHeight, "PERIOD 7", "02:30-03:20");
        drawHeaderCell(canvas, colX[10], colX[11], gridTop, headerRowHeight, "PERIOD 8", "03:20-04:10");

        // -------------------------------------------------------------------------
        // MAP SLOTS BY DAY AND PERIOD (1..8)
        // -------------------------------------------------------------------------
        Map<String, Map<Integer, TimetableSlot>> daySlotsMap = new LinkedHashMap<>();
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        for (String d : days) {
            daySlotsMap.put(d, new HashMap<>());
        }

        if (timetable.getSlots() != null) {
            for (TimetableSlot s : timetable.getSlots()) {
                if (s == null) continue;
                String d = s.getDay() != null ? s.getDay() : s.getDayOfWeek();
                if (d == null || d.trim().isEmpty()) d = "Monday";

                for (String stdDay : days) {
                    if (stdDay.equalsIgnoreCase(d.trim()) || d.trim().toUpperCase(Locale.ROOT).startsWith(stdDay.substring(0, 3).toUpperCase(Locale.ROOT))) {
                        int period = s.getPeriodIndex();
                        if (period <= 0) {
                            period = deducePeriodNumber(s.getStartTime());
                        }
                        if (period >= 1 && period <= 8) {
                            daySlotsMap.get(stdDay).put(period, s);
                        }
                        break;
                    }
                }
            }
        }

        // -------------------------------------------------------------------------
        // RENDER DAY ROWS
        // -------------------------------------------------------------------------
        float currentY = gridTop + headerRowHeight;

        for (int dayIdx = 0; dayIdx < days.length; dayIdx++) {
            String day = days[dayIdx];
            float rowTop = currentY;
            float rowBottom = rowTop + dayRowHeight;

            // 1. Day Column
            paintBox.setColor(dayIdx % 2 == 0 ? 0xFFF8F9FA : 0xFFECEFF1);
            canvas.drawRect(colX[0], rowTop, colX[1], rowBottom, paintBox);

            paintText.setColor(0xFF0D47A1);
            paintText.setTextAlign(Paint.Align.CENTER);
            paintText.setTextSize(8.5f);
            paintText.setFakeBoldText(true);
            canvas.drawText(day.toUpperCase(Locale.ROOT), colX[0] + (colX[1] - colX[0]) / 2f, rowTop + 25f, paintText);

            paintText.setTextSize(5.8f);
            paintText.setFakeBoldText(false);
            if ("Friday".equalsIgnoreCase(day)) {
                paintText.setColor(0xFF2E7D32);
                paintText.setFakeBoldText(true);
                canvas.drawText("Half Day (12:50 PM)", colX[0] + (colX[1] - colX[0]) / 2f, rowTop + 38f, paintText);
            } else if ("Saturday".equalsIgnoreCase(day)) {
                paintText.setColor(0xFFE65100);
                paintText.setFakeBoldText(true);
                canvas.drawText("2nd / 4th Sat", colX[0] + (colX[1] - colX[0]) / 2f, rowTop + 38f, paintText);
            } else {
                paintText.setColor(0xFF78909C);
                canvas.drawText("Working Day", colX[0] + (colX[1] - colX[0]) / 2f, rowTop + 38f, paintText);
            }

            Map<Integer, TimetableSlot> pMap = daySlotsMap.get(day);

            // Period 1
            drawSlotCell(canvas, colX[1], rowTop, colX[2] - colX[1], dayRowHeight, pMap != null ? pMap.get(1) : null);

            // Tea Break Column
            paintBox.setColor(0xFFF5F5F5);
            canvas.drawRect(colX[2], rowTop, colX[3], rowBottom, paintBox);
            paintText.setColor(0xFF78909C);
            paintText.setTextAlign(Paint.Align.CENTER);
            paintText.setTextSize(6.5f);
            paintText.setFakeBoldText(true);
            float teaCenterX = colX[2] + (colX[3] - colX[2]) / 2f;
            canvas.drawText("TEA", teaCenterX, rowTop + 22f, paintText);
            canvas.drawText("BREAK", teaCenterX, rowTop + 33f, paintText);
            paintText.setTextSize(5.0f);
            paintText.setColor(0xFF90A4AE);
            paintText.setFakeBoldText(false);
            canvas.drawText("20 min", teaCenterX, rowTop + 44f, paintText);

            // Periods 2, 3, 4, 5
            drawSlotCell(canvas, colX[3], rowTop, colX[4] - colX[3], dayRowHeight, pMap != null ? pMap.get(2) : null);
            drawSlotCell(canvas, colX[4], rowTop, colX[5] - colX[4], dayRowHeight, pMap != null ? pMap.get(3) : null);
            drawSlotCell(canvas, colX[5], rowTop, colX[6] - colX[5], dayRowHeight, pMap != null ? pMap.get(4) : null);
            drawSlotCell(canvas, colX[6], rowTop, colX[7] - colX[6], dayRowHeight, pMap != null ? pMap.get(5) : null);

            // Lunch Break Column
            paintBox.setColor(0xFFF5F5F5);
            canvas.drawRect(colX[7], rowTop, colX[8], rowBottom, paintBox);
            paintText.setColor(0xFF78909C);
            paintText.setTextAlign(Paint.Align.CENTER);
            paintText.setTextSize(6.5f);
            paintText.setFakeBoldText(true);
            float lunchCenterX = colX[7] + (colX[8] - colX[7]) / 2f;
            canvas.drawText("LUNCH", lunchCenterX, rowTop + 22f, paintText);
            canvas.drawText("BREAK", lunchCenterX, rowTop + 33f, paintText);
            paintText.setTextSize(5.0f);
            paintText.setColor(0xFF90A4AE);
            paintText.setFakeBoldText(false);
            canvas.drawText("50 min", lunchCenterX, rowTop + 44f, paintText);

            // Afternoon Schedule (Periods 6, 7, 8)
            if ("Friday".equalsIgnoreCase(day)) {
                // Friday afternoon closing policy (12:50 PM cutoff)
                paintBox.setColor(0xFFFFFDE7); // Pale amber/warm tint
                canvas.drawRect(colX[8], rowTop, colX[11], rowBottom, paintBox);

                float friCenterX = colX[8] + (colX[11] - colX[8]) / 2f;
                paintText.setColor(0xFF795548);
                paintText.setTextSize(8f);
                paintText.setFakeBoldText(true);
                paintText.setTextAlign(Paint.Align.CENTER);
                canvas.drawText("FRIDAY AFTERNOON RECESS / PRAYER", friCenterX, rowTop + 25f, paintText);

                paintText.setColor(0xFF8D6E63);
                paintText.setTextSize(6.2f);
                paintText.setFakeBoldText(false);
                canvas.drawText("Academic timetable concludes at 12:50 PM as per VTU institutional norms", friCenterX, rowTop + 39f, paintText);

            } else if ("Saturday".equalsIgnoreCase(day)) {
                // Saturday institutional working policy
                paintBox.setColor(0xFFF1F8E9); // Soft sage green
                canvas.drawRect(colX[8], rowTop, colX[11], rowBottom, paintBox);

                float satCenterX = colX[8] + (colX[11] - colX[8]) / 2f;
                paintText.setColor(0xFF33691E);
                paintText.setTextSize(7.8f);
                paintText.setFakeBoldText(true);
                paintText.setTextAlign(Paint.Align.CENTER);
                canvas.drawText("SATURDAY INSTITUTIONAL SCHEDULE", satCenterX, rowTop + 25f, paintText);

                paintText.setColor(0xFF558B2F);
                paintText.setTextSize(6.0f);
                paintText.setFakeBoldText(false);
                canvas.drawText("2nd Sat: Mon Order  •  4th Sat: Tue Order  •  1st & 3rd: Institutional Holiday", satCenterX, rowTop + 39f, paintText);

            } else {
                // Regular Mon-Thu afternoon periods 6, 7, 8
                drawSlotCell(canvas, colX[8], rowTop, colX[9] - colX[8], dayRowHeight, pMap != null ? pMap.get(6) : null);
                drawSlotCell(canvas, colX[9], rowTop, colX[10] - colX[9], dayRowHeight, pMap != null ? pMap.get(7) : null);
                drawSlotCell(canvas, colX[10], rowTop, colX[11] - colX[10], dayRowHeight, pMap != null ? pMap.get(8) : null);
            }

            currentY += dayRowHeight;
        }

        // -------------------------------------------------------------------------
        // DRAW TABLE GRID BORDERS (STRICTLY STROKE — NEVER FILL!)
        // -------------------------------------------------------------------------
        paintBorder.setColor(0xFFB0BEC5);
        paintBorder.setStrokeWidth(0.85f);

        // Vertical divider lines
        for (int i = 0; i <= 11; i++) {
            canvas.drawLine(colX[i], gridTop, colX[i], currentY, paintBorder);
        }

        // Horizontal divider lines
        for (float y = gridTop; y <= currentY + 0.1f; y += (y == gridTop ? headerRowHeight : dayRowHeight)) {
            canvas.drawLine(gridLeft, y, gridRight, y, paintBorder);
        }

        // Crisp Outer Table Border
        paintBorder.setColor(0xFF78909C);
        paintBorder.setStrokeWidth(1.4f);
        canvas.drawRect(gridLeft, gridTop, gridRight, currentY, paintBorder);

        // -------------------------------------------------------------------------
        // THREE BALANCED SIGNATURE BLOCKS AT BOTTOM
        // -------------------------------------------------------------------------
        float sigLineY = 515f;
        paintBorder.setColor(0xFF90A4AE);
        paintBorder.setStrokeWidth(1.0f);

        // 1. Timetable Coordinator (Left)
        float sig1Center = 140f;
        canvas.drawLine(50f, sigLineY, 230f, sigLineY, paintBorder);

        paintText.setColor(0xFF263238);
        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTextSize(7.5f);
        paintText.setFakeBoldText(true);
        canvas.drawText("TIMETABLE COORDINATOR", sig1Center, sigLineY + 13f, paintText);

        paintText.setTextSize(7.0f);
        paintText.setColor(0xFF455A64);
        paintText.setFakeBoldText(false);
        String coord = timetable.getCoordinatorName() != null ? timetable.getCoordinatorName() : "Dr. Sivarajan";
        canvas.drawText(coord, sig1Center, sigLineY + 24f, paintText);

        // 2. Head of Department (HOD) (Center)
        float sig2Center = width / 2f; // 421f
        canvas.drawLine(sig2Center - 90f, sigLineY, sig2Center + 90f, sigLineY, paintBorder);

        paintText.setColor(0xFF263238);
        paintText.setTextSize(7.5f);
        paintText.setFakeBoldText(true);
        canvas.drawText("HEAD OF DEPARTMENT (HOD)", sig2Center, sigLineY + 13f, paintText);

        paintText.setTextSize(7.0f);
        paintText.setColor(0xFF455A64);
        paintText.setFakeBoldText(false);
        String hod = timetable.getHodName() != null ? timetable.getHodName() : "Dr. Vilasini M";
        canvas.drawText(hod, sig2Center, sigLineY + 24f, paintText);

        // 3. Principal (Right)
        float sig3Center = 702f;
        canvas.drawLine(612f, sigLineY, 792f, sigLineY, paintBorder);

        paintText.setColor(0xFF263238);
        paintText.setTextSize(7.5f);
        paintText.setFakeBoldText(true);
        canvas.drawText("PRINCIPAL", sig3Center, sigLineY + 13f, paintText);

        paintText.setTextSize(7.0f);
        paintText.setColor(0xFF455A64);
        paintText.setFakeBoldText(false);
        String principal = timetable.getPrincipalName() != null ? timetable.getPrincipalName() : "Dr. Pushpa Mohan";
        canvas.drawText(principal, sig3Center, sigLineY + 24f, paintText);

        // -------------------------------------------------------------------------
        // INSTITUTIONAL COMPLIANCE FOOTER NOTE
        // -------------------------------------------------------------------------
        paintText.setColor(0xFF90A4AE);
        paintText.setTextSize(6.0f);
        paintText.setFakeBoldText(false);
        paintText.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(
                "CampusSchedulerAI Institutional Timetable System  •  Certified VTU & AICTE Compliance Standards  •  Bengaluru",
                width / 2f,
                568f,
                paintText
        );
    }

    private static void drawHeaderCell(Canvas canvas, float left, float right, float top, float height, String line1, String line2) {
        Paint paintText = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintText.setColor(Color.WHITE);
        paintText.setTextAlign(Paint.Align.CENTER);
        float centerX = left + (right - left) / 2f;

        if (line2 == null || line2.trim().isEmpty()) {
            paintText.setTextSize(7.5f);
            paintText.setFakeBoldText(true);
            canvas.drawText(line1, centerX, top + 16f, paintText);
        } else {
            paintText.setTextSize(7.0f);
            paintText.setFakeBoldText(true);
            canvas.drawText(line1, centerX, top + 11.5f, paintText);

            paintText.setTextSize(5.6f);
            paintText.setColor(0xFFBBDEFB);
            paintText.setFakeBoldText(false);
            canvas.drawText(line2, centerX, top + 21f, paintText);
        }
    }

    private static void drawSlotCell(Canvas canvas, float x, float y, float width, float height, TimetableSlot slot) {
        Paint paintBox = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintBox.setStyle(Paint.Style.FILL);
        Paint paintText = new Paint(Paint.ANTI_ALIAS_FLAG);

        if (slot == null) {
            paintBox.setColor(Color.WHITE);
            canvas.drawRect(x, y, x + width, y + height, paintBox);

            // Subtle dash indicating open/free slot
            paintText.setColor(0xFFCFD8DC);
            paintText.setTextSize(10f);
            paintText.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("-", x + width / 2f, y + height / 2f + 3f, paintText);
            return;
        }

        boolean isLab = slot.isLab() || "Lab".equalsIgnoreCase(slot.getClassType());
        paintBox.setColor(isLab ? 0xFFE0F7FA : Color.WHITE); // Ice Cyan for labs, pure white for theory
        canvas.drawRect(x, y, x + width, y + height, paintBox);

        if (isLab) {
            // Draw a left accent bar for Lab indicator
            Paint paintAccent = new Paint(Paint.ANTI_ALIAS_FLAG);
            paintAccent.setStyle(Paint.Style.FILL);
            paintAccent.setColor(0xFF00838F);
            canvas.drawRect(x, y, x + 3.2f, y + height, paintAccent);
        }

        paintText.setTextAlign(Paint.Align.CENTER);

        // Line 1: Subject Name / Code (Bold, with multi-line wrap if long)
        paintText.setColor(isLab ? 0xFF006064 : 0xFF0D47A1);
        paintText.setTextSize(6.8f);
        paintText.setFakeBoldText(true);
        String subName = slot.getSubjectName() != null ? slot.getSubjectName().trim() : "Subject";

        if (subName.length() > 14 && subName.contains(" ")) {
            int splitIdx = subName.lastIndexOf(" ", 15);
            if (splitIdx == -1) splitIdx = subName.indexOf(" ");
            String line1 = subName.substring(0, splitIdx).trim();
            String line2 = subName.substring(splitIdx).trim();
            if (line2.length() > 15) line2 = line2.substring(0, 14) + "…";
            canvas.drawText(line1, x + width / 2f, y + 13f, paintText);
            canvas.drawText(line2, x + width / 2f, y + 22.5f, paintText);
        } else {
            if (subName.length() > 16) subName = subName.substring(0, 15) + "…";
            canvas.drawText(subName, x + width / 2f, y + 17f, paintText);
        }

        // Line 2: Faculty Name
        paintText.setColor(0xFF37474F);
        paintText.setTextSize(6.0f);
        paintText.setFakeBoldText(false);
        String teacher = slot.getTeacherName() != null ? slot.getTeacherName().trim() :
                (slot.getFacultyName() != null ? slot.getFacultyName().trim() : "Faculty");
        if (teacher.length() > 20) teacher = teacher.substring(0, 19) + "…";
        canvas.drawText(teacher, x + width / 2f, y + 33.5f, paintText);

        // Line 3: Room & Class Type Badge
        paintText.setColor(isLab ? 0xFF00838F : 0xFF546E7A);
        paintText.setTextSize(5.8f);
        paintText.setFakeBoldText(true);
        String room = slot.getRoomName() != null ? slot.getRoomName().trim() :
                (slot.getRoomNumber() != null ? slot.getRoomNumber().trim() : "101");
        String badge = isLab ? "[LAB • " + room + "]" : "[ROOM " + room + "]";
        canvas.drawText(badge, x + width / 2f, y + 46f, paintText);
    }

    public static int deducePeriodNumber(String startTime) {
        if (startTime == null || startTime.trim().isEmpty()) return 1;
        int min = UIUtils.parseTimeToMinutes(startTime);
        if (min <= 0) {
            try {
                String clean = startTime.replaceAll("[^0-9:]", "").trim();
                String[] p = clean.split(":");
                int h = Integer.parseInt(p[0]);
                int m = p.length > 1 ? Integer.parseInt(p[1]) : 0;
                if (startTime.toUpperCase(Locale.ROOT).contains("PM") && h < 12) h += 12;
                if (startTime.toUpperCase(Locale.ROOT).contains("AM") && h == 12) h = 0;
                min = h * 60 + m;
            } catch (Exception ignored) {}
        }
        if (min <= 530) return 1; // 08:20 AM = 500
        if (min <= 585) return 2; // 09:20 AM = 560
        if (min <= 635) return 3; // 10:10 AM = 610
        if (min <= 690) return 4; // 11:00 AM = 660
        if (min <= 760) return 5; // 11:55 AM = 715
        if (min <= 845) return 6; // 01:40 PM = 820
        if (min <= 895) return 7; // 02:30 PM = 870
        return 8;                 // 03:20 PM = 920
    }

    public static String formatWefDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return "01-08-2026";
        }
        String s = dateStr.trim();
        // Convert YYYY-MM-DD to DD-MM-YYYY
        if (s.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
            String[] parts = s.split("-");
            return parts[2] + "-" + parts[1] + "-" + parts[0];
        }
        return s;
    }
}
