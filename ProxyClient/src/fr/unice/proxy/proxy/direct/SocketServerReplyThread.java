package fr.unice.proxy.proxy.direct;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import com.google.gson.Gson;
import fr.unice.proxy.components.ContextManager;
import fr.unice.proxy.components.RequestManager;
import fr.unice.proxy.serial.SecurityPreferences;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

public class SocketServerReplyThread extends Thread {

	private Socket hostThreadSocket;
	private InputStream input;
	String response;
	byte[] bytes;

	private short toUnsigned(byte c) {
		if (c >= 0) {
			return c;
		}
		return (short) (c + 256);
	}

	SocketServerReplyThread(Socket socket) {
		hostThreadSocket = socket;
		response = "";
		try {
			this.input = this.hostThreadSocket.getInputStream();
			System.out.println("IP=>"+this.hostThreadSocket.getInetAddress()+"--"+this.hostThreadSocket.getLocalPort());

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		/*
		 * for (int i = 0; i < 256; i++) {
		 * System.out.print(toUnsigned((byte)i)+" "); }
		 */
		try {
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(
					1024);
			byte[] buffer = new byte[1024];
			// System.out.print("Receive bytes:");
			int bytesRead;
			/*
			 * notice: inputStream.read() will block if no data return
			 */
			while ((bytesRead = this.input.read(buffer)) != -1) {
				byteArrayOutputStream.write(buffer, 0, bytesRead);
				response += byteArrayOutputStream.toString("UTF-8");
				if (response.contains("GET")) {
					Log.d("REQUETE", response);
					/*SendTask sendTask=new SendTask();
					sendTask.execute(response);*/
					String crypted = ProxyClientDirect.SecureBody(response);
					Log.d("CRYPTED",""+crypted);
					ClientThread client = new ClientThread(crypted);
					client.run();

					OutputAndroid outAndroid = new OutputAndroid(
							this.hostThreadSocket, ProxyServerDirect.DecryptBody(client.getResponseRouteur()));
					outAndroid.run();

				} 
				else
				{
					if(response.equals("azerty"))
					{
						Log.d("LA REPONSE SERV=>",response);
					}
					else {
						System.out
								.println("La requête ne provient pas du browser");
						break;
					}
				}
				

			}
			System.out.println("Sortie boucle");

		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			response = "UnknownHostException: " + e.toString();
			System.out.println(response);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			response = "IOException: " + e.toString();
			System.out.println(response);
		} finally {
			if (this.hostThreadSocket != null) {
				try {
					System.out.println("Fermeture du socket d'acceptation");
					this.hostThreadSocket.close();

				} catch (IOException e) { // TODO Auto-generated catch block
					System.out.println("Exception SocketServerReplyThread");
					e.printStackTrace();

				}
			}
		}

	}

	/*private class SendTask extends AsyncTask<String, Integer, Void> {
		@Override
		protected Void doInBackground(String... params) {
			byte[] clear = null;
			/*
			 * HttpEntity entityRequest = null, entityResponse = null;
			 * RequestManager requestManager = new RequestManager();
			 * SecurityPreferences securityPreferences; ContextManager
			 * contextManager = ContextManager.getInstance(); SharedPreferences
			 * prefs = PreferenceManager
			 * .getDefaultSharedPreferences(contextManager .getBaseContext());
			 * Gson gson = new Gson(); securityPreferences =
			 * gson.fromJson(params[0], SecurityPreferences.class);
			 * requestManager.parse(securityPreferences);
			 */
			// Get the target address and port number and create a HttpHost
			// based on
			// this info
			/*SecureRequest sr = new SecureRequest();
			sr.setUserLevel(SecureRequest.USER_LEVEL_BEGINNER);
			sr.setContentType(SecureRequest.TYPE_TEXT);
			sr.setContent(params[0].getBytes());*/
			/*
			 * String psIP = prefs.getString("proxyServerAddress", "127.0.0.1");
			 * 
			 * int psPort = Integer.parseInt(prefs.getString("proxyServerPort",
			 * "8001")); // parse the String to
			 */
			// int
			/*sr.setTarget("0.0.0.0", 8001);
			try {
				// Send the SecureRequest
				sr.send();
				// Return the response string that will be passed as a parameter
				// to the onPostExecute method
				// return sr.getResponse();

			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;

		}
	}*/
}