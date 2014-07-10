package org.nevadabike.renotracks;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TimeZone;

import org.nevadabike.renotracks.IconSpinnerAdapter.IconItem;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class SaveTripActivity extends Activity {
	Activity activity;
	TripData trip;

	private int selected_purpose_id = 0;
	private TextView purpose_description;
	private final ArrayList<IconItem> tripPurposes = new ArrayList<IconSpinnerAdapter.IconItem>();
	private final HashMap <Integer, String> purpDescriptions = new HashMap<Integer, String>();

	private Spinner tripPurposeSpinner;

	private Button prefsButton;
	private LinearLayout prefsButtonContainer;
	private Button btnSubmit;
	private Button btnDiscard;
	private LinearLayout tripButtons;

	private Intent recordingService;
	private ServiceConnection recordingServiceConnection;
	private RecordingService recordingServiceInterface;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.save);

		this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		recordingService = new Intent(this, RecordingService.class);
		recordingServiceConnection = new ServiceConnection() {
			@Override
			public void onServiceConnected(ComponentName name, IBinder binder) {
				Log.i(getClass().getName(), "onServiceConnected");

				recordingServiceInterface = ((RecordingService.RecordingServiceBinder) binder).getService();
				if (recordingServiceInterface.recordingState() != RecordingService.STATE_STOPPED) {
					recordingServiceInterface.stopRecording();
				}

				trip = recordingServiceInterface.getCurrentTrip();

				if (trip.getPoints().size() == 0) {
					discardTrip(true);
				}
			}

			@Override
			public void onServiceDisconnected(ComponentName name) {
				Log.i(getClass().getName(), "onServiceConnected");
			}
		};

		activity = this;

		// Set up trip purpose buttons
		preparePurposeButtons();

		prefsButton = (Button) findViewById(R.id.ButtonPrefs);
		prefsButtonContainer = (LinearLayout) findViewById(R.id.user_info_button);
		purpose_description = (TextView) findViewById(R.id.TextPurpDescription);
		tripButtons = (LinearLayout) findViewById(R.id.trip_buttons);
		btnSubmit = (Button) findViewById(R.id.ButtonSubmit);
		btnDiscard = (Button) findViewById(R.id.ButtonDiscard);

		//if the users has not yet entered their profile information, require them to do so now
		SharedPreferences settings = getSharedPreferences("PREFS", 0);
		if (settings.getAll().size() >= 1) {
			prefsButtonContainer.setVisibility(View.GONE);

			btnDiscard.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					discardTrip();
				}
			});

			btnSubmit.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					saveTrip();
				}
			});
		} else {
			tripButtons.setVisibility(View.GONE);

			prefsButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					startActivity(new Intent(activity, UserInfoActivity.class));
				}
			});
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		Log.i(getClass().getName(), "onPause");
		unbindService(recordingServiceConnection);
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.i(getClass().getName(), "onResume");
		bindService(recordingService, recordingServiceConnection, Context.BIND_AUTO_CREATE);
	}

	// Set up the purpose buttons to be one-click only
	void preparePurposeButtons() {
		tripPurposes.add(new IconSpinnerAdapter.IconItem(null, "", 0));
		tripPurposes.add(new IconSpinnerAdapter.IconItem(getResources().getDrawable(R.drawable.commute), getResources().getString(R.string.trip_purpose_commute), R.string.trip_purpose_commute));
		tripPurposes.add(new IconSpinnerAdapter.IconItem(getResources().getDrawable(R.drawable.school), getResources().getString(R.string.trip_purpose_school), R.string.trip_purpose_school));
		tripPurposes.add(new IconSpinnerAdapter.IconItem(getResources().getDrawable(R.drawable.work_related), getResources().getString(R.string.trip_purpose_work_rel), R.string.trip_purpose_work_rel));
		tripPurposes.add(new IconSpinnerAdapter.IconItem(getResources().getDrawable(R.drawable.exercise), getResources().getString(R.string.trip_purpose_exercise), R.string.trip_purpose_exercise));
		tripPurposes.add(new IconSpinnerAdapter.IconItem(getResources().getDrawable(R.drawable.social), getResources().getString(R.string.trip_purpose_social), R.string.trip_purpose_social));
		tripPurposes.add(new IconSpinnerAdapter.IconItem(getResources().getDrawable(R.drawable.shopping), getResources().getString(R.string.trip_purpose_shopping), R.string.trip_purpose_shopping));
		tripPurposes.add(new IconSpinnerAdapter.IconItem(getResources().getDrawable(R.drawable.errands), getResources().getString(R.string.trip_purpose_errand), R.string.trip_purpose_errand));
		tripPurposes.add(new IconSpinnerAdapter.IconItem(getResources().getDrawable(R.drawable.bike_event), getResources().getString(R.string.trip_purpose_bike_event), R.string.trip_purpose_bike_event));
		tripPurposes.add(new IconSpinnerAdapter.IconItem(getResources().getDrawable(R.drawable.scalley_cat), getResources().getString(R.string.trip_purpose_scalley_cat), R.string.trip_purpose_scalley_cat));
		tripPurposes.add(new IconSpinnerAdapter.IconItem(getResources().getDrawable(R.drawable.other), getResources().getString(R.string.trip_purpose_other), R.string.trip_purpose_other));

		purpDescriptions.put(0, getResources().getString(R.string.select_trip_purpose));
		purpDescriptions.put(R.string.trip_purpose_commute, getResources().getString(R.string.trip_purpose_commute_details));
		purpDescriptions.put(R.string.trip_purpose_school, getResources().getString(R.string.trip_purpose_school_details));
		purpDescriptions.put(R.string.trip_purpose_work_rel, getResources().getString(R.string.trip_purpose_work_rel_details));
		purpDescriptions.put(R.string.trip_purpose_exercise, getResources().getString(R.string.trip_purpose_exercise_details));
		purpDescriptions.put(R.string.trip_purpose_social, getResources().getString(R.string.trip_purpose_social_details));
		purpDescriptions.put(R.string.trip_purpose_shopping, getResources().getString(R.string.trip_purpose_shopping_details));
		purpDescriptions.put(R.string.trip_purpose_errand, getResources().getString(R.string.trip_purpose_errand_details));
		purpDescriptions.put(R.string.trip_purpose_bike_event, getResources().getString(R.string.trip_purpose_bike_event_details));
		purpDescriptions.put(R.string.trip_purpose_scalley_cat, getResources().getString(R.string.trip_purpose_scalley_cat_details));
		purpDescriptions.put(R.string.trip_purpose_other, getResources().getString(R.string.trip_purpose_other_details));

		tripPurposeSpinner = (Spinner) findViewById(R.id.tripPurposeSpinner);
		tripPurposeSpinner.setAdapter(new IconSpinnerAdapter(this, tripPurposes));
		tripPurposeSpinner.setOnItemSelectedListener(new OnItemSelectedListener(){
			@Override
			public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
				IconSpinnerAdapter.IconItem selected_purpose = tripPurposes.get(position);
				selected_purpose_id = selected_purpose.id;
				purpose_description.setText(Html.fromHtml(purpDescriptions.get(selected_purpose_id)));
			}

			@Override
			public void onNothingSelected(AdapterView<?> parentView) {
			}
		});
	}

	private void saveTrip() {
		trip.populateDetails();

		if (selected_purpose_id == 0) {
			// Oh no!  No trip purpose!
			Toast.makeText(getBaseContext(), getResources().getString(R.string.select_purpose), Toast.LENGTH_SHORT).show();
			return;
		}

		String fancyStartTime = DateFormat.getInstance().format(trip.startTime);

		// "3.5 miles in 26 minutes"
		SimpleDateFormat sdf = new SimpleDateFormat("m");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

		String minutes = sdf.format(trip.endTime - trip.startTime);
		String fancyEndInfo = String.format("%1.1f miles, %s minutes.", (0.0006212f * trip.distance), minutes);

		// Save the trip details to the phone database.
		trip.updateTrip(getResources().getString(selected_purpose_id), fancyStartTime, fancyEndInfo);
		trip.updateTripStatus(TripData.STATUS_COMPLETE);

		// TODO Upload the trip

		// And, show the map!
		Intent showTrip = new Intent(this, ShowMap.class);
		showTrip.putExtra("showtrip", trip.tripid);
		startActivity(showTrip);
		finish();
	}

	private void discardTrip() {
		discardTrip(false);
	}

	private void discardTrip(Boolean too_short) {
		Toast.makeText(getBaseContext(), getResources().getString(
			too_short ? R.string.no_gps_data : R.string.discarded
		), Toast.LENGTH_SHORT).show();

		trip.dropTrip();

		Intent discardTrip = new Intent(this, MainActivity.class);
		startActivity(discardTrip);
		finish();
	}
}
