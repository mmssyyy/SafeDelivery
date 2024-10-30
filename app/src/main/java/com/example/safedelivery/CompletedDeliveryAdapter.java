package com.example.safedelivery;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CompletedDeliveryAdapter extends RecyclerView.Adapter<CompletedDeliveryAdapter.ViewHolder> {
    private List<DeliveryRequest> completedDeliveries;

    public CompletedDeliveryAdapter(List<DeliveryRequest> completedDeliveries) {
        this.completedDeliveries = completedDeliveries;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_completed_delivery, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DeliveryRequest request = completedDeliveries.get(position);

        // 날짜 포맷 설정
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        String completedDate = dateFormat.format(new Date(request.getCompletedTime()));

        holder.tvCompletedTime.setText("완료 시간: " + completedDate);
        holder.tvPickupLocation.setText("픽업: " + request.getPickupLocation());
        holder.tvDeliveryLocation.setText("배달: " + request.getDeliveryLocation());
        holder.tvFee.setText("배달료: " + request.getFee() + "원");
    }

    @Override
    public int getItemCount() {
        return completedDeliveries.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCompletedTime, tvPickupLocation, tvDeliveryLocation, tvFee;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCompletedTime = itemView.findViewById(R.id.tvCompletedTime);
            tvPickupLocation = itemView.findViewById(R.id.tvPickupLocation);
            tvDeliveryLocation = itemView.findViewById(R.id.tvDeliveryLocation);
            tvFee = itemView.findViewById(R.id.tvFee);
        }
    }
}