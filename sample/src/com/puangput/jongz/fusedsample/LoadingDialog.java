package com.puangput.jongz.fusedsample;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

/**
 * Created by Sattha Puangput on 7/16/2015.
 */
public class LoadingDialog {

    private final Activity activity;
    private ProgressDialog progressDialog;
    private Handler handler = new Handler(Looper.getMainLooper());

    public LoadingDialog(Activity activity) {
        this.activity = activity;
    }

    public void show(String message) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (progressDialog == null || !progressDialog.isShowing()) {
                    progressDialog = ProgressDialog.show(activity, "", message);
                    progressDialog.show();
                }
            }
        });

    }

    public void show() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (progressDialog == null || !progressDialog.isShowing()) {
                    progressDialog = ProgressDialog.show(activity, "", "กรุณารอสักครู่");
                    progressDialog.show();
                }
            }
        });
    }


    public void dismiss() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (progressDialog != null) {
                    if (progressDialog.isShowing()) {
                        try {
                            progressDialog.dismiss();
                        } catch (IllegalArgumentException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }
}
