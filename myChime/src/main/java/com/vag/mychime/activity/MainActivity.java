package com.vag.mychime.activity;

import com.vag.mychime.preferences.MyPreferences;
import com.vag.mychime.service.TimeService;

import com.vag.vaghelper.HelperFunctions;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements MyPreferences.OnConfigurationChangedListener {

	private final String TAG = "MainActivity";
    private final boolean DEBUG = false;
	private final int isTTSAvailableIntentCode = 666;

	private Intent serviceIntent;
	private SharedPreferences settings;
	private TimeService service;
	private boolean isChimeOn, isSpeakTimeOn, isVibrationOn, isTTSAvailable;

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
		setContentView(R.layout.main_activity);

		Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
		setSupportActionBar(myToolbar);

		Log.d(TAG, "onCreate");

		if (HelperFunctions.isIntentAvailable(this, TextToSpeech.Engine.ACTION_CHECK_TTS_DATA, DEBUG)) { //TODO: use gradle build info
			Intent checkIntent = new Intent();
			checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
			startActivityForResult(checkIntent, isTTSAvailableIntentCode);
		}

		serviceIntent = new Intent(this, TimeService.class);

		controlService();

		Vibrator vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
		MyPreferences pref = MyPreferences.newInstance();
		Bundle bundle = new Bundle(1);
		bundle.putBoolean("hasVibration", vibrator.hasVibrator());
		pref.setArguments(bundle);
		getFragmentManager().beginTransaction().add(R.id.preferences_fragment, pref, "preference_fragment").commit();
	}

	private void controlService() {
		settings = PreferenceManager.getDefaultSharedPreferences(getApplication());
		if (!settings.contains("installFlag")) {
			SharedPreferences.Editor editor = settings.edit();
			editor.putInt("installFlag", 1);
			editor.commit();
		}
		boolean isEnabled = getState();
		boolean isServiceRunning = HelperFunctions.isServiceRunning(getApplication(),
				"com.vag.mychime.service.TimeService", DEBUG);
		Log.d(TAG, "isEnabled " + isEnabled);

		if (!isServiceRunning) {
			Log.d(TAG, "Service is not running");
			if (isEnabled) { // service should be running
				Log.d(TAG, "Starting service");

				startService(serviceIntent);
			}
		} else {
			if (!isEnabled) { // service not should be running
				stopService(serviceIntent);
			}
		}
	}

	private boolean getState() {

		if (!foundOldInstall()) {
			isSpeakTimeOn = settings.getBoolean("enableSpeak", false);
			isChimeOn = settings.getBoolean("enableChime", false);
			isVibrationOn = settings.getBoolean("enableVibration", false);

			return (isSpeakTimeOn || isChimeOn || isVibrationOn);
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
				isTTSAvailable = false; //TODO: do something if TTS not available
				// missing data, install it
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(R.string.titleMsg);
				builder.setMessage(R.string.installMsg);
				builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						Intent installIntent = new Intent();
						installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
						startActivity(installIntent);
					}
				});

				builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
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
		Log.d(TAG,
				"onStop isSpeakTimeOn=" + isSpeakTimeOn + " isChimeOn=" + isChimeOn + " isVibrateOn=" + isVibrationOn);

		boolean isServiceRunning = HelperFunctions.isServiceRunning(getApplication(),
				"com.vag.mychime.service.TimeService", DEBUG);

		if (!isServiceRunning) {
			Log.d(TAG, "Service is not running");
			Toast toast = Toast.makeText(this, getResources().getString(R.string.serviceStoped), Toast.LENGTH_LONG);

			toast.show();
		} else {
			Log.d(TAG, "Service is running");
			Toast toast = Toast.makeText(getApplication(), getResources().getString(R.string.serviceStarted),
					Toast.LENGTH_LONG);
			toast.show();
		}
	}

	@Override
	public void onConfigurationChanged() {
		Log.d(TAG, "onConfigurationChanged");
		controlService();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.d(TAG, "onCreateOptionsMenu");
		getMenuInflater().inflate(R.menu.toolbar, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case R.id.rate:
			Log.d(TAG, "Clicked on rate");
			HelperFunctions.rateApp(this, DEBUG);
			return true;

		default:
			// If we got here, the user's action was not recognized.
			// Invoke the superclass to handle it.
			return super.onOptionsItemSelected(item);
		}
	}

	private boolean foundOldInstall() {
		if (settings.contains("speakMuteOn") || settings.contains("chimeMuteOn")) {
			Log.d(TAG, "Found old install, clearing");
			SharedPreferences.Editor editor = settings.edit();
			editor.clear();
			editor.commit();

			return true;
		} else if (settings.contains("installFlag")) {
			// show changelog
		}

		return false;
	}

}
