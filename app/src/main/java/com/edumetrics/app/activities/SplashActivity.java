package com.edumetrics.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.edumetrics.app.R;
import com.edumetrics.app.activities.auth.LoginActivity;
import com.edumetrics.app.activities.faculty.FacultyDashboardActivity;
import com.edumetrics.app.activities.student.StudentDashboardActivity;
import com.edumetrics.app.utils.SessionManager;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION_MS = 2200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        View logoCircle = findViewById(R.id.logoCircle);
        TextView tvAppName = findViewById(R.id.tvAppName);
        TextView tvTagline = findViewById(R.id.tvTagline);
        ProgressBar progressBar = findViewById(R.id.progressBar);

        // --- Animate logo: scale + fade in ---
        logoCircle.setAlpha(0f);
        logoCircle.setScaleX(0f);
        logoCircle.setScaleY(0f);
        logoCircle.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(600)
                .setInterpolator(new OvershootInterpolator(1.5f))
                .start();

        // --- Animate app name: slide up + fade in ---
        tvAppName.setAlpha(0f);
        tvAppName.setTranslationY(30f);
        tvAppName.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay(400)
                .start();

        // --- Animate tagline ---
        tvTagline.setAlpha(0f);
        tvTagline.setTranslationY(20f);
        tvTagline.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setStartDelay(600)
                .start();

        // --- Animate progress bar ---
        progressBar.setAlpha(0f);
        progressBar.animate()
                .alpha(1f)
                .setDuration(300)
                .setStartDelay(800)
                .start();

        // --- Navigate after splash duration ---
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            SessionManager sessionManager = new SessionManager(this);

            Intent intent;
            if (sessionManager.isLoggedIn()) {
                // Auto-login: go directly to the right dashboard
                if (sessionManager.isFaculty()) {
                    intent = new Intent(this, FacultyDashboardActivity.class);
                } else {
                    intent = new Intent(this, StudentDashboardActivity.class);
                }
            } else {
                intent = new Intent(this, LoginActivity.class);
            }

            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);

            // Smooth cross-fade transition
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();

        }, SPLASH_DURATION_MS);
    }
}
