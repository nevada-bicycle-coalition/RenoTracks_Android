package org.nevadabike.renotracks;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.util.Log;
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
	public final static String TAG = "RecordingService";

	private final RecordingServiceBinder recordingServiceBinder = new RecordingServiceBinder();

	private RemoteViews notificationView;
	private Intent notificationIntent;
	private PendingIntent pendingNotificationIntent;
	private Builder notificationBuilder;
	private final static int NOTIFICATION_ID = 100;

	private BroadcastReceiver broadcastReceiver;

	public final static String NOTIFICATION_BROADCAST_ACTION_START = "NOTIFICATION_BROADCAST_ACTION_START";
	public final static String NOTIFICATION_BROADCAST_ACTION_STOP = "NOTIFICATION_BROADCAST_ACTION_STOP";
	public final static String NOTIFICATION_BROADCAST_ACTION_PAUSE = "NOTIFICATION_BROADCAST_ACTION_PAUSE";

	public final static String SERVICE_BROADCAST_ACTION_START = "SERVICE_BROADCAST_ACTION_START";
	public final static String SERVICE_BROADCAST_ACTION_STOP = "SERVICE_BROADCAST_ACTION_STOP";
	public final static String SERVICE_BROADCAST_ACTION_PAUSE = "SERVICE_BROADCAST_ACTION_PAUSE";

	public final static String BROADCAST_ACTION_LOCATION_CHANGED = "BROADCAST_ACTION_LOCATION_CHANGED";

	public static final int STATE_RECORDING = 1;
	public static final int STATE_PAUSED = 2;
	public static final int STATE_STOPPED = 3;

	private int recordingState = STATE_STOPPED;

	public final static int RECORDING_SPEED = 2 * 1000; //2 second intervals

	public static int BELL_INTERVAL = 5 * 60 * 1000; //5 minutes
	public static int NOTIFICATION_UPDATE_INTERVAL = 500; //HALF SECOND INTERVAL

    Timer bellTimer;
    Timer notificationTimer;
	SoundPool soundpool;
	int bikebell;

	double lastUpdate;
	Location lastLocation;
	float distanceTraveled;
	float curSpeed, maxSpeed;
	TripData trip;
	final float spdConvert = 2.2369f; //Meters per second to miles per hour

	private NotificationManager notificationManager;

	private Intent serviceStartIntent;
	private Intent serviceStopIntent;
	private Intent servicePauseIntent;

	//SERVICE LIFECYCLE FUNCTIONS
	public class RecordingServiceBinder extends Binder {
		RecordingService getService() {
			return RecordingService.this;
		}
	}

	@Override
	public void onCreate()
	{
		soundpool = new SoundPool(1,AudioManager.STREAM_NOTIFICATION,0);
	    bikebell = soundpool.load(this.getBaseContext(), R.raw.bikebell,1);

		notificationView = new RemoteViews(getPackageName(), R.layout.customnotification);
		notificationView.setImageViewResource(R.id.notification_icon, R.drawable.ic_launcher);

		broadcastReceiver = new BroadcastReceiver(){
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				Log.i(TAG, action);
				if (action == NOTIFICATION_BROADCAST_ACTION_START) {
					//resumeRecording();
				} else if (action == NOTIFICATION_BROADCAST_ACTION_PAUSE) {
					//pauseRecording();
				} else if (action == NOTIFICATION_BROADCAST_ACTION_STOP) {
					stopRecording();
				}
			}
		};

		serviceStartIntent = new Intent(SERVICE_BROADCAST_ACTION_START);
		serviceStopIntent = new Intent(SERVICE_BROADCAST_ACTION_STOP);
		servicePauseIntent = new Intent(SERVICE_BROADCAST_ACTION_PAUSE);

		//Intent notificationStartIntent = new Intent(NOTIFICATION_BROADCAST_ACTION_START);
		//notificationView.setOnClickPendingIntent(R.id.notification_record, PendingIntent.getBroadcast(this, 0, notificationStartIntent, 0));

		//Intent notificationPauseIntent = new Intent(NOTIFICATION_BROADCAST_ACTION_PAUSE);
		//notificationView.setOnClickPendingIntent(R.id.notification_pause, PendingIntent.getBroadcast(this, 0, notificationPauseIntent, 0));

		Intent notificationStopIntent = new Intent(this, SaveTripActivity.class);
		notificationView.setOnClickPendingIntent(R.id.notification_stop, PendingIntent.getActivity(this, 0, notificationStopIntent, 0));

		notificationIntent = new Intent(this, MainActivity.class);
		pendingNotificationIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

		notificationBuilder = new NotificationCompat.Builder(this)
			.setContentTitle(getResources().getString(R.string.recording))
			.setSmallIcon(R.drawable.ic_notification)
			.setOngoing(true)
			.setContentIntent(pendingNotificationIntent)
			.setContent(notificationView);

		notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "onStartCommand");
		locationClientStart();
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		Log.i(TAG, "onDestroy");
		locationClientStop();
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		Log.i(TAG, "onBind");
		return recordingServiceBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		Log.i(TAG, "onUnbind");
		return false;
	}

	//LOCATION FUNCTIONS
	private LocationClient locationClient;
	private LocationRequest locationRequest;

	private void locationClientStart() {
		Log.i(TAG, "locationClientInit");
		if (locationClient == null) {
			locationClient = new LocationClient(this, this, this);
			locationClient.connect();
		}
	}

	private void locationClientStop() {
		Log.i(TAG, "locationClientStopListening");
		locationClient.removeLocationUpdates(this);
	}

	@Override
	public void onConnected(Bundle arg0) {
		Log.i(TAG, "onConnected");
		locationRequest = new LocationRequest();
		locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY).setInterval(RECORDING_SPEED);
		locationClient.requestLocationUpdates(locationRequest, this);
	}

	@Override
	public void onConnectionFailed(ConnectionResult arg0) {
		// Required for location implementation
		Log.i(TAG, "onConnectionFailed");
	}

	@Override
	public void onDisconnected() {
		// Required for location implementation
		Log.i(TAG, "onDisconnected");
	}

	HashMap<String, Location> tripPoints;

	long startTime;

	@Override
	public void onLocationChanged(Location location)
	{
		if (location!= null) {
			Log.i(TAG, "onLocationChanged");
			Log.i(TAG, location.getLatitude() + ", " + location.getLongitude() + ", " + location.getAltitude() + " (" + location.getAccuracy() + ")");

			double currentTime = System.currentTimeMillis();
			if (recordingState == this.STATE_RECORDING && currentTime - lastUpdate >= 1000 && location.getAccuracy() < 50) {
				//Convert speed
				curSpeed = location.getSpeed() * spdConvert;

				//Don't record anything faster than 60 mph
		        maxSpeed = Math.min(Math.max(maxSpeed, curSpeed), 60);

		        if (lastLocation != null && location != lastLocation)
		        {
		            float segmentDistance = lastLocation.distanceTo(location);
		            distanceTraveled = distanceTraveled + segmentDistance;
		        }

		        tripPoints.put(String.valueOf(currentTime), location);

		        updateNotification();
			}

	        notifyListeners();

	        lastUpdate = currentTime;
	        lastLocation = location;
		}
	}

	public void startRecording() {
		Log.i(TAG, "startRecording");
		recordingState = STATE_RECORDING;

		sendBroadcast(serviceStartIntent);

		updateNotification();
		registerReceivers();

		startTime = System.currentTimeMillis();

		tripPoints = new HashMap<String, Location>();

		lastUpdate = curSpeed = maxSpeed = distanceTraveled = 0;
	    onLocationChanged(lastLocation);

	    setupTimers();
	}

	private void registerReceivers() {
		registerReceiver(broadcastReceiver, new IntentFilter(NOTIFICATION_BROADCAST_ACTION_START));
		registerReceiver(broadcastReceiver, new IntentFilter(NOTIFICATION_BROADCAST_ACTION_PAUSE));
	}

	private void unregisterReceivers() {
		unregisterReceiver(broadcastReceiver);
	}
	/*
	public void pauseRecording() {
		Log.i(TAG, "pauseRecording");
		recordingState = STATE_PAUSED;
		sendBroadcast(servicePauseIntent);
		updateNotification();
	}

	public void resumeRecording() {
		Log.i(TAG, "resumeRecording");
		recordingState = STATE_RECORDING;
		sendBroadcast(serviceStartIntent);
		updateNotification();
	}
	*/
	public void stopRecording()
	{
		Log.i(TAG, "stopRecording");
		recordingState = STATE_STOPPED;
		sendBroadcast(serviceStopIntent);
		notificationManager.cancel(NOTIFICATION_ID);
		unregisterReceivers();

		trip = TripData.createTrip(this, startTime);

		for(Entry<String, Location> entry : tripPoints.entrySet()) {
		    String pointTime = entry.getKey();
		    Location pointLocation = entry.getValue();

		    trip.addPoint(pointLocation, Double.parseDouble(pointTime));
		}

		trip.distance = distanceTraveled;
		trip.updateTrip();

		cancelTimers();
	}

	public int recordingState()
	{
		return recordingState;
	}

	private void updateNotification()
	{
        int total_seconds = Math.round((System.currentTimeMillis() - startTime)/1000);
        int seconds = total_seconds;
		int minutes = (int) Math.floor(seconds/60);
		seconds -= (minutes * 60);
		int hours = (int) Math.floor(minutes/60);
		minutes -= (hours * 60);

		//notificationView.setViewVisibility(R.id.notification_record, recordingState == STATE_RECORDING ? View.GONE : View.VISIBLE);
		//notificationView.setViewVisibility(R.id.notification_pause, recordingState == STATE_PAUSED ? View.GONE : View.VISIBLE);
		notificationView.setTextViewText(R.id.notification_distance_display, String.format("%1.1f miles", Common.METER_TO_MILE * distanceTraveled));
		notificationView.setTextViewText(R.id.notification_time_display, String.format("%1$02d:%2$02d:%3$02d", hours, minutes, seconds));

		notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
	}

    private void notifyListeners() {
    	sendBroadcast(new Intent(BROADCAST_ACTION_LOCATION_CHANGED));
    }

    public Location getLastLocation() {
    	return lastLocation;
    }

    final Handler timerHandler = new Handler();
    final Runnable mRemindUser = new Runnable()
    {
        public void run()
        {
        	remindUser();
        }
    };

    final Handler notificationHandler = new Handler();
    final Runnable mUpdateNotification = new Runnable()
    {
        public void run()
        {
        	updateNotification();
        }
    };

    private void setupTimers()
    {
		cancelTimers();

        bellTimer = new Timer();
        bellTimer.schedule (new TimerTask()
        {
            @Override public void run()
            {
                timerHandler.post(mRemindUser);
            }
        }, BELL_INTERVAL, BELL_INTERVAL);

        notificationTimer = new Timer();
        notificationTimer.schedule (new TimerTask()
        {
            @Override public void run()
            {
                notificationHandler.post(mUpdateNotification);
            }
        }, NOTIFICATION_UPDATE_INTERVAL, NOTIFICATION_UPDATE_INTERVAL);
	}

	private void cancelTimers() {
		if (bellTimer != null)
		{
			bellTimer.cancel();
			bellTimer.purge();
		}

		if (notificationTimer != null)
		{
			notificationTimer.cancel();
			notificationTimer.purge();
		}
	}

	private void remindUser()
	{
		Log.i(TAG, "PLAY THE BELL SOUND");
		soundpool.play(bikebell, 1.0f, 1.0f, 1, 0, 1.0f);
	}
}