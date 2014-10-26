package org.nevadabike.renotracks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.widget.Toast;

public class TripUploader extends AsyncTask <Long, Integer, Boolean> {
    Context mCtx;
    DbAdapter mDb;

    public static final String TRIP_COORDS_TIME = "rec";
    public static final String TRIP_COORDS_LAT = "lat";
    public static final String TRIP_COORDS_LON = "lon";
    public static final String TRIP_COORDS_ALT = "alt";
    public static final String TRIP_COORDS_SPEED = "spd";
    public static final String TRIP_COORDS_HACCURACY = "hac";
    public static final String TRIP_COORDS_VACCURACY = "vac";

    public static final String USER_AGE = "age";
    public static final String USER_EMAIL = "email";
    public static final String USER_GENDER = "gender";
    public static final String USER_ZIP_HOME = "homeZIP";
    public static final String USER_ZIP_WORK = "workZIP";
    public static final String USER_ZIP_SCHOOL = "schoolZIP";
    public static final String USER_CYCLING_FREQUENCY = "cyclingFreq";

    public static final String USER_ETHNICITY = "ethnicity";
    public static final String USER_INCOME = "income";
    public static final String USER_RIDERTYPE = "rider_type";
    public static final String USER_RIDERHISTORY = "rider_history";

    public TripUploader(Context ctx) {
        super();
        this.mCtx = ctx;
        this.mDb = new DbAdapter(this.mCtx);
    }

    private JSONObject getCoordsJSON(long tripId) throws JSONException {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        mDb.openReadOnly();
        Cursor tripCoordsCursor = mDb.fetchAllCoordsForTrip(tripId);

        // Build the map between JSON fieldname and phone db fieldname:
        Map<String, Integer> fieldMap = new HashMap<String, Integer>();
        fieldMap.put(TRIP_COORDS_TIME,
        		tripCoordsCursor.getColumnIndex(DbAdapter.K_POINT_TIME));
        fieldMap.put(TRIP_COORDS_LAT,
        		tripCoordsCursor.getColumnIndex(DbAdapter.K_POINT_LAT));
        fieldMap.put(TRIP_COORDS_LON,
        		tripCoordsCursor.getColumnIndex(DbAdapter.K_POINT_LGT));
        fieldMap.put(TRIP_COORDS_ALT,
        		tripCoordsCursor.getColumnIndex(DbAdapter.K_POINT_ALT));
        fieldMap.put(TRIP_COORDS_SPEED,
        		tripCoordsCursor.getColumnIndex(DbAdapter.K_POINT_SPEED));
        fieldMap.put(TRIP_COORDS_HACCURACY,
        		tripCoordsCursor.getColumnIndex(DbAdapter.K_POINT_ACC));
        fieldMap.put(TRIP_COORDS_VACCURACY,
        		tripCoordsCursor.getColumnIndex(DbAdapter.K_POINT_ACC));

        // Build JSON objects for each coordinate:
        JSONObject tripCoords = new JSONObject();
        while (!tripCoordsCursor.isAfterLast()) {
            JSONObject coord = new JSONObject();

            coord.put(TRIP_COORDS_TIME,
            		df.format(tripCoordsCursor.getDouble(fieldMap.get(TRIP_COORDS_TIME))));
            coord.put(TRIP_COORDS_LAT,
            		tripCoordsCursor.getDouble(fieldMap.get(TRIP_COORDS_LAT)));
            coord.put(TRIP_COORDS_LON,
            		tripCoordsCursor.getDouble(fieldMap.get(TRIP_COORDS_LON)));
            coord.put(TRIP_COORDS_ALT,
            		tripCoordsCursor.getDouble(fieldMap.get(TRIP_COORDS_ALT)));
            coord.put(TRIP_COORDS_SPEED,
            		tripCoordsCursor.getDouble(fieldMap.get(TRIP_COORDS_SPEED)));
            coord.put(TRIP_COORDS_HACCURACY,
            		tripCoordsCursor.getDouble(fieldMap.get(TRIP_COORDS_HACCURACY)));
            coord.put(TRIP_COORDS_VACCURACY,
            		tripCoordsCursor.getDouble(fieldMap.get(TRIP_COORDS_VACCURACY)));

            tripCoords.put(coord.getString("rec"), coord);
            tripCoordsCursor.moveToNext();
        }
        tripCoordsCursor.close();
        mDb.close();
        return tripCoords;
    }

    private JSONObject getUserJSON() throws JSONException {
        JSONObject user = new JSONObject();
        Map<String, Integer> fieldMap = new HashMap<String, Integer>();

        fieldMap.put(USER_EMAIL, Integer.valueOf(UserInfoActivity.PREF_EMAIL));

        fieldMap.put(USER_ZIP_HOME, Integer.valueOf(UserInfoActivity.PREF_ZIPHOME));
        fieldMap.put(USER_ZIP_WORK, Integer.valueOf(UserInfoActivity.PREF_ZIPWORK));
        fieldMap.put(USER_ZIP_SCHOOL, Integer.valueOf(UserInfoActivity.PREF_ZIPSCHOOL));

        SharedPreferences settings = mCtx.getSharedPreferences("PREFS", 0);
        for (Entry<String, Integer> entry : fieldMap.entrySet()) {
               user.put(entry.getKey(), settings.getString(entry.getValue().toString(), null));
        }
        user.put(USER_AGE, settings.getInt("" + UserInfoActivity.PREF_AGE, 0));
        user.put(USER_GENDER, settings.getInt("" + UserInfoActivity.PREF_GENDER, 0));
        user.put(USER_CYCLING_FREQUENCY, settings.getInt("" + UserInfoActivity.PREF_GENDER, 0)/100);
        //Integer.parseInt(settings.getString(""+UserInfoActivity.PREF_CYCLEFREQ, "0"))
        user.put(USER_ETHNICITY, settings.getInt("" + UserInfoActivity.PREF_ETHNICITY, 0));
        user.put(USER_INCOME, settings.getInt("" + UserInfoActivity.PREF_INCOME, 0));
        user.put(USER_RIDERTYPE, settings.getInt("" + UserInfoActivity.PREF_RIDERTYPE, 0));
        user.put(USER_RIDERHISTORY, settings.getInt("" + UserInfoActivity.PREF_RIDERHISTORY, 0));

        return user;
    }

    private Vector<String> getTripData(long tripId) {
        Vector<String> tripData = new Vector<String>();
        mDb.openReadOnly();
        Cursor tripCursor = mDb.fetchTrip(tripId);

        String note = tripCursor.getString(tripCursor.getColumnIndex(DbAdapter.K_TRIP_NOTE));
        String purpose = tripCursor.getString(tripCursor.getColumnIndex(DbAdapter.K_TRIP_PURP));
        Double startTime = tripCursor.getDouble(tripCursor.getColumnIndex(DbAdapter.K_TRIP_START));
        Double endTime = tripCursor.getDouble(tripCursor.getColumnIndex(DbAdapter.K_TRIP_END));
        tripCursor.close();
        mDb.close();

        SimpleDateFormat df = new SimpleDateFormat(Common.TIMESTAMP_FORMAT);
        tripData.add(note);
        tripData.add(purpose);
        tripData.add(df.format(startTime));
        tripData.add(df.format(endTime));

        return tripData;
    }

    private List<NameValuePair> getPostData(long tripId) throws JSONException {
        JSONObject coords = getCoordsJSON(tripId);
        JSONObject user = getUserJSON();
        String deviceId = Common.getDeviceId(mCtx.getContentResolver());
        Vector<String> tripData = getTripData(tripId);
        String notes = tripData.get(0);
        String purpose = tripData.get(1);
        String startTime = tripData.get(2);
        String endTime = tripData.get(3);

        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
        nameValuePairs.add(new BasicNameValuePair("coords", coords.toString()));
        nameValuePairs.add(new BasicNameValuePair("user", user.toString()));
        nameValuePairs.add(new BasicNameValuePair("device", deviceId));
        nameValuePairs.add(new BasicNameValuePair("notes", notes));
        nameValuePairs.add(new BasicNameValuePair("purpose", purpose));
        nameValuePairs.add(new BasicNameValuePair("start", startTime));
        nameValuePairs.add(new BasicNameValuePair("end", endTime));
        nameValuePairs.add(new BasicNameValuePair("version", "2"));

        return nameValuePairs;
    }

    private static String convertStreamToString(InputStream is) {
        /*
         * To convert the InputStream to String we use the BufferedReader.readLine()
         * method. We iterate until the BufferedReader return null which means
         * there's no more data to read. Each line will appended to a StringBuilder
         * and returned as String.
         */
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    boolean uploadOneTrip(long currentTripId) {
        boolean result = false;

        List<NameValuePair> nameValuePairs;
        try {
            nameValuePairs = getPostData(currentTripId);
        } catch (JSONException e) {
            e.printStackTrace();
            return result;
        }
        //Log.v("PostData", nameValuePairs.toString());

        HttpClient client = new DefaultHttpClient();
        //TODO: Server URL
        final String postUrl = Common.API_URL;
        HttpPost postRequest = new HttpPost(postUrl);

        try {
            postRequest.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            HttpResponse response = client.execute(postRequest);
            String responseString = convertStreamToString(response.getEntity().getContent());
            //Log.v("httpResponse", responseString);
            JSONObject responseData = new JSONObject(responseString);
            if (responseData.getString("status").equals("success")) {
                mDb.open();
                mDb.updateTripStatus(currentTripId, TripData.STATUS_SENT);
                mDb.close();
                result = true;
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
        return result;
    }

    @Override
    protected Boolean doInBackground(Long... tripid) {
        // First, send the trip user asked for:
        Boolean result = uploadOneTrip(tripid[0]);

        // Then, automatically try and send previously-completed trips
        // that were not sent successfully.
        Vector <Long> unsentTrips = new Vector <Long>();

        mDb.openReadOnly();
        Cursor cur = mDb.fetchUnsentTrips();
        if (cur != null && cur.getCount()>0) {
            //pd.setMessage("Sent. You have previously unsent trips; submitting those now.");
            while (!cur.isAfterLast()) {
                unsentTrips.add(Long.valueOf(cur.getLong(0)));
                cur.moveToNext();
            }
            cur.close();
        }
        mDb.close();

        for (Long trip: unsentTrips) {
            result &= uploadOneTrip(trip);
        }
        return result;
    }

    @Override
    protected void onPreExecute() {
        Toast.makeText(mCtx.getApplicationContext(),mCtx.getResources().getString(R.string.submitting), Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onPostExecute(Boolean result) {
        try {
            if (result) {
                Toast.makeText(mCtx.getApplicationContext(),mCtx.getResources().getString(R.string.submit_success), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(mCtx.getApplicationContext(),mCtx.getResources().getString(R.string.submit_fail), Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            // Just don't toast if the view has gone out of context
        }
    }
}
