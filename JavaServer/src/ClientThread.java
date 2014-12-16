import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class ClientThread extends Thread {
	String addr;
	int port;
	String response = "";
	String request = "";

	public ClientThread(String addr, int port, String request) {
		this.addr = addr;
		this.port = port;
		this.request = request;
		System.out.println("Création du client thread");

	}

	public String getResponseRouteur() {

		return response;
	}

	public void run() {
		Socket socket = null;
		try {
			socket = new Socket(this.addr, this.port);
			/*
			 * ByteArrayOutputStream byteArrayOutputStream = new
			 * ByteArrayOutputStream( 1024); byte[] buffer = new byte[1024];
			 * 
			 * int bytesRead;
			 */
			PrintWriter request = new PrintWriter(socket.getOutputStream());
			GenerateUrlStr urlStr = new GenerateUrlStr(this.request);
			String host = urlStr.getHost();
			String path = urlStr.getPath();
			System.out.println(host + "--------" + path);
			request.print("GET " + path + " HTTP/1.1\r\n" + "Host: " + host
					+ "\r\n"
					+ "Accept-Language: fr,fr-fr;q=0.8,en-us;q=0.5,en;q=0.3"
					+ "\r\n" + "Accept-Encoding: gzip, deflate" + "\r\n"
					+ "Connection: close\r\n\r\n");
			// request.print(this.request);
			request.flush();
			InputStream inStream = socket.getInputStream();
			BufferedReader rd = new BufferedReader(new InputStreamReader(
					inStream));
			String line;
			int i = 0;
		
			while ((line = rd.readLine()) != null) {
				response += line + "\n";
				System.out.println(i++);
				

			}
			String url = "http://"+host+path+"";
			/*DefaultHttpClient httpclient = new DefaultHttpClient();
			if ( useProxy == true ) {
			    HttpHost proxy = new HttpHost(proxyStr, 80, "http");
			    httpclient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
			}
			 
			HttpGet httpget = new HttpGet(url);
			//httpget.addHeader("Authorization", "Basic " + encodedAuth);
			 
			HttpResponse response = httpclient.execute(httpget);*/
			

			/*
			 * while ((bytesRead = inStream.read(buffer)) != -1) {
			 * byteArrayOutputStream.write(buffer, 0, bytesRead); response +=
			 * byteArrayOutputStream.toString("UTF-8");
			 * 
			 * System.out.println("La réponse du routeur=>" + response); }
			 */
			System.out.println(response);
			System.out.println(url);
			
			
			System.out.println("Sortie clientThread");

		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Exception levée ici");

		} finally {
			if (socket != null) {
				try {
					System.out.println("Fermeture socket");
					socket.close();
				} catch (IOException e) { // TODO Auto-generated catch block
					e.printStackTrace();
					System.out.println("Exception dans finally ClientThread");
				}

			}
		}

	}
}
