package com.edumetrics.app.database.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "subjects",
        foreignKeys = {
                @ForeignKey(entity = ClassEntity.class,
                        parentColumns = "id",
                        childColumns = "classId",
                        onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = User.class,
                        parentColumns = "id",
                        childColumns = "facultyId",
                        onDelete = ForeignKey.CASCADE)
        },
        indices = {@Index("classId"), @Index("facultyId")})
public class Subject {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;
    public int classId;
    public int facultyId;

    public Subject() {}

    public Subject(String name, int classId, int facultyId) {
        this.name = name;
        this.classId = classId;
        this.facultyId = facultyId;
    }
}
