package com.edumetrics.app.utils;

import android.graphics.Bitmap;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import org.json.JSONObject;

public class QRCodeHelper {

    /**
     * Generate a QR code bitmap for attendance session.
     * QR data format: JSON with subjectId, sessionId, timestamp
     */
    public static Bitmap generateQRCode(int subjectId, String sessionId, long timestamp, int size) {
        try {
            JSONObject qrData = new JSONObject();
            qrData.put("subjectId", subjectId);
            qrData.put("sessionId", sessionId);
            qrData.put("timestamp", timestamp);

            MultiFormatWriter writer = new MultiFormatWriter();
            BitMatrix bitMatrix = writer.encode(qrData.toString(), BarcodeFormat.QR_CODE, size, size);
            BarcodeEncoder encoder = new BarcodeEncoder();
            return encoder.createBitmap(bitMatrix);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Generate a unique session ID based on subject and current time.
     */
    public static String generateSessionId(int subjectId) {
        return "SES_" + subjectId + "_" + System.currentTimeMillis();
    }

    /**
     * Parse QR code data and validate session.
     * Returns parsed JSONObject or null if invalid/expired.
     */
    public static JSONObject parseQRData(String qrContent) {
        try {
            JSONObject data = new JSONObject(qrContent);
            if (data.has("subjectId") && data.has("sessionId") && data.has("timestamp")) {
                return data;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Check if QR session is still valid (within 5 minutes).
     */
    public static boolean isSessionValid(long sessionTimestamp) {
        return DateUtils.isWithinMinutes(sessionTimestamp, 5);
    }
}
