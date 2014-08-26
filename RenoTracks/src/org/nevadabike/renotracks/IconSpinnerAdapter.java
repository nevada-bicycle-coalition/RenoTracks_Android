package org.nevadabike.renotracks;

import java.util.ArrayList;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;


public class IconSpinnerAdapter extends ArrayAdapter<IconSpinnerAdapter.IconItem> {
    private final Activity activity;
	private final ArrayList<IconSpinnerAdapter.IconItem> data;

	public IconSpinnerAdapter(Activity activity, ArrayList<IconSpinnerAdapter.IconItem> data) {
        super(activity, 0, data);
        this.activity = activity;
        this.data = data;
    }

    @Override
    public View getDropDownView(int position, View convertView,ViewGroup parent) {
        return getCustomView(R.layout.icon_spinner_dropdown_item, position, convertView, parent);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getCustomView(R.layout.icon_spinner_item, position, convertView, parent);
    }

    public View getCustomView(int resource, int position, View convertView, ViewGroup parent) {
		View row = activity.getLayoutInflater().inflate(resource, parent, false);
		IconSpinnerAdapter.IconItem item = data.get(position);

		TextView label = (TextView) row.findViewById(android.R.id.text1);
		label.setText(item.label);
		if (item.icon != 0) {
			ImageView icon = (ImageView) row.findViewById(android.R.id.icon1);
			icon.setImageDrawable(activity.getResources().getDrawable(item.icon));
		}

		return row;
    }

    static class IconItem {
		public int icon;
		public String label;
		public int id;

		public IconItem(int icon, String label) {
			this(icon, label, 0);
		}
		public IconItem(int icon, String label, int id) {
			this.icon = icon;
			this.label = label;
			this.id = id;
		}
	}
}