package com.edumetrics.app.activities.faculty;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.edumetrics.app.R;
import com.edumetrics.app.adapters.StudentAdapter;
import com.edumetrics.app.database.EduMetricsDatabase;
import com.edumetrics.app.database.entities.User;
import com.edumetrics.app.utils.SessionManager;

import java.util.List;

public class StudentListActivity extends AppCompatActivity {

    private RecyclerView rvStudents;
    private EduMetricsDatabase db;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_list);

        db = EduMetricsDatabase.getInstance(this);
        sessionManager = new SessionManager(this);

        rvStudents = findViewById(R.id.rvStudents);
        rvStudents.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        loadStudents();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStudents();
        syncStudentsFromCloud();
        syncAttendanceFromCloud();
        syncTaskStatusFromCloud();
    }

    private void syncAttendanceFromCloud() {
        com.google.firebase.database.FirebaseDatabase.getInstance().getReference("attendance")
            .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(@androidx.annotation.NonNull com.google.firebase.database.DataSnapshot snapshot) {
                    new Thread(() -> {
                        for (com.google.firebase.database.DataSnapshot ds : snapshot.getChildren()) {
                            try {
                                com.edumetrics.app.database.entities.Attendance a = ds.getValue(com.edumetrics.app.database.entities.Attendance.class);
                                if (a != null && db.attendanceDao().isAlreadyMarked(a.studentId, a.subjectId, a.qrSessionId) == 0) {
                                    db.attendanceDao().insert(a);
                                }
                            } catch (Exception ignored) {}
                        }
                        runOnUiThread(() -> loadStudents());
                    }).start();
                }
                @Override
                public void onCancelled(@androidx.annotation.NonNull com.google.firebase.database.DatabaseError error) {}
            });
    }

    private void syncTaskStatusFromCloud() {
        com.google.firebase.database.FirebaseDatabase.getInstance().getReference("task_status")
            .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(@androidx.annotation.NonNull com.google.firebase.database.DataSnapshot snapshot) {
                    new Thread(() -> {
                        for (com.google.firebase.database.DataSnapshot taskDs : snapshot.getChildren()) {
                            try {
                                int taskId = Integer.parseInt(taskDs.getKey());
                                for (com.google.firebase.database.DataSnapshot studentDs : taskDs.getChildren()) {
                                    int studentId = Integer.parseInt(studentDs.getKey());
                                    com.edumetrics.app.database.entities.TaskStatus ts = studentDs.getValue(com.edumetrics.app.database.entities.TaskStatus.class);
                                    if (ts != null) {
                                        if (db.taskDao().getTaskStatus(taskId, studentId) == null) {
                                            db.taskDao().insertTaskStatus(ts);
                                        } else {
                                            db.taskDao().updateTaskStatus(ts);
                                        }
                                    }
                                }
                            } catch (Exception ignored) {}
                        }
                        runOnUiThread(() -> loadStudents());
                    }).start();
                }
                @Override
                public void onCancelled(@androidx.annotation.NonNull com.google.firebase.database.DatabaseError error) {}
            });
    }

    private void syncStudentsFromCloud() {
        int classId = sessionManager.getCurrentClassId();
        if (classId <= 0) return;

        com.google.firebase.database.FirebaseDatabase.getInstance().getReference("class_students")
            .child(String.valueOf(classId))
            .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(@androidx.annotation.NonNull com.google.firebase.database.DataSnapshot snapshot) {
                    new Thread(() -> {
                        for (com.google.firebase.database.DataSnapshot ds : snapshot.getChildren()) {
                            try {
                                Integer sId = ds.child("studentId").getValue(Integer.class);
                                if (sId == null) continue;
                                int studentId = sId;
                                String name = ds.child("name").getValue(String.class);
                                String email = ds.child("email").getValue(String.class);
                                String role = ds.child("role").getValue(String.class);
                                
                                User student = db.userDao().getUserById(studentId);
                                if (student == null) {
                                    student = new User(name != null ? name : "Student", email != null ? email : "", "", role != null ? role : "student");
                                    student.id = studentId; 
                                    try {
                                        db.userDao().insert(student);
                                    } catch(Exception ignored){}
                                }
                                
                                if (db.studentClassDao().isStudentInClass(studentId, classId) == 0) {
                                    db.studentClassDao().insert(new com.edumetrics.app.database.entities.StudentClass(studentId, classId));
                                }
                            } catch (Exception ignored) {}
                        }
                        runOnUiThread(() -> loadStudents());
                    }).start();
                }
                @Override
                public void onCancelled(@androidx.annotation.NonNull com.google.firebase.database.DatabaseError error) {}
            });
    }

    private void loadStudents() {
        int classId = sessionManager.getCurrentClassId();
        if (classId <= 0) return;

        List<User> students = db.studentClassDao().getStudentsInClass(classId);
        StudentAdapter adapter = new StudentAdapter(students, db, student -> {
            // Open add marks for selected student
            Intent intent = new Intent(this, AddMarksActivity.class);
            intent.putExtra("student_id", student.id);
            startActivity(intent);
        });
        rvStudents.setAdapter(adapter);
    }
}
