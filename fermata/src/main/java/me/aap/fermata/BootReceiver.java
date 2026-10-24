package me.aap.fermata;

import static me.aap.fermata.ui.activity.MainActivityDelegate.INTENT_ACTION_AUTOSTART;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import me.aap.fermata.ui.activity.MainActivity;
import me.aap.fermata.ui.activity.MainActivityPrefs;
import me.aap.utils.log.Log;

/**
 * Starts Fermata and resumes the last played video/audio after the vehicle boots or wakes up.
 * Supports instant MicroSD launch (if mounted, launches in 2 seconds instead of waiting 40s).
 * Implements a high-priority FullScreenIntent notification fallback so Android 10-14 background activity
 * start restrictions never block the autostart on Samsung or BYD DiLink systems.
 */
public class BootReceiver extends BroadcastReceiver {
	private static final String ACTION_LAUNCH = "me.aap.fermata.action.AUTOSTART_LAUNCH";
	private static final long FAST_LAUNCH_DELAY = 2000;  // 2s if MicroSD/storage is already mounted
	private static final long FALLBACK_DELAY = 30000;   // 30s backup if slow USB is still mounting

	@Override
	public void onReceive(Context ctx, Intent intent) {
		if (intent == null) return;
		String action = intent.getAction();
		if (action == null) return;

		Log.d("BootReceiver: Received action ", action);

		if (Intent.ACTION_BOOT_COMPLETED.equals(action)
				|| "android.intent.action.QUICKBOOT_POWERON".equals(action)
				|| "com.htc.intent.action.QUICKBOOT_POWERON".equals(action)) {
			if (isEnabled()) {
				// If storage (e.g. MicroSD card) is already mounted at boot, launch fast!
				long delay = isStorageMounted() ? FAST_LAUNCH_DELAY : FALLBACK_DELAY;
				scheduleLaunch(ctx, delay);
			}
		} else if (Intent.ACTION_MEDIA_MOUNTED.equals(action)) {
			// USB drive or MicroSD card just mounted - launch immediately!
			if (isEnabled()) {
				cancelScheduledLaunch(ctx);
				launch(ctx);
			}
		} else if (ACTION_LAUNCH.equals(action)) {
			if (isEnabled()) launch(ctx);
		}
	}

	private static boolean isStorageMounted() {
		try {
			return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
		} catch (Throwable t) {
			return false;
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
		Log.d("BootReceiver: Launching Fermata MainActivity");
		try {
			Intent i = new Intent(ctx, MainActivity.class);
			i.setAction(INTENT_ACTION_AUTOSTART);
			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
			ctx.startActivity(i);
		} catch (Exception ex) {
			Log.e(ex, "Direct startActivity failed, falling back to full screen intent");
		}

		// FullScreenIntent fallback to guarantee background launch on Android 10+ and DiLink
		try {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
				NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
				if (nm != null) {
					String channelId = "fermata_autostart";
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
						NotificationChannel chan = new NotificationChannel(channelId, "Autostart", NotificationManager.IMPORTANCE_HIGH);
						chan.setSound(null, null);
						chan.enableVibration(false);
						nm.createNotificationChannel(chan);
					}
					Intent i = new Intent(ctx, MainActivity.class);
					i.setAction(INTENT_ACTION_AUTOSTART);
					i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
					PendingIntent pi = PendingIntent.getActivity(ctx, 1001, i,
							PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
					Notification.Builder nb = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
							? new Notification.Builder(ctx, channelId)
							: new Notification.Builder(ctx);
					nb.setSmallIcon(android.R.drawable.ic_media_play)
							.setContentTitle("BYD Video")
							.setContentText("Araç içi medya oynatıcı başlatılıyor...")
							.setFullScreenIntent(pi, true)
							.setAutoCancel(true);
					nm.notify(1001, nb.build());
					new Handler(Looper.getMainLooper()).postDelayed(() -> {
						try {
							nm.cancel(1001);
						} catch (Throwable ignored) {}
					}, 4000);
				}
			}
		} catch (Throwable t) {
			Log.e(t, "FullScreenIntent notification failed");
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

	private static void scheduleLaunch(Context ctx, long delayMs) {
		try {
			AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
			if (am == null) {
				launch(ctx);
				return;
			}
			Intent i = new Intent(ctx, BootReceiver.class);
			i.setAction(ACTION_LAUNCH);
			PendingIntent pi = PendingIntent.getBroadcast(ctx, 1000, i,
					PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
			am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,
					SystemClock.elapsedRealtime() + delayMs, pi);
			Log.d("BootReceiver: Scheduled launch in ", delayMs, " ms");
		} catch (Exception ex) {
			Log.e(ex, "Failed to schedule the autostart launch");
			launch(ctx);
		}
	}
}
