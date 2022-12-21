package com.vag.mychime.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.vag.mychime.service.TimeService;

public class BootUpReceiver extends BroadcastReceiver {
	// starting the app on boot-up

	private final String TAG = "BootUpReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {

		Log.d(TAG, "onReceive");

		if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())
				|| "RestartTimeService".equals(intent.getAction())) {
			Log.d(TAG, "BootUpReceiver BOOT_COMPLETED");

			Intent i = new Intent(context, TimeService.class);
			context.startService(i);
		}
	}
}