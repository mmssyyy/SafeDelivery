package com.example.safedelivery;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.List;

public class DeliveryListActivity extends AppCompatActivity {

    private static final String TAG = "DeliveryListActivity";
    private RecyclerView rvDeliveryRequests;
    private DeliveryRequestAdapter adapter;
    private List<DeliveryRequest> deliveryRequests;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delivery_list);



        rvDeliveryRequests = findViewById(R.id.rvDeliveryRequests);
        rvDeliveryRequests.setLayoutManager(new LinearLayoutManager(this));

        deliveryRequests = new ArrayList<>();
        adapter = new DeliveryRequestAdapter(deliveryRequests);
        rvDeliveryRequests.setAdapter(adapter);

        mDatabase = FirebaseDatabase.getInstance().getReference("SafeDelivery").child("deliveryRequests");

        Button btnAddDelivery = findViewById(R.id.btnAddDelivery);
        btnAddDelivery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DeliveryListActivity.this, AddDeliveryRequestActivity.class);
                startActivity(intent);
            }
        });

        loadDeliveryRequests();
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