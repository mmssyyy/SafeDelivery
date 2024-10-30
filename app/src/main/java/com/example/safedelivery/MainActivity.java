package com.example.safedelivery;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {
    private FirebaseAuth mFirebaseAuth;
    private DatabaseReference mDatabase;
    private TextView tvWelcome;
    private Button btnStartDelivery, btnPointShop;
    private ImageButton btnHome, btnList, btnStore, btnProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mFirebaseAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("SafeDelivery");

        initializeViews();
        loadUserData();
        setupClickListeners();
    }

    private void initializeViews() {
        btnStartDelivery = findViewById(R.id.btnStartDelivery);
        btnPointShop = findViewById(R.id.btnPointShop);

        tvWelcome = findViewById(R.id.tvWelcome);

        // 하단 네비게이션 버튼들
        btnHome = findViewById(R.id.btnHome);
        btnList = findViewById(R.id.btnList);
        btnStore = findViewById(R.id.btnStore);
        btnProfile = findViewById(R.id.btnProfile);
    }

    private void loadUserData() {
        FirebaseUser user = mFirebaseAuth.getCurrentUser();
        if (user != null) {
            mDatabase.child("UserAccount").child(user.getUid())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            UserAccount userAccount = snapshot.getValue(UserAccount.class);
                            if (userAccount != null) {
                                tvWelcome.setText(userAccount.getName() + "님,\n오늘도 안전한 배송 되세요!");
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            // 오류 처리
                        }
                    });
        }
    }

    private void setupClickListeners() {
        // 메인 버튼들
        btnStartDelivery.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, DeliveryListActivity.class);
            startActivity(intent);
        });

        btnPointShop.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PointShopActivity.class);
            startActivity(intent);
        });

        // 하단 네비게이션
        btnHome.setOnClickListener(v -> {
            // 현재 화면이므로 아무 동작 없음
        });

        btnList.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, DeliveryListActivity.class);
            startActivity(intent);
        });

        btnStore.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PointShopActivity.class);
            startActivity(intent);
        });

        btnProfile.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, UserProfileActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // 로그인 상태 체크
        FirebaseUser user = mFirebaseAuth.getCurrentUser();
        if (user == null) {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }
}