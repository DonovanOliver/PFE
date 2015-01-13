package fr.unice.proxy.proxy.direct;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
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
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import com.google.gson.Gson;

import fr.unice.proxy.components.security.Confidentiality;
import fr.unice.proxy.serial.Base64;
import fr.unice.proxy.serial.SerialKeys;
import android.util.Log;

/**
 * This class is meant to store the secret keys of a connection with the other
 * proxy for a secure communication.
 * 
 * @author andrei
 * 
 */
public class SecureDirectConnection {

	/**
	 * The unique instance of the class. We use lazy loading, because we might
	 * not need this class, should the user decide to do the
	 * encryption/decryption on a a dedicated server.
	 */
	private static SecureDirectConnection uniqueInstance;

	/**
	 * The secret key that is generated at the end of Phase 1, which is used to
	 * encrypt all sent keys afterwards
	 */
	private SecretKey tempSecretKey = null;

	private SecretKey mySecretKeyAES = null;

	private SecretKey mySecretKeyDES = null;

	private PublicKey myPublicKeyRSA = null;
	private PrivateKey myPrivateKeyRSA = null;

	private PublicKey strangerPublicKeyRSA = null;

	private PublicKey myPublicKeyDSA = null;
	private PrivateKey myPrivateKeyDSA = null;

	private PublicKey strangerPublicKeyDSA = null;

	/**
	 * Private constructor. Do not use it to create an instance. Use {@link
	 * getInstance()} method instead.
	 * 
	 * @throws NoSuchAlgorithmException
	 */
	private SecureDirectConnection() {

	}

	/**
	 * Static method for getting the unique instance of the Singleton.
	 * 
	 * @return The unique instance of the {@link SecureDirectConnection}
	 */
	public static SecureDirectConnection getInstance() {
		if (uniqueInstance == null) {
			uniqueInstance = new SecureDirectConnection();
		}
		return uniqueInstance;
	}

	/**
	 * This function handles the entire initialization of the Secret Key
	 * establishment with another device (most probably the ProxyServer). This
	 * is a 2-phase initialization. The first phase consists of sending an
	 * HttpRequest to the ProxyServer with public key. The second phase is
	 * receiving from the other device its public key, so it can compute its own
	 * pair of public/private keys for the Diffie-Hellman Key Agreement.
	 * 
	 * @throws IOException
	 * @throws ClientProtocolException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeySpecException
	 * @throws InvalidAlgorithmParameterException
	 * @throws InvalidKeyException
	 * @throws NoSuchProviderException 
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 * @throws NoSuchPaddingException 
	 */
	public void initialize(String address, int port) throws NoSuchAlgorithmException, ClientProtocolException, IOException, InvalidKeySpecException, InvalidKeyException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException {
		Gson gson = new Gson();
		HttpHost host = new HttpHost(address, port);

		Log.i("myapps", " SDC AES Generating DH KeyPAir");

		// Generate a KeyPair for DH
		KeyPairGenerator kpgDH = KeyPairGenerator.getInstance("DH");
		kpgDH.initialize(512);
		KeyPair kpDH = kpgDH.generateKeyPair();

		// Get the public and private key
		PublicKey myPublicKey = kpDH.getPublic();
		PrivateKey myPrivateKey = kpDH.getPrivate();

		Log.i("myapps", " SDC AES DH Keypair generated. Got the public key");

		// Get the bytes from the public Key and encode into Base64
		byte[] pkAsBytes = myPublicKey.getEncoded();
		byte[] pkEncoded = Base64.encodeBytesToBytes(pkAsBytes);

		// Create a new HttpClient
		HttpClient httpclientPhase1 = new DefaultHttpClient();
		HttpPost postPhase1 = new HttpPost("/");

		// Create e HttpEntity that containts the public key encoded
		// in Base64
		HttpEntity pkEncodedEntity = new ByteArrayEntity(pkEncoded);

		postPhase1.setHeader("Action", "Key Exchange");
		postPhase1.setHeader("Phase", "1");
		postPhase1.setEntity(pkEncodedEntity);

		Log.i("myapps", " SDC AES Sending the public key to the other side... host "+ address + " port : "+ port);

		// Send the HttpRequest and get the response (The public key
		// from the other side)
		HttpResponse responsePhase1 = httpclientPhase1
				.execute(host, postPhase1);
		byte[] pkInBytes = Base64.decode(EntityUtils.toByteArray(responsePhase1
				.getEntity()));

		Log.i("myapps", " SDC AES Sent the public key and got the result back");

		// Reconstruct the public key based on the bytes received
		KeyFactory kf = KeyFactory.getInstance("DH");
		X509EncodedKeySpec x509Spec = new X509EncodedKeySpec(pkInBytes);
		PublicKey publicKeyFromUser = kf.generatePublic(x509Spec);

		Log.i("myapps", " SDC AES Regenerated the public key. Doing the key agreement...");

		// Make the KeyAgreement. We have all we need: our own
		// private key, and the public key received from the the
		// other side
		KeyAgreement ka = KeyAgreement.getInstance("DH");
		ka.init(myPrivateKey);
		ka.doPhase(publicKeyFromUser, true);

		// Generate the shared secret
		byte[] secret = ka.generateSecret();

		// Sha1-ize the secret, and only get the first 16 bytes
		// (because it needs to be 128 bits)
		secret = sha1ize(secret);
		secret = Arrays.copyOf(secret, 16);
		SecretKey secretKey = new SecretKeySpec(secret, "AES");

		// Set the secret key to the connection
		setTempSecretKey(secretKey);

		Log.i("myapps", " SDC AES Got the shared secret key and set it to the class");

		String qqc = new String(symmetricEncrypt("Hello", getTempSecretKey()));
		Log.i("myapps","Encrypted : "+ qqc);
		Log.i("myapps","Decrypted" + new String(symmetricDecrypt(qqc, getTempSecretKey())));
		Log.i("myapps","AES Secret Key " + secretKey.getEncoded() + " Hash_key "+ new String(
				sha1ize(secretKey.getEncoded()), "UTF-8"));

		// Generate all keys, put them into an SerialKeys object, and then
		// encrypt and serialize the object
		SecretKey aesKey = generateAESKey();
		setMySecretKeyAES(aesKey);

		SecretKey desKey = generateDESKey();
		setMySecretKeyDES(desKey);

		KeyPair kpRSA = generateRSAKeys();
		setMyPublicKeyRSA(kpRSA.getPublic());
		setMyPrivateKeyRSA(kpRSA.getPrivate());

		KeyPair kpDSA = generateDSAKeys();
		setMyPublicKeyDSA(kpDSA.getPublic());
		setMyPrivateKeyDSA(kpDSA.getPrivate());

		SerialKeys sk = new SerialKeys();
		sk.setSecretKeyAES(getMySecretKeyAES().getEncoded());
		sk.setSecretKeyDES(getMySecretKeyDES().getEncoded());
		sk.setPublicKeyRSA(getMyPublicKeyRSA().getEncoded());
		sk.setPublicKeyDSA(getMyPublicKeyDSA().getEncoded());

		String allKeys = gson.toJson(sk);
		byte[] allKeysEncrypted = symmetricEncrypt(allKeys, getTempSecretKey());

		// Create a new HttpClient
		HttpClient httpclientPhase2 = new DefaultHttpClient();
		HttpPost postPhase2 = new HttpPost("/");

		// Create e HttpEntity that containts the public key encoded in Base64
		HttpEntity allKeysEncryptedEntity = new ByteArrayEntity(
				allKeysEncrypted);

		postPhase2.setHeader("Action", "Key Exchange");
		postPhase2.setHeader("Phase", "2");
		postPhase2.setEntity(allKeysEncryptedEntity);

		Log.i("myapps", " SDC all keys Sending all keys....");

		// Send the HttpRequest and get the response (The public keys from the
		// other side)
		HttpResponse responsePhase2 = httpclientPhase2
				.execute(host, postPhase2);
		String otherKeysEncrypted = EntityUtils.toString(responsePhase2
				.getEntity());

		Log.i("myapps", " SDC all keys Received other public keys. Reconstructing...");

		// Decrypt the content received and get the SerialKeys inside
		String otherKeys = new String(symmetricDecrypt(otherKeysEncrypted,
				getTempSecretKey()));
		SerialKeys skBack = gson.fromJson(otherKeys, SerialKeys.class);

		// get the RSA and DSA keys in byte arrays
		byte[] otherRSAInBytes = skBack.getPublicKeyRSA();
		byte[] otherDSAInBytes = skBack.getPublicKeyDSA();

		Log.i("myapps", " SDC all keys Got the bytes. Reconstructing...");

		// Reconstruct the public keys
		KeyFactory kfRSA = KeyFactory.getInstance("RSA");
		X509EncodedKeySpec x509SpecRSA = new X509EncodedKeySpec(otherRSAInBytes);
		KeyFactory kfDSA = KeyFactory.getInstance("DSA");
		X509EncodedKeySpec x509SpecDSA = new X509EncodedKeySpec(otherDSAInBytes);

		Log.i("myapps", " SDC all keys Got the specs...");

		// Set the public keys generated
		setStrangerPublicKeyRSA(kfRSA.generatePublic(x509SpecRSA));
		setStrangerPublicKeyDSA(kfDSA.generatePublic(x509SpecDSA));

		Log.i("myapps", " SDC all keys " + 
				"The public keys that were received are set and ready to go!");

		Log.i("myapps", " SDC My AES Hash " +  new String(sha1ize(getMySecretKeyAES()
				.getEncoded()), "UTF-8"));
		Log.i("myapps", " SDC My DES Hash " +  new String(sha1ize(getMySecretKeyDES()
				.getEncoded()), "UTF-8"));
		Log.i("myapps", " SDC My Pub RSA Hash " +  new String(sha1ize(getMyPublicKeyRSA()
				.getEncoded()), "UTF-8"));
		Log.i("myapps","My Pub DSA Hash" + new String(sha1ize(getMyPublicKeyDSA()
				.getEncoded()), "UTF-8"));
		Log.i("myapps","Other Pub RSA Hash" + new String(
				sha1ize(getStrangerPublicKeyRSA().getEncoded()), "UTF-8"));
		Log.i("myapps","Other Pub DSA Hash"+ new String(
				sha1ize(getStrangerPublicKeyDSA().getEncoded()), "UTF-8"));

	}

	/**
	 * * This function handles the finalization of the Secret Key establishment
	 * with another device. This is a 2-phase initialization. The first phase
	 * consists of receiving an HttpRequest from the ProxyClient with public
	 * key. The second phase is sending back to the other device the public key,
	 * so it can compute its own pair of public/private keys for the
	 * Diffie-Hellman Key Agreement.
	 * 
	 * @param pkBytesSerialized
	 * @return This device's public key
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeySpecException
	 * @throws InvalidAlgorithmParameterException
	 * @throws InvalidKeyException
	 * @throws IOException
	 */
	public byte[] finalizeKeyAgreement(byte[] pkBytesSerialized) {
		byte[] pkCoded = null;

		try {
			// Decode the byte array from Base64
			byte[] pkBytes = Base64.decode(pkBytesSerialized);

			Log.i("myapps", " SDC AES Received pk bytes");

			// Recreate the public key based on what we received
			KeyFactory kf = KeyFactory.getInstance("DH");
			X509EncodedKeySpec x509Spec = new X509EncodedKeySpec(pkBytes);
			PublicKey publicKeyFromOtherDevice = kf.generatePublic(x509Spec);

			Log.i("myapps", " SDC AES Reconstructed public key");

			// Get the specs of this public key in order to generate our own
			// pair public/private, based on the params from the public key
			DHParameterSpec dhSpec = ((DHPublicKey) publicKeyFromOtherDevice)
					.getParams();

			KeyPairGenerator kpgDH = KeyPairGenerator.getInstance("DH");
			kpgDH.initialize(dhSpec);
			KeyPair kp = kpgDH.generateKeyPair();
			PublicKey myPublicKey = kp.getPublic();
			PrivateKey myPrivateKey = kp.getPrivate();

			Log.i("myapps", " SDC AES Generated my own keypair");

			// Make the KeyAgreement. We have all we need: our own private key,
			// and the public key received from the server
			KeyAgreement ka = KeyAgreement.getInstance("DH");
			ka.init(myPrivateKey);
			ka.doPhase(publicKeyFromOtherDevice, true);

			// Generate the shared secret
			byte[] secret = ka.generateSecret();

			// Sha1-ize the secret, and only get the first 16 bytes (because it
			// needs to be 128 bits)
			secret = sha1ize(secret);
			secret = Arrays.copyOf(secret, 16);
			SecretKey secretKey = new SecretKeySpec(secret, "AES");
			setTempSecretKey(secretKey);

			Log.i("myapps", " SDC AES Got the secret key too");

			String qqc = new String(symmetricEncrypt("Hello",
					getTempSecretKey()));
			Log.i("myapps","Encrypted :" + qqc);
			Log.i("myapps","Decrypted :" +
					new String(symmetricDecrypt(qqc, getTempSecretKey())));
			Log.i("myapps","AES Secret Key Hash : "+
					new String(sha1ize(secretKey.getEncoded()), "UTF-8"));

			// Finally, get the bytes from the public key and return it
			// serialized
			pkCoded = Base64.encodeBytesToBytes(myPublicKey.getEncoded());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return pkCoded;
	}

	public byte[] finalizeKeyExchange(byte[] pkBytesSerialized) {
		Gson gson = new Gson();
		String pkCrypted = new String(pkBytesSerialized);

		try {
			// Decode the byte array from Base64 and decrypt the content
			String skString = symmetricDecrypt(pkCrypted, getTempSecretKey());
			SerialKeys sk = gson.fromJson(skString, SerialKeys.class);

			Log.i("myapps", " SDC all keys Decrypted content and got the object inside");

			// Reconstruct the keys received and set them
			SecretKey secretKeyAES = new SecretKeySpec(sk.getSecretKeyAES(),
					"AES"); // reconstruct AES key
			setMySecretKeyAES(secretKeyAES); // set the AES key reconstructed

			Log.i("myapps", " SDC all keys Got the AES Key and reconstructed it");

			SecretKey secretKeyDES = new SecretKeySpec(sk.getSecretKeyDES(),
					"DES"); // reconstruct DES key
			setMySecretKeyDES(secretKeyDES); // set the DES key reconstructed

			Log.i("myapps", " SDC all keys Got the DES Key and reconstructed it");

			KeyFactory kfRSA = KeyFactory.getInstance("RSA");
			X509EncodedKeySpec x509SpecRSA = new X509EncodedKeySpec(
					sk.getPublicKeyRSA()); // reconstruct RSA key
			PublicKey otherpublicKeyRSA = kfRSA.generatePublic(x509SpecRSA);
			setStrangerPublicKeyRSA(otherpublicKeyRSA); // set the RSA public
														// key reconstructed

			Log.i("myapps", " SDC all keys Got the RSA Key and reconstructed it");

			KeyFactory kfDSA = KeyFactory.getInstance("DSA");
			X509EncodedKeySpec x509SpecDSA = new X509EncodedKeySpec(
					sk.getPublicKeyDSA()); // reconstruct DSA key
			PublicKey otherPublicKeyDSA = kfDSA.generatePublic(x509SpecDSA);
			setStrangerPublicKeyDSA(otherPublicKeyDSA); // set the DSA public
														// key reconstructed

			Log.i("myapps", " SDC all keys Got the DSA Key and reconstructed it");

			Log.i("myapps", " SDC all keys Generating my own RSA...");

			// Generate my own KeyPair for RSA
			KeyPair kpRSA = generateRSAKeys();
			setMyPublicKeyRSA(kpRSA.getPublic()); // set my RSA public key
			setMyPrivateKeyRSA(kpRSA.getPrivate()); // set my RSA private key

			Log.i("myapps", " SDC all keys Generating my own DSA...");

			// Generate my own KeyPair for DSA
			KeyPair kpDSA = generateDSAKeys();
			setMyPublicKeyDSA(kpDSA.getPublic());
			setMyPrivateKeyDSA(kpDSA.getPrivate());

			Log.i("myapps", " SDC all keys Putting the keys in the object...");

			// Put the two freshly generated public keys to a SerialKeys object
			// and send return it
			SerialKeys skBack = new SerialKeys();
			skBack.setPublicKeyRSA(kpRSA.getPublic().getEncoded());
			skBack.setPublicKeyDSA(kpDSA.getPublic().getEncoded());

			Log.i("myapps", " SDC all keys " + 
					"Converting object to JSON and encrypting it and sending it back...");

			Log.i("myapps", " SDC My AES Hash " +  new String(sha1ize(getMySecretKeyAES()
					.getEncoded()), "UTF-8"));
			Log.i("myapps", " SDC My DES Hash " +  new String(sha1ize(getMySecretKeyDES()
					.getEncoded()), "UTF-8"));
			Log.i("myapps", " SDC My Pub RSA Hash " +  new String(sha1ize(getMyPublicKeyRSA()
					.getEncoded()), "UTF-8"));
			Log.i("myapps","My Pub DSA Hash"+ new String(sha1ize(getMyPublicKeyDSA()
					.getEncoded()), "UTF-8"));
			Log.i("myapps","Other Pub RSA Hash"+ new String(
					sha1ize(getStrangerPublicKeyRSA().getEncoded()), "UTF-8"));
			Log.i("myapps","Other Pub DSA Hash"+ new String(
					sha1ize(getStrangerPublicKeyDSA().getEncoded()), "UTF-8"));

			// Get the JSON object and encrypt it
			String pkBack = gson.toJson(skBack);
			return symmetricEncrypt(pkBack, getTempSecretKey());

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private SecretKey generateAESKey() throws NoSuchAlgorithmException {
		Log.i("myapps", " SDC KeyGen " +  "Starting AES key generation...");
		KeyGenerator kgAES = KeyGenerator.getInstance("AES");
		kgAES.init(128);
		SecretKey secretKeyAES = kgAES.generateKey();
		Log.i("myapps", " SDC KeyGen " +  "Finished AES key generation...");
		return secretKeyAES;
	}

	private SecretKey generateDESKey() throws NoSuchAlgorithmException {
		Log.i("myapps", " SDC KeyGen " +  "Starting DES key generation...");
		KeyGenerator kgDES = KeyGenerator.getInstance("DES");
		kgDES.init(56);
		SecretKey secretKeyDES = kgDES.generateKey();
		Log.i("myapps", " SDC KeyGen " +  "Finished DES key generation...");
		return secretKeyDES;
	}

	private KeyPair generateRSAKeys() throws NoSuchAlgorithmException {
		Log.i("myapps", " SDC KeyGen " +  "Starting RSA keypair generation...");
		KeyPairGenerator kgRSA = KeyPairGenerator.getInstance("RSA");
		kgRSA.initialize(1024);
		KeyPair kpRSA = kgRSA.generateKeyPair();
		Log.i("myapps", " SDC KeyGen " +  "Finished RSA keypair generation...");
		return kpRSA;
	}

	private KeyPair generateDSAKeys() throws NoSuchAlgorithmException {
		Log.i("myapps", " SDC KeyGen " +  "Starting DSA keypair generation...");
		KeyPairGenerator kgDSA = KeyPairGenerator.getInstance("DSA");
		kgDSA.initialize(1024);
		KeyPair kpDSA = kgDSA.generateKeyPair();
		Log.i("myapps", " SDC KeyGen " +  "Finished DSA keypair generation...");
		return kpDSA;
	}

	public Key getEncryptionKeyForSender(String algorithm) {
		if (algorithm.contains("AES")) {
			return getMySecretKeyAES();
		} else if (algorithm.contains("DES")) {
			return getMySecretKeyDES();
		} else if (algorithm.contains("RSA")) {
			return getStrangerPublicKeyRSA();
		} else {
			return null;
		}
	}

	public Key getDecryptionKeyForReceiver(String algorithm) {
		if (algorithm.contains("AES")) {
			return getMySecretKeyAES();
		} else if (algorithm.contains("DES")) {
			return getMySecretKeyDES();
		} else if (algorithm.contains("RSA")) {
			return getMyPrivateKeyRSA();
		} else {
			return null;
		}
	}

	public Key getSignatureKeyForSender(String algorithm) {
		if (algorithm.contains("RSA")) {
			return getMyPrivateKeyRSA();
		} else if (algorithm.contains("DSA")) {
			return getMyPrivateKeyDSA();
		} else {
			return null;
		}
	}

	public Key getSignatureKeyForReceiver(String algorithm) {
		if (algorithm.contains("RSA")) {
			return getStrangerPublicKeyRSA();
		} else if (algorithm.contains("DSA")) {
			return getStrangerPublicKeyDSA();
		} else {
			return null;
		}
	}

	private byte[] symmetricEncrypt(String text, SecretKey secretKey)
			throws InvalidKeyException, NoSuchAlgorithmException,
			NoSuchPaddingException, IllegalBlockSizeException,
			BadPaddingException, NoSuchProviderException {
		Confidentiality confidentiality = new Confidentiality();
		confidentiality.setSecretKey(secretKey);
		return Base64.encodeBytesToBytes(confidentiality.encrypt(text
				.getBytes()));
	}

	public String symmetricDecrypt(String text, SecretKey secretKey)
			throws IOException, InvalidKeyException, NoSuchAlgorithmException,
			NoSuchPaddingException, IllegalBlockSizeException,
			BadPaddingException, NoSuchProviderException {
		byte[] encryptedText = null;
		Confidentiality confidentiality = new Confidentiality();
		confidentiality.setSecretKey(secretKey);
		encryptedText = Base64.decode(text);
		return new String(confidentiality.decrypt(encryptedText));
	}

	/**
	 * This function returns the SHA-1 footprint of a byte[] array
	 * 
	 * @param text
	 * @return
	 * @throws NoSuchAlgorithmException
	 */
	private byte[] sha1ize(byte[] text) throws NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance("SHA-1");
		return digest.digest(text);
	}

	// ---------------GETTERS and SETTERS----------------------------------

	public SecretKey getTempSecretKey() {
		return tempSecretKey;
	}

	public void setTempSecretKey(SecretKey tempSecretKey) {
		this.tempSecretKey = tempSecretKey;
	}

	public SecretKey getMySecretKeyAES() {
		return mySecretKeyAES;
	}

	public void setMySecretKeyAES(SecretKey mysecretKeyAES) {
		this.mySecretKeyAES = mysecretKeyAES;
	}

	public SecretKey getMySecretKeyDES() {
		return mySecretKeyDES;
	}

	public void setMySecretKeyDES(SecretKey mysecretKeyDES) {
		this.mySecretKeyDES = mysecretKeyDES;
	}

	public PublicKey getMyPublicKeyRSA() {
		return myPublicKeyRSA;
	}

	public void setMyPublicKeyRSA(PublicKey myPublicKeyRSA) {
		this.myPublicKeyRSA = myPublicKeyRSA;
	}

	public PrivateKey getMyPrivateKeyRSA() {
		return myPrivateKeyRSA;
	}

	public void setMyPrivateKeyRSA(PrivateKey myPrivateKeyRSA) {
		this.myPrivateKeyRSA = myPrivateKeyRSA;
	}

	public PublicKey getStrangerPublicKeyRSA() {
		return strangerPublicKeyRSA;
	}

	public void setStrangerPublicKeyRSA(PublicKey strangerPublicKeyRSA) {
		this.strangerPublicKeyRSA = strangerPublicKeyRSA;
	}

	public PublicKey getMyPublicKeyDSA() {
		return myPublicKeyDSA;
	}

	public void setMyPublicKeyDSA(PublicKey myPublicKeyDSA) {
		this.myPublicKeyDSA = myPublicKeyDSA;
	}

	public PrivateKey getMyPrivateKeyDSA() {
		return myPrivateKeyDSA;
	}

	public void setMyPrivateKeyDSA(PrivateKey myPrivateKeyDSA) {
		this.myPrivateKeyDSA = myPrivateKeyDSA;
	}

	public PublicKey getStrangerPublicKeyDSA() {
		return strangerPublicKeyDSA;
	}

	public void setStrangerPublicKeyDSA(PublicKey strangerPublicKeyDSA) {
		this.strangerPublicKeyDSA = strangerPublicKeyDSA;
	}

}
