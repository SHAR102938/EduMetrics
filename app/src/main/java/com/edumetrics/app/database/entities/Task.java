package com.edumetrics.app.database.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "tasks",
        foreignKeys = {
                @ForeignKey(entity = Subject.class,
                        parentColumns = "id",
                        childColumns = "subjectId",
                        onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = ClassEntity.class,
                        parentColumns = "id",
                        childColumns = "classId",
                        onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = User.class,
                        parentColumns = "id",
                        childColumns = "createdBy",
                        onDelete = ForeignKey.CASCADE)
        },
        indices = {@Index("subjectId"), @Index("classId"), @Index("createdBy")})
public class Task {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String title;
    public String description;
    public int subjectId;
    public int classId;
    public int createdBy;
    public String type; // "assignment", "practical", "personal"
    public String dueDate;
    public long createdAt;

    public Task() {
        this.createdAt = System.currentTimeMillis();
    }

    public Task(String title, String description, int subjectId, int classId,
                int createdBy, String type, String dueDate) {
        this.title = title;
        this.description = description;
        this.subjectId = subjectId;
        this.classId = classId;
        this.createdBy = createdBy;
        this.type = type;
        this.dueDate = dueDate;
        this.createdAt = System.currentTimeMillis();
    }
}
