package com.vag.mychime.preferences;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.CheckBoxPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;

import com.vag.mychime.R;

public class MyPreferences extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    OnConfigurationChangedListener onCfgChangedCB;
    SharedPreferences settings;

    final String TAG = "MyPreferences";

    // name for sharedPreferences location
    private static final String SHARED_PREFERENCES = "mychimeprefs";
    private static final String DIALOG_FRAGMENT_TAG =
            "androidx.preference.PreferenceFragmentCompat.DIALOG";

    boolean hasVibration;

    public interface OnConfigurationChangedListener {
        void onConfigurationChanged();
    }

    public MyPreferences() {
        super();
        Log.d(TAG, "MyPreferences ctr");
    }

    private Preference findAndAssertPreference(String key) {
        Preference p = findPreference(key);
        assert p != null;
        return p;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
//        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreatePreferences rootKey:" + rootKey);

        settings = PreferenceManager.getDefaultSharedPreferences(getContext());
        hasVibration = settings.getBoolean("hasVibration", false);
        setPreferencesFromResource(R.xml.root_preferences, rootKey);

        final PreferenceCategory pc_speak = (PreferenceCategory) findAndAssertPreference("speak");
        final PreferenceCategory pc_chime = (PreferenceCategory) findAndAssertPreference("chime");
        final PreferenceCategory pc_vibration = (PreferenceCategory) findAndAssertPreference("vibration");
        final CheckBoxPreference enableSpeak = (CheckBoxPreference) findAndAssertPreference("enableSpeak");
        final CheckBoxPreference enableChime = (CheckBoxPreference) findAndAssertPreference("enableChime");
        final CheckBoxPreference enableVibration = (CheckBoxPreference) findAndAssertPreference("enableVibration");
        final TimePickerPreference tp_start_speak = (TimePickerPreference) findAndAssertPreference("speakStartTime");
        final TimePickerPreference tp_end_speak = (TimePickerPreference) findAndAssertPreference("speakEndTime");
        final TimePickerPreference tp_start_chime = (TimePickerPreference) findAndAssertPreference("chimeStartTime");
        final TimePickerPreference tp_end_chime = (TimePickerPreference) findAndAssertPreference("chimeEndTime");
        final TimePickerPreference tp_start_vibration = (TimePickerPreference) findAndAssertPreference("vibrationStartTime");
        final TimePickerPreference tp_end_vibration = (TimePickerPreference) findAndAssertPreference("vibrationEndTime");

        enableSpeak.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean v = Boolean.parseBoolean(newValue.toString());
            if (!v) {
                pc_speak.removePreference(tp_end_speak);
                pc_speak.removePreference(tp_start_speak);
            }
            return true;
        });
        ListPreference speakEnableList = (ListPreference) findAndAssertPreference("speakOn");
        if (speakEnableList.getValue() == null) {
            // to ensure we don't get a null value
            // set first value by default
            speakEnableList.setValueIndex(0);
        }
        speakEnableList.setSummary(speakEnableList.getEntry().toString());
        if (!enableSpeak.isChecked() || !speakEnableList.getValue().equals("speakTimeRange")) {
            pc_speak.removePreference(tp_end_speak);
            pc_speak.removePreference(tp_start_speak);
        }
        speakEnableList.setOnPreferenceChangeListener((preference, newValue) -> {
            ListPreference lp = (ListPreference) preference;
            CharSequence[] entries = lp.getEntries();
            preference.setSummary(entries[lp.findIndexOfValue((String) newValue)]);

            if (!newValue.equals("speakTimeRange")) {
                pc_speak.removePreference(tp_end_speak);
                pc_speak.removePreference(tp_start_speak);
            } else {
                pc_speak.addPreference(tp_end_speak);
                pc_speak.addPreference(tp_start_speak);
            }
            return true;
        });

        enableChime.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean v = Boolean.parseBoolean(newValue.toString());
            if (!v) {
                pc_chime.removePreference(tp_end_chime);
                pc_chime.removePreference(tp_start_chime);
            }
            return true;
        });
        ListPreference chimeEnableList = (ListPreference) findAndAssertPreference("chimeOn");
        if (chimeEnableList.getValue() == null) {
            // to ensure we don't get a null value
            // set first value by default
            chimeEnableList.setValueIndex(0);
        }
        chimeEnableList.setSummary(chimeEnableList.getEntry().toString());
        if (!enableChime.isChecked() || !chimeEnableList.getValue().equals("chimeTimeRange")) {
            pc_chime.removePreference(tp_end_chime);
            pc_chime.removePreference(tp_start_chime);
        }
        chimeEnableList.setOnPreferenceChangeListener((preference, newValue) -> {
            ListPreference lp = (ListPreference) preference;
            CharSequence[] entries = lp.getEntries();
            preference.setSummary(entries[lp.findIndexOfValue((String) newValue)]);

            if (!newValue.equals("chimeTimeRange")) {
                pc_chime.removePreference(tp_end_chime);
                pc_chime.removePreference(tp_start_chime);
            } else {
                pc_chime.addPreference(tp_end_chime);
                pc_chime.addPreference(tp_start_chime);
            }
            return true;
        });

        ListPreference clockTypeList = (ListPreference) findAndAssertPreference("clockType");
        if (clockTypeList.getValue() == null) {
            // to ensure we don't get a null value
            // set first value by default
            clockTypeList.setValueIndex(0);
        }
        clockTypeList.setSummary(clockTypeList.getValue());
        clockTypeList.setOnPreferenceChangeListener((preference, newValue) -> {
            preference.setSummary(newValue.toString());
            return true;
        });

        if (!hasVibration) {
            pc_vibration.removeAll();
        } else {
            ListPreference vibrationEnableList = (ListPreference) findAndAssertPreference("vibrationOn");
            if (vibrationEnableList.getValue() == null) {
                // to ensure we don't get a null value
                // set first value by default
                vibrationEnableList.setValueIndex(0);
            }
            vibrationEnableList.setSummary(vibrationEnableList.getEntry().toString());
            if (!enableVibration.isChecked() || !vibrationEnableList.getValue().equals("vibrationTimeRange")) {
                pc_vibration.removePreference(tp_end_vibration);
                pc_vibration.removePreference(tp_start_vibration);
            }
            vibrationEnableList.setOnPreferenceChangeListener((preference, newValue) -> {
                ListPreference lp = (ListPreference) preference;
                CharSequence[] entries = lp.getEntries();
                preference.setSummary(entries[lp.findIndexOfValue((String) newValue)]);

                if (!newValue.equals("vibrationTimeRange")) {
                    pc_vibration.removePreference(tp_end_vibration);
                    pc_vibration.removePreference(tp_start_vibration);
                } else {
                    pc_vibration.addPreference(tp_end_vibration);
                    pc_vibration.addPreference(tp_start_vibration);
                }
                return true;
            });
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
//    public void onAttach(Activity activity) {
        super.onAttach(context);
        Log.d(TAG, "got attached to " + context.getPackageName());

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            onCfgChangedCB = (OnConfigurationChangedListener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity() + " must implement OnConfigurationSavedListener");
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences pref, String key) {
        Log.d(TAG, "onSharedPreferenceChanged");

        onCfgChangedCB.onConfigurationChanged();
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    ////////////
    // This method to store the custom preferences changes
    private void savePreferences(String key, String value) {
        Activity activity = getActivity();
        SharedPreferences myPreferences;
        if (activity != null) {
            myPreferences = activity.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
            SharedPreferences.Editor myEditor = myPreferences.edit();
            myEditor.putString(key, value);
            myEditor.apply();
        }
    }

    // This method to restore the custom preferences data
    private String restorePreferences(String key) {
        Activity activity = getActivity();
        SharedPreferences myPreferences;
        if (activity != null) {
            myPreferences = activity.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
            if (myPreferences.contains(key))
                return myPreferences.getString(key, "");
            else return "";
        } else return "";
    }
}