package org.nevadabike.renotracks;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class RecordingFragment extends Fragment {

	private Intent recordingService;
	private ServiceConnection recordingServiceConnection;
	private RecordingService recordingServiceInterface;
	private BroadcastReceiver recordingBroadcastReceiver;
	private BroadcastReceiver locationBroadcastReceiver;

	private ImageButton startButton;
	//private ImageButton resumeButton;
	//private ImageButton pauseButton;
	private ImageButton stopButton;
	private OnClickListener clickListener;

	private View view;
	private GoogleMap map;
	private FragmentActivity activity;

	private final LatLng mapCenter = new LatLng(39.505804, -119.789043);
	private SupportMapFragment mapFragment;
	private UiSettings mapUiSettings;
	private TripData trip;

	final SimpleDateFormat sdf = new SimpleDateFormat("H:mm:ss");
	private ImageButton markButton;
	private BitmapDescriptor myLocationMarkerIcon;

	Timer uiUpdateTimer;
    final Handler uiUpdateHandler = new Handler();
    final Runnable uiUpdateRunnable = new Runnable()
    {
        public void run()
        {
        	updateUI();
        }
    };
	private TextView elapsed_display;
	private TextView miles_display;
	private TextView mph_display;
	private TextView co2_display;
	private TextView calories_display;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		view = inflater.inflate(R.layout.recording_fragment, container, false);

		activity = getActivity();
		mapFragment = (SupportMapFragment) getFragmentManager().findFragmentById(R.id.map);

		map = mapFragment.getMap();
		map.moveCamera(CameraUpdateFactory.newLatLngZoom(mapCenter, 11));

		myLocationMarkerIcon = BitmapDescriptorFactory.fromResource(R.drawable.trip_start);

		//Disable most interaction with the map
		mapUiSettings = map.getUiSettings();
		mapUiSettings.setAllGesturesEnabled(false);
		mapUiSettings.setZoomControlsEnabled(false);

		recordingService = new Intent(activity, RecordingService.class);
		recordingServiceConnection = new ServiceConnection() {
			@Override
			public void onServiceConnected(ComponentName name, IBinder binder) {
				Log.i(getClass().getName(), "onServiceConnected");

				recordingServiceInterface = ((RecordingService.RecordingServiceBinder) binder).getService();

				updateUI();
				updateMap();
			}

			@Override
			public void onServiceDisconnected(ComponentName name) {
				Log.i(getClass().getName(), "onServiceConnected");
			}
		};

		recordingBroadcastReceiver = new BroadcastReceiver(){
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				Log.i(getClass().getName(), action);
				updateUI();
			}
		};

		locationBroadcastReceiver = new BroadcastReceiver(){
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				Log.i(getClass().getName(), action);
				updateMap();
			}
		};

		clickListener = new OnClickListener(){
			@Override
			public void onClick(View view) {
				switch(view.getId()) {
					case R.id.start_recording:
						startRecording();
						break;
					//case R.id.resume_recording:
					//	resumeRecording();
					//	break;
					//case R.id.pause_recording:
					//	pauseRecording();
					//	break;
					case R.id.stop_recording:
						stopRecording();
						break;
					case R.id.mark_button:
						makeMark();
						break;
				}
				updateUI();
			}
		};

		startButton = (ImageButton) view.findViewById(R.id.start_recording);
		startButton.setOnClickListener(clickListener);

		//resumeButton = (ImageButton) view.findViewById(R.id.resume_recording);
		//resumeButton.setOnClickListener(clickListener);

		//pauseButton = (ImageButton) view.findViewById(R.id.pause_recording);
		//pauseButton.setOnClickListener(clickListener);

		stopButton = (ImageButton) view.findViewById(R.id.stop_recording);
		stopButton.setOnClickListener(clickListener);

		markButton = (ImageButton) view.findViewById(R.id.mark_button);
		markButton.setOnClickListener(clickListener);

		elapsed_display = (TextView) view.findViewById(R.id.elapsed_display);
		miles_display = (TextView) view.findViewById(R.id.miles_display);
		mph_display = (TextView) view.findViewById(R.id.mph_display);
		co2_display = (TextView) view.findViewById(R.id.co2_display);
		calories_display = (TextView) view.findViewById(R.id.calories_display);

		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

		return view;
	}


	private Location myLocation;
	private LatLng myLocationLatLng;
	private Marker myLocationMarker;
	protected void updateMap() {
		myLocation = recordingServiceInterface.getLastLocation();

		if (myLocation != null) {
			if (myLocationMarker != null) myLocationMarker.remove();

			myLocationLatLng = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
			myLocationMarker = map.addMarker(new MarkerOptions().position(myLocationLatLng).icon(myLocationMarkerIcon).anchor(.5f,.5f));

			map.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocationLatLng, 15));
		}
	}

	protected void makeMark() {
		if (myLocation != null) {
			Intent markActivity = new Intent(activity, SaveMarkActivity.class);
			Bundle myLocationBundle = new Bundle();
			myLocationBundle.putParcelable("position", myLocation);
			markActivity.putExtras(myLocationBundle);
			activity.startActivity(markActivity);
		}
	}

	private void registerService() {
		activity.registerReceiver(recordingBroadcastReceiver, new IntentFilter(RecordingService.SERVICE_BROADCAST_ACTION_START));
		activity.registerReceiver(recordingBroadcastReceiver, new IntentFilter(RecordingService.SERVICE_BROADCAST_ACTION_PAUSE));
		activity.registerReceiver(locationBroadcastReceiver, new IntentFilter(RecordingService.BROADCAST_ACTION_LOCATION_CHANGED));
	}

	private void unregisterService() {
		activity.unregisterReceiver(recordingBroadcastReceiver);
		activity.unregisterReceiver(locationBroadcastReceiver);
	}

	private void updateUI() {
		Log.i(getClass().getName(), "updateUI");
		Log.i(getClass().getName(), String.valueOf(recordingServiceInterface.recordingState()));

		if (recordingServiceInterface != null)
		{
			startButton.setVisibility(recordingServiceInterface.recordingState() == RecordingService.STATE_STOPPED ? View.VISIBLE : View.GONE);
			//resumeButton.setVisibility(recordingServiceInterface.recordingState() == RecordingService.STATE_PAUSED ? View.VISIBLE : View.GONE);
			//pauseButton.setVisibility(recordingServiceInterface.recordingState() == RecordingService.STATE_RECORDING ? View.VISIBLE : View.GONE);
			stopButton.setVisibility(recordingServiceInterface.recordingState() != RecordingService.STATE_STOPPED ? View.VISIBLE : View.GONE);

			int total_seconds = 0;
			float distance = 0;
			if (recordingServiceInterface.recordingState() != RecordingService.STATE_STOPPED)
			{
				total_seconds = Math.round((System.currentTimeMillis() - recordingServiceInterface.startTime)/1000);
				distance = recordingServiceInterface.distanceTraveled;
			}

	        int seconds = total_seconds;
			int minutes = (int) Math.floor(seconds/60);
			seconds -= (minutes * 60);
			int hours = (int) Math.floor(minutes/60);
			minutes -= (hours * 60);

			double total_miles =  Common.METER_TO_MILE * distance;

			double speed = 0;
			if (total_seconds > 0)
			{
				speed = total_miles / total_seconds * (60 * 60);
			}

			double calories = Common.distanceToCals(distance);
			calories = Math.max(calories, 0);

			double co2 = Common.distanceToCO2(distance);

			elapsed_display.setText(String.format("%1$02d:%2$02d:%3$02d", hours, minutes, seconds));
			miles_display.setText(String.format(" %1.1f", total_miles));
			mph_display.setText(String.format("%1.1f", speed));
			co2_display.setText(String.format("%.1f", co2));
			calories_display.setText(String.format("%.1f", calories));
		}
	}

	private void startRecording() {
		recordingServiceInterface.startRecording();
		//registerService();
	}

	/*
	private void pauseRecording() {
		recordingServiceInterface.pauseRecording();
	}

	private void resumeRecording() {
		recordingServiceInterface.resumeRecording();
	}
	*/

	private void stopRecording()
	{
		//recordingServiceInterface.stopRecording();

		Intent finishedActivity = new Intent(activity, SaveTripActivity.class);
		startActivity(finishedActivity);
	}

	@Override
	public void onStart() {
		super.onStart();
		Log.i(getClass().getName(), "onStart");

		activity.startService(recordingService);

		registerService();
		activity.bindService(recordingService, recordingServiceConnection, Context.BIND_AUTO_CREATE);

		setupTimer();
	}

	@Override
	public void onStop() {
		Log.i(getClass().getName(), "onStop");
		super.onStop();

		if (recordingServiceInterface.recordingState() == RecordingService.STATE_STOPPED) {
			activity.stopService(recordingService);
		}

		unregisterService();
		activity.unbindService(recordingServiceConnection);

		cancelTimer();
	}

	public void clearMap() {
		if (activity != null && mapFragment != null) {
			activity.getSupportFragmentManager().beginTransaction().remove(mapFragment).commit();
		}
	}

	public void setupTimer()
	{
		cancelTimer();

    	uiUpdateTimer = new Timer();
    	uiUpdateTimer.schedule (new TimerTask()
        {
            @Override public void run()
            {
            	uiUpdateHandler.post(uiUpdateRunnable);
            }
        }, RecordingService.NOTIFICATION_UPDATE_INTERVAL, RecordingService.NOTIFICATION_UPDATE_INTERVAL);
	}

	private void cancelTimer()
	{
		if (uiUpdateTimer != null)
		{
			uiUpdateTimer.cancel();
			uiUpdateTimer.purge();
		}
	}
}

/*
public class RecordingFragment extends Activity {
	Intent fi;
	TripData trip;
	boolean isRecording = false;
	Button pauseButton;
	Button finishButton;
	Timer timer;
	float curDistance;

	TextView txtStat;
	TextView txtDistance;
	TextView txtDuration;
	TextView txtCurSpeed;
	TextView txtMaxSpeed;
	TextView txtAvgSpeed;

	// Need handler for callbacks to the UI thread
	final Handler mHandler = new Handler();
	final Runnable mUpdateTimer = new Runnable() {
		public void run() {
			updateTimer();
		}
	};

	Drawable pauseDrawable;
	Drawable recordDrawable;
	String pausedTitle;
	String recordingTitle;

	String pause;
	String resume;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.recording);

		txtStat =	 (TextView) findViewById(R.id.TextRecordStats);
		txtDistance = (TextView) findViewById(R.id.TextDistance);
		txtDuration = (TextView) findViewById(R.id.TextDuration);
		txtCurSpeed = (TextView) findViewById(R.id.TextSpeed);
		txtMaxSpeed = (TextView) findViewById(R.id.TextMaxSpeed);
		txtAvgSpeed = (TextView) findViewById(R.id.TextAvgSpeed);

		pauseDrawable = getResources().getDrawable(R.drawable.pause);
		recordDrawable = getResources().getDrawable(R.drawable.record);

		pausedTitle = getResources().getString(R.string.paused_title);
		recordingTitle = getResources().getString(R.string.recording_title);

		pauseButton = (Button) findViewById(R.id.ButtonRecordPause);
		finishButton = (Button) findViewById(R.id.ButtonFinished);

		pause = getResources().getString(R.string.pause);
		resume = getResources().getString(R.string.resume);

		// Query the RecordingService to figure out what to do.
		Intent rService = new Intent(this, RecordingService.class);
		startService(rService);
		ServiceConnection sc = new ServiceConnection() {
			public void onServiceDisconnected(ComponentName name) {}
			public void onServiceConnected(ComponentName name, IBinder service) {
				IRecordService rs = (IRecordService) service;

				switch (rs.getState()) {

				}
				rs.setListener(RecordingFragment.this);
				unbindService(this);
			}
		};
		bindService(rService, sc, Context.BIND_AUTO_CREATE);

		// Pause button
		pauseButton.setEnabled(false);
		pauseButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				isRecording = !isRecording;
				if (isRecording) {
					pauseButton.setText(pause);
					pauseButton.setCompoundDrawablesWithIntrinsicBounds(pauseDrawable, null, null, null);
					setTitle(recordingTitle);
					// Don't include pause time in trip duration
					if (trip.pauseStartedAt > 0) {
						trip.totalPauseTime += (System.currentTimeMillis() - trip.pauseStartedAt);
						trip.pauseStartedAt = 0;
					}
					Toast.makeText(getBaseContext(),getResources().getString(R.string.gps_restarted), Toast.LENGTH_LONG).show();
				} else {
					pauseButton.setText(resume);
					pauseButton.setCompoundDrawablesWithIntrinsicBounds(recordDrawable, null, null, null);
					setTitle(pausedTitle);
					trip.pauseStartedAt = System.currentTimeMillis();
					Toast.makeText(getBaseContext(),getResources().getString(R.string.recording_paused), Toast.LENGTH_LONG).show();
				}
				setListener();
			}
		});
	}

	public void updateStatus(int points, float distance, float spdCurrent, float spdMax) {
		this.curDistance = distance;

		//TODO: check task status before doing this?
		if (points>0) {
			txtStat.setText(points + getResources().getString(R.string.data_points_received));
		} else {
			txtStat.setText(getResources().getString(R.string.waiting_gps_fix));
		}
		txtCurSpeed.setText(String.format("%1.1f mph", spdCurrent));
		txtMaxSpeed.setText(String.format("%1.1f mph", spdMax));

		float miles = 0.0006212f * distance;
		txtDistance.setText(String.format("%1.1f miles", miles));
	}

	void setListener() {
		Intent rService = new Intent(this, RecordingService.class);
		ServiceConnection sc = new ServiceConnection() {
			public void onServiceDisconnected(ComponentName name) {}
			public void onServiceConnected(ComponentName name, IBinder service) {
				IRecordService rs = (IRecordService) service;
				if (isRecording) {
					rs.resumeRecording();
				} else {
					rs.pauseRecording();
				}
				unbindService(this);
			}
		};
		// This should block until the onServiceConnected (above) completes, but doesn't
		bindService(rService, sc, Context.BIND_AUTO_CREATE);
	}

	// onResume is called whenever this activity comes to foreground.
	// Use a timer to update the trip duration.
	@Override
	public void onResume() {
		super.onResume();

		timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				mHandler.post(mUpdateTimer);
			}
		}, 0, 1000);  // every second
	}

	void updateTimer() {
		if (trip != null && isRecording) {
			double dd = System.currentTimeMillis()
						- trip.startTime
						- trip.totalPauseTime;

			txtDuration.setText(sdf.format(dd));

			double avgSpeed = 3600.0 * 0.6212 * this.curDistance / dd;
			txtAvgSpeed.setText(String.format("%1.1f mph", avgSpeed));
		}
	}

	// Don't do pointless UI updates if the activity isn't being shown.
	@Override
	public void onPause() {
		super.onPause();
		if (timer != null) timer.cancel();
	}

	private void buildAlertMessageNoGps() {
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(getResources().getString(R.string.gps_disabled))
			   .setCancelable(false)
			   .setPositiveButton(getResources().getString(R.string.gps_settings), new DialogInterface.OnClickListener() {
				   public void onClick(final DialogInterface dialog, final int id) {
					   final ComponentName toLaunch = new ComponentName("com.android.settings","com.android.settings.SecuritySettings");
					   final Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
					   intent.addCategory(Intent.CATEGORY_LAUNCHER);
					   intent.setComponent(toLaunch);
					   intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					   startActivityForResult(intent, 0);
				   }
			   })
			   .setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
				   public void onClick(final DialogInterface dialog, final int id) {
						dialog.cancel();
				   }
			   });
		final AlertDialog alert = builder.create();
		alert.show();
	}
}
*/