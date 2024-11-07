package com.example.safedelivery;

public class SafetyMetrics {
    public int totalDrivingTime;
    public int laneKeepingTime;
    public int speedingTime;
    public int suddenStops;
    public int stopLineTime;
    public int signalViolations;
    public int safeDistanceTime;

    public SafetyMetrics() {
        reset();
    }

    public void reset() {
        totalDrivingTime = 0;
        laneKeepingTime = 0;
        speedingTime = 0;
        suddenStops = 0;
        stopLineTime = 0;
        signalViolations = 0;
        safeDistanceTime = 0;
    }

    // 모든 필드의 getter/setter 추가
    public int getTotalDrivingTime() { return totalDrivingTime; }
    public void setTotalDrivingTime(int totalDrivingTime) {
        this.totalDrivingTime = totalDrivingTime;
    }

    public int getLaneKeepingTime() { return laneKeepingTime; }
    public void setLaneKeepingTime(int laneKeepingTime) {
        this.laneKeepingTime = laneKeepingTime;
    }

    public int getSpeedingTime() { return speedingTime; }
    public void setSpeedingTime(int speedingTime) {
        this.speedingTime = speedingTime;
    }

    public int getSuddenStops() { return suddenStops; }
    public void setSuddenStops(int suddenStops) {
        this.suddenStops = suddenStops;
    }

    public int getStopLineTime() { return stopLineTime; }
    public void setStopLineTime(int stopLineTime) {
        this.stopLineTime = stopLineTime;
    }

    public int getSignalViolations() { return signalViolations; }
    public void setSignalViolations(int signalViolations) {
        this.signalViolations = signalViolations;
    }

    public int getSafeDistanceTime() { return safeDistanceTime; }
    public void setSafeDistanceTime(int safeDistanceTime) {
        this.safeDistanceTime = safeDistanceTime;
    }
}