package com.edumetrics.app.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import com.edumetrics.app.database.entities.Task;
import com.edumetrics.app.database.entities.TaskStatus;

import java.util.List;

@Dao
public interface TaskDao {
    @Insert
    long insertTask(Task task);

    @Insert
    long insertTaskStatus(TaskStatus taskStatus);

    @Update
    void updateTask(Task task);

    @Update
    void updateTaskStatus(TaskStatus taskStatus);

    @Delete
    void deleteTask(Task task);

    @Query("SELECT * FROM tasks")
    List<Task> getAllTasks();

    @Query("SELECT * FROM tasks WHERE classId = :classId AND type != 'personal' ORDER BY createdAt DESC")
    List<Task> getTasksByClass(int classId);

    @Query("SELECT * FROM tasks WHERE subjectId = :subjectId AND type != 'personal' ORDER BY createdAt DESC")
    List<Task> getTasksBySubject(int subjectId);

    @Query("SELECT * FROM tasks WHERE createdBy = :userId AND type = 'personal' ORDER BY createdAt DESC")
    List<Task> getPersonalTasks(int userId);

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    Task getTaskById(int id);

    @Query("SELECT * FROM task_status WHERE taskId = :taskId AND studentId = :studentId LIMIT 1")
    TaskStatus getTaskStatus(int taskId, int studentId);

    @Query("SELECT COUNT(*) FROM task_status WHERE studentId = :studentId AND completed = 1")
    int getCompletedTaskCount(int studentId);

    @Query("SELECT COUNT(*) FROM task_status WHERE studentId = :studentId AND completed = 0")
    int getPendingTaskCount(int studentId);

    @Query("SELECT COUNT(*) FROM task_status WHERE studentId = :studentId")
    int getTotalTaskCount(int studentId);

    @Query("SELECT t.* FROM tasks t INNER JOIN task_status ts ON t.id = ts.taskId " +
            "WHERE ts.studentId = :studentId ORDER BY t.createdAt DESC")
    List<Task> getTasksForStudent(int studentId);

    @Query("SELECT COUNT(*) FROM task_status WHERE studentId = :studentId AND completed = 1 AND completedAt > 0 " +
            "AND completedAt <= (SELECT t.createdAt + 86400000 * 7 FROM tasks t WHERE t.id = task_status.taskId)")
    int getOnTimeCompletedTasks(int studentId);

    @Query("SELECT * FROM tasks WHERE createdBy = :facultyId AND type != 'personal' ORDER BY createdAt DESC")
    List<Task> getTasksByFaculty(int facultyId);

    @Query("SELECT ts.* FROM task_status ts WHERE ts.taskId = :taskId")
    List<TaskStatus> getTaskStatusList(int taskId);
}
