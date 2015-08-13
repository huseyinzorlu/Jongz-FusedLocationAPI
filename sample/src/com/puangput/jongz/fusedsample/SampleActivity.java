package com.puangput.jongz.fusedsample;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.puangput.jongz.fusedlocation.OnLocationResponse;

public class SampleActivity extends Activity implements View.OnClickListener{

    private final String TAG = getClass().getSimpleName();
    public LoadingDialog loadingDialog;
    public SampleApp app;
    private TextView tvLat;
    private TextView tvLon;
    private TextView tvProvider;
    private TextView tvEvent;
    private TextView tvError;
    private Button btnGet;
    private OnLocationUpdateReceiver locationUpdateReceiver;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        tvLat = (TextView) findViewById(R.id.lat);
        tvLon = (TextView) findViewById(R.id.lon);
        tvProvider = (TextView) findViewById(R.id.provider);
        tvEvent = (TextView) findViewById(R.id.tvEvent);
        tvError = (TextView) findViewById(R.id.tvError);
        btnGet = (Button)findViewById(R.id.btnGet);
        btnGet.setOnClickListener(this);
        app = SampleApp.getInstance();
        loadingDialog = new LoadingDialog(this);
        startService(new Intent(this, SampleService.class));
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter intf = new IntentFilter();
        intf.addAction(SampleService.ACTION_ON_LOCATION_UPDATE);
        locationUpdateReceiver = new OnLocationUpdateReceiver();
        registerReceiver(locationUpdateReceiver, intf);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(locationUpdateReceiver);
    }

    @Override
    public void onClick(View v) {

        loadingDialog.show();

        app.getFLM().start();
        app.getFLM().getLastLocation(new OnLocationResponse() {
            @Override
            public void LocationResponseSuccess(Location loc) {
                Log.i(TAG, "Latitude=" + loc.getLatitude() + ", " + "Longitude=" + loc.getLongitude() + ", " + loc.getProvider() + ", onClick()");
                updateMessage(loc, "onClick()", null);
                loadingDialog.dismiss();
                app.getFLM().stop();
            }

            @Override
            public void LocationResponseFailure(String error) {
                Log.e(TAG, error);
                updateMessage(null, "onClick()", error);
                loadingDialog.dismiss();
                app.getFLM().stop();
            }
        });
    }

    private void updateMessage(Location loc, String event, String err) {
        if (loc == null) {
            tvLat.setText("Latitude: ---");
            tvLon.setText("Longitude: ---");
            tvProvider.setText("Provider: ---");
            tvEvent.setText("Event: " + event);
            tvError.setText("Error: " + err);
        } else {
            tvLat.setText("Latitude: " + loc.getLatitude());
            tvLon.setText("Longitude: " + loc.getLongitude());
            tvProvider.setText("Provider: " + loc.getProvider());
            tvEvent.setText("Event: " + event);
            tvError.setText("Error: ---");
        }
    }

    private class OnLocationUpdateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String sLocation = intent.getStringExtra(SampleService.EXTRA_LOCATION);
            Location location = app.getFLM().toLocation(sLocation);
            updateMessage(location, "Service", null);
        }
    }

}
