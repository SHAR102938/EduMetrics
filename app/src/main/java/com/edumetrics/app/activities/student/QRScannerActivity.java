package com.edumetrics.app.activities.student;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.edumetrics.app.R;
import com.edumetrics.app.database.EduMetricsDatabase;
import com.edumetrics.app.database.entities.Attendance;
import com.edumetrics.app.database.entities.Subject;
import com.edumetrics.app.utils.AnimationHelper;
import com.edumetrics.app.utils.DateUtils;
import com.edumetrics.app.utils.QRCodeHelper;
import com.edumetrics.app.utils.SessionManager;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import org.json.JSONObject;

public class QRScannerActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_CODE = 100;

    private DecoratedBarcodeView barcodeView;
    private CardView cardResult;
    private TextView tvResultIcon, tvResultMessage, tvResultDetail;
    private EduMetricsDatabase db;
    private SessionManager sessionManager;
    private boolean scanHandled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scanner);

        db = EduMetricsDatabase.getInstance(this);
        sessionManager = new SessionManager(this);

        cardResult = findViewById(R.id.cardResult);
        tvResultIcon = findViewById(R.id.tvResultIcon);
        tvResultMessage = findViewById(R.id.tvResultMessage);
        tvResultDetail = findViewById(R.id.tvResultDetail);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        checkCameraPermission();
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else {
            initScanner();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initScanner();
            } else {
                Toast.makeText(this, "Camera permission is required for QR scanning",
                        Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void initScanner() {
        barcodeView = new DecoratedBarcodeView(this);
        android.widget.FrameLayout container = findViewById(R.id.scannerContainer);
        container.addView(barcodeView);
        barcodeView.decodeContinuous(callback);
        barcodeView.resume();
    }

    private final BarcodeCallback callback = new BarcodeCallback() {
        @Override
        public void barcodeResult(BarcodeResult result) {
            if (scanHandled) return;
            scanHandled = true;

            if (barcodeView != null) {
                barcodeView.pause();
            }

            processQRResult(result.getText());
        }
    };

    private void processQRResult(String qrContent) {
        JSONObject data = QRCodeHelper.parseQRData(qrContent);

        if (data == null) {
            showResult(false, "Invalid QR Code", "This is not a valid attendance QR code.");
            return;
        }

        try {
            int subjectId = data.getInt("subjectId");
            String sessionId = data.getString("sessionId");
            long timestamp = data.getLong("timestamp");

            // Check session validity (5 min)
            if (!QRCodeHelper.isSessionValid(timestamp)) {
                showResult(false, "Session Expired",
                        "This QR code has expired. QR is valid for only 5 minutes.");
                return;
            }

            int studentId = sessionManager.getUserId();

            // Check duplicate
            if (db.attendanceDao().isAlreadyMarked(studentId, subjectId, sessionId) > 0) {
                showResult(false, "Already Marked",
                        "Your attendance has already been recorded for this session.");
                return;
            }

            // Mark attendance
            int attendanceId = Math.abs((studentId + "_" + subjectId + "_" + sessionId).hashCode());
            if (attendanceId == 0) attendanceId = 1;

            Attendance attendance = new Attendance(
                    subjectId, studentId,
                    DateUtils.getCurrentDate(),
                    DateUtils.getCurrentTime(),
                    1, sessionId
            );
            attendance.id = attendanceId;

            long id = db.attendanceDao().insert(attendance);
            if (id > 0) {
                try {
                    com.google.firebase.database.FirebaseDatabase.getInstance().getReference()
                        .child("attendance")
                        .child(String.valueOf(attendanceId))
                        .setValue(attendance);
                } catch (Exception e) {}
                
                Subject subject = db.subjectDao().getSubjectById(subjectId);
                String subName = subject != null ? subject.name : "Subject";
                showResult(true, "Attendance Marked!",
                        subName + " • " + DateUtils.formatDateForDisplay(DateUtils.getCurrentDate()));
            } else {
                showResult(false, "Error", "Could not mark attendance. Please try again.");
            }

        } catch (Exception e) {
            showResult(false, "Error", "Invalid QR data format.");
        }
    }

    private void showResult(boolean success, String message, String detail) {
        cardResult.setVisibility(View.VISIBLE);
        tvResultIcon.setText(success ? "✅" : "❌");
        tvResultMessage.setText(message);
        tvResultMessage.setTextColor(getColor(success ? R.color.accent : R.color.chart_red));
        tvResultDetail.setText(detail);

        AnimationHelper.scaleUp(cardResult);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (barcodeView != null && !scanHandled) {
            barcodeView.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (barcodeView != null) {
            barcodeView.pause();
        }
    }
}
