package com.vag.mychime.preferences;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.CheckBoxPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceDialogFragmentCompat;

import com.vag.mychime.R;

import java.util.Calendar;

public class IntervalPickerDialogFragment extends PreferenceDialogFragmentCompat implements Preference.OnPreferenceChangeListener {
    private final String TAG = "IntervalPickerDialogFragment";

    private int intervalSize = 60;
    private String intervalUnit = "Seconds";
    private IntervalPickerDialogFragment picker = null;
    private String intervalDefinition;

    @Override
    public void onCreate(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.interval_picker, rootKey);
        EditTextPreference intervalSize = findPreference("intervalSize");
        CheckBoxPreference intervalUnit = findPreference("intervalUnit");

        intervalSize.setSummaryProvider(EditTextPreference.SimpleSummaryProvider.getInstance());
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            Log.d(TAG, "onDialogClosed");
        }
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
        return false;
    }
}
