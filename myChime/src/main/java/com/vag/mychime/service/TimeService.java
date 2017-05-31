package com.vag.mychime.service;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Binder;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.vag.mychime.activity.MainActivity;
import com.vag.mychime.activity.R;
import com.vag.mychime.preferences.TimePickerPreference;
import com.vag.vaghelper.HelperFunctions;

import java.util.Calendar;
import java.util.Locale;

public class TimeService extends Service {

    private final IBinder mBinder = (IBinder) new MyBinder();

    private final int sdkVersion = Build.VERSION.SDK_INT;
    private final String TAG = "TimeService";

    private boolean isOn = false;
    private int bindCount = 0;

    enum ChimeType {
        SPEAK, BEEP
    }

    enum ActivationType {
        OFF, ALWAYS, HEADSET, TIMERANGE
    }

    private SharedPreferences settings;
    private CountDownTimer minutesTimer;
    private TextToSpeech tts;
    private boolean chime, chimeHalf, speak, vibration, hasSpoken;
    private String clockType;

    private Calendar now;
    private String text, am_pm;

    @Override
    public IBinder onBind(Intent intent) {
        bindCount++;
        Log.i(TAG, "Got bound (" + bindCount + ")");

        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Service started. Received start id " + startId + ": " + intent + " flags: " + flags);

        startNotification();

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

    private void startNotification() {
        Log.d(TAG, "Starting notification");
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_notify_service).setContentTitle(getResources().getString(R.string.app_name))
                .setContentText(getResources().getString(R.string.serviceRunning));

        Intent i = new Intent(this, MainActivity.class);

        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pi = PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder.setContentIntent(pi);

        startForeground(42066, mBuilder.build());
    }

    private void checkTime() {

        now = Calendar.getInstance();

        int currentMinute = now.get(Calendar.MINUTE);
        int currentHour = now.get(Calendar.HOUR);
        am_pm = (now.get(Calendar.AM_PM) == Calendar.AM) ? "A " : "P ";

        // Log.i(TAG, currentHour + ":" + currentMinute + " " + am_pm
        // + ": Checking time");

        if ((currentMinute == 0 || currentMinute == 30) && !hasSpoken) {
//        if ((currentMinute % 2 == 0) && !hasSpoken) { // debugging
            hasSpoken = true; // meant to avoid doublespeaking

            Log.d(TAG, "Time to chime!");

            getSettings();

            if (currentMinute == 30 && chimeHalf) {
                HelperFunctions.playAudio(this, R.raw.casiochimehalf, true);
            } else {

                if (chime) {
                    HelperFunctions.playAudio(this, R.raw.casiochime, true);
                }

                if (speak) {
                    try {//wait 1s after chiming, avoid sounding @ same time
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if (clockType.equals("24-hours"))
                        currentHour = now.get(Calendar.HOUR_OF_DAY);

                    text = String.valueOf(currentHour == 0 ? 12 : currentHour);

                    startTTS();
                }

                if (vibration) {
                    vibration();
                }
            }
        } else {
            hasSpoken = false;
        }
    }

    private void getSettings() {
        Log.d(TAG, "getSettings");
        String iniTimeSpeak, endTimeSpeak, iniTimeChime, endTimeChime;
        Calendar scheduleIni, scheduleEnd;

        settings = PreferenceManager.getDefaultSharedPreferences(getApplication());

        boolean enabledSpeak = settings.getBoolean("enableSpeak", false);
        String speakOnValue = settings.getString("speakOn", "unset");

        Log.i(TAG, "speak=" + enabledSpeak + " mode=" + speakOnValue);

        if (enabledSpeak && !speakOnValue.equals("unset")) {
            clockType = settings.getString("clockType", "12-hours");
            speak = true;

            if (speakOnValue.equals("speakHeadsetOn") && !checkHeadsetPlugged()) {
                speak = false;
            } else if (speakOnValue.equals("speakTimeRange")) {
                iniTimeSpeak = settings.getString("speakStartTime", "00:00");
                endTimeSpeak = settings.getString("speakEndTime", "00:00");
                Log.i(TAG, iniTimeSpeak + "  " + endTimeSpeak);
                scheduleIni = Calendar.getInstance();
                scheduleIni.set(Calendar.HOUR, TimePickerPreference.getHour(iniTimeSpeak));
                scheduleIni.set(Calendar.MINUTE, TimePickerPreference.getMinute(iniTimeSpeak));

                scheduleEnd = Calendar.getInstance();
                scheduleEnd.set(Calendar.HOUR, TimePickerPreference.getHour(endTimeSpeak));
                scheduleEnd.set(Calendar.MINUTE, TimePickerPreference.getMinute(endTimeSpeak));

                speak = isScheduledTime(scheduleIni, scheduleEnd);
            }
        } else {
            speak = false;
        }

        boolean enabledChime = settings.getBoolean("enableChime", false);
        String chimeOnValue = settings.getString("chimeOn", "unset");
        boolean enabledChimeHalf = settings.getBoolean("chimeHalf", false);

        Log.i(TAG, "chime=" + enabledChime + " mode=" + chimeOnValue);

        if (enabledChime && !chimeOnValue.equals("unset")) {

            chime = true;

            if (enabledChimeHalf)
                chimeHalf = true;

            if (chimeOnValue.equals("chimeHeadsetOn") && !checkHeadsetPlugged()) {
                chime = false;
            } else if (chimeOnValue.equals("chimeTimeRange")) {

                iniTimeChime = settings.getString("chimeStartTime", "00:00");
                endTimeChime = settings.getString("chimeEndTime", "00:00");
                Log.i(TAG, iniTimeChime + "  " + endTimeChime);

                scheduleIni = Calendar.getInstance();
                scheduleIni.set(Calendar.HOUR, TimePickerPreference.getHour(iniTimeChime));
                scheduleIni.set(Calendar.MINUTE, TimePickerPreference.getMinute(iniTimeChime));

                scheduleEnd = Calendar.getInstance();
                scheduleEnd.set(Calendar.HOUR, TimePickerPreference.getHour(endTimeChime));
                scheduleEnd.set(Calendar.MINUTE, TimePickerPreference.getMinute(endTimeChime));

                chime = isScheduledTime(scheduleIni, scheduleEnd);
            }
        } else {
            chime = false;
        }

        boolean enabledVibration = settings.getBoolean("enableVibration", false);
        String vibrationOnValue = settings.getString("vibrationOn", "unset");

        Log.i(TAG, "vibration=" + enabledVibration + " mode=" + vibrationOnValue);

        if (enabledVibration && !vibrationOnValue.equals("unset")) {
            vibration = true;

            if (vibrationOnValue.equals("vibrationHeadsetOn") && !checkHeadsetPlugged()) {
                vibration = false;
            } else if (vibrationOnValue.equals("vibrationTimeRange")) {

                iniTimeChime = settings.getString("vibrationStartTime", "00:00");
                endTimeChime = settings.getString("vibrationEndTime", "00:00");
                Log.i(TAG, iniTimeChime + "  " + endTimeChime);

                scheduleIni = Calendar.getInstance();
                scheduleIni.set(Calendar.HOUR, TimePickerPreference.getHour(iniTimeChime));
                scheduleIni.set(Calendar.MINUTE, TimePickerPreference.getMinute(iniTimeChime));

                scheduleEnd = Calendar.getInstance();
                scheduleEnd.set(Calendar.HOUR, TimePickerPreference.getHour(endTimeChime));
                scheduleEnd.set(Calendar.MINUTE, TimePickerPreference.getMinute(endTimeChime));

                vibration = isScheduledTime(scheduleIni, scheduleEnd);
            }
        } else
            vibration = false;
    }

    /**
     * Checks if it's on scheduled time
     */
    private boolean isScheduledTime(Calendar ini, Calendar end) {

        return ((ini.getTime()).compareTo(now.getTime()) <= 0 && (end.getTime()).compareTo(now.getTime()) >= 0);

    }

    @SuppressWarnings("deprecation")
    private void startTTS() {
        if (sdkVersion < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            tts = new TextToSpeech(this, ttsListener);
            tts.setEngineByPackageName("com.svox.pico");
        } else {
            tts = new TextToSpeech(this, ttsListener, "com.svox.pico");
        }
    }

    private void stopTTS() {
        if (tts != null) {
            tts.shutdown();
            tts = null;
        }
    }

    public void onDestroy() {
        super.onDestroy();

        settings = PreferenceManager.getDefaultSharedPreferences(getApplication());
        boolean isSpeakTimeOn = settings.getBoolean("enableSpeak", false);
        boolean isChimeOn = settings.getBoolean("enableChime", false);

        boolean shouldRestart = isSpeakTimeOn || isChimeOn;

        if (shouldRestart) // service wasn't stopped by the app, restart
            sendBroadcast(new Intent("RestartTimeService"));
            // startService(new Intent(this, TimeService.class));
        else {
            Log.d(TAG, "Service destroyed");
            minutesTimer.cancel();

            stopTTS();
            stopForeground(true);
        }
    }

    private TextToSpeech.OnInitListener ttsListener = new TextToSpeech.OnInitListener() {
        @SuppressWarnings("deprecation")
        @Override
        public void onInit(int status) {
            Log.d(TAG, "TTS engine started");

            isOn = (status == TextToSpeech.SUCCESS);
            Locale current = getResources().getConfiguration().locale;
            Log.i(TAG, "Current locale " + current.getDisplayName());

            tts.setLanguage(current);

            String currTime = getResources().getString(R.string.currentTimeText);

            if (sdkVersion < Build.VERSION_CODES.LOLLIPOP) {
                if (tts.speak(currTime, TextToSpeech.QUEUE_ADD, null) != TextToSpeech.SUCCESS) {
                    Log.e(TAG, "TTS queueing failed. Trying again");
                    tts.speak(currTime, TextToSpeech.QUEUE_ADD, null);
                }

                if (tts.speak(text, TextToSpeech.QUEUE_ADD, null) != TextToSpeech.SUCCESS) {
                    Log.e(TAG, "TTS queueing failed. Trying again");
                    tts.speak(text, TextToSpeech.QUEUE_ADD, null);
                }

                if (clockType.equals("12-hours")) {
                    tts.speak(am_pm, TextToSpeech.QUEUE_ADD, null);
                    tts.speak("M", TextToSpeech.QUEUE_ADD, null);
                }
            } else {
                // I rather repeat code than use giant one-liners
                if (tts.speak(currTime, TextToSpeech.QUEUE_ADD, null) != TextToSpeech.SUCCESS) {
                    Log.e(TAG, "TTS queueing failed. Trying again");
                    tts.speak(currTime, TextToSpeech.QUEUE_ADD, null);
                }

                if (tts.speak(text, TextToSpeech.QUEUE_ADD, null, "mychime") != TextToSpeech.SUCCESS) {
                    Log.e(TAG, "TTS queueing failed. Trying again");
                    tts.speak(text, TextToSpeech.QUEUE_ADD, null, "mychime");
                }

                if (clockType.equals("12-hours")) {
                    tts.speak(am_pm, TextToSpeech.QUEUE_ADD, null, "mychime");
                    tts.speak("M", TextToSpeech.QUEUE_ADD, null, "mychime");
                }
            }
        }
    };

    public class MyBinder extends Binder {
        public TimeService getService() {
            return TimeService.this;
        }
    }

    @SuppressWarnings("deprecation")
    private boolean checkHeadsetPlugged() {
        AudioManager audio = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        return audio.isWiredHeadsetOn();

    }

    private void vibration() {
        long[] pattern = {0, 500, 100, 500, 100, 500, 100};
        Log.d(TAG, "vibrating");
        Vibrator vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(pattern, -1);
    }
}
