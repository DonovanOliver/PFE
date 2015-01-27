package fr.unice.proxy.components;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import android.util.Log;

import com.google.gson.Gson;

import fr.unice.proxy.components.security.Authenticity;
import fr.unice.proxy.components.security.Confidentiality;
import fr.unice.proxy.components.security.NonRepudiation;
import fr.unice.proxy.proxy.direct.SecureDirectConnection;
import fr.unice.proxy.serial.SecurityPreferences;
import fr.unice.proxy.serial.SerialData;


/**
 * This class is responsible for interacting with the Policy Engine and applying the security properties
 * specified by the latter
 * @author andrei
 *
 */
public class RequestManager {
	
	private PolicyEngine policyEngine = null;
	
	private SecureDirectConnection sdc;
	
	private static String confAlgorithm = "AES";
	private static String authAlgorithm = "SHA1withDSA";
	private static String intAlgorithm = "SHA-1";
	
	
	/**
	 * Public constructor by-default
	 */
	public RequestManager() {
		policyEngine = PolicyEngine.getInstance();
		sdc = SecureDirectConnection.getInstance();
	}

	/**
	 * This method will parse the SecurityPreferences object that is passed as a parameter. If the object has a Beginner Mode, it will call the Policy
	 * Engine to set the properties to the object (wether it's Confidentiality, Authencitity, Integrity and/or Non Repudiation). If the object has
	 * an Intermediate Mode, it will set the algorithms by default to the chosen properties. If the object has an Advanced Mode, it will do nothing because
	 * all the properties and algorithms were already chosen by the user.
	 * @param sp
	 */
	public void parse(SecurityPreferences sp) {
		// If user level is Beginner(level 1)
		if (sp.getUserLevel() == 1) {
			policyEngine.setProperties(sp.getSecurityLevel(), sp.getDataType(), sp);
		}
		// Else if user level is Intermediate(level 2)
		else if (sp.getUserLevel() == 2) {
			if (sp.isConfidentiality()) {
				sp.setConfidentialityAlgorithm(confAlgorithm);
			}
			if (sp.isAuthenticity()) {
				sp.setAuthenticityAlgorithm(authAlgorithm);
			}
			if (sp.isIntegrity()) {
				sp.setIntegrityAlgorithm(intAlgorithm);
			}
		}
		// Else, the user level is Advanced(level 3), so nothing to do
		else {
			return;
		}
	}
	
	/**
	 * This method takes the clear text, and applies the Confidentiality, Authenticity and Non-Repudiation (in this order) to the text,
	 * if those are specified in the SecurityPreferences object passed as a second parameter. The secured data is returned as a string (SerialData
	 * object serialized to a JSON String).
	 * @param clear The clear text to be secured
	 * @param sp The security preferences that hold the properties to apply to the clear text
	 * @return The secured content
	 */
	public String getSecured(byte[] clear, SecurityPreferences sp) {
		Gson gson = new Gson();
		SerialData sdata = new SerialData();
		if (sp.isConfidentiality()) {
			Log.i("Confidentiality", "Starting Confidentiality");
			Confidentiality confidentiality = new Confidentiality();
			if (sp.getConfidentialityAlgorithm().contains("RSA")) {
				confidentiality.setMode(Confidentiality.ASYMMETRIC_MODE);
				confidentiality.setAsymAlgo(sp.getConfidentialityAlgorithm());
				confidentiality.setPublicKey((PublicKey) sdc.getEncryptionKeyForSender(sp.getConfidentialityAlgorithm()));
			}
			else if (sp.getConfidentialityAlgorithm().contains("AES") || sp.getConfidentialityAlgorithm().contains("DES")) {
				confidentiality.setMode(Confidentiality.SYMMETRIC_MODE);
				confidentiality.setSymAlgo(sp.getConfidentialityAlgorithm());
				confidentiality.setSecretKey((SecretKey) sdc.getEncryptionKeyForSender(sp.getConfidentialityAlgorithm()));
			}
			else {
				Log.i("Confidentiality", "Failed Confidentiality");
				return null;
			}
			
			byte[] encrypted = null;
			try {
				encrypted = confidentiality.encrypt(clear);
			} catch (Exception e) {
				e.printStackTrace();
				Log.i("Confidentiality", "Failed Confidentiality");
				return null;
			}
			sdata.setContent(encrypted);
			Log.i("Confidentiality", "Finished Confidentiality");
		}
		else {
			sdata.setContent(clear);
		}
		
		if (sp.isAuthenticity()) {
			Log.i("Authenticity", "Starting Authenticity");
			Authenticity authenticity = new Authenticity();
			authenticity.setAlgorithm(sp.getAuthenticityAlgorithm());
			authenticity.setPrivateKey((PrivateKey) sdc.getSignatureKeyForSender(sp.getAuthenticityAlgorithm()));
			byte[] signature = null;
			try {
				signature = authenticity.sign(sdata.getContent());
			} catch (Exception e) {
				Log.i("Authenticity", "Failed Authenticity");
				e.printStackTrace();
				return null;
			}
			sdata.setSignature(signature);
			Log.i("Authenticity", "Finished Authenticity");
		}
		
		if (sp.isNonrepudiation()) {
			Log.i("NonRepudiation", "Starting NonRepudiation");
			NonRepudiation nonrep = new NonRepudiation();
			nonrep.setPublicKey((PublicKey) sdc.getStrangerPublicKeyRSA());
			try {
				byte[] encryptedWithPayload = nonrep.applyPayload(sdata.getContent());
				sdata.setContent(encryptedWithPayload);
				sdata.setPayLoad(nonrep.getPayloadEncrypted());
			} catch (Exception e) {
				Log.i("NonRepudiation", "Failed NonRepudiation");
				e.printStackTrace();
				return null;
			}
			Log.i("NonRepudiation", "Finished NonRepudiation");
		}
		
		return gson.toJson(sdata);
	}
	
	/**
	 * This method takes the secured data and returns back the clear text (the original text) that was secured using the properties
	 * specified in the SecurityPreferences object.
	 * @param sd The secured
	 * @param sp
	 * @return
	 * @throws InvalidKeyException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 */
	public byte[] getClear(SerialData sd, SecurityPreferences sp) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
		if (sp.isNonrepudiation()) {
			Log.i("NonRepudiation", "Starting NonRepudiation");
			NonRepudiation nonrep = new NonRepudiation();
			nonrep.setPrivateKey((PrivateKey) sdc.getMyPrivateKeyRSA());
			try {
				byte[] decryptedWithPayload = nonrep.getClear(sd.getContent(), sd.getPayLoad());
				sd.setContent(decryptedWithPayload);
			} catch (Exception e) {
				Log.i("NonRepudiation", "Failed NonRepudiation");
				e.printStackTrace();
				return null;
			}
			Log.i("NonRepudiation", "Finished NonRepudiation");
		}
		
		if (sp.isAuthenticity()) {
			Log.i("Authenticity", "Starting Authenticity");
			// Get the signature from the object and the signed text, and then verify the signature
			byte[] signature = sd.getSignature();
			byte[] signedText = sd.getContent();
			Authenticity authenticity = new Authenticity();
			authenticity.setAlgorithm(sp.getAuthenticityAlgorithm());
			authenticity.setPublicKey((PublicKey) sdc.getSignatureKeyForReceiver(sp.getAuthenticityAlgorithm()));
			boolean authenticityValid = false;
			try {
				authenticityValid = authenticity.verifySignature(signedText, signature);
			} catch (Exception e) {
				Log.i("Authenticity", "Failed Authenticity");
				e.printStackTrace();
				return null;
			}
			if (!authenticityValid) {
				Log.i("Authenticity", "Failed Authenticity");
				return null;
			}
			Log.i("Authenticity", "Finished Authenticity");
		}
		
		byte[] originalText = null;
		
		if (sp.isConfidentiality()) {
			Log.i("Confidentiality", "Starting Confidentiality");
			// If the Confidentiality is among the Security Properties that were applied, decrypt it
			byte[] encryptedText = sd.getContent();
			Confidentiality confidentiality = new Confidentiality();
			if (sp.getConfidentialityAlgorithm().contains("RSA")) {
				confidentiality.setMode(Confidentiality.ASYMMETRIC_MODE);
				confidentiality.setAsymAlgo(sp.getConfidentialityAlgorithm());
				confidentiality.setPrivateKey((PrivateKey) sdc.getDecryptionKeyForReceiver(sp.getConfidentialityAlgorithm()));
			}
			else if (sp.getConfidentialityAlgorithm().contains("AES") || sp.getConfidentialityAlgorithm().contains("DES")) {
				confidentiality.setMode(Confidentiality.SYMMETRIC_MODE);
				confidentiality.setSymAlgo(sp.getConfidentialityAlgorithm());
				confidentiality.setSecretKey((SecretKey) sdc.getDecryptionKeyForReceiver(sp.getConfidentialityAlgorithm()));
			}
			else {
				Log.i("Confidentiality", "Failed Confidentiality");
				return null;
			}
			try {
				originalText = confidentiality.decrypt(encryptedText);
			} catch (Exception e) {
				Log.i("Confidentiality", "Failed Confidentiality");
				e.printStackTrace();
				return null;
			}
			Log.i("Confidentiality", "Finished Confidentiality");
		}
		else {
			// Otherwise, just get the clear text
			originalText = sd.getContent();
		}
		return originalText;
	}

	
	public static String getConfAlgorithm() {
		return confAlgorithm;
	}


	public static void setConfAlgorithm(String confAlgorithm) {
		RequestManager.confAlgorithm = confAlgorithm;
	}


	public static String getAuthAlgorithm() {
		return authAlgorithm;
	}


	public static void setAuthAlgorithm(String authAlgorithm) {
		RequestManager.authAlgorithm = authAlgorithm;
	}


	public static String getIntAlgorithm() {
		return intAlgorithm;
	}


	public static void setIntAlgorithm(String intAlgorithm) {
		RequestManager.intAlgorithm = intAlgorithm;
	}
	

}
