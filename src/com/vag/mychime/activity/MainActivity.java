package com.vag.mychime.activity;

import java.util.List;

import com.vag.mychime.preferences.MyPreferences;
import com.vag.mychime.service.TimeService;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
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

	String TAG = "MainActivity";
	int isTTSAvailableIntentCode = 666;

	Intent serviceIntent;
	SharedPreferences settings;
	TimeService service;
	CheckBox chimeCheck;
	boolean isChimeOn, isSpeakTimeOn, isTTSAvailable;
	ToggleButton speakTime, chime;
	MyPreferences pref;

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

		if (isIntentAvailable(this, TextToSpeech.Engine.ACTION_CHECK_TTS_DATA)) {
			Intent checkIntent = new Intent();
			checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
			startActivityForResult(checkIntent, isTTSAvailableIntentCode);
		}

		serviceIntent = new Intent(this, TimeService.class);

		setup();
		pref = new MyPreferences();
		// Display the fragment as the main content
		getFragmentManager().beginTransaction().addToBackStack(null)
				.replace(android.R.id.content, pref).commit();
	}

	public void setup() {
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = settings.edit();
		boolean isEnabled = getState();
		Log.d(TAG, "isEnabled " + isEnabled);

		if (isEnabled) { // service should be running, but maybe was killed
			startService(serviceIntent);
			bindService(serviceIntent, mConnection, 0);
			Toast toast = Toast.makeText(this,
					getResources().getString(R.string.serviceStarted),
					Toast.LENGTH_LONG);
			toast.show();
		} else { // service should be stopped
			try {
				unbindService(mConnection);
			} catch (IllegalArgumentException e) {
			}

			stopService(serviceIntent);
			Toast toast = Toast.makeText(this,
					getResources().getString(R.string.serviceStoped),
					Toast.LENGTH_LONG);
			toast.show();
		}

		editor.commit();
	}

	public boolean getState() {
		String speakOnValue = settings.getString("speakOn", "unset");
		String chimeOnvalue = settings.getString("chimeOn", "unset");
		isSpeakTimeOn = !speakOnValue.equals("unset");

		isChimeOn = !chimeOnvalue.equals("unset");

		return (isSpeakTimeOn || isChimeOn);
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

		try {
			unbindService(mConnection);
		} catch (IllegalArgumentException e) {

		}
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
		setup();

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
							setup();
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

}
