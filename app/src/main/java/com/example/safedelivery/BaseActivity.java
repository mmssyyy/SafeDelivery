package com.example.safedelivery;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity {
    protected static final int TAB_HOME = 0;
    protected static final int TAB_LIST = 1;
    protected static final int TAB_STORE = 2;
    protected static final int TAB_PROFILE = 3;

    protected int currentTab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupBottomNavigation();
    }

    protected void setupBottomNavigation() {
        ImageButton btnHome = findViewById(R.id.btnHome);
        ImageButton btnList = findViewById(R.id.btnList);
        ImageButton btnStore = findViewById(R.id.btnStore);
        ImageButton btnProfile = findViewById(R.id.btnProfile);

        btnHome.setOnClickListener(v -> {
            if (currentTab != TAB_HOME) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }
        });

        btnList.setOnClickListener(v -> {
            if (currentTab != TAB_LIST) {
                startActivity(new Intent(this, CompletedDeliveriesActivity.class));
                finish();
            }
        });

        btnStore.setOnClickListener(v -> {
            if (currentTab != TAB_STORE) {
                startActivity(new Intent(this, PointShopActivity.class));
                finish();
            }
        });

        btnProfile.setOnClickListener(v -> {
            if (currentTab != TAB_PROFILE) {
                startActivity(new Intent(this, UserProfileActivity.class));
                finish();
            }
        });

        highlightCurrentTab();
    }

    private void highlightCurrentTab() {
        ImageButton currentTabButton = null;
        switch (currentTab) {
            case TAB_HOME:
                currentTabButton = findViewById(R.id.btnHome);
                break;
            case TAB_LIST:
                currentTabButton = findViewById(R.id.btnList);
                break;
            case TAB_STORE:
                currentTabButton = findViewById(R.id.btnStore);
                break;
            case TAB_PROFILE:
                currentTabButton = findViewById(R.id.btnProfile);
                break;
        }

        if (currentTabButton != null) {
            currentTabButton.setColorFilter(Color.parseColor("#FF0000"));
        }
    }
}