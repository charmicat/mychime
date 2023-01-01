package com.vag.mychime.preferences;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.TimePicker;

import androidx.preference.PreferenceDialogFragmentCompat;

import java.util.Calendar;

public class TimePickerDialogFragment extends PreferenceDialogFragmentCompat implements TimePickerDialog.OnTimeSetListener {
    private final String TAG = "TimePickerDialogFragment";

    private int lastHour = 0;
    private int lastMinute = 0;
//    private TimePicker picker = null;
    private TimePickerDialog picker = null;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log.d(TAG, "onCreateDialog");
        // Use the current time as the default values for the picker
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        // Create a new instance of TimePickerDialog and return it
        picker = new TimePickerDialog(getActivity(), this, hour, minute,
                DateFormat.is24HourFormat(getActivity()));

        return picker;
    }

    public void onTimeSet(TimePicker view, int hour, int minute) {
        Log.d(TAG, "onTimeSet hour " + hour + " minute " + minute);
        lastHour = hour;
        lastMinute = minute;

        String time = (lastHour < 10 ? "0" + lastHour
                : String.valueOf(lastHour))
                + ":"
                + (lastMinute < 10 ? "0" + lastMinute
                : String.valueOf(lastMinute));
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            Log.d(TAG, "onCreate");
        }
    }
/*
    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            lastHour = picker.getHour;
            lastMinute = picker.getMinute();

            String time = (lastHour < 10 ? "0" + String.valueOf(lastHour)
                    : String.valueOf(lastHour))
                    + ":"
                    + (lastMinute < 10 ? "0" + String.valueOf(lastMinute)
                    : String.valueOf(lastMinute));

            if (callChangeListener(time)) {
                persistString(time); // TODO: check if String is a good idea
            }

            setSummary(time);
        }
    }

    @NonNull
    @Override
    public CreationExtras getDefaultViewModelCreationExtras() {
        return super.getDefaultViewModelCreationExtras();
    }

    @Override
    protected void onBindDialogView(View v) {
        super.onBindDialogView(v);

        picker.setHour(lastHour);
        picker.setMinute(lastMinute);
    }
 */
}
