package com.edumetrics.app.activities.faculty;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.edumetrics.app.R;
import com.edumetrics.app.activities.auth.LoginActivity;
import com.edumetrics.app.database.EduMetricsDatabase;
import com.edumetrics.app.database.entities.ClassEntity;
import com.edumetrics.app.utils.AnimationHelper;
import com.edumetrics.app.utils.SessionManager;

import java.util.List;

public class FacultyDashboardActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private EduMetricsDatabase db;
    private TextView tvUserName, tvClassName, tvClassCode, tvStudentCount;
    private com.edumetrics.app.services.FirebaseService firebaseService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_dashboard);

        sessionManager = new SessionManager(this);
        db = EduMetricsDatabase.getInstance(this);
        firebaseService = new com.edumetrics.app.services.FirebaseService(this);

        initViews();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardData();
        firebaseService.syncLocalToCloud();
        firebaseService.syncCloudToLocal(() -> loadDashboardData());
    }



    private void initViews() {
        tvUserName = findViewById(R.id.tvUserName);
        tvClassName = findViewById(R.id.tvClassName);
        tvClassCode = findViewById(R.id.tvClassCode);
        tvStudentCount = findViewById(R.id.tvStudentCount);
    }

    private void setupListeners() {
        findViewById(R.id.btnClassMgmt).setOnClickListener(v -> {
            AnimationHelper.buttonPress(v);
            startActivity(new Intent(this, ClassManagementActivity.class));
        });

        findViewById(R.id.btnSubjectMgmt).setOnClickListener(v -> {
            AnimationHelper.buttonPress(v);
            startActivity(new Intent(this, SubjectManagementActivity.class));
        });

        findViewById(R.id.btnGenerateQR).setOnClickListener(v -> {
            AnimationHelper.buttonPress(v);
            startActivity(new Intent(this, QRGeneratorActivity.class));
        });

        findViewById(R.id.btnAttendanceRec).setOnClickListener(v -> {
            AnimationHelper.buttonPress(v);
            startActivity(new Intent(this, AttendanceRecordsActivity.class));
        });

        findViewById(R.id.btnTaskMgmt).setOnClickListener(v -> {
            AnimationHelper.buttonPress(v);
            startActivity(new Intent(this, TaskManagementActivity.class));
        });

        findViewById(R.id.btnStudentList).setOnClickListener(v -> {
            AnimationHelper.buttonPress(v);
            startActivity(new Intent(this, StudentListActivity.class));
        });

        findViewById(R.id.btnReports).setOnClickListener(v -> {
            AnimationHelper.buttonPress(v);
            startActivity(new Intent(this, ReportActivity.class));
        });

        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
            sessionManager.logout();
            new Thread(() -> db.clearAllTables()).start();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void loadDashboardData() {
        tvUserName.setText(sessionManager.getUserName());

        List<ClassEntity> classes = db.classDao().getClassesByFaculty(sessionManager.getUserId());
        if (!classes.isEmpty()) {
            ClassEntity activeClass = null;
            int currentClassId = sessionManager.getCurrentClassId();
            
            // Try to find the user's previously selected class
            if (currentClassId > 0) {
                for (ClassEntity c : classes) {
                    if (c.id == currentClassId) {
                        activeClass = c;
                        break;
                    }
                }
            }
            
            // If none selected or not found, fallback to first class
            if (activeClass == null) {
                activeClass = classes.get(0);
                sessionManager.setCurrentClassId(activeClass.id);
            }
            
            tvClassName.setText(activeClass.name);
            tvClassCode.setText("Code: " + activeClass.classCode);

            int studentCount = db.classDao().getStudentCountInClass(activeClass.id);
            tvStudentCount.setText(studentCount + " students");
        } else {
            tvClassName.setText("No classes yet");
            tvClassCode.setText("Create a class to get started");
            tvStudentCount.setText("");
        }

        // Animate
        AnimationHelper.slideUpFadeIn(findViewById(R.id.cardClassInfo), 0);
    }
}
