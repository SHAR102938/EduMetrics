package com.edumetrics.app.activities.faculty;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.edumetrics.app.R;
import com.edumetrics.app.adapters.TaskAdapter;
import com.edumetrics.app.database.EduMetricsDatabase;
import com.edumetrics.app.database.entities.*;
import com.edumetrics.app.services.FirebaseService;
import com.edumetrics.app.utils.AnimationHelper;
import com.edumetrics.app.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class TaskManagementActivity extends AppCompatActivity {

    private RecyclerView rvTasks;
    private EduMetricsDatabase db;
    private SessionManager sessionManager;
    private FirebaseService firebaseService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_management);

        db = EduMetricsDatabase.getInstance(this);
        sessionManager = new SessionManager(this);
        firebaseService = new FirebaseService(this);

        rvTasks = findViewById(R.id.rvTasks);
        rvTasks.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.fabAddTask).setOnClickListener(v -> {
            AnimationHelper.buttonPress(v);
            showAddTaskDialog();
        });

        loadTasks();
    }

    private void loadTasks() {
        List<Task> tasks = db.taskDao().getTasksByFaculty(sessionManager.getUserId());
        TaskAdapter adapter = new TaskAdapter(tasks, db, 0, null);
        rvTasks.setAdapter(adapter);
    }

    private void showAddTaskDialog() {
        List<Subject> subjects = db.subjectDao().getSubjectsByFaculty(sessionManager.getUserId());
        if (subjects.isEmpty()) {
            Toast.makeText(this, "Create a subject first!", Toast.LENGTH_SHORT).show();
            return;
        }

        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_task, null);
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.Theme_EduMetrics)
                .setView(dialogView)
                .create();

        EditText etTitle = dialogView.findViewById(R.id.etTaskTitle);
        EditText etDesc = dialogView.findViewById(R.id.etTaskDesc);
        Spinner spSubject = dialogView.findViewById(R.id.spinnerSubject);
        Spinner spType = dialogView.findViewById(R.id.spinnerType);
        com.google.android.material.button.MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancelTask);
        com.google.android.material.button.MaterialButton btnConfirm = dialogView.findViewById(R.id.btnConfirmTask);

        // --- Styled Subject Spinner ---
        List<String> subjectNames = new ArrayList<>();
        for (Subject s : subjects) subjectNames.add(s.name);
        ArrayAdapter<String> subAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, subjectNames) {
            @Override
            public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                android.view.View v = super.getView(position, convertView, parent);
                ((android.widget.TextView) v).setTextColor(getColor(R.color.text_primary));
                return v;
            }
            @Override
            public android.view.View getDropDownView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                android.view.View v = super.getDropDownView(position, convertView, parent);
                ((android.widget.TextView) v).setTextColor(getColor(R.color.text_primary));
                v.setBackgroundColor(getColor(R.color.surface));
                return v;
            }
        };
        subAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSubject.setAdapter(subAdapter);

        // --- Styled Type Spinner ---
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, new String[]{"assignment", "practical"}) {
            @Override
            public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                android.view.View v = super.getView(position, convertView, parent);
                ((android.widget.TextView) v).setTextColor(getColor(R.color.text_primary));
                return v;
            }
            @Override
            public android.view.View getDropDownView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                android.view.View v = super.getDropDownView(position, convertView, parent);
                ((android.widget.TextView) v).setTextColor(getColor(R.color.text_primary));
                v.setBackgroundColor(getColor(R.color.surface));
                return v;
            }
        };
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spType.setAdapter(typeAdapter);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            if (title.isEmpty()) {
                Toast.makeText(this, "Title is required", Toast.LENGTH_SHORT).show();
                return;
            }

            Subject selectedSubject = subjects.get(spSubject.getSelectedItemPosition());
            String type = (String) spType.getSelectedItem();
            String desc = etDesc.getText().toString().trim();

            new Thread(() -> {
                int taskId = Math.abs((title + "_" + selectedSubject.id + "_" + System.currentTimeMillis()).hashCode());
                if (taskId == 0) taskId = 1;

                Task task = new Task(title, desc, selectedSubject.id, selectedSubject.classId,
                        sessionManager.getUserId(), type, "");
                task.id = taskId;
                db.taskDao().insertTask(task);

                // Sync task to cloud
                firebaseService.pushEntity("tasks", String.valueOf(taskId), task);

                // Create TaskStatus for all students in the class locally
                List<User> students = db.studentClassDao().getStudentsInClass(selectedSubject.classId);
                for (User student : students) {
                    TaskStatus status = new TaskStatus((int) taskId, student.id);
                    db.taskDao().insertTaskStatus(status);
                    // Sync individual task status if needed, but usually better on student sync
                }

                runOnUiThread(() -> {
                    Toast.makeText(this, "Assignment created and synced!", Toast.LENGTH_SHORT).show();
                    loadTasks();
                    dialog.dismiss();
                });
            }).start();
        });

        dialog.show();
    }
}
