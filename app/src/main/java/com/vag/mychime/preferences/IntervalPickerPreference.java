package com.vag.mychime.preferences;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.CheckBoxPreference;
import androidx.preference.DialogPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;

import com.vag.mychime.R;

public class IntervalPickerPreference extends DialogPreference implements Preference.OnPreferenceChangeListener {
    private final String TAG = "IntervalPickerPreference";

    private int intervalSize = 60;
    private String intervalUnit = "Seconds";
    private IntervalPickerDialogFragment picker = null;
    private String intervalDefinition;
    Context ctx;

    public static int getIntervalSize(String intervalDefinition) {
        String[] pieces = intervalDefinition.split(" ");

        return (Integer.parseInt(pieces[0]));
    }

    public static String getIntervalUnit(String intervalDefinition) {
        String[] pieces = intervalDefinition.split(" ");

        return pieces[1];
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.interval_picker, rootKey);
        EditTextPreference intervalSize = findPreference("intervalSize");
        CheckBoxPreference intervalUnit = findPreference("intervalUnit");

        intervalSize.setSummaryProvider(EditTextPreference.SimpleSummaryProvider.getInstance());
    }

    public IntervalPickerPreference(Context ctxt, AttributeSet attrs) {
        super(ctxt, attrs);
        Log.d(TAG, "IntervalPickerPreference");
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
        intervalDefinition = null;
        Log.d(TAG, "onSetInitialValue");
        if (defaultValue == null) {
            intervalDefinition = getPersistedString("60 Seconds");
        } else {
            intervalDefinition = getPersistedString(defaultValue.toString());
        }

        intervalSize = getIntervalSize(intervalDefinition);
        intervalUnit = getIntervalUnit(intervalDefinition);

        setSummary(intervalDefinition);
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
        Log.d(TAG, "onPreferenceChange");

        return false;
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
		public static final Creator<SavedState> CREATOR = new Creator<IntervalPickerPreference.SavedState>() {

            public IntervalPickerPreference.SavedState createFromParcel(Parcel in) {
                return new IntervalPickerPreference.SavedState(in);
            }

            public IntervalPickerPreference.SavedState[] newArray(int size) {
                return new IntervalPickerPreference.SavedState[size];
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
        myState.value = intervalDefinition;
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
        picker.setIntervalSize(getIntervalSize(myState.value));
        picker.setIntervalUnit(getIntervalUnit(myState.value));

        setSummary(intervalDefinition);
    }
}
