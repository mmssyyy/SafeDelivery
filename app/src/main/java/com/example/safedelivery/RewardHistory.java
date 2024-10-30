package com.example.safedelivery;

public class RewardHistory {
    private String userId;
    private String rewardName;
    private int pointCost;
    private String receiveInfo;
    private long timestamp;

    // Firebase를 위한 기본 생성자
    public RewardHistory() {}

    public RewardHistory(String userId, String rewardName, int pointCost, String receiveInfo, long timestamp) {
        this.userId = userId;
        this.rewardName = rewardName;
        this.pointCost = pointCost;
        this.receiveInfo = receiveInfo;
        this.timestamp = timestamp;
    }

    // Getter 메서드들
    public String getUserId() {
        return userId;
    }

    public String getRewardName() {
        return rewardName;
    }

    public int getPointCost() {
        return pointCost;
    }

    public String getReceiveInfo() {
        return receiveInfo;
    }

    public long getTimestamp() {
        return timestamp;
    }

    // Setter 메서드들
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setRewardName(String rewardName) {
        this.rewardName = rewardName;
    }

    public void setPointCost(int pointCost) {
        this.pointCost = pointCost;
    }

    public void setReceiveInfo(String receiveInfo) {
        this.receiveInfo = receiveInfo;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}