package com.example.safedelivery;

public class SafetyScore {
    private int score;
    private int totalPoints;
    private long timestamp;
    private SafetyMetrics metrics;

    // Firebase를 위한 빈 생성자
    public SafetyScore() {}

    public SafetyScore(int score, int totalPoints, SafetyMetrics metrics) {
        this.score = score;
        this.totalPoints = totalPoints;
        this.timestamp = System.currentTimeMillis();
        this.metrics = metrics;
    }

    // getter와 setter
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public int getTotalPoints() { return totalPoints; }
    public void setTotalPoints(int totalPoints) { this.totalPoints = totalPoints; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public SafetyMetrics getMetrics() { return metrics; }
    public void setMetrics(SafetyMetrics metrics) { this.metrics = metrics; }
}