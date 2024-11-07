package com.example.safedelivery;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PointHistoryAdapter extends RecyclerView.Adapter<PointHistoryAdapter.ViewHolder> {
    private List<PointHistory> historyList;
    private SimpleDateFormat dateFormat;

    public PointHistoryAdapter() {
        this.historyList = new ArrayList<>();
        this.dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_point_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PointHistory history = historyList.get(position);
        holder.tvDescription.setText(history.getDescription());
        holder.tvDate.setText(dateFormat.format(new Date(history.getTimestamp())));

        String pointText = (history.getType().equals("적립") ? "+" : "-") +
                history.getPoints() + "P";
        holder.tvPoints.setText(pointText);
        holder.tvPoints.setTextColor(history.getType().equals("적립") ?
                Color.BLUE : Color.RED);
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public void setItems(List<PointHistory> items) {
        this.historyList = items;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDescription, tvDate, tvPoints;

        ViewHolder(View view) {
            super(view);
            tvDescription = view.findViewById(R.id.tvDescription);
            tvDate = view.findViewById(R.id.tvDate);
            tvPoints = view.findViewById(R.id.tvPoints);
        }
    }
}