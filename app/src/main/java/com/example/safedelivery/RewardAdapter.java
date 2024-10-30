package com.example.safedelivery;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class RewardAdapter extends RecyclerView.Adapter<RewardAdapter.ViewHolder> {
    private List<RewardItem> rewards;
    private OnRewardClickListener listener;

    public interface OnRewardClickListener {
        void onRewardClick(RewardItem reward);
    }

    public RewardAdapter(List<RewardItem> rewards, OnRewardClickListener listener) {
        this.rewards = rewards;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reward, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RewardItem reward = rewards.get(position);
        holder.ivIcon.setImageResource(reward.getIconResId());
        holder.tvName.setText(reward.getName());
        holder.tvDescription.setText(reward.getDescription());
        holder.tvPointCost.setText(reward.getPointCost() + "P");

        holder.btnExchange.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRewardClick(reward);
            }
        });
    }

    @Override
    public int getItemCount() {
        return rewards.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvName, tvDescription, tvPointCost;
        Button btnExchange;

        ViewHolder(View view) {
            super(view);
            ivIcon = view.findViewById(R.id.ivIcon);
            tvName = view.findViewById(R.id.tvName);
            tvDescription = view.findViewById(R.id.tvDescription);
            tvPointCost = view.findViewById(R.id.tvPointCost);
            btnExchange = view.findViewById(R.id.btnExchange);
        }
    }
}