package fr.unice.proxy.proxy.direct;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.impl.client.DefaultHttpClient;
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

import com.google.gson.Gson;


import fr.unice.proxy.components.ContextManager;
import fr.unice.proxy.components.RequestManager;
import fr.unice.proxy.components.security.Integrity;
import fr.unice.proxy.proxy.RequestStructure;
import fr.unice.proxy.serial.Base64;
import fr.unice.proxy.serial.SecurityPreferences;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * This class is used to make the ProxyClient running to answer to the
 * HttpRequests
 * 
 * @author andrei
 * 
 */
public class ProxyClientDirect implements Runnable, HttpRequestHandler {
	private ServerSocket serverSocket;
	private HttpService httpService;
	private HttpParams httpParams;
	BasicHttpProcessor httpproc;
	HttpRequestHandlerRegistry registry;
	private boolean RUNNING = false;

	/**
	 * Constructor for the ProxyClient. Takes one argument: the port number which
	 * will be used to open the server-side socket
	 * 
	 * @param port
	 * @throws IOException
	 */
	public ProxyClientDirect(int port) throws IOException {
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
	 * Method used to start the Proxy Client
	 */
	public synchronized void startProxy() {
		RUNNING = true;
		Log.d("myapps", "Proxy Client Direct running !!!");
		(new Thread(this)).start();
	}

	/**
	 * Method used to stop the Proxy Client
	 */
	public synchronized void stopProxy() {
		RUNNING = false;
		Log.d("myapps","ProxyClientDirect" + "Proxy Client Direct stopping !!!");
		if (serverSocket != null) {
			try {
				Log.d("myapps","ProxyClientDirect" + "Proxy Client Direct stopped !!!");
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
	 * This method is the implementation for HttpRequestHandler, it is used to
	 * handle the HttpRequest and send back the response
	 */
	public void handle(HttpRequest request, HttpResponse response,
			HttpContext context) throws HttpException, IOException {

		Log.d("myapps"," PCD Secure the request ");
		
		byte[] clear = null;
		HttpEntity entityRequest = null, entityResponse = null;
		RequestManager requestManager = new RequestManager();
		SecurityPreferences securityPreferences;
		ContextManager contextManager = ContextManager.getInstance();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(contextManager.getBaseContext());
		Gson gson = new Gson();
		
		// Check if the request has an entity
		if (request instanceof HttpEntityEnclosingRequest) {
			// If it is, get the entity from it
			entityRequest = ((HttpEntityEnclosingRequest) request).getEntity();
			if (request.getFirstHeader("Data Details").getValue().startsWith("1")) {
				// If the content is text
				clear = EntityUtils.toByteArray(entityRequest);
			}
			else if (request.getFirstHeader("Data Details").getValue().startsWith("2")) {
				// If the content is a file
				clear = getDataFromEntity(entityRequest);
			}
			else {
				// Else, send back an error message indicating that the request does not specify the type of data sent
				entityResponse = new StringEntity("Error! Unknown data format!", HTTP.UTF_8);
				response.setEntity(entityResponse);
				return;
			}
		}
		else {
			// Else, send back an error message indicating that the request does not contain any entity
			entityResponse = new StringEntity("Error! The request is empty!", HTTP.UTF_8);
			response.setEntity(entityResponse);
			return;
		}
		
		securityPreferences = gson.fromJson(request.getFirstHeader("Security Preferences").getValue(), SecurityPreferences.class);
		
		requestManager.parse(securityPreferences);
		
		// Get the target address and port number and create a HttpHost based on this info
		String psIP = prefs.getString("proxyServerAddress", "127.0.0.1");
		int psPort = Integer.parseInt(prefs.getString("proxyServerPort", "8001")); // parse the String to int
		HttpHost proxy = new HttpHost(psIP, psPort);

		// Create an Http Client and set the ProxyServer as its proxy
		HttpClient httpClient = new DefaultHttpClient();
		httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
		
		// Create a new Http Request and set its destination the original one
		HttpPost post = new HttpPost(request.getRequestLine().getUri());
		post.setHeader("Action", "Decrypt");
		post.setHeader("Security Preferences", gson.toJson(securityPreferences));
		post.setHeader("Data Details", request.getFirstHeader("Data Details").getValue());
		Log.d("myapps"," ProxyClientDirect SP JSON + "+gson.toJson(securityPreferences));
		String body = null;
		try {
			
			body = requestManager.getSecured(clear, securityPreferences);
			//String bodyS = new String(body, "UTF-8");
			Log.i("myapps"," Proxy Client Direct Secure the request clear " + clear +" body = "+body);
		} catch (Exception e) {
			// Send back an error message to indicate that something went wrong during encryption/signing
			Log.i("myapps"," Proxy Client Direct Errror ");
			entityResponse = new StringEntity("Error! Something went wrong during the encryption/sign process. Please try again later.", HTTP.UTF_8);
			response.setEntity(entityResponse);
			return;
		}
		
		if (securityPreferences.isIntegrity() && !securityPreferences.isAuthenticity()) {
			Log.i("Integrity", "Starting Integrity");
			Integrity integrity = new Integrity();
			integrity.setAlgorithm(securityPreferences.getIntegrityAlgorithm());
			String hash = null;
			try {
				hash = Base64.encodeBytes(integrity.hash(body.getBytes()));
				post.setHeader("Hash", hash);
			} catch (Exception e) {
				Log.i("Integrity", "Failed Integrity");
				// Send back an error message to indicate that something went wrong during hashing
				entityResponse = new StringEntity("Error! Something went wrong during the hashing process. Please try again later.", HTTP.UTF_8);
				response.setEntity(entityResponse);
				return;
			}
			Log.i("Integrity", "Finished Integrity");
		}
		
		// Wrap an entity around the body and attach it to the HttpRequest
		HttpEntity entityToSend = new StringEntity(body, HTTP.UTF_8);
		post.setEntity(entityToSend);
		
		// Execute the request and get back the response
		HttpResponse responseReceived = httpClient.execute(post);
		
		StringBuilder sb = new StringBuilder();
		if (securityPreferences.isConfidentiality()) {
			sb.append("Confidentiality(" + securityPreferences.getConfidentialityAlgorithm() + ")");
		}
		if (securityPreferences.isAuthenticity()) {
			sb.append("Authenticity(" + securityPreferences.getAuthenticityAlgorithm() + ")");
		}
		if (securityPreferences.isIntegrity()) {
			sb.append("Integrity(" + securityPreferences.getIntegrityAlgorithm() + ")");
		}
		if (securityPreferences.isNonrepudiation()) {
			sb.append("NonRepudiation");
		}
		String propApplied = sb.toString();
		Log.i("Prop applied", propApplied);
		
		
		// Pass the response to the sender
		entityResponse = new StringEntity(EntityUtils.toString(responseReceived.getEntity())
				+ "Properties applied: " + propApplied , HTTP.UTF_8);
		response.setEntity(entityResponse);
	}
	
	/**
	 * This function gets the content from the HttpEntity as bytes
	 * @param entity The HttpEntity
	 * @return entity's content
	 * @throws IOException
	 */
	public byte[] getDataFromEntity(HttpEntity entity) throws IOException {
		// wrap the Httpentity into a BufferedHttpEntity
		BufferedHttpEntity buffEntity = new BufferedHttpEntity(entity);
		
		// Create a new temporary file on the disk
		File file = new File(Environment.getExternalStorageDirectory() + File.separator + "temp.txt");
		FileOutputStream fos = new FileOutputStream(file);
		
		// Write the content to the file
		buffEntity.writeTo(fos);
		while (buffEntity.isStreaming()) {
			buffEntity.writeTo(fos);
		}
		
		// Release resources
		fos.close();
		
		// Get the bytes from the file
		byte[] content = getFileBytes(file);
		
		// Delete the temporary file
		if (file.exists()) {
			file.delete();
		}
		return content;
	}
	
	/**
	 * This function retrieves the content of a file in a byte array
	 * @param file The file to be read
	 * @return file's content
	 * @throws IOException
	 */
	private byte[] getFileBytes(File file) throws IOException {
	    ByteArrayOutputStream ous = null;
	    InputStream ios = null;
	    try {
	        byte[] buffer = new byte[4096];
	        ous = new ByteArrayOutputStream();
	        ios = new FileInputStream(file);
	        int read = 0;
	        while ((read = ios.read(buffer)) != -1)
	            ous.write(buffer, 0, read);
	    } finally {
	        try {
	            if (ous != null)
	                ous.close();
	        } catch (IOException e) {
	            // swallow, since not that important
	        }
	        try {
	            if (ios != null)
	                ios.close();
	        } catch (IOException e) {
	            // swallow, since not that important
	        }
	    }
	    return ous.toByteArray();
	}
	
	

	/**
	 * This method is the implementation for HttpRequestHandler, it is used to
	 * handle the HttpRequest and send back the response
	 */
	static public String SecureBody(String ClearMessage) {

		Log.d("myapps"," PCD Secure Body ");
		
		byte[] clear = null;
		//HttpEntity entityRequest = null, entityResponse = null;
		RequestManager requestManager = new RequestManager();
		SecurityPreferences securityPreferences;
		ContextManager contextManager = ContextManager.getInstance();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(contextManager.getBaseContext());
		Gson gson = new Gson();
		
		/* Get security Preferences We have to change it manual*/
		
		// Create a new SecureRequest and set the user level as beginner
		securityPreferences = new SecurityPreferences();
		securityPreferences.setUserLevel(securityPreferences.USER_LEVEL_BEGINNER);

		
		// Get the security level and the data type and set them to the security preferences
		String levelSelected = "Level 2";
		int level = Integer.parseInt(levelSelected.substring(levelSelected.length()-1)); // Get only the level digit and parse to int
		securityPreferences.setSecurityLevel(level);
		
		String dataType = "Personal";
		securityPreferences.setDataType(dataType);
		//securityPreferences.send();
		//securityPreferences = gson.fromJson(request.getFirstHeader("Security Preferences").getValue(), SecurityPreferences.class);
		
		requestManager.parse(securityPreferences);
		
		//Log.d("myapps"," ProxyClientDirect SP JSON  "+gson.toJson(securityPreferences));
		String SecureMessage = null;
		try {
			
			SecureMessage = requestManager.getSecured(ClearMessage.getBytes(Charset.forName("UTF-8")), securityPreferences);
			//String bodyS = new String(body, "UTF-8");
			Log.i("myapps"," Proxy Client Direct Secure the request clear " + clear +" body = "+SecureMessage);
		} catch (Exception e) {
			// Send back an error message to indicate that something went wrong during encryption/signing
			Log.i("myapps"," Proxy Client Direct Errror ");
			return null;
		}
		
		/*if (securityPreferences.isIntegrity() && !securityPreferences.isAuthenticity()) {
			Log.i("Integrity", "Starting Integrity");
			Integrity integrity = new Integrity();
			integrity.setAlgorithm(securityPreferences.getIntegrityAlgorithm());
			String hash = null;
			try {
				hash = Base64.encodeBytes(integrity.hash(SecureMessage.getBytes()));
				post.setHeader("Hash", hash);
			} catch (Exception e) {
				Log.i("Integrity", "Failed Integrity");
				// Send back an error message to indicate that something went wrong during hashing
				return;
			}
			Log.i("Integrity", "Finished Integrity");
		}*/
		
		// Wrap an entity around the body and attach it to the HttpRequest
		//HttpEntity entityToSend = new StringEntity(body, HTTP.UTF_8);
		//post.setEntity(entityToSend);
		
		// Execute the request and get back the response
		//HttpResponse responseReceived = httpClient.execute(post);
		
		StringBuilder sb = new StringBuilder();
		if (securityPreferences.isConfidentiality()) {
			sb.append("Confidentiality(" + securityPreferences.getConfidentialityAlgorithm() + ")");
		}
		if (securityPreferences.isAuthenticity()) {
			sb.append("Authenticity(" + securityPreferences.getAuthenticityAlgorithm() + ")");
		}
		if (securityPreferences.isIntegrity()) {
			sb.append("Integrity(" + securityPreferences.getIntegrityAlgorithm() + ")");
		}
		if (securityPreferences.isNonrepudiation()) {
			sb.append("NonRepudiation");
		}
		String propApplied = sb.toString();
		Log.i("Prop applied", propApplied);
		
		
		RequestStructure rs = new RequestStructure(securityPreferences,SecureMessage,propApplied) ;
		String Jrs = gson.toJson(rs);
		
		Log.d("myapps"," PCD RequestStructure JSON  "+Jrs);
		return Jrs;
	}
}