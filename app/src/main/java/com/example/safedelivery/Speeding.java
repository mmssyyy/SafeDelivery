package com.example.safedelivery;

public class Speeding {
    private final int SPEED_LIMIT = 90; // 과속 기준 속도
    private final int PENALTY_POINTS = -3; // 과속 시 감점되는 점수

    public int evaluateSpeed(int speed) {
        if (speed > SPEED_LIMIT) {
            return PENALTY_POINTS; // 과속 시 점수 감점
        }
        return 0; // 정상 속도일 경우 점수 변동 없음
    }
}