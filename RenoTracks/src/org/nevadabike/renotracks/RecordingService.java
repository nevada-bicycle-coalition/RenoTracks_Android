package org.nevadabike.renotracks;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

public class RecordingService extends Service implements
	GooglePlayServicesClient.ConnectionCallbacks,
	GooglePlayServicesClient.OnConnectionFailedListener,
	LocationListener
{
	private final RecordingServiceBinder recordingServiceBinder = new RecordingServiceBinder();

	private RemoteViews notificationView;
	private Intent notificationIntent;
	private PendingIntent pendingNotificationIntent;
	private Builder notificationBuilder;
	private final static int NOTIFICATION_ID = 100;

	private BroadcastReceiver broadcastReceiver;

	public final static String BROADCAST_ACTION_START = "BROADCAST_ACTION_START";
	public final static String BROADCAST_ACTION_STOP = "BROADCAST_ACTION_STOP";
	public final static String BROADCAST_ACTION_PAUSE = "BROADCAST_ACTION_PAUSE";

	@Override
	public void onCreate() {
		notificationView = new RemoteViews(getPackageName(), R.layout.customnotification);
		notificationView.setImageViewResource(R.id.notification_icon, R.drawable.ic_launcher);

		broadcastReceiver = new BroadcastReceiver(){
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				Log.i(getClass().getName(), action);
				if (action == BROADCAST_ACTION_START) {
					resumeRecording();
				} else if (action == BROADCAST_ACTION_STOP) {
					stopRecording();
				} else if (action == BROADCAST_ACTION_PAUSE) {
					pauseRecording();
				}
			}
		};

		Intent startIntent = new Intent(BROADCAST_ACTION_START);
		notificationView.setOnClickPendingIntent(R.id.notification_record, PendingIntent.getBroadcast(this, 0, startIntent, 0));

		Intent stopIntent = new Intent(this, SaveTripActivity.class);
		notificationView.setOnClickPendingIntent(R.id.notification_stop, PendingIntent.getActivity(this, 0, stopIntent, 0));

		Intent pauseIntent = new Intent(BROADCAST_ACTION_PAUSE);
		notificationView.setOnClickPendingIntent(R.id.notification_pause, PendingIntent.getBroadcast(this, 0, pauseIntent, 0));

		notificationIntent = new Intent(this, MainActivity.class);
		pendingNotificationIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

		notificationBuilder = new NotificationCompat.Builder(this)
		.setContentTitle(getResources().getString(R.string.recording))
		.setSmallIcon(R.drawable.ic_notification)
		.setOngoing(true)
		.setContentIntent(pendingNotificationIntent)
		.setContent(notificationView);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(getClass().getName(), "onStartCommand");
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		Log.i(getClass().getName(), "onDestroy");
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		Log.i(getClass().getName(), "onBind");
		return recordingServiceBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		Log.i(getClass().getName(), "onUnbind");
		return false;
	}

	public class RecordingServiceBinder extends Binder {
		RecordingService getService() {
			return RecordingService.this;
		}
	}

	public void startRecording(TripData trip) {
		Log.i(getClass().getName(), "startRecording");
		recordingState = STATE_RECORDING;
		setUpdateNotification();
		updateTime();
		//setupTimer();

		registerReceivers();

		this.trip = trip;

	    curSpeed = maxSpeed = distanceTraveled = 0.0f;
	    lastLocation = null;
	    locationClientInit();
	}

	private void registerReceivers() {
		registerReceiver(broadcastReceiver, new IntentFilter(BROADCAST_ACTION_START));
		registerReceiver(broadcastReceiver, new IntentFilter(BROADCAST_ACTION_STOP));
		registerReceiver(broadcastReceiver, new IntentFilter(BROADCAST_ACTION_PAUSE));
	}

	public void pauseRecording() {
		Log.i(getClass().getName(), "pauseRecording");
		recordingState = STATE_PAUSED;
		setUpdateNotification();
		locationClientStopRecording();

		stopUpdatingTime();
	}

	public void resumeRecording() {
		Log.i(getClass().getName(), "resumeRecording");
		recordingState = STATE_RECORDING;
		setUpdateNotification();
		updateTime();

		registerReceivers();

		locationClientStartRecording();
	}

	public void stopRecording() {
		Log.i(getClass().getName(), "stopRecording");
		recordingState = STATE_STOPPED;
		stopForeground(true);

		stopUpdatingTime();
		locationClientStopRecording();

		unregisterReceiver(broadcastReceiver);
	}

	public void cancelRecording() {
		Log.i(getClass().getName(), "cancelRecording");
		if (trip != null) {
			trip.dropTrip();
		}
	}

	public long finishRecording() {
		Log.i(getClass().getName(), "finishRecording");
		return trip.tripid;
	}

	private void setUpdateNotification() {
		notificationView.setViewVisibility(R.id.notification_record, recordingState == STATE_RECORDING ? View.GONE : View.VISIBLE);
		notificationView.setViewVisibility(R.id.notification_pause, recordingState == STATE_PAUSED ? View.GONE : View.VISIBLE);
		startForeground(NOTIFICATION_ID, notificationBuilder.build());
	}

	public static final int STATE_RECORDING = 1;
	public static final int STATE_PAUSED = 2;
	public static final int STATE_STOPPED = 3;

	private int recordingState = STATE_STOPPED;
	public int recordingState() {
		return recordingState;
	}

	private void updateTime() {
		notificationView.setTextViewText(R.id.notification_time_display, String.valueOf(System.currentTimeMillis()));
		setUpdateNotification();

		updateDisplayTimer.postDelayed(updateDisplayHandler, LOOP_TIME);
	}

	private void stopUpdatingTime() {
		updateDisplayTimer.removeCallbacks(updateDisplayHandler);
	}

	private static final int LOOP_TIME = 500;
	private final Handler updateDisplayTimer = new Handler();
	private final Runnable updateDisplayHandler = new Runnable() {
		@Override
		public void run() {
			updateTime();
		}
	};

	double latestUpdate;
	Location lastLocation;
	float distanceTraveled;
	float curSpeed, maxSpeed;
	TripData trip;

	public final static int RECORDING_SPEED = 2 * 1000; //2 second intervals

	@Override
	public void onLocationChanged(Location location) {
		if (location != null) {
			double currentTime = System.currentTimeMillis();
			if (currentTime - latestUpdate >= 1000) {

				latestUpdate = currentTime;
				updateTripStats(location);
				trip.addPointNow(location, currentTime, distanceTraveled);
				// TODO Update the status page every time, if we can.
				//notifyListeners();
			}
		}
	}

	private LocationClient locationClient;
	private LocationRequest locationRequest;

	private void locationClientInit() {
		locationClient = new LocationClient(this, this, this);
		locationClient.connect();
	}

	private void locationClientStartRecording() {
		if (locationClient != null) {
			locationClient.requestLocationUpdates(locationRequest, this);
		}
	}

	private void locationClientStopRecording() {
		if (locationClient != null) {
			//locationClient.removeLocationUpdates(this);
		}
	}

	@Override
	public void onConnected(Bundle arg0) {
		locationRequest = new LocationRequest();
		locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY).setInterval(RECORDING_SPEED);
		locationClientStartRecording();
	}

	final float spdConvert = 2.2369f; //Meters per second to miles per hour
    private void updateTripStats(Location newLocation) {
    	//Update if accuracy is within 50 meters
    	if (newLocation.getAccuracy() > 50) return;

		//Convert speed TODO consider keeping at meters per second and only convert when displayed
		curSpeed = newLocation.getSpeed() * spdConvert;

		//Get out of the car and back on the bike
        if (curSpeed < 60) {
        	maxSpeed = Math.max(maxSpeed, curSpeed);
        }

        if (lastLocation != null) {
            float segmentDistance = lastLocation.distanceTo(newLocation);
            distanceTraveled = distanceTraveled + segmentDistance;
        }

        lastLocation = newLocation;
    }

	public long getCurrentTrip() {
		if (RecordingService.this.trip != null) {
			return RecordingService.this.trip.tripid;
		}
		return -1;
	}

	// Required for location implementation
	@Override
	public void onConnectionFailed(ConnectionResult arg0) {
	}

	@Override
	public void onDisconnected() {
	}
}

/*
public class RecordingService extends Service implements
	GooglePlayServicesClient.ConnectionCallbacks,
	GooglePlayServicesClient.OnConnectionFailedListener,
	LocationListener {

	RecordingActivity recordActivity;
	DbAdapter mDb;

	// Bike bell variables
	static int BELL_FIRST_INTERVAL = 20 * 60 * 1000; //20 minutes
	static int BELL_NEXT_INTERVAL = 5 * 60 * 1000; //5 minutes
    Timer timer;
	SoundPool soundpool;
	int bikebell;
    final Handler mHandler = new Handler();
    final Runnable mRemindUser = new Runnable() {
        public void run() { remindUser(); }
    };

	@Override
	public void onCreate() {
		super.onCreate();
	    soundpool = new SoundPool(1,AudioManager.STREAM_NOTIFICATION,0);
	    bikebell = soundpool.load(this.getBaseContext(), R.raw.bikebell,1);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
        cancelTimer();
	}

	public class MyServiceBinder extends Binder implements IRecordService {
		public long finishRecording() {
			return RecordingService.this.finishRecording();
		}

		public void setListener(RecordingActivity ra) {
			RecordingService.this.recordActivity = ra;
			notifyListeners();
		}
	}
	// END SERVICE METHODS

	// BEGIN RECORDING METHODS



	public void registerUpdates(RecordingActivity r) {
		this.recordActivity = r;
	}

	public TripData getCurrentTrip() {
		return trip;
	}
	// END RECORDING METHODS

	// BEGIN LOCATION METHODS



    void notifyListeners() {
    	if (recordActivity != null) {
    		recordActivity.updateStatus(trip.numpoints, distanceTraveled, curSpeed, maxSpeed);
    	}
    }

	@Override
	public void onConnected(Bundle bundle) {
		locationRequest = new LocationRequest();
		locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY).setInterval(RECORDING_SPEED);
		locationClientStartRecording();
	}

	@Override
	public void onConnectionFailed(ConnectionResult arg0) {
	}

	@Override
	public void onDisconnected() {
	}
	// END LOCATION METHODS

	// BEGIN BELL FUNCTIONS
	private void clearNotifications() {
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancelAll();

		cancelTimer();
	}

	private void setupTimer() {
		cancelTimer();
        timer = new Timer();
        timer.schedule (new TimerTask() {
            @Override public void run() {
                mHandler.post(mRemindUser);
            }
        }, BELL_FIRST_INTERVAL, BELL_NEXT_INTERVAL);
	}

	private void cancelTimer() {
		if (timer!=null) {
            timer.cancel();
            timer.purge();
		}
	}
	// END BELL FUNCTIONS
}
*/