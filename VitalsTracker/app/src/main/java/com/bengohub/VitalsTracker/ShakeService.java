package com.bengohub.VitalsTracker;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ShakeService extends Service implements SensorEventListener {

    private static final String TAG = "ShakeService";
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long lastShakeTime = 0;
    private static final int SHAKE_THRESHOLD_GRAVITY = 2;
    private static final int SHAKE_SLOP_TIME_MS = 500;
    private String user;
    private UserDB userDB;

    @Override
    public void onCreate() {
        super.onCreate();
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        // Initialize UserDB
        userDB = new UserDB(this);

        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Retrieve user from the Intent extras
        user = getUserFromIntent(intent);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
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

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        float gX = x / SensorManager.GRAVITY_EARTH;
        float gY = y / SensorManager.GRAVITY_EARTH;
        float gZ = z / SensorManager.GRAVITY_EARTH;

        float gForce = (float) Math.sqrt(gX * gX + gY * gY + gZ * gZ);

        if (gForce > SHAKE_THRESHOLD_GRAVITY) {
            final long now = System.currentTimeMillis();
            if (lastShakeTime + SHAKE_SLOP_TIME_MS > now) {
                return;
            }
            lastShakeTime = now;

            sendShakeDataToApi(user);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Ignore for this example
    }

    private String getUserFromIntent(Intent intent) {
        if (intent != null && intent.getExtras() != null) {
            String username = intent.getExtras().getString("Usr");
            // Get user credentials from the database
            String[] credentials = userDB.getUserCredentials(username);
            if (credentials != null && credentials.length == 2) {
                return credentials[0]; // Return the email as the user identifier
            } else {
                Log.e(TAG, "User not found in database");
                return "test1@test.com";
            }
        }
        return "test1@test.com";
    }

    private void sendShakeDataToApi(String user) {
        SharedPreferences sharedPreferences = getSharedPreferences("ApiSettings", Context.MODE_PRIVATE);
        String baseUrl = sharedPreferences.getString("api_base_url", "http://192.168.8.12:8000/api/"); // Replace with default URL if not set

        OkHttpClient client = new OkHttpClient();
        MediaType JSON = MediaType.get("application/json; charset=utf-8");

        String json = String.format("{\"user_email\":\"%s\"}", user);
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(baseUrl + "tremors/") // Update with the correct endpoint
                .post(body)
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                Log.e(TAG, "API request failed: " + e.getMessage());
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Unexpected code " + response);
                } else {
                    Log.i(TAG, "Tremor data sent successfully");
                }
            }
        });
    }
}
