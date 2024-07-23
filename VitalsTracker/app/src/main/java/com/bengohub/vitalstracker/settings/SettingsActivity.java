package com.bengohub.VitalsTracker.Settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bengohub.VitalsTracker.R;

public class SettingsActivity extends AppCompatActivity {

    private EditText apiEndpointEditText;
    private Button saveButton;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "ApiSettings";
    private static final String KEY_API_ENDPOINT = "api_endpoint";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize views
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        apiEndpointEditText = findViewById(R.id.api_endpoint);
        saveButton = findViewById(R.id.save_button);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Load saved API endpoint
        String savedEndpoint = sharedPreferences.getString(KEY_API_ENDPOINT, "");
        apiEndpointEditText.setText(savedEndpoint);

        // Set up Save button click listener
        saveButton.setOnClickListener(v -> {
            String apiEndpoint = apiEndpointEditText.getText().toString().trim();
            if (!apiEndpoint.isEmpty()) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(KEY_API_ENDPOINT, apiEndpoint);
                editor.apply();
                finish(); // Close the activity
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
