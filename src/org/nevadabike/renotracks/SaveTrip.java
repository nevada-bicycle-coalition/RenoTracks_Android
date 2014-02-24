package org.nevadabike.renotracks;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TimeZone;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Html;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class SaveTrip extends Activity {
	long tripid;
	HashMap <Integer, ToggleButton> purpButtons = new HashMap<Integer,ToggleButton>();
	String purpose = "";

	HashMap <Integer, String> purpDescriptions = new HashMap<Integer, String>();
	private String discarded;
	private String select_purpose;

	// Set up the purpose buttons to be one-click only
	void preparePurposeButtons() {
		purpButtons.put(R.id.ToggleCommute, (ToggleButton)findViewById(R.id.ToggleCommute));
		purpButtons.put(R.id.ToggleSchool,  (ToggleButton)findViewById(R.id.ToggleSchool));
		purpButtons.put(R.id.ToggleWorkRel, (ToggleButton)findViewById(R.id.ToggleWorkRel));
		purpButtons.put(R.id.ToggleExercise,(ToggleButton)findViewById(R.id.ToggleExercise));
		purpButtons.put(R.id.ToggleSocial,  (ToggleButton)findViewById(R.id.ToggleSocial));
		purpButtons.put(R.id.ToggleShopping,(ToggleButton)findViewById(R.id.ToggleShopping));
		purpButtons.put(R.id.ToggleErrand,  (ToggleButton)findViewById(R.id.ToggleErrand));
		purpButtons.put(R.id.ToggleOther,   (ToggleButton)findViewById(R.id.ToggleOther));

        purpDescriptions.put(R.id.ToggleCommute, getResources().getString(R.string.ToggleCommute));
		purpDescriptions.put(R.id.ToggleSchool, getResources().getString(R.string.ToggleSchool));
		purpDescriptions.put(R.id.ToggleWorkRel, getResources().getString(R.string.ToggleWorkRel));
		purpDescriptions.put(R.id.ToggleExercise, getResources().getString(R.string.ToggleExercise));
		purpDescriptions.put(R.id.ToggleSocial, getResources().getString(R.string.ToggleSocial));
		purpDescriptions.put(R.id.ToggleShopping, getResources().getString(R.string.ToggleShopping));
		purpDescriptions.put(R.id.ToggleErrand, getResources().getString(R.string.ToggleErrand));
		purpDescriptions.put(R.id.ToggleOther, getResources().getString(R.string.ToggleOther));

		CheckListener cl = new CheckListener();
		for (Entry<Integer, ToggleButton> e: purpButtons.entrySet()) {
			e.getValue().setOnCheckedChangeListener(cl);
		}
	}

	// Called every time a purp togglebutton is changed:
	class CheckListener implements CompoundButton.OnCheckedChangeListener {
		@Override
		public void onCheckedChanged(CompoundButton v, boolean isChecked) {
			// First, uncheck all purp buttons
			if (isChecked) {
				for (Entry<Integer, ToggleButton> e: purpButtons.entrySet()) {
					e.getValue().setChecked(false);
				}
				v.setChecked(true);
				purpose = v.getText().toString();
				((TextView) findViewById(R.id.TextPurpDescription)).setText(
				        Html.fromHtml(purpDescriptions.get(v.getId())));
			}
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.save);

		finishRecording();

		// Set up trip purpose buttons
		purpose = "";
		preparePurposeButtons();

        // User prefs btn
        final Button prefsButton = (Button) findViewById(R.id.ButtonPrefs);
        final Intent pi = new Intent(this, UserInfoActivity.class);
        prefsButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(pi);
            }
        });

        SharedPreferences settings = getSharedPreferences("PREFS", 0);
        if (settings.getAll().size() >= 1) {
            prefsButton.setVisibility(View.GONE);
        }

        discarded = getResources().getString(R.string.discarded);
        select_purpose = getResources().getString(R.string.select_purpose);

		// Discard btn
		final Button btnDiscard = (Button) findViewById(R.id.ButtonDiscard);
		btnDiscard.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Toast.makeText(getBaseContext(), discarded, Toast.LENGTH_SHORT).show();

				cancelRecording();

				Intent i = new Intent(SaveTrip.this, MainInput.class);
				i.putExtra("keepme", true);
				startActivity(i);
				SaveTrip.this.finish();
			}
		});

		// Submit btn
		final Button btnSubmit = (Button) findViewById(R.id.ButtonSubmit);
		btnSubmit.setEnabled(false);

		// Don't pop up the soft keyboard until user clicks!
		this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
	}

	// submit btn is only activated after the service.finishedRecording() is completed.
	void activateSubmitButton() {
		final Button btnSubmit = (Button) findViewById(R.id.ButtonSubmit);
		final Intent xi = new Intent(this, ShowMap.class);
		btnSubmit.setEnabled(true);

		btnSubmit.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {

				TripData trip = TripData.fetchTrip(SaveTrip.this, tripid);
				trip.populateDetails();

				// Make sure trip purpose has been selected
				if (purpose.equals("")) {
					// Oh no!  No trip purpose!
					Toast.makeText(getBaseContext(), select_purpose, Toast.LENGTH_SHORT).show();
					return;
				}

				EditText notes = (EditText) findViewById(R.id.NotesField);

				String fancyStartTime = DateFormat.getInstance().format(trip.startTime);

				// "3.5 miles in 26 minutes"
				SimpleDateFormat sdf = new SimpleDateFormat("m");
				sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
				String minutes = sdf.format(trip.endTime - trip.startTime);
				String fancyEndInfo = String.format("%1.1f miles, %s minutes.  %s",
						(0.0006212f * trip.distance),
						minutes,
						notes.getEditableText().toString());

				// Save the trip details to the phone database. W00t!
				trip.updateTrip(
						purpose,
						fancyStartTime, fancyEndInfo,
						notes.getEditableText().toString());
				trip.updateTripStatus(TripData.STATUS_COMPLETE);
				resetService();

				// Force-drop the soft keyboard for performance
				InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

				// Now create the MainInput Activity so BACK btn works properly
				Intent i = new Intent(getApplicationContext(), MainInput.class);
				startActivity(i);

				// And, show the map!
                xi.putExtra("showtrip", trip.tripid);
                xi.putExtra("uploadTrip", true);
				startActivity(xi);
				SaveTrip.this.finish();
			}
		});

	}

	void cancelRecording() {
		Intent rService = new Intent(this, RecordingService.class);
		ServiceConnection sc = new ServiceConnection() {
			public void onServiceDisconnected(ComponentName name) {}
			public void onServiceConnected(ComponentName name, IBinder service) {
				IRecordService rs = (IRecordService) service;
				rs.cancelRecording();
				unbindService(this);
			}
		};
		// This should block until the onServiceConnected (above) completes.
		bindService(rService, sc, Context.BIND_AUTO_CREATE);
	}

	void resetService() {
		Intent rService = new Intent(this, RecordingService.class);
		ServiceConnection sc = new ServiceConnection() {
			public void onServiceDisconnected(ComponentName name) {}
			public void onServiceConnected(ComponentName name, IBinder service) {
				IRecordService rs = (IRecordService) service;
				rs.reset();
				unbindService(this);
			}
		};
		// This should block until the onServiceConnected (above) completes.
		bindService(rService, sc, Context.BIND_AUTO_CREATE);
	}

	void finishRecording() {
		Intent rService = new Intent(this, RecordingService.class);
		ServiceConnection sc = new ServiceConnection() {
			public void onServiceDisconnected(ComponentName name) {}
			public void onServiceConnected(ComponentName name, IBinder service) {
				IRecordService rs = (IRecordService) service;
				tripid = rs.finishRecording();
				SaveTrip.this.activateSubmitButton();
				unbindService(this);
			}
		};
		// This should block until the onServiceConnected (above) completes.
		bindService(rService, sc, Context.BIND_AUTO_CREATE);
	}
}
