package org.nevadabike.renotracks;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

public class MenuFragment extends Fragment {
	private MainActivity activity;
	private View view;
	private OnClickListener clickListener;
	private Button recordButton;
	private Button tripsButton;
	private Button marksButton;
	private Button settingsButton;
	private Button helpButton;

	private Typeface menu_font_selected;
	private Typeface menu_font_unselected;

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		activity = (MainActivity) getActivity();
    	view = inflater.inflate(R.layout.menu_fragment, container, false);

    	menu_font_selected = Typeface.create(getResources().getString(R.string.menu_font_selected), Typeface.BOLD);
    	menu_font_unselected = Typeface.create(getResources().getString(R.string.menu_font_unselected), Typeface.NORMAL);

    	clickListener = new OnClickListener(){
			@Override
			public void onClick(View view) {
				switch(view.getId()) {
					case R.id.menu_record:
						activity.selectMenu(MainActivity.MENU_RECORD);
						break;
					case R.id.menu_trips:
						activity.selectMenu(MainActivity.MENU_TRIPS);
						break;
					case R.id.menu_marks:
						activity.selectMenu(MainActivity.MENU_MARKS);
						break;
					case R.id.menu_settings:
						activity.openUserInfo();
						break;
			        case R.id.menu_help:
			        	activity.openHelp();
						break;
				}
			}
        };

        recordButton = (Button) view.findViewById(R.id.menu_record);
        recordButton.setOnClickListener(clickListener);

        tripsButton = (Button) view.findViewById(R.id.menu_trips);
        tripsButton.setOnClickListener(clickListener);

        marksButton = (Button) view.findViewById(R.id.menu_marks);
        marksButton.setOnClickListener(clickListener);

        settingsButton = (Button) view.findViewById(R.id.menu_settings);
        settingsButton.setOnClickListener(clickListener);

        helpButton = (Button) view.findViewById(R.id.menu_help);
        helpButton.setOnClickListener(clickListener);

        activity.selectMenu(MainActivity.MENU_RECORD);

        return view;
    }

	public void selectMenu(int id) {
		recordButton.setTypeface(menu_font_unselected);
		tripsButton.setTypeface(menu_font_unselected);
		marksButton.setTypeface(menu_font_unselected);
		Log.i(getClass().getName(), String.valueOf(id));
		switch(id) {
			case MainActivity.MENU_RECORD:
				recordButton.setTypeface(menu_font_selected);
				break;
			case MainActivity.MENU_TRIPS:
				tripsButton.setTypeface(menu_font_selected);
				break;
			case MainActivity.MENU_MARKS:
				marksButton.setTypeface(menu_font_selected);
				break;
		}
	}
}
