package ua.cm.sensingtheenvironment;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

// CREDITS http://stackoverflow.com/a/28535885
public class GPSService extends Service {
    private static final String TAG = "SenseGPS";
    public static final String GPS_LAST_LOCATION = "gps_last" ;
    public static final String GPS_LATITUDE = "gps_lat";
    public static final String GPS_LONGITUDE = "gps_lng";
    public static final String GPS_LOCATION = "gps_loc";

    private static final int GPS = 0;
    private static final int NETWORK = 1;
    private LocationManager mLocationManager = null;
    private static final int LOCATION_INTERVAL = 1000;
    private static final float LOCATION_DISTANCE = 0;

    private class LocationListener implements android.location.LocationListener
    {
        Location mLastLocation;
        Boolean enabled = true;

        public LocationListener(String provider)
        {
            Log.e(TAG, "LocationListener " + provider);
            mLastLocation = new Location(provider);
        }

        @Override
        public void onLocationChanged(Location location)
        {
            Log.e(TAG, "onLocationChanged: " + location);
            GPSService.this.reportLocation();
            mLastLocation.set(location);
        }

        @Override
        public void onProviderDisabled(String provider)
        {
            enabled = false;
            Log.e(TAG, "onProviderDisabled: " + provider);
        }

        @Override
        public void onProviderEnabled(String provider)
        {
            enabled = true;
            Log.e(TAG, "onProviderEnabled: " + provider);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras)
        {
            Log.e(TAG, "onStatusChanged: " + provider);
        }
    }

    LocationListener[] mLocationListeners = new LocationListener[] {
            new LocationListener(LocationManager.GPS_PROVIDER),
            new LocationListener(LocationManager.NETWORK_PROVIDER)
    };

    public GPSService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.e(TAG, "onStartCommand");
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onCreate()
    {
        Log.e(TAG, "onCreate");
        initializeLocationManager();
        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    mLocationListeners[NETWORK]);
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "network provider does not exist, " + ex.getMessage());
        }
        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    mLocationListeners[GPS]);
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "gps provider does not exist " + ex.getMessage());
        }
    }

    @Override
    public void onDestroy()
    {
        Log.e(TAG, "onDestroy");
        super.onDestroy();
        if (mLocationManager != null) {
            for (int i = 0; i < mLocationListeners.length; i++) {
                try {
                    mLocationManager.removeUpdates(mLocationListeners[i]);
                } catch (Exception ex) {
                    Log.i(TAG, "fail to remove location listners, ignore", ex);
                }
            }
        }
    }

    private void initializeLocationManager() {
        Log.e(TAG, "initializeLocationManager");
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }
    }
    private void reportLocation() {
        Intent intent = new Intent(GPS_LAST_LOCATION);
        // You can also include some extra data.
        Bundle b = new Bundle();
        if(mLocationListeners[GPS].enabled) {
            b.putDouble(GPS_LATITUDE, mLocationListeners[GPS].mLastLocation.getLatitude());
            b.putDouble(GPS_LONGITUDE, mLocationListeners[GPS].mLastLocation.getLongitude());
        }else if(mLocationListeners[NETWORK].enabled){
            b.putDouble(GPS_LATITUDE, mLocationListeners[NETWORK].mLastLocation.getLatitude());
            b.putDouble(GPS_LONGITUDE, mLocationListeners[NETWORK].mLastLocation.getLongitude());
        }else
        {
            b.putDouble(GPS_LATITUDE, 0);
            b.putDouble(GPS_LONGITUDE, 0);
        }
        intent.putExtra(GPS_LOCATION, b);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }
}
