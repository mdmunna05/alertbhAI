package com.example.alertbhai;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_PERMISSIONS = 1001;

    private TextView tvSavedContact;
    private TextView tvMonitoringState;
    private Button btnStartMonitoring;
    private Button btnTestEmergency;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnGrantPermissions = findViewById(R.id.btnGrantPermissions);
        Button btnSetupContact = findViewById(R.id.btnSetupContact);
        btnStartMonitoring = findViewById(R.id.btnStartMonitoring);
        btnTestEmergency = findViewById(R.id.btnTestEmergency);
        tvSavedContact = findViewById(R.id.tvSavedContact);
        tvMonitoringState = findViewById(R.id.tvMonitoringState);

        btnGrantPermissions.setOnClickListener(v -> requestCorePermissions());
        btnSetupContact.setOnClickListener(v -> showContactSetupDialog());
        btnStartMonitoring.setOnClickListener(v -> toggleMonitoring());
        btnTestEmergency.setOnClickListener(v -> openEmergencyPopup("manual_test"));

        updateSavedContactUi();
        updateMonitoringUi();
        animateMainUi(btnGrantPermissions, btnSetupContact);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateSavedContactUi();
        updateMonitoringUi();
    }

    private void requestCorePermissions() {
        List<String> requiredPermissions = new ArrayList<>();
        requiredPermissions.add(Manifest.permission.SEND_SMS);
        requiredPermissions.add(Manifest.permission.CALL_PHONE);
        requiredPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        requiredPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requiredPermissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        List<String> missingPermissions = new ArrayList<>();
        for (String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }

        if (missingPermissions.isEmpty()) {
            Toast.makeText(this, "Permissions already granted", Toast.LENGTH_SHORT).show();
            return;
        }

        ActivityCompat.requestPermissions(this, missingPermissions.toArray(new String[0]), REQ_PERMISSIONS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQ_PERMISSIONS) {
            return;
        }

        List<String> deniedPermissions = new ArrayList<>();
        List<String> permanentlyDeniedPermissions = new ArrayList<>();

        for (int i = 0; i < permissions.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                deniedPermissions.add(permissions[i]);
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[i])) {
                    permanentlyDeniedPermissions.add(permissions[i]);
                }
            }
        }

        if (deniedPermissions.isEmpty()) {
            Toast.makeText(this, "All permissions granted", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!permanentlyDeniedPermissions.isEmpty()) {
            showOpenSettingsDialog();
        } else {
            Toast.makeText(this, "Some permissions denied. Emergency features may fail.", Toast.LENGTH_LONG).show();
        }
    }

    private void toggleMonitoring() {
        if (!hasAllCorePermissions()) {
            Toast.makeText(this, "Grant permissions first", Toast.LENGTH_SHORT).show();
            requestCorePermissions();
            return;
        }

        Intent serviceIntent = new Intent(this, AccidentService.class);
        if (AccidentService.isMonitoringEnabled(this)) {
            stopService(serviceIntent);
            Toast.makeText(this, "Monitoring stopped", Toast.LENGTH_SHORT).show();
        } else {
            ContextCompat.startForegroundService(this, serviceIntent);
            Toast.makeText(this, "Monitoring started", Toast.LENGTH_SHORT).show();
        }
        updateMonitoringUi();
    }

    private void openEmergencyPopup(String reason) {
        if (!hasAllCorePermissions()) {
            Toast.makeText(this, "Grant permissions first", Toast.LENGTH_SHORT).show();
            requestCorePermissions();
            return;
        }

        Intent intent = new Intent(this, EmergencyActivity.class);
        intent.putExtra(EmergencyActivity.EXTRA_REASON, reason);
        startActivity(intent);
        overridePendingTransition(R.anim.screen_fade_in, R.anim.screen_fade_out);
    }

    private void showOpenSettingsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Permissions Required")
                .setMessage("Location, SMS and Call permissions are required for emergency flow. Please enable them in App Settings.")
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.fromParts("package", getPackageName(), null));
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showContactSetupDialog() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = dpToPx(16);
        container.setPadding(padding, padding, padding, 0);

        EditText etName = new EditText(this);
        etName.setHint("Contact name");

        EditText etPhone = new EditText(this);
        etPhone.setHint("Phone number");
        etPhone.setInputType(InputType.TYPE_CLASS_PHONE);

        String[] savedContact = EmergencyActions.getSavedContact(this);
        etName.setText(savedContact[0]);
        etPhone.setText(savedContact[1]);

        container.addView(etName);
        container.addView(etPhone);

        new AlertDialog.Builder(this)
                .setTitle("Setup Emergency Contact")
                .setView(container)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String phone = etPhone.getText().toString().trim();

                    if (TextUtils.isEmpty(phone)) {
                        Toast.makeText(this, "Phone number is required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    EmergencyActions.saveContact(this, name, phone);
                    updateSavedContactUi();
                    Toast.makeText(this, "Emergency contact saved", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private boolean hasAllCorePermissions() {
        boolean hasMain = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
                && (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return hasMain && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return hasMain;
    }

    private void updateSavedContactUi() {
        String[] contact = EmergencyActions.getSavedContact(this);
        if (TextUtils.isEmpty(contact[1])) {
            tvSavedContact.setText("No emergency contact saved");
        } else {
            String name = TextUtils.isEmpty(contact[0]) ? "Primary Contact" : contact[0];
            tvSavedContact.setText("Saved contact: " + name + " (" + contact[1] + ")");
        }
    }

    private void updateMonitoringUi() {
        boolean isMonitoring = AccidentService.isMonitoringEnabled(this);
        tvMonitoringState.setText(isMonitoring ? "Monitoring: ACTIVE" : "Monitoring: OFF");
        btnStartMonitoring.setText(isMonitoring ? "Stop Monitoring" : "Start Monitoring");
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void animateMainUi(Button btnGrantPermissions, Button btnSetupContact) {
        View headerBar = findViewById(R.id.headerBar);
        View filterChips = findViewById(R.id.filterChips);
        View emergencyCard = findViewById(R.id.cardEmergencyControls);

        animateMainView(headerBar, 0);
        animateMainView(filterChips, 90);
        animateMainView(emergencyCard, 180);

        btnStartMonitoring.startAnimation(AnimationUtils.loadAnimation(this, R.anim.pulse));
        btnTestEmergency.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_slide_up));
        btnGrantPermissions.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_slide_up));
        btnSetupContact.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_slide_up));
    }

    private void animateMainView(View view, long delayMs) {
        view.setAlpha(0f);
        view.setTranslationY(30f);
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(360)
                .setStartDelay(delayMs)
                .start();
    }
}
