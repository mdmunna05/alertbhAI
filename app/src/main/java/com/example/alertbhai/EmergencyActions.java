package com.example.alertbhai;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.telephony.SmsManager;
import android.text.TextUtils;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

public final class EmergencyActions {

    private static final String PREFS_NAME = "alertbhai_prefs";
    private static final String KEY_CONTACT_NAME = "contact_name";
    private static final String KEY_CONTACT_PHONE = "contact_phone";

    private EmergencyActions() {
    }

    public static void saveContact(Context context, String name, String phone) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_CONTACT_NAME, name)
                .putString(KEY_CONTACT_PHONE, sanitizePhone(phone))
                .apply();
    }

    public static String[] getSavedContact(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String name = prefs.getString(KEY_CONTACT_NAME, "");
        String phone = prefs.getString(KEY_CONTACT_PHONE, "");
        return new String[]{name, phone};
    }

    public static void triggerEmergencyFlow(Context context, String reason) {
        String[] contact = getSavedContact(context);
        String contactPhone = contact[1];

        fetchLocation(context, location -> {
            if (!TextUtils.isEmpty(contactPhone)) {
                sendSmsTo(context, contactPhone, buildEmergencyMessage(reason, location));
            }

            boolean calledPrimary = callNumber(context, contactPhone);
            if (!calledPrimary) {
                callNumber(context, "108");
                callNumber(context, "100");
            }
        });
    }

    public static boolean callPrimaryContact(Context context) {
        return callNumber(context, getSavedContact(context)[1]);
    }

    public static boolean callNumber(Context context, String number) {
        if (TextUtils.isEmpty(number)) {
            return false;
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        try {
            Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + sanitizePhone(number)));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static void sendLocationSmsToContact(Context context) {
        String[] contact = getSavedContact(context);
        String contactPhone = contact[1];
        if (TextUtils.isEmpty(contactPhone)) {
            return;
        }

        fetchLocation(context, location -> {
            String message = "Live location from AlertbhAI helper mode: " + buildLocationLink(location);
            sendSmsTo(context, contactPhone, message);
        });
    }

    private static void fetchLocation(Context context, LocationCallback callback) {
        boolean hasFine = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean hasCoarse = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (!hasFine && !hasCoarse) {
            callback.onLocationReady(null);
            return;
        }

        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(context);
        CancellationTokenSource tokenSource = new CancellationTokenSource();

        int priority = hasFine ? Priority.PRIORITY_HIGH_ACCURACY : Priority.PRIORITY_BALANCED_POWER_ACCURACY;
        client.getCurrentLocation(priority, tokenSource.getToken())
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        callback.onLocationReady(location);
                    } else {
                        client.getLastLocation()
                                .addOnSuccessListener(callback::onLocationReady)
                                .addOnFailureListener(e -> callback.onLocationReady(null));
                    }
                })
                .addOnFailureListener(e -> callback.onLocationReady(null));
    }

    private static void sendSmsTo(Context context, String phoneNumber, String message) {
        if (TextUtils.isEmpty(phoneNumber)) {
            return;
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SmsManager.getDefault().sendTextMessage(sanitizePhone(phoneNumber), null, message, null, null);
            } else {
                SmsManager.getDefault().sendTextMessage(sanitizePhone(phoneNumber), null, message, null, null);
            }
        } catch (Exception ignored) {
        }
    }

    private static String buildEmergencyMessage(String reason, Location location) {
        return "AlertbhAI SOS! Possible accident detected (" + reason + "). Please help immediately. " + buildLocationLink(location);
    }

    private static String buildLocationLink(Location location) {
        if (location == null) {
            return "Location unavailable";
        }
        return "https://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude();
    }

    private static String sanitizePhone(String number) {
        if (number == null) {
            return "";
        }
        return number.replaceAll("[^0-9+]", "");
    }

    private interface LocationCallback {
        void onLocationReady(Location location);
    }
}

