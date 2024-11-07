package com.example.safedelivery;

public class LaneKeeping {
    private static final int LANE_DETECTION_THRESHOLD = 60; // 차선을 60초(1분) 동안 감지하지 않으면 인도로 간주
    private int undetectedTime; // 차선이 감지되지 않은 시간을 추적하는 변수

    public LaneKeeping() {
        this.undetectedTime = 0;
    }

    // 매초 차선 감지 상태를 업데이트하고 인도로 간주할지 여부를 체크
    public boolean updateLaneDetection(boolean isLaneDetected) {
        if (!isLaneDetected) {
            undetectedTime++;
        } else {
            undetectedTime = 0; // 차선을 감지하면 시간 초기화
        }

        // 일정 시간(60초) 이상 차선이 감지되지 않으면 false 반환, 그렇지 않으면 true 반환
        return undetectedTime < LANE_DETECTION_THRESHOLD;
    }

    // 차선 유지 평가, 인도로 간주되면 3점 깎음
    public int evaluate() {
        if (undetectedTime >= LANE_DETECTION_THRESHOLD) {
            System.out.println("차선 미감지 1분 초과: 인도로 인식, 3점 깎음");
            return -3; // 인도로 인식되어 3점 깎음
        }
        return 0; // 차선이 감지된 경우 점수 변동 없음
    }
}
