package com.example.safedelivery;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class UserProfileActivity extends AppCompatActivity {
    private TextView tvName, tvEmail, tvPoints, tvTotalDeliveries;
    private Button btnEditProfile, btnPointHistory, btnDeliveryHistory,  btnLogout;
    private DatabaseReference userRef;

    private TextView tvAverageSafetyScore, tvTotalSafetyPoints;
    private DatabaseReference completedDeliveriesRef;
    private String userId;
    private FirebaseAuth mAuth;

    private Button btnStartDelivery, btnPointShop;
    private ImageButton btnHome, btnList, btnStore, btnProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        mAuth = FirebaseAuth.getInstance();
        userId = mAuth.getCurrentUser().getUid();
        userRef = FirebaseDatabase.getInstance().getReference("SafeDelivery")
                .child("UserAccount").child(userId);
        completedDeliveriesRef = FirebaseDatabase.getInstance().getReference("SafeDelivery")
                .child("completedDeliveries");

        initializeViews();
        loadUserData();
        setupClickListeners();
        loadAverageSafetyScore();
        loadDeliveryStats();
    }

    private void loadAverageSafetyScore() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("SafeDelivery")
                .child("UserAccount").child(userId);

        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                UserAccount user = snapshot.getValue(UserAccount.class);
                if (user != null) {
                    tvAverageSafetyScore.setText(String.format("평균 안전 점수: %.1f점", user.getAverageSafetyScore()));
                    tvTotalSafetyPoints.setText(String.format("획득한 안전 운전 포인트: %dP", user.getTotalSafetyScore()));
                    tvPoints.setText(String.format("보유 포인트: %dP", user.getPoints()));
                    tvTotalDeliveries.setText(String.format("총 배달 건수: %d건", user.getTotalDeliveries()));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(UserProfileActivity.this,
                        "사용자 정보 로드 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initializeViews() {
        tvName = findViewById(R.id.tvName);
        tvEmail = findViewById(R.id.tvEmail);
        tvPoints = findViewById(R.id.tvPoints);
        tvTotalDeliveries = findViewById(R.id.tvTotalDeliveries);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnPointHistory = findViewById(R.id.btnPointHistory);
        btnDeliveryHistory = findViewById(R.id.btnDeliveryHistory);
        btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> showLogoutDialog());
        tvAverageSafetyScore = findViewById(R.id.tvAverageSafetyScore);  // TextView로 캐스팅
        tvTotalSafetyPoints = findViewById(R.id.tvTotalSafetyPoints);

        btnStartDelivery = findViewById(R.id.btnStartDelivery);
        btnPointShop = findViewById(R.id.btnPointShop);
        btnHome = findViewById(R.id.btnHome);
        btnList = findViewById(R.id.btnList);
        btnStore = findViewById(R.id.btnStore);
        btnProfile = findViewById(R.id.btnProfile);

        // 회원정보 수정 버튼
        btnEditProfile.setOnClickListener(v -> showEditProfileDialog());

        btnPointHistory.setOnClickListener(v -> {
            Intent intent = new Intent(this, PointHistoryActivity.class);
            startActivity(intent);
        });


        // 배달 내역 버튼
        btnDeliveryHistory.setOnClickListener(v -> {
            Intent intent = new Intent(this, CompletedDeliveriesActivity.class);
            startActivity(intent);
        });
    }

    // 로그아웃 확인 다이얼로그 표시
    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("로그아웃")
                .setMessage("정말 로그아웃 하시겠습니까?")
                .setPositiveButton("로그아웃", (dialog, which) -> logout())
                .setNegativeButton("취소", null)
                .show();
    }

    private void logout() {
        // Firebase 로그아웃
        mAuth.signOut();

        // 자동 로그인 정보 삭제
        getSharedPreferences("loginPrefs", MODE_PRIVATE)
                .edit()
                .clear()
                .apply();

        // 로그인 화면으로 이동
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();

        Toast.makeText(this, "로그아웃되었습니다.", Toast.LENGTH_SHORT).show();
    }


    private void loadUserData() {
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                UserAccount user = snapshot.getValue(UserAccount.class);
                if (user != null) {
                    tvName.setText("이름: " + user.getName());
                    tvEmail.setText("이메일: " + user.getEmailId());
                    tvPoints.setText("보유 포인트: " + user.getPoints() + "P");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(UserProfileActivity.this,
                        "데이터 로드 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupClickListeners() {

        // 하단 네비게이션
        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(UserProfileActivity.this, MainActivity.class);
            startActivity(intent);
        });

        btnList.setOnClickListener(v -> {
            Intent intent = new Intent(UserProfileActivity.this, DeliveryListActivity.class);
            startActivity(intent);
        });

        btnStore.setOnClickListener(v -> {
            Intent intent = new Intent(UserProfileActivity.this, PointShopActivity.class);
            startActivity(intent);

        });

        btnProfile.setOnClickListener(v -> {

        });
    }

    private void loadDeliveryStats() {
        completedDeliveriesRef.orderByChild("userId").equalTo(userId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        long totalDeliveries = snapshot.getChildrenCount();
                        tvTotalDeliveries.setText("총 배달 건수: " + totalDeliveries + "건");
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(UserProfileActivity.this,
                                "데이터 로드 실패", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void showEditProfileDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_profile, null);
        EditText etNewName = dialogView.findViewById(R.id.etNewName);
        EditText etNewPassword = dialogView.findViewById(R.id.etNewPassword);

        new AlertDialog.Builder(this)
                .setTitle("회원정보 수정")
                .setView(dialogView)
                .setPositiveButton("수정", (dialog, which) -> {
                    String newName = etNewName.getText().toString();
                    String newPassword = etNewPassword.getText().toString();

                    if (!newName.isEmpty()) {
                        userRef.child("name").setValue(newName);
                    }
                    if (!newPassword.isEmpty()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            user.updatePassword(newPassword)
                                    .addOnSuccessListener(aVoid ->
                                            Toast.makeText(this, "비밀번호가 변경되었습니다.",
                                                    Toast.LENGTH_SHORT).show())
                                    .addOnFailureListener(e ->
                                            Toast.makeText(this, "비밀번호 변경 실패: " + e.getMessage(),
                                                    Toast.LENGTH_SHORT).show());
                        }
                    }
                })
                .setNegativeButton("취소", null)
                .show();
    }
}