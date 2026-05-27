package com.edumetrics.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.edumetrics.app.R;
import com.edumetrics.app.database.EduMetricsDatabase;
import com.edumetrics.app.database.entities.ClassEntity;
import com.edumetrics.app.utils.AnimationHelper;

import java.util.List;

public class ClassAdapter extends RecyclerView.Adapter<ClassAdapter.ViewHolder> {

    private final List<ClassEntity> classes;
    private final EduMetricsDatabase db;
    private final OnClassClickListener listener;

    public interface OnClassClickListener {
        void onClick(ClassEntity classEntity);
    }

    public ClassAdapter(List<ClassEntity> classes, EduMetricsDatabase db,
                        OnClassClickListener listener) {
        this.classes = classes;
        this.db = db;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_class, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ClassEntity classEntity = classes.get(position);
        holder.tvClassName.setText(classEntity.name);
        holder.tvClassCode.setText("Code: " + classEntity.classCode);

        int count = db.classDao().getStudentCountInClass(classEntity.id);
        holder.tvStudentCount.setText(count + " students");

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(classEntity);
        });

        AnimationHelper.staggeredSlideUp(holder.itemView, position);
    }

    @Override
    public int getItemCount() { return classes.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvClassName, tvClassCode, tvStudentCount;

        ViewHolder(View view) {
            super(view);
            tvClassName = view.findViewById(R.id.tvClassName);
            tvClassCode = view.findViewById(R.id.tvClassCode);
            tvStudentCount = view.findViewById(R.id.tvStudentCount);
        }
    }
}
