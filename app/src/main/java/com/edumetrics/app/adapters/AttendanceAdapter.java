package com.edumetrics.app.adapters;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.edumetrics.app.R;
import com.edumetrics.app.database.EduMetricsDatabase;
import com.edumetrics.app.database.entities.Attendance;
import com.edumetrics.app.database.entities.User;

import java.util.List;

public class AttendanceAdapter extends RecyclerView.Adapter<AttendanceAdapter.ViewHolder> {

    private final List<Attendance> records;
    private final EduMetricsDatabase db;

    public AttendanceAdapter(List<Attendance> records, EduMetricsDatabase db) {
        this.records = records;
        this.db = db;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_attendance, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Attendance record = records.get(position);

        User student = db.userDao().getUserById(record.studentId);
        holder.tvStudentName.setText(student != null ? student.name : "Unknown");

        boolean isPresent = record.present == 1;
        holder.tvStatus.setText(isPresent ? "Present" : "Absent");
        holder.tvStatus.setTextColor(holder.itemView.getContext().getColor(
                isPresent ? R.color.status_present : R.color.status_absent));

        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setColor(holder.itemView.getContext().getColor(
                isPresent ? R.color.status_present : R.color.status_absent));
        holder.viewStatus.setBackground(dot);
    }

    @Override
    public int getItemCount() { return records.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvStudentName, tvStatus;
        View viewStatus;

        ViewHolder(View view) {
            super(view);
            tvStudentName = view.findViewById(R.id.tvStudentName);
            tvStatus = view.findViewById(R.id.tvStatus);
            viewStatus = view.findViewById(R.id.viewStatus);
        }
    }
}
