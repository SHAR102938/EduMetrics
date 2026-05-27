package com.edumetrics.app.activities.student;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.edumetrics.app.R;
import com.edumetrics.app.adapters.ScoreAdapter;
import com.edumetrics.app.database.EduMetricsDatabase;
import com.edumetrics.app.database.entities.Score;
import com.edumetrics.app.database.entities.Subject;
import com.edumetrics.app.database.entities.User;
import com.edumetrics.app.utils.AnimationHelper;
import com.edumetrics.app.utils.SessionManager;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.util.ArrayList;
import java.util.List;

public class SubjectDetailActivity extends AppCompatActivity {

    private EduMetricsDatabase db;
    private SessionManager sessionManager;
    private int subjectId;
    private com.edumetrics.app.services.FirebaseService firebaseService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subject_detail);

        db = EduMetricsDatabase.getInstance(this);
        sessionManager = new SessionManager(this);
        firebaseService = new com.edumetrics.app.services.FirebaseService(this);
        subjectId = getIntent().getIntExtra("subject_id", -1);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        loadSubjectData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSubjectData();
        syncScoresFromCloud();
    }

    private void syncScoresFromCloud() {
        if (subjectId == -1) return;
        int userId = sessionManager.getUserId();
        com.google.firebase.database.FirebaseDatabase.getInstance().getReference("scores")
            .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(@androidx.annotation.NonNull com.google.firebase.database.DataSnapshot snapshot) {
                    new Thread(() -> {
                        for (com.google.firebase.database.DataSnapshot ds : snapshot.getChildren()) {
                            try {
                                com.edumetrics.app.database.entities.Score score = ds.getValue(com.edumetrics.app.database.entities.Score.class);
                                if (score != null && score.studentId == userId && score.subjectId == subjectId) {
                                    if (db.scoreDao().getScoreById(score.id) == null) {
                                        db.scoreDao().insert(score);
                                    }
                                }
                            } catch (Exception ignored) {}
                        }
                        runOnUiThread(() -> loadSubjectData());
                    }).start();
                }
                @Override
                public void onCancelled(@androidx.annotation.NonNull com.google.firebase.database.DatabaseError error) {}
            });
    }

    private void loadSubjectData() {
        if (subjectId == -1) return;
        int userId = sessionManager.getUserId();

        Subject subject = db.subjectDao().getSubjectById(subjectId);
        if (subject == null) return;

        ((TextView) findViewById(R.id.tvSubjectName)).setText(subject.name);

        // Faculty name
        User faculty = db.userDao().getUserById(subject.facultyId);
        if (faculty != null) {
            ((TextView) findViewById(R.id.tvFacultyName)).setText("Faculty: " + faculty.name);
        }

        // Attendance
        int present = db.attendanceDao().getPresentCount(userId, subjectId);
        int total = db.attendanceDao().getTotalCount(userId, subjectId);
        int pct = total > 0 ? (present * 100 / total) : 0;

        ((TextView) findViewById(R.id.tvAttendancePct)).setText(pct + "%");
        ((TextView) findViewById(R.id.tvAttendanceDetail)).setText(present + "/" + total + " classes");
        AnimationHelper.animateProgressBar((ProgressBar) findViewById(R.id.pbAttendance), pct);

        // Scores
        List<Score> scores = db.scoreDao().getScoresByStudentAndSubject(userId, subjectId);
        RecyclerView rvScores = findViewById(R.id.rvScores);
        rvScores.setLayoutManager(new LinearLayoutManager(this));
        rvScores.setAdapter(new ScoreAdapter(scores));

        // Chart
        setupChart(scores);
    }

    private void setupChart(List<Score> scores) {
        BarChart chart = findViewById(R.id.chartScores);

        if (scores.isEmpty()) {
            chart.setNoDataText("No scores yet");
            chart.setNoDataTextColor(Color.WHITE);
            return;
        }

        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        for (int i = 0; i < scores.size(); i++) {
            Score score = scores.get(i);
            float percentage = score.maxMarks > 0 ? (score.marks / score.maxMarks * 100) : 0;
            entries.add(new BarEntry(i, percentage));
            labels.add(score.scoreType);
        }

        BarDataSet dataSet = new BarDataSet(entries, "Scores (%)");
        dataSet.setColors(
                Color.parseColor("#00C853"),
                Color.parseColor("#2196F3"),
                Color.parseColor("#FFC107"),
                Color.parseColor("#F44336")
        );
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(10f);

        BarData data = new BarData(dataSet);
        chart.setData(data);

        // Styling
        chart.getDescription().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setBackgroundColor(Color.TRANSPARENT);
        chart.getLegend().setTextColor(Color.WHITE);

        XAxis xAxis = chart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);

        chart.getAxisLeft().setTextColor(Color.WHITE);
        chart.getAxisLeft().setDrawGridLines(false);
        chart.getAxisRight().setEnabled(false);

        chart.animateY(1000);
        chart.invalidate();
    }
}
