package com.edumetrics.app.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import com.edumetrics.app.database.entities.Subject;

import java.util.List;

@Dao
public interface SubjectDao {
    @Insert
    long insert(Subject subject);

    @Update
    void update(Subject subject);

    @Delete
    void delete(Subject subject);

    @Query("SELECT * FROM subjects")
    List<Subject> getAllSubjects();

    @Query("SELECT * FROM subjects WHERE classId = :classId")
    List<Subject> getSubjectsByClass(int classId);

    @Query("SELECT * FROM subjects WHERE facultyId = :facultyId")
    List<Subject> getSubjectsByFaculty(int facultyId);

    @Query("SELECT * FROM subjects WHERE id = :id LIMIT 1")
    Subject getSubjectById(int id);

    @Query("SELECT s.* FROM subjects s INNER JOIN classes c ON s.classId = c.id " +
            "INNER JOIN students_class sc ON c.id = sc.classId WHERE sc.studentId = :studentId")
    List<Subject> getSubjectsByStudent(int studentId);
}
