package org.nevadabike.renotracks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.nevadabike.renotracks.IconSpinnerAdapter.IconItem;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.location.Location;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.Media;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;

public class SaveMarkActivity extends FragmentActivity {
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

	private File outputDir;
	private File outputFile;
	private Uri outputFileURI;

	DbAdapter mDb;

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
				saveMark();
			}
		});

		takePhotoButton = (ImageButton) findViewById(R.id.take_photo);
		takePhotoButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				dispatchTakePictureIntent();
			}
		});

		outputDir = new File(Environment.getExternalStorageDirectory(), getPackageName());
		if (!outputDir.exists()) outputDir.mkdirs();
		outputFile = new File(outputDir, "camera.tmp");
		outputFileURI = Uri.fromFile(outputFile);

		Log.i(getClass().getName(), outputFileURI.toString());
	}

	static final int REQUEST_IMAGE_CAPTURE = 1;

	private void dispatchTakePictureIntent() {
		Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileURI);
	    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
			try {
				Bitmap captureBmp = Media.getBitmap(getContentResolver(), outputFileURI);
				ExifInterface exif = new ExifInterface(outputFile.getAbsolutePath());

				int rotate, orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

				if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
					Log.i(getClass().getName(), "ORIENTATION_ROTATE_270");
					rotate = 270;
				} else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
					Log.i(getClass().getName(), "ORIENTATION_ROTATE_180");
					rotate = 180;
				} else if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
					Log.i(getClass().getName(), "ORIENTATION_ROTATE_90");
					rotate = 90;
				} else {
					Log.i(getClass().getName(), "ORIENTATION_NORMAL");
					rotate = 0;
				}

				if (rotate > 0) {
					Matrix m = new Matrix();
					m.postRotate(rotate);
					captureBmp = Bitmap.createBitmap(captureBmp, 0, 0, captureBmp.getWidth(), captureBmp.getHeight(), m, true);
				}

				int maxDimen = 1000;
				float sourceWidth = captureBmp.getWidth();
				float sourceHeight = captureBmp.getHeight();

				Log.i(getClass().getName(), "maxDimen: " + String.valueOf(maxDimen));
				Log.i(getClass().getName(), "sourceWidth: " + String.valueOf(sourceWidth));
				Log.i(getClass().getName(), "sourceHeight: " + String.valueOf(sourceHeight));

				if (Math.max(sourceWidth, sourceHeight) > maxDimen) {
					int targetWidth, targetHeight;

					if (sourceWidth > sourceHeight) {
						targetWidth = maxDimen;
						targetHeight = (int) (sourceHeight / sourceWidth * maxDimen);
					} else {
						targetWidth = (int) (sourceWidth / sourceHeight * maxDimen);
						targetHeight = maxDimen;
					}

					Log.i(getClass().getName(), "targetWidth: " + String.valueOf(targetWidth));
					Log.i(getClass().getName(), "targetHeight: " + String.valueOf(targetHeight));

					captureBmp = Bitmap.createScaledBitmap(captureBmp, targetWidth, targetHeight, true);
				}

				FileOutputStream fOut = new FileOutputStream(outputFile);
				captureBmp.compress(Bitmap.CompressFormat.JPEG, 85, fOut);
				fOut.flush();
				fOut.close();

		    	takePhotoButton.setImageBitmap(captureBmp);
		    	takePhotoButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
			} catch (IOException e) {
				e.printStackTrace();
			}
	    }
	}

	private void saveMark() {
		mDb = new DbAdapter(this);

		int type = 0;

		mDb.open();
		mDb.insertMark(0, markLocation, type, "details", "http://scaryuncledevin.com/codeigniter/uploads/camera10.tmp");
		mDb.close();

		UploadTask uploadTask = new UploadTask(outputFile);
		uploadTask.execute();
	}

	class UploadTask extends AsyncTask<Void, Void, String> {

		File uploadFile;

		UploadTask(File uploadFile) {
			this.uploadFile = uploadFile;
		}

		@Override
		protected String doInBackground(Void... params) {
			try {
				HttpClient client = new DefaultHttpClient();
				HttpPost request = new HttpPost("http://scaryuncledevin.com/upload");
				MultipartEntityBuilder builder = MultipartEntityBuilder.create();

				builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
				builder.addPart("file", new FileBody(uploadFile));
				request.setEntity(builder.build());
				HttpResponse response = client.execute(request);
				HttpEntity entity = response.getEntity();

				entity.consumeContent();
				client.getConnectionManager().shutdown();

				finish();
			} catch (Exception e) {
				e.printStackTrace();
			}

			return null;
		}
	}
}
