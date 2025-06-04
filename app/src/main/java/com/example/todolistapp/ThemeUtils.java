package com.example.todolistapp;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Calendar;

public class ThemeUtils {

    public static void applyTimeBasedTheme(View rootView, Context context) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        if (hour >= 6 && hour < 12) {
            // Morning
            rootView.setBackgroundColor(context.getResources().getColor(R.color.morningColor)); // light yellow
        } else if (hour >= 12 && hour < 17) {
            // Afternoon
            rootView.setBackgroundColor(context.getResources().getColor(R.color.afternoonColor)); // light blue
        } else if (hour >= 17 && hour < 20) {
            // Evening
            rootView.setBackgroundColor(context.getResources().getColor(R.color.eveningColor)); // orange
        } else {
            // Night
            rootView.setBackgroundColor(context.getResources().getColor(R.color.nightColor)); // dark/black
        }
    }
}
