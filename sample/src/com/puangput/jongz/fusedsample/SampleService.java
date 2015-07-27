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

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "onCreate()");
        FusedLocationManager manager = new FusedLocationManager.Builder(this)
                .setRequestInterval(30 * 1000)
                .setRequestFastInterval(30 * 1000)
                .setRequestDistance(100)
                .setMaxRetry(3)
                .setRetryTimeout(20 * 1000)
                .build();
        manager.setOnLocationUpdateListener(this);
        manager.isLocationServiceCanUse();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "onStartCommand()");
        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.e(TAG, "onTaskRemoved()");
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

    @Override
    public void locationUpdate(Location loc) {
        Log.e(TAG, "update: keep tracking user location...");
        Log.e(TAG, "Longitude: " + loc.getLongitude());
        Log.e(TAG, "Latitude: " + loc.getLatitude());
    }
}
