package fr.unice.proxy.proxy.distributed;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import fr.unice.proxy.serial.Base64;
import android.util.Log;

/**
 * This class is meant to store the secret key of a connection with the server
 * for a secure communication.
 * 
 * @author andrei
 * 
 */
public class SecureServerConnection {

	/**
	 * The unique instance of the class. We use lazy loading, because we might
	 * not need this class, should the user decides to do he
	 * encryption/decryption locally.
	 */
	private static SecureServerConnection uniqueInstance;

	/**
	 * The id of the device
	 */
	private String id = null;

	/**
	 * The secret key used to encrypt/decrypt data sent to/from the server
	 */
	private SecretKey secretKey = null;

	/**
	 * The dedicated server address
	 */
	private String serverAddress = null;

	/**
	 * The dedicated server port
	 */
	private int serverPort;
	
	/**
	 * Private constructor. Takes 2 arguments: the <b>id</b> and the <b>Secret
	 * Key</b> which will be used for the connexion.
	 * 
	 * @param id
	 * @param secretKey
	 * @throws NoSuchAlgorithmException
	 */
	private SecureServerConnection(String mac, String address, int port)
			throws NoSuchAlgorithmException {
		String id = hashMAC(mac);
		setId(id);
		setServerAddress(address);
		setServerPort(port);
	}

	/**
	 * Static method for creating the unique instance of the Singleton.
	 * 
	 * @param address
	 *            The IP address of the dedicated server
	 * @param port
	 *            The port number of the dedicated server
	 * 
	 * @return The freshly created unique instance of the
	 *         {@link SecureServerConnection}
	 * @throws NoSuchAlgorithmException
	 */
	public static SecureServerConnection createInstance(String mac,	
			String address, int port) throws NoSuchAlgorithmException {
		uniqueInstance = new SecureServerConnection(mac, address, port);
		return uniqueInstance;

	}

	/**
	 * Static method for getting the unique instance of the Singleton.
	 * 
	 * @return The unique instance of the {@link SecureServerConnection}
	 */
	public static SecureServerConnection getInstance() {
		return uniqueInstance;
	}

	/**
	 * This function handles the entire initialization of the Secret Key
	 * establishment with the dedicated server. This is a 3-phase
	 * initialization. The first phase consists of sending an HttpRequest to the
	 * server with the <b>id</b> in the Header, along with the Action type
	 * (which is Key Exchange). The second phase is receiving from the server
	 * its public key, so it can compute its own pair of public/private keys for
	 * the Diffie-Hellman Key Agreement. The third phase consists of sending its
	 * public key to the server so it can compute the Key Agreement as well.
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidKeySpecException 
	 * @throws InvalidAlgorithmParameterException 
	 * @throws InvalidKeyException 
	 */
	public void initialize() throws ClientProtocolException, IOException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidAlgorithmParameterException, InvalidKeyException {
		HttpClient httpclientPhase1 = new DefaultHttpClient();
		HttpHost host = new HttpHost(getServerAddress(), getServerPort());
		HttpPost postPhase1 = new HttpPost("/");
		
		postPhase1.setHeader("Action", "Key Exchange");
		postPhase1.setHeader("Phase", "1");
		
		// Create a new HttpEntity with the id, then set it to the request
		HttpEntity idEntity = new StringEntity(getId(), HTTP.UTF_8);
		postPhase1.setEntity(idEntity);
		
		Log.i("Phase 1", "Pending");
		
		// Send the HttpPost via the httpclient and get the response
		HttpResponse responsePhase2 = httpclientPhase1.execute(host, postPhase1);
		Log.i("Phase 1", "Completed");
		Log.i("Phase 3", "Starting");
		
		// Recreate the public key based on what we received
		byte[] publicKeyFromServerEncodedBytes = EntityUtils.toByteArray(responsePhase2.getEntity());
		byte[] publicKeyFromServerBytes = Base64.decode(publicKeyFromServerEncodedBytes);
		KeyFactory kf = KeyFactory.getInstance("DH");
		X509EncodedKeySpec x509Spec = new X509EncodedKeySpec(publicKeyFromServerBytes);
		PublicKey publicKeyFromServer = kf.generatePublic(x509Spec);
		
		Log.i("Phase 3", "Reconstructed public key");
		
		// Get the specs of this public key in order to generate our own pair public/private, based on the params from the public key
		DHParameterSpec dhSpec = ((DHPublicKey) publicKeyFromServer).getParams();
		KeyPairGenerator kpg = KeyPairGenerator.getInstance("DH");
		kpg.initialize(dhSpec);
		KeyPair kp = kpg.generateKeyPair();
		PublicKey myPublicKey = kp.getPublic();
		PrivateKey myPrivateKey = kp.getPrivate();
		
		Log.i("Phase 3", "Generated key pair");
		
		// Make the KeyAgreement. We have all we need: our own private key, and the public key received from the server
		KeyAgreement ka = KeyAgreement.getInstance("DH");
		ka.init(myPrivateKey);
		ka.doPhase(publicKeyFromServer, true);
		
		// Generate the shared secret
		byte[] secret = ka.generateSecret();
		
		Log.i("Phase 3", "Generated common secret");
		
		// Sha1-ize the secret, and only get the first 16 bytes (because it needs to be 128 bits)
	    MessageDigest digest = MessageDigest.getInstance("SHA-1");
	    secret = digest.digest(secret);
	    secret = Arrays.copyOf(secret, 16);
	    Log.i("Key bytes", new String(secret));
	    SecretKey secretKey = new SecretKeySpec(secret,"AES");
	    setSecretKey(secretKey);
	    
	    Log.i("Phase 3", "Constructed final secret key and set to the Connexion");
	    
		// Recreate another HttpPost and add the headers to indicate that it is about a key exchange for phase 3
		
		HttpClient httpclientPhase3 = new DefaultHttpClient();
		HttpPost postPhase3 = new HttpPost("/");
		postPhase3.setHeader("Action", "Key Exchange");
		postPhase3.setHeader("Phase", "3");
		postPhase3.setHeader("id", getId());
		
		// Create a new HttpEntity with the public key sent as byte[], then set it to the request
		byte[] myPublicKeyEncoded = Base64.encodeBytesToBytes(myPublicKey.getEncoded());
		HttpEntity mypkEntity = new ByteArrayEntity(myPublicKeyEncoded);
		postPhase3.setEntity(mypkEntity);
		
		Log.i("Phase 3", "Pending");
		
		// Get the response back from the server
		HttpResponse responsePhase4 = httpclientPhase3.execute(host, postPhase3);
		Log.i("Phase 3", "Completed");
		
		String testDecryption = EntityUtils.toString(responsePhase4.getEntity());
		
		Log.i("Post Phase 3", "Got the content");
		
		if (symmetricDecrypt(testDecryption, secretKey).equals("Testing")) {
			Log.i("Decrypted", "Connection established succesfully");
		}
		else {
			Log.i("Decrypted", "Connection failed");
			
		}
	}
	
	
	
	public static String symmetricDecrypt(String text, SecretKey secretKey) {
        Cipher cipher;
        String decrypteddString;
        byte[] encryptText = null;
        try {
            encryptText = Base64.decode(text);
            cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            decrypteddString = new String(cipher.doFinal(encryptText));
        } catch (Exception e) {
            e.printStackTrace();
            return "Error";
        }
        return decrypteddString;
    }
	
	/**
	 * This function returns the SHA-1 footprint of a byte[] array
	 * @param text
	 * @return
	 * @throws NoSuchAlgorithmException
	 */
	private byte[] sha1ize(byte[] text) throws NoSuchAlgorithmException {
	    MessageDigest digest = MessageDigest.getInstance("SHA-1");
	    return digest.digest(text);
	}

	/***
	 * This function generates the unique ID of the device, which is basically
	 * the SHA1-ized MAC address
	 * 
	 * @param mac
	 *            The device's MAC address
	 * @return The unique ID
	 * @throws NoSuchAlgorithmException
	 */
	public String hashMAC(String mac) throws NoSuchAlgorithmException {
		byte[] output = sha1ize(mac.getBytes());
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < output.length; i++) {
			sb.append(Integer.toString((output[i] & 0xff) + 0x100, 16)
					.substring(1));
		}
		return sb.toString();
	}

	// ---------------GETTERS and SETTERS----------------------------------

	/**
	 * Returns the Device ID
	 * 
	 * @return The unique Device ID
	 */
	public String getId() {
		return id;
	}

	/**
	 * Sets the device ID (the Hashed MAC address)
	 * 
	 * @param id
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Returns the secret key of the connection.
	 * 
	 * @return the secret key used to communicate securely with the server
	 */
	public SecretKey getSecretKey() {
		return secretKey;
	}

	/**
	 * Sets the secret key of the connection with the <b>secretkey</b>
	 * 
	 * @param secretKey
	 */
	public void setSecretKey(SecretKey secretKey) {
		this.secretKey = secretKey;
	}

	/**
	 * Returns the IP address of the dedicated server
	 * 
	 * @return the server's IP address
	 */
	public String getServerAddress() {
		return serverAddress;
	}

	/**
	 * Sets the IP address of the dedicated server
	 * 
	 * @param serverAddress
	 */
	public void setServerAddress(String serverAddress) {
		this.serverAddress = serverAddress;
	}

	/**
	 * Returns the port number on which the dedicated server is listening
	 * 
	 * @return the server port number
	 */
	public int getServerPort() {
		return serverPort;
	}

	/**
	 * Sets the port number on which the dedicated server is listening
	 * 
	 * @param serverPort
	 */
	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
	}

}
