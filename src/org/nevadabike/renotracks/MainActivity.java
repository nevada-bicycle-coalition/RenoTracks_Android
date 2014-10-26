package org.nevadabike.renotracks;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainActivity extends FragmentActivity {
	private final static String TAG = "MainActivity";

	private ActionBarDrawerToggle drawerToggle;
	private DrawerLayout drawerLayout;
	private FragmentManager fragmentManager;

	public final static int MENU_RECORD = 1;
	public final static int MENU_TRIPS = 2;
	public final static int MENU_MARKS = 3;
	private int currentView;

	private RecordingFragment recordingFragment;
	private TripsFragment tripsFragment;
	private MarksFragment marksFragment;
	private final MainActivity mActivity = this;

	public final static String PREFS_KEY = "PREFS";

	private Button recordButton;
	private Button tripsButton;
	private Button marksButton;
	private View settingsButton;
	private View helpButton;

	private Typeface menu_font_selected;
	private Typeface menu_font_unselected;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);

		fragmentManager = getSupportFragmentManager();

		recordingFragment = new RecordingFragment();
		tripsFragment = new TripsFragment();
		marksFragment = new MarksFragment();

		drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close);
		drawerLayout.setDrawerListener(drawerToggle);

		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeButtonEnabled(true);

		menu_font_selected = Typeface.create(getResources().getString(R.string.menu_font_selected), Typeface.BOLD);
		menu_font_unselected = Typeface.create(getResources().getString(R.string.menu_font_unselected), Typeface.NORMAL);

		recordButton = (Button) findViewById(R.id.menu_record);
		recordButton.setOnClickListener(menuClickListener);

		tripsButton = (Button) findViewById(R.id.menu_trips);
		tripsButton.setOnClickListener(menuClickListener);

		marksButton = (Button) findViewById(R.id.menu_marks);
		marksButton.setOnClickListener(menuClickListener);

		settingsButton = findViewById(R.id.menu_settings);
		settingsButton.setOnClickListener(menuClickListener);

		helpButton = findViewById(R.id.menu_help);
		helpButton.setOnClickListener(menuClickListener);

		selectMenu(MENU_RECORD);

		SharedPreferences settings = getSharedPreferences(PREFS_KEY, 0);
		if (settings.getAll().isEmpty())
		{
			showWelcomeDialog();
		}
	}

	private final OnClickListener menuClickListener = new OnClickListener()
	{

		@Override
		public void onClick(View v)
		{
			switch(v.getId())
			{
				case R.id.menu_record:
					selectMenu(MENU_RECORD);
					break;
				case R.id.menu_trips:
					selectMenu(MENU_TRIPS);
					break;
				case R.id.menu_marks:
					selectMenu(MENU_MARKS);
					break;
				case R.id.menu_settings:
					openUserInfo();
					break;
				case R.id.menu_help:
					openHelp();
					break;
			}
		}
	};

	public void selectMenu(int menuItem)
	{
		Fragment newFragment = null;
		if (menuItem == MENU_RECORD && currentView != MENU_RECORD)
		{
			currentView = MENU_RECORD;
			newFragment = recordingFragment;
		}

		if (menuItem == MENU_TRIPS && currentView != MENU_TRIPS)
		{
			currentView = MENU_TRIPS;
			newFragment = tripsFragment;
		}

		if (menuItem == MENU_MARKS && currentView != MENU_MARKS)
		{
			currentView = MENU_MARKS;
			newFragment = marksFragment;
		}

		if (newFragment != null)
		{
			recordingFragment.clearMap();
			fragmentManager.beginTransaction().replace(R.id.content_frame, newFragment).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).commit();
			//selectMenu(currentView);

			recordButton.setTypeface(menu_font_unselected);
			tripsButton.setTypeface(menu_font_unselected);
			marksButton.setTypeface(menu_font_unselected);

			switch(menuItem)
			{
				case MENU_RECORD:
					recordButton.setTypeface(menu_font_selected);
					break;
				case MENU_TRIPS:
					tripsButton.setTypeface(menu_font_selected);
					break;
				case MENU_MARKS:
					marksButton.setTypeface(menu_font_selected);
					break;
			}
		}

		drawerLayout.closeDrawers();
	}

	/*
		recordButton.setTypeface(menu_font_unselected);
		tripsButton.setTypeface(menu_font_unselected);
		marksButton.setTypeface(menu_font_unselected);
		Log.i(getClass().getName(), String.valueOf(id));

	 */

	@Override
	protected void onPostCreate(Bundle savedInstanceState)
	{
		super.onPostCreate(savedInstanceState);
		drawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
		drawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (drawerToggle.onOptionsItemSelected(item))
		{
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	private void showWelcomeDialog()
	{
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(getResources().getString(R.string.welcome_message))
			   .setCancelable(false).setTitle(getResources().getString(R.string.welcome_title))
			   .setPositiveButton(getResources().getString(R.string.okay), new DialogInterface.OnClickListener() {
				   public void onClick(final DialogInterface dialog, final int id) {
					   startActivity(new Intent(mActivity, UserInfoActivity.class));
				   }
			   });

		final AlertDialog alert = builder.create();
		alert.show();
	}

	public void openHelp()
	{
		Log.i(TAG, "opening help site");
		startActivity(new Intent(
   			Intent.ACTION_VIEW,
   			Uri.parse(getResources().getString(R.string.help_url))
   		));
	}

	public void openUserInfo()
	{
		startActivity(new Intent(this, UserInfoActivity.class));
	}
}