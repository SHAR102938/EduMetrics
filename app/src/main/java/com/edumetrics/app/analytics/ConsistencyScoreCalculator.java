package com.edumetrics.app.analytics;

import com.edumetrics.app.database.EduMetricsDatabase;

/**
 * Calculates Consistency Score based on:
 * - Attendance regularity (40%)
 * - Task completion rate (30%)
 * - Activity frequency / recent engagement (30%)
 */
public class ConsistencyScoreCalculator {

    private final EduMetricsDatabase db;

    public ConsistencyScoreCalculator(EduMetricsDatabase db) {
        this.db = db;
    }

    /**
     * Calculate consistency score for a student (0-100).
     */
    public float calculate(int studentId) {
        float attendanceScore = calculateAttendanceConsistency(studentId);
        float taskScore = calculateTaskConsistency(studentId);
        float activityScore = calculateActivityFrequency(studentId);

        // Weighted formula
        return (attendanceScore * 0.4f) + (taskScore * 0.3f) + (activityScore * 0.3f);
    }

    private float calculateAttendanceConsistency(int studentId) {
        int totalPresent = db.attendanceDao().getTotalPresentCount(studentId);
        int totalClasses = db.attendanceDao().getTotalAttendanceCount(studentId);

        if (totalClasses == 0) return 0;
        return (totalPresent * 100.0f) / totalClasses;
    }

    private float calculateTaskConsistency(int studentId) {
        int completedTasks = db.taskDao().getCompletedTaskCount(studentId);
        int totalTasks = db.taskDao().getTotalTaskCount(studentId);

        if (totalTasks == 0) return 0;
        return (completedTasks * 100.0f) / totalTasks;
    }

    private float calculateActivityFrequency(int studentId) {
        // Based on recent 30 days of engagement
        String thirtyDaysAgo = com.edumetrics.app.utils.DateUtils.getDateNDaysAgo(30);
        int recentAttendance = db.attendanceDao().getRecentPresentDays(studentId, thirtyDaysAgo);

        // Normalize: attending 20+ days in 30 = 100%
        float activityRate = Math.min(recentAttendance / 20.0f, 1.0f) * 100;
        return activityRate;
    }

    /**
     * Get risk level based on consistency score.
     */
    public static String getRiskLevel(float score) {
        if (score >= 75) return "LOW";
        if (score >= 50) return "MEDIUM";
        return "HIGH";
    }
}
