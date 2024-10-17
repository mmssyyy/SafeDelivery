package com.example.safedelivery;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class DeliveryRequest {
    private String id;
    private String pickupLocation;
    private String deliveryLocation;
    private String status;
    private double fee;

    // 기본 생성자
    public DeliveryRequest() {}

    // 생성자
    public DeliveryRequest(String id, String pickupLocation, String deliveryLocation, String status, double fee) {
        this.id = id;
        this.pickupLocation = pickupLocation;
        this.deliveryLocation = deliveryLocation;
        this.status = status;
        this.fee = fee;
    }

    // Getter와 Setter 메소드들
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPickupLocation() {
        return pickupLocation;
    }

    public void setPickupLocation(String pickupLocation) {
        this.pickupLocation = pickupLocation;
    }

    public String getDeliveryLocation() {
        return deliveryLocation;
    }

    public void setDeliveryLocation(String deliveryLocation) {
        this.deliveryLocation = deliveryLocation;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getFee() {
        return fee;
    }

    public void setFee(double fee) {
        this.fee = fee;
    }
}