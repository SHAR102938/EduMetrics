package com.edumetrics.app.activities.student;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.edumetrics.app.R;
import com.edumetrics.app.activities.auth.LoginActivity;
import com.edumetrics.app.analytics.ConsistencyScoreCalculator;
import com.edumetrics.app.analytics.DisciplineIndexCalculator;
import com.edumetrics.app.database.EduMetricsDatabase;
import com.edumetrics.app.utils.AnimationHelper;
import com.edumetrics.app.utils.SessionManager;

public class StudentDashboardActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private EduMetricsDatabase db;
    private com.edumetrics.app.services.FirebaseService firebaseService;

    private TextView tvUserName, tvAttendance, tvConsistency, tvDiscipline, tvDisciplineLabel;
    private TextView tvPendingTasks, tvCompletedTasks;
    private ProgressBar pbAttendance, pbConsistency;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

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
        tvAttendance = findViewById(R.id.tvAttendance);
        tvConsistency = findViewById(R.id.tvConsistency);
        tvDiscipline = findViewById(R.id.tvDiscipline);
        tvDisciplineLabel = findViewById(R.id.tvDisciplineLabel);
        tvPendingTasks = findViewById(R.id.tvPendingTasks);
        tvCompletedTasks = findViewById(R.id.tvCompletedTasks);
        pbAttendance = findViewById(R.id.pbAttendance);
        pbConsistency = findViewById(R.id.pbConsistency);
    }

    private void setupListeners() {
        findViewById(R.id.btnScanQR).setOnClickListener(v -> {
            AnimationHelper.buttonPress(v);
            startActivity(new Intent(this, QRScannerActivity.class));
            overridePendingTransition(R.anim.slide_up_fade_in, R.anim.fade_out);
        });

        findViewById(R.id.btnSubjects).setOnClickListener(v -> {
            AnimationHelper.buttonPress(v);
            startActivity(new Intent(this, SubjectListActivity.class));
        });

        findViewById(R.id.btnTasks).setOnClickListener(v -> {
            AnimationHelper.buttonPress(v);
            startActivity(new Intent(this, TaskListActivity.class));
        });

        findViewById(R.id.btnJoinClass).setOnClickListener(v -> {
            AnimationHelper.buttonPress(v);
            startActivity(new Intent(this, JoinClassActivity.class));
        });

        findViewById(R.id.ivProfile).setOnClickListener(v ->
                startActivity(new Intent(this, StudentProfileActivity.class)));

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
        int userId = sessionManager.getUserId();
        tvUserName.setText(sessionManager.getUserName());

        // Attendance
        int totalPresent = db.attendanceDao().getTotalPresentCount(userId);
        int totalClasses = db.attendanceDao().getTotalAttendanceCount(userId);
        int attendancePct = totalClasses > 0 ? (totalPresent * 100 / totalClasses) : 0;
        tvAttendance.setText(attendancePct + "%");
        AnimationHelper.animateProgressBar(pbAttendance, attendancePct);

        // Consistency Score
        ConsistencyScoreCalculator csCalc = new ConsistencyScoreCalculator(db);
        float consistency = csCalc.calculate(userId);
        tvConsistency.setText(String.format("%.0f", consistency));
        AnimationHelper.animateProgressBar(pbConsistency, (int) consistency);

        // Discipline Index
        DisciplineIndexCalculator diCalc = new DisciplineIndexCalculator(db);
        float discipline = diCalc.calculate(userId);
        tvDiscipline.setText(String.format("%.0f", discipline));
        tvDisciplineLabel.setText(DisciplineIndexCalculator.getLabel(discipline));

        // Tasks
        int pending = db.taskDao().getPendingTaskCount(userId);
        int completed = db.taskDao().getCompletedTaskCount(userId);
        tvPendingTasks.setText(String.valueOf(pending));
        tvCompletedTasks.setText(completed + " completed");

        // Animate cards
        AnimationHelper.staggeredSlideUp(findViewById(R.id.cardAttendance), 0);
        AnimationHelper.staggeredSlideUp(findViewById(R.id.cardConsistency), 1);
        AnimationHelper.staggeredSlideUp(findViewById(R.id.cardDiscipline), 2);
        AnimationHelper.staggeredSlideUp(findViewById(R.id.cardTasks), 3);
    }
}
