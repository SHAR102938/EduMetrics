package com.edumetrics.app.activities.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.edumetrics.app.R;
import com.edumetrics.app.activities.faculty.FacultyDashboardActivity;
import com.edumetrics.app.activities.student.StudentDashboardActivity;
import com.edumetrics.app.database.EduMetricsDatabase;
import com.edumetrics.app.database.entities.User;
import com.edumetrics.app.utils.AnimationHelper;
import com.edumetrics.app.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.security.MessageDigest;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private SessionManager sessionManager;
    private EduMetricsDatabase db;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        db = EduMetricsDatabase.getInstance(this);
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // PERSISTENCE FIX: Check if Firebase session is active
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            if (sessionManager.isLoggedIn()) {
                navigateToDashboard();
                return;
            } else {
                // If Firebase knows the user but local session is lost, restore it
                restoreSession(currentUser.getUid(), currentUser.getEmail());
                return;
            }
        }

        setContentView(R.layout.activity_login);
        initViews();
        setupListeners();
        animateUI();
    }

    private void restoreSession(String uid, String email) {
        firestore.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        String role = documentSnapshot.getString("role");
                        Long localIdObj = documentSnapshot.getLong("localId");
                        int localId = localIdObj != null ? localIdObj.intValue() : 1;
                        
                        // Re-create session
                        sessionManager.createSession(localId, name, email, role);
                        navigateToDashboard();
                    } else {
                        com.google.firebase.database.FirebaseDatabase.getInstance().getReference("users").child(uid).addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                            @Override
                            public void onDataChange(@androidx.annotation.NonNull com.google.firebase.database.DataSnapshot snapshot) {
                                if (snapshot.exists()) {
                                    String name = snapshot.child("name").getValue(String.class);
                                    String role = snapshot.child("role").getValue(String.class);
                                    Integer rtLocalId = snapshot.child("localId").getValue(Integer.class);
                                    int localId = rtLocalId != null ? rtLocalId : 1;
                                    sessionManager.createSession(localId, name, email, role);
                                    navigateToDashboard();
                                } else {
                                    auth.signOut();
                                    showLoginUI();
                                }
                            }
                            @Override
                            public void onCancelled(@androidx.annotation.NonNull com.google.firebase.database.DatabaseError error) {
                                auth.signOut();
                                showLoginUI();
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
                    if (msg.contains("network") || msg.contains("connection") || msg.contains("offline") || msg.contains("unavailable")) {
                        new Thread(() -> {
                            User localUser = db.userDao().getUserByEmail(email);
                            if (localUser != null) {
                                runOnUiThread(() -> {
                                    sessionManager.createSession(localUser.id, localUser.name, localUser.email, localUser.role);
                                    Toast.makeText(LoginActivity.this, "Offline Mode", Toast.LENGTH_SHORT).show();
                                    navigateToDashboard();
                                });
                            } else {
                                runOnUiThread(() -> {
                                    auth.signOut();
                                    showLoginUI();
                                });
                            }
                        }).start();
                    } else {
                        auth.signOut();
                        showLoginUI();
                    }
                });
    }

    private void showLoginUI() {
        setContentView(R.layout.activity_login);
        initViews();
        setupListeners();
    }

    private void initViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> {
            AnimationHelper.buttonPress(v);
            attemptLogin();
        });

        findViewById(R.id.tvRegister).setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });
    }

    private void animateUI() {
        AnimationHelper.slideUpFadeIn(findViewById(R.id.logoContainer), 0);
        AnimationHelper.slideUpFadeIn(btnLogin.getParent() instanceof android.view.View ?
                (android.view.View) btnLogin.getParent() : btnLogin, 200);
    }

    private void attemptLogin() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            return;
        }
        if (password.isEmpty()) {
            etPassword.setError("Password is required");
            return;
        }

        btnLogin.setEnabled(false);
        Toast.makeText(this, "Logging in...", Toast.LENGTH_SHORT).show();

        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser().getUid();
                    fetchProfileAndStartSession(uid, email, password);
                })
                .addOnFailureListener(e -> {
                    btnLogin.setEnabled(true);
                    String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
                    if (msg.contains("network") || msg.contains("connection") || msg.contains("offline") || msg.contains("unavailable")) {
                         checkLocalFallback(email, hashPassword(password), "Offline mode enabled.");
                    } else {
                         Toast.makeText(this, "Login Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchProfileAndStartSession(String uid, String email, String password) {
        firestore.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        String role = documentSnapshot.getString("role");
                        Long cloudLocalId = documentSnapshot.getLong("localId");
                        processLoginSuccess(name, role, password, cloudLocalId, uid, email);
                    } else {
                        com.google.firebase.database.FirebaseDatabase.getInstance().getReference("users").child(uid).addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                            @Override
                            public void onDataChange(@androidx.annotation.NonNull com.google.firebase.database.DataSnapshot snapshot) {
                                if (snapshot.exists()) {
                                    String name = snapshot.child("name").getValue(String.class);
                                    String role = snapshot.child("role").getValue(String.class);
                                    Integer rtLocalId = snapshot.child("localId").getValue(Integer.class);
                                    Long cloudLocalId = rtLocalId != null ? rtLocalId.longValue() : null;
                                    
                                    java.util.Map<String, Object> userMap = new java.util.HashMap<>();
                                    userMap.put("uid", uid);
                                    userMap.put("name", name);
                                    userMap.put("email", email);
                                    userMap.put("role", role);
                                    if (cloudLocalId != null) userMap.put("localId", cloudLocalId);
                                    firestore.collection("users").document(uid).set(userMap);
                                    
                                    processLoginSuccess(name, role, password, cloudLocalId, uid, email);
                                } else {
                                    btnLogin.setEnabled(true);
                                    Toast.makeText(LoginActivity.this, "Profile not found anywhere", Toast.LENGTH_SHORT).show();
                                }
                            }
                            @Override
                            public void onCancelled(@androidx.annotation.NonNull com.google.firebase.database.DatabaseError error) {
                                btnLogin.setEnabled(true);
                                Toast.makeText(LoginActivity.this, "RTDB Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    btnLogin.setEnabled(true);
                    String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
                    if (msg.contains("network") || msg.contains("connection") || msg.contains("offline") || msg.contains("unavailable")) {
                         checkLocalFallback(email, hashPassword(password), "Offline mode enabled.");
                    } else {
                         Toast.makeText(LoginActivity.this, "Cloud Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void processLoginSuccess(String name, String role, String password, Long cloudLocalId, String uid, String email) {
        String hashedPassword = hashPassword(password);
        int firebaseLocalIdVal = cloudLocalId != null ? cloudLocalId.intValue() : Math.abs(uid.hashCode());
        if (firebaseLocalIdVal == 0) firebaseLocalIdVal = 1;
        final int firebaseLocalId = firebaseLocalIdVal;

        if (cloudLocalId == null) {
             firestore.collection("users").document(uid).update("localId", firebaseLocalId);
        }

        new Thread(() -> {
            User localUser = db.userDao().getUserByEmail(email);
            long localIdVal;
            if (localUser == null) {
                localUser = new User(name, email, hashedPassword, role);
                localUser.id = firebaseLocalId;
                try {
                    localIdVal = db.userDao().insert(localUser);
                } catch (Exception e) {
                    User existing = db.userDao().getUserByEmail(email);
                    if (existing != null) {
                        localIdVal = existing.id;
                    } else {
                        localIdVal = firebaseLocalId;
                    }
                }
            } else {
                localUser.name = name;
                localUser.role = role;
                localUser.passwordHash = hashedPassword;
                db.userDao().update(localUser);
                localIdVal = localUser.id;
            }

            final long localId = localIdVal;
            runOnUiThread(() -> {
                sessionManager.createSession((int)localId, name, email, role);
                Toast.makeText(LoginActivity.this, "Welcome back, " + name, Toast.LENGTH_SHORT).show();
                navigateToDashboard();
            });
        }).start();
    }

    private void checkLocalFallback(String email, String hashedPassword, String message) {
        new Thread(() -> {
            User user = db.userDao().login(email, hashedPassword);
            runOnUiThread(() -> {
                if (user != null) {
                    sessionManager.createSession(user.id, user.name, user.email, user.role);
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                    navigateToDashboard();
                } else {
                    btnLogin.setEnabled(true);
                    Toast.makeText(this, "Invalid credentials or No Internet", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void navigateToDashboard() {
        Intent intent;
        if (sessionManager.isFaculty()) {
            intent = new Intent(this, FacultyDashboardActivity.class);
        } else {
            intent = new Intent(this, StudentDashboardActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }

    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return password; 
        }
    }
}
