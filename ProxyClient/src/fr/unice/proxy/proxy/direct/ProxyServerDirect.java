package fr.unice.proxy.proxy.direct;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecurityPermission;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.http.Header;
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
import org.apache.http.entity.ByteArrayEntity;
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

import fr.unice.proxy.components.RequestManager;
import fr.unice.proxy.components.security.Authenticity;
import fr.unice.proxy.components.security.Confidentiality;
import fr.unice.proxy.components.security.Integrity;
import fr.unice.proxy.proxy.RequestStructure;
import fr.unice.proxy.proxy.direct.SecureDirectConnection;
import fr.unice.proxy.serial.Base64;
import fr.unice.proxy.serial.SecurityPreferences;
import fr.unice.proxy.serial.SerialData;
import android.os.Environment;
import android.util.Log;

/**
 * This class is used to make the ProxyClient running to answer to the
 * HttpRequests
 * 
 * @author andrei
 * 
 */
public class ProxyServerDirect implements Runnable, HttpRequestHandler {
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
	public ProxyServerDirect(int port) throws IOException {
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
		Log.d("myapps","ProxyServerDirect" + "Proxy Server Direct running !!!");
		(new Thread(this)).start();
	}

	/**
	 * Method used to stop the Proxy Client
	 */
	public synchronized void stopProxy() {
		RUNNING = false;
		if (serverSocket != null) {
			try {
				Log.d("myapps","ProxyServerDirect" + "Proxy Server Direct stopped !!!");
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
		HttpEntity entityRequest = null, entityResponse = null;
		RequestManager requestManager = new RequestManager();
		Gson gson = new Gson();
		SecureDirectConnection sdc = SecureDirectConnection.getInstance();
		
		// Get the first header's value of Action in order to see what we must do with this request
		String action = request.getFirstHeader("Action").getValue();
		Log.d("myapps","PSD Get request action "+action);
		// If it's "Key Exchange"
		if (action.equals("Key Exchange")) {
			String phase = request.getFirstHeader("Phase").getValue();
			Log.d("myapps","PCD key exchange starting .... ");	
			// If it's phase "1", generate a keypair, and send back the public key
			if (phase.equals("1")) {
				Log.d("myapps","PCD key exchange starting step 1 ");
				// Check if the request has an entity
				if (request instanceof HttpEntityEnclosingRequest) {
					// If it is, get the entity from it
					entityRequest = ((HttpEntityEnclosingRequest) request).getEntity();
				}
				
				// Get the content from the entity and do the Key Agreement, and then send back the public key
				byte[] contentFromRequest = EntityUtils.toByteArray(entityRequest);
				byte[] contentForResponse;
				try {
					contentForResponse = sdc.finalizeKeyAgreement(contentFromRequest);
					entityResponse = new ByteArrayEntity(contentForResponse);
				} catch (Exception e) {
					e.printStackTrace();
				}
				response.setEntity(entityResponse);
			}
			else if(phase.equals("2")) {
				// Check if the request has an entity
				Log.d("myapps","PCD key exchange starting step 2 ");
				if (request instanceof HttpEntityEnclosingRequest) {
					// If it is, get the entity from it
					entityRequest = ((HttpEntityEnclosingRequest) request).getEntity();
				}
				// Get the content from the entity and do the Key Agreement, and then send back the public key
				byte[] contentFromRequest = EntityUtils.toByteArray(entityRequest);
				byte[] contentForResponse;
				try {
					contentForResponse = sdc.finalizeKeyExchange(contentFromRequest);
					entityResponse = new ByteArrayEntity(contentForResponse);
				} catch (Exception e) {
					e.printStackTrace();
				}
				response.setEntity(entityResponse);
				
			}
			else {
				entityResponse = new StringEntity("Error! Phase unknown!", HTTP.UTF_8);
				response.setEntity(entityResponse);
				return;
			}
			
		}
		
		else if (action.equals("Decrypt")) {
			byte[] content = null;
			// Check if the request has an entity
			if (request instanceof HttpEntityEnclosingRequest) {
				// If it is, get the entity from it
				entityRequest = ((HttpEntityEnclosingRequest) request).getEntity();
				if (request.getFirstHeader("Data Details").getValue().startsWith("1")) {
					// If the content is text
					content = EntityUtils.toByteArray(entityRequest);
				}
				else if (request.getFirstHeader("Data Details").getValue().startsWith("2")) {
					// If the content is a file
					content = getDataFromEntity(entityRequest);
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
			
			SecurityPreferences securityPreferences = gson.fromJson(request.getFirstHeader("Security Preferences").getValue(), SecurityPreferences.class);
			
			if (securityPreferences.isIntegrity() && !securityPreferences.isAuthenticity()) {
				Log.i("Integrity", "Starting Integrity");
				// Get the header that contains the Integrity and decode it from Base64
				byte[] hash = Base64.decode(request.getFirstHeader("Hash").getValue());
				Integrity integrity = new Integrity();
				integrity.setAlgorithm(securityPreferences.getIntegrityAlgorithm());
				boolean integrityValid = false;
				try {
					integrityValid = integrity.verifyHash(content, hash);
				} catch (Exception e) {
					Log.i("Integrity", "Failed Integrity");
					e.printStackTrace();
				}
				if (!integrityValid) {
					Log.i("Integrity", "Failed Integrity");
					// Send back an error message to indicate that the data was modified during transmission
					entityResponse = new StringEntity("Error! Data integrity check failed!", HTTP.UTF_8);
					response.setEntity(entityResponse);
					return;
				}
				Log.i("Integrity", "Finished Integrity");
			}
			
			// Get the Object from the serialized content
			SerialData sd = gson.fromJson(new String(content), SerialData.class);
			
			byte[] originalText = null;
			try {
				originalText = requestManager.getClear(sd, securityPreferences);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			if (originalText == null) {
				// Send back an error message to indicate that something went wrong during decryption
				entityResponse = new StringEntity("Error! Something went wrong during the decryption/signature verifying process. Please try again later.", HTTP.UTF_8);
				Log.e("myapps","Error! Something went wrong during the decryption/signature verifying process. Please try again later.");
				response.setEntity(entityResponse);
				return;
			}
			Log.i("myapps"," ORIGINAL TEST IS " + new String(originalText, "UTF-8"));
			// Create an Http Client
			HttpClient httpClient = new DefaultHttpClient();
			
			// Create a new Http Request and set its destination the original one
			HttpPost post = new HttpPost(request.getRequestLine().getUri());
			post.setHeader("Data Details", request.getFirstHeader("Data Details").getValue());
			
			// Create a new Http Entity with the original text, and attach it to the Http Request
			HttpEntity entityToSend = new ByteArrayEntity(originalText);
			post.setEntity(entityToSend);
			
			// Execute the request and get back the response
			HttpResponse responseReceived = httpClient.execute(post);
			
			// Pass the response to the sender
			entityResponse = new StringEntity(EntityUtils.toString(responseReceived.getEntity()), HTTP.UTF_8);
			response.setEntity(entityResponse);
		}
		
		else {
			entityResponse = new StringEntity("Error! Request unknown!", HTTP.UTF_8);
			response.setEntity(entityResponse);
		}	
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
	static public String DecryptBody(String recievedbody) {
		//HttpEntity entityRequest = null, entityResponse = null;
		RequestManager requestManager = new RequestManager();
		Gson gson = new Gson();
		SecureDirectConnection sdc = SecureDirectConnection.getInstance();
		
		// Get the first header's value of Action in order to see what we must do with this request
		//"Decrypt" 
	

		RequestStructure SecStruc = gson.fromJson(recievedbody, RequestStructure.class);
			
			/*if (SecStruc.getSecurityPreferences().isIntegrity() && !SecStruc.getSecurityPreferences().isAuthenticity()) {
				Log.i("Integrity", "Starting Integrity");
				// Get the header that contains the Integrity and decode it from Base64
				byte[] hash = Base64.decode(request.getFirstHeader("Hash").getValue());
				Integrity integrity = new Integrity();
				integrity.setAlgorithm(securityPreferences.getIntegrityAlgorithm());
				boolean integrityValid = false;
				try {
					integrityValid = integrity.verifyHash(content, hash);
				} catch (Exception e) {
					Log.i("Integrity", "Failed Integrity");
					e.printStackTrace();
				}
				if (!integrityValid) {
					Log.i("Integrity", "Failed Integrity");
					// Send back an error message to indicate that the data was modified during transmission
					entityResponse = new StringEntity("Error! Data integrity check failed!", HTTP.UTF_8);
					response.setEntity(entityResponse);
					return;
				}
				Log.i("Integrity", "Finished Integrity");
			}*/
			
			// Get the Object from the serialized content
			SerialData sd = gson.fromJson(SecStruc.getcryptedmsj(), SerialData.class);
			
			byte[] originalText = null;
			try {
				originalText = requestManager.getClear(sd, SecStruc.getSecurityPreferences());
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			if (originalText == null) {
				// Send back an error message to indicate that something went wrong during decryption
				//entityResponse = new StringEntity("Error! Something went wrong during the decryption/signature verifying process. Please try again later.", HTTP.UTF_8);
				Log.e("myapps","Error! Something went wrong during the decryption/signature verifying process. Please try again later.");
				//response.setEntity(entityResponse);
				return null;
			
			}
			
			String orig = null;
			try {
				orig = new String(originalText, "UTF-8");
				Log.i("myapps"," ORIGINAL TEST IS " + orig );
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return orig;
			// Create an Http Client
			//HttpClient httpClient = new DefaultHttpClient();
			
			// Create a new Http Request and set its destination the original one
			/*HttpPost post = new HttpPost(request.getRequestLine().getUri());
			post.setHeader("Data Details", request.getFirstHeader("Data Details").getValue());
			
			// Create a new Http Entity with the original text, and attach it to the Http Request
			HttpEntity entityToSend = new ByteArrayEntity(originalText);
			post.setEntity(entityToSend);
			
			// Execute the request and get back the response
			HttpResponse responseReceived = httpClient.execute(post);
			
			// Pass the response to the sender
			entityResponse = new StringEntity(EntityUtils.toString(responseReceived.getEntity()), HTTP.UTF_8);
			response.setEntity(entityResponse);*/

	}
	
	
	
	

}