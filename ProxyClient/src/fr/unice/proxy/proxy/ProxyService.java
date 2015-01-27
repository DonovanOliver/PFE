package fr.unice.proxy.proxy;


import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.widget.Toast;
import fr.unice.proxy.proxy.direct.ProxyClientDirect;
import fr.unice.proxy.proxy.direct.ProxyServerDirect;
import fr.unice.proxy.proxy.direct.SecureDirectConnection;
import fr.unice.proxy.proxy.distributed.ProxyClientDistributed;
import fr.unice.proxy.proxy.distributed.ProxyServerDistributed;
import fr.unice.proxy.proxy.distributed.SecureServerConnection;
import fr.unice.proxyclient.R;

/**
 * This class is intended to use the service that will be running in the
 * background to listen for HttpRequests
 * 
 * @author andrei
 * 
 */
public class ProxyService extends Service {

	private ProxyClientDistributed pcDistributed = null;
	private ProxyServerDistributed psDistributed = null;
	private ProxyClientDirect pcDirect = null;
	private ProxyServerDirect psDirect = null;
	private SecureServerConnection ssc = SecureServerConnection.getInstance();
	private SecureDirectConnection sdc = SecureDirectConnection.getInstance();
	
	private int pcPort;
	private int psPort;
	
	/**
	 * The system calls this method when another component, such as an activity,
	 * requests that the service be started, by calling startService(). Once
	 * this method executes, the service is started and can run in the
	 * background indefinitely
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		
		// Get the info from the preferences, and then extract the port numbers
		pcPort = Integer.parseInt(prefs.getString("pcPort", "8000"));
		psPort = Integer.parseInt(prefs.getString("psPort", "8001"));

		// Get the boolean indicating the usage of a third-party server (DS)
		boolean useThirdParty = prefs.getBoolean("useThirdParty", false);
		
		if (useThirdParty) {
			// If we work on the dedicated server
			try {
				// Create the ProxyClientDistributed and ProxyServerDistributed
				pcDistributed = new ProxyClientDistributed(pcPort);
				psDistributed = new ProxyServerDistributed(psPort);
				// Start the ProxyClientDistributed and ProxyServerDistributed
				pcDistributed.startProxy();
				psDistributed.startProxy();
				
				Toast.makeText(getApplicationContext(), getString(R.string.startProxyClientService), Toast.LENGTH_LONG).show();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else {
			// If we work locally
			try {
				// Create the ProxyClientDirect and ProxyServerDirect
				pcDirect = new ProxyClientDirect(pcPort);
				psDirect = new ProxyServerDirect(psPort);
				// Start the ProxyClientDirect and ProxyServerDirect
				pcDirect.startProxy();
				psDirect.startProxy();
				Toast.makeText(getApplicationContext(),	getString(R.string.startProxyClientService), Toast.LENGTH_LONG).show();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return START_NOT_STICKY;
	}

	/**
	 * Function called when destroying or stopping the service
	 */
	@Override
	public void onDestroy() {
		Toast.makeText(getApplicationContext(),
				getString(R.string.stopProxyClientService), Toast.LENGTH_LONG)
				.show();
		// Stop the Proxys
		if (pcDistributed != null) {
			pcDistributed.stopProxy();
			pcDistributed = null;
		}
		if (psDistributed != null) {
			psDistributed.stopProxy();
			psDistributed = null;
		}
		if (pcDirect != null) {
			pcDirect.stopProxy();
			pcDirect = null;
		}
		if (psDirect != null) {
			psDirect.stopProxy();
			psDirect = null;
		}
		
		if (ssc != null) ssc = null;
		if (sdc != null) sdc = null;
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
