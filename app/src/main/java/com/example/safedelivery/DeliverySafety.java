package com.example.safedelivery;

import static android.content.ContentValues.TAG;

import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class DeliverySafety {
    private int totalScore; // 총 점수
    private SignalViolation signalViolation; // 신호 위반 클래스
    private Speeding speeding; // 과속 클래스
    private SuddenStopsAndStarts suddenStops; // 급정거 클래스
    private LaneKeeping laneKeeping; // 인도 주행 클래스

    public DeliverySafety() {
        this.totalScore = 100; // 초기 점수 설정
        this.signalViolation = new SignalViolation();
        this.speeding = new Speeding();
        this.suddenStops = new SuddenStopsAndStarts();
        this.laneKeeping = new LaneKeeping();
    }

    // 운전 평가 메소드
    public int evaluateDriving(int signalViolations, int totalDrivingTime,
                               int laneKeepingTime, int speedingTime, int suddenStops, int stopLineTime) {
        int totalScore = 100; // 기본 점수 100점
        Log.d(TAG, "Initial Score: " + totalScore);

        // 초기 주행이거나 매우 짧은 주행시간일 경우 100점 반환
        if (totalDrivingTime < 10) {
            Log.d(TAG, "Short drive time, returning full score");
            return totalScore;
        }

        // 각 항목별 점수 계산 전 현재 값들 로그
        Log.d(TAG, "Current Metrics - " +
                "TotalDrivingTime: " + totalDrivingTime +
                ", LaneKeepingTime: " + laneKeepingTime +
                ", SpeedingTime: " + speedingTime +
                ", SuddenStops: " + suddenStops);

        int deduction = 0;

        // 신호 위반 감점 (20점)
        if (signalViolations > 0) {
            int signalDeduction = Math.min(signalViolations * 10, 20);
            deduction += signalDeduction;
            Log.d(TAG, "신호위반 감점: -" + signalDeduction);
        }

        // 2. 차선 유지 감점 (20점)
        if (totalDrivingTime > 0) {
            double laneKeepingRatio = (double)laneKeepingTime / totalDrivingTime;
            int laneKeepingDeduction = (int)((1 - laneKeepingRatio) * 20);
            deduction += laneKeepingDeduction;
            Log.d(TAG, "차선유지 감점: -" + laneKeepingDeduction);
        }

        // 3. 과속 감점 (20점)
        if (totalDrivingTime > 0) {
            double speedingRatio = (double)speedingTime / totalDrivingTime;
            int speedingDeduction = (int)(speedingRatio * 20);
            deduction += speedingDeduction;
            Log.d(TAG, "과속 감점: -" + speedingDeduction);
        }

        // 4. 급정거 감점 (40점)
        if (suddenStops > 0) {
            int suddenStopDeduction = Math.min(suddenStops * 10, 40);
            deduction += suddenStopDeduction;
            Log.d(TAG, "급정거 감점: -" + suddenStopDeduction);
        }

        // 총 감점 적용
        totalScore -= deduction;
        Log.d(TAG, "총 감점: -" + deduction);

        // 최종 점수 범위 제한
        totalScore = Math.max(0, Math.min(100, totalScore));
        Log.d(TAG, "최종 점수: " + totalScore);

        return totalScore;
    }

    // 실시간 업데이트 메소드들
    public void updateSignal(String detectedSignal) {
        if (signalViolation.updateSignalDetection(detectedSignal)) {
            totalScore += signalViolation.evaluate();
        }
    }

    public void updateSpeed(int speed) {
        totalScore += speeding.evaluateSpeed(speed);
    }

    public void updateSuddenStop(int currentSpeed) {
        totalScore += suddenStops.evaluateStop(currentSpeed);
    }

    public void updateLaneDetection(boolean isLaneDetected) {
        totalScore += laneKeeping.evaluate();
        laneKeeping.updateLaneDetection(isLaneDetected);
    }

    // 현재 점수 반환
    public int getTotalScore() {
        return Math.max(0, Math.min(100, totalScore));
    }
}

