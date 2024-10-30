package com.example.safedelivery;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
    private Button btnEditProfile, btnPointHistory, btnDeliveryHistory;
    private DatabaseReference userRef;
    private DatabaseReference completedDeliveriesRef;
    private String userId;
    private FirebaseAuth mAuth;

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
        loadDeliveryStats();
    }

    private void initializeViews() {
        tvName = findViewById(R.id.tvName);
        tvEmail = findViewById(R.id.tvEmail);
        tvPoints = findViewById(R.id.tvPoints);
        tvTotalDeliveries = findViewById(R.id.tvTotalDeliveries);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnPointHistory = findViewById(R.id.btnPointHistory);
        btnDeliveryHistory = findViewById(R.id.btnDeliveryHistory);

        // 회원정보 수정 버튼
        btnEditProfile.setOnClickListener(v -> showEditProfileDialog());


        // 배달 내역 버튼
        btnDeliveryHistory.setOnClickListener(v -> {
            Intent intent = new Intent(this, CompletedDeliveriesActivity.class);
            startActivity(intent);
        });
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