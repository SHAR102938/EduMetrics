# EduMetrics - Smart Academic Management System

EduMetrics is a comprehensive Android application designed to bridge the gap between students and faculty. It focuses on simplifying academic tracking through an "Offline-First" approach, ensuring that core functionalities like attendance and task management work seamlessly even without a stable internet connection, while providing real-time cloud synchronization via Firebase.

---

## 🚀 Key Features

### 👨‍🏫 For Faculty (Teachers)
*   **Class Management:** Create unique classes and generate join codes for students.
*   **Subject Management:** Organize subjects within specific classes.
*   **Smart Attendance (QR System):** Generate dynamic, time-sensitive QR codes for marking attendance.
*   **Task & Assignment Distribution:** Assign practicals and assignments to specific classes; track completion status in real-time.
*   **Academic Grading:** Add marks for assignments/practicals with detailed descriptions (e.g., "Unit Test 1", "Mid-term Project").
*   **Student Monitoring:** View a list of students joined in each class and monitor their progress.
*   **Automated Reports:** Generate PDF reports for attendance and performance.

### 👨‍🎓 For Students
*   **Join Class:** Enroll in classes using a simple 6-digit code provided by teachers.
*   **QR Scanner:** Mark attendance securely by scanning the teacher's dynamic QR code.
*   **Subject Overview:** View all enrolled subjects and relevant academic data.
*   **Task Tracker:** View assignments and practicals assigned by teachers. Mark them as completed upon finishing.
*   **Personal To-Do List:** Create and manage private personal tasks alongside academic assignments.
*   **Performance Analytics:** View attendance percentages and marks through interactive charts.

---

## 🛠 Tech Stack
*   **Language:** Java
*   **Database (Local):** Room Database (SQLite) for offline data persistence.
*   **Database (Cloud):** Firebase Firestore for real-time synchronization across devices.
*   **Authentication:** Firebase Auth (Email/Password).
*   **QR Functionality:** ZXing (Zebra Crossing) for generation and scanning.
*   **Visualizations:** MPAndroidChart for student performance graphs.
*   **Animations:** Lottie for a modern and interactive UI experience.

---

## 🛡 How We Solved the Attendance Issue

One of the biggest challenges in classroom management is **Attendance Fraud** (e.g., students marking attendance for their absent friends). EduMetrics solves this using a **Dynamic QR Security Layer**:

1.  **Time-Sensitive Tokens:** The teacher's QR code is not a static image. It contains a unique `sessionId` and a `timestamp`.
2.  **5-Minute Expiration:** The QR code automatically expires after 5 minutes. If a student tries to scan an old photo or a leaked code later, the app will reject it as "Session Expired."
3.  **One-Device-One-Scan:** Each attendance record is tied to a specific session ID and Student UID in the cloud. A student cannot mark attendance twice for the same session.
4.  **Geolocation/Proximity (Optional Logic):** The system ensures that the student is physically present by requiring them to scan the code directly from the teacher's screen in a limited time window.

---

## 🔄 Offline-First Synchronization

EduMetrics uses a sophisticated sync architecture:
*   **Immediate Local Save:** When a teacher creates a class or a student marks attendance, the data is saved instantly to the local **Room DB**. This ensures no data is lost if the internet drops.
*   **Background Sync:** The app uses a `FirebaseService` to push local records to **Firestore** as soon as a connection is detected.
*   **Cloud Fallback:** When a student joins a class, the app first checks the cloud if the code isn't found locally, ensuring seamless cross-device communication.

---

## 📝 Setup Instructions

1.  **Clone the Repository.**
2.  **Firebase Setup:**
    *   Create a project in [Firebase Console](https://console.firebase.google.com/).
    *   Enable **Email/Password** Authentication.
    *   Create a **Firestore Database**.
    *   Download `google-services.json` and place it in the `app/` directory.
3.  **Build & Run:** Open the project in Android Studio and run it on your device/emulator.

---

**Developed for a smarter, more transparent academic environment.**
