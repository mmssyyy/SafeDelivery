package com.example.safedelivery;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CompletedDeliveriesActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private CompletedDeliveryAdapter adapter;
    private List<DeliveryRequest> completedDeliveries;
    private DatabaseReference mDatabase;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_completed_deliveries);

        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // 툴바 설정
        setTitle("완료된 배달");


        // 리사이클러뷰 초기화
        recyclerView = findViewById(R.id.rvCompletedDeliveries);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        completedDeliveries = new ArrayList<>();
        adapter = new CompletedDeliveryAdapter(completedDeliveries);
        recyclerView.setAdapter(adapter);

        // Firebase 초기화 및 데이터 로드
        mDatabase = FirebaseDatabase.getInstance().getReference()
                .child("SafeDelivery")
                .child("completedDeliveries");

        loadCompletedDeliveries();
    }

    private void loadCompletedDeliveries() {
        mDatabase.orderByChild("userId").equalTo(userId)
                .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                completedDeliveries.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    try {
                        DeliveryRequest request = dataSnapshot.getValue(DeliveryRequest.class);
                        if (request != null && request.getUserId() != null
                                && request.getUserId().equals(userId)) {
                            completedDeliveries.add(request);
                            Log.d("CompletedDeliveries", "Added delivery with userId: "
                                    + request.getUserId());
                        }
                    } catch (Exception e) {
                        Log.e("CompletedDeliveries", "Error parsing delivery", e);
                    }
                }
                // 완료 시간 기준으로 정렬 (최신순)
                Collections.sort(completedDeliveries,
                        (a, b) -> Long.compare(b.getCompletedTime(), a.getCompletedTime()));

                adapter.notifyDataSetChanged();
                Log.d("CompletedDeliveries", "Total loaded deliveries: "
                        + completedDeliveries.size());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CompletedDeliveriesActivity.this,
                        "데이터 로드 실패: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}