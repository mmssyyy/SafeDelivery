package com.example.safedelivery;

import static androidx.core.location.LocationManagerCompat.getCurrentLocation;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.List;
import android.Manifest;
public class DeliveryListActivity extends AppCompatActivity {

    private FusedLocationProviderClient fusedLocationClient;
    private Location currentLocation;
    private static final String TAG = "DeliveryListActivity";
    private RecyclerView rvDeliveryRequests;
    private DeliveryRequestAdapter adapter;
    private List<DeliveryRequest> deliveryRequests;
    private DatabaseReference mDatabase;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delivery_list);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        rvDeliveryRequests = findViewById(R.id.rvDeliveryRequests);
        rvDeliveryRequests.setLayoutManager(new LinearLayoutManager(this));

        deliveryRequests = new ArrayList<>();
        adapter = new DeliveryRequestAdapter(deliveryRequests, null);
        rvDeliveryRequests.setAdapter(adapter);

        mDatabase = FirebaseDatabase.getInstance().getReference("SafeDelivery").child("deliveryRequests");

        Button btnAddDelivery = findViewById(R.id.btnAddDelivery);
        getCurrentLocation();
        btnAddDelivery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DeliveryListActivity.this, AddDeliveryRequestActivity.class);
                startActivity(intent);
            }
        });

        loadDeliveryRequests();
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // 권한이 없는 경우 권한 요청
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                currentLocation = location;
                adapter = new DeliveryRequestAdapter(deliveryRequests, currentLocation);
                rvDeliveryRequests.setAdapter(adapter);
                loadDeliveryRequests();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한이 승인된 경우 위치 가져오기 재시도
                getCurrentLocation();
            } else {
                // 권한이 거부된 경우
                Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadDeliveryRequests() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d(TAG, "Data changed. Children count: " + dataSnapshot.getChildrenCount());
                deliveryRequests.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        DeliveryRequest request = snapshot.getValue(DeliveryRequest.class);
                        if (request != null) {
                            deliveryRequests.add(request);
                            Log.d(TAG, "Added request: " + request.getId());
                        } else {
                            Log.w(TAG, "Received null request from snapshot: " + snapshot.getKey());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing delivery request", e);
                    }
                }
                adapter.notifyDataSetChanged();
                Log.d(TAG, "Total requests loaded: " + deliveryRequests.size());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error loading data", databaseError.toException());
                Toast.makeText(DeliveryListActivity.this, "데이터 로드 실패: " + databaseError.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}