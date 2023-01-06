package com.vag.mychime.preferences;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TimePicker;

import androidx.preference.DialogPreference;

public class TimePickerPreference extends DialogPreference {
    private final String TAG = "TimePickerPreference";

    private int lastHour = 0;
    private int lastMinute = 0;
    private final TimePicker picker = null;
    String time;
    Context ctx;

    public static int getHour(String time) {
        String[] pieces = time.split(":");

        return (Integer.parseInt(pieces[0]));
    }

    public static int getMinute(String time) {
        String[] pieces = time.split(":");

        return (Integer.parseInt(pieces[1]));
    }

    public TimePickerPreference(Context ctxt, AttributeSet attrs) {
        super(ctxt, attrs);
        Log.d(TAG, "TimePickerPreference");
        this.ctx = ctxt;
        setPositiveButtonText("Set");
        setNegativeButtonText("Cancel");
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        Log.d(TAG, "onGetDefaultValue");
        return (a.getString(index));
    }

    @Override
    protected void onSetInitialValue(Object defaultValue) {
        time = null;
        Log.d(TAG, "onSetInitialValue");
        if (defaultValue == null) {
            time = getPersistedString("00:00");
        } else {
            time = getPersistedString(defaultValue.toString());
        }

        lastHour = getHour(time);
        lastMinute = getMinute(time);

        setSummary(time);
    }

    private static class SavedState extends BaseSavedState {
        // Member that holds the setting's value
        String value;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public SavedState(Parcel source) {
            super(source);
            // Get the current preference's value
            value = source.readString();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            // Write the preference's value
            dest.writeString(value);
        }

        // Standard creator object using an instance of this class
        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {

            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Log.d(TAG, "onSaveInstanceState");
        final Parcelable superState = super.onSaveInstanceState();
        // Check whether this Preference is persistent (continually saved)
        if (isPersistent()) {
            // No need to save instance state since it's persistent, use
            // superclass state
            return superState;
        }

        // Create instance of custom BaseSavedState
        final SavedState myState = new SavedState(superState);
        // Set the state's value with the class member that holds current
        // setting value
        myState.value = time;
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        Log.d(TAG, "onRestoreInstanceState");
        // Check whether we saved the state in onSaveInstanceState
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save the state, so call superclass
            super.onRestoreInstanceState(state);
            return;
        }

        // Cast state to custom BaseSavedState and pass to superclass
        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());

        // Set this Preference's widget to reflect the restored state
        picker.setHour(getHour(myState.value));
        picker.setMinute(getMinute(myState.value));

        setSummary(time);
    }
}
