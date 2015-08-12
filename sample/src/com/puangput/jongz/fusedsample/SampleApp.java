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

    public FusedLocationManager getFLM() {
        if (manager == null) {
            manager = new FusedLocationManager.Builder(this)
                    .setIsRequestDistance(false) // not use distance update
                    .setCachedExpiredTime(15 * 1000) // set zero value to disable cached location
                    .build();
        }
        return manager;
    }
}
