package org.github.fufighters;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

public class GPSManager {
	
	public static int STATUS_NO_GPS = 0; // the device does not have gps
	public static int STATUS_GPS_DISABLED = 1; // the gps has been disabled by the system
	public static int STATUS_GPS_STOPPED = 2; // the gps is not currently active, call startGps()
	public static int STATUS_GPS_NO_FIX = 3;
	public static int STATUS_GPS_FIX = 4;
	private static long MIN_UPDATE_TIME_MILLIS = 10l * 1000l; // 10 seconds
	private static float MIN_UPDATE_DISTANCE_METERS = 1f; // 1 meter
	
	private LocationManager locationManager;
	private LocationListener locationListener;
	private Location locationCache;
	private int status;
	
	private synchronized void updateStatus(int newStatus) {
		status = newStatus;
	}
	
	private synchronized void updateLocationCache(Location newLocation) {
		locationCache = newLocation;
	}
	
	private synchronized Location getLocationCache() {
		return locationCache;
	}
	
	public synchronized int getStatus() {
		return status;
	}
	
	public double getAltitude() {
		return getLocationCache().getAltitude();
	}
	
	public void stopGPS() {
		// stop receiving updates
		locationManager.removeUpdates(locationListener);
		// update the status
		updateStatus(STATUS_GPS_STOPPED);
	}
	
	public boolean startGPS() {
		// check if the device has gps
		if(locationManager.getProvider(LocationManager.GPS_PROVIDER) == null) {
			// no gps, set the status and then bail
			updateStatus(STATUS_NO_GPS);
			return false; // we could not enable gps
		}
		// check if the gps is enabled
		if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			// the provider is disabled, update the status and then bail
			updateStatus(STATUS_GPS_DISABLED);
			return false; // we could not enable the gps
		}
		
		// add a listener to receive updates from the gps
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_UPDATE_TIME_MILLIS, MIN_UPDATE_DISTANCE_METERS,
				locationListener);
		
		return true; // we didn't have any problems, probably
	}
	
	public GPSManager(Context context) {
		// initialize variables
		locationManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
		// define a listener that listens for location updates
		locationListener = new LocationListener() {
			public void onStatusChanged(String provider, int status, Bundle extras) {
				// we don't need to do anything, status updates generated by this are handled elsewhere
			}
			
			public void onProviderEnabled(String provider) {
				// only listen to stuff about gps
				if(provider == LocationManager.GPS_PROVIDER){
					// if the status currently says that the gps is disabled, change it to stopped
					if(getStatus() == STATUS_GPS_DISABLED) {
						updateStatus(STATUS_GPS_STOPPED);
					}
				}
			}
			
			public void onProviderDisabled(String provider) {
				// only listen to stuff about gps
				if(provider == LocationManager.GPS_PROVIDER){
					// stop the gps, probably unneeded
					stopGPS();
					// update the status
					updateStatus(STATUS_GPS_DISABLED);
				}
			}
			
			public void onLocationChanged(Location location) {
				// test if the fix is useful
				if(location.hasAltitude()) {
					// store the new location
					updateLocationCache(location);
					// update the status, we have a fix
					updateStatus(STATUS_GPS_FIX);
				} else {
					// the gps is still trying to get a fix (or so we will pretend, we need altitude)
					updateStatus(STATUS_GPS_NO_FIX);
				}
			}
		};
	}
}
