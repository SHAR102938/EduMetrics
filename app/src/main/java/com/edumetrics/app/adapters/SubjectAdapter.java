package com.edumetrics.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.edumetrics.app.R;
import com.edumetrics.app.database.EduMetricsDatabase;
import com.edumetrics.app.database.entities.Subject;
import com.edumetrics.app.database.entities.User;
import com.edumetrics.app.utils.AnimationHelper;

import java.util.List;

public class SubjectAdapter extends RecyclerView.Adapter<SubjectAdapter.ViewHolder> {

    private final List<Subject> subjects;
    private final EduMetricsDatabase db;
    private final int studentId;
    private final OnSubjectClickListener listener;

    public interface OnSubjectClickListener {
        void onClick(Subject subject);
    }

    public SubjectAdapter(List<Subject> subjects, EduMetricsDatabase db,
                          int studentId, OnSubjectClickListener listener) {
        this.subjects = subjects;
        this.db = db;
        this.studentId = studentId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_subject, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Subject subject = subjects.get(position);
        holder.tvSubjectName.setText(subject.name);

        // Faculty name
        User faculty = db.userDao().getUserById(subject.facultyId);
        holder.tvFacultyName.setText(faculty != null ? faculty.name : "");

        // Attendance %
        if (studentId > 0) {
            int present = db.attendanceDao().getPresentCount(studentId, subject.id);
            int total = db.attendanceDao().getTotalCount(studentId, subject.id);
            int pct = total > 0 ? (present * 100 / total) : 0;
            holder.tvAttendance.setText("Att: " + pct + "%");

            float avgScore = db.scoreDao().getAverageScore(studentId, subject.id);
            holder.tvScore.setText("Score: " + String.format("%.0f", avgScore) + "%");
        } else {
            holder.tvAttendance.setText("Att: 0%");
            holder.tvScore.setText("Score: 0%");
        }

        if (listener != null) {
            holder.itemView.setOnClickListener(v -> listener.onClick(subject));
        }

        AnimationHelper.staggeredSlideUp(holder.itemView, position);
    }

    @Override
    public int getItemCount() { return subjects.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSubjectName, tvFacultyName, tvAttendance, tvScore;

        ViewHolder(View view) {
            super(view);
            tvSubjectName = view.findViewById(R.id.tvSubjectName);
            tvFacultyName = view.findViewById(R.id.tvFacultyName);
            tvAttendance = view.findViewById(R.id.tvAttendance);
            tvScore = view.findViewById(R.id.tvScore);
        }
    }
}
