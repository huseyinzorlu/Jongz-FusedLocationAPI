package com.puangput.jongz.fusedsample;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;
import com.puangput.jongz.fusedlocation.FusedLocationManager;
import com.puangput.jongz.fusedlocation.OnLocationUpdate;

/**
 * Created by Sattha Puangput on 7/23/2015.
 */
public class SampleService extends Service implements OnLocationUpdate {

    private final String TAG = getClass().getSimpleName();
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
                .setRequestInterval(60 * 1000)
                .setRequestFastInterval(60 * 1000)
                .setRequestDistance(100)
                .setIsRequestDistance(true)
                .setCachedExpiredTime(0)
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
        Toast.makeText(getApplicationContext(), "Service: Update Location \nLongitude: "+ loc.getLongitude()+"\nLatitude:"+loc.getLatitude(), Toast.LENGTH_SHORT).show();
        Log.e(TAG, "update: keep tracking user location...");
        Log.e(TAG, "Longitude: " + loc.getLongitude());
        Log.e(TAG, "Latitude: " + loc.getLatitude());
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "onDestroy");
        // cleanup FusedLocationManger object
        manager.stop();
        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.e(TAG, "onTaskRemoved()");
        // cleanup FusedLocationManger object
        manager.stop();
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
