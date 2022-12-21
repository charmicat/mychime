package com.vag.mychime.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.vag.mychime.R;
import com.vag.mychime.preferences.MyPreferences;
import com.vag.mychime.service.TimeService;
import com.vag.vaghelper.HelperFunctions;

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
    private final ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            Log.d(TAG, "onServiceConnected");
            service = ((TimeService.MyBinder) binder).getService();
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG, "onServiceDisconnected");
            service = null;
        }
    };

    ActivityResultLauncher<Intent> ttsActivityResultCB = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // Here, no request code
                        Intent data = result.getData();
                        Log.d(TAG, "onActivityResult " + result.getResultCode());

                        if (result.getResultCode() == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                            isTTSAvailable = true;
                        } else {
                            isTTSAvailable = false;
                            // missing data, install it
                            AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
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
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate");
        setContentView(R.layout.settings_activity);
        settings = PreferenceManager.getDefaultSharedPreferences(getApplication());
        Vibrator vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        if (!settings.contains("hasVibration")) {
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean("hasVibration", vibrator.hasVibrator());
            editor.commit();
        }
        if (savedInstanceState == null) {
            Log.d(TAG, "onCreate replacing");
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new MyPreferences(), prefFragmentTag)
                    .commit();
        }

        if (HelperFunctions.isIntentAvailable(this, TextToSpeech.Engine.ACTION_CHECK_TTS_DATA)) {
            Intent intent = new Intent();
            intent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
            ttsActivityResultCB.launch(intent);
        }

        serviceIntent = new Intent(this, TimeService.class);
        controlService();

        myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    public void controlService() {
        if (!settings.contains("installFlag")) {
            SharedPreferences.Editor editor = settings.edit();
            editor.putInt("installFlag", 1);
            editor.commit();
        }
        boolean isEnabled = getState();
        boolean isServiceRunning = HelperFunctions.isServiceRunning(getApplication(),
                "com.vag.mychime.service.TimeService", false);
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
        super.onActivityResult(requestCode, resultCode, data);
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
                "com.vag.mychime.service.TimeService", false);

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
        Log.d(TAG, "onConfigurationChanged nopar");
        controlService();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
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
                rateApp();
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    public void rateApp() {
        try {
            Intent rateIntent = rateIntentForUrl("market://details");
            startActivity(rateIntent);
        } catch (ActivityNotFoundException e) {
            Intent rateIntent = rateIntentForUrl("http://play.google.com/store/apps/details");
            startActivity(rateIntent);
        }
    }

    private Intent rateIntentForUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(String.format("%s?id=%s", url, getPackageName())));
        int flags = Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_MULTIPLE_TASK;
        if (Build.VERSION.SDK_INT >= 21) {
            flags |= Intent.FLAG_ACTIVITY_NEW_DOCUMENT;
        } else {
            flags |= Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET;
        }
        intent.addFlags(flags);
        return intent;
    }

    public boolean foundOldInstall() {
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