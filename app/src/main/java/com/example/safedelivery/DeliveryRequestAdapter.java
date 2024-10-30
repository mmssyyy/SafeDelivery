package com.example.safedelivery;

import android.content.Intent;
import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Locale;

public class DeliveryRequestAdapter extends RecyclerView.Adapter<DeliveryRequestAdapter.ViewHolder> {

    private List<DeliveryRequest> deliveryRequests;
    private Location currentLocation;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onAcceptClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public DeliveryRequestAdapter(List<DeliveryRequest> deliveryRequests, Location currentLocation) {
        this.deliveryRequests = deliveryRequests;
        this.currentLocation = currentLocation;
    }

    public void updateCurrentLocation(Location location) {
        this.currentLocation = location;
        notifyDataSetChanged();
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

        if (currentLocation != null) {
            // 거리 계산
            String pickupDistance = calculateDistance(
                    currentLocation.getLatitude(),
                    currentLocation.getLongitude(),
                    request.getPickupLat(),
                    request.getPickupLng()
            );

            String deliveryDistance = calculateDistance(
                    currentLocation.getLatitude(),
                    currentLocation.getLongitude(),
                    request.getDeliveryLat(),
                    request.getDeliveryLng()
            );

            holder.tvPickupLocation.setText(String.format("픽업: %s (%.1fkm)",
                    request.getPickupLocation(),
                    Float.parseFloat(pickupDistance)));

            holder.tvDeliveryLocation.setText(String.format("배달: %s (%.1fkm)",
                    request.getDeliveryLocation(),
                    Float.parseFloat(deliveryDistance)));
        } else {
            // 위치 정보가 없는 경우
            holder.tvPickupLocation.setText("픽업: " + request.getPickupLocation());
            holder.tvDeliveryLocation.setText("배달: " + request.getDeliveryLocation());
        }

        holder.tvFee.setText("배달료: " + request.getFee() + "원");

        holder.btnStartDelivery.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), NavigationActivity.class);
            intent.putExtra("deliveryId", request.getId());
            intent.putExtra("pickup", String.format(Locale.US, "%f,%f",
                    request.getPickupLat(), request.getPickupLng()));
            intent.putExtra("destination", String.format(Locale.US, "%f,%f",
                    request.getDeliveryLat(), request.getDeliveryLng()));
            v.getContext().startActivity(intent);
        });
    }

    private String calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // 지구의 반지름 (km)
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return String.format("%.1f", R * c);
    }

    @Override
    public int getItemCount() {
        return deliveryRequests.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPickupLocation, tvDeliveryLocation, tvFee;
        Button btnStartDelivery;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPickupLocation = itemView.findViewById(R.id.tvPickupLocation);
            tvDeliveryLocation = itemView.findViewById(R.id.tvDeliveryLocation);
            tvFee = itemView.findViewById(R.id.tvFee);
            btnStartDelivery = itemView.findViewById(R.id.btnStartDelivery);
        }
    }
}