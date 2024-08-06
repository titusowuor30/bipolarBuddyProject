package com.bengohub.VitalsTracker.Settings;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bengohub.VitalsTracker.R;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ReminderAdapter extends RecyclerView.Adapter<ReminderAdapter.ReminderViewHolder> {

    private Context context;
    private List<Reminder> reminderList;
    private OnReminderClickListener onReminderClickListener;

    public interface OnReminderClickListener {
        void onReminderClick(Reminder reminder);
    }

    public ReminderAdapter(Context context, List<Reminder> reminderList, OnReminderClickListener onReminderClickListener) {
        this.context = context;
        this.reminderList = reminderList;
        this.onReminderClickListener = onReminderClickListener;
    }

    @NonNull
    @Override
    public ReminderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_reminder, parent, false);
        return new ReminderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReminderViewHolder holder, int position) {
        Reminder reminder = reminderList.get(position);
        holder.bind(reminder, onReminderClickListener);
    }

    @Override
    public int getItemCount() {
        return reminderList.size();
    }

    static class ReminderViewHolder extends RecyclerView.ViewHolder {
        TextView reminderDescription, reminderDatetime,soundName;

        public ReminderViewHolder(@NonNull View itemView) {
            super(itemView);
            reminderDescription = itemView.findViewById(R.id.reminderDescription);
            reminderDatetime = itemView.findViewById(R.id.reminderDatetime);
            soundName=itemView.findViewById(R.id.reminderSound);
        }

        public void bind(Reminder reminder, OnReminderClickListener onReminderClickListener) {
            reminderDescription.setText("Description: "+reminder.getDescription());
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            reminderDatetime.setText("Date & Time: "+dateFormat.format(reminder.getDatetime()));
            soundName.setText("Sound: "+reminder.getSoundUri().split("&")[0].split("=")[1]);

            itemView.setOnClickListener(v -> onReminderClickListener.onReminderClick(reminder));
        }
    }
}
