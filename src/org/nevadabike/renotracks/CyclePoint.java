package org.nevadabike.renotracks;

import com.google.android.gms.maps.model.LatLng;


class CyclePoint {
	float accuracy;
	double altitude;
	float speed;
	double time;
	double latitude;
	double longitude;
	LatLng latLng;

    public CyclePoint(double latitude, double longitude, double currentTime) {
    	this.latitude = latitude;
    	this.longitude = longitude;
		this.time = currentTime;

		latLng = new LatLng(latitude, longitude);
    }

    public CyclePoint(double latitude, double longitude, double currentTime, float accuracy) {
    	this.latitude = latitude;
    	this.longitude = longitude;
        this.time = currentTime;
        this.accuracy = accuracy;
		this.time = currentTime;
		this.accuracy = accuracy;

		latLng = new LatLng(latitude, longitude);
    }

	public CyclePoint(double latitude, double longitude, double currentTime, float accuracy, double altitude, float speed) {
    	this.latitude = latitude;
    	this.longitude = longitude;
		this.time = currentTime;
		this.accuracy = accuracy;
		this.altitude = altitude;
		this.speed = speed;

		latLng = new LatLng(latitude, longitude);
	}

	@Override
	public String toString() {
		return time + ": " + latitude + ", " + longitude + " (" + accuracy + ")";
	}
}
