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
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.*;
import com.google.gson.Gson;

import java.lang.ref.WeakReference;

/**
 * Created by Sattha Puangput on 7/23/2015.
 */
public class FusedLocationManager implements  GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private final String TAG = getClass().getSimpleName();

    private static final String SHARED_PREF_LOCATION_STAMP = "shared_pref_location_stamp";
    private static final String LOCATION_LATITUDE = "location_stamp";
    private static final String LOCATION_LONGITUDE = "location_longitude";
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
    private boolean isConnected = false;
    private boolean isConnecting = false;
    private boolean isRequestRetrying = false;
    private boolean isGetLastLocationWaitConnection = false;
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

        public Builder setExpiredTime(int milliSec) {
            this.expiredTime = milliSec;
            return this;
        }

        public FusedLocationManager build() {
            return new FusedLocationManager(this);
        }
    }

    /**
     * Check Location Provider
     * Calling this static method to check location service provider.
     * For BroadcastReceiver use case, which kill object after intent
     * finish their job no stop() required.
     * */
    public static void checkLocationProvider(Context context){
        FusedLocationManager m = new FusedLocationManager.Builder(context).build();
        m.start(); m.isLocationProviderCanUse();
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
        googleApiClient.connect();
        isConnecting = true;

        // setup location request criteria
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        locationRequest.setInterval(REQUEST_INTERVAL);
        locationRequest.setFastestInterval(REQUEST_FAST_INTERVAL);
        if (IS_REQUEST_DISTANCE) locationRequest.setSmallestDisplacement(REQUEST_DISTANCE);
    }

    /**
     * Check Location Service
     * Calling this method to check user's phone requirement is prompt to use this services
     * */
    private synchronized boolean isLocationProviderCanUse() {
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
        isConnected = true;
        isConnecting = false;
        isLocationProviderCanUse();
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
        // case : getLastLocation() was call before onConnected was trigger
        if (isGetLastLocationWaitConnection) {
            isGetLastLocationWaitConnection = false;
            fetchLocationData();
        }
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
        saveTempLatLng(location);
        // sent to onLocationUpdateListener.
        if (onLocationUpdate != null) onLocationUpdate.locationUpdate(location);
        // if retry from get last location update result to caller.
        // cancel runnable stop waiting result.
        // coz, onLocationChanged might trigger first after location permission allow.
        if (isRequestRetrying) {
            isRequestRetrying = false;
            handler.removeCallbacks(runnableFetchLocation);
            if (onLocationResponse != null)
                onLocationResponse.LocationResponseSuccess(location);
        }
    }

    /**
     * Get Last Location
     * Calling this method to get user last update location
     * */
    public void getLastLocation(OnLocationResponse l) {

        this.onLocationResponse = l;

        if (isConnected && !isConnecting) {
            fetchLocationData();

        } else if (!isConnected && isConnecting) {
            isGetLastLocationWaitConnection = true;

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
            if (isLocationProviderCanUse()) {
                if (isLocationTempExpired()) {
                    if (requestRetry != MAX_RETRY) {
                        isRequestRetrying = true;
                        handler.postDelayed(runnableFetchLocation, RETRY_TIMEOUT);
                        ++requestRetry;
                    } else {
                        isRequestRetrying = false;
                        requestRetry = 0;
                        if (onLocationResponse != null) {
                            onLocationResponse.LocationResponseFailure("Error request timeout : " + MAX_RETRY);
                        }
                    }
                } else {
                    if (onLocationResponse != null) {
                        Toast.makeText(context, "Location From Temp", Toast.LENGTH_SHORT).show();
                        onLocationResponse.LocationResponseSuccess(getTempLocation());
                    }
                }
            } else {
                if (onLocationResponse != null) {
                    onLocationResponse.LocationResponseFailure("Location service settings not meet requirement.");
                }
            }
        } else {
            requestRetry = 0;
            if (onLocationResponse != null) {
                onLocationResponse.LocationResponseSuccess(loc);
                saveTempLatLng(loc);
            }
        }
    }

    /**
     * Star using GPS listener
     * Calling this function will start using GPS in your app
     * */
    public void start(){
        if (!isConnected && !isConnecting) {
            configureAPI();
        }
    }

    /**
     * Stop using GPS listener
     * Calling this function will stop using GPS in your app
     * */
    public void stop(){
        if(googleApiClient != null){
            try {
                LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
            } catch (IllegalStateException e) {
                e.printStackTrace();
            } finally {
                googleApiClient.disconnect();
                googleApiClient = null;
                locationRequest = null;
            }
        }
        clearFlag();
    }

    /**
     * Clear Flag
     * Calling this function will clear all flag in this class
     * */
    private void clearFlag() {
        // case : while request waiting service was stop unexpected or user call stop()
        if (isGetLastLocationWaitConnection) {
            if (onLocationResponse != null) {
                handler.removeCallbacks(runnableFetchLocation);
                onLocationResponse.LocationResponseFailure("Service was stop.");
            }
        }
        // remove all Flags
        isConnected = false;
        isConnecting = false;
        isRequestRetrying = false;
        isGetLastLocationWaitConnection = false;
    }

    /**
     * On Location Update
     * An interface to receive location update from google api
     * */
    public void setOnLocationUpdateListener(OnLocationUpdate l) {
        onLocationUpdate = l;
    }

    /**
     * Save Temp Latitude and Longitude
     * A method which save current location and timestamp for later use.
     * */
    private void saveTempLatLng(Location loc) {
        Gson gson = new Gson();
        String locJson = gson.toJson(loc);
        SharedPreferences pref = context.getSharedPreferences(SHARED_PREF_LOCATION_STAMP, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putLong(LOCATION_LATITUDE, Double.doubleToRawLongBits(loc.getLatitude())).apply();
        editor.putLong(LOCATION_LONGITUDE, Double.doubleToRawLongBits(loc.getLongitude())).apply();
        editor.putString(LOCATION_OBJECT, locJson).apply();
        editor.putLong(LOCATION_LAST_TIME, SystemClock.elapsedRealtime()).apply();
    }

    /**
     * Is Location Temp Expired
     * A method which check your last location temp is still usable.
     * set expiredTime() to 0 for disable use of temp location (default)
     * */
    public boolean isLocationTempExpired() {
        SharedPreferences pref = context.getSharedPreferences(SHARED_PREF_LOCATION_STAMP, Context.MODE_PRIVATE);
        long lastTime = pref.getLong(LOCATION_LAST_TIME, -1);
        return (((SystemClock.elapsedRealtime() - lastTime) > EXPIRED_TIME) || lastTime == -1);
    }

    /**
     * Get Temp Location
     * A method retrieve temp. location data
     * */
    public Location getTempLocation() {
        SharedPreferences pref = context.getSharedPreferences(SHARED_PREF_LOCATION_STAMP, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = pref.getString(LOCATION_OBJECT, "");
        if (!json.equals("")) {
            return gson.fromJson(json, Location.class);
        } else {
            return null;
        }
    }

    /**
     * Get Temp Latitude
     * A method retrieve temp. latitude data
     * */
    public double getTempLatitude() {
        SharedPreferences pref = context.getSharedPreferences(SHARED_PREF_LOCATION_STAMP, Context.MODE_PRIVATE);
        return Double.longBitsToDouble(pref.getLong(LOCATION_LATITUDE, 0));
    }

    /**
     * Get Temp Longitude
     * A method retrieve temp. longitude data
     * */
    public double getTempLongitude() {
        SharedPreferences pref = context.getSharedPreferences(SHARED_PREF_LOCATION_STAMP, Context.MODE_PRIVATE);
        return Double.longBitsToDouble(pref.getLong(LOCATION_LONGITUDE, 0));
    }

}
