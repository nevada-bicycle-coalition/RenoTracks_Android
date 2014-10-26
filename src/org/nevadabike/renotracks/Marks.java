package org.nevadabike.renotracks;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class Marks {

	private static final String TAG = "Marks";

	public static HashMap<Integer, Integer> purposeTitles;
	static {
		purposeTitles = new HashMap<Integer, Integer>();
		purposeTitles.put(0, R.string.marks_0);
		purposeTitles.put(1, R.string.marks_1);
		purposeTitles.put(2, R.string.marks_2);
		purposeTitles.put(3, R.string.marks_3);
		purposeTitles.put(4, R.string.marks_4);
		purposeTitles.put(5, R.string.marks_5);
		purposeTitles.put(6, R.string.marks_6);
		purposeTitles.put(7, R.string.marks_7);
		purposeTitles.put(8, R.string.marks_8);
		purposeTitles.put(9, R.string.marks_9);
		purposeTitles.put(10, R.string.marks_10);
		purposeTitles.put(11, R.string.marks_11);
	}

	public static HashMap<Integer, Integer> purposeIcons;
	static {
		purposeIcons = new HashMap<Integer, Integer>();
		purposeIcons.put(0, R.drawable.note_asset_picker);
		purposeIcons.put(1, R.drawable.note_asset_picker);
		purposeIcons.put(2, R.drawable.note_asset_picker);
		purposeIcons.put(3, R.drawable.note_asset_picker);
		purposeIcons.put(4, R.drawable.note_asset_picker);
		purposeIcons.put(5, R.drawable.note_asset_picker);
		purposeIcons.put(6, R.drawable.note_asset_picker);
		purposeIcons.put(7, R.drawable.note_issue_picker);
		purposeIcons.put(8, R.drawable.note_issue_picker);
		purposeIcons.put(9, R.drawable.note_issue_picker);
		purposeIcons.put(10, R.drawable.note_issue_picker);
		purposeIcons.put(11, R.drawable.note_issue_picker);
	}

	public static HashMap<Integer, Integer> purposeDescriptions;
	static {
		purposeDescriptions = new HashMap<Integer, Integer>();
		purposeDescriptions.put(-1, R.string.marks_no_desc);
		purposeDescriptions.put(0, R.string.marks_0_desc);
		purposeDescriptions.put(1, R.string.marks_1_desc);
		purposeDescriptions.put(2, R.string.marks_2_desc);
		purposeDescriptions.put(3, R.string.marks_3_desc);
		purposeDescriptions.put(4, R.string.marks_4_desc);
		purposeDescriptions.put(5, R.string.marks_5_desc);
		purposeDescriptions.put(6, R.string.marks_6_desc);
		purposeDescriptions.put(7, R.string.marks_7_desc);
		purposeDescriptions.put(8, R.string.marks_8_desc);
		purposeDescriptions.put(9, R.string.marks_9_desc);
		purposeDescriptions.put(10, R.string.marks_10_desc);
		purposeDescriptions.put(11, R.string.marks_11_desc);
	}

	public static class Mark {
		public long rowId;
		public int tripID;
		public int purpose;
		public double time;
		public String details;
		public String image_url;
		public double latitude;
		public double longitude;
		public double altitude;
		public float speed;
		public float accuracy;

		public Mark(int rowId, int tripID, int purpose, double time, String details, String image_url, double latitude, double longitude, double altitude, float speed, float accuracy)
		{
			this.rowId = rowId;
			this.tripID = tripID;
			this.purpose = purpose;
			this.time = time;
			this.details = details;
			this.image_url = image_url;
			this.latitude = latitude;
			this.longitude = longitude;
			this.altitude = altitude;
			this.speed = speed;
			this.accuracy = accuracy;
		}

		@Override
		public String toString()
		{
			HashMap<String, String> fields = new HashMap<String, String>();

			fields.put("rowId", String.valueOf(rowId));
			fields.put("tripID", String.valueOf(tripID));
			fields.put("purpose", String.valueOf(purpose));
			fields.put("time", String.valueOf(time));
			fields.put("details", String.valueOf(details));
			fields.put("image_url", String.valueOf(image_url));
			fields.put("latitude", String.valueOf(latitude));
			fields.put("longitude", String.valueOf(longitude));
			fields.put("altitude", String.valueOf(altitude));
			fields.put("speed", String.valueOf(speed));
			fields.put("accuracy", String.valueOf(accuracy));

			return fields.toString();
		}
	}

	public static class MarksAdapter extends ArrayAdapter<Mark> {
		public MarksAdapter(Context context, ArrayList<Mark> marks) {
			super(context, R.layout.marks_list_item, marks);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = LayoutInflater.from(getContext()).inflate(R.layout.marks_list_item, null);
			}

			Mark mark = getItem(position);
			TextView note = (TextView) convertView.findViewById(R.id.note);
			TextView timestamp = (TextView) convertView.findViewById(R.id.timestamp);
			ImageView icon = (ImageView) convertView.findViewById(R.id.icon);

			note.setText(purposeTitles.get(mark.purpose));
			timestamp.setText(String.format("%1$s at %2$s", DateFormat.getDateInstance(DateFormat.LONG).format(mark.time), DateFormat.getTimeInstance(DateFormat.SHORT).format(mark.time)));
			icon.setImageResource(purposeIcons.get(mark.purpose));

			return convertView;
		}
	}

	public static ArrayList<Mark> fetchMarks(Context context)
	{
		ArrayList<Mark> marks = new ArrayList<Mark>();

		// Get list from the real phone database. W00t!
		DbAdapter mDb = new DbAdapter(context);

		try {
			mDb.open();
			Cursor cursor = mDb.fetchAllMarks();

			if (cursor.moveToFirst())
			{
				do {
					marks.add(makeMark(cursor));
				} while(cursor.moveToNext());
			}

			mDb.close();
		} catch (Exception sqle) {
			// Do nothing, for now!
		}

		return marks;
	}

	public static Mark fetchMark(Context context, long rowId)
	{
		DbAdapter mDb = new DbAdapter(context);

		Mark mark = null;
		try {
			mDb.open();
			Cursor cursor = mDb.fetchMark(rowId);

			mark = makeMark(cursor);

			mDb.close();
		} catch (Exception sqle) {
			// Do nothing, for now!
		}

		return mark;
	}

	private static Mark makeMark(Cursor cursor)
	{
		return new Mark(
			cursor.getInt(cursor.getColumnIndex(DbAdapter.K_NOTE_ROWID)),
			cursor.getInt(cursor.getColumnIndex(DbAdapter.K_NOTE_TRIP)),
			cursor.getInt(cursor.getColumnIndex(DbAdapter.K_NOTE_PURP)),
			cursor.getDouble(cursor.getColumnIndex(DbAdapter.K_NOTE_TIME)),
			cursor.getString(cursor.getColumnIndex(DbAdapter.K_NOTE_DET)),
			cursor.getString(cursor.getColumnIndex(DbAdapter.K_NOTE_IMG)),
			cursor.getDouble(cursor.getColumnIndex(DbAdapter.K_NOTE_LAT)),
			cursor.getDouble(cursor.getColumnIndex(DbAdapter.K_NOTE_LGT)),
			cursor.getDouble(cursor.getColumnIndex(DbAdapter.K_NOTE_ALT)),
			cursor.getFloat(cursor.getColumnIndex(DbAdapter.K_NOTE_SPEED)),
			cursor.getFloat(cursor.getColumnIndex(DbAdapter.K_NOTE_ACC))
		);
	}
}
