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
import org.apache.http.util.EntityUtils;
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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

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

	private int selected_purpose_id = 0;
	private Boolean tookPicture = false;

	private Marker markMarker;

	DbAdapter mDb;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mark_activity);

		Bundle cmds = getIntent().getExtras();

		mDb = new DbAdapter(this);

		if (cmds == null) {
			//We don't have a current location, so we can't set up the mark
			return;
		}

		markLocation = cmds.getParcelable("position");
		markLocationLatLng = new LatLng(markLocation.getLatitude(), markLocation.getLongitude());

		mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

		map = mapFragment.getMap();
		map.moveCamera(CameraUpdateFactory.newLatLngZoom(markLocationLatLng, 16));

		//Disable interaction with the map
		mapUiSettings = map.getUiSettings();
		mapUiSettings.setAllGesturesEnabled(false);
		mapUiSettings.setZoomControlsEnabled(false);

		markPurposes.add(new IconSpinnerAdapter.IconItem(0, "", 0));
		markPurposes.add(new IconSpinnerAdapter.IconItem(Marks.purposeIcons.get(R.string.marks_0), getResources().getString(R.string.marks_0), R.string.marks_0));
		markPurposes.add(new IconSpinnerAdapter.IconItem(Marks.purposeIcons.get(R.string.marks_1), getResources().getString(R.string.marks_1), R.string.marks_1));
		markPurposes.add(new IconSpinnerAdapter.IconItem(Marks.purposeIcons.get(R.string.marks_2), getResources().getString(R.string.marks_2), R.string.marks_2));
		markPurposes.add(new IconSpinnerAdapter.IconItem(Marks.purposeIcons.get(R.string.marks_3), getResources().getString(R.string.marks_3), R.string.marks_3));
		markPurposes.add(new IconSpinnerAdapter.IconItem(Marks.purposeIcons.get(R.string.marks_4), getResources().getString(R.string.marks_4), R.string.marks_4));
		markPurposes.add(new IconSpinnerAdapter.IconItem(Marks.purposeIcons.get(R.string.marks_5), getResources().getString(R.string.marks_5), R.string.marks_5));
		markPurposes.add(new IconSpinnerAdapter.IconItem(Marks.purposeIcons.get(R.string.marks_6), getResources().getString(R.string.marks_6), R.string.marks_6));
		markPurposes.add(new IconSpinnerAdapter.IconItem(Marks.purposeIcons.get(R.string.marks_7), getResources().getString(R.string.marks_7), R.string.marks_7));
		markPurposes.add(new IconSpinnerAdapter.IconItem(Marks.purposeIcons.get(R.string.marks_8), getResources().getString(R.string.marks_8), R.string.marks_8));
		markPurposes.add(new IconSpinnerAdapter.IconItem(Marks.purposeIcons.get(R.string.marks_9), getResources().getString(R.string.marks_9), R.string.marks_9));
		markPurposes.add(new IconSpinnerAdapter.IconItem(Marks.purposeIcons.get(R.string.marks_10), getResources().getString(R.string.marks_10), R.string.marks_10));
		markPurposes.add(new IconSpinnerAdapter.IconItem(Marks.purposeIcons.get(R.string.marks_11), getResources().getString(R.string.marks_11), R.string.marks_11));

		markPurposeSpinner = (Spinner) findViewById(R.id.mark_type);
		markPurposeSpinner.setAdapter(new IconSpinnerAdapter(this, markPurposes));
		markPurposeSpinner.setOnItemSelectedListener(new OnItemSelectedListener(){
			@Override
			public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
				IconSpinnerAdapter.IconItem selected_purpose = markPurposes.get(position);
				selected_purpose_id = selected_purpose.id;

				if (markMarker != null) {
					markMarker.remove();
					markMarker = null;
				}
				if (selected_purpose.icon != 0) {
					markMarker = map.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(selected_purpose.icon)).anchor(0.5f, 0.5f).position(markLocationLatLng));
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parentView) {
			}
		});

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

		    	tookPicture = true;
			} catch (IOException e) {
				e.printStackTrace();
			}
	    }
	}

	private void saveMark() {
		if (selected_purpose_id == 0) {
			// Oh no!  No mark purpose!
			Toast.makeText(getBaseContext(), getResources().getString(R.string.select_mark_purpose), Toast.LENGTH_SHORT).show();
			return;
		}

		if (!tookPicture) {
			// Oh no!  No mark purpose!
			Toast.makeText(getBaseContext(), getResources().getString(R.string.take_mark_picture), Toast.LENGTH_SHORT).show();
			return;
		}

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
			String result = "";
			try {
				//Setup the client
				HttpClient client = new DefaultHttpClient();
				HttpPost request = new HttpPost("http://scaryuncledevin.com/upload");
				MultipartEntityBuilder builder = MultipartEntityBuilder.create();

				//Build the request
				builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
				builder.addPart("file", new FileBody(uploadFile));
				request.setEntity(builder.build());

				//Submit the request
				HttpResponse response = client.execute(request);

				//Process out the result and store it for later
				HttpEntity entity = response.getEntity();
				result = EntityUtils.toString(entity);

				//Clear the data and close the connection
				entity.consumeContent();
				client.getConnectionManager().shutdown();
			} catch (Exception e) {
				e.printStackTrace();
			}

			return result;
		}

		@Override
		protected void onPostExecute(String result) {
			if (result.isEmpty()) {
				Toast.makeText(getBaseContext(), getResources().getString(R.string.mark_upload_error), Toast.LENGTH_SHORT).show();
			} else {
				Log.i(getClass().getName(), result);
				mDb.open();
				mDb.insertMark(0, markLocation, selected_purpose_id, "details", "http://scaryuncledevin.com/codeigniter/uploads/camera.tmp");
				mDb.close();

				finish();
			}
		}
	}
}
