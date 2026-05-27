package com.edumetrics.app.activities.student;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.edumetrics.app.R;
import com.edumetrics.app.adapters.SubjectAdapter;
import com.edumetrics.app.database.EduMetricsDatabase;
import com.edumetrics.app.database.entities.Subject;
import com.edumetrics.app.utils.SessionManager;

import java.util.List;

public class SubjectListActivity extends AppCompatActivity {

    private RecyclerView rvSubjects;
    private EduMetricsDatabase db;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subject_list);

        db = EduMetricsDatabase.getInstance(this);
        sessionManager = new SessionManager(this);

        rvSubjects = findViewById(R.id.rvSubjects);
        rvSubjects.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        loadSubjects();
        syncSubjectsFromCloud();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSubjects();
        syncScoresFromCloud();
    }

    private void syncScoresFromCloud() {
        int userId = sessionManager.getUserId();
        com.google.firebase.database.FirebaseDatabase.getInstance().getReference("scores")
            .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(@androidx.annotation.NonNull com.google.firebase.database.DataSnapshot snapshot) {
                    new Thread(() -> {
                        for (com.google.firebase.database.DataSnapshot ds : snapshot.getChildren()) {
                            try {
                                com.edumetrics.app.database.entities.Score score = ds.getValue(com.edumetrics.app.database.entities.Score.class);
                                if (score != null && score.studentId == userId) {
                                    if (db.scoreDao().getScoreById(score.id) == null) {
                                        db.scoreDao().insert(score);
                                    }
                                }
                            } catch (Exception ignored) {}
                        }
                        runOnUiThread(() -> loadSubjects());
                    }).start();
                }
                @Override
                public void onCancelled(@androidx.annotation.NonNull com.google.firebase.database.DatabaseError error) {}
            });
    }

    private void syncSubjectsFromCloud() {
        com.google.firebase.database.FirebaseDatabase.getInstance().getReference("subjects")
            .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(@androidx.annotation.NonNull com.google.firebase.database.DataSnapshot snapshot) {
                    new Thread(() -> {
                        for (com.google.firebase.database.DataSnapshot ds : snapshot.getChildren()) {
                            Subject s = ds.getValue(Subject.class);
                            if (s != null) {
                                if (db.subjectDao().getSubjectById(s.id) == null) {
                                    try {
                                        db.subjectDao().insert(s);
                                    } catch(Exception ignored){}
                                } else {
                                    try {
                                        db.subjectDao().update(s);
                                    } catch(Exception ignored){}
                                }
                            }
                        }
                        runOnUiThread(() -> loadSubjects());
                    }).start();
                }
                @Override
                public void onCancelled(@androidx.annotation.NonNull com.google.firebase.database.DatabaseError error) {}
            });
    }

    private void loadSubjects() {
        int userId = sessionManager.getUserId();
        List<Subject> subjects = db.subjectDao().getSubjectsByStudent(userId);

        SubjectAdapter adapter = new SubjectAdapter(subjects, db, userId, subject -> {
            Intent intent = new Intent(this, SubjectDetailActivity.class);
            intent.putExtra("subject_id", subject.id);
            startActivity(intent);
        });
        rvSubjects.setAdapter(adapter);
    }
}
