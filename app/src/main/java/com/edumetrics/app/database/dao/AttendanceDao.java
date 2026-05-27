package com.edumetrics.app.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.edumetrics.app.database.entities.Attendance;

import java.util.List;

@Dao
public interface AttendanceDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(Attendance attendance);

    @Query("SELECT * FROM attendance")
    List<Attendance> getAllAttendance();

    @Query("SELECT * FROM attendance WHERE subjectId = :subjectId AND studentId = :studentId")
    List<Attendance> getAttendanceByStudentAndSubject(int subjectId, int studentId);

    @Query("SELECT * FROM attendance WHERE subjectId = :subjectId AND date = :date")
    List<Attendance> getAttendanceBySubjectAndDate(int subjectId, String date);

    @Query("SELECT * FROM attendance WHERE studentId = :studentId")
    List<Attendance> getAttendanceByStudent(int studentId);

    @Query("SELECT * FROM attendance WHERE subjectId = :subjectId")
    List<Attendance> getAttendanceBySubject(int subjectId);

    @Query("SELECT COUNT(*) FROM attendance WHERE studentId = :studentId AND subjectId = :subjectId AND present = 1")
    int getPresentCount(int studentId, int subjectId);

    @Query("SELECT COUNT(*) FROM attendance WHERE studentId = :studentId AND subjectId = :subjectId")
    int getTotalCount(int studentId, int subjectId);

    @Query("SELECT COUNT(*) FROM attendance WHERE studentId = :studentId AND present = 1")
    int getTotalPresentCount(int studentId);

    @Query("SELECT COUNT(*) FROM attendance WHERE studentId = :studentId")
    int getTotalAttendanceCount(int studentId);

    @Query("SELECT COUNT(*) FROM attendance WHERE studentId = :studentId AND subjectId = :subjectId AND qrSessionId = :sessionId")
    int isAlreadyMarked(int studentId, int subjectId, String sessionId);

    @Query("SELECT DISTINCT date FROM attendance WHERE subjectId = :subjectId ORDER BY date DESC")
    List<String> getAttendanceDates(int subjectId);

    @Query("SELECT COUNT(DISTINCT date) FROM attendance WHERE studentId = :studentId AND present = 1 AND date >= :fromDate")
    int getRecentPresentDays(int studentId, String fromDate);
}
