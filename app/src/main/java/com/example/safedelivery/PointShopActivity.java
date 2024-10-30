package com.example.safedelivery;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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
import java.util.List;

public class PointShopActivity extends AppCompatActivity {
    private TextView tvCurrentPoints;
    private RecyclerView rvRewards;
    private DatabaseReference userRef;
    private String userId;
    private int currentPoints = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_point_shop);

        // Firebase 초기화
        FirebaseAuth auth = FirebaseAuth.getInstance();
        userId = auth.getCurrentUser().getUid();
        userRef = FirebaseDatabase.getInstance().getReference("SafeDelivery")
                .child("UserAccount").child(userId);

        initializeViews();
        loadUserPoints();
        setupRewardsList();
    }

    private void initializeViews() {
        tvCurrentPoints = findViewById(R.id.tvCurrentPoints);
        rvRewards = findViewById(R.id.rvRewards);
        rvRewards.setLayoutManager(new LinearLayoutManager(this));
    }

    private void loadUserPoints() {
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                UserAccount user = snapshot.getValue(UserAccount.class);
                if (user != null) {
                    currentPoints = user.getPoints();
                    tvCurrentPoints.setText("현재 포인트: " + currentPoints + "P");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(PointShopActivity.this,
                        "포인트 정보 로드 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupRewardsList() {
        List<RewardItem> rewards = new ArrayList<>();
        rewards.add(new RewardItem("계좌 입금", 10000, "포인트를 현금으로 입금받기", R.drawable.ic_bank));
        rewards.add(new RewardItem("문화상품권", 5000, "온라인 문화상품권으로 교환", R.drawable.ic_gift));
        rewards.add(new RewardItem("기프티콘", 3000, "다양한 기프티콘으로 교환", R.drawable.ic_gifticon));

        RewardAdapter adapter = new RewardAdapter(rewards, (reward) -> {
            if (currentPoints >= reward.getPointCost()) {
                showRewardDialog(reward);
            } else {
                Toast.makeText(this, "포인트가 부족합니다", Toast.LENGTH_SHORT).show();
            }
        });
        rvRewards.setAdapter(adapter);
    }

    private void showRewardDialog(RewardItem reward) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_reward_exchange, null);
        EditText etInput = dialogView.findViewById(R.id.etInput);
        TextView tvDescription = dialogView.findViewById(R.id.tvDescription);

        String hint = "";
        String description = "";
        switch (reward.getName()) {
            case "계좌 입금":
                hint = "계좌번호를 입력하세요";
                description = "입금받으실 계좌번호를 입력해주세요.\n(예: 농협 123-4567-8901)";
                break;
            case "문화상품권":
                hint = "받으실 이메일을 입력하세요";
                description = "문화상품권을 받으실 이메일 주소를 입력해주세요.";
                break;
            case "기프티콘":
                hint = "받으실 번호를 입력하세요";
                description = "기프티콘을 받으실 휴대폰 번호를 입력해주세요.";
                break;
        }

        etInput.setHint(hint);
        tvDescription.setText(description);

        new AlertDialog.Builder(this)
                .setTitle(reward.getName() + " 교환")
                .setView(dialogView)
                .setPositiveButton("교환하기", (dialog, which) -> {
                    String input = etInput.getText().toString();
                    if (!input.isEmpty()) {
                        exchangeReward(reward, input);
                    }
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void exchangeReward(RewardItem reward, String receiveInfo) {
        // 포인트 차감
        int newPoints = currentPoints - reward.getPointCost();
        userRef.child("points").setValue(newPoints)
                .addOnSuccessListener(aVoid -> {
                    // 교환 내역 저장
                    DatabaseReference historyRef = FirebaseDatabase.getInstance()
                            .getReference("SafeDelivery")
                            .child("RewardHistory")
                            .push();

                    RewardHistory history = new RewardHistory(
                            userId,
                            reward.getName(),
                            reward.getPointCost(),
                            receiveInfo,
                            System.currentTimeMillis()
                    );

                    historyRef.setValue(history)
                            .addOnSuccessListener(aVoid2 -> {
                                Toast.makeText(this, "교환이 완료되었습니다",
                                        Toast.LENGTH_SHORT).show();
                            });
                });
    }
}