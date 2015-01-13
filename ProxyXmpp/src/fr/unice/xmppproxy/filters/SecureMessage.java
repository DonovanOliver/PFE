package fr.unice.xmppproxy.filters;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import com.google.gson.Gson;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import fr.unice.proxy.components.ContextManager;
import fr.unice.proxy.components.RequestManager;
import fr.unice.proxy.proxy.RequestStructure;
import fr.unice.proxy.proxy.direct.SecureDirectConnection;
import fr.unice.proxy.serial.SecurityPreferences;
import fr.unice.proxy.serial.SerialData;
import fr.unice.xmppproxy.utils.Base64;
import fr.unice.xmppproxy.utils.FormatData;
import fr.unice.xmppproxy.utils.HTTPConnection;

public class SecureMessage implements IFFilter{
	   public SecureMessage() {}

	    public String process(String data) {
	    	 //Log.i("myapps","--->  "+ data);
	     	if (data.startsWith("<message to=") && (data.contains("from=")) && (data.contains("<body>"))) {
	    	
	            Log.i("myapps","It is a message of the server ");
	        	return processServer(data);
	        	
		        }if (data.startsWith("<message") && (!data.contains("from=")) && (data.contains("<body>"))) {   
	        	Log.i("myapps","It is a message of the Client ");
	        	return processClient(data);
	        } else return data;
	    }

	    private String processServer(String data) {
	        String regex = "(?=<body>).*(?<=</body>)";
	        String message = data.substring(data.lastIndexOf("<body>"),data.indexOf("</body>"));
	        String plain = "<body>"+DecryptBody(message)+"</body>";
	        return data.replaceAll(regex, plain);
	    }

	    private String processClient(String data) {
	        String regex = "(?=<body>).*(?<=</body>)";
	        String message = data.substring(data.lastIndexOf("<body>"),data.indexOf("</body>"));
	        String plain = "<body>"+SecureBody(message)+"</body>";
	        return data.replaceAll(regex, plain);
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
			securityPreferences.readSecuPref();
			/*
			securityPreferences.setUserLevel(securityPreferences.USER_LEVEL_BEGINNER);

			
			// Get the security level and the data type and set them to the security preferences
			String levelSelected = "Level 2";
			int level = Integer.parseInt(levelSelected.substring(levelSelected.length()-1)); // Get only the level digit and parse to int
			securityPreferences.setSecurityLevel(level);
			
			String dataType = "Personal";
			securityPreferences.setDataType(dataType);
			//securityPreferences.send();
			//securityPreferences = gson.fromJson(request.getFirstHeader("Security Preferences").getValue(), SecurityPreferences.class);
			*/
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
