package fr.unice.apptest.webserver;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpRequestHandlerRegistry;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;
import org.apache.http.util.EntityUtils;

import fr.unice.apptest.MainActivity;
import android.content.Context;
import android.os.Environment;
import android.util.Log;

/**
 * This class is used to make the Web Server running to answer to the
 * HttpRequests
 * 
 * @author andrei
 * 
 */
public class WebServer implements Runnable, HttpRequestHandler {
	private ServerSocket serverSocket;
	private HttpService httpService;
	private HttpParams httpParams;
	BasicHttpProcessor httpproc;
	HttpRequestHandlerRegistry registry;
	private boolean RUNNING = false;

	/**
	 * Constructor for the Web Server. Takes one argument: the port number which
	 * will be used to open the server-side socket
	 * 
	 * @param port
	 * @throws IOException
	 */
	public WebServer(int port) throws IOException {
		this.serverSocket = new ServerSocket(port);

		// Create a new BasicHttpProcessor
		this.httpproc = new BasicHttpProcessor();
		httpproc.addInterceptor(new ResponseDate());
		httpproc.addInterceptor(new ResponseServer());
		httpproc.addInterceptor(new ResponseContent());
		httpproc.addInterceptor(new ResponseConnControl());

		// Create a registry and bind it to this class in order to treat
		// HttpRequests with the help of the
		// implementated function "handle" defined below
		this.registry = new HttpRequestHandlerRegistry();
		this.registry.register("*", this);

		this.httpService = new HttpService(httpproc,
				new DefaultConnectionReuseStrategy(),
				new DefaultHttpResponseFactory());
		this.httpService.setParams(this.httpParams);
		this.httpService.setHandlerResolver(registry);
	}

	/**
	 * Implementation for the Runnable
	 */
	public void run() {
		try {
			while (RUNNING) {
				Socket socket = this.serverSocket.accept();
				DefaultHttpServerConnection conn = new DefaultHttpServerConnection();
				conn.bind(socket, new BasicHttpParams());
				this.httpService.handleRequest(conn, new BasicHttpContext());
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (HttpException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Method used to start the Web Server
	 */
	public synchronized void startServer() {
		RUNNING = true;
		Log.d("WebServer", "Web Server running !!!");
		(new Thread(this)).start();
	}

	/**
	 * Method used to stop the Web Server
	 */
	public synchronized void stopServer() {
		RUNNING = false;
		if (serverSocket != null) {
			try {
				Log.d("WebServer", "Web Server stopped !!!");
				serverSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Returns the port number used by the socket for listening to connexions
	 * 
	 * @return port number
	 */
	public int getPort() {
		return this.serverSocket.getLocalPort();
	}

	/**
	 * This method is the implementation for HttpRequestHandler, it is used to handle the HttpRequest and send
	 * back the response
	 */
	public void handle(HttpRequest request, HttpResponse response,
			HttpContext context) throws HttpException, IOException {
		String responseBack = "";

		HttpEntity entity = null;
		
		// Check if the request has an entity
		if (request instanceof HttpEntityEnclosingRequest) {
			// If it is, get the entity from it
			entity = ((HttpEntityEnclosingRequest) request).getEntity();
		}
		
		String dataDetails = request.getFirstHeader("Data Details").getValue();
		
		if (dataDetails.startsWith("1")) {
			String data = EntityUtils.toString(entity);
			Log.i("Received Text", data);
			MainActivity.displayNotifText(data);
		}
		
		else if (dataDetails.startsWith("2")) {
			BufferedHttpEntity buffEntity = new BufferedHttpEntity(entity);
			
			/**
			 * Create the file in Download folder in sdcard
			 * The filename will be formed by the path to Download repertory concatenated with the
			 * received filename, which is basically the substring starting at position 1, because the first
			 * character is the type of data (text/file)
			 */
			File file = new File(Environment.getExternalStorageDirectory() 
					+ File.separator + "Download"
					+ File.separator + dataDetails.substring(1));
			FileOutputStream fos = new FileOutputStream(file);
			
			buffEntity.writeTo(fos);
			while (buffEntity.isStreaming()) {
				buffEntity.writeTo(fos);
			}
			
			fos.close();
			Log.i("Received", "ok");
			MainActivity.displayNotifFile(file.getPath());
		}
		
		
		

		
		// Make a simple response back using the data received
		responseBack = "Successfully received!";

		// Create a new entity for the response and then set it
		HttpEntity entityResponse = new StringEntity(responseBack, HTTP.UTF_8);
		response.setEntity(entityResponse);
	}

}