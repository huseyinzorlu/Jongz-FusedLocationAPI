package com.puangput.jongz.fusedlocation;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.*;

/**
 * Created by Sattha Puangput on 7/23/2015.
 */
public class FusedLocationManager implements  GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private final String TAG = getClass().getSimpleName();

    private static final int MAX_RETRY = 3;
    private static final int RETRY_DELAY = 20 * 1000;
    private static final long REQUEST_INTERVAL = 5 * 60 * 1000;
    private static final long REQUEST_FAST_INTERVAL = 5 * 60 * 1000;
    private static final float REQUEST_DISTANCE = 100;
    public static final String LOCATION_FIX_SCREEN = "location_fix_screen";
    public static final String IS_CLOSE = "is_close";

    private boolean isConnected = false;
    private boolean isConnecting = false;
    private boolean isRequestRetrying = false;
    private boolean isGetLastLocationWaitConnection = false;
    private int requestRetry = 0;

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

    public FusedLocationManager(Context context) {
        this.context = context;
        configureAPI();
    }

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
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY );
        locationRequest.setInterval(REQUEST_INTERVAL);
        locationRequest.setFastestInterval(REQUEST_FAST_INTERVAL);
        locationRequest.setSmallestDisplacement(REQUEST_DISTANCE);
    }

    public synchronized boolean isLocationServiceCanUse() {
        return  (checkPlayServices() && checkLocationSettings());
    }

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

    private boolean checkLocationSettings() {
        LocationManager locationServices = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean isGPSEnabled = locationServices.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationServices.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        boolean isLocationAccuracy = (isGPSEnabled && isNetworkEnabled);
        if (!isLocationAccuracy) {
            showLocationSettings();
        }
        return isLocationAccuracy;
    }

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
        isLocationServiceCanUse();
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

    public void getLastLocation(OnLocationResponse l) {

        this.onLocationResponse = l;

        if (isConnected && !isConnecting) {
            fetchLocationData();

        } else if (!isConnected && isConnecting) {
            isGetLastLocationWaitConnection = true;

        } else {
            if (onLocationResponse != null)
                onLocationResponse.LocationResponseFailure("Location service not start() yet !");

        }
    }

    private void fetchLocationData() {

        Location loc = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);

        if (loc == null) {
            if (isLocationServiceCanUse()) {
                if (requestRetry != MAX_RETRY) {
                    isRequestRetrying = true;
                    handler.postDelayed(runnableFetchLocation, RETRY_DELAY);
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
                    onLocationResponse.LocationResponseFailure("Location service settings not meet requirement.");
                }
            }
        } else {
            requestRetry = 0;
            if (onLocationResponse != null) {
                onLocationResponse.LocationResponseSuccess(loc);
            }
        }
    }

    /**
     * Star using GPS listener
     * Calling this function will start using GPS in your app
     * */
    public void startUsingGPS(){
        if (!isConnected && !isConnecting) {
            if(googleApiClient != null){
                isConnecting = true;
                googleApiClient.connect();
            }
        }
    }

    /**
     * Stop using GPS listener
     * Calling this function will stop using GPS in your app
     * */
    public void stopUsingGPS(){
        if(googleApiClient != null){
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
            googleApiClient.disconnect();
            clearFlag();
        }
    }

    private void clearFlag() {
        // case : while request waiting service was stop unexpected or user call stopUsingGPS()
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

    public void setOnLocationUpdateListener(OnLocationUpdate l) {
        onLocationUpdate = l;
    }

}
