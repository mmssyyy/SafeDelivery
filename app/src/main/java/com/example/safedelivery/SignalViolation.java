package com.example.safedelivery;

public class SignalViolation {
    private static final int VIOLATION_CHECK_DURATION = 3; // 신호 위반 체크 시간 (초)
    private static final int PENALTY_POINTS = 3; // 신호 위반 시 깎이는 점수
    private int noSignalTime; // 신호가 감지되지 않는 시간을 추적
    private boolean isRedLightPreviouslyDetected; // 이전에 빨간불이 감지되었는지 여부

    public SignalViolation() {
        this.noSignalTime = 0;
        this.isRedLightPreviouslyDetected = false;
    }

    // 신호 감지 상태를 업데이트하고 신호 위반 여부를 확인
    public boolean updateSignalDetection(String detectedSignal) {
        switch (detectedSignal) {
            case "red-light":
                isRedLightPreviouslyDetected = true;
                noSignalTime = 0; // 빨간불이 감지되면 신호 미감지 시간 초기화
                break;
            case "green":
                if (isRedLightPreviouslyDetected) {
                    // 빨간불 후 초록불이 감지된 경우 신호 위반이 아님
                    isRedLightPreviouslyDetected = false; // 초록불로 전환되면 빨간불 감지 초기화
                    noSignalTime = 0;
                }
                break;
            default:
                if (isRedLightPreviouslyDetected) {
                    noSignalTime++; // 빨간불이 사라진 상태에서 감지되지 않는 시간 증가
                }
                break;
        }

        // 빨간불이 인식되지 않은 상태에서 3초 동안 빨간불이나 초록불이 감지되지 않으면 신호 위반
        return isRedLightPreviouslyDetected && noSignalTime >= VIOLATION_CHECK_DURATION;
    }

    // 신호 위반 시 점수를 3점 깎음
    public int evaluate() {
        if (isRedLightPreviouslyDetected && noSignalTime >= VIOLATION_CHECK_DURATION) {
            System.out.println("신호 위반 발생: 3점 깎임");
            return -PENALTY_POINTS; // 신호 위반 시 3점 깎음
        }
        return 0; // 신호 위반이 아닌 경우 점수 변동 없음
    }
}