package com.example.safedelivery;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class TitleActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_title);

        // 2초 후에 LoginActivity로 이동
        moveToLoginActivity(2000);

        ImageView characterImageView = findViewById(R.id.character);
        Animation characterAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fly);
        characterImageView.startAnimation(characterAnimation);

        ImageView titleImageView = findViewById(R.id.title);
        Animation titleAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.title_scale);
        titleImageView.startAnimation(titleAnimation);
    }

    private void moveToLoginActivity(long delayMillis) {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(TitleActivity.this, LoginActivity.class);
                startActivity(intent);
                finish(); // 현재 액티비티 종료
            }
        }, delayMillis);
    }
}