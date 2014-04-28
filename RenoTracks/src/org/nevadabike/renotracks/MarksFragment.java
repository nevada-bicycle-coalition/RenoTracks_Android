package org.nevadabike.renotracks;

import java.util.ArrayList;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class MarksFragment extends Fragment {
    private View view;
	private ListView listView;

	private final int marksCount = 20;
	private ArrayList<Mark> marks;

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.marks_fragment, container, false);

		marks = new ArrayList<Mark>();
		for(int i = 1; i<=marksCount; i++) {
			marks.add(new Mark(i));
		}
		listView = (ListView) view.findViewById(R.id.listView1);

		MarksAdapter adapter = new MarksAdapter(getActivity(), marks);

		listView.setAdapter(adapter);

        return view;
    }

	private class Mark {
		public String note;
		public String timestamp = "April 23, 2014 at 12:00 PM";

		public Mark(int index) {
			note = "Mark " + index;
		}
	}

	private class MarksAdapter extends ArrayAdapter<Mark> {
		public MarksAdapter(Context context, ArrayList<Mark> marks) {
			super(context, R.layout.marks_list_item, marks);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Mark mark = getItem(position);
			if (convertView == null) {
				convertView = LayoutInflater.from(getContext()).inflate(R.layout.marks_list_item, null);
			}

			TextView note = (TextView) convertView.findViewById(R.id.note);
			TextView timestamp = (TextView) convertView.findViewById(R.id.timestamp);

			note.setText(mark.note);
			timestamp.setText(mark.timestamp);

			return convertView;
		}
	}
}