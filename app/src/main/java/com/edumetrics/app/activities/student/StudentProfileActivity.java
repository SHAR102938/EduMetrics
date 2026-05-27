package com.edumetrics.app.activities.student;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.edumetrics.app.R;
import com.edumetrics.app.adapters.SubjectAdapter;
import com.edumetrics.app.analytics.ConsistencyScoreCalculator;
import com.edumetrics.app.analytics.DisciplineIndexCalculator;
import com.edumetrics.app.database.EduMetricsDatabase;
import com.edumetrics.app.database.entities.Subject;
import com.edumetrics.app.database.entities.User;
import com.edumetrics.app.utils.AnimationHelper;
import com.edumetrics.app.utils.PDFReportGenerator;
import com.edumetrics.app.utils.SessionManager;
import com.github.mikephil.charting.charts.RadarChart;
import com.github.mikephil.charting.data.RadarData;
import com.github.mikephil.charting.data.RadarDataSet;
import com.github.mikephil.charting.data.RadarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class StudentProfileActivity extends AppCompatActivity {

    private EduMetricsDatabase db;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_profile);

        db = EduMetricsDatabase.getInstance(this);
        sessionManager = new SessionManager(this);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnDownloadPDF).setOnClickListener(v -> generatePDF());

        loadProfile();
    }

    private void loadProfile() {
        int userId = sessionManager.getUserId();
        User user = db.userDao().getUserById(userId);
        if (user == null) return;

        ((TextView) findViewById(R.id.tvName)).setText(user.name);
        ((TextView) findViewById(R.id.tvEmail)).setText(user.email);

        // Attendance
        int totalPresent = db.attendanceDao().getTotalPresentCount(userId);
        int totalClasses = db.attendanceDao().getTotalAttendanceCount(userId);
        int attPct = totalClasses > 0 ? (totalPresent * 100 / totalClasses) : 0;
        ((TextView) findViewById(R.id.tvAttendance)).setText(attPct + "%");

        // Consistency
        ConsistencyScoreCalculator csCalc = new ConsistencyScoreCalculator(db);
        float cs = csCalc.calculate(userId);
        ((TextView) findViewById(R.id.tvConsistency)).setText(String.format("%.0f", cs));

        // Discipline
        DisciplineIndexCalculator diCalc = new DisciplineIndexCalculator(db);
        float di = diCalc.calculate(userId);
        ((TextView) findViewById(R.id.tvDiscipline)).setText(String.format("%.0f", di));

        // Radar Chart
        setupRadarChart(attPct, cs, di);

        // Subject-wise attendance
        List<Subject> subjects = db.subjectDao().getSubjectsByStudent(userId);
        RecyclerView rv = findViewById(R.id.rvSubjectAttendance);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(new SubjectAdapter(subjects, db, userId, null));
    }

    private void setupRadarChart(int attendance, float consistency, float discipline) {
        RadarChart chart = findViewById(R.id.radarChart);

        ArrayList<RadarEntry> entries = new ArrayList<>();
        entries.add(new RadarEntry(attendance));
        entries.add(new RadarEntry(consistency));
        entries.add(new RadarEntry(discipline));

        // Add task completion
        int completed = db.taskDao().getCompletedTaskCount(sessionManager.getUserId());
        int total = db.taskDao().getTotalTaskCount(sessionManager.getUserId());
        float taskPct = total > 0 ? (completed * 100f / total) : 0;
        entries.add(new RadarEntry(taskPct));

        RadarDataSet dataSet = new RadarDataSet(entries, "Performance");
        dataSet.setColor(Color.parseColor("#00C853"));
        dataSet.setFillColor(Color.parseColor("#4000C853"));
        dataSet.setDrawFilled(true);
        dataSet.setFillAlpha(100);
        dataSet.setLineWidth(2f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(10f);

        RadarData data = new RadarData(dataSet);
        chart.setData(data);

        String[] labels = {"Attendance", "Consistency", "Discipline", "Tasks"};
        chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chart.getXAxis().setTextColor(Color.WHITE);
        chart.getXAxis().setTextSize(10f);

        chart.getYAxis().setTextColor(Color.WHITE);
        chart.getYAxis().setAxisMinimum(0f);
        chart.getYAxis().setAxisMaximum(100f);

        chart.getDescription().setEnabled(false);
        chart.getLegend().setTextColor(Color.WHITE);
        chart.setBackgroundColor(Color.TRANSPARENT);
        chart.setWebColor(Color.GRAY);
        chart.setWebColorInner(Color.DKGRAY);
        chart.setWebAlpha(100);

        chart.animateXY(1000, 1000);
        chart.invalidate();
    }

    private void generatePDF() {
        try {
            PDFReportGenerator generator = new PDFReportGenerator(this);
            File file = generator.generateStudentReport(sessionManager.getUserId());
            if (file != null) {
                Toast.makeText(this, "Report saved: " + file.getName(), Toast.LENGTH_LONG).show();
                generator.sharePDF(file);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error generating report", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}
