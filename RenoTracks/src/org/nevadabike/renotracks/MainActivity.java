package org.nevadabike.renotracks;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
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

public class MainActivity extends FragmentActivity {

	private ActionBarDrawerToggle drawerToggle;
	private DrawerLayout drawerLayout;
	private FragmentManager fragmentManager;

	public final static int MENU_RECORD = 1;
	public final static int MENU_TRIPS = 2;
	public final static int MENU_MARKS = 3;
	private int currentView;

	private MenuFragment menuFragment;
	private RecordingFragment recordingFragment;
	private TripsFragment tripsFragment;
	private MarksFragment marksFragment;
	private MainActivity activity;

	public final String PREFS_KEY = "PREFS";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);

		activity = this;

		menuFragment = new MenuFragment();
		recordingFragment = new RecordingFragment();
		tripsFragment = new TripsFragment();
		marksFragment = new MarksFragment();

		fragmentManager = getSupportFragmentManager();
		fragmentManager.beginTransaction().replace(R.id.menu_frame, menuFragment).commit();

		drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close);
		drawerLayout.setDrawerListener(drawerToggle);

		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeButtonEnabled(true);

        SharedPreferences settings = getSharedPreferences(PREFS_KEY, 0);
        if (settings.getAll().isEmpty()) {
        	showWelcomeDialog();
        }
	}

	public void selectMenu(int menuItem) {
		Fragment newFragment = null;
		if (menuItem == MENU_RECORD && currentView != MENU_RECORD) {
			currentView = MENU_RECORD;
			newFragment = recordingFragment;
		}

		if (menuItem == MENU_TRIPS && currentView != MENU_TRIPS) {
			currentView = MENU_TRIPS;
			newFragment = tripsFragment;
		}

		if (menuItem == MENU_MARKS && currentView != MENU_MARKS) {
			currentView = MENU_MARKS;
			newFragment = marksFragment;
		}

		if (newFragment != null) {
			 fragmentManager.beginTransaction().replace(R.id.content_frame, newFragment).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).commit();
			 menuFragment.selectMenu(currentView);
		}

		drawerLayout.closeDrawers();
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		drawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		drawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (drawerToggle.onOptionsItemSelected(item)) {
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onStart() {
		super.onStart();
		Log.i(getClass().getName(), "onStart");
	}

	@Override
	public void onStop() {
		super.onStop();
		Log.i(getClass().getName(), "onStop");
	}

    private void showWelcomeDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getResources().getString(R.string.welcome_message))
               .setCancelable(false).setTitle(getResources().getString(R.string.welcome_title))
               .setPositiveButton(getResources().getString(R.string.okay), new DialogInterface.OnClickListener() {
                   public void onClick(final DialogInterface dialog, final int id) {
                       startActivity(new Intent(activity, UserInfoActivity.class));
                   }
               });

        final AlertDialog alert = builder.create();
        alert.show();
    }

    public void openHelp() {
    	Log.i(getClass().getName(), "opening help site");
    	startActivity(new Intent(
   			Intent.ACTION_VIEW,
   			Uri.parse(getResources().getString(R.string.help_url))
   		));
    }

    public void openUserInfo() {
    	startActivity(new Intent(this, UserInfoActivity.class));
    }
}