package com.edumetrics.app.activities.faculty;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.edumetrics.app.R;
import com.edumetrics.app.adapters.SubjectAdapter;
import com.edumetrics.app.database.EduMetricsDatabase;
import com.edumetrics.app.database.entities.ClassEntity;
import com.edumetrics.app.database.entities.Subject;
import com.edumetrics.app.services.FirebaseService;
import com.edumetrics.app.utils.AnimationHelper;
import com.edumetrics.app.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class SubjectManagementActivity extends AppCompatActivity {

    private RecyclerView rvSubjects;
    private EduMetricsDatabase db;
    private SessionManager sessionManager;
    private FirebaseService firebaseService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subject_management);

        db = EduMetricsDatabase.getInstance(this);
        sessionManager = new SessionManager(this);
        firebaseService = new FirebaseService(this);

        rvSubjects = findViewById(R.id.rvSubjects);
        rvSubjects.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnAddSubject).setOnClickListener(v -> {
            AnimationHelper.buttonPress(v);
            showAddSubjectDialog();
        });

        loadSubjects();
    }

    private void loadSubjects() {
        List<Subject> subjects = db.subjectDao().getSubjectsByFaculty(sessionManager.getUserId());
        SubjectAdapter adapter = new SubjectAdapter(subjects, db, 0, null);
        rvSubjects.setAdapter(adapter);
    }

    private void showAddSubjectDialog() {
        List<ClassEntity> classes = db.classDao().getClassesByFaculty(sessionManager.getUserId());
        if (classes.isEmpty()) {
            Toast.makeText(this, "Create a class first!", Toast.LENGTH_SHORT).show();
            return;
        }

        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_subject, null);
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.Theme_EduMetrics)
                .setView(dialogView)
                .create();

        EditText etName = dialogView.findViewById(R.id.etSubjectName);
        Spinner spinner = dialogView.findViewById(R.id.spinnerClasses);
        com.google.android.material.button.MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancelSubject);
        com.google.android.material.button.MaterialButton btnConfirm = dialogView.findViewById(R.id.btnConfirmAddSubject);

        List<String> classNames = new ArrayList<>();
        for (ClassEntity c : classes) classNames.add(c.name);
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, classNames) {
            @Override
            public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                android.view.View v = super.getView(position, convertView, parent);
                ((android.widget.TextView) v).setTextColor(getColor(R.color.text_primary));
                return v;
            }
            @Override
            public android.view.View getDropDownView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                android.view.View v = super.getDropDownView(position, convertView, parent);
                ((android.widget.TextView) v).setTextColor(getColor(R.color.text_primary));
                v.setBackgroundColor(getColor(R.color.surface));
                return v;
            }
        };
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show();
                return;
            }

            int selectedIdx = spinner.getSelectedItemPosition();
            ClassEntity selectedClass = classes.get(selectedIdx);

            int subjectId = Math.abs((name + "_" + selectedClass.id).hashCode());
            if (subjectId == 0) subjectId = 1;
            final int finalSubjectId = subjectId;

            Subject subject = new Subject(name, selectedClass.id, sessionManager.getUserId());
            subject.id = finalSubjectId;
            
            new Thread(() -> {
                db.subjectDao().insert(subject);
                
                // Sync to cloud
                firebaseService.pushEntity("subjects", String.valueOf(finalSubjectId), subject);
                
                runOnUiThread(() -> {
                    Toast.makeText(this, "Subject added!", Toast.LENGTH_SHORT).show();
                    loadSubjects();
                    dialog.dismiss();
                });
            }).start();
        });

        dialog.show();
    }
}
