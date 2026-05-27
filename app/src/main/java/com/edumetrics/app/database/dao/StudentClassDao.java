package com.edumetrics.app.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Delete;

import com.edumetrics.app.database.entities.StudentClass;
import com.edumetrics.app.database.entities.User;

import java.util.List;

@Dao
public interface StudentClassDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(StudentClass studentClass);

    @Delete
    void delete(StudentClass studentClass);

    @Query("SELECT u.* FROM users u INNER JOIN students_class sc ON u.id = sc.studentId WHERE sc.classId = :classId")
    List<User> getStudentsInClass(int classId);

    @Query("SELECT * FROM students_class WHERE studentId = :studentId AND classId = :classId LIMIT 1")
    StudentClass getStudentClass(int studentId, int classId);

    @Query("SELECT COUNT(*) FROM students_class WHERE studentId = :studentId AND classId = :classId")
    int isStudentInClass(int studentId, int classId);
}
