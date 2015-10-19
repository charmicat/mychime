package com.vag.mychime.preferences;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.util.Log;

import com.vag.mychime.activity.R;

public class MyPreferences extends PreferenceFragment implements
		OnSharedPreferenceChangeListener {

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

		ListPreference speakEnableList = (ListPreference) findPreference("speakOn");
		if (speakEnableList.getValue() == null) {
			// to ensure we don't get a null value
			// set first value by default
			speakEnableList.setValueIndex(0);
		}
		speakEnableList.setSummary(speakEnableList.getValue().toString());
		speakEnableList
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						preference.setSummary(newValue.toString());
						return true;
					}
				});

		ListPreference chimeEnableList = (ListPreference) findPreference("chimeOn");
		if (chimeEnableList.getValue() == null) {
			// to ensure we don't get a null value
			// set first value by default
			chimeEnableList.setValueIndex(0);
		}
		chimeEnableList.setSummary(chimeEnableList.getValue().toString());
		chimeEnableList
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						preference.setSummary(newValue.toString());
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
		clockTypeList
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
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
			throw new ClassCastException(activity.toString()
					+ " must implement OnConfigurationSavedListener");
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
		getPreferenceScreen().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		getPreferenceScreen().getSharedPreferences()
				.unregisterOnSharedPreferenceChangeListener(this);
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
