package com.example.safedelivery;

import com.google.firebase.database.IgnoreExtraProperties;
import com.naver.maps.geometry.LatLng;

@IgnoreExtraProperties
public class DeliveryRequest {
    private String id;
    private String pickupLocation;
    private String deliveryLocation;
    private String status;

    private String userId;  // 추가

    private double fee;
    private double pickupLat;
    private double pickupLng;
    private double deliveryLat;
    private double deliveryLng;

    private long completedTime;

    // 기본 생성자
    public DeliveryRequest() {}

    // 기존 생성자
    public DeliveryRequest(String id, String pickupLocation, String deliveryLocation, String status, double fee) {
        this.id = id;
        this.pickupLocation = pickupLocation;
        this.deliveryLocation = deliveryLocation;
        this.status = status;
        this.fee = fee;
    }

    // 위치 좌표를 포함한 새로운 생성자
    public DeliveryRequest(String id, String pickupLocation, String deliveryLocation,
                           String status, double fee,
                           double pickupLat, double pickupLng,
                           double deliveryLat, double deliveryLng) {
        this.id = id;
        this.pickupLocation = pickupLocation;
        this.deliveryLocation = deliveryLocation;
        this.status = status;
        this.fee = fee;
        this.pickupLat = pickupLat;
        this.pickupLng = pickupLng;
        this.deliveryLat = deliveryLat;
        this.deliveryLng = deliveryLng;
    }

    // 기존 Getter와 Setter
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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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

    // 새로운 위치 좌표 관련 Getter와 Setter
    public double getPickupLat() {
        return pickupLat;
    }

    public void setPickupLat(double pickupLat) {
        this.pickupLat = pickupLat;
    }

    public double getPickupLng() {
        return pickupLng;
    }

    public void setPickupLng(double pickupLng) {
        this.pickupLng = pickupLng;
    }

    public double getDeliveryLat() {
        return deliveryLat;
    }

    public void setDeliveryLat(double deliveryLat) {
        this.deliveryLat = deliveryLat;
    }

    public double getDeliveryLng() {
        return deliveryLng;
    }

    public void setDeliveryLng(double deliveryLng) {
        this.deliveryLng = deliveryLng;
    }

    // 편의 메서드: 픽업 위치를 LatLng 객체로 반환
    public LatLng getPickupLatLng() {
        return new LatLng(pickupLat, pickupLng);
    }

    // 편의 메서드: 배달 위치를 LatLng 객체로 반환
    public LatLng getDeliveryLatLng() {
        return new LatLng(deliveryLat, deliveryLng);
    }

    public long getCompletedTime() {
        return completedTime;
    }

    public void setCompletedTime(long completedTime) {
        this.completedTime = completedTime;
    }
}