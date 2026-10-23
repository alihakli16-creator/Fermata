package me.aap.fermata;

import static me.aap.fermata.ui.activity.MainActivityDelegate.INTENT_ACTION_AUTOSTART;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import me.aap.fermata.ui.activity.MainActivity;
import me.aap.fermata.ui.activity.MainActivityPrefs;
import me.aap.utils.log.Log;

/**
 * Starts Fermata and resumes the last played video after the device boots.
 * The first launch waits about 25 seconds, so that the other apps that start with the car are up
 * first and do not take the screen or the audio from Fermata. The launch is repeated once about
 * 20 seconds later, because on some head units the system launcher comes to the front after the
 * boot broadcast and hides the app. Change the numbers below to wait longer or shorter.
 */
public class BootReceiver extends BroadcastReceiver {
	private static final String ACTION_RETRY = "me.aap.fermata.action.AUTOSTART_RETRY";
	// Milliseconds: wait after boot before the first launch, then before the second launch
	private static final long[] RETRY_DELAYS = {25000, 20000};

	@Override
	public void onReceive(Context ctx, Intent intent) {
		if (intent == null) return;
		String action = intent.getAction();
		if (action == null) return;

		if (Intent.ACTION_BOOT_COMPLETED.equals(action)
				|| "android.intent.action.QUICKBOOT_POWERON".equals(action)
				|| "com.htc.intent.action.QUICKBOOT_POWERON".equals(action)) {
			if (!isEnabled()) return;
			scheduleRetry(ctx, 0); // the first launch happens after the initial delay
		} else if (ACTION_RETRY.equals(action)) {
			int n = intent.getIntExtra("n", 0);
			if (!isEnabled()) return;
			launch(ctx);
			scheduleRetry(ctx, n + 1);
		}
	}

	private static boolean isEnabled() {
		try {
			return MainActivityPrefs.get().getBooleanPref(MainActivityPrefs.AUTOSTART);
		} catch (Exception ex) {
			Log.e(ex, "Failed to read the autostart preference");
			return true;
		}
	}

	private static void launch(Context ctx) {
		try {
			Intent i = new Intent(ctx, MainActivity.class);
			i.setAction(INTENT_ACTION_AUTOSTART);
			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			ctx.startActivity(i);
		} catch (Exception ex) {
			Log.e(ex, "Failed to autostart Fermata");
		}
	}

	private static void scheduleRetry(Context ctx, int n) {
		if (n >= RETRY_DELAYS.length) return;

		try {
			AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
			if (am == null) return;
			Intent i = new Intent(ctx, BootReceiver.class);
			i.setAction(ACTION_RETRY);
			i.putExtra("n", n);
			PendingIntent pi = PendingIntent.getBroadcast(ctx, 1000 + n, i,
					PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
			am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,
					SystemClock.elapsedRealtime() + RETRY_DELAYS[n], pi);
		} catch (Exception ex) {
			Log.e(ex, "Failed to schedule an autostart retry");
		}
	}
}
