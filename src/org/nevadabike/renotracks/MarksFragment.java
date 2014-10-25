package org.nevadabike.renotracks;

import java.util.ArrayList;

import org.nevadabike.renotracks.Marks.Mark;
import org.nevadabike.renotracks.Marks.MarksAdapter;

import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;

public class MarksFragment extends Fragment {
    private View view;
	private ListView listView;

	//private final int marksCount = 20;
	//private ArrayList<Mark> marks;
	private FragmentActivity activity;

    private final static int CONTEXT_DELETE = 1;
    private ArrayList<Mark> marks;

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		activity = getActivity();
		view = inflater.inflate(R.layout.trips_fragment, container, false);

		listView = (ListView) view.findViewById(R.id.listView1);
		populateList();

        return view;
    }

	void populateList() {
		// Get list from the real phone database. W00t!
		DbAdapter mDb = new DbAdapter(activity);

		marks = new ArrayList<Mark>();

		try {
			mDb.open();
			Cursor allTrips = mDb.fetchAllMarks();

			allTrips.moveToFirst();
			do {
				int purpose = allTrips.getInt(allTrips.getColumnIndex(DbAdapter.K_NOTE_PURP));
				double time = allTrips.getDouble(allTrips.getColumnIndex(DbAdapter.K_NOTE_TIME));
				marks.add(new Mark(purpose, time));
			} while(allTrips.moveToNext());

			mDb.close();
		} catch (SQLException sqle) {
			// Do nothing, for now!
		}

		MarksAdapter adapter = new MarksAdapter(getActivity(), marks);
		listView.setAdapter(adapter);



		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
		    public void onItemClick(AdapterView<?> parent, View v, int pos, long id) {
		        Intent i = new Intent(activity, ShowMark.class);
		        i.putExtra("showtrip", id);
		        startActivity(i);
		    }
		});
		registerForContextMenu(listView);
	}

	@Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
	    menu.add(0, CONTEXT_DELETE, 0,  getResources().getString(R.string.delete));
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
	    AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
	    switch (item.getItemId()) {
	    case CONTEXT_DELETE:
	        deleteMark(info.id);
	        return true;
	    default:
	        return super.onContextItemSelected(item);
	    }
	}

	private void deleteMark(long markID) {
	    DbAdapter mDbHelper = new DbAdapter(activity);
        mDbHelper.open();
        mDbHelper.deleteMark(markID);
        mDbHelper.close();
        listView.invalidate();
        populateList();
    }
}