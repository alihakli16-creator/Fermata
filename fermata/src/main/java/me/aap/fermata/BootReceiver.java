package me.aap.fermata;

import static me.aap.fermata.ui.activity.MainActivityDelegate.INTENT_ACTION_AUTOSTART;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import me.aap.fermata.ui.activity.MainActivity;
import me.aap.fermata.ui.activity.MainActivityPrefs;
import me.aap.utils.log.Log;

/**
 * Starts Fermata and resumes the last played video when the device boots.
 */
public class BootReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context ctx, Intent intent) {
		if (intent == null) return;
		String action = intent.getAction();
		if (action == null) return;

		if (Intent.ACTION_BOOT_COMPLETED.equals(action)
				|| "android.intent.action.QUICKBOOT_POWERON".equals(action)
				|| "com.htc.intent.action.QUICKBOOT_POWERON".equals(action)) {
			try {
				if (!MainActivityPrefs.get().getBooleanPref(MainActivityPrefs.AUTOSTART)) return;
				Intent i = new Intent(ctx, MainActivity.class);
				i.setAction(INTENT_ACTION_AUTOSTART);
				i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				ctx.startActivity(i);
			} catch (Exception ex) {
				Log.e(ex, "Failed to autostart Fermata");
			}
		}
	}
}
