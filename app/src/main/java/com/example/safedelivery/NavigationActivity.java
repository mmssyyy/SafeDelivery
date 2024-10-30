package com.example.safedelivery;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.List;
import java.util.Locale;

public class NavigationActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final String TAG = "NavigationActivity";
    private static final String TMAP_API_KEY = "EBgPuewLXy96lswHVhKiS4G3wexNG7T9ajxuAnOY";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private static final long UPDATE_INTERVAL = 10000; // 10초

    private MapView mapView;
    private NaverMap naverMap;
    private FusedLocationSource locationSource;
    private TextView navigationInfo;
    private TextView distanceTimeInfo;
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

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

        btnCompleteDelivery = findViewById(R.id.btnCompleteDelivery);
        btnCompleteDelivery.setOnClickListener(v -> showCompletionDialog());

        setupLocationUpdates();
        initializeViews();
        initializeMap(savedInstanceState);
    }


    private void showCompletionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("배달 완료")
                .setMessage("배달을 완료하시겠습니까?")
                .setPositiveButton("완료", (dialog, which) -> completeDelivery())
                .setNegativeButton("취소", null)
                .show();
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
                                        // 현재 포인트에 300 추가
                                        int updatedPoints = user.getPoints() + 300;
                                        userRef.child("points").setValue(updatedPoints);
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
                                                    Toast.makeText(NavigationActivity.this,
                                                            "배달이 완료되었습니다. 300포인트가 적립되었습니다!",
                                                            Toast.LENGTH_SHORT).show();
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
            float speedKPH = speedMPS * 3.6f;     // km/h로 변환

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

        targetLocation = destination;
        isNavigating = true;
        pathOverlay.setMap(null);

        // 네비게이션 모드 설정
        setupNavigationMode();
        speedInfo.setVisibility(View.VISIBLE);

        findPath();
    }

    // 네비게이션 모드 설정
    private void setupNavigationMode() {
        // 지도 회전 모드 활성화
        naverMap.setLocationTrackingMode(LocationTrackingMode.Face);

        // 지도 기울이기
        naverMap.setCameraPosition(new CameraPosition(
                currentLocation,  // 현재 위치
                17,              // 줌 레벨
                45,              // 기울기 각도
                0                // 베어링
        ));

        // 나침반 표시
        naverMap.getUiSettings().setCompassEnabled(true);

        // 현위치 버튼 표시
        naverMap.getUiSettings().setLocationButtonEnabled(true);
    }

    // 실시간 위치 업데이트 및 카메라 이동
    private void updateNavigationCamera(LatLng location) {
        if (!isNavigating) return;

        // 현재 카메라 위치 가져오기
        CameraPosition currentCameraPosition = naverMap.getCameraPosition();

        // 현재 위치와 목적지 위치 사이의 각도 계산
        double bearing = calculateBearing(location, targetLocation);

        CameraPosition cameraPosition = new CameraPosition(
                location,                           // 현재 위치
                currentCameraPosition.zoom,         // 현재 줌 레벨 유지
                currentCameraPosition.tilt,         // 현재 기울기 각도 유지
                bearing                             // 목적지 방향으로 회전
        );

        naverMap.moveCamera(CameraUpdate.toCameraPosition(cameraPosition));
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

        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL("https://apis.openapi.sk.com/tmap/routes?version=1");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("appKey", TMAP_API_KEY);
                conn.setDoOutput(true);

                // Request body 생성
                JSONObject requestBody = new JSONObject();
                requestBody.put("startX", String.format(Locale.US, "%.6f", currentLocation.longitude));
                requestBody.put("startY", String.format(Locale.US, "%.6f", currentLocation.latitude));
                requestBody.put("endX", String.format(Locale.US, "%.6f", targetLocation.longitude));
                requestBody.put("endY", String.format(Locale.US, "%.6f", targetLocation.latitude));
                requestBody.put("reqCoordType", "WGS84GEO");
                requestBody.put("resCoordType", "WGS84GEO");
                requestBody.put("searchOption", "0");

                // Request body 전송
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    BufferedReader errorReader = new BufferedReader(
                            new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    throw new IOException("HTTP error code: " + responseCode +
                            "\nError response: " + errorResponse.toString());
                }

                // 응답 읽기
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                processRouteResponse(response.toString());

            } catch (Exception e) {
                Log.e(TAG, "Error finding path: " + e.getMessage(), e);
                runOnUiThread(() ->
                        Toast.makeText(this, "경로 탐색 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show()
                );
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }).start();
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

    @Override protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override protected void onPause() {
        super.onPause();
        mapView.onPause();
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
    }

    @Override public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}