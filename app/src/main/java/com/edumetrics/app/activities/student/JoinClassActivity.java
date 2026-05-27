package com.edumetrics.app.activities.student;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.edumetrics.app.R;
import com.edumetrics.app.database.EduMetricsDatabase;
import com.edumetrics.app.database.entities.ClassEntity;
import com.edumetrics.app.database.entities.StudentClass;
import com.edumetrics.app.database.entities.Task;
import com.edumetrics.app.database.entities.TaskStatus;
import com.edumetrics.app.utils.AnimationHelper;
import com.edumetrics.app.utils.SessionManager;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class JoinClassActivity extends AppCompatActivity {

    private TextInputEditText etCode;
    private EduMetricsDatabase db;
    private SessionManager sessionManager;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_class);

        db = EduMetricsDatabase.getInstance(this);
        sessionManager = new SessionManager(this);
        mDatabase = FirebaseDatabase.getInstance().getReference();

        etCode = findViewById(R.id.etCode);

        findViewById(R.id.btnJoin).setOnClickListener(v -> {
            AnimationHelper.buttonPress(v);
            joinClass();
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void joinClass() {
        String code = etCode.getText() != null ? etCode.getText().toString().trim().toUpperCase() : "";
        if (code.isEmpty()) {
            etCode.setError("Please enter a class code");
            return;
        }

        // 1. Check Local DB first
        new Thread(() -> {
            ClassEntity localClass = db.classDao().getClassByCode(code);
            runOnUiThread(() -> {
                if (localClass != null) {
                    processJoin(localClass);
                } else {
                    // 2. Fallback: Search in Firebase Realtime Database
                    searchInFirebase(code);
                }
            });
        }).start();
    }

    private void searchInFirebase(String code) {
        Toast.makeText(this, "Searching for class...", Toast.LENGTH_SHORT).show();
        
        mDatabase.child("classes").child(code).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    ClassEntity cloudClass = snapshot.getValue(ClassEntity.class);
                    if (cloudClass != null) {
                        // Save class to local DB first
                        new Thread(() -> {
                            db.classDao().insert(cloudClass);
                            // Get the one with local ID
                            ClassEntity savedClass = db.classDao().getClassByCode(code);
                            runOnUiThread(() -> processJoin(savedClass));
                        }).start();
                    }
                } else {
                    etCode.setError("Invalid class code or class not found");
                    Toast.makeText(JoinClassActivity.this, "Invalid class code", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(JoinClassActivity.this, "Connection error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("JoinClass", "Error fetching class", error.toException());
            }
        });
    }

    private void processJoin(ClassEntity classEntity) {
        if (classEntity == null) return;
        int studentId = sessionManager.getUserId();

        new Thread(() -> {
            // Check if already joined
            if (db.studentClassDao().isStudentInClass(studentId, classEntity.id) > 0) {
                runOnUiThread(() -> Toast.makeText(this, "You are already in this class", Toast.LENGTH_SHORT).show());
                return;
            }

            // Check max limits for class (20 students)
            int currentStudentCount = db.classDao().getStudentCountInClass(classEntity.id);
            if (currentStudentCount >= 20) {
                runOnUiThread(() -> Toast.makeText(this, "Class is full. Maximum limit is 20 students.", Toast.LENGTH_SHORT).show());
                return;
            }

            // Join class locally
            StudentClass sc = new StudentClass(studentId, classEntity.id);
            db.studentClassDao().insert(sc);

            // Fetch User info and push to Firebase
            try {
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("studentId", studentId);
                map.put("name", sessionManager.getUserName());
                map.put("email", sessionManager.getUserEmail());
                map.put("role", "student");
                FirebaseDatabase.getInstance().getReference()
                        .child("class_students")
                        .child(String.valueOf(classEntity.id))
                        .child(String.valueOf(studentId))
                        .setValue(map);
            } catch (Exception e) {}

            sessionManager.setCurrentClassId(classEntity.id);

            // Fetch and create task statuses
            List<Task> tasks = db.taskDao().getTasksByClass(classEntity.id);
            for (Task task : tasks) {
                TaskStatus existingStatus = db.taskDao().getTaskStatus(task.id, studentId);
                if (existingStatus == null) {
                    TaskStatus status = new TaskStatus(task.id, studentId);
                    db.taskDao().insertTaskStatus(status);
                }
            }

            runOnUiThread(() -> {
                Toast.makeText(this, "Joined \"" + classEntity.name + "\" successfully!", Toast.LENGTH_SHORT).show();
                finish();
            });
        }).start();
    }
}
