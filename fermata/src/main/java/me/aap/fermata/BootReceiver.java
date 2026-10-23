package me.aap.fermata;

import static me.aap.fermata.ui.activity.MainActivityDelegate.INTENT_ACTION_AUTOSTART;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import me.aap.fermata.ui.activity.MainActivity;
import me.aap.fermata.ui.activity.MainActivityPrefs;
import me.aap.utils.log.Log;

/**
 * Starts Fermata and resumes the last played video after the device boots.
 * Supports configurable launch delay (0 to 150 seconds).
 * Combines exact AlarmManager broadcast + main-thread Handler timer so that both
 * Samsung Android 14 devices and BYD car head units launch reliably at the exact delay.
 */
public class BootReceiver extends BroadcastReceiver {
	private static final String ACTION_LAUNCH = "me.aap.fermata.action.AUTOSTART_LAUNCH";
	private static final long DEFAULT_DELAY_MS = 30000; // default 30 seconds
	private static boolean launched;
	private static boolean scheduled;
	private static final Handler handler = new Handler(Looper.getMainLooper());

	@Override
	public void onReceive(Context ctx, Intent intent) {
		if (intent == null) return;
		String action = intent.getAction();
		if (action == null) return;

		if (Intent.ACTION_BOOT_COMPLETED.equals(action)
				|| "android.intent.action.QUICKBOOT_POWERON".equals(action)
				|| "com.htc.intent.action.QUICKBOOT_POWERON".equals(action)) {
			launched = false;
			scheduled = false;
			if (isEnabled()) scheduleLaunch(ctx);
		} else if (Intent.ACTION_USER_PRESENT.equals(action)) {
			if (isEnabled() && !launched) {
				long delay = getLaunchDelay();
				if (delay <= 0) {
					launch(ctx);
				} else if (!scheduled) {
					scheduleLaunch(ctx);
				}
			}
		} else if (Intent.ACTION_MEDIA_MOUNTED.equals(action)) {
			if (isEnabled()) {
				long delay = getLaunchDelay();
				if (delay <= 0) {
					cancelScheduledLaunch(ctx);
					launch(ctx);
				} else if (SystemClock.elapsedRealtime() > (delay + 15000L)) {
					// Live USB/MicroSD plug-in while already driving
					launch(ctx);
				} else if (!scheduled && !launched) {
					scheduleLaunch(ctx);
				}
			}
		} else if (ACTION_LAUNCH.equals(action)) {
			if (isEnabled() && !launched) launch(ctx);
		}
	}

	private static boolean isEnabled() {
		try {
			return MainActivityPrefs.get().getBooleanPref(MainActivityPrefs.AUTOSTART);
		} catch (Throwable ex) {
			Log.e(ex, "Failed to read the autostart preference");
			return true;
		}
	}

	private static long getLaunchDelay() {
		try {
			int sec = MainActivityPrefs.get().getIntPref(MainActivityPrefs.AUTOSTART_DELAY);
			return Math.max(0, sec) * 1000L;
		} catch (Throwable ex) {
			Log.e(ex, "Failed to read the autostart delay preference");
			return DEFAULT_DELAY_MS;
		}
	}

	private static void launch(Context ctx) {
		if (launched) return;
		launched = true;
		scheduled = false;
		handler.removeCallbacksAndMessages(null);
		try {
			Intent i = new Intent(ctx, MainActivity.class);
			i.setAction(INTENT_ACTION_AUTOSTART);
			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			ctx.startActivity(i);
		} catch (Throwable ex) {
			Log.e(ex, "Failed to autostart Fermata");
		}
	}

	private static void cancelScheduledLaunch(Context ctx) {
		scheduled = false;
		handler.removeCallbacksAndMessages(null);
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
		} catch (Throwable ex) {
			Log.e(ex, "Failed to cancel scheduled launch");
		}
	}

	private static void scheduleLaunch(Context ctx) {
		if (launched) return;
		long delay = getLaunchDelay();
		if (delay <= 0) {
			launch(ctx);
			return;
		}

		scheduled = true;
		final Context appCtx = ctx.getApplicationContext();

		// 1. In-process Handler timer (fires at exact millisecond if process stays alive)
		handler.removeCallbacksAndMessages(null);
		handler.postDelayed(() -> {
			if (!launched && isEnabled()) {
				launch(appCtx);
			}
		}, delay);

		// 2. System AlarmManager broadcast backup (wakes receiver if process is killed)
		try {
			AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
			if (am == null) return;
			Intent i = new Intent(ctx, BootReceiver.class);
			i.setAction(ACTION_LAUNCH);
			PendingIntent pi = PendingIntent.getBroadcast(ctx, 1000, i,
					PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
			long triggerAt = SystemClock.elapsedRealtime() + delay;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				try {
					am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi);
				} catch (Throwable se) {
					// Fallback if exact alarm permission is not granted
					am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi);
				}
			} else {
				am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi);
			}
		} catch (Throwable ex) {
			Log.e(ex, "Failed to schedule the autostart launch");
			launch(ctx);
		}
	}
}
