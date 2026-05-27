package com.edumetrics.app.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.edumetrics.app.database.dao.*;
import com.edumetrics.app.database.entities.*;

@Database(entities = {
        User.class,
        ClassEntity.class,
        Subject.class,
        StudentClass.class,
        Attendance.class,
        Task.class,
        TaskStatus.class,
        Score.class
}, version = 1, exportSchema = false)
public abstract class EduMetricsDatabase extends RoomDatabase {

    private static volatile EduMetricsDatabase INSTANCE;

    public abstract UserDao userDao();
    public abstract ClassDao classDao();
    public abstract SubjectDao subjectDao();
    public abstract StudentClassDao studentClassDao();
    public abstract AttendanceDao attendanceDao();
    public abstract TaskDao taskDao();
    public abstract ScoreDao scoreDao();

    public static EduMetricsDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (EduMetricsDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    EduMetricsDatabase.class,
                                    "edumetrics_database")
                            .allowMainThreadQueries() // For simplicity; in production use async
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
