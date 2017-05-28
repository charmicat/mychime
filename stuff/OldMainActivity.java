package com.vag.mychime.activity;

import java.util.ArrayList;
import java.util.List;

import com.vag.mychime.preferences.MyPreferences;
import com.vag.mychime.service.TimeService;
import com.vag.mychime.shared.Constants;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class OldMainActivity extends Activity implements
		MyPreferences.OnConfigurationSavedListener {

	String TAG = "MainActivity";
	int isTTSAvailableIntentCode = 666;

	Intent service;
	SharedPreferences settings;
	TimeService s;
	CheckBox chimeCheck;
	boolean isServiceOn, isChimeOn, isSpeakTimeOn, isTTSAvailable;
	ToggleButton speakTime, chime;
	MyPreferences pref;

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder binder) {
			Log.d(TAG, "onServiceConnected");
			s = ((TimeService.MyBinder) binder).getService();
		}

		public void onServiceDisconnected(ComponentName className) {
			Log.d(TAG, "onServiceDisconnected");
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (isIntentAvailable(this, TextToSpeech.Engine.ACTION_CHECK_TTS_DATA)) {
			Intent checkIntent = new Intent();
			checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
			startActivityForResult(checkIntent, isTTSAvailableIntentCode);
		}

		isServiceOn = false;
		service = new Intent(this, TimeService.class);

		setup();
		pref = new MyPreferences();
		// Display the fragment as the main content
		getFragmentManager().beginTransaction().addToBackStack(null)
				.replace(android.R.id.content, pref).commit();
	}

	/*
	 * @Override protected void onCreate(Bundle savedInstanceState) {
	 * super.onCreate(savedInstanceState);
	 * 
	 * TODO: pre fragment, uncomment if needed
	 * setContentView(R.layout.activity_main); settings =
	 * getSharedPreferences(Constants.PREFS_NAME, 0); Spinner spinner =
	 * (Spinner) findViewById(R.id.clock_type); ArrayAdapter<CharSequence>
	 * adapter = ArrayAdapter.createFromResource( this, R.array.clock_type,
	 * android.R.layout.simple_spinner_item); adapter .setDropDownViewResource
	 * (android.R.layout.simple_spinner_dropdown_item );
	 * spinner.setAdapter(adapter);
	 * 
	 * spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
	 * SharedPreferences.Editor editor = settings.edit();
	 * 
	 * @Override public void onItemSelected(AdapterView<?> parent, View view,
	 * int pos, long id) { switch (pos) { case 0: // 12h
	 * editor.putInt("clockType", 12); break; case 1: editor.putInt("clockType",
	 * 24); }
	 * 
	 * editor.commit(); }
	 * 
	 * @Override public void onNothingSelected(AdapterView<?> arg0) { }
	 * 
	 * });
	 * 
	 * spinner.setSelection(settings.getInt("clockType", 12) == 12 ? 0 : 1);
	 * 
	 * ToggleButton speakTime = (ToggleButton) findViewById(R.id.onSpeak);
	 * isSpeakTimeOn = settings.getBoolean("speakTime", false);
	 * speakTime.setChecked(isSpeakTimeOn);
	 * 
	 * ToggleButton chime = (ToggleButton) findViewById(R.id.onChime); isChimeOn
	 * = settings.getBoolean("chime", false); chime.setChecked(isChimeOn);
	 * 
	 * if (isSpeakTimeOn || isChimeOn) { setConfig(null); }
	 * 
	 * }
	 */

	public void setup() {
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		isSpeakTimeOn = settings.getBoolean("speakOn", false);
		isChimeOn = settings.getBoolean("chimeOn", false);

		setConfig();
	}

	public void setConfig() {
		SharedPreferences.Editor editor = settings.edit();

		if ((isSpeakTimeOn || isChimeOn) && !isServiceOn) {
			startService(service);
			bindService(service, mConnection, 0);
			isServiceOn = true;
			Toast toast = Toast.makeText(this,
					getResources().getString(R.string.serviceStarted),
					Toast.LENGTH_LONG);
			toast.show();
			editor.putBoolean("serviceStarted", true);
		} else if ((!isSpeakTimeOn && !isChimeOn) && isServiceOn) {
			unbindService(mConnection);
			stopService(service);
			isServiceOn = false;
			Toast toast = Toast.makeText(this,
					getResources().getString(R.string.serviceStoped),
					Toast.LENGTH_LONG);
			toast.show();
			editor.putBoolean("serviceStarted", false);
		}

		editor.commit();
	}

	public void getState(View v) {
		ToggleButton tb = (ToggleButton) v;
		SharedPreferences.Editor editor = settings.edit();

		if (tb.getId() == R.id.onSpeak) {
			if (tb.isChecked() && isTTSAvailable) {
				editor.putBoolean("speakOn", true);
				isSpeakTimeOn = true;
			} else {
				editor.putBoolean("speakOn", false);
				isSpeakTimeOn = false;
			}
		} else if (tb.getId() == R.id.onChime) {
			if (tb.isChecked()) {
				editor.putBoolean("chimeOn", true);
				isChimeOn = true;
			} else {
				editor.putBoolean("chimeOn", false);
				isChimeOn = false;
			}
		}

		editor.commit();
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "onActivityResult " + resultCode);

		if (requestCode == isTTSAvailableIntentCode) {
			if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
				isTTSAvailable = true;
			} else {
				isTTSAvailable = false;
				// missing data, install it
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(R.string.titleMsg);
				builder.setMessage(R.string.installMsg);
				builder.setPositiveButton(R.string.ok,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								Intent installIntent = new Intent();
								installIntent
										.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
								startActivity(installIntent);
							}
						});

				builder.setNegativeButton(R.string.cancel,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
							}
						});

				builder.create();
				builder.show();

			}
		}
	}

	public void onStop() {
		super.onStop();
		Log.d(TAG, "onStop " + isSpeakTimeOn + " " + isChimeOn);

		if (isServiceOn)
			unbindService(mConnection);
	}

	/**
	 * Indicates whether the specified action can be used as an intent. This
	 * method queries the package manager for installed packages that can
	 * respond to an intent with the specified action. If no suitable package is
	 * found, this method returns false.
	 * 
	 * @param context
	 *            The application's environment.
	 * @param action
	 *            The Intent action to check for availability.
	 * 
	 * @return True if an Intent with the specified action can be sent and
	 *         responded to, false otherwise.
	 */
	public static boolean isIntentAvailable(Context context, String action) {
		final PackageManager packageManager = context.getPackageManager();
		final Intent intent = new Intent(action);
		List<ResolveInfo> list = packageManager.queryIntentActivities(intent,
				PackageManager.MATCH_DEFAULT_ONLY);
		return list.size() > 0;
	}

	@Override
	public void onConfigurationSaved() {
		Log.d(TAG, "onConfigurationSaved");
	}

}
