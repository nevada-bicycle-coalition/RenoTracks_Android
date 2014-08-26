package org.nevadabike.renotracks;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class Marks {

	public static HashMap<Integer, Integer> purposeIcons;
	static {
		purposeIcons = new HashMap<Integer, Integer>();
		purposeIcons.put(R.string.marks_0, R.drawable.note_asset_picker);
		purposeIcons.put(R.string.marks_1, R.drawable.note_asset_picker);
		purposeIcons.put(R.string.marks_2, R.drawable.note_asset_picker);
		purposeIcons.put(R.string.marks_3, R.drawable.note_asset_picker);
		purposeIcons.put(R.string.marks_4, R.drawable.note_asset_picker);
		purposeIcons.put(R.string.marks_5, R.drawable.note_asset_picker);
		purposeIcons.put(R.string.marks_6, R.drawable.note_issue_picker);
		purposeIcons.put(R.string.marks_7, R.drawable.note_issue_picker);
		purposeIcons.put(R.string.marks_8, R.drawable.note_issue_picker);
		purposeIcons.put(R.string.marks_9, R.drawable.note_issue_picker);
		purposeIcons.put(R.string.marks_10, R.drawable.note_issue_picker);
		purposeIcons.put(R.string.marks_11, R.drawable.note_issue_picker);
	}

	public static class Mark {
		public int purpose;
		public double time;

		public Mark(int purpose, double time) {
			this.purpose = purpose;
			this.time = time;
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

			note.setText(mark.purpose);
			timestamp.setText(String.format("%1$s at %2$s", DateFormat.getDateInstance(DateFormat.LONG).format(mark.time), DateFormat.getTimeInstance(DateFormat.SHORT).format(mark.time)));
			icon.setImageResource(Marks.purposeIcons.get(mark.purpose));
			return convertView;
		}
	}
}
