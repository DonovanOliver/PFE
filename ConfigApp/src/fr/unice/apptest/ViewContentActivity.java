package fr.unice.apptest;

import java.io.File;
import fr.unice.apptest.R;
import fr.unice.apptest.webserver.WebServerService;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class ViewContentActivity extends Activity {
	
	private TextView tvContent;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_view_content);
		
		tvContent = (TextView) findViewById(R.id.tvContent);
		showAppropriate();
		
		tvContent.setText(getIntent().getExtras().getString("content"));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	/**
	 * Assign actions to menu items when they are selected
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_quit:
			stopWebService();
			finish();
			break;

		case R.id.action_settings:
			Intent intent = new Intent(getBaseContext(),
					PreferencesActivity.class);
			startActivity(intent);
			break;

		}
		return false;
	}
	
	private void stopWebService() {
		// Stop the WebServerService
		Intent intent = new Intent(ViewContentActivity.this,
				WebServerService.class);
		stopService(intent);
	}
	
	private void showAppropriate() {
		if (getIntent().getExtras().getInt("type") == 1) {
			tvContent.setText(getIntent().getExtras().getString("content"));
		}
		else if (getIntent().getExtras().getInt("type") == 2) {
			String path = getIntent().getExtras().getString("content");
			File file = new File(path);
			Intent intent = new Intent();
			intent.setAction(android.content.Intent.ACTION_VIEW);
			String[] chunks = path.split("\\.");
			String type = chunks[chunks.length - 1];
			Log.i("Path", path);
			Log.i("File type", type);
			intent.setDataAndType(Uri.fromFile(file), android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(type));
			startActivity(intent);	
		}
	}

//	@Override
//	protected void onResume() {
//		showAppropriate();
//		super.onResume();
//	}

}
