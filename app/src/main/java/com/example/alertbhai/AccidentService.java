package com.example.alertbhai;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class AccidentService extends Service implements SensorEventListener {

    private static final String PREFS_NAME = "alertbhai_prefs";
    private static final String KEY_MONITORING = "monitoring_enabled";

    private static final String CHANNEL_ID = "alertbhai_monitoring";
    private static final int NOTIFICATION_ID = 1108;

    private static final float ACCIDENT_THRESHOLD_MS2 = 17.0f;
    private static final long SECOND_SPIKE_WINDOW_MS = 1400L;
    private static final long TRIGGER_COOLDOWN_MS = 45000L;

    private SensorManager sensorManager;
    private Sensor accelerometer;

    private float gravityX;
    private float gravityY;
    private float gravityZ;

    private long firstSpikeAt = 0L;
    private long lastTriggerAt = 0L;

    public static boolean isMonitoringEnabled(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_MONITORING, false);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildMonitoringNotification());

        setMonitoringEnabled(true);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (accelerometer != null) {
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        setMonitoringEnabled(false);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) {
            return;
        }

        float alpha = 0.8f;
        gravityX = alpha * gravityX + (1 - alpha) * event.values[0];
        gravityY = alpha * gravityY + (1 - alpha) * event.values[1];
        gravityZ = alpha * gravityZ + (1 - alpha) * event.values[2];

        float linearX = event.values[0] - gravityX;
        float linearY = event.values[1] - gravityY;
        float linearZ = event.values[2] - gravityZ;

        double magnitude = Math.sqrt(linearX * linearX + linearY * linearY + linearZ * linearZ);
        if (magnitude < ACCIDENT_THRESHOLD_MS2) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastTriggerAt < TRIGGER_COOLDOWN_MS) {
            return;
        }

        if (firstSpikeAt == 0L || (now - firstSpikeAt > SECOND_SPIKE_WINDOW_MS)) {
            firstSpikeAt = now;
            return;
        }

        firstSpikeAt = 0L;
        lastTriggerAt = now;
        launchEmergencyPopup();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No-op
    }

    private void launchEmergencyPopup() {
        Intent popupIntent = new Intent(this, EmergencyActivity.class);
        popupIntent.putExtra(EmergencyActivity.EXTRA_REASON, "sensor_detected");
        popupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent fullScreenIntent = PendingIntent.getActivity(
                this,
                2001,
                popupIntent,
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0) | PendingIntent.FLAG_UPDATE_CURRENT
        );

        Notification emergencyNotification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle("Possible accident detected")
                .setContentText("Tap to confirm if you are safe")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setAutoCancel(true)
                .setFullScreenIntent(fullScreenIntent, true)
                .build();

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(1109, emergencyNotification);
        }

        startActivity(popupIntent);
    }

    private Notification buildMonitoringNotification() {
        Intent openAppIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                1001,
                openAppIntent,
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0) | PendingIntent.FLAG_UPDATE_CURRENT
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setContentTitle("AlertbhAI monitoring active")
                .setContentText("Accident detection is running in background")
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "AlertbhAI Monitoring",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Foreground monitoring and emergency alerts");

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    private void setMonitoringEnabled(boolean enabled) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_MONITORING, enabled).apply();
    }
}

