package com.example.safedelivery;

public class SuddenStopsAndStarts {
    private static final int SUDDEN_STOP_THRESHOLD = 40; // 급정거 기준 속도 (현재 속도가 40 이하로 떨어지면)
    private boolean isPersonDetected; // 사람 인식 여부

    // 생성자
    public SuddenStopsAndStarts() {
        this.isPersonDetected = false; // 초기에는 사람을 인식하지 않음
    }

    // 사람 인식 업데이트
    public void updatePersonDetection(boolean detected) {
        isPersonDetected = detected; // 사람 인식 상태 업데이트
    }

    // 현재 속도를 평가하고 급정거 여부에 따라 점수를 반환
    public int evaluateStop(int currentSpeed) {
        // 현재 속도 -> 40 이하 + 사람 감지 x
        if (currentSpeed < SUDDEN_STOP_THRESHOLD && !isPersonDetected) {
            System.out.println("급정거 발생: 현재 속도 " + currentSpeed + "km/h, 3점 깎음");
            return -3; // 급정거 시 3점 깎음
        }
        return 0; // 급정거가 아닌 경우 점수 변동 없음
    }
}