package com.vag.mychime.service;

import java.util.Calendar;
import java.util.Locale;

import com.vag.mychime.activity.R;
import com.vag.mychime.preferences.TimePickerPreference;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.util.Log;

public class TimeService extends Service {

	private final IBinder mBinder = (IBinder) new MyBinder();

	private final int sdkVersion = Build.VERSION.SDK_INT;
	String TAG = "TimeService";
	boolean isOn = false;
	int bindCount = 0;

	enum ChimeType {
		SPEAK, BEEP
	};

	SharedPreferences settings;
	CountDownTimer minutesTimer;
	TextToSpeech tts;
	boolean chime, speakTime, hasSpoken, scheduledSpeak, scheduledChime;
	String clockType;
	String iniTimeSpeak, endTimeSpeak, iniTimeChime, endTimeChime;
	Calendar scheduleIniSpeak, scheduleEndSpeak, scheduleIniChime,
			scheduleEndChime;
	Calendar now;
	String text, am_pm;

	@Override
	public IBinder onBind(Intent intent) {
		bindCount++;
		Log.i(TAG, "Got bound (" + bindCount + ")");

		return mBinder;
	}

	public void onCreate() {
		Log.d(TAG, "onCreate");
		startService(new Intent(this, TimeService.class));
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "Service started. Received start id " + startId + ": "
				+ intent);

		hasSpoken = false;

		checkTime(); // primeira checagem, proxima daqui 30 segundos

		minutesTimer = new CountDownTimer(30000, 30000) {

			@Override
			public void onTick(long millisUntilFinished) {
			}

			@Override
			public void onFinish() {
				checkTime();
				start();
			}
		};

		minutesTimer.start();

		return Service.START_STICKY;
	}

	public void checkTime() {

		now = Calendar.getInstance();

		int currentMinute = now.get(Calendar.MINUTE);
		int currentHour = now.get(Calendar.HOUR);
		am_pm = now.get(Calendar.AM_PM) == 0 ? "A " : "P ";

		Log.i(TAG, currentHour + ":" + currentMinute + " " + am_pm
				+ ": Checking time");

		if (currentMinute == 0) {
			// if (currentMinute % 2 == 0) { // debugging
			if (!hasSpoken) { // time to chime
				MediaPlayer mediaPlayer = null;
				hasSpoken = true; // meant to avoid doublespeaking
				text = "The time is ";

				Log.d(TAG, "Time to chime!");

				getSettings();

				if (chime && checkSchedule(ChimeType.BEEP)) {
					mediaPlayer = MediaPlayer.create(getBaseContext(),
							R.raw.casiochime);
					mediaPlayer.start();

					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

				if (speakTime && checkSchedule(ChimeType.SPEAK)) {
					if (clockType.equals("24-hours"))
						currentHour = now.get(Calendar.HOUR_OF_DAY);

					text += (currentHour == 0 ? 12 : currentHour);

					startTTS();
				}

				if (chime && mediaPlayer != null) {
					mediaPlayer.reset();
					mediaPlayer.release();
					mediaPlayer = null;
				}
			}
		} else {
			hasSpoken = false;
		}
	}

	public void getSettings() {
		settings = PreferenceManager
				.getDefaultSharedPreferences(getApplication());

		// TODO: needs to make sure there's something set
		speakTime = settings.getBoolean("speakOn", false);
		if (speakTime) {
			clockType = settings.getString("clockType", "12-hours");
		}
		chime = settings.getBoolean("chimeOn", false);

		Log.i(TAG, "Settings: " + speakTime + " " + chime);

		scheduledSpeak = settings.getBoolean("speakMuteOn", false);
		if (scheduledSpeak) {
			iniTimeSpeak = settings.getString("speakStartTime", "00:00");
			endTimeSpeak = settings.getString("speakEndTime", "00:00");
			Log.i(TAG, iniTimeSpeak + "  " + endTimeSpeak);
			scheduleIniSpeak = Calendar.getInstance();
			scheduleIniSpeak.set(Calendar.HOUR,
					TimePickerPreference.getHour(iniTimeSpeak));
			scheduleIniSpeak.set(Calendar.MINUTE,
					TimePickerPreference.getMinute(iniTimeSpeak));

			scheduleEndSpeak = Calendar.getInstance();
			scheduleEndSpeak.set(Calendar.HOUR,
					TimePickerPreference.getHour(endTimeSpeak));
			scheduleEndSpeak.set(Calendar.MINUTE,
					TimePickerPreference.getMinute(endTimeSpeak));
		}

		scheduledChime = settings.getBoolean("chimeMuteOn", false);
		if (scheduledChime) {
			iniTimeChime = settings.getString("chimeStartTime", "00:00");
			endTimeChime = settings.getString("chimeEndTime", "00:00");
			Log.i(TAG, iniTimeChime + "  " + endTimeChime);

			scheduleIniChime = Calendar.getInstance();
			scheduleIniChime.set(Calendar.HOUR,
					TimePickerPreference.getHour(iniTimeChime));
			scheduleIniChime.set(Calendar.MINUTE,
					TimePickerPreference.getMinute(iniTimeChime));

			scheduleEndChime = Calendar.getInstance();
			scheduleEndChime.set(Calendar.HOUR,
					TimePickerPreference.getHour(endTimeChime));
			scheduleEndChime.set(Calendar.MINUTE,
					TimePickerPreference.getMinute(endTimeChime));
		}
	}

	/**
	 * Checks if specified ChimeType is scheduled and if it's on schedule
	 */
	public boolean checkSchedule(ChimeType type) {
		boolean isTime = false;

		switch (type) {
		case SPEAK:
			if (!scheduledSpeak
					|| ((scheduleIniSpeak.getTime()).compareTo(now.getTime()) <= 0 && (scheduleEndSpeak
							.getTime()).compareTo(now.getTime()) >= 0)) {
				isTime = true;
			}
			break;
		case BEEP:
			if (!scheduledChime
					|| ((scheduleIniChime.getTime()).compareTo(now.getTime()) <= 0 && (scheduleEndChime
							.getTime()).compareTo(now.getTime()) >= 0)) {
				isTime = true;
			}

			break;
		}

		return isTime;
	}

	@SuppressWarnings("deprecation")
	public void startTTS() {
		if (sdkVersion < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			tts = new TextToSpeech(this, ttsListener);
			tts.setEngineByPackageName("com.svox.pico");
		} else {
			tts = new TextToSpeech(this, ttsListener, "com.svox.pico");
		}
	}

	public void stopTTS() {
		if (tts != null) {
			tts.shutdown();
			tts = null;
		}
	}

	public void onDestroy() {
		super.onDestroy();

		settings = PreferenceManager
				.getDefaultSharedPreferences(getApplication());
		boolean shouldRestart = (settings.getBoolean("speakOn", false) || settings
				.getBoolean("chimeOn", false));

		if (shouldRestart) // service wasnt stopped by the app, restart
			startService(new Intent(this, TimeService.class));
		else {
			Log.d(TAG, "Service destroyed");
			minutesTimer.cancel();

			stopTTS();
		}
	}

	private TextToSpeech.OnInitListener ttsListener = new TextToSpeech.OnInitListener() {
		@Override
		public void onInit(int status) {
			Log.d(TAG, "TTS engine started");
			// tts.speak("", TextToSpeech.QUEUE_FLUSH, null);
			isOn = (status == TextToSpeech.SUCCESS);
			Locale current = getResources().getConfiguration().locale;
			Log.i(TAG, "Current locale " + current.getDisplayName());

			tts.setLanguage(current);

			if (tts.speak(text, TextToSpeech.QUEUE_ADD, null) != TextToSpeech.SUCCESS) {
				Log.e(TAG, "TTS queueing failed. Trying again");
				// startTTS();
				tts.speak(text, TextToSpeech.QUEUE_ADD, null);
			}

			if (clockType.equals("12-hours")) {
				tts.speak(am_pm, TextToSpeech.QUEUE_ADD, null);
				tts.speak("M", TextToSpeech.QUEUE_ADD, null);
			}

			// stopTTS();
		}
	};

	public class MyBinder extends Binder {
		public TimeService getService() {
			return TimeService.this;
		}
	}
}
