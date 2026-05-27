package com.edumetrics.app.database.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "task_status",
        foreignKeys = {
                @ForeignKey(entity = Task.class,
                        parentColumns = "id",
                        childColumns = "taskId",
                        onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = User.class,
                        parentColumns = "id",
                        childColumns = "studentId",
                        onDelete = ForeignKey.CASCADE)
        },
        indices = {@Index("taskId"), @Index("studentId"),
                @Index(value = {"taskId", "studentId"}, unique = true)})
public class TaskStatus {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int taskId;
    public int studentId;
    public int completed; // 1 = completed, 0 = pending
    public long completedAt;
    public String remarks;

    public TaskStatus() {}

    public TaskStatus(int taskId, int studentId) {
        this.taskId = taskId;
        this.studentId = studentId;
        this.completed = 0;
    }
}
