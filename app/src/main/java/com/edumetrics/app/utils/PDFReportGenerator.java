package com.edumetrics.app.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Environment;

import androidx.core.content.FileProvider;

import com.edumetrics.app.database.EduMetricsDatabase;
import com.edumetrics.app.database.entities.*;
import com.edumetrics.app.analytics.ConsistencyScoreCalculator;
import com.edumetrics.app.analytics.DisciplineIndexCalculator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class PDFReportGenerator {

    private static final int PAGE_WIDTH = 595;  // A4
    private static final int PAGE_HEIGHT = 842;
    private static final int MARGIN = 40;
    private static final int LINE_HEIGHT = 20;

    private final Context context;
    private final EduMetricsDatabase db;

    public PDFReportGenerator(Context context) {
        this.context = context;
        this.db = EduMetricsDatabase.getInstance(context);
    }

    /**
     * Generate individual student report PDF.
     */
    public File generateStudentReport(int studentId) throws IOException {
        User student = db.userDao().getUserById(studentId);
        if (student == null) return null;

        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        int y = MARGIN;

        // Header
        Paint titlePaint = new Paint();
        titlePaint.setColor(Color.parseColor("#00C853"));
        titlePaint.setTextSize(24);
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("EduMetrics - Student Report", MARGIN, y += 30, titlePaint);

        // Divider
        Paint linePaint = new Paint();
        linePaint.setColor(Color.parseColor("#00C853"));
        linePaint.setStrokeWidth(2);
        canvas.drawLine(MARGIN, y += 10, PAGE_WIDTH - MARGIN, y, linePaint);

        // Student Info
        Paint bodyPaint = new Paint();
        bodyPaint.setColor(Color.BLACK);
        bodyPaint.setTextSize(14);

        canvas.drawText("Name: " + student.name, MARGIN, y += LINE_HEIGHT + 10, bodyPaint);
        canvas.drawText("Email: " + student.email, MARGIN, y += LINE_HEIGHT, bodyPaint);
        canvas.drawText("Generated: " + DateUtils.getCurrentDate(), MARGIN, y += LINE_HEIGHT, bodyPaint);

        y += 10;
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint);

        // Analytics
        ConsistencyScoreCalculator csCalc = new ConsistencyScoreCalculator(db);
        DisciplineIndexCalculator diCalc = new DisciplineIndexCalculator(db);
        float consistencyScore = csCalc.calculate(studentId);
        float disciplineIndex = diCalc.calculate(studentId);

        Paint headerPaint = new Paint();
        headerPaint.setColor(Color.parseColor("#00C853"));
        headerPaint.setTextSize(16);
        headerPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        canvas.drawText("Analytics Overview", MARGIN, y += LINE_HEIGHT + 5, headerPaint);

        int totalPresent = db.attendanceDao().getTotalPresentCount(studentId);
        int totalClasses = db.attendanceDao().getTotalAttendanceCount(studentId);
        float attendancePct = totalClasses > 0 ? (totalPresent * 100.0f / totalClasses) : 0;

        canvas.drawText("Overall Attendance: " + String.format("%.1f%%", attendancePct), MARGIN, y += LINE_HEIGHT + 5, bodyPaint);
        canvas.drawText("Consistency Score: " + String.format("%.1f", consistencyScore), MARGIN, y += LINE_HEIGHT, bodyPaint);
        canvas.drawText("Discipline Index: " + String.format("%.1f", disciplineIndex) +
                " (" + DisciplineIndexCalculator.getLabel(disciplineIndex) + ")", MARGIN, y += LINE_HEIGHT, bodyPaint);

        y += 10;
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint);

        // Subject-wise breakdown
        canvas.drawText("Subject-wise Breakdown", MARGIN, y += LINE_HEIGHT + 5, headerPaint);

        List<Subject> subjects = db.subjectDao().getSubjectsByStudent(studentId);
        for (Subject subject : subjects) {
            if (y + LINE_HEIGHT * 4 > PAGE_HEIGHT - MARGIN) break; // Prevent overflow

            int present = db.attendanceDao().getPresentCount(studentId, subject.id);
            int total = db.attendanceDao().getTotalCount(studentId, subject.id);
            float pct = total > 0 ? (present * 100.0f / total) : 0;

            Paint subjectPaint = new Paint();
            subjectPaint.setColor(Color.DKGRAY);
            subjectPaint.setTextSize(13);
            subjectPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

            canvas.drawText("• " + subject.name, MARGIN + 10, y += LINE_HEIGHT + 5, subjectPaint);
            canvas.drawText("  Attendance: " + String.format("%.1f%%", pct) + " (" + present + "/" + total + ")", MARGIN + 20, y += LINE_HEIGHT, bodyPaint);

            List<Score> scores = db.scoreDao().getScoresByStudentAndSubject(studentId, subject.id);
            for (Score score : scores) {
                if (y + LINE_HEIGHT > PAGE_HEIGHT - MARGIN) break;
                canvas.drawText("  " + score.scoreType + ": " + score.marks + "/" + score.maxMarks, MARGIN + 20, y += LINE_HEIGHT, bodyPaint);
            }
        }

        document.finishPage(page);

        // Save file
        File dir = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "EduMetrics");
        if (!dir.exists()) dir.mkdirs();

        String filename = "StudentReport_" + student.name.replace(" ", "_") + "_" + DateUtils.getCurrentDate() + ".pdf";
        File file = new File(dir, filename);
        FileOutputStream fos = new FileOutputStream(file);
        document.writeTo(fos);
        fos.close();
        document.close();

        return file;
    }

    /**
     * Generate class report PDF for faculty.
     */
    public File generateClassReport(int classId) throws IOException {
        ClassEntity classEntity = db.classDao().getClassById(classId);
        if (classEntity == null) return null;

        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        int y = MARGIN;

        // Header
        Paint titlePaint = new Paint();
        titlePaint.setColor(Color.parseColor("#00C853"));
        titlePaint.setTextSize(24);
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("EduMetrics - Class Report", MARGIN, y += 30, titlePaint);

        Paint linePaint = new Paint();
        linePaint.setColor(Color.parseColor("#00C853"));
        linePaint.setStrokeWidth(2);
        canvas.drawLine(MARGIN, y += 10, PAGE_WIDTH - MARGIN, y, linePaint);

        Paint bodyPaint = new Paint();
        bodyPaint.setColor(Color.BLACK);
        bodyPaint.setTextSize(14);

        canvas.drawText("Class: " + classEntity.name, MARGIN, y += LINE_HEIGHT + 10, bodyPaint);
        canvas.drawText("Code: " + classEntity.classCode, MARGIN, y += LINE_HEIGHT, bodyPaint);
        canvas.drawText("Generated: " + DateUtils.getCurrentDate(), MARGIN, y += LINE_HEIGHT, bodyPaint);

        y += 10;
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint);

        // Table header
        Paint headerPaint = new Paint();
        headerPaint.setColor(Color.WHITE);
        headerPaint.setTextSize(12);
        headerPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        Paint bgPaint = new Paint();
        bgPaint.setColor(Color.parseColor("#00C853"));

        y += 5;
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 25, bgPaint);
        canvas.drawText("Student", MARGIN + 5, y + 17, headerPaint);
        canvas.drawText("Attendance", 200, y + 17, headerPaint);
        canvas.drawText("Consistency", 320, y + 17, headerPaint);
        canvas.drawText("Discipline", 440, y + 17, headerPaint);
        y += 25;

        // Student rows
        List<User> students = db.studentClassDao().getStudentsInClass(classId);
        ConsistencyScoreCalculator csCalc = new ConsistencyScoreCalculator(db);
        DisciplineIndexCalculator diCalc = new DisciplineIndexCalculator(db);

        Paint rowPaint = new Paint();
        rowPaint.setColor(Color.BLACK);
        rowPaint.setTextSize(11);

        for (User student : students) {
            if (y + LINE_HEIGHT > PAGE_HEIGHT - MARGIN) break;

            int totalPresent = db.attendanceDao().getTotalPresentCount(student.id);
            int totalCount = db.attendanceDao().getTotalAttendanceCount(student.id);
            float attPct = totalCount > 0 ? (totalPresent * 100.0f / totalCount) : 0;
            float cs = csCalc.calculate(student.id);
            float di = diCalc.calculate(student.id);

            String name = student.name.length() > 20 ? student.name.substring(0, 20) + "..." : student.name;
            canvas.drawText(name, MARGIN + 5, y += LINE_HEIGHT, rowPaint);
            canvas.drawText(String.format("%.1f%%", attPct), 200, y, rowPaint);
            canvas.drawText(String.format("%.1f", cs), 320, y, rowPaint);
            canvas.drawText(String.format("%.1f", di), 440, y, rowPaint);
        }

        document.finishPage(page);

        // Save
        File dir = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "EduMetrics");
        if (!dir.exists()) dir.mkdirs();

        String filename = "ClassReport_" + classEntity.name.replace(" ", "_") + "_" + DateUtils.getCurrentDate() + ".pdf";
        File file = new File(dir, filename);
        FileOutputStream fos = new FileOutputStream(file);
        document.writeTo(fos);
        fos.close();
        document.close();

        return file;
    }

    /**
     * Share a PDF file via Intent.
     */
    public void sharePDF(File file) {
        Uri uri = FileProvider.getUriForFile(context,
                context.getPackageName() + ".fileprovider", file);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(intent, "Share Report"));
    }
}
