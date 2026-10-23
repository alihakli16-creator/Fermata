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
 * Supports configurable launch delay (0 to 150 seconds).
 * Uses PendingIntent.getActivity with AlarmManager to bypass background activity
 * start restrictions on modern Android (including Android 14 on Samsung Galaxy S24 Ultra).
 */
public class BootReceiver extends BroadcastReceiver {
	private static final String ACTION_LAUNCH = "me.aap.fermata.action.AUTOSTART_LAUNCH";
	private static final long DEFAULT_DELAY_MS = 30000; // default 30 seconds
	private static boolean launched;

	@Override
	public void onReceive(Context ctx, Intent intent) {
		if (intent == null) return;
		String action = intent.getAction();
		if (action == null) return;

		if (Intent.ACTION_BOOT_COMPLETED.equals(action)
				|| "android.intent.action.QUICKBOOT_POWERON".equals(action)
				|| "com.htc.intent.action.QUICKBOOT_POWERON".equals(action)) {
			launched = false;
			if (isEnabled()) scheduleLaunch(ctx);
		} else if (Intent.ACTION_USER_PRESENT.equals(action)) {
			// On phones like Samsung S24 Ultra, user unlocks screen after boot.
			if (isEnabled() && !launched) {
				long delay = getLaunchDelay();
				if (delay <= 0 || SystemClock.elapsedRealtime() >= delay) {
					launch(ctx);
				}
			}
		} else if (Intent.ACTION_MEDIA_MOUNTED.equals(action)) {
			if (isEnabled()) {
				long delay = getLaunchDelay();
				// If device has been running for longer than the delay, it's a live plug-in
				if (SystemClock.elapsedRealtime() > (delay + 10000L)) {
					launch(ctx);
				}
			}
		} else if (ACTION_LAUNCH.equals(action)) {
			if (isEnabled()) launch(ctx);
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

	private static long getLaunchDelay() {
		try {
			int sec = MainActivityPrefs.get().getIntPref(MainActivityPrefs.AUTOSTART_DELAY);
			return Math.max(0, sec) * 1000L;
		} catch (Exception ex) {
			Log.e(ex, "Failed to read the autostart delay preference");
			return DEFAULT_DELAY_MS;
		}
	}

	private static void launch(Context ctx) {
		launched = true;
		try {
			Intent i = new Intent(ctx, MainActivity.class);
			i.setAction(INTENT_ACTION_AUTOSTART);
			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
			ctx.startActivity(i);
		} catch (Exception ex) {
			Log.e(ex, "Failed to autostart Fermata");
		}
	}

	private static void scheduleLaunch(Context ctx) {
		long delay = getLaunchDelay();
		// On modern Android (especially Android 14 on Samsung One UI 6.1), calling startActivity
		// synchronously inside BOOT_COMPLETED is blocked by OS background restrictions.
		// Scheduling a PendingIntent.getActivity via AlarmManager (minimum 1.5s even for 0s delay)
		// allows the system server to launch MainActivity cleanly.
		long triggerDelay = Math.max(1500L, delay);

		try {
			AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
			if (am == null) {
				launch(ctx);
				return;
			}

			// Direct Activity PendingIntent: System executes it directly, bypassing background limits
			Intent actIntent = new Intent(ctx, MainActivity.class);
			actIntent.setAction(INTENT_ACTION_AUTOSTART);
			actIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
			PendingIntent pi = PendingIntent.getActivity(ctx, 1000, actIntent,
					PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

			am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,
					SystemClock.elapsedRealtime() + triggerDelay, pi);
		} catch (Exception ex) {
			Log.e(ex, "Failed to schedule AlarmManager launch");
			launch(ctx);
		}
	}
}
