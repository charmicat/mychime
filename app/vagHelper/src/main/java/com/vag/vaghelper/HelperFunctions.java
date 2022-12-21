package com.vag.vaghelper;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import java.util.List;

@SuppressLint("InlinedApi")
public class HelperFunctions {

    private static final String TAG = "HelperFunctions";

    // Determine if servicename is currently running
    public static boolean isServiceRunning(Context context, String servicename, boolean debug) {
        if (debug) {
            Log.d(TAG, "isServiceRunning");
        }
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (debug)
                Log.d(TAG, service.service.getClassName());
            if (servicename.equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Indicates whether the specified action can be used as an intent. This
     * method queries the package manager for installed packages that can
     * respond to an intent with the specified action. If no suitable package is
     * found, this method returns false.
     *
     * @param context The application's environment.
     * @param action  The Intent action to check for availability.
     * @return True if an Intent with the specified action can be sent and
     * responded to, false otherwise.
     */
    public static boolean isIntentAvailable(Context context, String action) {
        Log.d(TAG, "isIntentAvailable");
        final PackageManager packageManager = context.getPackageManager();
        final Intent intent = new Intent(action);
        List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

    @SuppressWarnings("deprecation")
    private static Intent rateIntentForUrl(Context c, String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(String.format("%s?id=%s", url, c.getPackageName())));
        int flags = Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_MULTIPLE_TASK;
        if (Build.VERSION.SDK_INT >= 21) {
            flags |= Intent.FLAG_ACTIVITY_NEW_DOCUMENT;
        } else {
            // noinspection deprecation
            flags |= Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET;
        }
        intent.addFlags(flags);
        return intent;
    }

    public static Intent rateApp(Context c, boolean debug) {
        Intent rateIntent;
        if (debug) {
            Log.d(TAG, "rateApp");
        }

        // google play
        try {
            rateIntent = rateIntentForUrl(c, "market://details");
            c.startActivity(rateIntent);
        } catch (ActivityNotFoundException e) {
            rateIntent = rateIntentForUrl(c, "http://play.google.com/store/apps/details");
            c.startActivity(rateIntent);
        }
        // amazon appstore
        // as of now a different approach is not needed

        return rateIntent;
    }

    public static void playAudio(Context c, int resourceID, boolean debug) {
        if (debug) {
            Log.d(TAG, "playAudio");
        }
        AudioPlayer ap = new AudioPlayer();
        ap.play(c, resourceID);
    }
}
