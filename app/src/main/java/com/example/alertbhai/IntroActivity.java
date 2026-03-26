package com.example.alertbhai;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.view.View;
import android.view.animation.AnimationUtils;

import androidx.appcompat.app.AppCompatActivity;

public class IntroActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        TextView tvTitle = findViewById(R.id.tvIntroTitle);
        TextView tvTagline = findViewById(R.id.tvIntroTagline);
        TextView tvSubtitle = findViewById(R.id.tvIntroSubtitle);
        Button btnStart = findViewById(R.id.btnStart);

        animateIntroView(tvTitle, 0);
        animateIntroView(tvTagline, 100);
        animateIntroView(tvSubtitle, 180);
        animateIntroView(btnStart, 260);
        btnStart.startAnimation(AnimationUtils.loadAnimation(this, R.anim.pulse));

        btnStart.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            overridePendingTransition(R.anim.screen_fade_in, R.anim.screen_fade_out);
            finish();
        });
    }

    private void animateIntroView(View view, long delayMs) {
        view.setAlpha(0f);
        view.setTranslationY(24f);
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(delayMs)
                .setDuration(360)
                .start();
    }
}

