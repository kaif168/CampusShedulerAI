package com.campusscheduler.ai.utils;

import com.campusscheduler.ai.models.Classroom;
import com.campusscheduler.ai.models.Faculty;
import com.campusscheduler.ai.models.Student;
import com.campusscheduler.ai.models.User;
import com.google.firebase.firestore.WriteBatch;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExcelImporter {

    public interface ImportCallback {
        void onSuccess(int importedCount);
        void onError(String errorMessage);
    }


    // =========================================================
    // SAFE INTEGER PARSER
    // =========================================================

    private static int safeParseInt(String str, int fallback) {
        if (str == null) {
            return fallback;
        }

        try {
            String clean = str.trim().replaceAll("[^0-9]", "");

            if (clean.isEmpty()) {
                return fallback;
            }

            return Integer.parseInt(clean);

        } catch (Exception e) {
            return fallback;
        }
    }

    public static int normalizeSemester(String raw, int fallback) {
        if (raw == null) return fallback;
        String s = raw.trim();
        if (s.isEmpty()) return fallback;
        String digits = s.replaceAll("[^0-9]", "");
        if (!digits.isEmpty()) {
            try {
                int val = Integer.parseInt(digits);
                if (val >= 1 && val <= 8) return val;
            } catch (Exception ignored) {}
        }
        String upper = s.toUpperCase();
        if (upper.contains("VIII") || upper.equals("8")) return 8;
        if (upper.contains("VII") || upper.equals("7")) return 7;
        if (upper.contains("VI") || upper.equals("6")) return 6;
        if (upper.contains("V") || upper.equals("5")) return 5;
        if (upper.contains("IV") || upper.equals("4")) return 4;
        if (upper.contains("III") || upper.equals("3")) return 3;
        if (upper.contains("II") || upper.equals("2")) return 2;
        if (upper.contains("I") || upper.equals("1")) return 1;
        return fallback;
    }

    public static String normalizeSection(String raw) {
        if (raw == null) return "A";
        String s = raw.trim();
        if (s.isEmpty()) return "A";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?i)(?:sec(?:tion)?\\s*[-:]?\\s*([A-Za-z])|([A-Za-z])\\s*sec(?:tion)?|\\b([A-Za-z])\\b)").matcher(s);
        if (m.find()) {
            for (int i = 1; i <= m.groupCount(); i++) {
                if (m.group(i) != null && !m.group(i).isEmpty()) {
                    return m.group(i).toUpperCase();
                }
            }
        }
        return s.toUpperCase();
    }


    // =========================================================
    // NORMAL TEXT CLEANER
    //
    // IMPORTANT:
    // This does NOT remove @ from emails.
    // =========================================================

    private static String cleanCell(String input) {

        if (input == null) {
            return "";
        }

        String value = input.trim();

        // Remove UTF-8 BOM if present
        if (value.startsWith("\uFEFF")) {
            value = value.substring(1);
        }

        // Remove surrounding quotes
        if (value.length() >= 2
                && value.startsWith("\"")
                && value.endsWith("\"")) {

            value = value.substring(1, value.length() - 1);
        }

        return value.trim();
    }


    // =========================================================
    // OLD SANITIZER
    //
    // Kept for Student/Classroom compatibility.
    // DO NOT use this for email/phone.
    // =========================================================

    private static String sanitizeText(String input) {

        if (input == null) {
            return "";
        }

        return input
                .replaceAll("[^a-zA-Z0-9\\s._-]", "")
                .trim();
    }


    // =========================================================
    // CSV LINE PARSER
    //
    // Supports:
    // comma
    // semicolon
    // tab
    // quoted values
    //
    // Example:
    // "Mohammad Kaif","kaif@gmail.com","9876543210"
    // =========================================================

    private static String[] parseCsvLine(String line) {

        List<String> values = new ArrayList<>();

        if (line == null) {
            return new String[0];
        }

        char separator = ',';

        // Detect separator
        if (line.indexOf('\t') >= 0) {
            separator = '\t';
        } else if (line.indexOf(';') >= 0
                && line.indexOf(',') < 0) {
            separator = ';';
        }

        StringBuilder current = new StringBuilder();
        boolean insideQuotes = false;

        for (int i = 0; i < line.length(); i++) {

            char ch = line.charAt(i);

            if (ch == '"') {

                // Double quote inside quoted text
                if (insideQuotes
                        && i + 1 < line.length()
                        && line.charAt(i + 1) == '"') {

                    current.append('"');
                    i++;

                } else {
                    insideQuotes = !insideQuotes;
                }

            } else if (ch == separator && !insideQuotes) {

                values.add(cleanCell(current.toString()));
                current.setLength(0);

            } else {

                current.append(ch);
            }
        }

        values.add(cleanCell(current.toString()));

        return values.toArray(new String[0]);
    }


    // =========================================================
    // HEADER NORMALIZER
    // =========================================================

    private static String normalizeHeader(String header) {

        if (header == null) {
            return "";
        }

        return header
                .trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "");
    }


    // =========================================================
    // VALID EMAIL CHECK
    // =========================================================

    private static boolean isValidEmail(String email) {

        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        String value = email.trim();

        return TeacherProvisioningManager.isValidEmail(value);
    }



    // =========================================================
    // XLSX SUPPORT
    //
    // Uses Android/Java built-in ZIP + XML APIs.
    // No Apache POI dependency is required.
    // =========================================================

    private static Map<String, byte[]> unzipXlsx(InputStream inputStream) throws Exception {

        Map<String, byte[]> entries = new HashMap<>();
        ZipInputStream zipInputStream = new ZipInputStream(inputStream);

        try {
            ZipEntry entry;

            while ((entry = zipInputStream.getNextEntry()) != null) {

                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int read;

                while ((read = zipInputStream.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                }

                entries.put(entry.getName(), output.toByteArray());
                zipInputStream.closeEntry();
            }
        } finally {
            zipInputStream.close();
        }

        return entries;
    }


    private static String getXmlText(Node node) {

        if (node == null) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        NodeList textNodes = ((Element) node).getElementsByTagName("t");

        for (int i = 0; i < textNodes.getLength(); i++) {

            Node textNode = textNodes.item(i);

            if (textNode != null && textNode.getTextContent() != null) {
                result.append(textNode.getTextContent());
            }
        }

        return result.toString();
    }


    private static List<String> readSharedStrings(
            byte[] sharedStringsBytes
    ) throws Exception {

        List<String> sharedStrings = new ArrayList<>();

        if (sharedStringsBytes == null) {
            return sharedStrings;
        }

        Document document =
                DocumentBuilderFactory
                        .newInstance()
                        .newDocumentBuilder()
                        .parse(new ByteArrayInputStream(sharedStringsBytes));

        NodeList stringItems =
                document.getElementsByTagName("si");

        for (int i = 0; i < stringItems.getLength(); i++) {

            Node item = stringItems.item(i);

            if (item instanceof Element) {
                sharedStrings.add(getXmlText(item));
            } else {
                sharedStrings.add(
                        item != null
                                ? item.getTextContent()
                                : ""
                );
            }
        }

        return sharedStrings;
    }


    private static int excelColumnToIndex(String column) {

        int result = 0;

        for (int i = 0; i < column.length(); i++) {

            char c =
                    Character.toUpperCase(
                            column.charAt(i)
                    );

            if (c < 'A' || c > 'Z') {
                continue;
            }

            result =
                    result * 26
                            + (c - 'A' + 1);
        }

        return Math.max(0, result - 1);
    }


    private static String getCellReference(Element cell) {

        if (cell == null) {
            return "";
        }

        return cell.getAttribute("r");
    }


    private static String getCellValue(
            Element cell,
            List<String> sharedStrings
    ) {

        if (cell == null) {
            return "";
        }

        String type = cell.getAttribute("t");

        if ("inlineStr".equals(type)) {
            return getXmlText(cell);
        }

        NodeList valueNodes =
                cell.getElementsByTagName("v");

        if (valueNodes.getLength() == 0) {

            NodeList formulaString =
                    cell.getElementsByTagName("t");

            if (formulaString.getLength() > 0) {
                return formulaString.item(0).getTextContent();
            }

            return "";
        }

        String value =
                valueNodes.item(0).getTextContent();

        if ("s".equals(type)) {

            try {

                int index =
                        Integer.parseInt(
                                value.trim()
                        );

                if (index >= 0
                        && index < sharedStrings.size()) {

                    return sharedStrings.get(index);
                }

            } catch (Exception ignored) {
            }
        }

        return value != null
                ? value.trim()
                : "";
    }


    private static List<List<String>> readFirstWorksheet(
            InputStream inputStream
    ) throws Exception {

        Map<String, byte[]> entries =
                unzipXlsx(inputStream);

        byte[] sheetBytes =
                entries.get("xl/worksheets/sheet1.xml");

        if (sheetBytes == null) {

            for (String name : entries.keySet()) {

                if (name.startsWith("xl/worksheets/")
                        && name.endsWith(".xml")) {

                    sheetBytes = entries.get(name);
                    break;
                }
            }
        }

        if (sheetBytes == null) {
            throw new Exception(
                    "No worksheet was found inside the Excel file."
            );
        }

        List<String> sharedStrings =
                readSharedStrings(
                        entries.get("xl/sharedStrings.xml")
                );

        Document document =
                DocumentBuilderFactory
                        .newInstance()
                        .newDocumentBuilder()
                        .parse(new ByteArrayInputStream(sheetBytes));

        NodeList rowNodes =
                document.getElementsByTagName("row");

        List<List<String>> rows =
                new ArrayList<>();

        for (int r = 0;
             r < rowNodes.getLength();
             r++) {

            Node rowNode =
                    rowNodes.item(r);

            if (!(rowNode instanceof Element)) {
                continue;
            }

            Element row =
                    (Element) rowNode;

            NodeList cellNodes =
                    row.getElementsByTagName("c");

            Map<Integer, String> cells =
                    new HashMap<>();

            int maxColumn = -1;

            for (int c = 0;
                 c < cellNodes.getLength();
                 c++) {

                Node cellNode =
                        cellNodes.item(c);

                if (!(cellNode instanceof Element)) {
                    continue;
                }

                Element cell =
                        (Element) cellNode;

                String reference =
                        getCellReference(cell);

                String columnLetters =
                        reference.replaceAll(
                                "[^A-Za-z]",
                                ""
                        );

                int columnIndex =
                        excelColumnToIndex(
                                columnLetters
                        );

                cells.put(
                        columnIndex,
                        getCellValue(
                                cell,
                                sharedStrings
                        )
                );

                maxColumn =
                        Math.max(
                                maxColumn,
                                columnIndex
                        );
            }

            List<String> values =
                    new ArrayList<>();

            for (int c = 0;
                 c <= maxColumn;
                 c++) {

                values.add(
                        cells.containsKey(c)
                                ? cells.get(c)
                                : ""
                );
            }

            if (!values.isEmpty()) {
                rows.add(values);
            }
        }

        return rows;
    }


    private static int findHeaderColumn(
            List<String> header,
            String... acceptedNames
    ) {

        for (int i = 0; i < header.size(); i++) {

            String normalized =
                    normalizeHeader(
                            header.get(i)
                    );

            for (String accepted : acceptedNames) {

                if (normalized.equals(
                        normalizeHeader(accepted)
                )) {
                    return i;
                }
            }
        }

        return -1;
    }


    // =========================================================
    // FACULTY XLSX IMPORT
    //
    // Designed for the HKBK CSE Faculty Master Excel.
    //
    // Existing information is imported as-is.
    // Missing profile information remains blank.
    // No fake email/phone/qualification/expertise is created.
    // =========================================================

    public static List<Faculty> parseFacultyExcel(
            InputStream inputStream,
            String collegeId,
            String deptId
    ) throws Exception {

        List<List<String>> rows =
                readFirstWorksheet(inputStream);

        if (rows.isEmpty()) {
            return new ArrayList<>();
        }

        int headerRowIndex = -1;

        for (int i = 0; i < rows.size(); i++) {

            List<String> row = rows.get(i);

            boolean hasFacultyName = false;

            for (String cell : row) {

                String h =
                        normalizeHeader(cell);

                if (h.equals("facultyname")
                        || h.equals("name")) {

                    hasFacultyName = true;
                    break;
                }
            }

            if (hasFacultyName) {
                headerRowIndex = i;
                break;
            }
        }

        if (headerRowIndex < 0) {

            throw new Exception(
                    "Faculty Name column was not found in the Excel file."
            );
        }

        List<String> header =
                rows.get(headerRowIndex);

        int colId =
                findHeaderColumn(
                        header,
                        "Faculty ID",
                        "ID",
                        "FacultyID"
                );

        int colName =
                findHeaderColumn(
                        header,
                        "Faculty Name",
                        "Name"
                );

        int colShortName =
                findHeaderColumn(
                        header,
                        "Faculty Short Name",
                        "Short Name",
                        "ShortName"
                );

        int colDesignation =
                findHeaderColumn(
                        header,
                        "Designation (as provided)",
                        "Designation",
                        "Desig",
                        "Post",
                        "Title"
                );

        int colDepartment =
                findHeaderColumn(
                        header,
                        "Department",
                        "Dept"
                );

        int colEmail =
                findHeaderColumn(
                        header,
                        "Email",
                        "Email ID",
                        "EmailID"
                );

        int colPhone =
                findHeaderColumn(
                        header,
                        "Phone",
                        "Mobile",
                        "Contact"
                );

        int colQualification =
                findHeaderColumn(
                        header,
                        "Qualification",
                        "Degree",
                        "Education"
                );

        int colExperience =
                findHeaderColumn(
                        header,
                        "Experience",
                        "Experience Years",
                        "Years of Experience"
                );

        int colSpecialization =
                findHeaderColumn(
                        header,
                        "Specialization",
                        "Specialisation",
                        "Speciality"
                );

        int colExpertise =
                findHeaderColumn(
                        header,
                        "Expertise",
                        "Area of Expertise",
                        "Skills"
                );

        int colWorkload =
                findHeaderColumn(
                        header,
                        "Workload Hours",
                        "Workload",
                        "Max Workload",
                        "Max Workload Hours"
                );

        int colAvailability =
                findHeaderColumn(
                        header,
                        "Availability"
                );

        int colStatus =
                findHeaderColumn(
                        header,
                        "Profile Status",
                        "Status"
                );

        List<Faculty> result =
                new ArrayList<>();

        int generatedCount = 1;

        for (int rowIndex = headerRowIndex + 1;
             rowIndex < rows.size();
             rowIndex++) {

            List<String> row =
                    rows.get(rowIndex);

            String name =
                    getRowValue(
                            row,
                            colName
                    );

            String shortName =
                    getRowValue(
                            row,
                            colShortName
                    );

            String designation =
                    getRowValue(
                            row,
                            colDesignation
                    );

            String email =
                    getRowValue(
                            row,
                            colEmail
                    );

            String phone =
                    getRowValue(
                            row,
                            colPhone
                    );

            String qualification =
                    getRowValue(
                            row,
                            colQualification
                    );

            String experience =
                    getRowValue(
                            row,
                            colExperience
                    );

            String specialization =
                    getRowValue(
                            row,
                            colSpecialization
                    );

            String expertise =
                    getRowValue(
                            row,
                            colExpertise
                    );

            String availability =
                    getRowValue(
                            row,
                            colAvailability
                    );

            String status =
                    getRowValue(
                            row,
                            colStatus
                    );

            String sourceDepartment =
                    getRowValue(
                            row,
                            colDepartment
                    );

            String workloadText =
                    getRowValue(
                            row,
                            colWorkload
                    );

            boolean completelyEmpty =
                    name.isEmpty()
                            && shortName.isEmpty()
                            && designation.isEmpty()
                            && email.isEmpty()
                            && phone.isEmpty()
                            && qualification.isEmpty()
                            && experience.isEmpty()
                            && specialization.isEmpty()
                            && expertise.isEmpty()
                            && availability.isEmpty();

            if (completelyEmpty) {
                continue;
            }

            // Use the ID supplied in the master Excel when available.
            String facultyId =
                    getRowValue(
                            row,
                            colId
                    );

            if (facultyId.isEmpty()) {

                facultyId =
                        "FAC_"
                                + System.currentTimeMillis()
                                + "_"
                                + generatedCount;
            }

            // Never create fake contact information.
            if (!isValidEmail(email)) {
                email = "";
            }

            int workload = 0;

            if (!workloadText.isEmpty()) {
                workload =
                        safeParseInt(
                                workloadText,
                                0
                        );
            }

            // Do NOT infer Professor/Assistant/Associate
            // from "Dr." or "Prof.".
            //
            // The supplied HKBK file only provides the
            // designation/title as written in the source.
            // Exact academic designation can be completed
            // later in the Teacher Profile.

            Faculty faculty =
                    new Faculty(
                            facultyId,
                            null,
                            collegeId,
                            deptId,
                            name,
                            email,
                            phone,
                            designation,
                            specialization,
                            workload
                    );

            faculty.setQualification(
                    qualification
            );

            faculty.setExperience(
                    experience
            );

            faculty.setExpertise(
                    expertise
            );

            faculty.setMaxLoadPerWeek(
                    workload
            );

            faculty.setMaxWorkloadHours(
                    workload
            );

            faculty.setWorkloadHours(0);

            faculty.setStatus(
                    status.isEmpty()
                            ? "PENDING"
                            : status
            );

            /*
             * NOTE:
             * shortName, sourceDepartment and availability
             * are intentionally not forced into another Faculty
             * field. They must not be silently misclassified.
             *
             * They will be used in the subject-allotment mapping
             * step later.
             */

            result.add(faculty);
            generatedCount++;
        }

        return result;
    }


    private static String getRowValue(
            List<String> row,
            int index
    ) {

        if (index < 0
                || index >= row.size()
                || row.get(index) == null) {

            return "";
        }

        return cleanCell(
                row.get(index)
        );
    }


    // =========================================================
    // FACULTY CSV IMPORT
    //
    // IMPORTANT:
    // NO FAKE DATA IS GENERATED.
    //
    // Missing:
    // Name            -> blank
    // Email           -> blank
    // Phone           -> blank
    // Designation     -> blank
    // Qualification   -> blank
    // Experience      -> blank
    // Specialization  -> blank
    // Expertise       -> blank
    //
    // Workload:
    // Professor           = 10
    // Assistant Professor = 14
    // Associate Professor = 16
    // =========================================================

    public static List<Faculty> parseFacultyCsv(
            InputStream inputStream,
            String collegeId,
            String deptId
    ) throws Exception {

        List<Faculty> list = new ArrayList<>();

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        inputStream,
                        StandardCharsets.UTF_8
                )
        );

        String line;

        int count = 1;

        // Default positions for old CSV format
        int colName = 0;
        int colEmail = 1;
        int colPhone = 2;
        int colDesig = 3;
        int colSpec = 4;
        int colLoad = 5;

        int colQualification = -1;
        int colExperience = -1;
        int colExpertise = -1;

        boolean headerProcessed = false;

        while ((line = reader.readLine()) != null) {

            line = line.trim();

            if (line.isEmpty()) {
                continue;
            }

            String[] parts = parseCsvLine(line);

            if (parts.length == 0) {
                continue;
            }


            // =================================================
            // HEADER
            // =================================================

            if (!headerProcessed) {

                headerProcessed = true;

                boolean looksLikeHeader = false;

                for (String part : parts) {

                    String h = normalizeHeader(part);

                    if (h.contains("name")
                            || h.contains("email")
                            || h.contains("faculty")
                            || h.contains("teacher")
                            || h.contains("designation")
                            || h.contains("qualification")
                            || h.contains("experience")
                            || h.contains("expertise")) {

                        looksLikeHeader = true;
                        break;
                    }
                }


                if (looksLikeHeader) {

                    for (int i = 0; i < parts.length; i++) {

                        String h = normalizeHeader(parts[i]);

                        // -------------------------------------
                        // NAME
                        // -------------------------------------

                        if (h.equals("name")
                                || h.equals("facultyname")
                                || h.equals("teachername")
                                || h.equals("facultyfullname")
                                || h.equals("teacherfullname")) {

                            colName = i;
                        }

                        // -------------------------------------
                        // EMAIL
                        // -------------------------------------

                        else if (h.equals("email")
                                || h.equals("emailid")
                                || h.equals("mail")
                                || h.equals("facultyemail")
                                || h.equals("teacheremail")) {

                            colEmail = i;
                        }

                        // -------------------------------------
                        // PHONE
                        // -------------------------------------

                        else if (h.equals("phone")
                                || h.equals("mobile")
                                || h.equals("mobileno")
                                || h.equals("contact")
                                || h.equals("contactno")
                                || h.equals("phonenumber")) {

                            colPhone = i;
                        }

                        // -------------------------------------
                        // DESIGNATION
                        // -------------------------------------

                        else if (h.equals("designation")
                                || h.equals("desig")
                                || h.equals("post")
                                || h.equals("title")
                                || h.equals("role")) {

                            colDesig = i;
                        }

                        // -------------------------------------
                        // QUALIFICATION
                        // -------------------------------------

                        else if (h.equals("qualification")
                                || h.equals("qualifications")
                                || h.equals("degree")
                                || h.equals("education")) {

                            colQualification = i;
                        }

                        // -------------------------------------
                        // EXPERIENCE
                        // -------------------------------------

                        else if (h.equals("experience")
                                || h.equals("exp")
                                || h.equals("experienceyears")
                                || h.equals("yearsofexperience")) {

                            colExperience = i;
                        }

                        // -------------------------------------
                        // SPECIALIZATION
                        // -------------------------------------

                        else if (h.equals("specialization")
                                || h.equals("specialisation")
                                || h.equals("speciality")
                                || h.equals("spec")) {

                            colSpec = i;
                        }

                        // -------------------------------------
                        // EXPERTISE
                        // -------------------------------------

                        else if (h.equals("expertise")
                                || h.equals("areaofexpertise")
                                || h.equals("expertarea")
                                || h.equals("skill")
                                || h.equals("skills")) {

                            colExpertise = i;
                        }

                        // -------------------------------------
                        // MAX WORKLOAD
                        // -------------------------------------

                        else if (h.equals("maxload")
                                || h.equals("maximumload")
                                || h.equals("maxworkload")
                                || h.equals("maxworkloadhours")
                                || h.equals("maxloadperweek")
                                || h.equals("workload")
                                || h.equals("workloadhours")
                                || h.equals("maximumworkload")) {

                            colLoad = i;
                        }
                    }

                    // Header line is not faculty data
                    continue;
                }
            }


            // =================================================
            // FACULTY DATA
            // =================================================

            if (parts.length >= 1) {

                String name =
                        parts.length > colName
                                ? cleanCell(parts[colName])
                                : "";

                String email =
                        parts.length > colEmail
                                ? cleanCell(parts[colEmail])
                                : "";

                String phone =
                        parts.length > colPhone
                                ? cleanCell(parts[colPhone])
                                : "";

                String designation =
                        parts.length > colDesig
                                ? cleanCell(parts[colDesig])
                                : "";

                String qualification =
                        colQualification >= 0
                                && parts.length > colQualification
                                ? cleanCell(parts[colQualification])
                                : "";

                String experience =
                        colExperience >= 0
                                && parts.length > colExperience
                                ? cleanCell(parts[colExperience])
                                : "";

                String specialization =
                        parts.length > colSpec
                                ? cleanCell(parts[colSpec])
                                : "";

                String expertise =
                        colExpertise >= 0
                                && parts.length > colExpertise
                                ? cleanCell(parts[colExpertise])
                                : "";


                // ---------------------------------------------
                // Do NOT generate fake teacher names.
                //
                // Completely empty rows are ignored.
                // ---------------------------------------------

                if (name.isEmpty()
                        && email.isEmpty()
                        && phone.isEmpty()
                        && designation.isEmpty()
                        && qualification.isEmpty()
                        && experience.isEmpty()
                        && specialization.isEmpty()
                        && expertise.isEmpty()) {

                    continue;
                }


                // ---------------------------------------------
                // Invalid email is NOT replaced with fake email.
                //
                // Keep original value only if it is valid.
                // Otherwise blank.
                // ---------------------------------------------

                if (!isValidEmail(email)) {
                    email = "";
                }


                // ---------------------------------------------
                // MAX WORKLOAD
                //
                // If CSV has valid workload -> use it.
                // Otherwise derive from designation.
                // Missing designation -> 0.
                // ---------------------------------------------

                int designationLoad =
                        Faculty.getWorkloadForDesignation(designation);

                int maxLoad = 0;

                if (colLoad >= 0 && parts.length > colLoad) {

                    String loadText =
                            cleanCell(parts[colLoad]);

                    if (!loadText.isEmpty()) {

                        maxLoad = safeParseInt(
                                loadText,
                                designationLoad
                        );
                    } else {
                        maxLoad = designationLoad;
                    }

                } else {

                    maxLoad = designationLoad;
                }


                // ---------------------------------------------
                // APP-GENERATED UNIQUE FACULTY ID
                // ---------------------------------------------

                String docId =
                        "FAC_"
                                + System.currentTimeMillis()
                                + "_"
                                + count;


                // ---------------------------------------------
                // Create Faculty
                // ---------------------------------------------

                Faculty faculty = new Faculty(
                        docId,
                        null,
                        collegeId,
                        deptId,
                        name,
                        email,
                        phone,
                        designation,
                        specialization,
                        maxLoad
                );


                // ---------------------------------------------
                // Additional profile information
                // ---------------------------------------------

                faculty.setQualification(qualification);
                faculty.setExperience(experience);
                faculty.setExpertise(expertise);

                faculty.setMaxLoadPerWeek(maxLoad);
                faculty.setMaxWorkloadHours(maxLoad);

                // Current teaching workload starts at 0.
                faculty.setWorkloadHours(0);

                // Imported faculty is active as a data record.
                // Account can be linked later.
                faculty.setStatus("ACTIVE");


                list.add(faculty);

                count++;
            }
        }

        reader.close();

        return list;
    }


    // =========================================================
    // STUDENT EXCEL (.xlsx)
    // =========================================================

    public static List<Student> parseStudentExcel(
            InputStream inputStream,
            String collegeId,
            String deptId
    ) throws Exception {

        List<List<String>> rows = readFirstWorksheet(inputStream);
        if (rows.isEmpty()) {
            return new ArrayList<>();
        }

        int headerRowIndex = -1;
        for (int i = 0; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            for (String cell : row) {
                String h = normalizeHeader(cell);
                if (h.equals("studentname") || h.equals("name") || h.equals("usn") || h.equals("rollno")) {
                    headerRowIndex = i;
                    break;
                }
            }
            if (headerRowIndex >= 0) break;
        }

        if (headerRowIndex < 0) {
            throw new Exception("Student header row (Name or USN) not found in the Excel file.");
        }

        List<String> header = rows.get(headerRowIndex);

        int colUsn = findHeaderColumn(header, "USN", "Roll No", "Roll Number", "Registration No", "ID", "StudentID", "Student ID");
        int colName = findHeaderColumn(header, "Student Name", "Name", "StudentName", "Candidate Name");
        int colEmail = findHeaderColumn(header, "Email", "Email ID", "Student Email", "Email Address");
        int colPhone = findHeaderColumn(header, "Phone", "Mobile", "Contact", "Phone Number", "Mobile No");
        int colSem = findHeaderColumn(header, "Semester", "Sem", "Term");
        int colSec = findHeaderColumn(header, "Section", "Sec", "Division");

        List<Student> result = new ArrayList<>();
        int count = 1;

        for (int r = headerRowIndex + 1; r < rows.size(); r++) {
            List<String> row = rows.get(r);
            if (row.isEmpty()) continue;

            String name = colName >= 0 && colName < row.size() ? cleanCell(row.get(colName)) : "";
            String usn = colUsn >= 0 && colUsn < row.size() ? cleanCell(row.get(colUsn)) : "";

            if (name.isEmpty() && usn.isEmpty()) {
                continue;
            }

            if (name.isEmpty()) name = "Student " + count;
            if (usn.isEmpty()) usn = "USN_" + count;

            String email = colEmail >= 0 && colEmail < row.size() ? cleanCell(row.get(colEmail)) : "";
            if (!isValidEmail(email)) {
                email = "";
            }

            String phone = colPhone >= 0 && colPhone < row.size() ? cleanCell(row.get(colPhone)) : "";

            int sem = 1;
            if (colSem >= 0 && colSem < row.size()) {
                sem = normalizeSemester(row.get(colSem), 1);
            }
            if (sem <= 0 || sem > 8) sem = 1;

            String sec = colSec >= 0 && colSec < row.size() ? normalizeSection(row.get(colSec)) : "A";
            if (sec.isEmpty()) sec = "A";

            String cleanUsn = usn.replaceAll("[^a-zA-Z0-9]", "_").toUpperCase();
            String docId = "STU_" + collegeId + "_" + cleanUsn;
            String uid = "UID_" + docId;

            Student student = new Student(docId, uid, collegeId, deptId, usn, name, email, phone, sem, sec);
            result.add(student);
            count++;
        }

        return result;
    }


    // =========================================================
    // STUDENT CSV
    // =========================================================

    public static List<Student> parseStudentCsv(
            InputStream inputStream,
            String collegeId,
            String deptId
    ) throws Exception {

        List<Student> list = new ArrayList<>();

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        inputStream,
                        StandardCharsets.UTF_8
                )
        );

        String line;
        int count = 1;

        int colName = 0;
        int colUsn = 1;
        int colEmail = 2;
        int colPhone = 3;
        int colSem = 4;
        int colSec = 5;

        boolean headerProcessed = false;

        while ((line = reader.readLine()) != null) {

            line = line.trim();

            if (line.isEmpty()) {
                continue;
            }

            String[] parts = parseCsvLine(line);

            if (parts.length == 0) {
                continue;
            }


            if (!headerProcessed) {

                headerProcessed = true;

                String lowerLine = line.toLowerCase();

                if (lowerLine.contains("name")
                        || lowerLine.contains("usn")
                        || lowerLine.contains("student")) {

                    for (int i = 0; i < parts.length; i++) {

                        String h =
                                parts[i]
                                        .toLowerCase()
                                        .trim();

                        if (h.contains("name")) {
                            colName = i;
                        } else if (h.contains("usn")
                                || h.contains("roll")
                                || h.contains("id")) {

                            colUsn = i;

                        } else if (h.contains("email")) {

                            colEmail = i;

                        } else if (h.contains("phone")
                                || h.contains("mobile")) {

                            colPhone = i;

                        } else if (h.contains("sem")) {

                            colSem = i;

                        } else if (h.contains("sec")) {

                            colSec = i;
                        }
                    }

                    continue;
                }
            }


            if (parts.length >= 1) {

                String name =
                        parts.length > colName
                                ? sanitizeText(parts[colName])
                                : "";

                if (name.isEmpty()) {
                    name = "Student " + count;
                }

                String usn =
                        parts.length > colUsn
                                ? sanitizeText(parts[colUsn])
                                : "USN_" + count;

                String email =
                        parts.length > colEmail
                                ? sanitizeText(parts[colEmail])
                                : "";

                if (!isValidEmail(email)) {
                    email = "";
                }

                String phone =
                        parts.length > colPhone
                                ? sanitizeText(parts[colPhone])
                                : "";

                int sem =
                        parts.length > colSem
                                ? normalizeSemester(parts[colSem], 1)
                                : 1;

                String sec =
                        parts.length > colSec
                                ? normalizeSection(parts[colSec])
                                : "A";

                String cleanUsn = usn.replaceAll("[^a-zA-Z0-9]", "_").toUpperCase();
                String docId = "STU_" + collegeId + "_" + cleanUsn;
                String uid = "UID_" + docId;

                list.add(
                        new Student(
                                docId,
                                uid,
                                collegeId,
                                deptId,
                                usn,
                                name,
                                email,
                                phone,
                                sem,
                                sec
                        )
                );

                count++;
            }
        }

        reader.close();

        return list;
    }


    // =========================================================
    // CLASSROOM CSV
    //
    // Existing behavior preserved.
    // =========================================================

    public static List<Classroom> parseClassroomCsv(
            InputStream inputStream,
            String collegeId
    ) throws Exception {

        List<Classroom> list = new ArrayList<>();

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        inputStream,
                        StandardCharsets.UTF_8
                )
        );

        String line;
        int count = 1;

        int colRoom = 0;
        int colBlock = 1;
        int colCap = 2;
        int colProj = 3;
        int colLab = 4;

        boolean headerProcessed = false;

        while ((line = reader.readLine()) != null) {

            line = line.trim();

            if (line.isEmpty()) {
                continue;
            }

            String[] parts = parseCsvLine(line);

            if (parts.length == 0) {
                continue;
            }


            if (!headerProcessed) {

                headerProcessed = true;

                String lowerLine = line.toLowerCase();

                if (lowerLine.contains("room")
                        || lowerLine.contains("block")
                        || lowerLine.contains("capacity")) {

                    for (int i = 0; i < parts.length; i++) {

                        String h =
                                parts[i]
                                        .toLowerCase()
                                        .trim();

                        if (h.contains("room")
                                || h.contains("no")
                                || h.contains("number")) {

                            colRoom = i;

                        } else if (h.contains("block")
                                || h.contains("building")) {

                            colBlock = i;

                        } else if (h.contains("cap")
                                || h.contains("seat")) {

                            colCap = i;

                        } else if (h.contains("proj")) {

                            colProj = i;

                        } else if (h.contains("lab")) {

                            colLab = i;
                        }
                    }

                    continue;
                }
            }


            if (parts.length >= 1) {

                String roomNo =
                        parts.length > colRoom
                                ? sanitizeText(parts[colRoom])
                                : "LH-" + count;

                String block =
                        parts.length > colBlock
                                ? sanitizeText(parts[colBlock])
                                : "Main Block";

                int capacity =
                        parts.length > colCap
                                ? safeParseInt(parts[colCap], 60)
                                : 60;

                boolean projector =
                        parts.length > colProj
                                && Boolean.parseBoolean(
                                parts[colProj].trim()
                        );

                boolean isLab =
                        parts.length > colLab
                                && Boolean.parseBoolean(
                                parts[colLab].trim()
                        );

                String id =
                        "ROOM_"
                                + System.currentTimeMillis()
                                + "_"
                                + count;

                list.add(
                        new Classroom(
                                id,
                                collegeId,
                                roomNo,
                                block,
                                capacity,
                                projector,
                                isLab
                        )
                );

                count++;
            }
        }

        reader.close();

        return list;
    }


    // =========================================================
    // IMPORT FACULTY TO FIRESTORE
    //
    // Uses asynchronous Firestore commits.
    // This prevents blocking the Android main/UI thread.
    // =========================================================

    public static void importFacultyToFirestore(
            List<Faculty> facultyList,
            ImportCallback callback
    ) {

        if (facultyList == null || facultyList.isEmpty()) {

            if (callback != null) {
                callback.onSuccess(0);
            }

            return;
        }

        commitFacultyBatch(
                facultyList,
                0,
                0,
                callback
        );
    }


    private static void commitFacultyBatch(
            List<Faculty> facultyList,
            int start,
            int importedSoFar,
            ImportCallback callback
    ) {

        final int BATCH_LIMIT = 450;

        final int end =
                Math.min(
                        start + BATCH_LIMIT,
                        facultyList.size()
                );

        try {

            WriteBatch batch =
                    FirebaseHelper
                            .getFirestore()
                            .batch();

            for (int i = start;
                 i < end;
                 i++) {

                Faculty f =
                        facultyList.get(i);

                batch.set(
                        FirebaseHelper
                                .getFacultyRef()
                                .document(f.getId()),
                        f
                );

                String email =
                        f.getEmail();

                if (isValidEmail(email)) {

                    Map<String, Object> pendingUser =
                            new HashMap<>();

                    pendingUser.put(
                            "email",
                            email
                    );

                    pendingUser.put(
                            "name",
                            f.getName()
                    );

                    pendingUser.put(
                            "role",
                            User.ROLE_TEACHER
                    );

                    pendingUser.put(
                            "collegeId",
                            f.getCollegeId()
                    );

                    pendingUser.put(
                            "departmentId",
                            f.getDepartmentId()
                    );

                    pendingUser.put(
                            "facultyDocId",
                            f.getId()
                    );

                    pendingUser.put(
                            "status",
                            "INVITED"
                    );

                    String key =
                            email
                                    .toLowerCase()
                                    .replaceAll(
                                            "[^a-z0-9]",
                                            "_"
                                    );

                    batch.set(
                            FirebaseHelper
                                    .getFirestore()
                                    .collection(
                                            "user_invitations"
                                    )
                                    .document(key),
                            pendingUser
                    );

                    String uid = f.getUid() != null && !f.getUid().isEmpty() ? f.getUid() : "UID_" + f.getId();
                    User u = new User(uid, f.getName(), email, User.ROLE_TEACHER, f.getCollegeId(), f.getDepartmentId(), f.getPhone());
                    u.setDesignation(f.getDesignation());
                    batch.set(FirebaseHelper.getUsersRef().document(uid), u);
                }
            }

            final int currentBatchCount =
                    end - start;

            batch.commit()
                    .addOnSuccessListener(aVoid -> {

                        int newImportedTotal =
                                importedSoFar
                                        + currentBatchCount;

                        if (end >= facultyList.size()) {

                            if (callback != null) {
                                callback.onSuccess(
                                        newImportedTotal
                                );
                            }

                        } else {

                            commitFacultyBatch(
                                    facultyList,
                                    end,
                                    newImportedTotal,
                                    callback
                            );
                        }
                    })
                    .addOnFailureListener(e -> {

                        if (callback != null) {

                            callback.onError(
                                    e.getMessage() != null
                                            ? e.getMessage()
                                            : "Faculty import failed."
                            );
                        }
                    });

        } catch (Exception e) {

            if (callback != null) {

                callback.onError(
                        e.getMessage() != null
                                ? e.getMessage()
                                : "Faculty import failed."
                );
            }
        }
    }


    // =========================================================
    // IMPORT STUDENTS TO FIRESTORE
    // =========================================================

    public static void importStudentsToFirestore(
            List<Student> studentList,
            ImportCallback callback
    ) {

        if (studentList == null || studentList.isEmpty()) {

            if (callback != null) {
                callback.onSuccess(0);
            }

            return;
        }


        WriteBatch batch =
                FirebaseHelper
                        .getFirestore()
                        .batch();


        for (Student s : studentList) {

            batch.set(
                    FirebaseHelper
                            .getStudentsRef()
                            .document(s.getId()),
                    s
            );


            Map<String, Object> pendingUser =
                    new HashMap<>();

            pendingUser.put(
                    "email",
                    s.getEmail()
            );

            pendingUser.put(
                    "name",
                    s.getName()
            );

            pendingUser.put(
                    "usn",
                    s.getUsn()
            );

            pendingUser.put(
                    "role",
                    User.ROLE_STUDENT
            );

            pendingUser.put(
                    "collegeId",
                    s.getCollegeId()
            );

            pendingUser.put(
                    "departmentId",
                    s.getDepartmentId()
            );

            pendingUser.put(
                    "studentDocId",
                    s.getId()
            );

            pendingUser.put(
                    "status",
                    "INVITED"
            );


            String key =
                    s.getEmail()
                            .toLowerCase()
                            .replaceAll(
                                    "[^a-z0-9]",
                                    "_"
                            );


            batch.set(
                    FirebaseHelper
                            .getFirestore()
                            .collection(
                                    "user_invitations"
                            )
                            .document(key),
                    pendingUser
            );



            String uid = s.getUid() != null && !s.getUid().isEmpty() ? s.getUid() : "UID_" + s.getId();
            User u = new User(uid, s.getName(), s.getEmail(), User.ROLE_STUDENT, s.getCollegeId(), s.getDepartmentId(), s.getPhone());
            u.setSectionId(s.getSection());
            u.setSemester(s.getSemester() > 0 ? String.valueOf(s.getSemester()) : "");
            u.setUsn(s.getUsn());
            batch.set(FirebaseHelper.getUsersRef().document(uid), u);
        }


        batch.commit()
                .addOnSuccessListener(aVoid -> {

                    if (callback != null) {
                        callback.onSuccess(
                                studentList.size()
                        );
                    }

                })
                .addOnFailureListener(e -> {

                    if (callback != null) {
                        callback.onError(
                                e.getMessage()
                        );
                    }
                });
    }


    // =========================================================
    // IMPORT CLASSROOMS TO FIRESTORE
    // =========================================================

    public static void importClassroomsToFirestore(
            List<Classroom> classroomList,
            ImportCallback callback
    ) {

        if (classroomList == null
                || classroomList.isEmpty()) {

            if (callback != null) {
                callback.onSuccess(0);
            }

            return;
        }


        WriteBatch batch =
                FirebaseHelper
                        .getFirestore()
                        .batch();


        for (Classroom c : classroomList) {

            batch.set(
                    FirebaseHelper
                            .getClassroomsRef()
                            .document(c.getId()),
                    c
            );
        }


        batch.commit()
                .addOnSuccessListener(aVoid -> {

                    if (callback != null) {
                        callback.onSuccess(
                                classroomList.size()
                        );
                    }

                })
                .addOnFailureListener(e -> {

                    if (callback != null) {
                        callback.onError(
                                e.getMessage()
                        );
                    }
                });
    }
}
