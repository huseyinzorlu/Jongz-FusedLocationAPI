package com.puangput.jongz.fusedsample;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.util.Log;
import com.puangput.jongz.fusedlocation.FusedLocationManager;

/**
 * Created by Sattha Puangput on 7/23/2015.
 */
public class SampleReceiver extends BroadcastReceiver {

    private final String TAG = this.getClass().getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().matches(LocationManager.PROVIDERS_CHANGED_ACTION)) {
            Log.e(TAG, "onReceive was call because location providers have changes");
            FusedLocationManager.checkLocationProvider(context);
        }
    }
}
