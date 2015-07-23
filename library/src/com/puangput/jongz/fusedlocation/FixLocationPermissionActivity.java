package com.puangput.jongz.fusedlocation;

import android.app.Activity;
import android.app.Dialog;
import android.content.*;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.Status;
import com.puangput.jongz.fusedlocationapi.R;


public class FixLocationPermissionActivity extends Activity {
    public static final String CHECK_PLAY_SERVICES = "check_play_services";
    public static final String CHECK_LOCATION_SERVICES = "check_location_services";
    private static final int RESULT_LOCATION_SERVICE = 1000;
    private static final int RESULT_PLAY_SERVICES = 2000;
    private Status mStatus;
    private boolean isCanFinish = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(0,0);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.fixfusedlocation);
        Intent i = getIntent();
        if (i != null) {
            if (i.hasExtra(CHECK_PLAY_SERVICES)) {
                int resultCode = i.getIntExtra(CHECK_PLAY_SERVICES, ConnectionResult.SUCCESS);
                doCheckPlayServices(resultCode);
            } else if (i.hasExtra(CHECK_LOCATION_SERVICES)) {
                Status status = i.getParcelableExtra(CHECK_LOCATION_SERVICES);
                if (status != null) {
                    doCheckLocationService(status);
                }
            }
        }
    }

    private void doCheckLocationService(Status status) {
        mStatus = status;
        try {
            // Show the dialog by calling startResolutionForResult(),
            // and check the result in onActivityResult().
            status.startResolutionForResult(this, RESULT_LOCATION_SERVICE);
        } catch (IntentSender.SendIntentException e) {
            e.printStackTrace();
        }
    }

    private void doCheckPlayServices(int resultCode) {
        Dialog dialog = GooglePlayServicesUtil.getErrorDialog(resultCode, this, RESULT_PLAY_SERVICES);
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        dialog.setCancelable(false);
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                doCheckPlayServices(resultCode);
            }
        });
        dialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RESULT_LOCATION_SERVICE) {
            switch (resultCode) {
                case Activity.RESULT_OK:
                    // TODO
                    isCanFinish = true;
                    onBackPressed();
                    break;
                case Activity.RESULT_CANCELED:
                    // TODO
                    isCanFinish = false;
                    doCheckLocationService(mStatus);
                    break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        SharedPreferences pref = getSharedPreferences(FusedLocationManager.LOCATION_FIX_SCREEN, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean(FusedLocationManager.IS_CLOSE, true).apply();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (isCanFinish) super.onBackPressed();
    }
}
