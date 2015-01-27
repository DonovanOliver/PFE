package fr.unice.proxy.proxy.direct;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;

import android.os.AsyncTask;
import android.util.Log;

public class ClientThread extends Thread {
	String response = "";
	String request = "";
	String clair="";

	public ClientThread(String request) {
		this.request = request;
		this.clair= ProxyServerDirect.DecryptBody(this.request);
		Log.d("CLAIR",clair);
		System.out.println("Création du client thread");

	}

	public String getResponseRouteur() {

		return ProxyClientDirect.SecureBody(response);
	}

	public void run() {

		GenerateUrlStr urlStr = new GenerateUrlStr(this.clair);
		String host = urlStr.getHost();
		String path = urlStr.getPath();
		String url = "http://" + host + path + "";

		try {
			// Apache HTTP Reqeust
			/*HttpClient client = new DefaultHttpClient();
			HttpPost post = new HttpPost(url);
			List<NameValuePair> nvList = new ArrayList<NameValuePair>();
			BasicNameValuePair bnvp = new BasicNameValuePair("name", "test");
			// We can add more
			nvList.add(bnvp);
			post.setEntity(new UrlEncodedFormEntity(nvList));

			HttpResponse resp = client.execute(post);
			// We read the response
			InputStream is = resp.getEntity().getContent();
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(is));
			// StringBuilder str = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				response += line + "\n";
				// str.append(line + "\n");
				
			}
			
			is.close();*/
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
	 
			// optional default is GET
			con.setRequestMethod("GET");
	 
			//add request header
			con.setRequestProperty("User-Agent", "Mozilla/5.0");
	 
			int responseCode = con.getResponseCode();
			System.out.println("\nSending 'GET' request to URL : " + url);
			System.out.println("Response Code : " + responseCode);
	 
			BufferedReader in = new BufferedReader(
			        new InputStreamReader(con.getInputStream()));
			String inputLine;
			//StringBuffer response = new StringBuffer();
	 
			while ((inputLine = in.readLine()) != null) {
				response += inputLine + "\n";
			}
			in.close();
			/*ReturnResponseToClient retourClient=new ReturnResponseToClient("0.0.0.0",8000);
			retourClient.execute("azerty");*/
		
			
			// buffer.append(str.toString());
			// Done!
		} catch (Throwable t) {
			t.printStackTrace();
		}

		// return buffer.toString();
		

		Log.d("Reponse du routeur", response);
		Log.d("URL", url);

		System.out.println("Sortie clientThread");

	}
	public class ReturnResponseToClient extends AsyncTask<String,Integer, Void> {

		private String dstAddress;
		private int dstPort;

		ReturnResponseToClient(String addr, int port) {
			dstAddress = addr;
			dstPort = port;
		}

		@Override
		protected Void doInBackground(String... arg0) {
			OutputStream outputStream;
			Socket socket = null;

			try {
				Log.d("RETOUR CLIENT", "RETOURCLIENT");
				 socket = new Socket(dstAddress, dstPort);
				 Log.d("DOINBACKGROUND", socket.getLocalAddress()+" "+socket.getLocalPort());
				 outputStream =  socket.getOutputStream();
	             PrintStream printStream = new PrintStream(outputStream);
	             printStream.print(arg0);
	             printStream.close();
	             Log.d("FIN ENVOI", "FIN ENVOI");

			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				response = "UnknownHostException: " + e.toString();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				response = "IOException: " + e.toString();
			} finally {
				if (socket != null) {
					try {
						socket.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			return null;
		}


	}
	//private class SendTask extends AsyncTask<String, Integer, Void> {
		//@Override
		//protected Void doInBackground(String... params) {
			//byte[] clear = null;
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
			//SecureRequest sr = new SecureRequest();
			//sr.setUserLevel(SecureRequest.USER_LEVEL_BEGINNER);
			//sr.setContentType(SecureRequest.TYPE_TEXT);
			//sr.setContent(params[0].getBytes());
			/*
			 * String psIP = prefs.getString("proxyServerAddress", "127.0.0.1");
			 * 
			 * int psPort = Integer.parseInt(prefs.getString("proxyServerPort",
			 * "8001")); // parse the String to
			 */
			// int
			/*sr.setTarget("0.0.0.0", 8000);
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
