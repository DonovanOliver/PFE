package fr.unice.proxy.components.security;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * This class is responsible for the Non-Repudiation (The sender cannot deny
 * sending something)
 * 
 * @author andrei
 * 
 */
public class NonRepudiation {

	private int NONCE_LENGTH = 5;
	private int SYM_ALGO_KEY_SIZE = 56;
	
	private String symAlgo = "DES";
	private String asymAlgo = "RSA";
	
	private SecretKey secretKey;
	private PublicKey publicKey;
	private PrivateKey privateKey;
	
	private byte[] payloadEncrypted;
	private byte[] nonce;

	/**
	 * Public constructor by default
	 * @throws NoSuchAlgorithmException 
	 */
	public NonRepudiation(){
	}

	public byte[] applyPayload(byte[] body)
			throws NoSuchAlgorithmException, InvalidKeyException,
			NoSuchPaddingException, IllegalBlockSizeException,
			BadPaddingException {
		// Generate nonce (for payload) and secretkey (for encryption)
		generateNonce(NONCE_LENGTH);
		secretKeyGen();

		// Encrypt the document
		byte[] dataEncrypted = symmetric_encryption(body);

		// Create a payload byteStore the concatenation of the secret key and the nonce 
		byte pl[] = new byte[secretKey.getEncoded().length + nonce.length];

		// Copy the secret key into the payload
		for (int i = 0; i < secretKey.getEncoded().length; i++) {
			pl[i] = (secretKey.getEncoded())[i];
		}

		// Copy the nonce into the payload
		int k = 0;
		for (int j = secretKey.getEncoded().length; (j < (secretKey
				.getEncoded().length + nonce.length)) && (k < nonce.length); j++, k++) {
			pl[j] = nonce[k];
		}
		
		// encrypt the payload and set it
		this.payloadEncrypted = asymmetric_encryption(pl);
		
		// Return the encrypted data with the secret key
		return dataEncrypted;
	}
	
	public byte[] getClear(byte[] encrypted, byte[] payLoad) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
		// Decrypt the payLoad
		byte[] decryptedPayload = asymmetric_decryption(payLoad);
		// Get the Secret Key bytes from the decrypted payload, because it is secretKey concatenated with nonce (secretkey+nonce)
		byte[] skBytes = Arrays.copyOf(decryptedPayload, decryptedPayload.length - NONCE_LENGTH);
		
		// Set the secret key
		this.secretKey = new SecretKeySpec(skBytes, symAlgo);
		
		// Decrypt the content using the reconstructed secret key
		return symmetric_decryption(encrypted);
	}

	/**
	 * This function generates a secret key which will be used for encrypting
	 * 
	 * @throws NoSuchAlgorithmException
	 */
	private void secretKeyGen() throws NoSuchAlgorithmException {
		KeyGenerator keygen = null;
		// We set the algorithm to DES
		keygen = KeyGenerator.getInstance(symAlgo);
		keygen.init(SYM_ALGO_KEY_SIZE);
		// We generate the key
		this.secretKey = keygen.generateKey();
	}
	
	private void generateNonce(int length) {
		StringBuilder sb = new StringBuilder();
		String characters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
		int charactersLength = characters.length();
		for (int i = 0; i < length; i++) {
			double index = Math.random() * charactersLength;
			sb.append(characters.charAt((int) index));
		}
		this.nonce = sb.toString().getBytes();
	}

	/**
	 * Encrypt the given byte buffer using a Public Key
	 * 
	 * @param data
	 *            to encrypt
	 * @param publicKey
	 * @return encrypted byte buffer
	 * @throws NoSuchPaddingException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeyException
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 */
	private byte[] asymmetric_encryption(byte[] data)
			throws NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		// Create the cipher
		Cipher cipher = Cipher.getInstance(asymAlgo);
		// Initialize the cipher for encryption
		cipher.init(Cipher.ENCRYPT_MODE, publicKey);
		// Encrypt the cleartext
		return cipher.doFinal(data);
	}

	/**
	 * Decrypt the given byte buffer using a Private Key
	 * 
	 * @param data
	 *            to encrypt
	 * @param privateKey
	 * @return decrypted byte buffer
	 * @throws NoSuchPaddingException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeyException
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 */
	private byte[] asymmetric_decryption(byte[] data)
			throws NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		// Create the cipher
		Cipher cipher = Cipher.getInstance(asymAlgo);
		// Initialize the cipher for encryption
		cipher.init(Cipher.DECRYPT_MODE, privateKey);
		// Decrypt the encrypted data
		return cipher.doFinal(data);
	}

	/**
	 * This method encrypts a document using the SecretKey generated
	 * 
	 * @param data
	 *            document to encrypt
	 * @return encrypted byte buffer
	 * @throws NoSuchPaddingException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeyException
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 */
	private byte[] symmetric_encryption(byte[] data)
			throws NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		// Create the cipher
		Cipher cipher = Cipher.getInstance(symAlgo);
		// Initialize the cipher for encryption
		cipher.init(Cipher.ENCRYPT_MODE, secretKey);
		// Decrypt the encrypted data
		return cipher.doFinal(data);
	}

	/**
	 * This method encrypts a document using the SecretKey generated
	 * 
	 * @param data
	 *            document to decrypt
	 * @return encrypted byte buffer
	 * @throws NoSuchPaddingException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeyException
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 */
	private byte[] symmetric_decryption(byte[] data)
			throws NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		// Create the cipher
		Cipher cipher = Cipher.getInstance(symAlgo);
		// Initialize the cipher for encryption
		cipher.init(Cipher.DECRYPT_MODE, secretKey);
		// Decrypt the encrypted data
		return cipher.doFinal(data);
	}

	
	public void setPublicKey(PublicKey publicKey) {
		this.publicKey = publicKey;
	}
	
	public void setPrivateKey(PrivateKey privateKey) {
		this.privateKey = privateKey;
	}
	
	public byte[] getPayloadEncrypted() {
		return payloadEncrypted;
	}
}
