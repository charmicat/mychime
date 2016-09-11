package com.vag.mychime.activity;

import com.vag.mychime.preferences.MyPreferences;
import com.vag.mychime.service.TimeService;

import com.vag.vaghelper.HelperFunctions;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
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
import android.widget.CheckBox;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends AppCompatActivity implements MyPreferences.OnConfigurationChangedListener {

	private final String TAG = "MainActivity";
	private final int isTTSAvailableIntentCode = 666;
	private final String prefFragmentTag = "preference_fragment";

	Intent serviceIntent;
	SharedPreferences settings;
	TimeService service;
	CheckBox chimeCheck;
	boolean isChimeOn, isSpeakTimeOn, isVibrationOn, isTTSAvailable, uncomittedChanges;
	ToggleButton speakTime, chime;
	Toolbar myToolbar;
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
		setContentView(R.layout.main_activity);
		myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
		setSupportActionBar(myToolbar);

		Log.d(TAG, "onCreate");

		if (HelperFunctions.isIntentAvailable(this, TextToSpeech.Engine.ACTION_CHECK_TTS_DATA)) {
			Intent checkIntent = new Intent();
			checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
			startActivityForResult(checkIntent, isTTSAvailableIntentCode);
		}

		serviceIntent = new Intent(this, TimeService.class);

		controlService();

		Vibrator vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
		pref = new MyPreferences(vibrator.hasVibrator());
		getFragmentManager().beginTransaction().add(R.id.preferences_fragment, pref, prefFragmentTag).commit();
	}

	public void controlService() {
		settings = PreferenceManager.getDefaultSharedPreferences(getApplication());
		boolean isEnabled = getState();
		boolean isServiceRunning = HelperFunctions.isServiceRunning(getApplication(),
				"com.vag.mychime.service.TimeService", false);
		Log.d(TAG, "isEnabled " + isEnabled);

		if (!isServiceRunning) {
			Log.d(TAG, "Service is not running");
			if (isEnabled) { // service should be running
				Log.d(TAG, "Starting service");

				startService(serviceIntent);
				Toast toast = Toast.makeText(getApplication(), getResources().getString(R.string.serviceStarted),
						Toast.LENGTH_LONG);
				toast.show();
			}
		} else {
			if (!isEnabled) { // service not should be running
				stopService(serviceIntent);
				Toast toast = Toast.makeText(this, getResources().getString(R.string.serviceStoped), Toast.LENGTH_LONG);
				toast.show();
			}
		}
	}

	public boolean getState() {

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
				isTTSAvailable = false;
				// missing data, install it
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(R.string.titleMsg);
				builder.setMessage(R.string.installMsg);
				builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						Intent installIntent = new Intent();
						installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
						startActivity(installIntent);
					}
				});

				builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
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

		if (uncomittedChanges) {
			Toast toast = Toast.makeText(this, getResources().getString(R.string.unsavedSettingsMsg),
					Toast.LENGTH_LONG);
			toast.show();
		}
	}

	@Override
	public void onConfigurationChanged() {
		Log.d(TAG, "onConfigurationChanged");
		setSaveBtnState(true);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.d(TAG, "onCreateOptionsMenu");
		setSaveBtnState(false);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.save:
			Log.i(TAG, "Clicked on save");
			controlService();
			setSaveBtnState(false);
			Toast toast = Toast.makeText(this, getResources().getString(R.string.changesSaved), Toast.LENGTH_LONG);
			toast.show();
			return true;

		default:
			// If we got here, the user's action was not recognized.
			// Invoke the superclass to handle it.
			return super.onOptionsItemSelected(item);
		}
	}

	public void setSaveBtnState(boolean enabled) {
		uncomittedChanges = enabled;
		Menu menu = myToolbar.getMenu();
		menu.removeItem(R.id.save);
		MenuItem saveItem = menu.add(Menu.NONE, R.id.save, Menu.NONE, R.string.savedSettingsTitle);

		saveItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		saveItem.setTitleCondensed(getString(R.string.savedSettingsTitle));

		Drawable icon = getDrawable(R.drawable.ic_save_black_48dp);
		if (enabled) {
			icon.setAlpha(255);
			saveItem.setEnabled(true);
		} else {
			icon.setAlpha(50);
			saveItem.setEnabled(false);
		}
		saveItem.setIcon(icon);
	}

	public boolean foundOldInstall() {
		// TODO: remove this eventually
		if (settings.contains("speakMuteOn") || settings.contains("chimeMuteOn")) {
			Log.d(TAG, "Found old install, clearing");
			SharedPreferences.Editor editor = settings.edit();
			editor.clear();
			editor.commit();

			return true;
		}

		return false;
	}

}
