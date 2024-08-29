package com.bengohub.VitalsTracker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bengohub.VitalsTracker.Math.Fft;

import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.Math.ceil;

public class BloodPressureProcess extends Activity {

    // Variables Initialization
    private static final String TAG = "BloodPressureProcess";
    private static final AtomicBoolean processing = new AtomicBoolean(false);
    private SurfaceView preview = null;
    private static SurfaceHolder previewHolder = null;
    private static Camera camera = null;
    private static WakeLock wakeLock = null;

    //Toast
    private Toast mainToast;

    //DataBase
    public String user;
    UserDB Data = new UserDB(this);

    //ProgressBar
    private ProgressBar ProgBP;
    public int ProgP = 0;
    public int inc = 0;

    //Beats variable
    public int Beats = 0;
    public double bufferAvgB = 0;

    //Freq + timer variable
    private static long startTime = 0;
    private double SamplingFreq;

    //BloodPressure variables
    public double Gen, Agg, Hei, Wei;
    public double Q = 4.5;
    private static int SP = 0, DP = 0;

    //Arraylist
    public ArrayList<Double> GreenAvgList = new ArrayList<>();
    public ArrayList<Double> RedAvgList = new ArrayList<>();
    public int counter = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blood_pressure_process);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            user = extras.getString("Usr");
        }

        Hei = Integer.parseInt(Data.getheight(user));
        Wei = Integer.parseInt(Data.getweight(user));
        Agg = Integer.parseInt(Data.getage(user));
        Gen = Objects.equals(Data.getgender(user), "M") ? 1 : 2;

        if (Gen == 1) {
            Q = 5;
        }

        // XML - Java Connecting
        preview = findViewById(R.id.preview);
        previewHolder = preview.getHolder();
        previewHolder.addCallback(surfaceCallback);
        previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        ProgBP = findViewById(R.id.BPPB);
        ProgBP.setProgress(0);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "VitalsTracker::DoNotDimScreen");

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onResume() {
        super.onResume();

        wakeLock.acquire();

        camera = Camera.open();

        camera.setDisplayOrientation(90);

        startTime = System.currentTimeMillis();
    }

    @Override
    public void onPause() {
        super.onPause();
        wakeLock.release();
        camera.setPreviewCallback(null);
        camera.stopPreview();
        camera.release();
        camera = null;
    }

    private final PreviewCallback previewCallback = new PreviewCallback() {

        @Override
        public void onPreviewFrame(byte[] data, Camera cam) {
            if (data == null) throw new NullPointerException();
            Camera.Size size = cam.getParameters().getPreviewSize();
            if (size == null) throw new NullPointerException();

            if (!processing.compareAndSet(false, true)) return;

            int width = size.width;
            int height = size.height;

            double GreenAvg;
            double RedAvg;

            GreenAvg = ImageProcessing.decodeYUV420SPtoRedBlueGreenAvg(data.clone(), height, width, 3);
            RedAvg = ImageProcessing.decodeYUV420SPtoRedBlueGreenAvg(data.clone(), height, width, 1);

            GreenAvgList.add(GreenAvg);
            RedAvgList.add(RedAvg);

            ++counter;

            if (RedAvg < 200) {
                inc = 0;
                ProgP = inc;
                counter = 0;
                ProgBP.setProgress(ProgP);
                processing.set(false);
                return;
            }

            long endTime = System.currentTimeMillis();
            double totalTimeInSecs = (endTime - startTime) / 1000d;
            if (totalTimeInSecs >= 30) {

                Double[] Green = GreenAvgList.toArray(new Double[0]);
                Double[] Red = RedAvgList.toArray(new Double[0]);

                SamplingFreq = (counter / totalTimeInSecs);

                double HRFreq = Fft.FFT(Green, counter, SamplingFreq);
                double bpm = (int) ceil(HRFreq * 60);
                double HR1Freq = Fft.FFT(Red, counter, SamplingFreq);
                double bpm1 = (int) ceil(HR1Freq * 60);

                if ((bpm > 45 && bpm < 200)) {
                    if ((bpm1 > 45 && bpm1 < 200)) {
                        bufferAvgB = (bpm + bpm1) / 2;
                    } else {
                        bufferAvgB = bpm;
                    }
                } else if ((bpm1 > 45 && bpm1 < 200)) {
                    bufferAvgB = bpm1;
                }

                if (bufferAvgB < 45 || bufferAvgB > 200) {
                    inc = 0;
                    ProgP = inc;
                    ProgBP.setProgress(ProgP);
                    mainToast = Toast.makeText(getApplicationContext(), "Measurement Failed", Toast.LENGTH_SHORT);
                    mainToast.show();
                    startTime = System.currentTimeMillis();
                    counter = 0;
                    processing.set(false);
                    return;
                }

                Beats = (int) bufferAvgB;

                double ROB = 18.5;
                double ET = (364.5 - 1.23 * Beats);
                double BSA = 0.007184 * (Math.pow(Wei, 0.425)) * (Math.pow(Hei, 0.725));
                double SV = (-6.6 + (0.25 * (ET - 35)) - (0.62 * Beats) + (40.4 * BSA) - (0.51 * Agg));
                double PP = SV / ((0.013 * Wei - 0.007 * Agg - 0.004 * Beats) + 1.307);
                double MPP = Q * ROB;

                SP = (int) (MPP + 3 / 2 * PP);
                DP = (int) (MPP - PP / 3);
            }

            if ((SP != 0) && (DP != 0)) {
                sendResultsToApi(SP, DP, user);

                Intent i = new Intent(BloodPressureProcess.this, BloodPressureResult.class);
                i.putExtra("SP", SP);
                i.putExtra("DP", DP);
                i.putExtra("Usr", user);
                startActivity(i);
                finish();
            }

            ProgP = inc++ / 34;
            ProgBP.setProgress(ProgP);
            processing.set(false);
        }
    };

    private final SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            try {
                camera.setPreviewDisplay(previewHolder);
                camera.setPreviewCallback(previewCallback);
            } catch (Throwable t) {
                Log.e("PreviewDemo-surfaceCallback", "Exception in setPreviewDisplay()", t);
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Camera.Parameters parameters = camera.getParameters();
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);

            Camera.Size size = getSmallestPreviewSize(width, height, parameters);
            if (size != null) {
                parameters.setPreviewSize(size.width, size.height);
                Log.d(TAG, "Using width=" + size.width + " height=" + size.height);
            }

            camera.setParameters(parameters);
            camera.startPreview();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            // Ignore
        }
    };

    private static Camera.Size getSmallestPreviewSize(int width, int height, Camera.Parameters parameters) {
        Camera.Size result = null;

        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            if (size.width <= width && size.height <= height) {
                if (result == null) {
                    result = size;
                } else {
                    int resultArea = result.width * result.height;
                    int newArea = size.width * size.height;

                    if (newArea < resultArea) result = size;
                }
            }
        }

        return result;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent i = new Intent(BloodPressureProcess.this, StartVitalSigns.class);
        i.putExtra("Usr", user);
        startActivity(i);
        finish();
    }

    private String getBaseUrl() {
        SharedPreferences sharedPreferences = getSharedPreferences("ApiSettings", Context.MODE_PRIVATE);
        return sharedPreferences.getString("api_base_url", "http://192.168.8.12:8000/api/");
    }

    private void sendResultsToApi(int sp, int dp, String user) {
        String baseUrl = getBaseUrl();
        String endpoint = baseUrl + "vitals/";

        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(endpoint);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);

                // Fetch user credentials
                String[] credentials = Data.getUserCredentials(user); // Implement getUserCredentials in UserDB
                String userEmail = credentials[0];
                String userPassword = credentials[1];
                String auth = "Basic " + Base64.encodeToString((userEmail + ":" + userPassword).getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
                conn.setRequestProperty("Authorization", auth);

                JSONObject jsonParam = new JSONObject();
                jsonParam.put("blood_pressure", sp + "/" + dp);
                jsonParam.put("user", userEmail);
                Log.d(TAG, user);

                try (OutputStream os = conn.getOutputStream()) {
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));
                    writer.write(jsonParam.toString());
                    writer.flush();
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d(TAG, "Results sent successfully");
                } else {
                    Log.d(TAG, "Failed to send results: " + responseCode);
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception in sending results", e);
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }).start();
    }
}
