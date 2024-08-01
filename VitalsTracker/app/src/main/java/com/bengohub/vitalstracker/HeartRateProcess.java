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
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bengohub.VitalsTracker.Math.Fft;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.Math.ceil;

public class HeartRateProcess extends Activity {

    // Variables Initialization
    private static final String TAG = "HeartRateMonitor";
    private static final AtomicBoolean processing = new AtomicBoolean(false);
    private SurfaceView preview = null;
    private static SurfaceHolder previewHolder = null;
    private static Camera camera = null;
    private static WakeLock wakeLock = null;

    //Toast
    private Toast mainToast;

    //Beats variable
    public int Beats = 0;
    public double bufferAvgB = 0;

    //DataBase
    public String user;

    //ProgressBar
    private ProgressBar ProgHeart;
    public int ProgP = 0;
    public int inc = 0;

    //Freq + timer variable
    private static long startTime = 0;
    private double SamplingFreq;

    //Arraylist
    public ArrayList<Double> GreenAvgList = new ArrayList<Double>();
    public ArrayList<Double> RedAvgList = new ArrayList<Double>();
    public int counter = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_heart_rate_process);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            user = extras.getString("Usr");
        }

        // XML - Java Connecting
        preview = findViewById(R.id.preview);
        previewHolder = preview.getHolder();
        previewHolder.addCallback(surfaceCallback);
        previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        ProgHeart = findViewById(R.id.HRPB);
        ProgHeart.setProgress(0);

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
                ProgHeart.setProgress(ProgP);
                processing.set(false);
            }

            long endTime = System.currentTimeMillis();
            double totalTimeInSecs = (endTime - startTime) / 1000d;
            if (totalTimeInSecs >= 30) {
                Double[] Green = GreenAvgList.toArray(new Double[GreenAvgList.size()]);
                Double[] Red = RedAvgList.toArray(new Double[RedAvgList.size()]);

                SamplingFreq = (counter / totalTimeInSecs);

                double HRFreq = Fft.FFT(Green, counter, SamplingFreq);
                double bpm = (int) ceil(HRFreq * 60);
                double HR1Freq = Fft.FFT(Red, counter, SamplingFreq);
                double bpm1 = (int) ceil(HR1Freq * 60);

                if ((bpm > 45 || bpm < 200)) {
                    if ((bpm1 > 45 || bpm1 < 200)) {
                        bufferAvgB = (bpm + bpm1) / 2;
                    } else {
                        bufferAvgB = bpm;
                    }
                } else if ((bpm1 > 45 || bpm1 < 200)) {
                    bufferAvgB = bpm1;
                }

                if (bufferAvgB < 45 || bufferAvgB > 200) {
                    inc = 0;
                    ProgP = inc;
                    ProgHeart.setProgress(ProgP);
                    mainToast = Toast.makeText(getApplicationContext(), "Measurement Failed", Toast.LENGTH_SHORT);
                    mainToast.show();
                    startTime = System.currentTimeMillis();
                    counter = 0;
                    processing.set(false);
                    return;
                }

                Beats = (int) bufferAvgB;

                if (Beats != 0) {
                    sendHeartRateResult(Beats, user);
                    Intent i = new Intent(HeartRateProcess.this, HeartRateResult.class);
                    i.putExtra("bpm", Beats);
                    i.putExtra("Usr", user);
                    startActivity(i);
                    finish();
                }
            }

            if (RedAvg != 0) {
                ProgP = inc++ / 34;
                ProgHeart.setProgress(ProgP);
            }

            processing.set(false);
        }
    };

    private void sendHeartRateResult(int bpm, String user) {
        new Thread(() -> {
            try {
                SharedPreferences sharedPreferences = getSharedPreferences("ApiSettings", Context.MODE_PRIVATE);
                String baseUrl = sharedPreferences.getString("api_base_url", "http://192.168.8.12:8000/api/");
                URL url = new URL(baseUrl + "vitals/");

                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setDoOutput(true);
                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty("Content-Type", "application/json");

                JSONObject jsonParam = new JSONObject();
                jsonParam.put("heart_rate", bpm);
                jsonParam.put("user", user);

                OutputStream os = urlConnection.getOutputStream();
                os.write(jsonParam.toString().getBytes(StandardCharsets.UTF_8));
                os.close();

                int responseCode = urlConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d(TAG, "Data sent successfully");
                } else {
                    Log.e(TAG, "Error in sending data: " + responseCode);
                }

                urlConnection.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error in sending heart rate result", e);
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
}