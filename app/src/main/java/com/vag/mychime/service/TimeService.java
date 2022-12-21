package com.vag.mychime.service;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.vag.mychime.R;
import com.vag.mychime.activity.MainActivity;
import com.vag.mychime.preferences.TimePickerPreference;

import java.util.Calendar;
import java.util.Locale;

public class TimeService extends Service {
    private final IBinder mBinder = (IBinder) new MyBinder();

    private final int sdkVersion = Build.VERSION.SDK_INT;
    private final String TAG = "TimeService";

    boolean isOn = false;
    int bindCount = 0;

    enum ChimeType {
        SPEAK, BEEP
    }

    enum ActivationType {
        OFF, ALWAYS, HEADSET, TIMERANGE
    }

    SharedPreferences settings;
    CountDownTimer minutesTimer;
    TextToSpeech tts;
    boolean chime, speak, vibration, hasSpoken, scheduledSpeak, scheduledChime, scheduledVibration;
    boolean headsetPlugged;
    String clockType;
    String iniTimeSpeak, endTimeSpeak, iniTimeChime, endTimeChime;
    Calendar scheduleIni, scheduleEnd;
    Calendar now;
    String text, am_pm;

    public TimeService() {
    }

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

    public void startNotification() {
        Log.d(TAG, "Starting notification");

        Intent i = new Intent(this, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pi = PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_IMMUTABLE);

        String NOTIFICATION_CHANNEL_ID = "com.vag.mychime";
        String channelName = "MyChime Time Service";

        NotificationChannelCompat.Builder chan = new NotificationChannelCompat.Builder(NOTIFICATION_CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_NONE);
        chan.setName(channelName);
        NotificationManagerCompat.from(this).createNotificationChannel(chan.build());

        NotificationCompat.Builder notif = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_notify_service).setContentTitle("MyChime")
                .setContentText("Service started");
        notif.setContentIntent(pi);

        NotificationManagerCompat.from(this).notify(42066, notif.build());
    }

    public void checkTime() {

        now = Calendar.getInstance();

        int currentMinute = now.get(Calendar.MINUTE);
        int currentHour = now.get(Calendar.HOUR);
        am_pm = now.get(Calendar.AM_PM) == 0 ? "A " : "P ";

        Log.i(TAG, currentHour + ":" + currentMinute + " " + am_pm + ": Checking time");

        if (currentMinute == 0) {
            // if (currentMinute % 2 == 0) { // debugging
            if (!hasSpoken) { // time to chime
                MediaPlayer mediaPlayer = null;
                hasSpoken = true; // meant to avoid doublespeaking

                Log.d(TAG, "Time to chime!");

                // TODO: detect changes instead of reading every time
                getSettings();

                if (chime) {
                    mediaPlayer = MediaPlayer.create(getBaseContext(), R.raw.casiochime);
                    mediaPlayer.start();

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                if (speak) {
                    text = getResources().getString(R.string.speakTimeText_ini);

                    if (clockType.equals("24-hours"))
                        currentHour = now.get(Calendar.HOUR_OF_DAY);

                    text += (currentHour == 0 ? 12 : currentHour);

                    startTTS();
                }

                if (vibration) {
                    vibration();
                }

                if (mediaPlayer != null) { // cleanup
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
        Log.d(TAG, "getSettings");
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

        Log.i(TAG, "chime=" + enabledChime + " mode=" + chimeOnValue);

        if (enabledChime && !chimeOnValue.equals("unset")) {

            chime = true;

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
    public boolean isScheduledTime(Calendar ini, Calendar end) {

        return (ini.getTime()).compareTo(now.getTime()) <= 0 && (end.getTime()).compareTo(now.getTime()) >= 0;

    }

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

    private final TextToSpeech.OnInitListener ttsListener = new TextToSpeech.OnInitListener() {
        @Override
        public void onInit(int status) {
            Log.d(TAG, "TTS engine started");

            isOn = (status == TextToSpeech.SUCCESS);
            Locale current = getResources().getConfiguration().locale;
            Log.i(TAG, "Current locale " + current.getDisplayName());

            tts.setLanguage(current);

            if (sdkVersion < Build.VERSION_CODES.LOLLIPOP) {
                if (tts.speak(text, TextToSpeech.QUEUE_ADD, null) != TextToSpeech.SUCCESS) {
                    Log.e(TAG, "TTS queueing failed. Trying again");
                    // startTTS();
                    tts.speak(text, TextToSpeech.QUEUE_ADD, null);
                }

                if (clockType.equals("12-hours")) {
                    tts.speak(am_pm, TextToSpeech.QUEUE_ADD, null);
                    tts.speak("M", TextToSpeech.QUEUE_ADD, null);
                }
            } else {
                // I rather repeat code than use giant one-liners
                if (tts.speak(text, TextToSpeech.QUEUE_ADD, null, "") != TextToSpeech.SUCCESS) {
                    Log.e(TAG, "TTS queueing failed. Trying again");
                    // startTTS();
                    tts.speak(text, TextToSpeech.QUEUE_ADD, null, "");
                }

                if (clockType.equals("12-hours")) {
                    tts.speak(am_pm, TextToSpeech.QUEUE_ADD, null, "");
                    tts.speak("M", TextToSpeech.QUEUE_ADD, null, "");
                }
            }
        }
    };

    public class MyBinder extends Binder {
        public TimeService getService() {
            return TimeService.this;
        }
    }

    public boolean checkHeadsetPlugged() {
        AudioManager audio = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        return audio.isWiredHeadsetOn();
    }

    public void vibration() {
        long[] pattern = {0, 500, 100, 500, 100, 500, 100};
        Log.d(TAG, "vibrating");
        Vibrator vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(pattern, -1);
    }
}