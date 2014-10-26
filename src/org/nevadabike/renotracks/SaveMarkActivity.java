package org.nevadabike.renotracks;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.nevadabike.renotracks.IconSpinnerAdapter.IconItem;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.Marker;

public class SaveMarkActivity extends FragmentActivity
{
	private static final String TAG = "SaveMarkActivity";
	private static final int REQUEST_IMAGE_CAPTURE = 1;

	private ImageButton takePhotoButton;

	private Spinner markPurposeSpinner;
	private final ArrayList<IconItem> markPurposes = new ArrayList<IconSpinnerAdapter.IconItem>();
	private View discardButton;
	private View saveButton;
	private EditText markNoteInput;

	private File outputDir;
	private File outputFile;
	private Uri outputFileURI;

	private int selected_purpose_id = 0;

	private Marker markMarker;

	private Location markLocation;

	DbAdapter mDb;
	private TextView purpose_description;
	private Date now;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mark_activity);

		now = new Date();

		Bundle cmds = getIntent().getExtras();
		if (cmds == null)
		{
			markLocation = new Location("");
			markLocation.setLatitude(0.0d);
			markLocation.setLongitude(0.0d);
			markLocation.setAccuracy(0.0f);
			markLocation.setAltitude(0.0d);
			markLocation.setSpeed(0.0f);
		}
		else
		{
			markLocation = cmds.getParcelable("position");
		}

		mDb = new DbAdapter(this);

		markPurposes.add(new IconSpinnerAdapter.IconItem(0, getResources().getString(R.string.marks_no), -1));
		markPurposes.add(new IconSpinnerAdapter.IconItem(Marks.purposeIcons.get(0), getResources().getString(R.string.marks_0), 0));
		markPurposes.add(new IconSpinnerAdapter.IconItem(Marks.purposeIcons.get(1), getResources().getString(R.string.marks_1), 1));
		markPurposes.add(new IconSpinnerAdapter.IconItem(Marks.purposeIcons.get(2), getResources().getString(R.string.marks_2), 2));
		markPurposes.add(new IconSpinnerAdapter.IconItem(Marks.purposeIcons.get(3), getResources().getString(R.string.marks_3), 3));
		markPurposes.add(new IconSpinnerAdapter.IconItem(Marks.purposeIcons.get(4), getResources().getString(R.string.marks_4), 4));
		markPurposes.add(new IconSpinnerAdapter.IconItem(Marks.purposeIcons.get(5), getResources().getString(R.string.marks_5), 5));
		markPurposes.add(new IconSpinnerAdapter.IconItem(Marks.purposeIcons.get(6), getResources().getString(R.string.marks_6), 6));
		markPurposes.add(new IconSpinnerAdapter.IconItem(Marks.purposeIcons.get(7), getResources().getString(R.string.marks_7), 7));
		markPurposes.add(new IconSpinnerAdapter.IconItem(Marks.purposeIcons.get(8), getResources().getString(R.string.marks_8), 8));
		markPurposes.add(new IconSpinnerAdapter.IconItem(Marks.purposeIcons.get(9), getResources().getString(R.string.marks_9), 9));
		markPurposes.add(new IconSpinnerAdapter.IconItem(Marks.purposeIcons.get(10), getResources().getString(R.string.marks_10), 10));
		markPurposes.add(new IconSpinnerAdapter.IconItem(Marks.purposeIcons.get(11), getResources().getString(R.string.marks_11), 11));

		purpose_description = (TextView) findViewById(R.id.mark_purpose_description);

		markPurposeSpinner = (Spinner) findViewById(R.id.mark_type);
		markPurposeSpinner.setAdapter(new IconSpinnerAdapter(this, markPurposes));
		markPurposeSpinner.setOnItemSelectedListener(new OnItemSelectedListener()
		{
			@Override
			public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id)
			{
				IconSpinnerAdapter.IconItem selected_purpose = markPurposes.get(position);
				selected_purpose_id = selected_purpose.id;
				purpose_description.setText(Html.fromHtml(getResources().getString(Marks.purposeDescriptions.get(selected_purpose_id))));
			}

			@Override
			public void onNothingSelected(AdapterView<?> parentView)
			{
				//Nothing to do here
			}
		});

		discardButton = findViewById(R.id.ButtonDiscard);
		discardButton.setOnClickListener(buttonListeners);

		saveButton = findViewById(R.id.ButtonSubmit);
		saveButton.setOnClickListener(buttonListeners);

		takePhotoButton = (ImageButton) findViewById(R.id.take_photo);
		takePhotoButton.setOnClickListener(buttonListeners);

		markNoteInput = (EditText) findViewById(R.id.mark_note_input);
	}

	private final OnClickListener buttonListeners = new OnClickListener()
	{
		@Override
		public void onClick(View v) {
			switch(v.getId())
			{
				case R.id.take_photo:
					dispatchTakePictureIntent();
					break;
				case R.id.ButtonSubmit:
					saveMark();
					break;
				case R.id.ButtonDiscard:
					finish();
					break;
			}
		}
	};
	private ProgressDialog progressDialog;
	private String outputFileName = "";

	private void dispatchTakePictureIntent()
	{
		Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		// Ensure that there's a camera activity to handle the intent
		if (takePictureIntent.resolveActivity(getPackageManager()) != null)
		{
			try {
				//Create the photo file
				File photoFile = createImageFile();

				// Continue only if the File was successfully created
				if (photoFile != null)
				{
					takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileURI);
					startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}

	private File createImageFile() throws IOException
	{
		outputDir = new File(Environment.getExternalStorageDirectory(), getPackageName());
		if (!outputDir.exists()) outputDir.mkdirs();

		outputFileName = (new SimpleDateFormat("yyyyMMdd_HHmmss").format(now));

		outputFile = new File(outputDir, outputFileName + ".jpg");
		outputFileURI = Uri.fromFile(outputFile);

		return outputFile;
	}

	private void galleryAddPic()
	{
		Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
		mediaScanIntent.setData(outputFileURI);
		sendBroadcast(mediaScanIntent);
	}

	private void setPic()
	{
		// Get the dimensions of the View
		int targetW = takePhotoButton.getWidth();
		int targetH = takePhotoButton.getHeight();

		// Get the dimensions of the bitmap
		BitmapFactory.Options bmOptions = new BitmapFactory.Options();
		bmOptions.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(outputFile.getAbsolutePath(), bmOptions);
		int photoW = bmOptions.outWidth;
		int photoH = bmOptions.outHeight;

		// Determine how much to scale down the image
		int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

		// Decode the image file into a Bitmap sized to fill the View
		bmOptions.inJustDecodeBounds = false;
		bmOptions.inSampleSize = scaleFactor;
		bmOptions.inPurgeable = true;

		Bitmap bitmap = BitmapFactory.decodeFile(outputFile.getAbsolutePath(), bmOptions);
		takePhotoButton.setImageBitmap(bitmap);
		takePhotoButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK)
		{
			galleryAddPic();
			setPic();
		}
	}

	private void saveMark() {
		if (selected_purpose_id == -1) {
			// Oh no!  No mark purpose!
			Toast.makeText(getBaseContext(), getResources().getString(R.string.select_mark_purpose), Toast.LENGTH_SHORT).show();
			return;
		}

		progressDialog = new ProgressDialog(this);
		progressDialog.setMessage("Uploading mark");
		progressDialog.show();

		UploadTask uploadTask = new UploadTask();
		uploadTask.execute();
	}

	private class UploadTask extends AsyncTask<Void, Void, String>
	{
		@Override
		protected String doInBackground(Void... params)
		{
			String result = "";
			try
			{
				JSONObject note = new JSONObject();
				note.put("r", new SimpleDateFormat(Common.TIMESTAMP_FORMAT).format(now)); //recorded timestamp
				note.put("l", markLocation.getLatitude()); //latitude
				note.put("n", markLocation.getLongitude()); //longitude
				note.put("a", markLocation.getAltitude()); //altitude
				note.put("s", markLocation.getSpeed()); //speed
				note.put("t", selected_purpose_id); //note type
				note.put("d", markNoteInput.getText().toString()); //note type
				note.put("h", markLocation.getAccuracy()); //haccuracy
				note.put("v", markLocation.getAccuracy()); //vaccuracy
				note.put("i", outputFileName); //note type

				//Setup the client
				HttpClient client = new DefaultHttpClient();
				HttpPost request = new HttpPost(Common.API_URL);
				MultipartEntityBuilder builder = MultipartEntityBuilder.create();

				//Build the request
				builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
				if (outputFile != null)
				{
					builder.addPart("file", new FileBody(outputFile));
				}

				builder.addPart("device", new StringBody(Common.getDeviceId(getContentResolver())));
				builder.addPart("note", new StringBody(note.toString()));
				builder.addPart("version", new StringBody("4"));

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
		protected void onPostExecute(String result)
		{
			Log.i(TAG, "result: " + result);

			if (result.isEmpty())
			{
				progressDialog.hide();
				Toast.makeText(getBaseContext(), getResources().getString(R.string.mark_upload_error), Toast.LENGTH_SHORT).show();
			}
			else
			{
				progressDialog.hide();

				String image_url = "";
				if (outputFile != null)
				{
					image_url = outputFile.getAbsolutePath();
				}

				mDb.open();
				mDb.insertMark(0, markLocation, selected_purpose_id, markNoteInput.getText().toString(), image_url);
				mDb.close();

				finish();
			}
		}
	}
}
