package com.edumetrics.app.adapters;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.edumetrics.app.R;
import com.edumetrics.app.analytics.ConsistencyScoreCalculator;
import com.edumetrics.app.analytics.DisciplineIndexCalculator;
import com.edumetrics.app.database.EduMetricsDatabase;
import com.edumetrics.app.database.entities.User;
import com.edumetrics.app.utils.AnimationHelper;

import java.util.List;

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.ViewHolder> {

    private final List<User> students;
    private final EduMetricsDatabase db;
    private final OnStudentClickListener listener;

    public interface OnStudentClickListener {
        void onClick(User student);
    }

    public StudentAdapter(List<User> students, EduMetricsDatabase db,
                          OnStudentClickListener listener) {
        this.students = students;
        this.db = db;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_student, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User student = students.get(position);
        holder.tvStudentName.setText(student.name);

        // Calculate analytics
        int totalPresent = db.attendanceDao().getTotalPresentCount(student.id);
        int totalClasses = db.attendanceDao().getTotalAttendanceCount(student.id);
        int attPct = totalClasses > 0 ? (totalPresent * 100 / totalClasses) : 0;

        ConsistencyScoreCalculator csCalc = new ConsistencyScoreCalculator(db);
        float cs = csCalc.calculate(student.id);

        DisciplineIndexCalculator diCalc = new DisciplineIndexCalculator(db);
        float di = diCalc.calculate(student.id);

        holder.tvAttendance.setText("Att: " + attPct + "%");
        holder.tvConsistency.setText("CS: " + String.format("%.0f", cs));
        holder.tvDiscipline.setText("DI: " + String.format("%.0f", di));

        // Risk indicator
        String risk = ConsistencyScoreCalculator.getRiskLevel(cs);
        int riskColor;
        switch (risk) {
            case "HIGH":
                riskColor = holder.itemView.getContext().getColor(R.color.risk_high);
                break;
            case "MEDIUM":
                riskColor = holder.itemView.getContext().getColor(R.color.risk_medium);
                break;
            default:
                riskColor = holder.itemView.getContext().getColor(R.color.risk_low);
                break;
        }
        GradientDrawable riskBg = new GradientDrawable();
        riskBg.setShape(GradientDrawable.OVAL);
        riskBg.setColor(riskColor);
        holder.viewRisk.setBackground(riskBg);

        if (listener != null) {
            holder.itemView.setOnClickListener(v -> listener.onClick(student));
        }

        AnimationHelper.staggeredSlideUp(holder.itemView, position);
    }

    @Override
    public int getItemCount() { return students.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvStudentName, tvAttendance, tvConsistency, tvDiscipline;
        View viewRisk;

        ViewHolder(View view) {
            super(view);
            tvStudentName = view.findViewById(R.id.tvStudentName);
            tvAttendance = view.findViewById(R.id.tvAttendance);
            tvConsistency = view.findViewById(R.id.tvConsistency);
            tvDiscipline = view.findViewById(R.id.tvDiscipline);
            viewRisk = view.findViewById(R.id.viewRisk);
        }
    }
}
