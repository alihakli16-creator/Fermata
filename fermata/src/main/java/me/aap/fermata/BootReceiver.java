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
 * Listens for system boot, quickboot power on, and immediate USB and MicroSD media mount.
 * When a USB drive or MicroSD card mounts, the fallback delay is cancelled and Fermata launches immediately.
 */
public class BootReceiver extends BroadcastReceiver {
	private static final String ACTION_LAUNCH = "me.aap.fermata.action.AUTOSTART_LAUNCH";
	private static final long LAUNCH_DELAY = 40000; // milliseconds fallback delay after boot

	@Override
	public void onReceive(Context ctx, Intent intent) {
		if (intent == null) return;
		String action = intent.getAction();
		if (action == null) return;

		if (Intent.ACTION_BOOT_COMPLETED.equals(action)
				|| "android.intent.action.QUICKBOOT_POWERON".equals(action)
				|| "com.htc.intent.action.QUICKBOOT_POWERON".equals(action)) {
			if (isEnabled()) scheduleLaunch(ctx);
		} else if (Intent.ACTION_MEDIA_MOUNTED.equals(action)) {
			// USB drive or MicroSD card is mounted - launch immediately without waiting 40 seconds
			if (isEnabled()) {
				cancelScheduledLaunch(ctx);
				launch(ctx);
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

	private static void cancelScheduledLaunch(Context ctx) {
		try {
			AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
			if (am == null) return;
			Intent i = new Intent(ctx, BootReceiver.class);
			i.setAction(ACTION_LAUNCH);
			PendingIntent pi = PendingIntent.getBroadcast(ctx, 1000, i,
					PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_NO_CREATE);
			if (pi != null) {
				am.cancel(pi);
				pi.cancel();
			}
		} catch (Exception ex) {
			Log.e(ex, "Failed to cancel scheduled launch");
		}
	}

	private static void scheduleLaunch(Context ctx) {
		try {
			AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
			if (am == null) return;
			Intent i = new Intent(ctx, BootReceiver.class);
			i.setAction(ACTION_LAUNCH);
			PendingIntent pi = PendingIntent.getBroadcast(ctx, 1000, i,
					PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
			am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,
					SystemClock.elapsedRealtime() + LAUNCH_DELAY, pi);
		} catch (Exception ex) {
			Log.e(ex, "Failed to schedule the autostart launch");
			launch(ctx); // better to start right away than not at all
		}
	}
}
