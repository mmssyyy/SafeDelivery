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
        Animation characterAnimation = AnimationUtils.loadAnimation(this, R.anim.fly);
        characterImageView.startAnimation(characterAnimation);

        ImageView titleImageView = findViewById(R.id.title);
        Animation titleAnimation = AnimationUtils.loadAnimation(this, R.anim.title_scale);
        titleImageView.startAnimation(titleAnimation);
    }

    private void moveToLoginActivity(long delayMillis) {
        // 별도의 스레드에서 지연 작업 수행
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(TitleActivity.this, LoginActivity.class));
                // 현재 액티비티는 백스택에 유지
            }
        }, delayMillis);
    }
}
