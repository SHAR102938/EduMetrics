package com.edumetrics.app.activities.faculty;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.edumetrics.app.R;
import com.edumetrics.app.database.EduMetricsDatabase;
import com.edumetrics.app.database.entities.Score;
import com.edumetrics.app.database.entities.Subject;
import com.edumetrics.app.database.entities.User;
import com.edumetrics.app.services.FirebaseService;
import com.edumetrics.app.utils.AnimationHelper;
import com.edumetrics.app.utils.DateUtils;
import com.edumetrics.app.utils.SessionManager;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class AddMarksActivity extends AppCompatActivity {

    private Spinner spinnerSubject, spinnerStudent, spinnerType;
    private TextInputEditText etMarks, etMaxMarks, etDescription;
    private EduMetricsDatabase db;
    private SessionManager sessionManager;
    private FirebaseService firebaseService;
    private List<Subject> subjects = new ArrayList<>();
    private List<User> students = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_marks);

        db = EduMetricsDatabase.getInstance(this);
        sessionManager = new SessionManager(this);
        firebaseService = new FirebaseService(this);

        spinnerSubject = findViewById(R.id.spinnerSubject);
        spinnerStudent = findViewById(R.id.spinnerStudent);
        spinnerType = findViewById(R.id.spinnerType);
        etMarks = findViewById(R.id.etMarks);
        etMaxMarks = findViewById(R.id.etMaxMarks);
        etDescription = findViewById(R.id.etDescription);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSave).setOnClickListener(v -> {
            AnimationHelper.buttonPress(v);
            saveMarks();
        });

        loadData();
    }

    private void loadData() {
        // Subjects
        subjects = db.subjectDao().getSubjectsByFaculty(sessionManager.getUserId());
        List<String> subjectNames = new ArrayList<>();
        for (Subject s : subjects) subjectNames.add(s.name);
        if (subjectNames.isEmpty()) subjectNames.add("No subjects");
        spinnerSubject.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, subjectNames));

        // Students
        int classId = sessionManager.getCurrentClassId();
        if (classId > 0) {
            students = db.studentClassDao().getStudentsInClass(classId);
        }
        List<String> studentNames = new ArrayList<>();
        for (User u : students) studentNames.add(u.name);
        if (studentNames.isEmpty()) studentNames.add("No students");

        // Pre-select student if passed via intent
        int preSelectedStudentId = getIntent().getIntExtra("student_id", -1);
        int selectedIdx = 0;
        if (preSelectedStudentId > 0) {
            for (int i = 0; i < students.size(); i++) {
                if (students.get(i).id == preSelectedStudentId) {
                    selectedIdx = i;
                    break;
                }
            }
        }
        spinnerStudent.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, studentNames));
        spinnerStudent.setSelection(selectedIdx);

        // Score types
        String[] types = {"assignment", "practical", "exam"};
        spinnerType.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, types));
    }

    private void saveMarks() {
        if (subjects.isEmpty() || students.isEmpty()) {
            Toast.makeText(this, "Missing data", Toast.LENGTH_SHORT).show();
            return;
        }

        String marksStr = etMarks.getText() != null ? etMarks.getText().toString().trim() : "";
        String maxStr = etMaxMarks.getText() != null ? etMaxMarks.getText().toString().trim() : "";
        String description = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";

        if (marksStr.isEmpty() || maxStr.isEmpty()) {
            Toast.makeText(this, "Enter marks and max marks", Toast.LENGTH_SHORT).show();
            return;
        }

        float marks = Float.parseFloat(marksStr);
        float maxMarks = Float.parseFloat(maxStr);

        if (marks > maxMarks) {
            Toast.makeText(this, "Marks cannot exceed max marks", Toast.LENGTH_SHORT).show();
            return;
        }

        Subject subject = subjects.get(spinnerSubject.getSelectedItemPosition());
        User student = students.get(spinnerStudent.getSelectedItemPosition());
        String type = (String) spinnerType.getSelectedItem();

        Score score = new Score(student.id, subject.id, type, marks, maxMarks,
                DateUtils.getCurrentDate(), description);
        
        new Thread(() -> {
            int scoreId = Math.abs((student.id + "_" + subject.id + "_" + System.currentTimeMillis()).hashCode());
            if (scoreId == 0) scoreId = 1;
            score.id = scoreId;
            db.scoreDao().insert(score);
            
            // Sync to Firebase
            firebaseService.pushEntity("scores", String.valueOf(scoreId), score);
            
            runOnUiThread(() -> {
                Toast.makeText(this, "Marks saved and synced!", Toast.LENGTH_SHORT).show();
                etMarks.setText("");
                etMaxMarks.setText("");
                etDescription.setText("");
            });
        }).start();
    }
}
