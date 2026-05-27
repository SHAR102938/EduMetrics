package com.edumetrics.app.database.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "attendance",
        foreignKeys = {
                @ForeignKey(entity = Subject.class,
                        parentColumns = "id",
                        childColumns = "subjectId",
                        onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = User.class,
                        parentColumns = "id",
                        childColumns = "studentId",
                        onDelete = ForeignKey.CASCADE)
        },
        indices = {@Index("subjectId"), @Index("studentId"),
                @Index(value = {"studentId", "subjectId", "qrSessionId"}, unique = true)})
public class Attendance {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int subjectId;
    public int studentId;
    public String date;
    public String time;
    public int present; // 1 = present, 0 = absent
    public String qrSessionId;

    public Attendance() {}

    public Attendance(int subjectId, int studentId, String date, String time, int present, String qrSessionId) {
        this.subjectId = subjectId;
        this.studentId = studentId;
        this.date = date;
        this.time = time;
        this.present = present;
        this.qrSessionId = qrSessionId;
    }
}
