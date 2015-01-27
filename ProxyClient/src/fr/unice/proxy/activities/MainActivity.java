package fr.unice.proxy.activities;

import fr.unice.proxy.components.ContextManager;
import fr.unice.proxy.components.PolicyEngine;
import fr.unice.proxy.proxy.ProxyService;
import fr.unice.proxy.proxy.direct.SecureDirectConnection;
import fr.unice.proxy.proxy.distributed.SecureServerConnection;
import fr.unice.proxyclient.R;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

/**
 * Activity which displays two buttons for starting and stopping the Proxy
 * Service
 */
public class MainActivity extends Activity {

	// UI references
	private View viewStatus, viewProxy;
	private ProgressBar progressBar;
	private Button btnStart, btnStop;

	/**
	 * Used to retrieve user preferences
	 */
	SharedPreferences prefs;

	/**
	 * Used for getting the context for Non-Activity classes
	 */
	ContextManager contextManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Initialize the database with the activity's context
		PolicyEngine.initializeDatabase(this);

		// Get a Shared Preferences instance to get the user preferences
		prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

		// Get the instance of Contextmanager and set the context to it
		contextManager = ContextManager.getInstance();
		contextManager.setBaseContext(getBaseContext());

		// Add references to the UI components
		viewStatus = findViewById(R.id.view_status);
		viewProxy = findViewById(R.id.view_proxy);

		progressBar = (ProgressBar) findViewById(R.id.progressBar);

		btnStart = (Button) findViewById(R.id.btnStart);
		btnStop = (Button) findViewById(R.id.btnStop);

		// Set a listener for Start button click
		btnStart.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// Get the port number and check to see if it is valid, if not,
				// end this function
				String pcPort = prefs.getString("pcPort", "8000");
				String psPort = prefs.getString("psPort", "8001");
				if (TextUtils.isEmpty(pcPort)
						|| (!TextUtils.isDigitsOnly(pcPort))
						|| TextUtils.isEmpty(psPort)
						|| (!TextUtils.isDigitsOnly(psPort))) {
					Toast.makeText(getApplicationContext(),
							getString(R.string.portNumberInvalid),
							Toast.LENGTH_LONG).show();
					return;
				}

				boolean useThirdParty = prefs.getBoolean("useThirdParty", false);
				boolean isInitiator = prefs.getBoolean("isInitiator", false);
				if (useThirdParty) {
					// if we use the dedicated server, we start the AsyncTask
					// responsible for initializing the secure connexion with
					// the distributed server
					DistributedServerConnexionTask dsct = new DistributedServerConnexionTask();
					dsct.execute();
				} else if (isInitiator) {
					// if we use the direct connexion with the ProxyServer, we start the AsyncTask
					// responsible for initializing the secure connexion with
					// the proxy server
					DirectConnexionTask dct = new DirectConnexionTask();
					dct.execute();
				}
				else {
					startProxy();
				}

			}
		});

		// Set a listener for Stop button click
		btnStop.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// Stop the ProxyClientService
				stopProxy();
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_action_settings:
			Intent intent = new Intent(MainActivity.this,
					PreferencesActivity.class);
			startActivity(intent);
			break;
		case R.id.menu_action_quit:
			finish();
			break;
		}

		return false;
	}

	public void startProxy() {
		// First, we make sure that the service is stopped, by stopping it any way:
		stopProxy();
		 int processId = getApplicationInfo().uid;
	     String excludedUid = String.valueOf(processId);
	     Log.d("myapps","[Process id ] "+excludedUid);
		// Now, we start the Proxy:
		// Create a new intent to start the service
		Intent intent = new Intent(MainActivity.this, ProxyService.class);
		// Start the ProxyClientService using the intent
		startService(intent);
	}
	
	public void stopProxy() {
		// Create a new intent to stop the service
		Intent intent = new Intent(MainActivity.this, ProxyService.class);
		// Start the ProxyClientService using the intent
		stopService(intent);
	}

	public String getMACAddress() {
		WifiManager wifiMan = (WifiManager) this
				.getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiInf = wifiMan.getConnectionInfo();
		return wifiInf.getMacAddress();
	}

	private class DirectConnexionTask extends AsyncTask<Void, Integer, Void> {

		@Override
		protected void onPreExecute() {
			viewProxy.setVisibility(View.GONE);
			viewStatus.setVisibility(View.VISIBLE);
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			progressBar.setProgress(values[0]); // feed the progressbar
		}

		@Override
		protected Void doInBackground(Void... params) {
			// Get the proxy address and ip preferences
			String proxyServerAddress = prefs.getString("proxyServerAddress",
					"127.0.0.1");
			int proxyServerPort = Integer.parseInt(prefs.getString(
					"proxyServerPort", "8001"));

			// Establish the connexion with the proxy server
			SecureDirectConnection sdc = SecureDirectConnection.getInstance();
			try {
				sdc.initialize(proxyServerAddress, proxyServerPort);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			viewProxy.setVisibility(View.VISIBLE);
			viewStatus.setVisibility(View.GONE);
			startProxy();
		}

	}

	private class DistributedServerConnexionTask extends
			AsyncTask<Void, Integer, Void> {

		@Override
		protected void onPreExecute() {
			viewProxy.setVisibility(View.GONE);
			viewStatus.setVisibility(View.VISIBLE);
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			progressBar.setProgress(values[0]); // feed the progressbar
		}

		@Override
		protected Void doInBackground(Void... params) {
			String serverAddress = prefs.getString("dedicatedServerAddress",
					"192.168.1.93"); // get the dedicated server address from
										// preferences
			int serverPort = Integer.parseInt(prefs.getString(
					"dedicatedServerPort", "8004")); // get the dedicates server
														// port from preferences

			// Establish the connexion with the dedicated server
			SecureServerConnection ssc;
			try {
				ssc = SecureServerConnection.createInstance(getMACAddress(),
						serverAddress, serverPort);
				ssc.initialize();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			viewProxy.setVisibility(View.VISIBLE);
			viewStatus.setVisibility(View.GONE);
			startProxy();
		}

	}

}