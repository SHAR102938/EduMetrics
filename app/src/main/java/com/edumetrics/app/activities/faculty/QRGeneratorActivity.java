package com.edumetrics.app.activities.faculty;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.edumetrics.app.R;
import com.edumetrics.app.database.EduMetricsDatabase;
import com.edumetrics.app.database.entities.Subject;
import com.edumetrics.app.utils.AnimationHelper;
import com.edumetrics.app.utils.QRCodeHelper;
import com.edumetrics.app.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class QRGeneratorActivity extends AppCompatActivity {

    private Spinner spinnerSubject;
    private ImageView ivQRCode;
    private TextView tvTimer, tvTimerLabel;
    private EduMetricsDatabase db;
    private SessionManager sessionManager;
    private List<Subject> subjects;
    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_generator);

        db = EduMetricsDatabase.getInstance(this);
        sessionManager = new SessionManager(this);

        spinnerSubject = findViewById(R.id.spinnerSubject);
        ivQRCode = findViewById(R.id.ivQRCode);
        tvTimer = findViewById(R.id.tvTimer);
        tvTimerLabel = findViewById(R.id.tvTimerLabel);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnGenerate).setOnClickListener(v -> {
            AnimationHelper.buttonPress(v);
            generateQR();
        });

        loadSubjects();
    }

    private void loadSubjects() {
        subjects = db.subjectDao().getSubjectsByFaculty(sessionManager.getUserId());
        List<String> names = new ArrayList<>();
        for (Subject s : subjects) names.add(s.name);

        if (names.isEmpty()) names.add("No subjects");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSubject.setAdapter(adapter);
    }

    private void generateQR() {
        if (subjects.isEmpty()) {
            Toast.makeText(this, "Create a subject first!", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedIdx = spinnerSubject.getSelectedItemPosition();
        Subject subject = subjects.get(selectedIdx);

        String sessionId = QRCodeHelper.generateSessionId(subject.id);
        long timestamp = System.currentTimeMillis();

        Bitmap qrBitmap = QRCodeHelper.generateQRCode(subject.id, sessionId, timestamp, 800);
        if (qrBitmap != null) {
            ivQRCode.setImageBitmap(qrBitmap);
            AnimationHelper.scaleUp(ivQRCode);
            startTimer();
            Toast.makeText(this, "QR Generated for " + subject.name, Toast.LENGTH_SHORT).show();
        }
    }

    private void startTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new CountDownTimer(5 * 60 * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int minutes = (int) (millisUntilFinished / 60000);
                int seconds = (int) ((millisUntilFinished % 60000) / 1000);
                tvTimer.setText(String.format("%d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                tvTimer.setText("0:00");
                tvTimerLabel.setText("Session expired!");
                tvTimer.setTextColor(getColor(R.color.chart_red));
                ivQRCode.setImageResource(0);
            }
        };
        countDownTimer.start();
        tvTimer.setTextColor(getColor(R.color.accent));
        tvTimerLabel.setText("Session valid for 5 minutes");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}
