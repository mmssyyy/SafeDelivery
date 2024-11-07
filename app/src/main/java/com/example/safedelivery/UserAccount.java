package com.example.safedelivery;

/**
 *  사용자 계정 정보 모델 클래스
 */
public class UserAccount {
    private String idToken; // Firebase Uid (고유 토큰정보)
    private String emailId; // 이메일 아이디
    private String password; // 비밀번호
    private String name;

    private int totalSafetyScore;
    private int totalDrivingTime;
    private int totalDeliveries;
    private double averageSafetyScore;

    private int points;

    public UserAccount() { }

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }

    public String getEmailId() {
        return emailId;
    }

    public void setEmailId(String emailId) {
        this.emailId = emailId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPoints() {
        return points;
    }

    public int getTotalSafetyScore() { return totalSafetyScore; }
    public void setTotalSafetyScore(int totalSafetyScore) {
        this.totalSafetyScore = totalSafetyScore;
    }

    public int getTotalDeliveries() {
        return totalDeliveries;
    }

    public void setTotalDeliveries(int totalDeliveries) {
        this.totalDeliveries = totalDeliveries;
    }

    public int getTotalDrivingTime() { return totalDrivingTime; }
    public void setTotalDrivingTime(int totalDrivingTime) {
        this.totalDrivingTime = totalDrivingTime;
    }

    public double getAverageSafetyScore() { return averageSafetyScore; }
    public void setAverageSafetyScore(double averageSafetyScore) {
        this.averageSafetyScore = averageSafetyScore;
    }

    public void setPoints(int points) {
        this.points = points;
    }

}
