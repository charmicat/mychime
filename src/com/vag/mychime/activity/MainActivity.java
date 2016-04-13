package com.vag.mychime.activity;

import com.vag.mychime.preferences.MyPreferences;
import com.vag.mychime.service.TimeService;

import com.vag.vaghelper.HelperFunctions;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends Activity implements
		MyPreferences.OnConfigurationSavedListener {

	private final String TAG = "MainActivity";
	private final int isTTSAvailableIntentCode = 666;

	Intent serviceIntent;
	SharedPreferences settings;
	TimeService service;
	CheckBox chimeCheck;
	boolean isChimeOn, isSpeakTimeOn, isTTSAvailable;
	ToggleButton speakTime, chime;
	MyPreferences pref;

	@SuppressWarnings("unused")
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder binder) {
			Log.d(TAG, "onServiceConnected");
			service = ((TimeService.MyBinder) binder).getService();
		}

		public void onServiceDisconnected(ComponentName className) {
			Log.d(TAG, "onServiceDisconnected");
			service = null;
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate");

		if (HelperFunctions.isIntentAvailable(this,
				TextToSpeech.Engine.ACTION_CHECK_TTS_DATA)) {
			Intent checkIntent = new Intent();
			checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
			startActivityForResult(checkIntent, isTTSAvailableIntentCode);
		}

		serviceIntent = new Intent(this, TimeService.class);

		controlService();
		pref = new MyPreferences();
		// Display the fragment as the main content
		getFragmentManager().beginTransaction().addToBackStack(null)
				.replace(android.R.id.content, pref).commit();
	}

	public void controlService() {
		settings = PreferenceManager
				.getDefaultSharedPreferences(getApplication());
		boolean isEnabled = getState();
		boolean isServiceRunning = HelperFunctions.isServiceRunning(
				getApplication(), "com.vag.mychime.service.TimeService", false);
		Log.d(TAG, "isEnabled " + isEnabled);

		if (!isServiceRunning) {
			Log.d(TAG, "Service is not running");
			if (isEnabled) { // service should be running
				Log.d(TAG, "Starting service");

				startService(serviceIntent);
				Toast toast = Toast.makeText(getApplication(), getResources()
						.getString(R.string.serviceStarted), Toast.LENGTH_LONG);
				toast.show();
			}
		} else {
			if (!isEnabled) { // service not should be running
				stopService(serviceIntent);
				Toast toast = Toast.makeText(this,
						getResources().getString(R.string.serviceStoped),
						Toast.LENGTH_LONG);
				toast.show();
			}
		}
	}

	public boolean getState() {

		if (!foundOldInstall()) {
			isSpeakTimeOn = settings.getBoolean("enableSpeak", false);
			isChimeOn = settings.getBoolean("enableChime", false);

			return (isSpeakTimeOn || isChimeOn);
		} else {
			return false;
		}
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
		Log.d(TAG, "onStop isSpeakTimeOn=" + isSpeakTimeOn + " isChimeOn="
				+ isChimeOn);
	}

	@Override
	public void onConfigurationSaved() {
		Log.d(TAG, "onConfigurationSaved");
		controlService();

	}

	public void onUserInteractionFinished(boolean saveChanges) {
		Log.d(TAG, "onUserInteractionFinished");

		if (saveChanges) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);

			builder.setMessage(R.string.unsavedSettingsMsg).setTitle(
					R.string.unsavedSettingsTitle);

			builder.setPositiveButton(R.string.ok,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							controlService();
							finish();
						}
					});
			builder.setNegativeButton(R.string.cancel,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							finish();
						}
					});

			AlertDialog dialog = builder.create();

			dialog.show();
		} else {
			finish();
		}

	}

	public boolean foundOldInstall() {
		if (settings.contains("speakMuteOn")
				|| settings.contains("chimeMuteOn")) { // old install, reset
			Log.d(TAG, "Found old install, clearing");
			SharedPreferences.Editor editor = settings.edit();
			editor.clear();
			editor.commit();

			return true;
		}

		return false;
	}

}
