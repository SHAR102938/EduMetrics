package com.edumetrics.app.activities.student;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.edumetrics.app.R;
import com.edumetrics.app.adapters.TaskAdapter;
import com.edumetrics.app.database.EduMetricsDatabase;
import com.edumetrics.app.database.entities.Task;
import com.edumetrics.app.database.entities.TaskStatus;
import com.edumetrics.app.utils.SessionManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class TaskListActivity extends AppCompatActivity {

    private RecyclerView rvTasks;
    private EduMetricsDatabase db;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_list);

        db = EduMetricsDatabase.getInstance(this);
        sessionManager = new SessionManager(this);

        rvTasks = findViewById(R.id.rvTasks);
        rvTasks.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.fabAddTask).setOnClickListener(v -> showAddTaskDialog());

        loadTasks();
        syncTasksFromCloud();
    }

    private void syncTasksFromCloud() {
        com.google.firebase.database.FirebaseDatabase.getInstance().getReference("tasks")
                .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(
                            @androidx.annotation.NonNull com.google.firebase.database.DataSnapshot snapshot) {
                        new Thread(() -> {
                            for (com.google.firebase.database.DataSnapshot ds : snapshot.getChildren()) {
                                Task t = ds.getValue(Task.class);
                                if (t != null && !t.type.equals("personal")) {
                                    if (db.taskDao().getTaskById(t.id) == null) {
                                        try {
                                            db.taskDao().insertTask(t);
                                        } catch (Exception ignored) {
                                        }
                                    } else {
                                        try {
                                            db.taskDao().updateTask(t);
                                        } catch (Exception ignored) {
                                        }
                                    }
                                    try {
                                        if (db.taskDao().getTaskStatus(t.id, sessionManager.getUserId()) == null) {
                                            db.taskDao().insertTaskStatus(new TaskStatus(t.id, sessionManager.getUserId()));
                                        }
                                    } catch (Exception ignored) {}
                                }
                            }
                            runOnUiThread(() -> loadTasks());
                        }).start();
                    }

                    @Override
                    public void onCancelled(
                            @androidx.annotation.NonNull com.google.firebase.database.DatabaseError error) {
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTasks();
    }

    private void loadTasks() {
        int userId = sessionManager.getUserId();

        // Get faculty tasks + personal tasks
        List<Task> allTasks = new ArrayList<>();
        List<Task> assignedTasks = db.taskDao().getTasksForStudent(userId);
        List<Task> personalTasks = db.taskDao().getPersonalTasks(userId);
        allTasks.addAll(assignedTasks);
        allTasks.addAll(personalTasks);

        TaskAdapter adapter = new TaskAdapter(allTasks, db, userId, (task, completed) -> {
            TaskStatus status = db.taskDao().getTaskStatus(task.id, userId);
            if (status != null) {
                status.completed = completed ? 1 : 0;
                status.completedAt = completed ? System.currentTimeMillis() : 0;
                db.taskDao().updateTaskStatus(status);
                try {
                    com.google.firebase.database.FirebaseDatabase.getInstance().getReference("task_status")
                        .child(String.valueOf(task.id))
                        .child(String.valueOf(userId))
                        .setValue(status);
                } catch (Exception ignored) {}
            }
        });
        rvTasks.setAdapter(adapter);
    }

    private void showAddTaskDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.Theme_EduMetrics);
        builder.setTitle("Add Personal Task");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 32, 48, 16);

        EditText etTitle = new EditText(this);
        etTitle.setHint("Task Title");
        etTitle.setTextColor(getColor(R.color.text_primary));
        etTitle.setHintTextColor(getColor(R.color.text_hint));
        layout.addView(etTitle);

        EditText etDesc = new EditText(this);
        etDesc.setHint("Description (optional)");
        etDesc.setTextColor(getColor(R.color.text_primary));
        etDesc.setHintTextColor(getColor(R.color.text_hint));
        layout.addView(etDesc);

        builder.setView(layout);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String title = etTitle.getText().toString().trim();
            if (title.isEmpty()) {
                Toast.makeText(this, "Title is required", Toast.LENGTH_SHORT).show();
                return;
            }

            int userId = sessionManager.getUserId();
            Task task = new Task(
                    title,
                    etDesc.getText().toString().trim(),
                    0, 0, userId, "personal", "");
            long taskId = db.taskDao().insertTask(task);

            // Create status for self
            TaskStatus status = new TaskStatus((int) taskId, userId);
            db.taskDao().insertTaskStatus(status);

            Toast.makeText(this, "Task added!", Toast.LENGTH_SHORT).show();
            loadTasks();
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}
