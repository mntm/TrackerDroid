package ca.polymtl.inf8405.g2.trackerdroid.controller;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.lang.ref.WeakReference;
import java.util.Observable;


public class GPSController extends Observable implements LocationListener {

    private static final String TAG = "GPS";
    private static GPSController _inst = null;
    private LocationManager lm = null;
    private LocationListener listener;
    private Location current = null;

    private WeakReference<Context> main;

    private int speed = 0;
    private int minTime = 0;
    private int minDistance = 0;
    private boolean started = false;
    private String provider;

    private GPSController() {
    }

    public static GPSController buildInstance(Context activity) {
        if (_inst == null) {
            _inst = new GPSController();
            _inst.setActivity(activity);
            _inst.setLocationManager();
        }
        return _inst;
    }

    public static GPSController getInstance() {
        return _inst;
    }

    private void setActivity(Context activity) {
        this.main = new WeakReference<>(activity);
    }

    private void setLocationManager() {
        this.lm = (LocationManager) main.get().getSystemService(Context.LOCATION_SERVICE);
    }

    public void setLocationListener(LocationListener listener) {
        this.listener = listener;
    }

    public boolean isEnabled() {
        if (ContextCompat.checkSelfPermission(this.main.get(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        for (String provider : this.lm.getAllProviders()) {
            if (this.lm.isProviderEnabled(provider)) {
                return true;
            }
        }
        return false;
    }


    private String getBestProvider() {
        if (this.lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
            return LocationManager.NETWORK_PROVIDER;

        if (this.lm.isProviderEnabled(LocationManager.GPS_PROVIDER))
            return LocationManager.GPS_PROVIDER;

        if (this.lm.isProviderEnabled(LocationManager.PASSIVE_PROVIDER))
            return LocationManager.PASSIVE_PROVIDER;

        return null;
    }

    public void startLocationUpdates(int minTime, int minDistance) {
        this.minTime = minTime;
        this.minDistance = minDistance;
        this.startLocationUpdates(this.getBestProvider(), minTime, minDistance);
        started = true;
    }

    private void startLocationUpdates(String provider, int minTime, int minDistance) {
        try {
            if (ContextCompat.checkSelfPermission(this.main.get(),
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                this.lm.requestLocationUpdates(provider, minTime, minDistance, this);
                this.provider = provider;
                Log.d(TAG, "Selected provider (BEST!!!): " + provider);
            }
        } catch (NullPointerException n) {
            Log.w(TAG, "Request for location permission before requesting update it.");
        }
    }

    public void stopLocationUpdates() {
        this.lm.removeUpdates(this);
        started = false;
    }


    public LatLng getCurrentPosition() {
        if (!this.isEnabled() || this.current == null) {
            Criteria criteria = new Criteria();
            String bestProvider = this.getBestProvider();
            Location lastKnownLocation = null;

            try {
                if (ContextCompat.checkSelfPermission(this.main.get(),
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    lastKnownLocation = this.lm.getLastKnownLocation(bestProvider);
                }
            } catch (NullPointerException n) {
                Log.w(TAG, "Request for location before handling it.");
            }

            return (lastKnownLocation == null) ? null :
                    new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
        }
        return new LatLng(this.current.getLatitude(), this.current.getLongitude());
    }

    public int getCurrentSpeed() {
        return this.speed;
    }

    @Override
    public void onLocationChanged(Location location) {

        if (this.current != null) { // compute speed
            long timeDiff = location.getElapsedRealtimeNanos() - this.current.getElapsedRealtimeNanos();
            float distance = Math.abs(location.distanceTo(this.current));
            speed = (int) ((distance * 1000000) / timeDiff);
        }

        if (location.hasSpeed() && location.getSpeed() > 0) speed = (int) location.getSpeed();


        this.current = location;
        this.current.setSpeed(speed);

        this.setChanged();
        this.notifyObservers(this.current);
        if (listener != null) listener.onLocationChanged(this.current);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        if (listener != null) listener.onStatusChanged(provider, status, extras);
    }

    @Override
    public void onProviderEnabled(String provider) {
        if ((started) && (!this.provider.equals(provider)) && (provider.equals(getBestProvider()))) {
            this.stopLocationUpdates();
            this.startLocationUpdates(provider, minTime, minDistance);
        }
        if (listener != null) listener.onProviderEnabled(provider);
    }

    @Override
    public void onProviderDisabled(String provider) {
        if (started) {
            if (this.provider.equals(provider)) {
                this.stopLocationUpdates();
            }
            this.startLocationUpdates(minTime, minDistance);
        }

        if (listener != null) listener.onProviderDisabled(provider);
    }
}
