package com.puangput.jongz.fusedsample;

import android.app.Application;
import com.puangput.jongz.fusedlocation.FusedLocationManager;

/**
 * Created by Sattha Puangput on 7/23/2015.
 */
public class SampleApp extends Application {

    private static SampleApp app;
    private FusedLocationManager manager;

    public static SampleApp getInstance() {
        return app;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
    }

    public FusedLocationManager getLocationManager() {
        if (manager == null) {
            manager = new FusedLocationManager.Builder(this)
                    .setRequestInterval(30 * 1000)
                    .setRequestFastInterval(30 * 1000)
                    .setRequestDistance(100)
                    .setMaxRetry(3)
                    .setRetryTimeout(20 * 1000)
                    .build();
        }
        return manager;
    }
}
