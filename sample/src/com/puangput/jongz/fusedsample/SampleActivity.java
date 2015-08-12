package com.puangput.jongz.fusedsample;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.puangput.jongz.fusedlocation.OnLocationResponse;

public class SampleActivity extends Activity implements View.OnClickListener{

    private final String TAG = getClass().getSimpleName();
    public LoadingDialog loadingDialog;
    public SampleApp app;
    private TextView tvLat;
    private TextView tvLon;
    private TextView tvProvider;
    private TextView tvEvent;
    private Button btnGet;

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
        btnGet = (Button)findViewById(R.id.btnGet);
        btnGet.setOnClickListener(this);
        app = SampleApp.getInstance();
        loadingDialog = new LoadingDialog(this);
        startService(new Intent(this, SampleService.class));
    }

    @Override
    public void onClick(View v) {

        loadingDialog.show();

        app.getFLM().start();
        app.getFLM().getLastLocation(new OnLocationResponse() {
            @Override
            public void LocationResponseSuccess(Location loc) {
                Log.i(TAG, "Latitude=" + loc.getLatitude() + ", " + "Longitude=" + loc.getLongitude() + ", " + loc.getProvider() + ", onClick()");
                Toast.makeText(getApplicationContext(), "Latitude=" + loc.getLatitude() + ", " + "Longitude=" + loc.getLongitude() + ", " + loc.getProvider() + ", onClick()", Toast.LENGTH_SHORT).show();
                onReceiveLocation(loc, "OnClick()");
                loadingDialog.dismiss();
                app.getFLM().stop();
            }

            @Override
            public void LocationResponseFailure(String error) {
                Log.e(TAG, error);
                Toast.makeText(getApplicationContext(), error, Toast.LENGTH_SHORT).show();
                loadingDialog.dismiss();
                app.getFLM().stop();
            }
        });
    }

    public void onReceiveLocation(Location loc, String event) {
        tvLat.setText("Latitude: " + loc.getLatitude());
        tvLon.setText("Longitude: " + loc.getLongitude());
        tvProvider.setText("Provider: " + loc.getProvider());
        tvEvent.setText("Event: " + event);
    }

}
