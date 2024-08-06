package com.bengohub.VitalsTracker.Settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bengohub.VitalsTracker.R;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReminderListActivity extends AppCompatActivity {

    private RecyclerView reminderRecyclerView;
    private Button addReminderButton;
    private ReminderAdapter reminderAdapter;
    private List<Reminder> reminderList;
    private ExecutorService executorService;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder_list);

        reminderRecyclerView = findViewById(R.id.reminderRecyclerView);
        addReminderButton = findViewById(R.id.addReminderButton);
        reminderRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        executorService = Executors.newSingleThreadExecutor();

        addReminderButton.setOnClickListener(v -> {
            Intent intent = new Intent(ReminderListActivity.this, AddReminderActivity.class);
            startActivity(intent);
        });

        loadReminders();
    }

    private void loadReminders() {
        executorService.execute(() -> {
            reminderList = ReminderDatabase.getInstance(this).reminderDao().getAllReminders();
            runOnUiThread(() -> {
                reminderAdapter = new ReminderAdapter(this, reminderList, this::editReminder);
                reminderRecyclerView.setAdapter(reminderAdapter);
            });
        });
    }

    private void editReminder(Reminder reminder) {
        Intent intent = new Intent(ReminderListActivity.this, AddReminderActivity.class);
        intent.putExtra("reminder_id", reminder.getId());
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadReminders();
    }
}
