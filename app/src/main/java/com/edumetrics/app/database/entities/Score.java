package com.edumetrics.app.database.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "scores",
        foreignKeys = {
                @ForeignKey(entity = User.class,
                        parentColumns = "id",
                        childColumns = "studentId",
                        onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = Subject.class,
                        parentColumns = "id",
                        childColumns = "subjectId",
                        onDelete = ForeignKey.CASCADE)
        },
        indices = {@Index("studentId"), @Index("subjectId")})
public class Score {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int studentId;
    public int subjectId;
    public String scoreType; // "assignment", "practical", "exam"
    public float marks;
    public float maxMarks;
    public String date;
    public String description;

    public Score() {}

    public Score(int studentId, int subjectId, String scoreType, float marks, float maxMarks, String date, String description) {
        this.studentId = studentId;
        this.subjectId = subjectId;
        this.scoreType = scoreType;
        this.marks = marks;
        this.maxMarks = maxMarks;
        this.date = date;
        this.description = description;
    }
}
