package io.evolution.downtohang;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;


/**
 * The location listener for the entire app.
 */
class AppLocationListener implements LocationListener {

    private Location location;



    public Location getLocation() {
        return location;
    }

    /**
     * When the location is changed, update this location if it's not null.
     * @param location the new location.
     */
    @Override
    public void onLocationChanged(Location location) {
        if(location != null) {
            this.location = location;
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // do nothing
    }

    @Override
    public void onProviderEnabled(String provider) {
        // do nothing
    }

    @Override
    public void onProviderDisabled(String provider) {
        // do nothing
    }

    /**
     * Set a new location
     * @param newLoc the new location
     */
    public void setLocation(Location newLoc) {
        this.location = newLoc;
    }
}