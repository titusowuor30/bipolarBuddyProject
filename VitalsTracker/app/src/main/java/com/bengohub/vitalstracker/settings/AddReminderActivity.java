package com.bengohub.VitalsTracker.Settings;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.bengohub.VitalsTracker.R;

import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddReminderActivity extends AppCompatActivity {

    private EditText descriptionEditText;
    private CheckBox repeatCheckbox;

    private long selectedDateTime;
    private String selectedSoundUri;
    private Calendar calendar;
    private ExecutorService executorService;
    private int reminderId = -1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder);

        descriptionEditText = findViewById(R.id.descriptionEditText);
        repeatCheckbox = findViewById(R.id.repeatCheckbox);
        Button selectDateButton = findViewById(R.id.selectDateButton);
        Button selectTimeButton = findViewById(R.id.selectTimeButton);
        Button selectSoundButton = findViewById(R.id.selectSoundButton);
        Button saveButton = findViewById(R.id.saveButton);
        Button viewRemindersButton = findViewById(R.id.viewRemindersButton);

        calendar = Calendar.getInstance();
        executorService = Executors.newSingleThreadExecutor();

        selectDateButton.setOnClickListener(v -> showDatePicker());
        selectTimeButton.setOnClickListener(v -> showTimePicker());
        selectSoundButton.setOnClickListener(v -> selectReminderSound());
        saveButton.setOnClickListener(v -> saveReminder());
        viewRemindersButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AddReminderActivity.this, ReminderListActivity.class);
                startActivity(intent);
            }
        });

        // Check if we are editing an existing reminder
        if (getIntent().hasExtra("reminder_id")) {
            reminderId = getIntent().getIntExtra("reminder_id", -1);
            loadReminder(reminderId);
        }
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void showTimePicker() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);
            selectedDateTime = calendar.getTimeInMillis();
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true);
        timePickerDialog.show();
    }

    private void selectReminderSound() {
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            if (uri != null) {
                selectedSoundUri = uri.toString();
            }
        }
    }

    private void saveReminder() {
        String description = descriptionEditText.getText().toString();
        boolean repeat = repeatCheckbox.isChecked();

        // Save or update reminder in the database
        Reminder reminder = new Reminder();
        reminder.setDescription(description);
        reminder.setDatetime(selectedDateTime);
        reminder.setRepeat(repeat);
        reminder.setSoundUri(selectedSoundUri);
        if (reminderId != -1) {
            reminder.setId(reminderId);
        }

        executorService.execute(() -> {
            ReminderDatabase db = ReminderDatabase.getInstance(this);
            if (reminderId == -1) {
                db.reminderDao().insert(reminder);
            } else {
                db.reminderDao().update(reminder);
            }
            runOnUiThread(() -> {
                // Schedule or update reminder
                scheduleReminder(reminder);
                Toast.makeText(this, "Reminder saved!", Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }

    private void loadReminder(int id) {
        executorService.execute(() -> {
            Reminder reminder = ReminderDatabase.getInstance(this).reminderDao().getReminderById(id);
            if (reminder != null) {
                runOnUiThread(() -> {
                    descriptionEditText.setText(reminder.getDescription());
                    repeatCheckbox.setChecked(reminder.isRepeat());
                    calendar.setTimeInMillis(reminder.getDatetime());
                    selectedDateTime = reminder.getDatetime();
                    selectedSoundUri = reminder.getSoundUri();
                });
            }
        });
    }
    private void scheduleReminder(Reminder reminder) {
        Intent intent = new Intent(this, ReminderReceiver.class);
        intent.putExtra("reminder_id", reminder.getId());
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                reminder.getId(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE // Use FLAG_IMMUTABLE for security
        );

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminder.getDatetime(), pendingIntent);
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.S)
    private void requestExactAlarmPermission() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
        startActivity(intent);
    }
}
