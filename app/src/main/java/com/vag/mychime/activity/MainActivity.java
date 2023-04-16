package com.vag.mychime.activity;

import static com.vag.mychime.R.*;
import static com.vag.mychime.R.id.*;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
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
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.vag.mychime.R;
import com.vag.mychime.preferences.MyPreferences;
import com.vag.mychime.service.TimeService;
import com.vag.mychime.utils.ChangeLog;
import io.github.charmicat.vaghelper.HelperFunctions;

public class MainActivity extends AppCompatActivity implements MyPreferences.OnConfigurationChangedListener {

    private final String TAG = "MainActivity";
    private final boolean DEBUG = true;
    private final int isTTSAvailableIntentCode = 666;
    private final String prefFragmentTag = "preference_fragment";

    private Intent serviceIntent;
    private SharedPreferences settings;
    private TimeService service;
    private CheckBox chimeCheck;
    private boolean isChimeOn, isSpeakTimeOn, isVibrationOn, isTTSAvailable, uncomittedChanges;
    private ToggleButton speakTime, chime;
    private Toolbar myToolbar;

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
//                        Intent data = result.getData();
                        Log.d(TAG, "onActivityResult " + result.getResultCode());

                        if (result.getResultCode() == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                            isTTSAvailable = true;
                        } else {
                            isTTSAvailable = false;
                            // missing data, install it
                            AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
                            builder.setTitle(string.titleMsg);
                            builder.setMessage(string.installMsg);
                            builder.setPositiveButton(android.R.string.ok, (dialog, id) -> {
                                Intent installIntent = new Intent();
                                installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                                startActivity(installIntent);
                            });

                            builder.setNegativeButton(android.R.string.cancel, (dialog, id) -> {
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
        setContentView(layout.settings_activity);
        settings = PreferenceManager.getDefaultSharedPreferences(getApplication());
        Vibrator vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("hasVibration", vibrator.hasVibrator());
        editor.putBoolean("enableVibration", false);
        editor.commit();
        Log.d(TAG, vibrator.hasVibrator() + " onCreate " + settings.getAll());

        if (savedInstanceState == null) {
            Log.d(TAG, "onCreate replacing MyPreferences fragment");
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(id.settings, new MyPreferences(), prefFragmentTag)
                    .commit();
        }

        if (HelperFunctions.isIntentAvailable(this, TextToSpeech.Engine.ACTION_CHECK_TTS_DATA)) {
            Intent intent = new Intent();
            intent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
            ttsActivityResultCB.launch(intent);
        }

        serviceIntent = new Intent(this, TimeService.class);
        controlService();

        myToolbar = findViewById(my_toolbar);
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
        Log.d(TAG, "controlService: isEnabled " + isEnabled);

        if (!isServiceRunning) {
            Log.d(TAG, "Service is not running");
            if (isEnabled) { // service should be running
                Log.d(TAG, "controlService: Starting service");

                startService(serviceIntent);
            }
        } else {
            if (!isEnabled) { // service should not be running
                stopService(serviceIntent);
            }
        }
    }

    private boolean getState() {
        if (!isNewInstall()) {
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
        Log.d(TAG, "onActivityResult: requestCode" + requestCode + " resultCode " + resultCode);

        if (requestCode == isTTSAvailableIntentCode) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                isTTSAvailable = true;
            } else {
                isTTSAvailable = false; //TODO: do something if TTS not available
                // missing data, install it
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(string.titleMsg);
                builder.setMessage(string.installMsg);
                builder.setPositiveButton(android.R.string.ok, (dialog, id) -> {
                    Intent installIntent = new Intent();
                    installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                    startActivity(installIntent);
                });

                builder.setNegativeButton(android.R.string.cancel, (dialog, id) -> {
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
            Toast toast = Toast.makeText(this, getResources().getString(string.serviceStoped), Toast.LENGTH_LONG);

            toast.show();
        } else {
            Log.d(TAG, "Service is running");
            Toast toast = Toast.makeText(getApplication(), getResources().getString(string.serviceStarted),
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
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
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
        if (item.getItemId() == rate) {
            Log.d(TAG, "Clicked on rate");
            rateApp();
            return true;
        } else if (item.getItemId() == about) {
            ChangeLog cl = new ChangeLog(this);
            cl.getFullLogDialog().show();
            return true;
        }
        // If we got here, the user's action was not recognized.
        // Invoke the superclass to handle it.
        return super.onOptionsItemSelected(item);
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
        int flags = Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_MULTIPLE_TASK | Intent.FLAG_ACTIVITY_NEW_DOCUMENT;
        intent.addFlags(flags);
        return intent;
    }

    public boolean isNewInstall() {
        if (settings.contains("installFlag")) {
            // show changelog
            Log.d(TAG, "isNewInstall: false, showing changelog");
            return false;
        } else {
            Log.d(TAG, "isNewInstall: true, showing welcome");
            //TODO create welcome popup
        }
        return true;
    }


}