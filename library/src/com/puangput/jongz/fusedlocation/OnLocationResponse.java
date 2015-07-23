package com.puangput.jongz.fusedlocation;

import android.location.Location;

/**
 * Created by Sattha Puangput on 7/23/2015.
 */
public interface OnLocationResponse {
    void LocationResponseSuccess(Location location);
    void LocationResponseFailure(String error);
}
