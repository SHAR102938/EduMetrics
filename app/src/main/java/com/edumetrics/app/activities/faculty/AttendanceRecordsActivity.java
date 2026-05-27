package com.edumetrics.app.activities.faculty;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.edumetrics.app.R;
import com.edumetrics.app.adapters.AttendanceAdapter;
import com.edumetrics.app.database.EduMetricsDatabase;
import com.edumetrics.app.database.entities.Attendance;
import com.edumetrics.app.database.entities.Subject;
import com.edumetrics.app.utils.DateUtils;
import com.edumetrics.app.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class AttendanceRecordsActivity extends AppCompatActivity {

    private Spinner spinnerSubject;
    private RecyclerView rvDates, rvStudents;
    private TextView tvDateHeader;
    private EduMetricsDatabase db;
    private SessionManager sessionManager;
    private List<Subject> subjects = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_records);

        db = EduMetricsDatabase.getInstance(this);
        sessionManager = new SessionManager(this);

        spinnerSubject = findViewById(R.id.spinnerSubject);
        rvDates = findViewById(R.id.rvDates);
        rvStudents = findViewById(R.id.rvStudents);
        tvDateHeader = findViewById(R.id.tvDateHeader);

        rvDates.setLayoutManager(new LinearLayoutManager(this));
        rvStudents.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        syncAttendanceFromCloud();
    }

    private void syncAttendanceFromCloud() {
        com.google.firebase.database.FirebaseDatabase.getInstance().getReference("attendance")
            .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(@androidx.annotation.NonNull com.google.firebase.database.DataSnapshot snapshot) {
                    new Thread(() -> {
                        for (com.google.firebase.database.DataSnapshot ds : snapshot.getChildren()) {
                            try {
                                Attendance a = ds.getValue(Attendance.class);
                                if (a != null && db.attendanceDao().isAlreadyMarked(a.studentId, a.subjectId, a.qrSessionId) == 0) {
                                    db.attendanceDao().insert(a);
                                }
                            } catch (Exception ignored) {}
                        }
                        runOnUiThread(() -> loadSubjects());
                    }).start();
                }
                @Override
                public void onCancelled(@androidx.annotation.NonNull com.google.firebase.database.DatabaseError error) {}
            });
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

        spinnerSubject.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                if (!subjects.isEmpty()) loadDates(subjects.get(position).id);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadDates(int subjectId) {
        List<String> dates = db.attendanceDao().getAttendanceDates(subjectId);

        // Simple date adapter
        List<String> displayDates = new ArrayList<>();
        for (String date : dates) {
            List<Attendance> records = db.attendanceDao().getAttendanceBySubjectAndDate(subjectId, date);
            long presentCount = records.stream().filter(a -> a.present == 1).count();
            displayDates.add(DateUtils.formatDateForDisplay(date) + " (" + presentCount + "/" + records.size() + ")");
        }

        ArrayAdapter<String> dateAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, displayDates);

        // Use a simple adapter for dates
        rvDates.setAdapter(new SimpleTextAdapter(displayDates, position -> {
            if (position < dates.size()) {
                loadStudentsForDate(subjectId, dates.get(position));
            }
        }));
    }

    private void loadStudentsForDate(int subjectId, String date) {
        tvDateHeader.setText("Attendance for " + DateUtils.formatDateForDisplay(date));
        List<Attendance> records = db.attendanceDao().getAttendanceBySubjectAndDate(subjectId, date);
        AttendanceAdapter adapter = new AttendanceAdapter(records, db);
        rvStudents.setAdapter(adapter);
    }

    // Simple inner adapter for date list
    static class SimpleTextAdapter extends RecyclerView.Adapter<SimpleTextAdapter.VH> {
        private final List<String> items;
        private final OnItemClickListener listener;

        interface OnItemClickListener { void onClick(int position); }

        SimpleTextAdapter(List<String> items, OnItemClickListener listener) {
            this.items = items;
            this.listener = listener;
        }

        @androidx.annotation.NonNull
        @Override
        public VH onCreateViewHolder(@androidx.annotation.NonNull android.view.ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            tv.setPadding(32, 24, 32, 24);
            tv.setTextColor(parent.getContext().getColor(R.color.text_primary));
            tv.setTextSize(14);
            tv.setBackgroundResource(R.drawable.bg_card);
            android.view.ViewGroup.MarginLayoutParams params = new android.view.ViewGroup.MarginLayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            params.bottomMargin = 8;
            tv.setLayoutParams(params);
            return new VH(tv);
        }

        @Override
        public void onBindViewHolder(@androidx.annotation.NonNull VH holder, int position) {
            ((TextView) holder.itemView).setText(items.get(position));
            holder.itemView.setOnClickListener(v -> listener.onClick(position));
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            VH(android.view.View v) { super(v); }
        }
    }
}
