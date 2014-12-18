package fr.unice.apptest.webserver;

import java.io.IOException;

import fr.unice.apptest.R;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

/**
 * This class is intended to use the service that will be running in the
 * background to listen for HttpRequests
 * 
 * @author andrei
 * 
 */
public class WebServerService extends Service {

	private WebServer server = null;
	private int port;

	/**
	 * The system calls this method when another component, such as an activity,
	 * requests that the service be started, by calling startService(). Once
	 * this method executes, the service is started and can run in the
	 * background indefinitely
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		// Get the info from the preferences, and then extract the port number
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		port = Integer.parseInt(prefs.getString("localport", "8002"));
		Toast.makeText(getApplicationContext(),
				getString(R.string.startWebServerService) + port,
				Toast.LENGTH_LONG).show();
		try {
			server = new WebServer(port);
		} catch (IOException e) {
			e.printStackTrace();
		}
		server.startServer();
		return START_STICKY;
	}

	/**
	 * Function called when destroying or stopping the service
	 */
	@Override
	public void onDestroy() {
		Toast.makeText(getApplicationContext(),
				getString(R.string.stopWebServerService), Toast.LENGTH_LONG)
				.show();
		server.stopServer();
		super.onDestroy();

	}

	/**
	 * This function is called when another process tries to bind with the
	 * service. It is mandatory that this function be implemented
	 */
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
