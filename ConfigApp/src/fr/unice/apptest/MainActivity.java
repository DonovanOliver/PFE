package fr.unice.apptest;

import fr.unice.apptest.R;
import fr.unice.apptest.webserver.ContextManager;
import fr.unice.apptest.webserver.WebServerService;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

/**
 * Public class Activity representing the main entrance of the program. This
 * means that when you start the Application, this is the Activity that will
 * show first.
 * 
 * @author andrei
 * 
 */
public class MainActivity extends Activity {

	Button btnBeginner, btnIntermediate, btnAdvanced, btnSettings, btnExit;
	
	TextView textView;
	
	int state=0;
	
	String[] states={"Hangouts","Browser","HTTP"};
	
	Context context;

	/**
	 * This is the first method that is called when the Activity starts.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		context=this;

		ContextManager cm = ContextManager.getInstance();
		cm.setContext(MainActivity.this);

		startWebService();
		
		Intent myIntent = getIntent();
		
		textView=(TextView)findViewById(R.id.tvTitle);
		state=myIntent.getIntExtra("state", 0);
		textView.setText(states[state]);

		// Get the references to the UI buttons
		btnBeginner = (Button) findViewById(R.id.btnBeginner);
		btnIntermediate = (Button) findViewById(R.id.btnIntermediate);
		btnAdvanced = (Button) findViewById(R.id.btnAdvanced);
		btnSettings = (Button) findViewById(R.id.btnSettings);
		btnExit = (Button) findViewById(R.id.btnExit);

		// When the btnBeginner button is clicked
		btnBeginner.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(getBaseContext(),
						BeginnerActivity.class);
				intent.putExtra("state", state);
				startActivity(intent);

			}
		});

		// When the btnIntermediate button is clicked
		btnIntermediate.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(getBaseContext(),
						IntermediateActivity.class);
				intent.putExtra("state", state);
				startActivity(intent);

			}
		});

		// When the btnAdvanced button is clicked
		btnAdvanced.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(getBaseContext(),
						AdvancedActivity.class);
				intent.putExtra("state", state);
				startActivity(intent);

			}
		});

		// When the btnSettings button is clicked
		btnSettings.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(getBaseContext(),
						PreferencesActivity.class);
				startActivity(intent);

			}
		});

		// When the btnExit button is clicked
		btnExit.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				stopWebService();
				finish();

			}
		});
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

	private boolean isServiceRunning() {
		// Check to see if my service is running
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		// I search through all running services to see if mine is working
		for (RunningServiceInfo service : manager
				.getRunningServices(Integer.MAX_VALUE)) {
			if (WebServerService.class.getName().equals(
					service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	private void startWebService() {
		// If service is not running, then start it
		if (!isServiceRunning()) {
			Intent intent = new Intent(MainActivity.this,
					WebServerService.class);
			startService(intent);
		}
	}
	
	private void stopWebService() {
		// Stop the WebServerService
		Intent intent = new Intent(MainActivity.this,
				WebServerService.class);
		stopService(intent);
	}

	public static void displayNotifText(String content) {

		// Get instance of the ContextManager for accessing the context
		ContextManager cm = ContextManager.getInstance();

		// Get a notification manager to control the notification that we've
		// just created
		NotificationManager mNotificationManager = (NotificationManager) cm
				.getContext().getSystemService(Context.NOTIFICATION_SERVICE);

		mNotificationManager.cancelAll();

		// create a notification builder
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
				cm.getContext());

		mBuilder.setTicker("Message received");
		mBuilder.setContentTitle("New Message");
		mBuilder.setContentText(content);
		mBuilder.setSound(RingtoneManager
				.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
		mBuilder.setAutoCancel(true);
		mBuilder.setSmallIcon(android.R.drawable.stat_notify_more);

		// Creates an explicit intent for going into the ViewContentActivity
		Intent resultIntent = new Intent(cm.getContext(),
				ViewContentActivity.class);
		resultIntent.putExtra("type", 1);
		resultIntent.putExtra("content", content);

		PendingIntent resultPendingIntent = PendingIntent.getActivity(
				cm.getContext(), 0, resultIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);

		mBuilder.setContentIntent(resultPendingIntent);

		// Display notification
		mNotificationManager.notify(0, mBuilder.build());
	}

	public static void displayNotifFile(String path) {

		// Get instance of the ContextManager for accessing the context
		ContextManager cm = ContextManager.getInstance();

		// Get a notification manager to control the notification that we've
		// just created
		NotificationManager mNotificationManager = (NotificationManager) cm
				.getContext().getSystemService(Context.NOTIFICATION_SERVICE);

		mNotificationManager.cancelAll();

		// create a notification builder
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
				cm.getContext());

		mBuilder.setTicker("Message received");
		mBuilder.setContentTitle("New Message");
		mBuilder.setContentText(path);
		mBuilder.setSound(RingtoneManager
				.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
		mBuilder.setAutoCancel(true);
		mBuilder.setSmallIcon(android.R.drawable.stat_notify_more);

		// Creates an explicit intent for going into the ViewContentActivity
		Intent resultIntent = new Intent(cm.getContext(),
				ViewContentActivity.class);
		resultIntent.putExtra("type", 2);
		resultIntent.putExtra("content", path);

		PendingIntent resultPendingIntent = PendingIntent.getActivity(
				cm.getContext(), 0, resultIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);

		mBuilder.setContentIntent(resultPendingIntent);

		// Display notification
		mNotificationManager.notify(0, mBuilder.build());
	}

}
