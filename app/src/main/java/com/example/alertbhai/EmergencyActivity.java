package com.example.alertbhai;

import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class EmergencyActivity extends AppCompatActivity {

    public static final String EXTRA_REASON = "extra_reason";

    private TextView tvCountdown;
    private LinearLayout helperPanel;
    private CountDownTimer countDownTimer;
    private boolean emergencyTriggered = false;
    private String reason = "unknown";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency);
        keepScreenVisibleOnLockScreen();

        reason = getIntent().getStringExtra(EXTRA_REASON);
        if (TextUtils.isEmpty(reason)) {
            reason = "unknown";
        }

        tvCountdown = findViewById(R.id.tvCountdown);
        helperPanel = findViewById(R.id.helperPanel);

        Button btnImSafe = findViewById(R.id.btnImSafe);
        Button btnNeedHelp = findViewById(R.id.btnNeedHelp);
        Button btnCallContact = findViewById(R.id.btnCallContact);
        Button btnCall108 = findViewById(R.id.btnCall108);
        Button btnCall100 = findViewById(R.id.btnCall100);
        Button btnShareLocation = findViewById(R.id.btnShareLocation);

        animateEmergencyView(findViewById(R.id.tvEmergencyTitle), 0);
        animateEmergencyView(findViewById(R.id.tvEmergencyHint), 80);
        animateEmergencyView(tvCountdown, 140);
        animateEmergencyView(btnImSafe, 220);
        animateEmergencyView(btnNeedHelp, 280);
        tvCountdown.startAnimation(AnimationUtils.loadAnimation(this, R.anim.pulse));

        btnImSafe.setOnClickListener(v -> handleImSafe());
        btnNeedHelp.setOnClickListener(v -> triggerEmergencyNow("need_help_button"));

        btnCallContact.setOnClickListener(v -> {
            boolean success = EmergencyActions.callPrimaryContact(this);
            if (!success) {
                Toast.makeText(this, "Primary contact unavailable. Calling 108", Toast.LENGTH_SHORT).show();
                EmergencyActions.callNumber(this, "108");
            }
        });

        btnCall108.setOnClickListener(v -> EmergencyActions.callNumber(this, "108"));
        btnCall100.setOnClickListener(v -> EmergencyActions.callNumber(this, "100"));
        btnShareLocation.setOnClickListener(v -> {
            EmergencyActions.sendLocationSmsToContact(this);
            Toast.makeText(this, "Location SMS requested", Toast.LENGTH_SHORT).show();
        });

        startCountdown();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    private void keepScreenVisibleOnLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                            | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                            | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            );
        }
    }

    private void startCountdown() {
        countDownTimer = new CountDownTimer(10_000, 1_000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = Math.max(1, millisUntilFinished / 1000);
                tvCountdown.setText("Auto-alert in " + seconds + "s");
            }

            @Override
            public void onFinish() {
                triggerEmergencyNow("countdown_timeout");
            }
        };
        countDownTimer.start();
    }

    private void handleImSafe() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        Toast.makeText(this, "Marked safe. Alert cancelled.", Toast.LENGTH_SHORT).show();
        finish();
        overridePendingTransition(R.anim.screen_fade_in, R.anim.screen_fade_out);
    }

    private void triggerEmergencyNow(String triggerReason) {
        if (emergencyTriggered) {
            return;
        }
        emergencyTriggered = true;

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        String combinedReason = reason + "_" + triggerReason;
        EmergencyActions.triggerEmergencyFlow(this, combinedReason);

        helperPanel.setVisibility(View.VISIBLE);
        helperPanel.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_slide_up));
        tvCountdown.setText("Emergency actions triggered");
        Toast.makeText(this, "SOS flow started", Toast.LENGTH_SHORT).show();
    }

    private void animateEmergencyView(View view, long delayMs) {
        view.setAlpha(0f);
        view.setTranslationY(30f);
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(340)
                .setStartDelay(delayMs)
                .start();
    }
}


