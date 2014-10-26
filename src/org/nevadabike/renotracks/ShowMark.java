package org.nevadabike.renotracks;

import java.io.File;
import java.text.DateFormat;

import org.nevadabike.renotracks.Marks.Mark;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class ShowMark extends Activity
{
	private static final String TAG = "ShowMark";
	private long markId;
	private AlertDialog infoDialog;
	private GoogleMap map;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.show_map);

		Bundle cmds = getIntent().getExtras();
		if (cmds == null)
		{
			return;
		}

		markId = cmds.getLong("showtrip");
		Log.i(TAG, "markId: " + String.valueOf(markId));

		Mark mark = Marks.fetchMark(this, markId);
		Log.i(TAG, "mark: " + mark.toString());

		LatLng markLatLng = new LatLng(mark.latitude, mark.longitude);

		GoogleMap map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();

		map.moveCamera(CameraUpdateFactory.newLatLngZoom(markLatLng, 16));

		map.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(Marks.purposeIcons.get(mark.purpose))).anchor(0.5f, 0.5f).position(markLatLng));

		View markView = getLayoutInflater().inflate(R.layout.show_info, null);

		TextView text1 = (TextView) markView.findViewById(R.id.text1);
		ImageView markImageView = (ImageView) markView.findViewById(R.id.image_view);
		markImageView.setVisibility(View.GONE);

		if (!mark.image_url.isEmpty())
		{
			File imgFile = new File(mark.image_url);

			if(imgFile.exists())
			{
			    Bitmap myBitmap = BitmapFactory.decodeFile(mark.image_url);
			    markImageView.setImageBitmap(myBitmap);
			    markImageView.setVisibility(View.VISIBLE);
			}
		}

		text1.setText(Html.fromHtml(TextUtils.join("<br>", new String[]{
			String.format("<b>" + getString(R.string.date) + "</b> %s", DateFormat.getInstance().format(mark.time)),
			String.format("<b>" + getString(R.string.notes) + "</b> %s", mark.details)
		})));

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setView(markView);
		infoDialog = builder.create();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.show_map, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId())
		{
			case R.id.menu_info:
				infoDialog.show();
				return true;
		}
		return false;
	}
}
