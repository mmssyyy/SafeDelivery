package com.example.safedelivery;
public class PointHistory {
    private String id;
    private String userId;
    private int points;
    private String description;
    private long timestamp;
    private String type; // "적립" 또는 "사용"

    public PointHistory() {} // Firebase를 위한 빈 생성자

    public PointHistory(String userId, int points, String description, String type) {
        this.userId = userId;
        this.points = points;
        this.description = description;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }

    // Getter와 Setter 메서드들
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}