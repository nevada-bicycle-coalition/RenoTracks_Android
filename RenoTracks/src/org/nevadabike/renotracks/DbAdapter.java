package org.nevadabike.renotracks;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.text.TextUtils;
import android.util.Log;

/**
 * Simple database access helper class. Defines the basic CRUD operations, and
 * gives the ability to list all trips as well as retrieve or modify a specific
 * trip.
 *
 * This has been improved from the first version of this tutorial through the
 * addition of better error handling and also using returning a Cursor instead
 * of using a collection of inner classes (which is less scalable and not
 * recommended).
 *
 * **This code borrows heavily from Google demo app "Notepad" in the Android
 * SDK**
 */
public class DbAdapter {
    private static final int DATABASE_VERSION = 27;

    private static final String DATABASE_NAME = "data";
    private static final String DATA_TABLE_TRIPS = "trips";
    private static final String DATA_TABLE_COORDS = "coords";
    private static final String DATA_TABLE_MARKS = "marks";

    public static final String K_TRIP_ROWID = "_id";
    public static final String K_TRIP_PURP = "purp";
    public static final String K_TRIP_START = "start";
    public static final String K_TRIP_END = "endtime";
    public static final String K_TRIP_FANCYSTART = "fancystart";
    public static final String K_TRIP_FANCYINFO = "fancyinfo";
    public static final String K_TRIP_NOTE = "note";
    public static final String K_TRIP_DISTANCE = "distance";
    public static final String K_TRIP_STATUS = "status";

    public static final String K_POINT_ROWID = "_id";
    public static final String K_POINT_TRIP  = "trip";
    public static final String K_POINT_TIME  = "time";
    public static final String K_POINT_LAT   = "latitude";
    public static final String K_POINT_LGT   = "longitude";
    public static final String K_POINT_ACC   = "accuracy";
    public static final String K_POINT_ALT   = "altitude";
    public static final String K_POINT_SPEED = "speed";

    public static final String K_NOTE_ROWID = "_id";
    public static final String K_NOTE_TRIP  = "trip_id";
    public static final String K_NOTE_TIME  = "recorded";
    public static final String K_NOTE_LAT   = "latitude";
    public static final String K_NOTE_LGT   = "longitude";
    public static final String K_NOTE_ALT   = "altitude";
    public static final String K_NOTE_SPEED = "speed";
    public static final String K_NOTE_ACC   = "accuracy";
    public static final String K_NOTE_PURP  = "purp";
    public static final String K_NOTE_DET   = "details";
    public static final String K_NOTE_IMG   = "image_url";

    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;

    /**
     * Database creation sql statement
     */
    private static final String TABLE_CREATE_TRIPS =  TextUtils.join(" ", new String[]{
    	"CREATE TABLE", DATA_TABLE_TRIPS, "(",
    		TextUtils.join(", ", new String[]{
    			K_TRIP_ROWID + " integer primary key autoincrement",
    			K_TRIP_PURP + " text",
    			K_TRIP_START + " double",
    			K_TRIP_END + " double",
    			K_TRIP_FANCYSTART + " text",
    			K_TRIP_FANCYINFO + " text",
    			K_TRIP_NOTE + " text",
    			K_TRIP_DISTANCE + " float",
    			K_TRIP_STATUS + " integer"
    		}),
    	")"
   	});

    private static final String TABLE_CREATE_COORDS = TextUtils.join(" ", new String[]{
    	"CREATE TABLE", DATA_TABLE_COORDS, "(",
    		TextUtils.join(", ", new String[]{
    			K_POINT_ROWID + " integer primary key autoincrement",
    			K_POINT_TRIP + " integer",
    			K_POINT_TIME + " double",
    			K_POINT_LAT + " double",
    			K_POINT_LGT + " double",
    			K_POINT_ALT + " double",
    			K_POINT_SPEED + " float",
    			K_POINT_ACC + " float"
    		}),
    	")"
   	});

    private static final String TABLE_CREATE_MARKS = TextUtils.join(" ", new String[]{
		"CREATE TABLE", DATA_TABLE_MARKS, "(",
			TextUtils.join(", ", new String[]{
				K_NOTE_ROWID + " integer primary key autoincrement",
				K_NOTE_TRIP + " integer",
				K_NOTE_TIME + " double",
				K_NOTE_LAT + " double",
				K_NOTE_LGT + " double",
				K_NOTE_ALT + " double",
				K_NOTE_SPEED + " float",
				K_NOTE_ACC + " float",
				K_NOTE_PURP + " integer",
				K_NOTE_DET + " text",
				K_NOTE_IMG + " text"
			}),
		")"
	});

    private final Context mCtx;

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(TABLE_CREATE_TRIPS);
            db.execSQL(TABLE_CREATE_COORDS);
            db.execSQL(TABLE_CREATE_MARKS);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(getClass().getName(), "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + DATA_TABLE_TRIPS);
            db.execSQL("DROP TABLE IF EXISTS " + DATA_TABLE_COORDS);
            db.execSQL("DROP TABLE IF EXISTS " + DATA_TABLE_MARKS);
            onCreate(db);
        }
    }

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     *
     * @param ctx
     *            the Context within which to work
     */
    public DbAdapter(Context ctx) {
        this.mCtx = ctx;
    }

    /**
     * Open the database. If it cannot be opened, try to create a new instance
     * of the database. If it cannot be created, throw an exception to signal
     * the failure
     *
     * @return this (self reference, allowing this to be chained in an
     *         initialization call)
     * @throws SQLException
     *             if the database could be neither opened or created
     */
    public DbAdapter open() throws SQLException {
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }

    public DbAdapter openReadOnly() throws SQLException {
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getReadableDatabase();
        return this;
    }

    public void close() {
        mDbHelper.close();
    }

    // #### Coordinate table methods ####

    public boolean addCoordToTrip(long tripid, CyclePoint pt) {
    	boolean success = true;

    	// Add the latest point
        ContentValues rowValues = new ContentValues();
        rowValues.put(K_POINT_TRIP, tripid);
        rowValues.put(K_POINT_LAT, pt.latitude);
        rowValues.put(K_POINT_LGT, pt.longitude);
        rowValues.put(K_POINT_TIME, pt.time);
        rowValues.put(K_POINT_ACC, pt.accuracy);
        rowValues.put(K_POINT_ALT, pt.altitude);
        rowValues.put(K_POINT_SPEED, pt.speed);

        Log.i(getClass().getName(), pt.toString());
        Log.i(getClass().getName(), rowValues.toString());

        success = success && (mDb.insert(DATA_TABLE_COORDS, null, rowValues) > 0);

        // And update the trip stats
        rowValues = new ContentValues();
        rowValues.put(K_TRIP_END, pt.time);

        success = success && (mDb.update(DATA_TABLE_TRIPS, rowValues, K_TRIP_ROWID + "=" + tripid, null) > 0);

        return success;
    }

    public boolean deleteAllCoordsForTrip(long tripid) {
        return mDb.delete(DATA_TABLE_COORDS, K_POINT_TRIP + "=" + tripid, null) > 0;
    }

    public Cursor fetchAllCoordsForTrip(long tripid) {
    	try {
            Cursor mCursor = mDb.query(true, DATA_TABLE_COORDS, new String[] {
                    K_POINT_LAT, K_POINT_LGT, K_POINT_TIME,
                    K_POINT_ACC, K_POINT_ALT, K_POINT_SPEED },
                    K_POINT_TRIP + "=" + tripid,
                    null, null, null, K_POINT_TIME, null);

            if (mCursor != null) {
                mCursor.moveToFirst();
            }
            return mCursor;
    	} catch (Exception e) {
    		//Log.v("GOT!",e.toString());
    		return null;
    	}
    }

    // #### Trip table methods ####

    /**
     * Create a new trip using the data provided. If the trip is successfully
     * created return the new rowId for that trip, otherwise return a -1 to
     * indicate failure.
     */
    public long createTrip(String purp, double starttime, String fancystart,
            String note) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(K_TRIP_PURP, purp);
        initialValues.put(K_TRIP_START, starttime);
        initialValues.put(K_TRIP_FANCYSTART, fancystart);
        initialValues.put(K_TRIP_NOTE, note);
        initialValues.put(K_TRIP_STATUS, TripData.STATUS_INCOMPLETE);

        return mDb.insert(DATA_TABLE_TRIPS, null, initialValues);
    }

    public long createTrip() {
        return createTrip("", System.currentTimeMillis(), "", "");
    }

    /**
     * Return a Cursor positioned at the trip that matches the given rowId
     *
     * @param rowId id of trip to retrieve
     * @return Cursor positioned to matching trip, if found
     * @throws SQLException if trip could not be found/retrieved
     */
    public Cursor fetchTrip(long rowId) throws SQLException {
        Cursor mCursor = mDb.query(true, DATA_TABLE_TRIPS, new String[] {
                K_TRIP_ROWID, K_TRIP_PURP, K_TRIP_START, K_TRIP_FANCYSTART,
                K_TRIP_NOTE, K_TRIP_STATUS, K_TRIP_END, K_TRIP_FANCYINFO,
                K_TRIP_DISTANCE},
                K_TRIP_ROWID + "=" + rowId,
                null, null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }

    public boolean updateTrip(long tripid, String purp, double starttime, String fancystart, String fancyinfo, float distance) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(K_TRIP_PURP, purp);
        initialValues.put(K_TRIP_START, starttime);
        initialValues.put(K_TRIP_FANCYSTART, fancystart);
        initialValues.put(K_TRIP_FANCYINFO, fancyinfo);
        initialValues.put(K_TRIP_DISTANCE, distance);

        return mDb.update(DATA_TABLE_TRIPS, initialValues, K_TRIP_ROWID + "=" + tripid, null) > 0;
    }

    public boolean updateTripStatus(long tripid, int tripStatus) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(K_TRIP_STATUS, tripStatus);

        return mDb.update(DATA_TABLE_TRIPS, initialValues, K_TRIP_ROWID + "=" + tripid, null) > 0;
    }


    /**
     * Delete the trip with the given rowId
     *
     * @param rowId
     *            id of note to delete
     * @return true if deleted, false otherwise
     */
    public boolean deleteTrip(long rowId) {
        return mDb.delete(DATA_TABLE_TRIPS, K_TRIP_ROWID + "=" + rowId, null) > 0;
    }

    /**
     * Return a Cursor over the list of all notes in the database
     *
     * @return Cursor over all trips
     */
    public Cursor fetchAllTrips() {
        Cursor c = mDb.query(DATA_TABLE_TRIPS, new String[] { K_TRIP_ROWID,
                K_TRIP_PURP, K_TRIP_START, K_TRIP_FANCYSTART, K_TRIP_NOTE, K_TRIP_FANCYINFO },
                null, null, null, null, "-" + K_TRIP_START);
        if (c != null && c.getCount()>0) {
        	Log.i(getClass().getName(), "at least 1");
        	c.moveToFirst();
        }
        return c;
    }

    public Cursor fetchUnsentTrips() {
        Cursor c = mDb.query(DATA_TABLE_TRIPS, new String[] { K_TRIP_ROWID },
                K_TRIP_STATUS + "=" + TripData.STATUS_COMPLETE,
                null, null, null, null);
        if (c != null && c.getCount()>0) {
        	c.moveToFirst();
        }
        return c;
    }

    public int cleanTables() {
    	int badTrips = 0;

        Cursor c = mDb.query(DATA_TABLE_TRIPS, new String[]
                { K_TRIP_ROWID, K_TRIP_STATUS },
                K_TRIP_STATUS + "=" + TripData.STATUS_INCOMPLETE,
                null, null, null, null);

        if (c != null && c.getCount()>0) {
        	c.moveToFirst();
        	badTrips = c.getCount();

            while (!c.isAfterLast()) {
                long tripid = c.getInt(0);
                deleteAllCoordsForTrip(tripid);
                c.moveToNext();
            }
        }
        c.close();
        if (badTrips>0) {
        	mDb.delete(DATA_TABLE_TRIPS, K_TRIP_STATUS + "=" + TripData.STATUS_INCOMPLETE, null);
        }
        return badTrips;
    }

    public long insertMark(int tripID, Location location, int purpose, String details, String image_url) {

        ContentValues initialValues = new ContentValues();
        initialValues.put(K_NOTE_TRIP, tripID);
        initialValues.put(K_NOTE_TIME, System.currentTimeMillis());
        initialValues.put(K_NOTE_LAT, location.getLatitude());
        initialValues.put(K_NOTE_LGT, location.getLongitude());
        initialValues.put(K_NOTE_ALT, location.getAltitude());
        initialValues.put(K_NOTE_SPEED, location.getSpeed());
        initialValues.put(K_NOTE_ACC, location.getAccuracy());
        initialValues.put(K_NOTE_PURP, purpose);
        initialValues.put(K_NOTE_DET, details);
        initialValues.put(K_NOTE_IMG, image_url);

        Log.i(getClass().getName(), initialValues.toString());

        return mDb.insert(DATA_TABLE_MARKS, null, initialValues);
    }

    public Cursor fetchAllMarks() {
        Cursor c = mDb.query(DATA_TABLE_MARKS, new String[] { K_NOTE_ROWID,
        		K_NOTE_TRIP, K_NOTE_TIME, K_NOTE_PURP, K_NOTE_DET, K_NOTE_IMG },
                null, null, null, null, "-" + K_NOTE_TIME);
        if (c != null && c.getCount()>0) {
        	Log.i(getClass().getName(), "at least 1");
        	c.moveToFirst();
        }
        return c;
    }

	public boolean deleteMark(long rowId) {
	    return mDb.delete(DATA_TABLE_TRIPS, K_NOTE_ROWID + "=" + rowId, null) > 0;
	}
}
