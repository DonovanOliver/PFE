package fr.unice.proxy.components.security;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

/**
 * This class is responsible for encrypting/decrypting de data
 * 
 * @author andrei
 * 
 */
public class Confidentiality {

	/**
	 * The mode to be used for encrypting/decrypting
	 */
	private int mode = SYMMETRIC_MODE; // the mode used by default

	/**
	 * The symmetric algorithm to be used for encrypting/decrypting
	 */
	private String symAlgo = "AES"; // symmetric algorithm used by default
	
	/**
	 * The asymmetric algorithm to be used for encrypting/decrypting
	 */
	private String asymAlgo = "RSA"; // asymmetric algorithm used by default

	/**
	 * The Public Key to be used for asymmetric encryption
	 */
	private PublicKey publicKey = null;
	
	/**
	 * The Private Key to be used for asymmetric decryption
	 */
	private PrivateKey privateKey = null;
	
	/**
	 * The Secret Key to be used for symmetric encryption/decryption
	 */
	private SecretKey secretKey = null;

	public static final int SYMMETRIC_MODE = 1;
	public static final int ASYMMETRIC_MODE = 2;

	/**
	 * Public Default Constructor. Uses symmetric encryption/decryption with AES as algorithm by default.
	 */
	public Confidentiality() {
		setMode(SYMMETRIC_MODE);
		setSymAlgo("AES");
	}

	public byte[] encrypt(byte[] clear) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException {
		if (mode == SYMMETRIC_MODE) {
			return symmetricEncrypt(clear);
		} else if (mode == ASYMMETRIC_MODE) {
			return asymmetricEncrypt(clear);
		} else {
			return null;
		}

	}

	public byte[] decrypt(byte[] crypted) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException {
		if (mode == SYMMETRIC_MODE) {
			return symmetricDecrypt(crypted);
		} else if (mode == ASYMMETRIC_MODE) {
			return asymmetricDecrypt(crypted);
		} else {
			return null;
		}
	}

	private byte[] symmetricEncrypt(byte[] clear) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException {
		Cipher aCipher = Cipher.getInstance(getSymAlgo(), "BC");
		aCipher.init(Cipher.ENCRYPT_MODE, getSecretKey());
		return aCipher.doFinal(clear);

	}

	private byte[] symmetricDecrypt(byte[] crypted) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException {
		Cipher aCipher = Cipher.getInstance(getSymAlgo(), "BC");
		aCipher.init(Cipher.DECRYPT_MODE, getSecretKey());
		return aCipher.doFinal(crypted);
	}

	private byte[] asymmetricEncrypt(byte[] clear) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException {
		Cipher aCipher = Cipher.getInstance(getAsymAlgo(), "BC");
		aCipher.init(Cipher.ENCRYPT_MODE, getPublicKey());
		return aCipher.doFinal(clear);

	}

	private byte[] asymmetricDecrypt(byte[] crypted) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException {
		Cipher aCipher = Cipher.getInstance(getAsymAlgo(), "BC");
		aCipher.init(Cipher.DECRYPT_MODE, getPrivateKey());
		return aCipher.doFinal(crypted);

	}

	// -------GETTERS & SETTERS

	public int getMode() {
		return mode;
	}

	public void setMode(int mode) {
		this.mode = mode;
	}

	public String getSymAlgo() {
		return symAlgo;
	}

	public void setSymAlgo(String symAlgo) {
		this.symAlgo = symAlgo;
	}

	public String getAsymAlgo() {
		return asymAlgo;
	}

	public void setAsymAlgo(String asymAlgo) {
		this.asymAlgo = asymAlgo;
	}

	public PublicKey getPublicKey() {
		return publicKey;
	}

	public void setPublicKey(PublicKey publicKey) {
		this.publicKey = publicKey;
	}

	public PrivateKey getPrivateKey() {
		return privateKey;
	}

	public void setPrivateKey(PrivateKey privateKey) {
		this.privateKey = privateKey;
	}
	
	public SecretKey getSecretKey() {
		return secretKey;
	}

	public void setSecretKey(SecretKey secretKey) {
		this.secretKey = secretKey;
	}

}
