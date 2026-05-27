package com.edumetrics.app.activities.faculty;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.edumetrics.app.R;
import com.edumetrics.app.database.EduMetricsDatabase;
import com.edumetrics.app.database.entities.ClassEntity;
import com.edumetrics.app.services.FirebaseService;
import com.edumetrics.app.utils.AnimationHelper;
import com.edumetrics.app.utils.SessionManager;
import com.edumetrics.app.adapters.ClassAdapter;

import java.util.List;
import java.util.Random;

public class ClassManagementActivity extends AppCompatActivity {

    private RecyclerView rvClasses;
    private EduMetricsDatabase db;
    private SessionManager sessionManager;
    private FirebaseService firebaseService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class_management);

        db = EduMetricsDatabase.getInstance(this);
        sessionManager = new SessionManager(this);
        firebaseService = new FirebaseService(this);

        rvClasses = findViewById(R.id.rvClasses);
        rvClasses.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnCreateClass).setOnClickListener(v -> {
            AnimationHelper.buttonPress(v);
            showCreateClassDialog();
        });

        loadClasses();
    }

    private void loadClasses() {
        List<ClassEntity> classes = db.classDao().getClassesByFaculty(sessionManager.getUserId());
        ClassAdapter adapter = new ClassAdapter(classes, db, classEntity -> {
            sessionManager.setCurrentClassId(classEntity.id);
            Toast.makeText(this, "Selected: " + classEntity.name, Toast.LENGTH_SHORT).show();
        });
        rvClasses.setAdapter(adapter);
    }

    private void showCreateClassDialog() {
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_class, null);
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.Theme_EduMetrics)
                .setView(dialogView)
                .create();

        EditText etName = dialogView.findViewById(R.id.etClassName);
        com.google.android.material.button.MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        com.google.android.material.button.MaterialButton btnConfirm = dialogView.findViewById(R.id.btnConfirmCreate);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show();
                return;
            }

            String code = generateClassCode();
            int classId = Math.abs(code.hashCode());
            if (classId == 0) classId = 1;
            ClassEntity classEntity = new ClassEntity(name, code, sessionManager.getUserId());
            classEntity.id = classId;
            
            // Push to Firebase first, then Local DB
            firebaseService.pushEntity("classes", code, classEntity);

            // Background thread execution for DB
            new Thread(() -> {
                db.classDao().insert(classEntity);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Class created! Code: " + code, Toast.LENGTH_LONG).show();
                    loadClasses();
                    dialog.dismiss();
                });
            }).start();
        });

        dialog.show();
    }

    private String generateClassCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
