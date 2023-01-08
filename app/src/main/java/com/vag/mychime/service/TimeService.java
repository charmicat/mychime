package com.vag.mychime.service;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioDeviceInfo;
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

import io.github.charmicat.vaghelper.HelperFunctions;

public class TimeService extends Service {
    private final IBinder mBinder = new MyBinder();

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

    //TODO: use a notification type which doesnt appear on lock screen, "softer"

    private SharedPreferences settings;
    private CountDownTimer minutesTimer;
    private TextToSpeech tts;
    private boolean chime, speak, vibration, hasSpoken, scheduledSpeak, scheduledChime, scheduledVibration;
    private String clockType;
    private String iniTimeSpeak, endTimeSpeak, iniTimeChime, endTimeChime;
    private Calendar scheduleIni, scheduleEnd;
    private Calendar now;
    private String currentTimeText, am_pm;

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

    private void startNotification() {
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

    private void checkTime() {

        now = Calendar.getInstance();

        int currentMinute = now.get(Calendar.MINUTE);
        int currentHour = now.get(Calendar.HOUR);
        am_pm = now.get(Calendar.AM_PM) == 0 ? "A " : "P ";

//        Log.i(TAG, currentHour + ":" + currentMinute + " " + am_pm + ": Checking time");

        if (currentMinute == 0 && !hasSpoken) {
            // if (currentMinute % 2 == 0) { // debugging
            if (!hasSpoken) { // time to chime
                MediaPlayer mediaPlayer = null;
                hasSpoken = true; // meant to avoid doublespeaking

                Log.d(TAG, "Time to chime!");

                // TODO: detect changes instead of reading every time
                getSettings();

                if (chime) {
                    HelperFunctions.playAudio(getBaseContext(), R.raw.casiochime);
                }

                if (speak) {
                    try {//wait 1s after chiming, avoid sounding @ same time
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    currentTimeText = getResources().getString(R.string.speakTimeText_ini);

                    if (clockType.equals("24-hours"))
                        currentHour = now.get(Calendar.HOUR_OF_DAY);

                    currentTimeText += (currentHour == 0 ? 12 : currentHour);

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

    public void getSettings() {
        Log.d(TAG, "getSettings");
        settings = PreferenceManager.getDefaultSharedPreferences(getApplication());

        boolean enabledSpeak = settings.getBoolean("enableSpeak", false);
        String speakOnValue = settings.getString("speakOn", "unset");

        Log.i(TAG, "speak=" + enabledSpeak + " mode=" + speakOnValue);

        if (enabledSpeak && !speakOnValue.equals("unset")) {
            clockType = settings.getString("clockType", "12-hours");
            speak = true;

            if (speakOnValue.equals("speakHeadsetOn") && !isHeadsetPlugged(getBaseContext())) {
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

            if (chimeOnValue.equals("chimeHeadsetOn") && !isHeadsetPlugged(getBaseContext())) {
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

            if (vibrationOnValue.equals("vibrationHeadsetOn") && !isHeadsetPlugged(getBaseContext())) {
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
        return (ini.getTime()).compareTo(now.getTime()) <= 0 && (end.getTime()).compareTo(now.getTime()) >= 0;
    }

    private void startTTS() {
        tts = new TextToSpeech(this, ttsListener, "com.svox.pico");
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

    private final TextToSpeech.OnInitListener ttsListener = new TextToSpeech.OnInitListener() {
        @Override
        public void onInit(int status) {
            Log.d(TAG, "TTS engine started");

            isOn = (status == TextToSpeech.SUCCESS);
            Locale current = getResources().getConfiguration().locale;
            Log.i(TAG, "Current locale " + current.getDisplayName());

            tts.setLanguage(current);

            // I rather repeat code than use giant one-liners
            if (tts.speak(currentTimeText, TextToSpeech.QUEUE_ADD, null, "") != TextToSpeech.SUCCESS) {
                Log.e(TAG, "TTS queueing failed. Trying again");
                tts.speak(currentTimeText, TextToSpeech.QUEUE_ADD, null, "");
            }

            if (clockType.equals("12-hours")) {
                tts.speak(am_pm, TextToSpeech.QUEUE_ADD, null, "mychime");
                tts.speak("M", TextToSpeech.QUEUE_ADD, null, "mychime");
            }
        }
    };

    public class MyBinder extends Binder {
        public TimeService getService() {
            return TimeService.this;
        }
    }

    private boolean isHeadsetPlugged(Context context) {
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        if (am == null)
            return false;

        AudioDeviceInfo[] devices = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS);

        for (AudioDeviceInfo device : devices) {
            if (device.getType() == AudioDeviceInfo.TYPE_WIRED_HEADSET
                    || device.getType() == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
                    || device.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
                    || device.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
                return true;
            }
        }
        return false;
    }

    private void vibration() {
        long[] pattern = {0, 500, 100, 500, 100, 500, 100};
        Log.d(TAG, "vibrating");
        Vibrator vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(pattern, -1);
    }
}