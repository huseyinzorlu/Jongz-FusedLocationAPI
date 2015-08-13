package com.puangput.jongz.fusedlocation;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.*;
import com.google.gson.Gson;

/**
 * Created by Sattha Puangput on 7/23/2015.
 */
public class FusedLocationManager implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private final String TAG = getClass().getSimpleName();

    private static final String SHARED_PREF_LOCATION_STAMP = "shared_pref_location_stamp";
    private static final String LOCATION_LAST_TIME = "location_last_time";
    private static final String LOCATION_OBJECT = "location_object";

    private final boolean IS_REQUEST_DISTANCE;
    private final int MAX_RETRY;
    private final int EXPIRED_TIME;
    private final long RETRY_TIMEOUT;
    private final long REQUEST_INTERVAL;
    private final long REQUEST_FAST_INTERVAL;
    private final float REQUEST_DISTANCE;

    private int requestRetry = 0;
    private boolean isRequestRetrying = false;
    private Context context;
    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;
    private OnLocationUpdate onLocationUpdate;
    private OnLocationResponse onLocationResponse;

    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable runnableFetchLocation = new Runnable() {
        @Override
        public void run() {
            fetchLocationData();
        }
    };

    public FusedLocationManager(Builder builder) {
        this.context = builder.context;
        this.MAX_RETRY = builder.maxRetry;
        this.RETRY_TIMEOUT = builder.retryTimeOut;
        this.REQUEST_INTERVAL = builder.requestInterval;
        this.REQUEST_FAST_INTERVAL = builder.requestFastInterval;
        this.REQUEST_DISTANCE = builder.requestDistance;
        this.IS_REQUEST_DISTANCE = builder.isRequestDistance;
        this.EXPIRED_TIME = builder.expiredTime;
        configureAPI();
    }

    public static class Builder {

        private Context context;
        private int maxRetry = 3;
        private long retryTimeOut = 20 * 1000; // 20 sec.
        private long requestInterval = 30 * 60 * 1000; // 30 min.
        private long requestFastInterval = 30 * 60 * 1000; // 30 min.
        private float requestDistance = 500; // 500 m.
        private boolean isRequestDistance = true;
        private int expiredTime = 0; // 1 min

        public Builder(Context context) {
            this.context = context;
        }

        public Builder setRequestInterval(long milliSec) {
            this.requestInterval = milliSec;
            return this;
        }

        public Builder setRequestFastInterval(long milliSec) {
            this.requestFastInterval = milliSec;
            return this;
        }

        public Builder setRequestDistance(int m) {
            this.requestDistance = m;
            return this;
        }

        public Builder setRetryTimeout(long milliSec) {
            this.retryTimeOut = milliSec;
            return this;
        }

        public Builder setMaxRetry(int times) {
            this.maxRetry = times;
            return this;
        }

        public Builder setIsRequestDistance(boolean b) {
            this.isRequestDistance = b;
            return this;
        }

        public Builder setCachedExpiredTime(int milliSec) {
            this.expiredTime = milliSec;
            return this;
        }

        public FusedLocationManager build() {
            return new FusedLocationManager(this);
        }
    }

    /**
     * Configure API
     * Calling this method to let's library configure its location spec
     * */
    private void configureAPI() {
        // setup google API client
        googleApiClient = new GoogleApiClient.Builder(context)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        // setup location request criteria
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        locationRequest.setInterval(REQUEST_INTERVAL);
        locationRequest.setFastestInterval(REQUEST_FAST_INTERVAL);
        if (IS_REQUEST_DISTANCE) locationRequest.setSmallestDisplacement(REQUEST_DISTANCE);
    }

    /**
     * Is Connect
     * Calling this function for check this instance was call start() or not.
     * */
    public boolean isConnect() {
        return (googleApiClient.isConnected() || googleApiClient.isConnecting());
    }

    /**
     * Start using GPS listener
     * Calling this function will start using GPS in your app
     * */
    public void start(){
        if (!googleApiClient.isConnected() && !googleApiClient.isConnecting()) {
            googleApiClient.connect();
        }
    }

    /**
     * Stop using GPS listener
     * Calling this function will stop using GPS in your app
     * */
    public void stop(){

        // disconnect google api
        try {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } finally {
            googleApiClient.disconnect();
        }

        clearFlag();
    }

    /**
     * Clear Flag
     * Calling this function will clear all flag before start() again
     * */
    private void clearFlag() {
        // response back to caller if stop() call while requesting
        if (isRequestRetrying) {
            if (onLocationResponse != null) {
                handler.removeCallbacks(runnableFetchLocation);
                   onLocationResponse.LocationResponseFailure("Service was stop.");
            }
        }
        requestRetry = 0;
        isRequestRetrying = false;
    }

    /**
     * Check Location Service
     * Calling this method to check user's phone requirement is prompt to use this services
     *
     * Note : this methods return result from current system settings, but fixing activity
     *        won't be launch if start() method was not called.
     * */
    public synchronized boolean checkLocationProviderPrompt() {
        return  (checkPlayServices() && checkLocationSettings());
    }

    /**
     * Check Google Play Services
     * Calling this method to check and popup fix dialog, if possible
     * */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                Intent i = new Intent(context, FixLocationPermissionActivity.class);
                i.putExtra(FixLocationPermissionActivity.CHECK_PLAY_SERVICES, resultCode);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                context.startActivity(i);
            } else {
                Toast.makeText(context, "This device is not supported.", Toast.LENGTH_LONG).show();
            }
            return false;
        }
        return true;
    }

    /**
     * Check Location Settings
     * Calling this method to check a location settings
     * */
    private boolean checkLocationSettings() {
        LocationManager locationServices = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean isGPSEnabled = locationServices.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationServices.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        showLocationSettings();
        return (isGPSEnabled || isNetworkEnabled);
    }

    /**
     * Fix Location Settings
     * Calling this method to popup a location settings dialog
     * */
    private void showLocationSettings() {

        LocationSettingsRequest locationSettingsRequest = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
                .setAlwaysShow(true) //this is show settings dialog
                .build();

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(googleApiClient, locationSettingsRequest);

        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                final LocationSettingsStates state = result.getLocationSettingsStates();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can initialize location
                        // requests here.
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:

                        // if retry from get last location, update result to caller.
                        if (isRequestRetrying) {
                            isRequestRetrying = false;
                            updateResultToCaller(false, null, "Location settings are not satisfied.");
                        }

                        // Location settings are not satisfied. But could be fixed by showing the user
                        // a dialog.
                        Intent i = new Intent(context, FixLocationPermissionActivity.class);
                        i.putExtra(FixLocationPermissionActivity.CHECK_LOCATION_SERVICES, status);
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        context.startActivity(i);
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way to fix the
                        // settings so we won't show the dialog.
                        break;
                }
            }
        });
    }

    @Override
    public void onConnected(Bundle bundle) {
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        clearFlag();
        googleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        clearFlag();
        googleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
        saveCachedLocation(location);
        // sent to onLocationUpdateListener.
        if (onLocationUpdate != null) onLocationUpdate.locationUpdate(location);

        // if retry from get last location, update result to caller.
        // cancel runnable stop waiting result.
        // coz, onLocationChanged might trigger first after location permission allow.
        if (isRequestRetrying) {
            isRequestRetrying = false;
            updateResultToCaller(true, location, null);
        }
    }

    /**
     * On Location Update
     * An interface to receive location update from google api
     * */
    public void setOnLocationUpdateListener(OnLocationUpdate l) {
        onLocationUpdate = l;
    }

    /**
     * Get Last Location
     * Calling this method to get user last update location
     * */
    public void getLastLocation(OnLocationResponse l) {

        onLocationResponse = l;

        if (googleApiClient.isConnected() || googleApiClient.isConnecting()) {
            fetchLocationData();
        } else {
            if (onLocationResponse != null)
                onLocationResponse.LocationResponseFailure("FusedLocationManager instance is not call start() yet !");
        }
    }

    /**
     * Request data from google api
     * Handler the delay of getting the location from api
     * */
    private void fetchLocationData() {

        Location loc = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);

        if (loc == null) {
            if (isCachedLocationExpired()) {
                checkLocationProviderPrompt();
                if (requestRetry != MAX_RETRY) {
                    isRequestRetrying = true;
                    handler.postDelayed(runnableFetchLocation, RETRY_TIMEOUT);
                    ++requestRetry;
                } else {
                    isRequestRetrying = false;
                    requestRetry = 0;
                    if (onLocationResponse != null) {
                        onLocationResponse.LocationResponseFailure("Error location request timeout : " + MAX_RETRY);
                    }
                }
            } else {
                if (onLocationResponse != null) {
                    Log.e(TAG, "used cached location");
                    onLocationResponse.LocationResponseSuccess(getCachedLocation());
                }
            }
        } else {
            saveCachedLocation(loc);
            if (onLocationResponse != null) {
                onLocationResponse.LocationResponseSuccess(loc);
            }
        }
    }

    private void updateResultToCaller(boolean isSuccess, Location loc, String msg) {
        if (isSuccess) {
            handler.removeCallbacks(runnableFetchLocation);
            if (onLocationResponse != null)
                onLocationResponse.LocationResponseSuccess(loc);
        } else {
            handler.removeCallbacks(runnableFetchLocation);
            if (onLocationResponse != null)
                onLocationResponse.LocationResponseFailure(msg);
        }
    }

    /**
     * To String
     * A method which convert Location object to json string
     * */
    public String toString(Location loc) {
        Gson gson = new Gson();
        return gson.toJson(loc);
    }

    /**
     * To Location
     * A method which convert Json String of Location object object to Location Object
     * */
    public Location toLocation(String sLocation) {
        Gson gson = new Gson();
        return gson.fromJson(sLocation, Location.class);
    }

    /**
     * Save Temp Latitude and Longitude
     * A method which save current location and timestamp for later use.
     * */
    private synchronized void saveCachedLocation(Location loc) {
        SharedPreferences pref = context.getSharedPreferences(SHARED_PREF_LOCATION_STAMP, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(LOCATION_OBJECT, toString(loc)).apply();
        editor.putLong(LOCATION_LAST_TIME, SystemClock.elapsedRealtime()).apply();
    }

    /**
     * Is Temp Location Expired
     * A method which check your last location temp is still usable.
     * set expiredTime() to 0 for disable use of temp location (default)
     * */
    public boolean isCachedLocationExpired() {
        SharedPreferences pref = context.getSharedPreferences(SHARED_PREF_LOCATION_STAMP, Context.MODE_PRIVATE);
        long lastTime = pref.getLong(LOCATION_LAST_TIME, -1);
        return (((SystemClock.elapsedRealtime() - lastTime) > EXPIRED_TIME) || lastTime == -1);
    }

    /**
     * Get Temp Location
     * A method retrieve temp. location data
     * */
    public Location getCachedLocation() {
        SharedPreferences pref = context.getSharedPreferences(SHARED_PREF_LOCATION_STAMP, Context.MODE_PRIVATE);
        String json = pref.getString(LOCATION_OBJECT, "");
        if (!json.equals("")) {
            return toLocation(json);
        } else {
            return null;
        }
    }

    /**
     * Clean Up
     * Calling this function will clean up all flag in this class
     * */
    public void cleanUp() {
        if(googleApiClient != null){
            googleApiClient.unregisterConnectionCallbacks(this);
            googleApiClient.unregisterConnectionFailedListener(this);
            if (googleApiClient.isConnecting() || googleApiClient.isConnected()) {
                googleApiClient.disconnect();
            }
            googleApiClient = null;
        }
        if (locationRequest != null) {
            locationRequest = null;
        }
    }

}
