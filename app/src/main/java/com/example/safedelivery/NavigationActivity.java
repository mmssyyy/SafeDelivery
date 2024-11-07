package com.example.safedelivery;

import com.naver.maps.map.overlay.LocationOverlay;
import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.media.AudioManager;
import android.media.ToneGenerator;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.geometry.LatLngBounds;
import com.naver.maps.map.CameraAnimation;
import com.naver.maps.map.CameraPosition;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapView;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.overlay.PathOverlay;
import com.naver.maps.map.util.FusedLocationSource;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NavigationActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final String TAG = "NavigationActivity";
    private static final String TMAP_API_KEY = "EBgPuewLXy96lswHVhKiS4G3wexNG7T9ajxuAnOY";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private static final long UPDATE_INTERVAL = 10000; // 10초

    private MapView mapView;
    private NaverMap naverMap;
    private FusedLocationSource locationSource;

    private Button btnNavigateToPickup;
    private Button btnNavigateToDelivery;
    private boolean isNavigating = false;
    private LatLng currentLocation;
    private LatLng pickupLocation;
    private LatLng deliveryLocation;

    private LatLng targetLocation; // 현재 선택된 목적지
    private PathOverlay pathOverlay;
    private Marker currentMarker;
    private Marker pickupMarker;
    private Marker deliveryMarker;
    private long lastUpdateTime = 0;

    private TextView speedInfo;  // 클래스 멤버 변수로 추가

    private String deliveryId;
    private DatabaseReference mDatabase;
    private Button btnCompleteDelivery;

    private List<Float> speedHistory = new ArrayList<>();  // 최근 속도 기록
    private static final int SPEED_HISTORY_SIZE = 5;      // 평균 계산에 사용할 기록 수

    private TextView speedLimitView;
    private View safetyAlertView;
    private ImageView safetyIcon;
    private TextView safetyText;
    private ToneGenerator toneGenerator;
    private boolean isInSectionZone = false;
    private double sectionStartTime = 0;
    private double sectionDistance = 0;
    private double currentSpeed = 0;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private float[] lastAccelerometer = new float[3];
    private float[] lastMagnetometer = new float[3];
    private boolean haveSensor = false;
    private boolean haveSensor2 = false;
    private float[] rotationMatrix = new float[9];
    private float[] orientation = new float[3];
    private float currentDegree = 0f;

    private DeliverySafety deliverySafety;
    private long navigationStartTime;
    private SafetyMetrics currentMetrics = new SafetyMetrics();
    private float previousSpeed = 0f;
    private long previousLocationTime = 0;
    private static final float SUDDEN_STOP_THRESHOLD = 5.0f; // m/s^2

    private int currentSpeedLimit = 0;

    private TextView currentSafetyScore;
    private TextView expectedPoints;
    private LinearLayout safetyScoreLayout;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        deliverySafety = new DeliverySafety();
        currentMetrics = new SafetyMetrics();
        currentMetrics.reset();

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // Firebase 초기화
        mDatabase = FirebaseDatabase.getInstance().getReference("SafeDelivery");

        // 배달 ID 가져오기
        deliveryId = getIntent().getStringExtra("deliveryId");

        // 픽업 위치와 배달 위치 받기
        String pickup = getIntent().getStringExtra("pickup");
        String delivery = getIntent().getStringExtra("destination");

        if (pickup != null && pickup.contains(",")) {
            String[] coords = pickup.split(",");
            pickupLocation = new LatLng(
                    Double.parseDouble(coords[0]),
                    Double.parseDouble(coords[1])
            );
        }

        if (delivery != null && delivery.contains(",")) {
            String[] coords = delivery.split(",");
            deliveryLocation = new LatLng(
                    Double.parseDouble(coords[0]),
                    Double.parseDouble(coords[1])
            );
        }

        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            haveSensor = true;
        }

        if (sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null) {
            magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            haveSensor2 = true;
        }

        btnCompleteDelivery = findViewById(R.id.btnCompleteDelivery);
        btnCompleteDelivery.setOnClickListener(v -> showCompletionDialog());

        setupLocationUpdates();
        setupSafetyAlerts();
        initializeViews();
        initializeMap(savedInstanceState);
    }

    private void setupSafetyAlerts() {
        // 안전 운전 관련 UI 초기화
        safetyAlertView = findViewById(R.id.safetyAlert);
        speedLimitView = findViewById(R.id.speedLimitView);
        safetyIcon = findViewById(R.id.safetyIcon);
        safetyText = findViewById(R.id.safetyText);

        // 초기 상태 설정
        safetyAlertView.setVisibility(View.GONE);
        speedLimitView.setVisibility(View.GONE);
    }


    private void showCompletionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("배달 완료")
                .setMessage("배달을 완료하시겠습니까?")
                .setPositiveButton("완료", (dialog, which) -> completeDelivery())
                .setNegativeButton("취소", null)
                .show();
    }


    private void applyLowPassFilter(float[] input, float[] output) {
        final float alpha = 0.1f;
        for (int i = 0; i < input.length; i++) {
            output[i] = output[i] + alpha * (input[i] - output[i]);
        }
    }

    private void completeDelivery() {
        if (deliveryId == null) {
            Toast.makeText(this, "배달 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "로그인 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 안전 점수 계산
        int safetyScore = deliverySafety.evaluateDriving(
                currentMetrics.signalViolations,
                currentMetrics.totalDrivingTime,
                currentMetrics.laneKeepingTime,
                currentMetrics.speedingTime,
                currentMetrics.suddenStops,
                currentMetrics.stopLineTime
        );


        int basePoints = 300;
        int additionalPoints = calculateAdditionalPoints(safetyScore);
        int totalPoints = basePoints + additionalPoints;


        String userId = currentUser.getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("SafeDelivery")
                .child("UserAccount").child(userId);

        // 현재 배달 요청 정보 가져오기
        mDatabase.child("deliveryRequests").child(deliveryId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        DeliveryRequest request = snapshot.getValue(DeliveryRequest.class);
                        if (request != null) {
                            // 포인트 적립을 위해 사용자 정보 가져오기
                            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                                    UserAccount user = userSnapshot.getValue(UserAccount.class);
                                    if (user != null) {
                                        // 사용자의 평균 안전 점수와 총 안전 포인트 업데이트
                                        updateAverageSafetyScore(userId, safetyScore, currentMetrics.totalDrivingTime, additionalPoints);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Toast.makeText(NavigationActivity.this,
                                            "포인트 적립 실패", Toast.LENGTH_SHORT).show();
                                }
                            });

                            // 완료 시간 추가
                            request.setStatus("완료");
                            request.setCompletedTime(System.currentTimeMillis());
                            request.setUserId(userId);

                            // 완료된 배달 목록으로 이동
                            mDatabase.child("completedDeliveries")
                                    .child(deliveryId)
                                    .setValue(request)
                                    .addOnSuccessListener(aVoid -> {
                                        // 기존 배달 요청에서 삭제
                                        mDatabase.child("deliveryRequests")
                                                .child(deliveryId)
                                                .removeValue()
                                                .addOnSuccessListener(aVoid1 -> {
                                                    String message = String.format(
                                                            "배달이 완료되었습니다.\n획득 포인트: %dP (기본: %dP + 안전 운전: %dP)",
                                                            totalPoints, basePoints, additionalPoints
                                                    );
                                                    Toast.makeText(NavigationActivity.this,
                                                            message, Toast.LENGTH_LONG).show();
                                                    finish();
                                                })
                                                .addOnFailureListener(e ->
                                                        Toast.makeText(NavigationActivity.this,
                                                                "배달 요청 삭제 실패", Toast.LENGTH_SHORT).show());
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(NavigationActivity.this,
                                                    "완료 처리 실패", Toast.LENGTH_SHORT).show());
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(NavigationActivity.this,
                                "데이터 로드 실패", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void updateAverageSafetyScore(String userId, int safetyScore, int totalDrivingTime, int additionalPoints) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("SafeDelivery")
                .child("UserAccount").child(userId);
        DatabaseReference completedDeliveriesRef = FirebaseDatabase.getInstance().getReference("SafeDelivery")
                .child("completedDeliveries");

        completedDeliveriesRef.orderByChild("userId").equalTo(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        // 현재까지의 배달 건수 (완료된 배달만 계산)
                        long totalDeliveries = snapshot.getChildrenCount() > 0 ? snapshot.getChildrenCount() : 1;

                        Log.d(TAG, "Actual completed deliveries: " + totalDeliveries);

                        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                                UserAccount user = userSnapshot.getValue(UserAccount.class);
                                if (user != null) {
                                    // 기존 값 가져오기
                                    int oldTotalSafetyPoints = user.getTotalSafetyScore();
                                    int oldPoints = user.getPoints();
                                    int oldTotalDrivingTime = user.getTotalDrivingTime();

                                    // 새로운 평균 안전 점수 계산
                                    double newAverageSafetyScore;
                                    if (snapshot.getChildrenCount() == 0) { // 첫 배달인 경우
                                        newAverageSafetyScore = safetyScore;
                                        Log.d(TAG, "First delivery, setting initial score: " + safetyScore);
                                    } else {
                                        // 기존 평균과 현재 점수로 새로운 평균 계산
                                        double oldAverageSafetyScore = user.getAverageSafetyScore();
                                        newAverageSafetyScore = ((oldAverageSafetyScore * (totalDeliveries - 1)) + safetyScore) / totalDeliveries;
                                        Log.d(TAG, "Calculating new average: " + newAverageSafetyScore);
                                    }

                                    // 새로운 값 계산
                                    int newTotalSafetyPoints = oldTotalSafetyPoints + additionalPoints;
                                    int basePoints = 300;
                                    int newPoints = oldPoints + basePoints + additionalPoints;
                                    int newTotalDrivingTime = oldTotalDrivingTime + totalDrivingTime;

                                    // 로그 추가
                                    Log.d(TAG, "Update Values - " +
                                            "Total Deliveries: " + totalDeliveries +
                                            ", Safety Score: " + safetyScore +
                                            ", New Average: " + newAverageSafetyScore);

                                    // 데이터베이스 업데이트
                                    Map<String, Object> updates = new HashMap<>();
                                    updates.put("totalSafetyScore", newTotalSafetyPoints);
                                    updates.put("points", newPoints);
                                    updates.put("totalDrivingTime", newTotalDrivingTime);
                                    updates.put("averageSafetyScore", newAverageSafetyScore);
                                    updates.put("totalDeliveries", totalDeliveries); // 실제 완료된 배달 건수만 저장

                                    userRef.updateChildren(updates)
                                            .addOnSuccessListener(aVoid -> {
                                                Log.d(TAG, "Successfully updated user data with " + totalDeliveries + " deliveries");
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e(TAG, "Error updating user data", e);
                                            });
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Toast.makeText(NavigationActivity.this,
                                        "안전 점수 업데이트 실패", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(NavigationActivity.this,
                                "총 배달 건수 조회 실패", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private int calculateAdditionalPoints(int safetyScore) {
        if (safetyScore >= 90) return 200;
        if (safetyScore >= 80) return 150;
        if (safetyScore >= 70) return 100;
        if (safetyScore >= 50) return 50;
        return 0;
    }

    private void saveSafetyData(int safetyScore, int totalPoints) {
        // 실제 주행이 없었거나 점수가 0점인 경우 저장하지 않음
        if (safetyScore <= 0 || currentMetrics.totalDrivingTime == 0) {
            return;
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference safetyRef = FirebaseDatabase.getInstance()
                .getReference("SafeDelivery")
                .child("safetyScores")
                .child(userId)
                .child(deliveryId);

        SafetyScore safetyData = new SafetyScore(safetyScore, totalPoints, currentMetrics);
        safetyRef.setValue(safetyData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Safety score saved successfully: " + safetyScore);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving safety score", e);
                });
    }
    private void addPointHistory(String userId, int points, String description, String type) {
        DatabaseReference pointHistoryRef = FirebaseDatabase.getInstance()
                .getReference("SafeDelivery").child("pointHistory");

        PointHistory history = new PointHistory(userId, points, description, type);
        pointHistoryRef.push().setValue(history);
    }



    private SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
                System.arraycopy(event.values, 0, lastAccelerometer, 0, event.values.length);
            else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
                System.arraycopy(event.values, 0, lastMagnetometer, 0, event.values.length);

            if (SensorManager.getRotationMatrix(rotationMatrix, null, lastAccelerometer, lastMagnetometer)) {
                // 좌표계 리매핑 수정
                float[] remappedRotationMatrix = new float[9];
                SensorManager.remapCoordinateSystem(rotationMatrix,
                        SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Y,  // 축 변경
                        remappedRotationMatrix);

                SensorManager.getOrientation(remappedRotationMatrix, orientation);
                float azimuthInRadians = orientation[0];
                float azimuthInDegrees = (float) Math.toDegrees(azimuthInRadians);

                // 90도 회전 적용
                azimuthInDegrees = (azimuthInDegrees + 180 + 360) % 360;

                // 저역 통과 필터 적용
                float deltaRotation = azimuthInDegrees - currentDegree;

                // 180도를 넘는 회전을 보정
                if (deltaRotation > 180) {
                    deltaRotation -= 360;
                } else if (deltaRotation < -180) {
                    deltaRotation += 360;
                }

                // 임계값을 높이고 스무딩 팩터를 낮춤
                if (Math.abs(deltaRotation) > 8.0f && isNavigating && naverMap != null) {
                    currentDegree += deltaRotation * 0.03f;
                    currentDegree = (currentDegree + 360) % 360;

                    // 추가 노이즈 필터링
                    if (Math.abs(deltaRotation) < 45) {
                        runOnUiThread(() -> {
                            LocationOverlay locationOverlay = naverMap.getLocationOverlay();
                            locationOverlay.setBearing(currentDegree);

                            CameraUpdate cameraUpdate = CameraUpdate.toCameraPosition(
                                    new CameraPosition(
                                            new LatLng(currentLocation.latitude, currentLocation.longitude),
                                            naverMap.getCameraPosition().zoom,
                                            45,
                                            currentDegree
                                    )
                            ).animate(CameraAnimation.Easing, 500);

                            naverMap.moveCamera(cameraUpdate);
                        });
                    }
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };


    private void initializeViews() {
        mapView = findViewById(R.id.map_view);

        btnNavigateToPickup = findViewById(R.id.btnNavigateToPickup);
        btnNavigateToDelivery = findViewById(R.id.btnNavigateToDelivery);

        btnNavigateToPickup.setOnClickListener(v -> startNavigationTo(pickupLocation));
        btnNavigateToDelivery.setOnClickListener(v -> startNavigationTo(deliveryLocation));

        pathOverlay = new PathOverlay();
        pathOverlay.setColor(0xFF4FC3F7); // 파란색 계열의 깔끔한 색상으로 변경
        pathOverlay.setWidth(15);

        speedInfo = findViewById(R.id.speedInfo);

        speedLimitView = findViewById(R.id.speedLimitView);
        safetyAlertView = findViewById(R.id.safetyAlert);
        safetyIcon = findViewById(R.id.safetyIcon);
        safetyText = findViewById(R.id.safetyText);

        toneGenerator = new ToneGenerator(AudioManager.STREAM_ALARM, 100);

        safetyScoreLayout = findViewById(R.id.safetyScoreLayout);
        currentSafetyScore = findViewById(R.id.currentSafetyScore);
        expectedPoints = findViewById(R.id.expectedPoints);

        currentMarker = new Marker();
        pickupMarker = new Marker();
        deliveryMarker = new Marker();
    }

    private void initializeMap(Bundle savedInstanceState) {
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        locationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);
    }

    // 속도 업데이트 메서드
    private void updateSpeed(Location location) {
        if (location.hasSpeed()) {
            float speedMPS = location.getSpeed(); // 초당 미터 단위
            float speedKPH = speedMPS * 3.6f;
            currentSpeed = speedKPH; // km/h로 변환

            speedHistory.add(speedKPH);
            if (speedHistory.size() > SPEED_HISTORY_SIZE) {
                speedHistory.remove(0);
            }

            float avgSpeed = 0;
            for (float speed : speedHistory) {
                avgSpeed += speed;
            }
            avgSpeed /= speedHistory.size();

            final float finalAvgSpeed = avgSpeed;

            runOnUiThread(() -> {
                speedInfo.setText(String.format(Locale.getDefault(), "%.0f km/h", finalAvgSpeed));

                if (finalAvgSpeed > 100) {
                    speedInfo.setTextColor(Color.RED);
                } else {
                    speedInfo.setTextColor(Color.BLACK);
                }
            });
        } else {
            runOnUiThread(() -> speedInfo.setText("-- km/h"));
        }
    }

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        this.naverMap = naverMap;

        // 현재 위치 오버레이 설정
        LocationOverlay locationOverlay = naverMap.getLocationOverlay();
        locationOverlay.setVisible(true);

        // 이미지 아이콘 설정
        locationOverlay.setIcon(OverlayImage.fromResource(R.drawable.navigation_marker));
        locationOverlay.setIconWidth(50);  // 이미지 크기에 맞게 조정
        locationOverlay.setIconHeight(50);

        // 기존의 마커는 숨김
        if (currentMarker != null) {
            currentMarker.setMap(null);
        }
        // 아이콘 방향 설정
        locationOverlay.setBearing(getBearingToDestination());

        // 기본 설정
        naverMap.setLocationSource(locationSource);
        naverMap.setLocationTrackingMode(LocationTrackingMode.Follow);
        naverMap.getUiSettings().setLocationButtonEnabled(true);
        naverMap.getUiSettings().setZoomControlEnabled(true);
        naverMap.getUiSettings().setCompassEnabled(true);

        // 마커 설정
        setupMarkers();

        // 위치 변경 리스너 수정
        naverMap.addOnLocationChangeListener(location -> {
            currentLocation = new LatLng(location.getLatitude(), location.getLongitude());

            if (isNavigating) {
                updateNavigationCamera(currentLocation);
                updateSafetyMetrics(location);
                updateSpeed(location);  // 속도 업데이트 추가

                if (System.currentTimeMillis() - lastUpdateTime > UPDATE_INTERVAL) {
                    findPath();
                    checkArrival();
                    lastUpdateTime = System.currentTimeMillis();
                }
            }
        });

        // 초기 위치 가져오기
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.getFusedLocationProviderClient(this)
                    .getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            updateCurrentLocationMarker();
                        }
                    });
        }

        checkLocationPermission();
    }

    // 안전 메트릭 업데이트
    private void updateSafetyMetrics(Location location) {
        // 현재 속도 확인
        if (location.hasSpeed()) {
            float speedKmh = location.getSpeed() * 3.6f;

            // DeliverySafety에 속도 업데이트
            deliverySafety.updateSpeed((int)speedKmh);

            // 과속 시간 기록 (통계용)
            if (speedKmh > getCurrentSpeedLimit()) {
                currentMetrics.speedingTime++;
            }

            // 급정거 체크
            if (previousLocationTime != 0) {
                float acceleration = (location.getSpeed() - previousSpeed) /
                        ((location.getTime() - previousLocationTime) / 1000f);
                if (acceleration < -SUDDEN_STOP_THRESHOLD) {
                    currentMetrics.suddenStops++;
                    // DeliverySafety에 급정거 상태 업데이트
                    deliverySafety.updateSuddenStop((int)speedKmh);
                }
            }

            previousSpeed = location.getSpeed();
            previousLocationTime = location.getTime();
        }

        // 주행 시간 업데이트
        currentMetrics.totalDrivingTime =
                (int)((System.currentTimeMillis() - navigationStartTime) / 1000);

        // 차선 감지 상태 업데이트 (이 부분은 실제 차선 감지 로직으로 대체 필요)
        boolean isLaneDetected = true; // 임시로 true 설정
        deliverySafety.updateLaneDetection(isLaneDetected);

        updateRealTimeSafetyScore();
    }

    private int getCurrentSpeedLimit() {
        return currentSpeedLimit > 0 ? currentSpeedLimit : 60; // 기본값 60km/h
    }

    // 더 정확한 속도 측정을 위한 위치 업데이트 설정 추가
    private void setupLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            LocationRequest locationRequest = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(1000)  // 1초마다 업데이트
                    .setFastestInterval(500);

            LocationServices.getFusedLocationProviderClient(this)
                    .requestLocationUpdates(locationRequest, new LocationCallback() {
                        @Override
                        public void onLocationResult(LocationResult locationResult) {
                            if (locationResult != null) {
                                Location location = locationResult.getLastLocation();
                                if (location != null) {
                                    updateSpeed(location);
                                }
                            }
                        }
                    }, null);
        }
    }

    private void setupMarkers() {
        // 현재 위치 마커 설정
        currentMarker.setIconTintColor(Color.RED);  // 마커 색상을 빨간색으로 설정
        currentMarker.setCaptionText("현재 위치");

        // 픽업 위치 마커 설정
        if (pickupLocation != null) {
            pickupMarker.setPosition(pickupLocation);
            pickupMarker.setMap(naverMap);
            pickupMarker.setCaptionText("픽업 위치");
        }

        // 배달 위치 마커 설정
        if (deliveryLocation != null) {
            deliveryMarker.setPosition(deliveryLocation);
            deliveryMarker.setMap(naverMap);
            deliveryMarker.setCaptionText("배달 위치");
        }

        // 모든 마커가 보이도록 카메라 위치 조정
        if (pickupLocation != null && deliveryLocation != null) {
            LatLngBounds bounds = new LatLngBounds.Builder()
                    .include(pickupLocation)
                    .include(deliveryLocation)
                    .build();
            naverMap.moveCamera(CameraUpdate.fitBounds(bounds, 150));
        }
    }

    private void updateCurrentLocationMarker() {
        if (currentLocation != null) {
            currentMarker.setPosition(currentLocation);
            currentMarker.setMap(naverMap);
        }
    }

    // 내비게이션 시작 메서드
    private void startNavigationTo(LatLng destination) {
        Log.d(TAG, "Trying to navigate to destination: " + destination.latitude + "," + destination.longitude);

        if (currentLocation == null || destination == null) {
            Toast.makeText(this, "위치 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        currentSafetyScore.setText("안전 점수: 측정 중");
        expectedPoints.setText("예상 추가 포인트: 측정 중");

        targetLocation = destination;
        isNavigating = true;
        pathOverlay.setMap(null);
        navigationStartTime = System.currentTimeMillis();
        currentMetrics.reset();

        // 네비게이션 모드 설정
        setupNavigationMode();
        speedInfo.setVisibility(View.VISIBLE);

        findPath();
    }

    private void updateRealTimeSafetyScore() {
        if (!isNavigating) return;

        // 최소 10초 이상 주행했을 때부터 점수 표시
        if (currentMetrics.totalDrivingTime >= 5) {
            // DeliverySafety에서 직접 현재 점수 가져오기
            int currentScore = deliverySafety.getTotalScore();
            int additionalPoints = calculateAdditionalPoints(currentScore);


            runOnUiThread(() -> {
                currentSafetyScore.setText(String.format("안전 점수: %d점", currentScore));
                expectedPoints.setText(String.format("예상 추가 포인트: %dP", additionalPoints));

                // 점수에 따라 색상 변경
                if (currentScore >= 80) {
                    currentSafetyScore.setTextColor(Color.parseColor("#4CAF50")); // 초록색
                    expectedPoints.setTextColor(Color.parseColor("#4CAF50"));
                } else if (currentScore >= 60) {
                    currentSafetyScore.setTextColor(Color.parseColor("#FFC107")); // 노란색
                    expectedPoints.setTextColor(Color.parseColor("#FFC107"));
                } else {
                    currentSafetyScore.setTextColor(Color.parseColor("#F44336")); // 빨간색
                    expectedPoints.setTextColor(Color.parseColor("#F44336"));
                }
            });
        }
    }

    // 네비게이션 모드 설정
    private void setupNavigationMode() {
        // 지도 회전 모드 활성화
        naverMap.setLocationTrackingMode(LocationTrackingMode.Face);

        // 나침반 모드 활성화
        naverMap.getUiSettings().setCompassEnabled(true);

        // 초기 카메라 위치 설정
        CameraPosition cameraPosition = new CameraPosition(
                currentLocation,  // 현재 위치
                17,              // 줌 레벨
                45,              // 기울기 각도 (45도)
                getBearingToDestination()  // 목적지 방향으로 회전
        );

        naverMap.setCameraPosition(cameraPosition);
    }

    // 목적지 방향 계산
    private float getBearingToDestination() {
        if (currentLocation != null && targetLocation != null) {
            Location currentLoc = new Location("");
            currentLoc.setLatitude(currentLocation.latitude);
            currentLoc.setLongitude(currentLocation.longitude);

            Location targetLoc = new Location("");
            targetLoc.setLatitude(targetLocation.latitude);
            targetLoc.setLongitude(targetLocation.longitude);

            return currentLoc.bearingTo(targetLoc);
        }
        return 0f;
    }


    // 실시간 위치 업데이트 및 카메라 이동
    // 위치 업데이트 시 지도 회전
    private void updateNavigationCamera(LatLng location) {
        if (!isNavigating) return;

        float bearing = currentDegree;  // getBearingToDestination() 대신 현재 센서 방향 사용

        LocationOverlay locationOverlay = naverMap.getLocationOverlay();
        locationOverlay.setBearing(bearing);  // 오버레이 방향 업데이트

        CameraUpdate cameraUpdate = CameraUpdate.toCameraPosition(new CameraPosition(
                location,
                17,     // 줌 레벨
                45,     // 기울기
                bearing // 방향
        )).animate(CameraAnimation.Linear);

        naverMap.moveCamera(cameraUpdate);
    }
    // 두 지점 사이의 각도 계산하는 메서드
    private double calculateBearing(LatLng from, LatLng to) {
        double lat1 = Math.toRadians(from.latitude);
        double lon1 = Math.toRadians(from.longitude);
        double lat2 = Math.toRadians(to.latitude);
        double lon2 = Math.toRadians(to.longitude);

        double y = Math.sin(lon2 - lon1) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(lon2 - lon1);

        double bearing = Math.toDegrees(Math.atan2(y, x));
        return (bearing + 360) % 360;
    }

    private void startNavigationWithLocation(LatLng destination) {
        if (destination == null) {
            Toast.makeText(this, "목적지 위치를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        targetLocation = destination;
        isNavigating = true;

        // 로그 추가
        Log.d(TAG, "Starting navigation from: " + currentLocation.latitude + "," + currentLocation.longitude);
        Log.d(TAG, "To destination: " + destination.latitude + "," + destination.longitude);

        // 기존 경로 제거
        pathOverlay.setMap(null);

        // 경로 찾기
        findPath();

        // 카메라 현재 위치로 이동
        naverMap.setLocationTrackingMode(LocationTrackingMode.Follow);
    }


    private void findPath() {
        if (currentLocation == null || targetLocation == null) {
            return;
        }

        new Thread(() -> makePathRequest()).start();
    }

    private void makePathRequest() {
        HttpURLConnection conn = null;
        try {
            conn = setupConnection();
            sendRequestBody(conn);
            String response = getResponse(conn);
            processRouteResponse(response);
        } catch (Exception e) {
            handleError(e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private HttpURLConnection setupConnection() throws IOException {
        URL url = new URL("https://apis.openapi.sk.com/tmap/routes?version=1");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("appKey", TMAP_API_KEY);
        conn.setDoOutput(true);
        return conn;
    }

    private void sendRequestBody(HttpURLConnection conn) throws JSONException, IOException {
        JSONObject requestBody = createRequestBody();
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
    }

    private JSONObject createRequestBody() throws JSONException {
        JSONObject requestBody = new JSONObject();
        requestBody.put("startX", String.format(Locale.US, "%.6f", currentLocation.longitude));
        requestBody.put("startY", String.format(Locale.US, "%.6f", currentLocation.latitude));
        requestBody.put("endX", String.format(Locale.US, "%.6f", targetLocation.longitude));
        requestBody.put("endY", String.format(Locale.US, "%.6f", targetLocation.latitude));
        requestBody.put("reqCoordType", "WGS84GEO");
        requestBody.put("resCoordType", "WGS84GEO");
        requestBody.put("searchOption", "0");
        requestBody.put("trafficInfo", "Y");

        // 안전 운전 정보 요청 파라미터 추가
        requestBody.put("roadDetails", "Y");        // 도로 상세 정보
        requestBody.put("safer", "Y");              // 안전운전 정보
        requestBody.put("guidance", "Y");           // 상세 안내 정보
        requestBody.put("alertInfo", "Y");          // 경고 정보
        requestBody.put("speedLimit", "Y");
        requestBody.put("facilityInfo", "Y");      // 시설물 정보 요청
        requestBody.put("schoolZoneInfo", "Y");    // 어린이보호구역 정보 명시적 요청

        return requestBody;
    }

    private String getResponse(HttpURLConnection conn) throws IOException {
        checkResponseCode(conn);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }

    private void checkResponseCode(HttpURLConnection conn) throws IOException {
        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            try (BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                StringBuilder errorResponse = new StringBuilder();
                String line;
                while ((line = errorReader.readLine()) != null) {
                    errorResponse.append(line);
                }
                throw new IOException("HTTP error code: " + responseCode +
                        "\nError response: " + errorResponse.toString());
            }
        }
    }

    private void handleError(Exception e) {
        Log.e(TAG, "Error finding path: " + e.getMessage(), e);
        runOnUiThread(() ->
                Toast.makeText(this, "경로 탐색 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show()
        );
    }

    // 경로 응답 처리 메서드
    private void processRouteResponse(String response) {
        try {
            JSONObject jsonResponse = new JSONObject(response);
            JSONArray features = jsonResponse.getJSONArray("features");

            List<LatLng> routePoints = new ArrayList<>();
            StringBuilder instructionsBuilder = new StringBuilder();
            double totalDistance = 0;
            int totalTime = 0;

            // 현재 안내해야 할 구간 찾기
            for (int i = 0; i < features.length(); i++) {
                JSONObject feature = features.getJSONObject(i);
                JSONObject properties = feature.getJSONObject("properties");
                processSafetyInfo(properties);

                // 전체 거리와 시간 정보
                if (properties.has("totalDistance")) {
                    totalDistance = properties.getDouble("totalDistance");
                }
                if (properties.has("totalTime")) {
                    totalTime = properties.getInt("totalTime");
                }

                // 현재 구간의 안내 정보 업데이트
                if (properties.has("distance") && properties.has("turnType")) {
                    updateNavigationGuide(properties);
                }

                // 경로 좌표
                JSONObject geometry = feature.getJSONObject("geometry");
                if (geometry.getString("type").equals("LineString")) {
                    JSONArray coordinates = geometry.getJSONArray("coordinates");
                    for (int j = 0; j < coordinates.length(); j++) {
                        JSONArray coord = coordinates.getJSONArray(j);
                        routePoints.add(new LatLng(
                                coord.getDouble(1),
                                coord.getDouble(0)
                        ));
                    }
                }
            }

            // 전체 경로 정보 업데이트
            updateRouteInfo(totalDistance, totalTime);

            runOnUiThread(() -> {
                // 경로선 그리기
                pathOverlay.setCoords(routePoints);
                pathOverlay.setMap(naverMap);
            });

        } catch (Exception e) {
            Log.e(TAG, "Error processing route response", e);
        }
    }

    private void processSafetyInfo(JSONObject properties) throws JSONException {
        // 전체 properties 로그 출력
        Log.d(TAG, "Safety Properties: " + properties.toString());

        // 제한 속도
        if (properties.has("speedLimit")) {
            int speedLimit = properties.getInt("speedLimit");
            currentSpeedLimit = properties.getInt("speedLimit");
            Log.d(TAG, "Speed Limit: " + speedLimit);
            showSpeedLimit(speedLimit);
        }

        // 어린이보호구역
        if (properties.has("schoolZone") ||
                (properties.has("facilityType") && properties.getString("facilityType").contains("어린이")) ||
                (properties.has("roadType") && properties.getString("roadType").contains("스쿨존"))) {
            Log.d(TAG, "School Zone Detected!");
            if (properties.has("distance")) {
                double distance = properties.getDouble("distance");
                if (distance < 500) { // 500m 이내
                    showSchoolZoneAlert();
                    // 속도 제한 자동 설정
                    currentSpeedLimit = 30;
                    showSpeedLimit(30);
                }
            }
        }

        // 과속 단속 카메라
        if (properties.has("safetyCamera")) {
            String cameraType = properties.getString("cameraType");
            double distance = properties.getDouble("distance");
            Log.d(TAG, "Camera Type: " + cameraType + ", Distance: " + distance);
            showSafetyCameraAlert(cameraType, distance);
        }

        // 구간 단속
        if (properties.has("sectionControl")) {
            boolean isSectionControl = properties.getBoolean("sectionControl");
            Log.d(TAG, "Section Control: " + isSectionControl);
            handleSectionControl(properties);
        }
        if (properties.has("trafficSignal")) {
            String signalStatus = properties.getString("trafficSignal");
            deliverySafety.updateSignal(signalStatus);

            if (properties.getBoolean("isGreenLight")) {
                currentMetrics.signalViolations = 0;
            }
        }

        // 정지선 정보 처리
        if (properties.has("stopLine")) {
            if (properties.getBoolean("isStopLine")) {
                currentMetrics.stopLineTime++;
            }
        }

    }

    private void showSpeedLimit(int speedLimit) {
        runOnUiThread(() -> {
            if (speedLimitView == null) {
                Log.e(TAG, "Speed limit view is not initialized");
                return;
            }

            // 제한속도 표시 설정
            speedLimitView.setVisibility(View.VISIBLE);
            speedLimitView.setText(speedLimit + "km/h");

            // 제한속도 표시 스타일 설정 (더 잘 보이도록)
            speedLimitView.setTextSize(20);  // 텍스트 크기 키우기
            speedLimitView.setPadding(16, 8, 16, 8);  // 여백 추가
            speedLimitView.setBackgroundResource(R.drawable.speed_limit_background);  // 배경 설정

            // 현재 속도가 제한 속도를 초과하는 경우
            if (currentSpeed > speedLimit) {
                speedLimitView.setTextColor(Color.RED);
                speedLimitView.setTypeface(null, Typeface.BOLD);  // 글씨 굵게
                playWarningSound();
            } else {
                speedLimitView.setTextColor(Color.BLACK);
                speedLimitView.setTypeface(null, Typeface.NORMAL);
            }

            // 제한속도 변경시 잠시 강조 효과
            speedLimitView.setAlpha(1.0f);
            speedLimitView.animate()
                    .alpha(0.8f)
                    .setDuration(500)
                    .start();
        });
    }

    private void showSafetyCameraAlert(String type, double distance) {
        runOnUiThread(() -> {
            safetyAlertView.setVisibility(View.VISIBLE);

            switch (type) {
                case "FIXED":
                    safetyIcon.setImageResource(R.drawable.ic_speed_camera);
                    safetyText.setText(String.format("%.0fm 앞 단속 카메라", distance));
                    break;
                case "SECTION":
                    safetyIcon.setImageResource(R.drawable.ic_speed_camera);
                    safetyText.setText("구간 단속 구간 진입");
                    break;
            }

            playWarningSound();

            // 3초 후 알림 숨기기
            new Handler().postDelayed(() ->
                    safetyAlertView.setVisibility(View.GONE), 3000);
        });
    }

    private void showSchoolZoneAlert() {
        runOnUiThread(() -> {
            if (safetyAlertView == null || safetyIcon == null || safetyText == null) {
                Log.e(TAG, "Safety alert views are not initialized");
                return;
            }

            safetyAlertView.setVisibility(View.VISIBLE);
            safetyIcon.setImageResource(R.drawable.ic_school_zone);
            safetyText.setText("어린이보호구역\n제한속도 30km/h");
            playWarningSound();

            // 알림이 계속 보이도록 하기
            Handler handler = new Handler();
            handler.postDelayed(() -> {
                if (safetyAlertView != null) {
                    safetyAlertView.setVisibility(View.GONE);
                }
            }, 5000); // 5초 동안 표시
        });
    }

    private void handleSectionControl(JSONObject properties) throws JSONException {
        if (properties.getBoolean("sectionStart")) {
            isInSectionZone = true;
            sectionStartTime = System.currentTimeMillis();
            sectionDistance = properties.getDouble("sectionLength");
            showSectionStartAlert();
        } else if (properties.getBoolean("sectionEnd") && isInSectionZone) {
            calculateSectionSpeed();
            isInSectionZone = false;
        }
    }

    private void showSectionStartAlert() {
        runOnUiThread(() -> {
            safetyAlertView.setVisibility(View.VISIBLE);
            safetyIcon.setImageResource(R.drawable.ic_speed_camera);
            safetyText.setText("구간 단속 구간 진입");

            // 구간 단속 정보 표시
            TextView sectionControlInfo = findViewById(R.id.sectionControlInfo);
            sectionControlInfo.setVisibility(View.VISIBLE);
            sectionControlInfo.setText("구간 단속 중");

            // 경고음 재생
            playWarningSound();

            // 3초 후 알림 숨기기
            new Handler().postDelayed(() -> {
                safetyAlertView.setVisibility(View.GONE);
                // 구간 단속 정보는 구간을 벗어날 때까지 계속 표시
            }, 3000);
        });
    }

    private void calculateSectionSpeed() {
        double timeTaken = (System.currentTimeMillis() - sectionStartTime) / 1000.0; // 초 단위
        double averageSpeed = (sectionDistance / 1000.0) / (timeTaken / 3600.0); // km/h

        runOnUiThread(() -> {
            safetyAlertView.setVisibility(View.VISIBLE);
            safetyText.setText(String.format("구간 평균 속도: %.1f km/h", averageSpeed));
            new Handler().postDelayed(() ->
                    safetyAlertView.setVisibility(View.GONE), 3000);
        });
    }

    // 경고음 재생 메서드
    private void playWarningSound() {
        if (toneGenerator != null) {
            toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
        }
    }

    // 전체 경로 정보 업데이트
    private void updateRouteInfo(double totalDistance, int totalTime) {
        runOnUiThread(() -> {
            TextView totalDistanceView = findViewById(R.id.totalDistance);
            TextView estimatedTimeView = findViewById(R.id.estimatedTime);

            String distanceText = totalDistance >= 1000 ?
                    String.format("총 %.1fkm", totalDistance / 1000) :
                    String.format("총 %dm", (int)totalDistance);

            String timeText = String.format("예상 %d분", totalTime / 60);

            totalDistanceView.setText(distanceText);
            estimatedTimeView.setText(timeText);
        });
    }
    private void updateNavigationGuide(JSONObject properties) {
        try {
            runOnUiThread(() -> {
                // 다음 회전까지 남은 거리
                if (properties.has("distance")) {
                    double distance = properties.optDouble("distance", 0);
                    TextView distanceView = findViewById(R.id.nextTurnDistance);
                    if (distance >= 1000) {
                        distanceView.setText(String.format("%.1fkm", distance / 1000));
                    } else {
                        distanceView.setText(String.format("%dm", (int)distance));
                    }
                }

                // 회전 방향을 텍스트로 표시
                if (properties.has("turnType")) {
                    String turnType = properties.optString("turnType", "");
                    TextView turnView = findViewById(R.id.turnArrow);
                    turnView.setText(getTurnTypeText(turnType));
                }

                // 도로명
                if (properties.has("description")) {
                    TextView roadNameView = findViewById(R.id.roadName);
                    roadNameView.setText(properties.optString("description", ""));
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error updating navigation guide", e);
        }
    }

    // 회전 타입을 텍스트로 변환
    private String getTurnTypeText(String turnType) {
        switch (turnType) {
            case "11": return "직진";
            case "12": return "좌회전";
            case "13": return "우회전";
            case "14": return "유턴";
            case "16": return "8시방향";
            case "17": return "10시방향";
            case "18": return "2시방향";
            case "19": return "4시방향";
            default: return "직진";
        }
    }



    // 거리 포맷팅
    private String formatDistance(double distanceInMeters) {
        if (distanceInMeters >= 1000) {
            return String.format("%.1fkm", distanceInMeters / 1000);
        } else {
            return String.format("%.0fm", distanceInMeters);
        }
    }

    private void checkArrival() {
        if (currentLocation != null && targetLocation != null) {
            double distance = calculateDistance(
                    currentLocation.latitude, currentLocation.longitude,
                    targetLocation.latitude, targetLocation.longitude
            );

            if (distance < 50) { // 50미터 이내
                Toast.makeText(this, "목적지 근처에 도착했습니다!", Toast.LENGTH_LONG).show();
                stopNavigation();
            }
        }
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371e3; // 지구 반지름(미터)
        double φ1 = Math.toRadians(lat1);
        double φ2 = Math.toRadians(lat2);
        double Δφ = Math.toRadians(lat2 - lat1);
        double Δλ = Math.toRadians(lon2 - lon1);

        double a = Math.sin(Δφ/2) * Math.sin(Δφ/2) +
                Math.cos(φ1) * Math.cos(φ2) *
                        Math.sin(Δλ/2) * Math.sin(Δλ/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        return R * c;
    }

    private void stopNavigation() {
        isNavigating = false;
        targetLocation = null;
        speedInfo.setVisibility(View.GONE);

        pathOverlay.setMap(null);
        naverMap.setLocationTrackingMode(LocationTrackingMode.None);
    }

    private void checkLocationPermission() {
        String[] permissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };

        if (ContextCompat.checkSelfPermission(this, permissions[0]) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, permissions[1]) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            naverMap.setLocationTrackingMode(LocationTrackingMode.Follow);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (locationSource.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
            if (!locationSource.isActivated()) {
                naverMap.setLocationTrackingMode(LocationTrackingMode.None);
                Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    // 생명주기 메서드들
    @Override protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (haveSensor && haveSensor2) {
            sensorManager.registerListener(sensorEventListener, accelerometer,
                    SensorManager.SENSOR_DELAY_UI);
            sensorManager.registerListener(sensorEventListener, magnetometer,
                    SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (haveSensor && haveSensor2) {
            sensorManager.unregisterListener(sensorEventListener);
        }
    }
    @Override protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        if (isNavigating) {
            stopNavigation();
        }
        if (toneGenerator != null) {
            toneGenerator.release();
        }
    }

    @Override public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}