package com.edumetrics.app.activities.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.edumetrics.app.R;
import com.edumetrics.app.database.EduMetricsDatabase;
import com.edumetrics.app.database.entities.User;
import com.edumetrics.app.utils.AnimationHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etName, etEmail, etPassword, etConfirmPassword;
    private RadioGroup rgRole;
    private MaterialButton btnRegister;
    private EduMetricsDatabase db;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        db = EduMetricsDatabase.getInstance(this);
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        initViews();
        setupListeners();
    }

    private void initViews() {
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        rgRole = findViewById(R.id.rgRole);
        btnRegister = findViewById(R.id.btnRegister);
    }

    private void setupListeners() {
        btnRegister.setOnClickListener(v -> {
            AnimationHelper.buttonPress(v);
            attemptRegister();
        });

        findViewById(R.id.tvLogin).setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });
    }

    private void attemptRegister() {
        String name = getText(etName);
        String email = getText(etEmail);
        String password = getText(etPassword);
        String confirmPassword = getText(etConfirmPassword);

        // Validation
        if (name.isEmpty()) { etName.setError("Name is required"); return; }
        if (email.isEmpty()) { etEmail.setError("Email is required"); return; }
        if (!email.contains("@")) { etEmail.setError("Invalid email"); return; }
        if (password.isEmpty()) { etPassword.setError("Password is required"); return; }
        if (password.length() < 6) { etPassword.setError("Min 6 characters"); return; }
        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords don't match");
            return;
        }

        // Get role
        String role = rgRole.getCheckedRadioButtonId() == R.id.rbFaculty ? "faculty" : "student";

        // Firebase Registration First
        btnRegister.setEnabled(false);
        Toast.makeText(this, "Creating account...", Toast.LENGTH_SHORT).show();

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser().getUid();
                    
                    // Create user in Firestore (Consistent with Login/Join/Tasks)
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("uid", uid);
                    userMap.put("name", name);
                    userMap.put("email", email);
                    userMap.put("role", role);
                    
                    int tempId = Math.abs(uid.hashCode());
                    if (tempId == 0) tempId = 1;
                    final int uniqueId = tempId; // Fix: effectively final for lambda
                    userMap.put("localId", uniqueId);

                    firestore.collection("users").document(uid).set(userMap, SetOptions.merge())
                            .addOnSuccessListener(aVoid -> {
                                // Now save to local DB
                                String hashedPassword = LoginActivity.hashPassword(password);
                                User user = new User(name, email, hashedPassword, role);
                                user.id = uniqueId;
                                
                                new Thread(() -> {
                                    db.userDao().insert(user);
                                    runOnUiThread(() -> {
                                        com.edumetrics.app.utils.SessionManager sessionManager = new com.edumetrics.app.utils.SessionManager(this);
                                        sessionManager.createSession(uniqueId, name, email, role);
                                        Toast.makeText(this, "Account verified & created!", Toast.LENGTH_SHORT).show();
                                        Intent intent;
                                        if (role.equals("faculty")) {
                                            intent = new Intent(this, com.edumetrics.app.activities.faculty.FacultyDashboardActivity.class);
                                        } else {
                                            intent = new Intent(this, com.edumetrics.app.activities.student.StudentDashboardActivity.class);
                                        }
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                                        finish();
                                    });
                                }).start();
                            })
                            .addOnFailureListener(e -> {
                                btnRegister.setEnabled(true);
                                Toast.makeText(this, "Cloud Sync error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    btnRegister.setEnabled(true);
                    Toast.makeText(this, "Firebase Auth error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }
}
