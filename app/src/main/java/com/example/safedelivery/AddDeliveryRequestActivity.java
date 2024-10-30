package com.example.safedelivery;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.geometry.LatLngBounds;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.MapView;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.Marker;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class AddDeliveryRequestActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final String TAG = "AddDeliveryRequest";
    private static final String TMAP_API_KEY = "EBgPuewLXy96lswHVhKiS4G3wexNG7T9ajxuAnOY"; // T map API 키 입력

    private EditText etPickupLocation, etDeliveryLocation, etFee;
    private Button btnSubmit;
    private MapView mapView;
    private NaverMap naverMap;
    private DatabaseReference mDatabase;
    private Marker pickupMarker;
    private Marker deliveryMarker;
    private LatLng pickupLatLng;
    private LatLng deliveryLatLng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_delivery_request);

        initializeViews();
        setupMapView(savedInstanceState);
        setupClickListeners();

        mDatabase = FirebaseDatabase.getInstance().getReference("SafeDelivery").child("deliveryRequests");
    }

    private void initializeViews() {
        etPickupLocation = findViewById(R.id.etPickupLocation);
        etDeliveryLocation = findViewById(R.id.etDeliveryLocation);
        etFee = findViewById(R.id.etFee);
        btnSubmit = findViewById(R.id.btnSubmit);
        mapView = findViewById(R.id.mapView);

        // 마커 초기화
        pickupMarker = new Marker();
        deliveryMarker = new Marker();

        // EditText 클릭 불가능하게 설정 (주소 검색 다이얼로그를 통해서만 입력 가능)
        etPickupLocation.setFocusable(false);
        etDeliveryLocation.setFocusable(false);
    }

    private void setupMapView(Bundle savedInstanceState) {
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
    }

    private void setupClickListeners() {
        // 주소 검색 버튼 클릭 리스너
        ImageButton btnSearchPickup = findViewById(R.id.btnSearchPickup);
        ImageButton btnSearchDelivery = findViewById(R.id.btnSearchDelivery);


        btnSearchPickup.setOnClickListener(v -> showAddressSearchDialog(true));
        btnSearchDelivery.setOnClickListener(v -> showAddressSearchDialog(false));



        btnSubmit.setOnClickListener(v -> addDeliveryRequest());
    }

    private void showAddressSearchDialog(boolean isPickup) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.activity_search);

        EditText etSearch = dialog.findViewById(R.id.etAddressSearch);
        RecyclerView rvResults = dialog.findViewById(R.id.rvSearchResults);
        Button btnSearch = dialog.findViewById(R.id.btnSearch);

        rvResults.setLayoutManager(new LinearLayoutManager(this));
        AddressAdapter adapter = new AddressAdapter(address -> {
            dialog.dismiss();
            setAddress(address, isPickup);
        });
        rvResults.setAdapter(adapter);

        btnSearch.setOnClickListener(v -> {
            String query = etSearch.getText().toString();
            if (!query.isEmpty()) {
                searchAddress(query, adapter);
            }
        });

        dialog.show();
    }



    private void searchAddress(String query, AddressAdapter adapter) {
        new Thread(() -> {
            try {
                URL url = new URL("https://apis.openapi.sk.com/tmap/pois");
                StringBuilder urlBuilder = new StringBuilder(url.toString());
                urlBuilder.append("?version=1")
                        .append("&searchKeyword=").append(URLEncoder.encode(query, "UTF-8"))
                        .append("&searchType=all")  // 모든 유형의 결과 검색
                        .append("&searchtypCd=A")   // 주소 기준 검색
                        .append("&resCoordType=WGS84GEO")
                        .append("&reqCoordType=WGS84GEO")
                        .append("&count=20");       // 결과 개수

                HttpURLConnection conn = (HttpURLConnection) new URL(urlBuilder.toString()).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("appKey", TMAP_API_KEY);

                int responseCode = conn.getResponseCode();
                Log.d(TAG, "Response Code: " + responseCode);

                BufferedReader reader;
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                } else {
                    reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8"));
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    Log.e(TAG, "Error Response: " + errorResponse.toString());
                    throw new IOException("HTTP error code: " + responseCode);
                }

                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                Log.d(TAG, "API Response: " + response.toString());

                JSONObject jsonResponse = new JSONObject(response.toString());
                JSONObject searchPoiInfo = jsonResponse.getJSONObject("searchPoiInfo");
                JSONObject pois = searchPoiInfo.getJSONObject("pois");
                JSONArray poiList = pois.getJSONArray("poi");

                List<AddressItem> addressItems = new ArrayList<>();
                for (int i = 0; i < poiList.length(); i++) {
                    JSONObject poi = poiList.getJSONObject(i);

                    // 상세 주소 구성
                    StringBuilder fullAddress = new StringBuilder();

                    // 기본 주소
                    if (poi.has("upperAddrName")) {
                        fullAddress.append(poi.getString("upperAddrName")).append(" ");
                    }
                    if (poi.has("middleAddrName")) {
                        fullAddress.append(poi.getString("middleAddrName")).append(" ");
                    }
                    if (poi.has("lowerAddrName")) {
                        fullAddress.append(poi.getString("lowerAddrName")).append(" ");
                    }
                    if (poi.has("roadName")) {
                        fullAddress.append(poi.getString("roadName")).append(" ");
                    }
                    if (poi.has("buildingNo1")) {
                        fullAddress.append(poi.getString("buildingNo1"));
                        if (poi.has("buildingNo2") && !poi.getString("buildingNo2").isEmpty()) {
                            fullAddress.append("-").append(poi.getString("buildingNo2"));
                        }
                    }

                    // 건물 이름이 있는 경우 추가
                    if (poi.has("name") && !poi.getString("name").isEmpty()) {
                        fullAddress.append(" (").append(poi.getString("name")).append(")");
                    }

                    // 상세 위치 정보가 있는 경우 추가
                    if (poi.has("detailBizName") && !poi.getString("detailBizName").isEmpty()) {
                        fullAddress.append(" ").append(poi.getString("detailBizName"));
                    }

                    double lat = Double.parseDouble(poi.getString("noorLat"));
                    double lon = Double.parseDouble(poi.getString("noorLon"));

                    AddressItem item = new AddressItem(
                            fullAddress.toString().trim(),
                            new LatLng(lat, lon)
                    );
                    addressItems.add(item);
                }

                runOnUiThread(() -> {
                    adapter.setAddresses(addressItems);
                    if (addressItems.isEmpty()) {
                        Toast.makeText(AddDeliveryRequestActivity.this,
                                "검색 결과가 없습니다.", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error searching address: " + e.getMessage(), e);
                runOnUiThread(() ->
                        Toast.makeText(AddDeliveryRequestActivity.this,
                                "주소 검색 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private void setAddress(AddressItem address, boolean isPickup) {
        if (isPickup) {
            etPickupLocation.setText(address.address);
            pickupLatLng = address.latLng;
            pickupMarker.setPosition(pickupLatLng);
            pickupMarker.setMap(naverMap);
            pickupMarker.setCaptionText("픽업 위치");
        } else {
            etDeliveryLocation.setText(address.address);
            deliveryLatLng = address.latLng;
            deliveryMarker.setPosition(deliveryLatLng);
            deliveryMarker.setMap(naverMap);
            deliveryMarker.setCaptionText("배달 위치");
        }

        // 두 지점이 모두 지도에 표시되도록 카메라 이동
        if (pickupLatLng != null && deliveryLatLng != null) {
            LatLngBounds bounds = new LatLngBounds.Builder()
                    .include(pickupLatLng)
                    .include(deliveryLatLng)
                    .build();
            naverMap.moveCamera(CameraUpdate.fitBounds(bounds, 100));
        } else {
            naverMap.moveCamera(CameraUpdate.scrollTo(
                    isPickup ? pickupLatLng : deliveryLatLng));
        }
    }

    private void startNavigation(LatLng destination) {
        Intent intent = new Intent(this, NavigationActivity.class);
        intent.putExtra("destination", String.format("%.6f,%.6f",
                destination.latitude, destination.longitude));
        startActivity(intent);
    }

    private void addDeliveryRequest() {
        String pickupLocation = etPickupLocation.getText().toString().trim();
        String deliveryLocation = etDeliveryLocation.getText().toString().trim();
        String feeString = etFee.getText().toString().trim();

        if (pickupLocation.isEmpty() || deliveryLocation.isEmpty() || feeString.isEmpty()) {
            Toast.makeText(this, "모든 필드를 채워주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        if (pickupLatLng == null || deliveryLatLng == null) {
            Toast.makeText(this, "올바른 주소를 선택해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        double fee = Double.parseDouble(feeString);

        String key = mDatabase.push().getKey();
        DeliveryRequest newRequest = new DeliveryRequest(
                key,
                pickupLocation,
                deliveryLocation,
                "대기중",
                fee,
                pickupLatLng.latitude,
                pickupLatLng.longitude,
                deliveryLatLng.latitude,
                deliveryLatLng.longitude
        );

        if (key != null) {
            mDatabase.child(key).setValue(newRequest)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "배달 요청이 추가되었습니다", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this,
                            "배달 요청 추가 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        this.naverMap = naverMap;
        naverMap.getUiSettings().setZoomControlEnabled(true);
        naverMap.setMapType(NaverMap.MapType.Basic);
    }

    // MapView 생명주기 메서드들
    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
}