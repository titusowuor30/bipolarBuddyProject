package com.bengohub.VitalsTracker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Base64;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bengohub.VitalsTracker.Math.Fft;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.Math.ceil;
import static java.lang.Math.sqrt;

public class O2Process extends Activity {

    private static final String TAG = "HeartRateMonitor";
    private static final AtomicBoolean processing = new AtomicBoolean(false);
    private SurfaceView preview = null;
    private static SurfaceHolder previewHolder = null;
    private static Camera camera = null;
    private static PowerManager.WakeLock wakeLock = null;

    private Toast mainToast;
    public String user;
    UserDB Data = new UserDB(this); // Initialize UserDB

    private ProgressBar ProgO2;
    public int ProgP = 0;
    public int inc = 0;
    private static long startTime = 0;
    private double SamplingFreq;
    private static final double RedBlueRatio = 0;
    double Stdr = 0;
    double Stdb = 0;
    double sumred = 0;
    double sumblue = 0;
    public int o2;
    public ArrayList<Double> RedAvgList = new ArrayList<>();
    public ArrayList<Double> BlueAvgList = new ArrayList<>();
    public int counter = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_o2_process);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            user = extras.getString("Usr");
        }

        preview = findViewById(R.id.preview);
        previewHolder = preview.getHolder();
        previewHolder.addCallback(surfaceCallback);
        previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        ProgO2 = findViewById(R.id.O2PB);
        ProgO2.setProgress(0);

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

        wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/);

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
            double RedAvg;
            double BlueAvg;

            RedAvg = ImageProcessing.decodeYUV420SPtoRedBlueGreenAvg(data.clone(), height, width, 1);
            sumred = sumred + RedAvg;
            BlueAvg = ImageProcessing.decodeYUV420SPtoRedBlueGreenAvg(data.clone(), height, width, 2);
            sumblue = sumblue + BlueAvg;

            RedAvgList.add(RedAvg);
            BlueAvgList.add(BlueAvg);

            ++counter;

            if (RedAvg < 200) {
                inc = 0;
                ProgP = inc;
                ProgO2.setProgress(ProgP);
                processing.set(false);
            }

            long endTime = System.currentTimeMillis();
            double totalTimeInSecs = (endTime - startTime) / 1000d;
            if (totalTimeInSecs >= 30) {
                startTime = System.currentTimeMillis();
                SamplingFreq = (counter / totalTimeInSecs);
                Double[] Red = RedAvgList.toArray(new Double[0]);
                Double[] Blue = BlueAvgList.toArray(new Double[0]);
                double HRFreq = Fft.FFT(Red, counter, SamplingFreq);
                double bpm = (int) ceil(HRFreq * 60);

                double meanr = sumred / counter;
                double meanb = sumblue / counter;

                for (int i = 0; i < counter - 1; i++) {
                    Double bufferb = Blue[i];
                    Stdb = Stdb + ((bufferb - meanb) * (bufferb - meanb));
                    Double bufferr = Red[i];
                    Stdr = Stdr + ((bufferr - meanr) * (bufferr - meanr));
                }

                double varr = sqrt(Stdr / (counter - 1));
                double varb = sqrt(Stdb / (counter - 1));

                double R = (varr / meanr) / (varb / meanb);
                double spo2 = 100 - 5 * (R);
                o2 = (int) (spo2);

                if ((o2 < 80 || o2 > 99) || (bpm < 45 || bpm > 200)) {
                    inc = 0;
                    ProgP = inc;
                    ProgO2.setProgress(ProgP);
                    mainToast = Toast.makeText(getApplicationContext(), "Measurement Failed", Toast.LENGTH_SHORT);
                    mainToast.show();
                    startTime = System.currentTimeMillis();
                    counter = 0;
                    processing.set(false);
                    return;
                }

                // Send the results to the API
                new SendResultsTask(o2, bpm, user).execute();
            }

            if (o2 != 0) {
                Intent i = new Intent(O2Process.this, O2Result.class);
                i.putExtra("O2R", o2);
                i.putExtra("Usr", user);
                startActivity(i);
                finish();
            }

            if (RedAvg != 0) {
                ProgP = inc++ / 34;
                ProgO2.setProgress(ProgP);
            }

            processing.set(false);
        }
    };

    private class SendResultsTask extends AsyncTask<Void, Void, Boolean> {
        private int o2;
        private double bpm;
        private String username;

        SendResultsTask(int o2, double bpm, String username) {
            this.o2 = o2;
            this.bpm = bpm;
            this.username = username;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            // Get user credentials from the database
            String[] credentials = Data.getUserCredentials(username); // Ensure this method returns an array with email and password
            if (credentials == null || credentials.length < 2) {
                Log.e(TAG, "Failed to retrieve user credentials.");
                return false;
            }
            String email = credentials[0];
            String password = credentials[1];

            SharedPreferences sharedPreferences = getSharedPreferences("ApiSettings", Context.MODE_PRIVATE);
            String baseUrl = sharedPreferences.getString("api_base_url", "http://192.168.8.12:8000/api/");

            HttpURLConnection urlConnection = null;
            try {
                URL url = new URL(baseUrl + "vitals/"); // Ensure the correct endpoint
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty("Content-Type", "application/json; charset=utf-8");

                String authcredentials = email + ":" + password;
                String basicAuth = "Basic " + Base64.encodeToString(authcredentials.getBytes(), Base64.NO_WRAP);
                urlConnection.setRequestProperty("Authorization", basicAuth);

                String json = String.format("{\"user\":\"%s\",\"o2_saturation\":%d}", email, o2);
                byte[] outputInBytes = json.getBytes(StandardCharsets.UTF_8);
                OutputStream os = urlConnection.getOutputStream();
                os.write(outputInBytes);
                os.close();

                int responseCode = urlConnection.getResponseCode();
                return responseCode == 200;

            } catch (Exception e) {
                Log.e(TAG, "API request failed: " + e.getMessage(), e);
                return false;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                Toast.makeText(O2Process.this, "Results sent successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(O2Process.this, "Failed to send results", Toast.LENGTH_SHORT).show();
            }
        }
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
        Intent i = new Intent(O2Process.this, StartVitalSigns.class);
        i.putExtra("Usr", user);
        startActivity(i);
        finish();
    }
}
