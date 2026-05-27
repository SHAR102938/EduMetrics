package com.edumetrics.app.activities.faculty;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.edumetrics.app.R;
import com.edumetrics.app.database.EduMetricsDatabase;
import com.edumetrics.app.database.entities.User;
import com.edumetrics.app.utils.PDFReportGenerator;
import com.edumetrics.app.utils.SessionManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ReportActivity extends AppCompatActivity {

    private Spinner spinnerStudent;
    private ProgressBar progressBar;
    private EduMetricsDatabase db;
    private SessionManager sessionManager;
    private List<User> students = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        db = EduMetricsDatabase.getInstance(this);
        sessionManager = new SessionManager(this);

        spinnerStudent = findViewById(R.id.spinnerStudent);
        progressBar = findViewById(R.id.progressBar);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnClassReport).setOnClickListener(v -> generateClassReport());
        findViewById(R.id.btnStudentReport).setOnClickListener(v -> generateStudentReport());

        loadStudents();
    }

    private void loadStudents() {
        int classId = sessionManager.getCurrentClassId();
        if (classId > 0) {
            students = db.studentClassDao().getStudentsInClass(classId);
        }

        List<String> names = new ArrayList<>();
        for (User u : students) names.add(u.name);
        if (names.isEmpty()) names.add("No students");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStudent.setAdapter(adapter);
    }

    private void generateClassReport() {
        int classId = sessionManager.getCurrentClassId();
        if (classId <= 0) {
            Toast.makeText(this, "No class selected", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        try {
            PDFReportGenerator generator = new PDFReportGenerator(this);
            File file = generator.generateClassReport(classId);
            progressBar.setVisibility(View.GONE);

            if (file != null) {
                Toast.makeText(this, "Report saved: " + file.getName(), Toast.LENGTH_LONG).show();
                generator.sharePDF(file);
            }
        } catch (Exception e) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Error generating report", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void generateStudentReport() {
        if (students.isEmpty()) {
            Toast.makeText(this, "No students available", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedIdx = spinnerStudent.getSelectedItemPosition();
        User student = students.get(selectedIdx);

        progressBar.setVisibility(View.VISIBLE);

        try {
            PDFReportGenerator generator = new PDFReportGenerator(this);
            File file = generator.generateStudentReport(student.id);
            progressBar.setVisibility(View.GONE);

            if (file != null) {
                Toast.makeText(this, "Report saved: " + file.getName(), Toast.LENGTH_LONG).show();
                generator.sharePDF(file);
            }
        } catch (Exception e) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Error generating report", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}
