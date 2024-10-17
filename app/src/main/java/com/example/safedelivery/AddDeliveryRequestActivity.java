package com.example.safedelivery;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class AddDeliveryRequestActivity extends AppCompatActivity {

    private EditText etPickupLocation, etDeliveryLocation, etFee;
    private Button btnSubmit;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_delivery_request);

        mDatabase = FirebaseDatabase.getInstance().getReference("SafeDelivery").child("deliveryRequests");

        etPickupLocation = findViewById(R.id.etPickupLocation);
        etDeliveryLocation = findViewById(R.id.etDeliveryLocation);
        etFee = findViewById(R.id.etFee);
        btnSubmit = findViewById(R.id.btnSubmit);

        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addDeliveryRequest();
            }
        });
    }

    private void addDeliveryRequest() {
        String pickupLocation = etPickupLocation.getText().toString().trim();
        String deliveryLocation = etDeliveryLocation.getText().toString().trim();
        String feeString = etFee.getText().toString().trim();

        if (pickupLocation.isEmpty() || deliveryLocation.isEmpty() || feeString.isEmpty()) {
            Toast.makeText(this, "모든 필드를 채워주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        double fee = Double.parseDouble(feeString);

        String key = mDatabase.push().getKey();
        DeliveryRequest newRequest = new DeliveryRequest(key, pickupLocation, deliveryLocation, "대기중", fee);

        if (key != null) {
            mDatabase.child(key).setValue(newRequest)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(AddDeliveryRequestActivity.this, "배달 요청이 추가되었습니다", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(AddDeliveryRequestActivity.this, "배달 요청 추가 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }
}