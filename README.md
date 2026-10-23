# CampusSchedulerAI 🎓🤖

[![Android](https://img.shields.io/badge/Platform-Android%20(SDK%2034)-green.svg)](https://developer.android.com)
[![Language](https://img.shields.io/badge/Language-Java%2017-orange.svg)](https://www.oracle.com/java/)
[![Firebase](https://img.shields.io/badge/Backend-Google%20Firebase-yellow.svg)](https://firebase.google.com/)
[![Algorithm](https://img.shields.io/badge/AI-Genetic%20Algorithm%20%2B%20XAI-blue.svg)](https://github.com/kaif168/CampusShedulerAI)

> **CampusSchedulerAI** is an enterprise-grade, multi-tenant academic management and AI-driven automated timetable scheduling platform developed for universities, engineering colleges, and educational institutions.

---

## 🌟 Key Features

### 🧬 AI & Algorithmic Engines
- **Genetic Algorithm Scheduling**: Evolutionary multi-variable timetable solver allocating theory lectures and laboratory blocks across teachers, classrooms, and sections.
- **16 Strict Hard Constraints Evaluated**:
  - Teacher & section double-booking prevention
  - Room and laboratory clash detection
  - Designation-based faculty workload limits (Professor: 10h, Assistant Professor: 14h, Associate Professor: 16h)
  - Protected tea breaks (09:00 - 09:20 AM) and lunch recess (12:50 - 01:40 PM)
  - Friday & Saturday early cutoff compliance (Periods 1-5 strictly ending by 12:50 PM)
  - Multi-period laboratory continuity and room equipment matching
  - Cross-college tenant boundary isolation
  - Institutional start time contiguity (Period 1 at 08:20 AM)
- **Explainable AI (XAI)**: Generates human-readable audit rationales for every slot allocation and constraint outcome.
- **Academic NLP Assistant**: Context-aware academic assistant answering questions about upcoming lectures, tomorrow's first class, daily schedules, leave status, and substitute assignments.
- **Smart Teacher Substitution**: Intelligent substitute ranking and heuristic scoring during faculty leave based on subject compatibility, free-slot alignment, and remaining weekly workload.
- **Attendance Anomaly Engine**: Detects duplicate attendance submissions and statistical outlier spikes.

### 👥 Multi-Tenant Role Modules
- **Official Owner (`ROLE_OFFICIAL_OWNER`)**: Multi-college tenant onboarding, subscription metrics, global platform updates, and cross-college security audits.
- **College Administrator (`ROLE_COLLEGE_ADMIN`, `ADMIN`, `HOD`)**: Full master data management (Departments, Faculty, Students, Classrooms, Labs, Semesters, Sections), class assignments, AI timetable generator, exam schedules, and bulk imports.
- **Faculty (`ROLE_TEACHER`)**: Real-time personal schedule, slot-based attendance registers, assignments, learning material sharing, leave applications, and gradebook.
- **Student (`ROLE_STUDENT`)**: Live class detection ("What is my next class?", "Where is my lecture?"), attendance tracker, coursework submission, in-app quizzes, and academic forum discussions.

### 🛠️ Built-in Infrastructure
- **Native A4 Landscape PDF Engine**: Pixel-perfect vector PDF generation (`TimetablePdfExporter`) formatted for institutional standards with college branding, break indicators, and signature blocks.
- **Zero-Dependency Bulk Importer**: Custom streaming `.xlsx` (OpenXML ZIP/DOM) and CSV parser (`ExcelImporter`) for rapid ingestion of thousands of students and faculty without heavy external dependencies.
- **Multi-App Account Provisioning**: Uses secondary `FirebaseApp` sandboxes alongside Node.js Cloud Functions to provision teacher and student accounts without signing out the administrator.

---

## 🏗️ System Architecture

```
CampusSchedulerAI/
├── app/
│   ├── src/main/java/com/campusscheduler/ai/
│   │   ├── activities/     # 50+ persona-specific dashboard & management screens
│   │   ├── adapters/       # Generic and domain-specific RecyclerView adapters
│   │   ├── engine/         # AI scheduling, Genetic Algorithm, XAI, NLP chatbot
│   │   ├── models/         # 30+ domain models (Timetable, Faculty, Section, etc.)
│   │   ├── repository/     # Data abstraction layer for Cloud Firestore
│   │   └── utils/          # ExcelImporter, TimetablePdfExporter, Auth provisioning
│   └── src/main/res/       # Material Design XML layouts and vectors
├── functions/              # Node.js 18 serverless Cloud Functions
├── firestore.rules         # Multi-tenant security rules with RBAC
└── firebase.json           # Firebase project configurations
```

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio Iguana / Jellyfish** (or newer)
- **JDK 17**
- **Android SDK 34** (Min SDK: 24)
- **Firebase Project** with Authentication, Cloud Firestore, and Cloud Storage enabled

### Setup Instructions
1. Clone the repository:
   ```bash
   git clone https://github.com/kaif168/CampusShedulerAI.git
   cd CampusShedulerAI
   ```
2. Place your `google-services.json` file inside the `app/` directory:
   ```
   app/google-services.json
   ```
3. Sync Gradle and build the project in Android Studio or via CLI:
   ```bash
   ./gradlew assembleDebug
   ```
4. Deploy Firestore security rules:
   ```bash
   firebase deploy --only firestore:rules
   ```

---

## 📄 License
This project is proprietary and intended for institutional educational administration.
