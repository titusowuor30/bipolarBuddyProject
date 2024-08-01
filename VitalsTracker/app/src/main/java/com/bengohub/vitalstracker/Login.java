package com.bengohub.VitalsTracker;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Patterns;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class Login extends AppCompatActivity {

    public ImageButton LoginButton;
    public EditText ed1, ed2, ed3, ed4, ed5, ed6, ed7, ed8;
    private Toast mainToast;
    public Spinner GenderSpin;
    public String m1 = "Male";
    public String m2 = "Female";
    public String nameStr, weightStr, heightStr, ageStr, passStr, usrStr, usrStrlow, passConStr, emailStr;
    private int age, weight, height;
    UserDB Data = new UserDB(this);
    UserDB check = new UserDB(this);
    int c, y = 0;
    int check1 = 0;

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^" +
                    "(?=.*[a-zA-Z])" +
                    "(?=.*[@#$%^&+=])" +
                    "(?=\\S+$)" +
                    ".{4,}" +
                    "$");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
        }

        LoginButton = findViewById(R.id.Login);
        ed1 = findViewById(R.id.edth);
        ed2 = findViewById(R.id.edtw);
        ed3 = findViewById(R.id.edtn);
        ed4 = findViewById(R.id.edta);
        ed5 = findViewById(R.id.edtu);
        ed6 = findViewById(R.id.edtp);
        ed7 = findViewById(R.id.edtpc);
        ed8 = findViewById(R.id.edte);
        GenderSpin = findViewById(R.id.SGender);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.Gender, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        GenderSpin.setAdapter(adapter);

        LoginButton.setOnClickListener(v -> {
            check1 = 0;
            heightStr = ed1.getText().toString();
            weightStr = ed2.getText().toString();
            nameStr = ed3.getText().toString();
            ageStr = ed4.getText().toString();
            usrStrlow = ed5.getText().toString();
            passStr = ed6.getText().toString();
            passConStr = ed7.getText().toString();
            emailStr = ed8.getText().toString();
            usrStr = usrStrlow.toLowerCase();

            // Email Validation
            String emailInput = emailStr;
            if (emailInput.isEmpty()) {
                check1 = 1;
                ed8.setError("Field can't be empty");
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
                if (!Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
                    check1 = 1;
                    ed8.setError("Please enter a valid email address");
                } else {
                    check1 = 0;
                    ed8.setError(null);
                }
            }

            // Username Validation
            if (usrStr.isEmpty()) {
                ed5.setError("Field can't be empty");
                check1 = 1;
            } else if (usrStr.length() > 15) {
                ed5.setError("Username too long");
                check1 = 1;
            } else {
                ed5.setError(null);
                check1 = 0;
            }
            c = check.checkUser(usrStr);
            if (c == y) {
                check1 = 1;
                mainToast = Toast.makeText(getApplicationContext(), "Username already exists", Toast.LENGTH_SHORT);
                mainToast.show();
            }

            // Password Validation
            if (passStr.isEmpty()) {
                check1 = 1;
                ed6.setError("Field can't be empty");
            } else if (!PASSWORD_PATTERN.matcher(passStr).matches()) {
                check1 = 1;
                ed6.setError("Password too weak: must contain\nUppercase characters (A-Z)\n" +
                        "Lowercase characters (a-z)\n" +
                        "Digits (0-9)\n" +
                        "Special characters (~!@#$%&*_:;'.?/)");
            } else {
                check1 = 0;
                ed6.setError(null);
            }
            if (!passStr.equals(passConStr)) {
                check1 = 1;
                mainToast = Toast.makeText(getApplicationContext(), "Password don't match!", Toast.LENGTH_SHORT);
                mainToast.show();
            }

            // Checking other Inputs
            if (ageStr.isEmpty() || nameStr.isEmpty() || heightStr.isEmpty() || weightStr.isEmpty() || passStr.isEmpty() || passConStr.isEmpty() || emailStr.isEmpty() || usrStr.isEmpty()) {
                check1 = 1;
                mainToast = Toast.makeText(getApplicationContext(), "Please fill all your data", Toast.LENGTH_SHORT);
                mainToast.show();
            } else if (check1 == 0) {
                check1 = 0;
                age = Integer.parseInt(ageStr);
                weight = Integer.parseInt(weightStr);
                height = Integer.parseInt(heightStr);
                String text = GenderSpin.getSelectedItem().toString();
                int k = 0;

                if (text.equals(m1)) k = 1;
                if (text.equals(m2)) k = 2;

                user per = new user();
                per.setUsername(usrStr);
                per.setname(nameStr);
                per.setage(age);
                per.setemail(emailStr);
                per.setPass(passStr);
                per.setheight(height);
                per.setweight(weight);
                per.setgender(k);
                Data.addUser(per);

                // Retrieve API base URL from SharedPreferences
                SharedPreferences sharedPreferences = getSharedPreferences("ApiSettings", MODE_PRIVATE);
                String apiBaseUrl = sharedPreferences.getString("api_base_url", "http://192.168.8.12:8000/api/");

                // Post user details to API
                postUserDetailsToApi(apiBaseUrl, per);

                Intent i = new Intent(v.getContext(), First.class);
                mainToast = Toast.makeText(getApplicationContext(), "Your account has been created", Toast.LENGTH_SHORT);
                mainToast.show();
                startActivity(i);
                finish();
            }
        });
    }

    private void postUserDetailsToApi(String apiBaseUrl, user user) {
        new Thread(() -> {
            try {
                URL url = new URL(apiBaseUrl + "patients/signup/");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);

                JSONObject jsonParam = new JSONObject();
                jsonParam.put("username", user.getUsername());
                jsonParam.put("first_name", user.getname().split(" ")[0]);
                jsonParam.put("last_name", user.getname().split(" ")[1]);
                jsonParam.put("phone", "+2547xxxxxxxx");
                jsonParam.put("age", user.getage());
                jsonParam.put("email", user.getemail());
                jsonParam.put("password", user.getPass());
                jsonParam.put("height", user.getheight());
                jsonParam.put("weight", user.getweight());
                jsonParam.put("gender", user.getgender());

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonParam.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_CREATED) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String inputLine;
                    StringBuilder response = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();
                    Log.i("Login", "Response: " + response.toString());

                    // Success
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(getApplicationContext(), "User created successfully on server", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                    String inputLine;
                    StringBuilder response = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();
                    Log.e("Login", "Response code: " + responseCode + ", Response: " + response.toString());

                    // Failure
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(getApplicationContext(), "Failed to create user on server", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (JSONException e) {
                Log.e("Login", "JSON Exception: " + e.getMessage());
            } catch (Exception e) {
                Log.e("Login", "Exception: " + e.getMessage());
            }
        }).start();
    }
}
