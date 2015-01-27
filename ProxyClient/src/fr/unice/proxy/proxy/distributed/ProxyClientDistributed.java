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
public class ProxyClientDistributed implements Runnable, HttpRequestHandler {
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
	public ProxyClientDistributed(int port) throws IOException {
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
		Log.d("ProxyClientDistributed", "Proxy Client Distributed running !!!");
		(new Thread(this)).start();
	}

	/**
	 * Method used to stop the Proxy Client
	 */
	public synchronized void stopProxy() {
		RUNNING = false;
		if (serverSocket != null) {
			try {
				Log.d("ProxyClientDistributed", "Proxy Client Districuted stopped !!!");
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

		byte[] clear = null;
		HttpEntity entityRequest = null, entityResponse = null;
		SecurityPreferences securityPreferences;
		SecureServerConnection ssc = SecureServerConnection.getInstance();
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
		
		
		// Get the dedicated server address
		String dsIP = prefs.getString("dedicatedServerAddress", "127.0.0.1");
		int dsPort = Integer.parseInt(prefs.getString("dedicatedServerPort", "8001")); // parse the String to int
		HttpHost dedicatedServer = new HttpHost(dsIP, dsPort);
		
		// Create an Http Client and to use for sending data to the dedicated server
		HttpClient httpClientDS = new DefaultHttpClient();
		HttpPost postDS = new HttpPost("/");
		
		Log.i("id", ssc.getId());
		Log.i("Key", new String(ssc.getSecretKey().getEncoded()));
		postDS.setHeader("id", ssc.getId());
		postDS.setHeader("Action", "Encrypt");
		postDS.setHeader("Security Preferences", request.getFirstHeader("Security Preferences").getValue());
		
		
		byte[] bodyDS = null;
		try {
			byte[] crypted = symmetricEncrypt(clear, ssc.getSecretKey());
			Log.i("I send", new String(crypted, "UTF-8"));
			bodyDS = Base64.encodeBytesToBytes(crypted);
			Log.i("I send", new String(bodyDS, "UTF-8"));
		} catch (Exception e) {
			// Send back an error message to indicate that something went wrong during encryption/signing
			entityResponse = new StringEntity("Error! Something went wrong during the encryption/sign process. Please try again later.", HTTP.UTF_8);
			response.setEntity(entityResponse);
			return;
		}
		
		postDS.setEntity(new ByteArrayEntity(bodyDS));
		
		Log.i("Send to DS", "Sending data to DS for crypting");
		// execute the request to the dedicated server
		HttpResponse responseFromDS = httpClientDS.execute(dedicatedServer, postDS);
		Log.i("Send to DS", "Received crypted data from DS");
		
		String checkIntegrity = responseFromDS.getFirstHeader("CheckIntegrity").getValue();
		
		byte[] contentRsponseFromDS;
		if (request.getFirstHeader("Data Details").getValue().startsWith("1")) {
			// If the content is text
			// Get the content received from the dedicated server
			contentRsponseFromDS = EntityUtils.toByteArray(responseFromDS.getEntity());
		}
		else if (request.getFirstHeader("Data Details").getValue().startsWith("2")) {
			// If the content is a file
			// Get the content received from the dedicated server
			contentRsponseFromDS = getDataFromEntity(responseFromDS.getEntity());
		}
		else {
			// Else, send back an error message indicating that the request does not specify the type of data sent
			entityResponse = new StringEntity("Error! Unknown data format!", HTTP.UTF_8);
			response.setEntity(entityResponse);
			return;
		}
		
		// Get the content received from the dedicated server
		
		// Get the target address and port number of the Proxy Server and create a HttpHost based on this info
		String psIP = prefs.getString("proxyServerAddress", "127.0.0.1");
		int psPort = Integer.parseInt(prefs.getString("proxyServerPort", "8001")); // parse the String to int
		HttpHost proxy = new HttpHost(psIP, psPort);

		// Create an Http Client and set the ProxyServer as its proxy
		HttpClient httpClient = new DefaultHttpClient();
		httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
		
		// Create a new Http Request and set its destination the original one
		HttpPost post = new HttpPost(request.getRequestLine().getUri());
		post.setHeader("Action", "Decrypt");
		post.setHeader("Data Details", request.getFirstHeader("Data Details").getValue());
		
		Log.i("CheckIntegrity", "Checking to see integrity");
		Log.i("CheckIntegrity", checkIntegrity);
		if (checkIntegrity.startsWith("1")) {
			Log.i("Integrity", "Starting Integrity");
			Integrity integrity = new Integrity();
			integrity.setAlgorithm(responseFromDS.getFirstHeader("IntegrityAlgo").getValue());
			String hash = null;
			try {
				hash = Base64.encodeBytes(integrity.hash(contentRsponseFromDS));
				post.setHeader("CheckIntegrity", checkIntegrity);
				Log.i("CheckIntegrity", checkIntegrity);
				post.setHeader("IntegrityAlgo", responseFromDS.getFirstHeader("IntegrityAlgo").getValue());
				post.setHeader("Hash", hash);
			} catch (Exception e) {
				e.printStackTrace();
				Log.i("Integrity", "Failed Integrity");
				// Send back an error message to indicate that something went wrong during hashing
				entityResponse = new StringEntity("Error! Something went wrong during the hashing process. Please try again later.", HTTP.UTF_8);
				response.setEntity(entityResponse);
				return;
			}
			Log.i("Integrity", "Finished Integrity");
		}
		else if (checkIntegrity.startsWith("0")) {
			post.setHeader("CheckIntegrity", checkIntegrity);
		}
		
		// Wrap an entity around the body and attach it to the HttpRequest
		HttpEntity entityToSend = new ByteArrayEntity(contentRsponseFromDS);
		post.setEntity(entityToSend);
		
		// Execute the request and get back the response
		HttpResponse responseReceived = httpClient.execute(post);
	
		// Pass the response to the sender
		entityResponse = new StringEntity(EntityUtils.toString(responseReceived.getEntity()), HTTP.UTF_8);
		response.setEntity(entityResponse);
		
//		// Pass the response to the sender
//		entityResponse = new StringEntity("Done!", HTTP.UTF_8);
//		response.setEntity(entityResponse);
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