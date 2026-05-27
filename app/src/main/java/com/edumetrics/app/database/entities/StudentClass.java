package com.edumetrics.app.database.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "students_class",
        foreignKeys = {
                @ForeignKey(entity = User.class,
                        parentColumns = "id",
                        childColumns = "studentId",
                        onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = ClassEntity.class,
                        parentColumns = "id",
                        childColumns = "classId",
                        onDelete = ForeignKey.CASCADE)
        },
        indices = {@Index("studentId"), @Index("classId"),
                @Index(value = {"studentId", "classId"}, unique = true)})
public class StudentClass {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int studentId;
    public int classId;
    public long joinedAt;

    public StudentClass() {
        this.joinedAt = System.currentTimeMillis();
    }

    public StudentClass(int studentId, int classId) {
        this.studentId = studentId;
        this.classId = classId;
        this.joinedAt = System.currentTimeMillis();
    }
}
