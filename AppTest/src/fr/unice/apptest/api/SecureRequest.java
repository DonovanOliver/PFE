package fr.unice.apptest.api;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import com.google.gson.Gson;

/**
 * This class handles everything for sending a secure HTTP request, based on the user's preferences
 * @author andrei
 *
 */
public class SecureRequest {
	/**
	 * The security preferences that hold the user's details
	 */
	private SecurityPreferences securityPreferences = null;
	
	/**
	 * A HTTP client that executes HTTP Requests
	 */
	private HttpClient httpClient = null;
	
	/**
	 * The HTTP Requst which is used to hold the content and the details
	 */
	private HttpPost post = null;
	
	/**
	 * The target host to whom the HTTP Request is sent
	 */
	private HttpHost target = null;
	
	/**
	 * The HTTP Response received after the HTTP Client executes the HTTP Request
	 */
	private HttpResponse response = null;

	/**
	 * The content to be hold by the request
	 */
	private byte[] content;
	
	/**
	 * The type of the content
	 */
	private String contentType = "";
	
	/**
	 * Static value for Text type
	 */
	public static int TYPE_TEXT = 1;
	
	/**
	 * Static value for File type
	 */
	public static int TYPE_FILE = 2;
	
	/**
	 * Static value for Beginner mode
	 */
	public static int USER_LEVEL_BEGINNER = 1;
	
	/**
	 * Static value for Intermediate mode
	 */
	public static int USER_LEVEL_INTERMEDIATE = 2;
	
	/**
	 * Static value for Advanced mode
	 */
	public static int USER_LEVEL_ADVANCED = 3;
	
	/**
	 * Constructor by default. Takes no parameters
	 */
	public SecureRequest() {
		this.httpClient = new DefaultHttpClient();
		this.post = new HttpPost("/");
		this.securityPreferences = new SecurityPreferences();
	}
	
	/**
	 * Sets the target of the HTTP Client
	 * @param address The target's IP address
	 * @param port The target's port number
	 */
	public void setTarget(String address, int port) {
		this.target = new HttpHost(address, port);
	}
	
	/**
	 * Sets the content type for the HTTP Request
	 * @param contentType
	 */
	public void setContentType(int contentType) {
		this.contentType += contentType;
	}
	
	/**
	 * Sets the content to thte HTTP Request for type TEXT
	 * @param text
	 */
	public void setContent(byte[] text) {
		this.content = text;
		HttpEntity entity = new ByteArrayEntity(this.content);
		this.post.setEntity(entity);
	}
	
	/**
	 * Sets the content to thte HTTP Request for type FILE
	 * @param file
	 * @throws IOException
	 */
	public void setContent(File file) throws IOException {
		this.content = getDataFromFile(file);
		this.contentType += file.getName();
		HttpEntity entity = new ByteArrayEntity(this.content);
		this.post.setEntity(entity);
	}
	
	/**
	 * Sets the user level of the user. Values accepted: <b>USER_LEVEL_BEGINNER</b>, <b>USER_LEVEL_INTERMEDIATE</b>, <b>USER_LEVEL_ADVANCED</b>
	 * @param userLevel
	 */
	public void setUserLevel(int userLevel) {
		securityPreferences.setUserLevel(userLevel);
	}
	
	/**
	 * Sets the security level of the data. Values accepted: <b>1</b>, <b>2</b>, <b>3</b>, <b>4</b>
	 * @param secLevel
	 */
	public void setSecurityLevel(int secLevel) {
		securityPreferences.setSecurityLevel(secLevel);
	}
	
	/**
	 * Sets the data type of the data. Data type (in this context, for security types) accepted values: Personal, Administrative, Medical, Professional, Banking
	 * @param dataType
	 */
	public void setDataType(String dataType) {
		securityPreferences.setDataType(dataType);
	}
	
	/**
	 * Sets the confidentiality property
	 * @param isSet
	 */
	public void setConfidentiality(boolean isSet) {
		securityPreferences.setConfidentiality(isSet);
	}
	
	/**
	 * Sets the confidentiality property along with the desired algorithm
	 * @param isSet
	 * @param algorithm
	 */
	public void setConfidentiality(boolean isSet, String algorithm) {
		securityPreferences.setConfidentiality(isSet);
		securityPreferences.setConfidentialityAlgorithm(algorithm);
	}
	
	/**
	 * Sets the authenticity property
	 * @param isSet
	 */
	public void setAuthenticity(boolean isSet) {
		securityPreferences.setAuthenticity(isSet);
	}
	
	/**
	 * Sets the authenticity property along with the desired algorithm
	 * @param isSet
	 * @param algorithm
	 */
	public void setAuthenticity(boolean isSet, String algorithm) {
		securityPreferences.setAuthenticity(isSet);
		securityPreferences.setAuthenticityAlgorithm(algorithm);
	}
	
	/**
	 * Sets the integrity property
	 * @param isSet
	 */
	public void setIntegrity(boolean isSet) {
		securityPreferences.setIntegrity(isSet);
	}
	
	/**
	 * Sets the integrity property along with the desired algorithm
	 * @param isSet
	 * @param algorithm
	 */
	public void setIntegrity(boolean isSet, String algorithm) {
		securityPreferences.setIntegrity(isSet);
		securityPreferences.setIntegrityAlgorithm(algorithm);
	}
	
	/**
	 * Sets the non repudiation property
	 * @param isSet
	 */
	public void setNonRepudiation(boolean isSet) {
		securityPreferences.setNonrepudiation(isSet);
	}
	
	/**
	 * Sets a proxy to the HTTP Client
	 * @param address The proxy's IP address
	 * @param port The proxy's port number
	 */
	public void setProxy(String address, int port) {
		HttpHost proxy = new HttpHost(address, port);
		httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
	}
	
	/**
	 * Makes the HTTP Client execute the HTTP Request to the target
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public void send() throws ClientProtocolException, IOException {
		Gson gson = new Gson();
		String spHeader = gson.toJson(this.securityPreferences);
		this.post.setHeader("Security Preferences", spHeader);
		this.post.setHeader("Data Details", contentType);
		this.response = httpClient.execute(target, post);
		// Shut down the HttpClient in order to de-allocate resources
		httpClient.getConnectionManager().shutdown();
	}
	
	/**
	 * Gets the response from the request
	 * @return The server's response
	 * @throws ParseException
	 * @throws IOException
	 */
	public String getResponse() throws ParseException, IOException {
		return EntityUtils.toString(response.getEntity());
	}
	
	/**
	 * This function returns the data from a file as a byte array
	 * @param file The file to be read
	 * @return File's content as byte array
	 * @throws IOException
	 */
	private byte[] getDataFromFile(File file) throws IOException {
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

}
