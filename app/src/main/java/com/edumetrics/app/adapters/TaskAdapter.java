package com.edumetrics.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.edumetrics.app.R;
import com.edumetrics.app.database.EduMetricsDatabase;
import com.edumetrics.app.database.entities.Task;
import com.edumetrics.app.database.entities.TaskStatus;
import com.edumetrics.app.utils.AnimationHelper;
import com.edumetrics.app.utils.DateUtils;

import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.ViewHolder> {

    private final List<Task> tasks;
    private final EduMetricsDatabase db;
    private final int studentId;
    private final OnTaskActionListener listener;

    public interface OnTaskActionListener {
        void onToggleComplete(Task task, boolean completed);
    }

    public TaskAdapter(List<Task> tasks, EduMetricsDatabase db,
                       int studentId, OnTaskActionListener listener) {
        this.tasks = tasks;
        this.db = db;
        this.studentId = studentId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Task task = tasks.get(position);
        holder.tvTaskTitle.setText(task.title);
        holder.tvTaskDesc.setText(task.description != null && !task.description.isEmpty() ?
                task.description : "No description");

        // Type badge
        String typeLabel = task.type != null ?
                task.type.substring(0, 1).toUpperCase() + task.type.substring(1) : "";
        holder.tvTaskType.setText(typeLabel);

        // Due date
        if (task.dueDate != null && !task.dueDate.isEmpty()) {
            holder.tvDueDate.setText("Due: " + DateUtils.formatDateForDisplay(task.dueDate));
        } else {
            holder.tvDueDate.setText("");
        }

        // Completion status
        if (studentId > 0) {
            TaskStatus status = db.taskDao().getTaskStatus(task.id, studentId);
            boolean isCompleted = status != null && status.completed == 1;
            holder.cbCompleted.setChecked(isCompleted);

            holder.cbCompleted.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) {
                    listener.onToggleComplete(task, isChecked);
                }
            });
        } else {
            holder.cbCompleted.setVisibility(View.GONE);
        }

        AnimationHelper.staggeredSlideUp(holder.itemView, position);
    }

    @Override
    public int getItemCount() { return tasks.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTaskTitle, tvTaskDesc, tvTaskType, tvDueDate;
        CheckBox cbCompleted;

        ViewHolder(View view) {
            super(view);
            tvTaskTitle = view.findViewById(R.id.tvTaskTitle);
            tvTaskDesc = view.findViewById(R.id.tvTaskDesc);
            tvTaskType = view.findViewById(R.id.tvTaskType);
            tvDueDate = view.findViewById(R.id.tvDueDate);
            cbCompleted = view.findViewById(R.id.cbCompleted);
        }
    }
}
