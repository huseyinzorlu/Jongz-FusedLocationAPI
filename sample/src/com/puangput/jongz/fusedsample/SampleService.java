package com.puangput.jongz.fusedsample;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import com.puangput.jongz.fusedlocation.FusedLocationManager;
import com.puangput.jongz.fusedlocation.OnLocationUpdate;

/**
 * Created by Sattha Puangput on 7/23/2015.
 */
public class SampleService extends Service implements OnLocationUpdate {

    private final String TAG = getClass().getSimpleName();

    public static final String ACTION_ON_LOCATION_UPDATE = "on_location_update";
    public static final String EXTRA_LOCATION = "extra_location";
    private FusedLocationManager manager;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "onCreate()");
        manager = new FusedLocationManager.Builder(this)
                .setRequestInterval(30 * 1000)
                .setRequestFastInterval(30 * 1000)
                .setRequestDistance(100)
                .setIsRequestDistance(false)
                .setCachedExpiredTime(0)
                .setRetryTimeout(20 * 1000)
                .setMaxRetry(3)
                .build();
        manager.setOnLocationUpdateListener(this);
        manager.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "onStartCommand()");

        if (manager.isConnect()) {
            manager.checkLocationProviderPrompt();
        }

        return START_STICKY;
    }

    @Override
    public void locationUpdate(Location loc) {
        sendBroadcast(new Intent().setAction(ACTION_ON_LOCATION_UPDATE).putExtra(EXTRA_LOCATION, manager.toString(loc)));
        Log.e(TAG, "update: keep tracking user location...");
        Log.e(TAG, "Longitude: " + loc.getLongitude());
        Log.e(TAG, "Latitude: " + loc.getLatitude());
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.e(TAG, "onTaskRemoved()");
        // restart service from clear recent
        Intent restartService = new Intent(getApplicationContext(), this.getClass());
        restartService.setPackage(getPackageName());
        PendingIntent restartServicePI = PendingIntent.getService(
                getApplicationContext(),
                1,
                restartService,
                PendingIntent.FLAG_ONE_SHOT);
        AlarmManager alarmService = (AlarmManager) getApplicationContext().getSystemService(ALARM_SERVICE);
        alarmService.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1000, restartServicePI);
    }
}
