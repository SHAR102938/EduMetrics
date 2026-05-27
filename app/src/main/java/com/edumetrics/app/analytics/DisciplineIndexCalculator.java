package com.edumetrics.app.analytics;

import com.edumetrics.app.database.EduMetricsDatabase;

/**
 * Calculates Discipline Index based on:
 * Formula: (Attendance% + OnTimeTaskRate + ConsistencyScore) / 3
 * 
 * Components:
 * - Attendance % (overall)
 * - On-time task completion rate
 * - Consistency Score (from ConsistencyScoreCalculator)
 */
public class DisciplineIndexCalculator {

    private final EduMetricsDatabase db;
    private final ConsistencyScoreCalculator consistencyCalculator;

    public DisciplineIndexCalculator(EduMetricsDatabase db) {
        this.db = db;
        this.consistencyCalculator = new ConsistencyScoreCalculator(db);
    }

    /**
     * Calculate discipline index for a student (0-100).
     */
    public float calculate(int studentId) {
        float attendancePercentage = calculateAttendancePercentage(studentId);
        float onTimeTaskRate = calculateOnTimeTaskRate(studentId);
        float consistencyScore = consistencyCalculator.calculate(studentId);

        // Weighted average
        return (attendancePercentage + onTimeTaskRate + consistencyScore) / 3.0f;
    }

    private float calculateAttendancePercentage(int studentId) {
        int totalPresent = db.attendanceDao().getTotalPresentCount(studentId);
        int totalClasses = db.attendanceDao().getTotalAttendanceCount(studentId);

        if (totalClasses == 0) return 0;
        return (totalPresent * 100.0f) / totalClasses;
    }

    private float calculateOnTimeTaskRate(int studentId) {
        int onTimeTasks = db.taskDao().getOnTimeCompletedTasks(studentId);
        int totalTasks = db.taskDao().getTotalTaskCount(studentId);

        if (totalTasks == 0) return 0;
        return (onTimeTasks * 100.0f) / totalTasks;
    }

    /**
     * Get a descriptive label for the discipline index.
     */
    public static String getLabel(float index) {
        if (index >= 85) return "Excellent";
        if (index >= 70) return "Good";
        if (index >= 55) return "Average";
        if (index >= 40) return "Below Average";
        return "Needs Improvement";
    }

    /**
     * Get risk color resource name based on discipline index.
     */
    public static String getRiskColor(float index) {
        if (index >= 70) return "risk_low";
        if (index >= 45) return "risk_medium";
        return "risk_high";
    }
}
