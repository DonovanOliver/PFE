package fr.unice.proxy.proxy.distributed;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

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

import fr.unice.proxy.components.ContextManager;
import fr.unice.proxy.components.RequestManager;
import fr.unice.proxy.components.security.Integrity;
import fr.unice.proxy.proxy.direct.SecureDirectConnection;
import fr.unice.proxy.proxy.distributed.SecureServerConnection;
import fr.unice.proxy.serial.Base64;
import fr.unice.proxy.serial.SecurityPreferences;
import fr.unice.proxy.serial.SerialData;
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
public class ProxyServerDistributed implements Runnable, HttpRequestHandler {
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
	public ProxyServerDistributed(int port) throws IOException {
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
		Log.d("ProxyServerDistributed", "Proxy Server Distributed running !!!");
		(new Thread(this)).start();
	}

	/**
	 * Method used to stop the Proxy Client
	 */
	public synchronized void stopProxy() {
		RUNNING = false;
		if (serverSocket != null) {
			try {
				Log.d("ProxyServerDistributed", "Proxy Server Distributed stopped !!!");
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
		SecureServerConnection ssc = SecureServerConnection.getInstance();
		ContextManager contextManager = ContextManager.getInstance();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(contextManager.getBaseContext());
		RequestManager requestManager = new RequestManager();
		Gson gson = new Gson();
		SecureDirectConnection sdc = SecureDirectConnection.getInstance();
		
		// Get the first header's value of Action in order to see what we must do with this request
		String action = request.getFirstHeader("Action").getValue();
		String checkIntegrity;
		
		if (action.equals("Decrypt")) {
			byte[] content = null;
			// Check if the request has an entity
			if (request instanceof HttpEntityEnclosingRequest) {
				// If it is, get the entity from it
				checkIntegrity = request.getFirstHeader("CheckIntegrity").getValue();
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
			
			
			if (checkIntegrity.startsWith("1")) {
				Log.i("CheckIntegrity", checkIntegrity);
				Log.i("Integrity", "Starting Integrity");
				// Get the header that contains the Integrity and decode it from Base64
				byte[] hash = Base64.decode(request.getFirstHeader("Hash").getValue());
				Integrity integrity = new Integrity();
				integrity.setAlgorithm(request.getFirstHeader("IntegrityAlgo").getValue());
				boolean integrityValid = false;
				try {
					integrityValid = integrity.verifyHash(content, hash);
				} catch (Exception e) {
					e.printStackTrace();
					Log.i("Integrity", "Failed Integrity");
					// Send back an error message to indicate that something went wrong during hashing
					entityResponse = new StringEntity("Error! Something went wrong during the hash verifying process. Please try again later.", HTTP.UTF_8);
					response.setEntity(entityResponse);
					return;
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
			
			// Get the dedicated server address
			String dsIP = prefs.getString("dedicatedServerAddress", "127.0.0.1");
			int dsPort = Integer.parseInt(prefs.getString("dedicatedServerPort", "8001")); // parse the String to int
			HttpHost dedicatedServer = new HttpHost(dsIP, dsPort);
			
			// Create an Http Client and to use for sending data to the dedicated server
			HttpClient httpClientDS = new DefaultHttpClient();
			HttpPost postDS = new HttpPost("/");
			
			Log.i("id", ssc.getId());
			postDS.setHeader("id", ssc.getId());
			postDS.setHeader("Action", "Decrypt");
			
			postDS.setEntity(new ByteArrayEntity(content));
			
			// execute the request to the dedicated server
			HttpResponse responseFromDS = httpClientDS.execute(dedicatedServer, postDS);
			
			byte[] encryptedContentResponseFromDS;
			if (request.getFirstHeader("Data Details").getValue().startsWith("1")) {
				// If the content is text
				encryptedContentResponseFromDS = EntityUtils.toByteArray(responseFromDS.getEntity());
			}
			else if (request.getFirstHeader("Data Details").getValue().startsWith("2")) {
				// If the content is a file
				encryptedContentResponseFromDS = getDataFromEntity(responseFromDS.getEntity());
			}
			else {
				// Else, send back an error message indicating that the request does not specify the type of data sent
				entityResponse = new StringEntity("Error! Unknown data format!", HTTP.UTF_8);
				response.setEntity(entityResponse);
				return;
			}
			
			byte[] decrypted = null;
			try {
				byte[] decodedFromBase64 = Base64.decode(encryptedContentResponseFromDS);
				decrypted = symmetricDecrypt(decodedFromBase64, ssc.getSecretKey());
				Log.i("I receive", new String(decrypted, "UTF-8"));
			} catch (Exception e) {
				// Send back an error message to indicate that something went wrong during encryption/signing
				entityResponse = new StringEntity("Error! Something went wrong during the encryption/sign process. Please try again later.", HTTP.UTF_8);
				response.setEntity(entityResponse);
				return;
			}
			
			// Create an Http Client
			HttpClient httpClient = new DefaultHttpClient();
			
			// Create a new Http Request and set its destination the original one
			HttpPost post = new HttpPost(request.getRequestLine().getUri());
			post.setHeader("Data Details", request.getFirstHeader("Data Details").getValue());
			
			// Create a new Http Entity with the original text, and attach it to the Http Request
			HttpEntity entityToSend = new ByteArrayEntity(decrypted);
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
	
	private byte[] symmetricEncrypt(byte[] clear, SecretKey sk) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException {
		Cipher aCipher = Cipher.getInstance("AES");
		aCipher.init(Cipher.ENCRYPT_MODE, sk);
		return aCipher.doFinal(clear);

	}

	private byte[] symmetricDecrypt(byte[] crypted, SecretKey sk) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException {
		Cipher aCipher = Cipher.getInstance("AES");
		aCipher.init(Cipher.DECRYPT_MODE, sk);
		return aCipher.doFinal(crypted);
	}

}