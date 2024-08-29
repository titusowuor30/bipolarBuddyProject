package com.bengohub.VitalsTracker;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class First extends AppCompatActivity {

    public ImageButton Meas;
    public Button acc;
    public EditText ed1, ed2;
    private Toast mainToast;
    public static String passStr, usrStr, checkpassStr, usrStrlow;
    UserDB check = new UserDB(this);
    CheckBox chkRememberMe;
    private SharedPreferences loginPreferences;
    private SharedPreferences.Editor loginPrefsEditor;
    private Boolean saveLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first);

        Meas = findViewById(R.id.prime);
        acc = findViewById(R.id.newacc);
        ed1 = findViewById(R.id.edtu1);
        ed2 = findViewById(R.id.edtp1);
        chkRememberMe = findViewById(R.id.checkBoxRemember);
        loginPreferences = getSharedPreferences("loginPrefs", MODE_PRIVATE);
        loginPrefsEditor = loginPreferences.edit();
        saveLogin = loginPreferences.getBoolean("saveLogin", false);


        if (saveLogin) {
            ed1.setText(loginPreferences.getString("email", ""));
            ed2.setText(loginPreferences.getString("password", ""));
            chkRememberMe.setChecked(true);
        }

        Meas.setOnClickListener(v -> {
            usrStrlow = ed1.getText().toString();
            passStr = ed2.getText().toString();
            usrStr = usrStrlow.toLowerCase();


            if (usrStr.length() < 3 || usrStr.length() > 20) {
                mainToast = Toast.makeText(getApplicationContext(), "email length must be between 3-20 characters", Toast.LENGTH_SHORT);
                mainToast.show();
            }

            if (passStr.length() < 3 || passStr.length() > 20) {
                mainToast = Toast.makeText(getApplicationContext(), "Password length must be between 3-20 characters", Toast.LENGTH_SHORT);
                mainToast.show();
            } else if (passStr.isEmpty() || usrStr.isEmpty()) {
                mainToast = Toast.makeText(getApplicationContext(), "Please enter your Username and Password ", Toast.LENGTH_SHORT);
                mainToast.show();
            } else {
                checkpassStr = check.checkPass(usrStr);
                if (passStr.equals(checkpassStr)) {
                    if (chkRememberMe.isChecked()) {
                        loginPrefsEditor.putBoolean("saveLogin", true);
                        loginPrefsEditor.putString("email", usrStr);
                        loginPrefsEditor.putString("password", passStr);
                        loginPrefsEditor.apply();
                    } else {
                        loginPrefsEditor.clear();
                        loginPrefsEditor.commit();
                    }

                    Intent i = new Intent(v.getContext(), Primary.class);
                    i.putExtra("Usr", usrStr);
                    startActivity(i);
                    finish();
                    // Save the username to SharedPreferences for use in ShakeService
                    SharedPreferences sharedPreferences = getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("User", usrStr);
                    editor.apply();

                } else {
                    //Toast something
                    mainToast = Toast.makeText(getApplicationContext(), "Email/Password is incorrect", Toast.LENGTH_SHORT);
                    mainToast.show();
                }
            }

        });

        acc.setOnClickListener(v -> {
            Intent i = new Intent(v.getContext(), Login.class);
            startActivity(i);
            finish();
        });
    }


}
