package com.edumetrics.app.services;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.edumetrics.app.database.EduMetricsDatabase;
import com.edumetrics.app.database.entities.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service to handle synchronization between local Room DB and Firebase Realtime Database.
 * This maintains the "Offline-First" nature while providing Cloud Backup and Sync.
 */
public class FirebaseService {

    private static final String TAG = "FirebaseService";
    private final DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final EduMetricsDatabase localDb;

    public FirebaseService(Context context) {
        this.localDb = EduMetricsDatabase.getInstance(context);
    }

    /**
     * Push all local data to Realtime Database for a specific user.
     * This is typically called after login or when a manual sync is triggered.
     */
    public void syncLocalToCloud() {
        if (auth.getCurrentUser() == null) return;

        String uid = auth.getUid();
        if (uid == null) return;
        
        new Thread(() -> {
            // 1. Sync User Profile
            User localUser = localDb.userDao().getUserByEmail(auth.getCurrentUser().getEmail()); 
            if (localUser != null) {
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("name", localUser.name);
                userMap.put("email", localUser.email);
                userMap.put("role", localUser.role);
                userMap.put("localId", localUser.id);
                userMap.put("lastSync", System.currentTimeMillis());
                
                mDatabase.child("users").child(uid).updateChildren(userMap);
            }

            // 2. Sync Classes
            List<ClassEntity> classes = localDb.classDao().getAllClasses();
            for (ClassEntity c : classes) {
                mDatabase.child("classes").child(c.classCode).setValue(c);
            }

            // 3. Sync Subjects
            List<Subject> subjects = localDb.subjectDao().getAllSubjects();
            for (Subject s : subjects) {
                mDatabase.child("subjects").child(String.valueOf(s.id)).setValue(s);
            }

            // 4. Sync Attendance Records
            List<Attendance> attendanceList = localDb.attendanceDao().getAllAttendance();
            for (Attendance a : attendanceList) {
                mDatabase.child("attendance").child(String.valueOf(a.id)).setValue(a);
            }

            // 5. Sync Tasks
            List<Task> tasks = localDb.taskDao().getAllTasks();
            for (Task t : tasks) {
                mDatabase.child("tasks").child(String.valueOf(t.id)).setValue(t);
            }

            Log.d(TAG, "Full Sync Completed successfully to Realtime Database");
        }).start();
    }

    public void syncCloudToLocal(Runnable onComplete) {
        if (auth.getCurrentUser() == null) {
            if (onComplete != null) onComplete.run();
            return;
        }

        new Thread(() -> {
            try {
                com.google.android.gms.tasks.Task<DataSnapshot> syncTask = FirebaseDatabase.getInstance().getReference().get();
                com.google.android.gms.tasks.Tasks.await(syncTask);
                DataSnapshot root = syncTask.getResult();

                // 1. Sync classes
                for (DataSnapshot ds : root.child("classes").getChildren()) {
                    try {
                        ClassEntity c = ds.getValue(ClassEntity.class);
                        if (c != null && localDb.classDao().getClassByCode(c.classCode) == null) {
                            localDb.classDao().insert(c);
                        }
                    } catch (Exception e) {}
                }

                // 2. Sync all users
                for (DataSnapshot ds : root.child("users").getChildren()) {
                    try {
                        Integer localIdObj = ds.child("localId").getValue(Integer.class);
                        if (localIdObj != null && localDb.userDao().getUserById(localIdObj) == null) {
                            String email = ds.child("email").getValue(String.class);
                            String name = ds.child("name").getValue(String.class);
                            String r = ds.child("role").getValue(String.class);
                            User nu = new User(name, email, "", r);
                            nu.id = localIdObj;
                            localDb.userDao().insert(nu);
                        }
                    } catch (Exception e) {}
                }

                // 3. Sync Subjects
                for (DataSnapshot ds : root.child("subjects").getChildren()) {
                    try {
                        Subject s = ds.getValue(Subject.class);
                        if (s != null && localDb.subjectDao().getSubjectById(s.id) == null) {
                            localDb.subjectDao().insert(s);
                        }
                    } catch (Exception e) {}
                }

                // 4. Sync class_students
                for (DataSnapshot dsClass : root.child("class_students").getChildren()) {
                    try {
                        int cId = Integer.parseInt(dsClass.getKey());
                        for (DataSnapshot dsStudent : dsClass.getChildren()) {
                            Integer sId = dsStudent.child("studentId").getValue(Integer.class);
                            if (sId != null && localDb.studentClassDao().isStudentInClass(sId, cId) == 0) {
                                localDb.studentClassDao().insert(new StudentClass(sId, cId));
                            }
                        }
                    } catch (Exception e) {}
                }

                // 5. Sync Tasks
                for (DataSnapshot ds : root.child("tasks").getChildren()) {
                     try {
                         Task t = ds.getValue(Task.class);
                         if (t != null) {
                             boolean exists = false;
                             for (Task existing : localDb.taskDao().getAllTasks()) {
                                 if (existing.id == t.id) { exists = true; break; }
                             }
                             if (!exists) localDb.taskDao().insertTask(t);
                         }
                     } catch (Exception e) {}
                }

                // 6. Sync task_status
                for (DataSnapshot dsTask : root.child("task_status").getChildren()) {
                     try {
                         for (DataSnapshot dsStudent : dsTask.getChildren()) {
                             TaskStatus ts = dsStudent.getValue(TaskStatus.class);
                             if (ts != null) {
                                  if (localDb.taskDao().getTaskStatus(ts.taskId, ts.studentId) == null) {
                                      localDb.taskDao().insertTaskStatus(ts);
                                  } else {
                                      localDb.taskDao().updateTaskStatus(ts);
                                  }
                             }
                         }
                     } catch (Exception e) {}
                }

                // 7. Sync Attendance
                for (DataSnapshot ds : root.child("attendance").getChildren()) {
                     try {
                         Attendance a = ds.getValue(Attendance.class);
                         if (a != null && localDb.attendanceDao().isAlreadyMarked(a.studentId, a.subjectId, a.qrSessionId) == 0) {
                             localDb.attendanceDao().insert(a);
                         }
                     } catch (Exception e) {}
                }

                // 8. Sync Scores
                for (DataSnapshot ds : root.child("scores").getChildren()) {
                     try {
                         Score s = ds.getValue(Score.class);
                         if (s != null && localDb.scoreDao().getScoreById(s.id) == null) {
                             localDb.scoreDao().insert(s);
                         }
                     } catch (Exception e) {}
                }

            } catch (Exception e) {
                 Log.e(TAG, "Sync failed: " + e.getMessage());
            }

            if (onComplete != null) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(onComplete);
            }
        }).start();
    }

    /**
     * Helper to save a single entity to Realtime Database immediately with listeners.
     */
    public void pushEntity(String path, String key, Object entity) {
        if (auth.getCurrentUser() == null) {
            Log.e(TAG, "Auth user is null, cannot push entity");
            return;
        }
        
        mDatabase.child(path).child(key).setValue(entity)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Entity pushed: " + key))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to push entity: " + e.getMessage(), e));
    }
}
