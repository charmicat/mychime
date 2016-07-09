package com.vag.mychime.preferences;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.view.View;

import com.vag.mychime.activity.R;

public class MyPreferences extends PreferenceFragment implements OnSharedPreferenceChangeListener {

	OnConfigurationSavedListener onCfgSavedCB;
	Preference saveCfg;
	SharedPreferences settings;

	String TAG = "MyPreferences";

	public interface OnConfigurationSavedListener {
		public void onConfigurationSaved();

		public void onUserInteractionFinished(boolean saveChanges);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.preferences);

		saveCfg = (Preference) findPreference("saveCfg");
		saveCfg.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				onCfgSavedCB.onConfigurationSaved();
				saveCfg.setEnabled(false);

				return true;
			}
		});

		saveCfg.setEnabled(false);

		final TimePickerPreference tp_start_speak = (TimePickerPreference) findPreference("speakStartTime");
		final TimePickerPreference tp_end_speak = (TimePickerPreference) findPreference("speakEndTime");
		final TimePickerPreference tp_start_chime = (TimePickerPreference) findPreference("chimeStartTime");
		final TimePickerPreference tp_end_chime = (TimePickerPreference) findPreference("chimeEndTime");

		final CheckBoxPreference enableSpeak = (CheckBoxPreference) findPreference("enableSpeak");
		final CheckBoxPreference enableChime = (CheckBoxPreference) findPreference("enableChime");
		final PreferenceCategory pc_speak = (PreferenceCategory) findPreference("speak");
		final PreferenceCategory pc_chime = (PreferenceCategory) findPreference("chime");

		ListPreference speakEnableList = (ListPreference) findPreference("speakOn");
		if (speakEnableList.getValue() == null) {
			// to ensure we don't get a null value
			// set first value by default
			speakEnableList.setValueIndex(0);
		}
		speakEnableList.setSummary(speakEnableList.getEntry().toString());
		if (!speakEnableList.getValue().equals("speakTimeRange") || !enableSpeak.isChecked()) {
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

		ListPreference chimeEnableList = (ListPreference) findPreference("chimeOn");
		if (chimeEnableList.getValue() == null) {
			// to ensure we don't get a null value
			// set first value by default
			chimeEnableList.setValueIndex(0);
		}
		chimeEnableList.setSummary(chimeEnableList.getEntry().toString());
		if (!chimeEnableList.getValue().equals("chimeTimeRange") || !enableChime.isChecked()) {
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
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		// This makes sure that the container activity has implemented
		// the callback interface. If not, it throws an exception
		try {
			onCfgSavedCB = (OnConfigurationSavedListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement OnConfigurationSavedListener");
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences pref, String key) {
		Log.d(TAG, "onSharedPreferenceChanged");

		saveCfg.setEnabled(true);
	}

	@Override
	public void onResume() {
		super.onResume();
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

		// Fix PreferenceFragment's padding...
		int paddingSize = 0;
		if (Build.VERSION.SDK_INT < 14) {
			paddingSize = (int) (-32 );
		} else {
			paddingSize = (int) (-16 );
		}

		final View v = getView();

		//v.setPadding(paddingSize, 0, paddingSize, 0);
	}

	@Override
	public void onPause() {
		super.onPause();
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}

	public void onDetach() {
		super.onDetach();
		Log.d(TAG, "onDetach");
		if (saveCfg.isEnabled()) { // unsaved changes
			onCfgSavedCB.onUserInteractionFinished(true);
		} else {
			onCfgSavedCB.onUserInteractionFinished(false);
		}

	}

}
