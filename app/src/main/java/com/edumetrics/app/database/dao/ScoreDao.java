package com.edumetrics.app.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import com.edumetrics.app.database.entities.Score;

import java.util.List;

@Dao
public interface ScoreDao {
    @Insert
    long insert(Score score);

    @Update
    void update(Score score);

    @Delete
    void delete(Score score);

    @Query("SELECT * FROM scores WHERE id = :id LIMIT 1")
    Score getScoreById(int id);

    @Query("SELECT * FROM scores WHERE studentId = :studentId AND subjectId = :subjectId")
    List<Score> getScoresByStudentAndSubject(int studentId, int subjectId);

    @Query("SELECT * FROM scores WHERE studentId = :studentId")
    List<Score> getScoresByStudent(int studentId);

    @Query("SELECT * FROM scores WHERE subjectId = :subjectId")
    List<Score> getScoresBySubject(int subjectId);

    @Query("SELECT AVG(marks / maxMarks * 100) FROM scores WHERE studentId = :studentId AND subjectId = :subjectId")
    float getAverageScore(int studentId, int subjectId);

    @Query("SELECT AVG(marks / maxMarks * 100) FROM scores WHERE studentId = :studentId")
    float getOverallAverageScore(int studentId);
}
