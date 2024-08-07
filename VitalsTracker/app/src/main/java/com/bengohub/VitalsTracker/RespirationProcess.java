package com.bengohub.VitalsTracker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bengohub.VitalsTracker.Math.Fft2;
import com.google.gson.Gson;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Base64;

import static java.lang.Math.ceil;

public class RespirationProcess extends Activity {

    private static final String TAG = "HeartRateMonitor";
    private static final AtomicBoolean processing = new AtomicBoolean(false);
    private SurfaceView preview = null;
    private static SurfaceHolder previewHolder = null;
    private static Camera camera = null;
    private static PowerManager.WakeLock wakeLock = null;

    private Toast mainToast;

    public String user;
    UserDB Data = new UserDB(this);

    private ProgressBar ProgRR;
    public int ProgP = 0;
    public int inc = 0;

    public int Breath = 0;
    public double bufferAvgBr = 0;

    private static long startTime = 0;
    private double SamplingFreq;

    public ArrayList<Double> GreenAvgList = new ArrayList<>();
    public ArrayList<Double> RedAvgList = new ArrayList<>();
    public int counter = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_respiration_process);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            user = extras.getString("Usr");
        }

        preview = findViewById(R.id.preview);
        previewHolder = preview.getHolder();
        previewHolder.addCallback(surfaceCallback);
        previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        ProgRR = findViewById(R.id.HRPB);
        ProgRR.setProgress(0);

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

        wakeLock.acquire(10*60*1000L /*10 minutes*/);

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

    private final Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {

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
                ProgRR.setProgress(ProgP);
                processing.set(false);
            }

            long endTime = System.currentTimeMillis();
            double totalTimeInSecs = (endTime - startTime) / 1000d;
            if (totalTimeInSecs >= 30) {

                Double[] Green = GreenAvgList.toArray(new Double[0]);
                Double[] Red = RedAvgList.toArray(new Double[0]);

                SamplingFreq = (counter / totalTimeInSecs);
                double RRFreq = Fft2.FFT(Green, counter, SamplingFreq);
                double bpm = (int) ceil(RRFreq * 60);
                double RR1Freq = Fft2.FFT(Red, counter, SamplingFreq);
                double breath1 = (int) ceil(RR1Freq * 60);

                bufferAvgBr = (bpm + breath1) / 2;

                if (bufferAvgBr < 10 || bufferAvgBr > 24) {
                    inc = 0;
                    ProgP = inc;
                    ProgRR.setProgress(ProgP);
                    mainToast = Toast.makeText(getApplicationContext(), "Measurement Failed", Toast.LENGTH_SHORT);
                    mainToast.show();
                    startTime = System.currentTimeMillis();
                    counter = 0;
                    processing.set(false);
                    return;
                }
                Breath = (int) bufferAvgBr;

                // Send results to API
                sendResultsToApi(Breath, user);
            }

            if (Breath != 0) {
                Intent i = new Intent(RespirationProcess.this, RespirationResult.class);
                i.putExtra("bpm", Breath);
                i.putExtra("Usr", user);
                startActivity(i);
                finish();
            }

            if (RedAvg != 0) {
                ProgP = inc++ / 34;
                ProgRR.setProgress(ProgP);
            }
            processing.set(false);
        }
    };

    private void sendResultsToApi(int breath, String user) {
        // Get user credentials from the database
        String[] credentials = Data.getUserCredentials(user); // Assume this method returns an array with email and password
        String email = credentials[0];
        String password = credentials[1];

        SharedPreferences sharedPreferences = getSharedPreferences("ApiSettings", Context.MODE_PRIVATE);
        String baseUrl = sharedPreferences.getString("api_base_url", "http://192.168.8.12:8000/api/");

        String url = baseUrl + "vitals/"; // Adjust the endpoint accordingly

        // Create JSON object for the request body
        UserResult userResult = new UserResult(email, breath);
        String json = new Gson().toJson(userResult);

        new Thread(() -> {
            HttpURLConnection urlConnection = null;
            try {
                URL requestUrl = new URL(url);
                urlConnection = (HttpURLConnection) requestUrl.openConnection();
                urlConnection.setDoOutput(true);
                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                urlConnection.setRequestProperty("Accept", "application/json");

                // Set Basic Authentication header
                String auth = email + ":" + password;
                String encodedAuth = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
                }
                String authHeader = "Basic " + encodedAuth;
                urlConnection.setRequestProperty("Authorization", authHeader);

                try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(urlConnection.getOutputStream(), "UTF-8"))) {
                    writer.write(json);
                    writer.flush();
                }

                int responseCode = urlConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d(TAG, "Response: Success");
                } else {
                    Log.e(TAG, "Response: Failed with code " + responseCode);
                }
            } catch (IOException e) {
                Log.e(TAG, "Error sending results to API", e);
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
        }).start();
    }

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
            // Release resources if needed
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
        Intent i = new Intent(RespirationProcess.this, StartVitalSigns.class);
        i.putExtra("Usr", user);
        startActivity(i);
        finish();
    }
}

class UserResult {
    private String user;
    private int respiration_rate;

    public UserResult(String user, int breath) {
        this.user = user;
        this.respiration_rate = breath;
    }

    public String getUser() {
        return user;
    }

    public int getBreath() {
        return respiration_rate;
    }
}
