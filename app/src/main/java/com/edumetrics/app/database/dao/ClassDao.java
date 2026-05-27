package com.edumetrics.app.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import com.edumetrics.app.database.entities.ClassEntity;

import java.util.List;

@Dao
public interface ClassDao {
    @Insert
    long insert(ClassEntity classEntity);

    @Update
    void update(ClassEntity classEntity);

    @Delete
    void delete(ClassEntity classEntity);

    @Query("SELECT * FROM classes")
    List<ClassEntity> getAllClasses();

    @Query("SELECT * FROM classes WHERE facultyId = :facultyId")
    List<ClassEntity> getClassesByFaculty(int facultyId);

    @Query("SELECT * FROM classes WHERE classCode = :code LIMIT 1")
    ClassEntity getClassByCode(String code);

    @Query("SELECT * FROM classes WHERE id = :id LIMIT 1")
    ClassEntity getClassById(int id);

    @Query("SELECT c.* FROM classes c INNER JOIN students_class sc ON c.id = sc.classId WHERE sc.studentId = :studentId")
    List<ClassEntity> getClassesByStudent(int studentId);

    @Query("SELECT COUNT(*) FROM students_class WHERE classId = :classId")
    int getStudentCountInClass(int classId);
}
