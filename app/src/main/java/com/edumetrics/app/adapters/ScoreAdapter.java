package com.edumetrics.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.edumetrics.app.R;
import com.edumetrics.app.database.entities.Score;

import java.util.List;

public class ScoreAdapter extends RecyclerView.Adapter<ScoreAdapter.ViewHolder> {

    private final List<Score> scores;

    public ScoreAdapter(List<Score> scores) {
        this.scores = scores;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_score, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Score score = scores.get(position);

        String typeLabel = score.scoreType != null ?
                score.scoreType.substring(0, 1).toUpperCase() + score.scoreType.substring(1) : "";
        holder.tvScoreType.setText(typeLabel);
        holder.tvMarks.setText(String.format("%.0f/%.0f", score.marks, score.maxMarks));
    }

    @Override
    public int getItemCount() { return scores.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvScoreType, tvMarks;

        ViewHolder(View view) {
            super(view);
            tvScoreType = view.findViewById(R.id.tvScoreType);
            tvMarks = view.findViewById(R.id.tvMarks);
        }
    }
}
