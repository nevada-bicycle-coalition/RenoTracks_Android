package org.nevadabike.renotracks;

import java.util.ArrayList;

import org.nevadabike.renotracks.IconSpinnerAdapter.IconItem;

import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;

public class MarkActivity extends FragmentActivity {
	private GoogleMap map;
	private SupportMapFragment mapFragment;
	private UiSettings mapUiSettings;
	private ImageButton takePhotoButton;

	private Location markLocation;
	private LatLng markLocationLatLng;

	private Spinner markPurposeSpinner;
	private final ArrayList<IconItem> markPurposes = new ArrayList<IconSpinnerAdapter.IconItem>();
	private Button discardButton;
	private Button saveButton;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mark_activity);

		Bundle cmds = getIntent().getExtras();

		if (cmds == null) {
			markLocationLatLng = new LatLng(39.4047471, -119.731715);
		} else {
			markLocation = cmds.getParcelable("position");
			markLocationLatLng = new LatLng(markLocation.getLatitude(), markLocation.getLongitude());
		}

		mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

		map = mapFragment.getMap();
		map.moveCamera(CameraUpdateFactory.newLatLngZoom(markLocationLatLng, 14));

		//Disable interaction with the map
		mapUiSettings = map.getUiSettings();
		mapUiSettings.setAllGesturesEnabled(false);
		mapUiSettings.setZoomControlsEnabled(false);

		takePhotoButton = (ImageButton) findViewById(R.id.take_photo);
		takePhotoButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				dispatchTakePictureIntent();
			}
		});

		markPurposes.add(new IconSpinnerAdapter.IconItem(getResources().getDrawable(R.drawable.ic_launcher), getResources().getString(R.string.marks_0), 0));
		markPurposes.add(new IconSpinnerAdapter.IconItem(getResources().getDrawable(R.drawable.ic_launcher), getResources().getString(R.string.marks_1), 1));
		markPurposes.add(new IconSpinnerAdapter.IconItem(getResources().getDrawable(R.drawable.ic_launcher), getResources().getString(R.string.marks_2), 2));
		markPurposes.add(new IconSpinnerAdapter.IconItem(getResources().getDrawable(R.drawable.ic_launcher), getResources().getString(R.string.marks_3), 3));
		markPurposes.add(new IconSpinnerAdapter.IconItem(getResources().getDrawable(R.drawable.ic_launcher), getResources().getString(R.string.marks_4), 4));
		markPurposes.add(new IconSpinnerAdapter.IconItem(getResources().getDrawable(R.drawable.ic_launcher), getResources().getString(R.string.marks_5), 5));
		markPurposes.add(new IconSpinnerAdapter.IconItem(getResources().getDrawable(R.drawable.ic_launcher), getResources().getString(R.string.marks_6), 6));
		markPurposes.add(new IconSpinnerAdapter.IconItem(getResources().getDrawable(R.drawable.ic_launcher), getResources().getString(R.string.marks_7), 7));
		markPurposes.add(new IconSpinnerAdapter.IconItem(getResources().getDrawable(R.drawable.ic_launcher), getResources().getString(R.string.marks_8), 8));
		markPurposes.add(new IconSpinnerAdapter.IconItem(getResources().getDrawable(R.drawable.ic_launcher), getResources().getString(R.string.marks_9), 9));
		markPurposes.add(new IconSpinnerAdapter.IconItem(getResources().getDrawable(R.drawable.ic_launcher), getResources().getString(R.string.marks_10), 10));
		markPurposes.add(new IconSpinnerAdapter.IconItem(getResources().getDrawable(R.drawable.ic_launcher), getResources().getString(R.string.marks_11), 11));

		markPurposeSpinner = (Spinner) findViewById(R.id.mark_type);
		markPurposeSpinner.setAdapter(new IconSpinnerAdapter(this, markPurposes));

		discardButton = (Button) findViewById(R.id.ButtonDiscard);
		discardButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				finish();
			}
		});

		saveButton = (Button) findViewById(R.id.ButtonSubmit);
		saveButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {

			}
		});
	}

	static final int REQUEST_IMAGE_CAPTURE = 1;

	private void dispatchTakePictureIntent() {
	    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
	    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
	        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
	    }
	}

	private Bitmap markPhoto;

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
	        Bundle extras = data.getExtras();
	        markPhoto = (Bitmap) extras.get("data");
	        takePhotoButton.setImageBitmap(Bitmap.createScaledBitmap(markPhoto, takePhotoButton.getMeasuredHeight(), takePhotoButton.getMeasuredWidth(), true));
	    }
	}
}
