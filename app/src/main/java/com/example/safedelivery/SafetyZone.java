package com.example.safedelivery;

public class SafetyZone {
    private String type;  // SCHOOL, SPEED, DANGER
    private int speedLimit;
    private double latitude;
    private double longitude;
    private String description;

    // 기본 생성자
    public SafetyZone() {}

    // 모든 필드를 포함한 생성자
    public SafetyZone(String type, int speedLimit, double latitude, double longitude, String description) {
        this.type = type;
        this.speedLimit = speedLimit;
        this.latitude = latitude;
        this.longitude = longitude;
        this.description = description;
    }

    // Getter와 Setter
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getSpeedLimit() {
        return speedLimit;
    }

    public void setSpeedLimit(int speedLimit) {
        this.speedLimit = speedLimit;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}