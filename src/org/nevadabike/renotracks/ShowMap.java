package org.nevadabike.renotracks;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Point;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

public class ShowMap extends Activity {
	private GoogleMap map;
	private ArrayList<CyclePoint> gpspoints;
	private TripData trip;
	private AlertDialog infoDialog;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.show_map);

        Bundle cmds = getIntent().getExtras();

        if (cmds == null) {
        	//We have no trip to show, do nothing
        	return;
        }

        trip = TripData.fetchTrip(this, cmds.getLong("showtrip"));

        gpspoints = trip.getPoints();

        //Upload the trip if it hasn't yet been sent
        if (trip.status < TripData.STATUS_SENT || cmds.getBoolean("uploadTrip", false)) {
    	    // And upload to the cloud database, too!  W00t W00t!
           TripUploader uploader = new TripUploader(ShowMap.this);
           uploader.execute(trip.tripid);
    	}

        // Show trip details
        setTitle(trip.purp);
        //((TextView) findViewById(R.id.text2)).setText(trip.info);
        //((TextView) findViewById(R.id.text3)).setText(trip.fancystart);

        //Set up the map
        GoogleMap map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();

        CyclePoint startPoint = null;
        CyclePoint endPoint = null;
        LatLngBounds.Builder tripBoundsBuilder = new LatLngBounds.Builder();

        PolylineOptions tripLine = new PolylineOptions().color(getResources().getColor(R.color.accent_color));

        for(CyclePoint cyclepoint : gpspoints) {
        	Log.i(getClass().getName(), cyclepoint.latLng.latitude + ", " + cyclepoint.latLng.longitude);
        	//Add point to boundary calculator
        	tripBoundsBuilder.include(cyclepoint.latLng);

        	//Add to the trip line
        	tripLine.add(cyclepoint.latLng);

        	if (startPoint == null) startPoint = cyclepoint;
        	endPoint = cyclepoint;
        }

        LatLngBounds tripBounds = tripBoundsBuilder.build();
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        int minSize = Math.min(size.x, size.y);
        Log.i(getClass().getName(), String.valueOf(minSize));

        //Zoome the camera so it shows the entire trip on the map
        map.moveCamera(CameraUpdateFactory.newLatLngBounds(tripBounds, minSize, minSize, 0));

        //Draw the trip on the map
        map.addPolyline(tripLine);

        //Show the first and last markers
        map.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.trip_start)).anchor(0.5f, 0.5f).position(startPoint.latLng));
        map.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.trip_end)).anchor(0.5f, 0.5f).position(endPoint.latLng));

        int total_seconds = (int) Math.round((trip.endTime - trip.startTime)/1000);
        int seconds = total_seconds;
		int minutes = (int) Math.floor(seconds/60);
		seconds -= (minutes * 60);
		int hours = (int) Math.floor(minutes/60);
		minutes -= (hours * 60);

		double total_miles = 0.0006212f * trip.distance;

		double speed = total_miles / total_seconds * (60 * 60);

		double calories = Common.distanceToCals(trip.distance);
		calories = Math.max(calories, 0);

		View infoView = getLayoutInflater().inflate(R.layout.show_info, null);

		TextView text1 = (TextView) infoView.findViewById(R.id.text1);
		infoView.findViewById(R.id.image_view).setVisibility(View.GONE);

		text1.setText(Html.fromHtml(TextUtils.join("<br>", new String[]{
			String.format("<b>" + getString(R.string.start_time) + "</b> %s", trip.fancystart),
			String.format("<b>" + getString(R.string.time_elapsed) + "</b> %1$02d:%2$02d:%3$02d", hours, minutes, seconds),
			String.format("<b>" + getString(R.string.distance) + "</b> %1.1f miles", total_miles),
			String.format("<b>" + getString(R.string.avg_speed) + "</b> %1.1f mph", speed),
			String.format("<b>" + getString(R.string.est_cal) + "</b> %.1f kcal", calories),
			String.format("<b>" + getString(R.string.c02_reduced) + "</b> %.1f lbs", Common.distanceToCO2(trip.distance)),
			String.format("<b>" + getString(R.string.notes) + "</b> %s", trip.note)
		})));

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setView(infoView);
		infoDialog = builder.create();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.show_map, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId())
		{
			case R.id.menu_info:
				infoDialog.show();
				return true;
		}
		return false;
	}
}
