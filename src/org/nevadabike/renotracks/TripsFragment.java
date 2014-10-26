package org.nevadabike.renotracks;

import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.Toast;

public class TripsFragment extends Fragment
{
	private static final String TAG = "TripsFragment";
    private View view;
	private ListView listView;

	private FragmentActivity activity;

	private final static int CONTEXT_RETRY = 0;
    private final static int CONTEXT_DELETE = 1;

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
		activity = getActivity();
		view = inflater.inflate(R.layout.trips_fragment, container, false);

		listView = (ListView) view.findViewById(R.id.listView1);
		populateList();

        return view;
    }

	void populateList()
	{
		// Get list from the real phone database. W00t!
		DbAdapter mDb = new DbAdapter(activity);
		mDb.open();

		// Clean up any bad trips & coords from crashes
		int cleanedTrips = mDb.cleanTables();
		if (cleanedTrips > 0) {
		    Toast.makeText(activity, cleanedTrips + getResources().getString(R.string.trips_removed), Toast.LENGTH_SHORT).show();
		}

		try {
			Cursor allTrips = mDb.fetchAllTrips();

			SimpleCursorAdapter sca = new SimpleCursorAdapter(activity,
				R.layout.twolinelist, allTrips,
				new String[] { "purp", "fancystart", "fancyinfo"},
				new int[] {R.id.text1, R.id.text2, R.id.text3},
				0
			);
			listView.setAdapter(sca);

			/*
			int numtrips = allTrips.getCount();
			switch (numtrips)
			{
				case 0:
					counter.setText(getResources().getString(R.string.saved_trips_0));
					break;
				case 1:
					counter.setText(getResources().getString(R.string.saved_trips_1));
					break;
				default:
					counter.setText(numtrips + getResources().getString(R.string.saved_trips_X));
			}
			*/
		} catch (SQLException sqle) {
			// Do nothing, for now!
		}
		mDb.close();

		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
		    public void onItemClick(AdapterView<?> parent, View v, int pos, long id) {
		        Intent i = new Intent(activity, ShowMap.class);
		        i.putExtra("showtrip", id);
		        startActivity(i);
		    }
		});
		registerForContextMenu(listView);
	}

	@Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
	    menu.add(0, CONTEXT_RETRY, 0, getResources().getString(R.string.retry_upload));
	    menu.add(0, CONTEXT_DELETE, 0,  getResources().getString(R.string.delete));
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
	    AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
	    switch (item.getItemId()) {
	    case CONTEXT_RETRY:
	        retryTripUpload(info.id);
	        return true;
	    case CONTEXT_DELETE:
	        deleteTrip(info.id);
	        return true;
	    default:
	        return super.onContextItemSelected(item);
	    }
	}

	private void retryTripUpload(long tripId) {
	    TripUploader uploader = new TripUploader(activity);
        uploader.execute(tripId);
	}

	private void deleteTrip(long tripId) {
	    DbAdapter mDbHelper = new DbAdapter(activity);
        mDbHelper.open();
        mDbHelper.deleteAllCoordsForTrip(tripId);
        mDbHelper.deleteTrip(tripId);
        mDbHelper.close();
        listView.invalidate();
        populateList();
    }
}