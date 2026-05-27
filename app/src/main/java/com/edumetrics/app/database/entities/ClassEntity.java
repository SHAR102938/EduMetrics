package com.edumetrics.app.database.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "classes",
        foreignKeys = @ForeignKey(
                entity = User.class,
                parentColumns = "id",
                childColumns = "facultyId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("facultyId"), @Index(value = "classCode", unique = true)})
public class ClassEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;
    public String classCode;
    public int facultyId;
    public long createdAt;

    public ClassEntity() {
        this.createdAt = System.currentTimeMillis();
    }

    public ClassEntity(String name, String classCode, int facultyId) {
        this.name = name;
        this.classCode = classCode;
        this.facultyId = facultyId;
        this.createdAt = System.currentTimeMillis();
    }
}
