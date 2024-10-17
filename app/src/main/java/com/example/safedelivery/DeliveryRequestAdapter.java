package com.example.safedelivery;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class DeliveryRequestAdapter extends RecyclerView.Adapter<DeliveryRequestAdapter.ViewHolder> {

    private List<DeliveryRequest> deliveryRequests;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onAcceptClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public DeliveryRequestAdapter(List<DeliveryRequest> deliveryRequests) {
        this.deliveryRequests = deliveryRequests;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_delivery_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DeliveryRequest request = deliveryRequests.get(position);
        holder.tvDeliveryId.setText("배달 ID: " + request.getId());
        holder.tvPickupLocation.setText("픽업: " + request.getPickupLocation());
        holder.tvDeliveryLocation.setText("배달: " + request.getDeliveryLocation());
        holder.tvFee.setText("배달료: " + request.getFee() + "원");

        holder.btnAccept.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAcceptClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return deliveryRequests.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDeliveryId, tvPickupLocation, tvDeliveryLocation, tvFee;
        Button btnAccept;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDeliveryId = itemView.findViewById(R.id.tvDeliveryId);
            tvPickupLocation = itemView.findViewById(R.id.tvPickupLocation);
            tvDeliveryLocation = itemView.findViewById(R.id.tvDeliveryLocation);
            tvFee = itemView.findViewById(R.id.tvFee);
            btnAccept = itemView.findViewById(R.id.btnAccept);
        }
    }
}