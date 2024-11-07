package com.example.safedelivery;

import android.os.Bundle;
import android.widget.TextView;
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

public class PointHistoryActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private PointHistoryAdapter adapter;
    private TextView tvTotalPoints;
    private DatabaseReference pointHistoryRef;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_point_history);

        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        pointHistoryRef = FirebaseDatabase.getInstance().getReference("SafeDelivery")
                .child("pointHistory");

        initializeViews();
        loadPointHistory();
    }

    private void initializeViews() {
        tvTotalPoints = findViewById(R.id.tvTotalPoints);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new PointHistoryAdapter();
        recyclerView.setAdapter(adapter);
    }

    private void loadPointHistory() {
        pointHistoryRef.orderByChild("userId").equalTo(userId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<PointHistory> historyList = new ArrayList<>();
                        int usedPoints = 0;

                        for (DataSnapshot data : snapshot.getChildren()) {
                            PointHistory history = data.getValue(PointHistory.class);
                            if (history != null) {
                                history.setId(data.getKey());
                                historyList.add(history);
                                // 사용한 포인트만 합산
                                if (history.getType().equals("사용")) {
                                    usedPoints += history.getPoints();
                                }
                            }
                        }

                        // 시간순 정렬 (최신순)
                        Collections.sort(historyList,
                                (h1, h2) -> Long.compare(h2.getTimestamp(), h1.getTimestamp()));

                        adapter.setItems(historyList);
                        tvTotalPoints.setText(String.format("총 사용 포인트: %dP", usedPoints));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(PointHistoryActivity.this,
                                "데이터 로드 실패", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}