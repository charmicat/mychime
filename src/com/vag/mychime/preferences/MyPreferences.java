package com.vag.mychime.preferences;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.util.Log;

import com.vag.mychime.activity.R;

public class MyPreferences extends PreferenceFragment implements OnSharedPreferenceChangeListener {

	OnConfigurationChangedListener onCfgChangedCB;
	SharedPreferences settings;

	String TAG = "MyPreferences";

	boolean hasVibration;

	public interface OnConfigurationChangedListener {
		public void onConfigurationChanged();
	}

	public MyPreferences(boolean hasVibration) {
		super();
		this.hasVibration = true;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.preferences);

		final PreferenceCategory pc_speak = (PreferenceCategory) findPreference("speak");
		final PreferenceCategory pc_chime = (PreferenceCategory) findPreference("chime");
		final PreferenceCategory pc_vibration = (PreferenceCategory) findPreference("vibration");
		final CheckBoxPreference enableSpeak = (CheckBoxPreference) findPreference("enableSpeak");
		final CheckBoxPreference enableChime = (CheckBoxPreference) findPreference("enableChime");
		final CheckBoxPreference enableVibration = (CheckBoxPreference) findPreference("enableChime");

		final TimePickerPreference tp_start_speak = (TimePickerPreference) findPreference("speakStartTime");
		final TimePickerPreference tp_end_speak = (TimePickerPreference) findPreference("speakEndTime");
		final TimePickerPreference tp_start_chime = (TimePickerPreference) findPreference("chimeStartTime");
		final TimePickerPreference tp_end_chime = (TimePickerPreference) findPreference("chimeEndTime");
		final TimePickerPreference tp_start_vibration = (TimePickerPreference) findPreference("vibrationStartTime");
		final TimePickerPreference tp_end_vibration = (TimePickerPreference) findPreference("vibrationEndTime");

		enableSpeak.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				boolean v = Boolean.valueOf(newValue.toString());
				if (v == false) {
					pc_speak.removePreference(tp_end_speak);
					pc_speak.removePreference(tp_start_speak);
				}

				return true;
			}
		});

		ListPreference speakEnableList = (ListPreference) findPreference("speakOn");
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
		speakEnableList.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
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
			}
		});

		enableChime.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				boolean v = Boolean.valueOf(newValue.toString());
				if (v == false) {
					pc_chime.removePreference(tp_end_chime);
					pc_chime.removePreference(tp_start_chime);
				}

				return true;
			}
		});

		ListPreference chimeEnableList = (ListPreference) findPreference("chimeOn");
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
		chimeEnableList.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
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
			}
		});

		ListPreference clockTypeList = (ListPreference) findPreference("clockType");
		if (clockTypeList.getValue() == null) {
			// to ensure we don't get a null value
			// set first value by default
			clockTypeList.setValueIndex(0);
		}
		clockTypeList.setSummary(clockTypeList.getValue().toString());
		clockTypeList.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				preference.setSummary(newValue.toString());
				return true;
			}
		});

		if (!hasVibration) {
			pc_vibration.removeAll();
		}

		ListPreference vibrationEnableList = (ListPreference) findPreference("vibrationOn");
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
		vibrationEnableList.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
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
			}
		});
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		Log.d(TAG, "got attached to " + activity.getPackageName());

		// This makes sure that the container activity has implemented
		// the callback interface. If not, it throws an exception
		try {
			onCfgChangedCB = (OnConfigurationChangedListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement OnConfigurationSavedListener");
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
}
